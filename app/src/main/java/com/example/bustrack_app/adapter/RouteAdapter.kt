package com.example.bustrack_app.adapter

import android.app.AlertDialog
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.toColorInt
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.databinding.ItemRouteCardBinding
import com.example.bustrack_app.models.RouteModel

class RouteAdapter(
    private var routes: List<RouteModel>,
    private val onDetailsClick: (RouteModel) -> Unit
) : RecyclerView.Adapter<RouteAdapter.RouteViewHolder>() {

    fun updateData(newRoutes: List<RouteModel>) {
        routes = newRoutes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteViewHolder {
        val binding = ItemRouteCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RouteViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RouteViewHolder, position: Int) {
        holder.bind(routes[position])
    }

    override fun getItemCount(): Int = routes.size

    inner class RouteViewHolder(private val binding: ItemRouteCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(route: RouteModel) {
            binding.tvRouteCode.text = route.routeCode
            binding.tvRouteName.text = route.routeName
            binding.tvBusValue.text = if (route.busNo.isNullOrEmpty()) "Unassigned" else route.busNo
            binding.tvDriverValue.text = if (route.driverName.isNullOrEmpty()) "Unassigned" else route.driverName
            binding.tvMetaInfo.text = "📍 ${route.stopsCount}  •  👥 ${route.studentsCount}"

            // 1. Initial State Setting
            binding.switchRouteStatus.setOnCheckedChangeListener(null)
            binding.switchRouteStatus.isChecked = (route.status == "ACTIVE")

            // 2. Click Listener with Confirmation Popup Logic
            binding.switchRouteStatus.setOnClickListener {
                val isCurrentlyActive = (route.status == "ACTIVE")
                val targetState = !isCurrentlyActive

                val message = if (targetState) {
                    "Are you sure you want to ACTIVATE ${route.routeName}?"
                } else {
                    "Are you sure you want to DEACTIVATE ${route.routeName}?"
                }

                AlertDialog.Builder(itemView.context)
                    .setTitle("Route Status")
                    .setMessage(message)
                    .setPositiveButton("Yes") { _, _ ->
                        route.status = if (targetState) "ACTIVE" else "INACTIVE"
                        binding.switchRouteStatus.isChecked = targetState
                        Toast.makeText(itemView.context, "Status Updated", Toast.LENGTH_SHORT).show()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        binding.switchRouteStatus.isChecked = isCurrentlyActive
                        dialog.dismiss()
                    }
                    .show()
            }

            // Visual styles for Unassigned fields
            if (binding.tvDriverValue.text == "Unassigned" || binding.tvBusValue.text == "Unassigned") {
                binding.tvDriverValue.setTextColor(Color.parseColor("#94A3B8"))
                binding.tvBusValue.setTextColor(Color.parseColor("#94A3B8"))
            } else {
                binding.tvDriverValue.setTextColor(Color.parseColor("#1E293B"))
                binding.tvBusValue.setTextColor(Color.parseColor("#1E293B"))
            }

            binding.btnViewDetails.setOnClickListener { onDetailsClick(route) }
        }
    }
}