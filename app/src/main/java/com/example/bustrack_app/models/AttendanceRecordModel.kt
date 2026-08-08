package com.example.bustrack_app.models

data class AttendanceRecordModel(
    val studentId: String = "",
    val studentName: String = "",
    val route: String = "",
    val stop: String = "",
    val morningPickup: String = "",
    val morningDrop: String = "",
    val eveningPickup: String = "",
    val eveningDrop: String = "",
    val date: String = ""
)