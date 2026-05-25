package ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivityAddRouteBinding
import com.example.bustrack_app.models.StopItem
import com.example.bustrack_app.viewmodels.AddRouteViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions

class AddRouteActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityAddRouteBinding
    private lateinit var mMap: GoogleMap

    private val viewModel: AddRouteViewModel by viewModels()

    private var polyline: Polyline? = null

    // Route points
    private val routePoints = mutableListOf<LatLng>()

    // Stop markers
    private val stopMarkers = mutableListOf<Marker>()

    // Stop mode
    private var isAddingStop = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupMap()
        setupClicks()
        observeData()
    }

    private fun setupMap() {

        val mapFragment =
            supportFragmentManager.findFragmentById(binding.mapFragment.id)
                    as SupportMapFragment

        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {

        mMap = googleMap

        // Default location (Islamabad / Rawalpindi)
        val defaultLocation = LatLng(33.6844, 73.0479)

        mMap.moveCamera(
            CameraUpdateFactory.newLatLngZoom(
                defaultLocation,
                12f
            )
        )

        mMap.uiSettings.isZoomControlsEnabled = true
        mMap.uiSettings.isCompassEnabled = true
        mMap.uiSettings.isMapToolbarEnabled = true

        mapClickListener()
        longPressListener()
    }

    private fun mapClickListener() {

        mMap.setOnMapClickListener { latLng ->

            // Agar stop mode on hai to route na draw karo
            if (isAddingStop) return@setOnMapClickListener

            routePoints.add(latLng)

            drawPolyline()

            viewModel.updateInstruction(
                "Route drawing in progress"
            )
        }
    }

    private fun longPressListener() {

        mMap.setOnMapLongClickListener { latLng ->

            if (!isAddingStop) return@setOnMapLongClickListener

            val marker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Bus Stop ${stopMarkers.size + 1}")
            )

            marker?.let {
                stopMarkers.add(it)
            }

            // MODEL FIX
            val stop = StopItem(
                id = stopMarkers.size.toString(),
                stopName = "Stop ${stopMarkers.size}",
                time = "08:00 AM",
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )

            viewModel.addStop(stop)

            Toast.makeText(
                this,
                "Stop Added Successfully",
                Toast.LENGTH_SHORT
            ).show()

            viewModel.updateInstruction(
                "Stop added successfully"
            )
        }
    }

    private fun drawPolyline() {

        polyline?.remove()

        polyline = mMap.addPolyline(
            PolylineOptions()
                .addAll(routePoints)
                .width(10f)
        )

        when (routePoints.size) {

            0 -> {
                viewModel.updateInstruction(
                    "Tap on map to add route point"
                )
            }

            1 -> {
                viewModel.updateInstruction(
                    "Keep tapping on map to add route points"
                )
            }

            else -> {
                viewModel.updateInstruction(
                    "Route drawing in progress"
                )
            }
        }
    }

    private fun setupClicks() {

        // Back button
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Undo route point
        binding.btnUndo.setOnClickListener {

            if (routePoints.isNotEmpty()) {

                routePoints.removeLast()
                drawPolyline()
            }
        }

        // Clear all
        binding.btnClear.setOnClickListener {

            routePoints.clear()

            polyline?.remove()

            stopMarkers.forEach {
                it.remove()
            }

            stopMarkers.clear()

            viewModel.clearStops()

            viewModel.updateInstruction(
                "Tap on map to add route point"
            )

            Toast.makeText(
                this,
                "Route Cleared",
                Toast.LENGTH_SHORT
            ).show()
        }

        // Enable stop mode
        binding.btnAddStop.setOnClickListener {

            isAddingStop = !isAddingStop

            if (isAddingStop) {

                viewModel.updateInstruction(
                    "Long press on map to add stop"
                )

                Toast.makeText(
                    this,
                    "Stop Mode Enabled",
                    Toast.LENGTH_SHORT
                ).show()

            } else {

                viewModel.updateInstruction(
                    "Route drawing in progress"
                )
            }
        }

        // Save
        binding.btnSave.setOnClickListener {

            val routeName =
                binding.etRouteName.text.toString().trim()

            if (routeName.isEmpty()) {

                Toast.makeText(
                    this,
                    "Enter Route Name",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            if (routePoints.isEmpty()) {

                Toast.makeText(
                    this,
                    "Please draw route first",
                    Toast.LENGTH_SHORT
                ).show()

                return@setOnClickListener
            }

            Toast.makeText(
                this,
                "Route Saved Successfully",
                Toast.LENGTH_SHORT
            ).show()

            finish()
        }
    }

    private fun observeData() {

        viewModel.instruction.observe(this) {

            binding.tvInstruction.text = it
        }
    }
}