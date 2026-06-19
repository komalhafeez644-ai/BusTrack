package com.example.bustrack.ui.driver

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack.databinding.DriverprofileBinding

class DriverProfileActivity : AppCompatActivity() {

    private lateinit var binding: DriverprofileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DriverprofileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }

        // Settings Navigation
        binding.optionAccount.setOnClickListener {
            startActivity(Intent(this, AccountDetailsActivity::class.java))
        }

        binding.optionNotifications.setOnClickListener {
            startActivity(Intent(this, NotificationSettingsActivity::class.java))
        }

        binding.optionNavigation.setOnClickListener {
            startActivity(Intent(this, NavigationPreferencesActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            // Add Logout Logic Here
            finishAffinity() 
        }
    }
}
