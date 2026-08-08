package ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.bustrack_app.adapter.IntroSlideAdapter
import com.example.bustrack_app.databinding.ActivityIntroBinding
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.data.AuthRepository
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import ui_authentication.LoginActivity
import utils.ViewUtils

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding
    private var userRole: String? = null
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.parseColor("#051024")

        val pagerAdapter = IntroSlideAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Pre-fetch role in background to avoid delay on last slide
        prefetchUserRole()

        // Dots indicator ka setup
        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPager) { _, _ -> }.attach()

        // Dots ko stretch hone se rokne ke liye
        binding.tabLayoutIndicator.tabMode = TabLayout.MODE_FIXED

        binding.fabNextAction.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val currentPos = binding.viewPager.currentItem
            if (currentPos + 1 < 3) {
                binding.viewPager.currentItem = currentPos + 1
            } else {
                handleNavigation()
            }
        }

        binding.tvSkipAction.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            handleNavigation() 
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvSkipAction.visibility = if (position == 2) View.INVISIBLE else View.VISIBLE
            }
        })
    }

    private fun prefetchUserRole() {
        val currentUser = Firebase.auth.currentUser
        if (currentUser != null) {
            lifecycleScope.launch {
                userRole = AuthRepository().getCurrentUserRole()
            }
        }
    }

    private fun handleNavigation() {
        if (isNavigating) return
        
        val auth = Firebase.auth
        val currentUser = auth.currentUser

        if (currentUser != null) {
            isNavigating = true
            showLoading(true)
            
            lifecycleScope.launch {
                // If role isn't fetched yet, wait for it
                val role = userRole ?: AuthRepository().getCurrentUserRole()
                
                val targetClass = when (role) {
                    "admin" -> AdminDashboardActivity::class.java
                    "principal" -> ui.principal.PrincipalDashboardActivity::class.java
                    "driver" -> ui.driver.DriverDashboardActivity::class.java
                    else -> ui.parent.ParentDashboardActivity::class.java
                }
                
                // Add a small delay for smooth animation if it was too fast
                startActivity(Intent(this@IntroActivity, targetClass))
                finish()
            }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun showLoading(isLoading: Boolean) {
        if (isLoading) {
            binding.loadingIndicator.visibility = View.VISIBLE
            binding.fabNextAction.setImageDrawable(null)
            binding.fabNextAction.isEnabled = false
        } else {
            binding.loadingIndicator.visibility = View.GONE
            binding.fabNextAction.setImageResource(com.example.bustrack_app.R.drawable.baseline_arrow_forward_24)
            binding.fabNextAction.isEnabled = true
        }
    }
}