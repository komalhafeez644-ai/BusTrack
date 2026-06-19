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

            // 3. Driver & Route Check (Unassigned Logic)
            val driverName = bus.driverName
            val routeName = bus.routeName

            // Agar driver ya route mein se KOI EK cheez bhi missing hai
            val isUnassigned = driverName.isNullOrEmpty() || routeName.isNullOrEmpty()

            if (isUnassigned) {
                // Card text fields ko "Not Assigned" set karein
                binding.txtDriverVal.text = "Not Assigned"
                binding.txtRouteVal.text = "Not Assigned"

                // Style ko Italic karein jaisa requirements mein hai
                binding.txtDriverVal.setTypeface(null, Typeface.ITALIC)
                binding.txtRouteVal.setTypeface(null, Typeface.ITALIC)

                // Meta info icons hide karein
                binding.ivDriverMetaIcon.visibility = View.GONE
                binding.ivRouteMetaIcon.visibility = View.GONE

                // Status Badge styling for UNASSIGNED (Light Orange Background)
                binding.tvStatusBadge.text = "UNASSIGNED"
                binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFE0B2"))
                binding.tvStatusBadge.setTextColor(Color.parseColor("#E65100"))
            } else {
                // Agar dono assign hain to actual text show karein
                binding.txtDriverVal.text = driverName
                binding.txtRouteVal.text = routeName

                binding.txtDriverVal.setTypeface(null, Typeface.NORMAL)
                binding.txtRouteVal.setTypeface(null, Typeface.NORMAL)

                binding.ivDriverMetaIcon.visibility = View.VISIBLE
                binding.ivRouteMetaIcon.visibility = View.VISIBLE

                // Live Active/Inactive badge UI setup
                val displayStatus = if (bus.status == "UNASSIGNED") "ACTIVE" else bus.status.uppercase()
                binding.tvStatusBadge.text = displayStatus
                
                if (displayStatus == "ACTIVE") {
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9")) // Modern Light Green
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"))     // Dark Green
                } else {
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE")) // Modern Light Red
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#C62828"))     // Dark Red
                }
            }

            // 4. Switch Toggle Handling
            binding.switchBusStatus.setOnCheckedChangeListener(null) // Recycler view reuse loop protection

            // Switch tabhi ON dikhe jab status "ACTIVE" ho aur data missing na ho
            val isActiveState = bus.status.equals("ACTIVE", ignoreCase = true) && !isUnassigned
            binding.switchBusStatus.isChecked = isActiveState

            // Unassigned bus ka switch state disabled (lock) rahega
            binding.switchBusStatus.isEnabled = !isUnassigned

            binding.switchBusStatus.setOnCheckedChangeListener { _, isChecked ->
                val newStatus = if (isChecked) "ACTIVE" else "INACTIVE"
                binding.tvStatusBadge.text = newStatus
                
                if (isChecked) {
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E8F5E9"))
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#2E7D32"))
                } else {
                    binding.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEBEE"))
                    binding.tvStatusBadge.setTextColor(Color.parseColor("#C62828"))
                }

                // Main Activity callback ko notify karein
                onStatusChanged(bus, isChecked)
            }

            // 5. Edit Icon Action Listener
            binding.ivEditAction.setOnClickListener { onEditClicked(bus) }
        }
    }
}