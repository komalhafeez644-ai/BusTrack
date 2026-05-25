package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.adapter.BusAdapter
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

        utils.NavigationUtils.setupBottomNavigation(this)

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
                val isUnassigned = bus.driverName.isNullOrEmpty() || bus.routeName.isNullOrEmpty()
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

        // REQUIREMENT: Route select karne ki logic remove karni thi, to hum runtime layout fields ko completely hide kar dete hain
        // Is se aapko layout XML file se dropdown manually delete bhi nahi karna parega!
        // Note: spinnerRouteSelection dropdown se upar jo textview labels hain, unke paas direct unique IDs nahi thin, isliye layout structure ko maintain rakhte hue dropdown view control handle kiya gaya hai.
        dialogBinding.spinnerRouteSelection.parent.parent?.let { routeContainer ->
            if (routeContainer is View) {
                routeContainer.visibility = View.GONE
            }
        }
        // Route dropdown ke labels ko programmatic structure hide settings
        dialogBinding.spinnerRouteSelection.visibility = View.GONE

        // CANCEL BUTTON ACTIONS (Popup cross icon & text action handling)
        dialogBinding.btnCancelCross.setOnClickListener { bottomSheetDialog.dismiss() }
        dialogBinding.tvCancelFormAction.setOnClickListener { bottomSheetDialog.dismiss() }

        // SAVE BUTTON CLICK LOGIC (XML matching IDs)
        dialogBinding.btnSaveBusSubmit.setOnClickListener {
            val busNo = dialogBinding.etBusNumberInput.text.toString().trim()
            val capacityStr = dialogBinding.etCapacityInput.text.toString().trim()
            val selectedDriver = dialogBinding.spinnerDriverSelection.text.toString().trim()

            // 1. MUST VALIDATION: Bus number aur capacity mandatory filled honi chahiye
            if (busNo.isEmpty() || capacityStr.isEmpty()) {
                Toast.makeText(this, "Bus Number and Capacity are required!", Toast.LENGTH_SHORT).show()
            } else {
                val capacity = capacityStr.toIntOrNull() ?: 0

                // 2. OPTIONAL DRIVER LOGIC: Agar input empty hai to null pass hoga warna framework input content pass karega
                val driverValue = selectedDriver.ifEmpty { null }

                // 3. New Bus Object Data Matrix
                val newBus = BusModel(
                    busNumber = busNo,
                    totalSeats = capacity,
                    driverName = driverValue, // Optional Driver input string/null handle
                    routeName = null,         // Route option permanent disabled array configuration
                    status = "UNASSIGNED"     // Default status code
                )

                viewModel.addNewBus(newBus)

                // Button click event handling acknowledgment toast display
                Toast.makeText(this, "Bus Added Successfully!", Toast.LENGTH_SHORT).show()
                bottomSheetDialog.dismiss()
            }
        }

        bottomSheetDialog.show()
    }
}