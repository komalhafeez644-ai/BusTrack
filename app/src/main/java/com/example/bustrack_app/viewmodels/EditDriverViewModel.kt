package com.example.bustrack_app.viewmodels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.DriverModel

class EditDriverViewModel : ViewModel() {
    val driverData = MutableLiveData<DriverModel>()

    fun loadDriver(id: String) {
        driverData.value = DriverModel(
            id = id,
            name = "Ahmed Hassan",
            status = "Active",
            assignedBus = "Bus 101 (Volvo B11R)",
            route = "Route 42 - Sector 15 North",
            profileImage = 0,
            cnic = "42101-1234567-1",
            phone = "+92 333 4567890",
            email = "ahmed.hassan@email.com" // Pre-filled field data
        )
    }

    // FIXED: Added email argument signatures to match current dynamic model constraints
    fun updateDriver(name: String, cnic: String, route: String, phone: String, email: String) {
        val currentDriver = driverData.value
        currentDriver?.let {
            it.name = name
            it.cnic = cnic
            it.route = route
            it.phone = phone
            it.email = email // Update statement mapping

            driverData.value = it
        }
    }
}