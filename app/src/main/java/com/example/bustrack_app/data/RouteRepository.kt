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

    fun deleteRoute(routeId: String, onComplete: (Boolean) -> Unit = {}) {
        routesCollection.document(routeId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
}
