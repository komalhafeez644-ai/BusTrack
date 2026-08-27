package com.example.bustrack_app.models

import com.google.firebase.firestore.DocumentId
import java.io.Serializable

data class DriverModel(
    @DocumentId
    val driverId: String = "",
    val id: String = "",
    var name: String = "",
    var status: String = "",
    var assignedBus: String? = null,
    var route: String? = null,
    var profileImage: Int = 0,
    var profileImageUrl: String = "",
    var cnic: String = "",
    var phone: String = "",
    var email: String = "",
    var latitude: Double = 0.0,
    var longitude: Double = 0.0,
    var speed: Double = 0.0,
    var eta: String = "On Way",
    var load: String = "0/0",
    var lastUpdated: Long = 0L,
    var currentRoutePolyline: String? = null,
    var traveledPolyline: String? = null,
    var nextStopIndex: Int = 0,
    var stopArrivalTimes: Map<String, String> = emptyMap(),
    var isNavigating: Boolean = false
) : Serializable