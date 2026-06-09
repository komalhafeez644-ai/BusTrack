package ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.bustrack_app.R
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.models.StudentModel
import com.example.bustrack_app.viewmodels.ProfileViewModel
import com.bumptech.glide.Glide
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import ui.admin.*
import ui_authentication.LoginActivity

class ParentDashboardActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private lateinit var drawerLayout: DrawerLayout
    private val profileViewModel: ProfileViewModel by viewModels()
    private var myChild: StudentModel? = null
    
    private val busLocations = listOf(
        Point.fromLngLat(67.0011, 24.8607) to "Bus-01",
        Point.fromLngLat(67.0599, 24.8716) to "Bus-08"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_dashboard)

        supportActionBar?.hide()
        drawerLayout = findViewById(R.id.drawerLayout)

        // Initialize Mapbox Map
        mapView = findViewById(R.id.mapView)
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupMapAnnotations()
        }

        setupUI()
        setupDrawerListeners()
        loadParentAndChildData()
        
        // Handle Drawer opening if coming back from shared screens
        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun setupMapAnnotations() {
        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager()

        busLocations.forEach { (point, busId) ->
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withTextField(busId)
            pointAnnotationManager?.create(pointAnnotationOptions)
        }

        // Set initial camera
        if (busLocations.isNotEmpty()) {
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(busLocations[0].first)
                    .zoom(12.0)
                    .build()
            )
        }

        pointAnnotationManager?.addClickListener { annotation ->
            showChildCardForBus(annotation.textField ?: "")
            true
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    private fun setupUI() {
        findViewById<View>(R.id.btnMenuDrawer)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.btnCloseCard)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            findViewById<View>(R.id.studentCard)?.visibility = View.GONE
        }

        findViewById<View>(R.id.btnTrackBus)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, ParentTrackingActivity::class.java)
            startActivity(intent)
        }
    }

    private fun setupDrawerListeners() {
        findViewById<View>(R.id.drawerImgProfile)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ParentProfileActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerSettings)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, NotificationSettingsActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerPreferences)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, PreferencesActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, PrivacyPolicyActivityActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerTerms)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, TermsConditionsActivity::class.java)
            intent.putExtra("FROM_USER", "parent")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerAttendance)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, StudentAttendanceActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerNotifications)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ParentNotificationsActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerFaq)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ParentFaqActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            Firebase.auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun observeProfileData() {
        profileViewModel.adminData.observe(this) { user ->
            val parentName = user.fullName.ifEmpty { "Parent User" }
            val displayResult = if (parentName.contains("Admin", true)) "Parent User" else parentName
            
            findViewById<TextView>(R.id.tvParentName)?.text = displayResult
            
            // Update Drawer
            findViewById<TextView>(R.id.drawerName)?.text = displayResult
            findViewById<TextView>(R.id.drawerEmail)?.text = user.email
            
            val drawerImageView = findViewById<ImageView>(R.id.drawerImgProfile)
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(drawerImageView)
            }
        }
    }

    private fun loadParentAndChildData() {
        observeProfileData()
        StudentRepository.studentList.observe(this) { students ->
            myChild = students.find { it.name.contains("Ali", true) || it.name.contains("Rohan", true) }
            myChild?.let { updateChildUI(it, "Bus-01") }
        }
    }

    private fun showChildCardForBus(busId: String) {
        myChild?.let { updateChildUI(it, busId) } ?: run {
            Toast.makeText(this, "No child info found", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun updateChildUI(child: StudentModel, busId: String) {
        val card = findViewById<View>(R.id.studentCard)
        findViewById<TextView>(R.id.tvStudentName)?.text = child.name
        findViewById<TextView>(R.id.tvStudentId)?.text = "Student ID: ${child.id}"
        findViewById<TextView>(R.id.tvBusRoute)?.text = "${child.busNo ?: busId} • ${child.route ?: "Route-01"}"
        
        if (child.profileImage != 0) {
            findViewById<ImageView>(R.id.ivStudent)?.setImageResource(child.profileImage)
        }
        card?.visibility = View.VISIBLE
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
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