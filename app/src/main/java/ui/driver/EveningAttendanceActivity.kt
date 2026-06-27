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
import java.util.ArrayList

class EveningAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEveningAttendanceBinding
    private lateinit var adapter: AttendanceAdapter
    private var isMorning = true
    private val attendanceList = ArrayList<AttendanceRecordModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEveningAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
        loadMockData()
    }

    private fun setupUI() {
        binding.rvAttendance.layoutManager = LinearLayoutManager(this)
        adapter = AttendanceAdapter(ArrayList())
        binding.rvAttendance.adapter = adapter
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            val intent = Intent(this, DriverDashboardActivity::class.java)
            intent.putExtra("OPEN_DRAWER", true)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
            finish()
        }

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                isMorning = checkedId == R.id.btnMorning
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
    }

    private fun loadMockData() {
        attendanceList.clear()
        // statuses changed from time strings to "Present" or "Absent"
        attendanceList.add(AttendanceRecordModel("FC-9901", "Ali Khan", "Route 1", "Green Valley", "Present", "Present", "Pending", "Pending", "24/05/2026"))
        attendanceList.add(AttendanceRecordModel("FC-9021", "Arjun Jayaram", "Route 1", "Oak Ridge Estates", "Present", "Present", "Pending", "Pending", "24/05/2026"))
        attendanceList.add(AttendanceRecordModel("FC-9104", "Sarah Mitchell", "Route 2", "Silver Springs", "Absent", "Absent", "Pending", "Pending", "24/05/2026"))
        attendanceList.add(AttendanceRecordModel("FC-7721", "Emma Watson", "Route 3", "West Side", "Present", "Present", "Pending", "Pending", "24/05/2026"))
        
        adapter.updateData(attendanceList)
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
                updateAttendance(item, "Present")
            }

            holder.btnAbsent.setOnClickListener {
                updateAttendance(item, "Absent")
            }

            holder.btnEdit.setOnClickListener {
                updateAttendance(item, "Pending")
            }
        }

        private fun updateAttendance(item: AttendanceRecordModel, newStatus: String) {
            val index = attendanceList.indexOf(item)
            if (index != -1) {
                val updatedItem = if (isMorning) {
                    item.copy(morningPickup = newStatus, morningDrop = if(newStatus == "Absent") "Absent" else item.morningDrop)
                } else {
                    item.copy(eveningPickup = newStatus, eveningDrop = if(newStatus == "Absent") "Absent" else item.eveningDrop)
                }
                attendanceList[index] = updatedItem
                updateData(attendanceList)
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