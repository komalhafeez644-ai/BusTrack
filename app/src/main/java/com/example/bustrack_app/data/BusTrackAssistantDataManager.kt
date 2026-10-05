package com.example.bustrack_app.data

import android.location.Location
import android.util.Log
import com.example.bustrack_app.models.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Resolves live BusTrack data for AI Chatbot tool execution.
 *
 * Implements robust multi-identifier driver matching (document ID, uid, id/empId, driverId, email),
 * canonical bus number/route normalization, authoritative Firestore live querying with cache fallback,
 * unified On Duty evaluation, and structured diagnostic logging.
 */
object BusTrackAssistantDataManager {

    private val db get() = FirebaseFirestore.getInstance()
    private const val TAG = "CHATBOT_DUTY_RESOLUTION"

    /**
     * Canonical normalization function for bus numbers.
     * Examples: "ICT-2345", "ICT 2345", "ICT#2345", "Bus ICT-2345", "bus ict 2345" -> "ict2345"
     */
    fun normalizeBus(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.lowercase(Locale.ROOT)
            .replace("bus", "")
            .replace("#", "")
            .replace("-", "")
            .replace("_", "")
            .replace("\\s+".toRegex(), "")
            .trim()
    }

    /**
     * Canonical normalization function for route names.
     * Examples: "Gulistan Colony", "Gulistan Colony Road", "Route Gulistan Colony" -> "gulistancolony"
     */
    fun normalizeRoute(raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return raw.lowercase(Locale.ROOT)
            .replace("route", "")
            .replace("road", "")
            .replace("#", "")
            .replace("-", "")
            .replace("_", "")
            .replace("\\s+".toRegex(), "")
            .trim()
    }

    /**
     * ONE centralized On Duty evaluation function.
     * Evaluates true if:
     * - status is "Active", "On Duty", "ACTIVE", "Active (Return Trip)", or contains "active"/"duty"/"online"
     * - isNavigating == true
     * - locationStatus == "LIVE"
     */
    fun isDriverOnDuty(driver: DriverModel?): Boolean {
        if (driver == null) return false
        val status = driver.status.trim()
        val isNavigating = driver.isNavigating
        val locationStatus = driver.locationStatus.trim()

        val isExplicitlyInactive = status.equals("Inactive", ignoreCase = true) ||
                status.equals("Off Duty", ignoreCase = true) ||
                status.equals("OFFLINE", ignoreCase = true) ||
                status.equals("BUS_OFF_DUTY", ignoreCase = true)

        val hasActiveStatus = status.equals("Active", ignoreCase = true) ||
                status.equals("On Duty", ignoreCase = true) ||
                status.equals("ACTIVE", ignoreCase = true) ||
                status.startsWith("Active", ignoreCase = true) ||
                status.startsWith("On Duty", ignoreCase = true) ||
                status.equals("Online", ignoreCase = true)

        val isLiveLocation = locationStatus.equals("LIVE", ignoreCase = true)

        if (isExplicitlyInactive) {
            // Only considered active if navigation or live location override is actively emitting
            return isNavigating || isLiveLocation
        }

        return hasActiveStatus || isNavigating || isLiveLocation
    }

