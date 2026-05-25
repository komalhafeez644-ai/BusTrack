package com.example.bustrack_app.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ItemStudentBinding
import com.example.bustrack_app.models.StudentModel

class StudentAdapter(
    private var students: List<StudentModel> = listOf(),
    private val onAssignClick: (StudentModel) -> Unit,
    private val onEditClick: (StudentModel) -> Unit
) : RecyclerView.Adapter<StudentAdapter.StudentViewHolder>() {

    fun setStudents(newList: List<StudentModel>) {
        this.students = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StudentViewHolder {
        // Aapki XML file 'item_student.xml' hai, isliye ItemStudentBinding use hoga
        val itemBinding = ItemStudentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return StudentViewHolder(itemBinding)
    }

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) = holder.bind(students[position])
    override fun getItemCount(): Int = students.size

    inner class StudentViewHolder(private val itemBinding: ItemStudentBinding) : RecyclerView.ViewHolder(itemBinding.root) {
        fun bind(student: StudentModel) {
            // Basic Details
            itemBinding.txtStudentName.text = student.name
            itemBinding.txtStudentDetail.text = "Grade ${student.grade} • ${student.id}"

            val route = student.route ?: ""

            if (route.isEmpty()) {
                // --- CASE 1: UNASSIGNED (Image ka pehla card) ---
                itemBinding.statusBadge.text = "● UNASSIGNED"
                itemBinding.statusBadge.setBackgroundResource(R.drawable.bg_status_badge_red) //
                itemBinding.statusBadge.setTextColor(Color.parseColor("#E57373"))

                // Show Location, Hide Bus Info
                itemBinding.icLocation.visibility = View.VISIBLE
                itemBinding.txtLocationInfo.visibility = View.VISIBLE
                itemBinding.txtLocationInfo.text = student.location

                itemBinding.icBus.visibility = View.GONE
                itemBinding.txtBusInfo.visibility = View.GONE

                // Button Action
                itemBinding.btnAction.text = "Assign Route"
                itemBinding.btnAction.setOnClickListener { onAssignClick(student) }

            } else {
                // --- CASE 2: ASSIGNED (Image ka doosra card) ---
                // 1. Status Badge par Route ka naam aayega
                itemBinding.statusBadge.text = "● ${route.uppercase()}"
                itemBinding.statusBadge.setBackgroundResource(R.drawable.bg_filter_row) // Blue background
                itemBinding.statusBadge.setTextColor(Color.parseColor("#1E88E5"))

                // 2. Location Hide karein aur Bus Info Show karein
                itemBinding.icLocation.visibility = View.GONE
                itemBinding.txtLocationInfo.visibility = View.GONE

                itemBinding.icBus.visibility = View.VISIBLE
                itemBinding.txtBusInfo.visibility = View.VISIBLE
                itemBinding.txtBusInfo.text = "Bus #102" // Yahan student model se bus info dein

                // 3. Button change to View Details
                itemBinding.btnAction.text = "View Details"
                itemBinding.btnAction.setOnClickListener { onEditClick(student) }
            }

            // Photo vs Initials Logic (Aapki Driver Adapter wali same logic)
            if (student.profileImage == 0) {
                itemBinding.imgStudent.visibility = View.GONE
                itemBinding.txtAvatar.visibility = View.VISIBLE
                val initials = student.name.split(" ").filter { it.isNotEmpty() }.map { it[0] }.take(2).joinToString("")
                itemBinding.txtAvatar.text = initials.uppercase()
            } else {
                itemBinding.imgStudent.visibility = View.VISIBLE
                itemBinding.txtAvatar.visibility = View.GONE
                itemBinding.imgStudent.setImageResource(student.profileImage)
            }
        }
    }
}