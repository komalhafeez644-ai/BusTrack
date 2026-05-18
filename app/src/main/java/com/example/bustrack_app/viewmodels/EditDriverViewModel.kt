package com.example.bustrack_app.viewmodels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.DriverModel

class EditDriverViewModel : ViewModel() {
    // LiveData driver ka data hold karne ke liye
    val driverData = MutableLiveData<DriverModel>()

    /**
     * Driver ka data load karne ke liye function.
     * Filhal hum dummy data use kar rahe hain jo exactly aapki screen
     * design (Ahmed Hassan) ke mutabiq hai.
     */
    fun loadDriver(id: String) {
        // Dummy data exactly as per your image
        driverData.value = DriverModel(
            id = id,
            name = "Ahmed Hassan",
            status = "Active",
            assignedBus = "Bus 101 (Volvo B11R)",
            route = "Route 42 - Sector 15 North",
            profileImage = 0, // 0 matlab default/avatar show hoga
            cnic = "42101-1234567-1",
            phone = "+92 333 4567890"
        )
    }

    /**
     * UI se naya data lekar save karne ki logic yahan ayegi.
     */
    fun updateDriver(name: String, cnic: String, route: String, phone: String) {
        val currentDriver = driverData.value
        currentDriver?.let {
            it.name = name
            it.cnic = cnic
            it.route = route
            it.phone = phone

            // Yahan aap Firebase ya Database call add karengi baad mein
            driverData.value = it
        }
    }
}