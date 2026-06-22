package ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.R
import com.example.bustrack_app.data.AuthRepository
import kotlinx.coroutines.launch
import ui.parent.ParentDashboardActivity
import ui.driver.DriverDashboardActivity
import utils.ViewUtils

class TermsConditionsActivity : AppCompatActivity() {

    private val authRepo = AuthRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_terms_conditions)

        // Menu button logic - Opens Dashboard with Drawer open
        findViewById<android.view.View>(R.id.btnMenu)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleBackToDashboard()
        }

        val cbAgree = findViewById<CheckBox>(R.id.cbAgree)
        val btnContinue = findViewById<Button>(R.id.btnContinue)

        cbAgree.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                btnContinue.isEnabled = true
                btnContinue.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    resources.getColor(android.R.color.black, theme)
                )
            } else {
                btnContinue.isEnabled = false
                btnContinue.backgroundTintList = android.content.res.ColorStateList.valueOf(
                    resources.getColor(android.R.color.darker_gray, theme)
                )
            }
        }

        btnContinue.setOnClickListener {
            if (cbAgree.isChecked) {
                Toast.makeText(this, "Terms Accepted!", Toast.LENGTH_SHORT).show()
                finish() 
            } else {
                Toast.makeText(this, "Please agree to the terms to continue", Toast.LENGTH_SHORT).show()
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
        } else if (fromUser == "driver") {
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
            return
        }

        // Fallback to role-based detection if extra is missing
        lifecycleScope.launch {
            val role = authRepo.getCurrentUserRole()
            val targetClass = when (role) {
                "admin" -> AdminDashboardActivity::class.java
                "driver" -> DriverDashboardActivity::class.java
                else -> ParentDashboardActivity::class.java
            }
            
            val intent = Intent(this@TermsConditionsActivity, targetClass)
            intent.putExtra("OPEN_DRAWER", true)
            intent.flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            startActivity(intent)
            finish()
        }
    }
}