package com.example.bustrack_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import ui.admin.IntroActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide action bar
        supportActionBar?.hide()

        // Initialize Repositories with persistence
        com.example.bustrack_app.data.DriverRepository.init(this)
        com.example.bustrack_app.data.StudentRepository.init(this)

        // Delay for 3 seconds then go to IntroActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
