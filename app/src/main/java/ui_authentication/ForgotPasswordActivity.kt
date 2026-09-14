package ui_authentication

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthActionCodeException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import utils.ViewUtils

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private var isSending = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        auth = FirebaseAuth.getInstance()

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val layoutBackToLogin = findViewById<LinearLayout>(R.id.layoutBackToLogin)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val btnSendReset = findViewById<Button>(R.id.btnSendReset)
        val progressBar = findViewById<ProgressBar>(R.id.resetProgress)

        btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        layoutBackToLogin.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        btnSendReset.setOnClickListener {
            if (isSending) return@setOnClickListener

            ViewUtils.applyClickEffect(it)
            val email = etEmail.text.toString().trim().lowercase()

            etEmail.error = null

            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Please enter a valid email address (e.g. example@gmail.com)"
                return@setOnClickListener
            }

            // Show loading state
            setLoading(true, btnSendReset, progressBar)

            // Firebase sendPasswordResetEmail handles all 4 roles (Admin, Principal, Driver, Parent)
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    setLoading(false, btnSendReset, progressBar)

                    if (task.isSuccessful) {
                        showModernSuccessDialog(email)
                    } else {
                        val errorMessage = getFirebaseErrorMessage(task.exception)
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }
    }

    private fun showModernSuccessDialog(email: String) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.dialog_password_reset_sent)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val tvSentEmail = dialog.findViewById<TextView>(R.id.tvSentEmail)
        val btnBackToLogin = dialog.findViewById<Button>(R.id.btnBackToLogin)

        tvSentEmail.text = email

        btnBackToLogin.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            dialog.dismiss()
            finish()
        }

        dialog.show()
        val width = (resources.displayMetrics.widthPixels * 0.90).toInt()
        dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun getFirebaseErrorMessage(exception: Exception?): String {
        return when (exception) {
            is FirebaseAuthInvalidUserException -> "No account found with this email address."
            is FirebaseAuthInvalidCredentialsException -> "The email address is invalid."
            is FirebaseNetworkException -> "Network error. Please check your internet connection."
            is FirebaseAuthActionCodeException -> "The reset link is expired or invalid."
            is FirebaseAuthException -> {
                when (exception.errorCode) {
                    "ERROR_USER_NOT_FOUND", "user-not-found" -> "No account found with this email address."
                    "ERROR_INVALID_EMAIL", "invalid-email" -> "The email address is invalid."
                    "ERROR_NETWORK_REQUEST_FAILED", "network-request-failed" -> "Network error. Please check your internet connection."
                    "ERROR_TOO_MANY_REQUESTS", "too-many-requests" -> "Too many attempts. Please try again later."
                    "ERROR_INVALID_ACTION_CODE", "invalid-action-code" -> "The reset link is expired or invalid."
                    else -> exception.localizedMessage ?: "Failed to send reset email. Please try again."
                }
            }
            else -> exception?.localizedMessage ?: "Failed to send reset email. Please try again."
        }
    }

    private fun setLoading(loading: Boolean, button: Button, progressBar: ProgressBar) {
        isSending = loading
        if (loading) {
            button.isEnabled = false
            button.text = ""
            progressBar.visibility = View.VISIBLE
        } else {
            button.isEnabled = true
            button.text = "Send Reset Link"
            progressBar.visibility = View.GONE
        }
    }
}
