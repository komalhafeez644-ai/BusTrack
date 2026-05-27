package ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.material.button.MaterialButton
import utils.NavigationUtils

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

        findViewById<MaterialButton>(R.id.btnTrackDriver)?.setOnClickListener {
            val intent = Intent(this, TrackDriverActivity::class.java)
            startActivity(intent)
        }
        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationUtils.setupBottomNavigation(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        // Add map settings here
    }
}