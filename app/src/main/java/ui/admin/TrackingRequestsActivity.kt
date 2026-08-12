package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.TrackingRequestModel
import com.example.bustrack_app.viewmodels.TrackingRequestsViewModel
import com.google.android.material.chip.ChipGroup

class TrackingRequestsActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var adapter: TrackingRequestAdapter
    private val viewModel: TrackingRequestsViewModel by viewModels()
    private var allRequests = listOf<TrackingRequestModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_requests)

        supportActionBar?.hide()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        rvRequests = findViewById(R.id.rvRequests)
        rvRequests.layoutManager = LinearLayoutManager(this)
        
        adapter = TrackingRequestAdapter(emptyList()) { request ->
            val intent = Intent(this, TrackingApprovalDetailActivity::class.java)
            intent.putExtra("REQUEST_ID", request.requestId)
            intent.putExtra("PARENT_ID", request.parentId)
            intent.putExtra("STUDENT_ID", request.studentId)
            intent.putExtra("PARENT_NAME", request.parentName)
            intent.putExtra("STATUS", request.status)
            startActivity(intent)
        }
        rvRequests.adapter = adapter

        setupFilters()
        observeViewModel()
        viewModel.loadRequests()
    }

    private fun observeViewModel() {
        viewModel.requests.observe(this) { requests ->
            allRequests = requests
            val chipGroup = findViewById<ChipGroup>(R.id.chipGroupStatus)
            val filterStatus = when (chipGroup.checkedChipId) {
                R.id.chipApproved -> "Approved"
                R.id.chipRejected -> "Rejected"
                else -> "Pending"
            }
            updateList(filterStatus)
        }
    }

    override fun onResume() {
        super.onResume()
        utils.NavigationUtils.setupBottomNavigation(this)
        viewModel.loadRequests()
    }

    private fun setupFilters() {
        val chipGroup = findViewById<ChipGroup>(R.id.chipGroupStatus)
        chipGroup.setOnCheckedChangeListener { _, checkedId ->
            val filterStatus = when (checkedId) {
                R.id.chipApproved -> "Approved"
                R.id.chipRejected -> "Rejected"
                else -> "Pending"
            }
            updateList(filterStatus)
        }
    }

    private fun updateList(status: String) {
        val filteredList = allRequests.filter { it.status.equals(status, ignoreCase = true) }
        adapter.updateData(filteredList)
    }

    class TrackingRequestAdapter(
        private var requests: List<TrackingRequestModel>,
        private val onItemClick: (TrackingRequestModel) -> Unit
    ) : RecyclerView.Adapter<TrackingRequestAdapter.ViewHolder>() {

        fun updateData(newRequests: List<TrackingRequestModel>) {
            requests = newRequests
            notifyDataSetChanged()
        }

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvParentName: TextView = view.findViewById(R.id.tvParentName)
            val tvStatus: TextView = view.findViewById(R.id.tvStatus)
            val containerChildren: LinearLayout = view.findViewById(R.id.containerChildren)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tracking_request, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val request = requests[position]
            holder.tvParentName.text = request.parentName
            holder.tvStatus.text = request.status.replaceFirstChar { it.uppercase() }
            
            // Set status color
            when (request.status.lowercase()) {
                "approved" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#15803D"))
                }
                "rejected" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge_red)
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#991B1B"))
                }
                else -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_yellow)
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#92400E"))
                }
            }

            // Dynamically add student info to the card
            holder.containerChildren.removeAllViews()
            val childView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.layout_child_request_item, holder.containerChildren, false)
            childView.findViewById<TextView>(R.id.tvStudentInfo).text = "Student ID: ${request.studentId}"
            
            val tvRoute = childView.findViewById<TextView>(R.id.tvEnabledRoute)
            tvRoute.visibility = View.GONE
            
            holder.containerChildren.addView(childView)

            holder.itemView.setOnClickListener { onItemClick(request) }
        }

        override fun getItemCount() = requests.size
    }
}
