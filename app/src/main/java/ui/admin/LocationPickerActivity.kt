package ui.admin

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityLocationPickerBinding
import com.example.bustrack_app.models.LocationModel
import com.example.bustrack_app.data.LocationRepository
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.animation.MapAnimationOptions
import com.mapbox.maps.plugin.animation.flyTo
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import utils.ViewUtils
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * MAJOR FIX (this rewrite): the Mapbox Search SDK (mapbox-search-android) kept causing
 * version-mismatch compile/runtime problems in this project - its SearchEngineSettings
 * constructor signature did not match what the docs for other versions showed, and even
 * after correcting that, a boundingBox query silently returned zero results. Rather than
 * keep guessing at this specific SDK version's exact behavior under exam-day time
 * pressure, this now calls Mapbox's plain Search Box REST API directly over
 * HttpURLConnection - the same simple, dependency-free, version-proof pattern already
 * used for the chatbot in this project (see ChatbotRepository.kt). This removes ALL
 * dependency on the Search SDK's Kotlin API surface, so there is nothing left to
 * mismatch: it's just a URL and a JSON response, which is Mapbox's own documented public
 * API and does not change between SDK versions.
 *
 * FOLLOW-UP FIX: Geocoding v6's /forward and /reverse endpoints no longer return POI
 * data (landmarks, stations, businesses, etc.) - Mapbox removed POIs from the Geocoding
 * API and now only serves them via the separate Search Box API. That's why a query like
 * "railway" matched nothing relevant. Both search and reverse-geocode below now call
 * the Search Box API's /forward and /reverse endpoints instead
 * (https://docs.mapbox.com/api/search/search-box/), which cover addresses, places, AND
 * POIs in one response - same GeoJSON FeatureCollection shape as before, so the parsing
 * code barely changes. auto_complete=true is also set so partial words typed so far
 * (e.g. "railway r") are matched fuzzily instead of requiring a complete token.
 */
class LocationPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationPickerBinding
    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var selectedPoint: Point? = null
    private var selectedAddress: String = ""
    private val bitmapCache = mutableMapOf<Int, Bitmap>()

    private var searchJob: Job? = null
    private lateinit var searchAdapter: SearchResultAdapter

    // Set to true right before we call etSearchLocation.setText(...) ourselves (e.g.
    // after the user picks a result). The TextWatcher checks this flag and skips
    // triggering a new search in that case - otherwise our own setText() call fires
    // the watcher just like real typing would, which re-opens the suggestion list
    // right after the user already picked a location.
    private var isProgrammaticTextChange = false

    data class GeocodeResult(val name: String, val fullAddress: String, val point: Point, val distanceMeters: Double?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            val annotationApi = mapView?.annotations
            pointAnnotationManager = annotationApi?.createPointAnnotationManager()

            setupInitialCamera()
            setupMapListeners()

            // Seed global locations from repository
            lifecycleScope.launch {
                LocationRepository.seedInitialLocations()
            }
        }

        setupRecyclerView()
        setupClickListeners()
        setupSearch()
    }

    private fun setupRecyclerView() {
        searchAdapter = SearchResultAdapter { item ->
            if (item is GeocodeResult) {
                handleSelection(item.point, item.fullAddress.ifBlank { item.name })
            } else if (item is LocationModel) {
                handleSelection(Point.fromLngLat(item.longitude, item.latitude), item.name)
            }
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter
    }

    private fun handleSelection(point: Point, name: String) {
        runOnUiThread {
            // Cancel any debounced search still waiting to fire from the typing that
            // led to this selection - otherwise it can fire moments later and reopen
            // the suggestion list right after we close it below.
            searchJob?.cancel()

            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(16.0)
                .padding(EdgeInsets(0.0, 0.0, 350.0, 0.0))
                .build()

            mapView?.mapboxMap?.flyTo(
                cameraOptions,
                MapAnimationOptions.mapAnimationOptions { duration(2000) }
            )

            placeMarker(point)
            selectedAddress = name
            binding.tvSelectedAddress.text = name

            isProgrammaticTextChange = true
            binding.etSearchLocation.setText(name)
            binding.etSearchLocation.setSelection(name.length)
            isProgrammaticTextChange = false

            binding.rvSearchResults.visibility = View.GONE
            binding.etSearchLocation.clearFocus()
        }
    }

    private fun setupInitialCamera() {
        val initialPoint = Point.fromLngLat(73.0535, 33.5985)
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(initialPoint)
                .zoom(14.0)
                .build()
        )
    }

    private fun setupMapListeners() {
        mapView?.gestures?.apply {
            pinchToZoomEnabled = true
            scrollEnabled = true
        }

        mapView?.mapboxMap?.addOnMapClickListener { point ->
            placeMarker(point)
            reverseGeocode(point)
            true
        }
    }

    private fun placeMarker(point: Point) {
        selectedPoint = point
        pointAnnotationManager?.deleteAll()

        val bitmap = bitmapFromDrawableRes(this, R.drawable.outline_location)
        if (bitmap != null) {
            val pointAnnotationOptions = PointAnnotationOptions()
                .withPoint(point)
                .withIconImage(bitmap)
                .withIconSize(1.6)
                .withIconColor("#DC2626")
                .withIconAnchor(com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor.BOTTOM)

            pointAnnotationManager?.create(pointAnnotationOptions)
        }
    }

    private fun setupSearch() {
        binding.etSearchLocation.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                binding.btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE

                if (isProgrammaticTextChange) {
                    // This change came from our own setText() after a selection, not
                    // from the user typing - don't treat it as a new search query.
                    return
                }

                searchJob?.cancel()
                if (query.length >= 2) {
                    searchJob = lifecycleScope.launch {
                        delay(600)
                        performSearch(query)
                    }
                } else {
                    binding.rvSearchResults.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchLocation.text.clear()
            binding.rvSearchResults.visibility = View.GONE
        }

        binding.etSearchLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchLocation.text.toString().trim()
                if (query.isNotEmpty()) performSearch(query)
                true
            } else false
        }
    }

    private fun performSearch(query: String) {
        val cleanQuery = query.trim()
        lifecycleScope.launch {
            try {
                val variants = listOf(
                    cleanQuery,
                    cleanQuery.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    cleanQuery.uppercase()
                ).distinct()

                // Firestore custom-location lookup and the Mapbox geocoding call both run
                // in parallel (async/awaitAll), same performance fix as before.
                val firestoreDeferred = variants.map { v ->
                    async { LocationRepository.searchLocations(v) }
                }

                val mapCenter = mapView?.mapboxMap?.cameraState?.center ?: Point.fromLngLat(73.0679, 33.6007)
                val geocodeDeferred = async { fetchGeocodingResults(cleanQuery, mapCenter) }

                val finalCustomResults = firestoreDeferred.awaitAll().flatten().distinctBy { it.id }
                val geocodeResults = geocodeDeferred.await()

                val combined = mutableListOf<Any>()
                combined.addAll(finalCustomResults)
                combined.addAll(geocodeResults)
                if (combined.isNotEmpty()) {
                    searchAdapter.setResults(combined)
                    binding.rvSearchResults.visibility = View.VISIBLE
                } else {
                    binding.rvSearchResults.visibility = View.GONE
                }
            } catch (e: Exception) {
                Log.e("SearchDebug", "Search error: ${e.message}", e)
            }
        }
    }

    /**
     * Calls Mapbox's public Search Box "forward" endpoint directly. Documented here:
     * https://docs.mapbox.com/api/search/search-box/#text-search
     * Unlike Geocoding v6, this endpoint includes POI results (landmarks, stations,
     * shops, etc.) alongside addresses and places - which is what "railway" or any
     * other landmark-style query needs. No SDK classes involved at all - just a URL and
     * JSON, so there is no constructor/parameter mismatch possible regardless of which
     * mapbox-search-android version (or none at all) is on the classpath.
     * Note: the one-off /forward endpoint does not require a session_token (that's only
     * needed for the /suggest + /retrieve autocomplete-session pair). auto_complete=true
     * still enables fuzzy/partial-word matching for text typed as-you-go.
     * rank_strategy=distance sorts the returned features by proximity to the map center
     * rather than plain text relevance, so short (1-2 word) queries surface the nearest
     * matches first instead of the "most textually relevant" match countrywide.
     */
    private suspend fun fetchGeocodingResults(query: String, proximity: Point): List<GeocodeResult> =
        withContext(Dispatchers.IO) {
            try {
                val token = getString(R.string.mapbox_access_token)
                val encodedQuery = URLEncoder.encode(query, "UTF-8")
                val urlString = "https://api.mapbox.com/search/searchbox/v1/forward" +
                        "?q=$encodedQuery" +
                        "&access_token=$token" +
                        "&proximity=${proximity.longitude()},${proximity.latitude()}" +
                        "&country=pk" +
                        "&types=poi,address,place,street,locality,neighborhood,district,category" +
                        "&auto_complete=true" +
                        "&rank_strategy=distance" +
                        "&limit=10"

                Log.d("SearchDebug", "Requesting: $urlString")

                val connection = URL(urlString).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 8000
                connection.readTimeout = 8000

                val responseCode = connection.responseCode
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    // Read the error body too - Mapbox puts the real reason (e.g. "Not
                    // Authorized - Invalid Token" or "This endpoint requires a token with
                    // the SEARCH scope") in the error stream, not just the status code.
                    val errorBody = try {
                        BufferedReader(InputStreamReader(connection.errorStream)).use { it.readText() }
                    } catch (inner: Exception) { "<no error body>" }
                    Log.e("SearchDebug", "Geocoding HTTP error: $responseCode, body: $errorBody")
                    connection.disconnect()
                    return@withContext emptyList<GeocodeResult>()
                }

                val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                connection.disconnect()
                Log.d("SearchDebug", "Response: $response")

                val json = JSONObject(response)
                val features = json.optJSONArray("features") ?: return@withContext emptyList<GeocodeResult>()

                val results = mutableListOf<GeocodeResult>()
                for (i in 0 until features.length()) {
                    val feature = features.getJSONObject(i)
                    val geometry = feature.optJSONObject("geometry")
                    val coordinates = geometry?.optJSONArray("coordinates")
                    val properties = feature.optJSONObject("properties")

                    if (coordinates != null && coordinates.length() >= 2 && properties != null) {
                        val lng = coordinates.getDouble(0)
                        val lat = coordinates.getDouble(1)
                        val name = properties.optString("name", properties.optString("name_preferred", ""))

                        // BUG FIX: for "locality"/"place"-type results (like a named
                        // area/mohalla e.g. "Dhok Saiyidan"), Mapbox's own
                        // "full_address" property often only contains the PARENT
                        // region ("Rawalpindi, Punjab, Pakistan") and does not repeat
                        // the feature's own name - unlike a street address, where
                        // full_address = "<address>, <place_formatted>". Using
                        // full_address as-is in that case silently drops the specific
                        // place the user picked and shows only the broad region
                        // everywhere downstream (search bar, bottom card, student
                        // profile, residential address) even though the underlying
                        // coordinates stay correct (which is why route matching still
                        // worked fine). So: only trust full_address if it actually
                        // contains the feature's own name; otherwise build it manually
                        // as "<name>, <place_formatted>" so the specific place is never
                        // lost.
                        val rawFullAddress = properties.optString("full_address", "")
                        val placeFormatted = properties.optString("place_formatted", "")
                        val fullAddress = when {
                            rawFullAddress.isNotBlank() && (name.isBlank() || rawFullAddress.contains(name, ignoreCase = true)) -> rawFullAddress
                            name.isNotBlank() && placeFormatted.isNotBlank() -> "$name, $placeFormatted"
                            name.isNotBlank() -> name
                            placeFormatted.isNotBlank() -> placeFormatted
                            else -> name
                        }

                        val distanceResults = FloatArray(1)
                        android.location.Location.distanceBetween(
                            proximity.latitude(), proximity.longitude(), lat, lng, distanceResults
                        )

                        // Same 60km relevance filter as before, now computed against a
                        // real REST response instead of an SDK-provided distance field.
                        if (distanceResults[0] <= 60000f) {
                            results.add(GeocodeResult(name, fullAddress, Point.fromLngLat(lng, lat), distanceResults[0].toDouble()))
                        }
                    }
                }
                results
            } catch (e: Exception) {
                Log.e("SearchDebug", "Geocoding request failed: ${e.message}", e)
                emptyList()
            }
        }

    private fun reverseGeocode(point: Point) {
        lifecycleScope.launch {
            try {
                val address = withContext(Dispatchers.IO) {
                    val token = getString(R.string.mapbox_access_token)
                    val urlString = "https://api.mapbox.com/search/searchbox/v1/reverse" +
                            "?longitude=${point.longitude()}" +
                            "&latitude=${point.latitude()}" +
                            "&access_token=$token" +
                            "&limit=1"

                    val connection = URL(urlString).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 8000
                    connection.readTimeout = 8000

                    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                        connection.disconnect()
                        return@withContext null
                    }

                    val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
                    connection.disconnect()

                    val json = JSONObject(response)
                    val features = json.optJSONArray("features")
                    if (features != null && features.length() > 0) {
                        val properties = features.getJSONObject(0).optJSONObject("properties")
                        val name = properties?.optString("name", "") ?: ""
                        val rawFullAddress = properties?.optString("full_address", "") ?: ""
                        val placeFormatted = properties?.optString("place_formatted", "") ?: ""
                        // Same fix as the forward-search results: don't let a
                        // locality-level full_address silently drop the specific place
                        // name (see fetchGeocodingResults for the full explanation).
                        when {
                            rawFullAddress.isNotBlank() && (name.isBlank() || rawFullAddress.contains(name, ignoreCase = true)) -> rawFullAddress
                            name.isNotBlank() && placeFormatted.isNotBlank() -> "$name, $placeFormatted"
                            name.isNotBlank() -> name
                            placeFormatted.isNotBlank() -> placeFormatted
                            else -> null
                        }
                    } else null
                }

                selectedAddress = address ?: "Selected Point"
                binding.tvSelectedAddress.text = selectedAddress
            } catch (e: Exception) {
                Log.e("SearchDebug", "Reverse geocoding failed: ${e.message}", e)
                selectedAddress = "Selected Point"
                binding.tvSelectedAddress.text = selectedAddress
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

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { view ->
            ViewUtils.applyClickEffect(view)
            finish()
        }

        binding.btnConfirmLocation.setOnClickListener { view ->
            ViewUtils.applyClickEffect(view)
            if (selectedPoint != null && selectedAddress.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("SELECTED_ADDRESS", selectedAddress)
                resultIntent.putExtra("LATITUDE", selectedPoint?.latitude())
                resultIntent.putExtra("LONGITUDE", selectedPoint?.longitude())
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            } else {
                Toast.makeText(this, "Please mark a location on map first", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bitmapCache.clear()
        mapView?.onDestroy()
    }

    private class SearchResultAdapter(private val onResultClick: (Any) -> Unit) :
        RecyclerView.Adapter<SearchResultAdapter.ViewHolder>() {

        private var results: List<Any> = emptyList()

        fun setResults(newResults: List<Any>) {
            this.results = newResults
            notifyDataSetChanged()
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = results[position]
            if (item is GeocodeResult) {
                holder.tvName.text = item.name
                holder.tvAddress.text = item.fullAddress
            } else if (item is LocationModel) {
                holder.tvName.text = item.name
                holder.tvAddress.text = item.city
            }
            holder.itemView.setOnClickListener { onResultClick(item) }
        }

        override fun getItemCount(): Int = results.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvName: TextView = view.findViewById(R.id.tvResultName)
            val tvAddress: TextView = view.findViewById(R.id.tvResultAddress)
        }
    }
}