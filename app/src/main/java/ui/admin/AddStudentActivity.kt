package ui.admin

import android.annotation.SuppressLint
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
import utils.ViewUtils

class AddStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStudentBinding

    // Photo Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.imgStudentUpload.setImageURI(it)
            binding.imgStudentUpload.setPadding(0, 0, 0, 0)
            Toast.makeText(this, "Photo uploaded successfully", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGradeSpinner()
        setupClickListeners()
    }

    private fun setupGradeSpinner() {
        val grades = arrayOf("Grade 9", "Grade 10", "Grade 11", "Grade 12", "BS IT 7th semester")
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, grades)
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
            Toast.makeText(this, "Opening Map picker...", Toast.LENGTH_SHORT).show()
        }

        binding.btnOnlyAdd.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (validateForm()) {
                saveStudent() // Only add
                Toast.makeText(this, "Student Added Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        binding.btnAddAndNext.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (validateForm()) {
                val newStudent = saveStudent()
                // Navigation to Route Analysis
                val intent = Intent(this, RouteAnalysisActivity::class.java)
                intent.putExtra("APPLICATION_DATA", mapStudentToAppModel(newStudent))
                startActivity(intent)
                finish()
            }
        }

        binding.btnCancelForm.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }
    }

    private fun validateForm(): Boolean {
        if (binding.etFullName.text.toString().trim().isEmpty()) {
            binding.etFullName.error = "Name required"
            return false
        }
        if (binding.etEmployeeId.text.toString().trim().isEmpty()) {
            binding.etEmployeeId.error = "ID required"
            return false
        }
        return true
    }

    private fun saveStudent(): StudentModel {
        val student = StudentModel(
            id = binding.etEmployeeId.text.toString().trim(),
            name = binding.etFullName.text.toString().trim(),
            grade = binding.spinnerGrade.text.toString(),
            location = binding.etPickupAddress.text.toString().trim(),
            route = null,
            busNo = null,
            status = "UNASSIGNED",
            profileImage = 0,
            fatherName = binding.etParentName.text.toString().trim(),
            phoneNumber = binding.etEmergencyContact.text.toString().trim(),
            pickupTime = "TBD",
            insuranceStatus = "Pending"
        )
        StudentRepository.addStudent(student)
        return student
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
            parentName = s.fatherName
        )
    }
}
