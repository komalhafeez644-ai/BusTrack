package ui.admin
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.viewmodels.ApplicationDetailViewModel
import com.example.bustrack_app.databinding.ActivityApplicationDetailBinding

class ApplicationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityApplicationDetailBinding

    private val viewModel: ApplicationDetailViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityApplicationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        observeData()
        clickListeners()
    }

    override fun onResume() {
        super.onResume()
        loadData()
    }

    private fun loadData() {
        val originalData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
        
        // Find updated data from global list
        val applicationData = BusApplicationsActivity.applicationsList.find { it.id == originalData?.id } ?: originalData

        if (applicationData != null) {
            // Populate initial UI with passed data
            binding.tvStudentName.text = applicationData.studentName
            binding.tvStatus.text = applicationData.status
            binding.tvApplicationId.text = "#BT-${applicationData.id}"
            binding.tvDateTime.text = applicationData.time
            binding.tvPickupAddress.text = applicationData.pickupPoint
            binding.tvStudentInfo.text = applicationData.studentClass
            binding.tvParentName.text = "Parent: ${applicationData.parentName}"
            binding.tvPhone.text = applicationData.contactNumber
            binding.tvRouteName.text = applicationData.bestRoute
            binding.tvDistance.text = applicationData.distance
            binding.tvNearestStop.text = applicationData.nearestStop

            if (applicationData.image != 0) {
                binding.ivStudentProfile.setImageResource(applicationData.image)
                binding.ivStudentProfile.setPadding(0, 0, 0, 0)
            }
            
            // Status Color Logic for Detail Screen
            if (applicationData.status == "Approved") {
                binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
            } else {
                binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF3C7"))
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#D97706"))
            }
            
            // Optionally still load more details if needed
            viewModel.loadApplicationDetail(applicationData.studentName)
        } else {
            val studentName = intent.getStringExtra("STUDENT_NAME")
            viewModel.loadApplicationDetail(studentName)
        }
    }

    private fun observeData() {

        viewModel.applicationDetail.observe(this) { data ->

            binding.tvApplicationId.text = data.applicationId
            binding.tvStatus.text = data.status
            binding.tvDateTime.text = data.dateTime

            binding.tvStudentName.text = data.studentName
            binding.tvStudentInfo.text = data.studentInfo
            binding.tvParentName.text = data.parentName
            binding.tvPhone.text = data.phone

            binding.tvPickupAddress.text = data.pickupAddress
            binding.tvCity.text = data.city

            binding.tvRouteName.text = data.routeName
            binding.tvDistance.text = data.distance
            binding.tvNearestStop.text = data.nearestStop
        }
    }

    private fun clickListeners() {

        binding.btnApprove.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            // Add to Student Repository on Approval
            val originalData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
            originalData?.let { app ->
                val student = com.example.bustrack_app.models.StudentModel(
                    id = "#SR-${1000 + app.id}",
                    name = app.studentName,
                    grade = app.studentClass,
                    location = app.nearestStop,
                    route = app.bestRoute,
                    busNo = com.example.bustrack_app.data.RouteRepository.getBusForRoute(app.bestRoute),
                    status = "ASSIGNED",
                    profileImage = app.image,
                    fatherName = app.parentName,
                    phoneNumber = app.contactNumber
                )
                com.example.bustrack_app.data.StudentRepository.updateStudent(student)
            }
            Toast.makeText(this, "Application Approved & Student Added", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnReject.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            // Opening Edit Assignment screen
            val applicationData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
            val intent = android.content.Intent(this, EditAssignmentActivity::class.java)
            intent.putExtra("APPLICATION_DATA", applicationData)
            startActivity(intent)
        }

        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        binding.btnViewOnMap.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            // Logic to show map
        }
    }
}