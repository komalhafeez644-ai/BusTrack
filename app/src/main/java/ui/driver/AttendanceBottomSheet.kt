package ui.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.bustrack_app.R
import com.example.bustrack_app.databinding.CampustransitBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import utils.ViewUtils

class AttendanceBottomSheet : BottomSheetDialogFragment() {

    private var _binding: CampustransitBinding? = null
    private val binding get() = _binding!!
    private var stopName: String = ""
    private var routeName: String = ""

    // Track attendance state for students
    private var student1Status = "Pending"
    private var student2Status = "Pending"
    
    private var student1: com.example.bustrack_app.models.StudentModel? = null
    private var student2: com.example.bustrack_app.models.StudentModel? = null

    companion object {
        fun newInstance(stopName: String, routeName: String): AttendanceBottomSheet {
            val fragment = AttendanceBottomSheet()
            val args = Bundle()
            args.putString("STOP_NAME", stopName)
            args.putString("ROUTE_NAME", routeName)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentBottomSheetDialog)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = CampustransitBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        stopName = arguments?.getString("STOP_NAME") ?: ""
        routeName = arguments?.getString("ROUTE_NAME") ?: ""

        // UI Adjustments for BottomSheet mode
        binding.mapBg.visibility = View.GONE
        binding.headerCard.visibility = View.GONE
        binding.navCard.visibility = View.GONE
        
        // Fix layout constraints to prevent zero height
        val params = binding.manifestCard.layoutParams as androidx.constraintlayout.widget.ConstraintLayout.LayoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT
        params.topMargin = 0
        params.bottomToTop = -1 // Clear old constraint
        params.bottomToBottom = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID // Pin to bottom
        binding.manifestCard.layoutParams = params
        
        binding.manifestCard.cardElevation = 0f

        binding.tvCurrentStopName.text = stopName

        setupAttendanceListeners()
        loadStudentsAtStop()

        binding.btnSkip.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            Toast.makeText(context, "Attendance skipped for $stopName", Toast.LENGTH_SHORT).show()
            dismiss()
        }

