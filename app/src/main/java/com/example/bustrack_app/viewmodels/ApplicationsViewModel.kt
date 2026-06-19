package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.R
import com.example.bustrack_app.models.ApplicationModel

class ApplicationsViewModel : ViewModel() {

    private val _applications = MutableLiveData<List<ApplicationModel>>()
    val applications: LiveData<List<ApplicationModel>> = _applications

    init {
        loadSampleData()
    }

    private fun loadSampleData() {
        val list = listOf(
            ApplicationModel(1, "Aryan Sharma", "Grade 10 • Section B", "Green Park Sector 4", "92 3459745703", "2 HRS AGO", "Pending", R.drawable.ic_person),
            ApplicationModel(2, "Vanya Patel", "Grade 8 • Section A", "Sunrise Heights Dr.", "92 3459745703", "5 HRS AGO", "Pending", R.drawable.ic_person),
            ApplicationModel(3, "Rohan Gupta", "Grade 12 • Section C", "Oakwood Avenue 12", "92 3459745703", "YESTERDAY", "Pending", R.drawable.ic_person)
        )
        _applications.value = list
    }

    fun filterByStatus(status: String) {
        // In a real app, this would filter from a repository or local copy
        // For now, we just reload sample data if "All" is clicked, or filter if specific status
        val allData = listOf(
            ApplicationModel(1, "Aryan Sharma", "Grade 10 • Section B", "Green Park Sector 4", "92 3459745703", "2 HRS AGO", "Pending", R.drawable.ic_person),
            ApplicationModel(2, "Vanya Patel", "Grade 8 • Section A", "Sunrise Heights Dr.", "92 3459745703", "5 HRS AGO", "Pending", R.drawable.ic_person),
            ApplicationModel(3, "Rohan Gupta", "Grade 12 • Section C", "Oakwood Avenue 12", "92 3459745703", "YESTERDAY", "Pending", R.drawable.ic_person)
        )
        
        if (status == "All") {
            _applications.value = allData
        } else {
            _applications.value = allData.filter { it.status == status }
        }
    }
}
