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
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityAddDriverBinding
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.data.DriverRepository
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
                checkEmailAndProceed()
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
        if (empId.length > 10) {
            binding.etEmployeeId.error = "ID too long (max 10)"
            return false
        }

        if (name.isEmpty()) {
            binding.etFullName.error = "Full Name is required"
            return false
        }

        if (cnic.length < 15) {
            binding.etCnic.error = "CNIC must be 13 digits (15 with dashes)"
            return false
        }

        if (!FormUtils.isValidPhone(phone)) {
            binding.etPhone.error = "Invalid phone (11 digits)"
            return false
        }

        if (email.isEmpty()) {
             binding.etEmail.error = "Email is required"
             return false
        } else if (!FormUtils.isValidEmail(email)) {
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

    private fun checkEmailAndProceed() {
        val email = binding.etEmail.text.toString().trim().lowercase()
        binding.btnAddDriver.isEnabled = false
        
        FirebaseFirestore.getInstance().collection("users")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { query ->
                if (!query.isEmpty) {
                    binding.btnAddDriver.isEnabled = true
                    binding.etEmail.error = "This email is already registered"
                    Toast.makeText(this, "A user with this email already exists!", Toast.LENGTH_LONG).show()
                } else {
                    if (selectedImageUri != null) {
                        uploadAndSave()
                    } else {
                        handleAccountCreation("")
                    }
                }
            }
            .addOnFailureListener {
                binding.btnAddDriver.isEnabled = true
                Toast.makeText(this, "Verification failed. Check internet.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun uploadAndSave() {
        binding.btnAddDriver.isEnabled = false
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show()

        StorageUtils.uploadImage("driver_profiles", selectedImageUri!!) { url ->
            if (url != null) {
                handleAccountCreation(url)
            } else {
                binding.btnAddDriver.isEnabled = true
                Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleAccountCreation(imageUrl: String) {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString()
        
        lifecycleScope.launch {
            try {
                // To create a second account WITHOUT logging out the Admin:
                // Initialize a temporary secondary Firebase app instance
                val options = FirebaseApp.getInstance().options
                val secondaryApp = try {
                    FirebaseApp.initializeApp(this@AddDriverActivity, options, "Secondary")
                } catch (e: Exception) {
                    FirebaseApp.getInstance("Secondary")
                }
                
                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)
                
                val result = secondaryAuth.createUserWithEmailAndPassword(email, password).await()
                val uid = result.user?.uid
                
                if (uid != null) {
                    saveDriverToCollections(uid, imageUrl)
                    secondaryAuth.signOut() // Clean up secondary session
                }
            } catch (e: Exception) {
                binding.btnAddDriver.isEnabled = true
                Toast.makeText(this@AddDriverActivity, "Auth Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun saveDriverToCollections(uid: String, imageUrl: String) {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val empId = binding.etEmployeeId.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val cnic = binding.etCnic.text.toString().trim()

        val db = FirebaseFirestore.getInstance()

        // 1. Save to 'users' collection (for Role Based Login)
        val userData = mapOf(
            "uid" to uid,
            "fullName" to name,
            "email" to email,
            "phone" to phone,
            "role" to "driver",
            "employeeId" to empId
        )

        // 2. Save to 'drivers' collection (for Fleet Management)
        val driverData = DriverModel(
            id = empId,
            name = name,
            status = "Idle",
            assignedBus = null,
            route = null,
            profileImage = 0,
            profileImageUrl = imageUrl,
            cnic = cnic,
            phone = phone,
            email = email
        )

        lifecycleScope.launch {
            try {
                db.collection("users").document(uid).set(userData).await()
                db.collection("drivers").document(empId).set(driverData).await()
                
                showSuccessDialog(name, driverData)
            } catch (e: Exception) {
                binding.btnAddDriver.isEnabled = true
                Toast.makeText(this@AddDriverActivity, "Firestore Error: ${e.message}", Toast.LENGTH_LONG).show()
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

        txtMessage.text = "$driverName account created. Driver can now login with their email and provided password."

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