package ui.parent

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
import com.example.bustrack_app.R
import com.example.bustrack_app.models.StudentAttendanceModel
import com.example.bustrack_app.models.StudentModel

import com.example.bustrack_app.models.AttendanceRecordModel
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.data.ParentRepository
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentId: TextView
    private lateinit var rvAttendance: RecyclerView
    private lateinit var rvStudentSelector: RecyclerView
    
    private val parentRepository = ParentRepository()
    private val allAttendance = mutableListOf<AttendanceRecordModel>()
    private var selectedStudent: StudentModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_student_attendance)

        supportActionBar?.hide()

        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentId = findViewById(R.id.tvStudentId)
        rvAttendance = findViewById(R.id.rvAttendance)
        rvStudentSelector = findViewById(R.id.rvStudentSelector)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        setupChildSelector()
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
                        selectedStudent = selectedChild
                        loadStudentAttendance(selectedChild)
                    }
                    
                    // Default selection
                    selectedStudent = myChildren[0]
                    loadStudentAttendance(myChildren[0])
                }
            }
        }
    }

    private fun loadEmptyState() {
        tvStudentName.text = "No Approved Children"
        tvStudentId.text = "Please wait for Admin approval"
        rvAttendance.adapter = AttendanceAdapter(emptyList())
    }

    private fun loadStudentAttendance(student: StudentModel) {
        tvStudentName.text = student.name
        tvStudentId.text = "ID: ${student.id} | ${student.grade}"

        FirebaseRepository.fetchAttendance { list ->
            val studentAttendance = list.filter { it.studentId == student.id }
                .sortedByDescending { it.date } // Recent first
            
            runOnUiThread {
                rvAttendance.layoutManager = LinearLayoutManager(this)
                rvAttendance.adapter = AttendanceAdapter(studentAttendance)
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
            val ivAvatar: ImageView = view.findViewById(R.id.ivStudentAvatar)
            val tvName: TextView = view.findViewById(R.id.tvSelectorName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_student_selector, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val student = students[position]
            holder.tvName.text = student.name.split(" ")[0] // Show first name only for brevity
            
            // Highlight selected child
            if (position == selectedPosition) {
                holder.ivAvatar.alpha = 1.0f
                holder.tvName.setTextColor(Color.parseColor("#1E3A8A")) // Primary Blue
                holder.itemView.scaleX = 1.1f
                holder.itemView.scaleY = 1.1f
            } else {
                holder.ivAvatar.alpha = 0.5f
                holder.tvName.setTextColor(Color.GRAY)
                holder.itemView.scaleX = 1.0f
                holder.itemView.scaleY = 1.0f
            }

            holder.itemView.setOnClickListener {
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
            val tvPickupStatus: TextView = view.findViewById(R.id.tvPickupStatus)
            val tvPickupTime: TextView = view.findViewById(R.id.tvPickupTime)
            val tvDropStatus: TextView = view.findViewById(R.id.tvDropStatus)
            val tvDropTime: TextView = view.findViewById(R.id.tvDropTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_student_attendance, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = items[position]
            
            // Derive day from date if possible, or just show Date
            holder.tvDay.text = "Attendance" 
            holder.tvDate.text = item.date
            
            holder.tvPickupStatus.text = item.morningPickup
            holder.tvPickupTime.text = if (item.morningPickup.contains(":")) item.morningPickup else "--:--"
            
            holder.tvDropStatus.text = item.eveningPickup
            holder.tvDropTime.text = if (item.eveningPickup.contains(":")) item.eveningPickup else "--:--"

            // Dynamic colors for status
            setStatusColor(holder.tvPickupStatus, item.morningPickup)
            setStatusColor(holder.tvDropStatus, item.eveningPickup)
        }

        private fun setStatusColor(textView: TextView, status: String) {
            when {
                status.equals("Present", true) || status.contains(":") -> textView.setTextColor(0xFF4CAF50.toInt())
                status.equals("Absent", true) || status.equals("Missed", true) -> textView.setTextColor(0xFFF44336.toInt())
                else -> textView.setTextColor(0xFFFF9800.toInt())
            }
        }

        override fun getItemCount() = items.size
    }
}
