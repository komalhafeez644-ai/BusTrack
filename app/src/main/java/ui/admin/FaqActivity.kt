package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.FaqAdapter
import com.example.bustrack_app.models.FaqModel
import ui.driver.DriverDashboardActivity
import ui.parent.ParentDashboardActivity
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.data.AuthRepository
import kotlinx.coroutines.launch

class FaqActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_faq)

        val recyclerView = findViewById<RecyclerView>(R.id.rvFaq)
        recyclerView.layoutManager = LinearLayoutManager(this)
        
        val faqList = listOf(
            FaqModel(getString(R.string.faq_q1), getString(R.string.faq_a1)),
            FaqModel(getString(R.string.faq_q2), getString(R.string.faq_a2)),
            FaqModel(getString(R.string.faq_q3), getString(R.string.faq_a3)),
            FaqModel(getString(R.string.faq_q4), getString(R.string.faq_a4)),
            FaqModel(getString(R.string.faq_q5), getString(R.string.faq_a5)),
            FaqModel(getString(R.string.faq_q6), getString(R.string.faq_a6)),
            FaqModel(getString(R.string.faq_q7), getString(R.string.faq_a7)),
            FaqModel(getString(R.string.faq_q8), getString(R.string.faq_a8)),
            FaqModel(getString(R.string.faq_q9), getString(R.string.faq_a9))
        )

        recyclerView.adapter = FaqAdapter(faqList)

        // Task 6: Help & Support Chatbot - reuses this existing "Contact Support" button
        // instead of adding new UI. btnApiDocs is left as-is (unrelated to this task).
        findViewById<View>(R.id.btnSupport)?.setOnClickListener {
            startActivity(Intent(this, ui.chatbot.ChatbotActivity::class.java))
        }

        findViewById<View>(R.id.btnMenu).setOnClickListener {
            handleBackToDashboard()
        }
    }

    private fun handleBackToDashboard() {
        val fromUser = intent.getStringExtra("FROM_USER")
        if (fromUser == "parent") {
            val intent = Intent(this, ParentDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        } else if (fromUser == "admin") {
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        } else if (fromUser == "driver") {
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        }

        lifecycleScope.launch {
            val role = AuthRepository().getCurrentUserRole()
            val targetClass = when (role) {
                "admin" -> AdminDashboardActivity::class.java
                "driver" -> DriverDashboardActivity::class.java
                "principal" -> ui.principal.PrincipalDashboardActivity::class.java
                else -> ParentDashboardActivity::class.java
            }
            
            val intent = Intent(this@FaqActivity, targetClass)
            intent.putExtra("OPEN_DRAWER", true)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }
    }
}