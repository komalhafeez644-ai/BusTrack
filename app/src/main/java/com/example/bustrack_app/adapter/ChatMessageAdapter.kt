package com.example.bustrack_app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.ChatMessageModel
import com.google.android.material.card.MaterialCardView

class ChatMessageAdapter(private val messages: MutableList<ChatMessageModel>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 0
        private const val TYPE_BOT = 1
    }

    class UserViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
    }

    class BotViewHolder(view: android.view.View) : RecyclerView.ViewHolder(view) {
        val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        val card: MaterialCardView = view.findViewById(R.id.cardBubble)
    }

    override fun getItemViewType(position: Int): Int {
        return if (messages[position].role == "user") TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble_user, parent, false)
            UserViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_bubble_bot, parent, false)
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = messages[position]
        when (holder) {
            is UserViewHolder -> holder.tvMessage.text = item.content
            is BotViewHolder -> {
                holder.tvMessage.text = item.content
                if (item.isError) {
                    holder.card.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                    holder.tvMessage.setTextColor(Color.parseColor("#EF4444"))
                } else {
                    holder.card.setCardBackgroundColor(Color.parseColor("#F1F5F9"))
                    holder.tvMessage.setTextColor(Color.parseColor("#0D1B3E"))
                }
            }
        }
    }

    override fun getItemCount() = messages.size

    fun addMessage(message: ChatMessageModel) {
        messages.add(message)
        notifyItemInserted(messages.size - 1)
    }

    fun removeLastIfTyping() {
        if (messages.isNotEmpty() && messages.last().content == "…") {
            messages.removeAt(messages.size - 1)
            notifyItemRemoved(messages.size)
        }
    }
}
