package com.example.bustrack_app.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R // R file ka sahi path ensure karein
import com.example.bustrack_app.models.PrivacyPolicyModel

class PrivacyPolicyAdapter(private val list: List<PrivacyPolicyModel>) :
    RecyclerView.Adapter<PrivacyPolicyAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val desc: TextView = view.findViewById(R.id.tvDescription)
        val header: RelativeLayout = view.findViewById(R.id.headerLayout)
        val arrow: ImageView = view.findViewById(R.id.arrowIcon)
        val expandedContent: View = view.findViewById(R.id.expandedContent)
        val iconRole: ImageView = view.findViewById(R.id.iconRole)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_privacy_policy, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]

        holder.title.text = item.title
        holder.desc.text = item.description

        // Drop-down ki visibility manage karna
        holder.expandedContent.visibility = if (item.isExpanded) View.VISIBLE else View.GONE

        // Arrow ko rotate karna (expand ho toh neeche, collapse ho toh seedha)
        holder.arrow.rotation = if (item.isExpanded) 180f else 0f
        
        // Dynamic icons based on role if needed
        when (item.title) {
            "Student & Parents" -> holder.iconRole.setImageResource(R.drawable.outline_account_circle_24)
            "Drivers" -> holder.iconRole.setImageResource(R.drawable.directions_bus)
            "System Administrators" -> holder.iconRole.setImageResource(R.drawable.admin_panel_settings)
            else -> holder.iconRole.setImageResource(R.drawable.outline_account_circle_24)
        }

        // Click logic
        holder.header.setOnClickListener {
            item.isExpanded = !item.isExpanded
            notifyItemChanged(position) // Sirf us specific row ko update karta hai
        }
    }

    override fun getItemCount(): Int = list.size
}