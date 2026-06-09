package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.PrivacyPolicyAdapter
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.viewmodels.PrivacyPolicyViewModel
import kotlinx.coroutines.launch
import ui.parent.ParentDashboardActivity

class PrivacyPolicyActivityActivity : AppCompatActivity() {

    private lateinit var viewModel: PrivacyPolicyViewModel
    private lateinit var recyclerView: RecyclerView
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.privacy_policy_activity)

        // 1. RecyclerView ko initialize karein
        recyclerView = findViewById(R.id.rvPolicySections)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 2. ViewModel ko initialize karein
        viewModel = ViewModelProvider(this)[PrivacyPolicyViewModel::class.java]

        // 3. Data ko observe karein aur Adapter set karein
        viewModel.policyList.observe(this) { list ->
            val adapter = PrivacyPolicyAdapter(list)
            recyclerView.adapter = adapter
        }

        // 4. (Optional) Inquiry Button Logic
        val btnInquiry = findViewById<View>(R.id.btnInquiry)
        btnInquiry.setOnClickListener {
            // Yahan email intent ya support page ka code likhein
        }

        // Menu button logic - Opens Dashboard with Drawer open
        findViewById<View>(R.id.btnMenu)?.setOnClickListener {
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
        }

        lifecycleScope.launch {
            val role = authRepo.getCurrentUserRole()
            val targetClass = if (role == "admin") AdminDashboardActivity::class.java else ParentDashboardActivity::class.java
            
            val intent = Intent(this@PrivacyPolicyActivityActivity, targetClass)
            intent.putExtra("OPEN_DRAWER", true)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }
    }
}