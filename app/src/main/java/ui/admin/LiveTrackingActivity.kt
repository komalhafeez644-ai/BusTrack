package ui.admin
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import ui.admin.TrackDriverActivity
import com.google.android.material.button.MaterialButton
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class LiveTrackingActivity : AppCompatActivity(), OnMapReadyCallback {

    private var mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_live_tracking)

        supportActionBar?.hide()

        // Initialize Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        utils.NavigationUtils.setupBottomNavigation(this)

        findViewById<MaterialButton>(R.id.btnTrackDriver)?.setOnClickListener {
            val intent = Intent(this, TrackDriverActivity::class.java)
            startActivity(intent)
        }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        // Set a default location (e.g., London or any city)
        val defaultLocation = LatLng(31.5204, 74.3587) // Lahore example
        mMap?.addMarker(MarkerOptions().position(defaultLocation).title("Marker in Lahore"))
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 12f))
        
        // Customizing Map (Optional)
        mMap?.uiSettings?.isZoomControlsEnabled = false
        mMap?.uiSettings?.isCompassEnabled = true
    }
}