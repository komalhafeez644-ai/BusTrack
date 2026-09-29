package ui.admin

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.BusRepository
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.databinding.ActivityAlertDetailBinding
import com.example.bustrack_app.models.NotificationModel
import utils.NavigationUtils
import com.google.firebase.firestore.FirebaseFirestore

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertDetailBinding
    private var driverPhoneNumber: String = ""
    private var parentPhoneNumber: String = ""
    private var currentNotif: NotificationModel? = null
    private var parentLookupInProgress = false
    private val pendingParentPhoneCallbacks = mutableListOf<(String) -> Unit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar color
        window.statusBarColor = getColor(R.color.primaryDark)
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        binding.btnContactDriver.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val notif = currentNotif
            if (notif != null && notif.type.equals("TRACKING_REQUEST", ignoreCase = true)) {
                if (parentPhoneNumber.isNotBlank()) {
                    openDialer(parentPhoneNumber)
                } else {
                    Toast.makeText(this, "Looking up parent phone number...", Toast.LENGTH_SHORT).show()
                    resolveTrackingRequestParent(notif) { phone ->
                        if (phone.isNotBlank()) openDialer(phone)
                        else Toast.makeText(this, "Parent contact number is not available.", Toast.LENGTH_SHORT).show()
                    }
                }
                return@setOnClickListener
            }
            val phone = driverPhoneNumber.trim()
            if (phone.isNotBlank()) {
                openDialer(phone)
            } else {
                val notif = currentNotif
                if (notif != null) {
                    Toast.makeText(this, "Looking up driver phone number...", Toast.LENGTH_SHORT).show()
                    resolveDriverPhoneFallback(notif, notif.driverName.ifBlank { "Assigned Driver" }) { resolvedPhone ->
                        if (resolvedPhone.isNotBlank()) {
                            openDialer(resolvedPhone)
                        } else {
                            Toast.makeText(this, "Driver contact number is not available.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Driver contact number is not available.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnViewRoute.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, LiveTrackingActivity::class.java)
            startActivity(intent)
        }

        // Intent bundle extras unpacking (initial fallback)
        val notifId = intent.getStringExtra("NOTIFICATION_ID") ?: ""
        val alertTitle = intent.getStringExtra("ALERT_TITLE") ?: "Alert Details"
        val alertSubtitle = intent.getStringExtra("ALERT_SUBTITLE") ?: ""
        val alertType = intent.getStringExtra("ALERT_TYPE") ?: "GENERAL"
        val alertIcon = intent.getIntExtra("ALERT_ICON", android.R.drawable.stat_notify_error)
        val notificationKind = intent.getStringExtra("NOTIFICATION_KIND")
            ?: if (alertTitle.contains("Tracking Request", ignoreCase = true) ||
                alertSubtitle.contains("tracking request for Roll Number", ignoreCase = true)) "TRACKING_REQUEST" else "GENERAL"
        val relatedId = intent.getStringExtra("RELATED_ID").orEmpty()
        parentPhoneNumber = intent.getStringExtra("PARENT_PHONE").orEmpty()
        if (notificationKind.equals("TRACKING_REQUEST", ignoreCase = true)) {
            currentNotif = NotificationModel(
                type = "TRACKING_REQUEST",
                relatedId = relatedId,
                message = alertSubtitle,
                parentPhone = parentPhoneNumber
            )
            configureTrackingRequestActions()
            if (parentPhoneNumber.isBlank()) {
                resolveTrackingRequestParent(currentNotif!!) { parentPhoneNumber = it }
            }
        }

        // Basic initial assignment
        binding.tvDetailMainTitle.text = alertTitle
        binding.tvDetailDescription.text = alertSubtitle
        binding.tvDetailTime.text = "Time unavailable"
        binding.ivDetailIconBg.setImageResource(alertIcon)
        applyPriorityStyling(alertType)

        // If a real Firestore Notification ID was passed, fetch the full structured model
        if (notifId.isNotBlank()) {
            FirebaseRepository.getNotificationById(notifId) { notif ->
                if (notif != null && !isFinishing && !isDestroyed) {
                    currentNotif = notif
                    bindNotificationDetails(notif)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationUtils.setupBottomNavigation(this)
    }

    private fun bindNotificationDetails(notif: NotificationModel) {
        currentNotif = notif
        val isTrackingRequest = notif.type.equals("TRACKING_REQUEST", ignoreCase = true)
        if (isTrackingRequest) {
            configureTrackingRequestActions()
            if (notif.parentPhone.isNotBlank()) {
                parentPhoneNumber = notif.parentPhone
            } else if (parentPhoneNumber.isBlank()) {
                resolveTrackingRequestParent(notif) { phone -> parentPhoneNumber = phone }
            }
            // Do not try to resolve or display driver metadata for a parent request.
            binding.tvDetailVehicle.text = "Not available"
            binding.tvDetailRoute.text = "Not available"
            binding.tvDetailDriver.text = "Not available"
            binding.tvDetailMainTitle.text = notif.title.ifBlank { "New Tracking Request" }
            binding.tvDetailDescription.text = notif.message.ifBlank { notif.description }
            binding.tvDetailTime.text = notif.timestamp?.let {
                java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(it)
            } ?: "Time unavailable"
            return
        }
        // Older duty notifications only stored their details in the message. Recover
        // those values so the detail view does not leave the layout's sample labels.
        val dutyParts = Regex("Driver\\s+(.+?)\\s+\\(Bus\\s+([^,]+),\\s*Route:\\s*(.+?)\\)", RegexOption.IGNORE_CASE)
            .find(notif.message)
        val resolvedDriverName = notif.driverName.ifBlank { dutyParts?.groupValues?.getOrNull(1).orEmpty() }
        val resolvedBusNumber = notif.busNumber.ifBlank { dutyParts?.groupValues?.getOrNull(2).orEmpty() }
        val resolvedRouteName = notif.routeName.ifBlank {
            dutyParts?.groupValues?.getOrNull(3)?.trim()
                ?: notif.relatedId.takeIf { it.isNotBlank() }.orEmpty()
        }
        val contactNotification = notif.copy(
            driverName = resolvedDriverName,
            busNumber = resolvedBusNumber,
            routeName = resolvedRouteName
        )
        currentNotif = contactNotification
        if (notif.title.isNotBlank()) {
            binding.tvDetailMainTitle.text = notif.title
        }
        binding.tvDetailTime.text = notif.timestamp?.let {
            java.text.SimpleDateFormat("dd MMM yyyy, hh:mm a", java.util.Locale.getDefault()).format(it)
        } ?: "Time unavailable"

        val desc = when {
            notif.description.isNotBlank() -> notif.description
            notif.message.isNotBlank() -> notif.message
            else -> binding.tvDetailDescription.text.toString()
        }
        binding.tvDetailDescription.text = desc

        binding.tvDetailVehicle.text = resolvedBusNumber.takeIf { it.isNotBlank() }?.let { "Bus #$it" } ?: "Not available"

        if (resolvedRouteName.isNotBlank()) {
            val directionSuffix = if (notif.tripDirection.isNotBlank()) " (${notif.tripDirection})" else ""
            binding.tvDetailRoute.text = "$resolvedRouteName$directionSuffix"
        } else {
            binding.tvDetailRoute.text = "Not available"
        }

        val driverName = resolvedDriverName.ifBlank { "Assigned Driver" }

        if (notif.driverPhone.trim().isNotBlank()) {
            driverPhoneNumber = notif.driverPhone.trim()
            binding.tvDetailDriver.text = "$driverName (${notif.driverPhone.trim()})"
        } else {
            binding.tvDetailDriver.text = driverName
            resolveDriverPhoneFallback(contactNotification, driverName)
        }

        val effectiveType = when {
            notif.alertType in listOf("Accident", "Bus Breakdown", "Student Emergency") -> "CRITICAL"
            notif.alertType in listOf("Road Block", "Heavy Traffic", "Fuel Issue", "Bad Weather", "Police Check", "Wrong Route") -> "IMPORTANT"
            notif.type.isNotBlank() -> notif.type
            else -> "GENERAL"
        }
        applyPriorityStyling(effectiveType)
    }

    private fun configureTrackingRequestActions() {
        binding.cardTransportDetails.visibility = android.view.View.GONE
        binding.btnContactDriver.text = "Contact Parent"
        binding.btnViewRoute.visibility = android.view.View.GONE
    }

    private fun openDialer(phone: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone.trim(), null)))
        } catch (e: Exception) {
            Toast.makeText(this, "Unable to open phone dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resolveTrackingRequestParent(notif: NotificationModel, onResolved: (String) -> Unit) {
        if (parentPhoneNumber.isNotBlank()) {
            onResolved(parentPhoneNumber)
            return
        }
        pendingParentPhoneCallbacks.add(onResolved)
        if (parentLookupInProgress) return
        parentLookupInProgress = true

        var completed = false
        fun finish(phone: String) {
            if (completed) return
            completed = true
            parentLookupInProgress = false
            if (phone.isNotBlank()) parentPhoneNumber = phone
            val callbacks = pendingParentPhoneCallbacks.toList()
            pendingParentPhoneCallbacks.clear()
            callbacks.forEach { it(parentPhoneNumber) }
        }

        val db = FirebaseFirestore.getInstance()
        val requestId = notif.relatedId.trim()
        val rollNumber = notif.parentRollNumber.ifBlank {
            Regex("Roll Number\\s+([A-Za-z0-9_-]+)", RegexOption.IGNORE_CASE)
                .find(notif.message)?.groupValues?.getOrNull(1).orEmpty()
        }

        fun lookupParentProfile(parentId: String, fallbackPhone: String = "") {
            if (fallbackPhone.isNotBlank()) {
                finish(fallbackPhone)
                return
            }
            if (parentId.isBlank()) {
                finish("")
                return
            }
            db.collection("parents").document(parentId).get()
                .addOnSuccessListener { parent ->
                    finish(parent.getString("phone") ?: parent.getString("contactNumber") ?: "")
                }
                .addOnFailureListener { finish("") }
        }

        fun lookupRequestByRollNumber() {
            if (rollNumber.isBlank()) {
                finish("")
                return
            }
            db.collection("trackingRequests")
                .whereEqualTo("rollNumber", rollNumber.trim().uppercase())
                .limit(1)
                .get()
                .addOnSuccessListener { requests ->
                    val request = requests.documents.firstOrNull()
                    if (request == null) {
                        finish("")
                    } else {
                        lookupParentProfile(
                            request.getString("parentId").orEmpty(),
                            request.getString("phone").orEmpty()
                        )
                    }
                }
                .addOnFailureListener { finish("") }
        }

        if (requestId.isNotBlank()) {
            db.collection("trackingRequests").document(requestId).get()
                .addOnSuccessListener { request ->
                    if (request.exists()) {
                        lookupParentProfile(
                            request.getString("parentId").orEmpty(),
                            request.getString("phone").orEmpty()
                        )
                    } else {
                        lookupRequestByRollNumber()
                    }
                }
                .addOnFailureListener { lookupRequestByRollNumber() }
        } else {
            lookupRequestByRollNumber()
        }
    }

    private fun resolveDriverPhoneFallback(
        notif: NotificationModel,
        driverName: String,
        onResolved: ((String) -> Unit)? = null
    ) {
        fun onFound(phone: String) {
            if (phone.isNotBlank()) {
                driverPhoneNumber = phone
                if (!isFinishing && !isDestroyed) {
                    binding.tvDetailDriver.text = "$driverName ($phone)"
                }
                // Backfill the notification document in Firestore if id is present
                if (notif.id.isNotBlank()) {
                    FirebaseFirestore.getInstance().collection("notifications")
                        .document(notif.id)
                        .update("driverPhone", phone)
                }
                onResolved?.invoke(phone)
            }
        }

        // 1. Check in DriverRepository cache
        val allDrivers = DriverRepository.driverList.value ?: emptyList()
        val cachedDriver = allDrivers.find {
            (notif.senderId.isNotBlank() && (it.uid == notif.senderId || it.driverId == notif.senderId || it.id == notif.senderId)) ||
            (notif.driverEmail.isNotBlank() && it.email.trim().equals(notif.driverEmail.trim(), ignoreCase = true)) ||
            (notif.busNumber.isNotBlank() && it.assignedBus.equals(notif.busNumber, ignoreCase = true)) ||
            (notif.driverName.isNotBlank() && it.name.trim().equals(notif.driverName.trim(), ignoreCase = true)) ||
            (notif.routeName.isNotBlank() && it.route.equals(notif.routeName, ignoreCase = true))
        }

        if (cachedDriver != null && cachedDriver.phone.trim().isNotBlank()) {
            onFound(cachedDriver.phone.trim())
            return
        }

        // 2. Check BusRepository / RouteRepository caches to find driver name if bus or route is known
        val busAssignedDriver = if (notif.busNumber.isNotBlank()) {
            BusRepository.getBusByNumber(notif.busNumber)?.driverName
        } else null
        val routeAssignedDriver = if (notif.routeName.isNotBlank()) {
            RouteRepository.routeList.value?.find { it.routeName == notif.routeName }?.driverName
        } else null

        val secondaryDriverName = busAssignedDriver ?: routeAssignedDriver
        if (!secondaryDriverName.isNullOrBlank()) {
            val secondaryDriver = allDrivers.find { it.name.trim().equals(secondaryDriverName.trim(), ignoreCase = true) }
            if (secondaryDriver != null && secondaryDriver.phone.trim().isNotBlank()) {
                onFound(secondaryDriver.phone.trim())
                return
            }
        }

        // 3. Multi-strategy Firestore queries (users and drivers collections)
        val db = FirebaseFirestore.getInstance()
        val queryActions = mutableListOf<((String) -> Unit) -> Unit>()

        // 3a. Queries by senderId
        if (notif.senderId.isNotBlank()) {
            // users collection keyed by UID
            queryActions.add { cb ->
                db.collection("users").document(notif.senderId).get()
                    .addOnSuccessListener { doc ->
                        val phone = doc.getString("phone") ?: doc.getString("contactNumber") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
            // drivers collection keyed by Employee ID
            queryActions.add { cb ->
                db.collection("drivers").document(notif.senderId).get()
                    .addOnSuccessListener { doc ->
                        val phone = doc.getString("phone") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
            // drivers collection where uid == senderId
            queryActions.add { cb ->
                db.collection("drivers").whereEqualTo("uid", notif.senderId).limit(1).get()
                    .addOnSuccessListener { snap ->
                        val phone = snap.documents.firstOrNull()?.getString("phone") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
        }

        // 3b. Queries by driverEmail
        if (notif.driverEmail.isNotBlank()) {
            val emailLower = notif.driverEmail.trim().lowercase()
            queryActions.add { cb ->
                db.collection("users").whereEqualTo("email", emailLower).limit(1).get()
                    .addOnSuccessListener { snap ->
                        val phone = snap.documents.firstOrNull()?.getString("phone") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
            queryActions.add { cb ->
                db.collection("drivers").whereEqualTo("email", emailLower).limit(1).get()
                    .addOnSuccessListener { snap ->
                        val phone = snap.documents.firstOrNull()?.getString("phone") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
        }

        // 3c. Queries by busNumber
        if (notif.busNumber.isNotBlank()) {
            queryActions.add { cb ->
                db.collection("drivers").whereEqualTo("assignedBus", notif.busNumber).limit(1).get()
                    .addOnSuccessListener { snap ->
                        val phone = snap.documents.firstOrNull()?.getString("phone") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
        }

        // 3d. Queries by driverName (if non-generic)
        if (notif.driverName.isNotBlank() && !notif.driverName.equals("Driver", true) && !notif.driverName.equals("Assigned Driver", true)) {
            queryActions.add { cb ->
                db.collection("drivers").whereEqualTo("name", notif.driverName.trim()).limit(1).get()
                    .addOnSuccessListener { snap ->
                        val phone = snap.documents.firstOrNull()?.getString("phone") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
            queryActions.add { cb ->
                db.collection("users").whereEqualTo("fullName", notif.driverName.trim()).limit(1).get()
                    .addOnSuccessListener { snap ->
                        val phone = snap.documents.firstOrNull()?.getString("phone") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
        }

        // 3e. Queries by routeName
        if (notif.routeName.isNotBlank()) {
            queryActions.add { cb ->
                db.collection("drivers").whereEqualTo("route", notif.routeName).limit(1).get()
                    .addOnSuccessListener { snap ->
                        val phone = snap.documents.firstOrNull()?.getString("phone") ?: ""
                        cb(phone)
                    }
                    .addOnFailureListener { cb("") }
            }
        }

        if (queryActions.isEmpty()) {
            onResolved?.invoke("")
            return
        }

        var isResolved = false
        var completedQueries = 0
        val totalQueries = queryActions.size

        queryActions.forEach { queryAction ->
            queryAction { foundPhone ->
                if (isFinishing || isDestroyed) return@queryAction
                synchronized(this@AlertDetailActivity) {
                    if (isResolved) return@synchronized
                    val cleanPhone = foundPhone.trim()
                    if (cleanPhone.isNotBlank()) {
                        isResolved = true
                        onFound(cleanPhone)
                        return@synchronized
                    }
                    completedQueries++
                    if (completedQueries >= totalQueries && !isResolved) {
                        onResolved?.invoke("")
                    }
                }
            }
        }
    }

    private fun applyPriorityStyling(alertType: String) {
        val (headerBgColor, textColor, labelText) = when (alertType.uppercase()) {
            "CRITICAL" -> Triple("#FEE2E2", "#991B1B", "PRIORITY 1 HIGH-ALERT")
            "IMPORTANT" -> Triple("#FEF3C7", "#92400E", "PRIORITY 2 IMPORTANT")
            "GENERAL" -> Triple("#D1FAE5", "#065F46", "PRIORITY 3 STANDARD")
            else -> Triple("#F1F5F9", "#334155", "STANDARD NOTICE")
        }

        binding.cardDetailHeader.setCardBackgroundColor(Color.parseColor(headerBgColor))
        binding.tvDetailMainTitle.setTextColor(Color.parseColor(textColor))
        binding.tvDetailPriorityLabel.setTextColor(Color.parseColor(textColor))
        binding.tvDetailPriorityLabel.text = labelText
        binding.ivDetailIconBg.setColorFilter(Color.parseColor(textColor))
    }
}
