package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.BusRepository
import com.example.bustrack_app.models.BusModel

class BusViewModel : ViewModel() {

    val busList: LiveData<List<BusModel>> = BusRepository.busList

    fun updateBusDetails(busNumber: String, updatedBus: BusModel, onComplete: (Boolean) -> Unit = {}) {
        BusRepository.updateBusDetails(busNumber, updatedBus, onComplete)
    }

    fun deleteBusFromFleet(busNumber: String, onComplete: (Boolean) -> Unit = {}) {
        BusRepository.deleteBus(busNumber, onComplete)
    }

    fun addNewBus(newBus: BusModel, onComplete: (Boolean) -> Unit = {}) {
        BusRepository.addBus(newBus, onComplete)
    }
}
