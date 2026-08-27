package ui.admin

import android.app.DatePickerDialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.AttendanceRecordModel
import com.example.bustrack_app.viewmodels.AttendanceViewModel
import utils.NavigationUtils
import java.util.*

class AttendanceActivity : AppCompatActivity() {

    private lateinit var viewModel: AttendanceViewModel
    private lateinit var rvAttendance: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var txtReportDate: TextView
    private lateinit var spinnerFleet: Spinner
    
    private lateinit var tvTotalCount: TextView
    private lateinit var tvPresentCount: TextView
    private lateinit var tvAbsentCount: TextView
    
    private var selectedDate: String = "Select Date"
    private lateinit var adapter: AttendanceAdapter
    // Task 3: Principal reuses this exact screen but must not be able to edit records -
    // AttendanceActivity is launched with VIEW_ONLY=true from PrincipalDashboardActivity.
    private var isViewOnly: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        supportActionBar?.hide()

        isViewOnly = intent.getBooleanExtra("VIEW_ONLY", false)

        initViews()
        setupViewModel()
        setupFilters()
        setupSearch()

        findViewById<ImageView>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    private fun initViews() {
        rvAttendance = findViewById(R.id.rvAttendance)
        etSearch = findViewById(R.id.etSearch)
        txtReportDate = findViewById(R.id.txtReportDate)
        spinnerFleet = findViewById(R.id.spinnerFleet)
        
        tvTotalCount = findViewById(R.id.tvTotalCount)
        tvPresentCount = findViewById(R.id.tvPresentCount)
        tvAbsentCount = findViewById(R.id.tvAbsentCount)

        rvAttendance.layoutManager = LinearLayoutManager(this)
        adapter = AttendanceAdapter(emptyList()) { record ->
            if (isViewOnly) {
                Toast.makeText(this, "View only - Principal cannot edit attendance", Toast.LENGTH_SHORT).show()
            } else {
                showEditAttendanceDialog(record)
            }
        }
        rvAttendance.adapter = adapter
    }

