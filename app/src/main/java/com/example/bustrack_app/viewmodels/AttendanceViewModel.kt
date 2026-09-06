package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.AttendanceRecordModel

class AttendanceViewModel : ViewModel() {

    private val allRecords = mutableListOf<AttendanceRecordModel>()
    private val allStudents = mutableListOf<com.example.bustrack_app.models.StudentModel>()

    private val _records = MutableLiveData<List<AttendanceRecordModel>>()
    val records: LiveData<List<AttendanceRecordModel>> = _records

    private val _selectedDateText = MutableLiveData<String>()
    val selectedDateText: LiveData<String> = _selectedDateText

    private val _availableRoutes = MutableLiveData<List<String>>()
    val availableRoutes: LiveData<List<String>> = _availableRoutes

    // Selected filters
    private var selectedRoute = "All Route"
    private var selectedDate = ""

    private var studentsListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var attendanceListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        selectedDate = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        _selectedDateText.value = selectedDate
        loadInitialData()
    }

    private fun loadInitialData() {
        loadRoutes()
        
        // Listen to students separately
        studentsListener?.remove()
        studentsListener = com.example.bustrack_app.data.FirebaseRepository.fetchStudents { students ->
            allStudents.clear()
            allStudents.addAll(students)
            applyFilter()
        }

        // Listen to attendance separately (Real-time synchronization)
        attendanceListener?.remove()
        attendanceListener = com.example.bustrack_app.data.FirebaseRepository.fetchAttendance { list ->
            allRecords.clear()
            allRecords.addAll(list)
            applyFilter()
        }
    }

    private fun loadAttendanceData() {
        // This method is now legacy as the listener handles it in loadInitialData,
        // but we'll keep it for any manual refresh needs.
        com.example.bustrack_app.data.FirebaseRepository.fetchAttendance { list ->
            allRecords.clear()
            allRecords.addAll(list)
            applyFilter()
        }
    }

    override fun onCleared() {
        studentsListener?.remove()
        attendanceListener?.remove()
        super.onCleared()
    }

    private fun loadRoutes() {
        com.example.bustrack_app.data.FirebaseRepository.fetchRoutes { routes ->
            val routeNames = mutableListOf("All Route")
            routeNames.addAll(routes.map { it.routeName })
            _availableRoutes.value = routeNames
        }
    }

    // ⭐ MAIN FILTER FUNCTION (Route + Date)
    fun setFilters(route: String, date: String) {
        selectedRoute = route
        selectedDate = date
        _selectedDateText.value = if (date.isEmpty() || date == "Select Date") "All Dates" else date
        applyFilter()
    }

    fun saveRecord(record: AttendanceRecordModel) {
        com.example.bustrack_app.data.FirebaseRepository.saveAttendance(record) { success ->
            if (success) {
                loadAttendanceData()
            }
        }
    }

    private fun applyFilter() {
        if (selectedDate.isEmpty() || selectedDate == "Select Date") {
            _records.value = emptyList()
            return
        }

        // Normalize date for comparison with Firestore data (which uses hyphens)
        val normalizedSelectedDate = selectedDate.replace("/", "-")

        // 1. Filter students by route
        val studentsForRoute = if (selectedRoute == "All Route") {
            allStudents
        } else {
            allStudents.filter { it.route?.trim().equals(selectedRoute.trim(), ignoreCase = true) }
        }

        // 2. Map students to attendance records (using real data if exists, otherwise placeholder)
        val finalRecords = studentsForRoute.map { student ->
            val existing = allRecords.find { 
                it.studentId == student.id && (it.date == selectedDate || it.date == normalizedSelectedDate) 
            }
            existing ?: AttendanceRecordModel(
                studentId = student.id,
                studentName = student.name,
                route = student.route ?: "N/A",
                stop = student.stopName ?: "N/A",
                morningPickup = "Pending",
                morningDrop = "--",
                eveningPickup = "Pending",
                eveningDrop = "--",
                date = selectedDate
            )
        }

        _records.value = finalRecords
    }
}