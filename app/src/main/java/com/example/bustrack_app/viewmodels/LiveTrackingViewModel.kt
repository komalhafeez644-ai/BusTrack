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

    private fun startTracking() {
        FirebaseRepository.fetchDrivers { allDrivers ->
            _allDriversForSearch.value = allDrivers

            val active = allDrivers.filter { 
                (it.status.equals("Active", true) || it.status.equals("ACTIVE", true) || it.status.equals("On Duty", true)) 
                && it.latitude != 0.0 && it.longitude != 0.0 
            }
            
            // Process dynamic values for each active driver
            active.forEach { driver ->
                calculateSpeed(driver)
                calculateETA(driver)
                calculateLoad(driver)
            }
            
            _activeDrivers.value = active
            
            _selectedDriver.value?.let { selected ->
                active.find { it.id == selected.id }?.let { updated ->
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
        
        val last = lastLocations[driver.id]
        if (last != null) {
            val distance = last.first.distanceTo(currentLoc) // meters
            val timeDiff = (currentTime - last.second) / 1000.0 // seconds
            
            if (timeDiff > 0) {
                val speedMps = distance / timeDiff
                val speedKph = speedMps * 3.6
                driver.speed = if (speedKph < 2.0) 0.0 else speedKph // Noise filter
            }
        }
        
        lastLocations[driver.id] = Pair(currentLoc, currentTime)
    }

    private fun calculateETA(driver: DriverModel) {
        val route = allRoutes.find { it.routeName == driver.route || it.routeCode == driver.route }
        if (route == null || route.stopsList.isEmpty()) {
            driver.eta = "No Route"
            return
        }

        // Find next stop (first stop ahead of current position)
        // Simplification: Find the closest stop in the sequence that hasn't been reached
        val busLoc = Location("bus").apply {
            latitude = driver.latitude
            longitude = driver.longitude
        }

        var nextStop: StopItem? = null
        var minDistance = Float.MAX_VALUE

        // In a real app, we'd track which stops were already visited. 
        // Here we just find the closest one that is reasonably "ahead".
        for (stop in route.stopsList) {
            val stopLoc = Location("stop").apply {
                latitude = stop.latitude
                longitude = stop.longitude
            }
            val dist = busLoc.distanceTo(stopLoc)
            if (dist < minDistance) {
                minDistance = dist
                nextStop = stop
            }
        }

        if (nextStop != null) {
            if (minDistance < 100) { // Within 100 meters
                driver.eta = "Arriving"
            } else {
                val speedKph = if (driver.speed > 5) driver.speed else 30.0 // Default speed if slow
                val timeHours = (minDistance / 1000.0) / speedKph
                val timeMinutes = (timeHours * 60).toInt()
                driver.eta = if (timeMinutes <= 0) "1 min" else "$timeMinutes min"
            }
        } else {
            driver.eta = "On Way"
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