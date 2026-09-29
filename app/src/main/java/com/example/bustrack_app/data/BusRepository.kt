package com.example.bustrack_app.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.BusModel
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore

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
                val buses = snapshot.documents.mapNotNull { document ->
                    document.toObject(BusModel::class.java)?.copy(firestoreDocumentId = document.id)
                }
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

        // Update the document already representing this bus. Recreating it under the
        // bus number caused legacy/random-ID documents to survive failed queries and
        // later appear as duplicate buses in the snapshot listener.
        busesCollection.whereEqualTo("busNumber", originalNumber).get()
            .addOnSuccessListener { snapshot ->
                val existing = snapshot.documents
                val updateExisting: (List<DocumentSnapshot>) -> Unit = { matches ->
                    val target = matches.firstOrNull { it.id == updatedBus.firestoreDocumentId }
                        ?: matches.firstOrNull()
                    if (target == null) {
                        onComplete(false)
                    } else {
                        val finalBus = if (updatedBus.status == "INACTIVE") {
                            updatedBus.copy(driverName = null, routeName = null)
                        } else updatedBus
                        val batch = db.batch()
                        batch.update(
                            target.reference,
                            "busNumber", finalBus.busNumber,
                            "totalSeats", finalBus.totalSeats,
                            "driverName", finalBus.driverName,
                            "routeName", finalBus.routeName,
                            "status", finalBus.status
                        )
                        matches.filter { it.id != target.id }.forEach { batch.delete(it.reference) }
                        batch.commit()
                            .addOnSuccessListener { onComplete(true) }
                            .addOnFailureListener { onComplete(false) }
                    }
                }
                if (existing.isNotEmpty()) {
                    updateExisting(existing)
                } else {
                    busesCollection.document(updatedBus.firestoreDocumentId.ifBlank { originalNumber }).get()
                        .addOnSuccessListener { existingById ->
                            updateExisting(listOfNotNull(existingById.takeIf { it.exists() }))
                        }
                        .addOnFailureListener { onComplete(false) }
                }
            }
            .addOnFailureListener { onComplete(false) }
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

        // Delete every copy by its stored number and the legacy canonical document ID.
        busesCollection.whereEqualTo("busNumber", busNumber).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                val documentIds = (snapshot.documents.map { it.id } +
                    listOfNotNull(busToDelete?.firestoreDocumentId) + busNumber).toSet()
                documentIds.forEach { batch.delete(busesCollection.document(it)) }
                batch.commit()
                    .addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun addBus(newBus: BusModel, onComplete: (Boolean) -> Unit = {}) {
        // Reuse an existing document (including legacy IDs) and remove any duplicate
        // copies in the same batch. Only a genuinely new bus gets a new document.
        busesCollection.whereEqualTo("busNumber", newBus.busNumber).get()
            .addOnSuccessListener { snapshot ->
                if (snapshot.documents.isNotEmpty()) {
                    val target = snapshot.documents.first()
                    val batch = db.batch()
                    batch.set(target.reference, newBus.copy(firestoreDocumentId = ""))
                    snapshot.documents.drop(1).forEach { batch.delete(it.reference) }
                    batch.commit().addOnSuccessListener { onComplete(true) }
                        .addOnFailureListener { onComplete(false) }
                } else {
                    busesCollection.document(newBus.busNumber).get()
                        .addOnSuccessListener { existingById ->
                            val reference = if (existingById.exists()) existingById.reference
                                else busesCollection.document(newBus.busNumber)
                            reference.set(newBus.copy(firestoreDocumentId = ""))
                                .addOnSuccessListener { onComplete(true) }
                                .addOnFailureListener { onComplete(false) }
                        }
                        .addOnFailureListener { onComplete(false) }
                }
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun getBusByNumber(busNumber: String): BusModel? {
        return _busList.value?.find { it.busNumber == busNumber }
    }
}
