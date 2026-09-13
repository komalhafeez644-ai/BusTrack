package ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.example.bustrack_app.adapter.IntroSlideAdapter
import androidx.lifecycle.lifecycleScope
import com.example.bustrack_app.data.AuthRepository
import com.example.bustrack_app.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import ui_authentication.LoginActivity
import utils.ViewUtils

class IntroActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayoutIndicator: TabLayout
    private lateinit var fabNextAction: FloatingActionButton
    private lateinit var tvSkipAction: TextView
    private lateinit var loadingIndicator: ProgressBar
    private var userRole: String? = null
    private var isNavigating = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        viewPager = findViewById(R.id.viewPager)
        tabLayoutIndicator = findViewById(R.id.tabLayoutIndicator)
        fabNextAction = findViewById(R.id.fabNextAction)
        tvSkipAction = findViewById(R.id.tvSkipAction)
        loadingIndicator = findViewById(R.id.loadingIndicator)

        window.statusBarColor = Color.parseColor("#051024")

        val pagerAdapter = IntroSlideAdapter(this)
        viewPager.adapter = pagerAdapter

        // Pre-fetch role in background to avoid delay on last slide
        prefetchUserRole()

        // Dots indicator ka setup
        TabLayoutMediator(tabLayoutIndicator, viewPager) { _, _ -> }.attach()

        // Dots ko stretch hone se rokne ke liye
        tabLayoutIndicator.tabMode = TabLayout.MODE_FIXED

        fabNextAction.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val currentPos = viewPager.currentItem
            if (currentPos + 1 < 3) {
                viewPager.currentItem = currentPos + 1
            } else {
                handleNavigation()
            }
        }

        tvSkipAction.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleNavigation() 
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                tvSkipAction.visibility = if (position == 2) View.INVISIBLE else View.VISIBLE
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

        // Save onboarding completion status
        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        sharedPref.edit().putBoolean("onboardingCompleted", true).apply()
        
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
            loadingIndicator.visibility = View.VISIBLE
            fabNextAction.setImageDrawable(null)
            fabNextAction.isEnabled = false
        } else {
            loadingIndicator.visibility = View.GONE
            fabNextAction.setImageResource(R.drawable.baseline_arrow_forward_24)
            fabNextAction.isEnabled = true
        }
    }
}
