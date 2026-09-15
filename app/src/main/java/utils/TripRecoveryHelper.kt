package utils

import android.content.Context
import android.util.Log
import com.example.bustrack_app.models.ActiveTripState
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object TripRecoveryHelper {

    private const val TAG = "TripRecoveryHelper"
    private const val PREFS_NAME = "trip_recovery_prefs"
    private const val KEY_ACTIVE_TRIP = "key_active_trip_state"
    private const val TRIP_TIMEOUT_MS = 12 * 60 * 60 * 1000L // 12 hours

    private val gson = Gson()

    /**
     * Generates a deterministic stable trip ID for a driver's trip session.
     */
    fun generateTripId(driverId: String, routeName: String, direction: String, timestamp: Long = System.currentTimeMillis()): String {
        val date = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(timestamp))
        val cleanRoute = routeName.trim().replace(" ", "_").replace("/", "_").replace("-", "_")
        val cleanDriver = driverId.trim().replace(" ", "_")
        return "TRIP_${cleanDriver}_${cleanRoute}_${date}_${direction.uppercase()}"
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
            Log.d(TAG, "Saved active trip state: ${state.tripId}, stopIndex: ${state.nextStopIndex}, nav: ${state.isNavigating}")
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
            if (isTripValid(state)) {
                state
            } else {
                Log.d(TAG, "Persisted trip state is stale/invalid. Clearing.")
                clearActiveTrip(context)
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading active trip state: ${e.message}", e)
            null
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
     * Validates whether a trip state is fresh, unfinished, and belongs to today.
     */
    fun isTripValid(state: ActiveTripState?): Boolean {
        if (state == null) return false
        if (!state.isNavigating) return false
        if (state.driverId.isBlank() || (state.routeId.isBlank() && state.routeName.isBlank())) return false

        val now = System.currentTimeMillis()
        val elapsed = now - state.lastUpdated
        if (elapsed > TRIP_TIMEOUT_MS) {
            Log.d(TAG, "Trip expired: elapsed ${elapsed / 1000 / 60} minutes > 12h")
            return false
        }

        // Check if calendar date matches today
        val todayStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(now))
        val tripDateStr = SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date(state.tripStartTime))
        if (todayStr != tripDateStr) {
            Log.d(TAG, "Trip belongs to a previous date ($tripDateStr != $todayStr)")
            return false
        }

        return true
    }
}
