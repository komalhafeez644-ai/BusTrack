package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem

class RouteViewModel : ViewModel() {

    private val _routeList = MutableLiveData<List<RouteModel>>()
    val routeList: LiveData<List<RouteModel>> get() = _routeList

    private var allRoutes = listOf<RouteModel>()

    init {
        loadMockRoutes()
    }

    private fun loadMockRoutes() {
        // Har route ke liye dummy stops data ready karna
        val stopsForRoute5 = mutableListOf(
            StopItem("01", "Main Terminal", "07:00 AM", 33.6844, 73.0479),
            StopItem("02", "Library West Gate", "07:12 AM", 33.6900, 73.0500),
            StopItem("03", "Science Quad", "07:25 AM", 33.6950, 73.0550)
        )

        allRoutes = listOf(
            RouteModel("1", "ROUTE 05", "Express North", "ACTIVE", "Bus 42", "Ahmed Ali", 12, 45, stopsForRoute5),
            RouteModel("2", "ROUTE 12", "Downtown Link", "PARTIAL", "Bus 18", "Sarah Chen", 24, 112, mutableListOf()),
            RouteModel("3", "ROUTE 08", "South Perimeter", "INACTIVE", "TBD", "Unassigned", 18, 32, mutableListOf())
        )
        _routeList.value = allRoutes
    }

    // ➕ Map par click ya button se stop add karne ki logic
    fun addNewStopToRoute(routeId: String, name: String, time: String, lat: Double, lng: Double) {
        val currentList = _routeList.value ?: return
        val targetRoute = currentList.find { it.id == routeId }

        targetRoute?.let { route ->
            val nextIndex = String.format("%02d", route.stopsList.size + 1)
            route.stopsList.add(StopItem(nextIndex, name, time, lat, lng))
            _routeList.value = currentList // UI ko refresh karne ke liye list re-assign
        }
    }

    // ❌ Stop delete karne ki logic
    fun removeStopFromRoute(routeId: String, stopId: String) {
        val currentList = _routeList.value ?: return
        val targetRoute = currentList.find { it.id == routeId }

        targetRoute?.let { route ->
            route.stopsList.removeAll { it.id == stopId }
            _routeList.value = currentList // UI refresh trigger
        }
    }

    fun filterRoutes(query: String) {
        if (query.isEmpty()) {
            _routeList.value = allRoutes
        } else {
            _routeList.value = allRoutes.filter {
                it.routeName.lowercase().contains(query.lowercase()) ||
                        it.routeCode.lowercase().contains(query.lowercase()) ||
                        it.driverName.lowercase().contains(query.lowercase())
            }
        }
    }
}