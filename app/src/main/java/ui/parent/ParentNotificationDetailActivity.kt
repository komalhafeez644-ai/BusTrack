package ui.parent

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.models.NotificationType
import com.example.bustrack_app.models.ParentNotificationModel
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

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
                // ParentTrackingActivity was a dead/mock screen (fake location, "coming
                // soon" toast). This detail screen doesn't have route/driver context of
                // its own, so we redirect into ParentDashboardActivity, which already has
                // the real, working "Track Driver" flow (TrackDriverActivity, scoped to
                // the parent's approved route) - see ParentDashboardActivity.btnTrackDriver.
                val intent = Intent(this, ParentDashboardActivity::class.java)
                intent.putExtra("OPEN_TRACKING", true)
                startActivity(intent)
                finish()
            } else {
                // "Contact Transport Office" (CANCELLATION type - real TRACKING_REJECTED/
                // TRACKING_REVOKED notifications now route here). Was a fake "Calling
                // support..." toast - now looks up the real admin's phone number
                // (users collection, role == "admin") and opens the actual dialer.
                android.widget.Toast.makeText(this, "Looking up transport office number…", android.widget.Toast.LENGTH_SHORT).show()
                Firebase.firestore.collection("users")
                    .whereEqualTo("role", "admin")
                    .limit(1)
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val phone = snapshot.documents.firstOrNull()?.getString("phone")
                        if (!phone.isNullOrBlank()) {
                            startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                        } else {
                            android.widget.Toast.makeText(
                                this,
                                "Transport office phone number is not set up yet. Please contact the college administration directly.",
                                android.widget.Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    .addOnFailureListener {
                        android.widget.Toast.makeText(this, "Couldn't look up the transport office number. Please try again.", android.widget.Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
