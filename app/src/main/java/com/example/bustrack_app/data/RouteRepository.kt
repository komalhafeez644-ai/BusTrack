package com.example.bustrack_app.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem

object RouteRepository {
    private val _routeList = MutableLiveData<List<RouteModel>>()
    val routeList: LiveData<List<RouteModel>> get() = _routeList

    init {
        // Initial Dummy Data
        val stopsForRoute1 = mutableListOf(
            StopItem("01", "Sunrise Apartments", "07:00 AM", 33.7000, 73.0600),
            StopItem("02", "Green Park", "07:15 AM", 33.7100, 73.0700)
        )
        
        val stopsForRoute2 = mutableListOf(
            StopItem("01", "Oakwood Entry", "07:30 AM", 33.6844, 73.0479),
            StopItem("02", "Blue Tower", "07:45 AM", 33.6900, 73.0500)
        )

        _routeList.value = mutableListOf(
            RouteModel("1", "ROUTE 01", "Route 1", "ACTIVE", "BUS-102", "John Doe", 2, 45, stopsForRoute1),
            RouteModel("2", "ROUTE 02", "Route 2", "PARTIAL", "BUS-088", "Sarah Smith", 2, 112, stopsForRoute2),
            RouteModel("3", "ROUTE 03", "Route 3", "ACTIVE", "BUS-105", "Mike Ross", 5, 32, mutableListOf())
        )
    }

    fun getBusForRoute(routeName: String): String {
        return _routeList.value?.find { it.routeName.equals(routeName, true) || it.routeCode.equals(routeName, true) }?.busNo ?: ""
    }

    fun updateRoute(updatedRoute: RouteModel) {
        val current = _routeList.value?.toMutableList() ?: mutableListOf()
        val index = current.indexOfFirst { it.id == updatedRoute.id }
        if (index != -1) {
            current[index] = updatedRoute
            _routeList.value = current
            // Also notify Bus Repository to refresh its mapping
            BusRepository.refreshBusList()
        }
    }
}
