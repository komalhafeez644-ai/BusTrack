package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.BusAdapter
import com.example.bustrack_app.data.BusRepository
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.databinding.ActivityManageBusesBinding
import com.example.bustrack_app.databinding.DialogAddNewBusBinding
import com.example.bustrack_app.models.BusModel
import com.example.bustrack_app.viewmodels.BusViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton

class ManageBusesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageBusesBinding
    private lateinit var viewModel: BusViewModel
    private lateinit var busAdapter: BusAdapter

    private val temporaryBusList = mutableListOf<BusModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageBusesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[BusViewModel::class.java]

        DriverRepository.driverList.observe(this) { }
        RouteRepository.routeList.observe(this) { }

        setupRecyclerViewList()
        observeViewModelStreams()

        binding.btnFloatingAddBus.setOnClickListener {
            showAddNewBusBottomSheet()
        }

        binding.btnBackArrow.setOnClickListener {
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        BusRepository.refreshBusList()
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
                val isCurrentlyActive = bus.status.equals("ACTIVE", ignoreCase = true)
                
                if (isChecked != isCurrentlyActive) {
                    showCustomConfirmDialog(
                        title = if (isChecked) "Enable Bus?" else "Disable Bus?",
                        message = if (isChecked) {
                            "Are you sure you want to enable ${bus.busNumber}? It will become available for tracking and assignments."
                        } else {
                            "This bus is currently assigned to ${bus.driverName ?: "a driver"}. Disabling it will make it unavailable for operations."
                        },
                        iconRes = if (isChecked) R.drawable.directions_bus else R.drawable.warning,
                        confirmText = if (isChecked) "Enable" else "Disable",
                        onConfirm = {
                            viewModel.updateBusDetails(bus.busNumber, bus.copy(status = if (isChecked) "ACTIVE" else "INACTIVE"))
                        },
                        onCancel = {
                            // Revert switch UI
                            busAdapter.notifyDataSetChanged()
                        }
                    )
                }
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
            binding.tvTotalMetricCounter.text = temporaryBusList.size.toString()
            busAdapter.updateData(temporaryBusList)
        }
    }

    private fun showAddNewBusBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this)
        val dialogBinding = DialogAddNewBusBinding.inflate(LayoutInflater.from(this))
        bottomSheetDialog.setContentView(dialogBinding.root)

        bottomSheetDialog.behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
        bottomSheetDialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        val routes = RouteRepository.routeList.value ?: listOf()
        val routeNames = routes.map { it.routeName }.toMutableList()
        routeNames.add(0, "None")

        val routeAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, routeNames)
        dialogBinding.spinnerRouteSelection.setAdapter(routeAdapter)
        dialogBinding.spinnerRouteSelection.setText("None", false)

        val drivers = DriverRepository.driverList.value ?: listOf()
        val driverNames = drivers.map { it.name }.toMutableList()
        driverNames.add(0, "None")

        val driverAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, driverNames)
        dialogBinding.spinnerDriverSelection.setAdapter(driverAdapter)
        dialogBinding.spinnerDriverSelection.setText("None", false)

        utils.FormUtils.setupUppercaseInput(dialogBinding.etBusNumberInput)

        dialogBinding.btnCancelCross.setOnClickListener { bottomSheetDialog.dismiss() }
        dialogBinding.tvCancelFormAction.setOnClickListener { bottomSheetDialog.dismiss() }

        dialogBinding.btnSaveBusSubmit.setOnClickListener {
            val busNo = dialogBinding.etBusNumberInput.text.toString().trim()
            val capacityStr = dialogBinding.etCapacityInput.text.toString().trim()
            val selectedRouteName = dialogBinding.spinnerRouteSelection.text.toString().trim()
            val selectedDriverName = dialogBinding.spinnerDriverSelection.text.toString().trim()

            if (busNo.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(this, "Bus Number and Capacity are required!", Toast.LENGTH_SHORT).show()
            } else {
                val capacity = capacityStr.toIntOrNull() ?: 0
                val routeValue = if (selectedRouteName == "None") null else selectedRouteName
                val driverValue = if (selectedDriverName == "None") null else selectedDriverName

                val allBuses = viewModel.busList.value ?: emptyList()
                
                if (driverValue != null && allBuses.any { it.driverName == driverValue }) {
                    val otherBus = allBuses.find { it.driverName == driverValue }
                    Toast.makeText(this, "Driver $driverValue is already assigned to Bus ${otherBus?.busNumber}", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                if (routeValue != null && allBuses.any { it.routeName == routeValue }) {
                    val otherBus = allBuses.find { it.routeName == routeValue }
                    Toast.makeText(this, "Route $routeValue is already assigned to Bus ${otherBus?.busNumber}", Toast.LENGTH_LONG).show()
                    return@setOnClickListener
                }

                assignBusToRouteAndDriver(busNo, routeValue ?: "", driverValue ?: "")

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
        val finalRoute = if (routeName == "None") "" else routeName
        val finalDriver = if (driverName == "None") "" else driverName

        if (finalDriver.isNotEmpty()) {
            val drivers = DriverRepository.driverList.value ?: listOf()
            val targetDriver = drivers.find { it.name == finalDriver }
            targetDriver?.let { driver ->
                driver.assignedBus?.let { oldBusNo ->
                    if (oldBusNo != busNo) {
                        BusRepository.getBusByNumber(oldBusNo)?.let { oldBus ->
                            BusRepository.updateBusDetails(oldBusNo, oldBus.copy(driverName = null))
                        }
                        RouteRepository.routeList.value?.find { it.busNo == oldBusNo }?.let { oldRoute ->
                            RouteRepository.updateRoute(oldRoute.copy(driverName = ""))
                        }
                    }
                }
                DriverRepository.updateDriver(driver.copy(assignedBus = busNo, route = finalRoute.ifEmpty { null }))
            }
        }

        if (finalRoute.isNotEmpty()) {
            val allRoutes = RouteRepository.routeList.value ?: return
            val targetRoute = allRoutes.find { it.routeName == finalRoute }
            targetRoute?.let {
                RouteRepository.updateRoute(it.copy(busNo = busNo, driverName = finalDriver))
            }
        }
    }

    private fun showCustomConfirmDialog(
        title: String,
        message: String,
        iconRes: Int,
        confirmText: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_confirm_status, null)
        
        val ivIcon = dialogView.findViewById<ImageView>(R.id.ivDialogIcon)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMsg = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)

        tvTitle.text = title
        tvMsg.text = message
        ivIcon.setImageResource(iconRes)
        
        if (confirmText == "Disable") {
            ivIcon.setColorFilter(android.graphics.Color.parseColor("#DC2626"))
            btnConfirm.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DC2626"))
        } else {
            ivIcon.setColorFilter(android.graphics.Color.parseColor("#2563EB"))
            btnConfirm.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2563EB"))
        }
        
        btnConfirm.text = confirmText

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        btnCancel.setOnClickListener {
            onCancel()
            dialog.dismiss()
        }

        btnConfirm.setOnClickListener {
            onConfirm()
            dialog.dismiss()
        }

        dialog.show()
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)
    }
}
