package ui.parent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.StudentAdapter
import com.example.bustrack_app.data.ParentRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityParentProfileBinding
import com.example.bustrack_app.viewmodels.ProfileViewModel
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch
import utils.FormUtils
import utils.ViewUtils

class ParentProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private val parentRepository = ParentRepository()
    private lateinit var studentAdapter: StudentAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupRecyclerView()
        setupObservers()
        setupClickListeners()
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(
            students = listOf(),
            showActionButtons = false 
        )
        binding.rvChildren.layoutManager = LinearLayoutManager(this)
        binding.rvChildren.adapter = studentAdapter
    }

    private fun setupObservers() {
        // Parent Data Observer (Personal Details from original registration/form)
        viewModel.parentData.observe(this) { data ->
            if (data != null) {
                binding.tvParentName.text = data.name
                binding.tvPhone.text = data.phone.ifEmpty { "Not Provided" }
                // Use email from Admin/Account data as it's the login account
                viewModel.adminData.value?.let { admin ->
                    binding.tvEmail.text = admin.email
                    binding.tvAddress.text = admin.address.ifEmpty { "Not Provided" }
                    if (admin.profileImageUrl.isNotEmpty()) {
                        Glide.with(this).load(admin.profileImageUrl).placeholder(R.drawable.ic_person).into(binding.imgProfile)
                    }
                }
            } else {
                // Fallback to Admin data if Parent data doesn't exist yet
                viewModel.adminData.value?.let { admin ->
                    binding.tvParentName.text = admin.fullName
                    binding.tvEmail.text = admin.email
                    binding.tvPhone.text = admin.phone.ifEmpty { "Not Provided" }
                    binding.tvAddress.text = admin.address.ifEmpty { "Not Provided" }
                    if (admin.profileImageUrl.isNotEmpty()) {
                        Glide.with(this).load(admin.profileImageUrl).placeholder(R.drawable.ic_person).into(binding.imgProfile)
                    }
                }
            }
            updateChildrenStats()
        }

        viewModel.adminData.observe(this) { admin ->
            if (viewModel.parentData.value == null) {
                binding.tvParentName.text = admin.fullName
                binding.tvEmail.text = admin.email
                binding.tvPhone.text = admin.phone.ifEmpty { "Not Provided" }
                binding.tvAddress.text = admin.address.ifEmpty { "Not Provided" }
            } else {
                binding.tvEmail.text = admin.email
                binding.tvAddress.text = admin.address.ifEmpty { "Not Provided" }
            }
            
            if (admin.profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(admin.profileImageUrl).placeholder(R.drawable.ic_person).into(binding.imgProfile)
            }
        }

        viewModel.trackingRequests.observe(this) {
            updateChildrenStats()
        }
        
        StudentRepository.studentList.observe(this) { 
            updateChildrenStats()
        }
    }

    private fun updateChildrenStats() {
        val allStudents = StudentRepository.studentList.value ?: return
        val requests = viewModel.trackingRequests.value ?: return
        
        // 1. Total Children: Only those with tracking enabled
        val trackingEnabledStudentIds = requests.filter { 
            it.status.equals("APPROVED", true) && it.trackingEnabled 
        }.map { it.studentId }.toSet()

        val myTrackingChildren = allStudents.filter { trackingEnabledStudentIds.contains(it.id) }
        
        studentAdapter.setStudents(myTrackingChildren)
        binding.tvTotalChildren.text = myTrackingChildren.size.toString()

        // 2. Active Buses: Count only unique buses assigned to those tracking-enabled children
        val activeBuses = myTrackingChildren.mapNotNull { it.busNo }.filter { it.isNotEmpty() }.distinct()
        binding.tvActiveBuses.text = activeBuses.size.toString()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }
        
        binding.btnEditProfile.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, EditParentProfileActivity::class.java))
        }

        binding.btnAddMoreChild.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            showAddChildBottomSheet()
        }
    }

    private fun showAddChildBottomSheet() {
        val bottomSheetDialog = BottomSheetDialog(this, R.style.TransparentBottomSheetDialog)
        val view = layoutInflater.inflate(R.layout.layout_add_more_child_bottom_sheet, binding.root, false)
        bottomSheetDialog.setContentView(view)

        val etChildName = view.findViewById<EditText>(R.id.etChildName)
        val etStudentId = view.findViewById<EditText>(R.id.etStudentId)
        val tilChildName = view.findViewById<TextInputLayout>(R.id.tilChildName)
        val tilStudentId = view.findViewById<TextInputLayout>(R.id.tilStudentId)
        val btnAdd = view.findViewById<View>(R.id.btnAddChild)

        // Reuse Student ID formatting
        FormUtils.setupStudentIdFormatting(etStudentId)

        btnAdd.setOnClickListener {
            ViewUtils.applyPressEffect(it)
            val name = etChildName.text.toString().trim()
            val studentId = etStudentId.text.toString().trim()

            var isValid = true
            if (name.isEmpty()) {
                tilChildName.error = "Name required"
                isValid = false
            } else tilChildName.error = null

            if (studentId.isEmpty() || studentId.length < 7) {
                tilStudentId.error = "Enter valid Student ID (GCW-XXX)"
                isValid = false
            } else tilStudentId.error = null

            if (isValid) {
                lifecycleScope.launch {
                    btnAdd.isEnabled = false
                    
                    // Reuse existing parent data from ViewModel
                    val parent = viewModel.parentData.value
                    val admin = viewModel.adminData.value
                    
                    if (parent != null && admin != null) {
                        val (success, error) = parentRepository.submitTrackingRequest(
                            studentId = studentId,
                            parentName = parent.name,
                            phone = parent.phone,
                            relationship = parent.relationship
                        )

                        if (success) {
                            Toast.makeText(this@ParentProfileActivity, "Request submitted for $name", Toast.LENGTH_SHORT).show()
                            bottomSheetDialog.dismiss()
                        } else {
                            btnAdd.isEnabled = true
                            Toast.makeText(this@ParentProfileActivity, "Error: $error", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        btnAdd.isEnabled = true
                        Toast.makeText(this@ParentProfileActivity, "Parent profile data missing", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        bottomSheetDialog.show()
    }
}
