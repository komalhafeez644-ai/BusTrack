package ui.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.ContextCompat
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityRouteMapBinding
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class RouteMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteMapBinding
    private var currentRoute: RouteModel? = null
    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val routeId = intent.getStringExtra("ROUTE_ID")
        currentRoute = com.example.bustrack_app.data.RouteRepository.routeList.value?.find { it.id == routeId }

        if (currentRoute == null) {
            Toast.makeText(this, "Route not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        mapView = binding.mapView
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupInitialCamera()
            updateMapUI()
        }

        // Tap to show Add Stop Dialog
        mapView?.mapboxMap?.addOnMapClickListener { point ->
            showAddStopDialog(point)
            true
        }

        binding.btnClose.setOnClickListener {
            finish()
        }

        binding.btnSaveMap.setOnClickListener {
            currentRoute?.let {
                com.example.bustrack_app.data.RouteRepository.updateRoute(it)
                Toast.makeText(this, "Route Mapping Updated", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun setupInitialCamera() {
        val route = currentRoute ?: return
        val centerPoint = if (route.stopsList.isNotEmpty()) {
            Point.fromLngLat(route.stopsList.last().longitude, route.stopsList.last().latitude)
        } else if (route.pathPoints.isNotEmpty()) {
            Point.fromLngLat(route.pathPoints.first().longitude, route.pathPoints.first().latitude)
        } else {
            Point.fromLngLat(73.0535, 33.5985) // Rawalpindi FG College
        }

        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(centerPoint)
                .zoom(14.0)
                .build()
        )
    }

    private fun showAddStopDialog(point: Point) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_stop_map, null)
        val etName = dialogView.findViewById<EditText>(R.id.etStopName)
        val tvLat = dialogView.findViewById<TextView>(R.id.tvLatValue)
        val tvLng = dialogView.findViewById<TextView>(R.id.tvLngValue)

        tvLat.text = String.format(Locale.US, "%.6f", point.latitude())
        tvLng.text = String.format(Locale.US, "%.6f", point.longitude())

        AlertDialog.Builder(this)
            .setTitle("Add New Stop")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val name = etName.text.toString().trim()
                if (name.isNotEmpty()) {
                    saveNewStop(name, point)
                } else {
                    Toast.makeText(this, "Stop name is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun saveNewStop(name: String, point: Point) {
        val route = currentRoute ?: return
        val nextId = String.format(Locale.getDefault(), "%02d", route.stopsList.size + 1)
        
        val newStop = StopItem(
            id = nextId,
            stopName = name,
            time = "ETA: --",
            latitude = point.latitude(),
            longitude = point.longitude()
        )
        
        route.stopsList.add(newStop)
        updateMapUI()
    }

    private fun updateMapUI() {
        val route = currentRoute ?: return
        val annotationApi = mapView?.annotations ?: return
        
        if (pointAnnotationManager == null) {
            pointAnnotationManager = annotationApi.createPointAnnotationManager()
        }
        if (polylineAnnotationManager == null) {
            polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
        }
        
        pointAnnotationManager?.deleteAll()
        polylineAnnotationManager?.deleteAll()

        // 1. Draw Path Points (The highlighted route) - Primary Blue
        if (route.pathPoints.isNotEmpty()) {
            val path = route.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
            val pathOptions = PolylineAnnotationOptions()
                .withPoints(path)
                .withLineColor("#1565C0") // Primary Blue
                .withLineWidth(8.0)
                .withLineOpacity(0.7)
            polylineAnnotationManager?.create(pathOptions)
        }

        // 2. Draw Stop Markers
        val stopPoints = mutableListOf<Point>()
        val bitmap = bitmapFromDrawableRes(R.drawable.ic_marker_dest)
        
        route.stopsList.forEachIndexed { index, stop ->
            val point = Point.fromLngLat(stop.longitude, stop.latitude)
            stopPoints.add(point)

            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withTextField("${index + 1}. ${stop.stopName}")
                .withTextSize(12.0)
                .withTextColor("#DC2626")
                .withTextOffset(listOf(0.0, 2.0))
            
            bitmap?.let { b ->
                pointAnnotationOptions.withIconImage(b)
                pointAnnotationOptions.withIconSize(1.0)
            }
            
            pointAnnotationManager?.create(pointAnnotationOptions)
        }

        // 3. Draw Road Path between stops if path is empty
        if (stopPoints.size > 1 && route.pathPoints.isEmpty()) {
            fetchAndDrawRoadRoute(stopPoints)
        }

        pointAnnotationManager?.addLongClickListener { annotation ->
            val text = annotation.textField ?: ""
            val name = text.substringAfter(". ")
            val stop = currentRoute?.stopsList?.find { it.stopName == name }
            stop?.let {
                currentRoute?.stopsList?.remove(it)
                Toast.makeText(this, "Stop removed", Toast.LENGTH_SHORT).show()
                updateMapUI()
            }
            true
        }
    }

    private fun bitmapFromDrawableRes(resourceId: Int): Bitmap? {
        val drawable = ContextCompat.getDrawable(this, resourceId)
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

    private fun fetchAndDrawRoadRoute(points: List<Point>) {
        val client = MapboxDirections.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .routeOptions(
                RouteOptions.builder()
                    .coordinatesList(points)
                    .profile(DirectionsCriteria.PROFILE_DRIVING)
                    .overview(DirectionsCriteria.OVERVIEW_FULL)
                    .build()
            )
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                val body = response.body()
                if (body == null || body.routes().isEmpty()) return

                val geometry = body.routes()[0].geometry() ?: return
                val lineString = LineString.fromPolyline(geometry, 6)
                
                val polylineOptions = PolylineAnnotationOptions()
                    .withPoints(lineString.coordinates())
                    .withLineColor("#1565C0")
                    .withLineWidth(5.0)
                
                polylineAnnotationManager?.create(polylineOptions)
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("RouteMapActivity", "Error fetching route: ${t.message}")
            }
        })
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
        super.onDestroy()
        mapView?.onDestroy()
    }
}
