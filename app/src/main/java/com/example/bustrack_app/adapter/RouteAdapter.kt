package com.example.bustrack_app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ItemRouteCardBinding
import com.example.bustrack_app.models.RouteModel
import com.google.android.material.button.MaterialButton

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

            // 2. Click Listener with Custom Professional Logic
            binding.switchRouteStatus.setOnClickListener {
                val isCurrentlyActive = (route.status == "ACTIVE")
                val targetState = !isCurrentlyActive

                showCustomConfirmDialog(
                    title = if (targetState) "Activate Route?" else "Deactivate Route?",
                    message = if (targetState) {
                        "Are you sure you want to activate ${route.routeName}? This route will become available for bus assignments."
                    } else {
                        "Are you sure you want to deactivate ${route.routeName}? It will be hidden from new assignments."
                    },
                    iconRes = if (targetState) R.drawable.alt_route else R.drawable.warning,
                    confirmText = if (targetState) "Activate" else "Deactivate",
                    onConfirm = {
                        route.status = if (targetState) "ACTIVE" else "INACTIVE"
                        binding.switchRouteStatus.isChecked = targetState
                        Toast.makeText(itemView.context, "Route ${if (targetState) "Activated" else "Deactivated"}", Toast.LENGTH_SHORT).show()
                    },
                    onCancel = {
                        binding.switchRouteStatus.isChecked = isCurrentlyActive
                    }
                )
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

        private fun showCustomConfirmDialog(
            title: String,
            message: String,
            iconRes: Int,
            confirmText: String,
            onConfirm: () -> Unit,
            onCancel: () -> Unit
        ) {
            val context = itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_confirm_status, null)
            
            val ivIcon = dialogView.findViewById<ImageView>(R.id.ivDialogIcon)
            val tvTitle = dialogView.findViewById<TextView>(R.id.tvDialogTitle)
            val tvMsg = dialogView.findViewById<TextView>(R.id.tvDialogMessage)
            val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
            val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)

            tvTitle.text = title
            tvMsg.text = message
            ivIcon.setImageResource(iconRes)
            btnConfirm.text = confirmText

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .setCancelable(false)
                .create()

            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            btnCancel.setOnClickListener {
                onCancel()
                dialog.dismiss()
            }

            btnConfirm.setOnClickListener {
                onConfirm()
                dialog.dismiss()
            }

            dialog.show()
            
            val width = (context.resources.displayMetrics.widthPixels * 0.85).toInt()
            dialog.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }
}