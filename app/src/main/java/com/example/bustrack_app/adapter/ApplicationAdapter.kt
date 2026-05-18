package com.example.bustrack_app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.ApplicationModel

class ApplicationAdapter(
    private val list: List<ApplicationModel>
) : RecyclerView.Adapter<ApplicationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtClass: TextView = itemView.findViewById(R.id.txtClass)
        val txtPickup: TextView = itemView.findViewById(R.id.txtPickup)
        val txtRoute: TextView = itemView.findViewById(R.id.txtRoute)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtName.text = item.studentName
        holder.txtClass.text = item.studentClass
        holder.txtPickup.text = item.pickupPoint
        holder.txtRoute.text = item.routeMatch
    }
}