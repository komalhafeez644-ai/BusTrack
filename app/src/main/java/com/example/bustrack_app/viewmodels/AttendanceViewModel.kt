package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.AttendanceRecordModel

class AttendanceViewModel : ViewModel() {

    private val allRecords = mutableListOf<AttendanceRecordModel>()

    private val _records = MutableLiveData<List<AttendanceRecordModel>>()
    val records: LiveData<List<AttendanceRecordModel>> = _records

    // Selected filters
    private var selectedRoute = "All Route"
    private var selectedDate = ""

    init {
        loadData()
    }

    private fun loadData() {

        allRecords.clear()

        allRecords.addAll(
            listOf(

                AttendanceRecordModel("Arjun Jayaram", "#FC-9021", "Oak Ridge Estates", "07:15 AM", "LATE", "route 1", "24/05/2026"),
                AttendanceRecordModel("Sarah Mitchell", "#FC-9104", "Silver Springs", "07:30 AM", "PRESENT", "route 2", "24/05/2026"),
                AttendanceRecordModel("Leo Knight", "#FC-8850", "Maple Street", "07:45 AM", "ABSENT", "route 3", "23/05/2026"),
                AttendanceRecordModel("Emma Watson", "#FC-7721", "West Side", "07:10 AM", "PRESENT", "route 3", "24/05/2026"),
                AttendanceRecordModel("Ali Khan", "#FC-9901", "Green Valley", "07:20 AM", "PRESENT", "route 1", "23/05/2026"),
                AttendanceRecordModel("Hina Ali", "#FC-9902", "City Center", "07:35 AM", "LATE", "route 2", "24/05/2026"),
                AttendanceRecordModel("Usman Tariq", "#FC-9903", "Lake View", "07:50 AM", "ABSENT", "route 3", "24/05/2026")
            )
        )

        applyFilter()
    }

    // ⭐ MAIN FILTER FUNCTION (Route + Date)
    fun setFilters(route: String, date: String) {
        selectedRoute = route
        selectedDate = date
        applyFilter()
    }

    private fun applyFilter() {

        var filtered = allRecords.toList()

        // Route filter
        if (selectedRoute != "All Route") {
            filtered = filtered.filter {
                it.route.trim().equals(selectedRoute.trim(), ignoreCase = true)
            }
        }

        if (selectedDate.isNotEmpty() && selectedDate != "Select Date") {
            filtered = filtered.filter {
                it.date.trim().equals(selectedDate.trim(), ignoreCase = true)
            }
        }

        _records.value = filtered
    }
}