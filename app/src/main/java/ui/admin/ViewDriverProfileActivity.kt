package ui.admin

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.models.DriverModel
import com.google.android.material.button.MaterialButton
import utils.ViewUtils

class ViewDriverProfileActivity : AppCompatActivity() {

    private var driverData: DriverModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_driver_profile)

        // DYNAMIC DATA RECEIVE
        driverData = intent.getSerializableExtra("driver_data") as? DriverModel

        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        // Refresh from Repository
        driverData?.let { current ->
            val updated = DriverRepository.driverList.value?.find { it.id == current.id }
            updated?.let {
                driverData = it
                bindData(it)
            }
        }
    }

    private fun bindData(driver: DriverModel) {
        val txtName = findViewById<TextView>(R.id.txtDriverName)
        val txtId = findViewById<TextView>(R.id.txtDriverId)
        val txtCnic = findViewById<TextView>(R.id.txtCnicValue)
        val txtPhone = findViewById<TextView>(R.id.txtPhoneValue)
        val txtEmail = findViewById<TextView>(R.id.txtEmailValue)
        val txtRoute = findViewById<TextView>(R.id.txtRouteValue)
        val txtBus = findViewById<TextView>(R.id.txtBusValue)
        val imgAvatar = findViewById<ImageView>(R.id.imgDriverAvatar)

        txtName.text = driver.name
        txtId.text = "Driver ID: #${driver.id}"
        txtCnic.text = driver.cnic
        txtPhone.text = driver.phone
        txtEmail.text = driver.email
        txtRoute.text = driver.route ?: "Not Assigned"
        txtBus.text = driver.assignedBus ?: "Not Assigned"

        if (driver.profileImageUrl.isNotEmpty()) {
            Glide.with(this)
                .load(driver.profileImageUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(imgAvatar)
        } else if (driver.profileImage != 0) {
            imgAvatar.setImageResource(driver.profileImage)
        } else {
            imgAvatar.setImageResource(R.drawable.ic_person)
        }
    }

    private fun setupClickListeners() {
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnEdit = findViewById<Button>(R.id.btnNavigateToEdit)
        val btnDelete = findViewById<Button>(R.id.btnDeleteDriver)

        btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            finish()
        }

        btnEdit.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val intent = Intent(this, EditDriverActivity::class.java)
            intent.putExtra("driver_data", driverData)
            startActivity(intent)
        }

        btnDelete.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            showDeleteConfirmationDialog()
        }
    }

    private fun showDeleteConfirmationDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_delete_driver, null)
        val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val btnConfirmDelete = dialogView.findViewById<MaterialButton>(R.id.btnDelete)

        val dialogBuilder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(true)

        val alertDialog = dialogBuilder.create()
        alertDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

        alertDialog.show()

        // Size adjustment to prevent full screen
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        alertDialog.window?.setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT)

        btnCancel.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            alertDialog.dismiss()
        }

        btnConfirmDelete.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            driverData?.let { driver ->
                DriverRepository.deleteDriver(driver.id)
                Toast.makeText(this, "Driver Deleted Successfully", Toast.LENGTH_SHORT).show()
                finish()
            }
            alertDialog.dismiss()
        }
    }
}