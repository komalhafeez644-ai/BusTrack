package ui.parent

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.models.StudentModel
import com.google.android.material.imageview.ShapeableImageView

import com.example.bustrack_app.models.AttendanceRecordModel
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.data.ParentRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var tvStudentName: TextView
    private lateinit var tvGrade: TextView
    private lateinit var tvStudentId: TextView
    private lateinit var tvStatusBadge: TextView
    private lateinit var ivStudentProfile: ShapeableImageView
    private lateinit var tvAvatar: TextView
    private lateinit var layoutLocation: View
    private lateinit var tvLocationInfo: TextView
    private lateinit var layoutBus: View
    private lateinit var tvBusInfo: TextView
    private lateinit var btnViewDetails: View
    
    private lateinit var rvAttendance: RecyclerView
    private lateinit var rvStudentSelector: RecyclerView
    private lateinit var tvSelectedMonth: TextView
    private lateinit var btnMonthFilter: View
    private lateinit var tvEmptyState: TextView
    
    private val parentRepository = ParentRepository()
    private var selectedStudent: StudentModel? = null
    private var currentCalendar = java.util.Calendar.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_attendance)

        supportActionBar?.hide()

        val cardInclude = findViewById<View>(R.id.studentCardInclude)
        tvStudentName = cardInclude.findViewById(R.id.txtStudentName)
        tvGrade = cardInclude.findViewById(R.id.txtGrade)
        tvStudentId = cardInclude.findViewById(R.id.txtStudentId)
        tvStatusBadge = cardInclude.findViewById(R.id.statusBadge)
        ivStudentProfile = cardInclude.findViewById(R.id.imgStudent)
        tvAvatar = cardInclude.findViewById(R.id.txtAvatar)
        layoutLocation = cardInclude.findViewById(R.id.layoutLocation)
        tvLocationInfo = cardInclude.findViewById(R.id.txtLocationInfo)
        layoutBus = cardInclude.findViewById(R.id.layoutBus)
        tvBusInfo = cardInclude.findViewById(R.id.txtBusInfo)
        btnViewDetails = cardInclude.findViewById(R.id.btnAction)

        rvAttendance = findViewById(R.id.rvAttendance)
        rvStudentSelector = findViewById(R.id.rvStudentSelector)
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth)
        btnMonthFilter = findViewById(R.id.btnMonthFilter)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        updateMonthDisplay()
        
        btnMonthFilter.setOnClickListener {
            showMonthPicker()
        }

        setupChildSelector()
    }

    private fun updateMonthDisplay() {
        val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        tvSelectedMonth.text = sdf.format(currentCalendar.time)
    }

    private fun showMonthPicker() {
        val monthYearList = mutableListOf<String>()
        val calList = mutableListOf<java.util.Calendar>()
        
        val tempCal = java.util.Calendar.getInstance()
        tempCal.add(java.util.Calendar.MONTH, 1) // Start from next month
        
        val sdf = java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault())
        
        for (i in 0 until 12) {
            monthYearList.add(sdf.format(tempCal.time))
            calList.add(tempCal.clone() as java.util.Calendar)
            tempCal.add(java.util.Calendar.MONTH, -1)
        }
        
        val dialog = android.app.AlertDialog.Builder(this)
        dialog.setTitle("Select Month")
        dialog.setItems(monthYearList.toTypedArray()) { _, which ->
            val selectedCal = calList[which]
            currentCalendar.set(java.util.Calendar.MONTH, selectedCal.get(java.util.Calendar.MONTH))
            currentCalendar.set(java.util.Calendar.YEAR, selectedCal.get(java.util.Calendar.YEAR))
            updateMonthDisplay()
            selectedStudent?.let { loadStudentAttendance(it) }
        }
        dialog.show()
    }

    private fun setupChildSelector() {
        parentRepository.listenToAllTrackingRequests { requests ->
            val approvedIds = requests.filter { it.status.uppercase() == "APPROVED" }.map { it.studentId }
            
            if (approvedIds.isEmpty()) {
                loadEmptyState()
                return@listenToAllTrackingRequests
            }

            FirebaseRepository.fetchStudents { allStudents ->
                val myChildren = allStudents.filter { approvedIds.contains(it.id) }
                
                if (myChildren.isEmpty()) {
                    loadEmptyState()
                    return@fetchStudents
                }

                runOnUiThread {
                    rvStudentSelector.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
                    rvStudentSelector.adapter = StudentSelectorAdapter(myChildren) { selectedChild ->
                        if (selectedStudent?.id != selectedChild.id) {
                            selectedStudent = selectedChild
                            loadStudentAttendance(selectedChild)
                        }
                    }
                    
                    // Default selection if not already set
                    if (selectedStudent == null) {
                        selectedStudent = myChildren[0]
                        loadStudentAttendance(myChildren[0])
                    }
                }
            }
        }
    }

    private fun loadEmptyState() {
        tvStudentName.text = "No Approved Children"
        tvStudentId.text = "Please wait for Admin approval"
        rvAttendance.adapter = AttendanceAdapter(emptyList())
        tvEmptyState.visibility = View.VISIBLE
    }

    private fun loadStudentAttendance(student: StudentModel) {
        // Update Card UI (Matching StudentAdapter logic for consistency)
        tvStudentName.text = student.name
        tvGrade.text = student.grade
        tvStudentId.text = "ID: ${student.id}"

        val route = student.route ?: ""
        btnViewDetails.visibility = View.GONE // Hide action button in attendance screen

        if (route.isEmpty()) {
            tvStatusBadge.text = "● UNASSIGNED"
            tvStatusBadge.setBackgroundResource(R.drawable.bg_status_badge_red)
            tvStatusBadge.backgroundTintList = null
            tvStatusBadge.setTextColor(Color.parseColor("#EF4444"))

            layoutLocation.visibility = View.VISIBLE
            tvLocationInfo.text = student.location.ifEmpty { "Location not set" }
            layoutBus.visibility = View.GONE
        } else {
            val statusText = if (student.isActive) "ACTIVE" else "INACTIVE"
            val statusColor = if (student.isActive) "#22C55E" else "#64748B"

            tvStatusBadge.text = "● $statusText"
            tvStatusBadge.setBackgroundResource(R.drawable.bg_chip_selected)
            tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
            tvStatusBadge.setTextColor(Color.parseColor(statusColor))

            layoutLocation.visibility = View.GONE
            layoutBus.visibility = View.VISIBLE
            tvBusInfo.text = "${route}: ${student.busNo ?: "No Bus"}"
        }

        // Image Loading Logic
        if (student.profileImageUrl.isNotEmpty()) {
            ivStudentProfile.visibility = View.VISIBLE
            tvAvatar.visibility = View.GONE
            Glide.with(this).load(student.profileImageUrl).placeholder(R.drawable.ic_person).into(ivStudentProfile)
        } else {
            ivStudentProfile.visibility = View.GONE
            tvAvatar.visibility = View.VISIBLE
            val initials = student.name.split(" ").filter { it.isNotEmpty() }.map { it[0] }.take(2).joinToString("")
            tvAvatar.text = initials.uppercase()
        }

        val targetMonth = currentCalendar.get(java.util.Calendar.MONTH) + 1
        val targetYear = currentCalendar.get(java.util.Calendar.YEAR)

        FirebaseRepository.fetchAttendance { list ->
            val studentAttendance = list.filter { record ->
                if (record.studentId != student.id) return@filter false
                
                val parts = record.date.replace("/", "-").split("-") // Handle both formats
                if (parts.size == 3) {
                    val m = parts[1].toIntOrNull() ?: 0
                    val y = parts[2].toIntOrNull() ?: 0
                    m == targetMonth && y == targetYear
                } else {
                    false
                }
            }.sortedBy { record -> // Sort by date ascending (01 to 31)
                val parts = record.date.replace("/", "-").split("-")
                parts[0].toIntOrNull() ?: 0
            }
            
            runOnUiThread {
                rvAttendance.layoutManager = LinearLayoutManager(this)
                rvAttendance.adapter = AttendanceAdapter(studentAttendance)
                tvEmptyState.visibility = if (studentAttendance.isEmpty()) View.VISIBLE else View.GONE
            }
        }
    }

    // --- Adapters ---

    class StudentSelectorAdapter(
        private val students: List<StudentModel>,
        private val onStudentSelected: (StudentModel) -> Unit
    ) : RecyclerView.Adapter<StudentSelectorAdapter.ViewHolder>() {

        private var selectedPosition = 0

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val ivAvatar: ShapeableImageView = view.findViewById(R.id.ivStudentAvatar)
            val tvName: TextView = view.findViewById(R.id.tvSelectorName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_selector, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val student = students[position]
            holder.tvName.text = student.name.split(" ")[0]
            
            if (student.profileImageUrl.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(student.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .into(holder.ivAvatar)
            } else {
                holder.ivAvatar.setImageResource(R.drawable.ic_person)
            }

            if (position == selectedPosition) {
                holder.ivAvatar.alpha = 1.0f
                holder.ivAvatar.strokeWidth = 6f
                holder.tvName.setTextColor(Color.parseColor("#1E3A8A"))
                holder.itemView.scaleX = 1.1f
                holder.itemView.scaleY = 1.1f
            } else {
                holder.ivAvatar.alpha = 0.6f
                holder.ivAvatar.strokeWidth = 0f
                holder.tvName.setTextColor(Color.GRAY)
                holder.itemView.scaleX = 1.0f
                holder.itemView.scaleY = 1.0f
            }

            holder.itemView.setOnClickListener {
                if (selectedPosition == holder.adapterPosition) return@setOnClickListener
                val previous = selectedPosition
                selectedPosition = holder.adapterPosition
                notifyItemChanged(previous)
                notifyItemChanged(selectedPosition)
                onStudentSelected(student)
            }
        }

        override fun getItemCount() = students.size
    }

    class AttendanceAdapter(private val items: List<AttendanceRecordModel>) :
        RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvDay: TextView = view.findViewById(R.id.tvDay)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            val tvMorningTime: TextView = view.findViewById(R.id.tvMorningTime)
            val tvEveningTime: TextView = view.findViewById(R.id.tvEveningTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_student_attendance, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            
            // Format: dd-MM-yyyy -> dd MMM, yyyy
            try {
                val normalizedDate = item.date.replace("/", "-")
                val inputFormat = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
                val outputFormat = java.text.SimpleDateFormat("dd MMM, yyyy", java.util.Locale.getDefault())
                val dayFormat = java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault())
                val dateObj = inputFormat.parse(normalizedDate)
                if (dateObj != null) {
                    holder.tvDate.text = outputFormat.format(dateObj)
                    holder.tvDay.text = dayFormat.format(dateObj)
                } else {
                    holder.tvDate.text = item.date
                    holder.tvDay.text = "Attendance"
                }
            } catch (e: Exception) {
                holder.tvDate.text = item.date
                holder.tvDay.text = "Attendance"
            }
            
            val isPresent = item.morningPickup.equals("Present", true) || 
                            item.morningPickup.contains(":") || 
                            item.eveningPickup.equals("Present", true) || 
                            item.eveningPickup.contains(":")
            
            if (isPresent) {
                holder.tvStatus.text = "Present"
                holder.tvStatus.setTextColor(Color.parseColor("#15803D"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
            } else if (item.morningPickup.equals("Leave", true) || item.eveningPickup.equals("Leave", true)) {
                holder.tvStatus.text = "Leave"
                holder.tvStatus.setTextColor(Color.parseColor("#D97706"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_yellow)
            } else {
                holder.tvStatus.text = "Absent"
                holder.tvStatus.setTextColor(Color.parseColor("#991B1B"))
                holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge_red)
            }

            // Morning Timing
            val mTime = when {
                item.morningPickup.contains(":") -> item.morningPickup
                else -> "--:--"
            }
            holder.tvMorningTime.text = "M: $mTime"

            // Evening Timing
            val eTime = when {
                item.eveningDrop.contains(":") -> item.eveningDrop
                item.eveningPickup.contains(":") -> item.eveningPickup // Check both fields
                else -> "--:--"
            }
            holder.tvEveningTime.text = "E: $eTime"
        }

        override fun getItemCount() = items.size
    }
}
