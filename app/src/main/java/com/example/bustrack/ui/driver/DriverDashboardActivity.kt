package com.example.bustrack.ui.driver

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack.R
import com.example.bustrack.databinding.DriverdashboardBinding

class DriverDashboardActivity : AppCompatActivity() {

    private lateinit var binding: DriverdashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DriverdashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
        setupBottomNavigation()
    }

    private fun setupNavigation() {
        // Quick Actions Navigation
        binding.actionTracking.setOnClickListener {
            startActivity(Intent(this, navigationStart::class.java))
        }

        binding.actionAttendance.setOnClickListener {
            startActivity(Intent(this, AttendanceActivity::class.java))
        }

        binding.actionAlerts.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        binding.actionRoutes.setOnClickListener {
            startActivity(Intent(this, navigationStart::class.java))
        }

        // Other Buttons
        binding.btnViewRoute.setOnClickListener {
            startActivity(Intent(this, navigationStart::class.java))
        }

        binding.cardNotification.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        binding.ivProfile.setOnClickListener {
            startActivity(Intent(this, DriverProfileActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.selectedItemId = R.id.nav_dashboard
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> true
                R.id.nav_route -> {
                    startActivity(Intent(this, navigationStart::class.java))
                    true
                }
                R.id.nav_students -> {
                    startActivity(Intent(this, AttendanceActivity::class.java))
                    true
                }
                R.id.nav_alerts -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, DriverProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
