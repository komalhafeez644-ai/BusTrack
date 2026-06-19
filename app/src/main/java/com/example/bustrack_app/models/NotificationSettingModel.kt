package com.example.bustrack_app.models

data class NotificationSettingModel(
    val id: Int,
    val title: String,
    val description: String,
    val iconRes: Int,
    var isEnabled: Boolean
)