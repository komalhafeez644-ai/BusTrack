package ui.admin

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivityEditAdminProfileBinding

class EditAdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAdminProfileBinding

    // Gallery Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.imgProfile.setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAdminProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadData()
    }

    private fun loadData() {
        // Yahan data pre-fill hoga
        binding.etFullName.setText("John Admin")
        binding.etEmail.setText("admin@punjabcollege.edu")
        binding.etPhone.setText("+92 300 1234567")
        binding.etDept.setText("Logistics")
        binding.etEmpId.setText("CF-ADM-24")
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener { finish() }

        // Camera Icon click for gallery
        binding.imgCamera.setOnClickListener {
            pickImage.launch("image/*")
        }

        // Save Button logic
        binding.btnSave.setOnClickListener {
            val name = binding.etFullName.text.toString()
            // Yahan API/DB update call karein
            Toast.makeText(this, "Changes saved for $name", Toast.LENGTH_SHORT).show()
            finish() // Update ke baad wapas pichli screen par
        }

        binding.btnCancel.setOnClickListener { finish() }
    }
}