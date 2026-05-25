package ui.admin

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityRouteAnalysisBinding

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class RouteAnalysisActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityRouteAnalysisBinding
    private var mMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRouteAnalysisBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        setupUI()
        clickListeners()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        
        // Initial location view
        val sampleLocation = LatLng(31.5204, 74.3587)
        mMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(sampleLocation, 14f))
    }

    private fun setupUI() {
        // Get dynamic data from Intent
        val application = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
        
        application?.let {
            binding.tvStudentId.text = "Optimal match for Student ID:\n#SF-${1000 + it.id}"
            binding.tvRouteName.text = it.bestRoute
            binding.tvMatchPercent.text = "${it.matchPercent} Match"
            binding.tvNearestStop.text = it.nearestStop
            binding.tvDistance.text = it.distance
            
            if (it.image != 0) {
                binding.ivStudent.setImageResource(it.image)
                binding.ivStudent.setPadding(0, 0, 0, 0) // Remove padding if it's a real photo
            }
        }
    }

    private fun clickListeners() {
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnNotification.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
        }

        binding.btnConfirm.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            
            // Apply style change like confirmation screen
            binding.btnConfirm.backgroundTintList = android.content.res.ColorStateList.valueOf(
                androidx.core.content.ContextCompat.getColor(this, com.example.bustrack_app.R.color.primaryDark)
            )
            binding.btnConfirm.setTextColor(android.graphics.Color.WHITE)

            val application = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
            val intent = android.content.Intent(this, AssignmentConfirmationActivity::class.java)
            intent.putExtra("APPLICATION_DATA", application)
            
            // Delay slightly to show effect before transition
            it.postDelayed({
                startActivity(intent)
            }, 200)
        }

        binding.fabLocation.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            // Recenter map
        }
    }
}
