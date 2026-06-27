package ui.driver

import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import utils.ViewUtils

class DriverAlertDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_alert_detail)

        window.statusBarColor = Color.parseColor("#051024")

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnContactAdmin = findViewById<MaterialButton>(R.id.btnContactAdmin)
        val btnDismiss = findViewById<MaterialButton>(R.id.btnDismiss)
        
        val tvMainTitle = findViewById<TextView>(R.id.tvDetailMainTitle)
        val tvDescription = findViewById<TextView>(R.id.tvDetailDescription)
        val tvTime = findViewById<TextView>(R.id.tvDetailTime)
        val ivIcon = findViewById<ImageView>(R.id.ivDetailIconBg)
        val cardHeader = findViewById<MaterialCardView>(R.id.cardDetailHeader)
        val tvPriority = findViewById<TextView>(R.id.tvDetailPriorityLabel)

        btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        btnContactAdmin.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            Toast.makeText(this, "Calling Admin Office...", Toast.LENGTH_SHORT).show()
        }

        btnDismiss.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        // Get Data from Intent
        val title = intent.getStringExtra("ALERT_TITLE") ?: "Notification"
        val message = intent.getStringExtra("ALERT_MESSAGE") ?: ""
        val timeSent = intent.getStringExtra("ALERT_TIME") ?: ""
        val type = intent.getStringExtra("ALERT_TYPE") ?: "GENERAL"
        val iconRes = intent.getIntExtra("ALERT_ICON", R.drawable.notifications)

        tvMainTitle.text = title
        tvDescription.text = message
        tvTime.text = timeSent
        ivIcon.setImageResource(iconRes)

        // Dynamic Styling
        val (headerBg, textColor, priorityText) = when (type.uppercase()) {
            "CRITICAL" -> Triple("#FEE2E2", "#991B1B", "PRIORITY 1: HIGH-ALERT")
            "IMPORTANT" -> Triple("#FEF3C7", "#92400E", "PRIORITY 2: IMPORTANT")
            else -> Triple("#D1FAE5", "#065F46", "PRIORITY 3: STANDARD")
        }

        cardHeader.setCardBackgroundColor(Color.parseColor(headerBg))
        tvMainTitle.setTextColor(Color.parseColor(textColor))
        tvPriority.setTextColor(Color.parseColor(textColor))
        tvPriority.text = priorityText
        ivIcon.setColorFilter(Color.parseColor(textColor))
    }
}
