package com.example.bustrack_app.viewmodels

import androidx.lifecycle.*
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.models.RouteModel

class TrackDriverViewModel : ViewModel() {

    private val _driverId = MutableLiveData<String>()

    val targetDriver: LiveData<DriverModel?> = MediatorLiveData<DriverModel?>().apply {
        addSource(_driverId) { id ->
            val driver = DriverRepository.driverList.value?.find { it.driverId == id }
            value = driver
        }
        addSource(DriverRepository.driverList) { list ->
            val driver = list.find { it.driverId == _driverId.value }
            value = driver
        }
        addSource(RouteRepository.routeList) { _ ->
            val driver = value ?: DriverRepository.driverList.value?.find { it.driverId == _driverId.value }
            driver?.let { value = it }
        }
    }

    val assignedRoute: LiveData<RouteModel?> = targetDriver.switchMap { driver ->
        RouteRepository.routeList.map { routes ->
            driver?.assignedBus?.takeIf { it.isNotBlank() && !it.equals("Not Assigned", true) }
                ?.let { busNo -> routes.find { it.busNo.equals(busNo, ignoreCase = true) } }
        }
    }

    fun setDriverId(id: String) {
        _driverId.value = id
    }
}
