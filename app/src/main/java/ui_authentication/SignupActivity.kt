package ui_authentication

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.databinding.ActivitySignupBinding
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import utils.ViewUtils

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding
    private val auth = Firebase.auth
    private val db = Firebase.firestore

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
    }

    private fun handleSignup() {
        val name = binding.etFullName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()
        val confirmPass = binding.etConfirmPassword.text.toString().trim()
        val regNum = binding.etRegNumber.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()

        // Reset errors
        binding.tilFullName.error = null
        binding.tilEmail.error = null
        binding.tilPassword.error = null
        binding.tilConfirmPassword.error = null
        binding.tilRegNumber.error = null
        binding.tilPhone.error = null

        var isValid = true

        if (name.isEmpty()) {
            binding.tilFullName.error = "Full Name is required"
            isValid = false
        }

        if (email.isEmpty()) {
            binding.tilEmail.error = "Email is required"
            isValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Invalid email format"
            isValid = false
        }

        if (phone.isEmpty()) {
            binding.tilPhone.error = "Phone number is required"
            isValid = false
        }

        if (pass.isEmpty()) {
            binding.tilPassword.error = "Password is required"
            isValid = false
        } else if (pass.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
            isValid = false
        }

        if (confirmPass.isEmpty()) {
            binding.tilConfirmPassword.error = "Please confirm your password"
            isValid = false
        } else if (pass != confirmPass) {
            binding.tilConfirmPassword.error = "Passwords do not match"
            isValid = false
        }

        if (regNum.isEmpty()) {
            binding.tilRegNumber.error = "Student ID is required"
            isValid = false
        }

        if (!isValid) return

        lifecycleScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, pass).await()
                val uid = result.user?.uid
                if (uid != null) {
                    val userData = mapOf(
                        "uid" to uid,
                        "fullName" to name,
                        "email" to email,
                        "phone" to phone,
                        "registrationNumber" to regNum,
                        "role" to "parent" // Consistent with user instructions
                    )
                    db.collection("users").document(uid).set(userData).await()
                    Toast.makeText(this@SignupActivity, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@SignupActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }
}