package ui.admin

import android.app.Dialog
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import com.example.bustrack_app.R
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityAssignmentConfirmationBinding
import com.example.bustrack_app.viewmodels.AssignmentConfirmationViewModel
import com.google.android.material.button.MaterialButton

class AssignmentConfirmationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAssignmentConfirmationBinding
    private lateinit var viewModel: AssignmentConfirmationViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssignmentConfirmationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this).get(AssignmentConfirmationViewModel::class.java)

        observeViewModel()

        // Get dynamic data from Intent
        val application = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
        if (application != null) {
            // Populate UI with the system suggested data from previous screen
            binding.apply {
                tvStudentName.text = application.studentName
                tvStudentId.text = "#SF-${1000 + application.id}"
                tvBusNumber.text = "Bus ${application.bestRoute.split("-").lastOrNull() ?: "42"}"
                tvStopName.text = application.nearestStop
                tvCoveragePercentage.text = application.matchPercent
                // pbCoverage expects Int
                val progress = application.matchPercent.replace("%", "").toIntOrNull() ?: 95
                pbCoverage.progress = progress
                
                // Set logic note
                tvOptimizationLogic.text = "System optimized this assignment based on student's proximity to ${application.nearestStop} and ${application.bestRoute} capacity."
            
                if (application.image != 0) {
                    ivProfile.setImageResource(application.image)
                }
            }
        } else {
            // Initial fallback data load
            viewModel.loadAssignmentData()
        }
        
        // Ensure detail fields are updated if data changes
        refreshFromSource()

        // Button Click Listeners
        binding.btnConfirm.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)

            // Change Style
            binding.btnConfirm.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryDark))
            binding.btnConfirm.setTextColor(ContextCompat.getColor(this, R.color.white))
            
            saveToRepository()
            showSuccessDialog()
            viewModel.confirmAndNotify()
        }

        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            onBackPressedDispatcher.onBackPressed()
        }

        binding.btnEdit.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)

            // Change Style (Solid Primary Blue with White Text)
            val editBtn = binding.btnEdit as? MaterialButton
            editBtn?.let {
                it.backgroundTintList = ColorStateList.valueOf(ContextCompat.getColor(this, R.color.primaryBlue))
                it.setTextColor(ContextCompat.getColor(this, R.color.white))
                it.strokeWidth = 0
            }

            // Open Edit Assignment Screen
            val applicationData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
            val intent = android.content.Intent(this, EditAssignmentActivity::class.java)
            intent.putExtra("APPLICATION_DATA", applicationData)
            startActivity(intent)
            
            viewModel.editAssignment()
        }
    }

    private fun saveToRepository() {
        val originalData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
        originalData?.let {
            StudentRepository.assignRouteToStudent(
                it.studentName,
                it.bestRoute,
                com.example.bustrack_app.data.RouteRepository.getBusForRoute(it.bestRoute),
                it.nearestStop
            )
        }
    }

    private fun refreshFromSource() {
        val originalData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
        val application = BusApplicationsActivity.applicationsList.find { it.id == originalData?.id } ?: originalData
        
        application?.let {
            binding.tvStudentName.text = it.studentName
            val busNum = it.bestRoute.split("-").lastOrNull()?.trim() ?: it.bestRoute
            binding.tvBusNumber.text = "Bus $busNum"
            binding.tvStopName.text = it.nearestStop

            // Global Bus list se Driver Name uthana
            val globalBuses = com.example.bustrack_app.data.BusRepository.busList.value
            val actualBus = globalBuses?.find { bus -> bus.busNumber.contains(busNum, ignoreCase = true) }
            
            actualBus?.let { bus ->
                binding.tvDriverName.text = bus.driverName ?: "No Driver Assigned"
            }
        }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_success)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val btnDone = dialog.findViewById<MaterialButton>(R.id.btnDone)
        btnDone.setOnClickListener {
            dialog.dismiss()
            finish()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        // 1. Data Observer: UI update karne ke liye
        viewModel.assignmentDetail.observe(this) { data ->
            data?.let {
                binding.apply {
                    tvStudentName.text = it.studentName
                    tvStudentId.text = it.studentId

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
                }
            }
        }

        // 2. Action Observer: Buttons ke feedback ke liye (Jo maine ViewModel mein add kiya tha)
        viewModel.actionStatus.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
}
