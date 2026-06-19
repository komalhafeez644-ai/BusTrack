package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.data.RouteRepository
import com.example.bustrack_app.models.RouteModel

class RouteViewModel : ViewModel() {

    private val _searchQuery = MutableLiveData<String>("")
    private val _filteredRoutes = MediatorLiveData<List<RouteModel>>()
    val routeList: LiveData<List<RouteModel>> get() = _filteredRoutes

    init {
        // Both changes in query or repository should trigger an update
        _filteredRoutes.addSource(RouteRepository.routeList) { applyFilter() }
        _filteredRoutes.addSource(_searchQuery) { applyFilter() }
    }

    private fun applyFilter() {
        val query = _searchQuery.value ?: ""
        val fullList = RouteRepository.routeList.value ?: listOf()
        
        if (query.isEmpty()) {
            _filteredRoutes.value = fullList
        } else {
            _filteredRoutes.value = fullList.filter {
                it.routeName.lowercase().contains(query.lowercase()) ||
                        it.routeCode.lowercase().contains(query.lowercase()) ||
                        it.driverName.lowercase().contains(query.lowercase()) ||
                        it.busNo.lowercase().contains(query.lowercase())
            }
        }
    }

    fun filterRoutes(query: String) {
        _searchQuery.value = query
    }
}