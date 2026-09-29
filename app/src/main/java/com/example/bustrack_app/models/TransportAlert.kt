package com.example.bustrack_app.models

data class TransportAlert(
    val title: String,
    val subtitle: String,
    val type: String, // "CRITICAL", "IMPORTANT", ya "GENERAL"
    val iconResId: Int,
    val id: String = "", // Firestore notification id, used to mark as read on open
    val timeText: String = "Just now",
    val notificationKind: String = "GENERAL",
    val relatedId: String = "",
    val parentPhone: String = ""
)
