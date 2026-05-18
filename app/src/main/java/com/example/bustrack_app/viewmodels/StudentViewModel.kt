package com.example.bustrack_app.viewmodels
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.StudentModel

class StudentViewModel : ViewModel() {
    val studentList = MutableLiveData<List<StudentModel>>()

    fun loadStudents() {
        // Dummy data as per your design
        val list = listOf(
            StudentModel("#SR-9921", "Elena Rodriguez", "Grade 11", "North District", null, null, "UNASSIGNED", 0),
            StudentModel("#SR-8840", "Marcus Thompson", "Grade 9", "Downtown", "ROUTE 42-B", "Bus #102", "ASSIGNED", 0)
        )
        studentList.value = list
    }

    fun filterByRoute(route: String) {
        // Yahan filter logic ayegi
    }
}