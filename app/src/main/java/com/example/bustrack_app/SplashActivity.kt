package com.example.bustrack_app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.data.AuthRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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
            // Firebase persists this session across process/app restarts. A temporary
            // reload/network failure must not turn a valid signed-in driver into a
            // forced-login flow.
            lifecycleScope.launch {
                try {
                    try {
                        currentUser.reload().await()
                    } catch (e: FirebaseAuthInvalidUserException) {
                        Firebase.auth.signOut()
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        finish()
                        return@launch
                    }
                    val cleanEmail = currentUser.email?.trim()?.lowercase() ?: ""
                    val authRepo = AuthRepository()
                    val cachedRole = sharedPref.getString("authenticated_role_${currentUser.uid}", null)
                    val fetchedRole = authRepo.getCurrentUserRole()
                    // getCurrentUserRole intentionally falls back to parent when
                    // Firestore is temporarily unreachable. Do not overwrite a
                    // known driver session with that offline fallback.
                    val role = if (cachedRole == "driver" && fetchedRole == "parent") {
                        cachedRole
                    } else {
                        fetchedRole
                    }

                    sharedPref.edit()
                        .putString("authenticated_role_${currentUser.uid}", role)
                        .apply()

                    val isExempt = role == "driver" || role == "admin" || role == "principal" || 
                        cleanEmail == "admin@gmail.com" || cleanEmail == "principal@gmail.com"

                    if (!isExempt && !currentUser.isEmailVerified) {
                        Firebase.auth.signOut()
                        startActivity(Intent(this@SplashActivity, LoginActivity::class.java))
                        finish()
                        return@launch
                    }

                    authRepo.syncFcmToken(currentUser.uid, role)

                    val targetClass = when (role) {
                        "admin" -> ui.admin.AdminDashboardActivity::class.java
                        "principal" -> ui.principal.PrincipalDashboardActivity::class.java
                        "driver" -> ui.driver.DriverDashboardActivity::class.java
                        else -> ui.parent.ParentDashboardActivity::class.java
                    }
                    val targetIntent = Intent(this@SplashActivity, targetClass)
                    // Forward any notification routing extras if SplashActivity was launched via notification tap
                    intent.extras?.let { targetIntent.putExtras(it) }
                    startActivity(targetIntent)
                    finish()
                } catch (e: Exception) {
                    // If Firestore is temporarily unavailable, Firebase still has a
                    // valid authenticated session. Reuse the role saved on its last
                    // successful entry rather than asking the driver to log in again.
                    val cachedRole = sharedPref.getString("authenticated_role_${currentUser.uid}", null)
                    val targetClass = when (cachedRole) {
                        "admin" -> ui.admin.AdminDashboardActivity::class.java
                        "principal" -> ui.principal.PrincipalDashboardActivity::class.java
                        "driver" -> ui.driver.DriverDashboardActivity::class.java
                        "parent" -> ui.parent.ParentDashboardActivity::class.java
                        else -> LoginActivity::class.java
                    }
                    startActivity(Intent(this@SplashActivity, targetClass))
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
