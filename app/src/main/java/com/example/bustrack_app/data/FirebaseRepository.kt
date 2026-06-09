package com.example.bustrack_app.data

import com.example.bustrack_app.models.*
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.toObject

/**
 * Modern way to handle all database operations in one place.
 */
object FirebaseRepository {

    private val db = Firebase.firestore

    // --- STUDENTS ---
    fun fetchStudents(onResult: (List<StudentModel>) -> Unit) {
        db.collection("students").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<StudentModel>() } ?: emptyList()
            onResult(list)
        }
    }

    fun saveStudent(student: StudentModel, onComplete: (Boolean) -> Unit) {
        db.collection("students").document(student.id).set(student)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- DRIVERS ---
    fun fetchDrivers(onResult: (List<DriverModel>) -> Unit) {
        db.collection("drivers").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<DriverModel>() } ?: emptyList()
            onResult(list)
        }
    }

    fun saveDriver(driver: DriverModel, onComplete: (Boolean) -> Unit) {
        db.collection("drivers").document(driver.id).set(driver)
            .addOnCompleteListener { onComplete(it.isSuccessful) }
    }

    // --- BUSES ---
    fun fetchBuses(onResult: (List<BusModel>) -> Unit) {
        db.collection("buses").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<BusModel>() } ?: emptyList()
            onResult(list)
        }
    }

    // --- ROUTES ---
    fun fetchRoutes(onResult: (List<RouteModel>) -> Unit) {
        db.collection("routes").addSnapshotListener { snapshot, _ ->
            val list = snapshot?.documents?.mapNotNull { it.toObject<RouteModel>() } ?: emptyList()
            onResult(list)
        }
    }
}