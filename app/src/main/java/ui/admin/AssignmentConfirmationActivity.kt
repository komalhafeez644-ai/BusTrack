package ui.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.bustrack_app.databinding.ActivityAssignmentConfirmationBinding
import com.example.bustrack_app.viewmodels.AssignmentConfirmationViewModel

class AssignmentConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssignmentConfirmationBinding
    private lateinit var viewModel: AssignmentConfirmationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignmentConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(AssignmentConfirmationViewModel::class.java)

        observeViewModel()

        // Initial data load
        viewModel.loadAssignmentData()

        // Button Click Listeners
        binding.btnConfirm.setOnClickListener {
            viewModel.confirmAndNotify()
        }

        binding.btnEdit.setOnClickListener {
            viewModel.editAssignment()
        }
    }

    private fun observeViewModel() {
        // 1. Data Observer: UI update karne ke liye
        viewModel.assignmentDetail.observe(this) { data ->
            data?.let {
                binding.apply {
                    tvStudentName.text = it.studentName
                    tvStudentId.text = it.studentId // Screenshot mein sirf ID hai, "Student ID:" prefix hatana ho toh hata sakte hain

                    // Operational Details
                    tvBusNumber.text = it.busNumber
                    tvBusService.text = it.busServiceType

                    tvDriverName.text = it.driverName
                    tvDriverRole.text = it.driverRole

                    tvStopName.text = it.pickupStop
                    tvStopDetail.text = it.stopLocationDetail

                    // Progress & Coverage
                    tvCoveragePercentage.text = "${it.routeCoverage}%"
                    pbCoverage.progress = it.routeCoverage

                    // Logic & Time
                    tvOptimizationLogic.text = it.optimizationNote
                    tvEstimatedTime.text = it.estimatedPickup

                    // Agar aapne Status Chip lagayi hai (Successfully Assigned)
                    // binding.chipStatus.text = it.status
                }
            }
        }

        // 2. Action Observer: Buttons ke feedback ke liye (Jo maine ViewModel mein add kiya tha)
        viewModel.actionStatus.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}