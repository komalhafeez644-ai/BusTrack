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

        setupDropdowns()
        observeViewModel()

        // Initial data load (Ethan Sterling ki details load karne ke liye)
        viewModel.loadCurrentAssignment()

        // Button Click Listeners
        binding.btnUpdate.setOnClickListener {
            viewModel.updateAndNotify()
            Toast.makeText(this, "Assignment Updated Successfully", Toast.LENGTH_SHORT).show()
        }

        binding.btnCancel.setOnClickListener {
            finish() // Screen band karke pichli screen par wapas jane ke liye
        }
    }

    private fun setupDropdowns() {
        // Bus Selection Dropdown Data
        val buses = arrayOf(
            "Bus 101 - Express North 12/40 seats",
            "Bus 42 - Sector 15 25/40 seats",
            "Bus 09 - City Center 30/40 seats"
        )
        val busAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, buses)
        binding.spinnerBus.setAdapter(busAdapter)

        // Route Selection Dropdown Data
        val routes = arrayOf(
            "Route 5 - City Express",
            "Route 12 - Highway Link",
            "Route 03 - Suburban Route"
        )
        val routeAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, routes)
        binding.spinnerRoute.setAdapter(routeAdapter)
    }

    private fun observeViewModel() {
        viewModel.editData.observe(this) { data ->
            data?.let {
                binding.apply {
                    // Student Top Card
                    tvStudentName.text = it.studentName
                    tvStudentDetails.text = "ID ${it.studentId} • ${it.studentYear}"
                    tvStudentInitial.text = it.studentName.take(2).uppercase() // Ethan Sterling -> ES

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