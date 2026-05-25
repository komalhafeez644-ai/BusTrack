package ui.admin

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.databinding.ActivityEditDriverBinding
import com.example.bustrack_app.models.DriverModel
import com.google.android.material.button.MaterialButton
import utils.ViewUtils

class EditDriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDriverBinding
    private var driverData: DriverModel? = null

    // Photo Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            binding.imgDriverAvatar.setImageURI(it)
            binding.imgDriverAvatar.setPadding(0, 0, 0, 0)
            Toast.makeText(this, "Photo updated locally", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 1. Data Receive
        driverData = intent.getSerializableExtra("driver_data") as? DriverModel

        // 2. UI Fill
        driverData?.let {
            binding.etDriverId.setText(it.id)
            binding.etFullName.setText(it.name)
            binding.etCnic.setText(it.cnic)
            binding.etPhone.setText(it.phone)
            binding.etEmail.setText(it.email)
            binding.txtDriverId.text = "Driver ID: #${it.id}"
            binding.txtRouteValue.text = it.route
            binding.txtBusValue.text = it.assignedBus
            
            if (it.profileImage != 0) {
                binding.imgDriverAvatar.setImageResource(it.profileImage)
            }
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            finish() 
        }

        // Camera Icon click to open gallery
        binding.btnChangePhoto.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            pickImageLauncher.launch("image/*")
        }

        // Driver ID edit warning POPUP instead of Toast
        binding.etDriverId.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                showIdWarningDialog()
            }
        }
        
        binding.etDriverId.setOnClickListener {
            showIdWarningDialog()
        }

        binding.btnUpdate.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            
            driverData?.let { driver ->
                val updatedDriver = driver.copy(
                    id = binding.etDriverId.text.toString().trim(),
                    name = binding.etFullName.text.toString().trim(),
                    cnic = binding.etCnic.text.toString().trim(),
                    phone = binding.etPhone.text.toString().trim(),
                    email = binding.etEmail.text.toString().trim()
                )
                DriverRepository.updateDriver(updatedDriver)
                Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        // Restricted Fleet Assignment Click Logic with Custom Dialog
        val fleetClickListener = View.OnClickListener {
            ViewUtils.applyClickEffect(it)
            showRestrictedActionDialog()
        }

        binding.layoutRouteBlock.setOnClickListener(fleetClickListener)
        binding.layoutBusBlock.setOnClickListener(fleetClickListener)
    }

    private fun showIdWarningDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_restricted_action, null)
        val btnOk = dialogView.findViewById<MaterialButton>(R.id.btnOk)
        val tvTitle = dialogView.findViewById<android.widget.TextView>(R.id.tvTitle)
        val tvMsg = dialogView.findViewById<android.widget.TextView>(R.id.tvMessage)

        tvTitle.text = "Important Warning"
        tvMsg.text = "Changing Driver ID may affect linked records and data consistency."

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        btnOk.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            dialog.dismiss()
        }
    }

    private fun showRestrictedActionDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_restricted_action, null)
        val btnOk = dialogView.findViewById<MaterialButton>(R.id.btnOk)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()

        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        dialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        btnOk.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            dialog.dismiss()
        }
    }
}