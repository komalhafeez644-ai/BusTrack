package ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.example.bustrack_app.R
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import ui.admin.*
import ui_authentication.LoginActivity

class ParentTrackingActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parent_tracking)

        supportActionBar?.hide()
        drawerLayout = findViewById(R.id.drawerLayout)

        // Initialize Mapbox Map
        mapView = findViewById(R.id.mapView)
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupMapAnnotations()
        }

        setupClickListeners()
        setupDrawerListeners()
    }

    private fun setupMapAnnotations() {
        val busLocation = Point.fromLngLat(67.0011, 24.8607)
        
        // Set camera
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(busLocation)
                .zoom(14.0)
                .build()
        )

        // Add Marker
        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(busLocation)
            .withTextField("BUS-101")
        
        pointAnnotationManager?.create(pointAnnotationOptions)
    }

    private fun setupClickListeners() {
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        findViewById<TextView>(R.id.btnViewAll).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            Toast.makeText(this, "Opening full schedule...", Toast.LENGTH_SHORT).show()
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
            com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
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