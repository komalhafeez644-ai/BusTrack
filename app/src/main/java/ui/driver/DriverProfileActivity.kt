package ui.driver

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.DriverprofileBinding
import com.example.bustrack_app.data.DriverRepository
import com.google.firebase.auth.FirebaseAuth
import utils.ViewUtils

class DriverProfileActivity : AppCompatActivity() {

    private lateinit var binding: DriverprofileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DriverprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.parseColor("#0D1B3E")

        setupClickListeners()
        observeDriverRepo()
    }

    private fun observeDriverRepo() {
        DriverRepository.driverList.observe(this) { drivers ->
            val email = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase() ?: return@observe
            drivers.find { it.email.trim().lowercase() == email }?.let { driver ->
                binding.tvDriverName.text = driver.name
                binding.tvInfoEmail.text = driver.email
                binding.tvInfoPhone.text = driver.phone.ifEmpty { "Not provided" }
                binding.tvInfoDriverID.text = "DRIVER ID: ${driver.id}"
                
                binding.tvInfoBus.text = driver.assignedBus ?: "Not Assigned"
                binding.tvInfoRoute.text = driver.route ?: "No Route"

                utils.ImageUtils.loadProfileImage(this, driver.profileImageUrl, binding.imgProfile)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish() 
        }

        binding.btnEditProfile.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, EditDriverProfileActivity::class.java))
        }
    }
}