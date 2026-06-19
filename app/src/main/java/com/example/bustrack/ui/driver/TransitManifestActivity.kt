package com.example.bustrack.ui.driver

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack.databinding.CampustransitBinding

class TransitManifestActivity : AppCompatActivity() {
    private lateinit var binding: CampustransitBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = CampustransitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.headerCard.setOnClickListener { finish() }
    }
}
