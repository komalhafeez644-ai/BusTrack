package ui.driver

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.DriverdashboardBinding
import com.example.bustrack_app.viewmodels.DriverDashboardViewModel
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.models.DriverModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.locationcomponent.location
import com.mapbox.maps.plugin.viewport.viewport
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.LineString
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import utils.ViewUtils
import ui.admin.*
import ui.driver.NotificationSettingsActivity as DriverNotificationSettings
import ui_authentication.LoginActivity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var binding: DriverdashboardBinding
    private lateinit var drawerLayout: DrawerLayout
    private var mapView: MapView? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private val viewModel: DriverDashboardViewModel by viewModels()
    private var isDutyEnabled = false
    private var currentRouteGeometry: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DriverdashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        drawerLayout = binding.drawerLayout
        mapView = binding.mapView
        
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupInitialCamera()
            polylineAnnotationManager = mapView?.annotations?.createPolylineAnnotationManager()
            setupLocationPuck()
            fetchRouteData()
        }

        findViewById<View>(R.id.drawerDutyContainer)?.visibility = View.VISIBLE

        observeViewModel()
        observeDriverRepo()
        setupClickListeners()
        setupDrawerListeners()

        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.openDrawer(GravityCompat.END)
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProfileData()
    }

    private fun refreshProfileData() {
        val email = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase()
        if (email != null) {
            DriverRepository.driverList.value?.find { it.email.trim().lowercase() == email }?.let { driver ->
                updateHeaderUI(driver)
            }
        }
    }

    private fun observeDriverRepo() {
        DriverRepository.driverList.observe(this) { drivers ->
            val email = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase()
            if (email != null) {
                drivers.find { it.email.trim().lowercase() == email }?.let { driver ->
                    updateHeaderUI(driver)
                }
            }
        }
    }

    private fun updateHeaderUI(driver: DriverModel) {
        val greeting = getGreeting()
        binding.tvDriverName.text = "$greeting, ${driver.name}"
        
        // Top Bar Photo
        if (driver.profileImageUrl.isNotEmpty()) {
            Glide.with(this).load(driver.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(binding.ivProfile)
        } else {
            binding.ivProfile.setImageResource(R.drawable.ic_person)
        }

        // Drawer Header Info
        findViewById<TextView>(R.id.drawerName)?.text = driver.name
        findViewById<TextView>(R.id.drawerEmail)?.text = driver.email
        findViewById<ImageView>(R.id.drawerImgProfile)?.let { drawerImg ->
            if (driver.profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(driver.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(drawerImg)
            } else {
                drawerImg.setImageResource(R.drawable.ic_person)
            }
        }
    }

    private fun getGreeting(): String {
        val calendar = Calendar.getInstance()
        return when (calendar.get(Calendar.HOUR_OF_DAY)) {
            in 0..11 -> "Good Morning"
            in 12..15 -> "Good Afternoon"
            in 16..20 -> "Good Evening"
            else -> "Good Night"
        }
    }

    private fun setupInitialCamera() {
        val collegePoint = Point.fromLngLat(73.0535, 33.5985)
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(collegePoint)
                .zoom(14.0)
                .pitch(0.0)
                .build()
        )
    }

    private fun setupLocationPuck() {
        mapView?.location?.apply {
            enabled = true
            pulsingEnabled = true
        }
    }

    private fun observeViewModel() {
        viewModel.dashboardData.observe(this) { data ->
            binding.apply {
                tvBusNumberInfo.text = data.busNumber
                tvRouteNameInfo.text = data.currentRoute
                tvCurrentDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
                
                // Dashboard Card
                tvTotalStops.text = data.stopsCount
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnMenuDrawer.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }

        binding.ivProfile.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, DriverProfileActivity::class.java))
        }

        binding.btnStartNavigation.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (isDutyEnabled) {
                startNavigationAnimation()
            } else {
                Toast.makeText(this, "Please enable 'On Duty' from menu first", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnEndNavigation.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            setNavigationMode(false)
        }
        
        binding.btnMyLocation.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            setupInitialCamera()
        }

        binding.btnZoomIn.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            mapView?.camera?.easeTo(
                CameraOptions.Builder().zoom((mapView?.mapboxMap?.cameraState?.zoom ?: 14.0) + 1.0).build(),
                MapAnimationOptions.mapAnimationOptions { duration(500) }
            )
        }

        binding.btnZoomOut.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            mapView?.camera?.easeTo(
                CameraOptions.Builder().zoom((mapView?.mapboxMap?.cameraState?.zoom ?: 14.0) - 1.0).build(),
                MapAnimationOptions.mapAnimationOptions { duration(500) }
            )
        }

        binding.btnNotifications.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    private fun fetchRouteData() {
        val origin = Point.fromLngLat(73.0535, 33.5985)
        val destination = Point.fromLngLat(73.0782, 33.6067)

        val client = MapboxDirections.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .routeOptions(RouteOptions.builder()
                .coordinatesList(listOf(origin, destination))
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .build())
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                val route = response.body()?.routes()?.firstOrNull()
                route?.let {
                    currentRouteGeometry = it.geometry()
                    drawRouteOnMap(it.geometry()!!)
                    
                    val distanceKm = it.distance() / 1000.0
                    val durationMin = (it.duration() / 60.0).toInt()
                    
                    binding.tvEstDistance.text = String.format("%.1f km", distanceKm)
                    binding.tvEstDuration.text = String.format("%d min", durationMin)
                }
            }
            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("NavDebug", "Route fetch failed: ${t.message}")
            }
        })
    }

    private fun drawRouteOnMap(geometry: String) {
        val lineString = LineString.fromPolyline(geometry, 6)
        val polylineOptions = PolylineAnnotationOptions()
            .withPoints(lineString.coordinates())
            .withLineColor("#2563EB")
            .withLineWidth(5.0)
        
        polylineAnnotationManager?.deleteAll()
        polylineAnnotationManager?.create(polylineOptions)
    }

    private fun startNavigationAnimation() {
        val zoom = 18.0
        val tilt = 65.0
        
        mapView?.camera?.easeTo(
            CameraOptions.Builder()
                .zoom(zoom)
                .pitch(tilt)
                .build(),
            MapAnimationOptions.mapAnimationOptions {
                duration(2500)
            }
        )

        mapView?.postDelayed({
            val viewportPlugin = mapView?.viewport
            viewportPlugin?.transitionTo(
                viewportPlugin.makeFollowPuckViewportState(
                    FollowPuckViewportStateOptions.Builder()
                        .zoom(zoom)
                        .pitch(tilt) 
                        .build()
                )
            )
        }, 2000)

        setNavigationMode(true)
    }

    private fun setNavigationMode(isNavigating: Boolean) {
        binding.apply {
            if (isNavigating) {
                cardRouteDetails.visibility = View.GONE
                btnStartNavigation.visibility = View.GONE
                
                instructionCard.visibility = View.VISIBLE
                bottomSummaryCard.visibility = View.VISIBLE
                
                infoBar.setBackgroundColor(Color.parseColor("#E60D1B3E")) 
                cardMapControls.animate().translationY(-200f).setDuration(500).start()
            } else {
                mapView?.viewport?.idle()
                
                mapView?.camera?.easeTo(
                    CameraOptions.Builder()
                        .zoom(14.0)
                        .pitch(0.0)
                        .build(),
                    MapAnimationOptions.mapAnimationOptions {
                        duration(1000)
                    }
                )

                cardRouteDetails.visibility = View.VISIBLE
                btnStartNavigation.visibility = View.VISIBLE
                
                instructionCard.visibility = View.GONE
                bottomSummaryCard.visibility = View.GONE
                
                infoBar.setBackgroundColor(Color.TRANSPARENT)
                cardMapControls.animate().translationY(0f).setDuration(500).start()
            }
        }
    }

    private fun showLiveTrackingDialog(switch: SwitchMaterial) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_driver_live_tracking)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnEnable = dialog.findViewById<MaterialButton>(R.id.btnEnableTracking)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelTracking)

        btnEnable.setOnClickListener {
            isDutyEnabled = true
            updateDutyUI(true)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            switch.isChecked = false
            isDutyEnabled = false
            updateDutyUI(false)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateDutyUI(isOnDuty: Boolean) {
        val drawerDutyLabel = findViewById<TextView>(R.id.tvDrawerDutyLabel)
        binding.apply {
            if (isOnDuty) {
                drawerDutyLabel?.text = "DUTY STATUS: ON"
                btnStartNavigation.isEnabled = true
                btnStartNavigation.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
            } else {
                drawerDutyLabel?.text = "DUTY STATUS: OFF"
                btnStartNavigation.isEnabled = false
                btnStartNavigation.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
                setNavigationMode(false)
            }
        }
    }

    private fun setupDrawerListeners() {
        val dutySwitch = findViewById<SwitchMaterial>(R.id.switchDuty)
        dutySwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showLiveTrackingDialog(dutySwitch)
            } else {
                isDutyEnabled = false
                updateDutyUI(false)
            }
        }

        val headerAction = View.OnClickListener {
            startActivity(Intent(this, DriverProfileActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }
        findViewById<View>(R.id.drawerImgProfile)?.setOnClickListener(headerAction)
        findViewById<View>(R.id.drawerName)?.setOnClickListener(headerAction)
        findViewById<View>(R.id.drawerEmail)?.setOnClickListener(headerAction)
        
        findViewById<View>(R.id.drawerSettings)?.setOnClickListener {
            val intent = Intent(this, DriverNotificationSettings::class.java)
            intent.putExtra("FROM_USER", "driver")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerPreferences)?.setOnClickListener {
            val intent = Intent(this, PreferencesActivity::class.java)
            intent.putExtra("FROM_USER", "driver")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener {
            val intent = Intent(this, PrivacyPolicyActivityActivity::class.java)
            intent.putExtra("FROM_USER", "driver")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerTerms)?.setOnClickListener {
            val intent = Intent(this, TermsConditionsActivity::class.java)
            intent.putExtra("FROM_USER", "driver")
            startActivity(intent)
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerFaq)?.setOnClickListener {
            startActivity(Intent(this, FaqActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_logout_confirmation)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(true)

        val btnConfirm = dialog.findViewById<View>(R.id.btnConfirmLogout)
        val btnCancel = dialog.findViewById<View>(R.id.btnCancelLogout)

        btnConfirm.setOnClickListener {
            dialog.dismiss()
            FirebaseAuth.getInstance().signOut()
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