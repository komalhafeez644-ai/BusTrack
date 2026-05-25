package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.BusRepository
import com.example.bustrack_app.models.BusModel

class BusViewModel : ViewModel() {

    val busList: LiveData<List<BusModel>> = BusRepository.busList

    fun updateBusDetails(busNumber: String, updatedBus: BusModel) {
        BusRepository.updateBusDetails(busNumber, updatedBus)
    }

    fun deleteBusFromFleet(busNumber: String) {
        BusRepository.deleteBus(busNumber)
    }

    fun addNewBus(newBus: BusModel) {
        BusRepository.addBus(newBus)
    }
}
