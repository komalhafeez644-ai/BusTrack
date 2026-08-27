package com.example.bustrack_app.viewmodels

import android.location.Location
import androidx.lifecycle.*
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.models.AttendanceRecordModel
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StudentModel
import java.text.SimpleDateFormat
import java.util.*

class TrackDriverViewModel : ViewModel() {

    private val _driverId = MutableLiveData<String>()
    private val lastLocation = MutableLiveData<Pair<Location, Long>>()
    
    private val allStudents = MutableLiveData<List<StudentModel>>()
    private val todayAttendance = MutableLiveData<List<AttendanceRecordModel>>()

    init {
        FirebaseRepository.fetchStudents { allStudents.value = it }
        FirebaseRepository.fetchAttendance { todayAttendance.value = it }
    }

    val targetDriver: LiveData<DriverModel?> = MediatorLiveData<DriverModel?>().apply {
        addSource(_driverId) { id ->
            val driver = DriverRepository.driverList.value?.find { it.driverId == id }
            driver?.let { calculateLiveStats(it) }
            value = driver
        }
        addSource(DriverRepository.driverList) { list ->
            val driver = list.find { it.driverId == _driverId.value }
            driver?.let { calculateLiveStats(it) }
            value = driver
        }
        addSource(RouteRepository.routeList) { _ ->
            val driver = value ?: DriverRepository.driverList.value?.find { it.driverId == _driverId.value }
            driver?.let { 
                calculateLiveStats(it)
                value = it
            }
        }
    }

    val assignedRoute: LiveData<RouteModel?> = targetDriver.switchMap { driver ->
        RouteRepository.routeList.map { routes ->
            if (driver != null) {
                routes.find { it.routeName == driver.route || it.busNo == driver.assignedBus || it.id == driver.route }
            } else null
        }
    }

    private fun calculateLiveStats(driver: DriverModel) {
        // 1. Calculate Speed
        val currentLoc = Location("service").apply {
            latitude = driver.latitude
            longitude = driver.longitude
        }
        val currentTime = System.currentTimeMillis()
        
        lastLocation.value?.let { last ->
            val distance = last.first.distanceTo(currentLoc)
            val timeDiff = (currentTime - last.second) / 1000.0
            if (timeDiff > 0) {
                val speedKph = (distance / timeDiff) * 3.6
                driver.speed = if (speedKph < 2.0) 0.0 else speedKph
            }
        }
        lastLocation.value = Pair(currentLoc, currentTime)

        // 2. Calculate Load (Live from Attendance)
        val routeName = driver.route ?: ""
        val students = allStudents.value?.filter { it.route == routeName } ?: emptyList()
        val records = todayAttendance.value?.filter { it.route == routeName && it.date == SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date()) } ?: emptyList()
        
        val isMorning = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) < 14
        if (isMorning) {
            val present = records.count { it.morningPickup.equals("Present", true) }
            driver.load = "$present/${students.size}"
        } else {
            val eveningPresent = records.count { it.eveningPickup.equals("Present", true) }
            val dropped = records.count { it.eveningDrop.equals("Dropped", true) || it.eveningDrop.equals("Present", true) }
            val currentLoad = eveningPresent - dropped
            driver.load = "${if (currentLoad < 0) 0 else currentLoad}/$eveningPresent"
        }

        // 3. Calculate ETA (Current Location to each Stop)
        val route = RouteRepository.routeList.value?.find { it.routeName == driver.route || it.busNo == driver.assignedBus || it.id == driver.route }
        if (route != null && route.stopsList.isNotEmpty()) {
            val avgSpeedMs = if (driver.speed > 5) (driver.speed / 3.6) else (30.0 / 3.6)
            
            route.stopsList.forEachIndexed { index, stop ->
                // Use shared arrival time if available
                val arrival = driver.stopArrivalTimes[index.toString()]
                if (arrival != null || index < driver.nextStopIndex) {
                    // Priority: If we have a timestamp, show it. Otherwise, if the index is passed, show "Arrived".
                    stop.time = if (arrival != null) "Arrived: $arrival" else "Arrived"
                } else {
                    val stopLoc = Location("stop").apply {
                        latitude = stop.latitude
                        longitude = stop.longitude
                    }
                    val distance = currentLoc.distanceTo(stopLoc)
                    
                    if (distance < 150) {
                        stop.time = "Arrived"
                    } else {
                        val seconds = (distance / avgSpeedMs).toInt()
                        val minutes = seconds / 60
                        stop.time = "ETA: ${if (minutes <= 1) "1" else "$minutes"} min"
                    }
                }
            }

            val lastStop = route.stopsList.last()
            val destLoc = Location("dest").apply {
                latitude = lastStop.latitude
                longitude = lastStop.longitude
            }
            val distanceToDest = currentLoc.distanceTo(destLoc)
            
            if (distanceToDest < 200) {
                driver.eta = "Arrived"
            } else {
                val seconds = (distanceToDest / avgSpeedMs).toInt()
                val minutes = seconds / 60
                driver.eta = if (minutes <= 1) "Arriving" else "$minutes min"
            }
        } else {
            driver.eta = "On Way"
        }
    }

    fun setDriverId(id: String) {
        _driverId.value = id
    }
}