package com.example.bustrack_app.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.models.TermsConditionModel

class TermsAdapter(private val termsList: List<TermsConditionModel>) :
    RecyclerView.Adapter<TermsAdapter.TermsViewHolder>() {

    class TermsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvPoint: TextView = itemView.findViewById(R.id.tvPoint)
        val tvText: TextView = itemView.findViewById(R.id.tvText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TermsViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_terms_condition, parent, false)
        return TermsViewHolder(view)
    }

    override fun onBindViewHolder(holder: TermsViewHolder, position: Int) {
        val item = termsList[position]
        holder.tvPoint.text = item.pointNumber
        holder.tvText.text = item.termsText
    }

    override fun getItemCount(): Int = termsList.size
}