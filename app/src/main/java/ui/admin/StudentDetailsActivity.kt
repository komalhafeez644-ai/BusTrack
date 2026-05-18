package com.example.bustrack_app.ui.admin

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityStudentDetailsBinding
import com.example.bustrack_app.viewmodels.StudentDetailsViewModel

class StudentDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStudentDetailsBinding

    // Explicit type specify kar di taake "Cannot infer type" ka error khatam ho jaye
    private val viewModel: StudentDetailsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityStudentDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val receivedStudentId = intent.getStringExtra("STUDENT_ID")

        observeData()
        setupClickListeners()

        viewModel.loadStudentDetails(receivedStudentId)
    }

    private fun observeData() {
        viewModel.studentDetails.observe(this) { data ->
            // Sahi fields (StudentModel) ko XML IDs ke saath map kar diya
            binding.tvStudentName.text = data.name
            binding.tvStudentId.text = "STUDENT ID: ${data.id}"
            binding.tvBadgeSemester.text = data.grade
            binding.tvRouteName.text = data.route ?: "Not Assigned"
            binding.tvAssignedStop.text = data.location
            binding.tvBusNumber.text = data.busNo ?: "Not Assigned"
            binding.tvFatherName.text = data.fatherName
            binding.tvPhoneNumber.text = data.phoneNumber
            binding.tvPickupTime.text = data.pickupTime
            binding.tvInsuranceStatus.text = data.insuranceStatus

            binding.imgStudentProfile.setImageResource(data.profileImage)
        }
    }

    private fun setupClickListeners() {
        // Deprecated onBackPressed() ki jagah naya safe tareeqa
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.ivCallAction.setOnClickListener {
            val phoneNum = binding.tvPhoneNumber.text.toString()
            Toast.makeText(this, "Calling Parent: $phoneNum", Toast.LENGTH_SHORT).show()
        }

        // --- Edit Details Button Touch Listener (XML Text Color Error ka permanent hal) ---
        binding.btnEditDetails.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Jab user click kare (dabaaye rakhay) -> Text Color White ho jaye
                    binding.btnEditDetails.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // Jab user click chhor de -> Text Color wapas primaryBlue ho jaye
                    binding.btnEditDetails.setTextColor(ContextCompat.getColor(this, R.color.primaryBlue))
                }
            }
            false // Isay false rakhna zaroori hai taake niche wala onClickListener bhi chal sakay
        }

        // --- Edit Details Button Click Listener (Dialog Open karne ke liye) ---
        binding.btnEditDetails.setOnClickListener {
            showEditTransportDialog()
        }
    }

    // --- Edit Transport Assignment Dialog Function ---
    private fun showEditTransportDialog() {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_edit_transport, null)
        dialog.setContentView(view)

        // Dialog ke corners rounded dikhane ke liye background transparent hona zaroori hai
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Dialog ke andar ke components/views ko find karna
        val spinnerRoute = view.findViewById<Spinner>(R.id.dialogSpinnerRoute)
        val spinnerStop = view.findViewById<Spinner>(R.id.dialogSpinnerStop)
        val spinnerBus = view.findViewById<Spinner>(R.id.dialogSpinnerBus)
        val btnCancel = view.findViewById<AppCompatButton>(R.id.btnDialogCancel)
        val btnSave = view.findViewById<AppCompatButton>(R.id.btnDialogSave)

        // 👇 Aapki requirement ke mutabik options yahan direct code mein add kar diye hain
        val routes = arrayOf("Route 1", "Route 2", "Route 3", "Route 4")
        val stops = arrayOf("Stop 1", "Stop 2", "Stop 3", "Stop 4")
        val buses = arrayOf("Bus 42", "Bus 18", "Bus 05")

        // Dropdown Spinners ke upar adapters set karna taake options sahi se kaam karein
        val routeAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, routes)
        routeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerRoute.adapter = routeAdapter

        val stopAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stops)
        stopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStop.adapter = stopAdapter

        val busAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, buses)
        busAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerBus.adapter = busAdapter

        // Cancel button ki logic (sirf dialog close karega)
        btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        // Save Changes button ki logic (Data save karke pehli screen par wapas le jayega)
        btnSave.setOnClickListener {
            val selectedRoute = spinnerRoute.selectedItem.toString()
            val selectedStop = spinnerStop.selectedItem.toString()
            val selectedBus = spinnerBus.selectedItem.toString()

            Toast.makeText(this, "Saved: $selectedRoute, $selectedStop, $selectedBus", Toast.LENGTH_SHORT).show()

            dialog.dismiss() // Dialog band ho jayega
            finish()        // Ye screen close ho jayegi aur user direct pehli screen (ManageStudentActivity) par aa jayega
        }

        dialog.show()
    }
}