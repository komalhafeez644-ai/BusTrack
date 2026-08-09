package ui.admin

import android.content.Intent
import android.graphics.Bitmap
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
import com.mapbox.maps.plugin.gestures.addOnMapClickListener
import com.mapbox.maps.plugin.locationcomponent.location
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Locale

class DrawRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawRouteBinding
    private var mapView: MapView? = null
    private val tapPoints = mutableListOf<Point>()
    private val roadPathPoints = mutableListOf<Point>()
    
    private var circleAnnotationManager: CircleAnnotationManager? = null
    private var polylineAnnotationManager: PolylineAnnotationManager? = null
    private var searchMarkerManager: PointAnnotationManager? = null
    
    private var startAddress: String = ""
    private var endAddress: String = ""
    private var searchJob: Job? = null
    private lateinit var searchEngine: SearchEngine
    
    private lateinit var searchAdapter: SearchAdapter
    private val routeCache = mutableMapOf<String, String>()
    
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
            // Add point to route
            tapPoints.add(point)
            roadPathPoints.clear() 
            updateMapUI()
            searchMarkerManager?.deleteAll()
            
            // Try to get address for this point immediately
            updateAddressFromPoint(point, isStart = tapPoints.size == 1)
            true
        }

        setupRecyclerView()
        setupSearch()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClearPath.setOnClickListener { clearPath() }
        
        binding.btnDone.setOnClickListener {
            if (tapPoints.size < 2) {
                Toast.makeText(this, "Please select at least 2 points", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fetchRoadMatchedPath(onComplete = {
                // Final address check before finishing
                fetchAddressesAndFinish()
            })
        }
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
                handleSearchResult(Point.fromLngLat(item.longitude, item.latitude), item.name)
            }
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter
    }

    private fun enableUserLocation() {
        mapView?.location?.run {
            enabled = true
            pulsingEnabled = true
        }
    }

    private fun initAnnotationManagers() {
        val annotationApi = mapView?.annotations ?: return
        circleAnnotationManager = annotationApi.createCircleAnnotationManager()
        polylineAnnotationManager = annotationApi.createPolylineAnnotationManager()
        searchMarkerManager = annotationApi.createPointAnnotationManager()
    }

    private fun setupInitialCamera() {
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(Point.fromLngLat(73.0535, 33.5985)) // FG College
                .zoom(14.0)
                .build()
        )
    }

    private fun setupSearch() {
        binding.etSearchLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().trim()
                binding.btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                
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
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchLocation.text.clear()
            binding.rvSearchResults.visibility = View.GONE
            searchMarkerManager?.deleteAll()
        }
    }

    private fun performSearch(query: String) {
        val cleanQuery = query.trim()
        lifecycleScope.launch {
            try {
                val firestoreResults = mutableListOf<LocationModel>()
                val variants = listOf(
                    cleanQuery, 
                    cleanQuery.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() },
                    cleanQuery.uppercase()
                ).distinct()

                for (v in variants) {
                    firestoreResults.addAll(LocationRepository.searchLocations(v))
                }
                val finalCustomResults = firestoreResults.distinctBy { it.id }

                val mapCenter = mapView?.mapboxMap?.cameraState?.center ?: Point.fromLngLat(73.0679, 33.6007)
                val searchOptions = SearchOptions(
                    proximity = mapCenter,
                    countries = listOf(IsoCountryCode.PAKISTAN),
                    limit = 10,
                    types = listOf(QueryType.ADDRESS, QueryType.POI, QueryType.NEIGHBORHOOD, QueryType.PLACE, QueryType.LOCALITY)
                )

                searchEngine.search(cleanQuery, searchOptions, object : SearchSuggestionsCallback {
                    override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                        val combinedResults = mutableListOf<Any>()
                        combinedResults.addAll(finalCustomResults)
                        combinedResults.addAll(suggestions)
                        
                        runOnUiThread {
                            if (combinedResults.isNotEmpty()) {
                                searchAdapter.updateResults(combinedResults)
                                binding.rvSearchResults.visibility = View.VISIBLE
                            } else {
                                binding.rvSearchResults.visibility = View.GONE
                            }
                        }
                    }

                    override fun onError(e: Exception) {
                        runOnUiThread {
                            if (finalCustomResults.isNotEmpty()) {
                                searchAdapter.updateResults(finalCustomResults)
                                binding.rvSearchResults.visibility = View.VISIBLE
                            } else {
                                binding.rvSearchResults.visibility = View.GONE
                            }
                        }
                    }
                })
            } catch (e: Exception) {
                Log.e("SearchDebug", "Search error: ${e.message}")
            }
        }
    }

    private fun handleSearchResult(point: Point, name: String) {
        runOnUiThread {
            // Smooth Fly-to Animation
            val cameraOptions = CameraOptions.Builder()
                .center(point)
                .zoom(15.0)
                .build()

            mapView?.mapboxMap?.flyTo(
                cameraOptions,
                MapAnimationOptions.mapAnimationOptions { duration(1500) }
            )

            // Add point to route
            tapPoints.add(point)
            updateMapUI()
            
            // Set addresses based on position
            if (tapPoints.size == 1) {
                startAddress = name
            } else {
                endAddress = name
            }

            searchMarkerManager?.deleteAll()
            binding.rvSearchResults.visibility = View.GONE
            binding.etSearchLocation.setText(name)
            binding.etSearchLocation.clearFocus()
            
            Toast.makeText(this@DrawRouteActivity, "Added: $name", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAddressFromPoint(point: Point, isStart: Boolean) {
        val options = ReverseGeoOptions(center = point, limit = 1)
        searchEngine.search(options, object : SearchCallback {
            override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                val name = results.firstOrNull()?.name ?: results.firstOrNull()?.address?.formattedAddress() ?: "Mapped Point"
                runOnUiThread {
                    if (isStart) {
                        startAddress = name
                    } else {
                        endAddress = name
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
        
        lifecycleScope.launch {
            if (!manualStart.isNullOrEmpty()) {
                geocodeAndAddPoint(manualStart)
                startAddress = manualStart
            }
            if (!manualEnd.isNullOrEmpty()) {
                geocodeAndAddPoint(manualEnd)
                endAddress = manualEnd
            }
        }
    }

    private fun geocodeAndAddPoint(locationName: String) {
        val searchOptions = SearchOptions(
            proximity = Point.fromLngLat(73.0679, 33.6007),
            countries = listOf(IsoCountryCode.PAKISTAN),
            limit = 1
        )

        searchEngine.search(locationName, searchOptions, object : SearchSuggestionsCallback {
            override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                suggestions.firstOrNull()?.let { suggestion ->
                    searchEngine.select(suggestion, object : SearchSelectionCallback {
                        override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                            runOnUiThread {
                                tapPoints.add(result.coordinate)
                                updateMapUI()
                            }
                        }
                        override fun onResults(suggestion: SearchSuggestion, results: List<SearchResult>, responseInfo: ResponseInfo) {}
                        override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {}
                        override fun onError(e: Exception) {}
                    })
                }
            }
            override fun onError(e: Exception) {}
        })
    }

    private fun clearPath() {
        tapPoints.clear()
        roadPathPoints.clear()
        startAddress = ""
        endAddress = ""
        updateMapUI()
        searchMarkerManager?.deleteAll()
    }

    private fun fetchAddressesAndFinish() {
        if (tapPoints.isEmpty()) return
        
        // Final sanity check for addresses
        if (startAddress.isNotEmpty() && endAddress.isNotEmpty() && startAddress != "Mapped Point") {
            finishWithResult()
            return
        }

        val startPoint = tapPoints.first()
        val endPoint = tapPoints.last()

        val options = ReverseGeoOptions(center = startPoint, limit = 1)
        searchEngine.search(options, object : SearchCallback {
            override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                if (startAddress.isEmpty() || startAddress == "Mapped Point") {
                    startAddress = results.firstOrNull()?.name ?: results.firstOrNull()?.address?.formattedAddress() ?: "Start Point"
                }
                fetchEndAddressAndFinish(endPoint)
            }
            override fun onError(e: Exception) { fetchEndAddressAndFinish(endPoint) }
        })
    }

    private fun fetchEndAddressAndFinish(endPoint: Point) {
        val options = ReverseGeoOptions(center = endPoint, limit = 1)
        searchEngine.search(options, object : SearchCallback {
            override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                if (endAddress.isEmpty() || endAddress == "Mapped Point") {
                    endAddress = results.firstOrNull()?.name ?: results.firstOrNull()?.address?.formattedAddress() ?: "End Point"
                }
                finishWithResult()
            }
            override fun onError(e: Exception) { finishWithResult() }
        })
    }

    private fun finishWithResult() {
        val resultIntent = Intent()
        val latLngList = ArrayList<LatLngModel>()
        val finalPoints = roadPathPoints.ifEmpty { tapPoints }
        finalPoints.forEach { latLngList.add(LatLngModel(it.latitude(), it.longitude())) }
        
        resultIntent.putExtra("PATH_POINTS", latLngList)
        resultIntent.putExtra("START_ADDRESS", startAddress)
        resultIntent.putExtra("END_ADDRESS", endAddress)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    private fun fetchRoadMatchedPath(onComplete: () -> Unit) {
        val cacheKey = tapPoints.toString()
        if (routeCache.containsKey(cacheKey)) {
            val cachedGeometry = routeCache[cacheKey]!!
            val lineString = LineString.fromPolyline(cachedGeometry, 6)
            roadPathPoints.clear()
            roadPathPoints.addAll(lineString.coordinates())
            onComplete()
            return
        }

        Toast.makeText(this, "Finalizing road path...", Toast.LENGTH_SHORT).show()
        val client = MapboxDirections.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .routeOptions(RouteOptions.builder()
                .coordinatesList(tapPoints)
                .profile(DirectionsCriteria.PROFILE_DRIVING)
                .overview(DirectionsCriteria.OVERVIEW_FULL)
                .build())
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                response.body()?.routes()?.firstOrNull()?.geometry()?.let {
                    routeCache[cacheKey] = it 
                    val lineString = LineString.fromPolyline(it, 6)
                    roadPathPoints.clear()
                    roadPathPoints.addAll(lineString.coordinates())
                    updateMapUI()
                    onComplete()
                } ?: onComplete()
            }
            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) { onComplete() }
        })
    }

    private fun updateMapUI() {
        circleAnnotationManager?.deleteAll()
        polylineAnnotationManager?.deleteAll()

        tapPoints.forEach { point ->
            val circleOptions = CircleAnnotationOptions()
                .withPoint(point)
                .withCircleRadius(6.0)
                .withCircleColor("#1565C0")
                .withCircleStrokeWidth(2.0)
                .withCircleStrokeColor("#ffffff")
            circleAnnotationManager?.create(circleOptions)
        }

        val pathToDraw = roadPathPoints.ifEmpty { tapPoints }
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
