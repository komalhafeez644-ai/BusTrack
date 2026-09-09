package ui.driver

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.NavigationpreferencesBinding

class NavigationPreferencesActivity : AppCompatActivity() {

    private lateinit var binding: NavigationpreferencesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = NavigationpreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()
        setupListeners()

        val preferences = getSharedPreferences(VOICE_PREFS, MODE_PRIVATE)
        binding.switch3.isChecked = preferences.getBoolean(VOICE_ENABLED_KEY, true)

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            finish()
        }
        
        binding.switch1.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(this, "Live Tracking: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }

        binding.switch3.setOnCheckedChangeListener { _, isChecked ->
            getSharedPreferences(VOICE_PREFS, MODE_PRIVATE)
                .edit()
                .putBoolean(VOICE_ENABLED_KEY, isChecked)
                .apply()
            Toast.makeText(this, "Voice navigation: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_profile
        
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    finish() 
                    true
                }
                R.id.nav_profile -> true
                else -> {
                    Toast.makeText(this, "${item.title} coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }

    private companion object {
        const val VOICE_PREFS = "navigation_preferences"
        const val VOICE_ENABLED_KEY = "voice_enabled"
    }
}
