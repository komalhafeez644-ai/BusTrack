package ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import ui.admin.LiveTrackingActivity
import com.example.bustrack_app.R

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Hide the action bar if it exists
        supportActionBar?.hide()

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Live Tracking Card
        findViewById<CardView>(R.id.cardLiveTracking)?.setOnClickListener {
            openLiveTracking()
        }

        // Bottom Navigation - Live Tracking
        findViewById<LinearLayout>(R.id.navLiveTracking)?.setOnClickListener {
            openLiveTracking()
        }

        // Navigation clicks for other items can be added here
        findViewById<LinearLayout>(R.id.navDashboard)?.setOnClickListener {
            // Already on Dashboard
        }
    }

    private fun openLiveTracking() {
        val intent = Intent(this, LiveTrackingActivity::class.java)
        startActivity(intent)
    }
}