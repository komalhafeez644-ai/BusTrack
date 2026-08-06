package com.example.bustrack_app.adapter

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
    private var currentStopIndex: Int = 0
) : RecyclerView.Adapter<NavigationStopsAdapter.ViewHolder>() {

    fun updateStops(newStops: List<StopItem>, currentIndex: Int) {
        this.stops = newStops
        this.currentStopIndex = currentIndex
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
            binding.tvStopTime.text = stop.time
            binding.tvStopIndex.text = (position + 1).toString()

            // Hide line for last item
            binding.timelineLine.visibility = if (position == stops.size - 1) View.GONE else View.VISIBLE

            when {
                position < currentStopIndex -> {
                    // Completed
                    binding.dotIndicator.visibility = View.VISIBLE
                    binding.dotIndicator.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_green)
                    binding.ivStopBus.visibility = View.GONE
                    binding.outerRing.visibility = View.GONE
                    
                    binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.status_green_bg))
                    binding.tvStatusText.text = "Completed"
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_green))
                }
                position == currentStopIndex -> {
                    // Current/Next Stop
                    binding.dotIndicator.visibility = View.GONE
                    binding.ivStopBus.visibility = View.VISIBLE
                    binding.outerRing.visibility = View.VISIBLE
                    
                    binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.icon_bg_blue))
                    binding.tvStatusText.text = "Next Stop"
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_blue))
                }
                else -> {
                    // Upcoming
                    binding.dotIndicator.visibility = View.VISIBLE
                    binding.dotIndicator.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.text_secondary)
                    binding.ivStopBus.visibility = View.GONE
                    binding.outerRing.visibility = View.GONE
                    
                    binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.icon_bg_grey))
                    binding.tvStatusText.text = "Upcoming"
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_secondary))
                }
            }
        }
    }
}
