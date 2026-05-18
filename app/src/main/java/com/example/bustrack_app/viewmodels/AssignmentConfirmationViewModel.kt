package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.AssignmentDetailModel

class AssignmentConfirmationViewModel : ViewModel() {

    // Main data holder
    private val _assignmentDetail = MutableLiveData<AssignmentDetailModel>()
    val assignmentDetail: LiveData<AssignmentDetailModel> = _assignmentDetail

    // Button actions ke liye status (Events)
    private val _actionStatus = MutableLiveData<String>()
    val actionStatus: LiveData<String> = _actionStatus

    fun loadAssignmentData() {
        // Screenshot ke mutabiq exact data load karna
        _assignmentDetail.value = AssignmentDetailModel(
            studentName = "Ethan Sterling",
            studentId = "#CF-99281",
            status = "Successfully Assigned", // Updated as per image
            isAssigned = true,
            isRouteOptimized = true,
            busNumber = "Bus 42",
            busServiceType = "Inter-Campus Express",
            driverName = "Ahmed Ali",
            driverRole = "Senior Operative",
            pickupStop = "Sector 15 North",
            stopLocationDetail = "Main Gate Entry",
            routeCoverage = 98,
            optimizationNote = "The assignment was calculated based on proximity to Sector 15 and current bus occupancy. Bus 42 provides the minimal latency for the 8:00 AM window.",
            estimatedPickup = "07:45 AM"
        )
    }

    // "Confirm & Notify Parent" button ka logic
    fun confirmAndNotify() {
        // Yahan aap API call kar sakte hain. Filhal hum status update kar rahe hain.
        _actionStatus.value = "Notification sent to Parent successfully!"
    }

    // "Edit Assignment" button ka logic
    fun editAssignment() {
        _actionStatus.value = "Opening Edit Mode..."
    }
}