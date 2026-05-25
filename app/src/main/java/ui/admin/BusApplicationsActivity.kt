package ui.admin

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.adapter.ApplicationAdapter
import com.example.bustrack_app.models.ApplicationModel
import utils.NavigationUtils

class BusApplicationsActivity : AppCompatActivity() {

    companion object {
        var applicationsList = ArrayList<ApplicationModel>()
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ApplicationAdapter
    private var filteredList = ArrayList<ApplicationModel>()
    
    private lateinit var tabAll: TextView
    private lateinit var tabPending: TextView
    private lateinit var tabApproved: TextView
    private var currentFilter = "All"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bus_application)

        // Navigation
        NavigationUtils.setupBottomNavigation(this)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            onBackPressedDispatcher.onBackPressed()
        }

        recyclerView = findViewById(R.id.recyclerApplications)
        tabAll = findViewById(R.id.tabAll)
        tabPending = findViewById(R.id.tabPending)
        tabApproved = findViewById(R.id.tabApproved)
        val searchBox = findViewById<EditText>(R.id.searchBox)

        if (applicationsList.isEmpty()) {
            setupDummyData()
        }
        filteredList.addAll(applicationsList)

        adapter = ApplicationAdapter(filteredList) { application ->
            if (application.status == "Approved") {
                val intent = Intent(this, ApplicationDetailActivity::class.java)
                intent.putExtra("APPLICATION_DATA", application)
                startActivity(intent)
            } else {
                val intent = Intent(this, RouteAnalysisActivity::class.java)
                intent.putExtra("APPLICATION_DATA", application)
                startActivity(intent)
            }
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        setupSearch(searchBox)
        setupTabs()
    }

    private fun setupDummyData() {
        applicationsList.add(ApplicationModel(1, "Aryan Sharma", "Grade 10 • Section B", "Green Park Sector 4", "92 3459745703", "2 HRS AGO", "Pending", R.drawable.img, "Rajesh Sharma", "Route-01", "95%", "North Gate", "0.8km away"))
        applicationsList.add(ApplicationModel(2, "Vanya Patel", "Grade 8 • Section A", "Sunrise Heights", "92 3456789012", "5 HRS AGO", "Pending", R.drawable.ic_person, "Vikram Patel", "Route-05", "88%", "Sunrise Point", "1.2km away"))
        applicationsList.add(ApplicationModel(3, "Rohan Gupta", "Grade 12 • Section C", "Oakwood Avenue", "92 3123456789", "Yesterday", "Approved", R.drawable.img, "Sanjay Gupta", "Route-02", "75%", "Oakwood Entry", "2.1km away"))
        applicationsList.add(ApplicationModel(4, "Zoya Khan", "Grade 9 • Section D", "Blue Tower", "92 3001234567", "3 HRS AGO", "Approved", R.drawable.ic_person, "Imran Khan", "Route-01", "98%", "Central Hub", "0.5km away"))
    }

    private fun setupSearch(searchBox: EditText) {
        searchBox.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun setupTabs() {
        tabAll.setOnClickListener { 
            utils.ViewUtils.applyClickEffect(it)
            updateFilter("All") 
        }
        tabPending.setOnClickListener { 
            utils.ViewUtils.applyClickEffect(it)
            updateFilter("Pending") 
        }
        tabApproved.setOnClickListener { 
            utils.ViewUtils.applyClickEffect(it)
            updateFilter("Approved") 
        }
    }

    private fun updateFilter(filterType: String) {
        currentFilter = filterType
        
        // UI Reset
        val tabs = listOf(tabAll, tabPending, tabApproved)
        tabs.forEach {
            it.setBackgroundResource(R.drawable.bg_chip_unselected)
            it.setTextColor(ContextCompat.getColor(this, R.color.lightGray))
            it.setTypeface(null, Typeface.NORMAL)
        }

        // Selected Tab UI
        val selectedTab = when(filterType) {
            "Pending" -> tabPending
            "Approved" -> tabApproved
            else -> tabAll
        }
        selectedTab.setBackgroundResource(R.drawable.bg_chip_selected)
        selectedTab.setTextColor(ContextCompat.getColor(this, R.color.primaryDark))
        selectedTab.setTypeface(null, Typeface.BOLD)

        filter("") // Trigger filter logic with current tab
    }

    private fun filter(query: String) {
        val temp = if (currentFilter == "All") applicationsList else applicationsList.filter { it.status == currentFilter }
        
        filteredList.clear()
        if (query.isEmpty()) {
            filteredList.addAll(temp)
        } else {
            val searchResults = temp.filter { 
                it.studentName.contains(query, ignoreCase = true) || 
                it.pickupPoint.contains(query, ignoreCase = true) 
            }
            filteredList.addAll(searchResults)
        }
        adapter.notifyDataSetChanged()
    }
}