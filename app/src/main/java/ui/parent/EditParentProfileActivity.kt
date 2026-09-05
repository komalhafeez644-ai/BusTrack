package ui.parent

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityEditParentProfileBinding
import com.example.bustrack_app.viewmodels.ProfileViewModel

class EditParentProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditParentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditParentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupObservers()
        setupClickListeners()
    }

    private fun setupObservers() {
        // Load name from parentData (Father/Parent Name from form)
        viewModel.parentData.observe(this) { parent ->
            if (parent != null) {
                binding.etFullName.setText(parent.name)
            }
        }

        viewModel.adminData.observe(this) { data ->
            // Use account name as fallback only if parentData not yet loaded
            if (viewModel.parentData.value == null) {
                binding.etFullName.setText(data.fullName)
            }
            
            binding.etEmail.setText(data.email)
            binding.etPhone.setText(data.phone)
            binding.etAddress.setText(data.address)
            if (data.profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(data.profileImageUrl).placeholder(R.drawable.ic_person).into(binding.imgProfile)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }
        
        binding.btnCancel.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        binding.btnSave.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            
            val userData = mapOf(
                "fullName" to binding.etFullName.text.toString().trim(),
                "email" to binding.etEmail.text.toString().trim(),
                "phone" to binding.etPhone.text.toString().trim(),
                "address" to binding.etAddress.text.toString().trim(),
                "city" to binding.etCity.text.toString().trim()
            )

            if (userData["fullName"].toString().isEmpty()) {
                binding.etFullName.error = "Full Name is required"
                return@setOnClickListener
            }

            binding.btnSave.isEnabled = false
            viewModel.updateProfile(userData) { success, message ->
                binding.btnSave.isEnabled = true
                if (success) {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error: $message", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnUpdatePhoto.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            Toast.makeText(this, "Opening Gallery...", Toast.LENGTH_SHORT).show()
        }
    }
}