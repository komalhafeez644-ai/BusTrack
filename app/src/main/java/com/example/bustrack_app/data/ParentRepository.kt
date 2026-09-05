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

            // Sync name to basic user profile as well
            db.collection("users").document(uid)
                .update("fullName", parent.name)
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
                "status" to "PENDING",
                "trackingEnabled" to false,
                "trackingState" to "PENDING",
                "isSeenByAdmin" to false,
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

    fun listenToTrackingRequest(onResult: (TrackingRequestModel?) -> Unit) {
        val uid = auth.currentUser?.uid ?: run {
            Log.e("ParentRepository", "ERROR: No logged in user found!")
            return
        }
        
        Log.w("ParentRepository", ">>> STARTING LISTENER FOR UID: $uid <<<")

        db.collection("trackingRequests")
            .whereEqualTo("parentId", uid)
            .orderBy("submittedAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("ParentRepository", "!!! FIRESTORE QUERY FAILED !!!")
                    Log.e("ParentRepository", "Query: trackingRequests where parentId == $uid orderBy submittedAt DESC")
                    Log.e("ParentRepository", "Error Message: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val request = snapshot.documents.first().toObject(TrackingRequestModel::class.java)
                    Log.w("ParentRepository", "SUCCESS: Request Found. ID=${request?.requestId}, Status=${request?.status}")
                    onResult(request)
                } else {
                    Log.w("ParentRepository", "INFO: No requests found in database for this UID.")
                    onResult(null)
                }
            }
    }

    fun listenToAllTrackingRequests(onResult: (List<TrackingRequestModel>) -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        db.collection("trackingRequests")
            .whereEqualTo("parentId", uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onResult(emptyList())
                    return@addSnapshotListener
                }

                val requests = snapshot?.toObjects(TrackingRequestModel::class.java) ?: emptyList()
                onResult(requests)
            }
    }
}
