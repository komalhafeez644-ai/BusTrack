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
import com.example.bustrack_app.utils.Resource
import com.example.bustrack_app.viewmodels.LoginViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import ui.admin.AdminDashboardActivity
import ui.principal.PrincipalDashboardActivity
import utils.ViewUtils

class LoginActivity : AppCompatActivity() {

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient
    private var lastEnteredEmail = ""
    private var lastEnteredPassword = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val tilEmail = findViewById<TextInputLayout>(R.id.tilEmail)
        val tilPassword = findViewById<TextInputLayout>(R.id.tilPassword)
        val etEmail = findViewById<TextInputEditText>(R.id.etEmail)
        val etPassword = findViewById<TextInputEditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignUp = findViewById<TextView>(R.id.tvSignUp)
        val tvForgotPassword = findViewById<TextView>(R.id.tvForgotPassword)
        val btnGoogle = findViewById<Button>(R.id.btnGoogle)

        btnLogin.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()
            lastEnteredEmail = email
            lastEnteredPassword = password

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
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                val intent = Intent(this, SignupActivity::class.java)
                startActivity(intent)
            }, 200)
        }

        tvForgotPassword.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                // Confirm karein ke ForgotPasswordActivity bhi registered hai
                startActivity(Intent(this, ForgotPasswordActivity::class.java))
            }, 200)
        }

        val googleLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()) { result ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account.idToken?.let { viewModel.loginWithGoogle(it) }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google sign in failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        btnGoogle.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            googleLauncher.launch(googleSignInClient.signInIntent)
        }

        observeLogin()
        observeResendState()
    }

    private fun observeLogin() {
        val progressBar = findViewById<android.view.View>(R.id.loginProgress)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        viewModel.loginState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    progressBar.visibility = android.view.View.VISIBLE
                    btnLogin.text = ""
                    btnLogin.isEnabled = false
                }
                is Resource.Success -> {
                    progressBar.visibility = android.view.View.GONE
                    val role = resource.data
                    cacheAuthenticatedRole(role)
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
                        "parent" -> {
                            Toast.makeText(this, "Parent Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, ui.parent.ParentDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                        else -> {
                            // Fallback for generic 'user' or unexpected roles
                            Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, ui.parent.ParentDashboardActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    }
                }
                is Resource.Error -> {
                    progressBar.visibility = android.view.View.GONE
                    btnLogin.text = getString(R.string.sign_in)
                    btnLogin.isEnabled = true

                    val errorMsg = resource.message ?: "Login Failed"
                    if (errorMsg.startsWith("EMAIL_NOT_VERIFIED:")) {
                        val unverifiedEmail = errorMsg.substringAfter("EMAIL_NOT_VERIFIED:")
                        showEmailNotVerifiedDialog(unverifiedEmail, lastEnteredPassword)
                    } else {
                        Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    /** Keeps the already-resolved role available when a valid Firebase session is reopened offline. */
    private fun cacheAuthenticatedRole(role: String) {
        val uid = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: return
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .edit()
            .putString("authenticated_role_$uid", role)
            .apply()
    }

    private fun observeResendState() {
        viewModel.resendState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    Toast.makeText(this, "Sending verification email...", Toast.LENGTH_SHORT).show()
                }
                is Resource.Success -> {
                    androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle("Verification Sent")
                        .setMessage(resource.data ?: "A new verification link has been sent to your email. Please check your inbox.")
                        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
                is Resource.Error -> {
                    Toast.makeText(this, resource.message ?: "Failed to resend verification email", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun showEmailNotVerifiedDialog(email: String, password: String) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Email Not Verified")
            .setMessage("Your email address ($email) has not been verified yet.\n\nPlease check your inbox and verify your account to log in.")
            .setPositiveButton("Resend Verification") { dialog, _ ->
                dialog.dismiss()
                viewModel.resendVerificationEmail(email, password)
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
