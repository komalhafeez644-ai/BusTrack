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

    private var stopName: String = ""
    private var routeName: String = ""
    private var isMorning: Boolean = true

    private var students: List<StudentModel> = emptyList()
    private lateinit var adapter: AttendanceStudentAdapter
    private var existingRecordsByStudent: Map<String, AttendanceRecordModel> = emptyMap()

    companion object {
        fun newInstance(stopName: String, routeName: String, isMorning: Boolean = true): AttendanceBottomSheet {
            val fragment = AttendanceBottomSheet()
            val args = Bundle()
            args.putString("STOP_NAME", stopName)
            args.putString("ROUTE_NAME", routeName)
            args.putBoolean("IS_MORNING", isMorning)
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

        if (routeName.isEmpty()) {
            recyclerView.visibility = View.GONE
            tvNoStudents.visibility = View.VISIBLE
            return
        }

        FirebaseRepository.fetchStudentsByStop(routeName, stopName) { fetchedStudents ->
            if (!isAdded) return@fetchStudentsByStop

            students = fetchedStudents

            if (students.isEmpty()) {
                recyclerView.visibility = View.GONE
                tvNoStudents.visibility = View.VISIBLE
                return@fetchStudentsByStop
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
    }

    private fun saveAllAttendance() {
        if (!::adapter.isInitialized) {
            Toast.makeText(context, "No students to save yet", Toast.LENGTH_SHORT).show()
            return
        }

        val date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val marked = adapter.getMarkedAttendance().filterValues { it != "Pending" }

        if (marked.isEmpty()) {
            Toast.makeText(context, "No attendance marked to save", Toast.LENGTH_SHORT).show()
            return
        }

        val recordsToSave = students.filter { marked.containsKey(it.id) }.map { student ->
            val status = marked.getValue(student.id)
            val currentTime = if (status == "Present") SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date()) else status

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
                date = date
            )
        }

        var savedCount = 0
        var failed = false
        recordsToSave.forEach { record ->
            FirebaseRepository.saveAttendance(record) { success ->
                if (!isAdded) return@saveAttendance
                if (success) {
                    savedCount++
                    val status = marked.getValue(record.studentId)
                    FirebaseRepository.notifyParentsOfAttendance(record.studentId, record.studentName, status, isMorning)
                    if (savedCount == recordsToSave.size) {
                        val period = if (isMorning) "Morning" else "Evening"
                        Toast.makeText(context, "$period attendance saved for $savedCount student(s)", Toast.LENGTH_LONG).show()
                        dismiss()
                    }
                } else if (!failed) {
                    failed = true
                    Toast.makeText(context, "Failed to save attendance for ${record.studentName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}