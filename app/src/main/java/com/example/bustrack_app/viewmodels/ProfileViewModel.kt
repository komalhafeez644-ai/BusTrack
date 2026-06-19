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
        loadUserProfile()
    }

    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid).addSnapshotListener { snapshot, e ->
            if (snapshot != null && snapshot.exists()) {
                val admin = AdminModel(
                    id = snapshot.id,
                    fullName = snapshot.getString("fullName") ?: "",
                    email = snapshot.getString("email") ?: "",
                    department = snapshot.getString("department") ?: "",
                    employeeId = snapshot.getString("employeeId") ?: "",
                    phone = snapshot.getString("phone") ?: "",
                    address = snapshot.getString("address") ?: "",
                    profileImageUrl = snapshot.getString("profileImageUrl") ?: "",
                    campusName = "Punjab College",
                    isBusDelayNotifyEnabled = snapshot.getBoolean("busDelay") ?: true,
                    isEmergencyNotifyEnabled = snapshot.getBoolean("emergency") ?: true,
                    isDriverNotifyEnabled = snapshot.getBoolean("driverAlert") ?: false
                )
                _adminData.postValue(admin)
            }
        }
    }

    fun updateProfile(userData: Map<String, Any>, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("users").document(uid)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                onResult(true, "Profile updated successfully")
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "Update failed")
            }
    }
}