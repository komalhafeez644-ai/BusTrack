package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.example.bustrack_app.R
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.viewmodels.LiveTrackingViewModel
import com.google.android.material.button.MaterialButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotation
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class LiveTrackingActivity : AppCompatActivity() {

    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private val viewModel: LiveTrackingViewModel by viewModels()
    private val driverMarkers = mutableMapOf<String, PointAnnotation>()
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking)

        supportActionBar?.hide()

        mapView = findViewById(R.id.mapView)
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) { style ->
            val annotationApi = mapView?.annotations
            pointAnnotationManager = annotationApi?.createPointAnnotationManager()

            // Add bus icon to map style
            val bitmap = bitmapFromDrawableRes(this@LiveTrackingActivity, R.drawable.blue_bus)
            bitmap?.let { style.addImage("bus-icon", it) }

            // Default center on Islamabad (Pakistan)
            val defaultPoint = Point.fromLngLat(73.0479, 33.6844)
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(defaultPoint)
                    .zoom(12.0)
                    .build()
            )

            pointAnnotationManager?.addClickListener { annotation ->
                val driver = viewModel.activeDrivers.value?.find {
                    val latDiff = Math.abs(it.latitude - annotation.point.latitude())
                    val lngDiff = Math.abs(it.longitude - annotation.point.longitude())
                    latDiff < 0.0001 && lngDiff < 0.0001
                }
                driver?.let {
                    viewModel.selectDriver(it)
                    focusOnDriver(it)
                }
                true
            }

            observeViewModel()
        }

        setupUI()
    }

    private fun setupUI() {
        findViewById<MaterialButton>(R.id.btnTrackDriver)?.setOnClickListener {
            val selected = viewModel.selectedDriver.value
            if (selected != null) {
                val intent = Intent(this, TrackDriverActivity::class.java)
                intent.putExtra("DRIVER_ID", selected.id)
                startActivity(intent)
            } else {
                android.widget.Toast.makeText(this, "Please select a bus to track", android.widget.Toast.LENGTH_SHORT).show()
            }
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }

        findViewById<EditText>(R.id.etSearchBus)?.addTextChangedListener { text ->
            val query = text.toString().lowercase()
            if (query.isNotEmpty()) {
                viewModel.activeDrivers.value?.find { 
                    it.name.lowercase().contains(query) || it.assignedBus?.lowercase()?.contains(query) == true 
                }?.let { driver ->
                    focusOnDriver(driver)
                    viewModel.selectDriver(driver)
                }
            }
        }

        // Map Controls
        findViewById<View>(R.id.mapControls)?.let { controls ->
            controls.findViewById<View>(R.id.zoomInCard)?.setOnClickListener {
                mapView?.mapboxMap?.setCamera(CameraOptions.Builder().zoom(mapView?.mapboxMap?.cameraState?.zoom?.plus(1.0)).build())
            }
            controls.findViewById<View>(R.id.zoomOutCard)?.setOnClickListener {
                mapView?.mapboxMap?.setCamera(CameraOptions.Builder().zoom(mapView?.mapboxMap?.cameraState?.zoom?.minus(1.0)).build())
            }
            controls.findViewById<View>(R.id.myLocationCard)?.setOnClickListener {
                // Focus on all buses if available, otherwise default
                val drivers = viewModel.activeDrivers.value
                if (!drivers.isNullOrEmpty()) {
                    focusOnDriver(drivers[0])
                } else {
                    val defaultPoint = Point.fromLngLat(73.0479, 33.6844)
                    mapView?.mapboxMap?.setCamera(CameraOptions.Builder().center(defaultPoint).zoom(12.0).build())
                }
            }
        }
    }

    private fun observeViewModel() {
        viewModel.activeDrivers.observe(this) { drivers ->
            if (drivers.isEmpty()) {
                findViewById<View>(R.id.driverCard).visibility = View.GONE
            }
            updateMarkers(drivers)
        }

        viewModel.selectedDriver.observe(this) { driver ->
            updateDriverCard(driver)
        }
    }

    private fun updateMarkers(drivers: List<DriverModel>) {
        pointAnnotationManager?.deleteAll()
        driverMarkers.clear()

        drivers.forEach { driver ->
            val point = Point.fromLngLat(driver.longitude, driver.latitude)
            val options = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage("bus-icon")
                .withIconSize(1.5)
                .withTextField(driver.assignedBus ?: driver.name)
                .withTextOffset(listOf(0.0, 2.0))
                .withTextColor(android.graphics.Color.WHITE)
                .withTextHaloColor(android.graphics.Color.BLACK)
                .withTextHaloWidth(1.0)

            val annotation = pointAnnotationManager?.create(options)
            if (annotation != null) {
                driverMarkers[driver.id] = annotation
            }
        }

        if (drivers.isNotEmpty() && viewModel.selectedDriver.value == null) {
            viewModel.selectDriver(drivers[0])
            focusOnDriver(drivers[0])
        }
    }

    private fun focusOnDriver(driver: DriverModel) {
        if (driver.latitude != 0.0) {
            val point = Point.fromLngLat(driver.longitude, driver.latitude)
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(14.0)
                    .build()
            )
        }
    }

    private fun updateDriverCard(driver: DriverModel?) {
        val card = findViewById<View>(R.id.driverCard)
        if (driver == null) {
            card.visibility = View.GONE
            return
        }
        
        // Show with animation for inDrive feel
        if (card.visibility == View.GONE) {
            card.visibility = View.VISIBLE
            card.alpha = 0f
            card.translationY = 100f
            card.animate().alpha(1f).translationY(0f).setDuration(400).start()
        }

        findViewById<TextView>(R.id.tvDriverName)?.text = driver.name
        findViewById<TextView>(R.id.tvBusRouteInfo)?.text = "Bus #${driver.assignedBus ?: "N/A"} • ${driver.route ?: "No Route"}"
        findViewById<TextView>(R.id.tvRouteDetail)?.text = "Active Status: ${driver.status}"
        
        findViewById<TextView>(R.id.tvEta)?.text = "On Way"
        findViewById<TextView>(R.id.tvSpeed)?.text = if (driver.latitude != 0.0) "38 km/h" else "Idle"
        findViewById<TextView>(R.id.tvLoad)?.text = "Online"
    }

    override fun onResume() {
        super.onResume()
        utils.NavigationUtils.setupBottomNavigation(this)
    }

    override fun onStart() { super.onStart(); mapView?.onStart() }
    override fun onStop() { super.onStop(); mapView?.onStop() }
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

    override fun onDestroy() { 
        super.onDestroy()
        bitmapCache.clear()
        mapView?.onDestroy() 
    }
}