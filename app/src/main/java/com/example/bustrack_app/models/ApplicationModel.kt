package com.example.bustrack_app.models

import java.io.Serializable

data class ApplicationModel(
    val id: Int = 0,
    val studentName: String = "",
    val studentClass: String = "",
    val pickupPoint: String = "",
    val contactNumber: String = "",
    val time: String = "",
    val status: String = "",
    val image: Int = 0,
    val profileImageUrl: String = "",
    val regNo: String = "BT-8821",
    val parentName: String = "Rajesh Sharma",
    val bestRoute: String = "Route-01",
    val matchPercent: String = "95%",
    val nearestStop: String = "North Gate",
    val distance: String = "0.8km away",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val studentIdString: String = "",
    val assignedBus: String = "",
    val assignedDriver: String = "",
    val routeCode: String = ""
) : Serializable