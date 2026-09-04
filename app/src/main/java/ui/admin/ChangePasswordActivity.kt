package ui.admin

import android.os.Bundle
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.bustrack_app.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.data.AuthRepository
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.launch
import ui.driver.DriverDashboardActivity
import ui.parent.ParentDashboardActivity

class ChangePasswordActivity : AppCompatActivity() {

    private val authRepository = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        supportActionBar?.hide()

        val etCurrent = findViewById<TextInputEditText>(R.id.etCurrentPassword)
        val etNew = findViewById<TextInputEditText>(R.id.etNewPassword)
        val etConfirm = findViewById<TextInputEditText>(R.id.etConfirmPassword)
        val btnUpdate = findViewById<MaterialButton>(R.id.btnUpdatePassword)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val progressBar = findViewById<ProgressBar>(R.id.progressBar)

        btnBack.setOnClickListener {
            handleBackToDashboard()
        }

        btnUpdate.setOnClickListener {
            val current = etCurrent.text.toString().trim()
            val newPass = etNew.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            if (current.isEmpty() || newPass.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass == current) {
                Toast.makeText(this, "New password cannot be the same as current password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirm) {
                Toast.makeText(this, "New password and confirm password do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                btnUpdate.isEnabled = false
                progressBar.isVisible = true

                val result = authRepository.changePassword(current, newPass)

                progressBar.isVisible = false
                btnUpdate.isEnabled = true

                result.fold(
                    onSuccess = {
                        Toast.makeText(this@ChangePasswordActivity, "Password changed successfully.", Toast.LENGTH_LONG).show()
                        etCurrent.text?.clear()
                        etNew.text?.clear()
                        etConfirm.text?.clear()
                        finish()
                    },
                    onFailure = { exception ->
                        val errorMessage = when (exception) {
                            is FirebaseAuthException -> {
                                when (exception.errorCode) {
                                    "ERROR_WRONG_PASSWORD" -> "Existing password is incorrect."
                                    "ERROR_WEAK_PASSWORD" -> "The password is too weak."
                                    else -> exception.localizedMessage ?: "Failed to change password."
                                }
                            }
                            else -> exception.localizedMessage ?: "An error occurred."
                        }
                        Toast.makeText(this@ChangePasswordActivity, errorMessage, Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    private fun handleBackToDashboard() {
        val fromUser = intent.getStringExtra("FROM_USER")
        if (fromUser == "parent") {
            val intent = Intent(this, ParentDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        } else if (fromUser == "admin") {
            val intent = Intent(this, AdminDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        } else if (fromUser == "driver") {
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        } else if (fromUser == "principal") {
            val intent = Intent(this, ui.principal.PrincipalDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        }

        lifecycleScope.launch {
            val role = AuthRepository().getCurrentUserRole()
            val targetClass = when (role) {
                "admin" -> AdminDashboardActivity::class.java
                "driver" -> DriverDashboardActivity::class.java
                "principal" -> ui.principal.PrincipalDashboardActivity::class.java
                else -> ParentDashboardActivity::class.java
            }
            
            val intent = Intent(this@ChangePasswordActivity, targetClass)
            intent.putExtra("OPEN_DRAWER", true)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }
    }
}
