package com.example.bustrack_app.models

data class PrincipalModel(
    val id: String = "",
    val fullName: String = "",
    val email: String = "",
    val employeeId: String = "",
    val campusName: String = "",
    val phone: String = "",
    val address: String = "",
    val profileImageUrl: String = "",
    var isBusDelayNotifyEnabled: Boolean = true,
    var isEmergencyNotifyEnabled: Boolean = true,
    var isDriverNotifyEnabled: Boolean = false
)