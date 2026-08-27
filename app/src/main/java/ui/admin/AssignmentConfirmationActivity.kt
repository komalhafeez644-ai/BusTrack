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
                tvStudentId.text = application.studentIdString
                tvBusNumber.text = if (application.assignedBus.isNotEmpty()) application.assignedBus else "Bus ${application.bestRoute.split("-").lastOrNull() ?: "42"}"
                tvRouteCode.text = application.routeCode
                tvRouteName.text = application.bestRoute
                tvStopName.text = application.nearestStop
                tvCoveragePercentage.text = application.matchPercent
                // pbCoverage expects Int
                val progress = application.matchPercent.replace("%", "").replace(" Match", "").toIntOrNull() ?: 95
                pbCoverage.progress = progress
                
                // Set logic note
                tvOptimizationLogic.text = "System matched ${application.studentName} with ${application.bestRoute} (${application.routeCode}) based on proximity to ${application.nearestStop}. This route is served by ${application.assignedBus}."
            
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
            if (it.studentIdString.isEmpty()) {
                Toast.makeText(this, "Error: Student ID missing", Toast.LENGTH_SHORT).show()
                return
            }
            
            StudentRepository.assignRouteToStudent(
                it.studentIdString,
                it.bestRoute,
                com.example.bustrack_app.data.RouteRepository.getBusForRoute(it.bestRoute),
                it.nearestStop
            ) { success ->
                if (success) {
                    showSuccessDialog()
                    viewModel.confirmAndNotify()
                } else {
                    Toast.makeText(this, "Failed to save assignment in database", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun refreshFromSource() {
        val application = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
        
        application?.let {
            binding.tvStudentName.text = it.studentName
            binding.tvBusNumber.text = if (it.assignedBus.isNotEmpty()) it.assignedBus else "Bus ${it.bestRoute.split("-").lastOrNull() ?: "42"}"
            binding.tvRouteCode.text = it.routeCode
            binding.tvRouteName.text = it.bestRoute
            binding.tvStopName.text = it.nearestStop
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

                    tvRouteName.text = it.routeName

                    tvStopName.text = it.pickupStop

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
