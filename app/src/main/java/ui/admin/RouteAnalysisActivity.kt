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
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.turf.TurfMeasurement
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
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
        
        // Validation: If coordinates are missing, analysis won't be accurate
        if (app.latitude == 0.0 || app.longitude == 0.0) {
            Toast.makeText(this, "Student location coordinates missing. Please set location on map first.", Toast.LENGTH_LONG).show()
            updateUIWithNoMatch()
            return
        }

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
        
        // Reset matches
        matchedRoute = null
        matchedStop = null

        for (route in routes) {
            // Consider all assigned routes for optimization
            for (stop in route.stopsList) {
                // Skip invalid stop coordinates
                if (stop.latitude == 0.0 || stop.longitude == 0.0) continue
                
                val stopPoint = Point.fromLngLat(stop.longitude, stop.latitude)
                
                // Using Turf to get precise distance in Kilometers
                val distance = TurfMeasurement.distance(studentPoint, stopPoint, com.mapbox.turf.TurfConstants.UNIT_KILOMETERS)
                
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

        // 1. Draw Other Routes (Background - Light Gray)
        val allRoutes = RouteRepository.routeList.value ?: emptyList()
        for (r in allRoutes) {
            if (r.id == route?.id) continue // Skip matched route for now
            if (r.pathPoints.isNotEmpty()) {
                val path = r.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                val otherLineOptions = PolylineAnnotationOptions()
                    .withPoints(path)
                    .withLineColor("#94A3B8") // Slate Gray
                    .withLineWidth(3.0)
                    .withLineOpacity(0.3)
                polylineAnnotationManager?.create(otherLineOptions)
            }
        }

        // 2. Student Marker
        val studentOptions = PointAnnotationOptions()
            .withPoint(studentPoint)
            .withTextField("🏠 Residence")
            .withTextColor("#1E293B")
            .withTextSize(12.0)
            .withIconImage(ViewUtils.getBitmapFromVectorDrawable(this, R.drawable.outline_location)!!)
            .withIconColor("#1565C0")
        pointAnnotationManager?.create(studentOptions)

        if (stop != null && route != null) {
            val stopPoint = Point.fromLngLat(stop.longitude, stop.latitude)

            // 3. Matched Stop Marker
            val stopOptions = PointAnnotationOptions()
                .withPoint(stopPoint)
                .withTextField("🚏 Matched Stop: ${stop.stopName}")
                .withTextColor("#DC2626")
                .withTextSize(12.0)
                .withIconImage(ViewUtils.getBitmapFromVectorDrawable(this, R.drawable.outline_location)!!)
                .withIconColor("#DC2626")
            pointAnnotationManager?.create(stopOptions)

            // 4. Matched Route Line (Primary Blue - Bold)
            if (route.pathPoints.isNotEmpty()) {
                val path = route.pathPoints.map { Point.fromLngLat(it.longitude, it.latitude) }
                val matchedLineOptions = PolylineAnnotationOptions()
                    .withPoints(path)
                    .withLineColor("#1565C0") // Royal Blue
                    .withLineWidth(8.0)
                    .withLineOpacity(0.7)
                polylineAnnotationManager?.create(matchedLineOptions)
            }

            // 5. Road-Matched Connection Path (From Student to Stop)
            fetchRoadMatchedPath(studentPoint, stopPoint)

        } else {
            // Focus on student only if no match
            mapView?.mapboxMap?.flyTo(
                CameraOptions.Builder()
                    .center(studentPoint)
                    .zoom(15.0)
                    .build(),
                MapAnimationOptions.mapAnimationOptions { duration(2000) }
            )
        }
    }

    private fun fetchRoadMatchedPath(origin: Point, destination: Point) {
        val client = MapboxDirections.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .routeOptions(RouteOptions.builder()
                .coordinatesList(listOf(origin, destination))
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .build())
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                val body = response.body() ?: return
                val directionsRoute = body.routes().firstOrNull() ?: return
                val geometry = directionsRoute.geometry() ?: return
                
                val lineString = LineString.fromPolyline(geometry, 6)
                val roadPath = lineString.coordinates()

                runOnUiThread {
                    // Draw Connection Line (Green - Dash or solid)
                    val connectionOptions = PolylineAnnotationOptions()
                        .withPoints(roadPath)
                        .withLineColor("#10B981") // Emerald Green
                        .withLineWidth(6.0)
                    polylineAnnotationManager?.create(connectionOptions)

                    // Focus Camera on the connection path
                    val cameraOptions = mapView?.mapboxMap?.cameraForCoordinates(
                        roadPath,
                        EdgeInsets(200.0, 100.0, 200.0, 100.0), // Padding
                        null,
                        null
                    )
                    
                    if (cameraOptions != null) {
                        mapView?.mapboxMap?.flyTo(
                            cameraOptions,
                            MapAnimationOptions.mapAnimationOptions { duration(2500) }
                        )
                    }
                }
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                // Fallback to straight line if directions API fails
                runOnUiThread {
                    val fallbackPoints = listOf(origin, destination)
                    val fallbackOptions = PolylineAnnotationOptions()
                        .withPoints(fallbackPoints)
                        .withLineColor("#10B981")
                        .withLineWidth(6.0)
                    polylineAnnotationManager?.create(fallbackOptions)
                }
            }
        })
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
