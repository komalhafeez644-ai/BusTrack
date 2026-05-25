package com.example.bustrack_app.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.ApplicationModel
import com.google.android.material.card.MaterialCardView

class ApplicationAdapter(
    private var list: List<ApplicationModel>,
    private val onClick: (ApplicationModel) -> Unit
) : RecyclerView.Adapter<ApplicationAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtName: TextView = itemView.findViewById(R.id.txtName)
        val txtClass: TextView = itemView.findViewById(R.id.txtClass)
        val txtPickup: TextView = itemView.findViewById(R.id.txtPickup)
        val txtRoute: TextView = itemView.findViewById(R.id.txtRoute)
        val txtStatus: TextView = itemView.findViewById(R.id.txtStatus)
        val statusBadge: MaterialCardView = itemView.findViewById(R.id.statusBadge)
        val statusDot: View = itemView.findViewById(R.id.statusDot)
        val btnView: Button = itemView.findViewById(R.id.btnView)
        val imgStudent: android.widget.ImageView = itemView.findViewById(R.id.imgStudent)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_application, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    fun updateList(newList: List<ApplicationModel>) {
        list = newList
        notifyDataSetChanged()
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.txtName.text = item.studentName
        holder.txtClass.text = item.studentClass
        holder.txtPickup.text = item.pickupPoint
        holder.txtRoute.text = item.contactNumber
        holder.txtStatus.text = item.status

        // Status Color and Button Text Logic
        if (item.status == "Approved") {
            holder.statusBadge.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
            holder.statusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4CAF50"))
            holder.txtStatus.setTextColor(Color.parseColor("#2E7D32"))
            holder.btnView.text = "View Detail"
        } else {
            // Default Pending (Yellow)
            holder.statusBadge.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
            holder.statusDot.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FBC02D"))
            holder.txtStatus.setTextColor(Color.parseColor("#5D4037"))
            holder.btnView.text = "View Analytic"
        }

        holder.btnView.setOnClickListener {
            val originalBg = holder.btnView.backgroundTintList
            val originalText = holder.btnView.textColors

            // 1. Color change to Primary Dark
            holder.btnView.backgroundTintList = androidx.core.content.ContextCompat.getColorStateList(it.context, R.color.primaryDark)
            holder.btnView.setTextColor(Color.WHITE)

            // 2. Push Effect (In and Out)
            it.animate()
                .scaleX(0.92f)
                .scaleY(0.92f)
                .setDuration(100)
                .withEndAction {
                    it.animate().scaleX(1f).scaleY(1f).setDuration(100).withEndAction {
                        // 3. Revert back to original style
                        holder.btnView.backgroundTintList = originalBg
                        holder.btnView.setTextColor(originalText)
                        
                        onClick(item)
                    }.start()
                }
                .start()
        }

        // Display image
        // holder.imgStudent.setImageResource(item.image)
        // If image is a resource ID in item.image
        if (item.image != 0) {
            holder.imgStudent.setImageResource(item.image)
        }
    }
}
