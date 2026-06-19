package com.example.bustrack.ViewModels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack.Models.DriverDashboardModel
import com.example.bustrack.Models.QuickAction
import com.example.bustrack.R

class DriverDashboardViewModel : ViewModel() {

    private val _dashboardData = MutableLiveData<DriverDashboardModel>()
    val dashboardData: LiveData<DriverDashboardModel> = _dashboardData

    private val _quickActions = MutableLiveData<List<QuickAction>>()
    val quickActions: LiveData<List<QuickAction>> = _quickActions

    private val _isOnDuty = MutableLiveData<Boolean>(true)
    val isOnDuty: LiveData<Boolean> = _isOnDuty

    init {
        loadDashboardData()
        showInitialActions()
    }

    private fun showInitialActions() {
        _quickActions.value = listOf(
            QuickAction(1, "Live Tracking", android.R.drawable.ic_menu_directions),
            QuickAction(2, "Attendance", R.drawable.ic_passengers),
            QuickAction(3, "Alerts", R.drawable.notification),
            QuickAction(4, "Routes", R.drawable.ic_stops)
        )
    }

    fun showAllActions() {
        _quickActions.value = listOf(
            QuickAction(1, "Live Tracking", android.R.drawable.ic_menu_directions),
            QuickAction(2, "Attendance", R.drawable.ic_passengers),
            QuickAction(3, "Alerts", R.drawable.notification),
            QuickAction(4, "Routes", R.drawable.ic_stops),
            QuickAction(5, "Messages", R.drawable.ic_messages),
            QuickAction(6, "Profile", R.drawable.ic_driver),
            QuickAction(7, "Schedule", android.R.drawable.ic_menu_agenda),
            QuickAction(8, "Support", android.R.drawable.ic_menu_help)
        )
    }

    private fun loadDashboardData() {
        _dashboardData.value = DriverDashboardModel(
            driverName = "John Driver",
            busNumber = "BUS-101",
            currentRoute = "Route 1 (North)",
            plateNumber = "KL 01 AB 1234",
            stopsAssigned = 12,
            stopsCount = "0/12",
            studentsCount = "0/48",
            tripTime = "00:00",
            tripDistance = "0 km",
            isOnDuty = true
        )
    }

    fun toggleDutyStatus(isOnDuty: Boolean) {
        _isOnDuty.value = isOnDuty
        _dashboardData.value = _dashboardData.value?.copy(isOnDuty = isOnDuty)
    }
}
