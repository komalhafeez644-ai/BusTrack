package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MediatorLiveData
import com.google.firebase.auth.FirebaseAuth
import com.example.bustrack_app.models.DriverDashboardModel
import com.example.bustrack_app.models.QuickAction
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.BusRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.data.StudentRepository
import com.example.bustrack_app.R

class DriverDashboardViewModel : ViewModel() {

    private val _dashboardData = MediatorLiveData<DriverDashboardModel>()
    val dashboardData: LiveData<DriverDashboardModel> = _dashboardData

    private val _quickActions = MutableLiveData<List<QuickAction>>()
    val quickActions: LiveData<List<QuickAction>> = _quickActions

    private val _isOnDuty = MutableLiveData<Boolean>(true)
    val isOnDuty: LiveData<Boolean> = _isOnDuty

    init {
        setupMediator()
        showInitialActions()
    }

    private fun setupMediator() {
        _dashboardData.addSource(DriverRepository.driverList) { combineData() }
        _dashboardData.addSource(BusRepository.busList) { combineData() }
        _dashboardData.addSource(RouteRepository.routeList) { combineData() }
        _dashboardData.addSource(StudentRepository.studentList) { combineData() }
    }

    private fun showInitialActions() {
        _quickActions.value = listOf(
            QuickAction(1, "Live Tracking", R.drawable.tracking),
            QuickAction(2, "Attendance", R.drawable.groupattendance),
            QuickAction(3, "Alerts", R.drawable.notification_active),
            QuickAction(4, "Routes", R.drawable.map)
        )
    }

    private fun combineData() {
        val email = FirebaseAuth.getInstance().currentUser?.email?.trim()?.lowercase() ?: return
        val drivers = DriverRepository.driverList.value ?: emptyList()
        val buses = BusRepository.busList.value ?: emptyList()
        val routes = RouteRepository.routeList.value ?: emptyList()
        val students = StudentRepository.studentList.value ?: emptyList()

        val driver = drivers.find { it.email.trim().lowercase() == email } ?: return
        val bus = buses.find { it.busNumber == driver.assignedBus }
        val route = routes.find { it.routeName == driver.route || it.busNo == driver.assignedBus }

        // Filter students by route name or bus number
        val studentsInRoute = students.filter { 
            (it.route != null && it.route == route?.routeName) || 
            (it.busNo != null && it.busNo == driver.assignedBus) 
        }

        _dashboardData.value = DriverDashboardModel(
            driverName = driver.name,
            busNumber = driver.assignedBus ?: "Not Assigned",
            currentRoute = route?.routeName ?: driver.route ?: "No Route",
            capacity = if (bus != null) "${bus.totalSeats} Seats" else "N/A",
            stopsAssigned = route?.stopsList?.size ?: 0,
            stopsCount = "${route?.stopsList?.size ?: 0}",
            studentsCount = "${studentsInRoute.size}",
            tripTime = "Calculating...", 
            tripDistance = "Calculating...",
            isOnDuty = driver.status == "Active"
        )
    }

    fun toggleDutyStatus(isOnDuty: Boolean) {
        _isOnDuty.value = isOnDuty
        _dashboardData.value = _dashboardData.value?.copy(isOnDuty = isOnDuty)
    }
}