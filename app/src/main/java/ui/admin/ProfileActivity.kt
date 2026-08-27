package ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityProfileBinding
import com.example.bustrack_app.viewmodels.ProfileViewModel
import utils.NavigationUtils
import com.bumptech.glide.Glide

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.parseColor("#F8FAFC")
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        setupObservers()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        NavigationUtils.setupBottomNavigation(this)
    }

    private fun setupObservers() {
        viewModel.adminData.observe(this) { admin ->
            val name = admin.fullName.ifEmpty { "System Admin" }
            val empId = admin.employeeId.ifEmpty { "ADMIN-2024-001" }
            val dept = "Transport" // Fixed as per requirement
            
            binding.tvAdminName.text = name
            binding.tvInfoFullName.text = name
            binding.tvInfoEmail.text = admin.email.ifEmpty { "admin@gmail.com" }
            binding.tvInfoPhone.text = admin.phone.ifEmpty { "+92 300 1234567" }
            binding.tvInfoDept.text = dept
            binding.tvInfoEmpID.text = empId

            // Load Image from URL
            if (admin.profileImageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(admin.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.imgProfile)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditAdminProfileActivity::class.java)
            startActivity(intent)
        }
    }
}