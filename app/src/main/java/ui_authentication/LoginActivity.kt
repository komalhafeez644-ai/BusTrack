package ui_authentication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
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
import com.google.android.material.textfield.TextInputLayout
import ui.admin.AdminDashboardActivity
import ui.principal.PrincipalDashboardActivity
// IMPORT ADD KIYA: Taake intent ko pata chale Signup Activity kahan hai

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            // Reset errors
            tilEmail.error = null
            tilPassword.error = null

            if (email.isEmpty()) {
                tilEmail.error = "Email is required"
            } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                tilEmail.error = "Invalid email format"
            } else if (password.isEmpty()) {
                tilPassword.error = "Password is required"
            } else if (password.length < 6) {
                tilPassword.error = "Password must be at least 6 characters"
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
                    Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    val role = resource.data
                    when (role) {
                        "admin" -> {
                            Toast.makeText(this, "Admin Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, AdminDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        "principal" -> {
                            Toast.makeText(this, "Principal Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, PrincipalDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        "driver" -> {
                            Toast.makeText(this, "Driver Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, ui.driver.DriverDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        else -> {
                            Toast.makeText(this, "Parent Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, ui.parent.ParentDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Login Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}