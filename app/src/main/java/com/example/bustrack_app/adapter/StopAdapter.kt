package com.example.bustrack_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.databinding.ItemStopRowBinding // 👈 Aapki layout binding class
import com.example.bustrack_app.models.StopItem

class StopAdapter(
    private var stops: List<StopItem>,
    private val onDeleteClick: (StopItem) -> Unit // Delete click handle karne ke liye function
) : RecyclerView.Adapter<StopAdapter.StopViewHolder>() {

    fun updateStops(newStops: List<StopItem>) {
        stops = newStops
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StopViewHolder {
        val binding = ItemStopRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StopViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        holder.bind(stops[position])
    }

    override fun getItemCount(): Int = stops.size

    inner class StopViewHolder(private val binding: ItemStopRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(stop: StopItem) {
            binding.tvStopNumber.text = stop.id
            binding.tvStopName.text = stop.stopName
            binding.tvStopTime.text = stop.time

            // Delete action button layout ke id ke mutabik
            binding.btnDeleteStop.setOnClickListener {
                onDeleteClick(stop)
            }
        }
    }
}