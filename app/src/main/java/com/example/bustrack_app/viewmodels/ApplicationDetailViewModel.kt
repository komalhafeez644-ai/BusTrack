package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.ApplicationDetailModel

class ApplicationDetailViewModel : ViewModel() {

    private val _applicationDetail = MutableLiveData<ApplicationDetailModel>()
    val applicationDetail: LiveData<ApplicationDetailModel> = _applicationDetail

    fun loadApplicationDetail() {

        _applicationDetail.value = ApplicationDetailModel(
            applicationId = "#APP-2025-001",
            status = "Pending",
            dateTime = "Today, 08:45 AM",

            studentName = "Aryan Sharma",
            studentInfo = "Grade 10 - Section B",
            parentName = "Parent: Rajesh Sharma",
            phone = "+91 98765 43210",

            pickupAddress = "C-42, Sector 15",
            city = "Vasundhara Enclave, New Delhi",

            routeName = "Route 5 (Express)",
            distance = "1.2 KM",
            nearestStop = "Green Park Phase 2 (0.8 KM away)"
        )
    }

    fun approveApplication() {

    }

    fun rejectApplication() {

    }
}