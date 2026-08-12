package ui.admin

import android.app.Dialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.models.ParentModel
import com.example.bustrack_app.models.StudentModel
import com.example.bustrack_app.viewmodels.TrackingApprovalViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class TrackingApprovalDetailActivity : AppCompatActivity() {

    private val viewModel: TrackingApprovalViewModel by viewModels()
    private lateinit var container: LinearLayout
    private lateinit var btnApprove: Button
    private lateinit var btnReject: Button
    
    private var requestId: String? = null
    private var parentId: String? = null
    private var studentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_approval_detail)

        supportActionBar?.hide()

        requestId = intent.getStringExtra("REQUEST_ID")
        parentId = intent.getStringExtra("PARENT_ID")
        studentId = intent.getStringExtra("STUDENT_ID")
        val currentStatus = intent.getStringExtra("STATUS") ?: "Pending"

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        container = findViewById(R.id.containerStudents)
        btnApprove = findViewById(R.id.btnApprove)
        btnReject = findViewById(R.id.btnReject)

        setupObservers()
        
        if (parentId != null && studentId != null) {
            viewModel.loadDetails(parentId!!, studentId!!)
        }

        updateButtonUI(currentStatus)

        btnApprove.setOnClickListener {
            if (currentStatus.equals("Pending", true)) {
                Toast.makeText(this, "Please verify child and assign route below", Toast.LENGTH_LONG).show()
            } else {
                finish()
            }
        }

        btnReject.setOnClickListener {
            if (requestId != null) {
                viewModel.updateStatus(requestId!!, "Rejected", Firebase.auth.currentUser?.uid ?: "admin") { success ->
                    if (success) {
                        Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            }
        }
    }

    private fun setupObservers() {
        viewModel.parentData.observe(this) { parent ->
            parent?.let { updateParentUI(it) }
        }

        viewModel.studentData.observe(this) { student ->
            container.removeAllViews()
            if (student != null) {
                addStudentCard(container, student, intent.getStringExtra("STATUS") ?: "Pending")
            } else {
                val mockChild = StudentModel(id = studentId ?: "Unknown")
                addStudentCard(container, mockChild, intent.getStringExtra("STATUS") ?: "Pending")
            }
        }
    }

    private fun updateParentUI(parent: ParentModel) {
        findViewById<TextView>(R.id.tvParentName).text = parent.name
        findViewById<TextView>(R.id.tvParentCnic).text = "CNIC: ${parent.cnic}"
        findViewById<TextView>(R.id.tvParentPhone).text = "Phone: ${parent.phone}"
        findViewById<TextView>(R.id.tvRelationship).text = "Relationship: ${parent.relationship}"
    }

    private fun updateButtonUI(status: String) {
        if (status.equals("Approved", true)) {
            btnApprove.text = "Approved"
            btnApprove.isEnabled = false
            btnReject.visibility = View.GONE
        } else if (status.equals("Rejected", true)) {
            btnApprove.visibility = View.GONE
            btnReject.text = "Rejected"
            btnReject.isEnabled = false
        }
    }

    private fun addStudentCard(container: LinearLayout, student: StudentModel, status: String) {
        val cardView = layoutInflater.inflate(R.layout.item_student_approval_card, container, false)
        
        val tvName = cardView.findViewById<TextView>(R.id.tvStudentName)
        val tvId = cardView.findViewById<TextView>(R.id.tvStudentId)
        val tvSystemCheck = cardView.findViewById<TextView>(R.id.tvSystemCheck)
        val badge = cardView.findViewById<View>(R.id.badgeSystemCheck)
        val layoutRoute = cardView.findViewById<View>(R.id.layoutRouteInfo)
        val tvEnabledRoute = cardView.findViewById<TextView>(R.id.tvEnabledRoute)
        val btnView = cardView.findViewById<Button>(R.id.btnViewStudentDetail)
        val btnUpdate = cardView.findViewById<Button>(R.id.btnUpdateRoute)

        tvName.text = if (student.name.isEmpty()) "Unknown Student" else student.name
        tvId.text = "ID: ${student.id}"

        val studentExists = student.name.isNotEmpty()

        if (studentExists) {
            tvSystemCheck.text = if (status.equals("Approved", true)) "Active Tracking" else "Exists in System"
            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
            tvSystemCheck.setTextColor(Color.parseColor("#15803D"))
        } else {
            tvSystemCheck.text = "ID Not Found"
            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
            tvSystemCheck.setTextColor(Color.parseColor("#B91C1C"))
        }

        if (status.equals("Approved", true)) {
            layoutRoute.visibility = View.VISIBLE
            tvEnabledRoute.text = student.route ?: "No Route"
            btnUpdate.visibility = View.VISIBLE
            btnUpdate.text = "Change Route"
        } else if (status.equals("Pending", true) && studentExists) {
            btnUpdate.visibility = View.VISIBLE
            btnUpdate.text = "Approve & Assign"
        }

        btnView.setOnClickListener {
            if (studentExists) {
                val intent = Intent(this, StudentDetailsActivity::class.java)
                intent.putExtra("studentId", student.id)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show()
            }
        }

        btnUpdate.setOnClickListener {
            if (studentExists) {
                showRouteAssignmentDialog(student.id, student.route ?: "No Route Assigned", if (status.equals("Approved", true)) student.route else null)
            } else {
                Toast.makeText(this, "Cannot proceed: Student not in database", Toast.LENGTH_SHORT).show()
            }
        }

        container.addView(cardView)
    }

    private fun showRouteAssignmentDialog(studentId: String, systemRoute: String, currentEnabled: String?) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirm_enable_tracking)

        dialog.window?.let {
            it.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            it.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        }

        dialog.findViewById<TextView>(R.id.tvDialogStudentId).text = studentId
        dialog.findViewById<TextView>(R.id.tvSystemRoute).text = systemRoute

        val spinner = dialog.findViewById<Spinner>(R.id.spinnerRoutes)
        val routes = listOf("Route-01 (Gulshan)", "Route-02 (Johar)", "Route-03 (Defence)", "Route-08 (North)")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, routes)
        spinner.adapter = adapter

        val initialRoute = currentEnabled ?: systemRoute
        val routeIndex = routes.indexOfFirst { it.contains(initialRoute, true) }
        if (routeIndex != -1) spinner.setSelection(routeIndex)

        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmEnable)
        if (currentEnabled != null) btnConfirm.text = "Update Route"

        btnConfirm.setOnClickListener {
            val selectedRoute = spinner.selectedItem.toString()
            
            if (requestId != null) {
                viewModel.updateStatus(requestId!!, "Approved", Firebase.auth.currentUser?.uid ?: "admin") { success ->
                    if (success) {
                        Toast.makeText(this, "Tracking approved and updated to $selectedRoute", Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        finish()
                    }
                }
            } else {
                dialog.dismiss()
            }
        }

        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
