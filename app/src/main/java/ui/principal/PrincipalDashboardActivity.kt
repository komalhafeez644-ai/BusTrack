package ui.principal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.bustrack_app.R
import com.example.bustrack_app.viewmodels.ProfileViewModel
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import ui.admin.*
import ui_authentication.LoginActivity

class PrincipalDashboardActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private val profileViewModel: ProfileViewModel by viewModels()
    private var mapView: MapView? = null

    private val busLocations = listOf(
        Point.fromLngLat(67.0011, 24.8607) to "Bus #442-RT",
        Point.fromLngLat(67.0599, 24.8716) to "Bus #108-BT",
        Point.fromLngLat(67.0800, 24.8900) to "Bus #225-XP"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_principal_dashboard)

        supportActionBar?.hide()

        drawerLayout = findViewById(R.id.drawerLayout)
        
        findViewById<View>(R.id.btnMenuDrawer).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }

        // Initialize Mapbox Map
        mapView = findViewById(R.id.mapView)
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupMapAnnotations()
        }

        // Setup Drawer Listeners (Same as Admin)
        setupDrawerListeners()
        observeProfileData()

        // Driver Card Logic
        val driverCard = findViewById<View>(R.id.driverCard)
        findViewById<MaterialButton>(R.id.btnTrackDriver).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, TrackDriverActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.btnCloseCard).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            driverCard.visibility = View.GONE
        }
    }

    private fun setupMapAnnotations() {
        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager()

        busLocations.forEach { (point, busName) ->
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withTextField(busName)
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
            showBusDetails(annotation.textField ?: "Unknown Bus")
            true
        }
    }

    private fun observeProfileData() {
        profileViewModel.adminData.observe(this) { user ->
            // Update Dashboard Header
            findViewById<TextView>(R.id.tvPrincipalName).text = user.fullName
            
            // Update Drawer Header
            findViewById<TextView>(R.id.drawerName)?.text = user.fullName
            findViewById<TextView>(R.id.drawerEmail)?.text = user.email
            
            val profileImageView = findViewById<ImageView>(R.id.ivProfile)
            val drawerImageView = findViewById<ImageView>(R.id.drawerImgProfile)
            
            if (user.profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(profileImageView)
                Glide.with(this).load(user.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(drawerImageView)
            } else {
                profileImageView.setImageResource(R.drawable.ic_person)
                drawerImageView.setImageResource(R.drawable.ic_person)
            }
        }
    }

    private fun setupDrawerListeners() {
        findViewById<View>(R.id.drawerImgProfile)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, ProfileActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
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
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun showBusDetails(busName: String) {
        val driverCard = findViewById<View>(R.id.driverCard)
        findViewById<TextView>(R.id.tvBusInfo).text = "$busName • Route"
        findViewById<TextView>(R.id.tvDriverName).text = if (busName.contains("442")) "Marcus Thompson" else "James Wilson"
        findViewById<TextView>(R.id.tvRouteName).text = if (busName.contains("442")) "North-Alpha" else "South-Central"
        
        driverCard.visibility = View.VISIBLE
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}