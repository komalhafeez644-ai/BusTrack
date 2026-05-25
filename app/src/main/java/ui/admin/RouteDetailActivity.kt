package com.example.bustrack_app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.adapter.StopAdapter // 👈 StopAdapter ka import
import com.example.bustrack_app.databinding.ActivityRouteDetailBinding
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem
import java.util.Locale

class RouteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteDetailBinding
    private var currentRoute: RouteModel? = null
    private lateinit var stopAdapter: StopAdapter // 👈 Adapter ka global variable declare kiya

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRouteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get route ID from intent
        val routeId = intent.getStringExtra("ROUTE_ID")
        currentRoute = com.example.bustrack_app.data.RouteRepository.routeList.value?.find { it.id == routeId }
        
        // If not found, use a fallback (for testing)
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

        // ⚙️ RecyclerView aur Adapter Setup
        setupRecyclerView()

        // Back Action
        binding.btnBack.setOnClickListener { finish() }

        // Preview data display load karna
        setupDataDisplay()

        // Map Button Click Listener
        binding.btnViewOnMap.setOnClickListener {
            currentRoute?.let { route ->
                if (route.stopsList.isNotEmpty()) {
                    val firstStop = route.stopsList[0]
                    val mapUri = "geo:${firstStop.latitude},${firstStop.longitude}?q=${firstStop.latitude},${firstStop.longitude}(${firstStop.stopName})".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, mapUri)
                    intent.setPackage("com.google.android.apps.maps")

                    if (intent.resolveActivity(packageManager) != null) {
                        startActivity(intent)
                    } else {
                        val browserUri = "http://maps.google.com/?q=${firstStop.latitude},${firstStop.longitude}".toUri()
                        startActivity(Intent(Intent.ACTION_VIEW, browserUri))
                    }
                } else {
                    Toast.makeText(this, "No stops available to show on map", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Add Stop Click Listener
        binding.btnAddNewStopAction.setOnClickListener {
            val stopName = binding.etSearchOrAddStop.text.toString().trim()
            if (stopName.isNotEmpty()) {
                val nextId = String.format(Locale.getDefault(), "%02d", (currentRoute?.stopsList?.size ?: 0) + 1)
                val newStop = StopItem(nextId, stopName, "07:35 AM", 33.7000, 73.0600)

                currentRoute?.stopsList?.add(newStop)
                Toast.makeText(this, "$stopName added successfully!", Toast.LENGTH_SHORT).show()
                binding.etSearchOrAddStop.text?.clear()

                // 🔄 List aur counter dono ko live update karna
                updateStopsList()
            }
        }

        // Save Changes Click Listener
        binding.btnSaveChanges.setOnClickListener {
            currentRoute?.let { route ->
                com.example.bustrack_app.data.RouteRepository.updateRoute(route)
                Toast.makeText(this, "Changes saved successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    // RecyclerView aur Adapter ko initial settings dena
    private fun setupRecyclerView() {
        binding.rvStopsList.layoutManager = LinearLayoutManager(this)

        // Adapter initialize kiya aur cross icon click par onDeleteStopClicked run kiya
        stopAdapter = StopAdapter(mutableListOf()) { deletedStop ->
            onDeleteStopClicked(deletedStop)
        }
        binding.rvStopsList.adapter = stopAdapter
    }

    // Text data aur initially list load karne ke liye
    private fun setupDataDisplay() {
        currentRoute?.let { route ->
            binding.tvMainRouteName.text = "${route.routeName} -\nRoute ${route.routeCode}"
            binding.tvBusNo.text = route.busNo
            binding.tvDriverName.text = route.driverName
            binding.tvStudentsCount.text = route.studentsCount.toString()

            // Initial load for stops list
            updateStopsList()
        }
    }

    // counter aur adapter dono ko ek sath refresh karne ka standard function
    private fun updateStopsList() {
        currentRoute?.let { route ->
            // 1. Counter text change hoga
            binding.tvTotalStopsCount.text = "${route.stopsList.size} STOPS TOTAL"

            // 2. Adapter ke paas naya data jayega aur list re-render hogi
            stopAdapter.updateStops(route.stopsList)
        }
    }

    // Stop delete function jo adapter se trigger hoga
    fun onDeleteStopClicked(stop: StopItem) {
        currentRoute?.stopsList?.remove(stop)
        Toast.makeText(this, "${stop.stopName} removed", Toast.LENGTH_SHORT).show()

        // 🔄 Delete ke baad serial numbers aur list sync karne ke liye refresh
        fixStopSequences()
        updateStopsList()
    }

    // Stop delete hone ke baad serial numbers (01, 02, 03) ko sahi karne ke liye logic
    private fun fixStopSequences() {
        currentRoute?.stopsList?.forEachIndexed { index, stopItem ->
            // index 0 hai to "01" banayega, 1 hai to "02" banayega
            val newId = String.format(Locale.getDefault(), "%02d", index + 1)
            // Model ke andar id field update karne ke liye agar id variable writable (var) ho
            // Agar aapke StopItem mein id 'val' hai to aap ise skip bhi kar sakte hain.
        }
    }
}