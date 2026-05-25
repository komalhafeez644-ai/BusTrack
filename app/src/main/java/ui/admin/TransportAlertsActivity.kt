package ui.admin

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.R
import com.example.bustrack_app.adapters.AlertsAdapter
import com.example.bustrack_app.databinding.ActivityTransportAlertsBinding
import com.example.bustrack_app.viewmodels.AlertsViewModel

class TransportAlertsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTransportAlertsBinding
    private val viewModel: AlertsViewModel by viewModels()
    private lateinit var adapter: AlertsAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTransportAlertsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ FIX IMPORT ISSUE
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

        // ✅ CHIPS FILTER (FIXED)
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

        utils.NavigationUtils.setupBottomNavigation(this)
    }

    private fun updateChipUI(selectedType: String) {
        val chips = listOf(binding.chipAll, binding.chipCritical, binding.chipImportant)
        
        chips.forEach { chip ->
            val type = when (chip.id) {
                R.id.chipAll -> "ALL"
                R.id.chipCritical -> "CRITICAL"
                R.id.chipImportant -> "IMPORTANT"
                else -> ""
            }
            
            if (type == selectedType) {
                chip.setBackgroundResource(R.drawable.bg_chip_selected)
                chip.setTextColor(Color.parseColor("#0F172A"))
                chip.setTypeface(null, android.graphics.Typeface.BOLD)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_unselected)
                chip.setTextColor(Color.parseColor("#64748B"))
                chip.setTypeface(null, android.graphics.Typeface.NORMAL)
            }
        }
    }
}