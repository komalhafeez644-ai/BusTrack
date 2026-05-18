package ui.admin
import android.os.Bundle
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

        viewModel.loadApplicationDetail()
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
            viewModel.approveApplication()
        }

        binding.btnReject.setOnClickListener {
            viewModel.rejectApplication()
        }

        binding.btnBack.setOnClickListener {
            finish()
        }
    }
}