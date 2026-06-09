package ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.models.NotificationType
import com.example.bustrack_app.models.ParentNotificationModel
import com.google.android.material.button.MaterialButton

class ParentNotificationDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_notification_detail)

        supportActionBar?.hide()

        val notification = intent.getSerializableExtra("notification") as? ParentNotificationModel

        val ivIcon = findViewById<ImageView>(R.id.ivDetailIcon)
        val tvType = findViewById<TextView>(R.id.tvDetailType)
        val tvTitle = findViewById<TextView>(R.id.tvDetailTitle)
        val tvTime = findViewById<TextView>(R.id.tvDetailTime)
        val tvMessage = findViewById<TextView>(R.id.tvDetailMessage)
        val btnAction = findViewById<MaterialButton>(R.id.btnAction)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        notification?.let {
            tvTitle.text = it.title
            tvMessage.text = it.message
            tvTime.text = "Sent ${it.time}"
            tvType.text = it.type.name.replace("_", " ")

            when (it.type) {
                NotificationType.DELAY -> {
                    ivIcon.setImageResource(R.drawable.notifications)
                    ivIcon.setColorFilter(0xFFEF4444.toInt())
                    btnAction.visibility = View.VISIBLE
                    btnAction.text = "Track Live Location"
                }
                NotificationType.ARRIVAL -> {
                    ivIcon.setImageResource(R.drawable.person_check)
                    ivIcon.setColorFilter(0xFF22C55E.toInt())
                    btnAction.visibility = View.GONE
                }
                NotificationType.CANCELLATION -> {
                    ivIcon.setImageResource(R.drawable.warning)
                    ivIcon.setColorFilter(0xFFF97316.toInt())
                    btnAction.visibility = View.VISIBLE
                    btnAction.text = "Contact Transport Office"
                    btnAction.setIconResource(R.drawable.phone_24)
                }
                else -> {
                    ivIcon.setImageResource(R.drawable.notifications)
                    btnAction.visibility = View.GONE
                }
            }
        }

        btnAction.setOnClickListener {
            if (notification?.type == NotificationType.DELAY) {
                startActivity(Intent(this, ParentTrackingActivity::class.java))
            } else {
                // Mock contact
                android.widget.Toast.makeText(this, "Calling support...", android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