    private fun showEditAttendanceDialog(record: AttendanceRecordModel) {
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_edit_attendance)
        dialog.window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(Color.TRANSPARENT))
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        val tvName = dialog.findViewById<TextView>(R.id.tvDialogStudentName)
        val spinnerMorningPickup = dialog.findViewById<Spinner>(R.id.spinnerMorningPickup)
        val spinnerMorningDrop = dialog.findViewById<Spinner>(R.id.spinnerMorningDrop)
        val spinnerEveningPickup = dialog.findViewById<Spinner>(R.id.spinnerEveningPickup)
        val spinnerEveningDrop = dialog.findViewById<Spinner>(R.id.spinnerEveningDrop)
        val btnSave = dialog.findViewById<Button>(R.id.btnSaveAttendance)

        tvName.text = "${record.studentName} (${record.studentId})"

        val statusOptions = arrayOf("Pending", "Present", "Absent", "Skipped", "--")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, statusOptions)
        
        spinnerMorningPickup.adapter = statusAdapter
        spinnerMorningDrop.adapter = statusAdapter
        spinnerEveningPickup.adapter = statusAdapter
        spinnerEveningDrop.adapter = statusAdapter

        // Set current values
        spinnerMorningPickup.setSelection(statusOptions.indexOf(record.morningPickup).let { if (it == -1) 0 else it })
        spinnerMorningDrop.setSelection(statusOptions.indexOf(record.morningDrop).let { if (it == -1) 0 else it })
        spinnerEveningPickup.setSelection(statusOptions.indexOf(record.eveningPickup).let { if (it == -1) 0 else it })
        spinnerEveningDrop.setSelection(statusOptions.indexOf(record.eveningDrop).let { if (it == -1) 0 else it })

        btnSave.setOnClickListener {
            val updatedRecord = record.copy(
                morningPickup = spinnerMorningPickup.selectedItem.toString(),
                morningDrop = spinnerMorningDrop.selectedItem.toString(),
                eveningPickup = spinnerEveningPickup.selectedItem.toString(),
                eveningDrop = spinnerEveningDrop.selectedItem.toString()
            )
            viewModel.saveRecord(updatedRecord)
            dialog.dismiss()
            Toast.makeText(this, "Attendance updated successfully", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]

        viewModel.records.observe(this) { records ->
            adapter.updateList(records)
            updateSummary(records)
        }

        viewModel.selectedDateText.observe(this) { dateText ->
            txtReportDate.text = dateText
            selectedDate = dateText
        }

        viewModel.availableRoutes.observe(this) { routes ->
            setupFleetSpinner(routes)
        }
    }

    private fun setupFleetSpinner(routes: List<String>) {
        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, routes) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Color.parseColor("#051024"))
                v.textSize = 14f
                return v
            }

            override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getDropDownView(position, convertView, parent) as TextView
                v.setTextColor(Color.parseColor("#051024"))
                v.setBackgroundColor(Color.WHITE)
                v.setPadding(32, 32, 32, 32)
                return v
            }
        }

        spinnerFleet.adapter = spinnerAdapter

        spinnerFleet.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                viewModel.setFilters(routes[position], selectedDate)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun updateSummary(records: List<AttendanceRecordModel>) {
        val total = records.size
        // A student is present if they were picked up in the morning OR evening
        val present = records.count { 
            it.morningPickup.equals("Present", ignoreCase = true) || 
            it.eveningPickup.equals("Present", ignoreCase = true) ||
            it.morningPickup.contains(":", ignoreCase = true) || // Time format
            it.eveningPickup.contains(":", ignoreCase = true)
        }
        val absent = total - present

        tvTotalCount.text = String.format("%02d", total)
        tvPresentCount.text = String.format("%02d", present)
        tvAbsentCount.text = String.format("%02d", absent)
    }

    private fun setupFilters() {
        // Fleet Spinner setup is now handled in setupViewModel via LiveData observer
        
        // Date Picker
        txtReportDate.setOnClickListener {
            val c = Calendar.getInstance()
            // If selectedDate is valid, use it for the picker
            val currentParts = selectedDate.split("/")
            if (currentParts.size == 3) {
                c.set(Calendar.DAY_OF_MONTH, currentParts[0].toInt())
                c.set(Calendar.MONTH, currentParts[1].toInt() - 1)
                c.set(Calendar.YEAR, currentParts[2].toInt())
            }

            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = String.format("%02d/%02d/%d", day, month + 1, year)
                txtReportDate.text = selectedDate
                viewModel.setFilters(spinnerFleet.selectedItem?.toString() ?: "All Route", selectedDate)
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show()
        }
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()
                val records = viewModel.records.value ?: emptyList()
                val filtered = if (query.isEmpty()) {
                    records
                } else {
                    records.filter {
                        it.studentName.contains(query, true) || it.studentId.contains(query, true)
                    }
                }
                adapter.updateList(filtered, isFiltering = true)
                updateSummary(filtered)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    override fun onResume() {
        super.onResume()
        // Task 3: when Principal reuses this screen (VIEW_ONLY), the Admin bottom nav bar
        // must NOT be active - its Dashboard/Requests/Alerts tabs lead into Admin-only
        // management screens (student/bus/driver edit, tracking approvals) that Principal
        // should not be able to reach. Hide it entirely instead of wiring it up.
        if (isViewOnly) {
            findViewById<View>(R.id.bottomNavInclude)?.visibility = View.GONE
        } else {
            NavigationUtils.setupBottomNavigation(this)
        }
    }

    // --- Adapter ---
    class AttendanceAdapter(
        private var fullList: List<AttendanceRecordModel>,
        private val onItemClick: (AttendanceRecordModel) -> Unit
    ) : RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

        private var filteredList = fullList.toList()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val container: View = view
            val tvStudentId: TextView = view.findViewById(R.id.tvStudentId)
            val tvName: TextView = view.findViewById(R.id.tvStudentName)
            val tvRoute: TextView = view.findViewById(R.id.tvRoute)
            val tvStop: TextView = view.findViewById(R.id.tvStop)
            val tvMorningPickup: TextView = view.findViewById(R.id.tvMorningPickup)
            val tvMorningDrop: TextView = view.findViewById(R.id.tvMorningDrop)
            val tvEveningPickup: TextView = view.findViewById(R.id.tvEveningPickup)
            val tvEveningDrop: TextView = view.findViewById(R.id.tvEveningDrop)
            val tvDate: TextView = view.findViewById(R.id.tvDate)
            val txtAvatar: TextView = view.findViewById(R.id.txtAvatar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_attendance, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = filteredList[position]
            holder.tvStudentId.text = item.studentId
            holder.tvName.text = item.studentName
            holder.tvRoute.text = item.route
            holder.tvStop.text = item.stop
            
            holder.tvMorningPickup.text = item.morningPickup
            holder.tvMorningDrop.text = item.morningDrop
            holder.tvEveningPickup.text = item.eveningPickup
            holder.tvEveningDrop.text = item.eveningDrop
            holder.tvDate.text = item.date

            // Avatar initial
            holder.txtAvatar.text = if (item.studentName.isNotEmpty()) item.studentName[0].toString() else ""

            // Highlight "Absent" in Red
            setAbsentStyle(holder.tvMorningPickup)
            setAbsentStyle(holder.tvMorningDrop)
            setAbsentStyle(holder.tvEveningPickup)
            setAbsentStyle(holder.tvEveningDrop)

            holder.itemView.setOnClickListener {
                onItemClick(item)
            }
        }

        private fun setAbsentStyle(textView: TextView) {
            if (textView.text.toString().contains("Absent", ignoreCase = true)) {
                textView.setTextColor(Color.RED)
            } else {
                textView.setTextColor(Color.parseColor("#49454F"))
            }
        }

        override fun getItemCount() = filteredList.size

        fun updateList(newList: List<AttendanceRecordModel>, isFiltering: Boolean = false) {
            if (!isFiltering) {
                fullList = newList
            }
            filteredList = newList
            notifyDataSetChanged()
        }
    }
}
