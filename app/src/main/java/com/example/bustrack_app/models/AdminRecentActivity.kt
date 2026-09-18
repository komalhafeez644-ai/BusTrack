package com.example.bustrack_app.models

import com.example.bustrack_app.R

data class AdminRecentActivity(
    val id: String = "",
    val title: String,
    val description: String,
    val timeAgo: String,
    val iconRes: Int = R.drawable.ic_assignment,
    val iconTint: Int = 0xFF4CAF50.toInt(),
    val bgTint: Int = 0xFFE8F5E9.toInt()
)
