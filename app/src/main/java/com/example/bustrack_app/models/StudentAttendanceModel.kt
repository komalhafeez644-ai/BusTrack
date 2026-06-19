package com.example.bustrack_app.models

data class StudentAttendanceModel(
    val day: String,
    val date: String,
    val pickupStatus: String, // e.g., "Picked", "Missed", "Pending"
    val dropStatus: String,   // e.g., "Dropped", "In Bus", "Pending"
    val pickupTime: String = "--:--",
    val dropTime: String = "--:--"
)