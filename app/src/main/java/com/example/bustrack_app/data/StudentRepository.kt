package com.example.bustrack_app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.StudentModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object StudentRepository {
    private val _studentList = MutableLiveData<List<StudentModel>>()
    val studentList: LiveData<List<StudentModel>> get() = _studentList

    private const val PREFS_NAME = "student_prefs"
    private const val KEY_STUDENTS = "students_list"
    private var sharedPrefs: SharedPreferences? = null

    fun init(context: Context) {
        sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPrefs?.getString(KEY_STUDENTS, null)
        
        if (json != null) {
            val type = object : TypeToken<MutableList<StudentModel>>() {}.type
            val list: MutableList<StudentModel> = Gson().fromJson(json, type)
            _studentList.value = list
        } else {
            // Initial Dummy Data
            val initial = mutableListOf(
                StudentModel("#SR-9921", "Elena Rodriguez", "Grade 11", "North District", null, null, "UNASSIGNED", 0, "Marco Rodriguez", "+92 300 1111111", "07:30 AM", "Active"),
                StudentModel("#SR-8840", "Marcus Thompson", "Grade 9", "Downtown", "ROUTE 05", "BUS-102", "ASSIGNED", 0, "James Thompson", "+92 300 2222222", "07:45 AM", "Active"),
                StudentModel("#SR-2045", "Ali Hassan", "BS IT 7th Sem", "Sector 15 North", "ROUTE 12", "BUS-088", "ASSIGNED", 0, "Hassan Ahmed", "+92 300 3333333", "07:15 AM", "Active"),
                StudentModel("#SR-1011", "Zoya Khan", "Grade 10", "Blue Tower", null, null, "UNASSIGNED", 0, "Imran Khan", "+92 300 4444444", "08:00 AM", "Active")
            )
            _studentList.value = initial
            saveToPrefs()
        }
    }

    private fun saveToPrefs() {
        val json = Gson().toJson(_studentList.value)
        sharedPrefs?.edit()?.putString(KEY_STUDENTS, json)?.apply()
    }

    fun addStudent(student: StudentModel) {
        val current = _studentList.value?.toMutableList() ?: mutableListOf()
        current.add(0, student)
        _studentList.postValue(current)
        saveToPrefs()
    }

    fun deleteStudent(studentId: String) {
        val current = _studentList.value?.toMutableList() ?: mutableListOf()
        current.removeAll { it.id == studentId }
        _studentList.postValue(current)
        saveToPrefs()
    }

    fun updateStudent(updatedStudent: StudentModel) {
        val current = _studentList.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.id == updatedStudent.id || it.name == updatedStudent.name }
        if (index != -1) {
            current[index] = updatedStudent
            _studentList.postValue(current)
            saveToPrefs()
        } else {
            addStudent(updatedStudent)
        }
    }
    
    fun assignRouteToStudent(studentName: String, routeName: String, busNo: String, stop: String) {
        val current = _studentList.value?.toMutableList() ?: return
        val index = current.indexOfFirst { it.name.equals(studentName, true) }
        if (index != -1) {
            val student = current[index]
            val updated = student.copy(
                route = routeName,
                busNo = busNo,
                location = stop,
                status = "ASSIGNED"
            )
            current[index] = updated
            _studentList.postValue(current)
            saveToPrefs()
        }
    }
}
