package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.StudentModel

class StudentDetailsViewModel : ViewModel() {

    private val _studentDetails = MutableLiveData<StudentModel>()
    val studentDetails: LiveData<StudentModel> get() = _studentDetails

    fun loadStudentDetails(studentId: String?) {
        if (studentId.isNullOrBlank()) return

        // Do not depend on StudentRepository's snapshot cache here. A tracking request can
        // open this screen before that listener receives its first snapshot, which used to
        // make a valid student appear missing on the first attempt. This is the same
        // Firestore student document (and profileImageUrl) used by the approval flow.
        FirebaseRepository.fetchStudentById(studentId) { student ->
            student?.let { _studentDetails.postValue(it) }
        }
    }
}
