package utils

import android.content.Context
import android.util.Log
import com.example.bustrack_app.models.ActiveTripState
import com.example.bustrack_app.models.DriverModel
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TripRecoveryHelper {

    private const val TAG = "TripRecoveryHelper"
    private const val PREFS_NAME = "trip_recovery_prefs"
    private const val PREFS_COMPLETED = "completed_trips_prefs"
    private const val KEY_ACTIVE_TRIP = "key_active_trip_state"
    private const val KEY_COMPLETED_IDS = "key_completed_trip_ids"
    private const val TRIP_TIMEOUT_MS = 12 * 60 * 60 * 1000L // 12 hours

    private val gson = Gson()

    /**
     * Generates a deterministic stable trip ID for a driver's trip session.
     * Uniquely associates Driver + Bus + Route + Trip Period/Direction + Trip Date.
     */
    fun generateTripId(
        driverId: String,
        busNumber: String = "",
        routeName: String,
        direction: String,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
        val cleanRoute = routeName.trim().replace(" ", "_").replace("/", "_").replace("-", "_")
        val cleanDriver = driverId.trim().replace(" ", "_")
        val cleanBus = busNumber.trim().replace(" ", "_").replace("/", "_").replace("-", "_")
        val busPart = if (cleanBus.isNotEmpty()) "${cleanBus}_" else ""
        return "TRIP_${cleanDriver}_${busPart}${cleanRoute}_${date}_${direction.uppercase()}"
    }

    /**
     * Backward-compatible overload for generateTripId.
     */
    fun generateTripId(driverId: String, routeName: String, direction: String, timestamp: Long = System.currentTimeMillis()): String {
        return generateTripId(driverId = driverId, busNumber = "", routeName = routeName, direction = direction, timestamp = timestamp)
    }

    /**
     * Persists the active trip state to SharedPreferences (trip_recovery_prefs).
     */
    fun saveActiveTrip(context: Context, state: ActiveTripState) {
        try {
            val json = gson.toJson(state)
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACTIVE_TRIP, json)
                .apply()
            Log.d(TAG, "TRIP_SYNC: Saved active trip state: ${state.tripId}, stopIndex: ${state.nextStopIndex}, nav: ${state.isNavigating}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving active trip state: ${e.message}", e)
        }
    }

    /**
     * Retrieves the persisted active trip state from local storage.
     */
    fun getActiveTrip(context: Context): ActiveTripState? {
        return try {
            val json = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_ACTIVE_TRIP, null) ?: return null
            val state = gson.fromJson(json, ActiveTripState::class.java)
            if (isTripValid(state, context)) {
                state
            } else {
                Log.d(TAG, "TRIP_INVALIDATED: Persisted trip state is stale, completed or invalid. Clearing.")
                clearActiveTrip(context)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading active trip state: ${e.message}", e)
            null
        }
    }

    /**
     * Marks a trip as explicitly completed, preventing it from ever being restored as active.
     */
    fun markTripCompleted(context: Context, tripId: String) {
        if (tripId.isBlank()) return
        try {
            val prefs = context.getSharedPreferences(PREFS_COMPLETED, Context.MODE_PRIVATE)
            val currentSet = prefs.getStringSet(KEY_COMPLETED_IDS, emptySet())?.toMutableSet() ?: mutableSetOf()
            currentSet.add(tripId)
            prefs.edit().putStringSet(KEY_COMPLETED_IDS, currentSet).apply()
            Log.d(TAG, "TRIP_COMPLETED: Registered completed trip ID: $tripId")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking trip completed: ${e.message}", e)
        }
    }

    /**
     * Checks if a trip has already been completed.
     */
    fun isTripCompleted(context: Context, tripId: String): Boolean {
        if (tripId.isBlank()) return false
        return try {
            val prefs = context.getSharedPreferences(PREFS_COMPLETED, Context.MODE_PRIVATE)
            val currentSet = prefs.getStringSet(KEY_COMPLETED_IDS, emptySet()) ?: emptySet()
            currentSet.contains(tripId)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking completed trip status: ${e.message}", e)
            false
        }
    }

    /**
     * Clears the active trip state from local storage upon trip completion.
     */
    fun clearActiveTrip(context: Context) {
        try {
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .remove(KEY_ACTIVE_TRIP)
                .apply()
            Log.d(TAG, "Cleared active trip state from local storage.")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing active trip state: ${e.message}", e)
        }
    }

    /**
     * Validates whether a trip state is fresh, unfinished, not completed, and belongs to today.
     */
    fun isTripValid(state: ActiveTripState?, context: Context? = null): Boolean {
        if (state == null) return false
        if (!state.isNavigating) return false
        if (state.tripStatus != "ACTIVE") return false
        if (state.driverId.isBlank() || (state.routeId.isBlank() && state.routeName.isBlank())) return false

        // Check if explicitly marked completed
        if (context != null && isTripCompleted(context, state.tripId)) {
            Log.d(TAG, "TRIP_INVALIDATED: Trip ${state.tripId} was previously marked COMPLETED.")
            return false
        }

        val now = System.currentTimeMillis()
        val elapsed = now - state.lastUpdated
        if (elapsed > TRIP_TIMEOUT_MS) {
            Log.d(TAG, "TRIP_INVALIDATED: Trip expired: elapsed ${elapsed / 1000 / 60} minutes > 12h")
            return false
        }

        // Check if calendar date matches today
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
        val tripDateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(state.tripStartTime))
        if (todayStr != tripDateStr) {
            Log.d(TAG, "TRIP_INVALIDATED: Trip belongs to a previous date ($tripDateStr != $todayStr)")
            return false
        }

        return true
    }

    /**
     * Resolves local active-trip state against remote Firestore driver state.
     * Implements strict conflict resolution:
     * 1. Never overwrites a newer local stop index with a stale remote stop index (no moving backwards).
     * 2. Never restores a completed/cancelled trip.
     * 3. Retains local active trip when offline or remote is uninitialized.
     */
    fun resolveConflict(
        localState: ActiveTripState?,
        remoteDriver: DriverModel?,
        context: Context
    ): ActiveTripState? {
        val isLocalValid = localState != null && isTripValid(localState, context)

        // If local trip was completed, never restore from stale Firestore
        if (localState != null && isTripCompleted(context, localState.tripId)) {
            Log.d(TAG, "TRIP_RECOVERY_CONFLICT: Local trip ${localState.tripId} is COMPLETED. Ignoring remote state.")
            return null
        }

        // Check if remote tripId is completed
        val remoteTripId = remoteDriver?.activeTripId.orEmpty()
        if (remoteTripId.isNotEmpty() && isTripCompleted(context, remoteTripId)) {
            Log.d(TAG, "TRIP_RECOVERY_CONFLICT: Remote trip $remoteTripId is COMPLETED. Ignoring remote state.")
            return if (isLocalValid && localState!!.tripId != remoteTripId) localState else null
        }

        if (isLocalValid && localState != null) {
            if (remoteDriver == null || !remoteDriver.isNavigating) {
                Log.d(TAG, "TRIP_RESTORE: Local active trip ${localState.tripId} chosen (remote is idle/offline).")
                return localState
            }

            // Both local and remote are active
            if (remoteDriver.activeTripId.isNotEmpty() && remoteDriver.activeTripId == localState.tripId) {
                // Same trip: Merge stop states, never moving backward
                val authoritativeNextStopIndex = maxOf(localState.nextStopIndex, remoteDriver.nextStopIndex)
                if (localState.nextStopIndex != remoteDriver.nextStopIndex) {
                    Log.w(TAG, "TRIP_RECOVERY_CONFLICT: Next stop index mismatch (local: ${localState.nextStopIndex}, remote: ${remoteDriver.nextStopIndex}). Resolving to $authoritativeNextStopIndex without moving backward.")
                }

                // Merge stop arrival times and states (local prioritized)
                val mergedArrivals = mutableMapOf<Int, String>()
                remoteDriver.stopArrivalTimes.forEach { (k, v) ->
                    k.toIntOrNull()?.let { mergedArrivals[it] = v }
                }
                mergedArrivals.putAll(localState.stopArrivalTimes)

                val mergedStates = mutableMapOf<Int, String>()
                remoteDriver.stopArrivalTimes.forEach { (k, v) ->
                    k.toIntOrNull()?.let { idx ->
                        mergedStates[idx] = if (v == "Skipped") "SKIPPED" else if (idx < authoritativeNextStopIndex) "COMPLETED" else "ARRIVED"
                    }
                }
                mergedStates.putAll(localState.stopStates)

                return localState.copy(
                    nextStopIndex = authoritativeNextStopIndex,
                    stopArrivalTimes = mergedArrivals,
                    stopStates = mergedStates,
                    lastUpdated = maxOf(localState.lastUpdated, remoteDriver.lastUpdated)
                )
            } else {
                // Different trip IDs: local active session takes precedence
                Log.d(TAG, "TRIP_RESTORE: Local trip ${localState.tripId} takes precedence over remote ${remoteDriver.activeTripId}.")
                return localState
            }
        }

        // Local state is not valid/present, check if remote has an active trip
        if (remoteDriver != null && remoteDriver.isNavigating &&
            (!remoteDriver.activeRouteName.isNullOrBlank() || !remoteDriver.activeTripId.isNullOrBlank() || !remoteDriver.route.isNullOrBlank())
        ) {
            val direction = remoteDriver.tripDirection.ifEmpty { "FORWARD" }
            val rName = remoteDriver.activeRouteName.ifEmpty { remoteDriver.route.orEmpty() }
            val tripId = remoteDriver.activeTripId.ifEmpty {
                generateTripId(remoteDriver.driverId, remoteDriver.assignedBus.orEmpty(), rName, direction)
            }

            if (isTripCompleted(context, tripId)) {
                Log.d(TAG, "TRIP_INVALIDATED: Remote trip $tripId was completed. Skipping.")
                return null
            }

            val remoteState = ActiveTripState(
                tripId = tripId,
                driverId = remoteDriver.driverId,
                routeId = remoteDriver.activeRouteId,
                routeName = rName,
                busNumber = remoteDriver.assignedBus.orEmpty(),
                tripDirection = direction,
                isMorning = direction.equals("FORWARD", true),
                isDutyEnabled = remoteDriver.status.equals("ON DUTY", true) || remoteDriver.status.equals("Active", true),
                isNavigating = true,
                tripStatus = "ACTIVE",
                nextStopIndex = remoteDriver.nextStopIndex,
                stopStates = remoteDriver.stopArrivalTimes.mapNotNull { (k, v) ->
                    val idx = k.toIntOrNull() ?: return@mapNotNull null
                    idx to (if (v == "Skipped") "SKIPPED" else if (idx < remoteDriver.nextStopIndex) "COMPLETED" else "ARRIVED")
                }.toMap(),
                stopArrivalTimes = remoteDriver.stopArrivalTimes.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap(),
                stopEtaTexts = remoteDriver.stopEtaTimes.mapNotNull { (k, v) -> k.toIntOrNull()?.let { it to v } }.toMap(),
                isReverseTripActive = direction.equals("RETURN", true),
                lastKnownLat = remoteDriver.latitude,
                lastKnownLng = remoteDriver.longitude,
                tripStartTime = if (remoteDriver.locationTimestamp > 0L) remoteDriver.locationTimestamp else System.currentTimeMillis(),
                lastUpdated = if (remoteDriver.lastUpdated > 0L) remoteDriver.lastUpdated else System.currentTimeMillis()
            )

            if (isTripValid(remoteState, context)) {
                Log.d(TAG, "TRIP_RESTORE: Restoring active trip from remote Firestore state: $tripId")
                return remoteState
            }
        }

        return null
    }
}

