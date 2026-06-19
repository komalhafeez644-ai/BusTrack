package com.example.bustrack.ui.driver

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack.databinding.ActivityTracking1Binding

class navigationStart : AppCompatActivity() {

    private lateinit var binding: ActivityTracking1Binding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityTracking1Binding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnStartNav.setOnClickListener {
            // Sequence: Start Preview -> Live Navigation
            startActivity(Intent(this, navigationEnd::class.java))
            finish()
        }

        binding.btnMenu.setOnClickListener {
            finish() // Back to Dashboard
        }
    }
}