        binding.btnSaveAttendance.setOnClickListener {
            ViewUtils.applyClickEffect(it)
            saveAllAttendance()
        }
    }

    private fun loadStudentsAtStop() {
        if (routeName.isEmpty()) return

        com.example.bustrack_app.data.FirebaseRepository.fetchStudentsByStop(routeName, stopName) { students ->
            if (students.isEmpty()) {
                binding.cardS1.visibility = View.GONE
                binding.cardS2.visibility = View.GONE
                return@fetchStudentsByStop
            }

            // Student 1
            student1 = students.getOrNull(0)
            if (student1 != null) {
                binding.cardS1.visibility = View.VISIBLE
                binding.tvStudentNameS1.text = student1?.name
                binding.tvStudentDetailsS1.text = "ID: ${student1?.id} • ${student1?.grade}"
                
                if (!student1!!.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(student1!!.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(binding.ivStudentS1)
                } else {
                    binding.ivStudentS1.setImageResource(R.drawable.ic_person)
                }
            } else {
                binding.cardS1.visibility = View.GONE
            }

            // Student 2
            student2 = students.getOrNull(1)
            if (student2 != null) {
                binding.cardS2.visibility = View.VISIBLE
                binding.tvStudentNameS2.text = student2?.name
                binding.tvStudentDetailsS2.text = "ID: ${student2?.id} • ${student2?.grade}"
                
                if (!student2!!.profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this).load(student2!!.profileImageUrl).placeholder(R.drawable.ic_person).circleCrop().into(binding.ivStudentS2)
                } else {
                    binding.ivStudentS2.setImageResource(R.drawable.ic_person)
                }
            } else {
                binding.cardS2.visibility = View.GONE
            }
        }
    }

    private fun setupAttendanceListeners() {
        // Student 1 Logic
        binding.btnPresentS1.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            updateAttendanceUI(1, "Present") 
        }
        binding.btnAbsentS1.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            updateAttendanceUI(1, "Absent") 
        }
        binding.btnEditS1.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            updateAttendanceUI(1, "Pending") 
        }

        // Student 2 Logic
        binding.btnPresentS2.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            updateAttendanceUI(2, "Present") 
        }
        binding.btnAbsentS2.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            updateAttendanceUI(2, "Absent") 
        }
        binding.btnEditS2.setOnClickListener { 
            ViewUtils.applyClickEffect(it)
            updateAttendanceUI(2, "Pending") 
        }
    }

    private fun updateAttendanceUI(studentIndex: Int, status: String) {
        if (studentIndex == 1) student1Status = status else student2Status = status

        val layoutMark = if (studentIndex == 1) binding.layoutMarkS1 else binding.layoutMarkS2
        val layoutStatus = if (studentIndex == 1) binding.layoutStatusS1 else binding.layoutStatusS2
        val tvBadge = if (studentIndex == 1) binding.tvStatusBadgeS1 else binding.tvStatusBadgeS2
        val card = if (studentIndex == 1) binding.cardS1 else binding.cardS2

        if (status == "Pending") {
            layoutMark.visibility = View.VISIBLE
            layoutStatus.visibility = View.GONE
            card.setCardBackgroundColor(android.graphics.Color.WHITE)
            card.strokeColor = android.graphics.Color.parseColor("#F1F5F9")
        } else {
            layoutMark.visibility = View.GONE
            layoutStatus.visibility = View.VISIBLE
            tvBadge.text = status.uppercase()

            if (status == "Present") {
                tvBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#DCFCE7"))
                tvBadge.setTextColor(android.graphics.Color.parseColor("#10B981"))
                card.setCardBackgroundColor(android.graphics.Color.parseColor("#F0FDF4"))
                card.strokeColor = android.graphics.Color.parseColor("#BBF7D0")
            } else {
                tvBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEE2E2"))
                tvBadge.setTextColor(android.graphics.Color.parseColor("#EF4444"))
                card.setCardBackgroundColor(android.graphics.Color.parseColor("#FEF2F2"))
                card.strokeColor = android.graphics.Color.parseColor("#FECACA")
            }
        }
    }

    private fun saveAllAttendance() {
        val date = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        val recordsToSave = mutableListOf<com.example.bustrack_app.models.AttendanceRecordModel>()

        student1?.let { student ->
            if (student1Status != "Pending") {
                recordsToSave.add(com.example.bustrack_app.models.AttendanceRecordModel(
                    studentId = student.id,
                    studentName = student.name,
                    route = routeName,
                    stop = stopName,
                    morningPickup = student1Status,
                    morningDrop = "--",
                    eveningPickup = "--",
                    eveningDrop = "--",
                    date = date
                ))
            }
        }

        student2?.let { student ->
            if (student2Status != "Pending") {
                recordsToSave.add(com.example.bustrack_app.models.AttendanceRecordModel(
                    studentId = student.id,
                    studentName = student.name,
                    route = routeName,
                    stop = stopName,
                    morningPickup = student2Status,
                    morningDrop = "--",
                    eveningPickup = "--",
                    eveningDrop = "--",
                    date = date
                ))
            }
        }

        if (recordsToSave.isEmpty()) {
            Toast.makeText(context, "No attendance marked to save", Toast.LENGTH_SHORT).show()
            return
        }

        var savedCount = 0
        recordsToSave.forEach { record ->
            com.example.bustrack_app.data.FirebaseRepository.saveAttendance(record) { success ->
                if (success) {
                    savedCount++
                    if (savedCount == recordsToSave.size) {
                        Toast.makeText(context, "Morning attendance saved for $savedCount students", Toast.LENGTH_LONG).show()
                        dismiss()
                    }
                } else {
                    Toast.makeText(context, "Failed to save attendance for ${record.studentName}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}