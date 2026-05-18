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
            binding.tvBusValue.text = route.busNo
            binding.tvDriverValue.text = route.driverName
            binding.tvMetaInfo.text = "📍 ${route.stopsCount} Stops      👥 ${route.studentsCount} Students"

            // 1. Initial State Setting (Bina kisi listener ke taake card load hote waqt popup na aaye)
            binding.switchRouteStatus.setOnCheckedChangeListener(null)
            binding.switchRouteStatus.isChecked = (route.status == "ACTIVE")

            // 2. Click Listener with Confirmation Popup Logic
            binding.switchRouteStatus.setOnClickListener {
                val currentSwitchState = binding.switchRouteStatus.isChecked

                // Temporarily switch ko purani state par revert karein jab tak confirmation na mile
                binding.switchRouteStatus.isChecked = !currentSwitchState

                val message = if (currentSwitchState) {
                    "Are you sure you want to ENABLE ${route.routeCode}?"
                } else {
                    "Are you sure you want to DISABLE ${route.routeCode}?"
                }

                // Custom Alert Dialog Box
                AlertDialog.Builder(itemView.context)
                    .setTitle("Change Route Status")
                    .setMessage(message)
                    .setCancelable(false) // Taake user bahr click karke skip na kare
                    .setPositiveButton("Yes") { dialog, _ ->
                        // Confirmation milne par switch ko user ki requested state par set karein
                        binding.switchRouteStatus.isChecked = currentSwitchState

                        // Local data model ko bhi update karein
                        route.status = if (currentSwitchState) "ACTIVE" else "INACTIVE"

                        Toast.makeText(itemView.context, "Status Updated Successfully", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                    }
                    .setNegativeButton("Cancel") { dialog, _ ->
                        // Cancel karne par switch apni safe default position par hi rahega
                        dialog.dismiss()
                    }
                    .show()
            }

            // Unassigned fields visual styles adjust
            if (route.driverName == "Unassigned" || route.busNo == "TBD") {
                binding.tvDriverValue.setTextColor("#64748B".toColorInt())
                binding.tvBusValue.setTextColor("#64748B".toColorInt())
            } else {
                binding.tvDriverValue.setTextColor("#0A1D37".toColorInt())
                binding.tvBusValue.setTextColor("#0A1D37".toColorInt())
            }

            binding.btnViewDetails.setOnClickListener { onDetailsClick(route) }
        }
    }
}