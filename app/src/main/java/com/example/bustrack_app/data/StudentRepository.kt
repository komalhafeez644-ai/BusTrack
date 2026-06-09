package com.example.bustrack_app.data

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.bustrack_app.models.StudentModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObjects

object StudentRepository {
    private val db = FirebaseFirestore.getInstance()
    private val studentsCollection = db.collection("students")

    private val _studentList = MutableLiveData<List<StudentModel>>()
    val studentList: LiveData<List<StudentModel>> get() = _studentList

    init {
        fetchStudentsFromFirestore()
    }

    private fun fetchStudentsFromFirestore() {
        studentsCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("StudentRepository", "Listen failed.", error)
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val students = snapshot.toObjects<StudentModel>()
                _studentList.value = students
                Log.d("StudentRepository", "Fetched ${students.size} students from Firestore")
            }
        }
    }

    fun addStudent(student: StudentModel, onComplete: (Boolean) -> Unit = {}) {
        studentsCollection.document(student.id).set(student)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun deleteStudent(studentId: String, onComplete: (Boolean) -> Unit = {}) {
        studentsCollection.document(studentId).delete()
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateStudent(updatedStudent: StudentModel, onComplete: (Boolean) -> Unit = {}) {
        studentsCollection.document(updatedStudent.id).set(updatedStudent)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }
    
    fun assignRouteToStudent(studentId: String, routeName: String, busNo: String, stop: String) {
        studentsCollection.document(studentId).update(
            "route", routeName,
            "busNo", busNo,
            "location", stop,
            "status", "ASSIGNED"
        )
    }
}
