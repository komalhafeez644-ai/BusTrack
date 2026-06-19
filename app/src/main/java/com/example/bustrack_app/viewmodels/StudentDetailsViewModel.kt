package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.models.StudentModel

class StudentDetailsViewModel : ViewModel() {

    private val _studentDetails = MutableLiveData<StudentModel>()
    val studentDetails: LiveData<StudentModel> get() = _studentDetails

    fun loadStudentDetails(studentId: String?) {
        StudentRepository.studentList.value?.find { it.id == studentId }?.let {
            _studentDetails.value = it
        }
    }
}
