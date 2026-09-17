package com.example.bustrack_app.models

import java.io.Serializable

/**
 * Data model representing the active in-progress trip state of a driver.
 * Persisted locally in SharedPreferences via TripRecoveryHelper and synchronized
 * with Firestore for cross-session and crash recovery.
 */
data class ActiveTripState(
    val tripId: String = "",
    val driverId: String = "",
    val routeId: String = "",
    val routeName: String = "",
    val busNumber: String = "",
    val tripDirection: String = "FORWARD", // "FORWARD" or "RETURN"
    val isMorning: Boolean = true,
    val isDutyEnabled: Boolean = false,
    val isNavigating: Boolean = false,
    val tripStatus: String = "ACTIVE", // "ACTIVE", "COMPLETED", "CANCELLED"
    val currentStopIndex: Int = 0,
    val nextStopIndex: Int = 0,
    val stopStates: Map<Int, String> = emptyMap(),
    val stopArrivalTimes: Map<Int, String> = emptyMap(),
    val stopEtaTexts: Map<Int, String> = emptyMap(),
    val isReverseTripActive: Boolean = false,
    val reverseStopStates: Map<Int, String> = emptyMap(),
    val reverseStopArrivalTimes: Map<Int, String> = emptyMap(),
    val reverseStopEtaTexts: Map<Int, String> = emptyMap(),
    val lastKnownLat: Double = 0.0,
    val lastKnownLng: Double = 0.0,
    val tripStartTime: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val stateVersion: Long = 1L
) : Serializable
