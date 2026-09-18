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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.RecentActivityAdapter
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.AdminRecentActivity
import com.example.bustrack_app.models.NotificationModel
import com.example.bustrack_app.viewmodels.ProfileViewModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import ui_authentication.LoginActivity
import utils.FormUtils
import utils.NavigationUtils
import utils.ViewUtils

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val profileViewModel: ProfileViewModel by viewModels()

    private var isQuickActionsExpanded = false
    private var isRecentActivitiesExpanded = false
    private lateinit var recentActivityAdapter: RecentActivityAdapter
    private val retrievedActivities = mutableListOf<AdminRecentActivity>()
    private var notificationListeners: List<ListenerRegistration> = emptyList()

    private val sampleRecentActivities = listOf(
        AdminRecentActivity(
            id = "",
            title = "Route 42 Completed",
            description = "Successfully reached all 14 stops...",
            timeAgo = "2 mins ago",
            iconRes = R.drawable.ic_assignment,
            iconTint = 0xFF4CAF50.toInt(),
            bgTint = 0xFFE8F5E9.toInt()
        ),
        AdminRecentActivity(
            id = "",
            title = "Delay Detected: Bus #108",
            description = "Heavy traffic on Main St. Estimate...",
            timeAgo = "15 mins ago",
            iconRes = R.drawable.ic_lock,
            iconTint = 0xFFF44336.toInt(),
            bgTint = 0xFFFFF1F1.toInt()
        )
    )

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
        updateQuickActionsUI()
        setupRecentActivities()
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

        // Observe Buses for Active/Total count
        com.example.bustrack_app.data.BusRepository.busList.observe(this) { buses ->
            val totalBuses = buses.size
            val activeBuses = buses.count { it.status.uppercase() == "ACTIVE" }
            findViewById<TextView>(R.id.tvActiveBusesCount)?.text = "$activeBuses / $totalBuses"
        }

        // Observe Students for total count
        com.example.bustrack_app.data.StudentRepository.studentList.observe(this) { students ->
            findViewById<TextView>(R.id.tvTotalStudentsCount)?.text = students.size.toString()
        }

        // Observe Routes for total count
        com.example.bustrack_app.data.RouteRepository.routeList.observe(this) { routes ->
            findViewById<TextView>(R.id.tvTotalRoutesCount)?.text = routes.size.toString()
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

        findViewById<TextView>(R.id.tvQuickActionsSeeAll)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            isQuickActionsExpanded = !isQuickActionsExpanded
            updateQuickActionsUI()
        }

        findViewById<View>(R.id.layoutManageBuses)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ManageBusesActivity::class.java))
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

        findViewById<View>(R.id.drawerEveningAttendance)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, AttendanceActivity::class.java))
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

    private fun updateQuickActionsUI() {
        val btnManageDrivers = findViewById<View>(R.id.btnManageDrivers)
        val btnManageRoutes = findViewById<View>(R.id.btnManageRoutes)
        val quickActionsLayout = findViewById<LinearLayout>(R.id.quickActionsLayout)
        val tvQuickActionsSeeAll = findViewById<TextView>(R.id.tvQuickActionsSeeAll)

        if (isQuickActionsExpanded) {
            btnManageDrivers?.visibility = View.VISIBLE
            btnManageRoutes?.visibility = View.VISIBLE
            quickActionsLayout?.weightSum = 4f
            tvQuickActionsSeeAll?.text = "SHOW LESS"
        } else {
            btnManageDrivers?.visibility = View.GONE
            btnManageRoutes?.visibility = View.GONE
            quickActionsLayout?.weightSum = 2f
            tvQuickActionsSeeAll?.text = "SEE ALL"
        }
    }

    private fun setupRecentActivities() {
        val rv = findViewById<RecyclerView>(R.id.rvRecentActivities)
        rv?.layoutManager = LinearLayoutManager(this)

        recentActivityAdapter = RecentActivityAdapter(emptyList()) { item ->
            if (item.id.isNotBlank()) {
                FirebaseRepository.markNotificationRead(item.id)
            }
            val intent = Intent(this, AlertDetailActivity::class.java).apply {
                putExtra("NOTIFICATION_ID", item.id)
                putExtra("ALERT_TITLE", item.title)
                putExtra("ALERT_SUBTITLE", item.description)
                putExtra("ALERT_TYPE", "GENERAL")
                putExtra("ALERT_ICON", item.iconRes)
            }
            startActivity(intent)
        }
        rv?.adapter = recentActivityAdapter

        findViewById<TextView>(R.id.tvRecentActivityViewAll)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            isRecentActivitiesExpanded = !isRecentActivitiesExpanded
            updateRecentActivitiesUI()
        }

        updateRecentActivitiesUI()
        observeRecentActivities()
    }

    private fun updateRecentActivitiesUI() {
        val tvViewAll = findViewById<TextView>(R.id.tvRecentActivityViewAll)
        val activitiesToDisplay = if (retrievedActivities.isNotEmpty()) {
            retrievedActivities
        } else {
            sampleRecentActivities
        }

        val displayList = if (isRecentActivitiesExpanded) {
            activitiesToDisplay
        } else {
            activitiesToDisplay.take(2)
        }

        recentActivityAdapter.update(displayList)
        tvViewAll?.text = if (isRecentActivitiesExpanded) "SHOW LESS" else "VIEW ALL"
        tvViewAll?.visibility = View.VISIBLE
    }

    private fun observeRecentActivities() {
        val uid = Firebase.auth.currentUser?.uid ?: ""
        notificationListeners = FirebaseRepository.listenToNotifications(uid, "admin") { notifications ->
            retrievedActivities.clear()
            retrievedActivities.addAll(notifications.map { notif ->
                val (iconRes, iconTint, bgTint) = mapNotificationToVisuals(notif)
                AdminRecentActivity(
                    id = notif.id,
                    title = notif.title.ifBlank { "Activity Alert" },
                    description = notif.message.ifBlank { notif.description.ifBlank { "Recent activity update" } },
                    timeAgo = FormUtils.timeAgo(notif.timestamp),
                    iconRes = iconRes,
                    iconTint = iconTint,
                    bgTint = bgTint
                )
            })
            updateRecentActivitiesUI()
        }
    }

    private fun mapNotificationToVisuals(notif: NotificationModel): Triple<Int, Int, Int> {
        return when {
            notif.type == NotificationModel.TYPE_DRIVER_ALERT || notif.alertType.isNotBlank() -> {
                when (notif.alertType) {
                    "Accident", "Bus Breakdown", "Student Emergency" ->
                        Triple(R.drawable.warning, 0xFFF44336.toInt(), 0xFFFFF1F1.toInt())
                    "Road Block", "Heavy Traffic", "Fuel Issue", "Bad Weather", "Police Check", "Wrong Route" ->
                        Triple(R.drawable.notification_active, 0xFFD97706.toInt(), 0xFFFEF3C7.toInt())
                    else ->
                        Triple(R.drawable.notification_active, 0xFF0284C7.toInt(), 0xFFE0F2FE.toInt())
                }
            }
            notif.type == "ATTENDANCE" || notif.type == NotificationModel.TYPE_EMERGENCY ->
                Triple(R.drawable.person_check, 0xFFF44336.toInt(), 0xFFFFF1F1.toInt())
            notif.type == "TRACKING_REQUEST" ->
                Triple(R.drawable.security_shield, 0xFFD97706.toInt(), 0xFFFEF3C7.toInt())
            notif.type == NotificationModel.TYPE_IMPORTANT ->
                Triple(R.drawable.notification_active, 0xFFD97706.toInt(), 0xFFFEF3C7.toInt())
            notif.type.contains("ROUTE") || notif.type.contains("TRIP") ->
                Triple(R.drawable.ic_assignment, 0xFF4CAF50.toInt(), 0xFFE8F5E9.toInt())
            else ->
                Triple(R.drawable.notifications, 0xFF0284C7.toInt(), 0xFFE0F2FE.toInt())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationListeners.forEach { it.remove() }
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