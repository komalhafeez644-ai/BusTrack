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
    private var currentStopIndex: Int = 0,
    private var activeStopStatus: String = "NEXT" // NEXT, ARRIVED, PASSED
) : RecyclerView.Adapter<NavigationStopsAdapter.ViewHolder>() {

    fun updateStops(newStops: List<StopItem>, currentIndex: Int, status: String = "NEXT") {
        this.stops = newStops
        this.currentStopIndex = currentIndex
        this.activeStopStatus = status
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
            val displayTime = if (stop.time == "TBD" || stop.time.isEmpty()) "ETA: --" else stop.time
            binding.tvStopTime.text = displayTime
            binding.tvStopIndex.text = (position + 1).toString()

            // Hide line for last item
            binding.timelineLine.visibility = if (position == stops.size - 1) View.GONE else View.VISIBLE

            when {
                position < currentStopIndex -> {
                    // PASSED
                    binding.dotIndicator.visibility = View.VISIBLE
                    binding.dotIndicator.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_green)
                    binding.ivStopBus.visibility = View.GONE
                    binding.outerRing.visibility = View.GONE
                    
                    binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.status_green_bg))
                    binding.tvStatusText.text = "PASSED"
                    binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_green))
                }
                position == currentStopIndex -> {
                    // NEXT or ARRIVED or PASSED (if just transitioned)
                    if (activeStopStatus == "ARRIVED") {
                        binding.dotIndicator.visibility = View.GONE
                        binding.ivStopBus.visibility = View.VISIBLE
                        binding.outerRing.visibility = View.VISIBLE
                        binding.outerRing.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_green)
                        
                        binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.status_green_bg))
                        binding.tvStatusText.text = "ARRIVED"
                        binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_green))
                    } else if (activeStopStatus == "PASSED") {
                        // This case might happen momentarily before currentIndex increments
                        binding.dotIndicator.visibility = View.VISIBLE
                        binding.dotIndicator.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.status_green)
                        binding.ivStopBus.visibility = View.GONE
                        binding.outerRing.visibility = View.GONE
                        
                        binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.status_green_bg))
                        binding.tvStatusText.text = "PASSED"
                        binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.status_green))
                    } else {
                        // NEXT
                        binding.dotIndicator.visibility = View.GONE
                        binding.ivStopBus.visibility = View.VISIBLE
                        binding.outerRing.visibility = View.VISIBLE
                        binding.outerRing.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.icon_bg_blue)
                        
                        binding.statusBadge.setCardBackgroundColor(ContextCompat.getColor(itemView.context, R.color.icon_bg_blue))
                        binding.tvStatusText.text = "NEXT"
                        binding.tvStatusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.accent_blue))
                    }
                }
                else -> {
                    // UPCOMING
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
