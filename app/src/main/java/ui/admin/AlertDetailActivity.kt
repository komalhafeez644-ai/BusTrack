package ui.admin

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivityAlertDetailBinding

class AlertDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAlertDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlertDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Status bar constraints mapping (Aapka favorite primaryDark color)
        window.statusBarColor = Color.parseColor("#051024")
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }
        
        binding.btnContactDriver.setOnClickListener { utils.ViewUtils.applyClickEffect(it) }
        binding.btnViewRoute.setOnClickListener { utils.ViewUtils.applyClickEffect(it) }

        // Intent bundle extras unpacking
        val alertTitle = intent.getStringExtra("ALERT_TITLE") ?: "Alert Details"
        val alertSubtitle = intent.getStringExtra("ALERT_SUBTITLE") ?: ""
        val alertType = intent.getStringExtra("ALERT_TYPE") ?: "GENERAL"

        // 1. Icon Resource ID ko safely get kiya (Default framework error icon as backup)
        val alertIcon = intent.getIntExtra("ALERT_ICON", android.R.drawable.stat_notify_error)

        // Basic details assignment
        binding.tvDetailMainTitle.text = alertTitle
        binding.tvDetailDescription.text = alertSubtitle

        // 2. Received Icon ImageView par apply kar diya
        binding.ivDetailIconBg.setImageResource(alertIcon)

        // DYNAMIC COLOR MECHANISM (Har scenario ko handle karne ka perfect calculation)
        val (headerBgColor, textColor, labelText) = when (alertType.uppercase()) {
            "CRITICAL" -> Triple("#FEE2E2", "#991B1B", "PRIORITY 1 HIGH-ALERT") // Red theme
            "IMPORTANT" -> Triple("#FEF3C7", "#92400E", "PRIORITY 2 IMPORTANT")  // Yellow theme
            "GENERAL" -> Triple("#D1FAE5", "#065F46", "PRIORITY 3 STANDARD")   // Green theme
            else -> Triple("#F1F5F9", "#334155", "STANDARD NOTICE")
        }

        // Apply colors dynamically without any drawables
        binding.cardDetailHeader.setCardBackgroundColor(Color.parseColor(headerBgColor))
        binding.tvDetailMainTitle.setTextColor(Color.parseColor(textColor))
        binding.tvDetailPriorityLabel.setTextColor(Color.parseColor(textColor))
        binding.tvDetailPriorityLabel.text = labelText

        // 3. Icon image view ka tint color (color filter) dynamic adjust kiya scenario ke mutabiq
        binding.ivDetailIconBg.setColorFilter(Color.parseColor(textColor))
    }
}