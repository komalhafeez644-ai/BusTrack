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
import androidx.appcompat.app.AlertDialog
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
import com.mapbox.turf.TurfMisc
import kotlin.collections.firstOrNull
import com.mapbox.maps.plugin.ModelScaleMode
import com.mapbox.maps.plugin.delegates.listeners.OnCameraChangeListener
import com.mapbox.navigation.tripdata.maneuver.api.MapboxManeuverApi
import com.mapbox.navigation.base.formatter.DistanceFormatter

/**
 * Unidirectional lifecycle for a single stop: UPCOMING -> ARRIVED -> COMPLETED.
 * SKIPPED is a terminal branch taken directly from UPCOMING when navigation detects the
 * bus moved past a stop without an arrival being recorded. No transition ever moves a
 * stop backwards (e.g. ARRIVED can never revert to UPCOMING).
 */
enum class StopState { UPCOMING, ARRIVED, COMPLETED, SKIPPED }

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
    // Kept separately from isNavigating because Mapbox marks a trip as navigating
    // before the dashboard has entered navigation mode.  This lets us distinguish a
    // brand-new trip (where clearing history is correct) from a route/style update.
    private var navigationUiActive = false
    private var shouldFitCameraToRoute = true
    private var currentRouteGeometry: String? = null
    private var traveledRouteGeometry: String? = null
    private var isVoiceEnabled = true
    // Text-to-Speech initialises asynchronously. Keep the first instruction instead of
    // dropping it when Mapbox emits it before the Android engine is ready.
    private var pendingFallbackInstruction: String? = null
    private var fullNavigationPoints: List<Point> = emptyList()
    private lateinit var stopsAdapter: com.example.bustrack_app.adapter.NavigationStopsAdapter
    private lateinit var bottomSheetBehavior: com.google.android.material.bottomsheet.BottomSheetBehavior<View>

    private var lastLegIndex = -1
    private var navStartIndex = 0
    private var nextGlobalStopIndex = 0
    private var currentNavPoints: List<Point> = emptyList()
    private var latestRouteProgress: com.mapbox.navigation.base.trip.model.RouteProgress? = null
    private val stopArrivalTimes = mutableMapOf<Int, String>()
    // Authoritative per-stop state. stopArrivalTimes stays purely for display text
    // ("Arrived: 8:02 AM" / "Skipped"); stopStates is what drives all state transitions.
    private val stopStates = mutableMapOf<Int, StopState>()
    // Last-computed "ETA: ..." text per stop index. Kept separately from StopItem.time
    // because assignedRoute (and its StopItem instances) gets replaced wholesale whenever
    // RouteRepository/dashboardData emits, which would otherwise wipe the ETA back to "".
    private val stopEtaTexts = mutableMapOf<Int, String>()
    // The return journey is deliberately not a mutation of the forward journey.
    // Its stop instances, state, arrival times and ETA values are all independent.
    private var isReverseTripActive = false
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
    // If a location jump or reroute occurs with a large gap (>60m), segments remain
    // separated in a MultiLineString so no straight line is drawn across town.
    private val accumulatedTraveledSegments = mutableListOf<List<Point>>()
    private var activeTraveledSegment: List<Point> = emptyList()
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())

    // Authoritative navigation ETA directly calculated from navigation model state
    // (RouteProgress / durationRemaining) - never read from UI TextViews.
    private var currentNavigationEtaText: String? = null

    // Cached load stat ("Present/Total" or "Remaining/Expected") updated on route load
    // and stop events, avoiding per-second Firestore queries in routeProgressObserver.
    private var cachedLoadString: String = "0/0"

    private var lastGeocodeTime = 0L
    private var lastGeocodeLocation: Location? = null
    private var lastResolvedAddress: String? = null
    private val GEOCODE_MIN_INTERVAL_MS = 15000L
    private val GEOCODE_MIN_DISTANCE_METERS = 50f

    // Maps an original RouteModel stop index to the corresponding submitted Mapbox
    // waypoint/leg index. Invalid-coordinate stops are omitted from Mapbox requests
    // without corrupting the dashboard's original stop state.
    private val mapboxLegByOriginalStopIndex = mutableMapOf<Int, Int>()

    private var activeStopStatus = "NEXT" // NEXT, ARRIVED, PASSED
    private var lastArrivedStopIndex = -1
    private var isCurrentlyAtStop = false
    private val ARRIVAL_RADIUS = 80.0 // meters
    // Attendance must be ready before the bus is exactly inside the smaller
    // arrival geofence, otherwise the driver sees it too late at the stop.
    private val ATTENDANCE_PROMPT_RADIUS = 140.0 // meters
    private val DEPARTURE_RADIUS = 70.0 // meters
    // How many UPCOMING stops ahead of the current pointer we'll check when scanning
    // for geofence entry. Geofence proximity is the sole source of truth for stop
    // arrival/skip decisions (see checkGeofenceAndStopStatus below) - this bounds how
    // far ahead a single GPS fix can jump the pointer, so one noisy/bad fix can't mark
    // an unreasonable number of stops SKIPPED at once.
    private val SKIP_DETECTION_LOOKAHEAD_STOPS = 3
    private var currentRawLocation: Location? = null
    private var sourceArrivalRecordedForCurrentTrip = false

    private var departureCandidateIndex = -1
    private var departureConfirmCount = 0
    private val DEPARTURE_CONFIRM_THRESHOLD = 3

    private var lastSplitIndex = 0
    private val SPLIT_SEARCH_WINDOW = 120
    // Prevent a nearest-point lookup from jumping hundreds of metres ahead to a
    // parallel/opposite carriageway before the bus has physically made its U-turn.
    private val MIN_FORWARD_ROUTE_PROGRESS_METERS = 80.0

    private val OFF_ROUTE_THRESHOLD_METERS = 35.0
    private val PARALLEL_ROAD_OFF_ROUTE_THRESHOLD_METERS = 4.0
    private val OPPOSITE_DIRECTION_REROUTE_DEGREES = 100.0
    private var isRerouteInFlight = false
    private var lastOffRouteRerouteTimeMs = 0L
    private val MIN_OFFROUTE_REROUTE_GAP_MS = 3000L

    private var isNorthUp = false
    private var isUserTriggeredChange = true
    private var lastDutyToggleTime = 0L
    private val DUTY_SYNC_DEBOUNCE_MS = 3000L

    private val dutyHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var dutyAutoOffRunnable: Runnable? = null

    // MAP-mode world scale, compensated so the bus shrinks into the road when zooming
    // in and stays readable when zooming out — the same visual idea as stop markers.
    // VIEWPORT + a fixed scale made Re-Center (zoom 19) collapse the model to a speck.
    private val MIN_BUS_MODEL_SCALE = 1.7f
    private val MAX_BUS_MODEL_SCALE = 2.0f
    private val BUS_MODEL_SCALE_REFERENCE_ZOOM = 17.0
    private val BUS_MODEL_SCALE_REFERENCE_VALUE = 1.0f
    private val BUS_MODEL_SCALE_COMPENSATION_FACTOR = 0.5
    private val BUS_MODEL_PITCH_COMPENSATION_FLOOR = 0.35
    private val MIN_BUS_WORLD_SCALE = 1.2f
    private val MAX_BUS_WORLD_SCALE = 48.0f
    private val LOCATION_MODEL_LAYER_ID = "mapbox-location-model-layer"
    private var lastAppliedBusScale = -1f

    private val BUS_MODEL_ROLL_OFFSET_X_DEG = 0f
    private val BUS_MODEL_ROLL_OFFSET_Y_DEG = 0f
    private val DUTY_AUTO_OFF_GRACE_PERIOD_MS = 10 * 60 * 1000L

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private var currentLocation: Location? = null
    private var isCurrentLocationLive = false
    private var assignedRoute: RouteModel? = null

    private var lastFirestoreLocation: Location? = null
    private var lastFirestoreUpdateTime = 0L

    companion object {
        // Publish the live bus position and route split together at a cadence that
        // remains visually in step on the Admin tracking map.
        private const val FIRESTORE_UPDATE_INTERVAL = 1000L
        private const val FIRESTORE_MIN_DISTANCE = 2f
    }
    private var locationCallback: LocationCallback? = null
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    private var mapboxNavigation: MapboxNavigation? = null
    private var voiceInstructionsPlayer: MapboxVoiceInstructionsPlayer? = null
    private var speechApi: MapboxSpeechApi? = null
    private var fallbackTextToSpeech: TextToSpeech? = null
    private var isFallbackTtsReady = false
    private var audioFocusRequest: Any? = null
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
    // position can be published while the split route still represents the prior
    // point, which is precisely the visible line lag on Admin tracking.
    private val MIN_GPS_MOVEMENT_FOR_SNAP_METERS = 1.0

    private val NAV_ROUTE_SOURCE_ID = "nav-route-source"
    private val NAV_TRAVELED_SOURCE_ID = "nav-traveled-source"
    private val NAV_ROUTE_LAYER_ID = "nav-route-layer"
    private val NAV_ROUTE_CASING_LAYER_ID = "nav-route-casing-layer"
    private val NAV_TRAVELED_LAYER_ID = "nav-traveled-layer"

    // Visual puck uses Fused GPS only. Filter noise without waiting for a 6 m snap,
    // which looked like the bus jumping to a new coordinate.
    private var lastPuckPosition: Location? = null
    private var lastPuckElapsedNanos = 0L
    private var lastAcceptedLocationElapsedNanos = 0L
    private val MAX_ACCEPTABLE_PUCK_ACCURACY_METERS = 50f
    private val STATIONARY_HOLD_SPEED_MPS = 1.0f
    private val MIN_MOVING_PUCK_UPDATE_METERS = 1.5f
    private val MAX_PLAUSIBLE_PUCK_SPEED_MPS = 55.0
    private val STALE_CACHED_LOCATION_MAX_AGE_MS = 5000L

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
        isVoiceEnabled = getSharedPreferences("navigation_preferences", MODE_PRIVATE)
            .getBoolean("voice_enabled", true)

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
                (audioFocusRequest as? AudioFocusRequest)?.let {
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

    // FIX (registration race): MapboxNavigationApp.current() can still be null the
    // instant after attach(this) if the singleton hasn't finished wiring itself to
    // the Activity lifecycle yet. Registering observers with `?.` against that null
    // reference silently no-ops - routeProgressObserver/voiceInstructionsObserver
    // then never fire for the rest of the session (the nav card stays static, no
    // voice), even though nav.startTripSession() succeeds later on a fresh,
    // non-null instance and powers the native trip notification just fine.
    // MapboxNavigationObserver's onAttached callback is only invoked once Mapbox
    // hands back a guaranteed-valid instance, so registering through it removes
    // the race entirely instead of guessing at timing.
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

        // Registering through MapboxNavigationApp.registerObserver(...) is idempotent -
        // Mapbox tracks observers in a Set, so calling this again on a later
        // initNavigation() re-entry (e.g. from startNavigationAnimation()'s null-check
        // fallback) will not create duplicate registrations.
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
                            abandonNavigationAudioFocus()
                        }
                        override fun onError(utteranceId: String?) {
                            abandonNavigationAudioFocus()
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
        if (!isVoiceEnabled) return@VoiceInstructionsObserver
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

        Log.d("VoiceNav", "Triggering voice instruction: $announcement")
        val speech = speechApi
        if (speech != null) {
            speech.generate(voiceInstructions) { expected ->
                expected.fold(
                    { error ->
                        Log.w("VoiceNav", "SpeechApi generation error: $error, falling back")
                        val fallback = error.fallback
                        if (fallback != null && voiceInstructionsPlayer != null) {
                            // FIX (silent voice): audio focus was previously requested only
                            // for the Android TTS fallback path (speakFallbackInstruction),
                            // never for the Mapbox voice player. Without focus, playback can
                            // be silently ducked or blocked by another audio session.
                            requestNavigationAudioFocus()
                            voiceInstructionsPlayer?.play(fallback) { a ->
                                abandonNavigationAudioFocus()
                                speechApi?.clean(a)
                            }
                        } else {
                            runOnUiThread { speakFallbackInstruction(announcement) }
                        }
                    },
                    { value ->
                        requestNavigationAudioFocus()
                        voiceInstructionsPlayer?.play(value.announcement) { a ->
                            abandonNavigationAudioFocus()
                            speechApi?.clean(a)
                        }
                    }
                )
            }
        } else {
            speakFallbackInstruction(announcement)
        }
    }

    private fun speakFallbackInstruction(instruction: String?) {
        if (!isVoiceEnabled || instruction.isNullOrBlank()) return
        if (!isFallbackTtsReady) {
            pendingFallbackInstruction = instruction
            return
        }
        requestNavigationAudioFocus()
        val utteranceId = "nav_inst_${System.currentTimeMillis()}"
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
            // Same guard idea as the backup check below: only fire if no reroute is
            // already in flight AND enough time has passed since the last one. Without
            // this, this callback keeps firing on ~every GPS tick while off-route,
            // spamming triggerReroute() and causing each new request to cancel the
            // previous one before it ever completes.
            if (!isRerouteInFlight && now - lastOffRouteRerouteTimeMs > MIN_OFFROUTE_REROUTE_GAP_MS) {
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

    private fun triggerReroute() {
        val route = assignedRoute ?: return
        val nav = mapboxNavigation ?: return
        val loc = currentLocation ?: return

        // Belt-and-suspenders: even if a caller forgets to check isRerouteInFlight
        // before calling this, don't fire a second overlapping request.
        if (isRerouteInFlight) return
        isRerouteInFlight = true

        val currentPoint = Point.fromLngLat(loc.longitude, loc.latitude)
        val navPoints = mutableListOf<Point>()
        navPoints.add(currentPoint)

        val maxVisitedIdx = activeArrivalTimes().keys.maxOrNull() ?: -1
        val targetStopIndex = Math.max(nextGlobalStopIndex, maxVisitedIdx + 1)

        val remainingStops = activeStops().mapIndexed { index, stop -> index to stop }
            .filter { (index, stop) -> index >= targetStopIndex && stateOf(index) == StopState.UPCOMING && stop.latitude != 0.0 && stop.longitude != 0.0 }

        mapboxLegByOriginalStopIndex.clear()
        remainingStops.forEachIndexed { mapboxStopIndex, (originalIndex, stop) ->
            mapboxLegByOriginalStopIndex[originalIndex] = mapboxStopIndex
            navPoints.add(Point.fromLngLat(stop.longitude, stop.latitude))
        }

        if (isReverseTripActive) {
            originalRouteSource()?.let { navPoints.add(it) }
        } else if (remainingStops.isEmpty() && route.pathPoints.isNotEmpty()) {
            navPoints.add(Point.fromLngLat(route.pathPoints.last().longitude, route.pathPoints.last().latitude))
        } else if (route.pathPoints.isNotEmpty()) {
            // Stops are the authoritative navigation waypoints when present.
        }

        if (navPoints.size < 2) {
            isRerouteInFlight = false
            return
        }

        currentNavPoints = navPoints

        val currentBearing = if (loc.hasBearing() && loc.bearing != 0f) {
            loc.bearing.toDouble()
        } else if (lastValidBearing != 0.0) {
            lastValidBearing
        } else {
            null
        }

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
            .alternatives(false)

        if (currentBearing != null) {
            val bearings = mutableListOf<Bearing?>()
            bearings.add(Bearing.builder().angle(currentBearing).degrees(45.0).build())
            for (i in 1 until navPoints.size) {
                bearings.add(null)
            }
            routeOptionsBuilder.bearingsList(bearings)
        }

        nav.requestRoutes(
            routeOptionsBuilder.build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    runOnUiThread {
                        isRerouteInFlight = false
                        if (routes.isEmpty()) return@runOnUiThread

                        navStartIndex = targetStopIndex
                        nextGlobalStopIndex = targetStopIndex
                        nav.setNavigationRoutes(routes)

                        // Clear stale voice instructions and audio focus from previous path
                        speechApi?.cancel()
                        voiceInstructionsPlayer?.clear()
                        fallbackTextToSpeech?.stop()
                        abandonNavigationAudioFocus()

                        // Immediately update with the newly calculated road geometry
                        val newCoords = routes[0].directionsRoute.geometry()?.let {
                            LineString.fromPolyline(it, 6).coordinates()
                        } ?: emptyList()

                        if (newCoords.isNotEmpty()) {
                            if (newCoords != fullNavigationPoints) {
                                freezeActiveTraveledSegment()
                            }
                            fullNavigationPoints = newCoords
                            lastSplitIndex = 0
                            lastRawPositionForSnap = null
                            updateStopEtasFromNavigationRoute(routes[0])

                            // Draw the new route on the map without waiting for next GPS tick
                            currentLocation?.let { currentLoc ->
                                updateNavigationRouteProgress(Point.fromLngLat(currentLoc.longitude, currentLoc.latitude))
                            }
                        }

                        Log.d("NavDebug", "Automatic reroute successful from current location to stop $navStartIndex")
                    }
                }
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
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
                    isRerouteInFlight = false
                }
            }
        )
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

            val currentSpeed = rawEnhancedLocation.speed ?: 0.0
            val newBearing = rawEnhancedLocation.bearing
            if (currentSpeed >= MIN_SPEED_FOR_BEARING_UPDATE && newBearing != null) {
                lastValidBearing = newBearing
            }
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

            runOnUiThread {
                // Never move the visual puck from Mapbox's matcher. Enhanced/snapped
                // coordinates fight Fused GPS and make the bus jump, reverse, and drift.
                if (isNavigating) {
                    val speedKph = (androidLocation.speed * 3.6).toInt()
                    binding.bottomSummaryCard.findViewById<TextView>(R.id.tvSpeedSheet)?.text = "$speedKph km/h"
                    binding.tvSpeedNav.text = "$speedKph"

                    reverseGeocodeIfNeeded(effectiveLocation)
                }
            }
        }
    }

    /** Populate every upcoming stop immediately from the route response, before the
     * first RouteProgress callback arrives. RouteProgress then refines these values
     * every second as the bus moves. */
    private fun updateStopEtasFromNavigationRoute(navigationRoute: NavigationRoute) {
        val stops = activeStops()
        if (stops.isEmpty()) return
        val legs = navigationRoute.directionsRoute.legs() ?: return
        var accumulatedSeconds = 0

        stops.forEachIndexed { index, stop ->
            if (stateOf(index) != StopState.UPCOMING || index < nextGlobalStopIndex) return@forEachIndexed
            val legIndex = mapboxLegByOriginalStopIndex[index] ?: return@forEachIndexed
            val legSeconds = legs.getOrNull(legIndex)?.duration()?.toInt() ?: return@forEachIndexed
            accumulatedSeconds += legSeconds
            val etaText = "ETA: ${timeFormat.format(Calendar.getInstance().apply {
                add(Calendar.SECOND, accumulatedSeconds)
            }.time)}"
            activeEtaTexts()[index] = etaText
            stop.time = etaText
        }

        val nextEta = activeEtaTexts()[nextGlobalStopIndex]
        if (nextEta != null) {
            currentNavigationEtaText = nextEta.removePrefix("ETA: ")
            binding.bottomSummaryCard.findViewById<TextView>(R.id.tvEtaSheet)?.text = currentNavigationEtaText
            binding.tvEtaNav.text = "ETA: $currentNavigationEtaText"
        }
        updateUpcomingStopsUI()
    }

    private fun reverseGeocodeIfNeeded(location: Location) {
        val now = System.currentTimeMillis()
        val moved = lastGeocodeLocation?.distanceTo(location) ?: Float.MAX_VALUE
        if (now - lastGeocodeTime < GEOCODE_MIN_INTERVAL_MS || moved < GEOCODE_MIN_DISTANCE_METERS) return
        lastGeocodeTime = now
        lastGeocodeLocation = Location(location)

        lifecycleScope.launch(Dispatchers.IO) {
            val address = try {
                Geocoder(this@DriverDashboardActivity, Locale.getDefault())
                    .getFromLocation(location.latitude, location.longitude, 1)
                    ?.firstOrNull()?.getAddressLine(0)
                    ?.replace(Regex("^[A-Z0-9]{4,8}\\+[A-Z0-9]{2,4}\\s*"), "")
                    ?.let(::normalizeDisplayAddress)
            } catch (_: Exception) {
                null
            }
            if (!address.isNullOrBlank()) {
                withContext(Dispatchers.Main) {
                    lastResolvedAddress = address
                    binding.bottomSummaryCard.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = address
                }
            }
        }
    }

    private fun normalizeDisplayAddress(address: String): String {
        val parts = address.split(',')
            .map(String::trim)
            .filter(String::isNotEmpty)

        return parts.fold(mutableListOf<String>()) { uniqueParts, part ->
            if (uniqueParts.none { it.equals(part, ignoreCase = true) }) {
                uniqueParts += part
            }
            uniqueParts
        }.joinToString(", ")
    }

    // ---------------------------------------------------------------------------------
    // Stop state machine: UPCOMING -> ARRIVED -> COMPLETED (SKIPPED branches off UPCOMING).
    // Every transition is guarded by the stop's current state, so a stop can only ever
    // move forward. In particular, leaving a stop's geofence can only push it from
    // ARRIVED to COMPLETED — it can never fall back to UPCOMING.
    // ---------------------------------------------------------------------------------

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

    /** The configured route origin, never the driver's location when navigation began. */
    private fun originalRouteSource(): Point? = assignedRoute?.pathPoints
        ?.firstOrNull { it.latitude != 0.0 && it.longitude != 0.0 }
        ?.let { Point.fromLngLat(it.longitude, it.latitude) }

    private fun isMorningTrip(): Boolean =
        Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 14

    /** Stop attendance is a boarding action and belongs only to the forward AM trip. */
    private fun isMorningPickupTrip(): Boolean = !isReverseTripActive && isMorningTrip()

    private fun recordSourceArrivalIfNeeded(location: Location) {
        if (sourceArrivalRecordedForCurrentTrip || !isNavigating) return
        if (nextGlobalStopIndex < activeStops().size) return
        val source = originalRouteSource() ?: return
        val distance = FloatArray(1)
        Location.distanceBetween(location.latitude, location.longitude, source.latitude(), source.longitude(), distance)
        if (distance[0] > ARRIVAL_RADIUS) return
        sourceArrivalRecordedForCurrentTrip = true
        if (isMorningTrip()) FirebaseRepository.updateDropTimesForRoute(
            assignedRoute?.routeName.orEmpty(), "", true,
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time),
            timeFormat.format(Calendar.getInstance().time)
        )
        if (isReverseTripActive) {
            reverseTripCompleted = true
            currentNavigationEtaText = "Route completed"
            mapboxNavigation?.setNavigationRoutes(emptyList())
        }
    }

    private fun recordEveningDropAtStop(stopName: String) {
        if (isMorningTrip()) return
        FirebaseRepository.updateDropTimesForRoute(
            assignedRoute?.routeName.orEmpty(), stopName, false,
            SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time),
            timeFormat.format(Calendar.getInstance().time)
        )
    }

    /** UPCOMING -> ARRIVED. Records the arrival timestamp. No-op (returns false) if not currently UPCOMING. */
    private fun transitionToArrived(index: Int): Boolean {
        if (stateOf(index) != StopState.UPCOMING) return false
        activeStates()[index] = StopState.ARRIVED
        activeArrivalTimes()[index] = timeFormat.format(Calendar.getInstance().time)
        lastArrivedStopIndex = index
        isCurrentlyAtStop = true
        activeStopStatus = "ARRIVED"
        refreshLoadStat()
        return true
    }

    /**
     * ARRIVED -> COMPLETED. Advances nextGlobalStopIndex past this stop, which is what
     * shifts the ETA/distance calculation strictly onto the next stop. No-op if the stop
     * isn't currently ARRIVED (e.g. already COMPLETED), so it is never re-triggered.
     */
    private fun transitionToCompleted(index: Int): Boolean {
        if (stateOf(index) != StopState.ARRIVED) return false
        activeStates()[index] = StopState.COMPLETED
        isCurrentlyAtStop = false
        activeStopStatus = "PASSED"
        lastArrivedStopIndex = -1
        nextGlobalStopIndex = index + 1
        refreshLoadStat()
        return true
    }

    /** UPCOMING -> SKIPPED. No-op if the stop already advanced (e.g. it was already ARRIVED). */
    private fun transitionToSkipped(index: Int): Boolean {
        if (stateOf(index) != StopState.UPCOMING) return false
        activeStates()[index] = StopState.SKIPPED
        activeArrivalTimes()[index] = "Skipped"
        return true
    }

    private fun checkGeofenceAndStopStatus(location: Location) {
        val stops = activeStops()
        recordSourceArrivalIfNeeded(location)
        if (stops.isEmpty()) return
        if (nextGlobalStopIndex >= stops.size) return

        // If Android was saving state on the exact GPS tick that entered the stop,
        // the sheet could not safely be shown then. Retry while the bus is still at
        // that stop so attendance remains mandatory/visible at every arrival.
        if (isCurrentlyAtStop && lastArrivedStopIndex != -1 &&
            !attendancePromptedStops.contains(lastArrivedStopIndex)
        ) {
            stops.getOrNull(lastArrivedStopIndex)?.let { arrivedStop ->
                if (showAttendanceForStop(arrivedStop.stopName)) {
                    attendancePromptedStops.add(lastArrivedStopIndex)
                }
            }
        }

        // Attendance belongs to the forward flow and is intentionally not involved in
        // a reverse trip.
        if (isReverseTripActive) {
            checkActiveTripGeofence(location, stops)
            return
        }

        // Open attendance as the bus approaches its next stop, not only after it
        // crosses the tighter arrival radius. The stop is still marked ARRIVED only
        // at ARRIVAL_RADIUS below, so an early prompt cannot falsely complete it.
        if (!isCurrentlyAtStop && !attendancePromptedStops.contains(nextGlobalStopIndex)) {
            stops.getOrNull(nextGlobalStopIndex)?.let { nextStop ->
                val distance = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    nextStop.latitude, nextStop.longitude,
                    distance
                )
                if (distance[0] <= ATTENDANCE_PROMPT_RADIUS && showAttendanceForStop(nextStop.stopName)) {
                    attendancePromptedStops.add(nextGlobalStopIndex)
                }
            }
        }

        // 1. Entering Geofence: UPCOMING -> ARRIVED.
        // Geofence proximity is the SOLE source of truth for whether a stop was
        // reached - not Mapbox's currentLegProgress.legIndex (that only reflects
        // progress along the *planned* route geometry, and is wrong the moment the
        // driver deviates from it, e.g. a shortcut or a stale route mid-reroute).
        // We scan forward a bounded window of UPCOMING stops from the current
        // pointer, not just the single "next" stop, so that if the driver's real GPS
        // position lands inside a LATER stop's geofence directly, we can correctly
        // detect that the stops in between were genuinely skipped, based on actual
        // physical proximity rather than a route-progress guess.
        if (!isCurrentlyAtStop) {
            val scanLimit = minOf(stops.size, nextGlobalStopIndex + 1 + SKIP_DETECTION_LOOKAHEAD_STOPS)
            for (candidateIndex in nextGlobalStopIndex until scanLimit) {
                if (stateOf(candidateIndex) != StopState.UPCOMING) continue

                val candidateStop = stops[candidateIndex]
                val results = FloatArray(1)
                Location.distanceBetween(
                    location.latitude, location.longitude,
                    candidateStop.latitude, candidateStop.longitude,
                    results
                )

                if (results[0] <= ARRIVAL_RADIUS) {
                    // We're physically inside candidateIndex's geofence right now, so
                    // any still-UPCOMING stop strictly before it was never actually
                    // reached - mark those SKIPPED before advancing the pointer.
                    for (skippedIndex in nextGlobalStopIndex until candidateIndex) {
                        transitionToSkipped(skippedIndex)
                    }
                    nextGlobalStopIndex = candidateIndex

                    if (transitionToArrived(candidateIndex)) {
                        recordEveningDropAtStop(candidateStop.stopName)
                        // Trigger stop arrival notification to parents
                        val isMorning = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 14
                        val period = if (isMorning) "MORNING" else "EVENING"
                        val tripId = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Calendar.getInstance().time) + "_$period"

                        FirebaseRepository.notifyParentsOfStopArrival(
                            assignedRoute?.routeName ?: "",
                            candidateStop.stopName,
                            tripId
                        )

                        if (!attendancePromptedStops.contains(candidateIndex)) {
                            if (showAttendanceForStop(candidateStop.stopName)) {
                                attendancePromptedStops.add(candidateIndex)
                            }
                        }

                        updateUpcomingStopsUI()
                        drawPointsOnMap(fullNavigationPoints)
                    }
                    break
                }
            }
        }

        // 2. Exiting Geofence: ARRIVED -> COMPLETED (one-way; never returns to UPCOMING/ARRIVED)
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
                if (departResults[0] > DEPARTURE_RADIUS) {
                    if (departureCandidateIndex == arrivedIndex) {
                        departureConfirmCount++
                    } else {
                        departureCandidateIndex = arrivedIndex
                        departureConfirmCount = 1
                    }

                    if (departureConfirmCount >= DEPARTURE_CONFIRM_THRESHOLD) {
                        // Successfully exited geofence: finalize this stop as COMPLETED.
                        // transitionToCompleted() both keeps the recorded arrival timestamp
                        // and advances nextGlobalStopIndex, shifting the ETA strictly to
                        // whatever stop comes next.
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

    /** Same GPS/geofence progression as the forward trip, without touching forward state. */
    private fun checkActiveTripGeofence(location: Location, stops: List<com.example.bustrack_app.models.StopItem>) {
        recordSourceArrivalIfNeeded(location)
        if (nextGlobalStopIndex >= stops.size) return
        if (!isCurrentlyAtStop) {
            val scanLimit = minOf(stops.size, nextGlobalStopIndex + 1 + SKIP_DETECTION_LOOKAHEAD_STOPS)
            for (index in nextGlobalStopIndex until scanLimit) {
                if (stateOf(index) != StopState.UPCOMING) continue
                val distance = FloatArray(1)
                Location.distanceBetween(location.latitude, location.longitude, stops[index].latitude, stops[index].longitude, distance)
                if (distance[0] <= ARRIVAL_RADIUS) {
                    for (skipped in nextGlobalStopIndex until index) transitionToSkipped(skipped)
                    nextGlobalStopIndex = index
                    if (transitionToArrived(index)) {
                        updateUpcomingStopsUI()
                        drawPointsOnMap(fullNavigationPoints)
                    }
                    break
                }
            }
        }
        if (isCurrentlyAtStop && lastArrivedStopIndex != -1) {
            val arrived = lastArrivedStopIndex
            val distance = FloatArray(1)
            Location.distanceBetween(location.latitude, location.longitude, stops[arrived].latitude, stops[arrived].longitude, distance)
            if (distance[0] > DEPARTURE_RADIUS) {
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

    /**
     * Arrival events can race with Android saving the Activity state.  Never force a
     * FragmentManager transaction from the GPS callback: it can throw and restart the
     * app.  A later GPS update will keep the stop state intact if the UI cannot be
     * shown at that exact moment.
     */
    private fun showAttendanceForStop(stopName: String): Boolean {
        if (!isMorningPickupTrip()) return false
        if (isFinishing || isDestroyed || supportFragmentManager.isStateSaved) return false
        if (supportFragmentManager.findFragmentByTag("AttendanceSheet") != null) return false

        AttendanceBottomSheet
            .newInstance(stopName, assignedRoute?.routeName.orEmpty(), true)
            .show(supportFragmentManager, "AttendanceSheet")
        return true
    }

    private fun updateUpcomingStopsUI() {
        val stops = displayedStops()

        // Show the full route stop list for the entire active trip. Stops must never be
        // removed once reached — only each stop's displayed status/time text changes
        // (that's driven by stop.time, which routeProgressObserver already sets per-index
        // every tick: "Arrived: ..."/"Skipped" for reached stops, "ETA: ..." for the rest).
        // liveArrivedIndex tells the adapter which single stop is currently inside its
        // geofence (-> ARRIVED badge); every other already-arrived stop still renders as
        // PASSED with its preserved arrival time, exactly as updateBottomSheetInfo() does.
        val liveArrivedIndex = if (isViewingReverseTrip && isCurrentlyAtStop && lastArrivedStopIndex != -1) {
            lastArrivedStopIndex
        } else {
            -1
        }

        stopsAdapter.updateStops(stops, liveArrivedIndex)
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

                // Stop arrival/skip decisions are made exclusively by geofence proximity,
                // in checkGeofenceAndStopStatus() below. Mapbox's currentLegProgress.legIndex
                // is intentionally NOT used to advance or skip stops here anymore: legIndex
                // only reflects progress along the *planned* route geometry, so it would
                // happily claim stops were reached the moment the bus's snapped position
                // moved past them on that geometry - even if the driver took a shortcut, is
                // mid-reroute, or the route hasn't caught up with a deviation yet.
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
                } else if (displayStopIndex < stops.size) {
                    "${durationRemaining.toInt()} min"
                } else {
                    "Route completed"
                }
                currentNavigationEtaText = etaString
                tvEta?.text = etaString
                // instructionCard's own ETA readout (tvEtaNav) — was never being written to
                // anywhere, so it stayed stuck on the "ETA: --" placeholder baked into the
                // layout XML, while tvEtaSheet (bottomSummaryCard, which can be scrolled out
                // of view during nav via the collapsible BottomSheetBehavior) updated fine.
                // Mirror the same value here; tvEtaSheet has no "ETA:" prefix (it sits next
                // to its own tvEtaLabel), but tvEtaNav's text carries the "ETA:" label itself,
                // so only prepend it for the plain-duration case.
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

                val legs = routeProgress.route.legs()
                var accumulatedSeconds = (routeProgress.currentLegProgress?.durationRemaining ?: 0.0).toInt()

                stops.forEachIndexed { index, stop ->
                    val arrivalTime = arrivalTimes[index]
                    if (arrivalTime == "Skipped") {
                        stop.time = if (isReverseTripActive && isViewingReverseTrip) "NOT VISITED" else "Skipped"
                    } else if (arrivalTime != null) {
                        stop.time = "Arrived: $arrivalTime"
                    } else if (index == displayStopIndex) {
                        val etaTime = Calendar.getInstance().apply { add(Calendar.SECOND, accumulatedSeconds) }.time
                        val etaText = "ETA: ${timeFormat.format(etaTime)}"
                        stop.time = etaText
                        etaTexts[index] = etaText
                        Log.d("ETA_DEBUG", "Stop=$index (current), remainingSeconds=$accumulatedSeconds, calculatedETA=$etaText")
                    } else if (index > displayStopIndex) {
                        if (legs != null && (index - navStartIndex) < legs.size) {
                            val legIdx = mapboxLegByOriginalStopIndex[index] ?: -1
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
        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(java.util.Date())

        FirebaseRepository.fetchStudentsByRoute(routeName) { students ->
            FirebaseRepository.fetchAttendance { allAttendance ->
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

                cachedLoadString = loadString
                runOnUiThread { binding.bottomSummaryCard.findViewById<TextView>(R.id.tvLoadSheet)?.text = cachedLoadString }
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

    // Accuracy-aware GPS hold: drop noisy stationary wander, keep real movement, and
    // never animate the puck for a full second (Mapbox's default) — overlapping
    // interpolations were sliding the bus backward and sideways between fixes.
    private fun feedRawLocationToPuck(location: Location) {
        if (location.hasAccuracy() && location.accuracy > MAX_ACCEPTABLE_PUCK_ACCURACY_METERS) {
            return
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
                return
            }
            if (isMoving && movedMeters < MIN_MOVING_PUCK_UPDATE_METERS) {
                return
            }
            if (lastPuckElapsedNanos > 0L && location.elapsedRealtimeNanos > lastPuckElapsedNanos) {
                val dtSec = (location.elapsedRealtimeNanos - lastPuckElapsedNanos) / 1_000_000_000.0
                if (dtSec in 0.05..12.0 && (movedMeters / dtSec) > MAX_PLAUSIBLE_PUCK_SPEED_MPS) {
                    return
                }
            }
        }
        lastPuckPosition = Location(location)
        lastPuckElapsedNanos = location.elapsedRealtimeNanos

        val animMs = if (previous == null || !isMoving) 0L else 280L
        navigationLocationProvider.changePosition(
            location = toMapboxLocation(location),
            keyPoints = emptyList(),
            latLngTransitionOptions = { duration = animMs },
            bearingTransitionOptions = { duration = animMs }
        )
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), 1001)
            return
        }

        try {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    // lastLocation is an asynchronous cached read. It may return after
                    // a newer callback; never let that stale point pull the puck back.
                    val cacheAgeMs = System.currentTimeMillis() - location.time
                    if (cacheAgeMs > STALE_CACHED_LOCATION_MAX_AGE_MS) return@addOnSuccessListener
                    if (location.elapsedRealtimeNanos <= lastAcceptedLocationElapsedNanos) return@addOnSuccessListener
                    lastAcceptedLocationElapsedNanos = location.elapsedRealtimeNanos
                    val wasLive = isCurrentLocationLive
                    currentLocation = Location(location)
                    isCurrentLocationLive = true
                    feedRawLocationToPuck(location)
                    if (!wasLive) {
                        // FIX (dashboard auto-zoom): don't let the very first live GPS
                        // fix trigger a camera re-fit around the (now smaller) dynamic
                        // route preview - only refresh markers/route, keep camera as-is.
                        shouldFitCameraToRoute = false
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
                    // Fused can batch several old fixes. Animating every member of the
                    // batch replays the route visually; only the newest fix is valid for
                    // the bus marker and navigation state.
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
        }
    }

    /** Single coordinated route-state path. Raw Fused GPS is authoritative. */
    private fun handleLocationUpdate(location: Location) {
        if (!isDutyEnabled) return
        val wasLive = isCurrentLocationLive
        currentRawLocation = Location(location)
        currentLocation = Location(location)
        isCurrentLocationLive = true

        // The newest Fused GPS fix is the single visual-puck authority, both before
        // and during navigation. Mapbox matcher callbacks no longer animate it.
        feedRawLocationToPuck(location)

        // This callback is the app's authoritative live GPS source.  Do the
        // reverse-geocode here rather than relying on Mapbox's matcher callback,
        // which is not guaranteed to emit while a route is being rebuilt.
        if (isNavigating) {
            updateLocationSummary(location)
            reverseGeocodeIfNeeded(location)
        }

        if (isNavigating) {
            checkGeofenceAndStopStatus(location)
            updateNavigationRouteProgress(Point.fromLngLat(location.longitude, location.latitude))
        } else if (!wasLive) {
            // FIX (dashboard auto-zoom): same reasoning as startLocationUpdates() above.
            shouldFitCameraToRoute = false
            updateMapDisplay()
        }

        // Persist only after the route has been split at this GPS point. Admin,
        // Parent and Principal then receive marker + blue/grey line in one snapshot.
        syncTrackingDataToFirestore(location)
    }

    /** Immediately removes the layout placeholder while a human-readable address loads. */
    private fun updateLocationSummary(location: Location) {
        // Coordinates are useful for diagnostics but are not a driver-facing address.
        // Until geocoding completes, show the closest named stop instead of latitude /
        // longitude so this field always remains human-readable.
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

    private fun syncTrackingDataToFirestore(location: Location) {
        val driverId = viewModel.currentDriver.value?.driverId ?: return
        if (!isDutyEnabled) return

        val now = System.currentTimeMillis()
        val distanceMoved = lastFirestoreLocation?.distanceTo(location) ?: Float.MAX_VALUE

        val elapsed = now - lastFirestoreUpdateTime
        // Regular movement is paced at 1 s AND 2 m. A 10 s heartbeat preserves a
        // fresh lastUpdated value while the bus is stationary or GPS is noisy.
        if ((elapsed >= FIRESTORE_UPDATE_INTERVAL && distanceMoved >= FIRESTORE_MIN_DISTANCE) || elapsed >= 10000L) {

            val sheet = binding.bottomSummaryCard
            val tvEta = sheet.findViewById<TextView>(R.id.tvEtaSheet)
            val tvLoad = sheet.findViewById<TextView>(R.id.tvLoadSheet)

            val etaVal = currentNavigationEtaText
                ?.takeUnless { it == "--" || it == "Calculating..." }
                ?: "On Way"
            val speedVal = (location.speed * 3.6)
            val loadVal = tvLoad?.text?.toString() ?: "0/0"

            // Single consolidated write (was 3 separate .update() calls: location,
            // stats, route geometry) - see updateDriverLiveState() for why this
            // matters now that this runs roughly every ~1s instead of every ~5s.
            // stopEtaTexts (computed every tick in routeProgressObserver) is always
            // forwarded here too, so Firestore's stopEtaTimes field stays in sync
            // and Parent/Admin/Principal (TrackDriverActivity.applyDriverStopState)
            // can show a real per-stop ETA instead of falling back to "TBD".
            val arrivalMap = stopArrivalTimes.mapKeys { it.key.toString() }
            val etaMap = stopEtaTexts.mapKeys { it.key.toString() }
            val traveledSegments = currentTraveledSegments().map { LineString.fromLngLats(it).toPolyline(6) }
            FirebaseRepository.updateDriverLiveState(
                driverId,
                location.latitude,
                location.longitude,
                etaVal,
                speedVal,
                loadVal,
                currentRouteGeometry,
                traveledRouteGeometry,
                nextGlobalStopIndex,
                arrivalMap,
                etaMap,
                isNavigating,
                traveledSegments
            )

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

    private fun centerCameraOnUser() {
        val targetPoint = currentLocation?.let {
            Point.fromLngLat(it.longitude, it.latitude)
        } ?: Point.fromLngLat(73.0535, 33.5985)

        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(targetPoint)
                .zoom(16.0)
                .pitch(0.0)
                .build()
        )
    }

    private fun setupInitialCamera() {
        centerCameraOnUser()
    }

    private fun setupLocationPuck() {
        val cameraState = mapView?.mapboxMap?.cameraState
        val initialScale = computeBusModelScale(
            cameraState?.zoom ?: BUS_MODEL_SCALE_REFERENCE_ZOOM,
            cameraState?.pitch ?: 0.0
        )
        lastAppliedBusScale = initialScale

        mapView?.location?.apply {
            setLocationProvider(navigationLocationProvider)
            enabled = isDutyEnabled
            pulsingEnabled = isDutyEnabled
            puckBearingEnabled = true

            locationPuck = LocationPuck3D(
                modelUri = "asset://bus.glb",
                modelScale = listOf(initialScale, initialScale, initialScale),
                modelScaleExpression = busModelScaleExpression(),
                modelScaleMode = ModelScaleMode.MAP,
                modelTranslation = listOf(0f, 0f, 0f),
                modelRotation = listOf(BUS_MODEL_ROLL_OFFSET_X_DEG, BUS_MODEL_ROLL_OFFSET_Y_DEG, 90f)
            )
        }

        if (isCurrentLocationLive) {
            currentLocation?.let { feedRawLocationToPuck(it) }
        }
        mapView?.post {
            lastAppliedBusScale = -1f
            updateBusModelScaleForZoom()
        }
    }

    // Reassigning LocationPuck3D while the location component is active asks Mapbox
    // to add its model layer again. That can throw a fatal JNI exception because the
    // existing "mapbox-location-model-layer" already belongs to the component.
    // Scale is updated on that existing layer only.
    private val cameraChangeListener = OnCameraChangeListener {
        updateBusModelScaleForZoom()
    }

    private fun busModelScaleExpression(): String {
        val zoomStops = listOf(10.0, 12.0, 14.0, 16.0, 17.0, 17.5, 18.0, 19.0, 20.0)
        val interpolated = zoomStops.joinToString(",") { zoom ->
            val pitch = if (zoom >= 17.5) 65.0 else 0.0
            val scale = computeBusModelScale(zoom, pitch)
            """$zoom,["literal",[$scale,$scale,$scale]]"""
        }
        return """["interpolate",["linear"],["zoom"],$interpolated]"""
    }

    private fun computeBusModelScale(zoom: Double, pitch: Double = 0.0): Float {
        val pitchCompensation = 1.0 / kotlin.math.cos(Math.toRadians(pitch))
            .coerceAtLeast(BUS_MODEL_PITCH_COMPENSATION_FLOOR)

        val apparentExponent = (BUS_MODEL_SCALE_REFERENCE_ZOOM - zoom) * BUS_MODEL_SCALE_COMPENSATION_FACTOR
        val apparentTarget = (BUS_MODEL_SCALE_REFERENCE_VALUE * Math.pow(2.0, apparentExponent))
            .coerceIn(MIN_BUS_MODEL_SCALE.toDouble(), MAX_BUS_MODEL_SCALE.toDouble())

        val worldToScreenCompensation = Math.pow(2.0, BUS_MODEL_SCALE_REFERENCE_ZOOM - zoom) * pitchCompensation
        return (apparentTarget * worldToScreenCompensation)
            .coerceIn(MIN_BUS_WORLD_SCALE.toDouble(), MAX_BUS_WORLD_SCALE.toDouble())
            .toFloat()
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
        val cameraState = mapView?.mapboxMap?.cameraState ?: return
        val newScale = computeBusModelScale(cameraState.zoom, cameraState.pitch)
        if (kotlin.math.abs(newScale - lastAppliedBusScale) < 0.02f) return
        lastAppliedBusScale = newScale

        val scaleVec = listOf(newScale, newScale, newScale)
        (mapView?.location?.locationPuck as? LocationPuck3D)?.modelScale = scaleVec

        mapView?.mapboxMap?.getStyle { style ->
            val modelLayer = locationModelLayer(style) ?: return@getStyle
            modelLayer.modelScale(listOf(newScale.toDouble(), newScale.toDouble(), newScale.toDouble()))
        }
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

                // Rebuild stopStates from the restored arrival map (e.g. after process death /
                // activity recreation): any recorded stop before the current pointer is
                // COMPLETED, the recorded stop at the current pointer is still ARRIVED (being
                // serviced), "Skipped" entries become SKIPPED, everything else is UPCOMING.
                if (stopStates.isEmpty() && stopArrivalTimes.isNotEmpty()) {
                    stopArrivalTimes.forEach { (index, value) ->
                        stopStates[index] = when {
                            value == "Skipped" -> StopState.SKIPPED
                            index < nextGlobalStopIndex -> StopState.COMPLETED
                            else -> StopState.ARRIVED
                        }
                    }
                }

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
                val routeChangedWhileNavigating = assignedRoute?.id != route.id && isNavigating
                if (assignedRoute?.id != route.id) {
                    isNearStart = false
                    shouldFitCameraToRoute = true
                    stopArrivalTimes.clear()
                    stopStates.clear()
                    stopEtaTexts.clear()
                    nextGlobalStopIndex = 0
                    attendancePromptedStops.clear()
                    // A route assignment can change while the bus is navigating.
                    // Keep the already-driven road segment as a completed segment;
                    // only the upcoming line is replaced by the new route.
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
                if (routeChangedWhileNavigating) {
                    // The route assignment is now updated, so calculate the new
                    // upcoming path while retaining the frozen travelled segments.
                    triggerReroute()
                }
                refreshLoadStat()
                // route.stopsList is a fresh set of StopItem instances (time defaults to "").
                // Restore last-known display text from persisted, index-keyed state before
                // anything (map markers, bottom sheet, adapter) reads stop.time.
                route.stopsList.forEachIndexed { index, stop ->
                    val arrival = stopArrivalTimes[index]
                    stop.time = when {
                        arrival == "Skipped" -> "Skipped"
                        arrival != null -> "Arrived: $arrival"
                        else -> stopEtaTexts[index] ?: "TBD"
                    }
                }
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

        try {
            // 1. Check deviation against the route BEFORE any movement gating.
            // If the driver deviated or moved to another road, detect it immediately
            // regardless of whether vehicle is moving or stationary.
            val snappedPoint = TurfMisc.nearestPointOnLine(currentPos, fullNavigationPoints)
            val snappedP = snappedPoint.geometry() as? Point ?: return
            val actualDistanceToRoute = TurfMeasurement.distance(currentPos, snappedP, TurfConstants.UNIT_METERS)
            val nearestRouteIndex = fullNavigationPoints.indices.minByOrNull { index ->
                TurfMeasurement.distance(currentPos, fullNavigationPoints[index], TurfConstants.UNIT_METERS)
            } ?: 0
            val isFacingOppositeRouteDirection = currentLocation
                ?.takeIf { it.hasBearing() && it.speed >= MIN_SPEED_FOR_BEARING_UPDATE }
                ?.let { location ->
                    val routeBearing = routeBearingAt(nearestRouteIndex)
                    routeBearing != null && headingDifference(location.bearing.toDouble(), routeBearing) >= OPPOSITE_DIRECTION_REROUTE_DEGREES
                } ?: false

            // A GPS point can be close to the geometry of a parallel/two-way road
            // while the bus is travelling in the opposite direction. Distance alone
            // incorrectly advances the grey line in that case; direction detects it
            // and forces a route recalculation from the road the bus is actually on.
            val isOffRoute = actualDistanceToRoute > OFF_ROUTE_THRESHOLD_METERS ||
                    (actualDistanceToRoute > PARALLEL_ROAD_OFF_ROUTE_THRESHOLD_METERS && isFacingOppositeRouteDirection)
            if (isNavigating && isOffRoute) {
                val now = System.currentTimeMillis()
                if (!isRerouteInFlight && now - lastOffRouteRerouteTimeMs > MIN_OFFROUTE_REROUTE_GAP_MS) {
                    lastOffRouteRerouteTimeMs = now
                    Log.d("NavDebug", "Bus deviated from route ($actualDistanceToRoute m away). Triggering immediate reroute...")
                    triggerReroute()
                }
                return
            }

            // 2. Minimum movement filter: only filters updates when vehicle is strictly ON ROUTE
            val previousRawPosition = lastRawPositionForSnap
            previousRawPosition?.let { lastRaw ->
                val movedMeters = TurfMeasurement.distance(currentPos, lastRaw, TurfConstants.UNIT_METERS)
                if (movedMeters < MIN_GPS_MOVEMENT_FOR_SNAP_METERS) {
                    return
                }
            }
            lastRawPositionForSnap = currentPos

            // 3. Find split index along the planned route
            val searchStart = lastSplitIndex
            val searchEnd = minOf(fullNavigationPoints.size - 1, lastSplitIndex + SPLIT_SEARCH_WINDOW)
            var splitIndex = searchStart
            var minWindowDistance = Double.MAX_VALUE
            val maxForwardRouteDistance = previousRawPosition?.let { previous ->
                // Allow normal GPS noise and road curvature, but never a sudden jump
                // to the return carriageway far ahead in the route order.
                maxOf(MIN_FORWARD_ROUTE_PROGRESS_METERS,
                    TurfMeasurement.distance(currentPos, previous, TurfConstants.UNIT_METERS) * 3.0 + 30.0)
            } ?: MIN_FORWARD_ROUTE_PROGRESS_METERS
            var forwardRouteDistance = 0.0
            for (i in searchStart..searchEnd) {
                if (i > searchStart) {
                    forwardRouteDistance += TurfMeasurement.distance(
                        fullNavigationPoints[i - 1], fullNavigationPoints[i], TurfConstants.UNIT_METERS
                    )
                }
                if (forwardRouteDistance > maxForwardRouteDistance) break
                val dist = TurfMeasurement.distance(currentPos, fullNavigationPoints[i], TurfConstants.UNIT_METERS)
                if (dist < minWindowDistance) {
                    minWindowDistance = dist
                    splitIndex = i
                }
            }
            splitIndex = maxOf(splitIndex, lastSplitIndex)
            lastSplitIndex = splitIndex

            // 4. Road-Following Traveled Line:
            // Sliced strictly from the route's road polyline geometry (never straight-line GPS connections)
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

    private fun routeBearingAt(index: Int): Double? {
        val (startIndex, endIndex) = if (index < fullNavigationPoints.lastIndex) {
            index to index + 1
        } else {
            index - 1 to index
        }
        val start = fullNavigationPoints.getOrNull(startIndex) ?: return null
        val end = fullNavigationPoints.getOrNull(endIndex) ?: return null
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
                drawerLayout.openDrawer(GravityCompat.END)
                Toast.makeText(this, "Please enable On Duty Mode before starting navigation.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (route == null) {
                Toast.makeText(this, "No route assigned to you yet", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentLocation == null) {
                Toast.makeText(this, "Fetching current location...", Toast.LENGTH_SHORT).show()
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                ) {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                currentLocation = location
                                handleStartNavigation(route)
                            } else {
                                Toast.makeText(this@DriverDashboardActivity, "Unable to fetch location. Please check your GPS.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } catch (e: SecurityException) {
                        Log.e("NavDebug", "SecurityException accessing last location: ${e.message}", e)
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

        binding.bottomSummaryCard.findViewById<View>(R.id.btnStartReturnTrip)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            showStartReturnTripConfirmation()
        }

        binding.btnNotifications.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, NotificationActivity::class.java))
        }
    }

    private fun handleStartNavigation(route: RouteModel) {
        // A new forward navigation session gets one source-arrival/drop event.
        if (!isReverseTripActive) {
            sourceArrivalRecordedForCurrentTrip = false
            viewModel.currentDriver.value?.driverId?.let {
                FirebaseRepository.updateDriverTripDirection(it, "FORWARD")
            }
        }
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
            Location.distanceBetween(
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

    private fun beginReverseTrip() {
        val forwardStops = assignedRoute?.stopsList.orEmpty()
        val location = currentLocation
        if (forwardStops.isEmpty() || location == null) {
            Toast.makeText(this, "Current location and route stops are required for reverse trip", Toast.LENGTH_SHORT).show()
            return
        }
        // Copying is important: the adapter updates StopItem.time, so reusing the
        // forward objects would overwrite the forward-trip display/history.
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
        // SKIPPED without changing the original forward state/history.
        reverseForwardStopIndexes.forEachIndexed { reverseIndex, forwardIndex ->
            if ((stopStates[forwardIndex] ?: StopState.UPCOMING) == StopState.UPCOMING) {
                reverseStopStates[reverseIndex] = StopState.SKIPPED
                reverseStopArrivalTimes[reverseIndex] = "Skipped"
            }
        }
        isReverseTripActive = true
        isViewingReverseTrip = true
        viewModel.currentDriver.value?.driverId?.let {
            FirebaseRepository.updateDriverTripDirection(it, "RETURN")
        }
        reverseTripCompleted = false
        nextGlobalStopIndex = 0
        navStartIndex = 0
        lastArrivedStopIndex = -1
        isCurrentlyAtStop = false
        departureCandidateIndex = -1
        departureConfirmCount = 0
        attendancePromptedStops.clear()
        sourceArrivalRecordedForCurrentTrip = false
        currentNavigationEtaText = null
        clearTraveledRouteHistory()
        updateBottomSheetInfo()
        startNavigationAnimation()
        Toast.makeText(this, "Return trip started", Toast.LENGTH_SHORT).show()
    }

    private fun showStartReturnTripConfirmation() {
        if (isReverseTripActive) {
            Toast.makeText(this, "Return trip is already active", Toast.LENGTH_SHORT).show()
            return
        }
        AlertDialog.Builder(this)
            .setTitle("Start Return Trip?")
            .setMessage("Are you sure you want to start the return trip? The bus will begin travelling back toward the source.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Start Return") { _, _ -> beginReverseTrip() }
            .show()
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
        sheet.findViewById<TextView>(R.id.tvRouteSheet)?.text = when {
            isReverseTripActive && isViewingReverseTrip -> "${route.routeName} • Reverse Trip"
            isReverseTripActive -> "${route.routeName} • Forward Trip"
            else -> route.routeName
        }
        sheet.findViewById<TextView>(R.id.tvUpcomingLabel)?.text = when {
            isReverseTripActive && isViewingReverseTrip -> "Reverse Trip Stops"
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

        val liveArrivedIndexForSheet = if (isViewingReverseTrip && isCurrentlyAtStop && lastArrivedStopIndex != -1) lastArrivedStopIndex else -1
        stopsAdapter.updateStops(stops, liveArrivedIndexForSheet)
    }

    private fun startFollowingPuck() {
        binding.btnRecenter.visibility = View.GONE
        if (isNorthUp) {
            followPuckNorthUp()
        } else {
            followPuckHeadingUp()
        }
        lastAppliedBusScale = -1f
        updateBusModelScaleForZoom()
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
            val maxVisitedIdx = activeArrivalTimes().keys.maxOrNull() ?: -1
            var startIndex = Math.max(nextGlobalStopIndex, maxVisitedIdx + 1)

            if (startIndex >= activeStops().size) {
                // A completed reverse trip stays completed; it must never roll into a
                // fresh forward trip automatically.
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
                originalRouteSource()?.let { navPoints.add(it) }
            } else if (remainingStops.isEmpty() && route.pathPoints.isNotEmpty()) {
                navPoints.add(Point.fromLngLat(route.pathPoints.last().longitude, route.pathPoints.last().latitude))
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

        val currentBearing = currentLocation?.let { loc ->
            if (loc.hasBearing() && loc.bearing != 0f) loc.bearing.toDouble()
            else if (lastValidBearing != 0.0) lastValidBearing
            else null
        }

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
            .alternatives(false)

        if (currentBearing != null) {
            val bearings = mutableListOf<Bearing?>()
            bearings.add(Bearing.builder().angle(currentBearing).degrees(45.0).build())
            for (i in 1 until navPoints.size) {
                bearings.add(null)
            }
            routeOptionsBuilder.bearingsList(bearings)
        }

        nav.requestRoutes(
            routeOptionsBuilder.build(),
            object : NavigationRouterCallback {
                override fun onRoutesReady(routes: List<NavigationRoute>, routerOrigin: String) {
                    isNavigating = true
                    nav.setNavigationRoutes(routes)
                    updateStopEtasFromNavigationRoute(routes.first())

                    // Pre-populate Google Maps style instruction card with the route's initial maneuver
                    val firstLeg = routes.first().directionsRoute.legs()?.firstOrNull()
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

                    startFollowingPuck()
                    setNavigationMode(true, showPlaceholderInstruction = false)
                }
                override fun onFailure(reasons: List<RouterFailure>, routeOptions: RouteOptions) {
                    if (!routeOptions.bearingsList().isNullOrEmpty()) {
                        Log.w("NavDebug", "Initial route with bearing failed, retrying without bearing constraints...")
                        val unconstrainedOptions = routeOptions.toBuilder().bearingsList(null).build()
                        nav.requestRoutes(unconstrainedOptions, this)
                        return
                    }
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

    // FIX (static card on start): startNavigationAnimation()'s onRoutesReady callback
    // populates the card with the route's real first maneuver, then immediately calls
    // this function - which used to unconditionally overwrite that real data with
    // "Navigation starting" / "Distance: --" placeholders below. showPlaceholderInstruction
    // lets a caller that already wrote real data opt out of clobbering it.
    private fun setNavigationMode(isNavigating: Boolean, reloadStyle: Boolean = true, showPlaceholderInstruction: Boolean = true) {
        this.isNavigating = isNavigating
        cancelDutyAutoOffTimer()
        binding.apply {
            if (isNavigating) {
                val isNewNavigationSession = !navigationUiActive
                navigationUiActive = true
                if (isNewNavigationSession) {
                    clearTraveledRouteHistory()
                    traveledRouteGeometry = null
                    currentRouteGeometry = null
                }

                toolbar.visibility = View.GONE
                headerBg.visibility = View.GONE
                dashboardTopContent.visibility = View.GONE

                // Give the driver a useful card immediately; the next route-progress
                // update fills in the real instruction, road, distance and ETA.
                instructionCard.visibility = View.VISIBLE
                maneuverView.visibility = View.GONE
                if (showPlaceholderInstruction) {
                    tvNextInstruction.text = "Navigation starting"
                    tvNextStreet.text = "Finding next road"
                    tvDistanceNav.text = "Distance: --"
                    tvEtaNav.text = "ETA: --"
                }

                updateBottomSheetTheme(true)

                if (isNewNavigationSession) mapView?.mapboxMap?.loadStyle("mapbox://styles/mapbox/navigation-night-v1") { style ->
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

                        // FIX (route line missing after restart): on restart, this style's
                        // loadStyle callback runs *after* routesObserver already tried to
                        // draw the route once against the *old* style - before this style's
                        // sources existed, so that draw silently failed but still recorded
                        // currentLocation as lastRawPositionForSnap. Without this reset,
                        // updateNavigationRouteProgress() below sees near-zero movement
                        // since that recording and returns early, leaving the route line
                        // undrawn until the next genuine >=1m GPS fix arrives.
                        lastRawPositionForSnap = null
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

                // Do not write null route geometry when this is merely a reroute/UI
                // refresh: that write was erasing the persisted grey travelled line.
                if (isNewNavigationSession) {
                    viewModel.currentDriver.value?.driverId?.let { driverId ->
                        val arrivalMap = stopArrivalTimes.mapKeys { it.key.toString() }
                        val etaMap = stopEtaTexts.mapKeys { it.key.toString() }
                        FirebaseRepository.updateDriverRouteGeometry(
                            driverId, null, null, nextGlobalStopIndex, arrivalMap, true, etaMap
                        )
                    }
                }

                lastLegIndex = -1

                infoBar.setBackgroundColor(Color.parseColor("#0D1B3E"))
                layoutMapControls.visibility = View.VISIBLE
                layoutMapControls.animate().translationY(-240f).setDuration(500).start()
                btnRecenter.animate().translationY(-240f).setDuration(500).start()
            } else {
                navigationUiActive = false
                mapboxNavigation?.setNavigationRoutes(emptyList())
                fullNavigationPoints = emptyList()
                clearTraveledRouteHistory()
                currentRouteGeometry = null
                traveledRouteGeometry = null

                mapView?.viewport?.idle()
                mapView?.mapboxMap?.setCamera(CameraOptions.Builder().padding(EdgeInsets(0.0, 0.0, 0.0, 0.0)).build())

                toolbar.visibility = View.VISIBLE
                headerBg.visibility = View.VISIBLE
                dashboardTopContent.visibility = View.VISIBLE
                instructionCard.visibility = View.GONE
                maneuverView.visibility = View.GONE

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
                // Ending navigation must not leave Start Navigation stuck disabled/grey.
                // Its enabled/color state depends only on On Duty status, never on
                // navigation state, so re-assert it explicitly here.
                updateNavigationButtonState()

                bottomSummaryCard.visibility = View.GONE
                bottomSheetBehavior.isHideable = true
                bottomSheetBehavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN

                infoBar.setBackgroundColor(Color.TRANSPARENT)
                layoutMapControls.animate().translationY(0f).setDuration(500).start()

                viewModel.currentDriver.value?.driverId?.let { driverId ->
                    val arrivalMap = stopArrivalTimes.mapKeys { it.key.toString() }
                    lastDutyToggleTime = System.currentTimeMillis()
                    FirebaseRepository.updateDriverRouteGeometry(
                        driverId, null, null, nextGlobalStopIndex, arrivalMap, false
                    )
                }
            }
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

                viewModel.currentDriver.value?.driverId?.let { driverId ->
                    val routeName = assignedRoute?.routeName
                    FirebaseRepository.updateDriverStatus(driverId, "Active", routeName)
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

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            ) {
                try {
                    mapboxNavigation?.startTripSession()
                } catch (e: SecurityException) {
                    Log.e("DutyDebug", "SecurityException starting trip session: ${e.message}", e)
                }
            }

            viewModel.currentDriver.value?.driverId?.let { driverId ->
                FirebaseRepository.updateDriverStatus(driverId, "Active")
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
            stopStates.clear()
            stopEtaTexts.clear()
            nextGlobalStopIndex = 0
            attendancePromptedStops.clear()
            clearTraveledRouteHistory()

            viewModel.currentDriver.value?.driverId?.let { driverId ->
                FirebaseRepository.updateDriverStatus(driverId, "Inactive")
                FirebaseRepository.updateDriverRouteGeometry(
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
                        FirebaseRepository.updateDriverStatus(driverId, "Inactive")
                        FirebaseRepository.updateDriverRouteGeometry(
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
        // Unregistering through the binder (rather than mapboxNavigation?.unregisterX)
        // guarantees cleanup even if mapboxNavigation was never successfully bound.
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

    override fun onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END)
        } else {
            super.onBackPressed()
        }
    }
}