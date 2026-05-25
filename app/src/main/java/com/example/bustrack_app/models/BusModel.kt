package com.example.bustrack_app.models

data class BusModel(
    val busNumber: String,
    val totalSeats: Int,
    val driverName: String?,  // Nullable for UNASSIGNED state
    val routeName: String?,   // Nullable for UNASSIGNED state
    val status: String        // Exact matching values: "ACTIVE", "MAINTENANCE", "UNASSIGNED"
)