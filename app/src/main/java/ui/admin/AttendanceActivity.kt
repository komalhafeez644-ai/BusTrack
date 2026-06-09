package ui.admin

import android.app.DatePickerDialog
import android.content.res.ColorStateList
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

        rvAttendance.layoutManager = LinearLayoutManager(this)
        adapter = AttendanceAdapter(emptyList())
        rvAttendance.adapter = adapter
    }

    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[AttendanceViewModel::class.java]

        viewModel.records.observe(this) { records ->
            adapter.updateList(records)
        }
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
                adapter.filter(s.toString())
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
            val tvName: TextView = view.findViewById(R.id.tvStudentName)
            val tvRollNo: TextView = view.findViewById(R.id.tvRollNo)
            val tvBusStop: TextView = view.findViewById(R.id.tvBusStop)
            val tvRoute: TextView = view.findViewById(R.id.tvRoute)
            val tvTime: TextView = view.findViewById(R.id.tvTime)
            val tvStatus: TextView = view.findViewById(R.id.tvStatusBadge)
            val txtAvatar: TextView = view.findViewById(R.id.txtAvatar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_attendance, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = filteredList[position]
            holder.tvName.text = item.studentName
            holder.tvRollNo.text = item.rollNo
            holder.tvBusStop.text = item.busStop
            holder.tvRoute.text = item.route
            holder.tvTime.text = item.arrivalTime
            holder.tvStatus.text = item.status.uppercase()

            // Avatar initial
            holder.txtAvatar.text = if (item.studentName.isNotEmpty()) item.studentName[0].toString() else ""

            // Dynamic Styling for Status
            when (item.status.uppercase()) {
                "PRESENT" -> {
                    holder.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                    holder.tvStatus.setTextColor(Color.parseColor("#166534"))
                }
                "ABSENT" -> {
                    holder.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                    holder.tvStatus.setTextColor(Color.parseColor("#991B1B"))
                }
                "LATE" -> {
                    holder.tvStatus.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    holder.tvStatus.setTextColor(Color.parseColor("#92400E"))
                }
            }
        }

        override fun getItemCount() = filteredList.size

        fun updateList(newList: List<AttendanceRecordModel>) {
            fullList = newList
            filteredList = newList
            notifyDataSetChanged()
        }

        fun filter(query: String) {
            filteredList = if (query.isEmpty()) {
                fullList
            } else {
                fullList.filter { 
                    it.studentName.contains(query, true) || it.rollNo.contains(query, true) 
                }
            }
            notifyDataSetChanged()
        }
    }
}
