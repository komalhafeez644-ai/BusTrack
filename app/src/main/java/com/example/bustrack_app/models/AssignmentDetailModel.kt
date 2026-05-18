package com.example.bustrack_app.models
data class AssignmentDetailModel(
    val studentName: String,
    val studentId: String,
    val status: String, // e.g., "Success"
    val isAssigned: Boolean,
    val isRouteOptimized: Boolean,
    val busNumber: String,
    val busServiceType: String, // e.g., "Inter-Campus Express"
    val driverName: String,
    val driverRole: String, // e.g., "Senior Operative"
    val pickupStop: String,
    val stopLocationDetail: String, // e.g., "Main Gate Entry"
    val routeCoverage: Int,
    val optimizationNote: String,
    val estimatedPickup: String


)
