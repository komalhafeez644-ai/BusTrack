package ui.driver

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.databinding.CampustransitBinding

class TransitManifestActivity : AppCompatActivity() {
    private lateinit var binding: CampustransitBinding
    private var stopName: String = "Unknown Stop"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = CampustransitBinding.inflate(layoutInflater)
        setContentView(binding.root)

        stopName = intent.getStringExtra("STOP_NAME") ?: "Current Stop"
        
        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.tvCurrentStopName.text = stopName
    }

    private fun setupListeners() {
        binding.headerCard.setOnClickListener { finish() }
        
        // Student 1 Logic
        binding.btnPresentS1.setOnClickListener { updateAttendanceUI(1, "Present") }
        binding.btnAbsentS1.setOnClickListener { updateAttendanceUI(1, "Absent") }
        binding.btnEditS1.setOnClickListener { updateAttendanceUI(1, "Pending") }

        // Student 2 Logic
        binding.btnPresentS2.setOnClickListener { updateAttendanceUI(2, "Present") }
        binding.btnAbsentS2.setOnClickListener { updateAttendanceUI(2, "Absent") }
        binding.btnEditS2.setOnClickListener { updateAttendanceUI(2, "Pending") }

        binding.btnSaveAttendance.setOnClickListener {
            val date = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault()).format(java.util.Date())
            
            // Mock saving attendance for one student
            val record = com.example.bustrack_app.models.AttendanceRecordModel(
                studentId = "STU_101",
                studentName = "Ali Hamza",
                route = "Route 1",
                stop = stopName,
                morningPickup = "Present",
                morningDrop = "--",
                eveningPickup = "--",
                eveningDrop = "--",
                date = date
            )
            
            com.example.bustrack_app.data.FirebaseRepository.saveAttendance(record) { success ->
                if (success) {
                    Toast.makeText(this, "Attendance saved for $stopName", Toast.LENGTH_LONG).show()
                    finish()
                } else {
                    Toast.makeText(this, "Failed to sync attendance", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateAttendanceUI(studentIndex: Int, status: String) {
        val layoutMark = if (studentIndex == 1) binding.layoutMarkS1 else binding.layoutMarkS2
        val layoutStatus = if (studentIndex == 1) binding.layoutStatusS1 else binding.layoutStatusS2
        val tvBadge = if (studentIndex == 1) binding.tvStatusBadgeS1 else binding.tvStatusBadgeS2
        val card = if (studentIndex == 1) binding.cardS1 else binding.cardS2

        if (status == "Pending") {
            layoutMark.visibility = android.view.View.VISIBLE
            layoutStatus.visibility = android.view.View.GONE
            card.setCardBackgroundColor(android.graphics.Color.WHITE)
            card.strokeColor = android.graphics.Color.parseColor("#F1F5F9")
        } else {
            layoutMark.visibility = android.view.View.GONE
            layoutStatus.visibility = android.view.View.VISIBLE
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
}
