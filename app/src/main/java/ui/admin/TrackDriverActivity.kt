package ui.admin

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import com.example.bustrack_app.R
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.addLayer
import com.mapbox.maps.extension.style.layers.generated.lineLayer
import com.mapbox.maps.extension.style.sources.addSource
import com.mapbox.maps.extension.style.sources.generated.geoJsonSource
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class TrackDriverActivity : AppCompatActivity() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<NestedScrollView>
    private var mapView: MapView? = null
    private val ROUTE_SOURCE_ID = "route-source-id"
    private val ROUTE_LAYER_ID = "route-layer-id"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_track_driver)

        supportActionBar?.hide()

        // Initialize Mapbox Map
        mapView = findViewById(R.id.mapView)
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupMapAnnotations()
        }

        // Initialize Bottom Sheet
        val bottomSheet = findViewById<NestedScrollView>(R.id.bottomSheet)
        bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)

        // Optional: Bottom Sheet state change listener
        bottomSheetBehavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                // Handle state changes
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
                // Animation logic
            }
        })

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupMapAnnotations() {
        val originPoint = Point.fromLngLat(73.0535, 33.5985) // FG College Rawalpindi
        val destinationPoint = Point.fromLngLat(73.0450, 33.6050) // Nearby Saddar area
        
        // Add Markers for Origin and Destination
        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        
        // Origin Marker
        val originOptions = PointAnnotationOptions()
            .withPoint(originPoint)
            .withTextField("Driver: Marcus Thompson")
        pointAnnotationManager?.create(originOptions)

        // Destination Marker
        val destOptions = PointAnnotationOptions()
            .withPoint(destinationPoint)
            .withTextField("Bus Stop")
        pointAnnotationManager?.create(destOptions)

        // Fetch and draw route
        getRoute(originPoint, destinationPoint)
    }

    private fun getRoute(origin: Point, destination: Point) {
        val client = MapboxDirections.builder()
            .accessToken(getString(R.string.mapbox_access_token))
            .routeOptions(
                RouteOptions.builder()
                    .coordinatesList(listOf(origin, destination))
                    .profile(DirectionsCriteria.PROFILE_DRIVING)
                    .overview(DirectionsCriteria.OVERVIEW_FULL)
                    .build()
            )
            .build()

        client.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(call: Call<DirectionsResponse>, response: Response<DirectionsResponse>) {
                if (response.body() == null || response.body()!!.routes().isEmpty()) {
                    Log.e("TrackDriverActivity", "No routes found")
                    return
                }

                val currentRoute = response.body()!!.routes()[0]
                val geometry = currentRoute.geometry() ?: return
                
                // Draw route on map
                drawRoute(geometry)
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.e("TrackDriverActivity", "Error: " + t.message)
                Toast.makeText(this@TrackDriverActivity, "Error fetching route", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun drawRoute(geometry: String) {
        mapView?.mapboxMap?.getStyle { style ->
            // Remove existing source and layer if they exist
            if (style.styleSourceExists(ROUTE_SOURCE_ID)) {
                // style.removeStyleLayer(ROUTE_LAYER_ID) // V11 handles this differently or you can just update source
                // For simplicity in this example, we check and add
            }

            val lineString = LineString.fromPolyline(geometry, 6)
            
            // Add Source
            if (!style.styleSourceExists(ROUTE_SOURCE_ID)) {
                style.addSource(geoJsonSource(ROUTE_SOURCE_ID) {
                    geometry(lineString)
                })
            }

            // Add Layer
            if (!style.styleLayerExists(ROUTE_LAYER_ID)) {
                style.addLayer(lineLayer(ROUTE_LAYER_ID, ROUTE_SOURCE_ID) {
                    lineColor(Color.parseColor("#3bb2d0"))
                    lineWidth(5.0)
                    lineJoin(com.mapbox.maps.extension.style.layers.properties.generated.LineJoin.ROUND)
                })
            }

            // Zoom to fit route
            val cameraOptions = mapView?.mapboxMap?.cameraForGeometry(
                lineString,
                EdgeInsets(100.0, 100.0, 100.0, 100.0)
            )
            if (cameraOptions != null) {
                mapView?.mapboxMap?.setCamera(cameraOptions)
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