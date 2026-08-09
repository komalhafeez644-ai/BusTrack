package ui.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.bustrack_app.R
import com.example.bustrack_app.data.BusRepository
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
    private var initialDriver: String = ""
    private var initialRoute: String = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBusDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils.NavigationUtils.setupBottomNavigation(this)

        viewModel = ViewModelProvider(this)[BusViewModel::class.java]

        // Pre-fetch and observe data for dropdowns to ensure they are ready when needed
        DriverRepository.driverList.observe(this) { }
        RouteRepository.routeList.observe(this) { }

        // Intent se data receive karna
        originalBusNumber = intent.getStringExtra("BUS_NUMBER") ?: ""
        val intentCapacity = intent.getIntExtra("BUS_CAPACITY", 40)
        initialDriver = intent.getStringExtra("BUS_DRIVER") ?: ""
        initialRoute = intent.getStringExtra("BUS_ROUTE") ?: ""
        val intentStatus = intent.getStringExtra("BUS_STATUS") ?: "ACTIVE"

        val intentDriver = initialDriver
        val intentRoute = initialRoute

        // UI Views mein data set karna
        binding.tvCardBusNumber.text = originalBusNumber
        binding.tvCardBusDetails.text = if (intentRoute.isNotEmpty()) "$intentRoute • Route" else "No Route Assigned"
        binding.etEditBusNumber.setText(originalBusNumber)
        binding.etEditCapacity.setText(intentCapacity.toString())
        
        // Setup Route Dropdown
        setupRouteDropdown(intentRoute)
        
        // Setup Driver Dropdown
        setupDriverDropdown(intentDriver)

        // Formatting
        utils.FormUtils.setupUppercaseInput(binding.etEditBusNumber)

        // Back Arrow Click
        binding.btnBackArrow.setOnClickListener { 
            utils.ViewUtils.applyClickEffect(it)
            finish() 
        }

        // UPDATE BUTTON CLICK
        binding.btnUpdateBus.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val updatedNo = binding.etEditBusNumber.text.toString().trim()
            val updatedCapStr = binding.etEditCapacity.text.toString().trim()
            val selectedRouteName = binding.menuEditRoute.text.toString().trim()
            val selectedDriverName = binding.menuEditDriver.text.toString().trim()

            if (updatedNo.isEmpty() || updatedCapStr.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show()
            } else {
                val updatedCapacity = updatedCapStr.toIntOrNull() ?: 0
                
                // VALIDATION: Check if the selected driver is already assigned to another bus
                if (selectedDriverName != "None" && selectedDriverName.isNotEmpty()) {
                    val allBuses = viewModel.busList.value ?: emptyList()
                    val otherBus = allBuses.find { it.driverName == selectedDriverName && it.busNumber != originalBusNumber }
                    if (otherBus != null) {
                        Toast.makeText(this, "Driver $selectedDriverName is already assigned to Bus ${otherBus.busNumber}", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                }

                // VALIDATION: Check if the selected route is already assigned to another bus
                if (selectedRouteName != "None" && selectedRouteName.isNotEmpty()) {
                    val allBuses = viewModel.busList.value ?: emptyList()
                    val otherBus = allBuses.find { it.routeName == selectedRouteName && it.busNumber != originalBusNumber }
                    if (otherBus != null) {
                        Toast.makeText(this, "Route $selectedRouteName is already assigned to Bus ${otherBus.busNumber}", Toast.LENGTH_LONG).show()
                        return@setOnClickListener
                    }
                }

                // 1. Update the Route Assignment logic
                assignBusToRoute(updatedNo, selectedRouteName, selectedDriverName)

                // 2. Update Bus Details
                val updatedBus = BusModel(
                    busNumber = updatedNo,
                    totalSeats = updatedCapacity,
                    driverName = if (selectedDriverName == "None" || selectedDriverName.isEmpty()) null else selectedDriverName,
                    routeName = if (selectedRouteName == "None" || selectedRouteName.isEmpty()) null else selectedRouteName,
                    status = intentStatus
                )

                viewModel.updateBusDetails(originalBusNumber, updatedBus)
                Toast.makeText(this, "Bus & Route updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // DELETE BUTTON CLICK
        binding.btnDeleteBus.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            showDeleteConfirmationPopup()
        }
    }

    private fun setupRouteDropdown(currentRoute: String) {
        val routes = RouteRepository.routeList.value ?: listOf()
        val routeNames = routes.map { it.routeName }.toMutableList()
        routeNames.add(0, "None")

        val adapter = ArrayAdapter(this, com.example.bustrack_app.R.layout.spinner_dropdown_item, routeNames)
        binding.menuEditRoute.setAdapter(adapter)
        
        if (currentRoute.isNotEmpty()) {
            binding.menuEditRoute.setText(currentRoute, false)
        } else {
            binding.menuEditRoute.setText("None", false)
        }
    }

    private fun setupDriverDropdown(currentDriver: String) {
        val drivers = DriverRepository.driverList.value ?: listOf()
        val driverNames = drivers.map { it.name }.toMutableList()
        driverNames.add(0, "None")

        val adapter = ArrayAdapter(this, com.example.bustrack_app.R.layout.spinner_dropdown_item, driverNames)
        binding.menuEditDriver.setAdapter(adapter)

        if (currentDriver.isNotEmpty()) {
            binding.menuEditDriver.setText(currentDriver, false)
        } else {
            binding.menuEditDriver.setText("None", false)
        }
    }

    private fun assignBusToRoute(busNo: String, routeName: String, driverName: String) {
        val finalDriver = if (driverName == "None") "" else driverName
        val finalRoute = if (routeName == "None") "" else routeName
        
        // 1. Handle DRIVER Unassignment & Reassignment
        val drivers = DriverRepository.driverList.value ?: listOf()
        
        // Unassign Previous Driver if they were changed
        if (initialDriver.isNotEmpty() && initialDriver != finalDriver) {
            drivers.find { it.name == initialDriver }?.let {
                DriverRepository.updateDriver(it.copy(assignedBus = null, route = null))
            }
        }

        // Handle the NEW Driver being assigned
        if (finalDriver.isNotEmpty()) {
            drivers.find { it.name == finalDriver }?.let { driver ->
                // IMPORTANT: If this NEW driver was on another bus, clear that bus first
                driver.assignedBus?.let { oldBusNo ->
                    if (oldBusNo != busNo) {
                        BusRepository.getBusByNumber(oldBusNo)?.let { oldBus ->
                            BusRepository.updateBusDetails(oldBusNo, oldBus.copy(driverName = null))
                        }
                        // Also clear the driver from the old route
                        RouteRepository.routeList.value?.find { it.busNo == oldBusNo }?.let { oldRoute ->
                            RouteRepository.updateRoute(oldRoute.copy(driverName = ""))
                        }
                    }
                }
                // Now assign to this bus
                DriverRepository.updateDriver(driver.copy(assignedBus = busNo, route = finalRoute.ifEmpty { null }))
            }
        }

        // 2. Handle ROUTE Unassignment & Reassignment
        val allRoutes = RouteRepository.routeList.value ?: return

        // Clear previous route if it changed
        if (initialRoute.isNotEmpty() && initialRoute != finalRoute) {
            allRoutes.find { it.routeName == initialRoute }?.let {
                RouteRepository.updateRoute(it.copy(busNo = "", driverName = ""))
            }
        }

        // If the NEW route was linked to another bus, clear it
        if (finalRoute.isNotEmpty()) {
            allRoutes.find { it.routeName == finalRoute }?.let { route ->
                if (route.busNo.isNotEmpty() && route.busNo != busNo) {
                    BusRepository.getBusByNumber(route.busNo)?.let { oldBus ->
                        BusRepository.updateBusDetails(route.busNo, oldBus.copy(routeName = null))
                    }
                }
            }
        }

        // Always ensure this bus is not linked to any other random route
        allRoutes.forEach { route ->
            if (route.busNo == busNo && route.routeName != finalRoute) {
                RouteRepository.updateRoute(route.copy(busNo = "", driverName = ""))
            }
        }

        if (finalRoute.isNotEmpty()) {
            // Assign New Route
            allRoutes.find { it.routeName == finalRoute }?.let {
                RouteRepository.updateRoute(it.copy(busNo = busNo, driverName = finalDriver))
            }
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
            utils.ViewUtils.applyClickEffect(it)
            alertDialog.dismiss()
        }

        // 2. Delete Button Click
        dialogBinding.btnDelete.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            viewModel.deleteBusFromFleet(originalBusNumber)
            Toast.makeText(this, "Delete successfully", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
            finish()
        }

        alertDialog.show()
    }
}
