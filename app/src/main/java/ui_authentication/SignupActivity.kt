package ui_authentication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivitySignupBinding // Apne package ke mutabiq check karein

class SignupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySignupBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Sign In text par click karne se Login screen par jana
        binding.tvSignIn.setOnClickListener {
            finish() // Wapis pichli screen (Login) par le jayega
        }

        // Signup Button logic (Agar aapne XML mein button add kiya hai)
        /*
        binding.btnCreateAccount.setOnClickListener {
            handleSignup()
        }
        */
    }

    private fun handleSignup() {
        val name = binding.etFullName.text.toString()
        val email = binding.etEmail.text.toString()
        val pass = binding.etPassword.text.toString()
        val confirmPass = binding.etConfirmPassword.text.toString()
        val regNum = binding.etRegNumber.text.toString()

        if (name.isEmpty() || email.isEmpty() || pass.isEmpty() || regNum.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (pass != confirmPass) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Yahan registration logic aayega (Firebase ya API)
        Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
    }
}