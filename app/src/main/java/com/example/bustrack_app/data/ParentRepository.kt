package com.example.bustrack_app.data

import android.util.Log
import com.example.bustrack_app.models.ParentModel
import com.example.bustrack_app.models.TrackingRequestModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class ParentRepository {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    suspend fun saveParentData(parent: ParentModel): Pair<Boolean, String?> {
        return try {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                return Pair(false, "User not logged in")
            }
            
            val parentData = hashMapOf(
                "parentId" to uid,
                "name" to parent.name,
                "cnic" to parent.cnic,
                "phone" to parent.phone,
                "relationship" to parent.relationship,
                "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("parents").document(uid)
                .set(parentData, SetOptions.merge())
                .await()
            Pair(true, null)
        } catch (e: Exception) {
            Log.e("ParentRepository", "Error saving parent data: ${e.message}", e)
            Pair(false, e.localizedMessage)
        }
    }

    suspend fun submitTrackingRequest(studentId: String, parentName: String, phone: String, relationship: String): Pair<Boolean, String?> {
        return try {
            val uid = auth.currentUser?.uid
            if (uid == null) {
                return Pair(false, "User not logged in")
            }
            
            val requestId = db.collection("trackingRequests").document().id
            
            val request = hashMapOf(
                "requestId" to requestId,
                "parentId" to uid,
                "studentId" to studentId,
                "status" to "pending",
                "submittedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                "parentName" to parentName,
                "phone" to phone,
                "relationship" to relationship
            )
            
            db.collection("trackingRequests").document(requestId)
                .set(request)
                .await()
            Pair(true, null)
        } catch (e: Exception) {
            Log.e("ParentRepository", "Error submitting request: ${e.message}", e)
            Pair(false, e.localizedMessage)
        }
    }

    suspend fun getParentData(parentId: String): ParentModel? {
        return try {
            db.collection("parents").document(parentId).get().await().toObject(ParentModel::class.java)
        } catch (e: Exception) {
            null
        }
    }
}
