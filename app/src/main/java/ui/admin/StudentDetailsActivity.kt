package ui.admin

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityStudentDetailsBinding
import com.example.bustrack_app.viewmodels.StudentDetailsViewModel
import com.google.android.material.button.MaterialButton
import utils.ViewUtils

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
        
        // Observe student list to keep this screen in sync
        StudentRepository.studentList.observe(this) {
            viewModel.loadStudentDetails(currentStudentId)
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadStudentDetails(currentStudentId)
    }

    private fun observeData() {
        viewModel.studentDetails.observe(this) { data ->
            binding.tvStudentName.text = data.name
            binding.tvStudentId.text = "STUDENT ID: ${data.id}"
            binding.tvBadgeSemester.text = data.grade
            
            // Handle Unassigned cases for Transportation Details
            if (data.route.isNullOrEmpty()) {
                binding.tvRouteName.text = "Unassigned"
                binding.tvRouteName.setTextColor(Color.parseColor("#EF4444"))
                
                binding.tvAssignedStop.text = "Unassigned"
                binding.tvAssignedStop.setTextColor(Color.parseColor("#EF4444"))
                
                binding.tvBusNumber.text = "Unassigned"
                binding.tvBusNumber.setTextColor(Color.parseColor("#EF4444"))

                binding.tvStatusBadge.text = "UNASSIGNED"
                binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#EF4444"))
                binding.switchTransportStatus.visibility = View.GONE

                binding.btnAssignRouteAction.visibility = View.VISIBLE
            } else {
                binding.tvRouteName.text = data.route
                binding.tvRouteName.setTextColor(Color.BLACK)
                
                binding.tvAssignedStop.text = data.stopName ?: "Not Assigned"
                binding.tvAssignedStop.setTextColor(Color.BLACK)
                
                binding.tvBusNumber.text = data.busNo ?: "Not Assigned"
                binding.tvBusNumber.setTextColor(Color.BLACK)

                // Status Logic
                binding.switchTransportStatus.visibility = View.VISIBLE
                binding.switchTransportStatus.isChecked = data.isActive
                
                if (data.isActive) {
                    binding.tvStatusBadge.text = "ACTIVE"
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#22C55E")) // Green
                } else {
                    binding.tvStatusBadge.text = "INACTIVE"
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#64748B")) // Grey
                }

                binding.btnAssignRouteAction.visibility = View.GONE
            }
            
            binding.tvHomeAddress.text = data.location
            binding.tvFatherName.text = data.fatherName
            binding.tvPhoneNumber.text = data.phoneNumber
            
            // Image Loading logic
            if (data.profileImageUrl.isNotEmpty()) {
                com.bumptech.glide.Glide.with(this)
                    .load(data.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(binding.imgStudentProfile)
            } else if (data.profileImage != 0) {
                binding.imgStudentProfile.setImageResource(data.profileImage)
            } else {
                binding.imgStudentProfile.setImageResource(R.drawable.ic_person)
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

        binding.btnAssignRouteAction.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            viewModel.studentDetails.value?.let { student ->
                val intent = Intent(this, RouteAnalysisActivity::class.java)
                intent.putExtra("APPLICATION_DATA", com.example.bustrack_app.models.ApplicationModel(
                    id = student.id.filter { it.isDigit() }.toIntOrNull() ?: 0,
                    studentName = student.name,
                    studentClass = student.grade,
                    pickupPoint = student.location,
                    contactNumber = student.phoneNumber,
                    time = "Now",
                    status = "Pending",
                    image = student.profileImage,
                    profileImageUrl = student.profileImageUrl,
                    parentName = student.fatherName,
                    latitude = student.latitude,
                    longitude = student.longitude,
                    studentIdString = student.id
                ))
                startActivity(intent)
            }
        }

        binding.btnEditDetails.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val intent = Intent(this, EditStudentActivity::class.java)
            intent.putExtra("STUDENT_ID", currentStudentId)
            startActivity(intent)
        }

        binding.btnDeleteStudent.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            showDeleteConfirmationDialog()
        }

        binding.switchTransportStatus.setOnCheckedChangeListener { _, isChecked ->
            viewModel.studentDetails.value?.let { student ->
                val updatedStudent = student.copy(isActive = isChecked)
                StudentRepository.updateStudent(updatedStudent) { success ->
                    if (success) {
                        // The observer will update the UI
                        Toast.makeText(this, "Status updated to ${if (isChecked) "Active" else "Inactive"}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_restricted_action, null)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnOk)
        val tvTitle = dialogView.findViewById<TextView>(R.id.tvTitle)
        val tvMsg = dialogView.findViewById<TextView>(R.id.tvMessage)

        tvTitle.text = "Confirm Deletion"
        tvMsg.text = "Are you sure you want to delete this student? This action cannot be undone."
        btnConfirm.text = "Delete"
        btnConfirm.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#D32F2F"))

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        btnConfirm.setOnClickListener {
            currentStudentId?.let { id ->
                StudentRepository.deleteStudent(id) { success ->
                    if (success) {
                        Toast.makeText(this, "Student deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Failed to delete student", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            dialog.dismiss()
        }
    }
}