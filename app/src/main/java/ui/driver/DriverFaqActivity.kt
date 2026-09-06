package ui.driver

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.FaqAdapter
import com.example.bustrack_app.models.FaqModel
import com.example.bustrack_app.data.DriverRepository
import com.google.firebase.auth.FirebaseAuth
import com.bumptech.glide.Glide
import android.widget.ImageView

class DriverFaqActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_faq)

        setupRecyclerView()
        setupHeader()

        findViewById<View>(R.id.btnChatWithUs)?.setOnClickListener {
            startActivity(Intent(this, ui.chatbot.ChatbotActivity::class.java))
        }

        findViewById<View>(R.id.btnBack).setOnClickListener {
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        val recyclerView = findViewById<RecyclerView>(R.id.rvFaq)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        val faqList = listOf(
            FaqModel(getString(R.string.driver_faq_q1), getString(R.string.driver_faq_a1)),
            FaqModel(getString(R.string.driver_faq_q2), getString(R.string.driver_faq_a2)),
            FaqModel(getString(R.string.driver_faq_q3), getString(R.string.driver_faq_a3)),
            FaqModel(getString(R.string.driver_faq_q4), getString(R.string.driver_faq_a4)),
            FaqModel(getString(R.string.driver_faq_q5), getString(R.string.driver_faq_a5)),
            FaqModel(getString(R.string.driver_faq_q6), getString(R.string.driver_faq_a6))
        )

        recyclerView.adapter = FaqAdapter(faqList)
    }

    private fun setupHeader() {
        val ivDriver = findViewById<ImageView>(R.id.ivDriverPhoto)
        val email = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase()
        if (email != null) {
            DriverRepository.driverList.value?.find { it.email.trim().lowercase() == email }?.let { driver ->
                utils.ImageUtils.loadProfileImage(this, driver.profileImageUrl, ivDriver)
            }
        }
    }
}
