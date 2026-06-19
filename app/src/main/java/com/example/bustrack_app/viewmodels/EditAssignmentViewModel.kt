package com.example.bustrack_app.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.EditAssignmentModel

class EditAssignmentViewModel : ViewModel() {

    private val _editData = MutableLiveData<EditAssignmentModel>()
    val editData: LiveData<EditAssignmentModel> = _editData

    fun loadCurrentAssignment() {
        _editData.value = EditAssignmentModel(
            studentName = "Ethan Sterling",
            studentId = "#CF-9281",
            studentYear = "Senior Year",
            status = "Active",
            currentBus = "Bus 42",
            currentStop = "Sector 15 North",
            currentArrivalTime = "07:45 AM"
        )
    }

    fun updateAndNotify() {
        // Logic for Update & Notify button
    }

    fun cancelChanges() {
        // Logic for Cancel button
    }
}