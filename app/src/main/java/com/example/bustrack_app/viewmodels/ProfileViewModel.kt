package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.AdminModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileViewModel : ViewModel() {

    private val _adminData = MutableLiveData<AdminModel>()
    val adminData: LiveData<AdminModel> get() = _adminData

    private val db = Firebase.firestore
    private val auth = Firebase.auth

    init {
        loadAdminProfile()
    }

    fun loadAdminProfile() {
        val currentUserId = auth.currentUser?.uid ?: "test_admin_id"

        db.collection("admins").document(currentUserId).addSnapshotListener { snapshot, e ->
            if (snapshot != null && snapshot.exists()) {
                val admin = AdminModel(
                    fullName = snapshot.getString("fullName") ?: "John Admin",
                    email = snapshot.getString("email") ?: "admin@pjc.edu",
                    department = snapshot.getString("department") ?: "Logistics",
                    employeeId = snapshot.getString("employeeId") ?: "CF-ADM-24",
                    campusName = "Punjab College",
                    isBusDelayNotifyEnabled = snapshot.getBoolean("busDelay") ?: true,
                    isEmergencyNotifyEnabled = snapshot.getBoolean("emergency") ?: true,
                    isDriverNotifyEnabled = snapshot.getBoolean("driverAlert") ?: false
                )
                _adminData.postValue(admin)
            }
        }
    }

    fun updateBusDelayNotification(isEnabled: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: "test_admin_id"
        db.collection("admins").document(currentUserId).update("busDelay", isEnabled)
    }

    fun updateEmergencyNotification(isEnabled: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: "test_admin_id"
        db.collection("admins").document(currentUserId).update("emergency", isEnabled)
    }

    fun updateDriverNotification(isEnabled: Boolean) {
        val currentUserId = auth.currentUser?.uid ?: "test_admin_id"
        db.collection("admins").document(currentUserId).update("driverAlert", isEnabled)
    }
}