package com.example.bustrack_app.ui.admin

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.graphics.toColorInt // 👈 Color.parseColor ki warning fix karne ke liye extension import
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityAddStudentBinding

class AddStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddStudentBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGradeSpinner()
        setupClickListeners()
    }

    private fun setupGradeSpinner() {
        // Grade dropdown items list load dynamically array setup mapping configuration
        val grades = arrayOf("Grade 9", "Grade 10", "Grade 11", "Grade 12", "BS IT 7th semester")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, grades)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerGrade.adapter = adapter
    }

    @SuppressLint("ClickableViewAccessibility") // Custom Touch Listener handling ke liye Android Studio requirement annotation
    private fun setupClickListeners() {
        // Top Back Header Navigation Action control trigger
        binding.btnBack.setOnClickListener {
            finish()
        }

        // Map Selection Module Interface Activation Click Hook
        binding.btnSelectOnMap.setOnClickListener {
            Toast.makeText(this, "Opening Map Route Picker...", Toast.LENGTH_SHORT).show()
        }

        // =====================================================================
        // 1. --- Cancel Form Button Touch & Click Listener ---
        // =====================================================================
        binding.btnCancelForm.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Jab user click kare (dabaaye rakhay) -> Text White aur Background primaryBlue ho jaye
                    binding.btnCancelForm.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    binding.btnCancelForm.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryBlue))
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick() // 👈 performClick warning ka permanent proper handle!
                    // Jab user click chhor de -> Original default style reverse ho jaye
                    binding.btnCancelForm.setTextColor(ContextCompat.getColor(this, R.color.primaryBlue))
                    binding.btnCancelForm.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                }
                MotionEvent.ACTION_CANCEL -> {
                    binding.btnCancelForm.setTextColor(ContextCompat.getColor(this, R.color.primaryBlue))
                    binding.btnCancelForm.setBackgroundColor(ContextCompat.getColor(this, android.R.color.transparent))
                }
            }
            false // Click listener properly cascade hone ke liye false rakha hai
        }
        binding.btnCancelForm.setOnClickListener {
            finish()
        }

        // =====================================================================
        // 2. --- Only Add Button Touch & Click Listener ---
        // =====================================================================
        binding.btnOnlyAdd.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Click karne par text white aur background fill ho jaye
                    binding.btnOnlyAdd.setTextColor(ContextCompat.getColor(this, android.R.color.white))
                    binding.btnOnlyAdd.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryBlue))
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick() // 👈 Accessibility click compliance handle node
                    // Ungli uthane par wapas normal shape aur light gray background ho jaye
                    binding.btnOnlyAdd.setTextColor(ContextCompat.getColor(this, R.color.primaryBlue))
                    binding.btnOnlyAdd.setBackgroundColor("#F2F2F7".toColorInt()) // 👈 String extension KTX logic fix!
                }
                MotionEvent.ACTION_CANCEL -> {
                    binding.btnOnlyAdd.setTextColor(ContextCompat.getColor(this, R.color.primaryBlue))
                    binding.btnOnlyAdd.setBackgroundColor("#F2F2F7".toColorInt())
                }
            }
            false
        }
        binding.btnOnlyAdd.setOnClickListener {
            if (validateFormInputFields()) {
                Toast.makeText(this, "Student Profile Successfully Enrolled!", Toast.LENGTH_LONG).show()
                finish()
            }
        }

        // =====================================================================
        // 3. --- Add & Next Button Touch & Click Listener ---
        // =====================================================================
        binding.btnAddAndNext.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Click hone par dark background thoda change hokar primaryBlue feedback de
                    binding.btnAddAndNext.setBackgroundColor(ContextCompat.getColor(this, R.color.primaryBlue))
                }
                MotionEvent.ACTION_UP -> {
                    v.performClick() // 👈 Perform click node integration
                    // Chhorne par wapas original primaryDark color (#0A1D37) set ho jaye
                    binding.btnAddAndNext.setBackgroundColor("#0A1D37".toColorInt()) // 👈 Hex validation KTX fix!
                }
                MotionEvent.ACTION_CANCEL -> {
                    binding.btnAddAndNext.setBackgroundColor("#0A1D37".toColorInt())
                }
            }
            false
        }
        binding.btnAddAndNext.setOnClickListener {
            if (validateFormInputFields()) {
                Toast.makeText(this, "Moving to Step 2: Documents Verification System", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun validateFormInputFields(): Boolean {
        if (binding.etFullName.text.toString().trim().isEmpty()) {
            binding.etFullName.error = "Student name required"
            return false
        }
        if (binding.etParentName.text.toString().trim().isEmpty()) {
            binding.etParentName.error = "Parent name required"
            return false
        }
        if (binding.etEmergencyContact.text.toString().trim().isEmpty()) {
            binding.etEmergencyContact.error = "Contact configuration required"
            return false
        }
        return true
    }
}