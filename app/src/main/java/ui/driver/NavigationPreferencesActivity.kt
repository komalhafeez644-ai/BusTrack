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
}
