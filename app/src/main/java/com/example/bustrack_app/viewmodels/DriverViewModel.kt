package com.example.bustrack_app.viewmodels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.R
import com.example.bustrack_app.models.DriverModel

class DriverViewModel : ViewModel() {

    private val _drivers = MutableLiveData<List<DriverModel>>()
    val drivers: LiveData<List<DriverModel>> = _drivers

    init {
        loadDrivers()
    }

    private fun loadDrivers() {
        _drivers.value = listOf(
            DriverModel("1", "Robert Henderson", "Active", "Bus-01", "Route-02", R.drawable.driver_image_one),
            DriverModel("2", "Sarah Jenkins", "Idle", "Bus-08", "Route-14", 0),
            DriverModel("3", "Michael Chen", "Active", "Bus-04", "Express-1", 0)
        )
    }
    // DriverViewModel.kt mein ye function add karein
    fun addDriver(newDriver: DriverModel) {
        val currentList = _drivers.value?.toMutableList() ?: mutableListOf()
        currentList.add(0, newDriver) // Naya driver list ke shuru mein add hoga
        _drivers.value = currentList
    }
}