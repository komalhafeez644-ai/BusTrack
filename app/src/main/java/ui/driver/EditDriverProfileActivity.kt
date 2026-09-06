package ui.driver

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityEditDriverProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import utils.StorageUtils
import utils.ViewUtils

class EditDriverProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDriverProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null

    // Gallery Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            utils.ImageUtils.loadPreviewImage(this, it, binding.imgProfile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDriverProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        loadData()
    }

    private fun loadData() {
        val email = auth.currentUser?.email?.trim()?.lowercase() ?: return
        Log.d("EditProfile", "Loading data for: $email")
        
        db.collection("drivers").get()
            .addOnSuccessListener { querySnapshot ->
                val document = querySnapshot.documents.find { 
                    it.getString("email")?.trim()?.lowercase() == email 
                }

                if (document != null) {
                    fillFields(document)
                } else {
                    Log.e("EditProfile", "No driver document found for email: $email")
                    Toast.makeText(this, "Profile not found in database", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("EditProfile", "Error fetching drivers", e)
                Toast.makeText(this, "Failed to load data", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fillFields(document: com.google.firebase.firestore.DocumentSnapshot) {
        val name = document.getString("name") ?: ""
        val driverId = document.getString("id") ?: ""
        val phone = document.getString("phone") ?: ""
        val email = document.getString("email") ?: ""
        val imageUrl = document.getString("profileImageUrl")
        
        binding.etFullName.setText(name)
        binding.etEmail.setText(email)
        binding.etPhone.setText(phone)
        binding.tvDisplayId.text = "Driver ID: $driverId"
        
        binding.etEmail.isEnabled = false
        
        utils.ImageUtils.loadProfileImage(this, imageUrl, binding.imgProfile)
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        binding.imgCamera.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            pickImage.launch("image/*")
        }

        binding.btnSave.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val name = binding.etFullName.text.toString().trim()
            if (name.isEmpty()) {
                binding.etFullName.error = "Name is required"
                return@setOnClickListener
            }
            
            if (selectedImageUri != null) {
                uploadAndSave()
            } else {
                saveDataToFirestore(null)
            }
        }

        binding.btnCancel.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }
    }

    private fun uploadAndSave() {
        binding.btnSave.isEnabled = false
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
        
        StorageUtils.uploadImage("driver_profiles", selectedImageUri!!) { url ->
            if (url != null) {
                saveDataToFirestore(url)
            } else {
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDataToFirestore(imageUrl: String?) {
        val email = auth.currentUser?.email?.trim()?.lowercase() ?: return
        val uid = auth.currentUser?.uid ?: return
        
        val newName = binding.etFullName.text.toString().trim()
        val newPhone = binding.etPhone.text.toString().trim()

        lifecycleScope.launch {
            try {
                // 1. Update 'users' collection
                val userUpdate = mutableMapOf<String, Any>(
                    "fullName" to newName,
                    "phone" to newPhone
                )
                if (imageUrl != null) userUpdate["profileImageUrl"] = imageUrl
                
                db.collection("users").document(uid).update(userUpdate).await()

                // 2. Update 'drivers' collection
                val driverQuery = db.collection("drivers").get().await()
                val driverDoc = driverQuery.documents.find { 
                    it.getString("email")?.trim()?.lowercase() == email 
                }

                if (driverDoc != null) {
                    val driverUpdate = mutableMapOf<String, Any>(
                        "name" to newName,
                        "phone" to newPhone
                    )
                    if (imageUrl != null) driverUpdate["profileImageUrl"] = imageUrl
                    
                    db.collection("drivers").document(driverDoc.id).update(driverUpdate).await()
                }

                Toast.makeText(this@EditDriverProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                Toast.makeText(this@EditDriverProfileActivity, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}