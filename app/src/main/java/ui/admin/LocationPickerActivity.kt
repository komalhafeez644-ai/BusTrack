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
import android.view.inputmethod.InputMethodManager
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
import com.mapbox.search.*
import com.mapbox.search.common.IsoCountryCode
import com.mapbox.search.result.SearchResult
import com.mapbox.search.result.SearchSuggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import utils.ViewUtils
import java.util.Locale

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationPickerBinding
    private var mapView: MapView? = null
    private var pointAnnotationManager: PointAnnotationManager? = null
    private var selectedPoint: Point? = null
    private var selectedAddress: String = ""
    private val bitmapCache = mutableMapOf<Int, Bitmap>()
    
    private lateinit var searchEngine: SearchEngine
    private var searchJob: Job? = null
    private lateinit var searchAdapter: SearchResultAdapter
    private var lastSearchQuery: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        searchEngine = SearchEngine.createSearchEngineWithBuiltInDataProviders(
            SearchEngineSettings()
        )

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
            if (item is SearchSuggestion) {
                searchEngine.select(item, object : SearchSelectionCallback {
                    override fun onResult(suggestion: SearchSuggestion, result: SearchResult, responseInfo: ResponseInfo) {
                        handleSelection(result.coordinate, result.name)
                    }
                    override fun onResults(suggestion: SearchSuggestion, results: List<SearchResult>, responseInfo: ResponseInfo) {}
                    override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {}
                    override fun onError(e: Exception) {
                        runOnUiThread { Toast.makeText(this@LocationPickerActivity, "Error selecting location", Toast.LENGTH_SHORT).show() }
                    }
                })
            } else if (item is LocationModel) {
                handleSelection(Point.fromLngLat(item.longitude, item.latitude), item.name)
            }
        }
        binding.rvSearchResults.layoutManager = LinearLayoutManager(this)
        binding.rvSearchResults.adapter = searchAdapter
    }

    private fun handleSelection(point: Point, name: String) {
        runOnUiThread {
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
            binding.etSearchLocation.setText(name)
            binding.rvSearchResults.visibility = View.GONE
            binding.searchProgress.visibility = View.GONE
            binding.btnClearSearch.visibility = View.VISIBLE
            binding.etSearchLocation.clearFocus()
            
            // Hide keyboard
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.etSearchLocation.windowToken, 0)
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
                
                searchJob?.cancel()
                if (query.length >= 2) {
                    binding.searchProgress.visibility = View.VISIBLE
                    binding.btnClearSearch.visibility = View.GONE // Hide clear while searching
                    searchJob = lifecycleScope.launch {
                        delay(600)
                        performSearch(query)
                    }
                } else {
                    binding.searchProgress.visibility = View.GONE
                    binding.rvSearchResults.visibility = View.GONE
                }
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchLocation.text.clear()
            binding.rvSearchResults.visibility = View.GONE
            binding.searchProgress.visibility = View.GONE
            searchJob?.cancel()
        }

        binding.etSearchLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchLocation.text.toString().trim()
                if (query.isNotEmpty()) {
                    binding.searchProgress.visibility = View.VISIBLE
                    binding.btnClearSearch.visibility = View.GONE
                    performSearch(query)
                }
                true
            } else false
        }
    }

    private fun performSearch(query: String) {
        val cleanQuery = query.trim()
        lastSearchQuery = cleanQuery
        lifecycleScope.launch {
            try {
                // 1. Repository Search (Improved Firestore Global Locations Search)
                val firestoreResults = LocationRepository.searchLocations(cleanQuery)

                if (cleanQuery != lastSearchQuery) return@launch

                if (firestoreResults.isNotEmpty()) {
                    // Requirement: If Firestore match found, show it and skip Mapbox
                    runOnUiThread {
                        binding.searchProgress.visibility = View.GONE
                        binding.btnClearSearch.visibility = if (binding.etSearchLocation.text.isEmpty()) View.GONE else View.VISIBLE
                        searchAdapter.setResults(firestoreResults)
                        binding.rvSearchResults.visibility = View.VISIBLE
                    }
                } else {
                    // 2. Fallback to Mapbox Search API ONLY if no Firestore results found
                    val mapCenter = mapView?.mapboxMap?.cameraState?.center ?: Point.fromLngLat(73.0679, 33.6007)
                    val searchOptions = SearchOptions(
                        proximity = mapCenter,
                        countries = listOf(IsoCountryCode.PAKISTAN),
                        limit = 10,
                        types = listOf(QueryType.ADDRESS, QueryType.POI, QueryType.NEIGHBORHOOD, QueryType.PLACE, QueryType.LOCALITY, QueryType.DISTRICT)
                    )

                    searchEngine.search(cleanQuery, searchOptions, object : SearchSuggestionsCallback {
                        override fun onSuggestions(suggestions: List<SearchSuggestion>, responseInfo: ResponseInfo) {
                            if (cleanQuery != lastSearchQuery) return
                            
                            runOnUiThread {
                                binding.searchProgress.visibility = View.GONE
                                binding.btnClearSearch.visibility = if (binding.etSearchLocation.text.isEmpty()) View.GONE else View.VISIBLE
                                if (suggestions.isNotEmpty()) {
                                    searchAdapter.setResults(suggestions)
                                    binding.rvSearchResults.visibility = View.VISIBLE
                                } else {
                                    binding.rvSearchResults.visibility = View.GONE
                                }
                            }
                        }
                        override fun onError(e: Exception) {
                            if (cleanQuery != lastSearchQuery) return
                            runOnUiThread {
                                binding.searchProgress.visibility = View.GONE
                                binding.btnClearSearch.visibility = if (binding.etSearchLocation.text.isEmpty()) View.GONE else View.VISIBLE
                                binding.rvSearchResults.visibility = View.GONE
                            }
                        }
                    })
                }
            } catch (e: Exception) {
                Log.e("SearchDebug", "Search error: ${e.message}")
            }
        }
    }

    private fun reverseGeocode(point: Point) {
        val options = ReverseGeoOptions(center = point, limit = 1)
        searchEngine.search(options, object : SearchCallback {
            override fun onResults(results: List<SearchResult>, responseInfo: ResponseInfo) {
                runOnUiThread {
                    selectedAddress = results.firstOrNull()?.address?.formattedAddress() ?: "Selected Point"
                    binding.tvSelectedAddress.text = selectedAddress
                }
            }
            override fun onError(e: Exception) {}
        })
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
            if (item is SearchSuggestion) {
                holder.tvName.text = item.name
                holder.tvAddress.text = item.fullAddress ?: ""
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