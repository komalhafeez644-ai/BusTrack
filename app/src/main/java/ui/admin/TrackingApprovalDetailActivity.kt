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
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.StudentRepository

class TrackingApprovalDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_approval_detail)

        supportActionBar?.hide()

        val parentName = intent.getStringExtra("PARENT_NAME") ?: "Parent User"
        val currentStatus = intent.getStringExtra("STATUS") ?: "Pending"
        val childrenJson = intent.getStringExtra("CHILDREN_JSON")

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        findViewById<TextView>(R.id.tvParentName).text = parentName
        
        val container = findViewById<LinearLayout>(R.id.containerStudents)
        val btnApprove = findViewById<Button>(R.id.btnApprove)
        val btnReject = findViewById<Button>(R.id.btnReject)

        // Parse children from intent
        val gson = com.google.gson.Gson()
        val type = object : com.google.gson.reflect.TypeToken<List<TrackingRequestsActivity.ChildInfo>>() {}.type
        val requestedChildren: List<TrackingRequestsActivity.ChildInfo> = if (childrenJson != null) {
            gson.fromJson(childrenJson, type)
        } else {
            emptyList()
        }

        // --- Multi-Child Analysis ---
        // 1. Find the parent identity (Phone or Father Name) from the first requested child
        val allStudents = StudentRepository.studentList.value ?: emptyList()
        var parentPhone: String? = null
        var fatherNameInRepo: String? = null
        
        requestedChildren.firstOrNull()?.let { firstChild ->
            val repoStudent = allStudents.find { it.id == firstChild.id || it.name.equals(firstChild.name, true) }
            parentPhone = repoStudent?.phoneNumber
            fatherNameInRepo = repoStudent?.fatherName
        }

        // 2. Find all children of this parent in the system
        val systemChildren = if (parentPhone != null || fatherNameInRepo != null) {
            allStudents.filter { 
                (parentPhone != null && it.phoneNumber == parentPhone) || 
                (fatherNameInRepo != null && it.fatherName.equals(fatherNameInRepo, true))
            }
        } else {
            emptyList()
        }

        // 3. Merge requested children with system children to ensure we show everything
        // We use a Map to avoid duplicates by Student ID or Name
        val finalChildrenMap = mutableMapOf<String, TrackingRequestsActivity.ChildInfo>()
        
        // Add requested ones first
        requestedChildren.forEach { finalChildrenMap[it.id] = it }
        
        // Add others found in system
        systemChildren.forEach { student ->
            if (!finalChildrenMap.containsKey(student.id)) {
                finalChildrenMap[student.id] = TrackingRequestsActivity.ChildInfo(
                    student.name, 
                    student.id, 
                    if (student.status == "ASSIGNED") student.route else null
                )
            }
        }

        // Add children cards dynamically
        finalChildrenMap.values.forEach { child ->
            addStudentCard(container, child, currentStatus)
        }

        // Handle UI based on status
        if (currentStatus == "Approved") {
            btnApprove.visibility = View.GONE // Individually managed now
            btnReject.visibility = View.GONE
        } else if (currentStatus == "Rejected") {
            btnApprove.visibility = View.GONE
            btnReject.text = "Rejected"
            btnReject.isEnabled = false
        }

        btnApprove.setOnClickListener {
            // Bulk approve logic if needed, or just a toast
            Toast.makeText(this, "Proceeding with verification...", Toast.LENGTH_SHORT).show()
        }

        btnReject.setOnClickListener {
            Toast.makeText(this, "Request rejected", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun addStudentCard(container: LinearLayout, child: TrackingRequestsActivity.ChildInfo, status: String) {
        val cardView = layoutInflater.inflate(R.layout.item_student_approval_card, container, false)
        
        val tvName = cardView.findViewById<TextView>(R.id.tvStudentName)
        val tvId = cardView.findViewById<TextView>(R.id.tvStudentId)
        val tvSystemCheck = cardView.findViewById<TextView>(R.id.tvSystemCheck)
        val badge = cardView.findViewById<View>(R.id.badgeSystemCheck)
        val layoutRoute = cardView.findViewById<View>(R.id.layoutRouteInfo)
        val tvEnabledRoute = cardView.findViewById<TextView>(R.id.tvEnabledRoute)
        val btnView = cardView.findViewById<Button>(R.id.btnViewStudentDetail)
        val btnUpdate = cardView.findViewById<Button>(R.id.btnUpdateRoute)

        tvName.text = child.name
        tvId.text = "ID: ${child.id}"

        // Verify if Student exists in repository
        val student = StudentRepository.studentList.value?.find { it.id == child.id || it.name.equals(child.name, true) }
        val studentExists = student != null

        if (studentExists) {
            tvSystemCheck.text = if (status == "Approved") "Active Tracking" else "Exists in System"
            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
            tvSystemCheck.setTextColor(Color.parseColor("#15803D"))
        } else {
            tvSystemCheck.text = "ID Not Found"
            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
            tvSystemCheck.setTextColor(Color.parseColor("#B91C1C"))
        }

        // Route info
        if (status == "Approved" && child.enabledRoute != null) {
            layoutRoute.visibility = View.VISIBLE
            tvEnabledRoute.text = child.enabledRoute
            btnUpdate.visibility = View.VISIBLE
            btnUpdate.text = "Change Route"
        } else if (status == "Pending" && studentExists) {
            btnUpdate.visibility = View.VISIBLE
            btnUpdate.text = "Approve & Assign"
        }

        btnView.setOnClickListener {
            if (studentExists) {
                val intent = Intent(this, StudentDetailsActivity::class.java)
                intent.putExtra("studentId", student?.id ?: child.id)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show()
            }
        }

        btnUpdate.setOnClickListener {
            if (studentExists) {
                showRouteAssignmentDialog(student?.id ?: child.id, student?.route ?: "No Route Assigned", child.enabledRoute)
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

        // Pre-select current enabled route if it exists, else system route
        val initialRoute = currentEnabled ?: systemRoute
        val routeIndex = routes.indexOfFirst { it.contains(initialRoute, true) }
        if (routeIndex != -1) spinner.setSelection(routeIndex)

        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmEnable)
        if (currentEnabled != null) btnConfirm.text = "Update Route"

        btnConfirm.setOnClickListener {
            val selectedRoute = spinner.selectedItem.toString()
            Toast.makeText(this, "Tracking updated to $selectedRoute", Toast.LENGTH_LONG).show()
            dialog.dismiss()
            finish()
        }

        dialog.findViewById<Button>(R.id.btnCancel).setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
