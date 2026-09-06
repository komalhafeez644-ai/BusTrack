package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.AdminModel
import com.example.bustrack_app.models.ParentModel
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ProfileViewModel : ViewModel() {

    private val _adminData = MutableLiveData<AdminModel>()
    val adminData: LiveData<AdminModel> get() = _adminData

    private val _parentData = MutableLiveData<ParentModel?>()
    val parentData: LiveData<ParentModel?> get() = _parentData

    private val _trackingRequests = MutableLiveData<List<com.example.bustrack_app.models.TrackingRequestModel>>()
    val trackingRequests: LiveData<List<com.example.bustrack_app.models.TrackingRequestModel>> get() = _trackingRequests

    private val db = Firebase.firestore
    private val auth = Firebase.auth
    private val parentRepository = com.example.bustrack_app.data.ParentRepository()

    init {
        loadUserProfile()
        loadParentData()
        observeTrackingRequests()
    }

    private fun observeTrackingRequests() {
        parentRepository.listenToAllTrackingRequests { requests ->
            _trackingRequests.postValue(requests)
        }
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
                    city = snapshot.getString("city") ?: "",
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

    fun loadParentData() {
        val uid = auth.currentUser?.uid ?: return
        db.collection("parents").document(uid).addSnapshotListener { snapshot, _ ->
            if (snapshot != null && snapshot.exists()) {
                _parentData.postValue(snapshot.toObject(ParentModel::class.java))
            }
        }
    }

    fun updateProfile(userData: Map<String, Any>, onResult: (Boolean, String) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        // Update basic user account
        db.collection("users").document(uid)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                // If this is a parent, also sync name to parents collection
                if (userData.containsKey("fullName")) {
                    val name = userData["fullName"] as String
                    db.collection("parents").document(uid)
                        .update("name", name)
                        .addOnCompleteListener {
                            onResult(true, "Profile updated successfully")
                        }
                } else {
                    onResult(true, "Profile updated successfully")
                }
            }
            .addOnFailureListener { e ->
                onResult(false, e.message ?: "Update failed")
            }
    }
}