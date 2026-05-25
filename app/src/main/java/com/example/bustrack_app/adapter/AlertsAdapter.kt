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
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class AlertsAdapter(
    private var list: List<TransportAlert>,
    private val onClick: (TransportAlert) -> Unit
) : RecyclerView.Adapter<AlertsAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView = view.findViewById(R.id.tvAlertTitle)
        val subtitle: TextView = view.findViewById(R.id.tvAlertSubtitle)
        val icon: ImageView = view.findViewById(R.id.ivAlertIcon)
        val iconBg: View = view.findViewById(R.id.ivIconBackground)
        val tagText: TextView = view.findViewById(R.id.tvTagText)
        val cardTag: MaterialCardView = view.findViewById(R.id.cardTag)
        val btn: MaterialButton = view.findViewById(R.id.btnViewDetails)
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
        holder.tagText.text = item.type

        // Dynamic Colors based on Type
        when (item.type.uppercase()) {
            "CRITICAL" -> {
                holder.cardTag.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                holder.tagText.setTextColor(Color.parseColor("#EF4444"))
                holder.icon.setColorFilter(Color.parseColor("#EF4444"))
            }
            "IMPORTANT" -> {
                holder.cardTag.setCardBackgroundColor(Color.parseColor("#FEF3C7"))
                holder.tagText.setTextColor(Color.parseColor("#D97706"))
                holder.icon.setColorFilter(Color.parseColor("#D97706"))
            }
            "GENERAL" -> {
                holder.cardTag.setCardBackgroundColor(Color.parseColor("#E0F2FE"))
                holder.tagText.setTextColor(Color.parseColor("#0284C7"))
                holder.icon.setColorFilter(Color.parseColor("#0284C7"))
            }
        }

        holder.btn.setOnClickListener {
            val originalBg = holder.btn.backgroundTintList
            val originalText = holder.btn.textColors

            // 1. Color change to Primary Dark
            holder.btn.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(it.context, R.color.primaryDark)
            holder.btn.setTextColor(Color.WHITE)

            // 2. Push Effect (In and Out)
            it.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction {
                        // 3. Revert back to original style
                        holder.btn.backgroundTintList = originalBg
                        holder.btn.setTextColor(originalText)
                        
                        onClick(item)
                    }.start()
                }
                .start()
        }
    }

    fun update(newList: List<TransportAlert>) {
        list = newList
        notifyDataSetChanged()
    }
}