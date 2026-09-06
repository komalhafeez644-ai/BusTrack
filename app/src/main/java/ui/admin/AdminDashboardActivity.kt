package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.bustrack_app.R
import com.example.bustrack_app.viewmodels.ProfileViewModel
import com.bumptech.glide.Glide
import ui_authentication.LoginActivity
import utils.NavigationUtils

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val profileViewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        supportActionBar?.hide()
        drawerLayout = findViewById(R.id.drawerLayout)

        // Set initial greeting before data loads
        findViewById<TextView>(R.id.tvGreeting)?.text = "${getGreeting().uppercase()}, 👋"

        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        setupClickListeners()
        setupDrawerListeners()
        observeProfileData()
        observeDashboardStats()
    }

    private fun observeDashboardStats() {
        // Observe Tracking Requests for count
        com.example.bustrack_app.data.FirebaseRepository.fetchTrackingRequests { requests ->
            val unseenCount = requests.count { !it.isSeenByAdmin && it.status.uppercase() == "PENDING" }
            val tvBadge = findViewById<TextView>(R.id.tvPendingRequestsCount)
            if (unseenCount > 0) {
                tvBadge?.text = unseenCount.toString()
                tvBadge?.visibility = View.VISIBLE
            } else {
                tvBadge?.visibility = View.GONE
            }
        }
    }

    private fun observeProfileData() {
        profileViewModel.adminData.observe(this) { admin ->
            // Update Dashboard Header
            findViewById<TextView>(R.id.tvGreeting)?.text = "${getGreeting().uppercase()}, 👋"
            
            val nameToShow = admin.fullName.ifEmpty { "System Admin" }
            findViewById<TextView>(R.id.tvAdminName)?.text = nameToShow
            
            // Update Drawer Header
            findViewById<TextView>(R.id.drawerName)?.text = nameToShow
            findViewById<TextView>(R.id.drawerEmail)?.text = admin.email
            
            val profileImageView = findViewById<ImageView>(R.id.ivProfile)
            val drawerImageView = findViewById<ImageView>(R.id.drawerImgProfile)
            
            utils.ImageUtils.loadProfileImage(this, admin.profileImageUrl, profileImageView)
            utils.ImageUtils.loadProfileImage(this, admin.profileImageUrl, drawerImageView)
        }
    }

    private fun setupDrawerHeader() {
        // This is now handled in observeProfileData()
    }

    override fun onResume() {
        super.onResume()
        // Always refresh bottom nav state when coming back
        NavigationUtils.setupBottomNavigation(this)
        
        // Update greeting based on current time
        findViewById<TextView>(R.id.tvGreeting)?.text = "${getGreeting().uppercase()}, 👋"
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            findViewById<DrawerLayout>(R.id.drawerLayout)?.openDrawer(GravityCompat.END)
        }
    }

    private fun setupClickListeners() {
        findViewById<View>(R.id.btnMenuDrawer)?.setOnClickListener {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<CardView>(R.id.cardLiveTracking)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, LiveTrackingActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<CardView>(R.id.cardTrackingRequests)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, TrackingRequestsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardAttendanceHub)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, AttendanceActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<CardView>(R.id.cardSystemAlerts)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, TransportAlertsActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<CardView>(R.id.btnManageBuses)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ManageBusesActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnManageStudents)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ManageStudentActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnManageDrivers)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, DriversActivity::class.java))
        }

        findViewById<LinearLayout>(R.id.btnManageRoutes)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ManageRouteActivity::class.java))
        }
    }

    private fun setupDrawerListeners() {
        findViewById<View>(R.id.drawerImgProfile)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

/* Hide Preferences & Settings per Task requirements
        findViewById<View>(R.id.drawerSettings)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, NotificationSettingsActivity::class.java)
            intent.putExtra("FROM_USER", "admin")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerPreferences)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, PreferencesActivity::class.java)
            intent.putExtra("FROM_USER", "admin")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
*/

        findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, PrivacyPolicyActivityActivity::class.java)
            intent.putExtra("FROM_USER", "admin")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerTerms)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, TermsConditionsActivity::class.java)
            intent.putExtra("FROM_USER", "admin")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerFaq)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, FaqActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, ChangePasswordActivity::class.java)
            intent.putExtra("FROM_USER", "admin")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout_confirmation)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        dialog.setCancelable(true)

        val btnConfirm = dialog.findViewById<View>(R.id.btnConfirmLogout)
        val btnCancel = dialog.findViewById<View>(R.id.btnCancelLogout)

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            com.example.bustrack_app.data.FirebaseRepository.stopUnreadCountListener()
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun getGreeting(): String {
        val calendar = java.util.Calendar.getInstance()
        return when (calendar.get(java.util.Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..15 -> "Good Afternoon"
            in 16..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}