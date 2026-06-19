package com.example.bustrack.ui.driver

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack.databinding.DriveraccountdetailsBinding

class AccountDetailsActivity : AppCompatActivity() {
    private lateinit var binding: DriveraccountdetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DriveraccountdetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnBack.setOnClickListener { finish() }
    }
}
