package utils

import android.app.Activity
import android.content.Intent
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.bustrack_app.R
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import ui.admin.*
import ui.parent.*
import ui_authentication.LoginActivity

object NavigationUtils {

    fun setupParentDrawer(activity: Activity, drawerLayout: DrawerLayout) {
        val closeDrawer = { drawerLayout.closeDrawer(GravityCompat.END) }

        activity.findViewById<View>(R.id.drawerImgProfile)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (activity !is ParentProfileActivity) {
                val intent = Intent(activity, ParentProfileActivity::class.java)
                activity.startActivity(intent)
            }
            closeDrawer()
        }

        activity.findViewById<View>(R.id.drawerAttendance)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (activity !is StudentAttendanceActivity) {
                val intent = Intent(activity, StudentAttendanceActivity::class.java)
                activity.startActivity(intent)
            }
            closeDrawer()
        }

        activity.findViewById<View>(R.id.drawerNotifications)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (activity !is ParentNotificationsActivity) {
                val intent = Intent(activity, ParentNotificationsActivity::class.java)
                activity.startActivity(intent)
            }
            closeDrawer()
        }

        activity.findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (activity !is PrivacyPolicyActivityActivity) {
                val intent = Intent(activity, PrivacyPolicyActivityActivity::class.java)
                intent.putExtra("FROM_USER", "parent")
                activity.startActivity(intent)
            }
            closeDrawer()
        }

        activity.findViewById<View>(R.id.drawerTerms)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (activity !is TermsConditionsActivity) {
                val intent = Intent(activity, TermsConditionsActivity::class.java)
                intent.putExtra("FROM_USER", "parent")
                activity.startActivity(intent)
            }
            closeDrawer()
        }

        activity.findViewById<View>(R.id.drawerFaq)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (activity !is ParentFaqActivity) {
                val intent = Intent(activity, ParentFaqActivity::class.java)
                activity.startActivity(intent)
            }
            closeDrawer()
        }

        activity.findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (activity !is ChangePasswordActivity) {
                val intent = Intent(activity, ChangePasswordActivity::class.java)
                intent.putExtra("FROM_USER", "parent")
                activity.startActivity(intent)
            }
            closeDrawer()
        }

        activity.findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            Firebase.auth.signOut()
            val intent = Intent(activity, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            activity.startActivity(intent)
            activity.finish()
        }

        loadParentDrawerProfileData(activity)
    }

    private fun loadParentDrawerProfileData(activity: Activity) {
        val uid = Firebase.auth.currentUser?.uid ?: return
        FirebaseRepository.getAdminUser(uid) { user ->
            if (activity.isFinishing || activity.isDestroyed) return@getAdminUser
            if (user != null) {
                activity.findViewById<TextView>(R.id.drawerName)?.text = user.fullName.ifEmpty { "Parent User" }
                activity.findViewById<TextView>(R.id.drawerEmail)?.text = user.email
                val drawerImgProfile = activity.findViewById<ImageView>(R.id.drawerImgProfile)
                if (drawerImgProfile != null && user.profileImageUrl.isNotEmpty()) {
                    ImageUtils.loadProfileImage(activity, user.profileImageUrl, drawerImgProfile)
                }
            }
        }
        FirebaseRepository.getParent(uid) { parent ->
            if (activity.isFinishing || activity.isDestroyed) return@getParent
            if (parent != null && parent.name.isNotEmpty()) {
                activity.findViewById<TextView>(R.id.drawerName)?.text = parent.name
            }
        }
    }

    fun setupBottomNavigation(activity: Activity) {
        val navDashboard = activity.findViewById<LinearLayout>(R.id.navDashboard)
        val navLiveTracking = activity.findViewById<LinearLayout>(R.id.navLiveTracking)
        val navAttendance = activity.findViewById<LinearLayout>(R.id.navAttendance)
        val navRequests = activity.findViewById<LinearLayout>(R.id.navRequests)
        val navAlerts = activity.findViewById<LinearLayout>(R.id.navAlerts)

        // Reset all states first to ensure a clean UI
        val navs = listOf(navDashboard, navLiveTracking, navAttendance, navRequests, navAlerts)
        navs.forEach { it?.isSelected = false }

        // Set the correct selected state based on the current activity
        when (activity) {
            is AdminDashboardActivity -> navDashboard?.isSelected = true
            is LiveTrackingActivity -> navLiveTracking?.isSelected = true
            is AttendanceActivity -> navAttendance?.isSelected = true
            is TrackingRequestsActivity -> navRequests?.isSelected = true
            is TransportAlertsActivity -> navAlerts?.isSelected = true
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

        navRequests?.setOnClickListener {
            if (activity !is TrackingRequestsActivity) {
                navigateTo(activity, TrackingRequestsActivity::class.java)
            }
        }

        navAlerts?.setOnClickListener {
            if (activity !is TransportAlertsActivity) {
                navigateTo(activity, TransportAlertsActivity::class.java)
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
