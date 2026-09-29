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

            // Await removal of the Firestore user profile as well as the driver
            // document. The previous fire-and-forget email lookup could race a
            // refresh and leave the profile available to recreate a driver record.
            val usersCollection = db.collection("users")
            val email = driver?.email?.trim().orEmpty()
            val uid = driver?.uid?.trim().orEmpty().ifEmpty { driverId }
            val finishDelete: (
                List<com.google.firebase.firestore.DocumentSnapshot>,
                List<com.google.firebase.firestore.DocumentSnapshot>
            ) -> Unit = { drivers, users ->
                val batch = db.batch()
                (listOf(snapshot.reference) + drivers.map { it.reference } + users.map { it.reference } +
                    usersCollection.document(uid))
                    .distinctBy { it.path }
                    .forEach(batch::delete)
                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            val finishWithUsers: (List<com.google.firebase.firestore.DocumentSnapshot>) -> Unit = { drivers ->
                if (email.isNotEmpty()) {
                    usersCollection.whereEqualTo("email", email).get()
                        .addOnSuccessListener { finishDelete(drivers, it.documents) }
                        .addOnFailureListener { onComplete(false) }
                } else finishDelete(drivers, emptyList())
            }
            if (email.isNotEmpty()) {
                driversCollection.whereEqualTo("email", email).get()
                    .addOnSuccessListener { finishWithUsers(it.documents) }
                    .addOnFailureListener { onComplete(false) }
            } else finishWithUsers(emptyList())
        }.addOnFailureListener { onComplete(false) }
    }

    fun addDriver(newDriver: DriverModel, onComplete: (Boolean) -> Unit = {}) {
        driversCollection.document(newDriver.driverId.ifEmpty { newDriver.id }).set(newDriver)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateDriver(updatedDriver: DriverModel, onComplete: (Boolean) -> Unit = {}) {
        driversCollection.document(updatedDriver.driverId.ifBlank { updatedDriver.id }).set(updatedDriver)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
