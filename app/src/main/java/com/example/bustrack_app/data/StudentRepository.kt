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

    private val _studentList = MutableLiveData<List<StudentModel>>(emptyList())
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
        studentsCollection.whereEqualTo("id", studentId).get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                val refs = (snapshot.documents.map { it.reference } + studentsCollection.document(studentId))
                    .distinctBy { it.id }
                refs.forEach(batch::delete)
                batch.commit().addOnSuccessListener { onComplete(true) }
                    .addOnFailureListener { onComplete(false) }
            }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateStudent(updatedStudent: StudentModel, onComplete: (Boolean) -> Unit = {}) {
        studentsCollection.document(updatedStudent.id).set(updatedStudent)
            .addOnSuccessListener { onComplete(true) }
            .addOnFailureListener { onComplete(false) }
    }

    fun updateStudentCoordinates(studentDocumentId: String, latitude: Double, longitude: Double, onComplete: (Boolean) -> Unit = {}) {
        if (studentDocumentId.isBlank() || !latitude.isFinite() || !longitude.isFinite()) {
            onComplete(false)
            return
        }
        studentsCollection.document(studentDocumentId).update(
            "latitude", latitude,
            "longitude", longitude
        ).addOnCompleteListener { onComplete(it.isSuccessful) }
    }
    
    fun assignRouteToStudent(studentDocumentId: String, routeName: String, busNo: String, stop: String, onComplete: (Boolean) -> Unit = {}) {
        if (studentDocumentId.isBlank()) {
            onComplete(false)
            return
        }
        studentsCollection.document(studentDocumentId).update(
            "route", routeName,
            "busNo", busNo,
            "stopName", stop,
            "status", "ASSIGNED"
        ).addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    /** Legacy ApplicationModel fallback: resolve the document by its actual rollNumber field. */
    fun assignRouteToStudentByRollNumber(
        rollNumber: String,
        routeName: String,
        busNo: String,
        stop: String,
        onComplete: (Boolean) -> Unit = {}
    ) {
        val normalizedRollNumber = rollNumber.trim()
        if (normalizedRollNumber.isBlank()) {
            onComplete(false)
            return
        }
        studentsCollection.whereEqualTo("rollNumber", normalizedRollNumber).get()
            .addOnSuccessListener { snapshot ->
                // Don't guess when legacy data contains duplicate roll numbers.
                val student = snapshot.documents.singleOrNull()
                if (student == null) {
                    onComplete(false)
                    return@addOnSuccessListener
                }
                student.reference.update(
                    "route", routeName,
                    "busNo", busNo,
                    "stopName", stop,
                    "status", "ASSIGNED"
                ).addOnCompleteListener { onComplete(it.isSuccessful) }
            }
            .addOnFailureListener { onComplete(false) }
    }
}
