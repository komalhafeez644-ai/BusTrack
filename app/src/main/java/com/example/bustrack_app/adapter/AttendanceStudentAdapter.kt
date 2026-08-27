package com.example.bustrack_app.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.ItemAttendanceStudentCardBinding
import com.example.bustrack_app.models.StudentModel
import utils.ViewUtils

class AttendanceStudentAdapter(
    private val students: List<StudentModel>,
    private val initialStatuses: Map<String, String> = emptyMap(),
    private val onAttendanceMarked: (StudentModel, String) -> Unit
) : RecyclerView.Adapter<AttendanceStudentAdapter.ViewHolder>() {

    private val studentStatuses = mutableMapOf<String, String>()

    init {
        // Pre-fill from any attendance already saved for this stop/period today, so the
        // driver sees and can edit what was previously marked instead of losing it.
        students.forEach { studentStatuses[it.id] = initialStatuses[it.id] ?: "Pending" }
    }

    fun getMarkedAttendance(): Map<String, String> = studentStatuses

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAttendanceStudentCardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(students[position])
    }

    override fun getItemCount(): Int = students.size

    inner class ViewHolder(private val binding: ItemAttendanceStudentCardBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(student: StudentModel) {
            binding.tvStudentName.text = student.name
            binding.tvStudentDetails.text = "ID: ${student.id} • ${student.grade}"

            if (!student.profileImageUrl.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(student.profileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivStudent)
            } else {
                binding.ivStudent.setImageResource(R.drawable.ic_person)
            }

            val status = studentStatuses[student.id] ?: "Pending"
            updateUI(status)

            binding.btnPresent.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateStatus(student, "Present")
            }

            binding.btnAbsent.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateStatus(student, "Absent")
            }

            binding.btnLeave.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateStatus(student, "Leave")
            }

            binding.btnEdit.setOnClickListener {
                ViewUtils.applyClickEffect(it)
                updateStatus(student, "Pending")
            }
        }

        private fun updateStatus(student: StudentModel, status: String) {
            studentStatuses[student.id] = status
            updateUI(status)
            onAttendanceMarked(student, status)
        }

        private fun updateUI(status: String) {
            if (status == "Pending") {
                binding.layoutMark.visibility = View.VISIBLE
                binding.layoutStatus.visibility = View.GONE
                binding.cardStudent.setCardBackgroundColor(Color.WHITE)
                binding.cardStudent.strokeColor = Color.parseColor("#F1F5F9")
            } else {
                binding.layoutMark.visibility = View.GONE
                binding.layoutStatus.visibility = View.VISIBLE
                binding.tvStatusBadge.text = status.uppercase()

                when (status) {
                    "Present" -> {
                        binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                        binding.tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
                        binding.cardStudent.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
                        binding.cardStudent.strokeColor = Color.parseColor("#BBF7D0")
                    }
                    "Leave" -> {
                        binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                        binding.tvStatusBadge.setTextColor(Color.parseColor("#D97706"))
                        binding.cardStudent.setCardBackgroundColor(Color.parseColor("#FFFBEB"))
                        binding.cardStudent.strokeColor = Color.parseColor("#FDE68A")
                    }
                    else -> {
                        binding.tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                        binding.tvStatusBadge.setTextColor(Color.parseColor("#EF4444"))
                        binding.cardStudent.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
                        binding.cardStudent.strokeColor = Color.parseColor("#FECACA")
                    }
                }
            }
        }
    }
}
