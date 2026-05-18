package ui.admin

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
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

    // Launcher: Naya driver receive karne ke liye
    private val addDriverLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val newDriver = result.data?.getSerializableExtra("new_driver_data") as? DriverModel
            newDriver?.let {
                // ViewModel ke function ko call karke list update karein
                viewModel.addDriver(it)
                Toast.makeText(this, "Driver Added Successfully", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDriversBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[DriverViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupSearch()

        // Back Button
        binding.btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // FAB Add: Ab ye AddDriverActivity open karega
        binding.fabAddDriver.setOnClickListener {
            val intent = Intent(this, AddDriverActivity::class.java)
            addDriverLauncher.launch(intent)
        }
    }

    private fun setupRecyclerView() {
        // Adapter Init with Click Listener for Editing
        driverAdapter = DriverAdapter(listOf()) { driver ->
            val intent = Intent(this, EditDriverActivity::class.java)
            intent.putExtra("driver_data", driver)
            startActivity(intent)
        }

        binding.rvDrivers.apply {
            layoutManager = LinearLayoutManager(this@DriversActivity)
            adapter = driverAdapter
        }
    }

    private fun setupObservers() {
        // Data Observe
        viewModel.drivers.observe(this) { driverList ->
            fullDriverList = driverList
            driverAdapter.setDrivers(driverList)
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
                it.name.lowercase().contains(query) ||
                        it.assignedBus.lowercase().contains(query) ||
                        it.id.lowercase().contains(query) // ID par bhi search enable kar di
            }
        }
        driverAdapter.setDrivers(filtered)
    }
}