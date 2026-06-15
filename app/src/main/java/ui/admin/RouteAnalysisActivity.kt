package ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.databinding.ActivityRouteAnalysisBinding
import com.example.bustrack_app.models.ApplicationModel
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.turf.TurfMeasurement
import utils.ViewUtils

class RouteAnalysisActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteAnalysisBinding
    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    
    private var currentApplication: ApplicationModel? = null
    private var matchedRoute: RouteModel? = null
    private var matchedStop: StopItem? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentApplication = intent.getSerializableExtra("APPLICATION_DATA") as? ApplicationModel
        
        mapView = binding.mapView
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            initManagers()
            performAnalysis()
        }

        setupClickListeners()
    }

    private fun initManagers() {
        val annotationApi = mapView?.annotations
        pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        polylineAnnotationManager = annotationApi?.createPolylineAnnotationManager()
    }

    private fun performAnalysis() {
        val app = currentApplication ?: return
        val studentPoint = Point.fromLngLat(app.longitude, app.latitude)
        
        val routes = RouteRepository.routeList.value ?: emptyList()
        if (routes.isEmpty()) {
            Toast.makeText(this, "No routes available for analysis", Toast.LENGTH_SHORT).show()
            updateUIWithNoMatch()
            return
        }

        findBestMatch(studentPoint, routes)
        updateUI()
        drawOnMap(studentPoint)
    }

    private fun findBestMatch(studentPoint: Point, routes: List<RouteModel>) {
        var minDistance = Double.MAX_VALUE
        
        for (route in routes) {
            for (stop in route.stopsList) {
                val stopPoint = Point.fromLngLat(stop.longitude, stop.latitude)
                val distance = TurfMeasurement.distance(studentPoint, stopPoint)
                
                if (distance < minDistance) {
                    minDistance = distance
                    matchedRoute = route
                    matchedStop = stop
                }
            }
        }
    }

    private fun updateUI() {
        val app = currentApplication ?: return
        val route = matchedRoute
        val stop = matchedStop

        binding.tvStudentId.text = "Optimal match for Student ID:\n${app.studentIdString}"
        
        if (route != null && stop != null) {
            val distanceKm = TurfMeasurement.distance(
                Point.fromLngLat(app.longitude, app.latitude),
                Point.fromLngLat(stop.longitude, stop.latitude)
            )
            
            binding.tvRouteName.text = route.routeName
            binding.tvMatchPercent.text = calculateMatchPercent(distanceKm)
            binding.tvNearestStop.text = stop.stopName
            binding.tvDistance.text = String.format(java.util.Locale.US, "%.2f km away", distanceKm)
            
            if (app.image != 0) {
                binding.ivStudent.setImageResource(app.image)
            }
        } else {
            updateUIWithNoMatch()
        }
    }

    private fun calculateMatchPercent(distanceKm: Double): String {
        return when {
            distanceKm < 0.5 -> "98% Match"
            distanceKm < 1.0 -> "92% Match"
            distanceKm < 2.0 -> "85% Match"
            distanceKm < 5.0 -> "70% Match"
            else -> "Low Match"
        }
    }

    private fun updateUIWithNoMatch() {
        binding.tvRouteName.text = "No Route Found"
        binding.tvMatchPercent.text = "0% Match"
        binding.tvNearestStop.text = "N/A"
        binding.tvDistance.text = "Too far"
    }

    private fun drawOnMap(studentPoint: Point) {
        val stop = matchedStop
        val route = matchedRoute
        
        pointAnnotationManager?.deleteAll()
        polylineAnnotationManager?.deleteAll()

        // 1. Student Marker
        val studentOptions = PointAnnotationOptions()
            .withPoint(studentPoint)
            .withTextField("🏠 Student")
            .withTextColor("#1565C0")
            .withTextSize(12.0)
        pointAnnotationManager?.create(studentOptions)

        if (stop != null && route != null) {
            val stopPoint = Point.fromLngLat(stop.longitude, stop.latitude)

            // 2. Nearest Stop Marker
            val stopOptions = PointAnnotationOptions()
                .withPoint(stopPoint)
                .withTextField("🚏 Stop: ${stop.stopName}")
                .withTextColor("#DC2626")
                .withTextSize(12.0)
            pointAnnotationManager?.create(stopOptions)

            // 3. Matched Route Line (Primary Blue)
            if (route.pathPoints.isNotEmpty()) {
                val path = route.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                val lineOptions = PolylineAnnotationOptions()
                    .withPoints(path)
                    .withLineColor("#1565C0")
                    .withLineWidth(6.0)
                    .withLineOpacity(0.4)
                polylineAnnotationManager?.create(lineOptions)
            }

            // 4. Connection Line (Green) - From student to stop
            val connectionPoints = listOf(studentPoint, stopPoint)
            val connectionOptions = PolylineAnnotationOptions()
                .withPoints(connectionPoints)
                .withLineColor("#4CAF50") // Green
                .withLineWidth(4.0)
            polylineAnnotationManager?.create(connectionOptions)

            // Zoom out to show both student and stop
            val centerLat = (studentPoint.latitude() + stopPoint.latitude()) / 2
            val centerLng = (studentPoint.longitude() + stopPoint.longitude()) / 2
            
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(centerLng, centerLat))
                    .zoom(13.0)
                    .build()
            )
        } else {
            mapView?.mapboxMap?.setCamera(
                CameraOptions.Builder()
                    .center(studentPoint)
                    .zoom(15.0)
                    .build()
            )
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        binding.btnConfirm.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            
            val updatedApp = currentApplication?.copy(
                bestRoute = matchedRoute?.routeName ?: "None",
                routeCode = matchedRoute?.routeCode ?: "None",
                nearestStop = matchedStop?.stopName ?: "None",
                distance = binding.tvDistance.text.toString(),
                matchPercent = binding.tvMatchPercent.text.toString(),
                assignedBus = matchedRoute?.busNo ?: "Not Assigned",
                assignedDriver = matchedRoute?.driverName ?: "Not Assigned"
            )

            val intent = Intent(this, AssignmentConfirmationActivity::class.java)
            intent.putExtra("APPLICATION_DATA", updatedApp)
            startActivity(intent)
        }

        binding.fabLocation.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            currentApplication?.let {
                mapView?.mapboxMap?.setCamera(
                    CameraOptions.Builder()
                        .center(Point.fromLngLat(it.longitude, it.latitude))
                        .zoom(15.0)
                        .build()
                )
            }
        }
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
