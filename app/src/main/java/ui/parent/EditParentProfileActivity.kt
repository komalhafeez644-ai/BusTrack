package ui.parent

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityEditParentProfileBinding
import com.example.bustrack_app.viewmodels.ProfileViewModel
import utils.StorageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditParentProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditParentProfileBinding
    private val viewModel: ProfileViewModel by viewModels()
    private var selectedImageUri: Uri? = null
    private var cameraImageUri: Uri? = null

    // Photo selection contract
    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            displaySelectedImage(it)
        }
    }

    // Camera capture contract
    private val takePhoto = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success) {
            cameraImageUri?.let {
                selectedImageUri = it
                displaySelectedImage(it)
            }
        }
    }

    // Permission request contract
    private val requestCameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            launchCamera()
        } else {
            Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditParentProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.hide()

        setupObservers()
        setupClickListeners()
    }

    private fun displaySelectedImage(uri: Uri) {
        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_person)
            .circleCrop()
            .into(binding.imgProfile)
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
            binding.etCity.setText(data.city)
            
            // Only load from network if user hasn't selected a new local image
            if (selectedImageUri == null && data.profileImageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(data.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.imgProfile)
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
            saveProfile()
        }

        binding.btnUpdatePhoto.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            showImageSourceDialog()
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Take Photo", "Choose from Gallery")
        AlertDialog.Builder(this)
            .setTitle("Select Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> pickImage.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            launchCamera()
        } else {
            requestCameraPermission.launch(Manifest.permission.CAMERA)
        }
    }

    private fun launchCamera() {
        try {
            val photoFile = File.createTempFile(
                "IMG_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}_",
                ".jpg",
                getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            )
            
            cameraImageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                photoFile
            )
            
            cameraImageUri?.let { takePhoto.launch(it) }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to create image file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Full Name is required"
            return
        }

        val userData = mutableMapOf<String, Any>(
            "fullName" to fullName,
            "email" to binding.etEmail.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),
            "address" to binding.etAddress.text.toString().trim(),
            "city" to binding.etCity.text.toString().trim()
        )

        binding.btnSave.isEnabled = false

        if (selectedImageUri != null) {
            // Upload image first
            Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show()
            StorageUtils.uploadImage("parent_profiles", selectedImageUri!!) { url ->
                if (url != null) {
                    userData["profileImageUrl"] = url
                    updateFirestore(userData)
                } else {
                    binding.btnSave.isEnabled = true
                    Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            updateFirestore(userData)
        }
    }

    private fun updateFirestore(userData: Map<String, Any>) {
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
}
