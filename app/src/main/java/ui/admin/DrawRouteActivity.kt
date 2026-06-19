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
import androidx.recyclerview.widget.RecyclerView
import androidx.core.graphics.drawable.toBitmap
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityDrawRouteBinding
import com.example.bustrack_app.models.LatLngModel
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.CarmenFeature
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
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
    private var currentGeocodingCall: MapboxGeocoding? = null
    
    private val routeCache = mutableMapOf<String, String>()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupInitialCamera()
            initAnnotationManagers()
            enableUserLocation()
            handleIncomingManualPoints()
        }

        mapView?.mapboxMap?.addOnMapClickListener { point ->
            tapPoints.add(point)
            roadPathPoints.clear() 
            updateMapUI()
            searchMarkerManager?.deleteAll()
            true
        }

        setupSearch()

        binding.btnBack.setOnClickListener { finish() }
        binding.btnClearPath.setOnClickListener { clearPath() }
        
        binding.btnDone.setOnClickListener {
            if (tapPoints.size < 2) {
                Toast.makeText(this, "Please select at least 2 points", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            fetchRoadMatchedPath(onComplete = {
                fetchAddressesAndFinish()
            })
        }
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
                Log.d("SearchDebug", "onTextChanged: $query")
                binding.btnClearSearch.visibility = if (query.isEmpty()) View.GONE else View.VISIBLE
                
                searchJob?.cancel()
                if (query.length >= 2) { 
                    searchJob = lifecycleScope.launch {
                        delay(500) 
                        performSearch(query)
                    }
                } else {
                    binding.rvSearchResults.visibility = View.GONE
                    currentGeocodingCall?.cancelCall()
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
        val cleanQuery = query
            .replace("more", "mor", ignoreCase = true)
            .replace("morr", "mor", ignoreCase = true)

        Log.d("SearchDebug", "Searching for: $cleanQuery")
        currentGeocodingCall?.cancelCall()

        val geocoding = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .query(cleanQuery)
            .country("pk") 
            .autocomplete(true)
            .fuzzyMatch(true)
            .limit(10)
            .build()

        currentGeocodingCall = geocoding
        geocoding.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                Log.d("SearchDebug", "Response Code: ${response.code()}")
                if (response.isSuccessful) {
                    val results = response.body()?.features() ?: emptyList()
                    Log.d("SearchDebug", "Features found: ${results.size}")
                    
                    results.forEach {
                        Log.d("SearchDebug", "Name=${it.text()} Type=${it.placeType()} Address=${it.placeName()}")
                    }
                    
                    runOnUiThread {
                        if (results.isNotEmpty()) {
                            showSearchResults(results)
                        } else {
                            Log.d("SearchDebug", "No results for $cleanQuery")
                            binding.rvSearchResults.visibility = View.GONE
                            if (cleanQuery.length > 3) {
                                Toast.makeText(this@DrawRouteActivity, "No results found. Try another query.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                } else {
                    Log.e("SearchDebug", "Error: ${response.code()} ${response.message()}")
                    runOnUiThread {
                        binding.rvSearchResults.visibility = View.GONE
                    }
                }
            }
            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                if (call.isCanceled) {
                    Log.d("SearchDebug", "Canceled")
                } else {
                    Log.e("SearchDebug", "Failed: ${t.message}")
                    runOnUiThread {
                        binding.rvSearchResults.visibility = View.GONE
                    }
                }
            }
        })
    }

    private fun showSearchResults(results: List<CarmenFeature>) {
        binding.rvSearchResults.visibility = View.VISIBLE
        val adapter = SearchAdapter(results) { feature ->
            val point = feature.center() ?: return@SearchAdapter
            mapView?.mapboxMap?.setCamera(CameraOptions.Builder().center(point).zoom(16.0).build())
            searchMarkerManager?.deleteAll()
            
            val markerIcon = getBitmapFromVectorDrawable(R.drawable.outline_location)
            markerIcon?.let {
                val pointAnnotationOptions = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(it)
                    .withIconColor("#EF4444")
                    .withTextField(feature.text() ?: "")
                    .withTextOffset(listOf(0.0, 2.0))
                    .withIconSize(1.5)
                searchMarkerManager?.create(pointAnnotationOptions)
            }
            binding.rvSearchResults.visibility = View.GONE
            binding.etSearchLocation.setText(feature.text())
            Toast.makeText(this, "Tap map at red marker to add to route", Toast.LENGTH_SHORT).show()
        }
        binding.rvSearchResults.adapter = adapter
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap? {
        return ContextCompat.getDrawable(this, drawableId)?.toBitmap()
    }

    private fun handleIncomingManualPoints() {
        val manualStart = intent.getStringExtra("MANUAL_START")
        val manualEnd = intent.getStringExtra("MANUAL_END")
        if (!manualStart.isNullOrEmpty()) geocodeAndAddPoint(manualStart)
        if (!manualEnd.isNullOrEmpty()) geocodeAndAddPoint(manualEnd)
    }

    private fun geocodeAndAddPoint(locationName: String) {
        val geocoding = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .query(locationName)
            .country("pk")
            .proximity(Point.fromLngLat(73.0535, 33.5985))
            .build()

        geocoding.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                response.body()?.features()?.firstOrNull()?.center()?.let {
                    tapPoints.add(it)
                    updateMapUI()
                }
            }
            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {}
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
        val startPoint = tapPoints.first()
        val endPoint = tapPoints.last()

        val startGeocoding = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .query(Point.fromLngLat(startPoint.longitude(), startPoint.latitude()))
            .country("pk")
            .build()

        startGeocoding.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                startAddress = response.body()?.features()?.firstOrNull()?.placeName() ?: "Start Point"
                fetchEndAddressAndFinish(endPoint)
            }
            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) { fetchEndAddressAndFinish(endPoint) }
        })
    }

    private fun fetchEndAddressAndFinish(endPoint: Point) {
        val endGeocoding = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .query(Point.fromLngLat(endPoint.longitude(), endPoint.latitude()))
            .country("pk")
            .build()

        endGeocoding.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                endAddress = response.body()?.features()?.firstOrNull()?.placeName() ?: "End Point"
                finishWithResult()
            }
            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) { finishWithResult() }
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
    private val results: List<CarmenFeature>,
    private val onClick: (CarmenFeature) -> Unit
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvPlaceName)
        val tvAddress: TextView = view.findViewById(R.id.tvPlaceAddress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_search_result, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val feature = results[position]
        // Smart display: If the text is generic, use a part of placeName
        holder.tvName.text = feature.text() ?: "Unknown Location"
        holder.tvAddress.text = feature.placeName() ?: ""
        holder.itemView.setOnClickListener { onClick(feature) }
    }

    override fun getItemCount() = results.size
}
