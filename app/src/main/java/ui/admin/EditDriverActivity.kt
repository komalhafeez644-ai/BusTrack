package ui.admin

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.data.BusRepository
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.databinding.ActivityEditDriverBinding
import com.example.bustrack_app.models.BusModel
import com.example.bustrack_app.models.DriverModel
import com.google.android.material.button.MaterialButton
import utils.FormUtils
import utils.StorageUtils
import utils.ViewUtils

class EditDriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditDriverBinding
    private var driverData: DriverModel? = null
    private var selectedImageUri: Uri? = null

    // Photo Picker Launcher
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.imgDriverAvatar.setImageURI(it)
            binding.imgDriverAvatar.setPadding(0, 0, 0, 0)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        driverData = IntentCompat.getSerializableExtra(intent, "driver_data", DriverModel::class.java)

        driverData?.let {
            binding.etDriverId.setText(it.id)
            binding.etFullName.setText(it.name)
            binding.etCnic.setText(it.cnic)
            binding.etPhone.setText(it.phone)
            binding.etEmail.setText(it.email)
            binding.txtDriverId.text = "Driver ID: #${it.id}"
            
            // Set initial bus and route
            val initialBus = it.assignedBus ?: "Select Bus"
            binding.menuEditBus.setText(initialBus, false)
            binding.txtRouteValue.text = it.route ?: "No Route Assigned"
            
            // If bus is already assigned, fetch the route from repository for consistency
            if (it.assignedBus != null) {
                updateRouteDisplay(it.assignedBus!!)
            }
            
            when {
                it.profileImageUrl.isNotEmpty() -> {
                    Glide.with(this).load(it.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(binding.imgDriverAvatar)
                }
                it.profileImage != 0 -> {
                    binding.imgDriverAvatar.setImageResource(it.profileImage)
                }
                else -> {
                    binding.imgDriverAvatar.setImageResource(R.drawable.ic_person)
                }
            }
        }

        setupBusDropdown()
        setupFormFormatting()
        setupClickListeners()
    }

    private fun setupFormFormatting() {
        FormUtils.setupUppercaseInput(binding.etDriverId)
        FormUtils.setupTitleCaseInput(binding.etFullName)
        FormUtils.setupCnicFormatting(binding.etCnic)
    }

    private fun setupBusDropdown() {
        val buses = BusRepository.busList.value ?: listOf()
        val busNumbers = buses.map { it.busNumber }.toMutableList()
        busNumbers.add(0, "Select Bus")

        val adapter = ArrayAdapter(this, com.example.bustrack_app.R.layout.spinner_dropdown_item, busNumbers)
        binding.menuEditBus.setAdapter(adapter)

        binding.menuEditBus.setOnItemClickListener { parent, _, position, _ ->
            val selectedBus = parent.getItemAtPosition(position).toString()
            if (selectedBus != "Select Bus") {
                updateRouteDisplay(selectedBus)
            } else {
                binding.txtRouteValue.text = "No Route"
            }
        }
    }

    private fun updateRouteDisplay(busNo: String) {
        val routes = RouteRepository.routeList.value ?: listOf()
        val assignedRoute = routes.find { it.busNo == busNo }?.routeName ?: "No Route Assigned"
        binding.txtRouteValue.text = assignedRoute
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            finish() 
        }

        binding.btnChangePhoto.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            pickImageLauncher.launch("image/*")
        }

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
            if (selectedImageUri != null) {
                uploadAndSave()
            } else {
                updateDriver(driverData?.profileImageUrl ?: "")
            }
        }
    }

    private fun uploadAndSave() {
        binding.btnUpdate.isEnabled = false
        Toast.makeText(this, "Updating photo...", Toast.LENGTH_SHORT).show()

        StorageUtils.uploadImage("driver_profiles", selectedImageUri!!) { url ->
            if (url != null) {
                updateDriver(url)
            } else {
                binding.btnUpdate.isEnabled = true
                Toast.makeText(this, "Photo upload failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateDriver(imageUrl: String) {
        val name = binding.etFullName.text.toString().trim()
        val empId = binding.etDriverId.text.toString().trim()
        val cnic = binding.etCnic.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (empId.isEmpty()) {
            binding.etDriverId.error = "ID required"
            return
        }
        if (name.isEmpty()) {
            binding.etFullName.error = "Name required"
            return
        }
        if (cnic.length < 15) {
            binding.etCnic.error = "CNIC must be 13 digits (15 with dashes)"
            return
        }
        if (!FormUtils.isValidPhone(phone)) {
            binding.etPhone.error = "Invalid phone"
            return
        }
        if (email.isNotEmpty() && !FormUtils.isValidEmail(email)) {
            binding.etEmail.error = "Invalid email"
            return
        }

        driverData?.let { driver ->
            val selectedBus = binding.menuEditBus.text.toString().trim()
            val finalBus = if (selectedBus == "Select Bus") null else selectedBus
            val finalRoute = binding.txtRouteValue.text.toString().trim()
            val finalRouteValue = if (finalRoute == "No Route" || finalRoute == "No Route Assigned") null else finalRoute

            val updatedDriver = driver.copy(
                id = binding.etDriverId.text.toString().trim(),
                name = binding.etFullName.text.toString().trim(),
                cnic = binding.etCnic.text.toString().trim(),
                phone = binding.etPhone.text.toString().trim(),
                email = binding.etEmail.text.toString().trim(),
                assignedBus = finalBus,
                route = finalRouteValue,
                profileImageUrl = imageUrl
            )
            
            // 1. Dependency Logic: Sync with Bus and Route Repositories
            if (finalBus != null) {
                syncDriverToBusAndRoute(finalBus, updatedDriver.name)
            }

            // 2. Save to Firestore via FirebaseRepository
            com.example.bustrack_app.data.FirebaseRepository.saveDriver(updatedDriver) { success ->
                if (success) {
                    DriverRepository.updateDriver(updatedDriver)
                    Toast.makeText(this, "Profile Updated Successfully!", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    binding.btnUpdate.isEnabled = true
                    Toast.makeText(this, "Failed to update Firestore", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun syncDriverToBusAndRoute(busNo: String, driverName: String) {
        // 1. Update BusRepository
        val bus = BusRepository.getBusByNumber(busNo)
        bus?.let {
            BusRepository.updateBusDetails(busNo, it.copy(driverName = driverName))
        }

        // 2. Update RouteRepository (Interdependence)
        val allRoutes = RouteRepository.routeList.value ?: return
        val targetRoute = allRoutes.find { it.busNo == busNo }
        targetRoute?.let {
            RouteRepository.updateRoute(it.copy(driverName = driverName))
        }
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