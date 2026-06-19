package ui.parent

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.StudentAdapter
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityParentProfileBinding
import com.example.bustrack_app.viewmodels.ProfileViewModel

class ParentProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
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
        // We set showActionButtons = false because parents shouldn't see Admin-only action buttons
        studentAdapter = StudentAdapter(
            students = listOf(),
            showActionButtons = false 
        )
        binding.rvChildren.layoutManager = LinearLayoutManager(this)
        binding.rvChildren.adapter = studentAdapter
    }

    private fun setupObservers() {
        // Parent Data Observer
        viewModel.adminData.observe(this) { data ->
            binding.tvParentName.text = data.fullName
            binding.tvParentId.text = "PARENT ID: ${data.id.takeLast(6).uppercase()}"
            binding.tvEmail.text = data.email
            binding.tvPhone.text = data.phone.ifEmpty { "Not Provided" }
            binding.tvAddress.text = data.address.ifEmpty { "Not Provided" }

            if (data.profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(data.profileImageUrl).placeholder(R.drawable.ic_person).into(binding.imgProfile)
            }
            
            // Refresh children list based on loaded parent name
            filterChildren(data.fullName)
        }
        
        // Repository Observer
        StudentRepository.studentList.observe(this) { 
            filterChildren(binding.tvParentName.text.toString())
        }
    }

    private fun filterChildren(parentName: String) {
        val allStudents = StudentRepository.studentList.value ?: return
        
        // Flexible filter: try exact match first, then fall back to sample data for demo if needed
        var myChildren = allStudents.filter { it.fatherName.equals(parentName, true) && parentName.isNotEmpty() }
        
        if (myChildren.isEmpty()) {
            // For demo/testing: Show some students anyway if the list is empty
            myChildren = allStudents.filter { it.name.contains("Ali", true) || it.name.contains("Rohan", true) || it.id.contains("9921") }
        }
        
        studentAdapter.setStudents(myChildren)
        binding.tvTotalChildren.text = myChildren.size.toString()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }
        
        binding.btnEditProfile.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, EditParentProfileActivity::class.java))
        }
    }
}