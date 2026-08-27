package ui.principal

import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityEditPrincipalProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import utils.StorageUtils
import utils.ViewUtils

class PrincipalEditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditPrincipalProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
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
        binding = ActivityEditPrincipalProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupValidation()
        loadPrincipalData()
    }

    private fun setupValidation() {
        // Name Auto-Capitalization
        binding.etFullName.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (isUpdating || s.isNullOrEmpty()) return
                isUpdating = true
                
                val original = s.toString()
                val capitalized = original.split(" ").joinToString(" ") { part ->
                    part.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
                
                if (capitalized != original) {
                    val selection = binding.etFullName.selectionStart
                    binding.etFullName.setText(capitalized)
                    binding.etFullName.setSelection(selection.coerceAtMost(capitalized.length))
                }
                isUpdating = false
            }
        })

        // Phone Number: 11 digits, numbers only
        binding.etPhone.filters = arrayOf(InputFilter.LengthFilter(11), InputFilter { source, start, end, dest, dstart, dend ->
            for (i in start until end) {
                if (!Character.isDigit(source[i])) return@InputFilter ""
            }
            null
        })
    }

    private fun loadPrincipalData() {
        val uid = auth.currentUser?.uid ?: return
        
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val fullName = document.getString("fullName") ?: "Principal User"
                val empId = document.getString("employeeId") ?: "PRN-2024-001"
                val phone = document.getString("phone") ?: ""
                val email = document.getString("email") ?: "principal@gmail.com"
                
                binding.etFullName.setText(fullName)
                binding.etEmail.setText(email)
                binding.etPhone.setText(phone)
                binding.etEmpId.setText(empId)
                
                binding.tvDisplayId.text = "Principal ID: $empId"
                
                // Requirement: Official Email and Principal ID cannot be changed
                binding.etEmail.isEnabled = false
                binding.etEmpId.isEnabled = false
                
                val imageUrl = document.getString("profileImageUrl")
                if (!imageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(imageUrl).placeholder(R.drawable.ic_person).circleCrop().into(binding.imgProfile)
                }
            }
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
            val phone = binding.etPhone.text.toString().trim()

            if (name.isEmpty()) {
                binding.etFullName.error = "Name cannot be empty"
                return@setOnClickListener
            }

            // Phone Validation: 11 digits and starts with 03
            if (phone.length != 11) {
                binding.etPhone.error = "Phone number must be exactly 11 digits"
                return@setOnClickListener
            }
            if (!phone.startsWith("03")) {
                binding.etPhone.error = "Phone number must start with 03"
                return@setOnClickListener
            }
            
            if (selectedImageUri != null) {
                uploadAndSave()
            } else {
                saveDataToFirestore(null)
            }
        }

        binding.btnCancel.setOnClickListener {
            ViewUtils.applyPressEffect(it)
            it.postDelayed({
                finish()
            }, 250)
        }
    }

    private fun uploadAndSave() {
        binding.btnSave.isEnabled = false
        Toast.makeText(this, "Uploading image...", Toast.LENGTH_SHORT).show()
        
        StorageUtils.uploadImage("profiles", selectedImageUri!!) { url ->
            if (url != null) {
                saveDataToFirestore(url)
            } else {
                binding.btnSave.isEnabled = true
                Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveDataToFirestore(imageUrl: String?) {
        val uid = auth.currentUser?.uid ?: return
        
        val principalData = mutableMapOf<String, Any>(
            "fullName" to binding.etFullName.text.toString(),
            "phone" to binding.etPhone.text.toString()
        )
        
        if (imageUrl != null) {
            principalData["profileImageUrl"] = imageUrl
        }

        db.collection("users").document(uid)
            .update(principalData)
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
