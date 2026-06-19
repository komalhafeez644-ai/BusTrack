package ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityEditAdminProfileBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import utils.StorageUtils

class EditAdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAdminProfileBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private var selectedImageUri: Uri? = null

    // Gallery Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
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
        val currentUserId = auth.currentUser?.uid ?: return
        
        db.collection("users").document(currentUserId).get()
            .addOnSuccessListener { document ->
                // Loading data with strict defaults for missing fields
                val fullName = document.getString("fullName") ?: "System Admin"
                val empId = document.getString("employeeId") ?: "ADMIN-2024-001"
                val dept = "Transport" // Fixed as requested
                val phone = document.getString("phone") ?: "+92 300 1234567"
                val email = document.getString("email") ?: "admin@gmail.com"
                
                binding.etFullName.setText(fullName)
                binding.etEmail.setText(email)
                binding.etPhone.setText(phone)
                binding.etDept.setText(dept)
                binding.etEmpId.setText(empId)
                
                // Set the ID below the profile photo
                binding.tvDisplayId.text = "User ID: $empId"
                
                // Requirement: Department and Employee ID cannot be changed
                binding.etDept.isEnabled = false
                binding.etEmpId.isEnabled = false
                
                val imageUrl = document.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_person).circleCrop().into(binding.imgProfile)
                }
            }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        binding.imgCamera.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            pickImage.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val name = binding.etFullName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etFullName.error = "Name cannot be empty"
                return@setOnClickListener
            }
            
            if (selectedImageUri != null) {
                uploadAndSave()
            } else {
                saveDataToFirestore(null)
            }
        }

        binding.btnCancel.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }
    }

    private fun uploadAndSave() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "Session expired. Please login again.", Toast.LENGTH_LONG).show()
            return
        }

        binding.btnSave.isEnabled = false
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
        
        StorageUtils.uploadImage("profiles", selectedImageUri!!) { url ->
            if (url != null) {
                saveDataToFirestore(url)
            } else {
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Image upload failed. Make sure you have a stable internet connection.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDataToFirestore(imageUrl: String?) {
        val currentUserId = auth.currentUser?.uid ?: return
        
        val adminData = mutableMapOf(
            "fullName" to binding.etFullName.text.toString(),
            "email" to binding.etEmail.text.toString(),
            "phone" to binding.etPhone.text.toString(),
            "department" to binding.etDept.text.toString(),
            "employeeId" to binding.etEmpId.text.toString()
        )
        
        if (imageUrl != null) {
            adminData["profileImageUrl"] = imageUrl
        }

        db.collection("users").document(currentUserId)
            .set(adminData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}