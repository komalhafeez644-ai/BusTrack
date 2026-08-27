package com.example.bustrack_app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ItemAlertCardBinding
import com.example.bustrack_app.models.TransportAlert

class AlertsAdapter(
    private var alerts: List<TransportAlert>,
    private val onItemClick: (TransportAlert) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.ViewHolder>() {

    fun update(newAlerts: List<TransportAlert>) {
        this.alerts = newAlerts
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAlertCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(alerts[position])
    }

    override fun getItemCount(): Int = alerts.size

    inner class ViewHolder(private val binding: ItemAlertCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(alert: TransportAlert) {
            binding.tvAlertTitle.text = alert.title
            binding.tvAlertSubtitle.text = alert.subtitle
            binding.ivAlertIcon.setImageResource(alert.iconResId)
            binding.tvTagText.text = alert.type

            // Dynamic Styling based on Type
            when (alert.type.uppercase()) {
                "CRITICAL" -> {
                    binding.cvIconBg.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                    binding.ivAlertIcon.setColorFilter(Color.parseColor("#EF4444"))
                    binding.tvTagText.setTextColor(Color.parseColor("#EF4444"))
                    binding.tvTagText.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                }
                "IMPORTANT" -> {
                    binding.cvIconBg.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
                    binding.ivAlertIcon.setColorFilter(Color.parseColor("#D97706"))
                    binding.tvTagText.setTextColor(Color.parseColor("#D97706"))
                    binding.tvTagText.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                }
                else -> {
                    binding.cvIconBg.setCardBackgroundColor(Color.parseColor("#E0F2FE"))
                    binding.ivAlertIcon.setColorFilter(Color.parseColor("#0284C7"))
                    binding.tvTagText.setTextColor(Color.parseColor("#0284C7"))
                    binding.tvTagText.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E0F2FE"))
                }
            }

            binding.root.setOnClickListener { onItemClick(alert) }
        }
    }
}
