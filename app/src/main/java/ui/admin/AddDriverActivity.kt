package ui.admin

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityAddDriverBinding
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.data.DriverRepository
import utils.FormUtils
import utils.StorageUtils
import utils.ViewUtils

class AddDriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddDriverBinding
    private var selectedImageUri: Uri? = null

    // Photo Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imgUpload.setImageURI(it)
            binding.imgUpload.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupFormFormatting()

        binding.btnAddDriver.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (validateForm()) {
                if (selectedImageUri != null) {
                    uploadAndSave()
                } else {
                    saveDriver("")
                }
            }
        }

        binding.btnBack.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            finish() 
        }
        
        binding.btnCancel.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            finish() 
        }

        binding.btnPickImage.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            pickImageLauncher.launch("image/*")
        }
    }

    private fun setupFormFormatting() {
        FormUtils.setupUppercaseInput(binding.etEmployeeId)
        FormUtils.setupTitleCaseInput(binding.etFullName)
        FormUtils.setupCnicFormatting(binding.etCnic)
    }

    private fun validateForm(): Boolean {
        val name = binding.etFullName.text.toString().trim()
        val empId = binding.etEmployeeId.text.toString().trim()
        val cnic = binding.etCnic.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()

        if (empId.isEmpty()) {
            binding.etEmployeeId.error = "Employee ID is required"
            return false
        }

        if (name.isEmpty()) {
            binding.etFullName.error = "Full Name is required"
            return false
        }

        if (cnic.length < 15) {
            binding.etCnic.error = "Invalid CNIC (e.g. 00000-0000000-0)"
            return false
        }

        if (!FormUtils.isValidPhone(phone)) {
            binding.etPhone.error = "Invalid contact number"
            return false
        }

        if (email.isNotEmpty() && !FormUtils.isValidEmail(email)) {
            binding.etEmail.error = "Invalid email address"
            return false
        }

        if (!FormUtils.isValidPassword(pass)) {
            binding.etPassword.error = "Password must be at least 8 characters with letters and numbers"
            return false
        }

        if (pass != confirmPass) {
            binding.etConfirmPassword.error = "Passwords do not match"
            return false
        }
        return true
    }

    private fun uploadAndSave() {
        binding.btnAddDriver.isEnabled = false
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show()

        StorageUtils.uploadImage("driver_profiles", selectedImageUri!!) { url ->
            if (url != null) {
                saveDriver(url)
            } else {
                binding.btnAddDriver.isEnabled = true
                Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDriver(imageUrl: String) {
        val newDriver = DriverModel(
            id = binding.etEmployeeId.text.toString(),
            name = binding.etFullName.text.toString(),
            status = "Idle",
            assignedBus = null,
            route = null,
            profileImage = 0,
            profileImageUrl = imageUrl,
            cnic = binding.etCnic.text.toString(),
            phone = binding.etPhone.text.toString(),
            email = binding.etEmail.text.toString()
        )

        // Save to Firestore via DriverRepository
        DriverRepository.addDriver(newDriver) { success ->
            if (success) {
                showSuccessDialog(newDriver.name, newDriver)
            } else {
                binding.btnAddDriver.isEnabled = true
                Toast.makeText(this, "Failed to save driver to Firestore", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showSuccessDialog(driverName: String, newDriver: DriverModel) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_success_dialog)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnDone = dialog.findViewById<Button>(R.id.btnDone)
        val txtMessage = dialog.findViewById<TextView>(R.id.dialogMessage)

        txtMessage.text = "$driverName has been added successfully to the fleet management system."

        btnDone.setOnClickListener {
            dialog.dismiss()
            val intent = Intent()
            intent.putExtra("new_driver_data", newDriver)
            setResult(RESULT_OK, intent)
            finish()
        }
        dialog.show()
    }
}