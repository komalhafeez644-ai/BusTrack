package ui.admin

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.text.Editable
import android.text.TextWatcher
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
    private var addressWithCoordinates: String = ""

    // Location Picker Launcher
    private val pickLocationLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val address = result.data?.getStringExtra("SELECTED_ADDRESS")
            addressWithCoordinates = address.orEmpty()
            address?.let {
                binding.etPickupAddress.setText(it)
            }
            selectedLat = result.data?.getDoubleExtra("LATITUDE", 0.0) ?: 0.0
            selectedLng = result.data?.getDoubleExtra("LONGITUDE", 0.0) ?: 0.0
        }
    }

    // Photo Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            utils.ImageUtils.loadPreviewImage(this, it, binding.imgStudentUpload)
            binding.imgStudentUpload.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGradeSpinner()
        setupSemesterSpinner()
        setupAddressCoordinateTracking()
        setupFormFormatting()
        setupClickListeners()
    }

    private fun setupAddressCoordinateTracking() {
        binding.etPickupAddress.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().trim() != addressWithCoordinates) {
                    selectedLat = 0.0
                    selectedLng = 0.0
                }
            }
        })
    }

    private fun setupFormFormatting() {
        FormUtils.setupRollNumberFormatting(binding.etEmployeeId)
        FormUtils.setupTitleCaseInput(binding.etFullName)
        FormUtils.setupTitleCaseInput(binding.etParentName)
        FormUtils.setupPhoneFormatting(binding.etEmergencyContact)
    }

    private fun setupGradeSpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, utils.StudentEducationOptions.grades)
        binding.spinnerGrade.setAdapter(adapter)
    }

    private fun setupSemesterSpinner() {
        val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, utils.StudentEducationOptions.semesters)
        binding.etSection.setAdapter(adapter)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        binding.btnPickStudentImage.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            utils.ImageUtils.showPhotoOptionsDialog(this, it,
                onGallerySelected = { pickImageLauncher.launch("image/*") },
                onNoPhotoSelected = {
                    selectedImageUri = null
                    binding.imgStudentUpload.setImageResource(R.drawable.ic_person)
                    binding.imgStudentUpload.setPadding(20, 20, 20, 20)
                }
            )
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
        if (!FormUtils.isValidRollNumber(id)) {
            binding.etEmployeeId.error = "Roll Number required"
            return false
        }
        if (!FormUtils.isValidPhone(phone)) {
            binding.etEmergencyContact.error = "Invalid contact (03XX-XXXXXXX)"
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
        val gradeVal = binding.spinnerGrade.text.toString()
        val semVal = binding.etSection.text.toString()
        val combinedGrade = if (semVal.isNotEmpty() && semVal != "Select Sem") "$gradeVal $semVal" else gradeVal

        val student = StudentModel(
            id = binding.etEmployeeId.text.toString().trim(),
            rollNumber = binding.etEmployeeId.text.toString().trim(),
            name = binding.etFullName.text.toString().trim(),
            grade = combinedGrade,
            location = binding.etPickupAddress.text.toString().trim(),
            route = null,
            busNo = null,
            status = "UNASSIGNED",
            profileImage = 0,
            profileImageUrl = imageUrl,
            fatherName = binding.etParentName.text.toString().trim(),
            phoneNumber = binding.etEmergencyContact.text.toString().trim(),
            pickupTime = "ETA: --",
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
            studentIdString = s.rollNumber,
            studentDocumentId = s.id
        )
    }
}
