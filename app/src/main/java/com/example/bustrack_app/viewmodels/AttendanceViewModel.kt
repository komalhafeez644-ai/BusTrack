package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.AttendanceRecordModel

class AttendanceViewModel : ViewModel() {

    private val allRecords = mutableListOf<AttendanceRecordModel>()

    private val _records = MutableLiveData<List<AttendanceRecordModel>>()
    val records: LiveData<List<AttendanceRecordModel>> = _records

    private val _selectedDateText = MutableLiveData<String>()
    val selectedDateText: LiveData<String> = _selectedDateText

    // Selected filters
    private var selectedRoute = "All Route"
    private var selectedDate = "24/05/2026"

    init {
        _selectedDateText.value = selectedDate
        loadData()
    }

    private fun loadData() {
        com.example.bustrack_app.data.FirebaseRepository.fetchAttendance { list ->
            allRecords.clear()
            
            // Add initial mock data if database is empty for demo purposes
            if (list.isEmpty()) {
                allRecords.addAll(
                    listOf(
                        AttendanceRecordModel("FC-9901", "Ali Khan", "Route 1", "Green Valley", "07:20 AM", "08:05 AM", "02:20 PM", "03:05 PM", "24/05/2026"),
                        AttendanceRecordModel("FC-9021", "Arjun Jayaram", "Route 1", "Oak Ridge Estates", "07:15 AM", "08:00 AM", "02:35 PM", "03:20 PM", "24/05/2026"),
                        AttendanceRecordModel("FC-9104", "Sarah Mitchell", "Route 2", "Silver Springs", "07:30 AM", "08:15 AM", "02:15 PM", "03:00 PM", "24/05/2026")
                    )
                )
            } else {
                allRecords.addAll(list)
            }
            
            applyFilter()
        }
    }

    // ⭐ MAIN FILTER FUNCTION (Route + Date)
    fun setFilters(route: String, date: String) {
        selectedRoute = route
        selectedDate = date
        _selectedDateText.value = if (date.isEmpty() || date == "Select Date") "All Dates" else date
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

        if (selectedDate.isNotEmpty() && selectedDate != "Select Date" && selectedDate != "All Dates") {
            filtered = filtered.filter {
                it.date.trim().equals(selectedDate.trim(), ignoreCase = true)
            }
        }

        _records.value = filtered
    }
}