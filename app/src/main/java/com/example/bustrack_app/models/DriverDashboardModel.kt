package com.example.bustrack_app.models

data class DriverDashboardModel(
    val driverName: String,
    val busNumber: String,
    val currentRoute: String,
    val capacity: String,
    val stopsAssigned: Int,
    val stopsCount: String,
    val studentsCount: String,
    val tripTime: String,
    val tripDistance: String,
    val isOnDuty: Boolean = true
)