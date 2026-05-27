package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.bustrack_app.R
import ui_authentication.LoginActivity
import utils.NavigationUtils

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        supportActionBar?.hide()
        drawerLayout = findViewById(R.id.drawerLayout)

        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }

        setupClickListeners()
        setupDrawerListeners()
    }

    override fun onResume() {
        super.onResume()
        // Always refresh bottom nav state when coming back
        NavigationUtils.setupBottomNavigation(this)
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
            startActivity(Intent(this, LiveTrackingActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<CardView>(R.id.cardApplications)?.setOnClickListener {
            startActivity(Intent(this, BusApplicationsActivity::class.java))
        }

        findViewById<CardView>(R.id.cardAttendanceHub)?.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
            overridePendingTransition(0, 0)
        }

        findViewById<CardView>(R.id.cardSystemAlerts)?.setOnClickListener {
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
            startActivity(Intent(this, ProfileActivity::class.java))
            overridePendingTransition(0, 0)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerSettings)?.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerPreferences)?.setOnClickListener {
            startActivity(Intent(this, PreferencesActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener {
            startActivity(Intent(this, PrivacyPolicyActivityActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerTerms)?.setOnClickListener {
            startActivity(Intent(this, TermsConditionsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerFaq)?.setOnClickListener {
            startActivity(Intent(this, FaqActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener {
            Toast.makeText(this, "Change Password clicked", Toast.LENGTH_SHORT).show()
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
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

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}