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
import com.google.firebase.firestore.FirebaseFirestore

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertDetailBinding
    private var driverPhoneNumber: String = ""
    private var currentNotif: NotificationModel? = null

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
            val phone = driverPhoneNumber.trim()
            if (phone.isNotBlank()) {
                val cleanNumber = phone.replace(" ", "").replace("-", "")
                val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
                try {
                    startActivity(dialIntent)
                } catch (e: Exception) {
                    Toast.makeText(this, "Unable to open phone dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            } else {
                val notif = currentNotif
                if (notif != null) {
                    Toast.makeText(this, "Looking up driver phone number...", Toast.LENGTH_SHORT).show()
                    resolveDriverPhoneFallback(notif, notif.driverName.ifBlank { "Assigned Driver" }) { resolvedPhone ->
                        if (resolvedPhone.isNotBlank()) {
                            val cleanNumber = resolvedPhone.replace(" ", "").replace("-", "")
                            val dialIntent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber"))
                            try {
                                startActivity(dialIntent)
                            } catch (e: Exception) {
                                Toast.makeText(this, "Unable to open phone dialer: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
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

        // Basic initial assignment
        binding.tvDetailMainTitle.text = alertTitle
        binding.tvDetailDescription.text = alertSubtitle
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

    private fun bindNotificationDetails(notif: NotificationModel) {
        currentNotif = notif
        if (notif.title.isNotBlank()) {
            binding.tvDetailMainTitle.text = notif.title
        }

        val desc = when {
            notif.description.isNotBlank() -> notif.description
            notif.message.isNotBlank() -> notif.message
            else -> binding.tvDetailDescription.text.toString()
        }
        binding.tvDetailDescription.text = desc

        if (notif.busNumber.isNotBlank()) {
            binding.tvDetailVehicle.text = "Bus #${notif.busNumber}"
        }

        if (notif.routeName.isNotBlank()) {
            val directionSuffix = if (notif.tripDirection.isNotBlank()) " (${notif.tripDirection})" else ""
            binding.tvDetailRoute.text = "${notif.routeName}$directionSuffix"
        }

        val driverName = notif.driverName.ifBlank { "Assigned Driver" }

        if (notif.driverPhone.trim().isNotBlank()) {
            driverPhoneNumber = notif.driverPhone.trim()
            binding.tvDetailDriver.text = "$driverName (${notif.driverPhone.trim()})"
        } else {
            binding.tvDetailDriver.text = driverName
            resolveDriverPhoneFallback(notif, driverName)
        }

        val effectiveType = when {
            notif.alertType in listOf("Accident", "Bus Breakdown", "Student Emergency") -> "CRITICAL"
            notif.alertType in listOf("Road Block", "Heavy Traffic", "Fuel Issue", "Bad Weather", "Police Check", "Wrong Route") -> "IMPORTANT"
            notif.type.isNotBlank() -> notif.type
            else -> "GENERAL"
        }
        applyPriorityStyling(effectiveType)
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
