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
import ui_authentication.LoginActivity

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

        // Delay for 3 seconds then decide navigation
        Handler(Looper.getMainLooper()).postDelayed({
            checkSessionAndNavigate()
        }, 3000)
    }

    private fun checkSessionAndNavigate() {
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val onboardingCompleted = sharedPref.getBoolean("onboardingCompleted", false)

        if (!onboardingCompleted) {
            // First time user, show Intro
            startActivity(Intent(this, IntroActivity::class.java))
            finish()
            return
        }

        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            // User is logged in, fetch role and go to dashboard
            lifecycleScope.launch {
                try {
                    val role = AuthRepository().getCurrentUserRole()
                    val targetClass = when (role) {
                        "admin" -> ui.admin.AdminDashboardActivity::class.java
                        "principal" -> ui.principal.PrincipalDashboardActivity::class.java
                        "driver" -> ui.driver.DriverDashboardActivity::class.java
                        else -> ui.parent.ParentDashboardActivity::class.java
                    }
                    startActivity(Intent(this@SplashActivity, targetClass))
                    finish()
                } catch (e: Exception) {
                    // Fallback to Login if role fetch fails
                    startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                    finish()
                }
            }
        } else {
            // Onboarding completed but not logged in, show Login
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}
