package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.*
import android.location.Location
import java.text.SimpleDateFormat
import java.util.*

class LiveTrackingViewModel : ViewModel() {

    private val _activeDrivers = MutableLiveData<List<DriverModel>>()
    val activeDrivers: LiveData<List<DriverModel>> = _activeDrivers

    private val _selectedDriver = MutableLiveData<DriverModel?>()
    val selectedDriver: LiveData<DriverModel?> = _selectedDriver

    private val _allDriversForSearch = MutableLiveData<List<DriverModel>>()
    val allDriversForSearch: LiveData<List<DriverModel>> = _allDriversForSearch

    private val _trackingStatus = MutableLiveData<String?>()
    val trackingStatus: LiveData<String?> = _trackingStatus

    private var allowedRoutes = mutableSetOf<String>()
    private var isParentMode = false
    private val lastLocations = mutableMapOf<String, Pair<Location, Long>>()

    private var allRoutes = listOf<RouteModel>()
    private var allStudents = listOf<StudentModel>()
    private var todayAttendance = listOf<AttendanceRecordModel>()

    init {
        loadStaticData()
        startTracking()
    }

    private fun loadStaticData() {
        FirebaseRepository.fetchRoutes { allRoutes = it }
        FirebaseRepository.fetchStudents { allStudents = it }
        FirebaseRepository.fetchAttendance { todayAttendance = it }
    }

    fun setAllowedRoute(route: String?) {
        isParentMode = true
        if (route != null) {
            allowedRoutes.add(route)
        }
        // Re-trigger driver processing if route filter changes
        startTracking()
    }

    fun setAllowedRoutes(routes: Set<String>) {
        isParentMode = true
        allowedRoutes = routes.toMutableSet()
        startTracking()
    }

    private fun startTracking() {
        FirebaseRepository.fetchDrivers { allDrivers ->
            // If in parent mode but no route is assigned
            if (isParentMode && allowedRoutes.isEmpty()) {
                _trackingStatus.value = "NO_ROUTE"
                _activeDrivers.value = emptyList()
                return@fetchDrivers
            }

            // Filter by allowed route if in Parent Mode
            val filteredDrivers = if (isParentMode && allowedRoutes.isNotEmpty()) {
                allDrivers.filter { allowedRoutes.contains(it.route) }
            } else {
                allDrivers
            }

            _allDriversForSearch.value = filteredDrivers

            val currentTime = System.currentTimeMillis()
            val active = filteredDrivers.filter {
                val isOnDuty = it.status.equals("Active", true) || it.status.equals("ACTIVE", true) || it.status.equals("On Duty", true)

                // Bus track-able hone ke liye sirf On Duty hona chahiye + valid, recent
                // location - navigation start hona zaroori nahi hai. On Duty aur
                // Navigation do independent states hain: driver route par khada bhi ho
                // (bina navigation ke), tab bhi uski live location Parent/Principal/Admin
                // ko dikhni chahiye.
                isOnDuty && it.latitude != 0.0 && it.longitude != 0.0
                        && (currentTime - it.lastUpdated) < 1800000 // 30 mins window for emulator/testing
            }

            // Check availability if in Parent Mode
            if (isParentMode) {
                if (active.isEmpty()) {
                    _trackingStatus.value = "OFF_DUTY"
                } else {
                    _trackingStatus.value = "AVAILABLE"
                }
            }

            // Process dynamic values for each active driver
            active.forEach { driver ->
                calculateSpeed(driver)
                calculateETA(driver)
                calculateLoad(driver)
            }

            _activeDrivers.value = active

            _selectedDriver.value?.let { selected ->
                active.find { it.driverId == selected.driverId }?.let { updated ->
                    _selectedDriver.value = updated
                }
            }
        }
    }

    private fun calculateSpeed(driver: DriverModel) {
        val currentLoc = Location("service").apply {
            latitude = driver.latitude
            longitude = driver.longitude
        }
        val currentTime = System.currentTimeMillis()

        val last = lastLocations[driver.driverId]
        if (last != null) {
            val distance = last.first.distanceTo(currentLoc) // meters
            val timeDiff = (currentTime - last.second) / 1000.0 // seconds

            if (timeDiff > 0) {
                val speedMps = distance / timeDiff
                val speedKph = speedMps * 3.6
                driver.speed = if (speedKph < 2.0) 0.0 else speedKph // Noise filter
            }
        }

        lastLocations[driver.driverId] = Pair(currentLoc, currentTime)
    }

    private fun calculateETA(driver: DriverModel) {
        val route = allRoutes.find { it.routeName == driver.route || it.routeCode == driver.route }
        if (route == null || route.stopsList.isEmpty()) {
            driver.eta = "No Route"
            return
        }

        val busLoc = Location("bus").apply {
            latitude = driver.latitude
            longitude = driver.longitude
        }

        // Use the driver's current target stop index for precise tracking
        val stopIdx = driver.nextStopIndex.coerceIn(0, route.stopsList.size - 1)
        val targetStop = route.stopsList[stopIdx]

        val stopLoc = Location("stop").apply {
            latitude = targetStop.latitude
            longitude = targetStop.longitude
        }
        val dist = busLoc.distanceTo(stopLoc)

        // Check if currently arrived at this stop
        val arrivalTime = driver.stopArrivalTimes[stopIdx.toString()]
        if (arrivalTime != null || dist < 100) {
            driver.eta = if (arrivalTime != null) "ARRIVED $arrivalTime" else "Arriving"
        } else {
            val speedKph = if (driver.speed > 5) driver.speed else 30.0
            val timeHours = (dist / 1000.0) / speedKph
            val timeMinutes = (timeHours * 60).toInt()
            driver.eta = if (timeMinutes <= 0) "1 min" else "$timeMinutes min"
        }
    }

    private fun calculateLoad(driver: DriverModel) {
        val routeName = driver.route ?: ""
        val studentsOnRoute = allStudents.filter { it.route == routeName }
        val totalOnRoute = studentsOnRoute.size

        if (totalOnRoute == 0) {
            driver.load = "0/0"
            return
        }

        val today = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
        val records = todayAttendance.filter { it.route == routeName && it.date == today }

        // Logic for Morning/Evening
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val isMorning = hour < 14 // Before 2 PM is morning

        if (isMorning) {
            // Morning: Count those marked Present (picked up)
            val presentCount = records.count { it.morningPickup.equals("Present", true) }
            driver.load = "$presentCount/$totalOnRoute"
        } else {
            // Evening: Start with total attendance, subtract dropped
            val eveningAttendanceCount = records.count { it.eveningPickup.equals("Present", true) }
            val droppedCount = records.count { it.eveningDrop.equals("Dropped", true) || it.eveningDrop.equals("Present", true) } // Assuming "Dropped" means off the bus

            // User requirement: "Evening attendance = 40 ... Stop A -> 5 dropped ... 40 - 5 = 35"
            // So load is (Evening Attendance) - (Dropped)
            val currentLoad = eveningAttendanceCount - droppedCount
            driver.load = "${if (currentLoad < 0) 0 else currentLoad}/$eveningAttendanceCount"
        }
    }

    fun selectDriver(driver: DriverModel) {
        _selectedDriver.value = driver
    }
}