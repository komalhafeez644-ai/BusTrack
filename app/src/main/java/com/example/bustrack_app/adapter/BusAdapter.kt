package com.example.bustrack_app.adapter

import android.graphics.Color
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ItemBusBinding
import com.example.bustrack_app.models.BusModel

class BusAdapter(
    private var busList: List<BusModel> = listOf(),
    private val onEditClicked: (BusModel) -> Unit,
    private val onStatusChanged: (BusModel, Boolean) -> Unit
) : RecyclerView.Adapter<BusAdapter.BusViewHolder>() {

    fun updateData(newList: List<BusModel>) {
        this.busList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BusViewHolder {
        val binding = ItemBusBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return BusViewHolder(binding)
    }

    override fun onBindViewHolder(holder: BusViewHolder, position: Int) = holder.bind(busList[position])

    override fun getItemCount(): Int = busList.size

    inner class BusViewHolder(private val binding: ItemBusBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(bus: BusModel) {
            // 1. Title aur Seats Data Binding
            binding.tvBusNumTitle.text = bus.busNumber
            binding.txtSeatsVal.text = "${bus.totalSeats} Seats"

            // 2. Bus Icon Flat Blue Styling
            binding.frameBusIcon.setBackgroundResource(R.drawable.bg_bus_circle_blue)
            binding.ivBusGraphic.setColorFilter(Color.parseColor("#1E88E5"))

            // 3. Driver & Route Check (Refined Status Logic)
            val driverName = bus.driverName
            val routeName = bus.routeName

            val hasDriver = !driverName.isNullOrEmpty()
            val hasRoute = !routeName.isNullOrEmpty()

            // Set Driver Text
            if (hasDriver) {
                binding.txtDriverVal.text = driverName
                binding.txtDriverVal.setTypeface(null, Typeface.NORMAL)
                binding.ivDriverMetaIcon.visibility = View.VISIBLE
            } else {
                binding.txtDriverVal.text = "Not Assigned"
                binding.txtDriverVal.setTypeface(null, Typeface.ITALIC)
                binding.ivDriverMetaIcon.visibility = View.GONE
            }

            // Set Route Text
            if (hasRoute) {
                binding.txtRouteVal.text = routeName
                binding.txtRouteVal.setTypeface(null, Typeface.NORMAL)
                binding.ivRouteMetaIcon.visibility = View.VISIBLE
            } else {
                binding.txtRouteVal.text = "Not Assigned"
                binding.txtRouteVal.setTypeface(null, Typeface.ITALIC)
                binding.ivRouteMetaIcon.visibility = View.GONE
            }

            // 3. Status Badge Logic (Operational Status)
            val isBusActive = bus.status.equals("ACTIVE", ignoreCase = true)
            
            binding.tvStatusBadge.text = if (isBusActive) "ACTIVE" else "INACTIVE"
            if (isBusActive) {
                binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")) // Light Green
                binding.tvStatusBadge.setTextColor(Color.parseColor("#2E7D32")) // Dark Green
            } else {
                binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")) // Light Red
                binding.tvStatusBadge.setTextColor(Color.parseColor("#C62828")) // Dark Red
            }

            // 4. Switch Toggle Handling
            binding.switchBusStatus.setOnCheckedChangeListener(null)
            binding.switchBusStatus.isChecked = isBusActive
            binding.switchBusStatus.isEnabled = true // Always enabled for Active/Inactive toggle

            binding.switchBusStatus.setOnClickListener {
                val isChecked = (it as androidx.appcompat.widget.SwitchCompat).isChecked
                // Pass the event to Activity to handle confirmation logic
                onStatusChanged(bus, isChecked)
            }

            // 5. Edit Icon Action Listener
            binding.ivEditAction.setOnClickListener { onEditClicked(bus) }
        }
    }
}