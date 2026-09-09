package com.example.bustrack_app.data

import com.example.bustrack_app.models.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.toObject
import com.google.firebase.firestore.ListenerRegistration
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

/**
 * Modern way to handle all database operations in one place.
 */
object FirebaseRepository {

    private val db = Firebase.firestore

    // --- STUDENTS ---
    fun fetchStudents(onResult: (List<StudentModel>) -> Unit): ListenerRegistration {
        return db.collection("students").addSnapshotListener { snapshot, _ ->
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

    fun updateDriverRouteGeometry(driverId: String, currentPolyline: String?, traveledPolyline: String?, nextStopIndex: Int, stopArrivalTimes: Map<String, String>, isNavigating: Boolean, stopEtaTimes: Map<String, String> = emptyMap(), traveledRouteSegments: List<String> = emptyList()) {
        val updates = mutableMapOf<String, Any?>()
        updates["currentRoutePolyline"] = currentPolyline
        updates["traveledPolyline"] = traveledPolyline
        updates["nextStopIndex"] = nextStopIndex
        updates["stopArrivalTimes"] = stopArrivalTimes
        updates["stopEtaTimes"] = stopEtaTimes
        updates["traveledRouteSegments"] = traveledRouteSegments
        updates["isNavigating"] = isNavigating
        db.collection("drivers").document(driverId).update(updates)
    }

    /**
     * Consolidated live-tracking write for the Driver Module's high-frequency
     * location loop (DriverDashboardActivity.syncTrackingDataToFirestore).
     * Replaces what used to be three separate .update() calls in that one
     * function (updateDriverLocation + updateDriverStats +
     * updateDriverRouteGeometry) with a single Firestore write.
     *
     * Firestore bills per document write regardless of how many fields change
     * in it, so now that the Driver's write cadence has been tightened from
     * 5s to ~1s (to fix Admin/Parent/Principal's laggy marker movement and
     * the delayed grey traveled-route rendering), merging these three calls
     * into one avoids roughly tripling the write cost on top of that ~5x
     * frequency increase. Also carries stopEtaTimes (mirrors the Driver's own
     * in-memory stopEtaTexts, keyed the same way as stopArrivalTimes) so
     * Parent/Principal/Admin can show a real per-stop ETA for every upcoming
     * stop, not just the immediate next one.
     */
    fun updateDriverLiveState(
        driverId: String,
        lat: Double,
        lng: Double,
        eta: String,
        speed: Double,
        load: String,
        currentPolyline: String?,
        traveledPolyline: String?,
        nextStopIndex: Int,
        stopArrivalTimes: Map<String, String>,
        stopEtaTimes: Map<String, String>,
        isNavigating: Boolean,
        traveledRouteSegments: List<String> = emptyList()
    ) {
        val updates = mutableMapOf<String, Any?>(
            "latitude" to lat,
            "longitude" to lng,
            "lastUpdated" to System.currentTimeMillis(),
            "eta" to eta,
            "speed" to speed,
            "load" to load,
            "isNavigating" to isNavigating
        )
        if (isNavigating) {
            updates["currentRoutePolyline"] = currentPolyline
            updates["traveledPolyline"] = traveledPolyline
            updates["traveledRouteSegments"] = traveledRouteSegments
            updates["nextStopIndex"] = nextStopIndex
            updates["stopArrivalTimes"] = stopArrivalTimes
            updates["stopEtaTimes"] = stopEtaTimes
        }
        db.collection("drivers").document(driverId).update(updates)
    }

    // --- ATTENDANCE ---
    fun saveAttendance(record: AttendanceRecordModel, onComplete: (Boolean) -> Unit) {
        val normalizedDate = record.date.replace("/", "-")
        val normalizedRecord = record.copy(date = normalizedDate)
        val docId = "${record.studentId}_$normalizedDate"
        db.collection("attendance").document(docId).set(normalizedRecord, com.google.firebase.firestore.SetOptions.merge())
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun updateAttendanceField(studentId: String, date: String, field: String, value: String) {
        val normalizedDate = date.replace("/", "-")
        val docId = "${studentId}_$normalizedDate"
        db.collection("attendance").document(docId).update(field, value)
    }

    fun fetchAttendance(onResult: (List<AttendanceRecordModel>) -> Unit): ListenerRegistration {
        return db.collection("attendance").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<AttendanceRecordModel>() } ?: emptyList()
            onResult(list)
        }
    }

    /**
     * Automatically records the arrival time at a drop-off stop for all students
     * who were present on the bus.
     */
    fun updateDropTimesForRoute(routeName: String, stopName: String, isMorning: Boolean, date: String, time: String) {
        val field = if (isMorning) "morningDrop" else "eveningDrop"
        val normalizedDate = date.replace("/", "-")

        db.collection("attendance")
            .whereEqualTo("route", routeName)
            .whereEqualTo("date", normalizedDate)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { doc ->
                    val record = doc.toObject<AttendanceRecordModel>()
                    if (record != null) {
                        val pickupStatus = if (isMorning) record.morningPickup else record.eveningPickup
                        // Check if student was present during pickup
                        val isPresent = pickupStatus.contains(":") || pickupStatus.equals("Present", true) || pickupStatus.equals("School", true) || pickupStatus.equals("En Route", true)

                        val shouldUpdate = if (isMorning) {
                            // In morning, all present students drop at the end (usually School)
                            // We assume the caller only triggers this for the final stop.
                            isPresent && (record.morningDrop == "--" || record.morningDrop == "Pending" || record.morningDrop == "En Route")
                        } else {
                            // In evening, only students assigned to this specific stop drop here
                            isPresent && record.stop == stopName && (record.eveningDrop == "--" || record.eveningDrop == "Pending")
                        }

                        if (shouldUpdate) {
                            batch.update(doc.reference, field, time)
                        }
                    }
                }
                batch.commit()
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
     * System Notification helper for Drivers.
     */
    fun sendSystemNotification(
        driverId: String,
        title: String,
        message: String,
        type: String = NotificationModel.TYPE_GENERAL,
        relatedId: String = ""
    ) {
        sendNotification(
            recipientId = driverId,
            title = title,
            message = message,
            type = type,
            relatedId = relatedId
        )
    }

    fun notifyNewTripAssigned(driverId: String, routeName: String, busNo: String) {
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        sendNotification(
            id = "NEW_TRIP_${driverId}_$today",
            recipientId = driverId,
            title = "New Trip Assigned",
            message = "A new trip (Route: $routeName, Bus: $busNo) has been assigned to you. Please review your assigned route, stops, trip timing, and student details before starting the trip.",
            type = NotificationModel.TYPE_NEW_TRIP,
            relatedId = routeName
        )
    }

    fun notifyTripUpdated(driverId: String, routeName: String) {
        sendSystemNotification(
            driverId = driverId,
            title = "Trip Updated",
            message = "Your assigned trip details for $routeName have been updated. Please check the latest route, scheduled time, stops, and other trip information before starting navigation.",
            type = NotificationModel.TYPE_TRIP_UPDATE,
            relatedId = routeName
        )
    }

    fun notifyTripCancelled(driverId: String, routeName: String) {
        sendSystemNotification(
            driverId = driverId,
            title = "Trip Cancelled",
            message = "Your assigned trip ($routeName) has been cancelled by the administration. Please do not start navigation for this trip and check the latest trip schedule for further updates.",
            type = NotificationModel.TYPE_TRIP_CANCELLED,
            relatedId = routeName
        )
    }

    fun notifyRouteUpdated(driverId: String, routeName: String) {
        sendSystemNotification(
            driverId = driverId,
            title = "Route Updated",
            message = "Your assigned route $routeName has been updated. One or more route details, stops, or student assignments may have changed. Please review the latest route information before starting your trip.",
            type = NotificationModel.TYPE_ROUTE_UPDATE,
            relatedId = routeName
        )
    }

    fun notifyStopUpdated(driverId: String, routeName: String) {
        sendSystemNotification(
            driverId = driverId,
            title = "Stop Updated",
            message = "A stop on your assigned route ($routeName) has been added, removed, or updated. Please check the latest stop sequence and student assignments before continuing with your trip.",
            type = NotificationModel.TYPE_STOP_UPDATE,
            relatedId = routeName
        )
    }

    fun notifyAttendanceUpdateRequired(driverId: String, routeName: String) {
        sendSystemNotification(
            driverId = driverId,
            title = "Attendance Update Required",
            message = "An issue was found with the attendance record for your current trip ($routeName). Please open the attendance section, review the student records, and update the attendance if required.",
            type = NotificationModel.TYPE_ATTENDANCE_REQUIRED,
            relatedId = routeName
        )
    }

    fun notifyImportantAdminAlert(driverId: String, message: String) {
        sendSystemNotification(
            driverId = driverId,
            title = "Important Admin Alert",
            message = message,
            type = NotificationModel.TYPE_IMPORTANT
        )
    }

    fun notifyEmergencyAlert(driverId: String, message: String) {
        sendSystemNotification(
            driverId = driverId,
            title = "Emergency Alert",
            message = message,
            type = NotificationModel.TYPE_EMERGENCY
        )
    }

    /**
     * Sends a notification to either a specific user (recipientId) or an entire role
     * (recipientRole, e.g. "admin"/"driver"/"parent"/"principal") - pass exactly one.
     * If an [id] is provided, it uses it for deduplication.
     */
    fun sendNotification(
        id: String? = null,
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
        val docRef = if (id != null) db.collection("notifications").document(id)
        else db.collection("notifications").document()

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
        docRef.set(data, com.google.firebase.firestore.SetOptions.merge())
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

    private val unreadListeners = mutableListOf<ListenerRegistration>()
    private val _unreadCount = androidx.lifecycle.MutableLiveData<Int>()
    val unreadCount: androidx.lifecycle.LiveData<Int> get() = _unreadCount

    fun startUnreadCountListener(uid: String, role: String) {
        if (unreadListeners.isNotEmpty()) return // Already listening

        val query = db.collection("notifications")
            .whereEqualTo("isRead", false)
            .where(com.google.firebase.firestore.Filter.or(
                com.google.firebase.firestore.Filter.equalTo("recipientId", uid),
                com.google.firebase.firestore.Filter.equalTo("recipientRole", role)
            ))

        val reg = query.addSnapshotListener { snapshot, _ ->
            _unreadCount.postValue(snapshot?.size() ?: 0)
        }

        unreadListeners.add(reg)
    }

    fun stopUnreadCountListener() {
        unreadListeners.forEach { it.remove() }
        unreadListeners.clear()
    }

    /**
     * Attendance-related notification: tells a student's approved parent(s)
     * when their child is marked Absent. Includes deduplication by studentId + date.
     */
    fun notifyParentsOfAttendance(studentId: String, studentName: String, status: String, date: String, isMorning: Boolean) {
        if (!status.equals("Absent", true)) return

        val period = if (isMorning) "Morning" else "Evening"
        val notificationId = "ABSENT_${studentId}_${date.replace("/", "-")}_${period.uppercase()}"

        // Check for duplicate notification before sending
        db.collection("notifications").document(notificationId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    android.util.Log.d("NotifDebug", "Skipping duplicate absent notification for $studentId on $date")
                    return@addOnSuccessListener
                }

                db.collection("trackingRequests")
                    .whereEqualTo("studentId", studentId)
                    .whereEqualTo("status", "APPROVED")
                    .get()
                    .addOnSuccessListener { snapshot ->
                        val requests = snapshot.documents.mapNotNull { it.toObject<TrackingRequestModel>() }
                        if (requests.isEmpty()) {
                            android.util.Log.d("NotifDebug", "No approved tracking request found for student $studentId")
                        }
                        requests.forEach { req ->
                            sendNotification(
                                id = notificationId,
                                recipientId = req.parentId,
                                title = "Attendance Update: Absent",
                                message = "Your child, $studentName, was marked absent for $period pickup on $date.",
                                type = "ATTENDANCE",
                                relatedId = studentId
                            ) { success ->
                                android.util.Log.d("NotifDebug", "Absent notification for $studentName sent: $success")
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotifDebug", "Error checking for duplicate absent notif: ${e.message}")
            }
    }

    /**
     * Stop arrival notification: notifies parents of students assigned to a specific stop
     * that the bus has arrived. Includes deduplication for the current trip.
     */
    fun notifyParentsOfStopArrival(routeName: String, stopName: String, tripId: String) {
        val notificationIdPrefix = "ARRIVAL_${routeName.replace(" ", "_")}_${stopName.replace(" ", "_")}_${tripId}"

        android.util.Log.d("NotifDebug", "Checking stop arrival for $stopName on route $routeName (trip: $tripId)")

        // Find students assigned to this stop
        db.collection("students")
            .whereEqualTo("route", routeName)
            .whereEqualTo("stopName", stopName)
            .get()
            .addOnSuccessListener { studentSnapshot ->
                val students = studentSnapshot.documents.mapNotNull { it.toObject<StudentModel>() }
                if (students.isEmpty()) {
                    android.util.Log.d("NotifDebug", "No students assigned to stop $stopName")
                    return@addOnSuccessListener
                }

                val studentIds = students.map { it.id }

                // Firestore permits at most ten values in a whereIn filter. A stop
                // can legitimately have more students than that, and passing the full
                // list throws synchronously on the first arrival event (which made the
                // driver activity crash/restart). Query each legal batch instead.
                studentIds.distinct().chunked(10).forEach { studentIdBatch ->
                    db.collection("trackingRequests")
                        .whereIn("studentId", studentIdBatch)
                        .whereEqualTo("status", "APPROVED")
                        .whereEqualTo("trackingEnabled", true)
                        .get()
                        .addOnSuccessListener { reqSnapshot ->
                        val requests = reqSnapshot.documents.mapNotNull { it.toObject<TrackingRequestModel>() }
                        if (requests.isEmpty()) {
                            android.util.Log.d("NotifDebug", "No parents found with tracking enabled for stop $stopName")
                            return@addOnSuccessListener
                        }

                        // Group by parent to avoid duplicate notifications to same parent for multiple kids at same stop
                        val parentIds = requests.map { it.parentId }.distinct()

                        parentIds.forEach { parentId ->
                            val finalNotifId = "${notificationIdPrefix}_${parentId}"

                            // Deduplication check
                            db.collection("notifications").document(finalNotifId).get()
                                .addOnSuccessListener { doc ->
                                    if (doc.exists()) {
                                        android.util.Log.d("NotifDebug", "Arrival notif already sent to parent $parentId for stop $stopName")
                                        return@addOnSuccessListener
                                    }

                                    sendNotification(
                                        id = finalNotifId,
                                        recipientId = parentId,
                                        title = "Bus Arrived at Your Stop",
                                        message = "The bus on route $routeName has arrived at $stopName. Please be ready to pick up your child.",
                                        type = "ARRIVAL",
                                        relatedId = routeName
                                    ) { success ->
                                        android.util.Log.d("NotifDebug", "Arrival notif for $stopName sent to parent $parentId: $success")
                                    }
                                }
                        }
                        }
                        .addOnFailureListener { error ->
                            android.util.Log.e("NotifDebug", "Error loading tracking requests for stop $stopName", error)
                        }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotifDebug", "Error fetching students for stop arrival: ${e.message}")
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
        db.collection("trackingRequests").addSnapshotListener { snapshot, error ->
            if (error != null) {
                android.util.Log.e("FirebaseRepo", "Error fetching tracking requests: ${error.message}", error)
                onResult(emptyList())
                return@addSnapshotListener
            }
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
            "reviewedBy" to reviewedBy,
            "isSeenByAdmin" to true
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
            "reviewedBy" to reviewedBy,
            "isSeenByAdmin" to true
        )
        trackingRoute?.let { updates["assignedTrackingRoute"] = it }

        db.collection("trackingRequests").document(requestId).update(updates)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    fun markTrackingRequestAsSeen(requestId: String) {
        db.collection("trackingRequests").document(requestId).update("isSeenByAdmin", true)
    }

    // --- PARENTS ---
    fun fetchParent(parentId: String, onResult: (ParentModel?) -> Unit) {
        db.collection("parents").document(parentId).addSnapshotListener { snapshot, _ ->
            onResult(snapshot?.toObject<ParentModel>())
        }
    }
}
