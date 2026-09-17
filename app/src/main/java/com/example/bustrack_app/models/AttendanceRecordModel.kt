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
    val date: String = "",
    // Enhanced verification fields
    val busId: String = "",
    val routeId: String = "",
    val stopId: String = "",
    val stopName: String = "",
    val tripId: String = "",
    val tripDirection: String = "FORWARD",
    val attendanceType: String = "",
    val attendanceStatus: String = "",
    val timestamp: Long = 0L,
    val markedByDriverId: String = "",
    val markedByDriverName: String = "",
    val syncStatus: String = "SYNCED"
)