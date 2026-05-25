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
import utils.ViewUtils

class AddDriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddDriverBinding

    // Photo Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.imgUpload.setImageURI(it)
            binding.imgUpload.setPadding(0, 0, 0, 0)
            Toast.makeText(this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddDriver.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val name = binding.etFullName.text.toString()
            val empId = binding.etEmployeeId.text.toString()
            val cnic = binding.etCnic.text.toString()
            val phone = binding.etPhone.text.toString()
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            // Basic Validation
            if (name.isEmpty() || empId.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Naya Driver Object banaya
            val newDriver = DriverModel(
                id = empId,
                name = name,
                status = "Idle",
                assignedBus = "Not Assigned",
                route = "Not Assigned",
                profileImage = 0,
                cnic = cnic,
                phone = phone,
                email = email
            )

            // Repository mein add karein takay data save rahay
            DriverRepository.addDriver(newDriver)

            // Direct finish karne ki bajaye Success Dialog dikhayein
            showSuccessDialog(name, newDriver)
        }

        binding.btnBack.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            finish() 
        }
        
        binding.btnCancel.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            finish() 
        }

        // Camera Button Click
        binding.btnPickImage.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            pickImageLauncher.launch("image/*")
        }
    }

    // Custom Success Dialog Function
    private fun showSuccessDialog(driverName: String, newDriver: DriverModel) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_success_dialog)

        // Dialog ka background transparent karna zaroori hai corners rounded dikhane ke liye
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnDone = dialog.findViewById<Button>(R.id.btnDone)
        val txtMessage = dialog.findViewById<TextView>(R.id.dialogMessage)

        txtMessage.text = "$driverName has been added successfully to the fleet management system."

        btnDone.setOnClickListener {
            dialog.dismiss()

            // Intent ke zariye data wapis bhejna aur screen band karna
            val intent = Intent()
            intent.putExtra("new_driver_data", newDriver)
            setResult(RESULT_OK, intent)
            finish()
        }

        dialog.show()
    }
}
