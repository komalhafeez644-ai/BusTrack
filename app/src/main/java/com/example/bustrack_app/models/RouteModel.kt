package com.example.bustrack_app.models

import java.io.Serializable

data class StopItem(
    val id: String = "",
    var stopName: String = "",
    var time: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable

data class LatLngModel(
    val latitude: Double,
    val longitude: Double
) : Serializable

data class RouteModel(
    val id: String = "",
    val routeCode: String = "",
    val routeName: String = "",
    var status: String = "ACTIVE",
    val busNo: String = "",
    val driverName: String = "",
    val stopsCount: Int = 0,
    val studentsCount: Int = 0,
    var stopsList: MutableList<StopItem> = mutableListOf(),
    var description: String = "",
    var startPoint: String = "",
    var endPoint: String = "",
    var pathPoints: MutableList<LatLngModel> = mutableListOf()
) : Serializable
