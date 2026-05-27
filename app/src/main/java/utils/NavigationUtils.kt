package utils

import android.app.Activity
import android.content.Intent
import android.view.View
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

        // Reset all states first to ensure a clean UI
        val navs = listOf(navDashboard, navLiveTracking, navAttendance, navAlerts, navProfile)
        navs.forEach { it?.isSelected = false }

        // Set the correct selected state based on the current activity
        when (activity) {
            is AdminDashboardActivity -> navDashboard?.isSelected = true
            is LiveTrackingActivity -> navLiveTracking?.isSelected = true
            is AttendanceActivity -> navAttendance?.isSelected = true
            is TransportAlertsActivity -> navAlerts?.isSelected = true
            is ProfileActivity -> navProfile?.isSelected = true
        }

        // --- CLICK LISTENERS ---

        navDashboard?.setOnClickListener {
            if (activity !is AdminDashboardActivity) {
                navigateTo(activity, AdminDashboardActivity::class.java)
            }
        }

        navLiveTracking?.setOnClickListener {
            if (activity !is LiveTrackingActivity) {
                navigateTo(activity, LiveTrackingActivity::class.java)
            }
        }

        navAttendance?.setOnClickListener {
            if (activity !is AttendanceActivity) {
                navigateTo(activity, AttendanceActivity::class.java)
            }
        }

        navAlerts?.setOnClickListener {
            if (activity !is TransportAlertsActivity) {
                navigateTo(activity, TransportAlertsActivity::class.java)
            }
        }

        navProfile?.setOnClickListener {
            if (activity !is ProfileActivity) {
                navigateTo(activity, ProfileActivity::class.java)
            }
        }
    }

    /**
     * Common navigation logic to ensure screens don't flicker and memory is saved
     */
    private fun navigateTo(activity: Activity, target: Class<*>) {
        val intent = Intent(activity, target)
        // FLAG_ACTIVITY_REORDER_TO_FRONT: Brings an existing activity instance to top instead of creating new
        // FLAG_ACTIVITY_SINGLE_TOP: Avoids creating multiple instances if it's already at top
        intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        activity.startActivity(intent)
        // Remove standard "Slide in" animations for a professional "static" tab feel
        activity.overridePendingTransition(0, 0)
    }
}
