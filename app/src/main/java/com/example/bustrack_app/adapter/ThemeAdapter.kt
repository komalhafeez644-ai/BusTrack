package com.example.bustrack_app.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.TextView
import com.google.android.material.card.MaterialCardView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.ThemeOption

class ThemeAdapter(
    private var list: List<ThemeOption>,
    private val onClick: (ThemeOption) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeVH>() {

    class ThemeVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val title: TextView = itemView.findViewById(R.id.title)
        val desc: TextView = itemView.findViewById(R.id.desc)
        val icon: ImageView = itemView.findViewById(R.id.icon)
        val radio: RadioButton = itemView.findViewById(R.id.radio)
        val card: MaterialCardView = itemView.findViewById(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeVH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_theme, parent, false)
        return ThemeVH(view)
    }

    override fun onBindViewHolder(holder: ThemeVH, position: Int) {

        val item = list[position]

        holder.title.text = item.title
        holder.desc.text = item.description
        holder.icon.setImageResource(item.icon)
        holder.radio.isChecked = item.isSelected

        // UI STATE (ONLY SELECTION UI, NOT THEME)
        if (item.isSelected) {
            holder.card.strokeWidth = 3
            holder.card.setCardBackgroundColor(android.graphics.Color.parseColor("#E8F0FF"))
            holder.card.strokeColor = android.graphics.Color.parseColor("#2F6BFF")
        } else {
            holder.card.strokeWidth = 1
            holder.card.setCardBackgroundColor(android.graphics.Color.WHITE)
            holder.card.strokeColor = android.graphics.Color.parseColor("#D1D5DB")
        }

        holder.card.setOnClickListener {
            onClick(item)
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<ThemeOption>) {
        list = newList
        notifyDataSetChanged()
    }
}