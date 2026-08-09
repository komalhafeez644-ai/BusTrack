package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.DriverModel

class LiveTrackingViewModel : ViewModel() {

    private val _activeDrivers = MutableLiveData<List<DriverModel>>()
    val activeDrivers: LiveData<List<DriverModel>> = _activeDrivers

    private val _selectedDriver = MutableLiveData<DriverModel?>()
    val selectedDriver: LiveData<DriverModel?> = _selectedDriver

    init {
        startTracking()
    }

    private fun startTracking() {
        FirebaseRepository.fetchDrivers { allDrivers ->
            // Filter only active drivers who have location data (Case-insensitive check)
            val active = allDrivers.filter { 
                (it.status.equals("Active", true) || it.status.equals("ACTIVE", true) || it.status.equals("On Duty", true)) 
                && it.latitude != 0.0 && it.longitude != 0.0 
            }
            _activeDrivers.value = active
            
            // If the currently selected driver moved, update the detail card
            _selectedDriver.value?.let { selected ->
                active.find { it.id == selected.id }?.let { updated ->
                    _selectedDriver.value = updated
                }
            }
        }
    }

    fun selectDriver(driver: DriverModel) {
        _selectedDriver.value = driver
    }
}