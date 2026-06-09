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

    private fun validateForm(): Boolean {
        val name = binding.etFullName.text.toString()
        val empId = binding.etEmployeeId.text.toString()
        val pass = binding.etPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()

        if (name.isEmpty() || empId.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
            return false
        }

        if (pass != confirmPass) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
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

        // Save to Firestore via FirebaseRepository
        com.example.bustrack_app.data.FirebaseRepository.saveDriver(newDriver) { success ->
            if (success) {
                DriverRepository.addDriver(newDriver)
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