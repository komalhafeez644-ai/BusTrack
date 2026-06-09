package ui.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.bustrack_app.R
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.databinding.ActivityEditBusDetailsBinding
import com.example.bustrack_app.databinding.DialogDeleteBusBinding
import com.example.bustrack_app.models.BusModel
import com.example.bustrack_app.viewmodels.BusViewModel
import com.bumptech.glide.Glide

class EditBusDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBusDetailsBinding
    private lateinit var viewModel: BusViewModel
    private lateinit var originalBusNumber: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBusDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils.NavigationUtils.setupBottomNavigation(this)

        viewModel = ViewModelProvider(this)[BusViewModel::class.java]

        // Intent se data receive karna
        originalBusNumber = intent.getStringExtra("BUS_NUMBER") ?: ""
        val intentCapacity = intent.getIntExtra("BUS_CAPACITY", 40)
        val intentDriver = intent.getStringExtra("BUS_DRIVER") ?: ""
        val intentRoute = intent.getStringExtra("BUS_ROUTE") ?: ""
        val intentStatus = intent.getStringExtra("BUS_STATUS") ?: "ACTIVE"

        // UI Views mein data set karna
        binding.tvCardBusNumber.text = originalBusNumber
        binding.tvCardBusDetails.text = if (intentRoute.isNotEmpty()) "$intentRoute • Route" else "No Route Assigned"
        binding.etEditBusNumber.setText(originalBusNumber)
        binding.etEditCapacity.setText(intentCapacity.toString())
        
        // Setup Route Dropdown
        setupRouteDropdown(intentRoute)
        
        // Setup Driver Dropdown
        setupDriverDropdown(intentDriver)

        // Back Arrow Click
        binding.btnBackArrow.setOnClickListener { finish() }

        // UPDATE BUTTON CLICK
        binding.btnUpdateBus.setOnClickListener {
            val updatedNo = binding.etEditBusNumber.text.toString().trim()
            val updatedCapStr = binding.etEditCapacity.text.toString().trim()
            val selectedRouteName = binding.menuEditRoute.text.toString().trim()
            val selectedDriverName = binding.menuEditDriver.text.toString().trim()

            if (updatedNo.isEmpty() || updatedCapStr.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show()
            } else {
                val updatedCapacity = updatedCapStr.toIntOrNull() ?: 0
                
                // 1. Update the Route Assignment logic
                assignBusToRoute(updatedNo, selectedRouteName, selectedDriverName)

                // 2. Update Bus Details
                val updatedBus = BusModel(
                    busNumber = updatedNo,
                    totalSeats = updatedCapacity,
                    driverName = if (selectedDriverName == "Select Driver" || selectedDriverName.isEmpty()) null else selectedDriverName,
                    routeName = if (selectedRouteName == "Select Route" || selectedRouteName.isEmpty()) null else selectedRouteName,
                    status = intentStatus
                )

                viewModel.updateBusDetails(originalBusNumber, updatedBus)
                Toast.makeText(this, "Bus & Route updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // DELETE BUTTON CLICK
        binding.btnDeleteBus.setOnClickListener {
            showDeleteConfirmationPopup()
        }
    }

    private fun setupRouteDropdown(currentRoute: String) {
        val routes = RouteRepository.routeList.value ?: listOf()
        val routeNames = routes.map { it.routeName }.toMutableList()
        routeNames.add(0, "Select Route")

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, routeNames)
        binding.menuEditRoute.setAdapter(adapter)
        
        if (currentRoute.isNotEmpty()) {
            binding.menuEditRoute.setText(currentRoute, false)
        } else {
            binding.menuEditRoute.setText("Select Route", false)
        }
    }

    private fun setupDriverDropdown(currentDriver: String) {
        val drivers = DriverRepository.driverList.value ?: listOf()
        val driverNames = drivers.map { it.name }.toMutableList()
        driverNames.add(0, "Select Driver")

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, driverNames)
        binding.menuEditDriver.setAdapter(adapter)

        if (currentDriver.isNotEmpty()) {
            binding.menuEditDriver.setText(currentDriver, false)
        } else {
            binding.menuEditDriver.setText("Select Driver", false)
        }
    }

    private fun assignBusToRoute(busNo: String, routeName: String, driverName: String) {
        val finalDriver = if (driverName == "Select Driver") "" else driverName
        
        // 1. Update Driver Repository (Consistency)
        if (finalDriver.isNotEmpty()) {
            val drivers = DriverRepository.driverList.value ?: listOf()
            val targetDriver = drivers.find { it.name == finalDriver }
            targetDriver?.let {
                DriverRepository.updateDriver(it.copy(assignedBus = busNo, route = routeName))
            }
        }

        if (routeName == "Select Route" || routeName.isEmpty()) return

        val allRoutes = RouteRepository.routeList.value ?: return
        
        // 2. Remove this bus from any other route first
        allRoutes.forEach { route ->
            if (route.busNo == busNo) {
                RouteRepository.updateRoute(route.copy(busNo = "", driverName = ""))
            }
        }

        // 3. Assign this bus and driver to the selected route
        val targetRoute = allRoutes.find { it.routeName == routeName }
        targetRoute?.let {
            RouteRepository.updateRoute(it.copy(busNo = busNo, driverName = finalDriver))
        }
    }

    // Custom Delete Confirmation Popup ki Logic
    private fun showDeleteConfirmationPopup() {
        val dialogBinding = DialogDeleteBusBinding.inflate(layoutInflater)

        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)

        val alertDialog = builder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 1. Cancel Button Click
        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        // 2. Delete Button Click
        dialogBinding.btnDelete.setOnClickListener {
            viewModel.deleteBusFromFleet(originalBusNumber)
            Toast.makeText(this, "Delete successfully", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
            finish()
        }

        alertDialog.show()
    }
}
