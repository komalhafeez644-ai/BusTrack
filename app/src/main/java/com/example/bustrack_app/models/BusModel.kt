package com.example.bustrack_app.models

data class BusModel(
    val busNumber: String = "",
    val totalSeats: Int = 0,
    val driverName: String? = null,  // Nullable for UNASSIGNED state
    val routeName: String? = null,   // Nullable for UNASSIGNED state
    val status: String = ""        // Exact matching values: "ACTIVE", "MAINTENANCE", "UNASSIGNED"
)