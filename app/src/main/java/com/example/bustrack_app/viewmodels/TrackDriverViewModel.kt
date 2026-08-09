package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.MediatorLiveData
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.models.DriverModel
import com.example.bustrack_app.models.RouteModel

class TrackDriverViewModel : ViewModel() {

    private val _targetDriver = MediatorLiveData<DriverModel?>()
    val targetDriver: LiveData<DriverModel?> = _targetDriver

    private val _assignedRoute = MediatorLiveData<RouteModel?>()
    val assignedRoute: LiveData<RouteModel?> = _assignedRoute

    private var driverId: String? = null

    fun setDriverId(id: String) {
        this.driverId = id
        
        _targetDriver.addSource(DriverRepository.driverList) { list ->
            _targetDriver.value = list.find { it.id == id }
        }

        _assignedRoute.addSource(_targetDriver) { driver ->
            if (driver != null) {
                _assignedRoute.addSource(RouteRepository.routeList) { routes ->
                    _assignedRoute.value = routes.find { it.routeName == driver.route || it.busNo == driver.assignedBus }
                }
            }
        }
    }
}