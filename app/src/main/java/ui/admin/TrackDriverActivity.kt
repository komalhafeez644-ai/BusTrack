package ui.admin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
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
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.extension.style.layers.getLayer
import com.mapbox.maps.extension.style.sources.getSource
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.turf.TurfMeasurement

class TrackDriverActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private var mapView: MapView? = null
    private val viewModel: TrackDriverViewModel by viewModels()
    
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var driverMarker: PointAnnotation? = null
    private val stopMarkers = mutableListOf<PointAnnotation>()
    
    private lateinit var stopsAdapter: NavigationStopsAdapter
    private val ROUTE_SOURCE_ID = "route-source-id"
    private val ROUTE_LAYER_ID = "route-layer-id"
    
    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    private var currentRouteId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_driver)

        supportActionBar?.hide()

        val driverId = intent.getStringExtra("DRIVER_ID") ?: ""
        if (driverId.isEmpty()) {
            Toast.makeText(this, "Invalid Driver ID", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mapView = findViewById(R.id.mapView)
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            pointAnnotationManager = mapView?.annotations?.createPointAnnotationManager()
            observeViewModel()
        }

        setupBottomSheet()
        viewModel.setDriverId(driverId)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupBottomSheet() {
        val bottomSheet = findViewById<NestedScrollView>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        
        stopsAdapter = NavigationStopsAdapter(emptyList())
        val rvStops = findViewById<RecyclerView>(R.id.rvStopsTimeline)
        rvStops.layoutManager = LinearLayoutManager(this)
        rvStops.adapter = stopsAdapter
    }

    private fun observeViewModel() {
        viewModel.targetDriver.observe(this) { driver ->
            driver?.let { updateUI(it) }
        }

        viewModel.assignedRoute.observe(this) { route ->
            route?.let { 
                if (currentRouteId != it.id) {
                    currentRouteId = it.id
                    drawRouteOnMap(it)
                }
            }
        }
    }

    private fun updateUI(driver: DriverModel) {
        // Update Bottom Sheet Header
        findViewById<TextView>(R.id.tvBusIdSheet).text = driver.assignedBus ?: "N/A"
        findViewById<TextView>(R.id.tvDriverNameSheet).text = driver.name
        findViewById<TextView>(R.id.tvRouteNameSheet).text = driver.route ?: "No Route"
        findViewById<TextView>(R.id.tvCurrentLocationSheet).text = java.util.Locale.getDefault().let { locale ->
            "Current: Lat ${String.format(locale, "%.4f", driver.latitude)}, Lng ${String.format(locale, "%.4f", driver.longitude)}"
        }

        // Update Marker
        val point = Point.fromLngLat(driver.longitude, driver.latitude)
        if (driverMarker == null) {
            val bitmap = bitmapFromDrawableRes(this, R.drawable.ic_driver)
            bitmap?.let {
                val options = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(it)
                    .withIconSize(1.5)
                driverMarker = pointAnnotationManager?.create(options)
            }
        } else {
            driverMarker?.point = point
        }

        // Calculate Stop Progress
        viewModel.assignedRoute.value?.let { route ->
            calculateProgress(driver, route)
        }
        
        // Smooth Camera Move
        mapView?.mapboxMap?.flyTo(
            CameraOptions.Builder().center(point).build(),
            MapAnimationOptions.mapAnimationOptions { duration(1000) }
        )
    }

    private fun calculateProgress(driver: DriverModel, route: RouteModel) {
        val driverPoint = Point.fromLngLat(driver.longitude, driver.latitude)
        var currentIndex = 0
        var status = "NEXT"

        for (i in route.stopsList.indices) {
            val stop = route.stopsList[i]
            val stopPoint = Point.fromLngLat(stop.longitude, stop.latitude)
            val distance = TurfMeasurement.distance(driverPoint, stopPoint, com.mapbox.turf.TurfConstants.UNIT_METERS)
            
            if (distance < 50.0) { // 50m radius
                currentIndex = i
                status = "ARRIVED"
                break
            } else {
                // Determine if we passed it or are approaching
                // Simple version: find the first stop that is in front of us
                // For a more accurate logic, we'd need to check route orientation
                currentIndex = i
                status = "NEXT"
            }
        }
        
        stopsAdapter.updateStops(route.stopsList, currentIndex, status)
    }

    private fun drawRouteOnMap(route: RouteModel) {
        if (route.pathPoints.isEmpty()) return

        val points = route.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
        val lineString = LineString.fromLngLats(points)

        mapView?.mapboxMap?.getStyle { style ->
            if (!style.styleSourceExists(ROUTE_SOURCE_ID)) {
                style.addSource(geoJsonSource(ROUTE_SOURCE_ID) {
                    geometry(lineString)
                })
            } else {
                val source = style.getSource(ROUTE_SOURCE_ID) as? com.mapbox.maps.extension.style.sources.generated.GeoJsonSource
                source?.geometry(lineString)
            }

            if (!style.styleLayerExists(ROUTE_LAYER_ID)) {
                style.addLayer(lineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID) {
                    lineColor(Color.parseColor("#2563EB"))
                    lineWidth(6.0)
                    lineOpacity(0.6)
                })
            }
        }

        // Update Stop Markers
        stopMarkers.forEach { pointAnnotationManager?.delete(it) }
        stopMarkers.clear()
        
        route.stopsList.forEach { stop ->
            val bitmap = bitmapFromDrawableRes(this, R.drawable.ic_marker_dest)
            bitmap?.let {
                val options = PointAnnotationOptions()
                    .withPoint(Point.fromLngLat(stop.longitude, stop.latitude))
                    .withIconImage(it)
                    .withIconSize(1.0)
                    .withTextField(stop.stopName)
                    .withTextSize(10.0)
                    .withTextOffset(listOf(0.0, 1.5))
                pointAnnotationManager?.create(options)?.let { stopMarkers.add(it) }
            }
        }
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