package ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.google.android.material.button.MaterialButton
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager

class LiveTrackingActivity : AppCompatActivity() {

    private var mapView: MapView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking)

        supportActionBar?.hide()

        // Initialize Mapbox Map
        mapView = findViewById(R.id.mapView)
        mapView?.mapboxMap?.loadStyle(Style.MAPBOX_STREETS) {
            setupMap()
        }

        findViewById<MaterialButton>(R.id.btnTrackDriver)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, TrackDriverActivity::class.java)
            startActivity(intent)
        }

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fabAlert)?.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, BroadcastNotificationActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        utils.NavigationUtils.setupBottomNavigation(this)
    }

    private fun setupMap() {
        // Default location (Islamabad)
        val defaultPoint = Point.fromLngLat(73.0479, 33.6844)
        
        // Set camera
        mapView?.mapboxMap?.setCamera(
            CameraOptions.Builder()
                .center(defaultPoint)
                .zoom(12.0)
                .build()
        )

        // Add Marker
        val annotationApi = mapView?.annotations
        val pointAnnotationManager = annotationApi?.createPointAnnotationManager()
        
        val pointAnnotationOptions = PointAnnotationOptions()
            .withPoint(defaultPoint)
            .withTextField("Bus #442-RT")
        
        pointAnnotationManager?.create(pointAnnotationOptions)
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