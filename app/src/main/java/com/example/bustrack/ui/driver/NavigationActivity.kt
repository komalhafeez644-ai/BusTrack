package com.example.bustrack.ui.driver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack.R
import com.example.bustrack.databinding.ActivityNavigationBinding

class NavigationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNavigationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNavigationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup Bottom Navigation
        binding.bottomNav.selectedItemId = R.id.nav_stops
        
        // Setup Notification Badge
        val badge = binding.bottomNav.getOrCreateBadge(R.id.nav_messages)
        badge.isVisible = true
        badge.number = 3
        badge.backgroundColor = getColor(R.color.status_red)

        binding.btnEndNavigation.setOnClickListener {
            Toast.makeText(this, "Navigation Ended", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnMenu.setOnClickListener {
            Toast.makeText(this, "Opening Menu", Toast.LENGTH_SHORT).show()
        }
    }
}
