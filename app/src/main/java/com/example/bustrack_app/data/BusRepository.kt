package com.example.bustrack_app.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.BusModel

object BusRepository {
    private val _busList = MutableLiveData<List<BusModel>>()
    val busList: LiveData<List<BusModel>> get() = _busList

    init {
        refreshBusList()
    }

    fun refreshBusList() {
        val routes = RouteRepository.routeList.value ?: listOf()
        val initialBuses = mutableListOf(
            BusModel("BUS-102", 40, "John Doe", findRouteForBus("BUS-102", routes), "ACTIVE"),
            BusModel("BUS-088", 32, "Sarah Smith", findRouteForBus("BUS-088", routes), "INACTIVE"),
            BusModel("BUS-215", 55, null, findRouteForBus("BUS-215", routes), "INACTIVE"),
            BusModel("BUS-105", 40, "Mike Ross", findRouteForBus("BUS-105", routes), "ACTIVE")
        )
        _busList.value = initialBuses
    }

    private fun findRouteForBus(busNo: String, routes: List<com.example.bustrack_app.models.RouteModel>): String? {
        return routes.find { it.busNo == busNo }?.routeName
    }

    fun updateBusDetails(originalNumber: String, updatedBus: BusModel) {
        val current = _busList.value?.toMutableList() ?: mutableListOf()
        val index = current.indexOfFirst { it.busNumber == originalNumber }
        if (index != -1) {
            current[index] = updatedBus
            _busList.value = current
        }
    }

    fun deleteBus(busNumber: String) {
        val current = _busList.value?.toMutableList() ?: mutableListOf()
        current.removeAll { it.busNumber == busNumber }
        _busList.value = current
    }

    fun addBus(newBus: BusModel) {
        val current = _busList.value?.toMutableList() ?: mutableListOf()
        current.add(newBus)
        _busList.value = current
    }

    fun getBusByNumber(busNumber: String): BusModel? {
        return _busList.value?.find { it.busNumber == busNumber }
    }
}
