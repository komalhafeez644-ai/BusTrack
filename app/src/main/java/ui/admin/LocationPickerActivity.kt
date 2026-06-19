package ui.admin

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityLocationPickerBinding
import com.mapbox.api.geocoding.v5.MapboxGeocoding
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.gestures.gestures
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import utils.ViewUtils

class LocationPickerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLocationPickerBinding
    private var mapView: MapView? = null
    private var selectedPoint: Point? = null
    private var selectedAddress: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLocationPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mapView = binding.mapView
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupInitialCamera()
            setupMapListeners()
        }

        setupClickListeners()
        setupSearch()
    }

    private fun setupInitialCamera() {
        // Default to a central location (e.g., Rawalpindi)
        val initialPoint = Point.fromLngLat(73.0535, 33.5985)
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(initialPoint)
                .zoom(15.0)
                .build()
        )
        updateAddressAt(initialPoint)
    }

    private fun setupMapListeners() {
        // Explicitly enable all gestures
        mapView?.gestures?.apply {
            pinchToZoomEnabled = true
            doubleTapToZoomInEnabled = true
            doubleTouchToZoomOutEnabled = true
            quickZoomEnabled = true
            scrollEnabled = true
            rotateEnabled = true
            pitchEnabled = true
        }

        // Update address when map movement ends
        mapView?.gestures?.addOnMoveListener(object : com.mapbox.maps.plugin.gestures.OnMoveListener {
            override fun onMoveBegin(detector: com.mapbox.android.gestures.MoveGestureDetector) {}
            override fun onMove(detector: com.mapbox.android.gestures.MoveGestureDetector): Boolean = false
            override fun onMoveEnd(detector: com.mapbox.android.gestures.MoveGestureDetector) {
                mapView?.mapboxMap?.cameraState?.center?.let {
                    updateAddressAt(it)
                }
            }
        })

        // Also update address when map becomes idle (covers zoom/pinch)
        mapView?.mapboxMap?.addOnCameraChangeListener {
            // Optional: can add debounce if needed
        }
    }

    private fun setupSearch() {
        binding.etSearchLocation.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                binding.btnClearSearch.visibility = if (s.isNullOrEmpty()) android.view.View.GONE else android.view.View.VISIBLE
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        binding.btnClearSearch.setOnClickListener {
            binding.etSearchLocation.text.clear()
        }

        binding.etSearchLocation.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearchLocation.text.toString()
                if (query.isNotEmpty()) {
                    searchLocation(query)
                }
                true
            } else false
        }
    }

    private fun searchLocation(query: String) {
        val client = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .query(query)
            .build()

        client.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                val body = response.body()
                if (body != null && body.features().isNotEmpty()) {
                    val feature = body.features()[0]
                    val point = feature.center()
                    point?.let {
                        mapView?.mapboxMap?.setCamera(
                            CameraOptions.Builder()
                                .center(it)
                                .zoom(15.0)
                                .build()
                        )
                        updateAddressAt(it)
                    }
                } else {
                    Toast.makeText(this@LocationPickerActivity, "Location not found", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {
                Toast.makeText(this@LocationPickerActivity, "Search failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateAddressAt(point: Point) {
        selectedPoint = point
        val client = MapboxGeocoding.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .query(point)
            .build()

        client.enqueueCall(object : Callback<GeocodingResponse> {
            override fun onResponse(call: Call<GeocodingResponse>, response: Response<GeocodingResponse>) {
                val body = response.body()
                if (body != null && body.features().isNotEmpty()) {
                    selectedAddress = body.features()[0].placeName() ?: "Unknown Location"
                    binding.tvSelectedAddress.text = selectedAddress
                }
            }

            override fun onFailure(call: Call<GeocodingResponse>, t: Throwable) {}
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { view ->
            ViewUtils.applyClickEffect(view)
            finish()
        }

        binding.btnConfirmLocation.setOnClickListener { view ->
            ViewUtils.applyClickEffect(view)
            if (selectedAddress.isNotEmpty()) {
                val resultIntent = Intent()
                resultIntent.putExtra("SELECTED_ADDRESS", selectedAddress)
                resultIntent.putExtra("LATITUDE", selectedPoint?.latitude())
                resultIntent.putExtra("LONGITUDE", selectedPoint?.longitude())
                setResult(Activity.RESULT_OK, resultIntent)
                finish()
            }
        }
    }

    override fun onStart() {
        super.onStart()
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}