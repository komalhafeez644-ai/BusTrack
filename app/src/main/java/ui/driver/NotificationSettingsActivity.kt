package ui.driver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.NotificationsettingsBinding
import utils.ViewUtils

class NotificationSettingsActivity : AppCompatActivity() {
    private lateinit var binding: NotificationsettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = NotificationsettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnMenu.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }

        binding.btnSavePreferences.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            saveSettings()
        }
    }

    private fun saveSettings() {
        // Logic to save preferences locally or to database
        Toast.makeText(this, "Notification settings saved successfully!", Toast.LENGTH_SHORT).show()
        
        // Return to dashboard after saving
        val intent = Intent(this, DriverDashboardActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
        finish()
    }
}