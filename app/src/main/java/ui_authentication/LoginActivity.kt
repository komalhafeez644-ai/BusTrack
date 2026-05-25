package ui_authentication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.login.ForgotPasswordActivity
import com.example.bustrack_app.utils.Resource
import com.example.bustrack_app.viewmodels.LoginViewModel
import com.google.android.material.textfield.TextInputEditText
import ui.admin.AdminDashboardActivity
// IMPORT ADD KIYA: Taake intent ko pata chale Signup Activity kahan hai

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.login(email, password)
            }
        }

        // UPDATED: Sahi class name (SignupActivity) aur package ke saath link kiya
        tvSignUp.setOnClickListener {
            val intent = Intent(this, SignupActivity::class.java)
            startActivity(intent)
        }

        tvForgotPassword.setOnClickListener {
            // Confirm karein ke ForgotPasswordActivity bhi registered hai
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        btnGoogle.setOnClickListener {
            viewModel.loginWithGoogle()
        }

        observeLogin()
    }

    private fun observeLogin() {
        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    Toast.makeText(this, "Connecting to Google...", Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, AdminDashboardActivity::class.java)
                    startActivity(intent)
                    finish()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}