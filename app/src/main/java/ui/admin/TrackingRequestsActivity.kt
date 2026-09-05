package ui.admin

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
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
import utils.ViewUtils

class TrackingRequestsActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var adapter: TrackingRequestAdapter
    private val viewModel: TrackingRequestsViewModel by viewModels()
    private var allRequests = listOf<TrackingRequestModel>()
    
    private lateinit var btnPending: TextView
    private lateinit var btnApproved: TextView
    private lateinit var btnRework: TextView
    private var currentFilterStatus = "PENDING"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_requests)

        supportActionBar?.hide()

        findViewById<View>(R.id.btnBack).setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            finish() 
        }

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

        btnPending = findViewById(R.id.btnPending)
        btnApproved = findViewById(R.id.btnApproved)
        btnRework = findViewById(R.id.btnRework)

        setupFilters()
        observeViewModel()
        viewModel.loadRequests()
    }

    private fun observeViewModel() {
        viewModel.requests.observe(this) { requests ->
            allRequests = requests
            updateList(currentFilterStatus)
        }
    }

    override fun onResume() {
        super.onResume()
        utils.NavigationUtils.setupBottomNavigation(this)
        viewModel.loadRequests()
    }

    private fun setupFilters() {
        btnPending.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            currentFilterStatus = "PENDING"
            updateList(currentFilterStatus)
        }

        btnApproved.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            currentFilterStatus = "APPROVED"
            updateList(currentFilterStatus)
        }

        btnRework.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            currentFilterStatus = "REWORK"
            updateList(currentFilterStatus)
        }
    }

    private fun updateList(status: String) {
        val filteredList = allRequests.filter { 
            if (status.uppercase() == "APPROVED") {
                // The "Approved / Enabled" tab shows requests with APPROVED status
                // regardless of whether tracking is currently ENABLED or DISABLED.
                it.status.uppercase() == "APPROVED"
            } else {
                it.status.uppercase() == status.uppercase()
            }
        }
        adapter.updateData(filteredList)
        updateFilterUI(status)
    }

    private fun updateFilterUI(selectedStatus: String) {
        val selectedColor = Color.parseColor("#1B2B48")
        val unselectedColor = Color.parseColor("#6B7280")

        // Reset all
        btnPending.apply {
            background = null
            setTextColor(unselectedColor)
            setTypeface(null, Typeface.NORMAL)
        }
        btnApproved.apply {
            background = null
            setTextColor(unselectedColor)
            setTypeface(null, Typeface.NORMAL)
        }
        btnRework.apply {
            background = null
            setTextColor(unselectedColor)
            setTypeface(null, Typeface.NORMAL)
        }

        // Set selected
        val selectedBtn = when (selectedStatus) {
            "APPROVED" -> btnApproved
            "REWORK" -> btnRework
            else -> btnPending
        }

        selectedBtn.apply {
            setBackgroundResource(R.drawable.bg_filter_selected)
            setTextColor(selectedColor)
            setTypeface(null, Typeface.BOLD)
        }
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

            // Mark as seen if it's a new pending request
            if (!request.isSeenByAdmin && request.status.uppercase() == "PENDING") {
                com.example.bustrack_app.data.FirebaseRepository.markTrackingRequestAsSeen(request.requestId)
            }
            
            // Set Request Date
            request.submittedAt?.let { timestamp ->
                val date = timestamp.toDate()
                val format = java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault())
                holder.itemView.findViewById<TextView>(R.id.tvRequestDate).text = "Requested on: ${format.format(date)}"
            } ?: run {
                holder.itemView.findViewById<TextView>(R.id.tvRequestDate).text = "Requested just now"
            }
            
            // Set status color and text based on Final Workflow
            when (request.status.uppercase()) {
                "APPROVED" -> {
                    if (request.trackingEnabled) {
                        holder.tvStatus.text = "Tracking Enabled"
                        holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                        holder.tvStatus.setTextColor(Color.parseColor("#15803D"))
                    } else {
                        holder.tvStatus.text = "Tracking Disabled"
                        holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_yellow)
                        holder.tvStatus.setTextColor(Color.parseColor("#92400E"))
                    }
                }
                "REWORK" -> {
                    holder.tvStatus.text = "Rework"
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge_red)
                    holder.tvStatus.setTextColor(Color.parseColor("#991B1B"))
                }
                "REJECTED" -> {
                    holder.tvStatus.text = "Rejected"
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge_red)
                    holder.tvStatus.setTextColor(Color.parseColor("#991B1B"))
                }
                else -> {
                    holder.tvStatus.text = "Pending"
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_yellow)
                    holder.tvStatus.setTextColor(Color.parseColor("#92400E"))
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
