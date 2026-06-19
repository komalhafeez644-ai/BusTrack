package com.example.bustrack.ui.driver

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.bustrack.R
import com.example.bustrack.databinding.AttendanceBinding

class AttendanceActivity : AppCompatActivity() {

    private lateinit var binding: AttendanceBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        binding = AttendanceBinding.inflate(layoutInflater)
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
        binding.btnMenu.setOnClickListener {
            finish()
        }
        
        // Clicking on a student or stop row opens the detailed manifest
        binding.ivStudent1.parent.let { it as android.view.View }.setOnClickListener {
            startActivity(Intent(this, TransitManifestActivity::class.java))
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNav.selectedItemId = R.id.nav_students
        
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    finish()
                    true
                }
                R.id.nav_students -> true
                R.id.nav_alerts -> {
                    startActivity(Intent(this, NotificationActivity::class.java))
                    true
                }
                R.id.nav_profile -> {
                    startActivity(Intent(this, DriverProfileActivity::class.java))
                    true
                }
                else -> {
                    Toast.makeText(this, "${item.title} coming soon!", Toast.LENGTH_SHORT).show()
                    true
                }
            }
        }
    }
}
