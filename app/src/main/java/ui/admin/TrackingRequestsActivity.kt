package ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.google.android.material.chip.ChipGroup

class TrackingRequestsActivity : AppCompatActivity() {

    private lateinit var rvRequests: RecyclerView
    private lateinit var adapter: TrackingRequestAdapter
    private var allRequests = listOf<ParentRequest>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tracking_requests)

        supportActionBar?.hide()

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        rvRequests = findViewById(R.id.rvRequests)
        rvRequests.layoutManager = LinearLayoutManager(this)
        
        // Mock data with multi-child support and enabled routes
        allRequests = listOf(
            ParentRequest("John Doe", "Pending", listOf(
                ChildInfo("Elena Rodriguez", "#SR-9921")
            )),
            ParentRequest("Sara Ahmed", "Approved", listOf(
                ChildInfo("Ali Hassan", "#SR-2045", "Route-01 (Gulshan)"),
                ChildInfo("Zain Ahmed", "#SR-042", "Route-08 (North)")
            )),
            ParentRequest("M. Bilal", "Approved", listOf(
                ChildInfo("Hassan Bilal", "#SR-088", "Route-03 (Defence)")
            )),
            ParentRequest("Hamza Ali", "Rejected", listOf(
                ChildInfo("Fatima Hamza", "STD-2024-105")
            )),
            ParentRequest("Irfan Khan", "Pending", listOf(
                ChildInfo("Zoya Khan", "#SR-1011")
            ))
        )
        
        adapter = TrackingRequestAdapter(allRequests.filter { it.status == "Pending" }) { request ->
            val intent = Intent(this, TrackingApprovalDetailActivity::class.java)
            intent.putExtra("PARENT_NAME", request.parentName)
            intent.putExtra("STATUS", request.status)
            
            // Pass all children data as JSON
            val gson = com.google.gson.Gson()
            val childrenJson = gson.toJson(request.children)
            intent.putExtra("CHILDREN_JSON", childrenJson)

            startActivity(intent)
        }
        rvRequests.adapter = adapter

        setupFilters()
    }

    override fun onResume() {
        super.onResume()
        utils.NavigationUtils.setupBottomNavigation(this)
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
        val filteredList = allRequests.filter { it.status == status }
        adapter.updateData(filteredList)
    }

    data class ParentRequest(
        val parentName: String,
        val status: String,
        val children: List<ChildInfo>
    )

    data class ChildInfo(
        val name: String,
        val id: String,
        val enabledRoute: String? = null
    )

    class TrackingRequestAdapter(
        private var requests: List<ParentRequest>,
        private val onItemClick: (ParentRequest) -> Unit
    ) : RecyclerView.Adapter<TrackingRequestAdapter.ViewHolder>() {

        fun updateData(newRequests: List<ParentRequest>) {
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
            holder.tvStatus.text = request.status
            
            // Set status color
            when (request.status) {
                "Approved" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_active)
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#15803D"))
                }
                "Rejected" -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_status_badge_red)
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#991B1B"))
                }
                else -> {
                    holder.tvStatus.setBackgroundResource(R.drawable.bg_badge_yellow)
                    holder.tvStatus.setTextColor(android.graphics.Color.parseColor("#92400E"))
                }
            }

            // Dynamically add children to the card
            holder.containerChildren.removeAllViews()
            request.children.forEach { child ->
                val childView = LayoutInflater.from(holder.itemView.context).inflate(R.layout.layout_child_request_item, holder.containerChildren, false)
                childView.findViewById<TextView>(R.id.tvStudentInfo).text = "Child: ${child.name} (${child.id})"
                
                val tvRoute = childView.findViewById<TextView>(R.id.tvEnabledRoute)
                if (child.enabledRoute != null) {
                    tvRoute.text = "Tracking: ${child.enabledRoute}"
                    tvRoute.visibility = View.VISIBLE
                } else {
                    tvRoute.visibility = View.GONE
                }
                
                holder.containerChildren.addView(childView)
            }

            holder.itemView.setOnClickListener { onItemClick(request) }
        }

        override fun getItemCount() = requests.size
    }
}
