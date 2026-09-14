package ui_authentication

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivitySignupBinding
import com.example.bustrack_app.utils.Resource
import com.example.bustrack_app.viewmodels.SignupViewModel
import utils.ViewUtils

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val viewModel: SignupViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.tvSignIn.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                finish()
            }, 200)
        }

        binding.btnCreateAccount.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleSignup()
        }

        observeSignupState()
    }

    private fun handleSignup() {
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()

        // Reset errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null

        var isValid = true

        // 1. Email validation
        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Please enter a valid email address (e.g. example@gmail.com)"
            isValid = false
        }

        // 2. Password validation (Minimum 8 chars, at least 1 uppercase, at least 1 number)
        if (pass.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (pass.length < 8) {
            binding.tilPassword.error = "Password must be at least 8 characters"
            isValid = false
        } else if (!pass.any { it.isUpperCase() }) {
            binding.tilPassword.error = "Password must contain at least 1 uppercase letter"
            isValid = false
        } else if (!pass.any { it.isDigit() }) {
            binding.tilPassword.error = "Password must contain at least 1 number"
            isValid = false
        }

        // 3. Confirm Password validation
        if (confirmPass.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (pass != confirmPass) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (!isValid) return

        viewModel.register(email, pass)
    }

    private fun observeSignupState() {
        viewModel.signupState.observe(this) { resource ->
            when (resource) {
                is Resource.Loading -> {
                    binding.signupProgress.visibility = View.VISIBLE
                    binding.btnCreateAccount.isEnabled = false
                    binding.btnCreateAccount.text = ""
                }
                is Resource.Success -> {
                    binding.signupProgress.visibility = View.GONE
                    binding.btnCreateAccount.isEnabled = true
                    binding.btnCreateAccount.text = "Create Account"

                    val email = binding.etEmail.text.toString().trim()
                    AlertDialog.Builder(this)
                        .setTitle("Verify Your Email")
                        .setMessage("A verification email has been sent to $email.\n\nPlease verify your email address before logging in.")
                        .setCancelable(false)
                        .setPositiveButton("Go to Login") { dialog, _ ->
                            dialog.dismiss()
                            finish()
                        }
                        .show()
                }
                is Resource.Error -> {
                    binding.signupProgress.visibility = View.GONE
                    binding.btnCreateAccount.isEnabled = true
                    binding.btnCreateAccount.text = "Create Account"
                    Toast.makeText(this, resource.message ?: "Registration failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}