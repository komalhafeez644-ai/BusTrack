package com.example.bustrack_app.models

import java.io.Serializable

data class CustomLocation(
    val id: String = "",
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
) : Serializable
