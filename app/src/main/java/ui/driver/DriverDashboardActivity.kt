package ui.driver

import android.app.Dialog
import android.content.Intent
import android.content.res.Configuration
import android.content.res.ColorStateList
import android.app.PictureInPictureParams
import android.graphics.Rect
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Rational
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.DriverdashboardBinding
import com.example.bustrack_app.viewmodels.DriverDashboardViewModel
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.models.DriverModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.firebase.auth.FirebaseAuth
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.animation.easeTo
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
import com.mapbox.api.directions.v5.models.Bearing
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.geojson.LineString
import com.mapbox.geojson.MultiLineString
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import utils.ViewUtils
import utils.AddressDisplayFormatter
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
import android.media.AudioManager
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.os.Build
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.mapbox.navigation.voice.model.SpeechValue
import com.mapbox.bindgen.Expected
import android.text.SpannableString
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
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
import com.example.bustrack_app.models.ActiveTripState
import utils.TripRecoveryHelper
import com.example.bustrack_app.models.AlertOption
import com.example.bustrack_app.adapter.DriverAlertsAdapter
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.FirebaseRepository
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
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
import com.mapbox.navigation.voice.model.SpeechVolume
import com.mapbox.navigation.core.trip.session.VoiceInstructionsObserver
import com.mapbox.navigation.core.trip.session.OffRouteObserver
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.ModelLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.expressions.dsl.generated.*
import com.mapbox.maps.extension.style.layers.properties.generated.Visibility
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import kotlin.collections.firstOrNull
import com.mapbox.maps.plugin.ModelScaleMode
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.base.formatter.DistanceFormatter
import utils.TripPeriod
import utils.TripWindow
import utils.AttendanceStatus


enum class StopState { UPCOMING, ARRIVED, COMPLETED, SKIPPED }

class DriverDashboardActivity : AppCompatActivity() {
    private var notificationListeners = emptyList<com.google.firebase.firestore.ListenerRegistration>()

    private lateinit var binding: DriverdashboardBinding
    private lateinit var drawerLayout: DrawerLayout
    private var mapView: MapView? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private val viewModel: DriverDashboardViewModel by viewModels()
    private var isDutyEnabled = false
    private var isNearStart = false
    private var isNavigating = false
    private var navigationUiActive = false
    private var shouldFitCameraToRoute = true
    // A route can arrive from LiveData before Mapbox has finished loading its
    private var isMapStyleReady = false

    private var mapStyleLoadGeneration = 0L
    private var dashboardCameraFitPending = true
    private var hasFallbackCenteredMap = false
    private var currentRouteGeometry: String? = null
    private var traveledRouteGeometry: String? = null
    private var isVoiceEnabled = true

    private var pendingFallbackInstruction: String? = null
    private var fullNavigationPoints: List<Point> = emptyList()
    // Keep missed-stop checks tied to the full trip route.
    private var masterRouteChainagePoints: List<Point> = emptyList()
    private lateinit var stopsAdapter: com.example.bustrack_app.adapter.NavigationStopsAdapter
    private lateinit var bottomSheetBehavior: com.google.android.material.bottomsheet.BottomSheetBehavior<View>

    private var lastLegIndex = -1
    private var navStartIndex = 0
    private var nextGlobalStopIndex = 0
    private var currentNavPoints: List<Point> = emptyList()
    private var latestRouteProgress: com.mapbox.navigation.base.trip.model.RouteProgress? = null
    private val stopArrivalTimes = mutableMapOf<Int, String>()
    // Authoritative per-stop state. stopArrivalTimes stays purely for display text
    private val stopStates = mutableMapOf<Int, StopState>()

    private val stopEtaTexts = mutableMapOf<Int, String>()

    private var isReverseTripActive = false
    private var currentActiveTripId: String? = null
    private var currentRoundTripSessionId: String? = null
    private var hasCheckedActiveTripRecovery = false
    private var isViewingReverseTrip = false
    private var reverseTripCompleted = false
    private var reverseStops: List<com.example.bustrack_app.models.StopItem> = emptyList()
    private var reverseForwardStopIndexes: List<Int> = emptyList()
    // Marker colour belongs to the forward journey only. Freeze it at return start.
    private val forwardMarkerWasVisitedAtReturnStart = mutableMapOf<Int, Boolean>()
    private val reverseStopArrivalTimes = mutableMapOf<Int, String>()
    private val reverseStopStates = mutableMapOf<Int, StopState>()
    private val reverseStopEtaTexts = mutableMapOf<Int, String>()
    // Road-following traveled history: preserves distinct road-geometry segments.
    private val accumulatedTraveledSegments = mutableListOf<List<Point>>()
    private var activeTraveledSegment: List<Point> = emptyList()
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Authoritative navigation ETA directly calculated from navigation model state
    private var currentNavigationEtaText: String? = null

    // Cached load stat ("Present/Total" or "Remaining/Expected") updated on route load
    private var cachedLoadString: String = "0/0"

    private var lastGeocodeTime = 0L
    private var lastGeocodeLocation: Location? = null
    private var lastResolvedAddress: String? = null
    private var addressGeocodeGeneration = 0L
    private val GEOCODE_MIN_INTERVAL_MS = 15000L
    private val GEOCODE_MIN_DISTANCE_METERS = 50f


    private val mapboxLegByOriginalStopIndex = mutableMapOf<Int, Int>()

    private var activeStopStatus = "NEXT" // NEXT, ARRIVED, PASSED
    private var lastArrivedStopIndex = -1
    private var isCurrentlyAtStop = false
    private val ARRIVAL_RADIUS = 70.0 // meters
    private val RESUME_ROUTE_VALIDATION_RADIUS_METERS = 150.0

    private val ATTENDANCE_PROMPT_RADIUS = 140.0 // meters

    private val DEPARTURE_RADIUS = 85.0 // meters
    private var currentRawLocation: Location? = null
    private var latestMatchedLocationForStopDetection: Location? = null
    private var latestMatchedLocationTimeMs = 0L
    private var sourceArrivalRecordedForCurrentTrip = false

    private var departureCandidateIndex = -1
    private var departureConfirmCount = 0
    private var missedStopCandidateIndex = -1
    private var missedStopConfirmCount = 0
    private var missedStopFirstForwardProgressMeters = Double.NaN
    private var missedStopLastForwardProgressMeters = Double.NaN
    private var lastCountedMissedStopFixToken = Long.MIN_VALUE
    private var lastFullRouteGpsFixToken = Long.MIN_VALUE
    private var lastFullRouteGpsFixResult: GpsRoutePosition? = null
    private var lastFullRouteGpsPosition: GpsRoutePosition? = null
    private var lastFullRouteGpsLocation: Location? = null
    private val DEPARTURE_CONFIRM_THRESHOLD = 3
    private val MISSED_STOP_CONFIRM_THRESHOLD = 3
    private val MISSED_STOP_ROUTE_CLEARANCE_METERS = 60.0
    private val MISSED_STOP_CONFIRM_PROGRESS_METERS = 20.0
    private val MISSED_STOP_MAX_ROUTE_SNAP_METERS = 70.0
    private val CLOSE_STOP_DISTANCE_THRESHOLD_METERS = 50.0

    private val RETURN_STOP_PASSED_CONFIRM_METERS = 100.0
    private val RETURN_SOURCE_CONFIRM_FIXES = 3
    private var returnSourceConfirmCount = 0
    private var returnSourceConfirmFixToken = Long.MIN_VALUE
    private var dashboardFitLayoutRetries = 0
    private var hasDoneInitialDashboardFit = false
    private var arrivedStopRouteSegmentIndex: Int? = null

    private var lastSplitIndex = 0
    private val SPLIT_SEARCH_WINDOW = 120

    private val MIN_FORWARD_ROUTE_PROGRESS_METERS = 80.0

    private var isCameraFollowingBus = false
    private var lastCameraFollowLocation: Location? = null
    // A finger resting on a vibrating bus mount (or a tap with tiny drift) makes Mapbox fire
    private val USER_PAN_PAUSE_FOLLOW_DP = 24f
    private var panStartX = 0f
    private var panStartY = 0f
    private var panPausedFollow = false


    private val OFF_ROUTE_THRESHOLD_METERS = 35.0
    private val PARALLEL_ROAD_OFF_ROUTE_THRESHOLD_METERS = 4.0
    private val OPPOSITE_DIRECTION_REROUTE_DEGREES = 120.0
    private var isRerouteInFlight = false
    // True only for the window between a trip-direction start (startNavigationAnimation's
    private var isTripStartRouteRequestPending = false
    // A return request can be issued while a forward request/reroute is still in
    private var routeRequestGeneration = 0L
    private var lastOffRouteRerouteTimeMs = 0L
    private val MIN_OFFROUTE_REROUTE_GAP_MS = 3000L
    private var lastRerouteCompletedTimeMs = 0L
    private val REROUTE_SETTLE_GRACE_MS = 5000L
    private var voiceSessionId = 0L
    private var activeFallbackUtteranceId: String? = null
    private data class ReturnStopDeviation(val origin: Location, val distanceToStopMeters: Float)
    private val returnStopDeviations = mutableMapOf<Int, ReturnStopDeviation>()
    private val RETURN_SKIP_MIN_MOVEMENT_METERS = 60f
    private val RETURN_SKIP_DISTANCE_INCREASE_METERS = 35f

    private var isNorthUp = false
    private var isUserTriggeredChange = true
    private var lastDutyToggleTime = 0L
    private val DUTY_SYNC_DEBOUNCE_MS = 3000L

    private val dutyHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var dutyAutoOffRunnable: Runnable? = null


    // Keep the Driver's location puck on the same bounded scale curve as the
    private val MIN_BUS_MODEL_SCALE = 4.0f
    private val MAX_BUS_MODEL_SCALE = 5.0f
    private val BUS_MODEL_SCALE_REFERENCE_ZOOM = 17.0
    private val BUS_MODEL_SCALE_REFERENCE_VALUE = 1.0f
    private val BUS_MODEL_SCALE_COMPENSATION_FACTOR = 0.5
    private val MIN_ZOOM_FOR_BUS_SCALE = 13.0
    private val MAX_ZOOM_FOR_BUS_SCALE = 20.0
    private val LOCATION_MODEL_LAYER_ID = "mapbox-location-model-layer"
    private var lastAppliedBusScale = -1f
    private var busScaleUpdateGeneration = 0L
    private val busScaleHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var pendingBusScaleUpdate: Runnable? = null

    private val BUS_MODEL_ROLL_OFFSET_X_DEG = 0f
    private val BUS_MODEL_ROLL_OFFSET_Y_DEG = 0f
    private val DRIVER_RECENTER_ZOOM = 19.5
    private val DUTY_AUTO_OFF_GRACE_PERIOD_MS = 10 * 60 * 1000L

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var isCurrentLocationLive = false
    private var assignedRoute: RouteModel? = null
    // Locked during an active navigation session to protect route integrity from external LiveData changes
    private var lockedActiveRoute: RouteModel? = null
    private var lockedActiveBus: String? = null
    // Locked when a forward trip starts so a morning return after 11:00 is still
    private var activeTripIsMorning: Boolean? = null

    private var lastFirestoreLocation: Location? = null
    private var lastFirestoreUpdateTime = 0L
    // Each dashboard preview is asynchronous.  Only the newest request may redraw
    private var dashboardRoutePreviewGeneration = 0L
    private var lastDashboardPreviewRouteId: String? = null
    private var lastDashboardPreviewOrigin: Location? = null
    private val DASHBOARD_PREVIEW_MIN_MOVEMENT_METERS = 50f

    companion object {

        private const val FIRESTORE_UPDATE_INTERVAL = 1000L
        private const val FIRESTORE_MIN_DISTANCE = 2f
        private const val STATE_LAST_SPOKEN_INSTRUCTION = "last_spoken_instruction"
        private const val STATE_LAST_SPOKEN_TIME_MS = "last_spoken_time_ms"
    }
    private var locationCallback: LocationCallback? = null
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    private var mapboxNavigation: MapboxNavigation? = null
    private var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null
    private var speechApi: MapboxSpeechApi? = null
    private var fallbackTextToSpeech: TextToSpeech? = null
    private var isFallbackTtsReady = false
    private var audioFocusRequest: AudioFocusRequest? = null
    private var lastSpokenInstruction: String? = null
    private var lastSpokenTimeMs: Long = 0L
    private val MIN_VOICE_REPEAT_INTERVAL_MS = 10000L
    private val navigationLocationProvider = NavigationLocationProvider()
    private val maneuverApi by lazy {
        MapboxManeuverApi(DistanceFormatter { meters ->
            val label = if (meters < 1000) {
                "${meters.toInt()} m"
            } else {
                String.format(Locale.getDefault(), "%.1f km", meters / 1000.0)
            }
            SpannableString(label)
        })
    }
    private val attendancePromptedStops = mutableSetOf<Int>()
    private var lastValidBearing: Double = 0.0
    private val MIN_SPEED_FOR_BEARING_UPDATE = 0.8
    private var lastRawPositionForSnap: Point? = null
    // Keep this no greater than FIRESTORE_MIN_DISTANCE. Otherwise a new marker
    private val MIN_GPS_MOVEMENT_FOR_SNAP_METERS = 1.0

    private val NAV_ROUTE_SOURCE_ID = "nav-route-source"
    private val NAV_TRAVELED_SOURCE_ID = "nav-traveled-source"
    private val NAV_ROUTE_LAYER_ID = "nav-route-layer"
    private val NAV_ROUTE_CASING_LAYER_ID = "nav-route-casing-layer"
    private val NAV_TRAVELED_LAYER_ID = "nav-traveled-layer"

    // Visual puck uses Fused GPS only. Filter noise without waiting for a 6 m snap,
    private var lastPuckPosition: Location? = null
    private var lastPuckElapsedNanos = 0L
    private var puckTransitionDurationMs = 0L
    private var lastAcceptedLocationElapsedNanos = 0L
    private val MAX_ACCEPTABLE_PUCK_ACCURACY_METERS = 50f
    private val STATIONARY_HOLD_SPEED_MPS = 1.0f
    private val MIN_MOVING_PUCK_UPDATE_METERS = 0.5f
    private val MAX_PLAUSIBLE_PUCK_SPEED_MPS = 55.0
    private val STALE_CACHED_LOCATION_MAX_AGE_MS = 5000L
    private val MAX_CALLBACK_LOCATION_AGE_MS = 5000L

    enum class LocationReliabilityState {
        NORMAL_LIVE,
        GPS_UNAVAILABLE,
        GPS_ACCURACY_LOW,
        LOCATION_STALE,
        INTERNET_UNAVAILABLE_GPS_OK
    }

    private var currentReliabilityState: LocationReliabilityState = LocationReliabilityState.NORMAL_LIVE
    private var lastFreshLocationTimestamp: Long = 0L
    private val LOW_ACCURACY_THRESHOLD_METERS = 30f
    private val UNACCEPTABLE_ACCURACY_THRESHOLD_METERS = 65f
    private val STALE_LOCATION_TIMEOUT_MS = 10000L
    private val staleLocationHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val navigationLocationHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var staleLocationRunnable: Runnable? = null
    private var pendingNavigationStartRoute: RouteModel? = null
    private var pendingNavigationStartTimeout: Runnable? = null
    private var isInternetConnected: Boolean = true

    private val networkListener: (Boolean) -> Unit = { online ->
        isInternetConnected = online
        onNetworkStatusChanged(online)
    }

