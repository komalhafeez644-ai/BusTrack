package ui.admin

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.PrivacyPolicyAdapter
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.viewmodels.PrivacyPolicyViewModel
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import ui.driver.DriverDashboardActivity
import ui.parent.ParentDashboardActivity
import ui.principal.PrincipalDashboardActivity
import utils.ViewUtils

class PrivacyPolicyActivityActivity : AppCompatActivity() {

    private lateinit var viewModel: PrivacyPolicyViewModel
    private lateinit var recyclerView: RecyclerView
    private val authRepo = AuthRepository()
    private var currentUserRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.privacy_policy_activity)

        // 1. RecyclerView initialization
        recyclerView = findViewById(R.id.rvPolicySections)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // 2. ViewModel initialization
        viewModel = ViewModelProvider(this)[PrivacyPolicyViewModel::class.java]

        // 3. Observe policy sections data
        viewModel.policyList.observe(this) { list ->
            val adapter = PrivacyPolicyAdapter(list)
            recyclerView.adapter = adapter
        }

        val bottomSection = findViewById<View>(R.id.bottomSection)
        val tvContactMsg = findViewById<TextView>(R.id.tvContactMsg)
        val btnInquiry = findViewById<View>(R.id.btnInquiry)

        // Fast-path role check from Intent to prevent UI flicker
        val fromUserExtra = intent.getStringExtra("FROM_USER")?.lowercase()
        if (fromUserExtra == "principal") {
            bottomSection?.visibility = View.GONE
        } else if (fromUserExtra == "admin") {
            bottomSection?.visibility = View.VISIBLE
            tvContactMsg?.setText(R.string.contact_principal_msg)
        } else {
            bottomSection?.visibility = View.VISIBLE
            tvContactMsg?.setText(R.string.contact_admin_msg)
        }

        // Authoritative role check from Auth / Firestore
        lifecycleScope.launch {
            val role = authRepo.getCurrentUserRole().lowercase()
            currentUserRole = role
            when (role) {
                "principal" -> {
                    bottomSection?.visibility = View.GONE
                }
                "admin" -> {
                    bottomSection?.visibility = View.VISIBLE
                    tvContactMsg?.setText(R.string.contact_principal_msg)
                }
                else -> {
                    bottomSection?.visibility = View.VISIBLE
                    tvContactMsg?.setText(R.string.contact_admin_msg)
                }
            }
        }

        // Inquiry Button Click Logic
        btnInquiry?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            sendInquiry()
        }

        // Menu button logic - Opens Dashboard with Drawer open
        findViewById<View>(R.id.btnMenu)?.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleBackToDashboard()
        }
    }

    private fun sendInquiry() {
        lifecycleScope.launch {
            val role = currentUserRole ?: authRepo.getCurrentUserRole().lowercase()

            // Principal should not have inquiry option
            if (role == "principal") return@launch

            val (targetRole, fallbackEmail) = if (role == "admin") {
                "principal" to "principal@gmail.com"
            } else {
                "admin" to "admin@gmail.com"
            }

            val recipientEmail = fetchRecipientEmail(targetRole, fallbackEmail)
            launchEmailIntent(recipientEmail)
        }
    }

    private suspend fun fetchRecipientEmail(targetRole: String, fallbackEmail: String): String {
        return try {
            val snapshot = Firebase.firestore.collection("users")
                .whereEqualTo("role", targetRole)
                .limit(1)
                .get()
                .await()
            val email = snapshot.documents.firstOrNull()?.getString("email")?.trim()
            if (!email.isNullOrBlank()) email else fallbackEmail
        } catch (e: Exception) {
            fallbackEmail
        }
    }

    private fun launchEmailIntent(recipientEmail: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:$recipientEmail")
                putExtra(Intent.EXTRA_SUBJECT, "BusTrack Inquiry")
            }
            startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(
                this,
                "No email application found on device. Please contact $recipientEmail directly.",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Toast.makeText(
                this,
                "Unable to open email app: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun handleBackToDashboard() {
        val fromUser = intent.getStringExtra("FROM_USER")?.lowercase()
        when (fromUser) {
            "parent" -> {
                navigateBackTo(ParentDashboardActivity::class.java)
                return
            }
            "driver" -> {
                navigateBackTo(DriverDashboardActivity::class.java)
                return
            }
            "admin" -> {
                navigateBackTo(AdminDashboardActivity::class.java)
                return
            }
            "principal" -> {
                navigateBackTo(PrincipalDashboardActivity::class.java)
                return
            }
        }

        lifecycleScope.launch {
            val role = authRepo.getCurrentUserRole().lowercase()
            val targetClass = when (role) {
                "principal" -> PrincipalDashboardActivity::class.java
                "admin" -> AdminDashboardActivity::class.java
                "driver" -> DriverDashboardActivity::class.java
                else -> ParentDashboardActivity::class.java
            }
            navigateBackTo(targetClass)
        }
    }

    private fun navigateBackTo(targetClass: Class<*>) {
        val intent = Intent(this, targetClass).apply {
            putExtra("OPEN_DRAWER", true)
            flags = Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
        }
        startActivity(intent)
        finish()
    }
}