package ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.R
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.StudentRepository
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
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import utils.ViewUtils
import kotlinx.coroutines.launch

class RouteAnalysisActivity : AppCompatActivity() {

    private companion object {
        const val ROUTE_CORRIDOR_MARGIN_METERS = 500.0
        const val MAX_NEARBY_ROUTE_DISTANCE_METERS = 4_000.0
    }

    private lateinit var binding: ActivityRouteAnalysisBinding
    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    
    private var currentApplication: ApplicationModel? = null
    private var matchedRoute: RouteModel? = null
    private var matchedStop: StopItem? = null
    private var matchedDistanceKm: Double? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentApplication = intent.getSerializableExtra("APPLICATION_DATA") as? ApplicationModel
        
        mapView = binding.mapView
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            initManagers()
            setupInitialCamera()
            performAnalysis()
        }

        setupClickListeners()
    }

    private fun initManagers() {
        val annotationApi = mapView?.annotations
        pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        polylineAnnotationManager = annotationApi?.createPolylineAnnotationManager()
    }

    private fun setupInitialCamera() {
        // Never leave an unresolved residential address on Mapbox's world/globe view.
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(73.0535, 33.5985)) // FG/PG College service area
                .zoom(14.0)
                .build()
        )
    }

    private fun performAnalysis() {
        val app = currentApplication ?: return

        if (app.latitude != 0.0 && app.longitude != 0.0) {
            analyseStudentPoint(Point.fromLngLat(app.longitude, app.latitude))
            return
        }

        // Older student records can have a typed residential address but no map point.
        // Resolve it inside Rawalpindi before declaring that no route can be matched.
        if (app.pickupPoint.isBlank()) {
            Toast.makeText(this, "Student residential address is missing.", Toast.LENGTH_LONG).show()
            updateUIWithNoMatch()
            return
        }
        lifecycleScope.launch {
            val resolved = RawalpindiLocationResolver.resolve(this@RouteAnalysisActivity, app.pickupPoint)
            if (resolved == null) {
                Toast.makeText(this@RouteAnalysisActivity, "Couldn't find this address in Rawalpindi.", Toast.LENGTH_LONG).show()
                updateUIWithNoMatch()
                return@launch
            }
            currentApplication = app.copy(
                latitude = resolved.point.latitude(),
                longitude = resolved.point.longitude()
            )
            if (app.studentDocumentId.isNotBlank()) {
                StudentRepository.updateStudentCoordinates(
                    app.studentDocumentId,
                    resolved.point.latitude(),
                    resolved.point.longitude()
                )
            }
            analyseStudentPoint(resolved.point)
        }
    }

    private fun analyseStudentPoint(studentPoint: Point) {
        // The repository listener can still hold its initial/previous snapshot when
        // this screen opens. Read the saved route documents before choosing a stop.
        RouteRepository.fetchLatestRoutes { routes ->
            if (isFinishing || isDestroyed) return@fetchLatestRoutes
            if (routes == null) {
                Toast.makeText(this, "Could not load the latest routes. Please try again.", Toast.LENGTH_LONG).show()
                updateUIWithNoMatch()
                return@fetchLatestRoutes
            }
            val activeRoutes = routes.filter { it.status.equals("ACTIVE", ignoreCase = true) }
            routes.forEach { route ->
                Log.d(
                    "RouteAnalysis",
                    "route=${route.routeName} routeId=${route.id} status=${route.status} " +
                        "savedStops=${route.stopsList.size} validStops=${route.stopsList.count(::hasValidCoordinates)} " +
                        "savedPathPoints=${route.pathPoints.size}"
                )
            }
            if (activeRoutes.isEmpty()) {
                Toast.makeText(this, "No active routes available for analysis", Toast.LENGTH_SHORT).show()
                updateUIWithNoMatch()
                return@fetchLatestRoutes
            }

            // First prefer routes whose saved path actually passes near the student's
            // home. Path distance is orientation-independent; walking distance then
            // chooses the pickup stop within that nearby route corridor.
            val routesWithPathDistance = activeRoutes.mapNotNull { route ->
                // Ignore malformed/placeholder geometry; one bad (0, 0) point can
                // otherwise make a route look close to every student on the map.
                val validPath = route.pathPoints.filter(::hasValidCoordinates)
                if (validPath.size < 2) null
                else route to distanceToRoutePathMeters(studentPoint, validPath)
            }
            val closestRoutePathDistance = routesWithPathDistance.minOfOrNull { it.second }
            routesWithPathDistance.forEach { (route, distanceMeters) ->
                Log.d("RouteAnalysis", "routePath route=${route.routeName} routeId=${route.id} distanceM=${distanceMeters.toInt()}")
            }
            val routeScopedCandidates = if (
                closestRoutePathDistance != null && closestRoutePathDistance <= MAX_NEARBY_ROUTE_DISTANCE_METERS
            ) {
                val nearbyRouteIds = routesWithPathDistance
                    .filter { it.second <= closestRoutePathDistance + ROUTE_CORRIDOR_MARGIN_METERS }
                    .map { it.first.id }
                    .toSet()
                Log.d("RouteAnalysis", "routeScope=nearby routeIds=$nearbyRouteIds closestPathM=${closestRoutePathDistance.toInt()}")
                activeRoutes.filter { it.id in nearbyRouteIds }
            } else {
                // Legacy routes may have no path geometry. Keep nearest-stop matching
                // available for those routes and for homes far outside every route.
                Log.d("RouteAnalysis", "routeScope=allActive closestPathM=${closestRoutePathDistance?.toInt()} reason=no-nearby-path-or-legacy-path")
                activeRoutes
            }

            val candidates = routeScopedCandidates.flatMap { route ->
                route.stopsList.filter(::hasValidCoordinates).map { stop -> route to stop }
            }
            lifecycleScope.launch {
                val walkingDistances = WalkingStopDistanceResolver.resolveMeters(
                    this@RouteAnalysisActivity,
                    studentPoint,
                    candidates.map { (_, stop) -> Point.fromLngLat(stop.longitude, stop.latitude) }
                )
                if (isFinishing || isDestroyed) return@launch
                if (walkingDistances == null) {
                    Toast.makeText(this@RouteAnalysisActivity, "Could not calculate walking distance. Check your connection and try again.", Toast.LENGTH_LONG).show()
                    updateUIWithNoMatch()
                    return@launch
                }
                findBestMatch(candidates, walkingDistances)
                updateUI()
                drawOnMap(studentPoint)
            }
        }
    }

    private fun hasValidCoordinates(stop: StopItem): Boolean =
        stop.latitude.isFinite() && stop.longitude.isFinite() &&
            stop.latitude in -90.0..90.0 && stop.longitude in -180.0..180.0 &&
            !(stop.latitude == 0.0 && stop.longitude == 0.0)

    private fun hasValidCoordinates(point: com.example.bustrack_app.models.LatLngModel): Boolean =
        point.latitude.isFinite() && point.longitude.isFinite() &&
            point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0 &&
            !(point.latitude == 0.0 && point.longitude == 0.0)

    /** Direction-free distance from a point to the route's saved polyline, in meters. */
    private fun distanceToRoutePathMeters(
        point: Point,
        path: List<com.example.bustrack_app.models.LatLngModel>
    ): Double {
        val earthRadius = 6_371_008.8
        val referenceLatitudeRadians = Math.toRadians(point.latitude())
        fun project(latitude: Double, longitude: Double): Pair<Double, Double> =
            Math.toRadians(longitude - point.longitude()) * earthRadius * kotlin.math.cos(referenceLatitudeRadians) to
                Math.toRadians(latitude - point.latitude()) * earthRadius

        val projected = path.map { project(it.latitude, it.longitude) }
        if (projected.size == 1) return kotlin.math.hypot(projected[0].first, projected[0].second)
        var nearest = Double.MAX_VALUE
        projected.zipWithNext().forEach { (start, end) ->
            val dx = end.first - start.first
            val dy = end.second - start.second
            val segmentLengthSquared = dx * dx + dy * dy
            val fraction = if (segmentLengthSquared == 0.0) 0.0 else
                ((-start.first * dx - start.second * dy) / segmentLengthSquared).coerceIn(0.0, 1.0)
            val closestX = start.first + fraction * dx
            val closestY = start.second + fraction * dy
            nearest = minOf(nearest, kotlin.math.hypot(closestX, closestY))
        }
        return nearest
    }

    private fun findBestMatch(
        candidates: List<Pair<RouteModel, StopItem>>,
        walkingDistancesMeters: List<Double?>
    ) {
        var minDistance = Double.MAX_VALUE
        matchedRoute = null
        matchedStop = null
        matchedDistanceKm = null
        val measured = mutableListOf<Triple<Double, RouteModel, StopItem>>()

        candidates.forEachIndexed { index, (route, stop) ->
            val walkingMeters = walkingDistancesMeters.getOrNull(index)
            if (walkingMeters == null) return@forEachIndexed
            val distanceKm = walkingMeters / 1000.0
            measured += Triple(distanceKm, route, stop)
            if (distanceKm < minDistance) {
                minDistance = distanceKm
                matchedRoute = route
                matchedStop = stop
                matchedDistanceKm = distanceKm
            }
        }
        measured.sortedBy { it.first }.take(5).forEachIndexed { rank, (distanceKm, route, stop) ->
            Log.d(
                "RouteAnalysis",
                "walkRank=${rank + 1} route=${route.routeName} routeId=${route.id} stop=${stop.stopName} " +
                    "distanceM=${String.format(java.util.Locale.US, "%.1f", distanceKm * 1000.0)} " +
                    "stopLat=${stop.latitude} stopLng=${stop.longitude}"
            )
        }
    }

    private fun updateUI() {
        val app = currentApplication ?: return
        val route = matchedRoute
        val stop = matchedStop

        binding.tvStudentId.text = "Optimal match for Roll Number:\n${app.studentIdString.ifBlank { "Not set" }}"
        
        if (route != null && stop != null) {
            val distanceKm = matchedDistanceKm ?: run {
                updateUIWithNoMatch()
                return
            }
            
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

            // Keep every other valid saved stop visible as an
            // unfilled black outline marker.
            allRoutes.flatMap { it.stopsList }
                .filter(::hasValidCoordinates)
                .distinctBy { it.latitude to it.longitude }
                .filterNot { it.latitude == stop.latitude && it.longitude == stop.longitude }
                .forEach { otherStop ->
                    val otherStopOptions = PointAnnotationOptions()
                        .withPoint(Point.fromLngLat(otherStop.longitude, otherStop.latitude))
                        .withTextField(otherStop.stopName)
                        .withTextColor("#000000")
                        .withTextSize(10.0)
                        .withIconImage(ViewUtils.getBitmapFromVectorDrawable(this, R.drawable.outline_location)!!)
                        .withIconColor("#000000")
                    pointAnnotationManager?.create(otherStopOptions)
                }

            // 3. Matched Stop Marker
            val stopOptions = PointAnnotationOptions()
                .withPoint(stopPoint)
                .withTextField("🚏 Matched Stop: ${stop.stopName}")
                .withTextColor("#DC2626")
                .withTextSize(12.0)
                .withIconImage(ViewUtils.getBitmapFromVectorDrawable(this, R.drawable.ic_marker_dest)!!)
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
                .profile(DirectionsCriteria.PROFILE_WALKING)
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

            // A route assignment is valid only when analysis found both a route and one
            // of its stops. Do not let the fallback "None" values enter confirmation.
            if (matchedRoute == null || matchedStop == null) {
                Toast.makeText(this, "Please assign a route first, then confirm the assignment.", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            
            val updatedApp = currentApplication?.copy(
                bestRoute = matchedRoute!!.routeName,
                routeCode = matchedRoute!!.routeCode,
                nearestStop = matchedStop!!.stopName,
                distance = binding.tvDistance.text.toString(),
                matchPercent = binding.tvMatchPercent.text.toString(),
                assignedBus = matchedRoute!!.busNo ?: "Not Assigned",
                assignedDriver = matchedRoute!!.driverName ?: "Not Assigned"
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
