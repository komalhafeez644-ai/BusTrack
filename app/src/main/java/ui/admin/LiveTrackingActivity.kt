package ui.admin

import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import com.example.bustrack_app.R
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.viewmodels.LiveTrackingViewModel
import com.google.android.material.button.MaterialButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.easeTo
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.maps.extension.style.layers.generated.modelLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.layers.properties.generated.ModelType
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.extension.style.expressions.dsl.generated.*
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.RenderedQueryGeometry
import com.mapbox.maps.RenderedQueryOptions
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.gson.JsonArray

class LiveTrackingActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private val viewModel: LiveTrackingViewModel by viewModels()
    private val driverMarkers = mutableMapOf<String, PointAnnotation>()
    private val driverPreviousPositions = mutableMapOf<String, Point>()
    private val driverRenderedPositions = mutableMapOf<String, Point>()
    private val driverAnimators = mutableMapOf<String, ValueAnimator>()
    private val driverFixTimestamps = mutableMapOf<String, Long>()
    private val driverBearings = mutableMapOf<String, Double>()
    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    private var isUserInteracting = false
    private lateinit var searchAdapter: BusSearchAdapter
    private var unavailableDialog: android.app.Dialog? = null
    private var isUnavailablePopupDismissed = false
    private var isMapStyleReady = false
    private var pendingDrivers: List<DriverModel> = emptyList()
    private var hasInitializedTrackingCamera = false
    private var markerUpdateGeneration = 0L
    private var busGeoJsonSource: com.mapbox.maps.extension.style.sources.generated.GeoJsonSource? = null

    private val BUS_SOURCE_ID = "bus-source"
    private val BUS_MODEL_LAYER_ID = "bus-model-layer"
    private val BUS_LABEL_LAYER_ID = "bus-label-layer"
    private val BUS_MODEL_ID = "bus-model-id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking)

        supportActionBar?.hide()

        // Check for Parent Mode filter
        val allowedRoute = intent.getStringExtra("ALLOWED_ROUTE")
        if (allowedRoute != null) {
            viewModel.setAllowedRoute(allowedRoute)
            setupParentUI()
        }

        mapView = findViewById(R.id.mapView)
        // Start observing immediately and keep the latest snapshot for the loaded map.
        observeViewModel()
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) { style ->
            isMapStyleReady = true
            style.addStyleModel(BUS_MODEL_ID, "asset://bus.glb")

            val annotationApi = mapView?.annotations
            pointAnnotationManager = annotationApi?.createPointAnnotationManager()

            // Add bus icon to map style
            val bitmap = bitmapFromDrawableRes(this@LiveTrackingActivity, R.drawable.ic_marker_bus)
            bitmap?.let { style.addImage("bus-icon", it) }

            // Default center on FG Post Graduate College, Saddar (Rawalpindi)
            val defaultPoint = Point.fromLngLat(73.0478, 33.5977)
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(defaultPoint)
                    .zoom(15.0)
                    .build()
            )

            // Detect taps on rendered bus markers.
            mapView?.gestures?.addOnMapClickListener { point ->
                val screenCoordinate = mapView?.mapboxMap?.pixelForCoordinate(point)
                if (screenCoordinate != null) {
                    mapView?.mapboxMap?.queryRenderedFeatures(
                        RenderedQueryGeometry(screenCoordinate),
                        RenderedQueryOptions(listOf(BUS_MODEL_LAYER_ID, BUS_LABEL_LAYER_ID), null)
                    ) { result ->
                        val driverId = result.value
                            ?.firstOrNull()
                            ?.queriedFeature?.feature?.getStringProperty("driverId")
                        val driver = driverId?.let { id ->
                            viewModel.activeDrivers.value?.find { it.driverId == id }
                        }
                        driver?.let {
                            isUserInteracting = false
                            viewModel.selectDriver(it)
                            focusOnDriver(it)
                        }
                    }
                }
                false
            }

            if (pendingDrivers.isNotEmpty()) updateMarkers(pendingDrivers)
        }

        setupUI()
    }

    private fun setupParentUI() {
        // Hide Search Bar for Parents as they only see one bus
        findViewById<View>(R.id.searchContainer)?.visibility = View.GONE
        findViewById<View>(R.id.searchSuggestionsCard)?.visibility = View.GONE

        // Match Parent Module Style
        findViewById<View>(R.id.header)?.setBackgroundResource(R.drawable.bg_header_blue)
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Live Bus Tracking"

        // Hide Bottom Nav if it exists (Parents use their own dashboard navigation)
        findViewById<View>(R.id.bottomNavInclude)?.visibility = View.GONE
    }

    private fun setupUI() {
        findViewById<MaterialButton>(R.id.btnTrackDriver)?.setOnClickListener {
            val selected = viewModel.selectedDriver.value
            if (selected != null) {
                val intent = Intent(this, TrackDriverActivity::class.java)
                intent.putExtra("DRIVER_ID", selected.driverId)
                // Pass Parent Mode state
                if (getIntent().hasExtra("ALLOWED_ROUTE")) {
                    intent.putExtra("IS_PARENT", true)
                }
                startActivity(intent)
            } else {
                android.widget.Toast.makeText(this, "Please select a bus to track", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        // Setup Search Suggestions
        val rvSuggestions = findViewById<RecyclerView>(R.id.rvSearchSuggestions)
        val cardSuggestions = findViewById<View>(R.id.searchSuggestionsCard)

        rvSuggestions.layoutManager = LinearLayoutManager(this)
        searchAdapter = BusSearchAdapter { driver ->
            isUserInteracting = false
            viewModel.selectDriver(driver)
            if (driver.latitude != 0.0 && driver.longitude != 0.0) {
                focusOnDriver(driver)
            } else {
                android.widget.Toast.makeText(this, "Bus is currently offline", android.widget.Toast.LENGTH_SHORT).show()
            }
            cardSuggestions.visibility = View.GONE
            findViewById<EditText>(R.id.etSearchBus).setText(driver.assignedBus ?: driver.name)
            findViewById<EditText>(R.id.etSearchBus).clearFocus()
        }
        rvSuggestions.adapter = searchAdapter

        findViewById<EditText>(R.id.etSearchBus)?.setOnClickListener {
            if (cardSuggestions.visibility == View.GONE) {
                val drivers = viewModel.allDriversForSearch.value ?: emptyList()
                searchAdapter.updateData(drivers)
                if (drivers.isNotEmpty()) cardSuggestions.visibility = View.VISIBLE
            }
        }

        findViewById<EditText>(R.id.etSearchBus)?.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                val drivers = viewModel.allDriversForSearch.value ?: emptyList()
                searchAdapter.updateData(drivers)
                if (drivers.isNotEmpty()) cardSuggestions.visibility = View.VISIBLE
            } else {
                cardSuggestions.postDelayed({ cardSuggestions.visibility = View.GONE }, 200)
            }
        }

        findViewById<EditText>(R.id.etSearchBus)?.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            val drivers = viewModel.allDriversForSearch.value ?: emptyList()

            if (query.isNotEmpty()) {
                val filtered = drivers.filter {
                    it.name.lowercase().contains(query) || it.assignedBus?.lowercase()?.contains(query) == true
                }
                searchAdapter.updateData(filtered)
                cardSuggestions.visibility = if (filtered.isNotEmpty()) View.VISIBLE else View.GONE
            } else if (findViewById<EditText>(R.id.etSearchBus).isFocused) {
                searchAdapter.updateData(drivers)
                cardSuggestions.visibility = if (drivers.isNotEmpty()) View.VISIBLE else View.GONE
            }
        }

        mapView?.gestures?.addOnMoveListener(object : com.mapbox.maps.plugin.gestures.OnMoveListener {
            override fun onMoveBegin(detector: com.mapbox.android.gestures.MoveGestureDetector) {
                isUserInteracting = true
            }
            override fun onMove(detector: com.mapbox.android.gestures.MoveGestureDetector): Boolean = false
            override fun onMoveEnd(detector: com.mapbox.android.gestures.MoveGestureDetector) {}
        })
    }

    private fun observeViewModel() {
        viewModel.activeDrivers.observe(this) { drivers ->
            pendingDrivers = drivers
            if (drivers.isEmpty()) {
                findViewById<View>(R.id.driverCard).visibility = View.GONE
            } else {
                unavailableDialog?.dismiss()
                unavailableDialog = null
                isUnavailablePopupDismissed = false
            }
            if (isMapStyleReady) updateMarkers(drivers)
        }

        viewModel.selectedDriver.observe(this) { driver ->
            updateDriverCard(driver)
        }

        viewModel.trackingStatus.observe(this) { status ->
            when (status) {
                "OFF_DUTY" -> showUnavailableDialog()
                "AVAILABLE" -> {
                    unavailableDialog?.dismiss()
                    unavailableDialog = null
                    isUnavailablePopupDismissed = false
                }
            }
        }
    }

    private fun showUnavailableDialog() {
        if (unavailableDialog?.isShowing == true || isUnavailablePopupDismissed) return

        unavailableDialog = android.app.Dialog(this)
        unavailableDialog?.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        unavailableDialog?.setContentView(R.layout.dialog_request_submitted)
        unavailableDialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
        unavailableDialog?.setCancelable(false)

        val tvTitle = unavailableDialog?.findViewById<TextView>(R.id.tvStatusTitle)
        val tvMsg = unavailableDialog?.findViewById<TextView>(R.id.tvStatusMessage)
        val tvFooter = unavailableDialog?.findViewById<TextView>(R.id.tvFooterStatus)
        val ivIcon = unavailableDialog?.findViewById<ImageView>(R.id.ivStatusIcon)
        val btnOk = unavailableDialog?.findViewById<android.widget.Button>(R.id.btnOk)

        btnOk?.visibility = View.VISIBLE
        btnOk?.setOnClickListener {
            isUnavailablePopupDismissed = true
            unavailableDialog?.dismiss()
        }

        tvTitle?.text = "Live Tracking Unavailable"
        tvMsg?.text = "There are no buses currently on duty. Live location will be available when a bus goes on duty."
        tvFooter?.text = "Bus Offline"
        ivIcon?.setImageResource(R.drawable.warning)

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        unavailableDialog?.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        unavailableDialog?.show()
    }

    private fun updateMarkers(drivers: List<DriverModel>) {
        pendingDrivers = drivers
        val updateGeneration = ++markerUpdateGeneration
        mapView?.mapboxMap?.getStyle { style ->
            if (updateGeneration != markerUpdateGeneration || isDestroyed) return@getStyle
            drivers.filter { it.latitude != 0.0 && it.longitude != 0.0 }.forEach { driver ->
                val target = Point.fromLngLat(driver.longitude, driver.latitude)
                val receivedAt = driver.locationTimestamp.takeIf { it > 0L }
                    ?: driver.lastUpdated.takeIf { it > 0L }
                    ?: System.currentTimeMillis()
                val priorTimestamp = driverFixTimestamps[driver.driverId] ?: 0L
                if (receivedAt >= priorTimestamp) {
                    val start = driverRenderedPositions[driver.driverId] ?: target
                    val moved = start.latitude() != target.latitude() || start.longitude() != target.longitude()
                    if (moved && receivedAt > priorTimestamp) {
                        val interval = if (priorTimestamp > 0L) (receivedAt - priorTimestamp).coerceIn(600L, 2000L) else 1000L
                        driverAnimators.remove(driver.driverId)?.cancel()
                        animateDriverPosition(driver, start, target, interval)
                        driverFixTimestamps[driver.driverId] = receivedAt
                    } else if (!moved) {
                        driverRenderedPositions[driver.driverId] = target
                        followRenderedDriverIfNeeded(driver.driverId, target)
                    }
                    driverFixTimestamps[driver.driverId] = maxOf(priorTimestamp, receivedAt)
                }
            }
            val features = makeDriverFeatures(drivers)

            if (!style.styleSourceExists(BUS_SOURCE_ID)) {
                style.addSource(geoJsonSource(BUS_SOURCE_ID) {
                    featureCollection(FeatureCollection.fromFeatures(features))
                })
            }
            busGeoJsonSource = style.getSource(BUS_SOURCE_ID)
                    as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
            busGeoJsonSource?.featureCollection(FeatureCollection.fromFeatures(features))

            if (!style.styleLayerExists(BUS_MODEL_LAYER_ID)) {
                style.addLayer(modelLayer(BUS_MODEL_LAYER_ID, BUS_SOURCE_ID) {
                    modelId(BUS_MODEL_ID)
                    modelType(ModelType.COMMON_3D)
                    modelScale(listOf(15.0, 15.0, 15.0))
                    modelRotation(get("rotation"))
                })
            } else {
                (style.getLayer(BUS_MODEL_LAYER_ID) as? com.mapbox.maps.extension.style.layers.generated.ModelLayer)
                    ?.modelRotation(get("rotation"))
            }

            if (!style.styleLayerExists(BUS_LABEL_LAYER_ID)) {
                style.addLayer(symbolLayer(BUS_LABEL_LAYER_ID, BUS_SOURCE_ID) {
                    textField(get("name"))
                    textSize(12.0)
                    textColor(android.graphics.Color.WHITE)
                    textHaloColor(android.graphics.Color.BLACK)
                    textHaloWidth(1.0)
                    textOffset(listOf(0.0, -3.0))
                    textIgnorePlacement(true)
                    textAllowOverlap(true)
                })
            }

            if (!hasInitializedTrackingCamera && !isUserInteracting && drivers.isNotEmpty()) {
                val selected = viewModel.selectedDriver.value
                if (selected != null) {
                    val point = driverRenderedPositions[selected.driverId]
                        ?: Point.fromLngLat(selected.longitude, selected.latitude)
                    mapView?.mapboxMap?.setCamera(CameraOptions.Builder().center(point).zoom(15.0).build())
                    hasInitializedTrackingCamera = true
                } else if (drivers.size == 1) {
                    val driver = drivers[0]
                    val point = driverRenderedPositions[driver.driverId]
                        ?: Point.fromLngLat(driver.longitude, driver.latitude)
                    mapView?.mapboxMap?.setCamera(CameraOptions.Builder().center(point).zoom(15.0).build())
                    hasInitializedTrackingCamera = true
                } else {
                    val points = drivers.filter { it.latitude != 0.0 && it.longitude != 0.0 }
                        .map { driver -> driverRenderedPositions[driver.driverId] ?: Point.fromLngLat(driver.longitude, driver.latitude) }
                    mapView?.mapboxMap?.cameraForCoordinates(points, EdgeInsets(200.0, 100.0, 200.0, 100.0), null, null)
                        ?.let { mapView?.mapboxMap?.setCamera(it); hasInitializedTrackingCamera = true }
                }
            }
        }
    }

    private fun makeDriverFeatures(drivers: List<DriverModel>): List<Feature> = drivers
        .filter { it.latitude != 0.0 && it.longitude != 0.0 }
        .map { driver ->
            val point = driverRenderedPositions[driver.driverId]
                ?: Point.fromLngLat(driver.longitude, driver.latitude)
            val bearing = driverBearings[driver.driverId] ?: 0.0
            Feature.fromGeometry(point).apply {
                addStringProperty("driverId", driver.driverId)
                addStringProperty("name", driver.assignedBus ?: driver.name)
                addNumberProperty("bearing", bearing + 180.0)
                addProperty("rotation", JsonArray().apply { add(0.0); add(0.0); add(bearing + 180.0) })
            }
        }

    private fun animateDriverPosition(driver: DriverModel, start: Point, end: Point, durationMs: Long) {
        val prior = driverPreviousPositions[driver.driverId] ?: start
        val targetBearing = calculateBearing(prior, end).toDouble()
        driverPreviousPositions[driver.driverId] = end
        val animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = durationMs
            interpolator = LinearInterpolator()
            addUpdateListener { frame ->
                val fraction = frame.animatedValue as Float
                val point = Point.fromLngLat(
                    start.longitude() + (end.longitude() - start.longitude()) * fraction,
                    start.latitude() + (end.latitude() - start.latitude()) * fraction
                )
                driverRenderedPositions[driver.driverId] = point
                driverBearings[driver.driverId] = targetBearing
                if (driverAnimators[driver.driverId] === frame) {
                    busGeoJsonSource?.featureCollection(FeatureCollection.fromFeatures(makeDriverFeatures(pendingDrivers)))
                }
                followRenderedDriverIfNeeded(driver.driverId, point)
            }
        }
        driverAnimators[driver.driverId] = animator
        animator.start()
    }

    private fun followRenderedDriverIfNeeded(driverId: String, point: Point) {
        if (isUserInteracting) return
        val selectedId = viewModel.selectedDriver.value?.driverId
        if (selectedId == driverId || (selectedId == null && viewModel.activeDrivers.value?.size == 1)) {
            mapView?.mapboxMap?.setCamera(CameraOptions.Builder().center(point).build())
            hasInitializedTrackingCamera = true
        }
    }

    private fun calculateBearing(start: Point, end: Point): Float {
        val lat1 = Math.toRadians(start.latitude())
        val lon1 = Math.toRadians(start.longitude())
        val lat2 = Math.toRadians(end.latitude())
        val lon2 = Math.toRadians(end.longitude())

        val dLon = lon2 - lon1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon)
        val brng = Math.atan2(y, x)

        return ((Math.toDegrees(brng) + 360) % 360).toFloat()
    }

    private fun focusOnAllDrivers(drivers: List<DriverModel>) {
        val points = drivers.filter { it.latitude != 0.0 && it.longitude != 0.0 }
            .map { driver -> driverRenderedPositions[driver.driverId] ?: Point.fromLngLat(driver.longitude, driver.latitude) }
        val camera = mapView?.mapboxMap?.cameraForCoordinates(
            points,
            EdgeInsets(200.0, 100.0, 200.0, 100.0),
            null,
            null
        )
        camera?.let {
            mapView?.mapboxMap?.easeTo(it, MapAnimationOptions.mapAnimationOptions { duration(350) })
            hasInitializedTrackingCamera = true
        }
    }

    private fun focusOnDriver(driver: DriverModel) {
        if (driver.latitude != 0.0) {
            val point = driverRenderedPositions[driver.driverId] ?: Point.fromLngLat(driver.longitude, driver.latitude)
            mapView?.mapboxMap?.easeTo(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build(),
                MapAnimationOptions.mapAnimationOptions { duration(350) }
            )
            hasInitializedTrackingCamera = true
        }
    }

    private fun updateDriverCard(driver: DriverModel?) {
        val card = findViewById<View>(R.id.driverCard)
        if (driver == null) {
            card.visibility = View.GONE
            return
        }

        if (card.visibility == View.GONE) {
            card.visibility = View.VISIBLE
            card.alpha = 0f
            card.translationY = 100f
            card.animate().alpha(1f).translationY(0f).setDuration(400).start()
        }

        findViewById<TextView>(R.id.tvDriverName)?.text = driver.name
        findViewById<TextView>(R.id.tvBusRouteInfo)?.text = "Bus #${driver.assignedBus ?: "N/A"} • ${driver.route ?: "No Route"}"
        val trip = if (driver.tripDirection.equals("RETURN", true)) "Return Trip" else "Forward Trip"
        findViewById<TextView>(R.id.tvRouteDetail)?.text = "Active Status: ${driver.status} • $trip"
        val locationUpdatedAt = driver.locationTimestamp.takeIf { it > 0L } ?: driver.lastUpdated
        findViewById<TextView>(R.id.tvLastSynced)?.text = "Last synced: ${formatSyncTime(locationUpdatedAt)}"

        findViewById<TextView>(R.id.tvEta)?.text = driver.eta
        findViewById<TextView>(R.id.tvSpeed)?.text = "${driver.speed.toInt()} km/h"
        findViewById<TextView>(R.id.tvLoad)?.text = driver.load
    }

    private fun formatSyncTime(timestamp: Long): String =
        if (timestamp > 0L) java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault()).format(java.util.Date(timestamp))
        else "--"

    override fun onResume() {
        super.onResume()
        utils.NavigationUtils.setupBottomNavigation(this)
    }

    override fun onStart() { super.onStart(); mapView?.onStart() }
    override fun onStop() { super.onStop(); mapView?.onStop() }

    private fun bitmapFromDrawableRes(context: Context, resourceId: Int): Bitmap? {
        if (bitmapCache.containsKey(resourceId)) return bitmapCache[resourceId]
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable is BitmapDrawable) {
            bitmapCache[resourceId] = drawable.bitmap
            return drawable.bitmap
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

    override fun onDestroy() {
        super.onDestroy()
        driverAnimators.values.forEach { it.cancel() }
        driverAnimators.clear()
        bitmapCache.clear()
        mapView?.onDestroy()
    }

    inner class BusSearchAdapter(private val onItemSelected: (DriverModel) -> Unit) :
        RecyclerView.Adapter<BusSearchAdapter.ViewHolder>() {

        private var drivers = listOf<DriverModel>()

        fun updateData(newList: List<DriverModel>) {
            drivers = newList
            notifyDataSetChanged()
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvBusId: TextView = view.findViewById(R.id.tvBusId)
            val tvRouteInfo: TextView = view.findViewById(R.id.tvRouteInfo)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)

            init {
                view.setOnClickListener {
                    val pos = bindingAdapterPosition
                    if (pos != RecyclerView.NO_POSITION) {
                        onItemSelected(drivers[pos])
                    }
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bus_search_suggestion, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = drivers[position]
            holder.tvBusId.text = item.assignedBus ?: item.name
            holder.tvRouteInfo.text = "Route: ${item.route ?: "N/A"}"
            holder.tvStatus.text = item.status

            if (item.status.equals("Active", true) || item.status.equals("ACTIVE", true)) {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
            } else {
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_inactive)
            }
        }

        override fun getItemCount() = drivers.size
    }
}