    /**
     * Fetches all drivers from Firestore authoritatively with server read, falling back to cache.
     * Populates document ID onto model if missing.
     */
    private suspend fun fetchAllDriversAuthoritative(): List<Pair<String, DriverModel>> {
        // Try server read first for freshest status
        try {
            val snapshot = db.collection("drivers").get(Source.SERVER).await()
            if (!snapshot.isEmpty) {
                return snapshot.documents.mapNotNull { doc ->
                    val model = doc.toObject(DriverModel::class.java) ?: return@mapNotNull null
                    val docId = doc.id
                    val populated = model.copy(
                        driverId = model.driverId.ifBlank { docId },
                        id = model.id.ifBlank { docId }
                    )
                    Pair(docId, populated)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Server drivers read failed, falling back to default/cache query: ${e.message}")
        }

        // Default query (server with offline cache fallback)
        try {
            val snapshot = db.collection("drivers").get().await()
            if (!snapshot.isEmpty) {
                return snapshot.documents.mapNotNull { doc ->
                    val model = doc.toObject(DriverModel::class.java) ?: return@mapNotNull null
                    val docId = doc.id
                    val populated = model.copy(
                        driverId = model.driverId.ifBlank { docId },
                        id = model.id.ifBlank { docId }
                    )
                    Pair(docId, populated)
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Default drivers read failed, falling back to DriverRepository: ${e.message}")
        }

        // In-memory LiveData fallback
        return DriverRepository.driverList.value.orEmpty().map {
            Pair(it.driverId.ifBlank { it.id }, it)
        }
    }

    /**
     * Matches a driver for an authenticated user using ANY valid identifier:
     * 1. Firestore document ID (e.g. EMP-123)
     * 2. Firebase Auth UID (e.g. 1KHck7zo7sbErNDQolb7a6Vr2Nh1)
     * 3. Driver id / Employee ID
     * 4. Driver driverId
     * 5. Email
     */
    /**
     * Matches a driver for an authenticated user using ANY valid identifier:
     * 1. Firestore document ID (e.g. EMP-123)
     * 2. Firebase Auth UID (e.g. 1KHck7zo7sbErNDQolb7a6Vr2Nh1)
     * 3. Driver id / Employee ID
     * 4. Driver driverId
     * 5. Email
     */
    internal fun matchDriverAuth(
        drivers: List<Pair<String, DriverModel>>,
        driverUid: String,
        driverEmail: String
    ): Pair<String, DriverModel>? {
        val cleanUid = driverUid.trim()
        val cleanEmail = driverEmail.trim().lowercase(Locale.ROOT)

        if (cleanUid.isBlank() && cleanEmail.isBlank()) return null

        return drivers.find { (docId, d) ->
            (cleanUid.isNotEmpty() && (
                docId.equals(cleanUid, ignoreCase = true) ||
                d.uid.equals(cleanUid, ignoreCase = true) ||
                d.driverId.equals(cleanUid, ignoreCase = true) ||
                d.id.equals(cleanUid, ignoreCase = true)
            )) ||
            (cleanEmail.isNotEmpty() && d.email.trim().lowercase(Locale.ROOT) == cleanEmail)
        }
    }

    /**
     * Matches a driver assigned to a bus number or route name using normalized matching.
     */
    internal fun matchDriverForBusOrRoute(
        drivers: List<Pair<String, DriverModel>>,
        requestedBus: String,
        requestedRoute: String,
        routes: List<RouteModel> = emptyList()
    ): Pair<String, DriverModel>? {
        val normReqBus = normalizeBus(requestedBus)
        val normReqRoute = normalizeRoute(requestedRoute)

        // 1. Match by normalized assignedBus (e.g. "ict2345" == "ict2345")
        if (normReqBus.isNotEmpty()) {
            val byBus = drivers.find { (_, d) ->
                normalizeBus(d.assignedBus) == normReqBus
            }
            if (byBus != null) return byBus
        }

        // 2. Match by normalized route name or activeRouteName (e.g. "gulistancolony")
        if (normReqRoute.isNotEmpty()) {
            val byRoute = drivers.find { (_, d) ->
                val normDRoute = normalizeRoute(d.route)
                val normDActive = normalizeRoute(d.activeRouteName)
                (normDRoute.isNotEmpty() && (normDRoute == normReqRoute || normDRoute.contains(normReqRoute) || normReqRoute.contains(normDRoute))) ||
                (normDActive.isNotEmpty() && (normDActive == normReqRoute || normDActive.contains(normReqRoute) || normReqRoute.contains(normDActive)))
            }
            if (byRoute != null) return byRoute
        }

        // 3. Match via RouteModel links (Route has busNo and routeName and driverName)
        if (routes.isNotEmpty()) {
            val matchingRoute = routes.find { r ->
                (normReqRoute.isNotEmpty() && normalizeRoute(r.routeName) == normReqRoute) ||
                (normReqBus.isNotEmpty() && normalizeBus(r.busNo) == normReqBus)
            }
            if (matchingRoute != null) {
                val routeBusNorm = normalizeBus(matchingRoute.busNo)
                val routeDriverName = matchingRoute.driverName.trim().lowercase(Locale.ROOT)

                val byRouteEntity = drivers.find { (_, d) ->
                    (routeBusNorm.isNotEmpty() && normalizeBus(d.assignedBus) == routeBusNorm) ||
                    (routeDriverName.isNotEmpty() && d.name.trim().lowercase(Locale.ROOT) == routeDriverName)
                }
                if (byRouteEntity != null) return byRouteEntity
            }
        }

        // 4. Substring case-insensitive fallback
        return drivers.find { (_, d) ->
            (requestedBus.isNotBlank() && d.assignedBus?.contains(requestedBus, ignoreCase = true) == true) ||
            (requestedRoute.isNotBlank() && d.route?.contains(requestedRoute, ignoreCase = true) == true) ||
            (requestedRoute.isNotBlank() && d.activeRouteName.contains(requestedRoute, ignoreCase = true))
        }
    }

    // ==========================================
    // PARENT TOOLS RESOLVER
    // ==========================================

    /**
     * Resolves child details, assigned bus/route, and live tracking metrics for a Parent.
     * Optionally accepts explicit queryBus and queryRoute if mentioned by user.
     */
    suspend fun resolveParentChildAndBusStatus(
        parentUid: String,
        queryBus: String? = null,
        queryRoute: String? = null
    ): String = withContext(Dispatchers.IO) {
        val result = JSONObject()
        try {
            // 1. Fetch tracking requests for this parent if parentUid is present
            val reqSnapshot = if (parentUid.isNotBlank()) {
                try {
                    db.collection("trackingRequests")
                        .whereEqualTo("parentId", parentUid)
                        .get()
                        .await()
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to query trackingRequests: ${e.message}")
                    null
                }
            } else null

            val requests = reqSnapshot?.documents?.mapNotNull { it.toObject(TrackingRequestModel::class.java) }.orEmpty()
            val approvedRequest = requests.find { it.status.equals("APPROVED", ignoreCase = true) && it.trackingEnabled }
            val latestRequest = requests.firstOrNull()

            // Fetch linked student record if approved request exists
            var student: StudentModel? = null
            if (approvedRequest != null) {
                val studentId = approvedRequest.studentId
                val rollNumber = approvedRequest.rollNumber
                if (studentId.isNotBlank()) {
                    val doc = db.collection("students").document(studentId).get().await()
                    if (doc.exists()) {
                        student = doc.toObject(StudentModel::class.java)
                    }
                }
                if (student == null && rollNumber.isNotBlank()) {
                    val rollQuery = db.collection("students").whereEqualTo("rollNumber", rollNumber).limit(1).get().await()
                    student = rollQuery.documents.firstOrNull()?.toObject(StudentModel::class.java)
                }
            }

            val assignedRouteFromRequest = approvedRequest?.assignedTrackingRoute.orEmpty()
            val effectiveRoute = queryRoute?.takeIf { it.isNotBlank() }
                ?: student?.route?.takeIf { it.isNotBlank() }
                ?: assignedRouteFromRequest
            val effectiveBusNo = queryBus?.takeIf { it.isNotBlank() }
                ?: student?.busNo.orEmpty()
            val childStopName = student?.stopName.orEmpty()

            // 3. Fetch authoritative drivers list & routes list
            val drivers = fetchAllDriversAuthoritative()
            val routes = try {
                db.collection("routes").get().await().documents.mapNotNull { it.toObject(RouteModel::class.java) }
            } catch (_: Exception) {
                RouteRepository.routeList.value.orEmpty()
            }

            // 4. Match driver by bus number or route name
            val matchedDriverPair = matchDriverForBusOrRoute(
                drivers = drivers,
                requestedBus = effectiveBusNo,
                requestedRoute = effectiveRoute,
                routes = routes
            )

            val matchedDriver = matchedDriverPair?.second
            val matchedDocId = matchedDriverPair?.first.orEmpty()
            val onDuty = isDriverOnDuty(matchedDriver)

            result.put("found", matchedDriver != null)
            result.put("onDuty", onDuty)
            result.put("hasTrackingRequest", approvedRequest != null)
            result.put("status", if (onDuty) "Active" else (matchedDriver?.status ?: "BUS_OFF_DUTY"))

            val studentJson = JSONObject()
                .put("name", student?.name ?: approvedRequest?.parentName ?: "Child")
                .put("rollNumber", student?.rollNumber ?: approvedRequest?.rollNumber ?: "")
                .put("grade", student?.grade ?: "N/A")
                .put("assignedRoute", effectiveRoute.ifBlank { "Unassigned" })
                .put("assignedBus", effectiveBusNo.ifBlank { matchedDriver?.assignedBus ?: "Unassigned" })
                .put("assignedStop", childStopName.ifBlank { "Unassigned" })
                .put("pickupTime", student?.pickupTime ?: "")
            result.put("student", studentJson)

            if (matchedDriver != null) {
                val trackingJson = JSONObject()
                    .put("driverDocumentId", matchedDocId)
                    .put("driverUid", matchedDriver.uid)
                    .put("driverName", matchedDriver.name)
                    .put("busNumber", matchedDriver.assignedBus ?: effectiveBusNo)
                    .put("assignedRoute", matchedDriver.route ?: effectiveRoute)
                    .put("activeRouteName", matchedDriver.activeRouteName)
                    .put("status", matchedDriver.status)
                    .put("isOnDuty", onDuty)
                    .put("isNavigating", matchedDriver.isNavigating)
                    .put("locationStatus", matchedDriver.locationStatus)
                    .put("speedKph", matchedDriver.speed)
                    .put("tripDirection", matchedDriver.tripDirection)
                    .put("generalEta", matchedDriver.eta)
                    .put("currentLoad", matchedDriver.load)

                // Route stops & child ETA calculation
                var childStopEta: String? = null
                var nextStopName: String? = null

                val routeModel = routes.find {
                    (effectiveRoute.isNotBlank() && normalizeRoute(it.routeName) == normalizeRoute(effectiveRoute)) ||
                    (effectiveBusNo.isNotBlank() && normalizeBus(it.busNo) == normalizeBus(effectiveBusNo)) ||
                    (matchedDriver.assignedBus != null && normalizeBus(it.busNo) == normalizeBus(matchedDriver.assignedBus))
                }

                if (routeModel != null && routeModel.stopsList.isNotEmpty()) {
                    val nextIdx = matchedDriver.nextStopIndex.coerceIn(0, routeModel.stopsList.size - 1)
                    if (nextIdx < routeModel.stopsList.size) {
                        nextStopName = routeModel.stopsList[nextIdx].stopName
                    }

                    val childStopIdx = routeModel.stopsList.indexOfFirst {
                        it.stopName.equals(childStopName, ignoreCase = true)
                    }

                    if (childStopIdx != -1) {
                        val arrivalTime = matchedDriver.stopArrivalTimes[childStopIdx.toString()]
                        val etaTime = matchedDriver.stopEtaTimes[childStopIdx.toString()]

                        childStopEta = when {
                            arrivalTime != null -> "Arrived at $arrivalTime"
                            childStopIdx < matchedDriver.nextStopIndex -> "Stop already passed"
                            !etaTime.isNullOrBlank() -> etaTime
                            onDuty && matchedDriver.latitude != 0.0 -> {
                                val targetStop = routeModel.stopsList[childStopIdx]
                                val busLoc = Location("bus").apply {
                                    latitude = matchedDriver.latitude
                                    longitude = matchedDriver.longitude
                                }
                                val stopLoc = Location("stop").apply {
                                    latitude = targetStop.latitude
                                    longitude = targetStop.longitude
                                }
                                val distMeters = busLoc.distanceTo(stopLoc)
                                val speedKph = if (matchedDriver.speed > 5) matchedDriver.speed else 30.0
                                val timeMin = ((distMeters / 1000.0) / speedKph * 60).toInt()
                                if (timeMin <= 1) "1-2 mins" else "$timeMin mins"
                            }
                            else -> "TBD"
                        }
                    }
                }

                trackingJson.put("nextStopName", nextStopName ?: "N/A")
                trackingJson.put("childStopName", childStopName)
                trackingJson.put("etaToChildStop", childStopEta ?: matchedDriver.eta)
                result.put("liveTracking", trackingJson)
            } else {
                result.put("liveTracking", JSONObject()
                    .put("status", "BUS_OFF_DUTY")
                    .put("isOnDuty", false)
                    .put("message", "The bus is currently off duty or has no driver assigned."))
            }

            logDiagnostic(
                inputUid = parentUid,
                inputBus = effectiveBusNo,
                inputRoute = effectiveRoute,
                matched = matchedDriverPair,
                onDuty = onDuty,
                dataSource = "authoritative_firestore+normalization"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving parent live status: ${e.message}", e)
            result.put("error", e.localizedMessage ?: "Failed to resolve live status.")
        }
        result.toString()
    }

    /**
     * Resolves child attendance records (morning pickup, morning drop, evening pickup, evening drop).
     */
    suspend fun resolveChildAttendance(parentUid: String, dateQuery: String? = null): String = withContext(Dispatchers.IO) {
        val result = JSONObject()
        try {
            if (parentUid.isBlank()) {
                result.put("error", "Parent user is not authenticated.")
                return@withContext result.toString()
            }

            val reqSnapshot = db.collection("trackingRequests")
                .whereEqualTo("parentId", parentUid)
                .whereEqualTo("status", "APPROVED")
                .get()
                .await()

            val studentId = reqSnapshot.documents.firstOrNull()?.getString("studentId")
            val rollNumber = reqSnapshot.documents.firstOrNull()?.getString("rollNumber")

            if (studentId.isNullOrBlank() && rollNumber.isNullOrBlank()) {
                result.put("error", "No approved student record found for this parent.")
                return@withContext result.toString()
            }

            var resolvedStudentId = studentId.orEmpty()
            var studentName = "Your child"
            if (resolvedStudentId.isNotBlank()) {
                val sDoc = db.collection("students").document(resolvedStudentId).get().await()
                if (sDoc.exists()) {
                    studentName = sDoc.getString("name") ?: studentName
                }
            } else if (!rollNumber.isNullOrBlank()) {
                val sDoc = db.collection("students").whereEqualTo("rollNumber", rollNumber).limit(1).get().await()
                resolvedStudentId = sDoc.documents.firstOrNull()?.id ?: rollNumber
                studentName = sDoc.documents.firstOrNull()?.getString("name") ?: studentName
            }

            val targetDate = if (!dateQuery.isNullOrBlank()) {
                dateQuery.trim().replace("/", "-")
            } else {
                SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            }

            val docId = "${resolvedStudentId}_$targetDate"
            val altDocId = "${resolvedStudentId}_${targetDate.replace("-", "/")}"

            var attRecord: AttendanceRecordModel? = null
            val doc1 = db.collection("attendance").document(docId).get().await()
            if (doc1.exists()) {
                attRecord = doc1.toObject(AttendanceRecordModel::class.java)
            } else {
                val doc2 = db.collection("attendance").document(altDocId).get().await()
                if (doc2.exists()) {
                    attRecord = doc2.toObject(AttendanceRecordModel::class.java)
                } else {
                    val q1 = db.collection("attendance")
                        .whereEqualTo("studentId", resolvedStudentId)
                        .whereEqualTo("date", targetDate)
                        .limit(1)
                        .get()
                        .await()
                    attRecord = q1.documents.firstOrNull()?.toObject(AttendanceRecordModel::class.java)
                }
            }

            result.put("studentName", studentName)
            result.put("date", targetDate)

            if (attRecord != null) {
                result.put("recordFound", true)
                result.put("morningPickup", attRecord.morningPickup.ifBlank { "--" })
                result.put("morningDrop", attRecord.morningDrop.ifBlank { "--" })
                result.put("eveningPickup", attRecord.eveningPickup.ifBlank { "--" })
                result.put("eveningDrop", attRecord.eveningDrop.ifBlank { "--" })
                result.put("attendanceStatus", attRecord.attendanceStatus.ifBlank { attRecord.morningPickup })
                result.put("tripDirection", attRecord.tripDirection)
            } else {
                result.put("recordFound", false)
                result.put("message", "No attendance marked yet for $targetDate.")
                result.put("morningPickup", "--")
                result.put("morningDrop", "--")
                result.put("eveningPickup", "--")
                result.put("eveningDrop", "--")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving attendance: ${e.message}", e)
            result.put("error", e.localizedMessage ?: "Failed to resolve attendance.")
        }
        result.toString()
    }

    /**
     * Resolves all tracking requests submitted by the parent.
     */
    suspend fun resolveParentTrackingRequests(parentUid: String): String = withContext(Dispatchers.IO) {
        val result = JSONObject()
        try {
            if (parentUid.isBlank()) {
                result.put("error", "Parent user is not authenticated.")
                return@withContext result.toString()
            }

            val snapshot = db.collection("trackingRequests")
                .whereEqualTo("parentId", parentUid)
                .get()
                .await()

            val requests = snapshot.documents.mapNotNull { it.toObject(TrackingRequestModel::class.java) }
            val array = JSONArray()
            requests.forEach { req ->
                val obj = JSONObject()
                    .put("requestId", req.requestId)
                    .put("rollNumber", req.rollNumber)
                    .put("status", req.status)
                    .put("trackingEnabled", req.trackingEnabled)
                    .put("trackingState", req.trackingState)
                    .put("assignedTrackingRoute", req.assignedTrackingRoute ?: "None")
                array.put(obj)
            }
            result.put("totalRequests", requests.size)
            result.put("requests", array)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving tracking requests: ${e.message}", e)
            result.put("error", e.localizedMessage ?: "Failed to resolve tracking requests.")
        }
        result.toString()
    }

    // ==========================================
    // DRIVER TOOLS RESOLVER
    // ==========================================

    /**
     * Resolves assigned bus, assigned route, students count, and duty state for a Driver.
     */
    suspend fun resolveDriverAssignedDuty(driverUid: String, driverEmail: String): String = withContext(Dispatchers.IO) {
        val result = JSONObject()
        try {
            val drivers = fetchAllDriversAuthoritative()
            val matchedDriverPair = matchDriverAuth(drivers, driverUid, driverEmail)
            val driver = matchedDriverPair?.second
            val docId = matchedDriverPair?.first.orEmpty()

            if (driver == null) {
                result.put("found", false)
                result.put("onDuty", false)
                result.put("error", "Driver profile not found.")
                logDiagnostic(
                    inputUid = driverUid,
                    inputBus = "",
                    inputRoute = "",
                    matched = null,
                    onDuty = false,
                    dataSource = "driverAuth:notFound"
                )
                return@withContext result.toString()
            }

            val busNo = driver.assignedBus.orEmpty()
            val routeName = driver.route.orEmpty()

            val routes = try {
                db.collection("routes").get().await().documents.mapNotNull { it.toObject(RouteModel::class.java) }
            } catch (_: Exception) {
                RouteRepository.routeList.value.orEmpty()
            }

            val route = routes.find {
                (routeName.isNotBlank() && normalizeRoute(it.routeName) == normalizeRoute(routeName)) ||
                (busNo.isNotBlank() && normalizeBus(it.busNo) == normalizeBus(busNo))
            }

            val studentsCount = try {
                db.collection("students")
                    .whereEqualTo("route", route?.routeName ?: routeName)
                    .get()
                    .await()
                    .size()
            } catch (_: Exception) {
                route?.studentsCount ?: 0
            }

            val onDuty = isDriverOnDuty(driver)

            result.put("found", true)
            result.put("onDuty", onDuty)
            result.put("driverDocumentId", docId)
            result.put("driverUid", driver.uid)
            result.put("driverId", driver.id)
            result.put("driverName", driver.name)
            result.put("assignedBus", busNo.ifBlank { "Not Assigned" })
            result.put("assignedRoute", route?.routeName ?: routeName.ifBlank { "Not Assigned" })
            result.put("activeRouteName", driver.activeRouteName)
            result.put("routeCode", route?.routeCode ?: "N/A")
            result.put("startPoint", route?.startPoint ?: "N/A")
            result.put("endPoint", route?.endPoint ?: "N/A")
            result.put("stopsCount", route?.stopsList?.size ?: route?.stopsCount ?: 0)
            result.put("studentsCount", studentsCount)
            result.put("status", driver.status)
            result.put("isOnDuty", onDuty)
            result.put("isNavigating", driver.isNavigating)
            result.put("locationStatus", driver.locationStatus)
            result.put("tripDirection", driver.tripDirection)
            result.put("speedKph", driver.speed)
            result.put("currentLoad", driver.load)

            logDiagnostic(
                inputUid = driverUid,
                inputBus = busNo,
                inputRoute = routeName,
                matched = matchedDriverPair,
                onDuty = onDuty,
                dataSource = "authoritative_driver_auth"
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving driver duty: ${e.message}", e)
            result.put("error", e.localizedMessage ?: "Failed to resolve driver duty.")
        }
        result.toString()
    }

    /**
     * Resolves the full stop sequence, upcoming next stop, and ETA map for a Driver.
     */
    suspend fun resolveDriverRouteAndStops(driverUid: String, driverEmail: String): String = withContext(Dispatchers.IO) {
        val result = JSONObject()
        try {
            val drivers = fetchAllDriversAuthoritative()
            val matchedDriverPair = matchDriverAuth(drivers, driverUid, driverEmail)
            val driver = matchedDriverPair?.second

            if (driver == null) {
                result.put("found", false)
                result.put("onDuty", false)
                result.put("error", "Driver profile not found.")
                return@withContext result.toString()
            }

            val routeName = driver.route.orEmpty()
            val busNo = driver.assignedBus.orEmpty()

            val routes = try {
                db.collection("routes").get().await().documents.mapNotNull { it.toObject(RouteModel::class.java) }
            } catch (_: Exception) {
                RouteRepository.routeList.value.orEmpty()
            }

            val route = routes.find {
                (routeName.isNotBlank() && normalizeRoute(it.routeName) == normalizeRoute(routeName)) ||
                (busNo.isNotBlank() && normalizeBus(it.busNo) == normalizeBus(busNo))
            }

            if (route == null || route.stopsList.isEmpty()) {
                result.put("error", "No route stops found for assigned route.")
                return@withContext result.toString()
            }

            val stopsArray = JSONArray()
            route.stopsList.forEachIndexed { index, stop ->
                val stopObj = JSONObject()
                    .put("index", index)
                    .put("stopName", stop.stopName)
                    .put("scheduledTime", stop.time)
                    .put("status", when {
                        driver.stopArrivalTimes.containsKey(index.toString()) -> "ARRIVED (${driver.stopArrivalTimes[index.toString()]})"
                        index < driver.nextStopIndex -> "COMPLETED"
                        index == driver.nextStopIndex -> "NEXT_STOP"
                        else -> "UPCOMING"
                    })
                    .put("eta", driver.stopEtaTimes[index.toString()] ?: "TBD")
                stopsArray.put(stopObj)
            }

            val nextStopName = if (driver.nextStopIndex in route.stopsList.indices) {
                route.stopsList[driver.nextStopIndex].stopName
            } else "Trip Complete"

            val nextStopEta = driver.stopEtaTimes[driver.nextStopIndex.toString()] ?: driver.eta
            val onDuty = isDriverOnDuty(driver)

            result.put("found", true)
            result.put("onDuty", onDuty)
            result.put("routeName", route.routeName)
            result.put("tripDirection", driver.tripDirection)
            result.put("nextStopIndex", driver.nextStopIndex)
            result.put("nextStopName", nextStopName)
            result.put("nextStopEta", nextStopEta)
            result.put("totalStops", route.stopsList.size)
            result.put("stops", stopsArray)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving driver route stops: ${e.message}", e)
            result.put("error", e.localizedMessage ?: "Failed to resolve route stops.")
        }
        result.toString()
    }

    /**
     * Resolves attendance load summary for Driver's current route run today.
     */
    suspend fun resolveDriverAttendanceSummary(driverUid: String, driverEmail: String): String = withContext(Dispatchers.IO) {
        val result = JSONObject()
        try {
            val drivers = fetchAllDriversAuthoritative()
            val matchedDriverPair = matchDriverAuth(drivers, driverUid, driverEmail)
            val driver = matchedDriverPair?.second

            val routeName = driver?.route.orEmpty()
            if (routeName.isBlank()) {
                result.put("error", "Driver has no assigned route.")
                return@withContext result.toString()
            }

            val today = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault()).format(Date())
            val altToday = today.replace("-", "/")

            val attDocs = db.collection("attendance")
                .whereEqualTo("route", routeName)
                .get()
                .await()

            val records = attDocs.documents.mapNotNull { it.toObject(AttendanceRecordModel::class.java) }
                .filter { it.date == today || it.date == altToday }

            val totalStudents = db.collection("students")
                .whereEqualTo("route", routeName)
                .get()
                .await()
                .size()

            val morningPresent = records.count { it.morningPickup.equals("Present", true) || it.morningPickup.contains(":") }
            val morningAbsent = records.count { it.morningPickup.equals("Absent", true) }
            val morningLeave = records.count { it.morningPickup.equals("Leave", true) }

            val eveningPresent = records.count { it.eveningPickup.equals("Present", true) || it.eveningPickup.contains(":") }
            val eveningDropped = records.count { it.eveningDrop.equals("Dropped", true) || it.eveningDrop.contains(":") }

            result.put("routeName", routeName)
            result.put("date", today)
            result.put("totalAssignedStudents", totalStudents)
            result.put("morningPresentCount", morningPresent)
            result.put("morningAbsentCount", morningAbsent)
            result.put("morningLeaveCount", morningLeave)
            result.put("eveningPresentCount", eveningPresent)
            result.put("eveningDroppedCount", eveningDropped)
            result.put("driverLiveLoad", driver?.load ?: "$morningPresent/$totalStudents")
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving driver attendance summary: ${e.message}", e)
            result.put("error", e.localizedMessage ?: "Failed to resolve attendance summary.")
        }
        result.toString()
    }

    // ==========================================
    // NOTIFICATIONS RESOLVER
    // ==========================================

    /**
     * Resolves the latest notifications for the current user.
     */
    suspend fun resolveRecentNotifications(uid: String, role: String): String = withContext(Dispatchers.IO) {
        val result = JSONObject()
        try {
            if (uid.isBlank()) {
                result.put("error", "User is not authenticated.")
                return@withContext result.toString()
            }

            val notifs = mutableListOf<NotificationModel>()

            val personalQuery = db.collection("notifications")
                .whereEqualTo("recipientId", uid)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()
            notifs.addAll(personalQuery.documents.mapNotNull { NotificationModel.fromDocument(it) })

            if (role.isNotBlank()) {
                val roleQuery = db.collection("notifications")
                    .whereEqualTo("recipientRole", role)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .await()
                notifs.addAll(roleQuery.documents.mapNotNull { NotificationModel.fromDocument(it) })
            }

            val sorted = notifs.distinctBy { it.id }.sortedByDescending { it.timestamp?.time ?: 0L }.take(5)
            val array = JSONArray()
            val sdf = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault())
            sorted.forEach { n ->
                val obj = JSONObject()
                    .put("title", n.title)
                    .put("message", n.message)
                    .put("type", n.type)
                    .put("isRead", n.isRead)
                    .put("time", if (n.timestamp != null) sdf.format(n.timestamp) else "")
                array.put(obj)
            }
            result.put("total", sorted.size)
            result.put("notifications", array)
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving notifications: ${e.message}", e)
            result.put("error", e.localizedMessage ?: "Failed to resolve notifications.")
        }
        result.toString()
    }

    /**
     * Diagnostic structured logging for Task 8 verification.
     */
    private fun logDiagnostic(
        inputUid: String,
        inputBus: String,
        inputRoute: String,
        matched: Pair<String, DriverModel>?,
        onDuty: Boolean,
        dataSource: String
    ) {
        val driver = matched?.second
        val logBlock = """
            |==================================================
            |CHATBOT_DUTY_RESOLUTION
            |inputUid=$inputUid
            |inputBus=$inputBus
            |inputRoute=$inputRoute
            |resolvedDocumentId=${matched?.first ?: "NONE"}
            |resolvedUid=${driver?.uid ?: "NONE"}
            |resolvedEmployeeId=${driver?.id ?: "NONE"}
            |resolvedDriverId=${driver?.driverId ?: "NONE"}
            |resolvedBus=${driver?.assignedBus ?: "NONE"}
            |resolvedRoute=${driver?.route ?: "NONE"}
            |resolvedActiveRouteName=${driver?.activeRouteName ?: "NONE"}
            |resolvedStatus=${driver?.status ?: "NONE"}
            |resolvedIsNavigating=${driver?.isNavigating ?: false}
            |resolvedLocationStatus=${driver?.locationStatus ?: "NONE"}
            |resolvedOnDuty=$onDuty
            |dataSource=$dataSource
            |lastUpdated=${driver?.lastUpdated ?: 0L}
            |==================================================
        """.trimMargin()
        Log.d(TAG, logBlock)
    }
}
