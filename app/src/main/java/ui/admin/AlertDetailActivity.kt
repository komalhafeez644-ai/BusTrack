package ui.admin

import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.databinding.ActivityAlertDetailBinding
import com.example.bustrack_app.models.NotificationModel
import com.google.firebase.firestore.FirebaseFirestore

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertDetailBinding
    private var driverPhoneNumber: String = ""

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
                Toast.makeText(this, "Driver contact number is not available.", Toast.LENGTH_SHORT).show()
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
                    bindNotificationDetails(notif)
                }
            }
        }
    }

    private fun bindNotificationDetails(notif: NotificationModel) {
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

        if (notif.driverPhone.isNotBlank()) {
            driverPhoneNumber = notif.driverPhone
            binding.tvDetailDriver.text = "$driverName (${notif.driverPhone})"
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

    private fun resolveDriverPhoneFallback(notif: NotificationModel, driverName: String) {
        // 1. Check in DriverRepository cache first
        val cachedDriver = DriverRepository.driverList.value?.find {
            (notif.senderId.isNotBlank() && (it.uid == notif.senderId || it.driverId == notif.senderId || it.id == notif.senderId)) ||
            (notif.driverEmail.isNotBlank() && it.email.trim().equals(notif.driverEmail.trim(), ignoreCase = true)) ||
            (notif.busNumber.isNotBlank() && it.assignedBus.equals(notif.busNumber, ignoreCase = true)) ||
            (notif.driverName.isNotBlank() && it.name.trim().equals(notif.driverName.trim(), ignoreCase = true))
        }

        if (cachedDriver != null && cachedDriver.phone.isNotBlank()) {
            driverPhoneNumber = cachedDriver.phone
            binding.tvDetailDriver.text = "$driverName (${cachedDriver.phone})"
            return
        }

        // 2. Query Firestore 'drivers' or 'users' collection
        val db = FirebaseFirestore.getInstance()
        if (notif.senderId.isNotBlank()) {
            db.collection("users").document(notif.senderId).get().addOnSuccessListener { doc ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                val phone = doc.getString("phone") ?: doc.getString("contactNumber") ?: ""
                if (phone.isNotBlank()) {
                    driverPhoneNumber = phone
                    binding.tvDetailDriver.text = "$driverName ($phone)"
                }
            }
            db.collection("drivers").document(notif.senderId).get().addOnSuccessListener { doc ->
                if (isFinishing || isDestroyed) return@addOnSuccessListener
                val phone = doc.getString("phone") ?: ""
                if (phone.isNotBlank()) {
                    driverPhoneNumber = phone
                    binding.tvDetailDriver.text = "$driverName ($phone)"
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
