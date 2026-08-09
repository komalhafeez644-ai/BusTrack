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
import com.mapbox.maps.plugin.viewport.data.FollowPuckViewportStateBearing
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
import com.mapbox.android.gestures.MoveGestureDetector
import com.mapbox.maps.plugin.gestures.OnMoveListener
import com.mapbox.maps.plugin.gestures.gestures
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
import com.example.bustrack_app.models.AlertOption
import com.example.bustrack_app.adapter.DriverAlertsAdapter
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
    private var isNavigating = false
    private var currentRouteGeometry: String? = null
    private var isVoiceEnabled = true
    private lateinit var stopsAdapter: com.example.bustrack_app.adapter.NavigationStopsAdapter
    private lateinit var bottomSheetBehavior: com.google.android.material.bottomsheet.BottomSheetBehavior<View>

    private var lastLegIndex = -1
    private val stopArrivalTimes = mutableMapOf<Int, String>()
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private var activeStopStatus = "NEXT" // NEXT, ARRIVED, PASSED
    private var isCurrentlyAtStop = false
    private val ARRIVAL_RADIUS = 50.0 // 50 meters
    private val DEPARTURE_RADIUS = 70.0 // 70 meters to prevent flickering

    private var isNorthUp = false

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var assignedRoute: RouteModel? = null
    private var driverAnnotation: PointAnnotation? = null
    private var locationCallback: LocationCallback? = null
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    // Mapbox Navigation
    private var mapboxNavigation: MapboxNavigation? = null
    private var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null
    private val attendancePromptedStops = mutableSetOf<Int>()

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
            // Removed duplicate checkLocationSettings() to fix double "Yes" issue
            setupMapGestures()
            setupStopsRecyclerView()
        }

        findViewById<View>(R.id.drawerDutyContainer)?.visibility = View.VISIBLE

        binding.btnSound.setImageResource(if (isVoiceEnabled) R.drawable.volume_up else R.drawable.mute)
        binding.btnSound.imageTintList = ColorStateList.valueOf(Color.WHITE)

        // Start Location Flow First
        handleLocationFlow()

        observeViewModel()
        observeDriverRepo()
        setupClickListeners()
        setupDrawerListeners()
        initNavigation()

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

        // Try to get the instance immediately
        mapboxNavigation = MapboxNavigationApp.current()

        // If it's still null, it might take a moment to attach
        if (mapboxNavigation == null) {
            // We can retry after a short delay or just wait for the user click to handle it
        }

        mapboxNavigation?.registerRoutesObserver(routesObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)
        mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)

        // Initialize Voice Guidance
        if (voiceInstructionsPlayer == null) {
            voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(
                this,
                java.util.Locale.getDefault().language
            )
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        if (isVoiceEnabled) {
            voiceInstructionsPlayer?.play(
                SpeechAnnouncement.Builder(voiceInstructions.announcement() ?: "")
                    .ssmlAnnouncement(voiceInstructions.ssmlAnnouncement())
                    .build()
            ) { }
        }
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

                // Update current location text in bottom sheet
                currentLocation?.let { loc ->
                    binding.bottomSummaryCard.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = 
                        String.format(Locale.getDefault(), "Current: Lat %.4f, Lng %.4f", loc.latitude, loc.longitude)
                }

                val currentLegIndex = routeProgress.currentLegProgress?.legIndex ?: 0
                val stops = assignedRoute?.stopsList ?: emptyList()

                // Logic for UPCOMING -> NEXT -> ARRIVED -> PASSED
                if (currentLegIndex < stops.size) {
                    val currentStop = stops[currentLegIndex]
                    val distanceToStop = routeProgress.currentLegProgress?.distanceRemaining?.toDouble() ?: Double.MAX_VALUE

                    when {
                        !isCurrentlyAtStop && distanceToStop <= ARRIVAL_RADIUS -> {
                            // Bus just arrived at stop
                            isCurrentlyAtStop = true
                            activeStopStatus = "ARRIVED"
                            stopArrivalTimes[currentLegIndex] = timeFormat.format(Calendar.getInstance().time)
                            
                            // Trigger Attendance Sheet if Morning
                            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                            if (currentHour < 10 && !attendancePromptedStops.contains(currentLegIndex)) {
                                attendancePromptedStops.add(currentLegIndex)
                                val bottomSheet = AttendanceBottomSheet.newInstance(currentStop.stopName, assignedRoute?.routeName ?: "")
                                bottomSheet.show(supportFragmentManager, "AttendanceSheet")
                            }
                        }
                        isCurrentlyAtStop && distanceToStop > DEPARTURE_RADIUS -> {
                            // Bus just departed from stop
                            isCurrentlyAtStop = false
                            activeStopStatus = "PASSED"
                            // Mapbox will increment legIndex automatically soon
                        }
                        !isCurrentlyAtStop -> {
                            activeStopStatus = "NEXT"
                        }
                    }
                }

                // Update ETAs/Arrival times in the stops list
                val legs = routeProgress.route.legs()
                var accumulatedSeconds = (routeProgress.currentLegProgress?.durationRemaining ?: 0.0).toInt()

                stops.forEachIndexed { index, stop ->
                    when {
                        index < currentLegIndex -> {
                            val arrival = stopArrivalTimes[index]
                            stop.time = if (arrival != null) "Arrived: $arrival" else "Arrived"
                        }
                        index == currentLegIndex -> {
                            if (activeStopStatus == "ARRIVED") {
                                stop.time = "At Stop: ${stopArrivalTimes[index]}"
                            } else {
                                val etaTime = Calendar.getInstance().apply { add(Calendar.SECOND, accumulatedSeconds) }.time
                                stop.time = "ETA: ${timeFormat.format(etaTime)}"
                            }
                        }
                        else -> {
                            // Add duration of the leg leading to this stop
                            if (legs != null && index < legs.size) {
                                accumulatedSeconds += (legs[index].duration() ?: 0.0).toInt()
                            }
                            val etaTime = Calendar.getInstance().apply { add(Calendar.SECOND, accumulatedSeconds) }.time
                            stop.time = "ETA: ${timeFormat.format(etaTime)}"
                        }
                    }
                }

                // Update stop index in adapter based on leg progress and status
                stopsAdapter.updateStops(stops, currentLegIndex, activeStopStatus)

                val bannerInstructions = routeProgress.bannerInstructions
                val primary = bannerInstructions?.primary()
                val sub = bannerInstructions?.sub()
                binding.tvNextInstruction.text = primary?.text() ?: "Continue straight"
                binding.tvNextStreet.text = sub?.text() ?: "Current Route"
            }
        }
    }

    private fun handleLocationFlow() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val needsPermission = permissions.any {
            ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (needsPermission) {
            Toast.makeText(this, "Requesting Location Permissions...", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(this, permissions, 1001)
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
                    startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                }
            } else {
                startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
        }
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

                    // Sync location to Firestore if On Duty
                    if (isDutyEnabled) {
                        viewModel.currentDriver.value?.id?.let { driverId ->
                            com.example.bustrack_app.data.FirebaseRepository.updateDriverLocation(
                                driverId,
                                location.latitude,
                                location.longitude
                            )
                        }
                    }
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
                .withTextField(assignedRoute?.busNo ?: "My Bus")
                .withTextOffset(listOf(0.0, -2.0)) // Show above icon
                .withTextColor(Color.WHITE)
                .withTextHaloColor(Color.parseColor("#0D1B3E"))
                .withTextHaloWidth(1.5)
            
            driverAnnotation = pointAnnotationManager?.create(pointAnnotationOptions)
        }
    }

    private fun bitmapFromDrawableRes(context: Context, resourceId: Int): Bitmap? {
        if (bitmapCache.containsKey(resourceId)) {
            return bitmapCache[resourceId]
        }
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            bitmapCache[resourceId] = bitmap
            return bitmap
        }
        if (drawable != null) {
            val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 64
            val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 64
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmapCache[resourceId] = bitmap
            return bitmap
        }
        return null
    }

    private fun checkArrivalAtStart() {
        val route = assignedRoute ?: return
        val currentLoc = currentLocation ?: return

        // Throttle checking to save CPU
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
            
            // Only update UI if status actually changed to prevent flickering/lag
            if (wasNearStart != isNearStart) {
                runOnUiThread { updateMapDisplay() }
            }
            
            updateNavigationButtonState()
        }
    }

    private fun updateMapDisplay() {
        val route = assignedRoute ?: return
        
        // If we are currently navigating, don't clear the markers
        if (isNavigating) {
            return
        }
        
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

                // Update Local Duty State
                if (data.isOnDuty != isDutyEnabled) {
                    isDutyEnabled = data.isOnDuty
                    findViewById<SwitchMaterial>(R.id.switchDuty)?.isChecked = isDutyEnabled
                    updateDutyUI(isDutyEnabled)
                }
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
            addMarker(Point.fromLngLat(stop.longitude, stop.latitude), R.drawable.ic_marker_dest, stop.stopName)
        }

        // Start Marker (Green)
        val startName = assignedRoute?.startPoint ?: "Start"
        addMarker(points.first(), R.drawable.green_dot, startName)

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

    private fun addMarker(point: Point, iconRes: Int, title: String? = null) {
        // Reuse existing markers to prevent UI lag
        val markerKey = "${point.latitude()},${point.longitude()}"
        
        val bitmap = bitmapFromDrawableRes(this, iconRes)
        if (bitmap != null) {
            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(1.0)
            
            title?.let {
                options.withTextField(it)
                options.withTextSize(12.0)
                options.withTextOffset(listOf(0.0, 1.5))
                options.withTextColor(Color.BLACK)
                options.withTextHaloColor(Color.WHITE)
                options.withTextHaloWidth(1.0)
            }
            
            pointAnnotationManager?.create(options)
        }
    }

    private fun setupClickListeners() {
        binding.btnMenuDrawer.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.layoutProfileArea).setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val route = assignedRoute
            if (route != null && route.stopsList.isNotEmpty()) {
                val bottomSheet = AttendanceBottomSheet.newInstance(route.stopsList[0].stopName, route.routeName)
                bottomSheet.show(supportFragmentManager, "AttendanceSheet")
            } else {
                Toast.makeText(this, "No route or stops assigned yet", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnNorth.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            toggleNorthUpMode()
        }

        binding.btnSearchMap.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            Toast.makeText(this, "Search feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnSound.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            isVoiceEnabled = !isVoiceEnabled
            binding.btnSound.setImageResource(if (isVoiceEnabled) R.drawable.volume_up else R.drawable.mute)
            binding.btnSound.imageTintList = ColorStateList.valueOf(Color.WHITE)
            Toast.makeText(this, if (isVoiceEnabled) "Voice instructions ON" else "Voice instructions OFF", Toast.LENGTH_SHORT).show()
        }

        binding.btnRecenter.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startFollowingPuck()
        }

        binding.btnAlert.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            showAlertsBottomSheet()
        }

        binding.btnStartNavigation.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val route = assignedRoute
            
            if (!isDutyEnabled) {
                // Flow: Duty is OFF -> Open Drawer + Show Message
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
                Toast.makeText(this, "Please enable On Duty Mode before starting navigation.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            if (route == null) {
                Toast.makeText(this, "No route assigned to you yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isNearStart) {
                // Flow: Outside Geo-Fence -> Show Distance & Dialog
                val currentPoint = currentLocation?.let { com.mapbox.geojson.Point.fromLngLat(it.longitude, it.latitude) }
                val startPoint = if (route.pathPoints.isNotEmpty()) {
                    com.mapbox.geojson.Point.fromLngLat(route.pathPoints[0].longitude, route.pathPoints[0].latitude)
                } else if (route.stopsList.isNotEmpty()) {
                    com.mapbox.geojson.Point.fromLngLat(route.stopsList[0].longitude, route.stopsList[0].latitude)
                } else null

                if (currentPoint != null && startPoint != null) {
                    val results = FloatArray(1)
                    android.location.Location.distanceBetween(currentPoint.latitude(), currentPoint.longitude(), startPoint.latitude(), startPoint.longitude(), results)
                    val distanceKm = results[0] / 1000.0
                    Toast.makeText(this, String.format(java.util.Locale.getDefault(), "You are %.2f km away from the starting point.", distanceKm), Toast.LENGTH_LONG).show()
                }

                val startName = if (route.pathPoints.isNotEmpty()) route.startPoint.ifEmpty { "Start Point" } 
                               else if (route.stopsList.isNotEmpty()) route.stopsList[0].stopName 
                               else "Start Point"
                showReachStartDialog(startName)
            } else {
                // Flow: Everything OK -> Start Navigation
                updateBottomSheetInfo()
                startNavigationAnimation()
            }
        }

        binding.bottomSummaryCard.findViewById<View>(R.id.btnCloseNav)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            setNavigationMode(false)
        }

        binding.btnNotifications.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    private fun setupStopsRecyclerView() {
        bottomSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(binding.bottomSummaryCard)
        bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN

        stopsAdapter = com.example.bustrack_app.adapter.NavigationStopsAdapter(emptyList())
        val rvStops = binding.bottomSummaryCard.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvUpcomingStops)
        rvStops?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvStops?.adapter = stopsAdapter
    }

    private fun setupMapGestures() {
        mapView?.gestures?.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                if (binding.bottomSummaryCard.visibility == View.VISIBLE) {
                    binding.btnRecenter.visibility = View.VISIBLE
                }
            }
            override fun onMove(detector: MoveGestureDetector): Boolean = false
            override fun onMoveEnd(detector: MoveGestureDetector) {}
        })
    }

    private fun updateBottomSheetInfo() {
        val route = assignedRoute ?: return
        val email = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase()
        val driver = DriverRepository.driverList.value?.find { it.email.trim().lowercase() == email }

        binding.bottomSummaryCard.findViewById<TextView>(R.id.tvBusIdSheet)?.text = route.busNo.ifEmpty { "BUS-TRACK" }
        binding.bottomSummaryCard.findViewById<TextView>(R.id.tvRouteSheet)?.text = route.routeName
        binding.bottomSummaryCard.findViewById<TextView>(R.id.tvDriverNameSheet)?.text = driver?.name ?: "Driver"
        
        stopsAdapter.updateStops(route.stopsList, 0)
    }

    private fun startFollowingPuck() {
        binding.btnRecenter.visibility = View.GONE
        if (isNorthUp) {
            followPuckNorthUp()
        } else {
            followPuckHeadingUp()
        }
    }

    private fun followPuckHeadingUp() {
        mapView?.viewport?.transitionTo(
            mapView?.viewport?.makeFollowPuckViewportState(
                FollowPuckViewportStateOptions.Builder()
                    .zoom(18.0)
                    .pitch(65.0)
                    .build()
            )!!
        )
    }

    private fun followPuckNorthUp() {
        mapView?.viewport?.transitionTo(
            mapView?.viewport?.makeFollowPuckViewportState(
                FollowPuckViewportStateOptions.Builder()
                    .zoom(16.0)
                    .pitch(0.0)
                    .bearing(FollowPuckViewportStateBearing.Constant(0.0))
                    .build()
            )!!
        )
    }

    private fun toggleNorthUpMode() {
        isNorthUp = !isNorthUp
        if (isNorthUp) {
            // Switch to North-Up
            binding.btnNorth.setImageResource(R.drawable.ic_compass) // Assuming this icon indicates active compass/north
            binding.btnNorth.imageTintList = ColorStateList.valueOf(Color.RED) 
            followPuckNorthUp()
        } else {
            // Switch to Heading-Up
            binding.btnNorth.setImageResource(R.drawable.ic_compass)
            binding.btnNorth.imageTintList = ColorStateList.valueOf(Color.WHITE)
            followPuckHeadingUp()
        }
    }

    private fun showAlertsBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_driver_alerts, null)
        dialog.setContentView(view)

        val alerts = listOf(
            AlertOption("Road Block", "Road is closed, need alternative route", R.drawable.notification_active, "🚧"),
            AlertOption("Heavy Traffic", "Stuck in traffic, bus might be late", R.drawable.notification_active, "🚦"),
            AlertOption("Accident", "Accident on route or bus involved", R.drawable.notification_active, "🚗"),
            AlertOption("Bus Breakdown", "Engine or tyre issue", R.drawable.notification_active, "🚌"),
            AlertOption("Fuel Issue", "Low fuel or tank empty", R.drawable.notification_active, "⛽"),
            AlertOption("Bad Weather", "Heavy rain, fog or storm", R.drawable.notification_active, "🌧️"),
            AlertOption("Student Emergency", "Student needs medical help", R.drawable.notification_active, "👨‍🎓"),
            AlertOption("Police Check", "Security check causing delay", R.drawable.notification_active, "👮"),
            AlertOption("Wrong Route", "Assigned route is closed", R.drawable.notification_active, "📍"),
            AlertOption("Other", "Custom report or other issue", R.drawable.notification_active, "📝")
        )

        val rvAlerts = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvAlerts)
        rvAlerts.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvAlerts.adapter = DriverAlertsAdapter(alerts) { option ->
            if (option.title == "Other") {
                showOtherAlertContent()
            } else {
                Toast.makeText(this, "Reported: ${option.title}", Toast.LENGTH_SHORT).show()
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showOtherAlertContent() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_driver_live_tracking) // Reuse styled dialog layout
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDesc = dialog.findViewById<TextView>(R.id.tvDialogDescription)
        val btnSend = dialog.findViewById<MaterialButton>(R.id.btnEnableTracking)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelTracking)

        tvTitle?.text = "Other Issue"
        tvDesc?.text = "Please describe the issue you are facing."
        btnSend?.text = "Send Report"

        // For a real app, I'd add an EditText here. 
        // For now, I'll just show a toast or prompt the user.
        btnSend.setOnClickListener {
            Toast.makeText(this, "Custom report sent to Admin", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
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
        if (mapboxNavigation == null) {
            mapboxNavigation = MapboxNavigationApp.current()
        }
        
        val nav = mapboxNavigation
        if (nav == null) {
            Toast.makeText(this, "Initializing Map SDK... please wait 1 second.", Toast.LENGTH_SHORT).show()
            initNavigation()
            return
        }

        val route = assignedRoute
        if (route == null) {
            Toast.makeText(this, "No route data available", Toast.LENGTH_SHORT).show()
            return
        }

        val navPoints = mutableListOf<Point>()

        // Add current location as the starting point of the navigation
        currentLocation?.let {
            navPoints.add(Point.fromLngLat(it.longitude, it.latitude))
        }

        // Use stopsList for navigation coordinates
        if (route.stopsList.isNotEmpty()) {
            navPoints.addAll(route.stopsList
                .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                .map { Point.fromLngLat(it.longitude, it.latitude) })
        } else if (route.pathPoints.isNotEmpty()) {
            // Fallback to start and end if only path points exist
            navPoints.add(Point.fromLngLat(route.pathPoints.first().longitude, route.pathPoints.first().latitude))
            navPoints.add(Point.fromLngLat(route.pathPoints.last().longitude, route.pathPoints.last().latitude))
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
        this.isNavigating = isNavigating
        binding.apply {
            if (isNavigating) {
                cardRouteDetails.visibility = View.GONE
                btnStartNavigation.visibility = View.GONE

                instructionCard.visibility = View.VISIBLE
                bottomSummaryCard.visibility = View.VISIBLE
                bottomSheetBehavior.isHideable = false
                bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED

                // Reset navigation tracking
                lastLegIndex = -1
                stopArrivalTimes.clear()

                infoBar.setBackgroundColor(Color.parseColor("#E60D1B3E"))
                layoutMapControls.visibility = View.VISIBLE
                layoutMapControls.animate().translationY(-110f).setDuration(500).start()
                btnRecenter.animate().translationY(-260f).setDuration(500).start()
            } else {
                mapboxNavigation?.stopTripSession()
                mapboxNavigation?.setNavigationRoutes(emptyList())

                mapView?.viewport?.idle()
                binding.layoutMapControls.visibility = View.GONE
                binding.layoutMapControls.translationY = 0f
                binding.btnRecenter.visibility = View.GONE
                binding.btnRecenter.translationY = 0f

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
                bottomSheetBehavior.isHideable = true
                bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN

                infoBar.setBackgroundColor(Color.TRANSPARENT)
                layoutMapControls.animate().translationY(0f).setDuration(500).start()
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
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                isDutyEnabled = true
                if (switch?.isChecked == false) {
                    switch.isChecked = true
                }
                updateDutyUI(true)
                
                // Sync status to Firestore
                viewModel.currentDriver.value?.id?.let { driverId ->
                    com.example.bustrack_app.data.FirebaseRepository.updateDriverStatus(driverId, "Active")
                }
                
                // Close drawer automatically after enabling duty
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END)
                
                dialog.dismiss()
            }, 200)
        }

        btnCancel.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                if (switch?.isChecked == true) {
                    switch.isChecked = false
                }
                isDutyEnabled = false
                updateDutyUI(false)
                
                // Sync status to Firestore
                viewModel.currentDriver.value?.id?.let { driverId ->
                    com.example.bustrack_app.data.FirebaseRepository.updateDriverStatus(driverId, "Inactive")
                }
                
                dialog.dismiss()
            }, 200)
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
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                dialog.dismiss()
            }, 200)
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
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                val intent = Intent(this, DriverNotificationSettings::class.java)
                intent.putExtra("FROM_USER", "driver")
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }, 150)
        }

        findViewById<View>(R.id.drawerPreferences)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                val intent = Intent(this, PreferencesActivity::class.java)
                intent.putExtra("FROM_USER", "driver")
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }, 150)
        }

        findViewById<View>(R.id.drawerPrivacy)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                val intent = Intent(this, PrivacyPolicyActivityActivity::class.java)
                intent.putExtra("FROM_USER", "driver")
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }, 150)
        }

        findViewById<View>(R.id.drawerTerms)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                val intent = Intent(this, TermsConditionsActivity::class.java)
                intent.putExtra("FROM_USER", "driver")
                startActivity(intent)
                drawerLayout.closeDrawer(GravityCompat.END)
            }, 150)
        }

        findViewById<View>(R.id.drawerFaq)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                startActivity(Intent(this, DriverFaqActivity::class.java))
                drawerLayout.closeDrawer(GravityCompat.END)
            }, 150)
        }

        findViewById<View>(R.id.drawerChangePassword)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                startActivity(Intent(this, ChangePasswordActivity::class.java))
                drawerLayout.closeDrawer(GravityCompat.END)
            }, 150)
        }

        findViewById<View>(R.id.drawerEveningAttendance)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                val intent = Intent(this, EveningAttendanceActivity::class.java)
                intent.putExtra("ROUTE_NAME", assignedRoute?.routeName ?: "")
                startActivity(intent)
                overridePendingTransition(0, 0)
                drawerLayout.closeDrawer(GravityCompat.END)
            }, 200)
        }

        findViewById<View>(R.id.drawerLogout)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                showLogoutDialog()
            }, 200)
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
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                dialog.dismiss()
                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }, 200)
        }

        btnCancel.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                dialog.dismiss()
            }, 200)
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
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        mapboxNavigation?.unregisterRoutesObserver(routesObserver)
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
        voiceInstructionsPlayer?.shutdown()
        MapboxNavigationApp.detach(this)
        
        bitmapCache.clear()
        mapView?.onDestroy()
        super.onDestroy()
    }

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}