package ui.admin

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.TrackingRequestModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.ktx.Firebase

/**
 * Admin broadcast/announcement screen. Previously "Mock sending" only - now writes real
 * notifications via FirebaseRepository.sendNotification():
 *  - Level "All": role-broadcast to every parent or every driver (recipientRole).
 *  - Level "Individual": targets one specific parent (from real approved tracking
 *    requests) or driver (from DriverRepository), by their uid (recipientId).
 *  - Level "Group"/Route: targets every parent with approved tracking on that route,
 *    or every driver assigned to that route.
 */
class BroadcastNotificationActivity : AppCompatActivity() {

    private lateinit var tilSelect: TextInputLayout
    private lateinit var autoSelect: AutoCompleteTextView
    private lateinit var tilLevel: TextInputLayout
    private lateinit var autoLevel: AutoCompleteTextView
    private lateinit var etTitle: TextInputEditText
    private lateinit var etMessage: TextInputEditText
    private lateinit var toggleAudience: MaterialButtonToggleGroup

    // display name -> uid (individual) or route name (group)
    private var selectionMap: Map<String, String> = emptyMap()
    private var approvedParentRequests: List<TrackingRequestModel> = emptyList()
    private var driverListCache: List<DriverModel> = emptyList()
    private var routeListCache: List<RouteModel> = emptyList()

    private val selectionLevels = arrayOf("All", "Individual", "Group")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_broadcast_notification)

        supportActionBar?.hide()

        tilSelect = findViewById(R.id.tilSelectTarget)
        autoSelect = findViewById(R.id.autoCompleteSelect)
        tilLevel = findViewById(R.id.tilSelectionLevel)
        autoLevel = findViewById(R.id.autoCompleteLevel)
        etTitle = findViewById(R.id.etTitle)
        etMessage = findViewById(R.id.etMessage)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        toggleAudience = findViewById(R.id.toggleGroupAudience)
        val btnSend = findViewById<MaterialButton>(R.id.btnSendBroadcast)

        // Setup Selection Level Dropdown
        val levelAdapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, selectionLevels)
        autoLevel.setAdapter(levelAdapter)
        autoLevel.setOnItemClickListener { _, _, position, _ ->
            val selectedLevel = selectionLevels[position]
            handleLevelSelection(selectedLevel)
        }

        // Real data sources, loaded once so dropdowns reflect actual parents/drivers/routes.
        DriverRepository.driverList.observe(this) { driverListCache = it }
        RouteRepository.routeList.observe(this) { routeListCache = it }
        Firebase.firestore.collection("trackingRequests")
            .whereEqualTo("status", "APPROVED")
            .addSnapshotListener { snapshot, _ ->
                approvedParentRequests = snapshot?.documents?.mapNotNull { it.toObject<TrackingRequestModel>() } ?: emptyList()
            }

        btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }

        toggleAudience.addOnButtonCheckedListener { _, checkedId, isChecked ->
            autoSelect.setText("", false)
            if (isChecked) {
                val currentLevel = autoLevel.text.toString()
                if (currentLevel != "All") {
                    setupSelectionData(checkedId == R.id.btnTargetParents, currentLevel == "Group")
                }
            }
        }

        btnSend.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            sendBroadcast()
        }
    }

    private fun handleLevelSelection(level: String) {
        autoSelect.setText("", false)
        when (level) {
            "All" -> {
                tilSelect.visibility = View.GONE
            }
            "Individual" -> {
                tilSelect.visibility = View.VISIBLE
                tilSelect.hint = "Select Individual"
                setupSelectionData(toggleAudience.checkedButtonId == R.id.btnTargetParents)
            }
            "Group" -> {
                tilSelect.visibility = View.VISIBLE
                tilSelect.hint = "Select Group/Route"
                setupSelectionData(toggleAudience.checkedButtonId == R.id.btnTargetParents, true)
            }
        }
    }

    private fun setupSelectionData(isParents: Boolean, isGroup: Boolean = false) {
        val data: Map<String, String> = if (isGroup) {
            routeListCache.associate { it.routeName to it.routeName }
        } else if (isParents) {
            // One entry per approved tracking request - display parent name, value is their uid.
            approvedParentRequests.associate { "${it.parentName} (Student ${it.studentId})" to it.parentId }
        } else {
            driverListCache.associate { "${it.name} (${it.assignedBus ?: it.route ?: "Unassigned"})" to it.uid }
        }
        selectionMap = data

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, data.keys.toList())
        autoSelect.setAdapter(adapter)
    }

    private fun sendBroadcast() {
        val title = etTitle.text.toString().trim()
        val message = etMessage.text.toString().trim()
        val isParents = toggleAudience.checkedButtonId == R.id.btnTargetParents
        val role = if (isParents) "parent" else "driver"
        val selectedLevel = autoLevel.text.toString()

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Please fill in title and message", Toast.LENGTH_SHORT).show()
            return
        }

        when (selectedLevel) {
            "All" -> {
                FirebaseRepository.sendNotification(
                    recipientRole = role, title = title, message = message, type = "ADMIN_BROADCAST"
                ) { onSendComplete(it) }
            }

            "Individual" -> {
                val selectedText = autoSelect.text.toString()
                val uid = selectionMap[selectedText]
                if (uid.isNullOrBlank()) {
                    Toast.makeText(this, "Recipient identifier not found. The ${if (isParents) "parent" else "driver"} may need to login again.", Toast.LENGTH_LONG).show()
                    return
                }
                FirebaseRepository.sendNotification(
                    recipientId = uid, title = title, message = message, type = "ADMIN_BROADCAST"
                ) { onSendComplete(it) }
            }

            "Group" -> {
                val selectedText = autoSelect.text.toString()
                val route = selectionMap[selectedText]
                if (route.isNullOrBlank()) {
                    Toast.makeText(this, "Please select a valid group/route", Toast.LENGTH_SHORT).show()
                    return
                }
                
                val recipientUids: List<String> = if (isParents) {
                    approvedParentRequests.filter { it.assignedTrackingRoute == route }.map { it.parentId }
                } else {
                    driverListCache.filter { it.route == route }.mapNotNull { it.uid.ifBlank { null } }
                }.distinct() // Deduplicate recipient IDs

                if (recipientUids.isEmpty()) {
                    Toast.makeText(this, "No recipients with valid IDs found for route: $route", Toast.LENGTH_SHORT).show()
                    return
                }

                var remaining = recipientUids.size
                var anySuccess = false
                recipientUids.forEach { uid ->
                    FirebaseRepository.sendNotification(
                        recipientId = uid, title = title, message = message, type = "ADMIN_BROADCAST"
                    ) { success ->
                        remaining--
                        if (success) anySuccess = true
                        if (remaining == 0) onSendComplete(anySuccess)
                    }
                }
            }
        }
    }

    private fun onSendComplete(success: Boolean) {
        if (success) {
            Toast.makeText(this, "Broadcast Sent Successfully!", Toast.LENGTH_LONG).show()
            finish()
        } else {
            Toast.makeText(this, "Failed to send broadcast. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }
}
