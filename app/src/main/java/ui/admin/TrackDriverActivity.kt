package ui.admin

import android.animation.ValueAnimator
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.NavigationStopsAdapter
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.viewmodels.TrackDriverViewModel
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.addLayerAbove
import com.mapbox.maps.extension.style.layers.addLayerBelow
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.layers.generated.symbolLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.extension.style.layers.properties.generated.LineCap
import com.mapbox.maps.extension.style.layers.properties.generated.LineJoin
import com.mapbox.maps.extension.style.expressions.dsl.generated.*
import com.mapbox.turf.TurfConstants
import com.mapbox.turf.TurfMeasurement
import com.mapbox.turf.TurfMisc

import android.location.Geocoder
import android.location.Location
import com.mapbox.geojson.Feature
import com.mapbox.geojson.FeatureCollection
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directions.v5.DirectionsCriteria
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class TrackDriverActivity : AppCompatActivity() {

    enum class TrackingCameraMode { DRIVER_FOLLOW, ROUTE_OVERVIEW }
    private var currentCameraMode = TrackingCameraMode.DRIVER_FOLLOW

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<FrameLayout>
    private var mapView: MapView? = null
    private val viewModel: TrackDriverViewModel by viewModels()
    
    private var pointAnnotationManager: PointAnnotationManager? = null
    private val stopMarkers = mutableListOf<PointAnnotation>()
    
    private lateinit var stopsAdapter: NavigationStopsAdapter
    private val ROUTE_SOURCE_ID = "route-source-id"
    private val TRAVELED_ROUTE_SOURCE_ID = "traveled-route-source-id"
    private val STOPS_SOURCE_ID = "stops-source-id"
    private val ROUTE_LAYER_ID = "route-layer-id"
    private val TRAVELED_ROUTE_LAYER_ID = "traveled-route-layer-id"
    private val ROUTE_CASING_LAYER_ID = "route-casing-layer-id"
    private val STOPS_LAYER_ID = "stops-layer-id"
    private val DRIVER_SOURCE_ID = "driver-source-id"
    private val DRIVER_LAYER_ID = "driver-layer-id"
    
    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    private var currentRouteId: String? = null
    private var previousPoint: Point? = null
    private var unavailableDialog: Dialog? = null
    private var isUnavailablePopupDismissed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            setContentView(R.layout.activity_track_driver)
            supportActionBar?.hide()

            val driverId = intent.getStringExtra("DRIVER_ID") ?: ""
            if (driverId.isEmpty()) {
                Toast.makeText(this, "Invalid Driver ID", Toast.LENGTH_SHORT).show()
                finish()
                return
            }

            mapView = findViewById(R.id.mapView)
            mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) { style ->
                pointAnnotationManager = mapView?.annotations?.createPointAnnotationManager()
                
                // Use a realistic blue bus icon for a "3D" navigation look
                bitmapFromDrawableRes(this, R.drawable.blue_bus)?.let { style.addImage("bus-icon", it) }
                bitmapFromDrawableRes(this, R.drawable.ic_marker_dest)?.let { style.addImage("stop-icon", it) }
                bitmapFromDrawableRes(this, R.drawable.ic_marker_dest_grey)?.let { style.addImage("stop-icon-grey", it) }
                
                observeViewModel()
            }

            setupBottomSheet()
            viewModel.setDriverId(driverId)

            // Check for Parent Mode
            if (intent.getBooleanExtra("IS_PARENT", false)) {
                setupParentUI()
            }

            findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

            findViewById<View>(R.id.btnRecenter)?.setOnClickListener {
                currentCameraMode = TrackingCameraMode.DRIVER_FOLLOW
                viewModel.targetDriver.value?.let { driver ->
                    if (driver.latitude != 0.0 && driver.longitude != 0.0) {
                        val targetPoint = Point.fromLngLat(driver.longitude, driver.latitude)
                        mapView?.mapboxMap?.flyTo(
                            CameraOptions.Builder()
                                .center(targetPoint)
                                .zoom(17.0)
                                .pitch(60.0)
                                .build(),
                            MapAnimationOptions.mapAnimationOptions { duration(1500) }
                        )
                    }
                }
            }
            
        } catch (e: Exception) {
            Log.e("TrackDriverActivity", "Crash in onCreate", e)
            Toast.makeText(this, "Error initializing tracking", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupParentUI() {
        findViewById<View>(R.id.header)?.setBackgroundResource(R.drawable.bg_header_blue)
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Tracking Detail"
    }

    private fun setupBottomSheet() {
        val bottomSheet = findViewById<FrameLayout>(R.id.bottomSheet)
        if (bottomSheet != null) {
            bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
            bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
            
            // Ensure Bottom Sheet is in Light Mode for Admin
            forceLightBottomSheet(bottomSheet)

            // Driver Sheet Specific IDs
            val rvStops = bottomSheet.findViewById<RecyclerView>(R.id.rvUpcomingStops)
            stopsAdapter = NavigationStopsAdapter(emptyList())
            rvStops?.layoutManager = LinearLayoutManager(this)
            rvStops?.adapter = stopsAdapter

            bottomSheet.findViewById<View>(R.id.btnViewAllStops)?.setOnClickListener {
                bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
            
            bottomSheet.findViewById<View>(R.id.btnCloseNav)?.setOnClickListener {
                finish()
            }

            bottomSheet.findViewById<View>(R.id.btnViewRoute)?.setOnClickListener {
                currentCameraMode = TrackingCameraMode.ROUTE_OVERVIEW
                viewModel.assignedRoute.value?.let { route ->
                    if (route.pathPoints.isNotEmpty()) {
                        val points = route.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                        val cameraOptions = mapView?.mapboxMap?.cameraForCoordinates(
                            points,
                            EdgeInsets(100.0, 60.0, 350.0, 60.0), // Optimized padding for Admin view
                            0.0,
                            0.0
                        )
                        cameraOptions?.let {
                            mapView?.mapboxMap?.flyTo(it, MapAnimationOptions.mapAnimationOptions { duration(1500) })
                        }
                    } else {
                        Toast.makeText(this, "Route path not available", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun forceLightBottomSheet(sheet: View) {
        // Force background to white dialog style
        sheet.findViewById<View>(R.id.bottomSheetContainer)?.setBackgroundResource(R.drawable.bg_bottom_sheet_dialog)
        
        // Force text colors to dark for light background
        val colorTextPrimary = Color.parseColor("#0F172A")
        val colorTextSecondary = Color.parseColor("#64748B")
        
        // Outline Buttons Theme for Admin
        val btnClose = sheet.findViewById<View>(R.id.btnCloseNav)
        val btnRoute = sheet.findViewById<View>(R.id.btnViewRoute)
        val outlineColor = Color.parseColor("#CBD5E1")
        
        btnClose?.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_outline)
        btnRoute?.background = ContextCompat.getDrawable(this, R.drawable.bg_circle_outline)
        
        btnClose?.backgroundTintList = android.content.res.ColorStateList.valueOf(outlineColor)
        btnRoute?.backgroundTintList = android.content.res.ColorStateList.valueOf(outlineColor)

        (sheet.findViewById<ViewGroup>(R.id.btnCloseNav)?.getChildAt(0) as? ImageView)?.imageTintList = 
            android.content.res.ColorStateList.valueOf(Color.parseColor("#64748B"))
        (sheet.findViewById<ViewGroup>(R.id.btnViewRoute)?.getChildAt(0) as? ImageView)?.imageTintList = 
            android.content.res.ColorStateList.valueOf(Color.parseColor("#2563EB"))

        sheet.findViewById<TextView>(R.id.tvBusIdSheet)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvRouteSheet)?.setTextColor(colorTextSecondary)
        sheet.findViewById<TextView>(R.id.tvDriverNameSheet)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvCurrentLocSheet)?.setTextColor(colorTextSecondary)
        sheet.findViewById<TextView>(R.id.tvUpcomingLabel)?.setTextColor(colorTextPrimary)
        
        // Force label and value colors for live stats
        sheet.findViewById<TextView>(R.id.tvEtaLabel)?.setTextColor(colorTextSecondary)
        sheet.findViewById<TextView>(R.id.tvSpeedLabel)?.setTextColor(colorTextSecondary)
        sheet.findViewById<TextView>(R.id.tvLoadLabel)?.setTextColor(colorTextSecondary)
        
        sheet.findViewById<TextView>(R.id.tvEtaSheet)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvSpeedSheet)?.setTextColor(colorTextPrimary)
        sheet.findViewById<TextView>(R.id.tvLoadSheet)?.setTextColor(colorTextPrimary)
        
        // Force Card colors
        sheet.findViewById<com.google.android.material.card.MaterialCardView>(R.id.cardHeader)?.setCardBackgroundColor(Color.WHITE)
        sheet.findViewById<View>(R.id.dividerHeader)?.setBackgroundColor(Color.parseColor("#F1F5F9"))
        
        // Ensure stops list is also in light mode
        if (::stopsAdapter.isInitialized) {
            stopsAdapter.setTheme(false)
        }
    }

    private fun observeViewModel() {
        viewModel.targetDriver.observe(this) { driver ->
            if (driver != null) {
                val isStatusActive = driver.status.equals("Active", true) || 
                                    driver.status.equals("ACTIVE", true) || 
                                    driver.status.equals("On Duty", true)
                
                val isNavigating = driver.isNavigating || !driver.currentRoutePolyline.isNullOrEmpty()
                
                val currentTime = System.currentTimeMillis()
                val isDataRecent = (currentTime - driver.lastUpdated) < 1800000 // 30 mins window
                
                if (isStatusActive && isNavigating && isDataRecent && driver.latitude != 0.0) {
                    unavailableDialog?.dismiss()
                    unavailableDialog = null
                    isUnavailablePopupDismissed = false
                    updateUI(driver)
                } else {
                    showUnavailableDialog()
                }
            } else {
                showUnavailableDialog()
            }
        }

        viewModel.assignedRoute.observe(this) { route ->
            route?.let { 
                if (currentRouteId != it.id) {
                    currentRouteId = it.id
                    drawInitialRoute(it)
                    // Populate stops list immediately even if driver hasn't moved
                    stopsAdapter.updateStops(it.stopsList, 0, "NEXT")
                    // Trigger immediate UI refresh to clip route to current bus location
                    viewModel.targetDriver.value?.let { driver -> updateUI(driver) }
                }
            }
        }
    }

    private fun showUnavailableDialog() {
        if (unavailableDialog?.isShowing == true || isUnavailablePopupDismissed) return

        unavailableDialog = Dialog(this)
        unavailableDialog?.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        unavailableDialog?.setContentView(R.layout.dialog_request_submitted) 
        unavailableDialog?.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
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
        tvMsg?.text = "The assigned bus is currently not on duty. Live location will be available when the bus goes on duty."
        tvFooter?.text = "Bus Offline"
        ivIcon?.setImageResource(R.drawable.warning)

        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        unavailableDialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        
        unavailableDialog?.show()
    }

    private fun updateUI(driver: DriverModel) {
        try {
            val sheet = findViewById<FrameLayout>(R.id.bottomSheet)
            sheet?.let {
                it.findViewById<TextView>(R.id.tvBusIdSheet)?.text = driver.assignedBus ?: "BUS-101"
                it.findViewById<TextView>(R.id.tvRouteSheet)?.text = driver.route ?: "Route"
                it.findViewById<TextView>(R.id.tvDriverNameSheet)?.text = driver.name
                
                var locationName = ""
                val geocoder = Geocoder(this@TrackDriverActivity, Locale.getDefault())
                try {
                    val addresses = geocoder.getFromLocation(driver.latitude, driver.longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        val feature = addr.featureName
                        val street = addr.thoroughfare
                        val subLocality = addr.subLocality
                        val locality = addr.locality
                        
                        val cleanLocation = when {
                            street != null && subLocality != null -> "$street, $subLocality"
                            street != null -> street
                            feature != null && feature.contains("+").not() -> feature
                            subLocality != null -> subLocality
                            locality != null -> locality
                            else -> "Near Route"
                        }
                        
                        locationName = cleanLocation
                        val fullAddress = addr.getAddressLine(0)
                        val cleanedAddress = fullAddress.replace(Regex("[A-Z0-9]{4,8}\\+[A-Z0-9]{2,4}"), "")
                            .replace(Regex(",\\s*,"), ",")
                            .trim()
                            .removePrefix(",")
                            .removeSuffix(",")
                            .trim()
                        
                        it.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = cleanedAddress
                    } else {
                        locationName = "Moving"
                        it.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = "Location: ${String.format("%.4f", driver.latitude)}, ${String.format("%.4f", driver.longitude)}"
                    }
                } catch (e: Exception) {
                    locationName = "Moving"
                    it.findViewById<TextView>(R.id.tvCurrentLocSheet)?.text = "Location: ${String.format("%.4f", driver.latitude)}, ${String.format("%.4f", driver.longitude)}"
                }

                // Update Live Stats in Bottom Sheet
                val etaValue = if (driver.eta.isNullOrEmpty()) "On Way" else driver.eta
                it.findViewById<TextView>(R.id.tvEtaSheet)?.text = etaValue
                it.findViewById<TextView>(R.id.tvSpeedSheet)?.text = "${driver.speed.toInt()} km/h"
                it.findViewById<TextView>(R.id.tvLoadSheet)?.text = driver.load ?: "0/0"

                if (driver.latitude == 0.0 || driver.longitude == 0.0) return@let
                val targetPoint = Point.fromLngLat(driver.longitude, driver.latitude)

                mapView?.mapboxMap?.getStyle { style ->
                    if (!style.styleSourceExists(DRIVER_SOURCE_ID)) {
                        style.addSource(geoJsonSource(DRIVER_SOURCE_ID) {
                            geometry(targetPoint)
                        })
                    } else {
                        val source = style.getSource(DRIVER_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
                        source?.geometry(targetPoint)
                    }

                    if (!style.styleLayerExists(DRIVER_LAYER_ID)) {
                        val layer = symbolLayer(DRIVER_LAYER_ID, DRIVER_SOURCE_ID) {
                            iconImage("bus-icon")
                            iconSize(interpolate {
                                exponential(1.5)
                                zoom()
                                stop(10.0, 1.0)
                                stop(18.0, 2.5)
                            })
                            iconAllowOverlap(true)
                            iconIgnorePlacement(true)
                            iconRotationAlignment(com.mapbox.maps.extension.style.layers.properties.generated.IconRotationAlignment.MAP)
                            
                            textField(locationName)
                            textSize(interpolate {
                                exponential(1.5)
                                zoom()
                                stop(10.0, 8.0)
                                stop(18.0, 14.0)
                            })
                            textOffset(listOf(0.0, -2.5))
                            textColor(Color.BLUE)
                            textHaloColor(Color.WHITE)
                            textHaloWidth(1.0)
                            textIgnorePlacement(true)
                            textAllowOverlap(true)
                        }
                        
                        if (style.styleLayerExists(STOPS_LAYER_ID)) {
                            style.addLayerAbove(layer, STOPS_LAYER_ID)
                        } else {
                            style.addLayer(layer)
                        }
                    } else {
                        val layer = style.getLayer(DRIVER_LAYER_ID) as? com.mapbox.maps.extension.style.layers.generated.SymbolLayer
                        layer?.textField(locationName)
                        
                        previousPoint?.let { start ->
                            if (start.latitude() != targetPoint.latitude() || start.longitude() != targetPoint.longitude()) {
                                val bearing = calculateBearing(start, targetPoint)
                                layer?.iconRotate(bearing.toDouble())
                                animateDriver(start, targetPoint)
                            }
                        }
                    }
                }
            }

            if (driver.latitude == 0.0 || driver.longitude == 0.0) return
            val targetPoint = Point.fromLngLat(driver.longitude, driver.latitude)
            previousPoint = targetPoint

            viewModel.assignedRoute.value?.let { route ->
                updateRouteSplitting(targetPoint, route, driver)
                
                // Identify next stop status precisely from Driver's navigation state
                val globalNextIdx = driver.nextStopIndex
                if (globalNextIdx < route.stopsList.size) {
                    val stopPoint = Point.fromLngLat(route.stopsList[globalNextIdx].longitude, route.stopsList[globalNextIdx].latitude)
                    val distance = TurfMeasurement.distance(targetPoint, stopPoint, TurfConstants.UNIT_METERS)
                    
                    // Check if driver has reported arrival or is within radius
                    val hasArrived = driver.stopArrivalTimes.containsKey(globalNextIdx.toString()) || distance < 150.0
                    val status = if (hasArrived) "ARRIVED" else "NEXT"
                    
                    // The index should never decrease during a trip
                    stopsAdapter.updateStops(route.stopsList, globalNextIdx, status)
                } else {
                    calculateProgress(driver, route) // Fallback to distance-based
                }
            }
            
            // Apply a tilted 3D perspective
            if (currentCameraMode == TrackingCameraMode.DRIVER_FOLLOW) {
                val currentCamera = mapView?.mapboxMap?.cameraState
                val isAtDefaultGlobe = currentCamera?.center?.latitude() == 0.0 && currentCamera?.center?.longitude() == 0.0

                if (isAtDefaultGlobe) {
                    // FIRST LOAD: Instant jump to bus location to avoid "Globe Flash"
                    mapView?.mapboxMap?.setCamera(
                        CameraOptions.Builder()
                            .center(targetPoint)
                            .zoom(17.0)
                            .pitch(60.0)
                            .build()
                    )
                } else {
                    // SUBSEQUENT UPDATES: Smooth animation for moving bus
                    mapView?.mapboxMap?.flyTo(
                        CameraOptions.Builder()
                            .center(targetPoint)
                            .zoom(17.0)
                            .pitch(60.0)
                            .build(),
                        MapAnimationOptions.mapAnimationOptions { duration(1000) }
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("TrackDriverActivity", "Error updating UI", e)
        }
    }

    private fun animateDriver(start: Point, end: Point) {
        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 400
        animator.interpolator = LinearInterpolator()
        animator.addUpdateListener { animation ->
            val fraction = animation.animatedValue as Float
            val lat = start.latitude() + (end.latitude() - start.latitude()) * fraction
            val lng = start.longitude() + (end.longitude() - start.longitude()) * fraction
            val currentPoint = Point.fromLngLat(lng, lat)
            
            mapView?.mapboxMap?.getStyle { style ->
                val source = style.getSource(DRIVER_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
                source?.geometry(currentPoint)
            }
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

    private fun calculateProgress(driver: DriverModel, route: RouteModel) {
        val driverPoint = Point.fromLngLat(driver.longitude, driver.latitude)
        var closestIdx = 0
        var minDistance = Double.MAX_VALUE
        
        for (i in route.stopsList.indices) {
            val stop = route.stopsList[i]
            val stopPoint = Point.fromLngLat(stop.longitude, stop.latitude)
            val distance = TurfMeasurement.distance(driverPoint, stopPoint, TurfConstants.UNIT_METERS)
            if (distance < minDistance) {
                minDistance = distance
                closestIdx = i
            }
        }

        val hasArrived = driver.stopArrivalTimes.containsKey(closestIdx.toString()) || minDistance < 150.0
        val status = if (hasArrived) "ARRIVED" else "NEXT"
        
        // Use the driver's nextStopIndex to ensure we don't go backwards
        val displayIdx = Math.max(closestIdx, driver.nextStopIndex)
        stopsAdapter.updateStops(route.stopsList, displayIdx, status)
    }

    private fun drawInitialRoute(route: RouteModel) {
        if (route.pathPoints.isEmpty()) return

        // Update Stops List immediately
        stopsAdapter.updateStops(route.stopsList, 0, "NEXT")

        mapView?.mapboxMap?.getStyle { style ->
            try {
                // Initialize sources with empty geometry first to prevent showing full route from start
                val emptyLine = LineString.fromLngLats(emptyList())
                
                if (!style.styleSourceExists(ROUTE_SOURCE_ID)) {
                    style.addSource(geoJsonSource(ROUTE_SOURCE_ID) { geometry(emptyLine) })
                } else {
                    (style.getSource(ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                        ?.geometry(emptyLine)
                }

                if (!style.styleSourceExists(TRAVELED_ROUTE_SOURCE_ID)) {
                    style.addSource(geoJsonSource(TRAVELED_ROUTE_SOURCE_ID) { geometry(emptyLine) })
                } else {
                    (style.getSource(TRAVELED_ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                        ?.geometry(emptyLine)
                }

                // 1. Traveled Portion (Bottom)
                if (!style.styleLayerExists(TRAVELED_ROUTE_LAYER_ID)) {
                    style.addLayer(lineLayer(TRAVELED_ROUTE_LAYER_ID, TRAVELED_ROUTE_SOURCE_ID) {
                        lineColor(Color.parseColor("#94A3B8")) // Light Gray
                        lineWidth(interpolate {
                            linear()
                            zoom()
                            stop(10.0, 3.0)
                            stop(14.0, 7.0)
                            stop(18.0, 11.0)
                        })
                        lineOpacity(0.8)
                        lineJoin(LineJoin.ROUND)
                        lineCap(LineCap.ROUND)
                    })
                }

                // 2. Upcoming Casing (Above Traveled)
                if (!style.styleLayerExists(ROUTE_CASING_LAYER_ID)) {
                    style.addLayerAbove(lineLayer(ROUTE_CASING_LAYER_ID, ROUTE_SOURCE_ID) {
                        lineColor(Color.parseColor("#0D1B3E")) // Dark Navy
                        lineWidth(interpolate {
                            linear()
                            zoom()
                            stop(10.0, 5.0)
                            stop(14.0, 10.0)
                            stop(18.0, 16.0)
                        })
                        lineOpacity(1.0)
                        lineJoin(LineJoin.ROUND)
                        lineCap(LineCap.ROUND)
                    }, TRAVELED_ROUTE_LAYER_ID)
                }

                // 3. Upcoming Main Route (Above Casing)
                if (!style.styleLayerExists(ROUTE_LAYER_ID)) {
                    style.addLayerAbove(lineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID) {
                        lineColor(Color.parseColor("#007AFF")) // Bright Blue
                        lineWidth(interpolate {
                            linear()
                            zoom()
                            stop(10.0, 3.0)
                            stop(14.0, 7.0)
                            stop(18.0, 11.0)
                        })
                        lineOpacity(1.0)
                        lineJoin(LineJoin.ROUND)
                        lineCap(LineCap.ROUND)
                    }, ROUTE_CASING_LAYER_ID)
                }

                // 4. Stops Layer (Above everything else)
                val stopFeatures = route.stopsList.mapIndexed { index, stop ->
                    val isCompleted = viewModel.targetDriver.value?.stopArrivalTimes?.containsKey(index.toString()) == true || index < (viewModel.targetDriver.value?.nextStopIndex ?: 0)
                    Feature.fromGeometry(Point.fromLngLat(stop.longitude, stop.latitude)).apply {
                        addStringProperty("name", stop.stopName)
                        addStringProperty("iconName", if (isCompleted) "stop-icon-grey" else "stop-icon")
                    }
                }
                val stopCollection = FeatureCollection.fromFeatures(stopFeatures)

                if (!style.styleSourceExists(STOPS_SOURCE_ID)) {
                    style.addSource(geoJsonSource(STOPS_SOURCE_ID) {
                        featureCollection(stopCollection)
                    })
                } else {
                    (style.getSource(STOPS_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                        ?.featureCollection(stopCollection)
                }

                if (!style.styleLayerExists(STOPS_LAYER_ID)) {
                    style.addLayerAbove(symbolLayer(STOPS_LAYER_ID, STOPS_SOURCE_ID) {
                        iconImage(get("iconName"))
                        iconSize(0.8) // Consistent with Driver Dashboard
                        iconAllowOverlap(true)
                        iconIgnorePlacement(true)

                        textField(get("name"))
                        textSize(10.0)
                        textOffset(listOf(0.0, 1.5))
                        textColor(Color.BLACK)
                        textHaloColor(Color.WHITE)
                        textHaloWidth(1.0)
                        textIgnorePlacement(true)
                        textAllowOverlap(true)
                    }, ROUTE_LAYER_ID)
                } else {
                    // Update icon mapping even if layer exists
                    (style.getLayer(STOPS_LAYER_ID) as? com.mapbox.maps.extension.style.layers.generated.SymbolLayer)
                        ?.iconImage(get("iconName"))
                }
            } catch (e: Exception) {
                Log.e("TrackDriverActivity", "Error drawing route", e)
            }
        }
    }

    private var lastCalculatedOrigin: Point? = null
    private var lastCalculatedDest: Point? = null

    private fun updateRouteSplitting(currentPos: Point, route: RouteModel, driver: DriverModel) {
        try {
            // 1. Dynamic Rerouting Logic (Single Source of Truth)
            // If the driver has uploaded a dynamic navigation route, use it!
            if (!driver.currentRoutePolyline.isNullOrEmpty()) {
                val dynamicUpcoming = LineString.fromPolyline(driver.currentRoutePolyline!!, 6)
                
                mapView?.mapboxMap?.getStyle { style ->
                    // Update main active route with dynamic geometry from driver
                    (style.getSource(ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                        ?.geometry(dynamicUpcoming)
                    
                    // Update traveled portion if available from driver's actual history
                    if (!driver.traveledPolyline.isNullOrEmpty()) {
                        val dynamicTraveled = LineString.fromPolyline(driver.traveledPolyline!!, 6)
                        (style.getSource(TRAVELED_ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                            ?.geometry(dynamicTraveled)
                    }
                }
                return // Dynamic logic completes here
            }

            // 2. Smart Rerouting Fallback (Recalculate Bus -> Stop)
            // If the bus has deviated from the assigned route or navigation isn't started yet,
            // recalculate a fresh road-matched path from the bus's current location to the next pending stop.
            val globalNextIdx = driver.nextStopIndex.coerceIn(0, route.stopsList.size - 1)
            val nextStop = route.stopsList[globalNextIdx]
            val destination = Point.fromLngLat(nextStop.longitude, nextStop.latitude)
            
            // Optimization: Only recalculate if the bus has moved significantly (>50m) or target stop changed
            val distToLastOrigin = lastCalculatedOrigin?.let { TurfMeasurement.distance(currentPos, it, TurfConstants.UNIT_METERS) } ?: Double.MAX_VALUE
            val hasDestChanged = lastCalculatedDest?.let { it.latitude() != destination.latitude() || it.longitude() != destination.longitude() } ?: true
            
            if (distToLastOrigin > 50.0 || hasDestChanged) {
                lastCalculatedOrigin = currentPos
                lastCalculatedDest = destination

                val directionsPoints = mutableListOf<Point>()
                directionsPoints.add(currentPos) // Use raw current position, Mapbox will find the nearest drivable road
                directionsPoints.add(destination)

                val client = MapboxDirections.builder()
                    .accessToken(getString(R.string.mapbox_access_token))
                    .routeOptions(RouteOptions.builder()
                        .coordinatesList(directionsPoints)
                        .profile(DirectionsCriteria.PROFILE_DRIVING)
                        .overview(DirectionsCriteria.OVERVIEW_FULL)
                        .build())
                    .build()

                client.enqueueCall(object : Callback<DirectionsResponse> {
                    override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                        val roadMatchedGeometry = response.body()?.routes()?.firstOrNull()?.geometry()
                        if (roadMatchedGeometry != null) {
                            val lineString = LineString.fromPolyline(roadMatchedGeometry, 6)
                            mapView?.mapboxMap?.getStyle { style ->
                                (style.getSource(ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                                    ?.geometry(lineString)
                            }
                        }
                    }
                    override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                        // Fallback to straight line only if Directions API fails
                        val fallbackLine = LineString.fromLngLats(directionsPoints)
                        mapView?.mapboxMap?.getStyle { style ->
                            (style.getSource(ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                                ?.geometry(fallbackLine)
                        }
                    }
                })
            }

            // For history in fallback mode, draw from original start point to current bus position
            val fullPath = route.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            if (fullPath.size >= 2) {
                val splitIndex = findClosestPathIndex(currentPos, fullPath)
                val snappedPoint = TurfMisc.nearestPointOnLine(currentPos, fullPath)
                val snappedP = snappedPoint.geometry() as? Point
                
                val history = fullPath.subList(0, splitIndex + 1).toMutableList()
                snappedP?.let { history.add(it) }
                
                if (history.size >= 2) {
                    mapView?.mapboxMap?.getStyle { style ->
                        (style.getSource(TRAVELED_ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource)
                            ?.geometry(LineString.fromLngLats(history))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("TrackDriverActivity", "Error splitting route", e)
        }
    }

    private fun findClosestPathIndex(point: Point, path: List<Point>): Int {
        var minDistance = Double.MAX_VALUE
        var index = 0
        for (i in path.indices) {
            val dist = TurfMeasurement.distance(point, path[i], TurfConstants.UNIT_METERS)
            if (dist < minDistance) {
                minDistance = dist
                index = i
            }
        }
        return index
    }

    private fun bitmapFromDrawableRes(context: Context, resourceId: Int): Bitmap? {
        if (bitmapCache.containsKey(resourceId)) return bitmapCache[resourceId]
        val drawable = ContextCompat.getDrawable(context, resourceId)
        if (drawable is BitmapDrawable) {
            bitmapCache[resourceId] = drawable.bitmap
            return drawable.bitmap
        }
        if (drawable != null) {
            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth.takeIf { it > 0 } ?: 64, 
                                            drawable.intrinsicHeight.takeIf { it > 0 } ?: 64, 
                                            Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmapCache[resourceId] = bitmap
            return bitmap
        }
        return null
    }

    override fun onStart() { super.onStart(); mapView?.onStart() }
    override fun onStop() { super.onStop(); mapView?.onStop() }
    override fun onDestroy() { 
        super.onDestroy()
        bitmapCache.clear()
        mapView?.onDestroy() 
    }
}