package ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.NotificationAdapter
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.viewmodels.NotificationViewModel
import kotlinx.coroutines.launch
import ui.parent.ParentDashboardActivity
import ui.driver.DriverDashboardActivity

class NotificationSettingsActivity : AppCompatActivity() {

    private val viewModel: NotificationViewModel by viewModels()
    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_settings)

        // Setup Menu Button to go back and show drawer
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            handleBackToDashboard()
        }

        val recyclerView = findViewById<RecyclerView>(R.id.rvNotifications)
        recyclerView.layoutManager = LinearLayoutManager(this)

        viewModel.settings.observe(this) { list ->
            recyclerView.adapter = NotificationAdapter(list)
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
        } else if (fromUser == "driver") {
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        }

        lifecycleScope.launch {
            val role = authRepo.getCurrentUserRole()
            val targetClass = when (role) {
                "admin" -> AdminDashboardActivity::class.java
                "driver" -> DriverDashboardActivity::class.java
                else -> ParentDashboardActivity::class.java
            }
            
            val intent = Intent(this@NotificationSettingsActivity, targetClass)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }
}