    private val locationSettingsLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            startLocationUpdates()
            pendingNavigationStartRoute?.let(::requestOneShotNavigationLocation)
        } else {
            clearPendingNavigationStart()
            updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
            Toast.makeText(this, "GPS must be enabled to use this app", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lastSpokenInstruction = savedInstanceState?.getString(STATE_LAST_SPOKEN_INSTRUCTION)
        lastSpokenTimeMs = savedInstanceState?.getLong(STATE_LAST_SPOKEN_TIME_MS) ?: 0L

        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = ContextCompat.getColor(this, R.color.primaryDark)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.white)
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = true
        }
        binding = DriverdashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        MapboxOptions.accessToken = getString(R.string.mapbox_access_token)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        drawerLayout = binding.drawerLayout
        mapView = binding.mapView
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when {
                    drawerLayout.isDrawerOpen(GravityCompat.END) -> drawerLayout.closeDrawer(GravityCompat.END)
                    isNavigating && !isInPictureInPictureMode -> enterNavigationPictureInPicture()
                    else -> {
                        isEnabled = false
                        try {
                            onBackPressedDispatcher.onBackPressed()
                        } finally {
                            isEnabled = true
                        }
                    }
                }
            }
        })
        isVoiceEnabled = getSharedPreferences("navigation_preferences", MODE_PRIVATE)
            .getBoolean("voice_enabled", true)

        setupStopsRecyclerView()

        val initialStyleGeneration = ++mapStyleLoadGeneration
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            if (initialStyleGeneration != mapStyleLoadGeneration || isDestroyed) return@loadStyle
            isMapStyleReady = true
            dashboardCameraFitPending = true
            hasFallbackCenteredMap = false
            mapView?.mapboxMap?.setBounds(
                CameraBoundsOptions.Builder()
                    .minZoom(3.0)
                    .maxZoom(20.0)
                    .build()
            )
            recreateAnnotationManagers()
            setupLocationPuck()
            setupMapGestures()
            updateMapDisplay()
            centerMapOnLiveLocationWhenRouteIsUnavailable()
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

        val alreadyNavigating = mapboxNavigation?.getNavigationRoutes()?.isNotEmpty() == true
        if (alreadyNavigating) {
            setNavigationMode(true, reloadStyle = true)
            startFollowingPuck()
        } else {
            // This is initial UI setup, not an intentional End Navigation action.
            setNavigationMode(false, reloadStyle = false, clearActiveTrip = false)
        }

        if (intent.getBooleanExtra("OPEN_DRAWER", false)) {
            drawerLayout.post {
                drawerLayout.openDrawer(GravityCompat.END)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1001) {
            if (pendingNavigationStartRoute != null && !hasPreciseLocationPermission()) {
                clearPendingNavigationStart()
                Toast.makeText(
                    this,
                    "Precise location permission is required for navigation. Enable it in app permissions, then try again.",
                    Toast.LENGTH_LONG
                ).show()
            } else if (hasLocationPermission()) {
                checkLocationSettings()
            } else {
                clearPendingNavigationStart()
                updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
                Toast.makeText(this, "Location permission is required to get your current location.", Toast.LENGTH_LONG).show()
            }
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

    private fun requestNavigationAudioFocus(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return false
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val playbackAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(playbackAttributes)
                    .setAcceptsDelayedFocusGain(false)
                    .setOnAudioFocusChangeListener { /* Navigation guidance handles ducking */ }
                    .build()
                audioFocusRequest = request
                am.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            } else {
                @Suppress("DEPRECATION")
                am.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            }
        } catch (e: Exception) {
            Log.e("VoiceNav", "Error requesting audio focus: ${e.message}", e)
            false
        }
    }

    private fun abandonNavigationAudioFocus() {
        val am = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                audioFocusRequest?.let {
                    am.abandonAudioFocusRequest(it)
                }
                audioFocusRequest = null
            } else {
                @Suppress("DEPRECATION")
                am.abandonAudioFocus(null)
            }
        } catch (e: Exception) {
            Log.e("VoiceNav", "Error abandoning audio focus: ${e.message}", e)
        }
    }

    private fun formatManeuverDistance(meters: Number?): String {
        val d = meters?.toDouble() ?: return ""
        if (d <= 0.0) return ""
        return if (d < 25.0) {
            "Now"
        } else if (d < 1000.0) {
            val rounded = (Math.round(d / 10.0) * 10).toInt()
            "$rounded m"
        } else {
            String.format(Locale.getDefault(), "%.1f km", d / 1000.0)
        }
    }

    private fun getManeuverIconRes(type: String?, modifier: String?): Int {
        val t = type?.lowercase(Locale.ROOT) ?: ""
        val m = modifier?.lowercase(Locale.ROOT) ?: ""

        if (t.contains("arrive") || m.contains("arrive")) return R.drawable.ic_nav_arrive
        if (t.contains("depart") || m.contains("depart")) return R.drawable.ic_nav_depart
        if (t.contains("u-turn") || t.contains("uturn") || m.contains("u-turn") || m.contains("uturn")) return R.drawable.ic_nav_uturn
        if (t.contains("roundabout") || t.contains("rotary")) return R.drawable.ic_nav_roundabout
        if (t.contains("fork")) return R.drawable.ic_nav_fork
        if (t.contains("merge")) return R.drawable.ic_nav_merge

        return when {
            m.contains("slight left") -> R.drawable.ic_nav_turn_slight_left
            m.contains("slight right") -> R.drawable.ic_nav_turn_slight_right
            m.contains("sharp left") -> R.drawable.ic_nav_turn_sharp_left
            m.contains("sharp right") -> R.drawable.ic_nav_turn_sharp_right
            m.contains("left") -> R.drawable.ic_nav_turn_left
            m.contains("right") -> R.drawable.ic_nav_turn_right
            m.contains("straight") || t.contains("continue") -> R.drawable.ic_nav_straight
            else -> R.drawable.ic_nav_straight
        }
    }


    private val navObserverBinder = object : MapboxNavigationObserver {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            this@DriverDashboardActivity.mapboxNavigation = mapboxNavigation
            mapboxNavigation.registerRoutesObserver(routesObserver)
            mapboxNavigation.registerLocationObserver(locationObserver)
            mapboxNavigation.registerRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.registerVoiceInstructionsObserver(voiceInstructionsObserver)
            mapboxNavigation.registerOffRouteObserver(offRouteObserver)
            Log.d("ETA_DEBUG", "MapboxNavigationObserver.onAttached - all observers registered on a live instance")
        }

        override fun onDetached(mapboxNavigation: MapboxNavigation) {
            mapboxNavigation.unregisterRoutesObserver(routesObserver)
            mapboxNavigation.unregisterLocationObserver(locationObserver)
            mapboxNavigation.unregisterRouteProgressObserver(routeProgressObserver)
            mapboxNavigation.unregisterVoiceInstructionsObserver(voiceInstructionsObserver)
            mapboxNavigation.unregisterOffRouteObserver(offRouteObserver)
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
        MapboxNavigationApp.registerObserver(navObserverBinder)
        mapboxNavigation = MapboxNavigationApp.current()

        // Ensure language matches between Mapbox Voice API and device locale
        val locale = java.util.Locale.getDefault().toLanguageTag()

        if (speechApi == null) {
            speechApi = MapboxSpeechApi(this, locale)
        }
        if (voiceInstructionsPlayer == null) {
            voiceInstructionsPlayer = MapboxVoiceInstructionsPlayer(this, locale).apply {
                volume(SpeechVolume(1.0f))
            }
        }
        if (fallbackTextToSpeech == null) {
            fallbackTextToSpeech = TextToSpeech(this) { status ->
                isFallbackTtsReady = status == TextToSpeech.SUCCESS
                if (isFallbackTtsReady) {
                    val defaultLocale = Locale.getDefault()
                    val langResult = fallbackTextToSpeech?.setLanguage(defaultLocale)
                    if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.w("VoiceNav", "Default locale $defaultLocale not supported for TTS, falling back to Locale.US")
                        fallbackTextToSpeech?.setLanguage(Locale.US)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        fallbackTextToSpeech?.setAudioAttributes(
                            AudioAttributes.Builder()
                                .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                                .build()
                        )
                    }
                    fallbackTextToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}
                        override fun onDone(utteranceId: String?) {
                            if (utteranceId == activeFallbackUtteranceId) abandonNavigationAudioFocus()
                        }
                        override fun onError(utteranceId: String?) {
                            if (utteranceId == activeFallbackUtteranceId) abandonNavigationAudioFocus()
                        }
                    })
                    pendingFallbackInstruction?.let { instruction ->
                        pendingFallbackInstruction = null
                        speakFallbackInstruction(instruction)
                    }
                } else {
                    Log.e("VoiceNav", "Failed to initialize Android TextToSpeech engine")
                }
            }
        }
    }

    private val voiceInstructionsObserver = VoiceInstructionsObserver { voiceInstructions ->
        if (!isVoiceEnabled || !isNavigating) return@VoiceInstructionsObserver
        val announcement = voiceInstructions.announcement()
        if (announcement.isNullOrBlank()) return@VoiceInstructionsObserver

        val now = System.currentTimeMillis()
        // Deduplication: do not repeat the exact same instruction within MIN_VOICE_REPEAT_INTERVAL_MS
        if (announcement.equals(lastSpokenInstruction, ignoreCase = true) && (now - lastSpokenTimeMs) < MIN_VOICE_REPEAT_INTERVAL_MS) {
            Log.d("VoiceNav", "Skipping duplicated voice instruction: $announcement")
            return@VoiceInstructionsObserver
        }
        lastSpokenInstruction = announcement
        lastSpokenTimeMs = now

        val currentSessionId = voiceSessionId
        Log.d("VoiceNav", "Triggering voice instruction: $announcement")
        val speech = speechApi
        if (speech != null) {
            speech.generate(voiceInstructions) { expected ->
                if (currentSessionId != voiceSessionId || !isNavigating || !isVoiceEnabled) {
                    Log.d("VoiceNav", "Voice instruction discarded due to session/navigation state change")
                    return@generate
                }
                expected.fold(
                    { error ->
                        Log.w("VoiceNav", "SpeechApi generation error: $error, falling back")
                        val fallback = error.fallback
                        if (fallback != null && voiceInstructionsPlayer != null) {
                            fallbackTextToSpeech?.stop()
                            requestNavigationAudioFocus()
                            voiceInstructionsPlayer?.clear()
                            voiceInstructionsPlayer?.play(fallback) { a ->
                                if (currentSessionId == voiceSessionId) abandonNavigationAudioFocus()
                                speechApi?.clean(a)
                            }
                        } else {
                            runOnUiThread {
                                if (currentSessionId == voiceSessionId && isNavigating && isVoiceEnabled) {
                                    voiceInstructionsPlayer?.clear()
                                    speakFallbackInstruction(announcement)
                                }
                            }
                        }
                    },
                    { value ->
                        fallbackTextToSpeech?.stop()
                        requestNavigationAudioFocus()
                        voiceInstructionsPlayer?.clear()
                        voiceInstructionsPlayer?.play(value.announcement) { a ->
                            if (currentSessionId == voiceSessionId) abandonNavigationAudioFocus()
                            speechApi?.clean(a)
                        }
                    }
                )
            }
        } else {
            voiceInstructionsPlayer?.clear()
            speakFallbackInstruction(announcement)
        }
    }

    private fun speakFallbackInstruction(instruction: String?) {
        if (!isVoiceEnabled || !isNavigating || instruction.isNullOrBlank()) return
        if (!isFallbackTtsReady) {
            pendingFallbackInstruction = instruction
            return
        }
        requestNavigationAudioFocus()
        val utteranceId = "nav_inst_${System.currentTimeMillis()}"
        activeFallbackUtteranceId = utteranceId
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val params = Bundle().apply {
                putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
            }
            fallbackTextToSpeech?.speak(instruction, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        } else {
            @Suppress("DEPRECATION")
            val params = HashMap<String, String>().apply {
                put(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC.toString())
                put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
            }
            @Suppress("DEPRECATION")
            fallbackTextToSpeech?.speak(instruction, TextToSpeech.QUEUE_FLUSH, params)
        }
    }

    private val offRouteObserver = OffRouteObserver { isOffRoute ->
        if (isOffRoute && isNavigating) {
            val now = System.currentTimeMillis()
            val isSettlingAfterReroute = now - lastRerouteCompletedTimeMs < REROUTE_SETTLE_GRACE_MS

            if (!isSettlingAfterReroute && !isRerouteInFlight &&
                now - lastOffRouteRerouteTimeMs > MIN_OFFROUTE_REROUTE_GAP_MS
            ) {
                lastOffRouteRerouteTimeMs = now
                runOnUiThread {
                    Log.d("NavDebug", "Driver is off-route. Triggering automatic reroute...")
                    triggerReroute()
                }
            }
        }
    }
    private fun clearTraveledRouteHistory() {
        accumulatedTraveledSegments.clear()
        activeTraveledSegment = emptyList()
        currentNavigationEtaText = null
    }

    /** Preserve a road-matched segment; never join a relocation gap with a straight chord. */
    private fun freezeActiveTraveledSegment() {
        val segment = activeTraveledSegment
        if (segment.size < 2) return
        val lastStored = accumulatedTraveledSegments.lastOrNull()
        if (lastStored != null && lastStored == segment) return
        accumulatedTraveledSegments.add(segment)
        activeTraveledSegment = emptyList()
    }

    private fun currentTraveledSegments(): List<List<Point>> =
        (accumulatedTraveledSegments + listOf(activeTraveledSegment)).filter { it.size >= 2 }

    private fun triggerReroute(force: Boolean = false) {
        val route = assignedRoute ?: return
        val nav = mapboxNavigation ?: return
        val loc = currentLocation ?: return

        // Keep return-trip progress tied to its own route.
        if (masterRouteChainagePoints.size < 2 && isTripStartRouteRequestPending) {
            Log.w("STOP_PROGRESS", "triggerReroute skipped: trip-start route response still " +
                    "in flight, refusing to seed masterRouteChainagePoints from stale data " +
                    "isReverseTripActive=$isReverseTripActive")
            return
        }
        ensureMasterRouteChainage(fullNavigationPoints)


        if (isRerouteInFlight && !force) return
        isRerouteInFlight = true
        val requestGeneration = ++routeRequestGeneration

        // Invalidate and stop the prior route's instruction immediately.
        voiceSessionId++
        speechApi?.cancel()
        voiceInstructionsPlayer?.clear()
        fallbackTextToSpeech?.stop()
        activeFallbackUtteranceId = null
        abandonNavigationAudioFocus()

        // A reroute must begin at the real current position. Projecting the origin
        val currentPoint = Point.fromLngLat(loc.longitude, loc.latitude)
        val navPoints = mutableListOf<Point>()
        navPoints.add(currentPoint)

        val maxVisitedIdx = highestVisitedStopIndex()
        var targetStopIndex = Math.max(nextGlobalStopIndex, maxVisitedIdx + 1)
        targetStopIndex = advanceToUpcomingStop(targetStopIndex)
        nextGlobalStopIndex = targetStopIndex

        val remainingStops = activeStops().mapIndexed { index, stop -> index to stop }
            .filter { (index, stop) -> index >= targetStopIndex && stateOf(index) == StopState.UPCOMING && stop.latitude != 0.0 && stop.longitude != 0.0 }

        mapboxLegByOriginalStopIndex.clear()
        remainingStops.forEachIndexed { mapboxStopIndex, (originalIndex, stop) ->
            mapboxLegByOriginalStopIndex[originalIndex] = mapboxStopIndex
            navPoints.add(Point.fromLngLat(stop.longitude, stop.latitude))
        }

        if (isReverseTripActive) {
            appendReturnSourceIfNeeded(navPoints)
        } else if (remainingStops.isEmpty()) {
            forwardTripDestinationPoint(route)?.let(navPoints::add)
        } else if (route.pathPoints.isNotEmpty()) {
            // Stops are the authoritative navigation waypoints when present.
        }

        if (navPoints.size < 2) {
            isRerouteInFlight = false
            return
        }

        Log.i("STOP_PROGRESS", "reroute origin=${loc.latitude},${loc.longitude} targetIndex=$targetStopIndex " +
                "target=${activeStops().getOrNull(targetStopIndex)?.stopName ?: "route-end"} " +
                "targetState=${stateOf(targetStopIndex)} remaining=${remainingStops.map { it.first to it.second.stopName }} force=$force")

        currentNavPoints = navPoints

        val routeOptionsBuilder = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(navPoints)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .steps(true)
            .bannerInstructions(true)
            .voiceInstructions(true)
            .language("en")
            .voiceUnits(DirectionsCriteria.METRIC)
            .alternatives(navPoints.size == 2)

        // A reroute happens most often around a turn. Do not constrain its origin
        nav.requestRoutes(
            routeOptionsBuilder.build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    runOnUiThread {
                        if (requestGeneration != routeRequestGeneration) return@runOnUiThread
                        val currentTarget = advanceToUpcomingStop(nextGlobalStopIndex)
                        if (targetStopIndex < activeStops().size &&
                            (targetStopIndex != currentTarget || stateOf(targetStopIndex) != StopState.UPCOMING)
                        ) {
                            isRerouteInFlight = false
                            Log.w("STOP_PROGRESS", "discard stale reroute target=$targetStopIndex current=$currentTarget")
                            triggerReroute(force = true)
                            return@runOnUiThread
                        }
                        isRerouteInFlight = false
                        lastRerouteCompletedTimeMs = System.currentTimeMillis()
                        if (routes.isEmpty()) return@runOnUiThread
                        val selectedRoute = shortestRoadRoute(routes)

                        navStartIndex = targetStopIndex
                        nextGlobalStopIndex = targetStopIndex
                        nav.setNavigationRoutes(listOf(selectedRoute))

                        // Immediately update with the newly calculated road geometry
                        val newCoords = selectedRoute.directionsRoute.geometry()?.let {
                            LineString.fromPolyline(it, 6).coordinates()
                        } ?: emptyList()

                        if (newCoords.isNotEmpty()) {
                            if (newCoords != fullNavigationPoints) {
                                freezeActiveTraveledSegment()
                            }
                            fullNavigationPoints = newCoords

                            lastSplitIndex = 0
                            lastRawPositionForSnap = null
                            updateStopEtasFromNavigationRoute(selectedRoute)

                            // Draw the new route on the map without waiting for next GPS tick
                            currentLocation?.let { currentLoc ->
                                updateNavigationRouteProgress(Point.fromLngLat(currentLoc.longitude, currentLoc.latitude))
                            }
                        }

                        Log.d("NavDebug", "Automatic reroute successful from current location to stop $navStartIndex")
                    }
                }
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    if (requestGeneration != routeRequestGeneration) return
                    if (!routeOptions.bearingsList().isNullOrEmpty()) {
                        Log.w("NavDebug", "Reroute with bearing failed, retrying without bearing constraints...")
                        val unconstrainedOptions = routeOptions.toBuilder().bearingsList(null).build()
                        nav.requestRoutes(unconstrainedOptions, this)
                        return
                    }
                    isRerouteInFlight = false
                    Log.e("NavDebug", "Reroute failed: ${reasons.firstOrNull()?.message}")
                }
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    if (requestGeneration != routeRequestGeneration) return
                    isRerouteInFlight = false
                }
            }
        )
    }

    /** Select the shortest returned road-following alternative, never a straight line. */
    private fun shortestRoadRoute(routes: List<NavigationRoute>): NavigationRoute =
        routes.minByOrNull { it.directionsRoute.distance() ?: Double.MAX_VALUE } ?: routes.first()

    //Reverse stops can already end at the route source.
    private fun appendReturnSourceIfNeeded(points: MutableList<Point>) {
        val source = originalRouteSource() ?: return
        val lastPoint = points.lastOrNull()
        if (lastPoint == null || TurfMeasurement.distance(lastPoint, source, TurfConstants.UNIT_METERS) > 25.0) {
            points.add(source)
        }
    }

    private fun forwardTripDestinationPoint(route: RouteModel): Point? {
        val lastStop = route.stopsList.lastOrNull()
        if (lastStop != null) {
            if (lastStop.latitude == 0.0 || lastStop.longitude == 0.0) return null
            return Point.fromLngLat(lastStop.longitude, lastStop.latitude)
        }
        return route.pathPoints.lastOrNull()
            ?.takeIf { it.latitude != 0.0 && it.longitude != 0.0 }
            ?.let { Point.fromLngLat(it.longitude, it.latitude) }
    }

    private val routesObserver = object : RoutesObserver {
        override fun onRoutesChanged(result: RoutesUpdatedResult) {
            val routes = result.navigationRoutes
            if (routes.isNotEmpty()) {
                val route = routes[0]
                val coords = route.directionsRoute.geometry()?.let {
                    LineString.fromPolyline(it, 6).coordinates()
                } ?: emptyList()

                runOnUiThread {
                    if (coords.isNotEmpty() && coords != fullNavigationPoints) {
                        freezeActiveTraveledSegment()
                        fullNavigationPoints = coords

                        lastSplitIndex = 0
                        lastRawPositionForSnap = null
                        updateStopEtasFromNavigationRoute(route)
                    }

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
            Log.d("ETA_DEBUG", "onNewLocationMatcherResult fired: lat=${rawEnhancedLocation.latitude}, lng=${rawEnhancedLocation.longitude}, speed=${rawEnhancedLocation.speed}")

            val enhancedLocation = rawEnhancedLocation.toBuilder()
                .bearing(lastValidBearing)
                .build()

            val androidLocation = android.location.Location("mapbox").apply {
                latitude = enhancedLocation.latitude
                longitude = enhancedLocation.longitude
                speed = enhancedLocation.speed?.toFloat() ?: 0f
                bearing = enhancedLocation.bearing?.toFloat() ?: 0f
            }

            val effectiveLocation = currentRawLocation?.let { raw ->
                val dist = FloatArray(1)
                Location.distanceBetween(raw.latitude, raw.longitude, enhancedLocation.latitude, enhancedLocation.longitude, dist)
                if (dist[0] > OFF_ROUTE_THRESHOLD_METERS) raw else androidLocation
            } ?: androidLocation

            val rawToMatchedMeters = currentRawLocation?.let { raw ->
                val result = FloatArray(1)
                Location.distanceBetween(raw.latitude, raw.longitude, androidLocation.latitude, androidLocation.longitude, result)
                result[0]
            } ?: 0f
            if (rawToMatchedMeters <= OFF_ROUTE_THRESHOLD_METERS) {
                latestMatchedLocationForStopDetection = Location(androidLocation)
                latestMatchedLocationTimeMs = System.currentTimeMillis()
            }

            runOnUiThread {
                // Never move the visual puck from Mapbox's matcher.
                if (isNavigating) {
                    val speedKph = (androidLocation.speed * 3.6).toInt()
                    binding.bottomSummaryCard.findViewById<TextView>(R.id.tvSpeedSheet)?.text = "$speedKph km/h"
                    binding.tvSpeedNav.text = "$speedKph"

                    reverseGeocodeIfNeeded(effectiveLocation)
                    // Route progress must be split using Navigation's road-matched
                    updateNavigationRouteProgress(
                        Point.fromLngLat(enhancedLocation.longitude, enhancedLocation.latitude)
                    )
                }
            }
        }
    }


    private fun updateStopEtasFromNavigationRoute(navigationRoute: NavigationRoute) {
        val stops = activeStops()
        if (stops.isEmpty()) return
        val legs = navigationRoute.directionsRoute.legs() ?: return
        var accumulatedSeconds = 0
        var nextStopRemainingSeconds: Int? = null

        stops.forEachIndexed { index, stop ->
            if (stateOf(index) != StopState.UPCOMING || index < nextGlobalStopIndex) return@forEachIndexed

            val legIndex = mapboxLegByOriginalStopIndex[index] ?: (index - navStartIndex)
            val legSeconds = legs.getOrNull(legIndex)?.duration()?.toInt() ?: return@forEachIndexed
            accumulatedSeconds += legSeconds
            if (index == nextGlobalStopIndex) {
                nextStopRemainingSeconds = accumulatedSeconds
            }
            val etaText = "ETA: ${timeFormat.format(Calendar.getInstance().apply {
                add(Calendar.SECOND, accumulatedSeconds)
            }.time)}"
            activeEtaTexts()[index] = etaText
            stop.time = etaText
        }

        val nextEta = activeEtaTexts()[nextGlobalStopIndex]
        if (nextEta != null) {

            currentNavigationEtaText = formatRemainingEta(nextStopRemainingSeconds ?: accumulatedSeconds)
            binding.bottomSummaryCard.findViewById<TextView>(R.id.tvEtaSheet)?.text = currentNavigationEtaText
            binding.tvEtaNav.text = "ETA: $currentNavigationEtaText"
        }
        updateUpcomingStopsUI()
        // Publish the freshly calculated per-stop map immediately.
        currentLocation?.let { syncTrackingDataToFirestore(it, force = true) }
    }

    private fun formatRemainingEta(seconds: Int): String {
        val minutes = maxOf(1, kotlin.math.ceil(seconds / 60.0).toInt())
        return "$minutes min"
    }

    private fun reverseGeocodeIfNeeded(location: Location) {
        val now = System.currentTimeMillis()
        val moved = lastGeocodeLocation?.distanceTo(location) ?: Float.MAX_VALUE
        if (now - lastGeocodeTime < GEOCODE_MIN_INTERVAL_MS || moved < GEOCODE_MIN_DISTANCE_METERS) return
        lastGeocodeTime = now
        lastGeocodeLocation = Location(location)
        val generation = ++addressGeocodeGeneration

        lifecycleScope.launch(Dispatchers.IO) {
            val address = try {
                Geocoder(this@DriverDashboardActivity, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()?.getAddressLine(0)
                    ?.replace(Regex("^[A-Z0-9]{4,8}\\+[A-Z0-9]{2,4}\\s*"), "")
                    ?.let(AddressDisplayFormatter::normalize)
            } catch (_: Exception) {
                null
            }
            if (!address.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    if (generation != addressGeocodeGeneration || isFinishing || isDestroyed) return@withContext
                    lastResolvedAddress = address
                    binding.bottomSummaryCard.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = address
                }
            }
        }
    }


    private fun activeStops(): List<com.example.bustrack_app.models.StopItem> =
        if (isReverseTripActive) reverseStops else assignedRoute?.stopsList.orEmpty()

    private fun displayedStops(): List<com.example.bustrack_app.models.StopItem> =
        if (isReverseTripActive && !isViewingReverseTrip) assignedRoute?.stopsList.orEmpty() else activeStops()

    private fun activeArrivalTimes(): MutableMap<Int, String> =
        if (isReverseTripActive) reverseStopArrivalTimes else stopArrivalTimes

    private fun activeEtaTexts(): MutableMap<Int, String> =
        if (isReverseTripActive) reverseStopEtaTexts else stopEtaTexts

    private fun activeStates(): MutableMap<Int, StopState> =
        if (isReverseTripActive) reverseStopStates else stopStates

    private fun stateOf(index: Int): StopState = activeStates()[index] ?: StopState.UPCOMING

    // Arrival timestamps labelled Skipped are terminal, but do not count as visited.
    private fun highestVisitedStopIndex(): Int = activeArrivalTimes()
        .filterValues { !it.equals("Skipped", ignoreCase = true) }
        .keys.maxOrNull() ?: -1


    private fun originalRouteSource(): Point? = assignedRoute?.pathPoints
        ?.firstOrNull { it.latitude != 0.0 && it.longitude != 0.0 }
        ?.let { Point.fromLngLat(it.longitude, it.latitude) }

    private fun currentTripPeriod(): TripPeriod = TripWindow.currentPeriod()

    private fun isMorningTrip(): Boolean = currentTripPeriod() == TripPeriod.MORNING

    private fun isActiveTripMorning(): Boolean = activeTripIsMorning ?: isMorningTrip()

    /** A recovery session is valid only during the TripWindow that started it. */
    private fun isRecoveryOutsideCurrentWindow(state: ActiveTripState? = null): Boolean {
        val tripIsMorning = state?.isMorning ?: activeTripIsMorning ?: return false
        val tripPeriod = if (tripIsMorning) TripPeriod.MORNING else TripPeriod.EVENING
        return currentTripPeriod() != tripPeriod
    }

    private fun attendanceDate(): String =
        SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    /** Stop attendance is a boarding action and belongs only to the forward AM trip. */
    private fun isMorningPickupTrip(): Boolean = !isReverseTripActive && isActiveTripMorning()

    private fun recordSourceArrivalIfNeeded(location: Location) {
        if (sourceArrivalRecordedForCurrentTrip || !isNavigating) return
        if (nextGlobalStopIndex < activeStops().size) return
        val source = originalRouteSource() ?: return
        val distance = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, source.latitude(), source.longitude(), distance)
        if (distance[0] > ARRIVAL_RADIUS) return
        sourceArrivalRecordedForCurrentTrip = true
        // Morning drop is the terminal event of the morning RETURN leg only.
        if (isActiveTripMorning() && isReverseTripActive) FirebaseRepository.updateDropTimesForRoute(
            assignedRoute?.routeName.orEmpty(), "", true,
            attendanceDate(),
            timeFormat.format(Calendar.getInstance().time)
        )
        if (isReverseTripActive) {
            reverseTripCompleted = true
            currentNavigationEtaText = "Route completed"
            mapboxNavigation?.setNavigationRoutes(emptyList())
            completeCurrentTrip()
        }

    }

    private fun recordEveningDropAtStop(stopName: String) {
        if (isActiveTripMorning()) return
        FirebaseRepository.updateDropTimesForRoute(
            assignedRoute?.routeName.orEmpty(), stopName, false,
            attendanceDate(),
            timeFormat.format(Calendar.getInstance().time)
        )
    }

    //UPCOMING -> ARRIVED. Records the arrival timestamp.
    private fun transitionToArrived(index: Int): Boolean {
        if (stateOf(index) != StopState.UPCOMING) return false
        activeStates()[index] = StopState.ARRIVED
        activeArrivalTimes()[index] = timeFormat.format(Calendar.getInstance().time)
        lastArrivedStopIndex = index
        isCurrentlyAtStop = true
        activeStopStatus = "ARRIVED"
        arrivedStopRouteSegmentIndex = currentLocation
            ?.takeIf { fullNavigationPoints.size >= 2 }
            ?.let { location ->
                projectOntoForwardRoute(
                    Point.fromLngLat(location.longitude, location.latitude),
                    1000.0
                )?.segmentIndex
            }
        refreshLoadStat()
        persistCurrentActiveTripState()
        return true
    }

    // ARRIVED -> COMPLETED. Advances nextGlobalStopIndex past this stop
    private fun transitionToCompleted(index: Int): Boolean {
        if (stateOf(index) != StopState.ARRIVED) return false
        activeStates()[index] = StopState.COMPLETED
        isCurrentlyAtStop = false
        activeStopStatus = "PASSED"
        lastArrivedStopIndex = -1
        arrivedStopRouteSegmentIndex = null
        nextGlobalStopIndex = index + 1
        refreshLoadStat()
        persistCurrentActiveTripState()
        return true
    }

    //UPCOMING -> SKIPPED.
    private fun transitionToSkipped(index: Int): Boolean {
        if (stateOf(index) != StopState.UPCOMING) return false
        activeStates()[index] = StopState.SKIPPED
        activeArrivalTimes()[index] = "Skipped"
        if (nextGlobalStopIndex <= index) nextGlobalStopIndex = index + 1
        missedStopCandidateIndex = -1
        missedStopConfirmCount = 0
        missedStopFirstForwardProgressMeters = Double.NaN
        missedStopLastForwardProgressMeters = Double.NaN
        nextGlobalStopIndex = advanceToUpcomingStop(nextGlobalStopIndex)
        Log.i("STOP_PROGRESS", "transitionToSkipped index=$index next=$nextGlobalStopIndex " +
                "nextState=${stateOf(nextGlobalStopIndex)}")
        persistCurrentActiveTripState()
        return true
    }

    private fun arrivalDistanceToStop(location: Location, stop: com.example.bustrack_app.models.StopItem): Float {
        val rawDistance = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, stop.latitude, stop.longitude, rawDistance)

        val matchedLocation = latestMatchedLocationForStopDetection
        if (matchedLocation == null || System.currentTimeMillis() - latestMatchedLocationTimeMs > MAX_CALLBACK_LOCATION_AGE_MS) {
            return rawDistance[0]
        }

        val matchedDistance = FloatArray(1)
        Location.distanceBetween(
            matchedLocation.latitude, matchedLocation.longitude,
            stop.latitude, stop.longitude,
            matchedDistance
        )
        return minOf(rawDistance[0], matchedDistance[0])
    }


    private fun advanceToUpcomingStop(startIndex: Int): Int {
        val stops = activeStops()
        var index = startIndex.coerceAtLeast(0)
        while (index < stops.size &&
            (stateOf(index) == StopState.SKIPPED || stateOf(index) == StopState.COMPLETED)
        ) index++
        return index
    }

    private fun checkGeofenceAndStopStatus(location: Location) {
        val stops = activeStops()
        recordSourceArrivalIfNeeded(location)
        if (stops.isEmpty()) return
        Log.d("STOP_DEBUG", "checkGeofenceAndStopStatus: rawGPS=${location.latitude},${location.longitude} " +
                "accuracy=${if (location.hasAccuracy()) location.accuracy else -1f} nextGlobalStopIndex=$nextGlobalStopIndex " +
                "lastArrivedStopIndex=$lastArrivedStopIndex isCurrentlyAtStop=$isCurrentlyAtStop " +
                "isReverseTripActive=$isReverseTripActive stateOfNext=${stateOf(nextGlobalStopIndex)} " +
                "localArrivalMap=${activeArrivalTimes()}")
        if (nextGlobalStopIndex >= stops.size) return


        if (isCurrentlyAtStop && lastArrivedStopIndex != -1 &&
            !attendancePromptedStops.contains(lastArrivedStopIndex)
        ) {
            stops.getOrNull(lastArrivedStopIndex)?.let { arrivedStop ->
                if (showAttendanceForStop(arrivedStop)) {
                    attendancePromptedStops.add(lastArrivedStopIndex)
                }
            }
        }

        // Attendance belongs to the forward flow
        if (isReverseTripActive) {
            detectSkippedUpcomingStop(location, stops)
            checkActiveTripGeofence(location, stops)
            return
        }

        // Detect a skipped stop from the bus's GPS chainage on the complete route,
        val missedIndex = nextGlobalStopIndex
        val accuracyOk = !location.hasAccuracy() || location.accuracy <= MAX_ACCEPTABLE_PUCK_ACCURACY_METERS
        if (!isCurrentlyAtStop && accuracyOk && fullNavigationPoints.size >= 2 &&
            missedIndex in stops.indices && missedIndex + 1 < stops.size &&
            stateOf(missedIndex) == StopState.UPCOMING
        ) {
            val missedStop = stops[missedIndex]
            val fixToken = gpsFixToken(location)
            val gpsProgress = if (fixToken != null) fullRoutePositionForGps(location, fixToken) else null
            val missedStopProgress = routeProgressForStopIndex(missedIndex)
            val busRoutePosition = gpsProgress?.position
            val clearedByDistance = busRoutePosition?.let { busPosition ->
                missedStopProgress?.let { stopPosition ->
                    busPosition.progressMeters >= stopPosition.progressMeters + MISSED_STOP_ROUTE_CLEARANCE_METERS
                }
            } == true
            val enteredNextStopApproach = hasEnteredCloseNextStopApproach(
                location, stops, missedIndex, busRoutePosition, missedStopProgress
            )
            val clearlyPastStop = clearedByDistance || enteredNextStopApproach

            Log.d("STOP_PROGRESS", "gpsRouteProgress fix=$fixToken newFix=${gpsProgress?.isNewFix} " +
                    "masterChainagePoints=${masterRouteChainagePoints.size} " +
                    "progress=${gpsProgress?.position?.progressMeters}m segment=${gpsProgress?.position?.segmentIndex} " +
                    "crossTrack=${gpsProgress?.position?.crossTrackMeters}m stopIndex=$missedIndex " +
                    "stopState=${stateOf(missedIndex)} stopRouteProgress=${missedStopProgress?.progressMeters}m " +
                    "clearance=${MISSED_STOP_ROUTE_CLEARANCE_METERS}m closeApproach=$enteredNextStopApproach " +
                    "past=$clearlyPastStop next=$nextGlobalStopIndex")

            if (clearlyPastStop && gpsProgress?.isNewFix == true && fixToken != null &&
                fixToken != lastCountedMissedStopFixToken
            ) {
                val progressMeters = busRoutePosition!!.progressMeters
                if (missedStopCandidateIndex != missedIndex || missedStopFirstForwardProgressMeters.isNaN()) {
                    missedStopCandidateIndex = missedIndex
                    missedStopConfirmCount = 1
                    missedStopFirstForwardProgressMeters = progressMeters
                    missedStopLastForwardProgressMeters = progressMeters
                } else if (progressMeters < missedStopLastForwardProgressMeters - 8.0) {

                    missedStopCandidateIndex = -1
                    missedStopConfirmCount = 0
                    missedStopFirstForwardProgressMeters = Double.NaN
                    missedStopLastForwardProgressMeters = Double.NaN
                } else {
                    missedStopConfirmCount++
                    missedStopLastForwardProgressMeters = maxOf(missedStopLastForwardProgressMeters, progressMeters)
                }
                lastCountedMissedStopFixToken = fixToken

                val confirmedForwardMeters = if (missedStopFirstForwardProgressMeters.isNaN()) 0.0
                else missedStopLastForwardProgressMeters - missedStopFirstForwardProgressMeters
                Log.d("STOP_PROGRESS", "skipCandidate index=$missedIndex state=${stateOf(missedIndex)} " +
                        "count=$missedStopConfirmCount/$MISSED_STOP_CONFIRM_THRESHOLD " +
                        "forwardMeters=${"%.1f".format(Locale.US, confirmedForwardMeters)} " +
                        "routeProgress=${progressMeters}m")

                if (missedStopConfirmCount >= MISSED_STOP_CONFIRM_THRESHOLD &&
                    confirmedForwardMeters >= MISSED_STOP_CONFIRM_PROGRESS_METERS &&
                    transitionToSkipped(missedIndex)
                ) {
                    Log.w("STOP_PROGRESS", "STOP_SKIPPED index=$missedIndex name=${missedStop.stopName} " +
                            "routeProgress=${progressMeters}m stopProgress=${missedStopProgress?.progressMeters}m " +
                            "nextGlobalStopIndex=$nextGlobalStopIndex nextState=${stateOf(nextGlobalStopIndex)}")
                    updateUpcomingStopsUI()
                    triggerReroute(force = true)
                }
            } else if (!clearlyPastStop || gpsProgress?.position == null || !accuracyOk) {
                missedStopCandidateIndex = -1
                missedStopConfirmCount = 0
                missedStopFirstForwardProgressMeters = Double.NaN
                missedStopLastForwardProgressMeters = Double.NaN
            }
        } else if (!isCurrentlyAtStop) {
            missedStopCandidateIndex = -1
            missedStopConfirmCount = 0
            missedStopFirstForwardProgressMeters = Double.NaN
            missedStopLastForwardProgressMeters = Double.NaN
        }

        // Open attendance as the bus approaches its next stop
        if (!isCurrentlyAtStop && !attendancePromptedStops.contains(nextGlobalStopIndex)) {
            stops.getOrNull(nextGlobalStopIndex)?.let { nextStop ->
                val distance = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    nextStop.latitude, nextStop.longitude,
                    distance
                )
                if (distance[0] <= ATTENDANCE_PROMPT_RADIUS && showAttendanceForStop(nextStop)) {
                    attendancePromptedStops.add(nextGlobalStopIndex)
                }
            }
        }

        // 1. Entering Geofence: UPCOMING -> ARRIVED.
        if (!isCurrentlyAtStop) {
            val candidateIndex = nextGlobalStopIndex
            if (stateOf(candidateIndex) == StopState.UPCOMING) {
                val candidateStop = stops[candidateIndex]
                val arrivalDistance = arrivalDistanceToStop(location, candidateStop)

                Log.d("STOP_DEBUG", "arrivalCheck: targetStop=${candidateStop.stopName} candidateIndex=$candidateIndex " +
                        "distance=$arrivalDistance geofenceEntered=${arrivalDistance <= ARRIVAL_RADIUS} routeProgressValid=n/a")
                if (arrivalDistance <= ARRIVAL_RADIUS) {
                    if (transitionToArrived(candidateIndex)) {
                        recordEveningDropAtStop(candidateStop.stopName)
                        // Trigger stop arrival notification to parents
                        val isMorning = isActiveTripMorning()
                        val period = if (isMorning) "MORNING" else "EVENING"
                        val tripId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().time) + "_$period"

                        FirebaseRepository.notifyParentsOfStopArrival(
                            assignedRoute?.routeName ?: "",
                            candidateStop.stopName,
                            tripId
                        )

                        if (!attendancePromptedStops.contains(candidateIndex)) {
                            if (showAttendanceForStop(candidateStop)) {
                                attendancePromptedStops.add(candidateIndex)
                            }
                        }

                        updateUpcomingStopsUI()
                        drawPointsOnMap(fullNavigationPoints)
                    }
                }
            }
        }


        if (isCurrentlyAtStop && lastArrivedStopIndex != -1) {
            val arrivedIndex = lastArrivedStopIndex
            val currentStop = stops.getOrNull(arrivedIndex)
            if (currentStop != null) {
                val departResults = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    currentStop.latitude, currentStop.longitude,
                    departResults
                )
                // A stop is passed only after leaving its geofence
                val currentSegment = projectOntoForwardRoute(
                    Point.fromLngLat(location.longitude, location.latitude),
                    1000.0
                )?.segmentIndex

                if (arrivedStopRouteSegmentIndex == null && departResults[0] <= DEPARTURE_RADIUS) {
                    arrivedStopRouteSegmentIndex = currentSegment
                }

                val hasAdvancedForward = when {
                    arrivedStopRouteSegmentIndex != null && currentSegment != null -> {
                        currentSegment > arrivedStopRouteSegmentIndex!! || departResults[0] > (DEPARTURE_RADIUS * 2.0)
                    }
                    departResults[0] > DEPARTURE_RADIUS -> true
                    else -> false
                }
                Log.d("STOP_DEBUG", "departureCheck: arrivedIndex=$arrivedIndex targetStop=${currentStop.stopName} " +
                        "distance=${departResults[0]} departureExitConditionMet=${departResults[0] > DEPARTURE_RADIUS} " +
                        "hasAdvancedForward=$hasAdvancedForward departureConfirmedCount=$departureConfirmCount " +
                        "stateOfArrived=${stateOf(arrivedIndex)}")
                if (departResults[0] > DEPARTURE_RADIUS && hasAdvancedForward) {
                    if (departureCandidateIndex == arrivedIndex) {
                        departureConfirmCount++
                    } else {
                        departureCandidateIndex = arrivedIndex
                        departureConfirmCount = 1
                    }

                    if (departureConfirmCount >= DEPARTURE_CONFIRM_THRESHOLD) {
                        // Successfully exited geofence: finalize this stop as COMPLETED.
                        if (transitionToCompleted(arrivedIndex)) {

                            // Destroy active attendance context when leaving geofence (Section 2 Requirement)
                            (supportFragmentManager.findFragmentByTag("AttendanceSheet") as? com.google.android.material.bottomsheet.BottomSheetDialogFragment)?.dismissAllowingStateLoss()

                            if (nextGlobalStopIndex < stops.size) {
                                triggerReroute()
                            }

                            departureCandidateIndex = -1
                            departureConfirmCount = 0

                            updateUpcomingStopsUI()
                            drawPointsOnMap(fullNavigationPoints)
                        }
                    }
                } else {
                    departureCandidateIndex = -1
                    departureConfirmCount = 0
                }
            }
        }
    }

    /**
     * Same GPS/geofence progression as the forward trip, without touching forward state.
     *
     * FIX: previously this lacked the two safety fallbacks the forward-trip block above
     * has: (1) a lazy re-init of arrivedStopRouteSegmentIndex if it wasn't captured at
     * the moment of arrival, and (2) a plain distance-based escape hatch in
     * hasAdvancedForward for when the segment index is null or never advances. Without
     * these, a return-trip stop that entered ARRIVED (bus merely drove within
     * ARRIVAL_RADIUS without actually stopping) could get permanently stuck ARRIVED,
     * which also permanently blocks detectSkippedUpcomingStop() (guarded on
     * isCurrentlyAtStop) for the rest of the return trip - nextGlobalStopIndex could
     * then never advance, matching the reported "keeps re-targeting a passed stop" bug.
     */
    private fun checkActiveTripGeofence(location: Location, stops: List<com.example.bustrack_app.models.StopItem>) {
        recordSourceArrivalIfNeeded(location)
        if (nextGlobalStopIndex >= stops.size) return
        if (!isCurrentlyAtStop) {
            val index = nextGlobalStopIndex
            if (stateOf(index) == StopState.UPCOMING) {
                val arrivalDistance = arrivalDistanceToStop(location, stops[index])
                if (arrivalDistance <= ARRIVAL_RADIUS) {
                    if (transitionToArrived(index)) {
                        updateUpcomingStopsUI()
                        drawPointsOnMap(fullNavigationPoints)
                    }
                }
            }
        }
        if (isCurrentlyAtStop && lastArrivedStopIndex != -1) {
            val arrived = lastArrivedStopIndex
            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, stops[arrived].latitude, stops[arrived].longitude, distance)

            val currentSegment = projectOntoForwardRoute(
                Point.fromLngLat(location.longitude, location.latitude),
                1000.0
            )?.segmentIndex

            if (arrivedStopRouteSegmentIndex == null && distance[0] <= DEPARTURE_RADIUS) {
                arrivedStopRouteSegmentIndex = currentSegment
            }

            val hasAdvancedForward = when {
                arrivedStopRouteSegmentIndex != null && currentSegment != null -> {
                    currentSegment > arrivedStopRouteSegmentIndex!! || distance[0] > (DEPARTURE_RADIUS * 2.0)
                }
                distance[0] > DEPARTURE_RADIUS -> true
                else -> false
            }
            if (distance[0] > DEPARTURE_RADIUS && hasAdvancedForward) {
                departureConfirmCount = if (departureCandidateIndex == arrived) departureConfirmCount + 1 else 1
                departureCandidateIndex = arrived
                if (departureConfirmCount >= DEPARTURE_CONFIRM_THRESHOLD && transitionToCompleted(arrived)) {
                    departureCandidateIndex = -1
                    departureConfirmCount = 0
                    if (nextGlobalStopIndex < stops.size || originalRouteSource() != null) triggerReroute()
                    else {
                        reverseTripCompleted = true
                        currentNavigationEtaText = "Route completed"
                        mapboxNavigation?.setNavigationRoutes(emptyList())
                        completeCurrentTrip()
                    }
                    updateUpcomingStopsUI()
                    drawPointsOnMap(fullNavigationPoints)
                }
            } else {
                departureCandidateIndex = -1
                departureConfirmCount = 0
            }
        }
    }

    /** Apply the same route-chainage skip rule to the independently tracked return trip. */
    private fun detectSkippedUpcomingStop(
        location: Location,
        stops: List<com.example.bustrack_app.models.StopItem>
    ) {
        val missedIndex = nextGlobalStopIndex
        val accuracyOk = !location.hasAccuracy() || location.accuracy <= MAX_ACCEPTABLE_PUCK_ACCURACY_METERS
        if (!isCurrentlyAtStop && accuracyOk) completeReturnAtSourceIfNeeded(location, stops)
        if (isCurrentlyAtStop || !accuracyOk || masterRouteChainagePoints.size < 2 ||
            missedIndex !in stops.indices || nextGlobalStopIndex != missedIndex ||
            stateOf(missedIndex) != StopState.UPCOMING
        ) {
            if (!isCurrentlyAtStop) resetMissedStopCandidate()
            return
        }

        val stop = stops[missedIndex]
        val fixToken = gpsFixToken(location)
        val gpsProgress = fixToken?.let { fullRoutePositionForGps(location, it) }
        val stopProgress = routeProgressForStopIndex(missedIndex)
        val busRoutePosition = gpsProgress?.position
        val clearedByDistance = busRoutePosition?.let { busPosition ->
            stopProgress?.let { stopPosition ->
                busPosition.progressMeters >= stopPosition.progressMeters + MISSED_STOP_ROUTE_CLEARANCE_METERS
            }
        } == true
        val enteredNextStopApproach = hasEnteredCloseNextStopApproach(
            location, stops, missedIndex, busRoutePosition, stopProgress
        )
        val clearlyPast = clearedByDistance || enteredNextStopApproach
        Log.d("STOP_PROGRESS", "returnGpsRouteProgress fix=$fixToken newFix=${gpsProgress?.isNewFix} " +
                "progress=${busRoutePosition?.progressMeters}m segment=${busRoutePosition?.segmentIndex} " +
                "crossTrack=${busRoutePosition?.crossTrackMeters}m stopIndex=$missedIndex " +
                "stopState=${stateOf(missedIndex)} stopRouteProgress=${stopProgress?.progressMeters}m " +
                "clearance=${MISSED_STOP_ROUTE_CLEARANCE_METERS}m closeApproach=$enteredNextStopApproach " +
                "past=$clearlyPast next=$nextGlobalStopIndex")

        if (!clearlyPast || gpsProgress?.isNewFix != true || fixToken == null ||
            fixToken == lastCountedMissedStopFixToken
        ) {
            if (!clearlyPast || busRoutePosition == null) resetMissedStopCandidate()
            return
        }

        val progressMeters = busRoutePosition?.progressMeters ?: return
        if (missedStopCandidateIndex != missedIndex || missedStopFirstForwardProgressMeters.isNaN()) {
            missedStopCandidateIndex = missedIndex
            missedStopConfirmCount = 1
            missedStopFirstForwardProgressMeters = progressMeters
            missedStopLastForwardProgressMeters = progressMeters
        } else if (progressMeters < missedStopLastForwardProgressMeters - 8.0) {
            resetMissedStopCandidate()
        } else {
            missedStopConfirmCount++
            missedStopLastForwardProgressMeters = maxOf(missedStopLastForwardProgressMeters, progressMeters)
        }
        lastCountedMissedStopFixToken = fixToken

        val confirmedForwardMeters = if (missedStopFirstForwardProgressMeters.isNaN()) 0.0
        else missedStopLastForwardProgressMeters - missedStopFirstForwardProgressMeters
        Log.d("STOP_PROGRESS", "returnSkipCandidate index=$missedIndex state=${stateOf(missedIndex)} " +
                "count=$missedStopConfirmCount/$MISSED_STOP_CONFIRM_THRESHOLD " +
                "forwardMeters=${"%.1f".format(Locale.US, confirmedForwardMeters)} routeProgress=${progressMeters}m")

        // Keep return-trip progress tied to its own route.
        val metersPastStop = progressMeters - (stopProgress?.progressMeters ?: progressMeters)
        if (missedStopConfirmCount >= MISSED_STOP_CONFIRM_THRESHOLD &&
            (confirmedForwardMeters >= MISSED_STOP_CONFIRM_PROGRESS_METERS ||
                    metersPastStop >= RETURN_STOP_PASSED_CONFIRM_METERS) &&
            transitionToSkipped(missedIndex)
        ) {
            Log.w("STOP_PROGRESS", "RETURN_STOP_SKIPPED index=$missedIndex name=${stop.stopName} " +
                    "routeProgress=${progressMeters}m stopProgress=${stopProgress?.progressMeters}m " +
                    "nextGlobalStopIndex=$nextGlobalStopIndex nextState=${stateOf(nextGlobalStopIndex)}")
            updateUpcomingStopsUI()
            triggerReroute(force = true)
        }
    }

    private fun hasEnteredCloseNextStopApproach(
        location: Location,
        stops: List<com.example.bustrack_app.models.StopItem>,
        missedIndex: Int,
        busRoutePosition: GpsRoutePosition?,
        missedStopProgress: GpsRoutePosition?
    ): Boolean {
        val nextIndex = missedIndex + 1
        if (missedIndex !in stops.indices || nextIndex !in stops.indices) return false
        val currentRoutePosition = busRoutePosition ?: return false
        val currentStopProgress = missedStopProgress ?: return false

        val currentStop = stops[missedIndex]
        val nextStop = stops[nextIndex]
        if (currentStop.latitude == 0.0 || currentStop.longitude == 0.0 ||
            nextStop.latitude == 0.0 || nextStop.longitude == 0.0
        ) return false

        val stopGap = FloatArray(1)
        Location.distanceBetween(
            currentStop.latitude, currentStop.longitude,
            nextStop.latitude, nextStop.longitude,
            stopGap
        )
        if (stopGap[0] > CLOSE_STOP_DISTANCE_THRESHOLD_METERS) return false

        val nextStopProgress = routeProgressForStopIndex(nextIndex) ?: return false
        if (nextStopProgress.progressMeters <= currentStopProgress.progressMeters) return false
        val hasMovedPastCurrentStop = currentRoutePosition.progressMeters > currentStopProgress.progressMeters
        val hasReachedNextStopApproach = currentRoutePosition.progressMeters >=
                nextStopProgress.progressMeters - ARRIVAL_RADIUS
        val distanceToNextStop = FloatArray(1)
        Location.distanceBetween(
            location.latitude, location.longitude,
            nextStop.latitude, nextStop.longitude,
            distanceToNextStop
        )

        return hasMovedPastCurrentStop && hasReachedNextStopApproach &&
                distanceToNextStop[0] <= ARRIVAL_RADIUS
    }



    private fun completeReturnAtSourceIfNeeded(
        location: Location,
        stops: List<com.example.bustrack_app.models.StopItem>
    ) {
        if (!isReverseTripActive || nextGlobalStopIndex !in stops.indices) {
            returnSourceConfirmCount = 0
            return
        }
        val source = originalRouteSource()
        if (source == null || masterRouteChainagePoints.size < 2) return
        val distance = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, source.latitude(), source.longitude(), distance)
        if (distance[0] > ARRIVAL_RADIUS) {
            returnSourceConfirmCount = 0
            return
        }

        val pendingIndex = nextGlobalStopIndex
        val pending = stops[pendingIndex]
        val toPending = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, pending.latitude, pending.longitude, toPending)
        if (toPending[0] <= ARRIVAL_RADIUS) {
            returnSourceConfirmCount = 0
            return
        }
        val token = gpsFixToken(location) ?: return
        if (token == returnSourceConfirmFixToken) return
        returnSourceConfirmFixToken = token
        returnSourceConfirmCount++
        if (returnSourceConfirmCount < RETURN_SOURCE_CONFIRM_FIXES) return
        returnSourceConfirmCount = 0
        var changed = false
        for (i in nextGlobalStopIndex until stops.size) {
            if (stateOf(i) == StopState.UPCOMING) {
                activeStates()[i] = StopState.SKIPPED
                activeArrivalTimes()[i] = "Skipped"
                changed = true
            }
        }
        nextGlobalStopIndex = advanceToUpcomingStop(nextGlobalStopIndex)
        if (changed) {
            Log.w("STOP_PROGRESS", "RETURN_SKIPPED_REMAINING_AT_SOURCE next=$nextGlobalStopIndex")
            persistCurrentActiveTripState()
            updateUpcomingStopsUI()
            recordSourceArrivalIfNeeded(location)
        }
    }

    private fun resetMissedStopCandidate() {
        missedStopCandidateIndex = -1
        missedStopConfirmCount = 0
        missedStopFirstForwardProgressMeters = Double.NaN
        missedStopLastForwardProgressMeters = Double.NaN
    }

    //Displays the attendance sheet for the specified stop while validating active trip,

    private fun showAttendanceForStop(stop: com.example.bustrack_app.models.StopItem): Boolean {
        if (!isMorningPickupTrip()) return false
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) return false
        if (supportFragmentManager.findFragmentByTag("AttendanceSheet") != null) return false

        val route = assignedRoute
        if (route == null || stop.stopName.isBlank()) {
            Toast.makeText(this, "Unable to mark attendance. No active trip or stop found.", Toast.LENGTH_SHORT).show()
            return false
        }

        val today = attendanceDate().replace("/", "-")
        val period = if (isActiveTripMorning()) "MORNING" else "EVENING"
        val tripId = currentActiveTripId.takeUnless { it.isNullOrEmpty() } ?: "${today}_${period}_${route.id}"
        val driver = viewModel.currentDriver.value

        AttendanceBottomSheet
            .newInstance(
                stopName = stop.stopName,
                routeName = route.routeName,
                isMorning = isActiveTripMorning(),
                stopId = stop.id.ifEmpty { stop.stopName },
                routeId = route.id,
                tripId = tripId,
                tripDirection = if (isReverseTripActive) "RETURN" else "FORWARD",
                busId = driver?.assignedBus.orEmpty(),
                driverId = driver?.driverId.orEmpty(),
                driverName = driver?.name.orEmpty()
            )
            .show(supportFragmentManager, "AttendanceSheet")
        return true
    }

    private fun updateUpcomingStopsUI() {
        val stops = displayedStops()


        // The adapter receives the actively occupied stop for both trip
        val liveArrivedIndex = if (isCurrentlyAtStop && lastArrivedStopIndex != -1) {
            lastArrivedStopIndex
        } else {
            -1
        }

        val stopNumbers = if (isReverseTripActive && isViewingReverseTrip) {
            // Reverse rows retain the identity of their forward stops: 4, 3, 2, 1.
            reverseForwardStopIndexes.map { it + 1 }
        } else {
            emptyList()
        }
        stopsAdapter.updateStops(stops, liveArrivedIndex, displayNumbers = stopNumbers)
    }

    private val routeProgressObserver = object : RouteProgressObserver {
        override fun onRouteProgressChanged(routeProgress: com.mapbox.navigation.base.trip.model.RouteProgress) {
            latestRouteProgress = routeProgress
            Log.d("ETA_DEBUG", "onRouteProgressChanged fired: currentLegProgress=${routeProgress.currentLegProgress != null}, durationRemaining=${routeProgress.currentLegProgress?.durationRemaining}, legIndex=${routeProgress.currentLegProgress?.legIndex}, nextGlobalStopIndex=$nextGlobalStopIndex, navStartIndex=$navStartIndex")
            runOnUiThread {
                // Section 1: Recalculate ETA and distance ONLY for the next valid UPCOMING stop (current leg)
                val distanceRemaining = (routeProgress.currentLegProgress?.distanceRemaining?.toDouble() ?: 0.0) / 1000.0
                val durationRemaining = (routeProgress.currentLegProgress?.durationRemaining?.toDouble() ?: 0.0) / 60.0

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

                tvLoad?.text = cachedLoadString

                val stops = activeStops()

                currentLocation?.let { loc ->
                    checkGeofenceAndStopStatus(loc)
                }

                val liveArrivedIndex = if (isCurrentlyAtStop && lastArrivedStopIndex != -1) {
                    lastArrivedStopIndex
                } else {
                    -1
                }
                val displayStopIndex = if (liveArrivedIndex != -1) liveArrivedIndex else nextGlobalStopIndex

                val arrivalTimes = activeArrivalTimes()
                val etaTexts = activeEtaTexts()
                val etaString = if (liveArrivedIndex != -1 && arrivalTimes.containsKey(liveArrivedIndex)) {
                    val arrival = arrivalTimes[liveArrivedIndex]
                    "Arrived: $arrival"
                } else if (isRerouteInFlight) {
                    "Recalculating..."
                } else if (displayStopIndex < stops.size) {
                    "${durationRemaining.toInt()} min"
                } else {
                    "Route completed"
                }
                currentNavigationEtaText = etaString
                tvEta?.text = etaString

                if (isRerouteInFlight && liveArrivedIndex == -1) {
                    binding.tvEstDistance.text = "--"
                    binding.tvEstDuration.text = "Recalculating..."
                    binding.tvEtaNav.text = "ETA: Recalculating..."
                    binding.tvDistanceNav.text = "Distance: --"
                } else {
                    binding.tvEtaNav.text = if (etaString.startsWith("Arrived:") || etaString == "Route completed") {
                        etaString
                    } else {
                        "ETA: $etaString"
                    }
                    binding.tvDistanceNav.text = if (distanceRemaining < 1.0) {
                        "Distance: ${(distanceRemaining * 1000).toInt()} m"
                    } else {
                        String.format(Locale.getDefault(), "Distance: %.1f km", distanceRemaining)
                    }
                }

                // Publish the same freshly calculated top ETA immediately. Without
                currentLocation?.let { syncTrackingDataToFirestore(it, force = true) }

                val legs = routeProgress.route.legs()
                var accumulatedSeconds = (routeProgress.currentLegProgress?.durationRemaining ?: 0.0).toInt()

                stops.forEachIndexed { index, stop ->
                    val arrivalTime = arrivalTimes[index]
                    if (arrivalTime == "Skipped") {
                        stop.time = if (isReverseTripActive && isViewingReverseTrip) "NOT VISITED" else "Skipped"
                    } else if (arrivalTime != null) {
                        stop.time = "Arrived: $arrivalTime"
                    } else if (isRerouteInFlight && liveArrivedIndex == -1) {
                        if (index >= displayStopIndex) {
                            stop.time = "ETA: Recalculating..."
                        } else {
                            stop.time = "ETA: --"
                        }
                    } else if (index == displayStopIndex) {
                        val etaTime = Calendar.getInstance().apply { add(Calendar.SECOND, accumulatedSeconds) }.time
                        val etaText = "ETA: ${timeFormat.format(etaTime)}"
                        stop.time = etaText
                        etaTexts[index] = etaText
                        Log.d("ETA_DEBUG", "Stop=$index (current), remainingSeconds=$accumulatedSeconds, calculatedETA=$etaText")
                    } else if (index > displayStopIndex) {
                        if (legs != null && (index - navStartIndex) < legs.size) {
                            val legIdx = mapboxLegByOriginalStopIndex[index] ?: (index - navStartIndex)
                            if (legIdx >= 0 && legs[legIdx] != null) {
                                accumulatedSeconds += (legs[legIdx].duration() ?: 0.0).toInt()
                            }
                        }
                        val etaTime = Calendar.getInstance().apply { add(Calendar.SECOND, accumulatedSeconds) }.time
                        val etaText = "ETA: ${timeFormat.format(etaTime)}"
                        stop.time = etaText
                        etaTexts[index] = etaText
                        Log.d("ETA_DEBUG", "Stop=$index (upcoming), accumulatedSeconds=$accumulatedSeconds, calculatedETA=$etaText")
                    } else {
                        stop.time = "ETA: --"
                        Log.d("ETA_DEBUG", "Stop=$index fell into else branch (index < displayStopIndex=$displayStopIndex) -> ETA: --")
                    }
                }

                updateUpcomingStopsUI()

                val bannerInstructions = routeProgress.bannerInstructions
                val primary = bannerInstructions?.primary()
                val sub = bannerInstructions?.sub()
                val currentStepProgress = routeProgress.currentLegProgress?.currentStepProgress
                val stepManeuver = currentStepProgress?.step?.maneuver()

                val maneuverType = primary?.type() ?: stepManeuver?.type()
                val maneuverModifier = primary?.modifier() ?: stepManeuver?.modifier()
                val primaryInstruction = primary?.text() ?: stepManeuver?.instruction() ?: "Continue straight"
                val secondaryStreet = sub?.text() ?: currentStepProgress?.step?.name()?.takeIf { it.isNotBlank() } ?: "Current Route"
                val maneuverDistanceMeters = currentStepProgress?.distanceRemaining

                binding.ivArrow.setImageResource(getManeuverIconRes(maneuverType, maneuverModifier))
                binding.tvManeuverDistance.text = formatManeuverDistance(maneuverDistanceMeters)
                binding.tvNextInstruction.text = primaryInstruction
                binding.tvNextStreet.text = secondaryStreet

                binding.instructionCard.visibility = View.VISIBLE
                binding.maneuverView.visibility = View.GONE
            }
        }
    }

    /** Refreshes the cached attendance value only at meaningful trip events. */
    private fun refreshLoadStat() {
        val route = assignedRoute ?: return
        val routeName = route.routeName
        val today = attendanceDate()

        FirebaseRepository.fetchStudentsByRoute(routeName) { students ->
            FirebaseRepository.fetchAttendance { allAttendance ->
                val studentIds = students.mapTo(mutableSetOf()) { it.id }
                val records = allAttendance.filter {
                    it.route == routeName && it.date == today && it.studentId in studentIds
                }

                fun isPresent(value: String?): Boolean =
                    !value.isNullOrBlank() && value != "--" &&
                            !value.equals("Pending", true) &&
                            !value.equals("Absent", true) && !AttendanceStatus.isShortLeave(value)

                fun hasDropped(value: String?): Boolean =
                    isPresent(value) && !value.equals("School", true) &&
                            !value.equals("En Route", true)

                val isMorning = isActiveTripMorning()

                val loadString: String
                if (isMorning) {
                    val presentCount = records.count { isPresent(it.morningPickup) }
                    loadString = "$presentCount/${students.size}"
                } else {
                    val boardedCount = records.count { isPresent(it.eveningPickup) }
                    val droppedCount = records.count {
                        isPresent(it.eveningPickup) && hasDropped(it.eveningDrop)
                    }
                    val currentLoad = (boardedCount - droppedCount).coerceAtLeast(0)
                    loadString = "$currentLoad/${students.size}"
                }

                cachedLoadString = loadString
                runOnUiThread { binding.bottomSummaryCard.findViewById<TextView>(R.id.tvLoadSheet)?.text = cachedLoadString }
            }
        }
    }

    private fun handleLocationFlow() {
        if (!hasLocationPermission()) {
            Toast.makeText(this, "Requesting Location Permissions...", Toast.LENGTH_SHORT).show()
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
        } else {
            checkLocationSettings()
        }
    }

    private fun hasLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasPreciseLocationPermission(): Boolean =
        ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

    private fun hasFreshCurrentLocation(): Boolean {
        val location = currentLocation ?: return false
        val timestamp = location.time.takeIf { it > 0L } ?: lastFreshLocationTimestamp
        val ageMs = System.currentTimeMillis() - timestamp
        return timestamp > 0L && ageMs in 0..MAX_CALLBACK_LOCATION_AGE_MS
    }

    private fun requestNavigationStartLocation(route: RouteModel) {
        pendingNavigationStartRoute = route
        if (!hasPreciseLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                1001
            )
            return
        }
        checkLocationSettings()
    }

    private fun requestOneShotNavigationLocation(route: RouteModel) {
        if (pendingNavigationStartRoute?.id != route.id || !hasLocationPermission()) return
        pendingNavigationStartTimeout?.let(navigationLocationHandler::removeCallbacks)
        val timeout = Runnable {
            if (pendingNavigationStartRoute?.id != route.id) return@Runnable
            clearPendingNavigationStart()
            updateLocationReliabilityStatus(
                if (isGpsProviderEnabled()) LocationReliabilityState.LOCATION_STALE
                else LocationReliabilityState.GPS_UNAVAILABLE
            )
            Toast.makeText(
                this,
                "No fresh GPS fix arrived. Check location permission and device location settings, then try again.",
                Toast.LENGTH_LONG
            ).show()
        }
        pendingNavigationStartTimeout = timeout
        navigationLocationHandler.postDelayed(timeout, 15000L)

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                CancellationTokenSource().token
            ).addOnSuccessListener { location ->
                if (location == null) {
                    Log.w("LocationDebug", "One-shot current-location request returned no fix; waiting for location updates")
                    return@addOnSuccessListener
                }
                val ageMs = System.currentTimeMillis() - location.time
                if (ageMs > MAX_CALLBACK_LOCATION_AGE_MS || ageMs < -2000L ||
                    location.elapsedRealtimeNanos <= lastAcceptedLocationElapsedNanos
                ) {
                    Log.w("LocationDebug", "Ignoring one-shot location fix age=${ageMs}ms " +
                            "lastAcceptedElapsed=$lastAcceptedLocationElapsedNanos fixElapsed=${location.elapsedRealtimeNanos}")
                    return@addOnSuccessListener
                }
                lastAcceptedLocationElapsedNanos = location.elapsedRealtimeNanos
                handleLocationUpdate(location)
            }.addOnFailureListener { error ->
                Log.e("LocationDebug", "One-shot current-location request failed", error)
                if (error is SecurityException) {
                    clearPendingNavigationStart()
                    updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
                    Toast.makeText(this, "Location permission is required to start navigation.", Toast.LENGTH_LONG).show()
                }
            }
        } catch (error: SecurityException) {
            Log.e("LocationDebug", "Location permission was revoked during current-location request", error)
            clearPendingNavigationStart()
            updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
            Toast.makeText(this, "Location permission is required to start navigation.", Toast.LENGTH_LONG).show()
        }
    }

    private fun continuePendingNavigationStart() {
        val route = pendingNavigationStartRoute ?: return
        val location = currentLocation ?: return
        if (System.currentTimeMillis() - location.time > MAX_CALLBACK_LOCATION_AGE_MS) return
        clearPendingNavigationStart()
        handleStartNavigation(route)
    }

    private fun clearPendingNavigationStart() {
        pendingNavigationStartTimeout?.let(navigationLocationHandler::removeCallbacks)
        pendingNavigationStartTimeout = null
        pendingNavigationStartRoute = null
    }

    private fun isGpsProviderEnabled(): Boolean {
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
        return locationManager?.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) == true ||
                locationManager?.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER) == true
    }

    private fun updateLocationReliabilityStatus(state: LocationReliabilityState) {
        currentReliabilityState = state
        val banner = binding.layoutLocationReliabilityBanner
        val icon = banner.findViewById<ImageView>(R.id.ivReliabilityIcon)
        val text = banner.findViewById<TextView>(R.id.tvReliabilityStatus)

        when (state) {
            LocationReliabilityState.GPS_UNAVAILABLE -> {
                banner.visibility = View.VISIBLE
                banner.setBackgroundColor(Color.parseColor("#EF4444")) // Red
                icon.setImageResource(R.drawable.ic_gps_fixed_white)
                text.text = "Unable to get your current location."
            }
            LocationReliabilityState.GPS_ACCURACY_LOW -> {
                banner.visibility = View.VISIBLE
                banner.setBackgroundColor(Color.parseColor("#F59E0B")) // Amber
                icon.setImageResource(R.drawable.ic_gps_fixed_white)
                text.text = "Location accuracy is low."
            }
            LocationReliabilityState.LOCATION_STALE -> {
                banner.visibility = View.VISIBLE
                banner.setBackgroundColor(Color.parseColor("#EF4444")) // Red
                icon.setImageResource(R.drawable.ic_gps_fixed_white)
                text.text = "Location update unavailable. Checking GPS..."
            }
            LocationReliabilityState.INTERNET_UNAVAILABLE_GPS_OK -> {
                banner.visibility = View.VISIBLE
                banner.setBackgroundColor(Color.parseColor("#3B82F6")) // Blue
                icon.setImageResource(R.drawable.ic_wifi_off_white)
                text.text = "Internet unavailable. GPS tracking continues."
            }
            LocationReliabilityState.NORMAL_LIVE -> {
                banner.visibility = View.GONE
            }
        }
    }

    private fun onNetworkStatusChanged(online: Boolean) {
        if (!isDutyEnabled) return

        if (!online) {
            val now = System.currentTimeMillis()
            val isGpsFresh = lastFreshLocationTimestamp > 0L && (now - lastFreshLocationTimestamp) < STALE_LOCATION_TIMEOUT_MS
            if (isGpsFresh) {
                updateLocationReliabilityStatus(LocationReliabilityState.INTERNET_UNAVAILABLE_GPS_OK)
            }
        } else {
            if (currentReliabilityState == LocationReliabilityState.INTERNET_UNAVAILABLE_GPS_OK) {
                val now = System.currentTimeMillis()
                val isGpsFresh = lastFreshLocationTimestamp > 0L && (now - lastFreshLocationTimestamp) < STALE_LOCATION_TIMEOUT_MS
                if (isGpsFresh) {
                    val acc = currentLocation?.accuracy ?: 0f
                    if (acc > LOW_ACCURACY_THRESHOLD_METERS) {
                        updateLocationReliabilityStatus(LocationReliabilityState.GPS_ACCURACY_LOW)
                    } else {
                        updateLocationReliabilityStatus(LocationReliabilityState.NORMAL_LIVE)
                    }
                }
            }
            com.example.bustrack_app.sync.SyncQueueManager.processQueue()
            currentLocation?.let { syncTrackingDataToFirestore(it, force = true) }
        }
    }

    private fun startStaleLocationWatchdog() {
        stopStaleLocationWatchdog()
        val runnable = object : Runnable {
            override fun run() {
                if (!isDutyEnabled) return
                if (!isGpsProviderEnabled()) {
                    isCurrentLocationLive = false
                    updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
                    staleLocationHandler.postDelayed(this, 2000L)
                    return
                }
                val now = System.currentTimeMillis()
                if (lastFreshLocationTimestamp > 0L) {
                    val age = now - lastFreshLocationTimestamp
                    if (age >= STALE_LOCATION_TIMEOUT_MS) {
                        isCurrentLocationLive = false
                        updateLocationReliabilityStatus(LocationReliabilityState.LOCATION_STALE)
                    }
                } else {
                    isCurrentLocationLive = false
                    updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
                }
                staleLocationHandler.postDelayed(this, 2000L)
            }
        }
        staleLocationRunnable = runnable
        staleLocationHandler.postDelayed(runnable, 2000L)
    }

    private fun stopStaleLocationWatchdog() {
        staleLocationRunnable?.let { staleLocationHandler.removeCallbacks(it) }
        staleLocationRunnable = null
    }

    private fun checkLocationSettings() {
        if (!hasLocationPermission()) {
            handleLocationFlow()
            return
        }
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000)
            .build()

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
            pendingNavigationStartRoute?.let(::requestOneShotNavigationLocation)
        }

        task.addOnFailureListener { exception ->
            updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
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

        builder.bearing(lastValidBearing)

        if (location.hasSpeed()) {
            builder.speed(location.speed.toDouble())
        }
        return builder.build()
    }

    // Accuracy-aware GPS hold: drop noisy stationary wander, keep real movement, and
    private fun feedRawLocationToPuck(location: Location): Boolean {
        if (location.hasAccuracy() && location.accuracy > MAX_ACCEPTABLE_PUCK_ACCURACY_METERS) {
            return false
        }
        val previous = lastPuckPosition
        val movedMeters = previous?.distanceTo(location) ?: Float.MAX_VALUE
        val speed = if (location.hasSpeed()) location.speed else 0f
        val isMoving = speed >= STATIONARY_HOLD_SPEED_MPS
        if (previous != null) {
            val noiseFloor = maxOf(
                if (location.hasAccuracy()) location.accuracy else 8f,
                if (previous.hasAccuracy()) previous.accuracy else 8f,
                5f
            )
            if (!isMoving && movedMeters < noiseFloor) {
                return false
            }
            if (isMoving && movedMeters < MIN_MOVING_PUCK_UPDATE_METERS) {
                return false
            }
            if (lastPuckElapsedNanos > 0L && location.elapsedRealtimeNanos > lastPuckElapsedNanos) {
                val dtSec = (location.elapsedRealtimeNanos - lastPuckElapsedNanos) / 1_000_000_000.0
                if (dtSec in 0.05..12.0 && (movedMeters / dtSec) > MAX_PLAUSIBLE_PUCK_SPEED_MPS) {
                    return false
                }
                puckTransitionDurationMs = if (isMoving && dtSec in 0.05..12.0) {
                    (dtSec * 900.0).toLong().coerceIn(250L, 1800L)
                } else 0L
            } else {
                puckTransitionDurationMs = 0L
            }
        } else {
            puckTransitionDurationMs = 0L
        }
        updateBusHeading(location, previous, movedMeters)
        lastPuckPosition = Location(location)
        lastPuckElapsedNanos = location.elapsedRealtimeNanos

        val animMs = if (previous == null || !isMoving) 0L else puckTransitionDurationMs
        navigationLocationProvider.changePosition(
            location = toMapboxLocation(location),
            keyPoints = emptyList(),
            latLngTransitionOptions = { duration = animMs },
            bearingTransitionOptions = { duration = animMs }
        )
        return true
    }

    private fun startLocationUpdates() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
            return
        }

        if (!isGpsProviderEnabled()) {
            updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // lastLocation is an asynchronous cached read. It may return after
                    val cacheAgeMs = System.currentTimeMillis() - location.time
                    if (cacheAgeMs > STALE_CACHED_LOCATION_MAX_AGE_MS) return@addOnSuccessListener
                    if (location.elapsedRealtimeNanos <= lastAcceptedLocationElapsedNanos) return@addOnSuccessListener
                    lastAcceptedLocationElapsedNanos = location.elapsedRealtimeNanos

                    val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
                    if (accuracy <= UNACCEPTABLE_ACCURACY_THRESHOLD_METERS) {
                        lastFreshLocationTimestamp = location.time.takeIf { it > 0L }
                            ?: System.currentTimeMillis()
                        val wasLive = isCurrentLocationLive
                        currentLocation = Location(location)
                        isCurrentLocationLive = (accuracy <= LOW_ACCURACY_THRESHOLD_METERS)
                        feedRawLocationToPuck(location)

                        if (accuracy > LOW_ACCURACY_THRESHOLD_METERS) {
                            updateLocationReliabilityStatus(LocationReliabilityState.GPS_ACCURACY_LOW)
                        } else if (!isInternetConnected) {
                            updateLocationReliabilityStatus(LocationReliabilityState.INTERNET_UNAVAILABLE_GPS_OK)
                        } else {
                            updateLocationReliabilityStatus(LocationReliabilityState.NORMAL_LIVE)
                        }

                        if (!wasLive && isCurrentLocationLive) {
                            // A newly opened dashboard must retain its pending full-route
                            updateMapDisplay()
                        }
                        centerMapOnLiveLocationWhenRouteIsUnavailable()
                    }
                }
            }.addOnFailureListener {
                updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
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
                    // Fused can batch several old fixes. Animating every member of the
                    val location = locationResult.lastLocation ?: return
                    val timestamp = location.elapsedRealtimeNanos
                    if (timestamp <= lastAcceptedLocationElapsedNanos) return
                    lastAcceptedLocationElapsedNanos = timestamp
                    handleLocationUpdate(location)
                }
            }

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback!!, mainLooper)
        } catch (e: SecurityException) {
            Log.e("LocationDebug", "SecurityException in startLocationUpdates: ${e.message}", e)
            updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
        }
    }

    /** Single coordinated route-state path. Raw Fused GPS is authoritative. */
    private fun handleLocationUpdate(location: Location) {
        if (!isDutyEnabled) return
        val now = System.currentTimeMillis()
        val fixTimestamp = location.time.takeIf { it > 0L && it <= now + 2000L } ?: now
        if (now - fixTimestamp > MAX_CALLBACK_LOCATION_AGE_MS) {
            Log.d("LocationDebug", "Dropping delayed location fix age=${now - fixTimestamp}ms")
            return
        }
        val accuracy = if (location.hasAccuracy()) location.accuracy else 0f
        if (accuracy > UNACCEPTABLE_ACCURACY_THRESHOLD_METERS) {
            return
        }

        lastFreshLocationTimestamp = fixTimestamp
        val wasLive = isCurrentLocationLive
        currentRawLocation = Location(location)
        currentLocation = Location(location)
        isCurrentLocationLive = (accuracy <= LOW_ACCURACY_THRESHOLD_METERS)

        if (accuracy > LOW_ACCURACY_THRESHOLD_METERS) {
            updateLocationReliabilityStatus(LocationReliabilityState.GPS_ACCURACY_LOW)
        } else if (!isInternetConnected) {
            updateLocationReliabilityStatus(LocationReliabilityState.INTERNET_UNAVAILABLE_GPS_OK)
        } else {
            updateLocationReliabilityStatus(LocationReliabilityState.NORMAL_LIVE)
        }

        // The newest Fused GPS fix is the single visual-puck authority, both before
        if (feedRawLocationToPuck(location)) {
            // Camera follows the same accepted visual coordinate as the puck.
            lastPuckPosition?.let(::followLiveBusCamera)
        }

        if (isNavigating) {
            updateLocationSummary(location)
            reverseGeocodeIfNeeded(location)
        }

        if (isNavigating) {
            checkGeofenceAndStopStatus(location)
        } else if (!wasLive && isCurrentLocationLive) {
            // Keep the full-route fit requested by a fresh dashboard or by ending
            updateMapDisplay()
        }
        if (!isNavigating) centerMapOnLiveLocationWhenRouteIsUnavailable()

        // Persist only after the route has been split at this GPS point. Admin,
        syncTrackingDataToFirestore(location)
        continuePendingNavigationStart()
    }

    /**
     * The viewport follow state is useful for initial pitch/bearing, but it can become
     * idle after a style reload.  Drive the camera from the same accepted GPS fix as
     * the puck so navigation never leaves the bus behind.  This is disabled as soon
     * as the driver pans, and Re-centre enables it again.
     */
    private fun followLiveBusCamera(location: Location) {
        if (!isNavigating || !isCameraFollowingBus) {
            Log.d("CAMERA_DEBUG", "followLiveBusCamera skipped: isNavigating=$isNavigating " +
                    "isCameraFollowingBus=$isCameraFollowingBus gpsAccepted=false " +
                    "followSkippedReason=${if (!isNavigating) "notNavigating" else "isCameraFollowingBusFalse"}")
            return
        }

        val previous = lastCameraFollowLocation
        // Follow must never stall: besides bus movement, also compare with where the
        val cameraCenter = mapView?.mapboxMap?.cameraState?.center
        val cameraLagMeters = cameraCenter?.let {
            TurfMeasurement.distance(it, Point.fromLngLat(location.longitude, location.latitude), TurfConstants.UNIT_METERS)
        } ?: Double.MAX_VALUE
        if (previous != null && previous.distanceTo(location) < MIN_MOVING_PUCK_UPDATE_METERS &&
            cameraLagMeters < 3.0) {
            Log.d("CAMERA_DEBUG", "followLiveBusCamera skipped: gpsAccepted=false " +
                    "followSkippedReason=movedLessThan${MIN_MOVING_PUCK_UPDATE_METERS}m " +
                    "cameraBefore=${previous.latitude},${previous.longitude} targetBusLocation=${location.latitude},${location.longitude}")
            return
        }
        lastCameraFollowLocation = Location(location)

        val target = Point.fromLngLat(location.longitude, location.latitude)
        // The follow-puck viewport state is used to initialise the perspective,
        mapView?.viewport?.idle()
        mapView?.mapboxMap?.easeTo(
            CameraOptions.Builder()
                .center(target)
                .bearing(if (isNorthUp) 0.0 else lastValidBearing)
                .pitch(if (isNorthUp) 45.0 else 65.0)
                .zoom(if (isNorthUp) 17.5 else DRIVER_RECENTER_ZOOM)
                .padding(navigationCameraPadding())
                .build(),
            // Smooth normally; snap when the camera has fallen far behind the bus.
            MapAnimationOptions.mapAnimationOptions {
                duration(if (cameraLagMeters > 150.0 && cameraLagMeters != Double.MAX_VALUE) 0L else puckTransitionDurationMs)
            }
        )
        Log.d("CAMERA_DEBUG", "followLiveBusCamera applied: gpsAccepted=true followCalled=true " +
                "cameraBefore=${previous?.let { "${it.latitude},${it.longitude}" }} targetBusLocation=${target.latitude()},${target.longitude()} " +
                "bearing=${if (isNorthUp) 0.0 else lastValidBearing}")
    }

    /**
     * Derive the bus bearing only from consecutive accepted GPS positions. A phone
     * heading can point somewhere other than the vehicle's direction of travel.
     */
    private fun updateBusHeading(location: Location, previous: Location?, movedMeters: Float) {
        val measured = previous
            ?.takeIf { movedMeters >= MIN_MOVING_PUCK_UPDATE_METERS }
            ?.bearingTo(location)
            ?.toDouble()
            ?: return

        val previousBearingForLog = lastValidBearing
        lastValidBearing = if (lastValidBearing == 0.0) measured else {
            val delta = ((measured - lastValidBearing + 540.0) % 360.0) - 180.0
            (lastValidBearing + delta * 0.85 + 360.0) % 360.0
        }
        Log.d("HEADING_DEBUG", "previousRawGPS=${previous?.let { "${it.latitude},${it.longitude}" }} " +
                "currentRawGPS=${location.latitude},${location.longitude} movedMeters=$movedMeters " +
                "speed=${location.speed} calculatedBearing=$lastValidBearing previousBearing=$previousBearingForLog")
    }

    /** Immediately removes the layout placeholder while a human-readable address loads. */
    private fun updateLocationSummary(location: Location) {
        // Show a nearby stop when no address is available.
        val nearestStop = assignedRoute?.stopsList
            ?.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            ?.minByOrNull { stop ->
                val result = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, stop.latitude, stop.longitude, result)
                result[0]
            }
        val locationText = lastResolvedAddress
            ?: nearestStop?.let { "Near ${it.stopName}" }
            ?: "Finding current address..."
        binding.bottomSummaryCard.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = locationText
    }

    private fun syncTrackingDataToFirestore(location: Location, force: Boolean = false) {
        val driverId = viewModel.currentDriver.value?.driverId ?: return
        if (!isDutyEnabled) return
        if (currentActiveTripId != null && isRecoveryOutsideCurrentWindow()) {
            expireRecoveryForNextTripWindow(currentActiveTripId.orEmpty())
            return
        }

        val now = System.currentTimeMillis()
        val distanceMoved = lastFirestoreLocation?.distanceTo(location) ?: Float.MAX_VALUE

        val elapsed = now - lastFirestoreUpdateTime
        // Regular movement is paced at 1 s AND 2 m. A 10 s heartbeat preserves a
        if (force || (elapsed >= FIRESTORE_UPDATE_INTERVAL && distanceMoved >= FIRESTORE_MIN_DISTANCE) || elapsed >= 10000L) {

            val sheet = binding.bottomSummaryCard
            val tvEta = sheet.findViewById<TextView>(R.id.tvEtaSheet)
            val tvLoad = sheet.findViewById<TextView>(R.id.tvLoadSheet)

            val etaVal = currentNavigationEtaText
                ?.takeUnless { it == "--" || it == "Calculating..." }
                ?: "On Way"
            val speedVal = (location.speed * 3.6)
            val loadVal = tvLoad?.text?.toString() ?: "0/0"

            val arrivalMap = activeArrivalTimes().mapKeys { it.key.toString() }
            val etaMap = activeEtaTexts().mapKeys { it.key.toString() }
            val traveledSegments = currentTraveledSegments().map { LineString.fromLngLats(it).toPolyline(6) }

            val locStatus = when {
                !isCurrentLocationLive && (System.currentTimeMillis() - lastFreshLocationTimestamp >= STALE_LOCATION_TIMEOUT_MS) -> "STALE"
                currentReliabilityState == LocationReliabilityState.GPS_ACCURACY_LOW -> "LOW_ACCURACY"
                else -> "LIVE"
            }

            Log.d("STOP_DEBUG", "writingToFirestore: nextGlobalStopIndex=$nextGlobalStopIndex " +
                    "persistedStopArrivalTimes=$arrivalMap localState(isCurrentlyAtStop=$isCurrentlyAtStop, " +
                    "lastArrivedStopIndex=$lastArrivedStopIndex)")
            FirebaseRepository.updateDriverLiveState(
                driverId = driverId,
                lat = location.latitude,
                lng = location.longitude,
                eta = etaVal,
                speed = speedVal,
                load = loadVal,
                currentPolyline = currentRouteGeometry,
                traveledPolyline = traveledRouteGeometry,
                nextStopIndex = nextGlobalStopIndex,
                stopArrivalTimes = arrivalMap,
                stopEtaTimes = etaMap,
                isNavigating = isNavigating,
                tripDirection = if (isReverseTripActive) "RETURN" else "FORWARD",
                traveledRouteSegments = traveledSegments,
                activeTripId = if (isNavigating) currentActiveTripId else "",
                activeRouteId = if (isNavigating) assignedRoute?.id else "",
                activeRouteName = if (isNavigating) assignedRoute?.routeName else "",
                accuracy = if (location.hasAccuracy()) location.accuracy else 0f,
                locationTimestamp = if (lastFreshLocationTimestamp > 0L) lastFreshLocationTimestamp else now,
                locationStatus = locStatus
            )

            if (isNavigating) {
                persistCurrentActiveTripState()
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
        val route = assignedRoute
        if (route == null) {
            centerMapOnLiveLocationWhenRouteIsUnavailable()
            return
        }

        if (isNavigating) {
            return
        }

        // The annotations and camera calculation must run against a loaded style.
        if (!isMapStyleReady) {
            dashboardCameraFitPending = true
            return
        }

        if (dashboardCameraFitPending) {
            shouldFitCameraToRoute = true
        }

        // Render the locally available route first. Road geometry is a visual
        drawStaticSavedRoute(route)
        if (!shouldFitCameraToRoute) {
            dashboardCameraFitPending = false
        }

        val loc = currentLocation
        if (loc != null && isCurrentLocationLive) {
            val needsRoadPreview = lastDashboardPreviewRouteId != route.id ||
                    (lastDashboardPreviewOrigin?.distanceTo(loc) ?: Float.MAX_VALUE) >=
                    DASHBOARD_PREVIEW_MIN_MOVEMENT_METERS
            if (!needsRoadPreview) return

            lastDashboardPreviewRouteId = route.id
            lastDashboardPreviewOrigin = Location(loc)
            val previewGeneration = ++dashboardRoutePreviewGeneration
            fetchDynamicRoutePreview(route, loc, previewGeneration)
            return
        }

    }

    /**
     * Restore the complete dashboard route immediately after navigation ends.
     * The dynamic road preview is asynchronous; relying on it alone leaves the
     * old destination camera visible while its request is pending or fails.
     */
    private fun restoreDashboardRouteOverview() {
        val route = assignedRoute ?: return
        val previewGeneration = ++dashboardRoutePreviewGeneration
        shouldFitCameraToRoute = true
        dashboardCameraFitPending = true
        if (!isMapStyleReady) return
        drawStaticSavedRoute(route)
        if (!shouldFitCameraToRoute) {
            dashboardCameraFitPending = false
        }

        // Replace the immediate saved-path preview with road geometry when it is
        currentLocation?.takeIf { isCurrentLocationLive }?.let { location ->
            fetchDynamicRoutePreview(route, location, previewGeneration)
        }
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

    private fun fetchDynamicRoutePreview(
        route: RouteModel,
        origin: Location,
        previewGeneration: Long
    ) {
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
                    if (isNavigating || previewGeneration != dashboardRoutePreviewGeneration ||
                        assignedRoute?.id != route.id
                    ) return
                    val geometry = response.body()?.routes()?.firstOrNull()?.geometry()
                    if (geometry != null) {
                        val points = LineString.fromPolyline(geometry, 6).coordinates()
                        runOnUiThread {
                            if (previewGeneration == dashboardRoutePreviewGeneration &&
                                assignedRoute?.id == route.id
                            ) {
                                drawPointsOnMap(points)
                            }
                        }
                    } else {
                        runOnUiThread {
                            if (previewGeneration == dashboardRoutePreviewGeneration &&
                                assignedRoute?.id == route.id
                            ) {
                                drawStaticSavedRoute(route)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    Log.e("NavDebug", "Dynamic dashboard route preview failed: ${t.message}")
                    runOnUiThread {
                        if (!isNavigating && previewGeneration == dashboardRoutePreviewGeneration &&
                            assignedRoute?.id == route.id
                        ) {
                            drawStaticSavedRoute(route)
                        }
                    }
                }
            })
    }

    override fun onResume() {
        super.onResume()
        refreshProfileData()
        val voicePref = getSharedPreferences("navigation_preferences", MODE_PRIVATE)
            .getBoolean("voice_enabled", true)
        if (isVoiceEnabled != voicePref) {
            isVoiceEnabled = voicePref
            binding.btnSound.setImageResource(if (isVoiceEnabled) R.drawable.volume_up else R.drawable.mute)
            binding.btnSound.imageTintList = ColorStateList.valueOf(Color.WHITE)
            if (!isVoiceEnabled) {
                speechApi?.cancel()
                voiceInstructionsPlayer?.clear()
                fallbackTextToSpeech?.stop()
                abandonNavigationAudioFocus()
            }
        }
        if (hasLocationPermission()) {
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

        utils.ImageUtils.loadProfileImage(this, driver.profileImageUrl, binding.ivProfile)

        findViewById<TextView>(R.id.drawerName)?.text = driver.name
        findViewById<TextView>(R.id.drawerEmail)?.text = driver.email
        findViewById<ImageView>(R.id.drawerImgProfile)?.let { drawerImg ->
            utils.ImageUtils.loadProfileImage(this, driver.profileImageUrl, drawerImg)
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

    private fun setupLocationPuck() {
        // Style reloads can briefly expose Mapbox's default globe camera. Use the
        val initialScale = computeBusModelScale(BUS_MODEL_SCALE_REFERENCE_ZOOM)
        busScaleUpdateGeneration++
        lastAppliedBusScale = initialScale

        mapView?.location?.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = isDutyEnabled
            pulsingEnabled = isDutyEnabled
            puckBearingEnabled = true

            locationPuck = LocationPuck3D(
                modelUri = "asset://bus.glb",
                modelScale = listOf(initialScale, initialScale, initialScale),
                modelScaleMode = ModelScaleMode.MAP,
                modelTranslation = listOf(0f, 0f, 0f),
                modelRotation = listOf(BUS_MODEL_ROLL_OFFSET_X_DEG, BUS_MODEL_ROLL_OFFSET_Y_DEG, 90f)
            )
        }

        if (isCurrentLocationLive) {
            currentLocation?.let { feedRawLocationToPuck(it) }
        }
    }

    // Update the layer Mapbox's location component already owns; never create a
    private val cameraChangeListener = OnCameraChangeListener { scheduleBusScaleUpdate() }

    private fun computeBusModelScale(zoom: Double): Float {
        val clampedZoom = zoom.coerceIn(MIN_ZOOM_FOR_BUS_SCALE, MAX_ZOOM_FOR_BUS_SCALE)
        val apparentExponent =
            (BUS_MODEL_SCALE_REFERENCE_ZOOM - clampedZoom) * BUS_MODEL_SCALE_COMPENSATION_FACTOR
        val apparentTarget = (BUS_MODEL_SCALE_REFERENCE_VALUE * Math.pow(2.0, apparentExponent))
            .coerceIn(MIN_BUS_MODEL_SCALE.toDouble(), MAX_BUS_MODEL_SCALE.toDouble())
        val worldToScreenCompensation = Math.pow(2.0, BUS_MODEL_SCALE_REFERENCE_ZOOM - clampedZoom)
        return (apparentTarget * worldToScreenCompensation).toFloat()
    }

    private fun locationModelLayer(style: Style): ModelLayer? {
        (style.getLayer(LOCATION_MODEL_LAYER_ID) as? ModelLayer)?.let { return it }
        val layerId = style.styleLayers.firstOrNull { layer ->
            layer.id.contains("location", ignoreCase = true) &&
                    layer.id.contains("model", ignoreCase = true)
        }?.id ?: return null
        return style.getLayer(layerId) as? ModelLayer
    }

    private fun updateBusModelScaleForZoom() {
        val generation = ++busScaleUpdateGeneration
        mapView?.mapboxMap?.getStyle { style ->
            if (generation != busScaleUpdateGeneration || isDestroyed) return@getStyle
            val zoom = mapView?.mapboxMap?.cameraState?.zoom ?: return@getStyle
            val newScale = computeBusModelScale(zoom)
            if (kotlin.math.abs(newScale - lastAppliedBusScale) < 0.05f) return@getStyle
            val layer = locationModelLayer(style) ?: return@getStyle
            layer.modelScale(listOf(newScale.toDouble(), newScale.toDouble(), newScale.toDouble()))
            lastAppliedBusScale = newScale
        }
    }

    private fun scheduleBusScaleUpdate() {
        if (pendingBusScaleUpdate != null) return
        pendingBusScaleUpdate = Runnable {
            pendingBusScaleUpdate = null
            updateBusModelScaleForZoom()
        }.also { busScaleHandler.postDelayed(it, 100L) }
    }

    private fun observeViewModel() {
        viewModel.currentDriver.observe(this) { driver ->
            if (driver != null) {
                val assignedBus = driver.assignedBus?.takeIf { it.isNotBlank() && !it.equals("Not Assigned", true) }
                if (assignedBus == null) {
                    if (DriverRepository.isLatestSnapshotFromCache) return@observe
                    hasCheckedActiveTripRecovery = true
                    assignedRoute = null
                    lockedActiveRoute = null
                    lockedActiveBus = null
                    if (isNavigating || currentActiveTripId != null) {
                        setNavigationMode(false, reloadStyle = false, clearActiveTrip = true)
                    } else {
                        TripRecoveryHelper.clearActiveTrip(this)
                        currentActiveTripId = null
                        currentRoundTripSessionId = null
                    }
                    if (driver.isNavigating || driver.activeTripId.isNotBlank() || driver.activeRouteId.isNotBlank()) {
                        FirebaseRepository.clearDriverNavigationState(driver.driverId.ifBlank { driver.id })
                    }
                    binding.tvBusNumberInfo.text = "Not Assigned"
                    binding.tvRouteNameInfo.text = "No Bus Assigned"
                    binding.tvTotalStops.text = "0"
                    binding.cardRouteDetails.visibility = View.GONE
                    binding.cardNoAssignment.visibility = View.VISIBLE
                    binding.tvAssignmentStateTitle.text = "No Bus Assigned"
                    binding.tvAssignmentStateMessage.text = "No bus or route has been assigned to you. Please contact the college admin."
                    updateNavigationButtonState()
                    updateMapDisplay()
                    return@observe
                }

                // Restore the persisted direction before its stop maps. Otherwise a
                if (driver.isNavigating && driver.tripDirection.equals("RETURN", true) && !isReverseTripActive) {
                    isReverseTripActive = true
                    isViewingReverseTrip = true
                    ensureReverseTripStops()
                    reverseStopArrivalTimes.putAll(driver.stopArrivalTimes.mapKeys { it.key.toInt() })
                    reverseStopEtaTexts.putAll(driver.stopEtaTimes.mapKeys { it.key.toInt() })
                }

                if (!isReverseTripActive && stopArrivalTimes.isEmpty() && driver.stopArrivalTimes.isNotEmpty()) {
                    driver.stopArrivalTimes.forEach { (k, v) ->
                        stopArrivalTimes[k.toInt()] = v
                    }
                }

                if (!isTripStartRouteRequestPending && nextGlobalStopIndex == 0 && driver.nextStopIndex != 0) {
                    Log.d("STOP_DEBUG", "persistedState-recovery: local nextGlobalStopIndex was 0, " +
                            "adopting driver.nextStopIndex=${driver.nextStopIndex} from Firestore " +
                            "(persisted stopArrivalTimes=${driver.stopArrivalTimes}, isNavigating=$isNavigating, " +
                            "currentActiveTripId=$currentActiveTripId)")
                    nextGlobalStopIndex = driver.nextStopIndex
                }

                // Rebuild stopStates from the restored arrival map (e.g. after process death /
                val restoredArrivalTimes = activeArrivalTimes()
                val restoredStates = activeStates()
                if (restoredStates.isEmpty() && restoredArrivalTimes.isNotEmpty()) {
                    restoredArrivalTimes.forEach { (index, value) ->
                        restoredStates[index] = when {
                            value == "Skipped" -> StopState.SKIPPED
                            index < nextGlobalStopIndex -> StopState.COMPLETED
                            else -> StopState.ARRIVED
                        }
                    }
                }

                nextGlobalStopIndex = advanceToUpcomingStop(nextGlobalStopIndex)

                if (!isCurrentlyAtStop && lastArrivedStopIndex == -1 &&
                    stateOf(nextGlobalStopIndex) == StopState.ARRIVED
                ) {
                    isCurrentlyAtStop = true
                    lastArrivedStopIndex = nextGlobalStopIndex
                }

                if (currentLocation == null && driver.latitude != 0.0 && driver.longitude != 0.0) {
                    currentLocation = android.location.Location("firestore").apply {
                        latitude = driver.latitude
                        longitude = driver.longitude
                    }
                    // This is a persisted tracking snapshot, not a current GPS fix.
                    isCurrentLocationLive = false
                }

                checkAndResumeActiveTrip()
            }
        }

        viewModel.dashboardData.observe(this) { data ->
            if (DriverRepository.isLatestSnapshotFromCache || RouteRepository.isLatestSnapshotFromCache) return@observe

            binding.apply {
                if (isNavigating && !lockedActiveBus.isNullOrBlank()) {
                    tvBusNumberInfo.text = lockedActiveBus
                } else {
                    tvBusNumberInfo.text = data.busNumber
                }
                if (isNavigating && lockedActiveRoute != null) {
                    tvRouteNameInfo.text = lockedActiveRoute?.routeName
                } else {
                    tvRouteNameInfo.text = data.currentRoute
                }
                val hasBus = viewModel.currentDriver.value?.assignedBus?.isNotBlank() == true &&
                        !viewModel.currentDriver.value?.assignedBus.equals("Not Assigned", true)
                val hasRoute = assignedRoute != null
                cardNoAssignment.visibility = if (!isNavigating && !hasRoute) View.VISIBLE else View.GONE
                cardRouteDetails.visibility = if (!isNavigating && !hasRoute) View.GONE else View.VISIBLE
                tvAssignmentStateTitle.text = if (hasBus) "No Route Assigned" else "No Bus Assigned"
                tvAssignmentStateMessage.text = if (hasBus) {
                    "No route has been assigned to your bus. Please contact the college admin."
                } else {
                    "No bus or route has been assigned to you. Please contact the college admin."
                }
                tvCurrentDate.text = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

                tvTotalStops.text = data.stopsCount

                val currentTime = System.currentTimeMillis()
                val isPendingSync = (currentTime - lastDutyToggleTime) < DUTY_SYNC_DEBOUNCE_MS

                val activeTrip = isNavigating || currentActiveTripId != null
                if (!isPendingSync && data.isOnDuty != isDutyEnabled &&
                    !(activeTrip && !data.isOnDuty)
                ) {
                    isDutyEnabled = data.isOnDuty

                    isUserTriggeredChange = false
                    findViewById<SwitchMaterial>(R.id.switchDuty)?.isChecked = isDutyEnabled
                    isUserTriggeredChange = true

                    updateDutyUI(isDutyEnabled, reloadStyle = false)
                }
            }

            val driver = viewModel.currentDriver.value
            val hasAssignedBus = !driver?.assignedBus.isNullOrBlank() && !driver?.assignedBus.equals("Not Assigned", true)
            val resolvedRoute = if (hasAssignedBus) resolveAssignedRoute(data) else null
            if (resolvedRoute == null) {
                hasCheckedActiveTripRecovery = true
                assignedRoute = null
                lockedActiveRoute = null
                lockedActiveBus = null
                dashboardRoutePreviewGeneration++
                hasCheckedActiveTripRecovery = true
                if (isNavigating || currentActiveTripId != null) {
                    setNavigationMode(false, reloadStyle = false, clearActiveTrip = true)
                } else {
                    TripRecoveryHelper.clearActiveTrip(this)
                    currentActiveTripId = null
                    currentRoundTripSessionId = null
                }
                if (driver?.isNavigating == true || !driver?.activeTripId.isNullOrBlank() || !driver?.activeRouteId.isNullOrBlank()) {
                    FirebaseRepository.clearDriverNavigationState(driver?.let { it.driverId.ifBlank { it.id } }.orEmpty())
                }
                binding.tvTotalStops.text = "0"
                binding.tvRouteNameInfo.text = if (hasAssignedBus) "No Route Assigned" else "No Bus Assigned"
                clearAssignedRouteFromMap()
                updateNavigationButtonState()
                updateMapDisplay()
                return@observe
            }

            // Route & Trip Integrity: an active trip may stay locked only while the
            if (isNavigating || currentActiveTripId != null) {
                Log.d("TripIntegrity", "TRIP_ROUTE_LOCKED: Active trip $currentActiveTripId is locked to route ${assignedRoute?.routeName}. Ignoring external route change.")
                checkAndResumeActiveTrip()
                return@observe
            }

            resolvedRoute.let { route ->
                hasCheckedActiveTripRecovery = false
                binding.cardNoAssignment.visibility = View.GONE
                binding.cardRouteDetails.visibility = if (isNavigating) View.GONE else View.VISIBLE
                val routeChangedWhileNavigating = assignedRoute?.id != route.id && isNavigating
                if (assignedRoute != null && assignedRoute?.id != route.id) {
                    isNearStart = false
                    shouldFitCameraToRoute = true
                    stopArrivalTimes.clear()
                    stopStates.clear()
                    stopEtaTexts.clear()
                    nextGlobalStopIndex = 0
                    attendancePromptedStops.clear()
                    // Keep missed-stop checks tied to the full trip route.
                    isCurrentlyAtStop = false
                    lastArrivedStopIndex = -1
                    arrivedStopRouteSegmentIndex = null
                    departureCandidateIndex = -1
                    departureConfirmCount = 0
                    // A reassigned route can have an entirely different stop list, so the
                    masterRouteChainagePoints = emptyList()
                    resetFullRouteGpsProgress()
                    // A route assignment can change while the bus is navigating.
                    if (isNavigating) {
                        freezeActiveTraveledSegment()
                        lastSplitIndex = 0
                        lastRawPositionForSnap = null
                    } else {
                        clearTraveledRouteHistory()
                        viewModel.currentDriver.value?.id?.let { driverId ->
                            lastDutyToggleTime = System.currentTimeMillis()
                            FirebaseRepository.updateDriverRouteGeometry(
                                driverId, null, null, 0, emptyMap(), false
                            )
                        }
                    }
                }
                assignedRoute = route
                updateNavigationButtonState()
                ensureReverseTripStops()
                if (routeChangedWhileNavigating) {
                    // The route assignment is now updated, so calculate the new
                    triggerReroute()
                }
                refreshLoadStat()
                // route.stopsList is a fresh set of StopItem instances (time defaults to "").
                route.stopsList.forEachIndexed { index, stop ->
                    val arrival = stopArrivalTimes[index]
                    stop.time = when {
                        arrival == "Skipped" -> "Skipped"
                        arrival != null -> "Arrived: $arrival"
                        else -> stopEtaTexts[index] ?: "TBD"
                    }
                }
                updateTripAddresses(route)

                updateMapDisplay()

                checkAndResumeActiveTrip()
            }
        }

        RouteRepository.routeList.observe(this) {
            checkAndResumeActiveTrip()
        }
    }

    private fun isOrphanedForwardTripAtSource(route: RouteModel, state: ActiveTripState? = null): Boolean {
        val reverseActive = state?.isReverseTripActive ?: isReverseTripActive
        if (reverseActive) return false

        val stopsCount = route.stopsList.size
        val nextIdx = state?.nextStopIndex ?: nextGlobalStopIndex
        if (stopsCount == 0 || nextIdx < stopsCount) return false

        val loc = currentLocation ?: (if (state != null && state.lastKnownLat != 0.0 && state.lastKnownLng != 0.0) {
            Location("state_loc").apply {
                latitude = state.lastKnownLat
                longitude = state.lastKnownLng
            }
        } else null) ?: return false

        val currentPoint = Point.fromLngLat(loc.longitude, loc.latitude)
        val startPoint = route.pathPoints.firstOrNull()?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: route.stopsList.firstOrNull()?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: return false

        val distToSource = TurfMeasurement.distance(currentPoint, startPoint, TurfConstants.UNIT_METERS)
        return distToSource <= RESUME_ROUTE_VALIDATION_RADIUS_METERS
    }

    private fun clearAssignedRouteFromMap() {
        pointAnnotationManager?.deleteAll()
        polylineAnnotationManager?.deleteAll()
        mapView?.mapboxMap?.getStyle { style ->
            (style.getSource(NAV_ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                ?.geometry(LineString.fromLngLats(emptyList()))
            (style.getSource(NAV_TRAVELED_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                ?.geometry(LineString.fromLngLats(emptyList()))
        }
    }

    /** Keep an initialized regional map visible while route data is not yet available. */
    private fun centerMapOnLiveLocationWhenRouteIsUnavailable() {
        if (!isMapStyleReady || isNavigating || hasFallbackCenteredMap) return
        val route = assignedRoute
        val hasRenderableRoutePoint = route?.let {
            it.pathPoints.any { point -> point.latitude != 0.0 && point.longitude != 0.0 } ||
                    it.stopsList.any { stop -> stop.latitude != 0.0 && stop.longitude != 0.0 }
        } == true
        if (hasRenderableRoutePoint) return
        val location = currentLocation ?: return
        val timestamp = location.time.takeIf { it > 0L } ?: lastFreshLocationTimestamp
        val ageMs = System.currentTimeMillis() - timestamp
        if (timestamp <= 0L || ageMs !in 0..MAX_CALLBACK_LOCATION_AGE_MS) return
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(location.longitude, location.latitude))
                .zoom(15.0)
                .pitch(0.0)
                .bearing(0.0)
                .build()
        )
        hasFallbackCenteredMap = true
    }

    private fun isForwardCompleted(route: RouteModel, state: ActiveTripState? = null): Boolean {
        val reverseActive = state?.isReverseTripActive ?: isReverseTripActive
        val nextIndex = state?.nextStopIndex ?: nextGlobalStopIndex
        return !reverseActive && route.stopsList.isNotEmpty() && nextIndex >= route.stopsList.size
    }

    private fun isReturnCompleted(route: RouteModel, state: ActiveTripState? = null): Boolean {
        val reverseActive = state?.isReverseTripActive ?: isReverseTripActive
        val direction = state?.tripDirection.orEmpty()
        if (!reverseActive && !direction.equals("RETURN", true)) return false
        val nextIndex = state?.nextStopIndex ?: nextGlobalStopIndex
        val stopCount = if (state != null && state.reverseStopStates.isNotEmpty()) {
            state.reverseStopStates.size
        } else if (reverseStops.isNotEmpty()) {
            reverseStops.size
        } else {
            route.stopsList.size
        }
        return stopCount > 0 && nextIndex >= stopCount
    }

    /** A completed Return leg closes the round trip for this configured window. */
    private fun isCurrentTripWindowCompleted(route: RouteModel): Boolean {
        val period = currentTripPeriod()
        if (period == TripPeriod.GAP) return false
        val driverId = viewModel.currentDriver.value?.driverId.orEmpty()
        if (driverId.isBlank()) return false
        val busNumber = binding.tvBusNumberInfo.text.toString().trim()
            .ifEmpty { viewModel.currentDriver.value?.assignedBus.orEmpty() }
        val completedReturnTripId = TripRecoveryHelper.generateTripId(
            driverId = driverId,
            busNumber = busNumber,
            routeName = route.routeName,
            direction = "RETURN",
            sessionPeriod = period.name
        )
        return TripRecoveryHelper.isTripCompleted(this, completedReturnTripId)
    }

    private fun stateLocation(state: ActiveTripState?): Location? = currentLocation ?: state
        ?.takeIf { it.lastKnownLat != 0.0 && it.lastKnownLng != 0.0 }
        ?.let {
            Location("persisted_trip_location").apply {
                latitude = it.lastKnownLat
                longitude = it.lastKnownLng
            }
        }

    private fun isNearSource(route: RouteModel, location: Location?): Boolean {
        val source = originalRouteSource() ?: return false
        val loc = location ?: return false
        return TurfMeasurement.distance(
            Point.fromLngLat(loc.longitude, loc.latitude), source, TurfConstants.UNIT_METERS
        ) <= RESUME_ROUTE_VALIDATION_RADIUS_METERS
    }

    private fun isNearLastStop(route: RouteModel, location: Location?): Boolean {
        val lastStop = route.stopsList.lastOrNull { it.latitude != 0.0 && it.longitude != 0.0 } ?: return false
        val loc = location ?: return false
        return TurfMeasurement.distance(
            Point.fromLngLat(loc.longitude, loc.latitude),
            Point.fromLngLat(lastStop.longitude, lastStop.latitude),
            TurfConstants.UNIT_METERS
        ) <= RESUME_ROUTE_VALIDATION_RADIUS_METERS
    }

    /** Restores a completed forward leg without restarting it; Start Navigation can now begin Return. */
    private fun restoreForwardWaitingForReturn(route: RouteModel, state: ActiveTripState) {
        currentActiveTripId = state.tripId
        currentRoundTripSessionId = state.roundTripSessionId.ifBlank { state.tripId }
        assignedRoute = route
        lockedActiveRoute = route
        lockedActiveBus = state.busNumber.ifEmpty { binding.tvBusNumberInfo.text.toString().trim() }
        activeTripIsMorning = state.isMorning
        isReverseTripActive = false
        isViewingReverseTrip = false
        nextGlobalStopIndex = state.nextStopIndex
        stopStates.clear()
        state.stopStates.forEach { (index, value) ->
            stopStates[index] = try { StopState.valueOf(value) } catch (_: Exception) { StopState.UPCOMING }
        }
        stopArrivalTimes.clear()
        stopArrivalTimes.putAll(state.stopArrivalTimes)
        currentLocation = stateLocation(state)
        updateTripAddresses(route)
        updateBottomSheetInfo()
        Toast.makeText(this, "Forward trip completed. Start Navigation to begin the return trip.", Toast.LENGTH_LONG).show()
    }

    private fun checkAndResumeActiveTrip() {
        if (hasCheckedActiveTripRecovery || isNavigating) return
        val driver = viewModel.currentDriver.value ?: return
        if (driver.assignedBus.isNullOrBlank() || driver.assignedBus.equals("Not Assigned", true)) {
            TripRecoveryHelper.clearActiveTrip(this)
            currentActiveTripId = null
            currentRoundTripSessionId = null
            hasCheckedActiveTripRecovery = true
            return
        }
        val localTrip = TripRecoveryHelper.getActiveTrip(this)
        val activeState = TripRecoveryHelper.resolveConflict(localTrip, driver, this)

        if (activeState != null && TripRecoveryHelper.isTripValid(activeState, this)) {
            if (isRecoveryOutsideCurrentWindow(activeState)) {
                expireRecoveryForNextTripWindow(activeState.tripId)
                hasCheckedActiveTripRecovery = true
                return
            }

            val routes = RouteRepository.routeList.value
            if (routes.isNullOrEmpty()) return
            val matchingRoute = routes.find { it.busNo.equals(driver.assignedBus, ignoreCase = true) }
            if (matchingRoute != null) {
                val location = stateLocation(activeState)
                val isReturnTrip = activeState.isReverseTripActive || activeState.tripDirection.equals("RETURN", true)
                if (isReturnTrip && (isReturnCompleted(matchingRoute, activeState) || isNearSource(matchingRoute, location))) {
                    Log.d("TripIntegrity", "TRIP_RECOVERY_RETURN_RESET: Finalizing completed return trip ${activeState.tripId} at source.")
                    resetClosedTripState(activeState.tripId)
                    hasCheckedActiveTripRecovery = true
                    return
                }

                if (isForwardCompleted(matchingRoute, activeState) && activeState.isMorning &&
                    currentTripPeriod() == TripPeriod.MORNING && isNearLastStop(matchingRoute, location)) {
                    hasCheckedActiveTripRecovery = true
                    restoreForwardWaitingForReturn(matchingRoute, activeState)
                    return
                }

                hasCheckedActiveTripRecovery = true
                resumeActiveTrip(matchingRoute, activeState)
                return
            }
        }
        hasCheckedActiveTripRecovery = true
    }

    private fun resumeActiveTrip(route: RouteModel, state: ActiveTripState) {
        Log.d("TripIntegrity", "TRIP_RESTORE: Resuming active trip: ${state.tripId}, route: ${route.routeName}, stopIndex: ${state.nextStopIndex}, direction: ${state.tripDirection}")
        currentActiveTripId = state.tripId
        currentRoundTripSessionId = state.roundTripSessionId.ifBlank { state.tripId }
        assignedRoute = route
        lockedActiveRoute = route
        lockedActiveBus = state.busNumber.ifEmpty { binding.tvBusNumberInfo.text.toString().trim() }
        if (!lockedActiveBus.isNullOrBlank()) {
            binding.tvBusNumberInfo.text = lockedActiveBus
        }
        binding.tvRouteNameInfo.text = route.routeName

        isDutyEnabled = true
        isUserTriggeredChange = false
        findViewById<SwitchMaterial>(R.id.switchDuty)?.isChecked = true
        isUserTriggeredChange = true
        updateDutyUI(true, reloadStyle = false)

        activeTripIsMorning = state.isMorning
        isNearStart = true // Do not block on initial start geofence during recovery!

        if (state.tripDirection.equals("RETURN", true) || state.isReverseTripActive) {
            isReverseTripActive = true
            isViewingReverseTrip = true
            ensureReverseTripStops()
            reverseStopArrivalTimes.clear()
            val rArrivals = if (state.reverseStopArrivalTimes.isNotEmpty()) state.reverseStopArrivalTimes else state.stopArrivalTimes
            reverseStopArrivalTimes.putAll(rArrivals)
            reverseStopEtaTexts.clear()
            val rEtas = if (state.reverseStopEtaTexts.isNotEmpty()) state.reverseStopEtaTexts else state.stopEtaTexts
            reverseStopEtaTexts.putAll(rEtas)
            reverseStopStates.clear()
            val rStates = if (state.reverseStopStates.isNotEmpty()) state.reverseStopStates else state.stopStates
            rStates.forEach { (k, v) ->
                reverseStopStates[k] = try { StopState.valueOf(v) } catch (_: Exception) { StopState.UPCOMING }
            }
        } else {
            isReverseTripActive = false
            isViewingReverseTrip = false
            stopArrivalTimes.clear()
            stopArrivalTimes.putAll(state.stopArrivalTimes)
            stopEtaTexts.clear()
            stopEtaTexts.putAll(state.stopEtaTexts)
            stopStates.clear()
            state.stopStates.forEach { (k, v) ->
                stopStates[k] = try { StopState.valueOf(v) } catch (_: Exception) { StopState.UPCOMING }
            }
        }

        nextGlobalStopIndex = state.nextStopIndex
        if (state.lastKnownLat != 0.0 && state.lastKnownLng != 0.0 && currentLocation == null) {
            currentLocation = Location("recovery").apply {
                latitude = state.lastKnownLat
                longitude = state.lastKnownLng
            }
        }

        updateTripAddresses(route)
        updateBottomSheetInfo()
        persistCurrentActiveTripState()

        Log.d("TripIntegrity", "TRIP_ROUTE_RESTORED: Route ${route.id} (${route.routeName})")
        Log.d("TripIntegrity", "TRIP_STOP_RESTORED: nextStopIndex=$nextGlobalStopIndex, totalStops=${activeStops().size}")
        Log.d("TripIntegrity", "TRIP_DIRECTION_RESTORED: direction=${state.tripDirection}, isMorning=${state.isMorning}")

        Toast.makeText(this, "Resuming Active Trip for ${route.routeName}...", Toast.LENGTH_SHORT).show()
        startNavigationAnimation()
    }

    private fun persistCurrentActiveTripState() {
        if (!isNavigating) return
        val driver = viewModel.currentDriver.value
        val driverId = driver?.driverId ?: return
        val route = lockedActiveRoute ?: assignedRoute ?: return
        val loc = currentLocation
        val busNum = lockedActiveBus ?: binding.tvBusNumberInfo.text.toString().trim()
        val tripId = currentActiveTripId ?: TripRecoveryHelper.generateTripId(
            driverId = driverId,
            busNumber = busNum,
            routeName = route.routeName,
            direction = if (isReverseTripActive) "RETURN" else "FORWARD"
        ).also { currentActiveTripId = it }

        val currentStopIdx = if (isCurrentlyAtStop && lastArrivedStopIndex != -1) lastArrivedStopIndex else nextGlobalStopIndex

        val state = ActiveTripState(
            tripId = tripId,
            roundTripSessionId = currentRoundTripSessionId ?: tripId,
            driverId = driverId,
            routeId = route.id,
            routeName = route.routeName,
            busNumber = busNum,
            tripDirection = if (isReverseTripActive) "RETURN" else "FORWARD",
            isMorning = isActiveTripMorning(),
            isDutyEnabled = isDutyEnabled,
            isNavigating = isNavigating,
            tripStatus = if (isNavigating) "ACTIVE" else "IDLE",
            currentStopIndex = currentStopIdx,
            nextStopIndex = nextGlobalStopIndex,
            stopStates = stopStates.mapValues { it.value.name },
            stopArrivalTimes = stopArrivalTimes.toMap(),
            stopEtaTexts = stopEtaTexts.toMap(),
            isReverseTripActive = isReverseTripActive,
            reverseStopStates = reverseStopStates.mapValues { it.value.name },
            reverseStopArrivalTimes = reverseStopArrivalTimes.toMap(),
            reverseStopEtaTexts = reverseStopEtaTexts.toMap(),
            lastKnownLat = loc?.latitude ?: 0.0,
            lastKnownLng = loc?.longitude ?: 0.0,
            tripStartTime = System.currentTimeMillis(),
            lastUpdated = System.currentTimeMillis()
        )
        TripRecoveryHelper.saveActiveTrip(this, state)
    }

    private fun clearCurrentActiveTripState() {
        val completedTripId = currentActiveTripId
        if (!completedTripId.isNullOrBlank()) {
            TripRecoveryHelper.markTripCompleted(this, completedTripId)
            Log.d("TripIntegrity", "TRIP_COMPLETED: Finished trip $completedTripId")
        }
        currentActiveTripId = null
        currentRoundTripSessionId = null
        lockedActiveRoute = null
        lockedActiveBus = null
        activeTripIsMorning = null
        TripRecoveryHelper.clearActiveTrip(this)
    }

    /** Clears an active recovery session after its configured TripWindow has ended. */
    private fun expireRecoveryForNextTripWindow(expiredTripId: String) {
        if (isNavigating) {
            setNavigationMode(false, clearActiveTrip = false)
        }
        resetClosedTripState(expiredTripId)
        Log.d("TripIntegrity", "TRIP_RECOVERY_EXPIRED: Cleared recovery state after its trip window ended: $expiredTripId")
    }

    /** Completes a finished Return leg so it cannot be restored as an active trip. */
    private fun completeCurrentTrip() {
        val completedTripId = currentActiveTripId.orEmpty()
        if (isNavigating) {
            setNavigationMode(false, clearActiveTrip = true)
        } else {
            clearCurrentActiveTripState()
        }
        resetClosedTripState(completedTripId)
    }

    /** Removes only active/recovery state; completed-trip history remains registered. */
    private fun resetClosedTripState(closedTripId: String) {
        if (closedTripId.isNotBlank()) {
            TripRecoveryHelper.markTripCompleted(this, closedTripId)
        }
        TripRecoveryHelper.clearActiveTrip(this)

        currentActiveTripId = null
        currentRoundTripSessionId = null
        lockedActiveRoute = null
        lockedActiveBus = null
        activeTripIsMorning = null
        isReverseTripActive = false
        isViewingReverseTrip = false
        reverseTripCompleted = false
        reverseStops = emptyList()
        reverseForwardStopIndexes = emptyList()
        forwardMarkerWasVisitedAtReturnStart.clear()
        stopArrivalTimes.clear()
        stopEtaTexts.clear()
        stopStates.clear()
        reverseStopArrivalTimes.clear()
        reverseStopEtaTexts.clear()
        reverseStopStates.clear()
        attendancePromptedStops.clear()
        returnStopDeviations.clear()
        nextGlobalStopIndex = 0
        navStartIndex = 0
        lastArrivedStopIndex = -1
        isCurrentlyAtStop = false
        arrivedStopRouteSegmentIndex = null
        departureCandidateIndex = -1
        departureConfirmCount = 0
        sourceArrivalRecordedForCurrentTrip = false
        currentNavigationEtaText = null
        currentNavPoints = emptyList()
        fullNavigationPoints = emptyList()
        masterRouteChainagePoints = emptyList()
        isTripStartRouteRequestPending = false
        resetFullRouteGpsProgress()
        clearTraveledRouteHistory()

        assignedRoute?.let(::updateTripAddresses)
        updateTripDirectionButton()
        viewModel.currentDriver.value?.driverId?.let { driverId ->
            FirebaseRepository.updateDriverTripDirection(driverId, "FORWARD")
            FirebaseRepository.updateDriverRouteGeometry(
                driverId, null, null, 0, emptyMap(), false, emptyMap(), emptyList()
            )
        }
    }

    /** Update the displayed endpoints whenever forward/return direction changes. */
    private fun updateTripAddresses(route: RouteModel) {
        val source = route.startPoint.ifEmpty { "Main Terminal" }
        val savedDestination = route.endPoint.ifEmpty { "Main Terminal" }
        val forwardDestination = route.stopsList.lastOrNull()?.stopName ?: savedDestination

        if (isReverseTripActive) {
            binding.tvStartAddress.text = cleanDisplayAddress(savedDestination)
            binding.tvEndAddress.text = cleanDisplayAddress(source)
        } else {
            binding.tvStartAddress.text = cleanDisplayAddress(source)
            binding.tvEndAddress.text = cleanDisplayAddress(forwardDestination)
        }
    }

    /** Avoid rendering a duplicated adjacent place name such as "Scheme 3, Scheme 3". */
    private fun cleanDisplayAddress(address: String): String =
        address.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)
            .fold(mutableListOf<String>()) { parts, part ->
                if (parts.none { it.equals(part, ignoreCase = true) }) parts.add(part)
                parts
            }
            .joinToString(", ")
            .ifEmpty { "Main Terminal" }

    private fun drawPointsOnMap(points: List<Point>) {
        if (points.isEmpty()) return

        pointAnnotationManager?.deleteAll()
        val markerStops = if (isReverseTripActive) assignedRoute?.stopsList.orEmpty() else activeStops()
        markerStops.forEachIndexed { index, stop ->
            val isCompleted = if (isReverseTripActive) forwardMarkerWasVisitedAtReturnStart[index] == true
            else stateOf(index) != StopState.UPCOMING || (isNavigating && index < nextGlobalStopIndex)
            val iconRes = if (isCompleted) R.drawable.ic_marker_dest_grey else R.drawable.ic_marker_dest
            addMarker(Point.fromLngLat(stop.longitude, stop.latitude), iconRes, stop.stopName)
        }
        originalRouteSource()?.let { addMarker(it, R.drawable.red_dot, "Source", iconSize = 0.45) }

        polylineAnnotationManager?.deleteAll()
        if (!isNavigating) {
            val polylineOptions = PolylineAnnotationOptions()
                .withPoints(points)
                .withLineColor("#1565C0")
                .withLineWidth(4.0)
                .withLineOpacity(0.8)
            polylineAnnotationManager?.create(polylineOptions)

        }

        if (isNavigating) return

        if (!shouldFitCameraToRoute) return

        val map = mapView ?: return
        if (map.width == 0 || map.height == 0) {
            if (dashboardFitLayoutRetries++ < 40) {
                map.postDelayed({
                    if (!isDestroyed && !isNavigating && shouldFitCameraToRoute) drawPointsOnMap(points)
                }, 50L)
            }
            return
        }
        dashboardFitLayoutRetries = 0

        // Fit the route AND every stop so the whole trip is visible, exactly like the
        val fitPoints = points + markerStops
            .filter { it.latitude != 0.0 && it.longitude != 0.0 }
            .map { Point.fromLngLat(it.longitude, it.latitude) }

        val cameraOptions = if (fitPoints.size == 1) {
            CameraOptions.Builder()
                .center(fitPoints[0])
                .zoom(15.0)
                .pitch(0.0)
                .bearing(0.0)
                .build()
        } else {
            map.mapboxMap.cameraForCoordinates(
                fitPoints,
                EdgeInsets(250.0, 100.0, 150.0, 100.0),
                0.0,
                0.0
            )
        }

        cameraOptions?.let {
            map.viewport.idle()
            if (!hasDoneInitialDashboardFit) {
                // First entry: jump straight to the fitted camera instead of flying
                map.mapboxMap.setCamera(it)
                hasDoneInitialDashboardFit = true
            } else {
                map.camera.flyTo(
                    it,
                    MapAnimationOptions.mapAnimationOptions {
                        duration(1500)
                    }
                )
            }
            shouldFitCameraToRoute = false
        }
    }

    private fun updateNavigationRouteProgress(currentPos: Point) {
        if (fullNavigationPoints.size < 2) return

        try {
            val previousRawPosition = lastRawPositionForSnap
            val maxForwardRouteDistance = previousRawPosition?.let { previous ->
                maxOf(
                    MIN_FORWARD_ROUTE_PROGRESS_METERS,
                    TurfMeasurement.distance(currentPos, previous, TurfConstants.UNIT_METERS) * 3.0 + 30.0
                )
            } ?: MIN_FORWARD_ROUTE_PROGRESS_METERS

            // Pick a point on a *forward* route segment, not merely the closest point
            val projection = projectOntoForwardRoute(currentPos, maxForwardRouteDistance)
            if (projection == null) {
                // On a real return-road deviation there may be no forward segment
                if (isNavigating) {
                    val now = System.currentTimeMillis()
                    val isSettlingAfterReroute = now - lastRerouteCompletedTimeMs < REROUTE_SETTLE_GRACE_MS
                    if (!isSettlingAfterReroute && !isRerouteInFlight &&
                        now - lastOffRouteRerouteTimeMs > MIN_OFFROUTE_REROUTE_GAP_MS
                    ) {
                        lastOffRouteRerouteTimeMs = now
                        triggerReroute()
                    }
                }
                return
            }
            val snappedP = projection.point
            val actualDistanceToRoute = projection.distanceMeters
            val nearestRouteIndex = projection.segmentIndex
            val isFacingOppositeRouteDirection = currentLocation
                ?.takeIf { it.hasBearing() && it.speed >= MIN_SPEED_FOR_BEARING_UPDATE }
                ?.let { location ->
                    val routeBearing = routeBearingAt(nearestRouteIndex)
                    routeBearing != null && headingDifference(location.bearing.toDouble(), routeBearing) >= OPPOSITE_DIRECTION_REROUTE_DEGREES
                } ?: false

            // Navigation's matcher can remain on the old, nearby carriageway for a
            val rawDistanceToMatchedRoute = currentRawLocation?.let { raw ->
                TurfMeasurement.distance(
                    Point.fromLngLat(raw.longitude, raw.latitude),
                    snappedP,
                    TurfConstants.UNIT_METERS
                )
            } ?: 0.0


            val isOffRoute = actualDistanceToRoute > OFF_ROUTE_THRESHOLD_METERS ||
                    (rawDistanceToMatchedRoute > PARALLEL_ROAD_OFF_ROUTE_THRESHOLD_METERS &&
                            isFacingOppositeRouteDirection)
            if (isNavigating && isOffRoute) {
                val now = System.currentTimeMillis()
                val isSettlingAfterReroute = now - lastRerouteCompletedTimeMs < REROUTE_SETTLE_GRACE_MS
                if (!isSettlingAfterReroute && !isRerouteInFlight &&
                    now - lastOffRouteRerouteTimeMs > MIN_OFFROUTE_REROUTE_GAP_MS
                ) {
                    lastOffRouteRerouteTimeMs = now
                    Log.d("NavDebug", "Bus deviated from route ($actualDistanceToRoute m away). Triggering immediate reroute...")
                    triggerReroute()
                }
                return
            }

            // 2. Minimum movement filter: only filters updates when vehicle is strictly ON ROUTE
            previousRawPosition?.let { lastRaw ->
                val movedMeters = TurfMeasurement.distance(currentPos, lastRaw, TurfConstants.UNIT_METERS)
                if (movedMeters < MIN_GPS_MOVEMENT_FOR_SNAP_METERS) {
                    return
                }
            }
            lastRawPositionForSnap = currentPos

            // The projection and split now always refer to the same road segment.
            val splitIndex = maxOf(projection.segmentIndex, lastSplitIndex)
            lastSplitIndex = splitIndex

            // 4. Road-Following Traveled Line:
            val currentLegTraveled = mutableListOf<Point>()
            if (fullNavigationPoints.isNotEmpty()) {
                val endIdx = minOf(splitIndex + 1, fullNavigationPoints.size)
                currentLegTraveled.addAll(fullNavigationPoints.subList(0, endIdx))
                if (currentLegTraveled.isEmpty() || currentLegTraveled.last() != snappedP) {
                    currentLegTraveled.add(snappedP)
                }
            }

            activeTraveledSegment = currentLegTraveled
            val traveledSegments = currentTraveledSegments()

            // 5. Active Upcoming Route: starts seamlessly from snappedP (on road at bus) to destination
            val upcomingPoints = mutableListOf<Point>()
            upcomingPoints.add(snappedP)
            if (splitIndex + 1 < fullNavigationPoints.size) {
                upcomingPoints.addAll(fullNavigationPoints.subList(splitIndex + 1, fullNavigationPoints.size))
            }

            mapView?.mapboxMap?.getStyle { style ->
                var traveledPolyline: String? = null
                if (traveledSegments.isNotEmpty()) {
                    val traveledGeometry = if (traveledSegments.size == 1) {
                        LineString.fromLngLats(traveledSegments.first())
                    } else {
                        MultiLineString.fromLineStrings(traveledSegments.map { LineString.fromLngLats(it) })
                    }
                    (style.getSource(NAV_TRAVELED_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                        ?.geometry(traveledGeometry)
                    // Kept for old app versions; new versions consume all independent segments.
                    traveledPolyline = LineString.fromLngLats(traveledSegments.last()).toPolyline(6)
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

    private data class RouteProjection(
        val point: Point,
        val segmentIndex: Int,
        val distanceMeters: Double
    )

    private data class GpsRoutePosition(
        val progressMeters: Double,
        val segmentIndex: Int,
        val crossTrackMeters: Double
    )

    private data class GpsRouteProgressFix(
        val position: GpsRoutePosition?,
        val isNewFix: Boolean
    )

    private fun gpsFixToken(location: Location): Long? = when {
        location.elapsedRealtimeNanos > 0L -> location.elapsedRealtimeNanos
        location.time > 0L -> location.time * 1_000_000L
        else -> null
    }

    /**
     * Match one unique GPS fix against the complete active route and express its
     * position as cumulative route metres. Unlike projectOntoForwardRoute(), this
     * matching path is not constrained by lastSplitIndex or the Mapbox leg cursor.
     * Forward bounds, accuracy, cross-track distance and travel bearing prevent a
     * crossing, U-turn, parallel road or isolated noisy fix from claiming progress.
     */
    private fun fullRoutePositionForGps(location: Location, fixToken: Long): GpsRouteProgressFix {
        if (fixToken == lastFullRouteGpsFixToken) {
            return GpsRouteProgressFix(lastFullRouteGpsFixResult, false)
        }
        // Measured against masterRouteChainagePoints (stable for the whole trip), never
        val chainagePoints = masterRouteChainagePoints
        if (chainagePoints.size < 2) {
            lastFullRouteGpsFixToken = fixToken
            lastFullRouteGpsFixResult = null
            return GpsRouteProgressFix(null, true)
        }

        val cumulativeMeters = DoubleArray(chainagePoints.size)
        for (segmentIndex in 0 until chainagePoints.lastIndex) {
            cumulativeMeters[segmentIndex + 1] = cumulativeMeters[segmentIndex] + TurfMeasurement.distance(
                chainagePoints[segmentIndex], chainagePoints[segmentIndex + 1], TurfConstants.UNIT_METERS
            )
        }

        val gpsPoint = Point.fromLngLat(location.longitude, location.latitude)
        val previousPosition = lastFullRouteGpsPosition
        val previousLocation = lastFullRouteGpsLocation
        val straightMovementMeters = previousLocation?.distanceTo(location)?.toDouble()
        val maxForwardMeters = straightMovementMeters?.let { maxOf(80.0, it * 4.0 + 40.0) } ?: Double.MAX_VALUE
        val gpsBearing = location.takeIf {
            it.hasBearing() && it.speed >= MIN_SPEED_FOR_BEARING_UPDATE
        }?.bearing?.toDouble()
        var best: GpsRoutePosition? = null
        var bestDistanceMeters = Double.MAX_VALUE
        var bestProgressDelta = Double.MAX_VALUE

        for (segmentIndex in 0 until chainagePoints.lastIndex) {
            val start = chainagePoints[segmentIndex]
            val end = chainagePoints[segmentIndex + 1]
            val routeBearing = routeBearingAt(segmentIndex, chainagePoints)
            if (gpsBearing != null && routeBearing != null &&
                headingDifference(gpsBearing, routeBearing) >= OPPOSITE_DIRECTION_REROUTE_DEGREES
            ) continue

            val projected = projectPointOntoSegment(gpsPoint, start, end)
            val crossTrackMeters = TurfMeasurement.distance(gpsPoint, projected, TurfConstants.UNIT_METERS)
            val segmentProgress = cumulativeMeters[segmentIndex] +
                    TurfMeasurement.distance(start, projected, TurfConstants.UNIT_METERS)
            if (previousPosition != null &&
                (segmentProgress < previousPosition.progressMeters - 12.0 ||
                        segmentProgress > previousPosition.progressMeters + maxForwardMeters)
            ) continue

            val progressDelta = if (previousPosition == null) segmentProgress
            else kotlin.math.abs(segmentProgress - (previousPosition.progressMeters + (straightMovementMeters ?: 0.0)))
            if (crossTrackMeters < bestDistanceMeters - 3.0 ||
                (kotlin.math.abs(crossTrackMeters - bestDistanceMeters) <= 3.0 && progressDelta < bestProgressDelta)
            ) {
                bestDistanceMeters = crossTrackMeters
                bestProgressDelta = progressDelta
                best = GpsRoutePosition(
                    progressMeters = maxOf(previousPosition?.progressMeters ?: 0.0, segmentProgress),
                    segmentIndex = segmentIndex,
                    crossTrackMeters = crossTrackMeters
                )
            }
        }

        val accuracyAllowance = if (location.hasAccuracy()) (location.accuracy * 1.25).toDouble() else 45.0
        val maxCrossTrack = maxOf(45.0, minOf(MISSED_STOP_MAX_ROUTE_SNAP_METERS, accuracyAllowance))
        val acceptedPosition = best?.takeIf { it.crossTrackMeters <= maxCrossTrack }
        lastFullRouteGpsFixToken = fixToken
        lastFullRouteGpsFixResult = acceptedPosition
        if (acceptedPosition != null) {
            lastFullRouteGpsPosition = acceptedPosition
            lastFullRouteGpsLocation = Location(location)
        }
        return GpsRouteProgressFix(acceptedPosition, true)
    }

    /** Project an ordered stop onto full-route chainage, independent of bus proximity. */
    private fun routeProgressForStop(stopPoint: Point, minProgressMeters: Double = 0.0): GpsRoutePosition? {
        // Same stable masterRouteChainagePoints reference as fullRoutePositionForGps():
        val chainagePoints = masterRouteChainagePoints
        if (chainagePoints.size < 2) return null
        var cumulativeMeters = 0.0
        var best: GpsRoutePosition? = null
        for (segmentIndex in 0 until chainagePoints.lastIndex) {
            val start = chainagePoints[segmentIndex]
            val end = chainagePoints[segmentIndex + 1]
            val projected = projectPointOntoSegment(stopPoint, start, end)
            val crossTrackMeters = TurfMeasurement.distance(stopPoint, projected, TurfConstants.UNIT_METERS)
            val segmentProgress = cumulativeMeters + TurfMeasurement.distance(start, projected, TurfConstants.UNIT_METERS)
            if (segmentProgress >= minProgressMeters &&
                (best == null || crossTrackMeters < best.crossTrackMeters)
            ) {
                best = GpsRoutePosition(segmentProgress, segmentIndex, crossTrackMeters)
            }
            cumulativeMeters += TurfMeasurement.distance(start, end, TurfConstants.UNIT_METERS)
        }
        return best?.takeIf { it.crossTrackMeters <= 300.0 }
    }

    /** Resolve a stop in route order so loops/crossings do not select an earlier pass. */
    private fun routeProgressForStopIndex(stopIndex: Int): GpsRoutePosition? {
        val stops = activeStops()
        if (stopIndex !in stops.indices) return null
        var minimumProgressMeters = 0.0
        var selected: GpsRoutePosition? = null
        for (index in 0..stopIndex) {
            // Keep return-trip progress tied to its own route.
            if (stateOf(index) == StopState.SKIPPED) continue
            val stop = stops[index]
            val point = Point.fromLngLat(stop.longitude, stop.latitude)
            val resolved = routeProgressForStop(point, minimumProgressMeters) ?: routeProgressForStop(point)
            ?: return null
            selected = resolved
            minimumProgressMeters = resolved.progressMeters + 1.0
        }
        return selected
    }

    private fun resetFullRouteGpsProgress() {
        returnSourceConfirmCount = 0
        returnSourceConfirmFixToken = Long.MIN_VALUE
        lastFullRouteGpsFixToken = Long.MIN_VALUE
        lastFullRouteGpsFixResult = null
        lastFullRouteGpsPosition = null
        lastFullRouteGpsLocation = null
        missedStopCandidateIndex = -1
        missedStopConfirmCount = 0
        missedStopFirstForwardProgressMeters = Double.NaN
        missedStopLastForwardProgressMeters = Double.NaN
        lastCountedMissedStopFixToken = Long.MIN_VALUE
    }

    /**
     * Establish (or re-establish) the stable chainage reference used for missed-stop
     * detection. Deliberately a no-op whenever masterRouteChainagePoints is already
     * populated: a routine reroute must never replace it, or the skip-detection engine
     * would be right back to measuring progress against a moving, stop-seeking target.
     * It is only cleared - and therefore eligible to be re-seeded here - at genuine
     * trip boundaries: a fresh forward/return trip start, a route reassignment, or a
     * full navigation stop/restart. See clearCurrentActiveTripState(), setNavigationMode()
     * and the route-reassignment block in the driver observer for those reset points.
     */
    private fun ensureMasterRouteChainage(points: List<Point>) {
        if (masterRouteChainagePoints.size >= 2 || points.size < 2) return
        masterRouteChainagePoints = points
        resetFullRouteGpsProgress()
        Log.i("STOP_PROGRESS", "masterRouteChainage established points=${points.size} " +
                "nextGlobalStopIndex=$nextGlobalStopIndex")
    }

    /**
     * Projects GPS onto the reachable, forward portion of the route.  Restricting the
     * search both by route order and travelled distance makes progress monotonic and
     * prevents GPS jitter near a crossing from selecting an earlier route section.
     */
    private fun projectOntoForwardRoute(currentPos: Point, maxForwardMeters: Double): RouteProjection? {
        val firstSegment = lastSplitIndex.coerceIn(0, fullNavigationPoints.lastIndex - 1)
        val lastSegment = minOf(fullNavigationPoints.lastIndex - 1, firstSegment + SPLIT_SEARCH_WINDOW)
        var routeDistance = 0.0
        var best: RouteProjection? = null

        for (segmentIndex in firstSegment..lastSegment) {
            val start = fullNavigationPoints[segmentIndex]
            val end = fullNavigationPoints[segmentIndex + 1]
            if (segmentIndex > firstSegment) {
                routeDistance += TurfMeasurement.distance(
                    fullNavigationPoints[segmentIndex - 1], start, TurfConstants.UNIT_METERS
                )
            }
            if (routeDistance > maxForwardMeters) break

            val routeBearing = routeBearingAt(segmentIndex)
            val busBearing = currentLocation
                ?.takeIf { it.speed >= MIN_SPEED_FOR_BEARING_UPDATE }
                ?.let { if (it.hasBearing()) it.bearing.toDouble() else lastValidBearing }
                ?: lastValidBearing.takeIf { it != 0.0 }
            // On a divided/two-way road, ignore a nearby segment that points in the
            if (busBearing != null && routeBearing != null &&
                headingDifference(busBearing, routeBearing) >= OPPOSITE_DIRECTION_REROUTE_DEGREES
            ) continue

            val projected = projectPointOntoSegment(currentPos, start, end)
            val distance = TurfMeasurement.distance(currentPos, projected, TurfConstants.UNIT_METERS)
            if (best == null || distance < best.distanceMeters) {
                best = RouteProjection(projected, segmentIndex, distance)
            }
        }
        return best
    }

    private fun projectPointOntoSegment(point: Point, start: Point, end: Point): Point {
        // Routes cover small geographic areas, so an equirectangular projection gives
        val latitudeScale = 111_320.0
        val longitudeScale = latitudeScale * kotlin.math.cos(Math.toRadians((start.latitude() + end.latitude()) / 2.0))
        val px = (point.longitude() - start.longitude()) * longitudeScale
        val py = (point.latitude() - start.latitude()) * latitudeScale
        val dx = (end.longitude() - start.longitude()) * longitudeScale
        val dy = (end.latitude() - start.latitude()) * latitudeScale
        val denominator = dx * dx + dy * dy
        val t = if (denominator == 0.0) 0.0 else ((px * dx + py * dy) / denominator).coerceIn(0.0, 1.0)
        return Point.fromLngLat(
            start.longitude() + (end.longitude() - start.longitude()) * t,
            start.latitude() + (end.latitude() - start.latitude()) * t
        )
    }

    private fun routeBearingAt(index: Int, points: List<Point> = fullNavigationPoints): Double? {
        val (startIndex, endIndex) = if (index < points.lastIndex) {
            index to index + 1
        } else {
            index - 1 to index
        }
        val start = points.getOrNull(startIndex) ?: return null
        val end = points.getOrNull(endIndex) ?: return null
        val lat1 = Math.toRadians(start.latitude())
        val lat2 = Math.toRadians(end.latitude())
        val deltaLongitude = Math.toRadians(end.longitude() - start.longitude())
        val y = Math.sin(deltaLongitude) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) -
                Math.sin(lat1) * Math.cos(lat2) * Math.cos(deltaLongitude)
        return (Math.toDegrees(Math.atan2(y, x)) + 360.0) % 360.0
    }

    private fun headingDifference(first: Double, second: Double): Double {
        val difference = kotlin.math.abs(first - second) % 360.0
        return minOf(difference, 360.0 - difference)
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

    private fun recreateAnnotationManagers() {
        pointAnnotationManager?.let { mapView?.annotations?.removeAnnotationManager(it) }
        polylineAnnotationManager?.let { mapView?.annotations?.removeAnnotationManager(it) }
        polylineAnnotationManager = mapView?.annotations?.createPolylineAnnotationManager()
        pointAnnotationManager = mapView?.annotations?.createPointAnnotationManager()
    }

    private fun clearNavigationRouteGeometry(style: Style) {
        val empty = LineString.fromLngLats(emptyList())
        (style.getSource(NAV_ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
            ?.geometry(empty)
        (style.getSource(NAV_TRAVELED_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
            ?.geometry(empty)
    }

    /** Restores route sources after a style reload without requiring a GPS update. */
    private fun restoreNavigationRouteGeometry(style: Style) {
        if (fullNavigationPoints.size < 2) return
        (style.getSource(NAV_ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
            ?.geometry(LineString.fromLngLats(fullNavigationPoints))

        val traveledSegments = currentTraveledSegments()
        if (traveledSegments.isNotEmpty()) {
            val traveledGeometry = if (traveledSegments.size == 1) {
                LineString.fromLngLats(traveledSegments.first())
            } else {
                MultiLineString.fromLineStrings(traveledSegments.map { LineString.fromLngLats(it) })
            }
            (style.getSource(NAV_TRAVELED_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                ?.geometry(traveledGeometry)
        }
    }

    /**
     * Driver assignments in existing Firestore data may contain a route name,
     * route code, route document id, or assigned bus number.  The dashboard model
     * already accepts those forms; restoring the map must use the same matching
     * rules instead of only comparing the display name.
     */
    private fun resolveAssignedRoute(data: com.example.bustrack_app.models.DriverDashboardModel): RouteModel? {
        val driver = viewModel.currentDriver.value
        val busNo = driver?.assignedBus?.takeIf { it.isNotBlank() && !it.equals("Not Assigned", true) } ?: return null
        return RouteRepository.routeList.value?.find { it.busNo.equals(busNo, ignoreCase = true) }
    }

    private fun setupClickListeners() {
        binding.btnMenuDrawer.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            drawerLayout.openDrawer(GravityCompat.END)
        }
        binding.btnNavigationDrawer.setOnClickListener {
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

        // Search is not available on this map yet.

        binding.btnSound.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            isVoiceEnabled = !isVoiceEnabled
            getSharedPreferences("navigation_preferences", MODE_PRIVATE)
                .edit()
                .putBoolean("voice_enabled", isVoiceEnabled)
                .apply()
            binding.btnSound.setImageResource(if (isVoiceEnabled) R.drawable.volume_up else R.drawable.mute)
            binding.btnSound.imageTintList = ColorStateList.valueOf(Color.WHITE)
            if (!isVoiceEnabled) {
                speechApi?.cancel()
                voiceInstructionsPlayer?.clear()
                fallbackTextToSpeech?.stop()
                abandonNavigationAudioFocus()
            }
            Toast.makeText(this, if (isVoiceEnabled) "Voice instructions ON" else "Voice instructions OFF", Toast.LENGTH_SHORT).show()
        }

        binding.btnRecenter.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            Log.d("CAMERA_DEBUG", "Dashboard manual RECENTER tapped: isCameraFollowingBusBefore=$isCameraFollowingBus " +
                    "isNavigating=$isNavigating")
            startFollowingPuck()
        }

        binding.btnAlert.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            showAlertsBottomSheet()
        }

        binding.btnStartNavigation.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (isNavigating) {
                Toast.makeText(this, "An active trip is already in progress.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val route = assignedRoute

            val currentDriver = viewModel.currentDriver.value
            val busNo = currentDriver?.assignedBus?.takeIf { it.isNotBlank() && !it.equals("Not Assigned", true) }
            if (busNo == null) {
                assignedRoute = null
                binding.tvRouteNameInfo.text = "No Bus Assigned"
                updateNavigationButtonState()
                Toast.makeText(this, "No bus or route has been assigned to you. Please contact the college admin.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            val latestRoute = RouteRepository.routeList.value?.find { it.busNo.equals(busNo, true) }
            if (latestRoute == null) {
                assignedRoute = null
                binding.tvRouteNameInfo.text = "No Route Assigned"
                updateNavigationButtonState()
                Toast.makeText(this, "No route has been assigned to your bus. Please contact the college admin.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (!isDutyEnabled) {
                drawerLayout.openDrawer(GravityCompat.END)
                Toast.makeText(this, "Please enable On Duty Mode before starting navigation.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            assignedRoute = latestRoute

            if (!hasPreciseLocationPermission()) {
                requestNavigationStartLocation(latestRoute)
                return@setOnClickListener
            }

            if (!hasFreshCurrentLocation()) {
                Toast.makeText(this, "Fetching current location...", Toast.LENGTH_SHORT).show()
                requestNavigationStartLocation(latestRoute)
                return@setOnClickListener
            }

            handleStartNavigation(latestRoute)
        }

        binding.bottomSummaryCard.findViewById<View>(R.id.btnCloseNav)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            // Keep the active trip and its stop state so Start can resume it, but
            setNavigationMode(false, clearActiveTrip = false)
        }

        binding.bottomSummaryCard.findViewById<View>(R.id.btnViewRoute)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startFollowingPuck()
        }

        updateTripDirectionButton()

        binding.btnNotifications.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    private fun handleStartNavigation(route: RouteModel) {
        if (isNavigating) {
            Toast.makeText(this, "An active trip is already in progress.", Toast.LENGTH_SHORT).show()
            return
        }
        val isResumingTrip = currentActiveTripId != null

        if (isResumingTrip) {
            val location = currentLocation
            if (isRecoveryOutsideCurrentWindow()) {
                expireRecoveryForNextTripWindow(currentActiveTripId.orEmpty())
                handleStartNavigation(route)
                return
            }
            if (isReturnCompleted(route)) {
                completeCurrentTrip()
                Toast.makeText(this, "Current trip is already completed.", Toast.LENGTH_LONG).show()
                return
            }
            if (isForwardCompleted(route)) {
                when {
                    activeTripIsMorning == true && currentTripPeriod() == TripPeriod.MORNING && isNearLastStop(route, location) -> {
                        beginReverseTrip()
                    }
                    else -> Toast.makeText(
                        this,
                        "Completed trip is waiting for return at the last stop. It cannot be replaced from this location or time window.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                return
            }
            if (location == null || !isOnOrNearAssignedRoute(route, location)) {
                Toast.makeText(this, "Move back to your assigned route or a route stop to resume navigation.", Toast.LENGTH_LONG).show()
                return
            }
            lockedActiveRoute = route
            assignedRoute = route
            updateBottomSheetInfo()
            startNavigationAnimation()
            return
        }

        if (currentTripPeriod() == TripPeriod.GAP) {
            Toast.makeText(this, "Navigation can start only during a configured trip window.", Toast.LENGTH_LONG).show()
            return
        }

        if (isCurrentTripWindowCompleted(route)) {
            Toast.makeText(this, "Current trip is already completed.", Toast.LENGTH_LONG).show()
            return
        }

        // A genuinely new trip must begin inside the source geofence.
        val currentPoint = currentLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }
        val startPoint = route.pathPoints.firstOrNull()?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: route.stopsList.firstOrNull()?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        }
        val isAtSource = currentPoint != null && startPoint != null &&
                TurfMeasurement.distance(currentPoint, startPoint, TurfConstants.UNIT_METERS) <= RESUME_ROUTE_VALIDATION_RADIUS_METERS
        if (!isAtSource) {
            showStartPointError(route)
            return
        }

        val driverId = viewModel.currentDriver.value?.driverId ?: ""
        val busNum = binding.tvBusNumberInfo.text.toString().trim().ifEmpty { viewModel.currentDriver.value?.assignedBus.orEmpty() }
        lockedActiveBus = busNum
        lockedActiveRoute = route
        assignedRoute = route
        currentActiveTripId = TripRecoveryHelper.generateTripId(
            driverId = driverId,
            busNumber = busNum,
            routeName = route.routeName,
            direction = if (isReverseTripActive) "RETURN" else "FORWARD",
            sessionPeriod = currentTripPeriod().name
        )
        currentRoundTripSessionId = currentActiveTripId
        // A new forward navigation session gets one source-arrival/drop event.
        if (!isReverseTripActive) {
            activeTripIsMorning = isMorningTrip()
            sourceArrivalRecordedForCurrentTrip = false
            viewModel.currentDriver.value?.driverId?.let {
                FirebaseRepository.updateDriverTripDirection(it, "FORWARD")
            }
        }
        Log.d("TripIntegrity", "TRIP_START: driver=$driverId, bus=$busNum, route=${route.routeName}, direction=${if (isReverseTripActive) "RETURN" else "FORWARD"}, tripId=$currentActiveTripId, isMorning=$activeTripIsMorning")
        isNearStart = true
        updateBottomSheetInfo()
        startNavigationAnimation()
    }

    private fun beginReverseTrip() {
        val forwardStops = assignedRoute?.stopsList.orEmpty()
        val location = currentLocation
        if (forwardStops.isEmpty() || location == null) {
            Toast.makeText(this, "Current location and route stops are required for reverse trip", Toast.LENGTH_SHORT).show()
            return
        }
        originalRouteSource()?.let { source ->
            val distanceToSource = FloatArray(1)
            Location.distanceBetween(
                location.latitude, location.longitude,
                source.latitude(), source.longitude(), distanceToSource
            )
            // Same/nearby origin and destination are not a valid driving route.
            if (distanceToSource[0] <= ARRIVAL_RADIUS) {
                Toast.makeText(this, "Bus is already at the source; no return route is needed", Toast.LENGTH_LONG).show()
                return
            }
        }
        val driverId = viewModel.currentDriver.value?.driverId ?: ""
        val busNum = lockedActiveBus ?: binding.tvBusNumberInfo.text.toString().trim().ifEmpty { viewModel.currentDriver.value?.assignedBus.orEmpty() }
        lockedActiveBus = busNum
        lockedActiveRoute = assignedRoute
        currentActiveTripId = TripRecoveryHelper.generateTripId(
            driverId = driverId,
            busNumber = busNum,
            routeName = assignedRoute?.routeName ?: "",
            direction = "RETURN",
            sessionPeriod = if (activeTripIsMorning == true) "MORNING" else "EVENING"
        )
        currentRoundTripSessionId = currentRoundTripSessionId ?: currentActiveTripId
        // Copying is important: the adapter updates StopItem.time, so reusing the
        reverseForwardStopIndexes = forwardStops.indices.reversed().toList()
        reverseStops = reverseForwardStopIndexes.map { forwardStops[it].copy(time = "TBD") }
        reverseStopArrivalTimes.clear()
        reverseStopStates.clear()
        reverseStopEtaTexts.clear()
        forwardMarkerWasVisitedAtReturnStart.clear()
        forwardStops.forEachIndexed { index, _ ->
            forwardMarkerWasVisitedAtReturnStart[index] =
                (stopStates[index] ?: StopState.UPCOMING) != StopState.UPCOMING ||
                        (isNavigating && index < nextGlobalStopIndex)
        }
        // A forward UPCOMING stop was never visited. Retain it in the return list as
        reverseForwardStopIndexes.forEachIndexed { reverseIndex, forwardIndex ->
            if ((stopStates[forwardIndex] ?: StopState.UPCOMING) == StopState.UPCOMING) {
                reverseStopStates[reverseIndex] = StopState.SKIPPED
                reverseStopArrivalTimes[reverseIndex] = "Skipped"
            }
        }
        voiceSessionId++
        speechApi?.cancel()
        voiceInstructionsPlayer?.clear()
        fallbackTextToSpeech?.stop()
        activeFallbackUtteranceId = null
        isReverseTripActive = true
        isViewingReverseTrip = true
        masterRouteChainagePoints = emptyList()
        resetFullRouteGpsProgress()
        // Set here, not only inside startNavigationAnimation(), so it is already true
        isTripStartRouteRequestPending = true
        // Return remains in the period selected when the forward leg started.
        viewModel.currentDriver.value?.driverId?.let {
            FirebaseRepository.updateDriverTripDirection(it, "RETURN")
        }
        reverseTripCompleted = false
        nextGlobalStopIndex = 0
        navStartIndex = 0
        lastArrivedStopIndex = -1
        isCurrentlyAtStop = false
        arrivedStopRouteSegmentIndex = null
        departureCandidateIndex = -1
        departureConfirmCount = 0
        returnStopDeviations.clear()
        attendancePromptedStops.clear()
        sourceArrivalRecordedForCurrentTrip = false
        currentNavigationEtaText = null
        clearTraveledRouteHistory()
        assignedRoute?.let(::updateTripAddresses)
        updateBottomSheetInfo()
        persistCurrentActiveTripState()
        Log.d("TripIntegrity", "TRIP_START (RETURN): driver=$driverId, bus=$busNum, route=${assignedRoute?.routeName}, tripId=$currentActiveTripId")
        startNavigationAnimation()
        Toast.makeText(this, "Return trip started", Toast.LENGTH_SHORT).show()
    }

    private fun showStartReturnTripConfirmation() {
        if (isReverseTripActive) {
            Toast.makeText(this, "Return trip is already active", Toast.LENGTH_SHORT).show()
            return
        }
        val dialogView = layoutInflater.inflate(R.layout.dialog_start_return_trip, null)
        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogView)
            .create()

        dialogView.findViewById<MaterialButton>(R.id.btnCancelReturnTrip).setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<MaterialButton>(R.id.btnConfirmReturnTrip).setOnClickListener {
            dialog.dismiss()
            beginReverseTrip()
        }

        dialog.show()
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    private fun ensureReverseTripStops() {
        if (!isReverseTripActive || reverseStops.isNotEmpty()) return
        val forwardStops = assignedRoute?.stopsList.orEmpty()
        reverseForwardStopIndexes = forwardStops.indices.reversed().toList()
        reverseStops = reverseForwardStopIndexes.map { forwardStops[it].copy(time = "TBD") }
    }

    /** The bottom-sheet action always offers the opposite of the active trip. */
    private fun updateTripDirectionButton() {
        val button = binding.bottomSummaryCard.findViewById<MaterialButton>(R.id.btnStartReturnTrip) ?: return
        button.text = if (isReverseTripActive) "Start Forward Trip" else "Start Return Trip"
        button.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (isReverseTripActive) showStartForwardTripConfirmation() else showStartReturnTripConfirmation()
        }
    }

    private fun showStartForwardTripConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Start Forward Trip?")
            .setMessage("Are you sure you want to switch back to the forward trip?")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Start Forward") { _, _ -> beginForwardTrip() }
            .show()
    }

    private fun beginForwardTrip() {
        if (!isReverseTripActive) return

        val driverId = viewModel.currentDriver.value?.driverId ?: ""
        val busNum = lockedActiveBus ?: binding.tvBusNumberInfo.text.toString().trim().ifEmpty { viewModel.currentDriver.value?.assignedBus.orEmpty() }
        lockedActiveBus = busNum
        lockedActiveRoute = assignedRoute
        currentActiveTripId = TripRecoveryHelper.generateTripId(
            driverId = driverId,
            busNumber = busNum,
            routeName = assignedRoute?.routeName ?: "",
            direction = "FORWARD",
            sessionPeriod = currentTripPeriod().name
        )
        currentRoundTripSessionId = currentActiveTripId
        voiceSessionId++
        speechApi?.cancel()
        voiceInstructionsPlayer?.clear()
        fallbackTextToSpeech?.stop()
        activeFallbackUtteranceId = null
        activeTripIsMorning = isMorningTrip()
        isReverseTripActive = false
        isViewingReverseTrip = false
        masterRouteChainagePoints = emptyList()
        resetFullRouteGpsProgress()
        // See beginReverseTrip(): closes the same stale-Firestore-adoption race for
        isTripStartRouteRequestPending = true
        reverseTripCompleted = false
        nextGlobalStopIndex = 0
        navStartIndex = 0
        lastArrivedStopIndex = -1
        isCurrentlyAtStop = false
        arrivedStopRouteSegmentIndex = null
        departureCandidateIndex = -1
        departureConfirmCount = 0
        returnStopDeviations.clear()
        sourceArrivalRecordedForCurrentTrip = false
        currentNavigationEtaText = null
        clearTraveledRouteHistory()
        assignedRoute?.let(::updateTripAddresses)
        persistCurrentActiveTripState()
        Log.d("TripIntegrity", "TRIP_START (FORWARD): driver=$driverId, bus=$busNum, route=${assignedRoute?.routeName}, tripId=$currentActiveTripId")

        viewModel.currentDriver.value?.driverId?.let {
            FirebaseRepository.updateDriverTripDirection(it, "FORWARD")
        }
        updateBottomSheetInfo()
        startNavigationAnimation()
        Toast.makeText(this, "Forward trip started", Toast.LENGTH_SHORT).show()
    }

    private fun setupStopsRecyclerView() {
        bottomSheetBehavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(binding.bottomSummaryCard)
        bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
        // This shared bottom-sheet layout is also used by the Admin/Parent/
        binding.bottomSummaryCard.findViewById<View>(R.id.tvLastSyncedSheet)?.visibility = View.INVISIBLE

        stopsAdapter = com.example.bustrack_app.adapter.NavigationStopsAdapter(emptyList())
        val rvStops = binding.bottomSummaryCard.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rvUpcomingStops)
        rvStops?.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        rvStops?.adapter = stopsAdapter

        binding.bottomSummaryCard.findViewById<View>(R.id.btnViewAllStops)?.setOnClickListener {
            if (::bottomSheetBehavior.isInitialized) {
                val nextState = if (bottomSheetBehavior.state == com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED) {
                    com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
                } else {
                    com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
                }
                bottomSheetBehavior.state = nextState
            }
        }
    }

    private fun setupMapGestures() {
        mapView?.gestures?.addOnMoveListener(object : OnMoveListener {
            override fun onMoveBegin(detector: MoveGestureDetector) {
                Log.d("CAMERA_DEBUG", "Dashboard onMoveBegin fired: isNavigating=$isNavigating " +
                        "isCameraFollowingBusBefore=$isCameraFollowingBus lifecycle=onMoveBegin")
                panStartX = detector.focalPoint.x
                panStartY = detector.focalPoint.y
                panPausedFollow = false
                // While following, do not pause on the first touch-move event: it also fires
                if (!(isNavigating && isCameraFollowingBus) &&
                    binding.bottomSummaryCard.visibility == View.VISIBLE) {
                    binding.btnRecenter.visibility = View.VISIBLE
                }
            }
            override fun onMove(detector: MoveGestureDetector): Boolean {
                if (panPausedFollow || !isNavigating || !isCameraFollowingBus) return false
                val thresholdPx = USER_PAN_PAUSE_FOLLOW_DP * resources.displayMetrics.density
                val dx = detector.focalPoint.x - panStartX
                val dy = detector.focalPoint.y - panStartY
                if (dx * dx + dy * dy >= thresholdPx * thresholdPx) {
                    // Driver deliberately panned: pause follow until Re-centre is tapped.
                    panPausedFollow = true
                    isCameraFollowingBus = false
                    mapView?.viewport?.idle()
                    Log.d("CAMERA_DEBUG", "Follow paused by user pan: distancePx=${Math.hypot(dx.toDouble(), dy.toDouble())}")
                    if (binding.bottomSummaryCard.visibility == View.VISIBLE) {
                        binding.btnRecenter.visibility = View.VISIBLE
                    }
                }
                return false
            }
            override fun onMoveEnd(detector: MoveGestureDetector) {
                panPausedFollow = false
            }
        })
    }

    private fun updateBottomSheetInfo() {
        val route = assignedRoute ?: return
        val email = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase()
        val driver = DriverRepository.driverList.value?.find { it.email.trim().lowercase() == email }

        val sheet = binding.bottomSummaryCard
        updateTripDirectionButton()
        sheet.findViewById<TextView>(R.id.tvBusIdSheet)?.text = route.busNo.ifEmpty { "BUS-TRACK" }
        sheet.findViewById<TextView>(R.id.tvRouteSheet)?.text = when {
            isReverseTripActive && isViewingReverseTrip -> "${route.routeName} • Reverse Trip"
            isReverseTripActive -> "${route.routeName} • Forward Trip"
            else -> route.routeName
        }
        sheet.findViewById<TextView>(R.id.tvUpcomingLabel)?.text = when {
            isReverseTripActive && isViewingReverseTrip -> "Return Trip Stops"
            isReverseTripActive -> "Forward Trip Stops"
            else -> "Upcoming Stops"
        }
        sheet.findViewById<TextView>(R.id.tvDriverNameSheet)?.text = driver?.name ?: "Driver"

        val tvEta = sheet.findViewById<TextView>(R.id.tvEtaSheet)
        val tvSpeed = sheet.findViewById<TextView>(R.id.tvSpeedSheet)
        val tvLoad = sheet.findViewById<TextView>(R.id.tvLoadSheet)

        val etaText = currentNavigationEtaText ?: "Calculating..."
        tvEta?.text = etaText
        binding.tvEtaNav.text = if (etaText.startsWith("Arrived:") || etaText == "Route completed") {
            etaText
        } else {
            "ETA: $etaText"
        }

        val currentSpeed = (currentLocation?.speed?.times(3.6)) ?: 0.0
        tvSpeed?.text = "${currentSpeed.toInt()} km/h"
        binding.tvSpeedNav.text = currentSpeed.toInt().toString()
        currentLocation?.let(::updateLocationSummary)

        tvLoad?.text = cachedLoadString
        refreshLoadStat()

        val stops = displayedStops()
        val arrivalTimes = if (isReverseTripActive && isViewingReverseTrip) reverseStopArrivalTimes else stopArrivalTimes
        val etaTexts = if (isReverseTripActive && isViewingReverseTrip) reverseStopEtaTexts else stopEtaTexts
        stops.forEachIndexed { index, stop ->
            val arrival = arrivalTimes[index]
            if (arrival == "Skipped") {
                stop.time = if (isReverseTripActive && isViewingReverseTrip) "NOT VISITED" else "Skipped"
            } else if (arrival != null) {
                stop.time = "Arrived: $arrival"
            } else {
                stop.time = etaTexts[index] ?: "TBD"
            }
        }

        // This is the same adapter state supplied by updateUpcomingStopsUI().
        val liveArrivedIndexForSheet = if (isCurrentlyAtStop && lastArrivedStopIndex != -1) lastArrivedStopIndex else -1
        val stopNumbers = if (isReverseTripActive && isViewingReverseTrip) {
            reverseForwardStopIndexes.map { it + 1 }
        } else {
            emptyList()
        }
        stopsAdapter.updateStops(stops, liveArrivedIndexForSheet, displayNumbers = stopNumbers)
    }

    private fun startFollowingPuck() {
        if (!isNavigating) return
        isCameraFollowingBus = true
        lastCameraFollowLocation = null
        binding.btnRecenter.visibility = View.GONE
        // A viewport follow transition and the GPS-driven easeTo below must never
        mapView?.viewport?.idle()
        lastPuckPosition?.let(::followLiveBusCamera)
        lastAppliedBusScale = -1f
        updateBusModelScaleForZoom()
    }

    private fun followPuckHeadingUp() {
        mapView?.viewport?.transitionTo(
            mapView?.viewport?.makeFollowPuckViewportState(
                FollowPuckViewportStateOptions.Builder()
                    .zoom(DRIVER_RECENTER_ZOOM)
                    .pitch(65.0)
                    .bearing(FollowPuckViewportStateBearing.SyncWithLocationPuck)
                    .padding(navigationCameraPadding())
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
                    .padding(navigationCameraPadding())
                    .build()
            )!!
        )
    }

    private fun navigationCameraPadding(): EdgeInsets {
        val density = resources.displayMetrics.density
        val height = (mapView?.height?.takeIf { it > 0 }
            ?: resources.displayMetrics.heightPixels).toDouble()
        val sheetInset = if (::bottomSheetBehavior.isInitialized && isNavigating) {
            bottomSheetBehavior.peekHeight.toDouble()
        } else {
            110.0 * density
        }
        // Keep the bus in the lower map area, while leaving room to see the route ahead.
        val topInset = height * 0.5
        return EdgeInsets(topInset, 0.0, sheetInset, 0.0)
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

    private var isSubmittingAlert = false

    private fun submitDriverAlert(
        alertType: String,
        customDescription: String = "",
        onFinished: () -> Unit = {}
    ) {
        if (isSubmittingAlert) return
        isSubmittingAlert = true

        val driver = viewModel.currentDriver.value
        val user = FirebaseAuth.getInstance().currentUser
        val userEmail = user?.email?.trim()?.lowercase() ?: ""
        val userUid = user?.uid ?: ""

        val cachedDriver = DriverRepository.driverList.value?.find {
            (userEmail.isNotEmpty() && it.email.trim().equals(userEmail, ignoreCase = true)) ||
                    (userUid.isNotEmpty() && (it.uid == userUid || it.driverId == userUid || it.id == userUid)) ||
                    (driver != null && (it.driverId == driver.driverId || it.id == driver.id || it.uid == driver.uid))
        }

        val driverId = driver?.driverId?.ifEmpty { null }
            ?: cachedDriver?.driverId?.ifEmpty { null }
            ?: cachedDriver?.id?.ifEmpty { null }
            ?: userUid
        val driverName = driver?.name?.ifEmpty { null }
            ?: cachedDriver?.name?.ifEmpty { null }
            ?: user?.displayName
            ?: "Driver"
        val driverEmail = driver?.email?.ifEmpty { null }
            ?: cachedDriver?.email?.ifEmpty { null }
            ?: userEmail
        val driverPhone = driver?.phone?.trim()?.ifEmpty { null }
            ?: cachedDriver?.phone?.trim()
            ?: ""
        val busNumber = viewModel.dashboardData.value?.busNumber?.ifEmpty { null }
            ?: driver?.assignedBus
            ?: cachedDriver?.assignedBus
            ?: binding.tvBusNumberInfo.text.toString().trim()
        val routeName = assignedRoute?.routeName?.ifEmpty { null }
            ?: viewModel.dashboardData.value?.currentRoute
            ?: binding.tvRouteNameInfo.text.toString().trim()

        val lat = currentLocation?.latitude ?: 0.0
        val lng = currentLocation?.longitude ?: 0.0
        val address = lastResolvedAddress ?: ""
        val direction = if (isReverseTripActive) "RETURN" else "FORWARD"
        val tripStatus = if (isNavigating) "NAVIGATING" else if (isDutyEnabled) "ON_DUTY" else "IDLE"
        val alertUuid = java.util.UUID.randomUUID().toString()

        FirebaseRepository.sendDriverAlert(
            driverId = driverId,
            driverName = driverName,
            driverEmail = driverEmail,
            driverPhone = driverPhone,
            busNumber = busNumber,
            routeName = routeName,
            alertType = alertType,
            customDescription = customDescription,
            latitude = lat,
            longitude = lng,
            locationAddress = address,
            tripDirection = direction,
            tripStatus = tripStatus,
            alertId = alertUuid
        ) { success ->
            isSubmittingAlert = false
            onFinished()
            if (success) {
                Toast.makeText(this@DriverDashboardActivity, "Alert sent to Admin successfully", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@DriverDashboardActivity, "Alert saved offline and will be synced when connected", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showAlertsBottomSheet() {
        val dialog = BottomSheetDialog(this)
        val view = layoutInflater.inflate(R.layout.dialog_driver_alerts, null)
        dialog.setContentView(view)

        val alerts = listOf(
            AlertOption("Road Block", "Road is closed, need alternative route", R.drawable.notification_active, "🚧"),
            AlertOption("Heavy Traffic", "Stuck in traffic, bus might be late", R.drawable.notification_active, "🚥"),
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
            dialog.dismiss()
            if (option.title.equals("Other", ignoreCase = true)) {
                showOtherAlertContent()
            } else {
                Toast.makeText(this, "Sending alert: ${option.title}...", Toast.LENGTH_SHORT).show()
                submitDriverAlert(option.title)
            }
        }

        dialog.show()
    }

    private fun showOtherAlertContent() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_driver_custom_alert)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val etDesc = dialog.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.etCustomDescription)
        val tilDesc = dialog.findViewById<com.google.android.material.textfield.TextInputLayout>(R.id.tilDescription)
        val btnSend = dialog.findViewById<MaterialButton>(R.id.btnSubmitReport)
        val btnCancel = dialog.findViewById<MaterialButton>(R.id.btnCancelCustomAlert)
        val progressBar = dialog.findViewById<android.widget.ProgressBar>(R.id.progressBarCustomAlert)

        btnSend?.setOnClickListener {
            val text = etDesc?.text?.toString()?.trim() ?: ""
            if (text.isEmpty()) {
                tilDesc?.error = "Please enter an issue description"
                return@setOnClickListener
            }
            tilDesc?.error = null
            btnSend.isEnabled = false
            btnCancel?.isEnabled = false
            progressBar?.visibility = View.VISIBLE

            submitDriverAlert("Other", text) {
                dialog.dismiss()
            }
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
            val maxVisitedIdx = highestVisitedStopIndex()
            var startIndex = Math.max(nextGlobalStopIndex, maxVisitedIdx + 1)
            startIndex = advanceToUpcomingStop(startIndex)

            if (startIndex >= activeStops().size) {
                // A completed reverse trip stays completed; it must never roll into a
                if (isReverseTripActive) return
                stopArrivalTimes.clear()
                stopStates.clear()
                stopEtaTexts.clear()
                attendancePromptedStops.clear()
                lastSplitIndex = 0
                startIndex = 0
            }

            val remainingStops = activeStops().mapIndexed { index, stop -> index to stop }
                .filter { (index, stop) -> index >= startIndex && stateOf(index) == StopState.UPCOMING && stop.latitude != 0.0 && stop.longitude != 0.0 }
            mapboxLegByOriginalStopIndex.clear()
            remainingStops.forEachIndexed { mapboxStopIndex, (originalIndex, stop) ->
                mapboxLegByOriginalStopIndex[originalIndex] = mapboxStopIndex
                navPoints.add(Point.fromLngLat(stop.longitude, stop.latitude))
            }

            if (isReverseTripActive) {
                appendReturnSourceIfNeeded(navPoints)
            } else if (remainingStops.isEmpty()) {
                forwardTripDestinationPoint(route)?.let(navPoints::add)
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
        val requestGeneration = ++routeRequestGeneration
        Toast.makeText(this, "Requesting Route...", Toast.LENGTH_SHORT).show()

        // A fresh route is a new instruction stream. This prevents a delayed
        voiceSessionId++
        speechApi?.cancel()
        voiceInstructionsPlayer?.clear()
        fallbackTextToSpeech?.stop()
        activeFallbackUtteranceId = null
        abandonNavigationAudioFocus()

        val routeOptionsBuilder = RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .coordinatesList(navPoints)
            .profile(DirectionsCriteria.PROFILE_DRIVING_TRAFFIC)
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .steps(true)
            .bannerInstructions(true)
            .voiceInstructions(true)
            .language("en")
            .voiceUnits(DirectionsCriteria.METRIC)
            .alternatives(navPoints.size == 2)

        // Do not pin a new trip to the instantaneous GPS heading. At a source or
        isTripStartRouteRequestPending = true
        nav.requestRoutes(
            routeOptionsBuilder.build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    isTripStartRouteRequestPending = false
                    if (requestGeneration != routeRequestGeneration) return
                    if (routes.isEmpty()) return
                    val selectedRoute = shortestRoadRoute(routes)
                    // Keep a local, immediately usable copy before the asynchronous
                    selectedRoute.directionsRoute.geometry()?.let { geometry ->
                        fullNavigationPoints = LineString.fromPolyline(geometry, 6).coordinates()
                        // Trip start: this first route threads through every remaining
                        ensureMasterRouteChainage(fullNavigationPoints)
                    }
                    isNavigating = true
                    viewModel.currentDriver.value?.let { driver ->
                        FirebaseRepository.updateDriverStatus(
                            driver.driverId.ifBlank { driver.id },
                            "Active",
                            route.routeName
                        )
                    }
                    nav.setNavigationRoutes(listOf(selectedRoute))
                    updateStopEtasFromNavigationRoute(selectedRoute)

                    // Pre-populate Google Maps style instruction card with the route's initial maneuver
                    val firstLeg = selectedRoute.directionsRoute.legs()?.firstOrNull()
                    val firstStep = firstLeg?.steps()?.firstOrNull()
                    val firstManeuver = firstStep?.maneuver()
                    if (firstManeuver != null) {
                        val initialType = firstManeuver.type()
                        val initialModifier = firstManeuver.modifier()
                        binding.ivArrow.setImageResource(getManeuverIconRes(initialType, initialModifier))
                        binding.tvManeuverDistance.text = formatManeuverDistance(firstStep.distance())
                        binding.tvNextInstruction.text = firstManeuver.instruction() ?: "Head towards first stop"
                        binding.tvNextStreet.text = firstStep.name()?.takeIf { it.isNotBlank() } ?: "Current Route"
                    }
                    binding.instructionCard.visibility = View.VISIBLE
                    binding.maneuverView.visibility = View.GONE

                    if (ActivityCompat.checkSelfPermission(this@DriverDashboardActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(this@DriverDashboardActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                    ) {
                        try {
                            nav.startTripSession()
                        } catch (e: SecurityException) {
                            Log.e("ETA_DEBUG", "SecurityException starting trip session: ${e.message}", e)
                        }
                    } else {
                        Log.e("ETA_DEBUG", "startTripSession() SKIPPED - no location permission. RouteProgress/ETA will never update.")
                        Toast.makeText(this@DriverDashboardActivity, "Location permission missing - navigation tracking will not update.", Toast.LENGTH_LONG).show()
                    }

                    setNavigationMode(true, showPlaceholderInstruction = false)
                    persistCurrentActiveTripState()
                }
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    isTripStartRouteRequestPending = false
                    if (requestGeneration != routeRequestGeneration) return
                    val errorDetail = reasons.firstOrNull()?.message ?: "Unknown error"
                    Log.e("NavDebug", "Navigation failed: $errorDetail")
                    Toast.makeText(this@DriverDashboardActivity, "Navigation Error: $errorDetail", Toast.LENGTH_LONG).show()
                }
                override fun onCanceled(routeOptions: RouteOptions, routerOrigin: String) {
                    isTripStartRouteRequestPending = false
                    if (requestGeneration != routeRequestGeneration) return
                    Log.d("NavDebug", "Route request canceled")
                }
            }
        )
    }

    // Keep the first route instruction visible in the card.
    private fun setNavigationMode(
        isNavigating: Boolean,
        reloadStyle: Boolean = true,
        showPlaceholderInstruction: Boolean = true,
        clearActiveTrip: Boolean = true
    ) {
        val wasNavigating = this.isNavigating
        if (!isNavigating && wasNavigating) {
            // Save the current trip before ending the SDK session.  This intentionally
            persistCurrentActiveTripState()
            // An End Navigation action also invalidates any outstanding directions
            routeRequestGeneration++
            // Make every pending asynchronous speech result from this navigation
            voiceSessionId++
            speechApi?.cancel()
            voiceInstructionsPlayer?.clear()
            fallbackTextToSpeech?.stop()
            activeFallbackUtteranceId = null
            abandonNavigationAudioFocus()
        }
        this.isNavigating = isNavigating
        cancelDutyAutoOffTimer()
        binding.apply {
            if (isNavigating) {
                val isNewNavigationSession = !navigationUiActive
                navigationUiActive = true
                if (isNewNavigationSession) {
                    // Route creation sets isNavigating before this UI transition.
                    isCameraFollowingBus = true
                    lastCameraFollowLocation = null
                    btnRecenter.visibility = View.GONE
                }
                if (isNewNavigationSession) {
                    clearTraveledRouteHistory()
                    traveledRouteGeometry = null
                    currentRouteGeometry = null
                }

                setNavigationTopHeader(true)
                dashboardTopContent.visibility = View.GONE

                // Give the driver a useful card immediately; the next route-progress
                instructionCard.visibility = View.VISIBLE
                maneuverView.visibility = View.GONE
                if (showPlaceholderInstruction) {
                    tvNextInstruction.text = "Navigation starting"
                    tvNextStreet.text = "Finding next road"
                    tvDistanceNav.text = "Distance: --"
                    tvEtaNav.text = "ETA: --"
                }

                updateBottomSheetTheme(true)

                if (isNewNavigationSession) {
                    val styleGeneration = ++mapStyleLoadGeneration
                    mapView?.mapboxMap?.loadStyle("mapbox://styles/mapbox/navigation-night-v1") { style ->
                        if (styleGeneration != mapStyleLoadGeneration || !this@DriverDashboardActivity.isNavigating || isDestroyed) return@loadStyle
                        isMapStyleReady = true
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
                            // Lifecycle recreation (requirement 10): only seeds the master
                            ensureMasterRouteChainage(fullNavigationPoints)

                            drawPointsOnMap(fullNavigationPoints)
                            // A style reload starts with empty GeoJSON sources. The bus
                            restoreNavigationRouteGeometry(style)

                            lastRawPositionForSnap = null
                            currentLocation?.let { loc ->
                                updateNavigationRouteProgress(Point.fromLngLat(loc.longitude, loc.latitude))
                            }
                        }

                        startFollowingPuck()
                    }
                }

                cardRouteDetails.visibility = View.GONE
                btnStartNavigation.visibility = View.GONE
                bottomSummaryCard.visibility = View.VISIBLE
                bottomSheetBehavior.isHideable = false
                bottomSheetBehavior.peekHeight = (110 * resources.displayMetrics.density).toInt()
                bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED

                // Do not write null route geometry when this is merely a reroute/UI
                if (isNewNavigationSession) {
                    viewModel.currentDriver.value?.driverId?.let { driverId ->
                        val arrivalMap = stopArrivalTimes.mapKeys { it.key.toString() }
                        val etaMap = stopEtaTexts.mapKeys { it.key.toString() }
                        val routePolyline = fullNavigationPoints
                            .takeIf { it.size >= 2 }
                            ?.let { LineString.fromLngLats(it).toPolyline(6) }
                        FirebaseRepository.updateDriverRouteGeometry(
                            driverId, routePolyline, null, nextGlobalStopIndex, arrivalMap, true, etaMap
                        )
                    }
                }

                lastLegIndex = -1

                infoBar.setBackgroundColor(Color.parseColor("#0D1B3E"))
                layoutMapControls.visibility = View.VISIBLE
                val navOffset = -android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 106f, resources.displayMetrics)
                layoutMapControls.animate().translationY(navOffset).setDuration(500).start()
                btnRecenter.animate().translationY(navOffset).setDuration(500).start()
            } else {
                if (clearActiveTrip) clearCurrentActiveTripState()
                navigationUiActive = false
                isCameraFollowingBus = false
                lastCameraFollowLocation = null
                mapboxNavigation?.setNavigationRoutes(emptyList())
                mapboxNavigation?.stopTripSession()
                fullNavigationPoints = emptyList()
                masterRouteChainagePoints = emptyList()
                isTripStartRouteRequestPending = false
                resetFullRouteGpsProgress()
                clearTraveledRouteHistory()
                currentRouteGeometry = null
                traveledRouteGeometry = null

                mapView?.viewport?.idle()
                mapView?.mapboxMap?.setCamera(
                    CameraOptions.Builder()
                        .padding(EdgeInsets(0.0, 0.0, 0.0, 0.0))
                        .build()
                )
                dashboardCameraFitPending = true

                setNavigationTopHeader(false)
                dashboardTopContent.visibility = View.VISIBLE
                instructionCard.visibility = View.GONE
                maneuverView.visibility = View.GONE

                updateBottomSheetTheme(false)

                if (reloadStyle) {
                    isMapStyleReady = false
                    val styleGeneration = ++mapStyleLoadGeneration
                    mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
                        if (styleGeneration != mapStyleLoadGeneration || this@DriverDashboardActivity.isNavigating || isDestroyed) return@loadStyle
                        isMapStyleReady = true
                        recreateAnnotationManagers()
                        setupLocationPuck()
                        mapView?.location?.pulsingEnabled = true
                        restoreDashboardRouteOverview()
                    }
                } else {
                    restoreDashboardRouteOverview()
                }

                binding.layoutMapControls.visibility = View.GONE
                binding.layoutMapControls.translationY = 0f
                binding.btnRecenter.visibility = View.GONE
                binding.btnRecenter.translationY = 0f

                cardRouteDetails.visibility = View.VISIBLE
                btnStartNavigation.visibility = View.VISIBLE
                // Ending navigation must not leave Start Navigation stuck disabled/grey.
                updateNavigationButtonState()

                bottomSummaryCard.visibility = View.GONE
                bottomSheetBehavior.isHideable = true
                bottomSheetBehavior.peekHeight = (220 * resources.displayMetrics.density).toInt()
                bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN

                infoBar.setBackgroundColor(Color.TRANSPARENT)
                layoutMapControls.animate().translationY(0f).setDuration(500).start()

                viewModel.currentDriver.value?.driverId?.let { driverId ->
                    val arrivalMap = activeArrivalTimes().mapKeys { it.key.toString() }
                    val etaMap = activeEtaTexts().mapKeys { it.key.toString() }
                    lastDutyToggleTime = System.currentTimeMillis()
                    FirebaseRepository.updateDriverRouteGeometry(
                        driverId, null, null, nextGlobalStopIndex, arrivalMap, false, etaMap
                    )
                }
            }
        }
        updatePictureInPictureParams()
        if (!isNavigating && isInPictureInPictureMode) finish()
    }

    private fun setNavigationTopHeader(navigationActive: Boolean) {
        val density = resources.displayMetrics.density
        val headerParams = binding.headerBg.layoutParams
        headerParams.height = ((if (navigationActive) 56 else 220) * density).toInt()
        binding.headerBg.layoutParams = headerParams
        binding.headerBg.visibility = View.VISIBLE

        binding.toolbar.visibility = View.VISIBLE
        binding.toolbar.setBackgroundColor(
            if (navigationActive) Color.parseColor("#0D1B3E") else Color.TRANSPARENT
        )
        (binding.toolbar.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { params ->
            params.topMargin = ((if (navigationActive) 0 else 24) * density).toInt()
            binding.toolbar.layoutParams = params
        }
        binding.layoutProfileArea.visibility = if (navigationActive) View.GONE else View.VISIBLE
        binding.btnNotifications.visibility = View.VISIBLE
        binding.btnMenuDrawer.visibility = if (navigationActive) View.GONE else View.VISIBLE
        binding.btnNavigationDrawer.visibility = if (navigationActive) View.VISIBLE else View.GONE
        binding.layoutNavigationGreeting.visibility = if (navigationActive) View.VISIBLE else View.GONE
        binding.tvNavigationGreeting.text = binding.tvGreeting.text
        binding.tvNavigationDriverName.text = binding.tvDriverName.text

        (binding.layoutActions.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams)?.let { params ->
            if (navigationActive) {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.marginStart = 0
            } else {
                params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.UNSET
                params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
                params.marginStart = 0
            }
            binding.layoutActions.layoutParams = params
        }

        val instructionParams = binding.instructionCard.layoutParams as? androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        instructionParams?.topMargin = ((if (navigationActive) 16 else 8) * density).toInt()
        if (instructionParams != null) binding.instructionCard.layoutParams = instructionParams

        binding.speedometer.visibility = if (navigationActive) View.GONE else View.VISIBLE
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
        val outlineColor = if (isDark) Color.parseColor("#334155") else Color.parseColor("#CBD5E1")

        sheet.findViewById<View>(R.id.bottomSheetContainer)?.setBackgroundResource(
            if (isDark) R.drawable.bg_bottom_sheet_dark else R.drawable.bg_bottom_sheet_dialog
        )

        val btnClose = sheet.findViewById<View>(R.id.btnCloseNav)
        val btnRoute = sheet.findViewById<View>(R.id.btnViewRoute)

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

                viewModel.currentDriver.value?.let { driver ->
                    val driverId = driver.driverId.ifBlank { driver.id }
                    val routeName = assignedRoute?.routeName.orEmpty()
                    val busNo = driver.assignedBus ?: assignedRoute?.busNo ?: "Bus"
                    FirebaseRepository.updateDriverStatus(driverId, "Active", routeName)
                    FirebaseRepository.notifyDriverDutyStarted(
                        driverId = driverId,
                        driverName = driver.name.ifBlank { "Driver" },
                        busNo = busNo,
                        routeName = routeName
                    )
                }

                drawerLayout.closeDrawer(GravityCompat.END)
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
                    FirebaseRepository.updateDriverStatus(driverId, "Inactive")
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
            startStaleLocationWatchdog()
            if (!isGpsProviderEnabled()) {
                updateLocationReliabilityStatus(LocationReliabilityState.GPS_UNAVAILABLE)
            }

            viewModel.currentDriver.value?.let { driver ->
                val driverId = driver.driverId.ifBlank { driver.id }.ifBlank { driver.uid }
                if (driverId.isNotBlank()) {
                    FirebaseRepository.updateDriverStatus(driverId, "Active", assignedRoute?.routeName)
                }
            }
        } else {
            drawerDutyLabel?.text = "DUTY STATUS: OFF"

            stopStaleLocationWatchdog()
            lastFreshLocationTimestamp = 0L
            updateLocationReliabilityStatus(LocationReliabilityState.NORMAL_LIVE)
            isCurrentLocationLive = false
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
            mapView?.location?.enabled = false

            if (isNavigating) {
                // Turning duty off interrupts navigation but must not complete a
                setNavigationMode(false, reloadStyle, clearActiveTrip = false)
            }

            mapboxNavigation?.stopTripSession()

            // Keep in-memory progress when a session is recoverable. The persisted
            if (currentActiveTripId == null) {
                stopArrivalTimes.clear()
                stopStates.clear()
                stopEtaTexts.clear()
                nextGlobalStopIndex = 0
                attendancePromptedStops.clear()
                clearTraveledRouteHistory()
            }

            viewModel.currentDriver.value?.let { driver ->
                val driverId = driver.driverId.ifBlank { driver.id }.ifBlank { driver.uid }
                if (driverId.isNotBlank()) {
                    FirebaseRepository.updateDriverStatus(driverId, "Inactive")
                    FirebaseRepository.updateDriverRouteGeometry(
                        driverId, null, null, 0, emptyMap(), false
                    )
                }
            }
        }
        updateNavigationButtonState()
    }

    private fun updateNavigationButtonState() {
        val assignedBus = viewModel.currentDriver.value?.assignedBus
        val enabled = isDutyEnabled && !assignedBus.isNullOrBlank() &&
                !assignedBus.equals("Not Assigned", true) && assignedRoute != null
        binding.btnStartNavigation.isEnabled = enabled
        binding.btnStartNavigation.alpha = if (enabled) 1.0f else 0.5f
        binding.btnStartNavigation.backgroundTintList = ColorStateList.valueOf(
            if (enabled) Color.parseColor("#22C55E") else Color.GRAY
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
            Location.distanceBetween(
                currentPoint.latitude(), currentPoint.longitude(),
                startPoint.latitude(), startPoint.longitude(),
                results
            )
            val distanceKm = results[0] / 1000.0
            Toast.makeText(this, String.format(Locale.getDefault(), "You are %.2f km away from the starting point.", distanceKm), Toast.LENGTH_LONG).show()
        }

        val startName = if (route.pathPoints.isNotEmpty()) route.startPoint.ifEmpty { "Start Point" }
        else if (route.stopsList.isNotEmpty()) route.stopsList[0].stopName
        else "Start Point"
        showReachStartDialog(startName)
    }

    /** A resumed trip may start from any nearby part of its assigned route or stop. */
    private fun isOnOrNearAssignedRoute(route: RouteModel, location: Location): Boolean {
        val currentPoint = Point.fromLngLat(location.longitude, location.latitude)
        val routePoints = route.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
        val nearRouteLine = routePoints.zipWithNext().any { (start, end) ->
            TurfMeasurement.distance(
                currentPoint,
                projectPointOntoSegment(currentPoint, start, end),
                TurfConstants.UNIT_METERS
            ) <= RESUME_ROUTE_VALIDATION_RADIUS_METERS
        }
        if (nearRouteLine) return true

        return route.stopsList.any { stop ->
            stop.latitude != 0.0 && stop.longitude != 0.0 &&
                    TurfMeasurement.distance(
                        currentPoint,
                        Point.fromLngLat(stop.longitude, stop.latitude),
                        TurfConstants.UNIT_METERS
                    ) <= RESUME_ROUTE_VALIDATION_RADIUS_METERS
        }
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
                val route = assignedRoute
                val driver = viewModel.currentDriver.value
                val today = attendanceDate().replace("/", "-")
                val period = if (isActiveTripMorning()) "MORNING" else "EVENING"
                val tripId = currentActiveTripId.takeUnless { it.isNullOrEmpty() } ?: "${today}_${period}_${route?.id.orEmpty()}"

                val intent = Intent(this, EveningAttendanceActivity::class.java).apply {
                    putExtra("ROUTE_NAME", route?.routeName ?: "")
                    putExtra("ROUTE_ID", route?.id ?: "")
                    putExtra("BUS_ID", driver?.assignedBus ?: "")
                    putExtra("DRIVER_ID", driver?.driverId ?: "")
                    putExtra("DRIVER_NAME", driver?.name ?: "")
                    putExtra("TRIP_ID", tripId)
                    putExtra("TRIP_DIRECTION", if (isReverseTripActive) "RETURN" else "FORWARD")
                    putExtra("IS_MORNING", isActiveTripMorning())
                }
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
                    viewModel.currentDriver.value?.let { driver ->
                        val driverId = driver.driverId.ifBlank { driver.id }.ifBlank { driver.uid }
                        if (driverId.isNotBlank()) {
                            FirebaseRepository.updateDriverStatus(driverId, "Inactive")
                            FirebaseRepository.updateDriverRouteGeometry(
                                driverId, null, null, 0, emptyMap(), false
                            )
                        }
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
        if (!isDutyEnabled || isNavigating || currentActiveTripId != null) return

        cancelDutyAutoOffTimer()
        val driver = viewModel.currentDriver.value
        val driverIdSnapshot = driver?.driverId?.ifBlank { driver.id }?.ifBlank { driver.uid } ?: return
        if (driverIdSnapshot.isBlank()) return

        val runnable = Runnable {
            if (isNavigating || currentActiveTripId != null) return@Runnable
            FirebaseRepository.updateDriverStatus(driverIdSnapshot, "Inactive")
            FirebaseRepository.updateDriverRouteGeometry(
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
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            notificationListeners = FirebaseRepository.listenToNotifications(uid, "driver") { notifications ->
                runOnUiThread {
                    val unread = notifications.count { !it.isRead }
                    binding.tvNotificationBadge.text = unread.toString()
                    binding.tvNotificationBadge.visibility = if (unread > 0) View.VISIBLE else View.GONE
                }
            }
        }
        cancelDutyAutoOffTimer()
        com.example.bustrack_app.sync.network.NetworkMonitor.addListener(networkListener)
        if (isDutyEnabled) {
            startStaleLocationWatchdog()
        }
    }

    override fun onStop() {
        super.onStop()
        notificationListeners.forEach { it.remove() }
        notificationListeners = emptyList()
        mapView?.onStop()
        com.example.bustrack_app.sync.network.NetworkMonitor.removeListener(networkListener)
        stopStaleLocationWatchdog()
        scheduleDutyAutoOffTimer()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (Build.VERSION.SDK_INT in Build.VERSION_CODES.O until Build.VERSION_CODES.S) {
            enterNavigationPictureInPicture()
        }
    }

    override fun onPictureInPictureModeChanged(
        isInPictureInPictureMode: Boolean,
        newConfig: Configuration
    ) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        if (isInPictureInPictureMode) {
            showPictureInPictureNavigationUi()
        } else if (isNavigating && navigationUiActive) {
            setNavigationMode(true, reloadStyle = false)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        if (isNavigating) persistCurrentActiveTripState()
        outState.putString(STATE_LAST_SPOKEN_INSTRUCTION, lastSpokenInstruction)
        outState.putLong(STATE_LAST_SPOKEN_TIME_MS, lastSpokenTimeMs)
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        com.example.bustrack_app.sync.network.NetworkMonitor.removeListener(networkListener)
        stopStaleLocationWatchdog()
        locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        pendingBusScaleUpdate?.let(busScaleHandler::removeCallbacks)
        mapView?.mapboxMap?.removeOnCameraChangeListener(cameraChangeListener)
        // Unregistering through the binder (rather than mapboxNavigation?.unregisterX)
        MapboxNavigationApp.unregisterObserver(navObserverBinder)

        speechApi?.cancel()
        speechApi = null
        voiceInstructionsPlayer?.shutdown()
        voiceInstructionsPlayer = null
        fallbackTextToSpeech?.stop()
        fallbackTextToSpeech?.shutdown()
        fallbackTextToSpeech = null
        abandonNavigationAudioFocus()

        MapboxNavigationApp.detach(this)

        bitmapCache.clear()
        mapView?.onDestroy()
        super.onDestroy()
    }

    private fun updatePictureInPictureParams() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) return

        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(isNavigating)
        }
        val sourceRect = Rect()
        if (mapView?.getGlobalVisibleRect(sourceRect) == true && !sourceRect.isEmpty) {
            builder.setSourceRectHint(sourceRect)
        }
        setPictureInPictureParams(builder.build())
    }

    private fun enterNavigationPictureInPicture() {
        if (!isNavigating || Build.VERSION.SDK_INT < Build.VERSION_CODES.O || isInPictureInPictureMode) return
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)) {
            moveTaskToBack(true)
            return
        }

        updatePictureInPictureParams()
        enterPictureInPictureMode(
            PictureInPictureParams.Builder()
                .setAspectRatio(Rational(16, 9))
                .build()
        )
    }

    private fun showPictureInPictureNavigationUi() {
        binding.toolbar.visibility = View.GONE
        binding.headerBg.visibility = View.GONE
        binding.dashboardTopContent.visibility = View.GONE
        binding.instructionCard.visibility = View.VISIBLE
        binding.maneuverView.visibility = View.GONE
        binding.cardRouteDetails.visibility = View.GONE
        binding.btnStartNavigation.visibility = View.GONE
        binding.bottomSummaryCard.visibility = View.GONE
        bottomSheetBehavior.isHideable = true
        bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
        binding.layoutMapControls.visibility = View.GONE
        binding.btnRecenter.visibility = View.GONE
    }
}
