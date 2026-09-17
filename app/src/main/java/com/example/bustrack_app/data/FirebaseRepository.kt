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
        com.example.bustrack_app.sync.SyncQueueManager.enqueueUpdate(
            syncId = "LOC_LATEST_$driverId",
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_LOCATION_LATEST,
            targetCollection = "drivers",
            targetDocumentId = driverId,
            updates = updates,
            isConflated = true,
            immediateSync = true
        )
    }

    fun updateDriverStats(driverId: String, eta: String, speed: Double, load: String) {
        val updates = mapOf(
            "eta" to eta,
            "speed" to speed,
            "load" to load
        )
        com.example.bustrack_app.sync.SyncQueueManager.enqueueUpdate(
            syncId = "LOC_LATEST_$driverId",
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_LOCATION_LATEST,
            targetCollection = "drivers",
            targetDocumentId = driverId,
            updates = updates,
            isConflated = true,
            immediateSync = true
        )
    }

    fun updateDriverStatus(driverId: String, status: String, route: String? = null) {
        val updates = mutableMapOf<String, Any>(
            "status" to status,
            "lastUpdated" to System.currentTimeMillis()
        )
        route?.let { updates["route"] = it }
        val syncId = "DUTY_${driverId}_STATUS"
        com.example.bustrack_app.sync.SyncQueueManager.enqueueUpdate(
            syncId = syncId,
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_DUTY_STATUS,
            targetCollection = "drivers",
            targetDocumentId = driverId,
            updates = updates,
            isConflated = true,
            immediateSync = true
        )
    }

    fun updateDriverTripDirection(driverId: String, tripDirection: String) {
        val updates = mapOf(
            "tripDirection" to tripDirection,
            "lastUpdated" to System.currentTimeMillis()
        )
        val syncId = "DIRECTION_$driverId"
        com.example.bustrack_app.sync.SyncQueueManager.enqueueUpdate(
            syncId = syncId,
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_DUTY_STATUS,
            targetCollection = "drivers",
            targetDocumentId = driverId,
            updates = updates,
            isConflated = true,
            immediateSync = true
        )
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
        com.example.bustrack_app.sync.SyncQueueManager.enqueueUpdate(
            syncId = "LOC_LATEST_$driverId",
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_LOCATION_LATEST,
            targetCollection = "drivers",
            targetDocumentId = driverId,
            updates = updates,
            isConflated = true,
            immediateSync = true
        )
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
        tripDirection: String,
        traveledRouteSegments: List<String> = emptyList(),
        activeTripId: String? = null,
        activeRouteId: String? = null,
        activeRouteName: String? = null,
        accuracy: Float = 0f,
        locationTimestamp: Long = 0L,
        locationStatus: String = "LIVE"
    ) {
        val now = System.currentTimeMillis()
        val updates = mutableMapOf<String, Any?>(
            "latitude" to lat,
            "longitude" to lng,
            "lastUpdated" to now,
            "locationTimestamp" to (if (locationTimestamp > 0L) locationTimestamp else now),
            "accuracy" to accuracy,
            "locationStatus" to locationStatus,
            "eta" to eta,
            "speed" to speed,
            "load" to load,
            "isNavigating" to isNavigating,
            // Keep direction in the same atomic live-state write as the route and
            // stop maps. Tracking clients must never pair return-trip geometry with
            // an older forward-trip direction.
            "tripDirection" to tripDirection
        )
        activeTripId?.let { updates["activeTripId"] = it }
        activeRouteId?.let { updates["activeRouteId"] = it }
        activeRouteName?.let { updates["activeRouteName"] = it }

        if (isNavigating) {
            updates["currentRoutePolyline"] = currentPolyline
            updates["traveledPolyline"] = traveledPolyline
            updates["traveledRouteSegments"] = traveledRouteSegments
            updates["nextStopIndex"] = nextStopIndex
            updates["stopArrivalTimes"] = stopArrivalTimes
            updates["stopEtaTimes"] = stopEtaTimes
        } else {
            updates["activeTripId"] = ""
        }

        // Use conflation so offline GPS ticks overwrite locally and only 1 write occurs on reconnection
        val syncId = "LOC_LATEST_$driverId"
        com.example.bustrack_app.sync.SyncQueueManager.enqueueUpdate(
            syncId = syncId,
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_LOCATION_LATEST,
            targetCollection = "drivers",
            targetDocumentId = driverId,
            updates = updates,
            isConflated = true,
            immediateSync = true
        )
    }

    // --- ATTENDANCE ---
    fun saveAttendance(record: AttendanceRecordModel, onComplete: (Boolean) -> Unit) {
        saveAttendanceWithResult(record) { result ->
            onComplete(result != com.example.bustrack_app.sync.SyncQueueManager.SyncResult.FAILED)
        }
    }

    fun saveAttendanceWithResult(
        record: AttendanceRecordModel,
        onComplete: (com.example.bustrack_app.sync.SyncQueueManager.SyncResult) -> Unit
    ) {
        val normalizedDate = record.date.replace("/", "-")
        val normalizedRecord = record.copy(date = normalizedDate)
        val docId = "${record.studentId}_$normalizedDate"
        val syncId = "ATT_${record.studentId}_$normalizedDate"

        val effectiveTimestamp = if (normalizedRecord.timestamp > 0L) normalizedRecord.timestamp else System.currentTimeMillis()
        val effectiveStopName = if (normalizedRecord.stopName.isNotEmpty()) normalizedRecord.stopName else normalizedRecord.stop
        val effectiveStop = if (normalizedRecord.stop.isNotEmpty()) normalizedRecord.stop else effectiveStopName

        val dataMap = mutableMapOf<String, Any?>(
            "studentId" to normalizedRecord.studentId,
            "studentName" to normalizedRecord.studentName,
            "route" to normalizedRecord.route,
            "stop" to effectiveStop,
            "morningPickup" to normalizedRecord.morningPickup,
            "morningDrop" to normalizedRecord.morningDrop,
            "eveningPickup" to normalizedRecord.eveningPickup,
            "eveningDrop" to normalizedRecord.eveningDrop,
            "date" to normalizedRecord.date,
            "busId" to normalizedRecord.busId,
            "routeId" to normalizedRecord.routeId,
            "stopId" to normalizedRecord.stopId,
            "stopName" to effectiveStopName,
            "tripId" to normalizedRecord.tripId,
            "tripDirection" to normalizedRecord.tripDirection,
            "attendanceType" to normalizedRecord.attendanceType,
            "attendanceStatus" to normalizedRecord.attendanceStatus,
            "timestamp" to effectiveTimestamp,
            "markedByDriverId" to normalizedRecord.markedByDriverId,
            "markedByDriverName" to normalizedRecord.markedByDriverName,
            "syncStatus" to normalizedRecord.syncStatus
        )

        com.example.bustrack_app.sync.SyncQueueManager.enqueueSetWithResult(
            syncId = syncId,
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_ATTENDANCE,
            targetCollection = "attendance",
            targetDocumentId = docId,
            data = dataMap,
            immediateSync = true,
            onComplete = onComplete
        )
    }

    fun updateAttendanceField(studentId: String, date: String, field: String, value: String) {
        val normalizedDate = date.replace("/", "-")
        val docId = "${studentId}_$normalizedDate"
        val syncId = "ATT_FIELD_${studentId}_${normalizedDate}_$field"

        com.example.bustrack_app.sync.SyncQueueManager.enqueueUpdate(
            syncId = syncId,
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_ATTENDANCE,
            targetCollection = "attendance",
            targetDocumentId = docId,
            updates = mapOf(field to value),
            immediateSync = true
        )
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
     * Sends a real structured Driver Alert / Issue Report to Admin.
     * Uses a stable, idempotent [alertId] (generated once when triggered) to prevent duplicate
     * Firestore alert documents across automatic sync retries.
     */
    fun sendDriverAlert(
        driverId: String,
        driverName: String,
        driverEmail: String = "",
        driverPhone: String = "",
        busNumber: String = "",
        routeName: String = "",
        alertType: String,
        customDescription: String = "",
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        locationAddress: String = "",
        tripDirection: String = "FORWARD",
        tripStatus: String = "IDLE",
        alertId: String = java.util.UUID.randomUUID().toString(),
        onComplete: (Boolean) -> Unit = {}
    ) {
        val notifId = alertId
        val syncId = "ALERT_$notifId"

        val effectiveTitle = if (alertType.equals("Other", ignoreCase = true)) {
            "🚨 Driver Alert: Driver Reported Issue"
        } else {
            "🚨 Driver Alert: $alertType"
        }

        val effectiveDesc = if (customDescription.isNotBlank()) {
            customDescription
        } else {
            when (alertType) {
                "Road Block" -> "Road is blocked/closed. Alternate route required."
                "Heavy Traffic" -> "Heavy traffic causing significant delay on route."
                "Accident" -> "Accident reported on route or vehicle involved."
                "Bus Breakdown" -> "Bus has experienced a mechanical breakdown / technical fault."
                "Fuel Issue" -> "Fuel level critical or fuel issue encountered."
                "Bad Weather" -> "Severe weather conditions (heavy rain/fog/storm) causing hazard."
                "Student Emergency" -> "Student emergency requiring immediate medical/administrative help."
                "Police Check" -> "Police / security checkpoint causing delay."
                "Wrong Route" -> "Route road is blocked or inaccessible."
                else -> "Driver reported an issue."
            }
        }

        val locPart = if (locationAddress.isNotBlank()) " near $locationAddress" else ""
        val busPart = if (busNumber.isNotBlank()) " for Bus $busNumber" else ""
        val routePart = if (routeName.isNotBlank()) " on Route $routeName" else ""
        val driverPart = if (driverName.isNotBlank()) " by Driver $driverName" else ""

        val conciseMessage = "$alertType reported$driverPart$busPart$routePart$locPart. $effectiveDesc"

        fun saveWithPhone(resolvedPhone: String) {
            val data = hashMapOf(
                "id" to notifId,
                "recipientId" to "",
                "recipientRole" to "admin",
                "senderId" to driverId,
                "senderRole" to "driver",
                "driverName" to driverName,
                "driverEmail" to driverEmail,
                "driverPhone" to resolvedPhone.trim(),
                "busNumber" to busNumber,
                "routeName" to routeName,
                "alertType" to alertType,
                "title" to effectiveTitle,
                "message" to conciseMessage,
                "description" to effectiveDesc,
                "latitude" to latitude,
                "longitude" to longitude,
                "locationAddress" to locationAddress,
                "tripDirection" to tripDirection,
                "tripStatus" to tripStatus,
                "type" to NotificationModel.TYPE_DRIVER_ALERT,
                "relatedId" to notifId,
                "isRead" to false,
                "timestamp" to com.google.firebase.Timestamp.now()
            )

            com.example.bustrack_app.sync.SyncQueueManager.enqueueSet(
                syncId = syncId,
                actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_DRIVER_ALERT,
                targetCollection = "notifications",
                targetDocumentId = notifId,
                data = data,
                immediateSync = true,
                onComplete = onComplete
            )
        }

        // If phone is already supplied, save directly
        if (driverPhone.trim().isNotBlank()) {
            saveWithPhone(driverPhone.trim())
            return
        }

        // 1. Check local DriverRepository cache
        val cachedDriver = DriverRepository.driverList.value?.find {
            (driverId.isNotBlank() && (it.driverId == driverId || it.id == driverId || it.uid == driverId)) ||
            (driverEmail.isNotBlank() && it.email.trim().equals(driverEmail.trim(), ignoreCase = true)) ||
            (busNumber.isNotBlank() && it.assignedBus.equals(busNumber, ignoreCase = true)) ||
            (driverName.isNotBlank() && it.name.trim().equals(driverName.trim(), ignoreCase = true))
        }
        val cachedPhone = cachedDriver?.phone?.trim() ?: ""
        if (cachedPhone.isNotBlank()) {
            saveWithPhone(cachedPhone)
            return
        }

        // 2. Query Firestore asynchronously (users & drivers collections)
        val queryActions = mutableListOf<((String) -> Unit) -> Unit>()

        if (driverId.isNotBlank()) {
            queryActions.add { callback ->
                db.collection("users").document(driverId).get()
                    .addOnSuccessListener { doc ->
                        val phone = doc.getString("phone") ?: doc.getString("contactNumber") ?: ""
                        callback(phone)
                    }
                    .addOnFailureListener { callback("") }
            }
            queryActions.add { callback ->
                db.collection("drivers").document(driverId).get()
                    .addOnSuccessListener { doc ->
                        val phone = doc.getString("phone") ?: ""
                        callback(phone)
                    }
                    .addOnFailureListener { callback("") }
            }
            queryActions.add { callback ->
                db.collection("drivers").whereEqualTo("uid", driverId).limit(1).get()
                    .addOnSuccessListener { snapshot ->
                        val phone = snapshot.documents.firstOrNull()?.getString("phone") ?: ""
                        callback(phone)
                    }
                    .addOnFailureListener { callback("") }
            }
        }

        if (driverEmail.isNotBlank()) {
            queryActions.add { callback ->
                db.collection("users").whereEqualTo("email", driverEmail.trim().lowercase()).limit(1).get()
                    .addOnSuccessListener { snapshot ->
                        val phone = snapshot.documents.firstOrNull()?.getString("phone") ?: ""
                        callback(phone)
                    }
                    .addOnFailureListener { callback("") }
            }
            queryActions.add { callback ->
                db.collection("drivers").whereEqualTo("email", driverEmail.trim().lowercase()).limit(1).get()
                    .addOnSuccessListener { snapshot ->
                        val phone = snapshot.documents.firstOrNull()?.getString("phone") ?: ""
                        callback(phone)
                    }
                    .addOnFailureListener { callback("") }
            }
        }

        if (busNumber.isNotBlank()) {
            queryActions.add { callback ->
                db.collection("drivers").whereEqualTo("assignedBus", busNumber).limit(1).get()
                    .addOnSuccessListener { snapshot ->
                        val phone = snapshot.documents.firstOrNull()?.getString("phone") ?: ""
                        callback(phone)
                    }
                    .addOnFailureListener { callback("") }
            }
        }

        if (queryActions.isEmpty()) {
            saveWithPhone("")
            return
        }

        var phoneResolved = false
        var completedCount = 0
        val totalQueries = queryActions.size

        queryActions.forEach { action ->
            action { foundPhone ->
                synchronized(notifId) {
                    if (phoneResolved) return@synchronized
                    if (foundPhone.trim().isNotBlank()) {
                        phoneResolved = true
                        saveWithPhone(foundPhone.trim())
                        return@synchronized
                    }
                    completedCount++
                    if (completedCount >= totalQueries && !phoneResolved) {
                        saveWithPhone("")
                    }
                }
            }
        }
    }

    fun getNotificationById(notificationId: String, onResult: (NotificationModel?) -> Unit) {
        if (notificationId.isBlank()) {
            onResult(null)
            return
        }
        db.collection("notifications").document(notificationId).get()
            .addOnSuccessListener { snapshot ->
                val notif = if (snapshot != null && snapshot.exists()) {
                    try {
                        NotificationModel.fromDocument(snapshot)
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRepo", "Error parsing notification $notificationId: ${e.message}", e)
                        null
                    }
                } else null
                onResult(notif)
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    /**
     * Sends a notification to either a specific user (recipientId) or an entire role
     * (recipientRole, e.g. "admin"/"driver"/"parent"/"principal") - pass exactly one.
     * Uses deterministic sync ID and SetOptions.merge() for deduplication and retry safety.
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
        val notifId = if (!id.isNullOrBlank()) id else java.util.UUID.randomUUID().toString()
        val syncId = "NOTIF_$notifId"

        val data = hashMapOf(
            "id" to notifId,
            "recipientId" to (recipientId ?: ""),
            "recipientRole" to (recipientRole ?: ""),
            "title" to title,
            "message" to message,
            "type" to type,
            "relatedId" to relatedId,
            "isRead" to false,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        com.example.bustrack_app.sync.SyncQueueManager.enqueueSet(
            syncId = syncId,
            actionType = com.example.bustrack_app.sync.data.SyncQueueEntity.ACTION_NOTIFICATION,
            targetCollection = "notifications",
            targetDocumentId = notifId,
            data = data,
            immediateSync = true,
            onComplete = onComplete
        )
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepo", "Error listening to personal notifications: ${error.message}", error)
                    return@addSnapshotListener
                }
                personal = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        NotificationModel.fromDocument(doc)
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRepo", "Error parsing personal notif doc ${doc.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
                emit()
            }

        val reg2 = db.collection("notifications")
            .whereEqualTo("recipientRole", role)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    android.util.Log.e("FirebaseRepo", "Error listening to role notifications: ${error.message}", error)
                    return@addSnapshotListener
                }
                roleBroadcast = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        NotificationModel.fromDocument(doc)
                    } catch (e: Exception) {
                        android.util.Log.e("FirebaseRepo", "Error parsing role notif doc ${doc.id}: ${e.message}", e)
                        null
                    }
                } ?: emptyList()
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
     * when their child boards the bus (Present) or is marked Absent.
     * Includes deduplication by studentId + date + period.
     */
    fun notifyParentsOfAttendance(studentId: String, studentName: String, status: String, date: String, isMorning: Boolean) {
        val isPresent = status.equals("Present", true) || status.contains(":")
        val isAbsent = status.equals("Absent", true)
        if (!isPresent && !isAbsent) return

        val period = if (isMorning) "Morning" else "Evening"
        val notifPrefix = if (isPresent) "BOARDED" else "ABSENT"
        val notificationId = "${notifPrefix}_${studentId}_${date.replace("/", "-")}_${period.uppercase()}"

        val notifTitle = if (isPresent) "Child Boarded Bus" else "Attendance Update: Absent"
        val notifMessage = if (isPresent) {
            "Your child, $studentName, has boarded the bus for $period trip on $date."
        } else {
            "Your child, $studentName, was marked absent for $period pickup on $date."
        }

        // Check for duplicate notification before sending
        db.collection("notifications").document(notificationId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    android.util.Log.d("NotifDebug", "Skipping duplicate attendance notification ($notifPrefix) for $studentId on $date")
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
                        val parentIds = requests.map { it.parentId }.distinct()
                        parentIds.forEach { pid ->
                            sendNotification(
                                id = notificationId,
                                recipientId = pid,
                                title = notifTitle,
                                message = notifMessage,
                                type = "ATTENDANCE",
                                relatedId = studentId
                            ) { success ->
                                android.util.Log.d("NotifDebug", "Attendance ($notifPrefix) notification for $studentName sent: $success")
                            }
                        }
                    }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotifDebug", "Error checking for duplicate attendance notif: ${e.message}")
            }
    }

    /**
     * Driver On Duty event:
     * - Notifies Admin and Principal via role broadcast that driver/bus is on duty.
     * - Strictly notifies ONLY approved parents connected to that driver's assigned route.
     * - Deduplicates by date and driver/route to avoid duplicate alerts on activity recreation.
     */
    fun notifyDriverDutyStarted(driverId: String, driverName: String, busNo: String, routeName: String) {
        if (routeName.isBlank()) return
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())

        // 1. Notify Admin (Role broadcast)
        val adminNotifId = "DUTY_ADMIN_${driverId}_${routeName.replace(" ", "_")}_$today"
        sendNotification(
            id = adminNotifId,
            recipientRole = "admin",
            title = "Bus On Duty: $busNo",
            message = "Driver $driverName (Bus $busNo, Route: $routeName) is now On Duty and navigation is active.",
            type = NotificationModel.TYPE_TRIP_STARTED,
            relatedId = routeName
        )

        // 2. Notify Principal (Role broadcast)
        val principalNotifId = "DUTY_PRINCIPAL_${driverId}_${routeName.replace(" ", "_")}_$today"
        sendNotification(
            id = principalNotifId,
            recipientRole = "principal",
            title = "Bus On Duty: $busNo",
            message = "Bus $busNo on Route $routeName (Driver: $driverName) has gone on duty.",
            type = NotificationModel.TYPE_TRIP_STARTED,
            relatedId = routeName
        )

        // 3. Notify ONLY approved parents connected to this assigned route
        db.collection("trackingRequests")
            .whereEqualTo("assignedTrackingRoute", routeName)
            .whereEqualTo("status", "APPROVED")
            .whereEqualTo("trackingEnabled", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val requests = snapshot.documents.mapNotNull { it.toObject<TrackingRequestModel>() }
                val parentIds = requests.map { it.parentId }.distinct()
                parentIds.forEach { parentId ->
                    val parentNotifId = "DUTY_PARENT_${parentId}_${routeName.replace(" ", "_")}_$today"
                    sendNotification(
                        id = parentNotifId,
                        recipientId = parentId,
                        title = "Bus Started Journey",
                        message = "Bus $busNo on route $routeName is now on duty and has started its journey.",
                        type = NotificationModel.TYPE_TRIP_STARTED,
                        relatedId = routeName
                    )
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("NotifDebug", "Failed to query parents for on duty route $routeName: ${e.message}")
            }
    }

    /**
     * Route update notification to parents assigned to that specific route.
     */
    fun notifyParentsOfRouteUpdate(routeName: String, updateMessage: String) {
        if (routeName.isBlank()) return
        db.collection("trackingRequests")
            .whereEqualTo("assignedTrackingRoute", routeName)
            .whereEqualTo("status", "APPROVED")
            .whereEqualTo("trackingEnabled", true)
            .get()
            .addOnSuccessListener { snapshot ->
                val requests = snapshot.documents.mapNotNull { it.toObject<TrackingRequestModel>() }
                val parentIds = requests.map { it.parentId }.distinct()
                parentIds.forEach { parentId ->
                    sendNotification(
                        recipientId = parentId,
                        title = "Route Update: $routeName",
                        message = updateMessage,
                        type = NotificationModel.TYPE_ROUTE_UPDATE,
                        relatedId = routeName
                    )
                }
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

    fun getParent(uid: String, onResult: (ParentModel?) -> Unit) {
        db.collection("parents").document(uid).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.toObject<ParentModel>())
            }
            .addOnFailureListener {
                onResult(null)
            }
    }

    fun getAdminUser(uid: String, onResult: (AdminModel?) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { snapshot ->
                onResult(snapshot.toObject<AdminModel>())
            }
            .addOnFailureListener {
                onResult(null)
            }
    }
}
