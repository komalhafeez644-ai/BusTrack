package com.example.bustrack_app.data

import com.example.bustrack_app.models.TrackingRequestModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TrackingApprovalPolicyTest {
    @Test
    fun enabledRequestForAnotherParentIsFoundByRollNumber() {
        val request = enabledRequest(parentId = "parent-a", rollNumber = "ABC-123")

        val result = TrackingApprovalPolicy.findEnabledRequestForRollNumber(listOf(request), "ABC-123")

        assertEquals("parent-a", result?.parentId)
    }

    @Test
    fun enabledRequestForSameParentIsAlsoFoundByRollNumber() {
        val request = enabledRequest(parentId = "parent-a", rollNumber = "ABC-123")

        val result = TrackingApprovalPolicy.findEnabledRequestForRollNumber(listOf(request), "abc-123")

        assertEquals("parent-a", result?.parentId)
    }

    @Test
    fun disabledTrackingIsNotReportedAsActive() {
        val request = enabledRequest(parentId = "parent-a", rollNumber = "ABC-123")
            .copy(trackingEnabled = false, trackingState = "DISABLED")

        assertNull(TrackingApprovalPolicy.findEnabledRequestForRollNumber(listOf(request), "ABC-123"))
    }

    @Test
    fun enabledStateIsRecognizedEvenWhenLegacyBooleanIsMissingOrFalse() {
        val request = enabledRequest(parentId = "parent-a", rollNumber = "ABC-123")
            .copy(trackingEnabled = false, trackingState = "ENABLED")

        assertEquals(
            "parent-a",
            TrackingApprovalPolicy.findEnabledRequestForRollNumber(listOf(request), "ABC-123")?.parentId
        )
    }

    private fun enabledRequest(parentId: String, rollNumber: String) = TrackingRequestModel(
        requestId = "request-$parentId",
        parentId = parentId,
        rollNumber = rollNumber,
        status = "APPROVED",
        trackingEnabled = true,
        trackingState = "ENABLED",
        parentName = parentId
    )
}
