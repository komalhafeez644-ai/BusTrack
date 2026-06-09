package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.adapter.BusAdapter
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.databinding.ActivityManageBusesBinding
import com.example.bustrack_app.databinding.DialogAddNewBusBinding
import com.example.bustrack_app.models.BusModel
import com.example.bustrack_app.viewmodels.BusViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog

class ManageBusesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBusesBinding
    private lateinit var viewModel: BusViewModel
    private lateinit var busAdapter: BusAdapter

    // Local memory list testing ke liye
    private val temporaryBusList = mutableListOf<BusModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBusesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BusViewModel::class.java]

        setupRecyclerViewList()
        observeViewModelStreams()

        // Floating Action Button (+) Click Listener
        binding.btnFloatingAddBus.setOnClickListener {
            showAddNewBusBottomSheet()
        }

        // App Bar Back Arrow Click Listener
        binding.btnBackArrow.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh mapping from Route Management
        com.example.bustrack_app.data.BusRepository.refreshBusList()
        utils.NavigationUtils.setupBottomNavigation(this)
    }

    private fun setupRecyclerViewList() {
        busAdapter = BusAdapter(
            busList = temporaryBusList,
            onEditClicked = { bus ->
                val intent = Intent(this, EditBusDetailsActivity::class.java).apply {
                    putExtra("BUS_NUMBER", bus.busNumber)
                    putExtra("BUS_CAPACITY", bus.totalSeats)
                    putExtra("BUS_DRIVER", bus.driverName ?: "")
                    putExtra("BUS_ROUTE", bus.routeName ?: "")
                    putExtra("BUS_STATUS", bus.status)
                }
                startActivity(intent)
            },
            onStatusChanged = { bus, isChecked ->
                val isUnassigned = bus.routeName.isNullOrEmpty()
                val newStatus = when {
                    isUnassigned -> "UNASSIGNED"
                    isChecked -> "ACTIVE"
                    else -> "INACTIVE"
                }

                viewModel.updateBusDetails(bus.busNumber, bus.copy(status = newStatus))
            }
        )

        binding.rvBusesListing.apply {
            layoutManager = LinearLayoutManager(this@ManageBusesActivity)
            adapter = busAdapter
        }
    }

    private fun observeViewModelStreams() {
        viewModel.busList.observe(this) { standardList ->
            temporaryBusList.clear()
            temporaryBusList.addAll(standardList)
            // Header metric counter card items count update
            binding.tvTotalMetricCounter.text = temporaryBusList.size.toString()
            busAdapter.updateData(temporaryBusList)
        }
    }

    private fun showAddNewBusBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogBinding = DialogAddNewBusBinding.inflate(LayoutInflater.from(this))
        bottomSheetDialog.setContentView(dialogBinding.root)

        // Setup Route Dropdown from RouteRepository
        val routes = RouteRepository.routeList.value ?: listOf()
        val routeNames = routes.map { it.routeName }.toMutableList()
        routeNames.add(0, "Select Route")

        val routeAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, routeNames)
        dialogBinding.spinnerRouteSelection.setAdapter(routeAdapter)
        dialogBinding.spinnerRouteSelection.setText("Select Route", false)

        // Setup Driver Dropdown from DriverRepository
        val drivers = DriverRepository.driverList.value ?: listOf()
        val driverNames = drivers.map { it.name }.toMutableList()
        driverNames.add(0, "Select Driver")

        val driverAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, driverNames)
        dialogBinding.spinnerDriverSelection.setAdapter(driverAdapter)
        dialogBinding.spinnerDriverSelection.setText("Select Driver", false)

        // CANCEL BUTTON ACTIONS
        dialogBinding.btnCancelCross.setOnClickListener { bottomSheetDialog.dismiss() }
        dialogBinding.tvCancelFormAction.setOnClickListener { bottomSheetDialog.dismiss() }

        // SAVE BUTTON CLICK LOGIC
        dialogBinding.btnSaveBusSubmit.setOnClickListener {
            val busNo = dialogBinding.etBusNumberInput.text.toString().trim()
            val capacityStr = dialogBinding.etCapacityInput.text.toString().trim()
            val selectedRouteName = dialogBinding.spinnerRouteSelection.text.toString().trim()
            val selectedDriverName = dialogBinding.spinnerDriverSelection.text.toString().trim()

            if (busNo.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(this, "Bus Number and Capacity are required!", Toast.LENGTH_SHORT).show()
            } else {
                val capacity = capacityStr.toIntOrNull() ?: 0
                val routeValue = if (selectedRouteName == "Select Route") null else selectedRouteName
                val driverValue = if (selectedDriverName == "Select Driver") null else selectedDriverName

                // 1. Dependency Logic: Update Repositories if route/driver is selected
                assignBusToRouteAndDriver(busNo, routeValue ?: "", driverValue ?: "")

                // 2. New Bus Object
                val newBus = BusModel(
                    busNumber = busNo,
                    totalSeats = capacity,
                    driverName = driverValue,
                    routeName = routeValue,
                    status = if (routeValue != null) "ACTIVE" else "UNASSIGNED"
                )

                viewModel.addNewBus(newBus)
                Toast.makeText(this, "Bus Added Successfully!", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetDialog.show()
    }

    private fun assignBusToRouteAndDriver(busNo: String, routeName: String, driverName: String) {
        val finalRoute = if (routeName == "Select Route") "" else routeName
        val finalDriver = if (driverName == "Select Driver") "" else driverName

        // 1. Update Driver Repository
        if (finalDriver.isNotEmpty()) {
            val drivers = DriverRepository.driverList.value ?: listOf()
            val targetDriver = drivers.find { it.name == finalDriver }
            targetDriver?.let {
                DriverRepository.updateDriver(it.copy(assignedBus = busNo, route = finalRoute))
            }
        }

        // 2. Update Route Repository
        if (finalRoute.isNotEmpty()) {
            val allRoutes = RouteRepository.routeList.value ?: return
            val targetRoute = allRoutes.find { it.routeName == finalRoute }
            targetRoute?.let {
                RouteRepository.updateRoute(it.copy(busNo = busNo, driverName = finalDriver))
            }
        }
    }
}