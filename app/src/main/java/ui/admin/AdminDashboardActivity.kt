package ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.example.bustrack_app.R
import ui.admin.*
import utils.NavigationUtils

class AdminDashboardActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        // Hide the action bar if it exists
        supportActionBar?.hide()

        setupClickListeners()
        NavigationUtils.setupBottomNavigation(this)
    }

    private fun setupClickListeners() {
        // --- TOP SECTION ---
        // Notification Icon
        findViewById<android.view.View>(R.id.btnNotifications)?.setOnClickListener {
            startActivity(Intent(this, TransportAlertsActivity::class.java))
        }

        // --- MAIN MENU GRID ---
        // Live Tracking Card
        findViewById<CardView>(R.id.cardLiveTracking)?.setOnClickListener {
            openLiveTracking()
        }

        // Bus Applications Card
        findViewById<CardView>(R.id.cardApplications)?.setOnClickListener {
            startActivity(Intent(this, BusApplicationsActivity::class.java))
        }

        // Attendance Hub Card
        findViewById<CardView>(R.id.cardAttendanceHub)?.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }

        // System Alerts Card (Corrected ID reference if needed)
        findViewById<CardView>(R.id.cardSystemAlerts)?.setOnClickListener {
            startActivity(Intent(this, TransportAlertsActivity::class.java))
        }

        // --- QUICK ACTIONS ---
        // Manage Buses
        findViewById<CardView>(R.id.btnManageBuses)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ManageBusesActivity::class.java))
        }

        // Manage Students
        findViewById<LinearLayout>(R.id.btnManageStudents)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ManageStudentActivity::class.java))
        }

        // Manage Drivers
        findViewById<LinearLayout>(R.id.btnManageDrivers)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, DriversActivity::class.java))
        }

        // Manage Routes
        findViewById<LinearLayout>(R.id.btnManageRoutes)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ManageRouteActivity::class.java))
        }
    }

    private fun openLiveTracking() {
        val intent = Intent(this, LiveTrackingActivity::class.java)
        startActivity(intent)
    }
}