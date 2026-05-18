package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.AttendanceRecordModel

class AttendanceViewModel : ViewModel() {

    private val _records = MutableLiveData<List<AttendanceRecordModel>>()
    val records: LiveData<List<AttendanceRecordModel>> = _records

    init {
        loadData()
    }

    private fun loadData() {
        // Dummy data jo aapke table mein nazar aayega
        _records.value = listOf(
            AttendanceRecordModel("Arjun Jayaram", "#FC-9021", "Oak Ridge Estates", "07:15 AM", "LATE"),
            AttendanceRecordModel("Sarah Mitchell", "#FC-9104", "Silver Springs", "07:30 AM", "PRESENT"),
            AttendanceRecordModel("Leo Knight", "#FC-8850", "Maple Street", "07:45 AM", "ABSENT"),
            AttendanceRecordModel("Emma Watson", "#FC-7721", "West Side", "07:10 AM", "PRESENT")
        )
    }

    // Spinner ya Date filter karne ke liye function (Future use)
    fun filterData(route: String, date: String) {
        // Yahan logic aayega jab aap database connect karengi
    }
}