package com.example.bustrack_app.data

import com.example.bustrack_app.models.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ListenerRegistration

/**
 * Modern way to handle all database operations in one place.
 */
object FirebaseRepository {

    private val db = Firebase.firestore

    // --- STUDENTS ---
    fun fetchStudents(onResult: (List<StudentModel>) -> Unit) {
        db.collection("students").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<StudentModel>() } ?: emptyList()
            onResult(list)
        }
    }

    fun fetchStudentsByRoute(routeName: String, onResult: (List<StudentModel>) -> Unit) {
        db.collection("students")
            .whereEqualTo("route", routeName)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject<StudentModel>() } ?: emptyList()
                onResult(list)
            }
    }

    fun fetchStudentsByStop(routeName: String, stopName: String, onResult: (List<StudentModel>) -> Unit) {
        db.collection("students")
            .whereEqualTo("route", routeName)
            .whereEqualTo("stopName", stopName)
            .addSnapshotListener { snapshot, _ ->
                val list = snapshot?.documents?.mapNotNull { it.toObject<StudentModel>() } ?: emptyList()
                onResult(list)
            }
    }

    fun saveStudent(student: StudentModel, onComplete: (Boolean) -> Unit) {
        db.collection("students").document(student.id).set(student)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun fetchStudentById(studentId: String, onResult: (StudentModel?) -> Unit) {
        db.collection("students").document(studentId).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.toObject<StudentModel>())
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun listenToStudent(studentId: String, onResult: (StudentModel?) -> Unit): ListenerRegistration {
        return db.collection("students").document(studentId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onResult(null)
                    return@addSnapshotListener
                }
                onResult(snapshot?.toObject<StudentModel>())
            }
    }

    // --- DRIVERS ---
    fun fetchDrivers(onResult: (List<DriverModel>) -> Unit) {
        db.collection("drivers").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<DriverModel>() } ?: emptyList()
            onResult(list)
        }
    }

    fun saveDriver(driver: DriverModel, onComplete: (Boolean) -> Unit) {
        db.collection("drivers").document(driver.driverId).set(driver)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun updateDriverLocation(driverId: String, lat: Double, lng: Double) {
        val updates = mapOf(
            "latitude" to lat,
            "longitude" to lng,
            "lastUpdated" to System.currentTimeMillis()
        )
        db.collection("drivers").document(driverId).update(updates)
    }

    fun updateDriverStats(driverId: String, eta: String, speed: Double, load: String) {
        val updates = mapOf(
            "eta" to eta,
            "speed" to speed,
            "load" to load
        )
        db.collection("drivers").document(driverId).update(updates)
    }

    fun updateDriverStatus(driverId: String, status: String, route: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "lastUpdated" to System.currentTimeMillis()
        )
        route?.let { updates["route"] = it }
        db.collection("drivers").document(driverId).update(updates)
    }

    fun updateDriverRouteGeometry(driverId: String, currentPolyline: String?, traveledPolyline: String?, nextStopIndex: Int, stopArrivalTimes: Map<String, String>, isNavigating: Boolean) {
        val updates = mutableMapOf<String, Any?>()
        updates["currentRoutePolyline"] = currentPolyline
        updates["traveledPolyline"] = traveledPolyline
        updates["nextStopIndex"] = nextStopIndex
        updates["stopArrivalTimes"] = stopArrivalTimes
        updates["isNavigating"] = isNavigating
        db.collection("drivers").document(driverId).update(updates)
    }

    // --- ATTENDANCE ---
    fun saveAttendance(record: AttendanceRecordModel, onComplete: (Boolean) -> Unit) {
        val docId = "${record.studentId}_${record.date.replace("/", "-")}"
        db.collection("attendance").document(docId).set(record, com.google.firebase.firestore.SetOptions.merge())
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun updateAttendanceField(studentId: String, date: String, field: String, value: String) {
        val docId = "${studentId}_${date.replace("/", "-")}"
        db.collection("attendance").document(docId).update(field, value)
    }

    fun fetchAttendance(onResult: (List<AttendanceRecordModel>) -> Unit) {
        db.collection("attendance").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<AttendanceRecordModel>() } ?: emptyList()
            onResult(list)
        }
    }

    /**
     * Fetches today's already-saved attendance records for a specific set of students,
     * keyed by studentId, so a screen (e.g. the driver's Attendance Bottom Sheet) can
     * pre-fill previously marked Present/Absent/Leave status and let the driver edit it
     * instead of re-marking from scratch or overwriting it.
     */
    fun fetchAttendanceForStudents(studentIds: List<String>, date: String, onResult: (Map<String, AttendanceRecordModel>) -> Unit) {
        if (studentIds.isEmpty()) {
            onResult(emptyMap())
            return
        }
        val docIds = studentIds.map { "${it}_${date.replace("/", "-")}" }
        // Firestore whereIn supports up to 30 values per query; a single stop's roster is
        // always well under that, so a single batched query is fine here.
        db.collection("attendance")
            .whereIn(com.google.firebase.firestore.FieldPath.documentId(), docIds)
            .get()
            .addOnSuccessListener { snapshot ->
                val map = snapshot.documents
                    .mapNotNull { it.toObject<AttendanceRecordModel>() }
                    .associateBy { it.studentId }
                onResult(map)
            }
            .addOnFailureListener {
                onResult(emptyMap())
            }
    }

    // --- NOTIFICATIONS ---

    /**
     * Sends a notification to either a specific user (recipientId) or an entire role
     * (recipientRole, e.g. "admin"/"driver"/"parent"/"principal") - pass exactly one.
     */
    fun sendNotification(
        recipientId: String? = null,
        recipientRole: String? = null,
        title: String,
        message: String,
        type: String = "GENERAL",
        relatedId: String = "",
        onComplete: (Boolean) -> Unit = {}
    ) {
        if (recipientId.isNullOrBlank() && recipientRole.isNullOrBlank()) {
            onComplete(false)
            return
        }
        val docRef = db.collection("notifications").document()
        val data = hashMapOf(
            "recipientId" to (recipientId ?: ""),
            "recipientRole" to (recipientRole ?: ""),
            "title" to title,
            "message" to message,
            "type" to type,
            "relatedId" to relatedId,
            "isRead" to false,
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
        docRef.set(data)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Real-time feed for a single user: everything addressed to their uid directly,
     * merged with everything broadcast to their role. Keeps two live listeners and
     * re-merges on every change from either one, sorted newest-first.
     */
    fun listenToNotifications(uid: String, role: String, onResult: (List<NotificationModel>) -> Unit): List<ListenerRegistration> {
        var personal: List<NotificationModel> = emptyList()
        var roleBroadcast: List<NotificationModel> = emptyList()

        fun emit() {
            val merged = (personal + roleBroadcast)
                .distinctBy { it.id }
                .sortedByDescending { it.timestamp?.time ?: 0L }
            onResult(merged)
        }

        val reg1 = db.collection("notifications")
            .whereEqualTo("recipientId", uid)
            .addSnapshotListener { snapshot, _ ->
                personal = snapshot?.documents?.mapNotNull { it.toObject<NotificationModel>() } ?: emptyList()
                emit()
            }

        val reg2 = db.collection("notifications")
            .whereEqualTo("recipientRole", role)
            .addSnapshotListener { snapshot, _ ->
                roleBroadcast = snapshot?.documents?.mapNotNull { it.toObject<NotificationModel>() } ?: emptyList()
                emit()
            }

        return listOf(reg1, reg2)
    }

    fun markNotificationRead(notificationId: String, onComplete: (Boolean) -> Unit = {}) {
        db.collection("notifications").document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun markAllNotificationsRead(notificationIds: List<String>, onComplete: (Boolean) -> Unit = {}) {
        if (notificationIds.isEmpty()) {
            onComplete(true)
            return
        }
        val batch = db.batch()
        notificationIds.forEach { id ->
            batch.update(db.collection("notifications").document(id), "isRead", true)
        }
        batch.commit()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    /**
     * Attendance-related notification (Task 5): tells a student's approved parent(s)
     * when their child is marked Absent or Leave (Present is the expected default, so
     * we don't spam parents for it - "do not add unnecessary notification types").
     */
    fun notifyParentsOfAttendance(studentId: String, studentName: String, status: String, isMorning: Boolean) {
        if (!status.equals("Absent", true) && !status.equals("Leave", true)) return

        db.collection("trackingRequests")
            .whereEqualTo("studentId", studentId)
            .whereEqualTo("status", "APPROVED")
            .get()
            .addOnSuccessListener { snapshot ->
                val period = if (isMorning) "Morning" else "Evening"
                snapshot.documents.mapNotNull { it.toObject<TrackingRequestModel>() }.forEach { req ->
                    sendNotification(
                        recipientId = req.parentId,
                        title = "$period Attendance: $status",
                        message = "$studentName was marked $status for $period pickup today.",
                        type = "ATTENDANCE",
                        relatedId = studentId
                    )
                }
            }
    }

    // --- BUSES ---
    fun fetchBuses(onResult: (List<BusModel>) -> Unit) {
        db.collection("buses").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<BusModel>() } ?: emptyList()
            onResult(list)
        }
    }

    // --- ROUTES ---
    fun fetchRoutes(onResult: (List<RouteModel>) -> Unit) {
        db.collection("routes").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<RouteModel>() } ?: emptyList()
            onResult(list)
        }
    }

    // --- TRACKING REQUESTS ---
    fun fetchTrackingRequests(onResult: (List<TrackingRequestModel>) -> Unit) {
        db.collection("trackingRequests").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<TrackingRequestModel>() } ?: emptyList()
            onResult(list)
        }
    }

    fun updateTrackingRequest(
        requestId: String,
        status: String,
        trackingEnabled: Boolean,
        trackingState: String,
        reviewedBy: String,
        trackingRoute: String? = null,
        onComplete: (Boolean) -> Unit
    ) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "trackingEnabled" to trackingEnabled,
            "trackingState" to trackingState,
            "reviewedAt" to com.google.firebase.Timestamp.now(),
            "reviewedBy" to reviewedBy
        )
        
        if (status == "REWORK") {
            updates["reworkAt"] = com.google.firebase.Timestamp.now()
        }
        
        trackingRoute?.let { updates["assignedTrackingRoute"] = it }

        db.collection("trackingRequests").document(requestId).update(updates)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun updateTrackingRequestStatus(requestId: String, status: String, reviewedBy: String, trackingRoute: String? = null, onComplete: (Boolean) -> Unit) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "reviewedAt" to com.google.firebase.Timestamp.now(),
            "reviewedBy" to reviewedBy
        )
        trackingRoute?.let { updates["assignedTrackingRoute"] = it }

        db.collection("trackingRequests").document(requestId).update(updates)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- PARENTS ---
    fun fetchParent(parentId: String, onResult: (ParentModel?) -> Unit) {
        db.collection("parents").document(parentId).addSnapshotListener { snapshot, _ ->
            onResult(snapshot?.toObject<ParentModel>())
        }
    }
}
