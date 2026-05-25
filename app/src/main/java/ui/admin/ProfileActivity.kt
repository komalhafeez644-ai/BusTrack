package ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivityProfileBinding
import com.example.bustrack_app.viewmodels.ProfileViewModel

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
        utils.NavigationUtils.setupBottomNavigation(this)
    }

    private fun setupObservers() {
        viewModel.adminData.observe(this) { admin ->
            binding.tvAdminName.text = admin.fullName
            binding.tvInfoFullName.text = admin.fullName
            binding.tvInfoEmail.text = admin.email
            binding.tvInfoDept.text = admin.department
            binding.tvInfoEmpID.text = admin.employeeId

            binding.switchBus.isChecked = admin.isBusDelayNotifyEnabled
            binding.switchEmergency.isChecked = admin.isEmergencyNotifyEnabled
            binding.switchDriver.isChecked = admin.isDriverNotifyEnabled
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { finish() }

        // EDIT BUTTON CLICKED - Navigating to Edit Profile
        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditAdminProfileActivity::class.java)
            startActivity(intent)
        }

        binding.switchBus.setOnCheckedChangeListener { _, isChecked ->
            viewModel.updateBusDelayNotification(isChecked)
        }

        binding.btnLogout.setOnClickListener {
            Toast.makeText(this, "Logging out...", Toast.LENGTH_SHORT).show()
        }
    }
}