package com.example.bustrack.ui.driver

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack.R
import com.example.bustrack.databinding.Activitytracking2Binding

class navigationEnd : AppCompatActivity() {

    private lateinit var binding: Activitytracking2Binding
    private var isMuted = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = Activitytracking2Binding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        setupBottomNavigation()
    }

    private fun setupListeners() {
        // End Navigation Button
        binding.btnEndNavigation.setOnClickListener {
            Toast.makeText(this, "Navigation Ended", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Mute Button
        binding.btnMute.setOnClickListener {
            isMuted = !isMuted
            Toast.makeText(this, if (isMuted) "Voice Muted" else "Voice On", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    finish()
                    true
                }
                else -> {
                    Toast.makeText(this, "Item ${item.title} Clicked", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }
}
