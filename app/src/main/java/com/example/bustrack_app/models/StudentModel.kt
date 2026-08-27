package com.example.bustrack_app.models

data class StudentModel(
    val id: String = "",                  // e.g., #SR-9921 / ST-2045
    val name: String = "",                // e.g., Elena Rodriguez / Ali Hassan
    val grade: String = "",               // e.g., Grade 11 / BS IT 7th semester
    val location: String = "",            // e.g., Street #4, Sector 15
    val stopName: String? = null,         // e.g., Sector 15 North
    val route: String? = null,              // e.g., ROUTE 42-B / Route 12-A
    val busNo: String? = null,              // e.g., Bus #102 / Bus 42
    val status: String = "UNASSIGNED",              // e.g., UNASSIGNED or ASSIGNED
    val isActive: Boolean = true,                   // Operational status
    val profileImage: Int = 0,           // Drawable resource ID
    val profileImageUrl: String = "",    // Firebase Storage URL

    // ---- Nayi Fields Jo Detail Screen Ke Liya Chahiye ----
    val fatherName: String = "",     // e.g., Ahmed Khan
    val phoneNumber: String = "",    // e.g., +92 300 1234567
    val pickupTime: String = "",     // e.g., 07:15 AM
    val insuranceStatus: String = "", // e.g., Active
    val latitude: Double = 0.0,
    val longitude: Double = 0.0
)