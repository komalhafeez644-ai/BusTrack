package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.StopItem

class AddRouteViewModel : ViewModel() {

    // Instruction text
    private val _instruction =
        MutableLiveData("Tap on map to add route point")

    val instruction: LiveData<String>
        get() = _instruction


    // Stops list
    private val _stops =
        MutableLiveData<MutableList<StopItem>>(mutableListOf())

    val stops: LiveData<MutableList<StopItem>>
        get() = _stops


    fun updateInstruction(text: String) {
        _instruction.value = text
    }

    fun addStop(stop: StopItem) {

        val currentStops =
            _stops.value ?: mutableListOf()

        currentStops.add(stop)

        _stops.value = currentStops
    }

    fun clearStops() {
        _stops.value = mutableListOf()
    }
}