package ui.admin

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.models.StudentModel
import com.google.android.material.imageview.ShapeableImageView
import utils.*

class EditStudentActivity : AppCompatActivity() {

    private var studentId: String? = null
    private var studentData: StudentModel? = null
    private var selectedImageUri: Uri? = null

    private lateinit var imgStudentEdit: ShapeableImageView
    private lateinit var etEditStudentId: EditText
    private lateinit var etEditFullName: EditText
    private lateinit var spinnerEditGrade: AutoCompleteTextView
    private lateinit var etEditParentName: EditText
    private lateinit var etEditEmergencyContact: EditText
    private lateinit var etEditPickupAddress: EditText
    private lateinit var tilRoute: com.google.android.material.textfield.TextInputLayout
    private lateinit var tilStop: com.google.android.material.textfield.TextInputLayout
    private lateinit var spinnerEditRoute: AutoCompleteTextView
    private lateinit var spinnerEditStop: AutoCompleteTextView
    private lateinit var txtEditBusValue: TextView
    private lateinit var btnUpdateStudent: Button
    private lateinit var btnBack: View
    private lateinit var btnCancelEdit: View
    private lateinit var btnChangeImage: View

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            imgStudentEdit.setImageURI(it)
            imgStudentEdit.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_edit)

        initViews()
        
        studentId = intent.getStringExtra("STUDENT_ID")
        loadStudentData()

        setupGradeSpinner()
        setupRouteAndStopSpinners()
        setupClickListeners()
        setupFormFormatting()
    }

    private fun initViews() {
        imgStudentEdit = findViewById(R.id.imgStudentEdit)
        etEditStudentId = findViewById(R.id.etEditStudentId)
        etEditFullName = findViewById(R.id.etEditFullName)
        spinnerEditGrade = findViewById(R.id.spinnerEditGrade)
        etEditParentName = findViewById(R.id.etEditParentName)
        etEditEmergencyContact = findViewById(R.id.etEditEmergencyContact)
        etEditPickupAddress = findViewById(R.id.etEditPickupAddress)
        tilRoute = findViewById(R.id.tilRoute)
        tilStop = findViewById(R.id.tilStop)
        spinnerEditRoute = findViewById(R.id.spinnerEditRoute)
        spinnerEditStop = findViewById(R.id.spinnerEditStop)
        txtEditBusValue = findViewById(R.id.txtEditBusValue)
        btnUpdateStudent = findViewById(R.id.btnUpdateStudent)
        btnBack = findViewById(R.id.btnBack)
        btnCancelEdit = findViewById(R.id.btnCancelEdit)
        btnChangeImage = findViewById(R.id.btnChangeImage)
    }

    private fun loadStudentData() {
        studentData = StudentRepository.studentList.value?.find { it.id == studentId }
        studentData?.let {
            etEditStudentId.setText(it.id)
            etEditFullName.setText(it.name)
            spinnerEditGrade.setText(it.grade, false)
            etEditParentName.setText(it.fatherName)
            etEditEmergencyContact.setText(it.phoneNumber)
            etEditPickupAddress.setText(it.location)
            
            if (it.route.isNullOrEmpty()) {
                spinnerEditRoute.setText("Unassigned", false)
                spinnerEditRoute.isEnabled = false
                tilRoute.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_NONE
                
                spinnerEditStop.setText("Unassigned", false)
                spinnerEditStop.isEnabled = false
                tilStop.endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_NONE

                txtEditBusValue.text = "Unassigned"
            } else {
                spinnerEditRoute.setText(it.route, false)
                spinnerEditStop.setText(it.stopName ?: "Select Stop", false)
                txtEditBusValue.text = it.busNo ?: "No Bus Assigned"
                updateStopsForRoute(it.route, it.stopName)
            }

            if (it.profileImageUrl.isNotEmpty()) {
                Glide.with(this).load(it.profileImageUrl).placeholder(R.drawable.ic_person).into(imgStudentEdit)
            } else if (it.profileImage != 0) {
                imgStudentEdit.setImageResource(it.profileImage)
            } else {
                imgStudentEdit.setImageResource(R.drawable.ic_person)
            }
        }
    }

    private fun setupGradeSpinner() {
        val grades = arrayOf("Grade 9", "Grade 10", "Grade 11", "Grade 12", "BS IT 7th semester")
        val adapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, grades)
        spinnerEditGrade.setAdapter(adapter)
    }

    private fun setupRouteAndStopSpinners() {
        val routes = RouteRepository.routeList.value ?: listOf()
        val routeNames = routes.map { it.routeName }.toMutableList()
        routeNames.add(0, "None")

        val routeAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, routeNames)
        spinnerEditRoute.setAdapter(routeAdapter)

        spinnerEditRoute.setOnItemClickListener { parent, _, position, _ ->
            val selectedRouteName = parent.getItemAtPosition(position).toString()
            if (selectedRouteName != "None") {
                val busNo = RouteRepository.getBusForRoute(selectedRouteName)
                txtEditBusValue.text = if (busNo.isNotEmpty()) busNo else "No Bus Assigned"
                updateStopsForRoute(selectedRouteName, null)
            } else {
                txtEditBusValue.text = "No Bus Assigned"
                spinnerEditStop.setAdapter(null)
                spinnerEditStop.setText("Select Stop", false)
            }
        }
    }
    
    private fun updateStopsForRoute(routeName: String, currentStop: String?) {
        val routes = RouteRepository.routeList.value ?: return
        val selectedRoute = routes.find { it.routeName == routeName }
        val stopNames = selectedRoute?.stopsList?.map { it.stopName } ?: listOf()
        
        if (stopNames.isNotEmpty()) {
            val stopAdapter = ArrayAdapter(this, R.layout.spinner_dropdown_item, stopNames)
            spinnerEditStop.setAdapter(stopAdapter)
            
            if (currentStop != null && stopNames.contains(currentStop)) {
                spinnerEditStop.setText(currentStop, false)
            } else {
                spinnerEditStop.setText(stopNames[0], false)
            }
        } else {
            spinnerEditStop.setAdapter(null)
            spinnerEditStop.setText("No Stops Available", false)
        }
    }

    private fun setupClickListeners() {
        btnBack.setOnClickListener { 
            utils.ViewUtils.applyClickEffect(it)
            finish() 
        }
        btnCancelEdit.setOnClickListener { 
            utils.ViewUtils.applyClickEffect(it)
            finish() 
        }

        btnChangeImage.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            pickImageLauncher.launch("image/*")
        }

        btnUpdateStudent.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            if (validateForm()) {
                if (selectedImageUri != null) {
                    uploadAndSave()
                } else {
                    updateStudent(studentData?.profileImageUrl ?: "")
                }
            }
        }
    }

    private fun setupFormFormatting() {
        utils.FormUtils.setupTitleCaseInput(etEditFullName)
        utils.FormUtils.setupTitleCaseInput(etEditParentName)
    }

    private fun validateForm(): Boolean {
        if (etEditFullName.text.toString().trim().isEmpty()) {
            etEditFullName.error = "Name is required"
            return false
        }
        val phone = etEditEmergencyContact.text.toString().trim()
        if (!utils.FormUtils.isValidPhone(phone)) {
            etEditEmergencyContact.error = "Invalid contact (11 digits)"
            return false
        }
        return true
    }

    private fun uploadAndSave() {
        btnUpdateStudent.isEnabled = false
        Toast.makeText(this, "Uploading photo...", Toast.LENGTH_SHORT).show()

        utils.StorageUtils.uploadImage("student_profiles", selectedImageUri!!) { url ->
            if (url != null) {
                updateStudent(url)
            } else {
                btnUpdateStudent.isEnabled = true
                Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateStudent(imageUrl: String) {
        studentData?.let { oldData ->
            val route = spinnerEditRoute.text.toString()
            val finalRoute = if (route == "None" || route == "No Route Selected" || route == "Unassigned") null else route
            
            val stop = spinnerEditStop.text.toString()
            val finalStop = if (stop == "Select Stop" || stop == "No Stops Available" || stop == "Unassigned") null else stop
            
            val bus = txtEditBusValue.text.toString()
            val finalBus = if (bus == "No Bus Assigned" || bus == "Unassigned") null else bus

            val updatedStudent = oldData.copy(
                name = etEditFullName.text.toString().trim(),
                grade = spinnerEditGrade.text.toString(),
                fatherName = etEditParentName.text.toString().trim(),
                phoneNumber = etEditEmergencyContact.text.toString().trim(),
                location = etEditPickupAddress.text.toString().trim(),
                stopName = finalStop,
                route = finalRoute,
                busNo = finalBus,
                status = if (finalRoute != null) "ASSIGNED" else "UNASSIGNED",
                profileImageUrl = imageUrl
            )

            StudentRepository.updateStudent(updatedStudent) { success ->
                if (success) {
                    Toast.makeText(this, "Student updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    btnUpdateStudent.isEnabled = true
                    Toast.makeText(this, "Failed to update student", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}