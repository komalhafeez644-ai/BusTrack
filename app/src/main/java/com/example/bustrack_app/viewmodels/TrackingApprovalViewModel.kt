package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.ParentModel
import com.example.bustrack_app.models.StudentModel

class TrackingApprovalViewModel : ViewModel() {

    private val _parentData = MutableLiveData<ParentModel?>()
    val parentData: LiveData<ParentModel?> get() = _parentData

    private val _studentData = MutableLiveData<StudentModel?>()
    val studentData: LiveData<StudentModel?> get() = _studentData

    fun loadDetails(parentId: String, studentId: String) {
        FirebaseRepository.fetchParent(parentId) {
            _parentData.postValue(it)
        }
        
        FirebaseRepository.fetchStudentById(studentId) {
            _studentData.postValue(it)
        }
    }

    fun updateStatus(requestId: String, status: String, adminUid: String, onResult: (Boolean) -> Unit) {
        FirebaseRepository.updateTrackingRequestStatus(requestId, status, adminUid, onResult)
    }
}
