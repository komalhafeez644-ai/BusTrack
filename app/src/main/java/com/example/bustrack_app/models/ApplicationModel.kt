package com.example.bustrack_app.models

data class ApplicationModel(
    val id: Int,
    val studentName: String,
    val studentClass: String,
    val pickupPoint: String,
    val routeMatch: String,
    val time: String,
    val status: String,
    val image: Int
)
