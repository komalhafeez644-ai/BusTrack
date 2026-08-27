package com.example.bustrack_app.models
data class ApplicationDetailModel(

    val applicationId: String,
    val status: String,
    val dateTime: String,

    val studentName: String,
    val studentInfo: String,
    val parentName: String,
    val phone: String,

    val pickupAddress: String,
    val city: String,

    val routeName: String,
    val distance: String,
    val nearestStop: String
)