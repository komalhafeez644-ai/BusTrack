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
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityEveningAttendanceBinding
import com.example.bustrack_app.models.AttendanceRecordModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import utils.ViewUtils
import utils.AttendanceStatus
import java.util.ArrayList

class EveningAttendanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEveningAttendanceBinding
    private lateinit var adapter: AttendanceAdapter
    private var isMorning = true
    private var routeName = ""
    private var routeId = ""
    private var busId = ""
    private var driverId = ""
    private var driverName = ""
    private var tripId = ""
    private var tripDirection = "FORWARD"
    private val attendanceList = ArrayList<AttendanceRecordModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEveningAttendanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        routeName = intent.getStringExtra("ROUTE_NAME") ?: ""
        routeId = intent.getStringExtra("ROUTE_ID") ?: ""
        busId = intent.getStringExtra("BUS_ID") ?: ""
        driverId = intent.getStringExtra("DRIVER_ID") ?: ""
        driverName = intent.getStringExtra("DRIVER_NAME") ?: ""
        tripId = intent.getStringExtra("TRIP_ID") ?: ""
        tripDirection = intent.getStringExtra("TRIP_DIRECTION") ?: "FORWARD"
        // The existing toggle remains available; its initial state follows the
        // active navigation session rather than a separate clock rule.
        isMorning = intent.getBooleanExtra("IS_MORNING", true)

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
        binding.toggleGroup.check(if (isMorning) R.id.btnMorning else R.id.btnEvening)
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
                
                // System Notification: Attendance Submitted
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { driverId ->
                    com.example.bustrack_app.data.FirebaseRepository.sendSystemNotification(
                        driverId = driverId,
                        title = "Attendance Submitted",
                        message = "Student attendance for your trip has been successfully submitted. The attendance record has been saved for the current trip and date.",
                        type = com.example.bustrack_app.models.NotificationModel.TYPE_GENERAL
                    )
                }

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
                val normalizedToday = todayDate.replace("/", "-")
                
                students.forEach { student ->
                    // Find if student has record for today (check both slash and hyphen formats)
                    val existing = allAttendance.find { 
                        it.studentId == student.id && (it.date == todayDate || it.date == normalizedToday) 
                    }
                    
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
            val btnLeave: MaterialButton = view.findViewById(R.id.btnMarkLeave)
            
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
            val rollNumber = StudentRepository.studentList.value
                ?.firstOrNull { it.id == item.studentId }
                ?.rollNumber
                .orEmpty()
            holder.tvId.text = "${rollNumber.ifBlank { "N/A" }} • ${item.stop}"
            
            val rawStatus = if (isMorning) {
                item.morningPickup
            } else {
                // For evening, if drop is marked but pickup is still pending/school, show the drop status
                if (item.eveningDrop.contains(":") || item.eveningDrop == "Absent" || AttendanceStatus.isShortLeave(item.eveningDrop)) {
                    item.eveningDrop
                } else {
                    item.eveningPickup
                }
            }
            
            // Normalize status for UI
            val displayStatus = when {
                rawStatus.equals("Pending", true) || rawStatus.equals("--", true) || rawStatus.equals("Pending Drop", true) || rawStatus.isBlank() -> "Pending"
                rawStatus.equals("Absent", true) -> "Absent"
                AttendanceStatus.isShortLeave(rawStatus) -> "Leave"
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
                val currentTime = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
                updateAttendance(item, currentTime)
            }

            holder.btnAbsent.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateAttendance(item, "Absent")
            }

            holder.btnLeave.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateAttendance(item, AttendanceStatus.SHORT_LEAVE)
            }

            holder.btnEdit.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateAttendance(item, "Pending")
            }
        }

        private fun updateAttendance(item: AttendanceRecordModel, newStatus: String) {
            val storedStatus = AttendanceStatus.forStorage(newStatus)
            val markTimestamp = System.currentTimeMillis()
            val currentTime = if (storedStatus == "Present") java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date(markTimestamp)) else storedStatus
            // This screen records the manual pickup at the college.  The later
            // geofence event is the only code path that writes an Evening Drop.
            val attendanceType = if (isMorning) "MORNING_PICKUP" else "EVENING_PICKUP"
            val attendanceStatus = when (storedStatus) {
                "Present" -> "PICKED_UP"
                "Absent" -> "ABSENT"
                AttendanceStatus.SHORT_LEAVE -> "S_LEAVE"
                else -> newStatus.uppercase(java.util.Locale.getDefault())
            }

            val updatedItem = if (isMorning) {
                // Morning: Pickup is marked now, Drop is pending until bus reaches school.
                item.copy(
                    morningPickup = preservedPickup(item.morningPickup, storedStatus, currentTime),
                    morningDrop = if (storedStatus == "Absent" || AttendanceStatus.isShortLeave(storedStatus) || storedStatus == "Pending") "--" else pendingDrop(item.morningDrop),
                    busId = if (item.busId.isNotEmpty()) item.busId else this@EveningAttendanceActivity.busId,
                    routeId = if (item.routeId.isNotEmpty()) item.routeId else this@EveningAttendanceActivity.routeId,
                    stopId = if (item.stopId.isNotEmpty()) item.stopId else item.stop,
                    stopName = if (item.stopName.isNotEmpty()) item.stopName else item.stop,
                    tripId = if (item.tripId.isNotEmpty()) item.tripId else this@EveningAttendanceActivity.tripId,
                    tripDirection = if (item.tripDirection.isNotEmpty()) item.tripDirection else this@EveningAttendanceActivity.tripDirection,
                    attendanceType = attendanceType,
                    attendanceStatus = attendanceStatus,
                    timestamp = markTimestamp,
                    markedByDriverId = if (item.markedByDriverId.isNotEmpty()) item.markedByDriverId else this@EveningAttendanceActivity.driverId,
                    markedByDriverName = if (item.markedByDriverName.isNotEmpty()) item.markedByDriverName else this@EveningAttendanceActivity.driverName,
                    syncStatus = if (com.example.bustrack_app.sync.network.NetworkMonitor.isOnline) "SYNCED" else "PENDING"
                )
            } else {
                // Evening: Pickup is marked now (at school), Drop is pending until bus reaches home stop.
                item.copy(
                    eveningPickup = preservedPickup(item.eveningPickup, storedStatus, currentTime),
                    eveningDrop = if (storedStatus == "Absent" || AttendanceStatus.isShortLeave(storedStatus) || storedStatus == "Pending") "--" else pendingDrop(item.eveningDrop),
                    busId = if (item.busId.isNotEmpty()) item.busId else this@EveningAttendanceActivity.busId,
                    routeId = if (item.routeId.isNotEmpty()) item.routeId else this@EveningAttendanceActivity.routeId,
                    stopId = if (item.stopId.isNotEmpty()) item.stopId else item.stop,
                    stopName = if (item.stopName.isNotEmpty()) item.stopName else item.stop,
                    tripId = if (item.tripId.isNotEmpty()) item.tripId else this@EveningAttendanceActivity.tripId,
                    tripDirection = if (item.tripDirection.isNotEmpty()) item.tripDirection else this@EveningAttendanceActivity.tripDirection,
                    attendanceType = attendanceType,
                    attendanceStatus = attendanceStatus,
                    timestamp = markTimestamp,
                    markedByDriverId = if (item.markedByDriverId.isNotEmpty()) item.markedByDriverId else this@EveningAttendanceActivity.driverId,
                    markedByDriverName = if (item.markedByDriverName.isNotEmpty()) item.markedByDriverName else this@EveningAttendanceActivity.driverName,
                    syncStatus = if (com.example.bustrack_app.sync.network.NetworkMonitor.isOnline) "SYNCED" else "PENDING"
                )
            }

            com.example.bustrack_app.data.FirebaseRepository.saveAttendanceWithResult(updatedItem) { result ->
                if (result != com.example.bustrack_app.sync.SyncQueueManager.SyncResult.FAILED) {
                    // Update local list to reflect changes immediately
                    val index = attendanceList.indexOfFirst { it.studentId == item.studentId && it.date == item.date }
                    if (index != -1) {
                        attendanceList[index] = updatedItem
                        adapter.updateData(attendanceList)
                    }
                    com.example.bustrack_app.data.FirebaseRepository.notifyParentsOfAttendance(
                        item.studentId,
                        item.studentName,
                        storedStatus,
                        item.date,
                        isMorning,
                        busNumber = updatedItem.busId.ifBlank { busId },
                        routeName = updatedItem.route.ifBlank { routeName },
                        stopName = updatedItem.stopName.ifBlank { updatedItem.stop },
                        stopNumber = updatedItem.stopId
                    )

                    if (result == com.example.bustrack_app.sync.SyncQueueManager.SyncResult.QUEUED_OFFLINE) {
                        Toast.makeText(this@EveningAttendanceActivity, "Attendance marked successfully. Waiting for internet sync.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EveningAttendanceActivity, "Attendance could not be saved. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        /** A later edit must not replace the original successful pickup timestamp. */
        private fun preservedPickup(existing: String, newStatus: String, replacement: String): String =
            if (newStatus == "Present" && isRecordedPickup(existing)) existing else replacement

        private fun isRecordedPickup(value: String): Boolean =
            value.contains(":") || value.equals("Present", true) ||
                    value.equals("School", true) || value.equals("En Route", true)

        private fun pendingDrop(existing: String): String =
            if (existing.contains(":")) existing else "Pending Drop"

        private fun updateStatusUI(holder: ViewHolder, status: String) {
            val displayStatus = when {
                status.equals("Pending", true) -> "Pending"
                status.equals("Absent", true) -> "Absent"
                AttendanceStatus.isShortLeave(status) -> "Leave"
                else -> "Present"
            }
            
            holder.tvStatusBadge.text = when {
                displayStatus == "Leave" -> AttendanceStatus.SHORT_LEAVE
                displayStatus == "Present" && !status.equals("Present", true) -> status.uppercase()
                else -> displayStatus.uppercase()
            }
            
            if (displayStatus == "Present") {
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
                holder.card.setCardBackgroundColor(Color.parseColor("#F0FDF4")) // Very light green
                holder.card.strokeWidth = 2
                holder.card.strokeColor = Color.parseColor("#BBF7D0")
            } else if (displayStatus == "Absent") {
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"))
                holder.card.setCardBackgroundColor(Color.parseColor("#FEF2F2")) // Very light red
                holder.card.strokeWidth = 2
                holder.card.strokeColor = Color.parseColor("#FECACA")
            } else if (displayStatus == "Leave") {
                holder.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                holder.tvStatusBadge.setTextColor(Color.parseColor("#D97706"))
                holder.card.setCardBackgroundColor(Color.parseColor("#FFFBEB")) // Very light amber
                holder.card.strokeWidth = 2
                holder.card.strokeColor = Color.parseColor("#FDE68A")
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
