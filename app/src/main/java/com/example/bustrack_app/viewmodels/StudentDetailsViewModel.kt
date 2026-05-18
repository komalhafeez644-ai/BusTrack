package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.R
import com.example.bustrack_app.models.StudentModel

class StudentDetailsViewModel : ViewModel() {

    private val _studentDetails = MutableLiveData<StudentModel>()
    val studentDetails: LiveData<StudentModel> get() = _studentDetails

    fun loadStudentDetails(studentId: String?) {
        // Sahi fields ke saath mock data set kiya hai
        val mockData = StudentModel(
            id = studentId ?: "#ST-2045",
            name = "Ali Hassan",
            grade = "BS IT 7th semester",
            location = "Sector 15 North",
            route = "Route 12-A",
            busNo = "Bus 42",
            status = "ASSIGNED",
            profileImage = R.drawable.driver_image_one, // Check kar lein aapke paas yeh image ho
            fatherName = "Ahmed Khan",
            phoneNumber = "+92 300 1234567",
            pickupTime = "07:15 AM",
            insuranceStatus = "Active"
        )
        _studentDetails.value = mockData
    }
}