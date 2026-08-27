package com.example.bustrack_app.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.databinding.ItemAlertOptionBinding
import com.example.bustrack_app.models.AlertOption

class DriverAlertsAdapter(
    private val options: List<AlertOption>,
    private val onOptionClick: (AlertOption) -> Unit
) : RecyclerView.Adapter<DriverAlertsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertOptionBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(options[position])
    }

    override fun getItemCount(): Int = options.size

    inner class ViewHolder(private val binding: ItemAlertOptionBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(option: AlertOption) {
            binding.tvEmoji.text = option.emoji
            binding.tvAlertTitle.text = option.title
            binding.tvAlertDesc.text = option.description
            
            binding.root.setOnClickListener {
                onOptionClick(option)
            }
        }
    }
}
