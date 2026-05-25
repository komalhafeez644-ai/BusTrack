package ui.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityEditBusDetailsBinding
import com.example.bustrack_app.databinding.DialogDeleteBusBinding
import com.example.bustrack_app.models.BusModel
import com.example.bustrack_app.viewmodels.BusViewModel

class EditBusDetailsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBusDetailsBinding
    private lateinit var viewModel: BusViewModel
    private lateinit var originalBusNumber: String

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditBusDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        utils.NavigationUtils.setupBottomNavigation(this)

        viewModel = ViewModelProvider(this)[BusViewModel::class.java]

        // Intent se data receive karna
        originalBusNumber = intent.getStringExtra("BUS_NUMBER") ?: "BUS-102"
        val intentCapacity = intent.getIntExtra("BUS_CAPACITY", 40)
        val intentDriver = intent.getStringExtra("BUS_DRIVER") ?: ""
        val intentRoute = intent.getStringExtra("BUS_ROUTE") ?: ""
        val intentStatus = intent.getStringExtra("BUS_STATUS") ?: "ACTIVE"

        // UI Views mein data set karna (Route view hataya gaya hai)
        binding.etEditBusNumber.setText(originalBusNumber)
        binding.etEditCapacity.setText(intentCapacity.toString())
        binding.menuEditDriver.setText(intentDriver)

        // Route details non-editable display (as per requirement)
        // binding.menuEditRoute.setText(intentRoute) - if view exists but should be locked
        // We'll just pass the intentRoute back in update logic

        // Back Arrow Click
        binding.btnBackArrow.setOnClickListener { finish() }

        // UPDATE BUTTON CLICK (Route management separate hai, isliye route hardcoded chalega)
        binding.btnUpdateBus.setOnClickListener {
            val updatedNo = binding.etEditBusNumber.text.toString().trim()
            val updatedCapStr = binding.etEditCapacity.text.toString().trim()
            val updatedDriver = binding.menuEditDriver.text.toString().trim()

            if (updatedNo.isEmpty() || updatedCapStr.isEmpty()) {
                Toast.makeText(this, "Fields cannot be empty!", Toast.LENGTH_SHORT).show()
            } else {
                val updatedCapacity = updatedCapStr.toIntOrNull() ?: 0

                val updatedBus = BusModel(
                    busNumber = updatedNo,
                    totalSeats = updatedCapacity,
                    driverName = updatedDriver.ifEmpty { null },
                    routeName = intentRoute.ifEmpty { null }, // Route purana hi pass hoga bina edit kiye
                    status = intentStatus
                )

                viewModel.updateBusDetails(originalBusNumber, updatedBus)
                Toast.makeText(this, "Bus updated successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // DELETE BUTTON CLICK (Custom dialog open hoga)
        binding.btnDeleteBus.setOnClickListener {
            showDeleteConfirmationPopup()
        }
    }

    // Custom Delete Confirmation Popup ki Logic
    private fun showDeleteConfirmationPopup() {
        val dialogBinding = DialogDeleteBusBinding.inflate(layoutInflater)

        // FIXED: CustomDialogTheme hata kar simple AlertDialog.Builder use kiya hai taaki error khatam ho jaye
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogBinding.root)

        val alertDialog = builder.create()

        // Dialog ke outer square background ko transparent kiya taaki XML card ke rounded corners perfect show hon
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        // 1. Cancel Button Click
        dialogBinding.btnCancel.setOnClickListener {
            alertDialog.dismiss()
        }

        // 2. Delete Button Click
        dialogBinding.btnDelete.setOnClickListener {
            // ViewModel se bus remove ki
            viewModel.deleteBusFromFleet(originalBusNumber)

            // Toast message jaisa aapne kaha
            Toast.makeText(this, "Delete successfully", Toast.LENGTH_SHORT).show()

            alertDialog.dismiss()
            finish() // Screen close ho kar wapas list par chali jayegi
        }

        alertDialog.show()
    }
}