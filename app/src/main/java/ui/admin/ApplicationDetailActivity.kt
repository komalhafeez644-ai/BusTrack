package ui.admin
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
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

            utils.ImageUtils.loadProfileImage(this, applicationData.profileImageUrl, binding.ivStudentProfile)
            
            // Status Color Logic for Detail Screen
            if (applicationData.status == "Approved") {
                binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                binding.cardAssignmentDetails.visibility = android.view.View.VISIBLE
                binding.cardNotAssigned.visibility = android.view.View.GONE
                
                binding.btnApprove.text = "Close"
                binding.btnApprove.setIconResource(R.drawable.ic_close)
                binding.btnReject.text = "Edit"
                binding.btnReject.setIconResource(R.drawable.outline_edit_24)
            } else {
                // Pending status (Matches badge color from item list)
                binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF3C7"))
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#92400E"))
                binding.cardAssignmentDetails.visibility = android.view.View.GONE
                binding.cardNotAssigned.visibility = android.view.View.VISIBLE

                binding.btnApprove.text = "Reject"
                binding.btnApprove.setIconResource(R.drawable.ic_close)
                binding.btnReject.text = "View Analytic"
                binding.btnReject.setIconResource(R.drawable.logistics) // Using logistics icon for analytics
            }
            
            // Optionally still load more details if needed
            viewModel.loadApplicationDetail(applicationData.studentName, applicationData.status)
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

            // Handle Assignment Card Visibility in Live Observer
            if (data.status == "Approved") {
                binding.cardAssignmentDetails.visibility = android.view.View.VISIBLE
                binding.cardNotAssigned.visibility = android.view.View.GONE
                binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#E8F5E9"))
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#4CAF50"))
                
                binding.btnApprove.text = "Close"
                binding.btnApprove.setIconResource(R.drawable.ic_close)
                binding.btnReject.text = "Edit"
                binding.btnReject.setIconResource(R.drawable.outline_edit_24)
            } else {
                binding.cardAssignmentDetails.visibility = android.view.View.GONE
                binding.cardNotAssigned.visibility = android.view.View.VISIBLE
                binding.tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF3C7"))
                binding.tvStatus.setTextColor(android.graphics.Color.parseColor("#92400E"))

                binding.btnApprove.text = "Reject"
                binding.btnApprove.setIconResource(R.drawable.ic_close)
                binding.btnReject.text = "View Analytic"
                binding.btnReject.setIconResource(R.drawable.logistics)
            }
        }
    }

    private fun clickListeners() {

        binding.btnApprove.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val applicationData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel

            if (applicationData?.status == "Pending") {
                // Logic for Rejection
                Toast.makeText(this, "Application Rejected", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                // Logic for Close (Approved state)
                finish()
            }
        }

        binding.btnReject.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val applicationData = intent.getSerializableExtra("APPLICATION_DATA") as? com.example.bustrack_app.models.ApplicationModel
            
            if (applicationData?.status == "Pending") {
                // Open Analytics for Pending applications
                val intent = android.content.Intent(this, RouteAnalysisActivity::class.java)
                intent.putExtra("APPLICATION_DATA", applicationData)
                startActivity(intent)
            } else {
                // Opening Edit Assignment screen for Approved applications
                val intent = android.content.Intent(this, EditAssignmentActivity::class.java)
                intent.putExtra("APPLICATION_DATA", applicationData)
                startActivity(intent)
            }
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