package com.example.bustrack_app.models

data class TransportAlert(
    val title: String,
    val subtitle: String,
    val type: String, // "CRITICAL", "IMPORTANT", ya "GENERAL"
    val iconResId: Int
)