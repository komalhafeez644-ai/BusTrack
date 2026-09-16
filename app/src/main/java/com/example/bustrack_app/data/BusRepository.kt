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
        if (updatedBus.status == "INACTIVE") {
            // Business Logic for INACTIVE status
            val currentBus = _busList.value?.find { it.busNumber == originalNumber }
            
            currentBus?.let { bus ->
                // 1. Release Driver: Hassan becomes available for another active bus
                bus.driverName?.let { dName ->
                    DriverRepository.driverList.value?.find { it.name == dName }?.let { driver ->
                        DriverRepository.updateDriver(driver.copy(assignedBus = null, route = null))
                    }
                }
                
                // 2. Release Route: Route becomes available for another active bus
                bus.routeName?.let { rName ->
                    RouteRepository.routeList.value?.find { it.routeName == rName }?.let { route ->
                        RouteRepository.updateRoute(route.copy(busNo = "", driverName = ""))
                    }
                }
            }
            
            // Clear assignments in the document itself to ensure consistency
            val finalBus = updatedBus.copy(driverName = null, routeName = null)
            busesCollection.document(originalNumber).set(finalBus)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        } else {
            // For ACTIVE or other statuses, proceed with standard update
            busesCollection.document(originalNumber).set(updatedBus)
                .addOnSuccessListener { onComplete(true) }
                .addOnFailureListener { onComplete(false) }
        }
    }

    fun deleteBus(busNumber: String, onComplete: (Boolean) -> Unit = {}) {
        busesCollection.document(busNumber).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun addBus(newBus: BusModel, onComplete: (Boolean) -> Unit = {}) {
        busesCollection.document(newBus.busNumber).set(newBus)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun getBusByNumber(busNumber: String): BusModel? {
        return _busList.value?.find { it.busNumber == busNumber }
    }
}
