package ui.admin

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.example.bustrack_app.R
import com.example.bustrack_app.models.AttendanceRecordModel
import com.example.bustrack_app.viewmodels.AttendanceViewModel
import java.util.*

class AttendanceActivity : AppCompatActivity() {

    private lateinit var viewModel: AttendanceViewModel
    private lateinit var tableLayout: TableLayout

    private var selectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        tableLayout = findViewById(R.id.attendanceTable)

        // ⭐ VIEWMODEL
        viewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]

        viewModel.records.observe(this) { records ->
            populateTable(records)
        }

        // ⭐ SPINNER
        val spinnerFleet = findViewById<Spinner>(R.id.spinnerFleet)

        val fleetOptions = arrayOf(
            "All Route",
            "Route 1",
            "Route 2",
            "Route 3"
        )

        val adapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            fleetOptions
        )

        adapter.setDropDownViewResource(R.layout.spinner_dropdown_item)
        spinnerFleet.adapter = adapter

        spinnerFleet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: android.view.View?,
                position: Int,
                id: Long
            ) {
                val selectedRoute = fleetOptions[position]
                viewModel.setFilters(selectedRoute, selectedDate)
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // ⭐ DATE PICKER
        val txtReportDate = findViewById<TextView>(R.id.txtReportDate)

        txtReportDate.setOnClickListener {

            val c = Calendar.getInstance()
            DatePickerDialog(
                this,
                { _, year, month, day ->

                    selectedDate = String.format(
                        "%02d/%02d/%d",
                        day,
                        month + 1,
                        year
                    )

                    txtReportDate.text = selectedDate

                    val selectedRoute = spinnerFleet.selectedItem.toString()
                    viewModel.setFilters(selectedRoute, selectedDate)

                },
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH),
                c.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        utils.NavigationUtils.setupBottomNavigation(this)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener {
            finish()
        }
    }

    // ⭐ TABLE RENDER
    private fun populateTable(records: List<AttendanceRecordModel>) {

        if (tableLayout.childCount > 1) {
            tableLayout.removeViews(1, tableLayout.childCount - 1)
        }

        for (record in records) {

            val row = LayoutInflater.from(this)
                .inflate(R.layout.item_attendance_row, null) as TableRow

            // Avatar
            val avatarTxt = row.findViewById<TextView>(R.id.txtAvatar)
            val initials = record.studentName.split(" ")
                .filter { it.isNotBlank() }
                .map { it[0] }
                .joinToString("")
                .uppercase()

            avatarTxt?.text = initials

            // Data
            row.findViewById<TextView>(R.id.txtStudentName)?.text = record.studentName
            row.findViewById<TextView>(R.id.txtRollNo)?.text = record.rollNo
            row.findViewById<TextView>(R.id.txtBusStop)?.text = record.busStop
            row.findViewById<TextView>(R.id.txtRoute)?.text = record.route
            row.findViewById<TextView>(R.id.txtArrivalTime)?.text = record.arrivalTime

            // Status color
            val statusView = row.findViewById<TextView>(R.id.txtStatus)
            statusView?.text = record.status

            when (record.status.uppercase()) {
                "LATE" -> statusView?.setTextColor(Color.RED)
                "PRESENT" -> statusView?.setTextColor(Color.parseColor("#2E7D32"))
                "ABSENT" -> statusView?.setTextColor(Color.parseColor("#BA1A1A"))
                else -> statusView?.setTextColor(Color.GRAY)
            }

            tableLayout.addView(row)
        }
    }
}