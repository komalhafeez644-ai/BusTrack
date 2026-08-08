package ui.driver

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ActivityEveningAttendanceBinding
import com.example.bustrack_app.models.AttendanceRecordModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import utils.ViewUtils
import java.util.ArrayList

class EveningAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEveningAttendanceBinding
    private lateinit var adapter: AttendanceAdapter
    private var isMorning = true
    private var routeName = ""
    private val attendanceList = ArrayList<AttendanceRecordModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEveningAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        routeName = intent.getStringExtra("ROUTE_NAME") ?: ""

        setupUI()
        setupListeners()
        loadData()
    }

    private fun setupUI() {
        binding.rvAttendance.layoutManager = LinearLayoutManager(this)
        adapter = AttendanceAdapter(ArrayList())
        binding.rvAttendance.adapter = adapter
        
        // Initial button text
        binding.btnSaveAttendance.text = if (isMorning) "SAVE UPDATE" else "SAVE ATTENDANCE"
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }

        binding.btnCalendar.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            Toast.makeText(this, "Calendar selection coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isMorning = checkedId == R.id.btnMorning
                binding.btnSaveAttendance.text = if (isMorning) "SAVE UPDATE" else "SAVE ATTENDANCE"
                adapter.updateData(attendanceList)
            }
        }

        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        binding.btnSaveAttendance.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            it.postDelayed({
                Toast.makeText(this, "Daily attendance records synced successfully!", Toast.LENGTH_LONG).show()
                // The individual records are already being saved on selection, 
                // but this button serves as a final confirmation/sync trigger.
                finish()
            }, 200)
        }
    }

    private fun loadData() {
        if (routeName.isEmpty()) {
            Toast.makeText(this, "Error: No route assigned", Toast.LENGTH_SHORT).show()
            return
        }

        val todayDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())

        // Step 1: Fetch Students for this route
        com.example.bustrack_app.data.FirebaseRepository.fetchStudentsByRoute(routeName) { students ->
            if (isFinishing || isDestroyed) return@fetchStudentsByRoute

            // Step 2: Fetch existing attendance for today
            com.example.bustrack_app.data.FirebaseRepository.fetchAttendance { allAttendance ->
                if (isFinishing || isDestroyed) return@fetchAttendance

                attendanceList.clear()
                
                students.forEach { student ->
                    // Find if student has record for today
                    val existing = allAttendance.find { it.studentId == student.id && it.date == todayDate }
                    
                    if (existing != null) {
                        attendanceList.add(existing)
                    } else {
                        // Create a "Pending" record for the UI
                        attendanceList.add(AttendanceRecordModel(
                            studentId = student.id,
                            studentName = student.name,
                            route = routeName,
                            stop = student.stopName ?: "Unknown",
                            morningPickup = "Pending",
                            morningDrop = "--",
                            eveningPickup = "Pending",
                            eveningDrop = "--",
                            date = todayDate
                        ))
                    }
                }
                
                sortAndDisplay()
            }
        }
    }

    private fun sortAndDisplay() {
        if (!::adapter.isInitialized) return
        
        // Sort by Route and then by Stop Order defined in RouteRepository
        val routes = com.example.bustrack_app.data.RouteRepository.routeList.value ?: emptyList()
        
        val sortedList = attendanceList.sortedWith(compareBy({ it.route }, { record ->
            val routeData = routes.find { it.routeName.equals(record.route, true) }
            // Find the index of the stop in the route's stop list
            val index = routeData?.stopsList?.indexOfFirst { it.stopName.equals(record.stop, true) } ?: -1
            if (index == -1) 999 else index
        }))
        
        adapter.updateData(sortedList)
    }

    inner class AttendanceAdapter(private var fullList: List<AttendanceRecordModel>) :
        RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

        private var filteredList = fullList

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val card: MaterialCardView = view.findViewById(R.id.studentCard)
            val tvName: TextView = view.findViewById(R.id.tvName)
            val tvId: TextView = view.findViewById(R.id.tvId)
            
            val layoutMark: LinearLayout = view.findViewById(R.id.layoutMarkAttendance)
            val btnPresent: MaterialButton = view.findViewById(R.id.btnMarkPresent)
            val btnAbsent: MaterialButton = view.findViewById(R.id.btnMarkAbsent)
            
            val layoutStatus: LinearLayout = view.findViewById(R.id.layoutStatus)
            val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
            val btnEdit: ImageView = view.findViewById(R.id.btnEditStatus)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_evening_attendance_student, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = filteredList[position]
            holder.tvName.text = item.studentName
            holder.tvId.text = "ID: ${item.studentId} • ${item.stop}"
            
            val rawStatus = if (isMorning) item.morningPickup else item.eveningPickup
            
            // Normalize status for UI
            val displayStatus = when {
                rawStatus.equals("Pending", true) -> "Pending"
                rawStatus.equals("Absent", true) -> "Absent"
                else -> "Present" // Any other value (like a time) is treated as Present
            }
            
            if (displayStatus == "Pending") {
                holder.layoutMark.visibility = View.VISIBLE
                holder.layoutStatus.visibility = View.GONE
                holder.card.setCardBackgroundColor(Color.WHITE)
                holder.card.strokeWidth = 2
                holder.card.strokeColor = Color.parseColor("#F1F5F9")
            } else {
                holder.layoutMark.visibility = View.GONE
                holder.layoutStatus.visibility = View.VISIBLE
                updateStatusUI(holder, displayStatus)
            }

            holder.btnPresent.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateAttendance(item, "Present")
            }

            holder.btnAbsent.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateAttendance(item, "Absent")
            }

            holder.btnEdit.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateAttendance(item, "Pending")
            }
        }

        private fun updateAttendance(item: AttendanceRecordModel, newStatus: String) {
            val updatedItem = if (isMorning) {
                item.copy(morningPickup = newStatus, morningDrop = if(newStatus == "Absent") "Absent" else item.morningDrop)
            } else {
                item.copy(eveningPickup = newStatus, eveningDrop = if(newStatus == "Absent") "Absent" else item.eveningDrop)
            }
            
            com.example.bustrack_app.data.FirebaseRepository.saveAttendance(updatedItem) { success ->
                if (success) {
                    // Update local list to reflect changes immediately
                    val index = attendanceList.indexOfFirst { it.studentId == item.studentId && it.date == item.date }
                    if (index != -1) {
                        attendanceList[index] = updatedItem
                        adapter.updateData(attendanceList)
                    }
                    Toast.makeText(this@EveningAttendanceActivity, "Attendance updated", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@EveningAttendanceActivity, "Failed to sync", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun updateStatusUI(holder: ViewHolder, status: String) {
            holder.tvStatusBadge.text = status.uppercase()
            if (status.equals("Present", true)) {
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
                holder.card.setCardBackgroundColor(Color.parseColor("#F0FDF4")) // Very light green
                holder.card.strokeWidth = 2
                holder.card.strokeColor = Color.parseColor("#BBF7D0")
            } else if (status.equals("Absent", true)) {
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"))
                holder.card.setCardBackgroundColor(Color.parseColor("#FEF2F2")) // Very light red
                holder.card.strokeWidth = 2
                holder.card.strokeColor = Color.parseColor("#FECACA")
            } else {
                // Default fallback to avoid reuse issues
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#64748B"))
                holder.card.setCardBackgroundColor(Color.WHITE)
                holder.card.strokeWidth = 2
                holder.card.strokeColor = Color.parseColor("#F1F5F9")
            }
        }

        override fun getItemCount() = filteredList.size

        fun updateData(newList: List<AttendanceRecordModel>) {
            fullList = newList
            filteredList = newList
            notifyDataSetChanged()
        }

        fun filter(query: String) {
            filteredList = if (query.isEmpty()) {
                fullList
            } else {
                fullList.filter { it.studentName.contains(query, true) || it.studentId.contains(query, true) }
            }
            notifyDataSetChanged()
        }
    }
}