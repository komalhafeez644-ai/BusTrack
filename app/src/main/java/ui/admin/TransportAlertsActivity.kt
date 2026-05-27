package ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.R
import com.example.bustrack_app.adapters.AlertsAdapter
import com.example.bustrack_app.databinding.ActivityTransportAlertsBinding
import com.example.bustrack_app.viewmodels.AlertsViewModel
import utils.NavigationUtils

class TransportAlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransportAlertsBinding
    private val viewModel: AlertsViewModel by lazy {
        ViewModelProvider(this)[AlertsViewModel::class.java]
    }
    private lateinit var adapter: AlertsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTransportAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.statusBarColor = Color.parseColor("#051024")

        // BACK BUTTON
        binding.btnBack.setOnClickListener {
            finish()
        }

        // RECYCLER
        adapter = AlertsAdapter(emptyList()) { alert ->
            val intent = Intent(this, AlertDetailActivity::class.java)
            intent.putExtra("ALERT_TITLE", alert.title)
            intent.putExtra("ALERT_SUBTITLE", alert.subtitle)
            intent.putExtra("ALERT_TYPE", alert.type)
            intent.putExtra("ALERT_ICON", alert.iconResId)
            startActivity(intent)
        }

        binding.rvAlerts.layoutManager = LinearLayoutManager(this)
        binding.rvAlerts.adapter = adapter

        // Initial UI State
        updateChipUI("ALL")

        // VIEWMODEL OBSERVE
        viewModel.alerts.observe(this) { list ->
            adapter.update(list)
        }

        // ✅ CHIPS FILTER
        binding.chipAll.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            updateChipUI("ALL")
            viewModel.loadAll()
        }

        binding.chipCritical.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            updateChipUI("CRITICAL")
            viewModel.filterByType("CRITICAL")
        }

        binding.chipImportant.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            updateChipUI("IMPORTANT")
            viewModel.filterByType("IMPORTANT")
        }
    }

    override fun onResume() {
        super.onResume()
        NavigationUtils.setupBottomNavigation(this)
    }

    private fun updateChipUI(type: String) {
        val selectedBg = R.drawable.bg_chip_selected
        val unselectedBg = R.drawable.bg_chip_unselected
        
        val selectedText = Color.parseColor("#0F172A")
        val unselectedText = Color.parseColor("#64748B")

        binding.chipAll.setBackgroundResource(if (type == "ALL") selectedBg else unselectedBg)
        binding.chipAll.setTextColor(if (type == "ALL") selectedText else unselectedText)

        binding.chipCritical.setBackgroundResource(if (type == "CRITICAL") selectedBg else unselectedBg)
        binding.chipCritical.setTextColor(if (type == "CRITICAL") selectedText else unselectedText)

        binding.chipImportant.setBackgroundResource(if (type == "IMPORTANT") selectedBg else unselectedBg)
        binding.chipImportant.setTextColor(if (type == "IMPORTANT") selectedText else unselectedText)
    }
}