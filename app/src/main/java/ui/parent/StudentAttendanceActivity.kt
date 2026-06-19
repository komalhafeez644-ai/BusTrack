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

class StudentAttendanceActivity : AppCompatActivity() {

    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentId: TextView
    private lateinit var rvAttendance: RecyclerView
    private lateinit var rvStudentSelector: RecyclerView

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
        // Default selection
        loadStudentAttendance("Ali Hassan", "ID: #SR-2045 | BS IT 7th Sem")
    }

    private fun setupChildSelector() {
        val children = listOf(
            StudentModel("#SR-2045", "Ali Hassan", "BS IT 7th Sem"),
            StudentModel("#SR-1011", "Zoya Khan", "Grade 10"),
            StudentModel("#SR-9921", "Elena Rodriguez", "Grade 11")
        )

        rvStudentSelector.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        rvStudentSelector.adapter = StudentSelectorAdapter(children) { selectedChild ->
            loadStudentAttendance(selectedChild.name, "ID: ${selectedChild.id} | ${selectedChild.grade}")
        }
    }

    private fun loadStudentAttendance(name: String, details: String) {
        tvStudentName.text = name
        tvStudentId.text = details

        rvAttendance.layoutManager = LinearLayoutManager(this)
        
        // Mock data based on student (you can vary it if needed)
        val attendanceList = listOf(
            StudentAttendanceModel("Monday", "Oct 23, 2023", "Picked", "Dropped", "07:30 AM", "02:15 PM"),
            StudentAttendanceModel("Tuesday", "Oct 24, 2023", "Picked", "Dropped", "07:28 AM", "02:20 PM"),
            StudentAttendanceModel("Wednesday", "Oct 25, 2023", "Missed", "Dropped (Private)", "--:--", "02:10 PM"),
            StudentAttendanceModel("Thursday", "Oct 26, 2023", "Picked", "Dropped", "07:35 AM", "02:18 PM"),
            StudentAttendanceModel("Friday", "Oct 27, 2023", "Picked", "In Bus", "07:32 AM", "Pending")
        )

        rvAttendance.adapter = AttendanceAdapter(attendanceList)
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

    class AttendanceAdapter(private val items: List<StudentAttendanceModel>) :
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
            holder.tvDay.text = item.day
            holder.tvDate.text = item.date
            holder.tvPickupStatus.text = item.pickupStatus
            holder.tvPickupTime.text = item.pickupTime
            holder.tvDropStatus.text = item.dropStatus
            holder.tvDropTime.text = item.dropTime

            // Dynamic colors for status
            when (item.pickupStatus) {
                "Picked" -> holder.tvPickupStatus.setTextColor(0xFF4CAF50.toInt())
                "Missed" -> holder.tvPickupStatus.setTextColor(0xFFF44336.toInt())
                else -> holder.tvPickupStatus.setTextColor(0xFFFF9800.toInt())
            }

            when (item.dropStatus) {
                "Dropped" -> holder.tvDropStatus.setTextColor(0xFF4CAF50.toInt())
                "In Bus" -> holder.tvDropStatus.setTextColor(0xFF2196F3.toInt())
                else -> holder.tvDropStatus.setTextColor(0xFFFF9800.toInt())
            }
        }

        override fun getItemCount() = items.size
    }
}
