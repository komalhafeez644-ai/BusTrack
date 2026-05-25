package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.DriverRepository
import com.example.bustrack_app.models.DriverModel

class DriverViewModel : ViewModel() {

    val drivers: LiveData<List<DriverModel>> = DriverRepository.driverList

    fun addDriver(newDriver: DriverModel) {
        DriverRepository.addDriver(newDriver)
    }

    fun deleteDriver(driverId: String) {
        DriverRepository.deleteDriver(driverId)
    }
}
