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

        findViewById<View>(R.id.btnMenu).setOnClickListener {
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }
    }
}