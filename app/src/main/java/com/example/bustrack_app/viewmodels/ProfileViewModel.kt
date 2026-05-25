package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.AdminModel

class ProfileViewModel : ViewModel() {

    private val _adminData = MutableLiveData<AdminModel>()
    val adminData: LiveData<AdminModel> get() = _adminData

    init {
        loadAdminProfile()
    }

    private fun loadAdminProfile() {
        val currentAdmin = AdminModel(
            fullName = "Komal Hafeez",
            email = "admin.komal@pjc.edu.pk",
            department = "Logistics & Transport",
            employeeId = "BT-ADM-2026",
            campusName = "Punjab College",
            isBusDelayNotifyEnabled = true,
            isEmergencyNotifyEnabled = true,
            isDriverNotifyEnabled = false // Naya field
        )
        _adminData.value = currentAdmin
    }

    fun updateBusDelayNotification(isEnabled: Boolean) {
        _adminData.value?.let { it.isBusDelayNotifyEnabled = isEnabled }
    }

    fun updateEmergencyNotification(isEnabled: Boolean) {
        _adminData.value?.let { it.isEmergencyNotifyEnabled = isEnabled }
    }

    fun updateDriverNotification(isEnabled: Boolean) {
        _adminData.value?.let { it.isDriverNotifyEnabled = isEnabled }
    }
}