package com.example.bustrack_app.models

import com.google.firebase.Timestamp

data class TrackingRequestModel(
    val requestId: String = "",
    val parentId: String = "",
    val studentId: String = "",
    val status: String = "pending",
    val submittedAt: Timestamp? = null,
    val reviewedAt: Timestamp? = null,
    val reviewedBy: String? = null,
    // Optional snapshot fields for easier display in lists
    val parentName: String = "",
    val phone: String = "",
    val relationship: String = ""
)
