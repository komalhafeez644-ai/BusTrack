package ui.admin

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityAddDriverBinding
import com.example.bustrack_app.models.DriverModel

class AddDriverActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddDriverBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddDriverBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAddDriver.setOnClickListener {
            val name = binding.etFullName.text.toString()
            val empId = binding.etEmployeeId.text.toString()
            val cnic = binding.etCnic.text.toString()
            val phone = binding.etPhone.text.toString()
            val email = binding.etEmail.text.toString()
            val pass = binding.etPassword.text.toString()
            val confirmPass = binding.etConfirmPassword.text.toString()

            // Basic Validation
            if (name.isEmpty() || empId.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Please fill required fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (pass != confirmPass) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Naya Driver Object banaya
            val newDriver = DriverModel(
                id = empId,
                name = name,
                status = "Idle",
                assignedBus = "Not Assigned",
                route = "Not Assigned",
                profileImage = 0,
                cnic = cnic,
                phone = phone,
                email = email
            )

            // Direct finish karne ki bajaye Success Dialog dikhayein
            showSuccessDialog(name, newDriver)
        }

        binding.btnBack.setOnClickListener { finish() }
        binding.btnCancel.setOnClickListener { finish() }
    }

    // Custom Success Dialog Function
    private fun showSuccessDialog(driverName: String, newDriver: DriverModel) {
        val dialog = Dialog(this)
        dialog.setContentView(R.layout.layout_success_dialog)

        // Dialog ka background transparent karna zaroori hai corners rounded dikhane ke liye
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)

        val btnDone = dialog.findViewById<Button>(R.id.btnDone)
        val txtMessage = dialog.findViewById<TextView>(R.id.dialogMessage)

        txtMessage.text = "$driverName has been added successfully to the fleet management system."

        btnDone.setOnClickListener {
            dialog.dismiss()

            // Intent ke zariye data wapis bhejna aur screen band karna
            val intent = Intent()
            intent.putExtra("new_driver_data", newDriver)
            setResult(RESULT_OK, intent)
            finish()
        }

        dialog.show()
    }
}