package com.example.bustrack_app.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.BusModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

object BusRepository {
    private val db = FirebaseFirestore.getInstance()
    private val busesCollection = db.collection("buses")

    private val _busList = MutableLiveData<List<BusModel>>(emptyList())
    val busList: LiveData<List<BusModel>> get() = _busList

    init {
        fetchBusesFromFirestore()
    }

    private fun fetchBusesFromFirestore() {
        busesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("BusRepository", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val buses = snapshot.toObjects<BusModel>()
                _busList.value = buses
                Log.d("BusRepository", "Fetched ${buses.size} buses from Firestore")
            }
        }
    }

    fun refreshBusList() {
        val currentBuses = _busList.value ?: return
        val routes = RouteRepository.routeList.value ?: listOf()
        
        val updatedBuses = currentBuses.map { bus ->
            val assignedRoute = routes.find { it.busNo == bus.busNumber }
            
            // Sync from route if found, otherwise keep bus's own data
            val routeName = assignedRoute?.routeName ?: bus.routeName
            val driverName = assignedRoute?.driverName ?: bus.driverName
            
            // Status logic: 
            // 1. If status is INACTIVE or Disabled, keep it as is
            // 2. If no route name at all -> UNASSIGNED
            // 3. If has route but status was UNASSIGNED -> ACTIVE (First time assignment)
            // 4. Otherwise keep current status
            val newStatus = when {
                bus.status == "INACTIVE" || bus.status == "Disabled" -> bus.status
                routeName.isNullOrEmpty() -> "UNASSIGNED"
                bus.status == "UNASSIGNED" -> "ACTIVE"
                else -> bus.status
            }

            bus.copy(
                routeName = routeName,
                driverName = driverName,
                status = newStatus
            )
        }
        
        // This is a local refresh, but we should probably save these back to Firestore 
        // if we want them to persist across all apps.
        _busList.value = updatedBuses
    }

    fun updateBusDetails(originalNumber: String, updatedBus: BusModel, onComplete: (Boolean) -> Unit = {}) {
        val isNumberChanged = originalNumber != updatedBus.busNumber

        // 1. Cleanup assignments first if status is INACTIVE
        if (updatedBus.status == "INACTIVE") {
            val currentBus = _busList.value?.find { it.busNumber == originalNumber }
            currentBus?.let { bus ->
                bus.driverName?.let { dName ->
                    DriverRepository.driverList.value?.find { it.name == dName }?.let { driver ->
                        DriverRepository.updateDriver(driver.copy(assignedBus = null, route = null))
                    }
                }
                bus.routeName?.let { rName ->
                    RouteRepository.routeList.value?.find { it.routeName == rName }?.let { route ->
                        RouteRepository.updateRoute(route.copy(busNo = "", driverName = ""))
                    }
                }
            }
        }

        // 2. Resolve duplicates and update the authoritative document
        // We query by busNumber field to find any documents (potentially with random IDs) 
        // that match the bus being updated.
        busesCollection.whereEqualTo("busNumber", originalNumber).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                
                // Delete all existing documents that have this bus number
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                
                // Commit the deletions
                batch.commit().addOnCompleteListener {
                    // 3. Now save the authoritative document using busNumber as ID
                    val finalBus = if (updatedBus.status == "INACTIVE") {
                        updatedBus.copy(driverName = null, routeName = null)
                    } else {
                        updatedBus
                    }
                    
                    busesCollection.document(updatedBus.busNumber).set(finalBus)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }
            }
            .addOnFailureListener {
                // Fallback to direct set if query fails
                busesCollection.document(updatedBus.busNumber).set(updatedBus)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
    }

    fun deleteBus(busNumber: String, onComplete: (Boolean) -> Unit = {}) {
        // 1. Cleanup assignments in other collections
        val busToDelete = _busList.value?.find { it.busNumber == busNumber }
        busToDelete?.let { bus ->
            bus.driverName?.let { dName ->
                DriverRepository.driverList.value?.find { it.name == dName }?.let { driver ->
                    DriverRepository.updateDriver(driver.copy(assignedBus = null, route = null))
                }
            }
            bus.routeName?.let { rName ->
                RouteRepository.routeList.value?.find { it.routeName == rName }?.let { route ->
                    RouteRepository.updateRoute(route.copy(busNo = "", driverName = ""))
                }
            }
        }

        // 2. Delete ALL documents matching this bus number to clean up any duplicates/ghosts
        busesCollection.whereEqualTo("busNumber", busNumber).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener {
                // Fallback to direct ID-based delete
                busesCollection.document(busNumber).delete()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
    }

    fun addBus(newBus: BusModel, onComplete: (Boolean) -> Unit = {}) {
        // Find and delete any existing documents that have the same bus number
        // but potentially different IDs to prevent duplicates.
        busesCollection.whereEqualTo("busNumber", newBus.busNumber).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                for (doc in snapshot.documents) {
                    batch.delete(doc.reference)
                }
                batch.commit().addOnCompleteListener {
                    // Now save the new bus with busNumber as ID
                    busesCollection.document(newBus.busNumber).set(newBus)
                        .addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                }
            }
            .addOnFailureListener {
                // Fallback to direct set
                busesCollection.document(newBus.busNumber).set(newBus)
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
    }

    fun getBusByNumber(busNumber: String): BusModel? {
        return _busList.value?.find { it.busNumber == busNumber }
    }
}
