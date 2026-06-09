package com.example.bustrack_app.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.TransportAlert

class AlertsAdapter(
    private var list: List<TransportAlert>,
    private val onClick: (TransportAlert) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvAlertTitle)
        val subtitle: TextView = view.findViewById(R.id.tvAlertSubtitle)
        val icon: ImageView = view.findViewById(R.id.ivAlertIcon)
        val cvIconBg: androidx.cardview.widget.CardView = view.findViewById(R.id.cvIconBg)
        val tagText: TextView = view.findViewById(R.id.tvTagText)
        val timeText: TextView = view.findViewById(R.id.tvTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_alert_card, parent, false)
        return VH(v)
    }

    override fun getItemCount() = list.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]

        holder.title.text = item.title
        holder.subtitle.text = item.subtitle
        holder.icon.setImageResource(item.iconResId)
        holder.tagText.text = item.type.uppercase()
        holder.timeText.text = "Now" // Mocking time

        // Dynamic Colors based on Type
        when (item.type.uppercase()) {
            "CRITICAL" -> {
                holder.cvIconBg.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                holder.tagText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEE2E2")))
                holder.tagText.setTextColor(Color.parseColor("#EF4444"))
                holder.icon.setColorFilter(Color.parseColor("#EF4444"))
            }
            "IMPORTANT" -> {
                holder.cvIconBg.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
                holder.tagText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FEF3C7")))
                holder.tagText.setTextColor(Color.parseColor("#D97706"))
                holder.icon.setColorFilter(Color.parseColor("#D97706"))
            }
            "GENERAL" -> {
                holder.cvIconBg.setCardBackgroundColor(Color.parseColor("#E0F2FE"))
                holder.tagText.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E0F2FE")))
                holder.tagText.setTextColor(Color.parseColor("#0284C7"))
                holder.icon.setColorFilter(Color.parseColor("#0284C7"))
            }
        }

        holder.itemView.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            onClick(item)
        }
    }

    fun update(newList: List<TransportAlert>) {
        list = newList
        notifyDataSetChanged()
    }
}
