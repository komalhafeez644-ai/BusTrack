package com.example.bustrack_app.models

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

/**
 * Real Firestore-backed notification, shared by Admin/Driver/Parent/Principal.
 *
 * Targeting: EITHER recipientId (a specific user's uid - used for "this parent",
 * "this driver") OR recipientRole (a role broadcast - "all admins", "all drivers",
 * "all parents") is set, never both. FirebaseRepository.listenToNotifications()
 * queries both fields and merges results so a user sees notifications addressed to
 * them personally as well as ones broadcast to their role.
 */
data class NotificationModel(
    @DocumentId
    val id: String = "",
    val recipientId: String = "",   // specific user's uid, empty if role-targeted
    val recipientRole: String = "", // "admin" | "driver" | "parent" | "principal", empty if user-targeted
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL",   // TRACKING_REQUEST, TRACKING_APPROVED, TRACKING_REJECTED, TRACKING_REVOKED, ATTENDANCE, BROADCAST, GENERAL
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false,
    val relatedId: String = ""      // optional: requestId / studentId / route, for future deep-linking
) {
    companion object {
        const val TYPE_GENERAL = "GENERAL"
        const val TYPE_IMPORTANT = "IMPORTANT"
        const val TYPE_EMERGENCY = "EMERGENCY"
        const val TYPE_ADMIN_BROADCAST = "ADMIN_BROADCAST"
        const val TYPE_ATTENDANCE = "ATTENDANCE"
        const val TYPE_ATTENDANCE_REQUIRED = "ATTENDANCE_REQUIRED"
        const val TYPE_TRIP_UPDATE = "TRIP_UPDATE"
        const val TYPE_TRIP_CANCELLED = "TRIP_CANCELLED"
        const val TYPE_NEW_TRIP = "NEW_TRIP"
        const val TYPE_ROUTE_UPDATE = "ROUTE_UPDATE"
        const val TYPE_STOP_UPDATE = "STOP_UPDATE"
        const val TYPE_NAV_READY = "NAV_READY"
        const val TYPE_TRIP_REMINDER = "TRIP_REMINDER"
        const val TYPE_TRIP_STARTED = "TRIP_STARTED"
    }
}
