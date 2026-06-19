package ui.admin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.StudentAdapter
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityManageStudentBinding
import com.example.bustrack_app.models.StudentModel
import com.example.bustrack_app.viewmodels.StudentViewModel

class ManageStudentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageStudentBinding
    private lateinit var viewModel: StudentViewModel
    private lateinit var studentAdapter: StudentAdapter
    private var fullStudentList = listOf<StudentModel>()
    private var currentFilter = "ALL"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageStudentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        viewModel = ViewModelProvider(this)[StudentViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupSearch()
        
        binding.btnMenu.setOnClickListener { 
            utils.ViewUtils.applyClickEffect(it)
            finish() 
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-generate dynamic chips in case routes were added/removed
        setupDynamicFilters()
        utils.NavigationUtils.setupBottomNavigation(this)
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentAdapter(
            students = listOf(),
            onAssignClick = { student ->
                val intent = Intent(this, RouteAnalysisActivity::class.java)
                intent.putExtra("APPLICATION_DATA", mapStudentToAppModel(student))
                startActivity(intent)
            },
            onEditClick = { student ->
                val intent = Intent(this, StudentDetailsActivity::class.java)
                intent.putExtra("STUDENT_ID", student.id)
                startActivity(intent)
            }
        )
        binding.rvStudents.apply {
            layoutManager = LinearLayoutManager(this@ManageStudentActivity)
            adapter = studentAdapter
        }

        binding.fabAddStudent.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            startActivity(Intent(this, AddStudentActivity::class.java))
        }
    }

    private fun setupObservers() {
        viewModel.studentList.observe(this) { list ->
            fullStudentList = list
            applyFilter() // Filter data based on current selection
            binding.txtTotalCount.text = "${list.size} Students Total"
        }

        // Observe Route List to update filters dynamically
        RouteRepository.routeList.observe(this) { routes ->
            setupDynamicFilters()
        }
    }

    private fun setupDynamicFilters() {
        val filterContainer = binding.filtersScroll.getChildAt(0) as LinearLayout
        filterContainer.removeAllViews()

        // 1. "All Students" Chip
        addFilterChip(filterContainer, "All Students", "ALL")

        // 2. "Unassigned" Chip
        addFilterChip(filterContainer, "Unassigned", "UNASSIGNED")

        // 3. Dynamic Route Chips from Repository
        val routes = RouteRepository.routeList.value ?: listOf()
        routes.forEach { route ->
            addFilterChip(filterContainer, route.routeName, route.routeName.uppercase())
        }
        
        // Initial Selection UI
        updateChipsUI()
    }

    private fun addFilterChip(container: LinearLayout, label: String, filterId: String) {
        val textView = TextView(this)
        textView.text = label
        textView.gravity = Gravity.CENTER
        textView.textSize = 14f
        textView.setPadding(40, 20, 40, 20)
        
        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 0, 16, 0)
        textView.layoutParams = params
        
        textView.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            currentFilter = filterId
            updateChipsUI()
            applyFilter()
        }
        
        textView.tag = filterId
        container.addView(textView)
    }

    private fun updateChipsUI() {
        val filterContainer = binding.filtersScroll.getChildAt(0) as LinearLayout
        for (i in 0 until filterContainer.childCount) {
            val child = filterContainer.getChildAt(i) as TextView
            if (child.tag == currentFilter) {
                child.setBackgroundResource(R.drawable.bg_filter_selected)
                child.setTextColor(Color.parseColor("#1B2B48"))
                child.setTypeface(null, Typeface.BOLD)
            } else {
                child.background = null
                child.setTextColor(Color.parseColor("#6B7280"))
                child.setTypeface(null, Typeface.NORMAL)
            }
        }
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "ALL" -> fullStudentList
            "UNASSIGNED" -> fullStudentList.filter { it.route.isNullOrEmpty() }
            else -> fullStudentList.filter { it.route?.uppercase() == currentFilter }
        }
        studentAdapter.setStudents(filtered)
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString().lowercase().trim()
                val searched = fullStudentList.filter { 
                    it.name.lowercase().contains(query) || it.id.lowercase().contains(query)
                }
                studentAdapter.setStudents(searched)
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    // Helper to convert StudentModel to ApplicationModel for Route Analysis
    private fun mapStudentToAppModel(s: StudentModel): com.example.bustrack_app.models.ApplicationModel {
        return com.example.bustrack_app.models.ApplicationModel(
            id = s.id.filter { it.isDigit() }.toIntOrNull() ?: 0,
            studentName = s.name,
            studentClass = s.grade,
            pickupPoint = s.location,
            contactNumber = s.phoneNumber,
            time = "Now",
            status = "Pending",
            image = s.profileImage
        )
    }
}