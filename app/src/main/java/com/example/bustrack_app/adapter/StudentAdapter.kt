package com.example.bustrack_app.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ItemStudentBinding
import com.example.bustrack_app.models.StudentModel

class StudentAdapter(
    private var students: List<StudentModel> = listOf(),
    private val showActionButtons: Boolean = true,
    private val onAssignClick: (StudentModel) -> Unit = {},
    private val onEditClick: (StudentModel) -> Unit = {}
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    fun setStudents(newList: List<StudentModel>) {
        this.students = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        val itemBinding = ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudentViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) = holder.bind(students[position])
    override fun getItemCount(): Int = students.size

    inner class StudentViewHolder(private val itemBinding: ItemStudentBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(student: StudentModel) {
            itemBinding.txtStudentName.text = student.name
            itemBinding.txtGrade.text = student.grade
            itemBinding.txtStudentId.text = "ID: ${student.id}"

            val route = student.route ?: ""

            // Toggle action button visibility based on flag
            itemBinding.btnAction.visibility = if (showActionButtons) View.VISIBLE else View.GONE

            if (route.isEmpty()) {
                itemBinding.statusBadge.text = "● UNASSIGNED"
                itemBinding.statusBadge.setBackgroundResource(R.drawable.bg_status_badge_red)
                itemBinding.statusBadge.backgroundTintList = null 
                itemBinding.statusBadge.setTextColor(Color.parseColor("#EF4444")) 

                itemBinding.layoutLocation.visibility = View.VISIBLE
                itemBinding.txtLocationInfo.text = student.location.ifEmpty { "Location not set" }
                itemBinding.layoutBus.visibility = View.GONE

                itemBinding.btnAction.text = "View Details"
                itemBinding.btnAction.setOnClickListener { onEditClick(student) }

            } else {
                val statusText = if (student.isActive) "ACTIVE" else "INACTIVE"
                val statusColor = if (student.isActive) "#22C55E" else "#64748B"
                
                itemBinding.statusBadge.text = "● $statusText"
                itemBinding.statusBadge.setBackgroundResource(R.drawable.bg_chip_selected)
                itemBinding.statusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                itemBinding.statusBadge.setTextColor(Color.parseColor(statusColor)) 

                itemBinding.layoutLocation.visibility = View.GONE
                itemBinding.layoutBus.visibility = View.VISIBLE
                itemBinding.txtBusInfo.text = "${route}: ${student.busNo ?: "No Bus"}"

                itemBinding.btnAction.text = "View Details"
                itemBinding.btnAction.setOnClickListener { onEditClick(student) }
            }

            // Image Loading Logic
            if (student.profileImageUrl.isNotEmpty()) {
                itemBinding.imgStudent.visibility = View.VISIBLE
                itemBinding.txtAvatar.visibility = View.GONE
                utils.ImageUtils.loadProfileImage(itemBinding.root.context, student.profileImageUrl, itemBinding.imgStudent)
            } else if (student.profileImage != 0) {
                itemBinding.imgStudent.visibility = View.VISIBLE
                itemBinding.txtAvatar.visibility = View.GONE
                itemBinding.imgStudent.setImageResource(student.profileImage)
            } else {
                itemBinding.imgStudent.visibility = View.GONE
                itemBinding.txtAvatar.visibility = View.VISIBLE
                val initials = student.name.split(" ").filter { it.isNotEmpty() }.map { it[0] }.take(2).joinToString("")
                itemBinding.txtAvatar.text = initials.uppercase()
            }
        }
    }
}