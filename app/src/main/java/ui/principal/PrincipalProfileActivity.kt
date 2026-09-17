package ui.principal

import android.graphics.Color
import android.os.Bundle
import android.content.Intent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityPrincipalProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class PrincipalProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPrincipalProfileBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrincipalProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = getColor(R.color.primaryDark)
        window.decorView.systemUiVisibility = 0

        setupClickListeners()
        loadPrincipalProfile()
    }

    private fun loadPrincipalProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).addSnapshotListener { snapshot, e ->
            if (snapshot != null && snapshot.exists()) {
                val name = snapshot.getString("fullName") ?: "Principal User"
                val email = snapshot.getString("email") ?: "principal@gmail.com"
                val phone = snapshot.getString("phone") ?: ""
                val empId = snapshot.getString("employeeId") ?: "PRN-2024-001"
                val imageUrl = snapshot.getString("profileImageUrl") ?: ""

                binding.tvPrincipalName.text = name
                binding.tvInfoFullName.text = name
                binding.tvInfoEmail.text = email
                binding.tvInfoPhone.text = phone.ifEmpty { "Not provided" }
                binding.tvInfoEmpID.text = empId

                utils.ImageUtils.loadProfileImage(this, imageUrl, binding.imgProfile)
            }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setImageResource(com.example.bustrack_app.R.drawable.ic_menu)
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, PrincipalDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }

        binding.btnEditProfile.setOnClickListener {
            val intent = Intent(this, PrincipalEditProfileActivity::class.java)
            startActivity(intent)
        }
    }
}
