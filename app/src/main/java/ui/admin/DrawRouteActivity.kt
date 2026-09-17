package ui.admin

import android.content.Intent
import android.graphics.Bitmap
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.drawable.toBitmap
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityDrawRouteBinding
import com.example.bustrack_app.models.LatLngModel
import com.example.bustrack_app.models.LocationModel
import com.example.bustrack_app.data.LocationRepository
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.search.*
import com.mapbox.search.result.*
import com.mapbox.search.common.IsoCountryCode
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.*
import com.mapbox.maps.plugin.annotation.Annotation
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

class DrawRouteActivity : AppCompatActivity() {

    companion object {
        // Same Rawalpindi service area used by LocationPickerActivity. Keeping this
        // boundary makes addresses entered before "Select on Map" resolve exactly as
        // they do in the location picker, instead of depending on the SDK's broad,
        // occasionally empty suggestion response.
        private const val RAWALPINDI_BBOX = "72.70,33.35,73.35,33.90"
    }

    private lateinit var binding: ActivityDrawRouteBinding
    private var mapView: MapView? = null

    private var sourcePoint: Point? = null
    private var destinationPoint: Point? = null
    private var roadPathPoints = mutableListOf<Point>()

    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var sourceMarker: PointAnnotation? = null
    private var destinationMarker: PointAnnotation? = null

    private var startAddress: String = ""
    private var endAddress: String = ""
    private var searchJob: Job? = null
    private lateinit var searchEngine: SearchEngine

    private lateinit var searchAdapter: SearchAdapter
    private val routeCache = mutableMapOf<String, String>()

    private var isSearchingSource = true

