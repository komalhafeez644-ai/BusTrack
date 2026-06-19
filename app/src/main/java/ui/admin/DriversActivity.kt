package ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.adapter.DriverAdapter
import com.example.bustrack_app.databinding.ActivityDriversBinding
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.viewmodels.DriverViewModel

class DriversActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDriversBinding
    private lateinit var viewModel: DriverViewModel
    private lateinit var driverAdapter: DriverAdapter
    private var fullDriverList = listOf<DriverModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDriversBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DriverViewModel::class.java]

        // Observe Repository list - Updates automatically on any change
        viewModel.drivers.observe(this) { list ->
            fullDriverList = list
            filterList(binding.etSearchDriver.text.toString().lowercase().trim())
        }

        setupRecyclerView()
        setupSearch()

        // Back Button
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            onBackPressedDispatcher.onBackPressed()
        }

        // FAB Add
        binding.fabAddDriver.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            val intent = Intent(this, AddDriverActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        utils.NavigationUtils.setupBottomNavigation(this)
    }

    private fun setupRecyclerView() {
        driverAdapter = DriverAdapter(listOf()) { driver ->
            val intent = Intent(this, ViewDriverProfileActivity::class.java)
            intent.putExtra("driver_data", driver)
            startActivity(intent)
        }

        binding.rvDrivers.apply {
            layoutManager = LinearLayoutManager(this@DriversActivity)
            adapter = driverAdapter
        }
    }

    private fun setupSearch() {
        binding.etSearchDriver.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterList(s.toString().lowercase().trim())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun filterList(query: String) {
        val filtered = if (query.isEmpty()) {
            fullDriverList
        } else {
            fullDriverList.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.assignedBus?.contains(query, ignoreCase = true) == true ||
                        it.id.contains(query, ignoreCase = true)
            }
        }
        driverAdapter.setDrivers(filtered)
    }
}
