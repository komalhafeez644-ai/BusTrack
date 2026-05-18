package ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import ui.admin.TrackDriverActivity
import com.google.android.material.button.MaterialButton

class LiveTrackingActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking)

        supportActionBar?.hide()

        setupBottomNavigation()

        findViewById<MaterialButton>(R.id.btnTrackDriver)?.setOnClickListener {
            val intent = Intent(this, TrackDriverActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupBottomNavigation() {
        // Handle Dashboard click to go back
        findViewById<LinearLayout>(R.id.navDashboard)?.setOnClickListener {
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Live Tracking is already selected here
        findViewById<LinearLayout>(R.id.navLiveTracking)?.setOnClickListener {
            // Already on this screen
        }

        // Add other navigation clicks as needed
        findViewById<LinearLayout>(R.id.navAttendance)?.setOnClickListener {
            // Navigate to Attendance
        }

        findViewById<LinearLayout>(R.id.navAlerts)?.setOnClickListener {
            // Navigate to Alerts
        }

        findViewById<LinearLayout>(R.id.navProfile)?.setOnClickListener {
            // Navigate to Profile
        }
    }
}