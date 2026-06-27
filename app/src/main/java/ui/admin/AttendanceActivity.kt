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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_attendance)

        supportActionBar?.hide()

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
        adapter = AttendanceAdapter(emptyList())
        rvAttendance.adapter = adapter
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
    }

    private fun updateSummary(records: List<AttendanceRecordModel>) {
        val total = records.size
        val absent = records.count { it.morningPickup.equals("Absent", ignoreCase = true) }
        val present = total - absent

        tvTotalCount.text = String.format("%02d", total)
        tvPresentCount.text = String.format("%02d", present)
        tvAbsentCount.text = String.format("%02d", absent)
    }

    private fun setupFilters() {
        // Fleet Spinner
        val fleetOptions = arrayOf("All Route", "Route 1", "Route 2", "Route 3")
        
        // Custom Spinner Adapter for better visibility
        val spinnerAdapter = object : ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, fleetOptions) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = super.getView(position, convertView, parent) as TextView
                v.setTextColor(Color.parseColor("#051024")) // primaryDark
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
                viewModel.setFilters(fleetOptions[position], selectedDate)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Date Picker
        txtReportDate.setOnClickListener {
            val c = Calendar.getInstance()
            DatePickerDialog(this, { _, year, month, day ->
                selectedDate = String.format("%02d/%02d/%d", day, month + 1, year)
                txtReportDate.text = selectedDate
                viewModel.setFilters(spinnerFleet.selectedItem.toString(), selectedDate)
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
        NavigationUtils.setupBottomNavigation(this)
    }

    // --- Adapter ---
    class AttendanceAdapter(private var fullList: List<AttendanceRecordModel>) :
        RecyclerView.Adapter<AttendanceAdapter.ViewHolder>() {

        private var filteredList = fullList.toList()

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
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
