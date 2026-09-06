package ui.driver

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
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
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.animation.camera
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.LocationPuck3D
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
import android.location.Geocoder
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
import com.example.bustrack_app.data.FirebaseRepository
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.trip.session.LocationMatcherResult
import com.mapbox.navigation.core.trip.session.LocationObserver
import com.mapbox.navigation.ui.maps.location.NavigationLocationProvider
import com.mapbox.navigation.core.trip.session.RouteProgressObserver
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.directions.session.RoutesUpdatedResult
import com.mapbox.common.MapboxOptions
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.voice.api.MapboxVoiceInstructionsPlayer
import com.mapbox.navigation.voice.api.MapboxSpeechApi
import com.mapbox.navigation.voice.model.SpeechAnnouncement
import com.mapbox.navigation.voice.model.SpeechError
import com.mapbox.navigation.voice.options.MapboxSpeechApiOptions
import com.mapbox.navigation.voice.options.VoiceInstructionsPlayerOptions
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.expressions.dsl.generated.*
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc
import kotlin.collections.firstOrNull
import com.mapbox.maps.plugin.ModelScaleMode
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener

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
    private var shouldFitCameraToRoute = true
    private var currentRouteGeometry: String? = null
    private var traveledRouteGeometry: String? = null
    private var isVoiceEnabled = true
    private var fullNavigationPoints: List<Point> = emptyList()
    private lateinit var stopsAdapter: com.example.bustrack_app.adapter.NavigationStopsAdapter
    private lateinit var bottomSheetBehavior: com.google.android.material.bottomsheet.BottomSheetBehavior<View>

    private var lastLegIndex = -1
    private var navStartIndex = 0
    private var nextGlobalStopIndex = 0
    private var currentNavPoints: List<Point> = emptyList()
    private var latestRouteProgress: com.mapbox.navigation.base.trip.model.RouteProgress? = null
    private val stopArrivalTimes = mutableMapOf<Int, String>()
    private val traveledHistoryPoints = mutableListOf<Point>()
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private var activeStopStatus = "NEXT" // NEXT, ARRIVED, PASSED
    private var lastArrivedStopIndex = -1
    private var isCurrentlyAtStop = false
    private val ARRIVAL_RADIUS = 80.0 // meters
    private val DEPARTURE_RADIUS = 70.0 // 70 meters to prevent flickering
    private var currentRawLocation: Location? = null

    private var departureCandidateIndex = -1
    private var departureConfirmCount = 0
    private val DEPARTURE_CONFIRM_THRESHOLD = 3

    private var lastSplitIndex = 0
    private val SPLIT_SEARCH_WINDOW = 120

    private var offRouteBackupCount = 0
    private val OFF_ROUTE_BACKUP_THRESHOLD_METERS = 80.0
    private val OFF_ROUTE_BACKUP_CONFIRM_COUNT = 3
    private var lastBackupRerouteTimeMs = 0L
    private val MIN_REROUTE_GAP_MS = 5000L

    private var isNorthUp = false
    private var isUserTriggeredChange = true
    private var lastDutyToggleTime = 0L
    private val DUTY_SYNC_DEBOUNCE_MS = 3000L

    private val dutyHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var dutyAutoOffRunnable: Runnable? = null

    private var lastAppliedBusScale = -1f
    private val MIN_BUS_MODEL_SCALE = 1.7f
    private val MAX_BUS_MODEL_SCALE = 2.0f
    private val BUS_MODEL_SCALE_REFERENCE_ZOOM = 17.0
    private val BUS_MODEL_SCALE_REFERENCE_VALUE = 1.0f
    private val BUS_MODEL_SCALE_COMPENSATION_FACTOR = 0.5
    private val BUS_MODEL_PITCH_COMPENSATION_FLOOR = 0.35

    // X/Y here correct a lean baked into the mesh's own coordinate space, separate from the
    // 90 degree Z value below which only sets facing direction. IMPORTANT: any non-zero X/Y
    // here interacts badly with turns. puckBearingEnabled rotates the model around the
    // world-vertical (Z) axis to match heading - that's correct and is the ONLY rotation
    // that should visibly change as the bus turns. But rotations don't commute: composing a
    // fixed roll/pitch (X/Y) with a heading-dependent yaw (Z) means the offset's visible
    // lean axis itself rotates as heading changes, so the model looks upright only near the
    // heading it was tuned at and increasingly tilted/sideways approaching 90 degrees away
    // from it - which is exactly the "tilts on left/right turns" symptom. Direct inspection
    // of bus.glb's own transform chain (see the flattened bus.glb from earlier) showed it is
    // already correctly upright with no real lean to correct, so both are reset to 0 - do
    // not re-introduce a non-zero X/Y here to fix a perceived tilt; that tilt was likely
    // this same offset, not something to compensate for.
    private val BUS_MODEL_ROLL_OFFSET_X_DEG = 0f
    private val BUS_MODEL_ROLL_OFFSET_Y_DEG = 0f
    private val DUTY_AUTO_OFF_GRACE_PERIOD_MS = 10 * 60 * 1000L // 10 minutes

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var isCurrentLocationLive = false
    private var assignedRoute: RouteModel? = null

    private var lastFirestoreLocation: Location? = null
    private var lastFirestoreUpdateTime = 0L

    companion object {
        private const val FIRESTORE_UPDATE_INTERVAL = 5000L
        private const val FIRESTORE_MIN_DISTANCE = 10f
    }
    private var locationCallback: LocationCallback? = null
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    private var mapboxNavigation: MapboxNavigation? = null
    private var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null
    private var speechApi: MapboxSpeechApi? = null
    private val navigationLocationProvider = NavigationLocationProvider()
    private val attendancePromptedStops = mutableSetOf<Int>()
    private var lastValidBearing: Double = 0.0
    private val MIN_SPEED_FOR_BEARING_UPDATE = 0.8 // m/s (~3 km/h)
    private var lastRawPositionForSnap: Point? = null
    private val MIN_GPS_MOVEMENT_FOR_SNAP_METERS = 3.0

    private val NAV_ROUTE_SOURCE_ID = "nav-route-source"
    private val NAV_TRAVELED_SOURCE_ID = "nav-traveled-source"
    private val NAV_ROUTE_LAYER_ID = "nav-route-layer"
    private val NAV_ROUTE_CASING_LAYER_ID = "nav-route-casing-layer"
    private val NAV_TRAVELED_LAYER_ID = "nav-traveled-layer"

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

        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        drawerLayout = binding.drawerLayout
        mapView = binding.mapView

        setupStopsRecyclerView()

        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            mapView?.mapboxMap?.setBounds(
                CameraBoundsOptions.Builder()
                    .minZoom(3.0)
                    .maxZoom(20.0)
                    .build()
            )
            setupInitialCamera()
            recreateAnnotationManagers()
            setupLocationPuck()
            setupMapGestures()
            updateMapDisplay()
            mapView?.mapboxMap?.addOnCameraChangeListener(cameraChangeListener)
        }

        findViewById<View>(R.id.drawerDutyContainer)?.visibility = View.VISIBLE

        binding.btnSound.setImageResource(if (isVoiceEnabled) R.drawable.volume_up else R.drawable.mute)
        binding.btnSound.imageTintList = ColorStateList.valueOf(Color.WHITE)

        handleLocationFlow()

        observeViewModel()
        observeDriverRepo()
        setupClickListeners()
        setupDrawerListeners()
        initNavigation()

        // mapboxNavigation is attached via MapboxNavigationApp, which is process/app-scoped -
        // its trip session and active route survive this Activity being destroyed and
        // recreated (backgrounding, a config change, or the OS reclaiming memory), as long as
        // the process itself isn't killed. So if a route is already active here, navigation
        // never actually stopped - only this Activity's own UI would go back to showing the
        // plain dashboard if we unconditionally reset to non-navigating mode below, which is
        // what made an in-progress navigation session look like it had disappeared when
        // returning to this screen. Restore navigation mode instead when that's the case.
        val alreadyNavigating = mapboxNavigation?.getNavigationRoutes()?.isNotEmpty() == true
        if (alreadyNavigating) {
            setNavigationMode(true, reloadStyle = true)
            startFollowingPuck()
        } else {
            setNavigationMode(false, reloadStyle = false)
        }

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

        mapboxNavigation?.registerRoutesObserver(routesObserver)
        mapboxNavigation?.registerLocationObserver(locationObserver)
        mapboxNavigation?.registerRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.registerVoiceInstructionsObserver(voiceInstructionsObserver)
        mapboxNavigation?.registerOffRouteObserver(offRouteObserver)

        val locale = java.util.Locale.getDefault().language

        if (speechApi == null) {
            speechApi = MapboxSpeechApi(this, locale)
        }
        if (voiceInstructionsPlayer == null) {
            voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(this, locale)
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        if (isVoiceEnabled) {
            speechApi?.generate(voiceInstructions) { expected ->
                val value = expected.value
                if (value != null) {
                    voiceInstructionsPlayer?.play(value.announcement) { speechAnnouncement ->
                        speechApi?.clean(speechAnnouncement)
                    }
                }
            }
        }
    }

    private val offRouteObserver = OffRouteObserver { isOffRoute ->
        if (isOffRoute && isNavigating) {
            runOnUiThread {
                Log.d("NavDebug", "Driver is off-route. Triggering automatic reroute...")
                triggerReroute()
            }
        }
    }

    private fun triggerReroute() {
        val route = assignedRoute ?: return
        val nav = mapboxNavigation ?: return
        val loc = currentLocation ?: return

        val currentPoint = Point.fromLngLat(loc.longitude, loc.latitude)

        val navPoints = mutableListOf<Point>()
        navPoints.add(currentPoint)

        val progress = latestRouteProgress

        val maxVisitedIdx = stopArrivalTimes.keys.maxOrNull() ?: -1
        val targetStopIndex = Math.max(nextGlobalStopIndex, maxVisitedIdx + 1)

        val allStops = route.stopsList
            .filter { it.latitude != 0.0 && it.longitude != 0.0 }
            .map { Point.fromLngLat(it.longitude, it.latitude) }

        if (targetStopIndex < allStops.size) {
            navPoints.addAll(allStops.subList(targetStopIndex, allStops.size))
        } else if (route.pathPoints.isNotEmpty()) {
            navPoints.add(Point.fromLngLat(route.pathPoints.last().longitude, route.pathPoints.last().latitude))
        }

        if (navPoints.size < 2) return

        currentNavPoints = navPoints

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
                    navStartIndex = targetStopIndex
                    nextGlobalStopIndex = targetStopIndex
                    nav.setNavigationRoutes(routes)
                    Log.d("NavDebug", "Automatic reroute successful from current location to stop $navStartIndex")
                }
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    Log.e("NavDebug", "Reroute failed: ${reasons.firstOrNull()?.message}")
                }
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {}
            }
        )
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(result: RoutesUpdatedResult) {
            val routes = result.navigationRoutes
            if (routes.isNotEmpty()) {
                val route = routes[0]
                fullNavigationPoints = route.directionsRoute.geometry()?.let {
                    LineString.fromPolyline(it, 6).coordinates()
                } ?: emptyList()
                lastSplitIndex = 0

                if (isNavigating) {
                    currentLocation?.let { loc ->
                        updateNavigationRouteProgress(Point.fromLngLat(loc.longitude, loc.latitude))
                    }
                } else {
                    drawPointsOnMap(fullNavigationPoints)
                }
            }
        }
    }

    private val locationObserver = object : LocationObserver {
        override fun onNewRawLocation(rawLocation: com.mapbox.common.location.Location) {
            currentRawLocation = android.location.Location("raw").apply {
                latitude = rawLocation.latitude
                longitude = rawLocation.longitude
            }
        }
        override fun onNewLocationMatcherResult(locationMatcherResult: LocationMatcherResult) {
            val rawEnhancedLocation = locationMatcherResult.enhancedLocation

            val currentSpeed = rawEnhancedLocation.speed ?: 0.0
            val newBearing = rawEnhancedLocation.bearing
            if (currentSpeed >= MIN_SPEED_FOR_BEARING_UPDATE && newBearing != null) {
                lastValidBearing = newBearing
            }
            val enhancedLocation = rawEnhancedLocation.toBuilder()
                .bearing(lastValidBearing)
                .build()

            val transitionOptions: (android.animation.ValueAnimator.() -> Unit) = { duration = 1000 }
            navigationLocationProvider.changePosition(
                location = enhancedLocation,
                keyPoints = locationMatcherResult.keyPoints,
                latLngTransitionOptions = transitionOptions,
                bearingTransitionOptions = transitionOptions
            )

            val androidLocation = android.location.Location("mapbox").apply {
                latitude = enhancedLocation.latitude
                longitude = enhancedLocation.longitude
                speed = enhancedLocation.speed?.toFloat() ?: 0f
                bearing = enhancedLocation.bearing?.toFloat() ?: 0f
            }
            runOnUiThread {
                val wasLive = isCurrentLocationLive
                currentLocation = androidLocation
                isCurrentLocationLive = true

                if (isNavigating) {
                    updateNavigationRouteProgress(Point.fromLngLat(androidLocation.longitude, androidLocation.latitude))

                    val speedKph = (androidLocation.speed * 3.6).toInt()
                    binding.bottomSummaryCard.findViewById<TextView>(R.id.tvSpeedSheet)?.text = "$speedKph km/h"
                    binding.tvSpeedNav.text = "$speedKph"

                    val geocoder = Geocoder(this@DriverDashboardActivity, Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(androidLocation.latitude, androidLocation.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val displayAddr = addr.getAddressLine(0).replace(Regex("^[A-Z0-9]{4,8}\\+[A-Z0-9]{2,4}\\s*"), "")
                            binding.bottomSummaryCard.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = displayAddr
                        }
                    } catch (e: Exception) {}
                } else if (!wasLive) {
                    updateMapDisplay()
                }
            }
        }
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: com.mapbox.navigation.base.trip.model.RouteProgress) {
            latestRouteProgress = routeProgress
            runOnUiThread {
                val distanceRemaining = routeProgress.distanceRemaining / 1000.0
                val durationRemaining = routeProgress.durationRemaining / 60.0

                binding.tvEstDistance.text = String.format(Locale.getDefault(), "%.1f km", distanceRemaining)
                binding.tvEstDuration.text = String.format(Locale.getDefault(), "%d min", durationRemaining.toInt())

                val sheet = binding.bottomSummaryCard
                val tvEta = sheet.findViewById<TextView>(R.id.tvEtaSheet)
                val tvSpeed = sheet.findViewById<TextView>(R.id.tvSpeedSheet)
                val tvLoad = sheet.findViewById<TextView>(R.id.tvLoadSheet)

                val speedKphDouble = (currentLocation?.speed?.times(3.6)) ?: 0.0
                val speedKph = speedKphDouble.toInt()
                tvSpeed?.text = "$speedKph km/h"
                binding.tvSpeedNav.text = "$speedKph"

                currentLocation?.let { loc ->
                    val geocoder = Geocoder(this@DriverDashboardActivity, Locale.getDefault())
                    try {
                        val addresses = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
                        if (!addresses.isNullOrEmpty()) {
                            val addr = addresses[0]
                            val displayAddr = addr.getAddressLine(0).replace(Regex("^[A-Z0-9]{4,8}\\+[A-Z0-9]{2,4}\\s*"), "")
                            binding.bottomSummaryCard.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = displayAddr
                        } else {
                            binding.bottomSummaryCard.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text =
                                String.format(Locale.getDefault(), "Current: Lat %.4f, Lng %.4f", loc.latitude, loc.longitude)
                        }
                    } catch (e: Exception) {
                        binding.bottomSummaryCard.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text =
                            String.format(Locale.getDefault(), "Current: Lat %.4f, Lng %.4f", loc.latitude, loc.longitude)
                    }
                }

                updateLoadStat(tvLoad)

                val currentLegIndex = routeProgress.currentLegProgress?.legIndex ?: 0
                val mapboxSuggestedIndex = navStartIndex + currentLegIndex

                if (!isCurrentlyAtStop && mapboxSuggestedIndex > nextGlobalStopIndex) {
                    nextGlobalStopIndex = mapboxSuggestedIndex
                }

                val stops = assignedRoute?.stopsList ?: emptyList()

                stops.forEachIndexed { index, stop ->
                    if (index == nextGlobalStopIndex && !stopArrivalTimes.containsKey(index)) {
                        val matchedResults = FloatArray(1)
                        Location.distanceBetween(
                            currentLocation?.latitude ?: 0.0,
                            currentLocation?.longitude ?: 0.0,
                            stop.latitude,
                            stop.longitude,
                            matchedResults
                        )

                        var distance = matchedResults[0].toDouble()

                        currentRawLocation?.let { raw ->
                            val rawResults = FloatArray(1)
                            Location.distanceBetween(
                                raw.latitude, raw.longitude,
                                stop.latitude, stop.longitude,
                                rawResults
                            )
                            if (rawResults[0] < distance) distance = rawResults[0].toDouble()
                        }

                        if (distance <= ARRIVAL_RADIUS) {
                            val arrivalTime = timeFormat.format(Calendar.getInstance().time)
                            stopArrivalTimes[index] = arrivalTime
                            lastArrivedStopIndex = index
                            isCurrentlyAtStop = true
                            activeStopStatus = "ARRIVED"
                        }
                    }

                    if (index < nextGlobalStopIndex && !stopArrivalTimes.containsKey(index)) {
                        stopArrivalTimes[index] = "Skipped"
                    }

                    if (stopArrivalTimes.containsKey(index) && stopArrivalTimes[index] != "Skipped" && !attendancePromptedStops.contains(index)) {
                        attendancePromptedStops.add(index)

                        (supportFragmentManager.findFragmentByTag("AttendanceSheet") as? com.google.android.material.bottomsheet.BottomSheetDialogFragment)?.dismiss()
                        val isMorningTrip = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 14
                        val bottomSheet = AttendanceBottomSheet.newInstance(stop.stopName, assignedRoute?.routeName ?: "", isMorningTrip)
                        bottomSheet.show(supportFragmentManager, "AttendanceSheet")
                    }
                }

                if (isCurrentlyAtStop && lastArrivedStopIndex != -1) {
                    val currentStop = stops.getOrNull(lastArrivedStopIndex)
                    if (currentStop != null) {
                        val results = FloatArray(1)
                        Location.distanceBetween(
                            currentLocation?.latitude ?: 0.0,
                            currentLocation?.longitude ?: 0.0,
                            currentStop.latitude,
                            currentStop.longitude,
                            results
                        )
                        val distanceFromStop = results[0].toDouble()

                        if (distanceFromStop > DEPARTURE_RADIUS) {
                            if (departureCandidateIndex == lastArrivedStopIndex) {
                                departureConfirmCount++
                            } else {
                                departureCandidateIndex = lastArrivedStopIndex
                                departureConfirmCount = 1
                            }

                            if (departureConfirmCount >= DEPARTURE_CONFIRM_THRESHOLD) {
                                val departedStopIndex = lastArrivedStopIndex

                                isCurrentlyAtStop = false
                                activeStopStatus = "PASSED"

                                nextGlobalStopIndex = departedStopIndex + 1

                                if (nextGlobalStopIndex < stops.size) {
                                    triggerReroute()
                                }

                                lastArrivedStopIndex = -1
                                departureCandidateIndex = -1
                                departureConfirmCount = 0
                            }
                        } else {
                            departureCandidateIndex = -1
                            departureConfirmCount = 0
                        }
                    }
                }

                val liveArrivedIndex = if (isCurrentlyAtStop && lastArrivedStopIndex != -1) {
                    lastArrivedStopIndex
                } else {
                    -1
                }
                val displayStopIndex = if (liveArrivedIndex != -1) liveArrivedIndex else nextGlobalStopIndex

                val isRouteProgressFreshForDisplay = mapboxSuggestedIndex == nextGlobalStopIndex

                val etaString = if (liveArrivedIndex != -1 && stopArrivalTimes.containsKey(liveArrivedIndex)) {
                    val arrival = stopArrivalTimes[liveArrivedIndex]
                    "Arrived: $arrival"
                } else if (!isRouteProgressFreshForDisplay) {
                    tvEta?.text?.toString() ?: "On Way"
                } else if (displayStopIndex < stops.size) {
                    "${durationRemaining.toInt()} min"
                } else {
                    "Route completed"
                }
                tvEta?.text = etaString

                val legs = routeProgress.route.legs()
                var accumulatedSeconds = (routeProgress.currentLegProgress?.durationRemaining ?: 0.0).toInt()

                stops.forEachIndexed { index, stop ->
                    val arrivalTime = stopArrivalTimes[index]
                    if (arrivalTime == "Skipped") {
                        stop.time = "Skipped"
                    } else if (arrivalTime != null) {
                        stop.time = "Arrived: $arrivalTime"
                    } else if (index == displayStopIndex) {
                        val etaTime = Calendar.getInstance().apply { add(Calendar.SECOND, accumulatedSeconds) }.time
                        stop.time = "ETA: ${timeFormat.format(etaTime)}"
                    } else if (index > displayStopIndex) {
                        if (legs != null && (index - navStartIndex) < legs.size) {
                            val legIdx = index - navStartIndex
                            if (legIdx >= 0 && legs[legIdx] != null) {
                                accumulatedSeconds += (legs[legIdx].duration() ?: 0.0).toInt()
                            }
                        }
                        val etaTime = Calendar.getInstance().apply { add(Calendar.SECOND, accumulatedSeconds) }.time
                        stop.time = "ETA: ${timeFormat.format(etaTime)}"
                    } else {
                        stop.time = "ETA: --"
                    }
                }

                stopsAdapter.updateStops(stops, liveArrivedIndex)

                val bannerInstructions = routeProgress.bannerInstructions
                val primary = bannerInstructions?.primary()
                val sub = bannerInstructions?.sub()
                binding.tvNextInstruction.text = primary?.text() ?: "Continue straight"
                binding.tvNextStreet.text = sub?.text() ?: "Current Route"
            }
        }
    }

    private fun updateLoadStat(tvLoad: TextView?) {
        val route = assignedRoute ?: return
        val routeName = route.routeName
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date())

        if (tvLoad?.text == "--" || tvLoad?.text.isNullOrEmpty()) tvLoad?.text = "0/0"

        com.example.bustrack_app.data.FirebaseRepository.fetchStudentsByRoute(routeName) { students ->
            com.example.bustrack_app.data.FirebaseRepository.fetchAttendance { allAttendance ->
                val records = allAttendance.filter { it.route == routeName && it.date == today }
                val isMorning = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 14

                val loadString: String
                if (isMorning) {
                    val presentCount = records.count { it.morningPickup.equals("Present", true) }
                    loadString = "$presentCount/${students.size}"
                } else {
                    val eveningExpected = records.count { it.eveningPickup.equals("Present", true) }
                    val droppedCount = records.count { it.eveningDrop.equals("Dropped", true) }
                    val currentLoad = eveningExpected - droppedCount
                    loadString = "${if (currentLoad < 0) 0 else currentLoad}/$eveningExpected"
                }

                runOnUiThread {
                    tvLoad?.text = loadString
                }
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

    private fun toMapboxLocation(location: Location): com.mapbox.common.location.Location {
        val builder = com.mapbox.common.location.Location.Builder()
            .latitude(location.latitude)
            .longitude(location.longitude)

        if (location.hasSpeed() && location.speed >= MIN_SPEED_FOR_BEARING_UPDATE && location.hasBearing()) {
            lastValidBearing = location.bearing.toDouble()
        }
        builder.bearing(lastValidBearing)

        if (location.hasSpeed()) {
            builder.speed(location.speed.toDouble())
        }
        return builder.build()
    }

    private fun feedRawLocationToPuck(location: Location) {
        navigationLocationProvider.changePosition(
            location = toMapboxLocation(location)
        )
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1001)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                val wasLive = isCurrentLocationLive
                currentLocation = location
                isCurrentLocationLive = true
                feedRawLocationToPuck(location)
                if (!wasLive) {
                    updateMapDisplay()
                }
            }
        }

        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(1000)
            .setMaxUpdateDelayMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                if (!isDutyEnabled) return

                for (location in locationResult.locations) {
                    val wasLive = isCurrentLocationLive
                    currentLocation = location
                    isCurrentLocationLive = true
                    feedRawLocationToPuck(location)

                    if (isDutyEnabled) {
                        syncTrackingDataToFirestore(location)

                        if (isNavigating) {
                            runOnUiThread {
                                updateNavigationRouteProgress(Point.fromLngLat(location.longitude, location.latitude))
                            }
                        } else if (!wasLive) {
                            runOnUiThread { updateMapDisplay() }
                        }
                    }
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, mainLooper)
    }

    private fun syncTrackingDataToFirestore(location: Location) {
        val driverId = viewModel.currentDriver.value?.driverId ?: return
        if (!isDutyEnabled) return

        val now = System.currentTimeMillis()
        val distanceMoved = lastFirestoreLocation?.distanceTo(location) ?: Float.MAX_VALUE

        if (now - lastFirestoreUpdateTime >= FIRESTORE_UPDATE_INTERVAL || distanceMoved >= FIRESTORE_MIN_DISTANCE) {

            com.example.bustrack_app.data.FirebaseRepository.updateDriverLocation(
                driverId,
                location.latitude,
                location.longitude
            )

            val sheet = binding.bottomSummaryCard
            val tvEta = sheet.findViewById<TextView>(R.id.tvEtaSheet)
            val tvLoad = sheet.findViewById<TextView>(R.id.tvLoadSheet)

            val etaVal = tvEta?.text?.toString() ?: "On Way"
            val speedVal = (location.speed * 3.6)
            val loadVal = tvLoad?.text?.toString() ?: "0/0"

            com.example.bustrack_app.data.FirebaseRepository.updateDriverStats(
                driverId,
                etaVal,
                speedVal,
                loadVal
            )

            if (isNavigating) {
                val arrivalMap = stopArrivalTimes.mapKeys { it.key.toString() }
                com.example.bustrack_app.data.FirebaseRepository.updateDriverRouteGeometry(
                    driverId,
                    currentRouteGeometry,
                    traveledRouteGeometry,
                    nextGlobalStopIndex,
                    arrivalMap,
                    isNavigating
                )
            }

            lastFirestoreUpdateTime = now
            lastFirestoreLocation = Location(location)
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

    private fun updateMapDisplay() {
        val route = assignedRoute ?: return

        if (isNavigating) {
            return
        }

        val loc = currentLocation
        if (loc != null && isCurrentLocationLive) {
            fetchDynamicRoutePreview(route, loc)
            return
        }

        drawStaticSavedRoute(route)
    }

    private fun drawStaticSavedRoute(route: RouteModel) {
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
    }

    private fun fetchDynamicRoutePreview(route: RouteModel, origin: Location) {
        val originPoint = Point.fromLngLat(origin.longitude, origin.latitude)

        val remainingStopPoints = route.stopsList
            .filter { it.latitude != 0.0 && it.longitude != 0.0 }
            .map { Point.fromLngLat(it.longitude, it.latitude) }

        val destinationPoints = if (remainingStopPoints.isNotEmpty()) {
            remainingStopPoints
        } else if (route.pathPoints.isNotEmpty()) {
            listOf(Point.fromLngLat(route.pathPoints.last().longitude, route.pathPoints.last().latitude))
        } else {
            emptyList()
        }

        if (destinationPoints.isEmpty()) {
            drawStaticSavedRoute(route)
            return
        }

        val coordinates = mutableListOf(originPoint)
        coordinates.addAll(destinationPoints)

        val accessToken = MapboxOptions.accessToken ?: getString(R.string.mapbox_access_token)
        val routeOptions = RouteOptions.builder()
            .coordinatesList(coordinates)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
            .build()

        MapboxDirections.builder()
            .accessToken(accessToken)
            .routeOptions(routeOptions)
            .build()
            .enqueueCall(object : Callback<DirectionsResponse> {
                override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                    if (isNavigating) return
                    val geometry = response.body()?.routes()?.firstOrNull()?.geometry()
                    if (geometry != null) {
                        val points = LineString.fromPolyline(geometry, 6).coordinates()
                        runOnUiThread { drawPointsOnMap(points) }
                    } else {
                        runOnUiThread { drawStaticSavedRoute(route) }
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Log.e("NavDebug", "Dynamic dashboard route preview failed: ${t.message}")
                    runOnUiThread { drawStaticSavedRoute(route) }
                }
            })
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
        binding.tvGreeting.text = "${greeting.uppercase()}, \ud83d\udc4b"
        binding.tvDriverName.text = driver.name

        if (driver.profileImageUrl.isNotEmpty()) {
            Glide.with(this).load(driver.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(binding.ivProfile)
        } else {
            binding.ivProfile.setImageResource(R.drawable.ic_person)
        }

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

    private fun computeBusModelScale(zoom: Double, pitch: Double = 0.0): Float {
        val pitchCompensation = 1.0 / kotlin.math.cos(Math.toRadians(pitch))
            .coerceAtLeast(BUS_MODEL_PITCH_COMPENSATION_FLOOR)

        val apparentExponent = (BUS_MODEL_SCALE_REFERENCE_ZOOM - zoom) * BUS_MODEL_SCALE_COMPENSATION_FACTOR
        val apparentTarget = (BUS_MODEL_SCALE_REFERENCE_VALUE * Math.pow(2.0, apparentExponent))
            .coerceIn(MIN_BUS_MODEL_SCALE.toDouble(), MAX_BUS_MODEL_SCALE.toDouble())

        val worldToScreenCompensation = Math.pow(2.0, BUS_MODEL_SCALE_REFERENCE_ZOOM - zoom) * pitchCompensation
        return (apparentTarget * worldToScreenCompensation).toFloat()
    }

    private fun setupLocationPuck() {
        mapView?.location?.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = isDutyEnabled
            pulsingEnabled = isDutyEnabled
            puckBearingEnabled = true

            val initialZoom = mapView?.mapboxMap?.cameraState?.zoom ?: BUS_MODEL_SCALE_REFERENCE_ZOOM
            val initialPitch = mapView?.mapboxMap?.cameraState?.pitch ?: 0.0
            val initialScale = computeBusModelScale(initialZoom, initialPitch)

            locationPuck = LocationPuck3D(
                modelUri = "asset://bus.glb",
                modelScale = listOf(initialScale, initialScale, initialScale),
                modelScaleMode = ModelScaleMode.MAP,
                modelTranslation = listOf(0f, 0f, 0f),
                modelRotation = listOf(BUS_MODEL_ROLL_OFFSET_X_DEG, BUS_MODEL_ROLL_OFFSET_Y_DEG, 90f)
            )
            lastAppliedBusScale = initialScale
        }

        currentLocation?.let {
            feedRawLocationToPuck(it)
        }
        updateBusModelScaleForZoom()
    }

    private val cameraChangeListener = OnCameraChangeListener {
        updateBusModelScaleForZoom()
    }

    private fun updateBusModelScaleForZoom() {
        val cameraState = mapView?.mapboxMap?.cameraState ?: return
        val newScale = computeBusModelScale(cameraState.zoom, cameraState.pitch)

        if (kotlin.math.abs(newScale - lastAppliedBusScale) < 0.05f) return
        lastAppliedBusScale = newScale

        mapView?.location?.locationPuck = LocationPuck3D(
            modelUri = "asset://bus.glb",
            modelScale = listOf(newScale, newScale, newScale),
            modelScaleMode = ModelScaleMode.MAP,
            modelTranslation = listOf(0f, 0f, 0f),
            modelRotation = listOf(BUS_MODEL_ROLL_OFFSET_X_DEG, BUS_MODEL_ROLL_OFFSET_Y_DEG, 90f)
        )
    }

    private fun observeViewModel() {
        viewModel.currentDriver.observe(this) { driver ->
            if (driver != null) {
                if (stopArrivalTimes.isEmpty() && driver.stopArrivalTimes.isNotEmpty()) {
                    driver.stopArrivalTimes.forEach { (k, v) ->
                        stopArrivalTimes[k.toInt()] = v
                    }
                }

                if (nextGlobalStopIndex == 0 && driver.nextStopIndex != 0) {
                    nextGlobalStopIndex = driver.nextStopIndex
                }

                if (currentLocation == null && driver.latitude != 0.0 && driver.longitude != 0.0) {
                    currentLocation = android.location.Location("firestore").apply {
                        latitude = driver.latitude
                        longitude = driver.longitude
                    }
                    isCurrentLocationLive = false
                    centerCameraOnUser()
                }
            }
        }

        viewModel.dashboardData.observe(this) { data ->
            binding.apply {
                tvBusNumberInfo.text = data.busNumber
                tvRouteNameInfo.text = data.currentRoute
                tvCurrentDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

                tvTotalStops.text = data.stopsCount

                val currentTime = System.currentTimeMillis()
                val isPendingSync = (currentTime - lastDutyToggleTime) < DUTY_SYNC_DEBOUNCE_MS

                if (!isPendingSync && data.isOnDuty != isDutyEnabled) {
                    isDutyEnabled = data.isOnDuty

                    isUserTriggeredChange = false
                    findViewById<SwitchMaterial>(R.id.switchDuty)?.isChecked = isDutyEnabled
                    isUserTriggeredChange = true

                    updateDutyUI(isDutyEnabled, reloadStyle = false)
                }
            }

            RouteRepository.routeList.value?.find { it.routeName == data.currentRoute }?.let { route ->
                if (assignedRoute?.id != route.id) {
                    isNearStart = false
                    shouldFitCameraToRoute = true
                    stopArrivalTimes.clear()
                    nextGlobalStopIndex = 0
                    attendancePromptedStops.clear()
                    traveledHistoryPoints.clear()

                    viewModel.currentDriver.value?.id?.let { driverId ->
                        // Same self-write echo risk as the End Navigation fix above: this
                        // write is read back via this same dashboardData listener, which
                        // could misread it as a real Duty change and flip isDutyEnabled off -
                        // this time triggered by an admin reassigning the driver's route
                        // instead of ending navigation. Same guard, same reason.
                        lastDutyToggleTime = System.currentTimeMillis()
                        com.example.bustrack_app.data.FirebaseRepository.updateDriverRouteGeometry(
                            driverId, null, null, 0, emptyMap(), false
                        )
                    }
                }
                assignedRoute = route
                binding.tvStartAddress.text = route.startPoint.ifEmpty { "Main Terminal" }
                if (route.stopsList.isNotEmpty()) {
                    binding.tvEndAddress.text = route.stopsList[0].stopName
                } else {
                    binding.tvEndAddress.text = route.endPoint
                }

                updateMapDisplay()
            }
        }
    }

    private fun drawPointsOnMap(points: List<Point>) {
        if (points.isEmpty()) return

        pointAnnotationManager?.deleteAll()
        assignedRoute?.stopsList?.forEachIndexed { index, stop ->
            val isCompleted = stopArrivalTimes.containsKey(index) || (isNavigating && index < nextGlobalStopIndex)
            val iconRes = if (isCompleted) R.drawable.ic_marker_dest_grey else R.drawable.ic_marker_dest
            addMarker(Point.fromLngLat(stop.longitude, stop.latitude), iconRes, stop.stopName)
        }

        polylineAnnotationManager?.deleteAll()
        if (!isNavigating) {
            val polylineOptions = PolylineAnnotationOptions()
                .withPoints(points)
                .withLineColor("#1565C0")
                .withLineWidth(4.0)
                .withLineOpacity(0.8)
            polylineAnnotationManager?.create(polylineOptions)

            // Smaller than the default marker size (0.8) so this fixed route-start point
            // doesn't visually read as a live location indicator - that role belongs only to
            // the 3D bus marker.
            addMarker(points.first(), R.drawable.green_dot, iconSize = 0.45)
        }

        if (isNavigating) return

        if (!shouldFitCameraToRoute) return

        val cameraOptions = if (points.size == 1) {
            CameraOptions.Builder()
                .center(points[0])
                .zoom(15.0)
                .pitch(0.0)
                .bearing(0.0)
                .build()
        } else {
            mapView?.mapboxMap?.cameraForCoordinates(
                points,
                EdgeInsets(250.0, 100.0, 150.0, 100.0),
                0.0,
                0.0
            )
        }

        cameraOptions?.let {
            mapView?.camera?.flyTo(
                it,
                MapAnimationOptions.mapAnimationOptions {
                    duration(1500)
                }
            )
            shouldFitCameraToRoute = false
        }
    }

    private fun updateNavigationRouteProgress(currentPos: Point) {
        if (fullNavigationPoints.size < 2) return

        lastRawPositionForSnap?.let { lastRaw ->
            val movedMeters = TurfMeasurement.distance(currentPos, lastRaw, TurfConstants.UNIT_METERS)
            if (movedMeters < MIN_GPS_MOVEMENT_FOR_SNAP_METERS) {
                return
            }
        }
        lastRawPositionForSnap = currentPos

        try {
            val snappedPoint = TurfMisc.nearestPointOnLine(currentPos, fullNavigationPoints)
            val snappedP = snappedPoint.geometry() as? Point ?: return

            val searchStart = lastSplitIndex
            val searchEnd = minOf(fullNavigationPoints.size - 1, lastSplitIndex + SPLIT_SEARCH_WINDOW)
            var splitIndex = searchStart
            var minDistance = Double.MAX_VALUE
            for (i in searchStart..searchEnd) {
                val dist = TurfMeasurement.distance(currentPos, fullNavigationPoints[i], TurfConstants.UNIT_METERS)
                if (dist < minDistance) {
                    minDistance = dist
                    splitIndex = i
                }
            }
            splitIndex = maxOf(splitIndex, lastSplitIndex)
            lastSplitIndex = splitIndex

            // NOTE: the bus marker itself is fed from the actual (already map-matched/
            // jitter-gated) GPS position in locationObserver/feedRawLocationToPuck - not
            // from snappedP here. snappedP is used ONLY for the drawn traveled/upcoming
            // route-line split below and for off-route distance checks. Forcing the visual
            // marker onto the assigned route geometry would hide genuine deviation (e.g. a
            // wrong turn) behind a bus that always looks like it's on-route - the
            // off-route detector below is what should catch and react to that instead.

            if (isNavigating && minDistance > OFF_ROUTE_BACKUP_THRESHOLD_METERS) {
                offRouteBackupCount++
                val now = System.currentTimeMillis()
                if (offRouteBackupCount >= OFF_ROUTE_BACKUP_CONFIRM_COUNT &&
                    now - lastBackupRerouteTimeMs > MIN_REROUTE_GAP_MS
                ) {
                    Log.d("NavDebug", "Backup off-route check triggered reroute (missed turn?)")
                    lastBackupRerouteTimeMs = now
                    offRouteBackupCount = 0
                    triggerReroute()
                }
            } else {
                offRouteBackupCount = 0
            }

            val traveledPoints = fullNavigationPoints.subList(0, splitIndex + 1)

            val upcomingPoints = mutableListOf<Point>()
            upcomingPoints.add(snappedP)
            if (splitIndex + 1 < fullNavigationPoints.size) {
                upcomingPoints.addAll(fullNavigationPoints.subList(splitIndex + 1, fullNavigationPoints.size))
            }

            mapView?.mapboxMap?.getStyle { style ->
                var traveledPolyline: String? = null
                if (traveledPoints.size >= 2) {
                    val traveledLine = LineString.fromLngLats(traveledPoints)
                    (style.getSource(NAV_TRAVELED_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                        ?.geometry(traveledLine)
                    traveledPolyline = traveledLine.toPolyline(6)
                }

                var currentPolyline: String? = null
                val upcomingLine = LineString.fromLngLats(upcomingPoints)
                (style.getSource(NAV_ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                    ?.geometry(upcomingLine)

                if (upcomingPoints.size >= 2) {
                    currentPolyline = upcomingLine.toPolyline(6)
                }

                this@DriverDashboardActivity.currentRouteGeometry = currentPolyline
                this@DriverDashboardActivity.traveledRouteGeometry = traveledPolyline
            }
        } catch (e: Exception) {
            Log.e("NavDebug", "Error updating route progress", e)
        }
    }

    private fun addMarker(point: Point, iconRes: Int, title: String? = null, iconSize: Double = 0.8) {
        val bitmap = bitmapFromDrawableRes(this, iconRes)
        if (bitmap != null) {
            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(iconSize)

            title?.let {
                options.withTextField(it)
                options.withTextSize(10.0)
                options.withTextOffset(listOf(0.0, 1.5))
                options.withTextColor(Color.BLACK)
                options.withTextHaloColor(Color.WHITE)
                options.withTextHaloWidth(1.0)
            }

            pointAnnotationManager?.create(options)
        }
    }

    // Creates fresh point/polyline annotation managers, first properly removing any managers
    // already held in polylineAnnotationManager/pointAnnotationManager. Each
    // createXAnnotationManager() call adds its own new layer to the current style; calling it
    // again without removing the previous manager leaves that old layer (and every marker/
    // polyline on it, e.g. the route-start green dot) behind on the map, where it stacks up
    // every time navigation is started again. removeAnnotationManager() is safe to call even
    // if the underlying style was already swapped out (e.g. by loadStyle) and the old layer is
    // already gone - it's a no-op in that case rather than throwing.
    private fun recreateAnnotationManagers() {
        pointAnnotationManager?.let { mapView?.annotations?.removeAnnotationManager(it) }
        polylineAnnotationManager?.let { mapView?.annotations?.removeAnnotationManager(it) }
        polylineAnnotationManager = mapView?.annotations?.createPolylineAnnotationManager()
        pointAnnotationManager = mapView?.annotations?.createPointAnnotationManager()
    }

    // Wipes the active-navigation route/traveled GeoJson sources back to empty geometry. Used
    // right when navigation (re)starts, before the first real update fills them in, so a
    // previous session's route geometry can never linger and get drawn as a stray/jumping
    // segment underneath (or crossing) the new route while the fresh geometry is still loading.
    private fun clearNavigationRouteGeometry(style: Style) {
        val empty = LineString.fromLngLats(emptyList())
        (style.getSource(NAV_ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
            ?.geometry(empty)
        (style.getSource(NAV_TRAVELED_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
            ?.geometry(empty)
    }

    private fun setupClickListeners() {
        binding.btnMenuDrawer.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }

        findViewById<View>(R.id.layoutProfileArea).setOnClickListener(null)
        findViewById<View>(R.id.layoutProfileArea).isClickable = false
        findViewById<View>(R.id.layoutProfileArea).isFocusable = false

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
                drawerLayout.openDrawer(androidx.core.view.GravityCompat.END)
                Toast.makeText(this, "Please enable On Duty Mode before starting navigation.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (route == null) {
                Toast.makeText(this, "No route assigned to you yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentLocation == null) {
                Toast.makeText(this, "Fetching current location...", Toast.LENGTH_SHORT).show()
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            currentLocation = location
                            handleStartNavigation(route)
                        } else {
                            Toast.makeText(this@DriverDashboardActivity, "Unable to fetch location. Please check your GPS.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                return@setOnClickListener
            }

            handleStartNavigation(route)
        }

        binding.bottomSummaryCard.findViewById<View>(R.id.btnCloseNav)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            setNavigationMode(false)
        }

        binding.bottomSummaryCard.findViewById<View>(R.id.btnViewRoute)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startFollowingPuck()
        }

        binding.btnNotifications.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    private fun handleStartNavigation(route: RouteModel) {
        if (isNearStart) {
            updateBottomSheetInfo()
            startNavigationAnimation()
            return
        }

        val currentPoint = currentLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }
        val startPoint = if (route.pathPoints.isNotEmpty()) {
            Point.fromLngLat(route.pathPoints[0].longitude, route.pathPoints[0].latitude)
        } else if (route.stopsList.isNotEmpty()) {
            Point.fromLngLat(route.stopsList[0].longitude, route.stopsList[0].latitude)
        } else null

        if (currentPoint != null && startPoint != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                currentPoint.latitude(), currentPoint.longitude(),
                startPoint.latitude(), startPoint.longitude(),
                results
            )
            val distanceMeters = results[0]

            if (distanceMeters <= 150.0) {
                isNearStart = true
                updateBottomSheetInfo()
                startNavigationAnimation()
            } else {
                showStartPointError(route)
            }
        } else {
            showStartPointError(route)
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

        val sheet = binding.bottomSummaryCard
        sheet.findViewById<TextView>(R.id.tvBusIdSheet)?.text = route.busNo.ifEmpty { "BUS-TRACK" }
        sheet.findViewById<TextView>(R.id.tvRouteSheet)?.text = route.routeName
        sheet.findViewById<TextView>(R.id.tvDriverNameSheet)?.text = driver?.name ?: "Driver"

        val tvEta = sheet.findViewById<TextView>(R.id.tvEtaSheet)
        val tvSpeed = sheet.findViewById<TextView>(R.id.tvSpeedSheet)
        val tvLoad = sheet.findViewById<TextView>(R.id.tvLoadSheet)

        tvEta?.text = "Calculating..."

        val currentSpeed = (currentLocation?.speed?.times(3.6)) ?: 0.0
        tvSpeed?.text = "${currentSpeed.toInt()} km/h"

        updateLoadStat(tvLoad)

        var startIndex = 0
        currentLocation?.let { loc ->
            val curPoint = Point.fromLngLat(loc.longitude, loc.latitude)
            var minDistance = Double.MAX_VALUE
            for (i in route.stopsList.indices) {
                val stop = route.stopsList[i]
                val dist = TurfMeasurement.distance(curPoint, Point.fromLngLat(stop.longitude, stop.latitude), TurfConstants.UNIT_METERS)
                if (dist < minDistance) {
                    minDistance = dist
                    startIndex = i
                }
            }
        }
        val stops = route.stopsList
        stops.forEachIndexed { index, stop ->
            val arrival = stopArrivalTimes[index]
            if (arrival == "Skipped") {
                stop.time = "Skipped"
            } else if (arrival != null) {
                stop.time = "Arrived: $arrival"
            } else {
                stop.time = "TBD"
            }
        }

        val liveArrivedIndexForSheet = if (isCurrentlyAtStop && lastArrivedStopIndex != -1) lastArrivedStopIndex else -1
        stopsAdapter.updateStops(stops, liveArrivedIndexForSheet)
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
                    .zoom(19.0)
                    .pitch(65.0)
                    .bearing(FollowPuckViewportStateBearing.SyncWithLocationPuck)
                    .padding(EdgeInsets(450.0, 0.0, 150.0, 0.0))
                    .build()
            )!!
        )
    }

    private fun followPuckNorthUp() {
        mapView?.viewport?.transitionTo(
            mapView?.viewport?.makeFollowPuckViewportState(
                FollowPuckViewportStateOptions.Builder()
                    .zoom(17.5)
                    .pitch(45.0)
                    .bearing(FollowPuckViewportStateBearing.Constant(0.0))
                    .build()
            )!!
        )
    }

    private fun toggleNorthUpMode() {
        isNorthUp = !isNorthUp
        if (isNorthUp) {
            binding.btnNorth.setImageResource(R.drawable.ic_compass)
            binding.btnNorth.imageTintList = ColorStateList.valueOf(Color.RED)
            followPuckNorthUp()
        } else {
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
            AlertOption("Road Block", "Road is closed, need alternative route", R.drawable.notification_active, "\ud83d\udea7"),
            AlertOption("Heavy Traffic", "Stuck in traffic, bus might be late", R.drawable.notification_active, "\ud83d\udea6"),
            AlertOption("Accident", "Accident on route or bus involved", R.drawable.notification_active, "\ud83d\ude97"),
            AlertOption("Bus Breakdown", "Engine or tyre issue", R.drawable.notification_active, "\ud83d\ude8c"),
            AlertOption("Fuel Issue", "Low fuel or tank empty", R.drawable.notification_active, "\u26fd"),
            AlertOption("Bad Weather", "Heavy rain, fog or storm", R.drawable.notification_active, "\ud83c\udf27\ufe0f"),
            AlertOption("Student Emergency", "Student needs medical help", R.drawable.notification_active, "\ud83d\udc68\u200d\ud83c\udf93"),
            AlertOption("Police Check", "Security check causing delay", R.drawable.notification_active, "\ud83d\udc6e"),
            AlertOption("Wrong Route", "Assigned route is closed", R.drawable.notification_active, "\ud83d\udccd"),
            AlertOption("Other", "Custom report or other issue", R.drawable.notification_active, "\ud83d\udcdd")
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
        dialog.setContentView(R.layout.dialog_driver_live_tracking)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDesc = dialog.findViewById<TextView>(R.id.tvDialogDescription)
        val btnSend = dialog.findViewById<MaterialButton>(R.id.btnEnableTracking)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelTracking)

        tvTitle?.text = "Other Issue"
        tvDesc?.text = "Please describe the issue you are facing."
        btnSend?.text = "Send Report"

        btnSend?.setOnClickListener {
            Toast.makeText(this, "Custom report sent to Admin", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        btnCancel?.setOnClickListener { dialog.dismiss() }
        dialog.show()
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

        currentLocation?.let { loc ->
            navPoints.add(Point.fromLngLat(loc.longitude, loc.latitude))
        }

        if (route.stopsList.isNotEmpty()) {
            val allStops = route.stopsList
                .filter { it.latitude != 0.0 && it.longitude != 0.0 }
                .map { Point.fromLngLat(it.longitude, it.latitude) }

            val maxVisitedIdx = stopArrivalTimes.keys.maxOrNull() ?: -1
            var startIndex = Math.max(nextGlobalStopIndex, maxVisitedIdx + 1)

            if (startIndex >= allStops.size) {
                // Every stop was already marked visited by the PREVIOUS navigation session on
                // this same route (route fully completed) - without this reset, startIndex
                // would stay at/past allStops.size forever, leaving zero stops to navigate to
                // and silently failing with "insufficient valid stops" below every time Start
                // Navigation is pressed again. Pressing Start Navigation is an explicit,
                // intentional action, so treat it as starting a brand new session for this
                // route: clear the previous run's completion state and start over from stop 0.
                // (This only fires when the WHOLE route was finished - it does not affect the
                // normal "resume mid-route after accidentally closing navigation" case, where
                // startIndex is still less than allStops.size and is left untouched below.)
                stopArrivalTimes.clear()
                attendancePromptedStops.clear()
                lastSplitIndex = 0
                startIndex = 0
            }

            if (startIndex < allStops.size) {
                navPoints.addAll(allStops.subList(startIndex, allStops.size))
            }

            navStartIndex = startIndex
            nextGlobalStopIndex = startIndex
        } else if (route.pathPoints.isNotEmpty()) {
            navPoints.add(Point.fromLngLat(route.pathPoints.first().longitude, route.pathPoints.first().latitude))
            navPoints.add(Point.fromLngLat(route.pathPoints.last().longitude, route.pathPoints.last().latitude))
            navStartIndex = 0
        }

        if (navPoints.size < 2) {
            Toast.makeText(this, "Route has insufficient valid stops to navigate", Toast.LENGTH_LONG).show()
            return
        }

        currentNavPoints = navPoints
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
                    isNavigating = true
                    nav.setNavigationRoutes(routes)

                    if (ActivityCompat.checkSelfPermission(this@DriverDashboardActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        nav.startTripSession()
                    }

                    startFollowingPuck()
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

    private fun setNavigationMode(isNavigating: Boolean, reloadStyle: Boolean = true) {
        this.isNavigating = isNavigating
        cancelDutyAutoOffTimer()
        binding.apply {
            if (isNavigating) {
                traveledHistoryPoints.clear()
                traveledRouteGeometry = null
                currentRouteGeometry = null

                toolbar.visibility = View.GONE
                headerBg.visibility = View.GONE
                dashboardTopContent.visibility = View.GONE

                instructionCard.visibility = View.VISIBLE
                instructionCard.setCardBackgroundColor(Color.parseColor("#0D1B3E"))

                updateBottomSheetTheme(true)

                mapView?.mapboxMap?.loadStyle("mapbox://styles/mapbox/navigation-night-v1") { style ->
                    setupNavigationLayers(style)
                    clearNavigationRouteGeometry(style)

                    style.styleLayers.forEach { layer ->
                        if (layer.id.contains("traffic") || layer.id.contains("congestion") || layer.id.contains("road-casing")) {
                            style.getLayer(layer.id)?.visibility(Visibility.NONE)
                        }
                    }

                    recreateAnnotationManagers()

                    setupLocationPuck()
                    mapView?.location?.pulsingEnabled = false

                    mapboxNavigation?.getNavigationRoutes()?.firstOrNull()?.let { navRoute ->
                        fullNavigationPoints = LineString.fromPolyline(navRoute.directionsRoute.geometry()!!, 6).coordinates()

                        drawPointsOnMap(fullNavigationPoints)

                        currentLocation?.let { loc ->
                            updateNavigationRouteProgress(Point.fromLngLat(loc.longitude, loc.latitude))
                        }
                    }

                    startFollowingPuck()
                }

                cardRouteDetails.visibility = View.GONE
                btnStartNavigation.visibility = View.GONE
                bottomSummaryCard.visibility = View.VISIBLE
                bottomSheetBehavior.isHideable = false
                bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED

                viewModel.currentDriver.value?.driverId?.let { driverId ->
                    val arrivalMap = stopArrivalTimes.mapKeys { it.key.toString() }
                    com.example.bustrack_app.data.FirebaseRepository.updateDriverRouteGeometry(
                        driverId, null, null, nextGlobalStopIndex, arrivalMap, true
                    )
                }

                lastLegIndex = -1

                infoBar.setBackgroundColor(Color.parseColor("#0D1B3E"))
                layoutMapControls.visibility = View.VISIBLE
                layoutMapControls.animate().translationY(-240f).setDuration(500).start()
                btnRecenter.animate().translationY(-240f).setDuration(500).start()
            } else {
                mapboxNavigation?.setNavigationRoutes(emptyList())
                fullNavigationPoints = emptyList()
                traveledHistoryPoints.clear()
                currentRouteGeometry = null
                traveledRouteGeometry = null

                mapView?.viewport?.idle()
                mapView?.mapboxMap?.setCamera(CameraOptions.Builder().padding(EdgeInsets(0.0, 0.0, 0.0, 0.0)).build())

                toolbar.visibility = View.VISIBLE
                headerBg.visibility = View.VISIBLE
                dashboardTopContent.visibility = View.VISIBLE
                instructionCard.visibility = View.GONE

                updateBottomSheetTheme(false)

                if (reloadStyle) {
                    mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
                        shouldFitCameraToRoute = true

                        recreateAnnotationManagers()

                        setupLocationPuck()
                        mapView?.location?.pulsingEnabled = true
                        updateMapDisplay()
                    }
                } else {
                    shouldFitCameraToRoute = true
                    updateMapDisplay()
                }

                binding.layoutMapControls.visibility = View.GONE
                binding.layoutMapControls.translationY = 0f
                binding.btnRecenter.visibility = View.GONE
                binding.btnRecenter.translationY = 0f

                cardRouteDetails.visibility = View.VISIBLE
                btnStartNavigation.visibility = View.VISIBLE

                bottomSummaryCard.visibility = View.GONE
                bottomSheetBehavior.isHideable = true
                bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN

                infoBar.setBackgroundColor(Color.TRANSPARENT)
                layoutMapControls.animate().translationY(0f).setDuration(500).start()

                viewModel.currentDriver.value?.driverId?.let { driverId ->
                    val arrivalMap = stopArrivalTimes.mapKeys { it.key.toString() }
                    // This write is being read back via the dashboardData listener (data.isOnDuty
                    // in observeViewModel()), which was interpreting it as an actual Duty change
                    // and flipping isDutyEnabled/the drawer switch off - even though ending
                    // navigation never touched Duty. lastDutyToggleTime is the exact guard already
                    // used for manual Duty toggles (see isPendingSync there) to suppress this kind
                    // of self-triggered echo; it just wasn't being set for this write.
                    lastDutyToggleTime = System.currentTimeMillis()
                    com.example.bustrack_app.data.FirebaseRepository.updateDriverRouteGeometry(
                        driverId, null, null, nextGlobalStopIndex, arrivalMap, false
                    )
                }
            }
        }
    }

    private fun showFullRouteOverview() {
        val navRoutes = mapboxNavigation?.getNavigationRoutes()
        if (navRoutes.isNullOrEmpty()) {
            Toast.makeText(this, "No active route to show", Toast.LENGTH_SHORT).show()
            return
        }

        mapView?.viewport?.idle()
        binding.btnRecenter.visibility = View.VISIBLE

        val route = navRoutes[0]
        val points = LineString.fromPolyline(route.directionsRoute.geometry()!!, 6).coordinates()

        val cameraOptions = mapView?.mapboxMap?.cameraForCoordinates(
            points,
            EdgeInsets(150.0, 80.0, 180.0, 80.0),
            0.0,
            0.0
        )

        cameraOptions?.let {
            mapView?.camera?.flyTo(
                it,
                MapAnimationOptions.mapAnimationOptions {
                    duration(1500)
                }
            )
            shouldFitCameraToRoute = false
        }
    }

    private fun setupNavigationLayers(style: Style) {
        if (!style.styleSourceExists(NAV_ROUTE_SOURCE_ID)) {
            style.addSource(geoJsonSource(NAV_ROUTE_SOURCE_ID))
        }
        if (!style.styleSourceExists(NAV_TRAVELED_SOURCE_ID)) {
            style.addSource(geoJsonSource(NAV_TRAVELED_SOURCE_ID))
        }

        if (!style.styleLayerExists(NAV_TRAVELED_LAYER_ID)) {
            style.addLayer(lineLayer(NAV_TRAVELED_LAYER_ID, NAV_TRAVELED_SOURCE_ID) {
                lineColor("#94A3B8")
                lineWidth(interpolate {
                    linear()
                    zoom()
                    stop(12.0, 5.0)
                    stop(18.0, 11.0)
                })
                lineOpacity(0.8)
                lineJoin(LineJoin.ROUND)
                lineCap(LineCap.ROUND)
            })
        }

        if (!style.styleLayerExists(NAV_ROUTE_CASING_LAYER_ID)) {
            style.addLayer(lineLayer(NAV_ROUTE_CASING_LAYER_ID, NAV_ROUTE_SOURCE_ID) {
                lineColor("#0D1B3E")
                lineWidth(interpolate {
                    linear()
                    zoom()
                    stop(12.0, 8.0)
                    stop(18.0, 16.0)
                })
                lineOpacity(1.0)
                lineJoin(LineJoin.ROUND)
                lineCap(LineCap.ROUND)
            })
        }

        if (!style.styleLayerExists(NAV_ROUTE_LAYER_ID)) {
            style.addLayer(lineLayer(NAV_ROUTE_LAYER_ID, NAV_ROUTE_SOURCE_ID) {
                lineColor("#007AFF")
                lineWidth(interpolate {
                    linear()
                    zoom()
                    stop(12.0, 5.0)
                    stop(18.0, 11.0)
                })
                lineOpacity(1.0)
                lineJoin(LineJoin.ROUND)
                lineCap(LineCap.ROUND)
            })
        }
    }

    private fun updateBottomSheetTheme(isDark: Boolean) {
        val sheet = binding.bottomSummaryCard
        val colorTextPrimary = if (isDark) Color.WHITE else Color.parseColor("#0F172A")
        val colorTextSecondary = if (isDark) Color.parseColor("#B0BEC5") else Color.parseColor("#64748B")
        val cardBg = if (isDark) Color.parseColor("#152039") else Color.WHITE
        val iconColor = if (isDark) Color.WHITE else Color.parseColor("#1E293B")
        val buttonBg = if (isDark) Color.parseColor("#1F2937") else Color.parseColor("#F1F5F9")

        sheet.findViewById<View>(R.id.bottomSheetContainer)?.setBackgroundResource(
            if (isDark) R.drawable.bg_bottom_sheet_dark else R.drawable.bg_bottom_sheet_dialog
        )

        val btnClose = sheet.findViewById<View>(R.id.btnCloseNav)
        val btnRoute = sheet.findViewById<View>(R.id.btnViewRoute)
        val outlineColor = if (isDark) Color.parseColor("#334155") else Color.parseColor("#CBD5E1")

        btnClose?.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_outline)
        btnRoute?.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_outline)

        btnClose?.backgroundTintList = ColorStateList.valueOf(outlineColor)
        btnRoute?.backgroundTintList = ColorStateList.valueOf(outlineColor)

        (sheet.findViewById<ViewGroup>(R.id.btnCloseNav)?.getChildAt(0) as? ImageView)?.imageTintList =
            ColorStateList.valueOf(if (isDark) Color.WHITE else Color.parseColor("#64748B"))
        (sheet.findViewById<ViewGroup>(R.id.btnViewRoute)?.getChildAt(0) as? ImageView)?.imageTintList =
            ColorStateList.valueOf(if (isDark) Color.WHITE else Color.parseColor("#2563EB"))

        sheet.findViewById<TextView>(R.id.tvBusIdSheet)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvRouteSheet)?.setTextColor(colorTextSecondary)
        sheet.findViewById<TextView>(R.id.tvDriverNameSheet)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvCurrentLocSheet)?.setTextColor(colorTextSecondary)
        sheet.findViewById<TextView>(R.id.tvUpcomingLabel)?.setTextColor(colorTextPrimary)

        sheet.findViewById<TextView>(R.id.tvEtaLabel)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvSpeedLabel)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvLoadLabel)?.setTextColor(colorTextPrimary)

        sheet.findViewById<TextView>(R.id.tvEtaSheet)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvSpeedSheet)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvLoadSheet)?.setTextColor(colorTextPrimary)

        stopsAdapter.setTheme(isDark)

        sheet.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardHeader)?.let { card ->
            card.setCardBackgroundColor(cardBg)
            card.strokeColor = if (isDark) Color.parseColor("#1F2937") else Color.parseColor("#F1F5F9")
        }
        sheet.findViewById<View>(R.id.dividerHeader)?.setBackgroundColor(if (isDark) Color.parseColor("#1F2937") else Color.parseColor("#F1F5F9"))
    }

    private fun showLiveTrackingDialog(switch: SwitchMaterial?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_driver_live_tracking)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnEnable = dialog.findViewById<MaterialButton>(R.id.btnEnableTracking)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelTracking)

        btnEnable?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                lastDutyToggleTime = System.currentTimeMillis()
                isDutyEnabled = true

                isUserTriggeredChange = false
                if (switch?.isChecked == false) {
                    switch.isChecked = true
                }
                isUserTriggeredChange = true

                updateDutyUI(true)

                viewModel.currentDriver.value?.driverId?.let { driverId ->
                    val routeName = assignedRoute?.routeName
                    com.example.bustrack_app.data.FirebaseRepository.updateDriverStatus(driverId, "Active", routeName)
                }

                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.END)

                dialog.dismiss()
            }, 200)
        }

        btnCancel?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                lastDutyToggleTime = System.currentTimeMillis()
                isUserTriggeredChange = false
                if (switch?.isChecked == true) {
                    switch.isChecked = false
                }
                isUserTriggeredChange = true

                isDutyEnabled = false
                updateDutyUI(false)

                viewModel.currentDriver.value?.driverId?.let { driverId ->
                    com.example.bustrack_app.data.FirebaseRepository.updateDriverStatus(driverId, "Inactive")
                }

                dialog.dismiss()
            }, 200)
        }

        dialog.show()
    }

    private fun updateDutyUI(isOnDuty: Boolean, reloadStyle: Boolean = true) {
        val drawerDutyLabel = findViewById<TextView>(R.id.tvDrawerDutyLabel)
        val dutySwitch = findViewById<SwitchMaterial>(R.id.switchDuty)

        cancelDutyAutoOffTimer()

        this.isDutyEnabled = isOnDuty

        isUserTriggeredChange = false
        if (dutySwitch?.isChecked != isOnDuty) {
            dutySwitch?.isChecked = isOnDuty
        }
        isUserTriggeredChange = true

        if (isOnDuty) {
            drawerDutyLabel?.text = "DUTY STATUS: ON"
            startLocationUpdates()
            setupLocationPuck()

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mapboxNavigation?.startTripSession()
            }

            viewModel.currentDriver.value?.driverId?.let { driverId ->
                com.example.bustrack_app.data.FirebaseRepository.updateDriverStatus(driverId, "Active")
            }
        } else {
            drawerDutyLabel?.text = "DUTY STATUS: OFF"

            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }

            mapView?.location?.enabled = false

            if (isNavigating) {
                setNavigationMode(false, reloadStyle)
            }

            mapboxNavigation?.stopTripSession()

            stopArrivalTimes.clear()
            nextGlobalStopIndex = 0
            attendancePromptedStops.clear()
            traveledHistoryPoints.clear()

            viewModel.currentDriver.value?.driverId?.let { driverId ->
                com.example.bustrack_app.data.FirebaseRepository.updateDriverStatus(driverId, "Inactive")
                com.example.bustrack_app.data.FirebaseRepository.updateDriverRouteGeometry(
                    driverId, null, null, 0, emptyMap(), false
                )
            }
        }
        updateNavigationButtonState()
    }

    private fun updateNavigationButtonState() {
        binding.btnStartNavigation.isEnabled = isDutyEnabled
        binding.btnStartNavigation.alpha = if (isDutyEnabled) 1.0f else 0.5f
        binding.btnStartNavigation.backgroundTintList = ColorStateList.valueOf(
            if (isDutyEnabled) Color.parseColor("#22C55E") else Color.GRAY
        )
    }

    private fun showStartPointError(route: RouteModel) {
        val currentPoint = currentLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }
        val startPoint = if (route.pathPoints.isNotEmpty()) {
            Point.fromLngLat(route.pathPoints[0].longitude, route.pathPoints[0].latitude)
        } else if (route.stopsList.isNotEmpty()) {
            Point.fromLngLat(route.stopsList[0].longitude, route.stopsList[0].latitude)
        } else null

        if (currentPoint != null && startPoint != null) {
            val results = FloatArray(1)
            android.location.Location.distanceBetween(
                currentPoint.latitude(), currentPoint.longitude(),
                startPoint.latitude(), startPoint.longitude(),
                results
            )
            val distanceKm = results[0] / 1000.0
            Toast.makeText(this, String.format(java.util.Locale.getDefault(), "You are %.2f km away from the starting point.", distanceKm), Toast.LENGTH_LONG).show()
        }

        val startName = if (route.pathPoints.isNotEmpty()) route.startPoint.ifEmpty { "Start Point" }
        else if (route.stopsList.isNotEmpty()) route.stopsList[0].stopName
        else "Start Point"
        showReachStartDialog(startName)
    }

    private fun showReachStartDialog(locationName: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_driver_live_tracking)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvDesc = dialog.findViewById<TextView>(R.id.tvDialogDescription)
        val btnOk = dialog.findViewById<MaterialButton>(R.id.btnEnableTracking)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelTracking)

        tvTitle?.text = "Reach Start Location"
        tvDesc?.text = "You are not at the starting point yet. Please reach '$locationName' to begin navigation."
        btnOk?.text = "Got it"
        btnCancel?.visibility = View.GONE

        btnOk?.setOnClickListener {
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
            if (!isUserTriggeredChange) return@setOnCheckedChangeListener

            lastDutyToggleTime = System.currentTimeMillis()

            if (isChecked && !isDutyEnabled) {
                showLiveTrackingDialog(dutySwitch)
            } else if (!isChecked && isDutyEnabled) {
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
                val intent = Intent(this, ChangePasswordActivity::class.java)
                intent.putExtra("FROM_USER", "driver")
                startActivity(intent)
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

        btnConfirm?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                dialog.dismiss()

                cancelDutyAutoOffTimer()
                if (isDutyEnabled) {
                    viewModel.currentDriver.value?.driverId?.let { driverId ->
                        com.example.bustrack_app.data.FirebaseRepository.updateDriverStatus(driverId, "Inactive")
                        com.example.bustrack_app.data.FirebaseRepository.updateDriverRouteGeometry(
                            driverId, null, null, 0, emptyMap(), false
                        )
                    }
                }

                FirebaseAuth.getInstance().signOut()
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }, 200)
        }

        btnCancel?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                dialog.dismiss()
            }, 200)
        }

        dialog.show()
    }

    private fun scheduleDutyAutoOffTimer() {
        if (!isDutyEnabled || isNavigating) return

        cancelDutyAutoOffTimer()

        val driverIdSnapshot = viewModel.currentDriver.value?.driverId ?: return

        val runnable = Runnable {
            Log.d("DutyDebug", "Grace period khatam - driver wapas nahi aya, auto Off Duty")
            com.example.bustrack_app.data.FirebaseRepository.updateDriverStatus(driverIdSnapshot, "Inactive")
            com.example.bustrack_app.data.FirebaseRepository.updateDriverRouteGeometry(
                driverIdSnapshot, null, null, 0, emptyMap(), false
            )
        }
        dutyAutoOffRunnable = runnable
        dutyHandler.postDelayed(runnable, DUTY_AUTO_OFF_GRACE_PERIOD_MS)
    }

    private fun cancelDutyAutoOffTimer() {
        dutyAutoOffRunnable?.let { dutyHandler.removeCallbacks(it) }
        dutyAutoOffRunnable = null
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
        cancelDutyAutoOffTimer()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
        scheduleDutyAutoOffTimer()
    }

    override fun onDestroy() {
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        mapView?.mapboxMap?.removeOnCameraChangeListener(cameraChangeListener)
        mapboxNavigation?.unregisterRoutesObserver(routesObserver)
        mapboxNavigation?.unregisterLocationObserver(locationObserver)
        mapboxNavigation?.unregisterRouteProgressObserver(routeProgressObserver)
        mapboxNavigation?.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)

        voiceInstructionsPlayer = null
        speechApi = null

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