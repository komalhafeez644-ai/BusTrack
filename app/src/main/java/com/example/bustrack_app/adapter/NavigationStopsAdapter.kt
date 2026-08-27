package com.example.bustrack_app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ItemStopTimelineBinding
import com.example.bustrack_app.models.StopItem

class NavigationStopsAdapter(
    private var stops: List<StopItem>,
    private var currentStopIndex: Int = 0,
    private var activeStopStatus: String = "NEXT",
    private var isDarkMode: Boolean = false
) : RecyclerView.Adapter<NavigationStopsAdapter.ViewHolder>() {

    fun updateStops(newStops: List<StopItem>, currentIndex: Int, status: String = "NEXT") {
        this.stops = newStops
        this.currentStopIndex = currentIndex
        this.activeStopStatus = status
        notifyDataSetChanged()
    }

    fun setTheme(isDark: Boolean) {
        this.isDarkMode = isDark
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemStopTimelineBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(stops[position], position)
    }

    override fun getItemCount(): Int = stops.size

    inner class ViewHolder(private val binding: ItemStopTimelineBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stop: StopItem, position: Int) {
            binding.tvStopName.text = stop.stopName

            val displayTime = when {
                stop.time.isEmpty() || stop.time == "TBD" -> "ETA: --"
                stop.time == "Skipped" -> "Skipped"
                stop.time.startsWith("Arrived:") -> stop.time
                stop.time.startsWith("COMPLETED") -> stop.time.replace("COMPLETED", "Arrived:")
                stop.time.startsWith("ARRIVED") -> stop.time.replace("ARRIVED", "Arrived:")
                stop.time.contains("min") || stop.time.contains("Arriving") -> {
                    if (stop.time.startsWith("ETA:")) stop.time else "ETA: ${stop.time}"
                }
                else -> stop.time // Likely a fixed time like "09:35 AM"
            }
            binding.tvStopTime.text = displayTime
            binding.tvStopIndex.text = (position + 1).toString()

            // Apply Theme Colors
            val primaryColor = if (isDarkMode) Color.WHITE else Color.parseColor("#0F172A")
            val secondaryColor = if (isDarkMode) Color.parseColor("#B0BEC5") else Color.parseColor("#64748B")

            binding.tvStopName.setTextColor(primaryColor)
            binding.tvStopTime.setTextColor(secondaryColor)

            binding.timelineLine.visibility = if (position == stops.size - 1) View.GONE else View.VISIBLE

            // Exact spec: Upcoming -> Arrived -> Passed
            // - hasArrived: is stop ka arrival time record ho chuka hai (stopArrivalTimes se,
            //   permanently set - kabhi reset/recalculate nahi hota, chahe status Arrived ho
            //   ya baad mein Passed ban jaye).
            // - currentStopIndex (activity se "liveArrivedIndex" ke roop mein aata hai): sirf
            //   WAHI stop jo ABHI geofence ke andar hai. Jab bus geofence se nikal jaati hai,
            //   ye -1 (ya kisi aur stop ka index) ho jaata hai - is stop ka status turant aur
            //   permanently "Passed" ban jaata hai (time same rehta hai).
            val hasArrived = displayTime.startsWith("Arrived:")
            val isSkipped = displayTime == "Skipped"
            val isLive = hasArrived && position == currentStopIndex

            when {
                isLive -> {
                    // ARRIVED - geofence ke andar hai abhi
                    binding.dotIndicator.visibility = View.GONE
                    binding.ivStopBus.visibility = View.VISIBLE
                    binding.outerRing.visibility = View.VISIBLE
                    binding.outerRing.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_green)

                    binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.status_green_bg))
                    binding.tvStatusText.text = "ARRIVED"
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_green))
                }
                hasArrived -> {
                    // PASSED - pehle arrive hua tha, ab geofence se nikal chuka hai.
                    // Arrival time (displayTime) bilkul same rehta hai, sirf status badge badalta hai.
                    binding.dotIndicator.visibility = View.VISIBLE
                    binding.dotIndicator.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_green)
                    binding.ivStopBus.visibility = View.GONE
                    binding.outerRing.visibility = View.GONE

                    binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.icon_bg_grey))
                    binding.tvStatusText.text = "PASSED"
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                }
                isSkipped -> {
                    // SKIPPED - is stop par bus genuinely nahi ruki
                    binding.dotIndicator.visibility = View.VISIBLE
                    binding.dotIndicator.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.text_secondary)
                    binding.ivStopBus.visibility = View.GONE
                    binding.outerRing.visibility = View.GONE

                    binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.icon_bg_grey))
                    binding.tvStatusText.text = "SKIPPED"
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                }
                else -> {
                    // UPCOMING - abhi tak geofence mein nahi pahunchi (ETA dikhega)
                    binding.dotIndicator.visibility = View.VISIBLE
                    binding.dotIndicator.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.text_secondary)
                    binding.ivStopBus.visibility = View.GONE
                    binding.outerRing.visibility = View.GONE

                    binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.icon_bg_grey))
                    binding.tvStatusText.text = "UPCOMING"
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                }
            }
        }
    }
}