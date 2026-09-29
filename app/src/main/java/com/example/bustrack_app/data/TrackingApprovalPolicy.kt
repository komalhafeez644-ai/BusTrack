package com.example.bustrack_app.data

import com.example.bustrack_app.models.TrackingRequestModel

internal object TrackingApprovalPolicy {
    fun findEnabledRequestForRollNumber(
        requests: List<TrackingRequestModel>,
        rollNumber: String
    ): TrackingRequestModel? {
        val requestedRollNumber = rollNumber.trim()
        if (requestedRollNumber.isBlank()) return null

        return requests.firstOrNull { request ->
                request.rollNumber.trim().equals(requestedRollNumber, ignoreCase = true) &&
                    request.status.equals("APPROVED", ignoreCase = true) &&
                    (request.trackingEnabled || request.trackingState.equals("ENABLED", ignoreCase = true)) &&
                !request.trackingState.equals("DISABLED", ignoreCase = true) &&
                !request.trackingState.equals("REVOKED", ignoreCase = true)
        }
    }
}
