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
import com.example.bustrack_app.adapter.ThemeAdapter
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.viewmodels.ThemeViewModel
import kotlinx.coroutines.launch
import ui.parent.ParentDashboardActivity

class PreferencesActivity : AppCompatActivity() {

    private val viewModel: ThemeViewModel by viewModels()
    private val authRepo = AuthRepository()
    private lateinit var adapter: ThemeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_preferences)

        // Setup Menu Button to go back and show drawer
        findViewById<ImageView>(R.id.btnMenu).setOnClickListener {
            handleBackToDashboard()
        }

        val recycler = findViewById<RecyclerView>(R.id.recyclerThemes)

        adapter = ThemeAdapter(emptyList()) { item ->
            viewModel.selectTheme(item.id)
        }

        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        viewModel.themes.observe(this) {
            adapter.updateList(it)
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
            
            val intent = Intent(this@PreferencesActivity, targetClass)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(intent)
            finish()
        }
    }
}