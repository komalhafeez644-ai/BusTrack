package com.example.bustrack_app.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.StopAdapter
import com.example.bustrack_app.databinding.ActivityRouteDetailBinding
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.viewmodels.RouteViewModel
import com.google.android.material.button.MaterialButton
import ui.admin.RouteMapActivity
import java.util.Locale

class RouteDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRouteDetailBinding
    private val viewModel: RouteViewModel by viewModels()
    private var currentRoute: RouteModel? = null
    private lateinit var stopAdapter: StopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRouteDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get route ID from intent
        val routeId = intent.getStringExtra("ROUTE_ID")
        
        viewModel.routeList.observe(this) { routes ->
            currentRoute = routes.find { it.id == routeId }
            setupDataDisplay()
        }

        setupRecyclerView()

        // Full Screen Map Navigation
        binding.btnViewOnMap.setOnClickListener {
            val intent = Intent(this, RouteMapActivity::class.java)
            intent.putExtra("ROUTE_ID", currentRoute?.id)
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener { finish() }

        binding.btnDeleteRoute.setOnClickListener {
            currentRoute?.let { route ->
                showDeleteConfirmationDialog(route)
            }
        }

        binding.btnSaveChanges.setOnClickListener {
            currentRoute?.let { route ->
                binding.btnSaveChanges.isEnabled = false
                binding.btnSaveChanges.text = "Saving..."
                
                com.example.bustrack_app.data.RouteRepository.updateRoute(route) { success ->
                    if (success) {
                        // Notify the assigned driver about the route update
                        val allDrivers = com.example.bustrack_app.data.DriverRepository.driverList.value ?: emptyList()
                        val assignedDriver = allDrivers.find { it.assignedBus == route.busNo }
                        assignedDriver?.let { driver ->
                            val driverTarget = driver.uid.ifBlank { driver.driverId.ifBlank { driver.id } }
                            FirebaseRepository.notifyRouteUpdated(driverTarget, route.routeName)
                        }
                        FirebaseRepository.notifyParentsOfRouteUpdate(
                            route.routeName,
                            "Your child's assigned route (${route.routeName}) has been updated by administration. Please review the updated stop details."
                        )

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

    private fun showDeleteConfirmationDialog(route: RouteModel) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_status, null)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)
            .create()

        dialogView.findViewById<ImageView>(R.id.ivDialogIcon).setImageResource(R.drawable.warning)
        dialogView.findViewById<ImageView>(R.id.ivDialogIcon).setColorFilter(android.graphics.Color.parseColor("#DC2626"))
        dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = "Delete Route?"
        dialogView.findViewById<TextView>(R.id.tvDialogMessage).text = "Are you sure you want to permanently delete route '${route.routeName}'? This action cannot be undone."
        
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        btnConfirm.text = "Delete"
        btnConfirm.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DC2626"))
        
        btnConfirm.setOnClickListener {
            viewModel.deleteRoute(route.id) { success ->
                if (success) {
                    Toast.makeText(this, "Route deleted successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to delete route", Toast.LENGTH_SHORT).show()
                }
            }
            dialog.dismiss()
        }

        dialogView.findViewById<MaterialButton>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
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