    // Stale-search fix: har naye search attempt (ya selection/clear) par ye badhta hai.
    // performSearch() apna khud ka async coroutine spawn karta hai jo searchJob.cancel() se
    // cancel NAHI hota - is liye purani/dheemi query ka result baad mein aakar UI ko galat
    // taur par overwrite/hide kar deta tha (suggestion dikh kar gayab hona, select na hona).
    // Ab har UI-update se pehle check hota hai ki ye result abhi bhi "latest" request ka hai.
    private var searchRequestId = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            SearchEngineSettings()
        )

        mapView = binding.mapView
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupInitialCamera()
            initAnnotationManagers()
            enableUserLocation()
            handleIncomingManualPoints()
        }

        mapView?.mapboxMap?.addOnMapClickListener { point ->
            if (sourcePoint == null) {
                setSource(point, "Dropped Pin")
            } else if (destinationPoint == null) {
                setDestination(point, "Dropped Pin")
            } else {
                // Already have both, maybe move destination? Or toast?
                Toast.makeText(this, "Drag markers to adjust locations", Toast.LENGTH_SHORT).show()
            }
            true
        }

        setupRecyclerView()
        setupSearch()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClearPath.setOnClickListener { clearPath() }

        binding.btnDone.setOnClickListener {
            if (sourcePoint == null || destinationPoint == null) {
                Toast.makeText(this, "Please select both source and destination", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            finishWithResult()
        }
    }

    private fun setSource(point: Point, address: String? = null) {
        sourcePoint = point
        updateSourceMarker(point)
        if (address != null) {
            startAddress = address
            binding.etSourceLocation.setText(address)
        } else {
            updateAddressFromPoint(point, isStart = true)
        }
        calculateRoute()
    }

    private fun setDestination(point: Point, address: String? = null) {
        destinationPoint = point
        updateDestinationMarker(point)
        if (address != null) {
            endAddress = address
            binding.etDestinationLocation.setText(address)
        } else {
            updateAddressFromPoint(point, isStart = false)
        }
        calculateRoute()
    }

    private fun updateSourceMarker(point: Point) {
        sourceMarker?.let { pointAnnotationManager?.delete(it) }
        val options = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(ContextCompat.getDrawable(this, R.drawable.green_dot)!!.toBitmap())
            .withIconSize(1.0)
            .withDraggable(true)
        sourceMarker = pointAnnotationManager?.create(options)
    }

    private fun updateDestinationMarker(point: Point) {
        destinationMarker?.let { pointAnnotationManager?.delete(it) }
        val options = PointAnnotationOptions()
            .withPoint(point)
            .withIconImage(ContextCompat.getDrawable(this, R.drawable.ic_marker_dest)!!.toBitmap())
            .withIconSize(1.0)
            .withDraggable(true)
        destinationMarker = pointAnnotationManager?.create(options)
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchAdapter(listOf()) { item ->
            if (item is SearchSuggestion) {
                searchEngine.select(item, object : SearchSelectionCallback {
                    override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                        handleSearchResult(result.coordinate, result.name)
                    }
                    override fun onResults(suggestion: SearchSuggestion, results: List<SearchResult>, responseInfo: ResponseInfo) {}
                    override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {}
                    override fun onError(e: Exception) {
                        runOnUiThread { Toast.makeText(this@DrawRouteActivity, "Error selecting location", Toast.LENGTH_SHORT).show() }
                    }
                })
            } else if (item is LocationModel) {
                try {
                    val lat = item.latitude
                    val lng = item.longitude

                    // Invalid/missing/garbage coordinates ko yahin pakdo - crash hone se pehle.
                    val isValidCoordinate = lat.isFinite() && lng.isFinite() &&
                            !(lat == 0.0 && lng == 0.0) &&
                            lat in -90.0..90.0 && lng in -180.0..180.0

                    if (!isValidCoordinate) {
                        Log.e("DrawRoute", "Stored location '${item.name}' (id=${item.id}) has invalid coordinates: lat=$lat, lng=$lng")
                        Toast.makeText(this@DrawRouteActivity, "DEBUG3: invalid coords lat=$lat lng=$lng for '${item.name}'", Toast.LENGTH_LONG).show()
                    } else {
                        // Lat/Lng Firestore mein already available hain - dobara geocode/search
                        // karne ki zaroorat nahi, seedha use karo.
                        handleSearchResult(Point.fromLngLat(lng, lat), item.name)
                    }
                } catch (e: Exception) {
                    // Firestore ka toObject() Kotlin ki null-safety ko reflection se bypass kar
                    // sakta hai (missing/mismatched field name -> boxed null -> unboxing par NPE).
                    // Is se app crash na ho, is liye yahan catch karke gracefully handle karo.
                    Log.e("DrawRoute", "Crash prevented while selecting stored location '${item.name}': ${e.message}", e)
                    // TEMP DEBUG: exact exception seedha Toast mein dikha rahe hain taaki
                    // Logcat access ke bagair bhi root cause pata chal sake. Baad mein hata dena.
                    Toast.makeText(this@DrawRouteActivity, "DEBUG: ${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter
    }

    private fun initAnnotationManagers() {
        val annotationApi = mapView?.annotations ?: return
        polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
        pointAnnotationManager = annotationApi.createPointAnnotationManager()

        pointAnnotationManager?.addDragListener(object : OnPointAnnotationDragListener {
            override fun onAnnotationDragStarted(annotation: Annotation<*>) {}
            override fun onAnnotationDrag(annotation: Annotation<*>) {}
            override fun onAnnotationDragFinished(annotation: Annotation<*>) {
                val point = (annotation as PointAnnotation).point
                if (annotation.id == sourceMarker?.id) {
                    sourcePoint = point
                    updateAddressFromPoint(point, true)
                } else if (annotation.id == destinationMarker?.id) {
                    destinationPoint = point
                    updateAddressFromPoint(point, false)
                }
                calculateRoute()
            }
        })
    }

    private fun setupInitialCamera() {
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(73.0535, 33.5985)) // FG College
                .zoom(14.0)
                .build()
        )
    }

    private fun enableUserLocation() {
        mapView?.location?.run {
            enabled = true
            pulsingEnabled = true
        }
    }

    private fun setupSearch() {
        binding.etSourceLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                binding.btnClearSource.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                if (query == startAddress) return // Don't search if it's what we just set

                isSearchingSource = true
                searchJob?.cancel()
                if (query.length >= 2) {
                    val requestId = ++searchRequestId
                    searchJob = lifecycleScope.launch {
                        delay(600)
                        performSearch(query, requestId)
                    }
                } else {
                    searchRequestId++ // Invalidate any in-flight search's late callback
                    binding.rvSearchResults.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etDestinationLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                binding.btnClearDestination.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                if (query == endAddress) return

                isSearchingSource = false
                searchJob?.cancel()
                if (query.length >= 2) {
                    val requestId = ++searchRequestId
                    searchJob = lifecycleScope.launch {
                        delay(600)
                        performSearch(query, requestId)
                    }
                } else {
                    searchRequestId++ // Invalidate any in-flight search's late callback
                    binding.rvSearchResults.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSource.setOnClickListener {
            searchRequestId++ // Invalidate any in-flight search
            searchJob?.cancel()
            binding.etSourceLocation.text.clear()
            startAddress = ""
            sourcePoint = null
            sourceMarker?.let { pointAnnotationManager?.delete(it) }
            sourceMarker = null
            binding.rvSearchResults.visibility = View.GONE
            calculateRoute()
        }

        binding.btnClearDestination.setOnClickListener {
            searchRequestId++ // Invalidate any in-flight search
            searchJob?.cancel()
            binding.etDestinationLocation.text.clear()
            endAddress = ""
            destinationPoint = null
            destinationMarker?.let { pointAnnotationManager?.delete(it) }
            destinationMarker = null
            binding.rvSearchResults.visibility = View.GONE
            calculateRoute()
        }
    }

    private fun performSearch(query: String, requestId: Long) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return

        lifecycleScope.launch {
            try {
                // 1. Check Firestore Locations first (Case-insensitive Improved Search)
                val firestoreResults = LocationRepository.searchLocations(cleanQuery)

                // Stale check
                if (requestId != searchRequestId) return@launch

                if (firestoreResults.isNotEmpty()) {
                    // Requirement: If Firestore match found, DO NOT call Mapbox
                    runOnUiThread {
                        if (requestId != searchRequestId) return@runOnUiThread
                        searchAdapter.updateResults(firestoreResults)
                        binding.rvSearchResults.visibility = View.VISIBLE
                    }
                } else {
                    // 2. Search the local road data first. Mapbox's autocomplete can
                    // return a similarly spelled place (e.g. Jamalabad for Kamalabad)
                    // instead of the exact Rawalpindi locality, while this lookup
                    // returns all exact local matches with English display labels.
                    val localRoadResults = withContext(Dispatchers.IO) {
                        findOpenStreetMapLocations(cleanQuery)
                    }
                    if (requestId != searchRequestId) return@launch
                    if (localRoadResults.isNotEmpty()) {
                        searchAdapter.updateResults(localRoadResults)
                        binding.rvSearchResults.visibility = View.VISIBLE
                        return@launch
                    }

                    // 3. Mapbox remains available for places not present in the
                    // local-road data.
                    val mapCenter = mapView?.mapboxMap?.cameraState?.center ?: Point.fromLngLat(73.0679, 33.6007)
                    val searchOptions = SearchOptions(
                        proximity = mapCenter,
                        countries = listOf(IsoCountryCode.PAKISTAN),
                        limit = 10,
                        types = listOf(QueryType.ADDRESS, QueryType.POI, QueryType.NEIGHBORHOOD, QueryType.PLACE, QueryType.LOCALITY)
                    )

                    searchEngine.search(cleanQuery, searchOptions, object : SearchSuggestionsCallback {
                        override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                            if (requestId != searchRequestId) return

                            runOnUiThread {
                                if (requestId != searchRequestId) return@runOnUiThread
                                if (suggestions.isNotEmpty()) {
                                    searchAdapter.updateResults(suggestions)
                                    binding.rvSearchResults.visibility = View.VISIBLE
                                } else {
                                    showRouteGeocoderFallback(cleanQuery, requestId)
                                }
                            }
                        }

                        override fun onError(e: Exception) {
                            if (requestId != searchRequestId) return
                            showRouteGeocoderFallback(cleanQuery, requestId)
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e("SearchDebug", "Search error: ${e.message}")
            }
        }
    }

    /** Shows a directly resolved road/area when the SDK autocomplete index has none. */
    private fun showRouteGeocoderFallback(query: String, requestId: Long) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { findRouteLocation(query) }
            if (requestId != searchRequestId) return@launch
            if (result == null) {
                binding.rvSearchResults.visibility = View.GONE
                return@launch
            }
            val point = result.first
            val resolvedLocation = LocationModel(
                id = "route-geocoder:${point.longitude()},${point.latitude()}",
                name = result.second,
                latitude = point.latitude(),
                longitude = point.longitude(),
                city = "Rawalpindi"
            )
            searchAdapter.updateResults(listOf(resolvedLocation))
            binding.rvSearchResults.visibility = View.VISIBLE
        }
    }

    private fun normalize(text: String): String {
        return text.lowercase(java.util.Locale.ROOT)
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    private fun handleSearchResult(point: Point, name: String) {
        runOnUiThread {
            try {
                // Selection ho chuki hai - koi bhi abhi bhi background mein chal raha purana
                // search result ab stale hai aur usse UI update nahi karni.
                searchRequestId++
                searchJob?.cancel()

                // Smooth Fly-to Animation
                val cameraOptions = CameraOptions.Builder()
                    .center(point)
                    .zoom(15.0)
                    .build()

                mapView?.mapboxMap?.flyTo(
                    cameraOptions,
                    MapAnimationOptions.mapAnimationOptions { duration(1500) }
                )

                if (isSearchingSource) {
                    setSource(point, name)
                } else {
                    setDestination(point, name)
                }

                binding.rvSearchResults.visibility = View.GONE

                Toast.makeText(this@DrawRouteActivity, "Set: $name", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e("DrawRoute", "Failed to apply selected location '$name' to map/route: ${e.message}", e)
                // TEMP DEBUG
                Toast.makeText(this@DrawRouteActivity, "DEBUG2: ${e.javaClass.simpleName}: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun calculateRoute() {
        val start = sourcePoint
        val end = destinationPoint

        if (start == null || end == null) {
            roadPathPoints.clear()
            polylineAnnotationManager?.deleteAll()
            return
        }

        val client = MapboxDirections.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .routeOptions(RouteOptions.builder()
                .coordinatesList(listOf(start, end))
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .geometries(DirectionsCriteria.GEOMETRY_POLYLINE6)
                .build())
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                val route = response.body()?.routes()?.firstOrNull()
                if (route != null) {
                    val geometry = route.geometry()
                    if (geometry != null) {
                        val lineString = LineString.fromPolyline(geometry, 6)
                        roadPathPoints.clear()
                        roadPathPoints.addAll(lineString.coordinates())
                        runOnUiThread { updateMapUI() }
                    }
                }
            }
            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("DrawRoute", "Route calculation failed: ${t.message}")
            }
        })
    }

    private fun updateAddressFromPoint(point: Point, isStart: Boolean) {
        val options = ReverseGeoOptions(center = point, limit = 1)
        searchEngine.search(options, object : SearchCallback {
            override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                val name = results.firstOrNull()?.name ?: results.firstOrNull()?.address?.formattedAddress() ?: "Mapped Point"
                runOnUiThread {
                    if (isStart) {
                        startAddress = name
                        binding.etSourceLocation.setText(name)
                    } else {
                        endAddress = name
                        binding.etDestinationLocation.setText(name)
                    }
                    Log.d("DrawRoute", "Updated ${if (isStart) "Start" else "End"} Address: $name")
                }
            }
            override fun onError(e: Exception) {}
        })
    }

    private fun handleIncomingManualPoints() {
        val manualStart = intent.getStringExtra("MANUAL_START")
        val manualEnd = intent.getStringExtra("MANUAL_END")

        if (!manualStart.isNullOrBlank()) geocodeAndSetPoint(manualStart, true)
        if (!manualEnd.isNullOrBlank()) geocodeAndSetPoint(manualEnd, false)
    }

    private fun geocodeAndSetPoint(locationName: String, isStart: Boolean) {
        lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { findRouteLocation(locationName) }
            if (result == null) {
                Toast.makeText(
                    this@DrawRouteActivity,
                    "Couldn't find ${if (isStart) "source" else "destination"}: $locationName",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }

            if (isStart) setSource(result.first, result.second)
            else setDestination(result.first, result.second)
        }
    }

    /**
     * The location picker uses Mapbox's HTTP geocoder with a bounded local search.
     * Draw Route previously used a separate SDK autocomplete request here; that request
     * can return no suggestion for a valid typed address, leaving the route with no
     * source/destination point. Use the same network path and Android fallback instead.
     */
    private fun findRouteLocation(query: String): Pair<Point, String>? {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return null

        try {
            val encodedQuery = URLEncoder.encode(cleanQuery, "UTF-8")
            val token = getString(R.string.mapbox_access_token)
            val urlString = "https://api.mapbox.com/search/geocode/v6/forward" +
                    "?q=$encodedQuery" +
                    "&access_token=$token" +
                    "&bbox=$RAWALPINDI_BBOX" +
                    "&proximity=73.0679,33.6007" +
                    "&country=pk" +
                    "&types=address,street,place,locality,neighborhood,district" +
                    "&autocomplete=true&language=en&limit=1"
            val connection = URL(urlString).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000

            if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                val feature = JSONObject(response).optJSONArray("features")?.optJSONObject(0)
                val coordinates = feature?.optJSONObject("geometry")?.optJSONArray("coordinates")
                if (coordinates != null && coordinates.length() >= 2) {
                    val lng = coordinates.optDouble(0, Double.NaN)
                    val lat = coordinates.optDouble(1, Double.NaN)
                    if (lng.isFinite() && lat.isFinite()) {
                        val properties = feature.optJSONObject("properties")
                        val name = properties?.optString("name").orEmpty().ifBlank { feature.optString("name") }
                        val fullAddress = properties?.optString("full_address").orEmpty()
                            .ifBlank { properties?.optString("place_formatted").orEmpty() }
                            .ifBlank { name }
                            .ifBlank { cleanQuery }
                        connection.disconnect()
                        return Point.fromLngLat(lng, lat) to fullAddress
                    }
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.w("DrawRoute", "Mapbox geocoding failed for '$cleanQuery'", e)
        }

        findOpenStreetMapLocations(cleanQuery).firstOrNull()?.let { location ->
            return Point.fromLngLat(location.longitude, location.latitude) to location.name
        }

        // A valid local landmark can be absent from Mapbox's index. This is the same
        // last-resort geocoder used by the location picker for those cases.
        if (!Geocoder.isPresent()) return null
        return try {
            @Suppress("DEPRECATION")
            val address = Geocoder(this, Locale("en", "PK"))
                .getFromLocationName(cleanQuery, 1, 33.35, 72.70, 33.90, 73.35)
                ?.firstOrNull() ?: return null
            val fullAddress = (0..address.maxAddressLineIndex)
                .mapNotNull { address.getAddressLine(it) }
                .joinToString(", ")
                .ifBlank { cleanQuery }
            Point.fromLngLat(address.longitude, address.latitude) to fullAddress
        } catch (e: Exception) {
            Log.w("DrawRoute", "Android geocoder fallback failed for '$cleanQuery'", e)
            null
        }
    }

    /**
     * Mapbox does not currently index some local Rawalpindi roads (for example,
     * Girja Road). OpenStreetMap contains those roads, so use it only after Mapbox
     * returns no coordinate. This function is called at most once per debounced query.
     */
    private fun findOpenStreetMapLocations(query: String): List<LocationModel> {
        return try {
            // Treat the common "... abad" / "... a bad" typing variants as the
            // canonical locality spelling.  This fixes inputs such as "Gulshan abad",
            // "Kamala bad", and "Kamalaabad" without breaking ordinary multi-word
            // names such as "Bahria Town".
            val canonicalQuery = canonicalizeLocalityQuery(query)
            val placeQuery = if (canonicalQuery.contains("rawalpindi", ignoreCase = true)) {
                "$canonicalQuery, Pakistan"
            } else {
                "$canonicalQuery, Rawalpindi, Pakistan"
            }
            val url = URL(
                "https://nominatim.openstreetmap.org/search?q=" +
                        URLEncoder.encode(placeQuery, "UTF-8") +
                        "&format=jsonv2&limit=5&countrycodes=pk&accept-language=en"
            )
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "BusTrackApp/1.0 (Android route location search)")
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                connection.disconnect()
                return emptyList()
            }
            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            connection.disconnect()
            val results = JSONArray(response)
            buildList {
                for (index in 0 until results.length()) {
                    val item = results.optJSONObject(index) ?: continue
                    val lat = item.optString("lat").toDoubleOrNull()
                    val lng = item.optString("lon").toDoubleOrNull()
                    if (lat == null || lng == null || !lat.isFinite() || !lng.isFinite()) continue

                    // Nominatim's English display_name puts the actual road/area
                    // first. Keep only that short label as the main row title; the
                    // remaining English locality details appear in the smaller row.
                    val displayName = item.optString("display_name").ifBlank { query }
                    val title = displayName.substringBefore(",").ifBlank { query }
                    val subtitle = shortEnglishAddress(displayName)
                    add(
                        LocationModel(
                            id = "osm:$lng,$lat",
                            name = title,
                            latitude = lat,
                            longitude = lng,
                            city = subtitle
                        )
                    )
                }
            // A locality can have several OSM boundaries/nodes. One clean suggestion
            // is more helpful than identical names repeated with different coordinates.
            }.distinctBy { it.name.lowercase(Locale.ROOT) }
        } catch (e: Exception) {
            Log.w("DrawRoute", "OpenStreetMap fallback failed for '$query'", e)
            emptyList()
        }
    }

    private fun shortEnglishAddress(displayName: String): String {
        val parts = displayName.split(",").map { it.trim() }.filter { it.isNotBlank() }
        val localityParts = parts.drop(1)
        val rawalpindiIndex = localityParts.indexOfFirst { it.equals("Rawalpindi", ignoreCase = true) }
        val concise = when {
            rawalpindiIndex == 0 -> listOf("Rawalpindi")
            rawalpindiIndex > 0 -> (localityParts.take(1) + "Rawalpindi").distinct()
            else -> localityParts.take(1)
        }
        return (concise + "Punjab" + "Pakistan").distinct().joinToString(", ")
            .ifBlank { "Rawalpindi, Pakistan" }
    }

    private fun canonicalizeLocalityQuery(query: String): String {
        val trimmed = query.trim()
        val needsAbadCorrection = Regex("(?i)(\\s+abad|a\\s+bad|aabad)").containsMatchIn(trimmed)
        if (!needsAbadCorrection) return trimmed
        return trimmed
            .replace(Regex("\\s+abad", RegexOption.IGNORE_CASE), "abad")
            .replace(Regex("a\\s+bad", RegexOption.IGNORE_CASE), "abad")
            .replace(Regex("aabad", RegexOption.IGNORE_CASE), "abad")
    }

    private fun clearPath() {
        sourcePoint = null
        destinationPoint = null
        roadPathPoints.clear()
        startAddress = ""
        endAddress = ""

        pointAnnotationManager?.deleteAll()
        sourceMarker = null
        destinationMarker = null

        binding.etSourceLocation.text.clear()
        binding.etDestinationLocation.text.clear()

        updateMapUI()
    }

    private fun finishWithResult() {
        val resultIntent = Intent()
        val latLngList = ArrayList<LatLngModel>()

        // Return the road-matched path if available, otherwise just start and end points
        val finalPoints = roadPathPoints.ifEmpty {
            listOfNotNull(sourcePoint, destinationPoint)
        }

        finalPoints.forEach { latLngList.add(LatLngModel(it.latitude(), it.longitude())) }

        resultIntent.putExtra("PATH_POINTS", latLngList)
        resultIntent.putExtra("START_ADDRESS", startAddress)
        resultIntent.putExtra("END_ADDRESS", endAddress)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun updateMapUI() {
        polylineAnnotationManager?.deleteAll()

        val pathToDraw = roadPathPoints
        if (pathToDraw.size > 1) {
            val polylineOptions = PolylineAnnotationOptions()
                .withPoints(pathToDraw)
                .withLineColor("#1565C0")
                .withLineWidth(6.0)
                .withLineOpacity(0.8)
            polylineAnnotationManager?.create(polylineOptions)
        }
    }

    override fun onStart() { super.onStart() }
    override fun onStop() { super.onStop() }
    override fun onDestroy() { super.onDestroy() }
}

class SearchAdapter(
    private var results: List<Any>,
    private val onClick: (Any) -> Unit
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    fun updateResults(newResults: List<Any>) {
        results = newResults
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvResultName)
        val tvAddress: TextView = view.findViewById(R.id.tvResultAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = results[position]
        if (item is SearchSuggestion) {
            holder.tvName.text = item.name
            holder.tvAddress.text = item.fullAddress ?: ""
        } else if (item is LocationModel) {
            holder.tvName.text = item.name
            holder.tvAddress.text = item.city
        }
        holder.itemView.setOnClickListener { onClick(item) }
    }

    override fun getItemCount() = results.size
}
