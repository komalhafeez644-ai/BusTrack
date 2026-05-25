package ui.admin

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import com.example.bustrack_app.R
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityStudentDetailsBinding
import com.example.bustrack_app.viewmodels.StudentDetailsViewModel

class StudentDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDetailsBinding
    private val viewModel: StudentDetailsViewModel by viewModels()
    private var currentStudentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        currentStudentId = intent.getStringExtra("STUDENT_ID")

        observeData()
        setupClickListeners()

        viewModel.loadStudentDetails(currentStudentId)
    }

    private fun observeData() {
        viewModel.studentDetails.observe(this) { data ->
            binding.tvStudentName.text = data.name
            binding.tvStudentId.text = "STUDENT ID: ${data.id}"
            binding.tvBadgeSemester.text = data.grade
            binding.tvRouteName.text = data.route ?: "Not Assigned"
            binding.tvAssignedStop.text = data.location
            binding.tvBusNumber.text = data.busNo ?: "Not Assigned"
            binding.tvFatherName.text = data.fatherName
            binding.tvPhoneNumber.text = data.phoneNumber


            if (data.profileImage != 0) {
                binding.imgStudentProfile.setImageResource(data.profileImage)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivCallAction.setOnClickListener {
            val phoneNum = binding.tvPhoneNumber.text.toString()
            Toast.makeText(this, "Calling Parent: $phoneNum", Toast.LENGTH_SHORT).show()
        }

        binding.btnEditDetails.setOnClickListener {
            showEditTransportDialog()
        }
    }

    private fun showEditTransportDialog() {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_transport, null)
        dialog.setContentView(view)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val spinnerRoute = view.findViewById<Spinner>(R.id.dialogSpinnerRoute)
        val spinnerStop = view.findViewById<Spinner>(R.id.dialogSpinnerStop)
        val tvBus = view.findViewById<TextView>(R.id.dialogTvBus)
        val btnCancel = view.findViewById<AppCompatButton>(R.id.btnDialogCancel)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnDialogSave)

        // Load Routes from Repository
        val routes = RouteRepository.routeList.value ?: listOf()
        val routeNames = routes.map { it.routeName }
        
        val routeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routeNames)
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRoute.adapter = routeAdapter

        // Bus selection based on route
        spinnerRoute.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedRouteName = routeNames[position]
                val busNo = RouteRepository.getBusForRoute(selectedRouteName)
                
                tvBus.text = if (busNo.isNotEmpty()) busNo else "No Bus Assigned"
                
                // Update Stops based on selected Route
                val selectedRoute = routes.find { it.routeName == selectedRouteName }
                val stopNames = selectedRoute?.stopsList?.map { it.stopName } ?: listOf("Main Stop")
                val stopAdapter = ArrayAdapter(this@StudentDetailsActivity, android.R.layout.simple_spinner_item, stopNames)
                stopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerStop.adapter = stopAdapter
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        btnSave.setOnClickListener {
            val selectedRoute = spinnerRoute.selectedItem.toString()
            val selectedStop = spinnerStop.selectedItem.toString()
            val busNo = tvBus.text.toString()

            // Save to Repository
            StudentRepository.assignRouteToStudent(
                binding.tvStudentName.text.toString(),
                selectedRoute,
                busNo,
                selectedStop
            )

            Toast.makeText(this, "Transport updated successfully", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
            viewModel.loadStudentDetails(currentStudentId) // Refresh UI
        }

        dialog.show()
    }
}
