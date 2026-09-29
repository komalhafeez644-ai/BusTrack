package ui.admin

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.widget.ArrayAdapter
import android.view.View
import android.view.ViewGroup
import android.widget.AutoCompleteTextView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.bustrack_app.R
import com.example.bustrack_app.data.BusRepository
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.databinding.ActivityEditAssignmentBinding
import com.example.bustrack_app.models.ApplicationModel

class EditAssignmentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditAssignmentBinding
    private var applicationData: ApplicationModel? = null
    private var routeNames: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditAssignmentBinding.inflate(layoutInflater)
        setContentView(binding.root)
        applicationData = intent.getSerializableExtra("APPLICATION_DATA") as? ApplicationModel

        applicationData?.let(::populateData)
        configureDropdownAppearance()
        observeRoutes()

        binding.btnUpdate.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            saveChanges()
        }
        binding.btnCancel.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }
        binding.btnBack.setOnClickListener {
            utils.ViewUtils.applyClickEffect(it)
            finish()
        }
    }

    private fun populateData(item: ApplicationModel) = with(binding) {
        tvStudentName.text = item.studentName
        tvStudentDetails.text = "Roll Number: ${item.studentIdString.ifBlank { "Not set" }} • ${item.studentClass}"
        utils.ImageUtils.loadProfileImage(this@EditAssignmentActivity, item.profileImageUrl, ivStudent)
        tvBusNumber.text = item.bestRoute
        tvStopName.text = item.nearestStop
        tvArrivalTime.text = "07:30 AM"
        spinnerRoute.setText(item.bestRoute, false)
        spinnerBus.setText(item.assignedBus.ifBlank {
            RouteRepository.getBusForRoute(item.bestRoute).ifBlank { "No Bus Assigned" }
        }, false)
        // pickupPoint is the student's residential address; the stop assignment is nearestStop.
        etPickupStop.setText(item.nearestStop, false)
    }

    private fun configureDropdownAppearance() = with(binding) {
        val white = ColorDrawable(getColor(R.color.white))
        spinnerRoute.setDropDownBackgroundDrawable(white)
        spinnerBus.setDropDownBackgroundDrawable(white)
        etPickupStop.setDropDownBackgroundDrawable(white)
        spinnerBus.isEnabled = false
        inputBus.isEnabled = false
    }

    private fun observeRoutes() {
        RouteRepository.routeList.observe(this) { routes ->
            val currentName = binding.spinnerRoute.text.toString().trim()
            routeNames = routes
                .filter { it.status.equals("ACTIVE", ignoreCase = true) || it.routeName == applicationData?.bestRoute }
                .map { it.routeName }
                .filter(String::isNotBlank)
                .distinct()
            binding.spinnerRoute.setAdapter(createDropdownAdapter(routeNames, binding.spinnerRoute))

            val selectedName = routeNames.firstOrNull { it.equals(currentName, ignoreCase = true) }
                ?: routeNames.firstOrNull { it.equals(applicationData?.bestRoute, ignoreCase = true) }
            if (selectedName != null) {
                binding.spinnerRoute.setText(selectedName, false)
                val selectedStop = applicationData?.nearestStop
                    ?.takeIf { it.isNotBlank() && routes.find { route -> route.routeName == selectedName }
                        ?.stopsList?.any { stop -> stop.stopName == it } == true }
                updateStopsForRoute(selectedName, selectedStop)
                updateBusForRoute(selectedName)
            } else {
                binding.etPickupStop.setAdapter(createDropdownAdapter(emptyList(), binding.etPickupStop))
                binding.etPickupStop.setText("", false)
                binding.spinnerBus.setText("No Bus Assigned", false)
            }

            binding.spinnerRoute.setOnItemClickListener { parent, _, position, _ ->
                val selectedRouteName = parent.getItemAtPosition(position).toString()
                binding.spinnerRoute.setText(selectedRouteName, false)
                updateBusForRoute(selectedRouteName)
                // Route changes discard the previous route's stop immediately.
                binding.etPickupStop.setText("", false)
                updateStopsForRoute(selectedRouteName, null)
            }
        }
        BusRepository.busList.observe(this) {
            val selectedRouteName = binding.spinnerRoute.text.toString().trim()
            if (selectedRouteName.isNotBlank()) updateBusForRoute(selectedRouteName)
        }
    }

    private fun updateBusForRoute(routeName: String) {
        val routeBusNumber = RouteRepository.getBusForRoute(routeName)
        val busExists = BusRepository.busList.value.orEmpty().any { it.busNumber == routeBusNumber }
        val assignedBus = routeBusNumber.takeIf { it.isNotBlank() && busExists }
        binding.spinnerBus.setText(assignedBus ?: "No Bus Assigned", false)
    }

    private fun updateStopsForRoute(routeName: String, preferredStop: String?) {
        val route = RouteRepository.routeList.value.orEmpty()
            .firstOrNull { it.routeName.equals(routeName, ignoreCase = true) }
        val stops = route?.stopsList.orEmpty().map { it.stopName.trim() }
            .filter(String::isNotBlank).distinct()
        binding.etPickupStop.setAdapter(createDropdownAdapter(stops, binding.etPickupStop))
        binding.etPickupStop.setText(preferredStop?.takeIf(stops::contains).orEmpty(), false)
        binding.etPickupStop.isEnabled = stops.isNotEmpty()
        if (stops.isEmpty()) binding.etPickupStop.hint = "No Stops Available"
        else binding.etPickupStop.hint = "Pickup Stop"
    }

    private fun createDropdownAdapter(
        items: List<String>,
        selectedView: AutoCompleteTextView
    ): ArrayAdapter<String> = object : ArrayAdapter<String>(this, R.layout.spinner_dropdown_item, items) {
        override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = (convertView as? TextView)
                ?: super.getDropDownView(position, null, parent) as TextView
            row.text = getItem(position).orEmpty()
            val isSelected = getItem(position).orEmpty()
                .equals(selectedView.text.toString(), ignoreCase = true)
            row.setBackgroundColor(getColor(if (isSelected) R.color.nav_selected_bg else R.color.white))
            row.setTextColor(getColor(R.color.primaryDark))
            return row
        }
    }.also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }

    private fun saveChanges() {
        val item = applicationData ?: run {
            Toast.makeText(this, "Assignment data is unavailable.", Toast.LENGTH_SHORT).show()
            return
        }
        val selectedRouteName = binding.spinnerRoute.text.toString().trim()
        val selectedRoute = RouteRepository.routeList.value.orEmpty()
            .firstOrNull { it.routeName.equals(selectedRouteName, ignoreCase = true) }
        val selectedStopName = binding.etPickupStop.text.toString().trim()
        if (selectedRoute == null || selectedRouteName !in routeNames) {
            Toast.makeText(this, "Select a valid route.", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedRoute.stopsList.none { it.stopName == selectedStopName }) {
            Toast.makeText(this, "Select a stop belonging to this route.", Toast.LENGTH_SHORT).show()
            return
        }

        val selectedBus = binding.spinnerBus.text.toString()
            .takeUnless { it.equals("No Bus Assigned", ignoreCase = true) }.orEmpty()
        val updatedApplication = item.copy(
            bestRoute = selectedRoute.routeName,
            routeCode = selectedRoute.routeCode,
            nearestStop = selectedStopName,
            assignedBus = selectedBus
        )
        binding.btnUpdate.isEnabled = false
        val onSaved: (Boolean) -> Unit = { success ->
            binding.btnUpdate.isEnabled = true
            if (success) {
                applicationData = updatedApplication
                val listIndex = BusApplicationsActivity.applicationsList.indexOfFirst { existing ->
                    (item.studentDocumentId.isNotBlank() && existing.studentDocumentId == item.studentDocumentId) ||
                        (item.studentIdString.isNotBlank() && existing.studentIdString.equals(item.studentIdString, true))
                }
                if (listIndex != -1) BusApplicationsActivity.applicationsList[listIndex] = updatedApplication

                FirebaseRepository.notifyParentsOfStudentAssignment(
                    studentDocumentId = item.studentDocumentId,
                    rollNumber = item.studentIdString,
                    studentName = item.studentName,
                    routeName = selectedRoute.routeName,
                    busNumber = selectedBus,
                    stopName = selectedStopName
                )
                setResult(RESULT_OK, android.content.Intent().putExtra("UPDATED_APPLICATION", updatedApplication))
                showSuccessDialog()
            } else {
                Toast.makeText(this, "Assignment could not be saved. Check the student Roll Number and try again.", Toast.LENGTH_LONG).show()
            }
        }

        if (item.studentDocumentId.isNotBlank()) {
            StudentRepository.assignRouteToStudent(
                item.studentDocumentId, selectedRoute.routeName, selectedBus, selectedStopName, onSaved
            )
        } else {
            StudentRepository.assignRouteToStudentByRollNumber(
                item.studentIdString, selectedRoute.routeName, selectedBus, selectedStopName, onSaved
            )
        }
    }

    private fun showSuccessDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.dialog_success)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnDone)
            .setOnClickListener {
                dialog.dismiss()
                finish()
            }
        dialog.show()
    }
}
