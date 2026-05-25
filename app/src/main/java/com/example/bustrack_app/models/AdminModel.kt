package com.example.bustrack_app.models

data class AdminModel(
    val fullName: String = "",
    val email: String = "",
    val department: String = "",
    val employeeId: String = "",
    val campusName: String = "",
    var isBusDelayNotifyEnabled: Boolean = true,
    var isEmergencyNotifyEnabled: Boolean = true,
    var isDriverNotifyEnabled: Boolean = false
)