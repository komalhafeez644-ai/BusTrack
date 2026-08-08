package com.example.bustrack_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.data.AuthRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import ui.admin.IntroActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Hide action bar
        supportActionBar?.hide()

        // Pre-fetch user role during splash to speed up dashboard entry
        if (Firebase.auth.currentUser != null) {
            lifecycleScope.launch {
                AuthRepository().getCurrentUserRole()
            }
        }

        // Delay for 3 seconds then go to IntroActivity
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, IntroActivity::class.java)
            startActivity(intent)
            finish()
        }, 3000)
    }
}
