package ui.admin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityAddStudentBinding
import com.example.bustrack_app.models.StudentModel
import utils.FormUtils
import utils.StorageUtils
import utils.ViewUtils

class AddStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStudentBinding
    private var selectedImageUri: Uri? = null
    private var selectedLat: Double = 0.0
    private var selectedLng: Double = 0.0

    // Location Picker Launcher
    private val pickLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val address = result.data?.getStringExtra("SELECTED_ADDRESS")
            selectedLat = result.data?.getDoubleExtra("LATITUDE", 0.0) ?: 0.0
            selectedLng = result.data?.getDoubleExtra("LONGITUDE", 0.0) ?: 0.0
            address?.let {
                binding.etPickupAddress.setText(it)
            }
        }
    }

    // Photo Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imgStudentUpload.setImageURI(it)
            binding.imgStudentUpload.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGradeSpinner()
        setupFormFormatting()
        setupClickListeners()
    }

    private fun setupFormFormatting() {
        FormUtils.setupUppercaseInput(binding.etEmployeeId)
        FormUtils.setupTitleCaseInput(binding.etFullName)
        FormUtils.setupTitleCaseInput(binding.etParentName)
    }

    private fun setupGradeSpinner() {
        val grades = arrayOf("Grade 9", "Grade 10", "Grade 11", "Grade 12", "BS IT 7th semester")
        val adapter = ArrayAdapter(this, com.example.bustrack_app.R.layout.spinner_dropdown_item, grades)
        binding.spinnerGrade.setAdapter(adapter)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        binding.btnPickStudentImage.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            pickImageLauncher.launch("image/*")
        }

        binding.btnSelectOnMap.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val intent = Intent(this, LocationPickerActivity::class.java)
            pickLocationLauncher.launch(intent)
        }

        binding.btnOnlyAdd.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (validateForm()) {
                if (selectedImageUri != null) {
                    uploadAndSave(false)
                } else {
                    saveStudent("") {
                        Toast.makeText(this, "Student Added Successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }

        binding.btnAddAndNext.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (validateForm()) {
                if (selectedImageUri != null) {
                    uploadAndSave(true)
                } else {
                    saveStudent("") { newStudent ->
                        navigateToAnalysis(newStudent)
                    }
                }
            }
        }

        binding.btnCancelForm.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }
    }

    private fun validateForm(): Boolean {
        val name = binding.etFullName.text.toString().trim()
        val id = binding.etEmployeeId.text.toString().trim()
        val phone = binding.etEmergencyContact.text.toString().trim()

        if (name.isEmpty()) {
            binding.etFullName.error = "Name required"
            return false
        }
        if (id.isEmpty()) {
            binding.etEmployeeId.error = "ID required"
            return false
        }
        if (id.length > 10) {
            binding.etEmployeeId.error = "ID too long (max 10)"
            return false
        }
        if (!FormUtils.isValidPhone(phone)) {
            binding.etEmergencyContact.error = "Invalid contact (11 digits)"
            return false
        }
        return true
    }

    private fun uploadAndSave(goNext: Boolean) {
        binding.btnOnlyAdd.isEnabled = false
        binding.btnAddAndNext.isEnabled = false
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show()

        StorageUtils.uploadImage("student_profiles", selectedImageUri!!) { url ->
            if (url != null) {
                saveStudent(url) { student ->
                    Toast.makeText(this, "Student Added Successfully!", Toast.LENGTH_SHORT).show()
                    if (goNext) {
                        navigateToAnalysis(student)
                    } else {
                        finish()
                    }
                }
            } else {
                binding.btnOnlyAdd.isEnabled = true
                binding.btnAddAndNext.isEnabled = true
                Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveStudent(imageUrl: String, onComplete: (StudentModel) -> Unit) {
        val student = StudentModel(
            id = binding.etEmployeeId.text.toString().trim(),
            name = binding.etFullName.text.toString().trim(),
            grade = binding.spinnerGrade.text.toString(),
            location = binding.etPickupAddress.text.toString().trim(),
            route = null,
            busNo = null,
            status = "UNASSIGNED",
            profileImage = 0,
            profileImageUrl = imageUrl,
            fatherName = binding.etParentName.text.toString().trim(),
            phoneNumber = binding.etEmergencyContact.text.toString().trim(),
            pickupTime = "TBD",
            insuranceStatus = "Pending",
            latitude = selectedLat,
            longitude = selectedLng
        )
        
        // Save to Firestore via FirebaseRepository
        com.example.bustrack_app.data.FirebaseRepository.saveStudent(student) { success ->
            if (success) {
                StudentRepository.addStudent(student)
                onComplete(student)
            } else {
                binding.btnOnlyAdd.isEnabled = true
                binding.btnAddAndNext.isEnabled = true
                Toast.makeText(this, "Failed to save student to Firestore", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToAnalysis(student: StudentModel) {
        val intent = Intent(this, RouteAnalysisActivity::class.java)
        intent.putExtra("APPLICATION_DATA", mapStudentToAppModel(student))
        startActivity(intent)
        finish()
    }

    private fun mapStudentToAppModel(s: StudentModel): com.example.bustrack_app.models.ApplicationModel {
        return com.example.bustrack_app.models.ApplicationModel(
            id = s.id.filter { it.isDigit() }.toIntOrNull() ?: (100..999).random(),
            studentName = s.name,
            studentClass = s.grade,
            pickupPoint = s.location,
            contactNumber = s.phoneNumber,
            time = "Now",
            status = "Pending",
            image = s.profileImage,
            profileImageUrl = s.profileImageUrl,
            parentName = s.fatherName,
            latitude = s.latitude,
            longitude = s.longitude,
            studentIdString = s.id
        )
    }
}