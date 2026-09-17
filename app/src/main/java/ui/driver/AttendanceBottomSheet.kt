package ui.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.adapter.AttendanceStudentAdapter
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.AttendanceRecordModel
import com.example.bustrack_app.models.StudentModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.bustrack_app.R
import utils.ViewUtils

/**
 * Attendance Bottom Sheet shown to the driver when the geofence/arrival logic in
 * DriverDashboardActivity detects the bus has reached a stop.
 */
class AttendanceBottomSheet : BottomSheetDialogFragment() {

    private var stopId: String = ""
    private var stopName: String = ""
    private var routeId: String = ""
    private var routeName: String = ""
    private var tripId: String = ""
    private var tripDirection: String = "FORWARD"
    private var busId: String = ""
    private var driverId: String = ""
    private var driverName: String = ""
    private var isMorning: Boolean = true

    private var students: List<StudentModel> = emptyList()
    private lateinit var adapter: AttendanceStudentAdapter
    private var existingRecordsByStudent: Map<String, AttendanceRecordModel> = emptyMap()
    private var isSavingInProgress = false

    companion object {
        fun newInstance(
            stopName: String,
            routeName: String,
            isMorning: Boolean = true,
            stopId: String = "",
            routeId: String = "",
            tripId: String = "",
            tripDirection: String = "FORWARD",
            busId: String = "",
            driverId: String = "",
            driverName: String = ""
        ): AttendanceBottomSheet {
            val fragment = AttendanceBottomSheet()
            val args = Bundle().apply {
                putString("STOP_NAME", stopName)
                putString("ROUTE_NAME", routeName)
                putBoolean("IS_MORNING", isMorning)
                putString("STOP_ID", stopId)
                putString("ROUTE_ID", routeId)
                putString("TRIP_ID", tripId)
                putString("TRIP_DIRECTION", tripDirection)
                putString("BUS_ID", busId)
                putString("DRIVER_ID", driverId)
                putString("DRIVER_NAME", driverName)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.bottomsheet_attendance, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        stopName = arguments?.getString("STOP_NAME") ?: ""
        routeName = arguments?.getString("ROUTE_NAME") ?: ""
        isMorning = arguments?.getBoolean("IS_MORNING", true) ?: true
        stopId = arguments?.getString("STOP_ID") ?: stopName
        routeId = arguments?.getString("ROUTE_ID") ?: ""
        tripId = arguments?.getString("TRIP_ID") ?: ""
        tripDirection = arguments?.getString("TRIP_DIRECTION") ?: "FORWARD"
        busId = arguments?.getString("BUS_ID") ?: ""
        driverId = arguments?.getString("DRIVER_ID") ?: ""
        driverName = arguments?.getString("DRIVER_NAME") ?: ""

        view.findViewById<TextView>(R.id.tvCurrentStopName).text = stopName
        view.findViewById<TextView>(R.id.tvAttendancePeriod).text =
            if (isMorning) "Morning Attendance" else "Evening Attendance"

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvAttendanceStudents)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        loadStudentsAtStop(view)

        view.findViewById<View>(R.id.btnSkip).setOnClickListener {
            ViewUtils.applyClickEffect(it)
            Toast.makeText(context, "Attendance skipped for $stopName", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        view.findViewById<View>(R.id.btnSaveAttendance).setOnClickListener {
            ViewUtils.applyClickEffect(it)
            saveAllAttendance()
        }
    }

    private fun loadStudentsAtStop(root: View) {
        val recyclerView = root.findViewById<RecyclerView>(R.id.rvAttendanceStudents)
        val tvNoStudents = root.findViewById<TextView>(R.id.tvNoStudents)

        if (routeName.isEmpty() || stopName.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoStudents.visibility = View.VISIBLE
            return
        }

        val onStudentsLoaded: (List<StudentModel>) -> Unit = studentsLoaded@{ fetchedStudents ->
            if (!isAdded) return@studentsLoaded

            students = fetchedStudents

            if (students.isEmpty()) {
                recyclerView.visibility = View.GONE
                tvNoStudents.visibility = View.VISIBLE
                return@studentsLoaded
            }

            recyclerView.visibility = View.VISIBLE
            tvNoStudents.visibility = View.GONE

            val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
            val studentIds = students.map { it.id }

            FirebaseRepository.fetchAttendanceForStudents(studentIds, today) { existingRecords ->
                if (!isAdded) return@fetchAttendanceForStudents

                existingRecordsByStudent = existingRecords

                val initialStatuses = existingRecords.mapValues { (_, record) ->
                    val value = if (isMorning) {
                        record.morningPickup
                    } else {
                        record.eveningPickup
                    }
                    if (value.isBlank() || value == "--" || value.equals("Pending", true) || value.equals("Pending Drop", true)) "Pending" else value
                }

                adapter = AttendanceStudentAdapter(students, initialStatuses) { _, _ -> }
                recyclerView.adapter = adapter
            }
        }

        FirebaseRepository.fetchStudentsByStop(routeName, stopName, onStudentsLoaded)
    }

    private fun saveAllAttendance() {
        if (isSavingInProgress) return

        if (!::adapter.isInitialized) {
            Toast.makeText(context, "No students to save yet", Toast.LENGTH_SHORT).show()
            return
        }

        if (routeName.isBlank() || stopName.isBlank()) {
            Toast.makeText(context, "Unable to mark attendance. No active trip or stop found.", Toast.LENGTH_SHORT).show()
            return
        }

        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val marked = adapter.getMarkedAttendance().filterValues { it != "Pending" }

        if (marked.isEmpty()) {
            Toast.makeText(context, "No attendance marked to save", Toast.LENGTH_SHORT).show()
            return
        }

        isSavingInProgress = true
        view?.findViewById<View>(R.id.btnSaveAttendance)?.isEnabled = false

        val markTimestamp = System.currentTimeMillis()
        val recordsToSave = students.filter { marked.containsKey(it.id) }.map { student ->
            val status = marked.getValue(student.id)
            val currentTime = if (status == "Present") SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(markTimestamp)) else status
            val attendanceType = if (isMorning) "MORNING_PICKUP" else "EVENING_DROP"
            val attendanceStatus = when (status) {
                "Present" -> if (isMorning) "PICKED_UP" else "DROPPED_OFF"
                "Absent" -> "ABSENT"
                "Leave" -> "LEAVE"
                else -> status.uppercase(Locale.getDefault())
            }

            val existing = existingRecordsByStudent[student.id]
            AttendanceRecordModel(
                studentId = student.id,
                studentName = student.name,
                route = routeName,
                stop = stopName,
                morningPickup = if (isMorning) currentTime else (existing?.morningPickup ?: "--"),
                morningDrop = if (isMorning) (if (status == "Absent" || status == "Leave") status else "Pending Drop") else (existing?.morningDrop ?: "--"),
                eveningPickup = if (!isMorning) currentTime else (existing?.eveningPickup ?: "--"),
                eveningDrop = if (!isMorning) (if (status == "Absent" || status == "Leave") status else "Pending Drop") else (existing?.eveningDrop ?: "--"),
                date = date,
                busId = busId,
                routeId = routeId,
                stopId = stopId,
                stopName = stopName,
                tripId = tripId,
                tripDirection = tripDirection,
                attendanceType = attendanceType,
                attendanceStatus = attendanceStatus,
                timestamp = markTimestamp,
                markedByDriverId = driverId,
                markedByDriverName = driverName,
                syncStatus = if (com.example.bustrack_app.sync.network.NetworkMonitor.isOnline) "SYNCED" else "PENDING"
            )
        }

        var completedCount = 0
        var queuedOfflineCount = 0
        var syncedOnlineCount = 0
        var failedCount = 0

        recordsToSave.forEach { record ->
            FirebaseRepository.saveAttendanceWithResult(record) { result ->
                if (!isAdded) return@saveAttendanceWithResult
                completedCount++

                when (result) {
                    com.example.bustrack_app.sync.SyncQueueManager.SyncResult.SYNCED_ONLINE -> {
                        syncedOnlineCount++
                    }
                    com.example.bustrack_app.sync.SyncQueueManager.SyncResult.QUEUED_OFFLINE -> {
                        queuedOfflineCount++
                    }
                    com.example.bustrack_app.sync.SyncQueueManager.SyncResult.FAILED -> {
                        failedCount++
                    }
                }

                if (result != com.example.bustrack_app.sync.SyncQueueManager.SyncResult.FAILED) {
                    val status = marked.getValue(record.studentId)
                    FirebaseRepository.notifyParentsOfAttendance(
                        record.studentId,
                        record.studentName,
                        status,
                        record.date,
                        isMorning
                    )
                }

                if (completedCount == recordsToSave.size) {
                    isSavingInProgress = false
                    view?.findViewById<View>(R.id.btnSaveAttendance)?.isEnabled = true

                    if (failedCount > 0 && syncedOnlineCount == 0 && queuedOfflineCount == 0) {
                        Toast.makeText(context, "Attendance could not be saved. Please try again.", Toast.LENGTH_SHORT).show()
                    } else {
                        if (queuedOfflineCount > 0) {
                            Toast.makeText(context, "Attendance marked successfully. Waiting for internet sync.", Toast.LENGTH_LONG).show()
                        } else {
                            val period = if (isMorning) "Morning" else "Evening"
                            Toast.makeText(context, "$period attendance saved for $syncedOnlineCount student(s)", Toast.LENGTH_LONG).show()
                        }

                        // System Notification: Attendance Submitted
                        val effectiveDriverId = driverId.ifEmpty {
                            com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
                        }
                        if (effectiveDriverId.isNotEmpty()) {
                            FirebaseRepository.sendSystemNotification(
                                driverId = effectiveDriverId,
                                title = "Attendance Submitted",
                                message = "Student attendance for stop '$stopName' has been successfully recorded for the current trip.",
                                type = com.example.bustrack_app.models.NotificationModel.TYPE_GENERAL
                            )
                        }

                        dismiss()
                    }
                }
            }
        }
    }
}
