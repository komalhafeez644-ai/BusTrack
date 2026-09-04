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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup

class LiveTrackingActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private val viewModel: LiveTrackingViewModel by viewModels()
    private val driverMarkers = mutableMapOf<String, PointAnnotation>()
    private val driverPreviousPositions = mutableMapOf<String, Point>()
    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    private var isUserInteracting = false
    private lateinit var searchAdapter: BusSearchAdapter
    private var unavailableDialog: android.app.Dialog? = null
    private var isUnavailablePopupDismissed = false

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
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) { style ->
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

            pointAnnotationManager?.addClickListener { annotation ->
                val driversList = viewModel.activeDrivers.value
                val driver = driversList?.find {
                    val latDiff = Math.abs(it.latitude - annotation.point.latitude())
                    val lngDiff = Math.abs(it.longitude - annotation.point.longitude())
                    latDiff < 0.0001 && lngDiff < 0.0001
                }
                driver?.let {
                    isUserInteracting = false
                    viewModel.selectDriver(it)
                    focusOnDriver(it)
                }
                true
            }

            // Sync Compass UI with map rotation
            mapView?.mapboxMap?.subscribeCameraChanged {
                val bearing = mapView?.mapboxMap?.cameraState?.bearing?.toFloat() ?: 0f
                findViewById<ImageView>(R.id.ivCompass)?.rotation = -bearing
            }

            observeViewModel()
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

        // Map Controls
        findViewById<View>(R.id.mapControls)?.let { controls ->
            controls.findViewById<View>(R.id.compassCard)?.setOnClickListener {
                // Reset map rotation to North
                mapView?.mapboxMap?.flyTo(CameraOptions.Builder().bearing(0.0).build())
            }

            controls.findViewById<View>(R.id.myLocationCard)?.setOnClickListener {
                isUserInteracting = false
                val drivers = viewModel.activeDrivers.value
                if (!drivers.isNullOrEmpty()) {
                    val selected = viewModel.selectedDriver.value
                    if (selected != null) {
                        focusOnDriver(selected)
                    } else {
                        if (drivers.size == 1) focusOnDriver(drivers[0]) else focusOnAllDrivers(drivers)
                    }
                } else {
                    val defaultPoint = Point.fromLngLat(73.0478, 33.5977)
                    mapView?.mapboxMap?.flyTo(CameraOptions.Builder().center(defaultPoint).zoom(15.0).build())
                }
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
            if (drivers.isEmpty()) {
                findViewById<View>(R.id.driverCard).visibility = View.GONE
                showUnavailableDialog()
            } else {
                unavailableDialog?.dismiss()
                unavailableDialog = null
                isUnavailablePopupDismissed = false
            }
            updateMarkers(drivers)
        }

        viewModel.selectedDriver.observe(this) { driver ->
            updateDriverCard(driver)
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
        mapView?.mapboxMap?.getStyle { style ->
            val features = drivers.filter { it.latitude != 0.0 && it.longitude != 0.0 }.map { driver ->
                val point = Point.fromLngLat(driver.longitude, driver.latitude)
                val prevPoint = driverPreviousPositions[driver.driverId]
                val bearing = if (prevPoint != null && (prevPoint.latitude() != point.latitude() || prevPoint.longitude() != point.longitude())) {
                    calculateBearing(prevPoint, point).toDouble()
                } else {
                    0.0
                }
                driverPreviousPositions[driver.driverId] = point

                Feature.fromGeometry(point).apply {
                    addStringProperty("driverId", driver.driverId)
                    addStringProperty("name", driver.assignedBus ?: driver.name)
                    addNumberProperty("bearing", bearing + 180.0)
                }
            }

            if (!style.styleSourceExists(BUS_SOURCE_ID)) {
                style.addSource(geoJsonSource(BUS_SOURCE_ID) {
                    featureCollection(FeatureCollection.fromFeatures(features))
                })
            } else {
                val source = style.getSource(BUS_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
                source?.featureCollection(FeatureCollection.fromFeatures(features))
            }

            if (!style.styleLayerExists(BUS_MODEL_LAYER_ID)) {
                style.addLayer(modelLayer(BUS_MODEL_LAYER_ID, BUS_SOURCE_ID) {
                    modelId(BUS_MODEL_ID)
                    modelType(ModelType.COMMON_3D)
                    modelScale(listOf(15.0, 15.0, 15.0))
                    modelRotation(array {
                        literal(0.0)
                        literal(0.0)
                        get("bearing")
                    })
                })
            } else {
                (style.getLayer(BUS_MODEL_LAYER_ID) as? com.mapbox.maps.extension.style.layers.generated.ModelLayer)
                    ?.modelRotation(array {
                        literal(0.0)
                        literal(0.0)
                        get("bearing")
                    })
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
        }

        // 3. Camera handling
        if (!isUserInteracting && drivers.isNotEmpty()) {
            val selected = viewModel.selectedDriver.value
            if (selected != null) {
                focusOnDriver(selected)
            } else if (drivers.size == 1) {
                focusOnDriver(drivers[0])
            } else {
                focusOnAllDrivers(drivers)
            }
        }
    }

    private fun animateMarker(annotation: PointAnnotation, start: Point, end: Point) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 400
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val lat = start.latitude() + (end.latitude() - start.latitude()) * fraction
            val lng = start.longitude() + (end.longitude() - start.longitude()) * fraction
            annotation.point = Point.fromLngLat(lng, lat)
        }
        animator.start()
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
        val points = drivers.map { Point.fromLngLat(it.longitude, it.latitude) }
        val camera = mapView?.mapboxMap?.cameraForCoordinates(
            points,
            EdgeInsets(200.0, 100.0, 200.0, 100.0),
            null,
            null
        )
        camera?.let {
            // This runs on every driver-location Firestore update (see the "3. Camera
            // handling" block below), same as TrackDriverActivity's bug: flyTo()'s
            // dramatic globe animation kept getting interrupted by the next update
            // before it finished, which is what looked like blinking. easeTo() is a
            // direct pan/zoom with no globe-orbit effect, so an interruption is a
            // non-issue - it just smoothly redirects toward the newer target instead.
            mapView?.mapboxMap?.easeTo(it, MapAnimationOptions.mapAnimationOptions { duration(800) })
        }
    }

    private fun focusOnDriver(driver: DriverModel) {
        if (driver.latitude != 0.0) {
            val point = Point.fromLngLat(driver.longitude, driver.latitude)
            mapView?.mapboxMap?.easeTo(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build(),
                MapAnimationOptions.mapAnimationOptions { duration(800) }
            )
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
        findViewById<TextView>(R.id.tvRouteDetail)?.text = "Active Status: ${driver.status}"

        findViewById<TextView>(R.id.tvEta)?.text = driver.eta
        findViewById<TextView>(R.id.tvSpeed)?.text = "${driver.speed.toInt()} km/h"
        findViewById<TextView>(R.id.tvLoad)?.text = driver.load
    }

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