package com.example.bustrack.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack.Models.QuickAction
import com.example.bustrack.databinding.ItemQuickActionBinding

class QuickActionAdapter(
    private val actions: List<QuickAction>,
    private val onItemClick: (QuickAction) -> Unit
) : RecyclerView.Adapter<QuickActionAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemQuickActionBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemQuickActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val action = actions[position]
        holder.binding.tvActionTitle.text = action.title
        holder.binding.ivActionIcon.setImageResource(action.iconRes)
        holder.binding.root.setOnClickListener { onItemClick(action) }
    }

    override fun getItemCount() = actions.size
}
