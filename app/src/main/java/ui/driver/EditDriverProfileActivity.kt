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
import com.google.firebase.firestore.SetOptions
import com.google.android.gms.tasks.Tasks
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import utils.StorageUtils
import utils.ViewUtils

class EditDriverProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDriverProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedImageUri: Uri? = null
    private var isImageRemoved = false
    private var driverDocumentId: String? = null

    // Gallery Picker
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imgProfile.setPadding(0, 0, 0, 0)
            binding.imgProfile.scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
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
        driverDocumentId = document.id
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
            utils.ImageUtils.showPhotoOptionsDialog(this, it,
                onGallerySelected = { pickImage.launch("image/*") },
                onNoPhotoSelected = {
                    selectedImageUri = null
                    isImageRemoved = true
                    binding.imgProfile.setImageResource(R.drawable.ic_person)
                    binding.imgProfile.setPadding(20, 20, 20, 20)
                }
            )
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
                saveDataToFirestore(if (isImageRemoved) "" else null)
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
        
        StorageUtils.uploadImage(
            folder = "driver_profiles",
            uri = selectedImageUri!!,
            immediateContext = this,
            onResult = { url ->
                runOnUiThread {
                    if (url != null) {
                        saveDataToFirestore(url)
                    } else {
                        binding.btnSave.isEnabled = true
                        Toast.makeText(this, "Upload failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        )
    }

    private fun saveDataToFirestore(imageUrl: String?) {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            binding.btnSave.isEnabled = true
            Toast.makeText(this, "Please sign in again to update your profile.", Toast.LENGTH_SHORT).show()
            return
        }
        val targetDriverDocumentId = driverDocumentId
        if (targetDriverDocumentId.isNullOrBlank()) {
            binding.btnSave.isEnabled = true
            Toast.makeText(this, "Profile is still loading. Please try again.", Toast.LENGTH_SHORT).show()
            return
        }
        
        val newName = binding.etFullName.text.toString().trim()
        val newPhone = binding.etPhone.text.toString().trim()

        binding.btnSave.isEnabled = false
        lifecycleScope.launch {
            try {
                // Patch only profile fields; use the document ID captured when this
                // profile was loaded instead of querying every driver again on Save.
                val driverUpdate = mutableMapOf<String, Any>("name" to newName, "phone" to newPhone)
                if (imageUrl != null) driverUpdate["profileImageUrl"] = imageUrl

                val userUpdate = mutableMapOf<String, Any>("fullName" to newName, "phone" to newPhone)
                if (imageUrl != null) userUpdate["profileImageUrl"] = imageUrl
                val driverWrite = db.collection("drivers").document(targetDriverDocumentId).update(driverUpdate)
                val userWrite = db.collection("users").document(uid).set(userUpdate, SetOptions.merge())
                Tasks.whenAll(driverWrite, userWrite).await()

                Toast.makeText(this@EditDriverProfileActivity, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                finish()

            } catch (e: Exception) {
                binding.btnSave.isEnabled = true
                Toast.makeText(this@EditDriverProfileActivity, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
