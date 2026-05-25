package com.example.bustrack_app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.R
import com.example.bustrack_app.models.DriverModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object DriverRepository {
    private val _driverList = MutableLiveData<List<DriverModel>>()
    val driverList: LiveData<List<DriverModel>> get() = _driverList

    private const val PREFS_NAME = "driver_prefs"
    private const val KEY_DRIVERS = "drivers_list"
    private var sharedPrefs: SharedPreferences? = null

    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs?.getString(KEY_DRIVERS, null)
        
        if (json != null) {
            val type = object : TypeToken<MutableList<DriverModel>>() {}.type
            val list: MutableList<DriverModel> = Gson().fromJson(json, type)
            _driverList.value = list
        } else {
            // Initial Dummy Data
            val initial = mutableListOf(
                DriverModel("1", "Robert Henderson", "Active", "Bus-01", "Route-02 - Sector 11", R.drawable.driver_image_one, "42101-1234567-1", "+92 300 1234567", "robert.henderson@email.com"),
                DriverModel("2", "Sarah Jenkins", "Idle", "Bus-08", "Route-14", 0, "42101-7654321-2", "+92 312 9876543", "sarah.jenkins@email.com"),
                DriverModel("3", "Michael Chen", "Active", "Bus-04", "Express-1", 0, "42101-1111111-3", "+92 333 5555555", "michael.chen@email.com")
            )
            _driverList.value = initial
            saveToPrefs()
        }
    }

    private fun saveToPrefs() {
        val json = Gson().toJson(_driverList.value)
        sharedPrefs?.edit()?.putString(KEY_DRIVERS, json)?.apply()
    }

    fun deleteDriver(driverId: String) {
        val current = _driverList.value?.toMutableList() ?: mutableListOf()
        current.removeAll { it.id == driverId }
        _driverList.postValue(current)
        saveToPrefs()
    }

    fun addDriver(newDriver: DriverModel) {
        val current = _driverList.value?.toMutableList() ?: mutableListOf()
        current.add(0, newDriver)
        _driverList.postValue(current)
        saveToPrefs()
    }

    fun updateDriver(updatedDriver: DriverModel) {
        val current = _driverList.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.id == updatedDriver.id }
        if (index != -1) {
            current[index] = updatedDriver
            _driverList.postValue(current)
            saveToPrefs()
        }
    }
}
