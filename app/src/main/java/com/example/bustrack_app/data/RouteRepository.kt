package com.example.bustrack_app.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.RouteModel
import com.example.bustrack_app.models.StopItem
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges

object RouteRepository {
    private val db = FirebaseFirestore.getInstance()
    private val routesCollection = db.collection("routes")

    private val _routeList = MutableLiveData<List<RouteModel>>(emptyList())
    val routeList: LiveData<List<RouteModel>> get() = _routeList
    @Volatile
    var isLatestSnapshotFromCache: Boolean = true
        private set

    init {
        fetchRoutesFromFirestore()
    }

    private fun fetchRoutesFromFirestore() {
        routesCollection.addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
            if (error != null) {
                Log.e("RouteRepository", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                publishRoutes(snapshot.documents.mapNotNull { document ->
                    document.toObject(RouteModel::class.java)?.copy(id = document.id)
                }, isFromCache = snapshot.metadata.isFromCache)
            }
        }
    }

    /** Gets a one-shot Firestore snapshot for decisions that must use the latest saved stops. */
    fun fetchLatestRoutes(onResult: (List<RouteModel>?) -> Unit) {
        routesCollection.get()
            .addOnSuccessListener { snapshot ->
                val routes = snapshot.documents.mapNotNull { document ->
                    document.toObject(RouteModel::class.java)?.copy(id = document.id)
                }
                publishRoutes(routes, isFromCache = snapshot.metadata.isFromCache)
                onResult(routes)
            }
            .addOnFailureListener { error ->
                Log.e("RouteRepository", "Could not fetch latest routes", error)
                onResult(null)
            }
    }

    private fun publishRoutes(routes: List<RouteModel>, isFromCache: Boolean = true) {
        isLatestSnapshotFromCache = isFromCache
        _routeList.postValue(routes)
        Log.d("RouteRepository", "Fetched ${routes.size} routes from Firestore")
        BusRepository.refreshBusList()
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
            routesCollection.document(finalRoute.id).update(
                "routeCode", finalRoute.routeCode,
                "routeName", finalRoute.routeName,
                "status", finalRoute.status,
                "busNo", finalRoute.busNo,
                "driverName", finalRoute.driverName,
                "stopsCount", finalRoute.stopsCount,
                "studentsCount", finalRoute.studentsCount,
                "stopsList", finalRoute.stopsList,
                "description", finalRoute.description,
                "startPoint", finalRoute.startPoint,
                "endPoint", finalRoute.endPoint,
                "pathPoints", finalRoute.pathPoints,
                "createdAt", finalRoute.createdAt
            )
                .addOnSuccessListener {
                    Log.d("RouteRepository", "Route successfully disabled and unassigned in Firestore!")
                    onComplete(true)
                }
                .addOnFailureListener { e ->
                    Log.e("RouteRepository", "Error disabling route", e)
                    onComplete(false)
                }
        } else {
            routesCollection.document(updatedRoute.id).update(
                "routeCode", updatedRoute.routeCode,
                "routeName", updatedRoute.routeName,
                "status", updatedRoute.status,
                "busNo", updatedRoute.busNo,
                "driverName", updatedRoute.driverName,
                "stopsCount", updatedRoute.stopsCount,
                "studentsCount", updatedRoute.studentsCount,
                "stopsList", updatedRoute.stopsList,
                "description", updatedRoute.description,
                "startPoint", updatedRoute.startPoint,
                "endPoint", updatedRoute.endPoint,
                "pathPoints", updatedRoute.pathPoints,
                "createdAt", updatedRoute.createdAt
            )
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
        routesCollection.whereEqualTo("id", routeId).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                val refs = (snapshot.documents.map { it.reference } + routesCollection.document(routeId))
                    .distinctBy { it.id }
                refs.forEach(batch::delete)
                batch.commit().addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }
}
