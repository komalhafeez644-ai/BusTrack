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

class IntroActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIntroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIntroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.parseColor("#051024")

        val pagerAdapter = IntroSlideAdapter(this)
        binding.viewPager.adapter = pagerAdapter

        // Dots indicator ka setup
        TabLayoutMediator(binding.tabLayoutIndicator, binding.viewPager) { _, _ -> }.attach()

        // Dots ko stretch hone se rokne ke liye
        binding.tabLayoutIndicator.tabMode = TabLayout.MODE_FIXED

        binding.fabNextAction.setOnClickListener {
            val currentPos = binding.viewPager.currentItem
            if (currentPos + 1 < 3) {
                binding.viewPager.currentItem = currentPos + 1
            } else {
                handleNavigation()
            }
        }

        binding.tvSkipAction.setOnClickListener { handleNavigation() }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvSkipAction.visibility = if (position == 2) View.INVISIBLE else View.VISIBLE
            }
        })
    }

    private fun handleNavigation() {
        val auth = Firebase.auth
        val currentUser = auth.currentUser

        if (currentUser != null) {
            lifecycleScope.launch {
                val role = AuthRepository().getCurrentUserRole()
                val targetClass = when (role) {
                    "admin" -> AdminDashboardActivity::class.java
                    "principal" -> ui.principal.PrincipalDashboardActivity::class.java
                    "driver" -> ui.driver.DriverDashboardActivity::class.java
                    else -> ui.parent.ParentDashboardActivity::class.java
                }
                startActivity(Intent(this@IntroActivity, targetClass))
                finish()
            }
        } else {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}