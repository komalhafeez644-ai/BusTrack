package ui_authentication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.R
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import utils.ViewUtils

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var oobCode: String? = null
    private var verifiedEmail: String? = null

    private lateinit var tilNewPassword: TextInputLayout
    private lateinit var tilConfirmPassword: TextInputLayout
    private lateinit var etNewPassword: TextInputEditText
    private lateinit var etConfirmPassword: TextInputEditText
    private lateinit var btnSetPassword: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var tvAccountEmail: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        auth = FirebaseAuth.getInstance()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val layoutBackToLogin = findViewById<LinearLayout>(R.id.layoutBackToLogin)
        tilNewPassword = findViewById(R.id.tilNewPassword)
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword)
        etNewPassword = findViewById(R.id.etNewPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        btnSetPassword = findViewById(R.id.btnSetPassword)
        progressBar = findViewById(R.id.resetProgress)
        tvAccountEmail = findViewById(R.id.tvAccountEmail)

        btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        layoutBackToLogin.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            navigateToLogin()
        }

        btnSetPassword.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleSetPassword()
        }

        extractAndVerifyCode()
    }

    private fun extractAndVerifyCode() {
        val data = intent.data
        oobCode = data?.getQueryParameter("oobCode") ?: intent.getStringExtra("oobCode")
        val mode = data?.getQueryParameter("mode") ?: intent.getStringExtra("mode")

        if (oobCode.isNullOrBlank()) {
            showInvalidLinkDialog("Invalid link. No verification code was found.")
            return
        }

        setLoading(true)

        lifecycleScope.launch {
            try {
                // Verify the action code with Firebase Auth
                val email = auth.verifyPasswordResetCode(oobCode!!).await()
                verifiedEmail = email
                setLoading(false)
                tvAccountEmail.text = "Account: $email"
            } catch (e: Exception) {
                setLoading(false)
                val message = when (e) {
                    is FirebaseAuthActionCodeException -> "This link is expired or has already been used. Please ask Admin or use Forgot Password to receive a new link."
                    is FirebaseNetworkException -> "Network error. Please check your internet connection."
                    else -> e.localizedMessage ?: "Invalid or expired setup link."
                }
                showInvalidLinkDialog(message)
            }
        }
    }

    private fun handleSetPassword() {
        val code = oobCode
        if (code.isNullOrBlank()) {
            showInvalidLinkDialog("Missing or expired setup code.")
            return
        }

        val newPass = etNewPassword.text.toString()
        val confirmPass = etConfirmPassword.text.toString()

        tilNewPassword.error = null
        tilConfirmPassword.error = null

        var isValid = true

        // 1. Minimum 8 characters
        if (newPass.isEmpty()) {
            tilNewPassword.error = "Password is required"
            isValid = false
        } else if (newPass.length < 8) {
            tilNewPassword.error = "Password must be at least 8 characters"
            isValid = false
        } else if (!newPass.any { it.isUpperCase() }) {
            // 2. At least 1 uppercase letter
            tilNewPassword.error = "Password must contain at least 1 uppercase letter"
            isValid = false
        } else if (!newPass.any { it.isDigit() }) {
            // 3. At least 1 number
            tilNewPassword.error = "Password must contain at least 1 number"
            isValid = false
        }

        // 4. Confirm Password must match
        if (confirmPass.isEmpty()) {
            tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (newPass != confirmPass) {
            tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (!isValid) return

        setLoading(true)

        lifecycleScope.launch {
            try {
                // Confirm the new password with Firebase Authentication
                auth.confirmPasswordReset(code, newPass).await()
                setLoading(false)

                AlertDialog.Builder(this@ResetPasswordActivity)
                    .setTitle("Password Created Successfully")
                    .setMessage("Your password has been set. You can now log in to BusTrack with your email and password.")
                    .setCancelable(false)
                    .setPositiveButton("Go to Login") { dialog, _ ->
                        dialog.dismiss()
                        navigateToLogin()
                    }
                    .show()
            } catch (e: Exception) {
                setLoading(false)
                val errorMessage = when (e) {
                    is FirebaseAuthActionCodeException -> "This setup link has expired. Please request a new one."
                    is FirebaseAuthInvalidCredentialsException -> "Password does not meet security criteria."
                    is FirebaseNetworkException -> "Network error. Please check your internet connection."
                    is FirebaseAuthException -> e.localizedMessage ?: "Failed to set password. Please try again."
                    else -> e.localizedMessage ?: "Failed to set password."
                }
                Toast.makeText(this@ResetPasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showInvalidLinkDialog(message: String) {
        btnSetPassword.isEnabled = false
        etNewPassword.isEnabled = false
        etConfirmPassword.isEnabled = false

        AlertDialog.Builder(this)
            .setTitle("Link Expired or Invalid")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("Back to Login") { dialog, _ ->
                dialog.dismiss()
                navigateToLogin()
            }
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        startActivity(intent)
        finish()
    }

    private fun setLoading(loading: Boolean) {
        if (loading) {
            btnSetPassword.isEnabled = false
            btnSetPassword.text = ""
            progressBar.visibility = View.VISIBLE
        } else {
            btnSetPassword.isEnabled = true
            btnSetPassword.text = "Set Password"
            progressBar.visibility = View.GONE
        }
    }
}