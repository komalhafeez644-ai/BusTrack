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
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.ParentModel
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StudentModel
import com.example.bustrack_app.viewmodels.TrackingApprovalViewModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import utils.ViewUtils

class TrackingApprovalDetailActivity : AppCompatActivity() {

    private val viewModel: TrackingApprovalViewModel by viewModels()
    private lateinit var container: LinearLayout
    private lateinit var btnApprove: Button
    private lateinit var btnReject: Button
    private lateinit var btnRework: Button
    private lateinit var btnDisableEnable: Button
    
    private var requestId: String? = null
    private var parentId: String? = null
    private var studentId: String? = null
    private var currentStatus: String = "PENDING"
    private var currentTrackingEnabled: Boolean = false
    private var currentTrackingState: String = ""
    private var availableRoutes: List<RouteModel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_approval_detail)

        supportActionBar?.hide()

        requestId = intent.getStringExtra("REQUEST_ID")
        parentId = intent.getStringExtra("PARENT_ID")
        studentId = intent.getStringExtra("STUDENT_ID")
        currentStatus = intent.getStringExtra("STATUS")?.uppercase() ?: "PENDING"

        findViewById<View>(R.id.btnBack).setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            finish() 
        }

        container = findViewById(R.id.containerStudents)
        btnApprove = findViewById(R.id.btnApprove)
        btnReject = findViewById(R.id.btnReject)
        btnRework = findViewById(R.id.btnRework)
        btnDisableEnable = findViewById(R.id.btnDisableEnable)

        setupObservers()
        
        if (parentId != null && studentId != null) {
            viewModel.loadDetails(parentId!!, studentId!!)
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        btnApprove.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleApprove()
        }

        btnReject.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleReject()
        }

        btnDisableEnable.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleDisableEnable()
        }

        btnRework.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            handleRework()
        }
    }

    private fun handleApprove() {
        val student = viewModel.studentData.value
        if (student == null) {
            Toast.makeText(this, "Loading student data. Please wait.", Toast.LENGTH_SHORT).show()
            return
        }

        val assignedRoute = student.route ?: "Unassigned"
        
        requestId?.let { id ->
            viewModel.updateTrackingRequest(id, "APPROVED", true, "ENABLED", Firebase.auth.currentUser?.uid ?: "admin", assignedRoute) { success ->
                if (success) {
                    parentId?.let { pid ->
                        FirebaseRepository.sendNotification(
                            recipientId = pid,
                            title = "Tracking Request Approved",
                            message = "Your tracking request has been approved. You can now track your child's bus on route $assignedRoute.",
                            type = "TRACKING_APPROVED",
                            relatedId = studentId ?: ""
                        )
                    }
                    showSuccessDialog("Tracking Enabled", "Parent tracking access has been enabled for the student's assigned route.")
                    currentStatus = "APPROVED"
                    currentTrackingEnabled = true
                    currentTrackingState = "ENABLED"
                    updateButtonUI()
                } else {
                    Toast.makeText(this, "Approval failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleReject() {
        requestId?.let { id ->
            viewModel.updateTrackingRequest(id, "REJECTED", false, "REJECTED", Firebase.auth.currentUser?.uid ?: "admin") { success ->
                if (success) {
                    parentId?.let { pid ->
                        FirebaseRepository.sendNotification(
                            recipientId = pid,
                            title = "Tracking Request Rejected",
                            message = "Your tracking request was not approved. Please contact the college administration for details.",
                            type = "TRACKING_REJECTED",
                            relatedId = studentId ?: ""
                        )
                    }
                    Toast.makeText(this, "Request Rejected. Parent will be notified.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }

    private fun handleDisableEnable() {
        val nextTrackingEnabled = !currentTrackingEnabled
        val nextTrackingState = if (nextTrackingEnabled) "ENABLED" else "DISABLED"
        val msg = if (nextTrackingEnabled) "Tracking Enabled" else "Tracking Temporarily Disabled"
        
        requestId?.let { id ->
            viewModel.updateTrackingRequest(id, "APPROVED", nextTrackingEnabled, nextTrackingState, Firebase.auth.currentUser?.uid ?: "admin") { success ->
                if (success) {
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    currentTrackingEnabled = nextTrackingEnabled
                    currentTrackingState = nextTrackingState
                    updateButtonUI()
                }
            }
        }
    }

    private fun handleRework() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_confirm_status)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val tvTitle = dialog.findViewById<TextView>(R.id.tvDialogTitle)
        val tvMsg = dialog.findViewById<TextView>(R.id.tvDialogMessage)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirm)
        val btnCancel = dialog.findViewById<Button>(R.id.btnCancel)

        tvTitle.text = "Permanent Revocation?"
        tvMsg.text = "Are you sure you want to permanently revoke tracking access (Rework)? This cannot be undone from this screen."
        btnConfirm.text = "Confirm Rework"
        btnConfirm.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D32F2F"))

        btnConfirm.setOnClickListener {
            requestId?.let { id ->
                viewModel.updateTrackingRequest(id, "REWORK", false, "REVOKED", Firebase.auth.currentUser?.uid ?: "admin") { success ->
                    if (success) {
                        parentId?.let { pid ->
                            FirebaseRepository.sendNotification(
                                recipientId = pid,
                                title = "Tracking Access Revoked",
                                message = "Your tracking access has been revoked by the administration.",
                                type = "TRACKING_REVOKED",
                                relatedId = studentId ?: ""
                            )
                        }
                        Toast.makeText(this, "Request moved to Rework (Historical Record)", Toast.LENGTH_SHORT).show()
                        currentStatus = "REWORK"
                        currentTrackingEnabled = false
                        currentTrackingState = "REVOKED"
                        updateButtonUI()
                        dialog.dismiss()
                    }
                }
            }
        }
        btnCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun updateButtonUI() {
        when (currentStatus) {
            "PENDING" -> {
                btnApprove.visibility = View.VISIBLE
                btnReject.visibility = View.VISIBLE
                btnRework.visibility = View.GONE
                btnDisableEnable.visibility = View.GONE
            }
            "APPROVED" -> {
                btnApprove.visibility = View.GONE
                btnReject.visibility = View.GONE
                btnRework.visibility = View.VISIBLE
                btnDisableEnable.visibility = View.VISIBLE
                
                if (currentTrackingEnabled) {
                    btnDisableEnable.text = "DISABLE"
                } else {
                    btnDisableEnable.text = "ENABLE"
                }
            }
            "REWORK" -> {
                btnApprove.visibility = View.GONE
                btnReject.visibility = View.GONE
                btnRework.visibility = View.GONE
                btnDisableEnable.visibility = View.GONE
            }
            else -> {
                btnApprove.visibility = View.GONE
                btnReject.visibility = View.GONE
                btnRework.visibility = View.GONE
                btnDisableEnable.visibility = View.GONE
            }
        }
    }

    private fun showSuccessDialog(title: String, message: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_success)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.findViewById<TextView>(R.id.tvTitle).text = title
        dialog.findViewById<TextView>(R.id.tvMessage).text = message
        
        dialog.findViewById<Button>(R.id.btnDone).setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun setupObservers() {
        viewModel.parentData.observe(this) { parent ->
            parent?.let { updateParentUI(it) }
        }

        FirebaseRepository.fetchTrackingRequests { requests ->
            requests.find { it.requestId == requestId }?.let { req ->
                currentStatus = req.status.uppercase()
                currentTrackingEnabled = req.trackingEnabled
                currentTrackingState = req.trackingState
                updateButtonUI()
            }
        }

        viewModel.isLoadingStudent.observe(this) { isLoading ->
            val currentStudent = viewModel.studentData.value
            container.removeAllViews()
            if (currentStudent != null) {
                addStudentCard(container, currentStudent, currentStatus, isLoading)
            } else {
                val mockChild = StudentModel(id = studentId ?: "Unknown")
                addStudentCard(container, mockChild, currentStatus, isLoading)
            }
        }

        viewModel.studentData.observe(this) { student ->
            val isLoading = viewModel.isLoadingStudent.value ?: false
            container.removeAllViews()
            if (student != null) {
                addStudentCard(container, student, currentStatus, isLoading)
            } else {
                val mockChild = StudentModel(id = studentId ?: "Unknown")
                addStudentCard(container, mockChild, currentStatus, isLoading)
            }
        }

        RouteRepository.routeList.observe(this) { routes ->
            availableRoutes = routes
        }
    }

    private fun updateParentUI(parent: ParentModel) {
        findViewById<TextView>(R.id.tvParentName).text = parent.name
        findViewById<TextView>(R.id.tvParentCnic).text = "CNIC: ${parent.cnic}"
        findViewById<TextView>(R.id.tvParentPhone).text = "Phone: ${parent.phone}"
        findViewById<TextView>(R.id.tvRelationship).text = "Relationship: ${parent.relationship}"
    }

    private fun addStudentCard(container: LinearLayout, student: StudentModel, status: String, isLoading: Boolean = false) {
        val cardView = layoutInflater.inflate(R.layout.item_student_approval_card, container, false)
        
        val tvName = cardView.findViewById<TextView>(R.id.tvStudentName)
        val tvId = cardView.findViewById<TextView>(R.id.tvStudentId)
        val tvSystemCheck = cardView.findViewById<TextView>(R.id.tvSystemCheck)
        val badge = cardView.findViewById<View>(R.id.badgeSystemCheck)
        val layoutRoute = cardView.findViewById<View>(R.id.layoutRouteInfo)
        val btnView = cardView.findViewById<Button>(R.id.btnViewStudentDetail)
        val btnUpdate = cardView.findViewById<Button>(R.id.btnUpdateRoute)

        tvName.text = if (student.name.isEmpty()) (if (isLoading) "Loading..." else "Unknown Student") else student.name
        tvId.text = "ID: ${student.id}"

        val studentExists = student.name.isNotEmpty()

        if (isLoading) {
            tvSystemCheck.text = "Verifying..."
            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
            tvSystemCheck.setTextColor(Color.parseColor("#92400E"))
        } else if (studentExists) {
            tvSystemCheck.text = if (status.equals("APPROVED", true)) "Active Tracking" else "Exists in System"
            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
            tvSystemCheck.setTextColor(Color.parseColor("#15803D"))
        } else {
            tvSystemCheck.text = "ID Not Found"
            badge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
            tvSystemCheck.setTextColor(Color.parseColor("#B91C1C"))
        }

        layoutRoute.visibility = View.GONE
        btnUpdate.visibility = View.GONE

        btnView.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            if (isLoading) {
                Toast.makeText(this, "Loading student data, please wait...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (studentExists) {
                val existsInDb = StudentRepository.studentList.value?.any { it.id == student.id } == true
                if (existsInDb) {
                    val intent = Intent(this, StudentDetailsActivity::class.java)
                    intent.putExtra("STUDENT_ID", student.id)
                    intent.putExtra("VIEW_ONLY", true)
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Student record not found in database", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Student profile not found", Toast.LENGTH_SHORT).show()
            }
        }
        container.addView(cardView)
    }
}
