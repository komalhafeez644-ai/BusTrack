package com.example.bustrack_app.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

object RouteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val routesCollection = db.collection("routes")

    private val _routeList = MutableLiveData<List<RouteModel>>(emptyList())
    val routeList: LiveData<List<RouteModel>> get() = _routeList

    init {
        fetchRoutesFromFirestore()
    }

    private fun fetchRoutesFromFirestore() {
        routesCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("RouteRepository", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val routes = snapshot.toObjects<RouteModel>()
                _routeList.value = routes
                Log.d("RouteRepository", "Fetched ${routes.size} routes from Firestore")
                
                // Refresh BusRepository to sync with new route data
                BusRepository.refreshBusList()
            }
        }
    }

    fun getBusForRoute(routeName: String): String {
        return _routeList.value?.find { it.routeName.equals(routeName, true) || it.routeCode.equals(routeName, true) }?.busNo ?: ""
    }

    fun updateRoute(updatedRoute: RouteModel, onComplete: (Boolean) -> Unit = {}) {
        if (updatedRoute.status == "Disabled") {
            // 1. Release assigned bus
            if (updatedRoute.busNo.isNotEmpty()) {
                BusRepository.getBusByNumber(updatedRoute.busNo)?.let { bus ->
                    BusRepository.updateBusDetails(updatedRoute.busNo, bus.copy(routeName = null))
                }
            }

            // 2. Release assigned driver
            if (updatedRoute.driverName.isNotEmpty()) {
                DriverRepository.driverList.value?.find { it.name == updatedRoute.driverName }?.let { driver ->
                    DriverRepository.updateDriver(driver.copy(assignedBus = null, route = null))
                }
            }

            // Clear assignments in the route model itself
            val finalRoute = updatedRoute.copy(busNo = "", driverName = "")
            routesCollection.document(finalRoute.id).set(finalRoute)
                .addOnSuccessListener {
                    Log.d("RouteRepository", "Route successfully disabled and unassigned in Firestore!")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("RouteRepository", "Error disabling route", e)
                    onComplete(false)
                }
        } else {
            routesCollection.document(updatedRoute.id).set(updatedRoute)
                .addOnSuccessListener {
                    Log.d("RouteRepository", "Route successfully updated in Firestore!")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("RouteRepository", "Error updating route", e)
                    onComplete(false)
                }
        }
    }

    fun deleteRoute(routeId: String, onComplete: (Boolean) -> Unit = {}) {
        routesCollection.document(routeId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
