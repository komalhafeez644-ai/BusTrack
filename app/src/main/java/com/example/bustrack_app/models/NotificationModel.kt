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
)
