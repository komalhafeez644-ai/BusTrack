package com.example.bustrack_app.models

// Naya simple data class stop ke data ke liye
data class StopItem(
    val id: String,
    var stopName: String,
    var time: String,
    val latitude: Double,
    val longitude: Double
)

data class RouteModel(
    val id: String,
    val routeCode: String,
    val routeName: String,
    var status: String, // "ACTIVE", "PARTIAL", "INACTIVE"
    val busNo: String,
    val driverName: String,
    val stopsCount: Int,
    val studentsCount: Int,
    // 👇 Yeh line humne add ki hai taake is route ke saare stops ismein save hon
    var stopsList: MutableList<StopItem> = mutableListOf()
)