package com.example.bustrack_app.adapter

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.AdminRecentActivity
import utils.ViewUtils

class RecentActivityAdapter(
    private var activities: List<AdminRecentActivity>,
    private val onItemClick: (AdminRecentActivity) -> Unit = {}
) : RecyclerView.Adapter<RecentActivityAdapter.ViewHolder>() {

    fun update(newActivities: List<AdminRecentActivity>) {
        this.activities = newActivities
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_activity, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(activities[position])
    }

    override fun getItemCount(): Int = activities.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivIcon: ImageView = itemView.findViewById(R.id.ivActivityIcon)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvActivityTitle)
        private val tvDesc: TextView = itemView.findViewById(R.id.tvActivityDescription)
        private val tvTime: TextView = itemView.findViewById(R.id.tvActivityTime)

        fun bind(item: AdminRecentActivity) {
            tvTitle.text = item.title
            tvDesc.text = item.description
            tvTime.text = item.timeAgo
            ivIcon.setImageResource(item.iconRes)
            ivIcon.backgroundTintList = ColorStateList.valueOf(item.bgTint)
            ivIcon.imageTintList = ColorStateList.valueOf(item.iconTint)

            itemView.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                onItemClick(item)
            }
        }
    }
}
