package com.example.bustrack_app.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ServerTimestamp
import java.io.Serializable
import java.util.Date

/** Notification data shared by all app roles. */
data class NotificationModel(
    val id: String = "",
    val recipientId: String = "",   // specific user's uid, empty if role-targeted
    val recipientRole: String = "", // "admin" | "driver" | "parent" | "principal", empty if user-targeted
    val title: String = "",
    val message: String = "",
    val type: String = "GENERAL",   // TRACKING_REQUEST, TRACKING_APPROVED, TRACKING_REJECTED, TRACKING_REVOKED, ATTENDANCE, BROADCAST, GENERAL
    @ServerTimestamp
    val timestamp: Date? = null,
    val isRead: Boolean = false,
    val relatedId: String = "",      // optional: requestId / studentId / route, for future deep-linking
    val senderId: String = "",
    val senderRole: String = "",     // "driver", "admin", etc.
    val driverName: String = "",
    val driverEmail: String = "",
    val driverPhone: String = "",
    val parentName: String = "",
    val parentPhone: String = "",
    val parentRollNumber: String = "",
    val busNumber: String = "",
    val routeName: String = "",
    val alertType: String = "",      // "Road Block", "Heavy Traffic", "Accident", "Bus Breakdown", "Fuel Issue", "Bad Weather", "Student Emergency", "Police Check", "Wrong Route", "Other"
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val locationAddress: String = "",
    val tripDirection: String = "",  // "FORWARD", "RETURN"
    val tripStatus: String = ""      // "NAVIGATING", "ON_DUTY", "IDLE"
) : Serializable {
    companion object {
        const val TYPE_GENERAL = "GENERAL"
        const val TYPE_IMPORTANT = "IMPORTANT"
        const val TYPE_EMERGENCY = "EMERGENCY"
        const val TYPE_DRIVER_ALERT = "DRIVER_ALERT"
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

        /** Converts Firestore and legacy timestamp values to a date. */
        fun parseDate(raw: Any?): Date? {
            return when (raw) {
                is Timestamp -> raw.toDate()
                is Date -> raw
                is Number -> {
                    val millis = raw.toLong()
                    if (millis > 0L) Date(millis) else null
                }
                is String -> {
                    raw.toLongOrNull()?.let { Date(it) } ?: try {
                        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()).parse(raw)
                    } catch (_: Exception) {
                        null
                    }
                }
                else -> null
            }
        }

        fun fromDocument(doc: DocumentSnapshot): NotificationModel {
            val docId = doc.id
            val parsedDate = parseDate(doc.get("timestamp"))

            return NotificationModel(
                // Use the Firestore document key for writes such as mark-as-read.
                // The optional "id" field can be stale or differ from the document key.
                id = docId,
                recipientId = doc.getString("recipientId") ?: "",
                recipientRole = doc.getString("recipientRole") ?: "",
                title = doc.getString("title") ?: "",
                message = doc.getString("message") ?: "",
                type = doc.getString("type") ?: TYPE_GENERAL,
                timestamp = parsedDate,
                isRead = doc.getBoolean("isRead") ?: false,
                relatedId = doc.getString("relatedId") ?: "",
                senderId = doc.getString("senderId") ?: "",
                senderRole = doc.getString("senderRole") ?: "",
                driverName = doc.getString("driverName") ?: "",
                driverEmail = doc.getString("driverEmail") ?: "",
                driverPhone = doc.getString("driverPhone") ?: "",
                parentName = doc.getString("parentName") ?: "",
                parentPhone = doc.getString("parentPhone") ?: "",
                parentRollNumber = doc.getString("parentRollNumber") ?: "",
                busNumber = doc.getString("busNumber") ?: "",
                routeName = doc.getString("routeName") ?: "",
                alertType = doc.getString("alertType") ?: "",
                description = doc.getString("description") ?: "",
                latitude = doc.getDouble("latitude") ?: 0.0,
                longitude = doc.getDouble("longitude") ?: 0.0,
                locationAddress = doc.getString("locationAddress") ?: "",
                tripDirection = doc.getString("tripDirection") ?: "",
                tripStatus = doc.getString("tripStatus") ?: ""
            )
        }
    }
}
