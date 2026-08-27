package com.example.bustrack_app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.adapter.StopAdapter
import com.example.bustrack_app.databinding.ActivityRouteDetailBinding
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem
import ui.admin.RouteMapActivity
import java.util.Locale

class RouteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteDetailBinding
    private var currentRoute: RouteModel? = null
    private lateinit var stopAdapter: StopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRouteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get route ID from intent
        val routeId = intent.getStringExtra("ROUTE_ID")
        currentRoute = com.example.bustrack_app.data.RouteRepository.routeList.value?.find { it.id == routeId }
        
        if (currentRoute == null) {
            currentRoute = com.example.bustrack_app.models.RouteModel(
                id = "1",
                routeCode = "05",
                routeName = "Express North",
                status = "ACTIVE",
                busNo = "BUS-102",
                driverName = "Ahmed Ali",
                stopsCount = 8,
                studentsCount = 48,
                stopsList = mutableListOf(
                    com.example.bustrack_app.models.StopItem("01", "Sunrise Apartments", "07:00 AM", 33.7000, 73.0600),
                    com.example.bustrack_app.models.StopItem("02", "Green Park", "07:15 AM", 33.7100, 73.0700),
                    com.example.bustrack_app.models.StopItem("03", "Library West Gate", "07:25 AM", 33.7200, 73.0800)
                )
            )
        }

        setupRecyclerView()

        // Full Screen Map Navigation
        binding.btnViewOnMap.setOnClickListener {
            val intent = Intent(this, RouteMapActivity::class.java)
            intent.putExtra("ROUTE_ID", currentRoute?.id)
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener { finish() }
        setupDataDisplay()

        binding.btnSaveChanges.setOnClickListener {
            currentRoute?.let { route ->
                binding.btnSaveChanges.isEnabled = false
                binding.btnSaveChanges.text = "Saving..."
                
                com.example.bustrack_app.data.RouteRepository.updateRoute(route) { success ->
                    if (success) {
                        Toast.makeText(this, "Changes saved to Cloud", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        binding.btnSaveChanges.isEnabled = true
                        binding.btnSaveChanges.text = "Save Changes"
                        Toast.makeText(this, "Failed to update", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun setupRecyclerView() {
        binding.rvStopsList.layoutManager = LinearLayoutManager(this)
        stopAdapter = StopAdapter(mutableListOf())
        binding.rvStopsList.adapter = stopAdapter
    }

    private fun setupDataDisplay() {
        currentRoute?.let { route ->
            binding.tvMainRouteName.text = "${route.routeName} -\nRoute ${route.routeCode}"
            binding.tvBusNo.text = route.busNo
            binding.tvDriverName.text = route.driverName
            binding.tvStudentsCount.text = route.studentsCount.toString()
            updateStopsList()
        }
    }

    private fun updateStopsList() {
        currentRoute?.let { route ->
            binding.tvTotalStopsCount.text = "${route.stopsList.size} STOPS TOTAL"
            stopAdapter.updateStops(route.stopsList)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh if changes made on full screen map
        updateStopsList()
        utils.NavigationUtils.setupBottomNavigation(this)
    }
}
