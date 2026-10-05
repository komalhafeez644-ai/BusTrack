package com.example.bustrack_app.data

import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.models.RouteModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BusTrackAssistantDutyResolutionTest {

    @Test
    fun normalizeBus_handlesAllVariations() {
        assertEquals("ict2345", BusTrackAssistantDataManager.normalizeBus("ICT-2345"))
        assertEquals("ict2345", BusTrackAssistantDataManager.normalizeBus("ICT 2345"))
        assertEquals("ict2345", BusTrackAssistantDataManager.normalizeBus("Bus ICT-2345"))
        assertEquals("ict2345", BusTrackAssistantDataManager.normalizeBus("bus ict 2345"))
        assertEquals("ict2345", BusTrackAssistantDataManager.normalizeBus("#ICT-2345"))
        assertEquals("ict2345", BusTrackAssistantDataManager.normalizeBus("ICT_2345"))
        assertEquals("", BusTrackAssistantDataManager.normalizeBus(null))
        assertEquals("", BusTrackAssistantDataManager.normalizeBus("   "))
    }

    @Test
    fun normalizeRoute_handlesAllVariations() {
        assertEquals("gulistancolony", BusTrackAssistantDataManager.normalizeRoute("Gulistan Colony"))
        assertEquals("gulistancolony", BusTrackAssistantDataManager.normalizeRoute("Gulistan Colony Road"))
        assertEquals("gulistancolony", BusTrackAssistantDataManager.normalizeRoute("Route Gulistan Colony"))
        assertEquals("gulistancolony", BusTrackAssistantDataManager.normalizeRoute("route gulistan colony road"))
        assertEquals("", BusTrackAssistantDataManager.normalizeRoute(null))
        assertEquals("", BusTrackAssistantDataManager.normalizeRoute(""))
    }

    @Test
    fun isDriverOnDuty_evaluatesCorrectlyForActiveStates() {
        val activeDriver = DriverModel(
            id = "EMP-123",
            driverId = "EMP-123",
            uid = "1KHck7zo7sbErNDQolb7a6Vr2Nh1",
            assignedBus = "ICT-2345",
            route = "Gulistan Colony",
            status = "Active",
            isNavigating = true,
            locationStatus = "LIVE"
        )
        assertTrue(BusTrackAssistantDataManager.isDriverOnDuty(activeDriver))

        val returnTripDriver = activeDriver.copy(status = "Active (Return Trip)", isNavigating = true)
        assertTrue(BusTrackAssistantDataManager.isDriverOnDuty(returnTripDriver))

        val onDutyDriver = activeDriver.copy(status = "On Duty", isNavigating = false, locationStatus = "OFFLINE")
        assertTrue(BusTrackAssistantDataManager.isDriverOnDuty(onDutyDriver))

        val inactiveDriver = activeDriver.copy(status = "Inactive", isNavigating = false, locationStatus = "OFFLINE")
        assertFalse(BusTrackAssistantDataManager.isDriverOnDuty(inactiveDriver))

        assertFalse(BusTrackAssistantDataManager.isDriverOnDuty(null))
    }

    @Test
    fun matchDriverAuth_matchesAllIdentifierTypes() {
        val driver = DriverModel(
            id = "EMP-123",
            driverId = "EMP-123",
            uid = "1KHck7zo7sbErNDQolb7a6Vr2Nh1",
            email = "driver@bustrack.com",
            name = "Komal Hafeez",
            assignedBus = "ICT-2345",
            route = "Gulistan Colony",
            status = "Active",
            isNavigating = true
        )
        val driversList = listOf(Pair("EMP-123", driver))

        val byDocId = BusTrackAssistantDataManager.matchDriverAuth(driversList, "EMP-123", "")
        assertNotNull(byDocId)
        assertEquals("EMP-123", byDocId?.first)

        val byUid = BusTrackAssistantDataManager.matchDriverAuth(driversList, "1KHck7zo7sbErNDQolb7a6Vr2Nh1", "")
        assertNotNull(byUid)
        assertEquals("1KHck7zo7sbErNDQolb7a6Vr2Nh1", byUid?.second?.uid)

        val byEmail = BusTrackAssistantDataManager.matchDriverAuth(driversList, "", "driver@bustrack.com")
        assertNotNull(byEmail)
        assertEquals("driver@bustrack.com", byEmail?.second?.email)

        val notFound = BusTrackAssistantDataManager.matchDriverAuth(driversList, "UNKNOWN_UID", "")
        assertNull(notFound)
    }

    @Test
    fun matchDriverForBusOrRoute_matchesNormalizedValues() {
        val driver = DriverModel(
            id = "EMP-123",
            driverId = "EMP-123",
            uid = "1KHck7zo7sbErNDQolb7a6Vr2Nh1",
            assignedBus = "ICT 2345",
            route = "Gulistan Colony",
            activeRouteName = "Gulistan Colony",
            status = "Active",
            isNavigating = true
        )
        val driversList = listOf(Pair("EMP-123", driver))

        val byBus = BusTrackAssistantDataManager.matchDriverForBusOrRoute(
            drivers = driversList,
            requestedBus = "ICT-2345",
            requestedRoute = ""
        )
        assertNotNull(byBus)
        assertEquals("EMP-123", byBus?.first)

        val byRoute = BusTrackAssistantDataManager.matchDriverForBusOrRoute(
            drivers = driversList,
            requestedBus = "",
            requestedRoute = "Gulistan Colony Road"
        )
        assertNotNull(byRoute)
        assertEquals("EMP-123", byRoute?.first)
    }
}
