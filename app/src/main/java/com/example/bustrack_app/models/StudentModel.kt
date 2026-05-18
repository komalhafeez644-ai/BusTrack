package com.example.bustrack_app.models

data class StudentModel(
    val id: String,                  // e.g., #SR-9921 / ST-2045
    val name: String,                // e.g., Elena Rodriguez / Ali Hassan
    val grade: String,               // e.g., Grade 11 / BS IT 7th semester
    val location: String,            // e.g., North District / Sector 15 North
    val route: String?,              // e.g., ROUTE 42-B / Route 12-A
    val busNo: String?,              // e.g., Bus #102 / Bus 42
    val status: String,              // e.g., UNASSIGNED or ASSIGNED
    val profileImage: Int,           // Drawable resource ID

    // ---- Nayi Fields Jo Detail Screen Ke Liya Chahiye ----
    val fatherName: String = "",     // e.g., Ahmed Khan
    val phoneNumber: String = "",    // e.g., +92 300 1234567
    val pickupTime: String = "",     // e.g., 07:15 AM
    val insuranceStatus: String = "" // e.g., Active
)