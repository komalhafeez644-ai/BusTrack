package com.example.bustrack_app.models

import java.io.Serializable

data class LocationModel(
    val id: String = "",
    val name: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String = "Rawalpindi",
    val type: String = "AREA"
) : Serializable
