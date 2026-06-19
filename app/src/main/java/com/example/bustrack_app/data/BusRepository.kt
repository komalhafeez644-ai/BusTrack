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
            // 1. If no route name at all -> UNASSIGNED
            // 2. If has route but status was UNASSIGNED -> ACTIVE (First time assignment)
            // 3. Otherwise keep current status (respects INACTIVE toggle)
            val newStatus = when {
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
        busesCollection.document(originalNumber).set(updatedBus)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
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
