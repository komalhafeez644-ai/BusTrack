package ui.admin

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.bustrack_app.databinding.ActivityEditAssignmentBinding
import com.example.bustrack_app.viewmodels.EditAssignmentViewModel

class EditAssignmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAssignmentBinding
    private lateinit var viewModel: EditAssignmentViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAssignmentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(EditAssignmentViewModel::class.java)

        val applicationData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
        
        if (applicationData != null) {
            populateData(applicationData)
        } else {
            // Initial data load (Fallback or default)
            viewModel.loadCurrentAssignment()
        }

        setupDropdowns()
        observeViewModel()

        // Button Click Listeners
        binding.btnUpdate.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            showSuccessDialog()
            viewModel.updateAndNotify()
        }

        binding.btnCancel.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish() // Screen band karke pichli screen par wapas jane ke liye
        }
        
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }
    }

    private fun showSuccessDialog() {
        saveChanges()
        val dialog = android.app.Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(com.example.bustrack_app.R.layout.dialog_success)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))

        val btnDone = dialog.findViewById<com.google.android.material.button.MaterialButton>(com.example.bustrack_app.R.id.btnDone)
        btnDone.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun saveChanges() {
        val applicationData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
        applicationData?.let { currentItem ->
            val updatedRoute = binding.spinnerRoute.text.toString()
            val updatedBus = binding.spinnerBus.text.toString()
            val updatedStop = binding.etPickupStop.text.toString()

            // Find and update in global list
            val index = BusApplicationsActivity.applicationsList.indexOfFirst { it.id == currentItem.id }
            if (index != -1) {
                val oldItem = BusApplicationsActivity.applicationsList[index]
                val newItem = oldItem.copy(
                    bestRoute = updatedRoute, // Saving the Route Name here
                    pickupPoint = updatedStop,
                    nearestStop = updatedStop.split(" - ").first()
                )
                BusApplicationsActivity.applicationsList[index] = newItem
            }
        }
    }

    private fun populateData(item: com.example.bustrack_app.models.ApplicationModel) {
        binding.apply {
            tvStudentName.text = item.studentName
            tvStudentDetails.text = "ID #BT-${item.id} • ${item.studentClass}"
            
            if (item.image != 0) {
                ivStudent.setImageResource(item.image)
            } else {
                ivStudent.setImageResource(com.example.bustrack_app.R.drawable.ic_person) // General Icon
            }
            
            tvBusNumber.text = item.bestRoute
            tvStopName.text = item.nearestStop
            tvArrivalTime.text = "07:30 AM" // Placeholder or add to model

            // Initial selection for dropdowns
            spinnerRoute.setText(item.bestRoute, false)
            
            val mappedBus = com.example.bustrack_app.data.RouteRepository.getBusForRoute(item.bestRoute)
            if (mappedBus.isNotEmpty()) {
                spinnerBus.setText(mappedBus, false)
                inputBus.isEnabled = false
                spinnerBus.isEnabled = false
            } else {
                spinnerBus.setText("No Bus Assigned", false)
            }

            etPickupStop.setText(item.pickupPoint, false)
        }
    }

    private fun setupDropdowns() {
        // Route Selection Dropdown Data from Global Repository
        val globalRoutes = com.example.bustrack_app.data.RouteRepository.routeList.value ?: listOf()
        val routeStrings = globalRoutes.map { it.routeName }.toTypedArray()
        
        val routeAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, routeStrings)
        binding.spinnerRoute.setAdapter(routeAdapter)

        // Bus Dropdown Logic (Read-only after selection)
        binding.spinnerRoute.setOnItemClickListener { parent, view, position, id ->
            val selectedRouteName = routeStrings[position]
            val assignedBusNo = com.example.bustrack_app.data.RouteRepository.getBusForRoute(selectedRouteName)
            
            if (assignedBusNo.isNotEmpty()) {
                binding.spinnerBus.setText(assignedBusNo, false)
                // Lock the bus dropdown
                binding.spinnerBus.isEnabled = false
                binding.inputBus.isEnabled = false
                Toast.makeText(this, "Bus $assignedBusNo is automatically assigned to $selectedRouteName", Toast.LENGTH_SHORT).show()
            } else {
                binding.spinnerBus.setText("No Bus Assigned", false)
                binding.spinnerBus.isEnabled = false
                binding.inputBus.isEnabled = false
            }
        }

        // Initially setup for Bus dropdown (Make it look non-editable)
        binding.inputBus.isEnabled = false
        binding.spinnerBus.isEnabled = false

        // Pickup Stop Dropdown Data
        val stops = arrayOf(
            "Oakwood Square - Gate 3",
            "Green Park - Sector 4",
            "Sunrise Heights - Main Entry",
            "Blue Tower - North Wing",
            "Central Hub - Platform 2"
        )
        val stopAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, stops)
        binding.etPickupStop.setAdapter(stopAdapter)
    }

    private fun observeViewModel() {
        viewModel.editData.observe(this) { data ->
            data?.let {
                binding.apply {
                    // Student Top Card
                    tvStudentName.text = it.studentName
                    tvStudentDetails.text = "ID ${it.studentId} • ${it.studentYear}"
                    
                    ivStudent.setImageResource(com.example.bustrack_app.R.drawable.ic_person)

                    // Current Assignment Card
                    tvBusNumber.text = it.currentBus
                    tvStopName.text = it.currentStop
                    tvArrivalTime.text = it.currentArrivalTime

                    // Re-assignment Section (Default values set karna)
                    etPickupStop.setText("Oakwood Square - Gate 3")
                }
            }
        }
    }
}