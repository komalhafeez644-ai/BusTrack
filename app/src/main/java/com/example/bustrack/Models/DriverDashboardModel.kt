package com.example.bustrack.Models

data class DriverDashboardModel(
    val driverName: String,
    val busNumber: String,
    val currentRoute: String,
    val plateNumber: String,
    val stopsAssigned: Int,
    val stopsCount: String,
    val studentsCount: String,
    val tripTime: String,
    val tripDistance: String,
    val isOnDuty: Boolean = true
)
