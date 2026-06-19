package com.example.bustrack_app.models

import java.io.Serializable

data class ParentNotificationModel(
    val id: String,
    val title: String,
    val message: String,
    val time: String,
    val type: NotificationType,
    var isRead: Boolean = false
) : Serializable

enum class NotificationType {
    DELAY, ARRIVAL, ROUTE_CHANGE, CANCELLATION, GENERAL
}
