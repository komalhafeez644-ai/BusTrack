package ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivityEditAdminProfileBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class EditAdminProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAdminProfileBinding
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Gallery Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.imgProfile.setImageURI(it)
            // Note: Upload logic for image will go here (Firebase Storage)
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
        val currentUserId = auth.currentUser?.uid ?: "test_admin_id" // Use fallback for testing
        
        db.collection("admins").document(currentUserId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.etFullName.setText(document.getString("fullName"))
                    binding.etEmail.setText(document.getString("email"))
                    binding.etPhone.setText(document.getString("phone"))
                    binding.etDept.setText(document.getString("department"))
                    binding.etEmpId.setText(document.getString("employeeId"))
                } else {
                    // Fallback to defaults if no data in Firestore
                    binding.etFullName.setText("John Admin")
                    binding.etEmail.setText("admin@punjabcollege.edu")
                    binding.etPhone.setText("+92 300 1234567")
                    binding.etDept.setText("Logistics")
                    binding.etEmpId.setText("CF-ADM-24")
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupClickListeners() {
        // Back Button
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        // Camera Icon click for gallery
        binding.imgCamera.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            pickImage.launch("image/*")
        }

        // Save Button logic
        binding.btnSave.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            saveDataToFirestore()
        }

        binding.btnCancel.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }
    }

    private fun saveDataToFirestore() {
        val fullName = binding.etFullName.text.toString()
        val email = binding.etEmail.text.toString()
        val phone = binding.etPhone.text.toString()
        val department = binding.etDept.text.toString()
        val employeeId = binding.etEmpId.text.toString()

        val adminData = hashMapOf(
            "fullName" to fullName,
            "email" to email,
            "phone" to phone,
            "department" to department,
            "employeeId" to employeeId
        )

        val currentUserId = auth.currentUser?.uid ?: "test_admin_id"

        db.collection("admins").document(currentUserId)
            .set(adminData)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                
                // Explicitly navigate back to ProfileActivity with modern style
                val intent = Intent(this, ProfileActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
                startActivity(intent)
                finish()
                overridePendingTransition(0, 0)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error updating profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}