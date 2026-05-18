package ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.ActivityEditDriverBinding
import com.example.bustrack_app.models.DriverModel

class EditDriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDriverBinding
    private var driverData: DriverModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Data Receive Karein
        driverData = intent.getSerializableExtra("driver_data") as? DriverModel

        // 2. UI Fill Karein
        driverData?.let {
            binding.etFullName.setText(it.name)
            binding.etCnic.setText(it.cnic)
            binding.etPhone.setText(it.phone)
            binding.autoCompleteRoute.setText(it.route, false)
            // Naya Field: Assigned Bus ko set kiya
            binding.autoCompleteBus.setText(it.assignedBus, false)
            binding.txtDriverId.text = "Driver ID: #${it.id}"
        }

        setupDropdowns() // Name change kiya kyunki ab 2 dropdowns hain

        // 3. Save Click Logic
        binding.btnSaveChanges.setOnClickListener {
            val updatedDriver = driverData?.copy(
                name = binding.etFullName.text.toString(),
                cnic = binding.etCnic.text.toString(),
                phone = binding.etPhone.text.toString(),
                route = binding.autoCompleteRoute.text.toString(),
                // Naya Field: Bus ka data bhi save kiya
                assignedBus = binding.autoCompleteBus.text.toString()
            )

            val intent = Intent()
            intent.putExtra("updated_driver", updatedDriver)
            setResult(RESULT_OK, intent)
            Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
            finish()
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    private fun setupDropdowns() {
        // Routes Dropdown
        val routes = arrayOf("Route 42 - Sector 15 North", "Route 12 - Mall Road", "Express-1")
        val routeAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, routes)
        binding.autoCompleteRoute.setAdapter(routeAdapter)

        // Bus Dropdown (Naya)
        val buses = arrayOf("Bus 101 (Volvo B11R)", "Bus-08", "Bus-04")
        val busAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, buses)
        binding.autoCompleteBus.setAdapter(busAdapter)
    }
}