package com.example.bustrack_app.models
data class AssignmentDetailModel(
    val studentName: String,
    val studentId: String,
    val status: String,
    val isAssigned: Boolean,
    val isRouteOptimized: Boolean,
    val busNumber: String,
    val routeName: String,
    val pickupStop: String,
    val routeCoverage: Int,
    val optimizationNote: String,
    val estimatedPickup: String
)