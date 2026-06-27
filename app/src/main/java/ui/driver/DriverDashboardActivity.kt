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
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
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
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.provider.Settings
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.IntentSenderRequest
import com.mapbox.maps.CameraBoundsOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.EdgeInsets
import com.google.android.material.bottomsheet.BottomSheetDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.data.RouteRepository
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import kotlin.collections.firstOrNull

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var binding: DriverdashboardBinding
    private lateinit var drawerLayout: DrawerLayout
    private var mapView: MapView? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private val viewModel: DriverDashboardViewModel by viewModels()
    private var isDutyEnabled = false
    private var isNearStart = false
    private var currentRouteGeometry: String? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var assignedRoute: RouteModel? = null
    private var driverAnnotation: PointAnnotation? = null
    private var locationCallback: LocationCallback? = null

    // Mapbox Navigation
    private var mapboxNavigation: MapboxNavigation? = null
    private var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startLocationUpdates()
        } else {
            Toast.makeText(this, "GPS must be enabled to use this app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DriverdashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set Access Token Globally for Mapbox v3
        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        drawerLayout = binding.drawerLayout
        mapView = binding.mapView

        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            mapView?.mapboxMap?.setBounds(
                CameraBoundsOptions.Builder()
                    .minZoom(3.0)
                    .maxZoom(20.0)
                    .build()
            )
            setupInitialCamera()
            polylineAnnotationManager = mapView?.annotations?.createPolylineAnnotationManager()
            pointAnnotationManager = mapView?.annotations?.createPointAnnotationManager()
            setupLocationPuck()
            checkLocationSettings()
        }

        findViewById<View>(R.id.drawerDutyContainer)?.visibility = View.VISIBLE

        observeViewModel()
        observeDriverRepo()
        setupClickListeners()
        setupDrawerListeners()
        initNavigation()
        handleLocationFlow()

        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.post {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettings()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.post {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }
    }

    private fun initNavigation() {
        if (!MapboxNavigationApp.isSetup()) {
            MapboxNavigationApp.setup(
                NavigationOptions.Builder(applicationContext)
                    .build()
            )
        }
        MapboxNavigationApp.attach(this)

        mapboxNavigation = MapboxNavigationApp.current()

        // Initialize Voice Guidance
        voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
            this,
            Locale.getDefault().language
        )

        mapboxNavigation?.registerRoutesObserver(routesObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)
        mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        voiceInstructionsPlayer?.play(
            SpeechAnnouncement.Builder(voiceInstructions.announcement() ?: "")
                .ssmlAnnouncement(voiceInstructions.ssmlAnnouncement())
                .build()
        ) { }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(result: RoutesUpdatedResult) {
            val routes = result.navigationRoutes
            if (routes.isNotEmpty()) {
                val route = routes[0]
                // Update map with route from Navigation SDK
                val points = route.directionsRoute.geometry()?.let {
                    LineString.fromPolyline(it, 6).coordinates()
                } ?: emptyList()
                drawPointsOnMap(points)
            }
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: com.mapbox.common.location.Location) {}
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val enhancedLocation = locationMatcherResult.enhancedLocation
            // Convert Mapbox Location to Android Location for UI update
            val androidLocation = android.location.Location("mapbox").apply {
                latitude = enhancedLocation.latitude
                longitude = enhancedLocation.longitude
            }
            runOnUiThread {
                updateDriverMarker(androidLocation)
                currentLocation = androidLocation
                checkArrivalAtStart()
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: com.mapbox.navigation.base.trip.model.RouteProgress) {
            runOnUiThread {
                val distanceRemaining = routeProgress.distanceRemaining / 1000.0
                val durationRemaining = routeProgress.durationRemaining / 60.0

                // Update Dashboard Card (Hidden during navigation, but updated)
                binding.tvEstDistance.text = String.format(Locale.getDefault(), "%.1f km", distanceRemaining)
                binding.tvEstDuration.text = String.format(Locale.getDefault(), "%d min", durationRemaining.toInt())

                // Update Bottom Summary Card (Visible during navigation)
                binding.tvNavDistance.text = String.format(Locale.getDefault(), "%.1f km", distanceRemaining)
                
                // Calculate ETA
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.SECOND, routeProgress.durationRemaining.toInt())
                val etaTime = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(calendar.time)
                binding.tvNavEta.text = etaTime

                // Update Next Stop Distance (using leg distance remaining as a proxy or just showing leg progress)
                val legProgress = routeProgress.currentLegProgress
                if (legProgress != null) {
                    binding.tvNavNextStopDist.text = String.format(Locale.getDefault(), "%.1f km", legProgress.distanceRemaining / 1000.0)
                }

                val bannerInstructions = routeProgress.bannerInstructions
                val primary = bannerInstructions?.primary()
                val sub = bannerInstructions?.sub()
                binding.tvNextInstruction.text = primary?.text() ?: "Continue straight"
                binding.tvNextStreet.text = sub?.text() ?: "Current Route"
            }
        }
    }

    private fun handleLocationFlow() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
        } else {
            checkLocationSettings()
        }
    }

    private fun checkLocationSettings() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                    locationSettingsLauncher.launch(intentSenderRequest)
                } catch (sendEx: Exception) {
                    Log.e("DriverDashboard", "Error launching location settings resolution", sendEx)
                }
            }
        }
    }

    private fun showEnableGpsDialog() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_driver_live_tracking, null)
        bottomSheetDialog.setContentView(view)
        bottomSheetDialog.setCancelable(false)

        view.findViewById<TextView>(R.id.btnEnableTracking)?.text = "Enable GPS"

        view.findViewById<MaterialButton>(R.id.btnEnableTracking).setOnClickListener {
            startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            bottomSheetDialog.dismiss()
        }

        view.findViewById<MaterialButton>(R.id.btnCancelTracking).setOnClickListener {
            bottomSheetDialog.dismiss()
        }

        bottomSheetDialog.show()
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(3000)
            .setMaxUpdateDelayMillis(10000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    currentLocation = location
                    updateDriverMarker(location)
                    checkArrivalAtStart()
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, mainLooper)
    }

    private fun updateDriverMarker(location: Location) {
        val point = Point.fromLngLat(location.longitude, location.latitude)

        driverAnnotation?.let {
            try { pointAnnotationManager?.delete(it) } catch (e: Exception) {}
        }

        val bitmap = bitmapFromDrawableRes(this, R.drawable.ic_driver)
        if (bitmap != null) {
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(1.5)
            driverAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)
        }
    }

    private fun bitmapFromDrawableRes(context: Context, resourceId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable is BitmapDrawable) return drawable.bitmap
        if (drawable != null) {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 64
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 64
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            return bitmap
        }
        return null
    }

    private fun checkArrivalAtStart() {
        val route = assignedRoute ?: return
        val currentLoc = currentLocation ?: return

        val startPoint = if (route.pathPoints.isNotEmpty()) {
            Point.fromLngLat(route.pathPoints[0].longitude, route.pathPoints[0].latitude)
        } else if (route.stopsList.isNotEmpty()) {
            Point.fromLngLat(route.stopsList[0].longitude, route.stopsList[0].latitude)
        } else {
            null
        }

        startPoint?.let {
            val results = FloatArray(1)
            Location.distanceBetween(currentLoc.latitude, currentLoc.longitude, it.latitude(), it.longitude(), results)
            val distanceInMeters = results[0]

            val wasNearStart = isNearStart
            isNearStart = distanceInMeters < 300 // within 300m
            
            // Update map if status changed
            if (wasNearStart != isNearStart) {
                updateMapDisplay()
            }
            
            updateNavigationButtonState()
        }
    }

    private fun updateMapDisplay() {
        val route = assignedRoute ?: return
        
        if (isNearStart) {
            // Draw route on map
            val points = if (route.pathPoints.isNotEmpty()) {
                route.pathPoints
                    .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                    .map { Point.fromLngLat(it.longitude, it.latitude) }
            } else if (route.stopsList.isNotEmpty()) {
                route.stopsList
                    .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                    .map { Point.fromLngLat(it.longitude, it.latitude) }
            } else {
                emptyList()
            }

            if (points.isNotEmpty()) {
                drawPointsOnMap(points)
            }
        } else {
            // Clear route, show only current location
            polylineAnnotationManager?.deleteAll()
            pointAnnotationManager?.deleteAll()
            currentLocation?.let { 
                updateDriverMarker(it)
                // Center camera on driver
                mapView?.mapboxMap?.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(it.longitude, it.latitude))
                        .zoom(15.0)
                        .build()
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshProfileData()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
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
        binding.tvGreeting.text = "${greeting.uppercase()}, 👋"
        binding.tvDriverName.text = driver.name

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

    private fun centerCameraOnUser() {
        val targetPoint = currentLocation?.let { 
            Point.fromLngLat(it.longitude, it.latitude) 
        } ?: Point.fromLngLat(73.0535, 33.5985)

        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(targetPoint)
                .zoom(15.0)
                .pitch(0.0)
                .build()
        )
    }

    private fun setupInitialCamera() {
        centerCameraOnUser()
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

            // Fetch route details to update Start/Stop 1
            RouteRepository.routeList.value?.find { it.routeName == data.currentRoute }?.let { route ->
                assignedRoute = route
                binding.tvStartAddress.text = route.startPoint.ifEmpty { "Main Terminal" }
                if (route.stopsList.isNotEmpty()) {
                    binding.tvEndAddress.text = route.stopsList[0].stopName
                } else {
                    binding.tvEndAddress.text = route.endPoint
                }

                checkArrivalAtStart()
                updateMapDisplay()
            }
        }
    }

    private fun drawPointsOnMap(points: List<Point>) {
        if (points.isEmpty()) return

        polylineAnnotationManager?.deleteAll()
        val polylineOptions = PolylineAnnotationOptions()
            .withPoints(points)
            .withLineColor("#2563EB")
            .withLineWidth(6.0)
        polylineAnnotationManager?.create(polylineOptions)

        pointAnnotationManager?.deleteAll()
        driverAnnotation = null

        // Add Markers for all stops in the route
        assignedRoute?.stopsList?.forEach { stop ->
            addMarker(Point.fromLngLat(stop.longitude, stop.latitude), R.drawable.ic_marker_dest)
        }

        // Start Marker (Green)
        addMarker(points.first(), R.drawable.green_dot)

        currentLocation?.let { updateDriverMarker(it) }

        if (points.size == 1) {
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(points[0])
                    .zoom(15.0)
                    .build()
            )
        } else {
            // Fit camera to route
            val camera = mapView?.mapboxMap?.cameraForCoordinates(
                points,
                EdgeInsets(350.0, 100.0, 150.0, 100.0),
                null,
                null
            )
            camera?.let { mapView?.mapboxMap?.setCamera(it) }
        }
    }

    private fun addMarker(point: Point, iconRes: Int) {
        val bitmap = bitmapFromDrawableRes(this, iconRes)
        if (bitmap != null) {
            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(1.0)
            pointAnnotationManager?.create(options)
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
            val route = assignedRoute
            
            if (!isDutyEnabled) {
                // Flow 1: Duty is OFF
                val dutySwitch = findViewById<SwitchMaterial>(R.id.switchDuty)
                showLiveTrackingDialog(dutySwitch)
            } else if (route == null) {
                Toast.makeText(this, "No route assigned to you yet", Toast.LENGTH_SHORT).show()
            } else if (!isNearStart) {
                // Flow 2: Duty is ON but too far from start
                val startName = if (route.pathPoints.isNotEmpty()) route.startPoint.ifEmpty { "Start Point" } 
                               else if (route.stopsList.isNotEmpty()) route.stopsList[0].stopName 
                               else "Start Point"
                showReachStartDialog(startName)
            } else {
                // Flow 3: Everything OK
                startNavigationAnimation()
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

                    binding.tvEstDistance.text = String.format(Locale.getDefault(), "%.1f km", distanceKm)
                    binding.tvEstDuration.text = String.format(Locale.getDefault(), "%d min", durationMin)
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
        val nav = mapboxNavigation
        if (nav == null) {
            Toast.makeText(this, "Navigation SDK not initialized. Please wait...", Toast.LENGTH_SHORT).show()
            initNavigation()
            return
        }

        val route = assignedRoute
        if (route == null) {
            Toast.makeText(this, "No route data available", Toast.LENGTH_SHORT).show()
            return
        }

        // Use stopsList for navigation coordinates to avoid "Too many coordinates" (max 25)
        val navPoints = if (route.stopsList.isNotEmpty()) {
            route.stopsList
                .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                .map { Point.fromLngLat(it.longitude, it.latitude) }
        } else if (route.pathPoints.isNotEmpty()) {
            // Fallback to start and end if only path points exist
            listOf(
                Point.fromLngLat(route.pathPoints.first().longitude, route.pathPoints.first().latitude),
                Point.fromLngLat(route.pathPoints.last().longitude, route.pathPoints.last().latitude)
            )
        } else {
            emptyList()
        }

        if (navPoints.size < 2) {
            Toast.makeText(this, "Route has insufficient valid stops to navigate", Toast.LENGTH_LONG).show()
            return
        }

        Toast.makeText(this, "Requesting Route...", Toast.LENGTH_SHORT).show()

        nav.requestRoutes(
            RouteOptions.builder()
                .applyDefaultNavigationOptions()
                .coordinatesList(navPoints)
                .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .alternatives(false)
                .build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    nav.setNavigationRoutes(routes)

                    if (ActivityCompat.checkSelfPermission(this@DriverDashboardActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        nav.startTripSession()
                    }

                    val zoom = 18.0
                    val tilt = 65.0

                    mapView?.viewport?.transitionTo(
                        mapView?.viewport?.makeFollowPuckViewportState(
                            FollowPuckViewportStateOptions.Builder()
                                .zoom(zoom)
                                .pitch(tilt)
                                .build()
                        )!!
                    )

                    setNavigationMode(true)
                }
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    val errorDetail = reasons.firstOrNull()?.message ?: "Unknown error"
                    Log.e("NavDebug", "Navigation failed: $errorDetail")
                    Toast.makeText(this@DriverDashboardActivity, "Navigation Error: $errorDetail", Toast.LENGTH_LONG).show()
                }
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    Log.d("NavDebug", "Route request canceled")
                }
            }
        )
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
                mapboxNavigation?.stopTripSession()
                mapboxNavigation?.setNavigationRoutes(emptyList())

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

    private fun showLiveTrackingDialog(switch: SwitchMaterial?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_driver_live_tracking)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnEnable = dialog.findViewById<MaterialButton>(R.id.btnEnableTracking)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelTracking)

        btnEnable.setOnClickListener {
            isDutyEnabled = true
            if (switch?.isChecked == false) {
                switch.isChecked = true
            }
            updateDutyUI(true)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            if (switch?.isChecked == true) {
                switch.isChecked = false
            }
            isDutyEnabled = false
            updateDutyUI(false)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateDutyUI(isOnDuty: Boolean) {
        val drawerDutyLabel = findViewById<TextView>(R.id.tvDrawerDutyLabel)
        if (isOnDuty) {
            drawerDutyLabel?.text = "DUTY STATUS: ON"
        } else {
            drawerDutyLabel?.text = "DUTY STATUS: OFF"
            setNavigationMode(false)
        }
        updateNavigationButtonState()
    }

    private fun updateNavigationButtonState() {
        // As per requirement: Button always enabled and green
        binding.btnStartNavigation.isEnabled = true
        binding.btnStartNavigation.alpha = 1.0f
        binding.btnStartNavigation.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#22C55E"))
    }

    private fun showReachStartDialog(locationName: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_driver_live_tracking) // Reusing the same styled layout
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        
        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDesc = dialog.findViewById<TextView>(R.id.tvDialogDescription)
        val btnOk = dialog.findViewById<MaterialButton>(R.id.btnEnableTracking)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelTracking)

        tvTitle?.text = "Reach Start Location"
        tvDesc?.text = "You are not at the starting point yet. Please reach '$locationName' to begin navigation."
        btnOk?.text = "Got it"
        btnCancel?.visibility = View.GONE

        btnOk.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun setupDrawerListeners() {
        val dutySwitch = findViewById<SwitchMaterial>(R.id.switchDuty)
        dutySwitch?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isDutyEnabled) {
                showLiveTrackingDialog(dutySwitch)
            } else if (!isChecked && isDutyEnabled) {
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
            startActivity(Intent(this, DriverFaqActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener {
            startActivity(Intent(this, ChangePasswordActivity::class.java))
            drawerLayout.closeDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.drawerEveningAttendance)?.setOnClickListener {
            startActivity(Intent(this, EveningAttendanceActivity::class.java))
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
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapboxNavigation?.unregisterRoutesObserver(routesObserver)
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        voiceInstructionsPlayer?.shutdown()
        MapboxNavigationApp.detach(this)
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}