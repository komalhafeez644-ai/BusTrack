package ui_authentication

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
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
            val email = etEmail.text.toString().trim()

            if (email.isEmpty()) {
                etEmail.error = "Email is required"
                return@setOnClickListener
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.error = "Please enter a valid email address"
                return@setOnClickListener
            }

            // Show loading state
            setLoading(true, btnSendReset, progressBar)

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    setLoading(false, btnSendReset, progressBar)
                    
                    if (task.isSuccessful) {
                        // generic success message to prevent email enumeration
                        Toast.makeText(
                            this,
                            "If an account exists for this email, a password reset link has been sent.",
                            Toast.LENGTH_LONG
                        ).show()
                        
                        // Optionally close the activity after some delay or immediately
                        it.postDelayed({ finish() }, 2000)
                    } else {
                        val errorMessage = task.exception?.localizedMessage ?: "Failed to send reset email"
                        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
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
