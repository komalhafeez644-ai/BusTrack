package com.example.bustrack_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.NotificationSettingModel

class NotificationAdapter(private val list: List<NotificationSettingModel>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvTitle)
        val description: TextView = view.findViewById(R.id.tvDescription)
        val icon: ImageView = view.findViewById(R.id.ivIcon)
        val switch: SwitchCompat = view.findViewById(R.id.switchSetting)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_notification, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.title.text = item.title
        holder.description.text = item.description
        holder.icon.setImageResource(item.iconRes)
        
        holder.switch.setOnCheckedChangeListener(null)
        holder.switch.isChecked = item.isEnabled
        
        holder.switch.setOnCheckedChangeListener { _, isChecked ->
            item.isEnabled = isChecked
        }
    }

    override fun getItemCount() = list.size
}