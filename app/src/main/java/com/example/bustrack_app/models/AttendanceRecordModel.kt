package com.example.bustrack_app.models

data class AttendanceRecordModel(
    val studentName: String,
    val rollNo: String,
    val busStop: String,
    val arrivalTime: String,
    val status: String,
    val route: String,
    val date: String
)