package utils

import android.app.Activity
import android.content.Intent
import android.widget.LinearLayout
import com.example.bustrack_app.R
import ui.admin.*

object NavigationUtils {

    fun setupBottomNavigation(activity: Activity) {
        val navDashboard = activity.findViewById<LinearLayout>(R.id.navDashboard)
        val navLiveTracking = activity.findViewById<LinearLayout>(R.id.navLiveTracking)
        val navAttendance = activity.findViewById<LinearLayout>(R.id.navAttendance)
        val navAlerts = activity.findViewById<LinearLayout>(R.id.navAlerts)
        val navProfile = activity.findViewById<LinearLayout>(R.id.navProfile)

        // Set selected state
        navDashboard?.isSelected = activity is AdminDashboardActivity
        navLiveTracking?.isSelected = activity is LiveTrackingActivity
        navAttendance?.isSelected = activity is AttendanceActivity
        navAlerts?.isSelected = activity is TransportAlertsActivity
        navProfile?.isSelected = activity is ProfileActivity

        // Dashboard click
        navDashboard?.setOnClickListener {
            if (activity !is AdminDashboardActivity) {
                activity.startActivity(Intent(activity, AdminDashboardActivity::class.java))
                activity.finish()
            }
        }

        // Live Tracking click
        navLiveTracking?.setOnClickListener {
            if (activity !is LiveTrackingActivity) {
                activity.startActivity(Intent(activity, LiveTrackingActivity::class.java))
                if (activity !is AdminDashboardActivity) activity.finish()
            }
        }

        // Attendance click
        navAttendance?.setOnClickListener {
            if (activity !is AttendanceActivity) {
                activity.startActivity(Intent(activity, AttendanceActivity::class.java))
                if (activity !is AdminDashboardActivity) activity.finish()
            }
        }

        // Alerts click
        navAlerts?.setOnClickListener {
            if (activity !is TransportAlertsActivity) {
                activity.startActivity(Intent(activity, TransportAlertsActivity::class.java))
                if (activity !is AdminDashboardActivity) activity.finish()
            }
        }

        // Profile click
        navProfile?.setOnClickListener {
            if (activity !is ProfileActivity) {
                activity.startActivity(Intent(activity, ProfileActivity::class.java))
                if (activity !is AdminDashboardActivity) activity.finish()
            }
        }
    }
}
