package com.example.bustrack_app.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.DriverModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

object DriverRepository {
    private val db = FirebaseFirestore.getInstance()
    private val driversCollection = db.collection("drivers")

    private val _driverList = MutableLiveData<List<DriverModel>>(emptyList())
    val driverList: LiveData<List<DriverModel>> get() = _driverList

    init {
        fetchDriversFromFirestore()
    }

    private fun fetchDriversFromFirestore() {
        driversCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("DriverRepository", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val drivers = snapshot.toObjects<DriverModel>()
                _driverList.value = drivers
                Log.d("DriverRepository", "Fetched ${drivers.size} drivers from Firestore")
            }
        }
    }

    fun deleteDriver(driverId: String, onComplete: (Boolean) -> Unit = {}) {
        driversCollection.document(driverId).get().addOnSuccessListener { snapshot ->
            val driver = snapshot.toObject(DriverModel::class.java)
            
            // 1. Unassign from Bus and Route first
            driver?.assignedBus?.let { busNo ->
                BusRepository.getBusByNumber(busNo)?.let { bus ->
                    BusRepository.updateBusDetails(busNo, bus.copy(driverName = null))
                }
                
                RouteRepository.routeList.value?.find { it.busNo == busNo }?.let { route ->
                    RouteRepository.updateRoute(route.copy(driverName = ""))
                }
            }

            // 2. Delete from Auth (users collection) - We need the UID
            db.collection("users").whereEqualTo("email", driver?.email).get()
                .addOnSuccessListener { users ->
                    users.documents.firstOrNull()?.id?.let { uid ->
                        db.collection("users").document(uid).delete()
                    }
                }

            // 3. Delete from drivers collection
            driversCollection.document(driverId).delete()
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun addDriver(newDriver: DriverModel, onComplete: (Boolean) -> Unit = {}) {
        driversCollection.document(newDriver.driverId.ifEmpty { newDriver.id }).set(newDriver)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateDriver(updatedDriver: DriverModel, onComplete: (Boolean) -> Unit = {}) {
        driversCollection.document(updatedDriver.driverId.ifEmpty { updatedDriver.id }).set(updatedDriver)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
