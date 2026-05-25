package com.example.bustrack_app.models
import java.io.Serializable

data class DriverModel(
    val id: String,
    var name: String,
    var status: String,
    var assignedBus: String,
    var route: String,
    var profileImage: Int,
    var cnic: String = "",
    var phone: String = "",
    var email: String = "" // Yeh humne verify kiya ke naya field available hai
) : Serializable