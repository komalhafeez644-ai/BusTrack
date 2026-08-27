 package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.R
import com.example.bustrack_app.models.NotificationSettingModel

class NotificationViewModel : ViewModel() {
    private val _settings = MutableLiveData<List<NotificationSettingModel>>()
    val settings: LiveData<List<NotificationSettingModel>> get() = _settings

    init {
        _settings.value = listOf(
            NotificationSettingModel(
                1, 
                "Bus Arrival Alert", 
                "Get notified when bus is 5 mins away", 
                R.drawable.directions_bus, 
                true
            ),
            NotificationSettingModel(
                2, 
                "Delay Notification", 
                "Alerts for traffic or breakdown delays", 
                R.drawable.warning, 
                false
            ),
            NotificationSettingModel(
                3, 
                "Route Change Alert", 
                "Updates about changes in bus route", 
                R.drawable.alt_route, 
                true
            ),
            NotificationSettingModel(
                4, 
                "Safety Alerts", 
                "Emergency and safety related broadcasts", 
                R.drawable.security_shield, 
                true
            )
        )
    }
}