package com.example.bustrack_app.data

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<Unit> {
        val user = auth.currentUser ?: return Result.failure(Exception("User not logged in"))
        val email = user.email ?: return Result.failure(Exception("User email not found"))

        return try {
            val credential = EmailAuthProvider.getCredential(email, currentPassword)
            user.reauthenticate(credential).await()
            user.updatePassword(newPassword).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepo", "Change Password Error: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Tries to login and returns Role (String) or Error Message (String).
     * Strictly uses signInWithEmailAndPassword for all users.
     */
    suspend fun login(email: String, password: String): String? {
        val cleanEmail = email.trim().lowercase()

        return try {
            // 1. Try to Login
            val authResult = try {
                auth.signInWithEmailAndPassword(cleanEmail, password).await()
            } catch (e: Exception) {
                // Special handling for pre-created Principal account
                if (cleanEmail == "principal@gmail.com" && password == "principal123") {
                    try {
                        // Create the account if it doesn't exist
                        auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                    } catch (creationException: Exception) {
                        // If user already exists but password is wrong, throw original exception
                        throw e
                    }
                } else {
                    throw e
                }
            }

            val uid = authResult.user?.uid

            if (uid != null) {
                return getOrSyncRole(uid, cleanEmail)
            }
            "Authentication failed"
        } catch (e: Exception) {
            Log.e("AuthRepo", "Login Error: ${e.message}")
            e.localizedMessage ?: "Invalid email or password"
        }
    }

    suspend fun signInWithGoogle(idToken: String): String? {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                return getOrSyncRole(user.uid, user.email ?: "")
            }
            "Google Authentication failed"
        } catch (e: Exception) {
            Log.e("AuthRepo", "Google Login Error: ${e.message}")
            e.localizedMessage ?: "Google Sign-In failed"
        }
    }

    private suspend fun getOrSyncRole(uid: String, email: String): String {
        val cleanEmail = email.trim().lowercase()
        var role: String? = null
        var existingName: String? = null
        
        // 1. Try to fetch existing profile from Firestore
        try {
            val document = db.collection("users").document(uid).get().await()
            if (document.exists()) {
                role = document.getString("role")
                existingName = document.getString("fullName")
            }
        } catch (e: Exception) {
            Log.w("AuthRepo", "Firestore read failed: ${e.message}")
        }

        // 2. Fallback Role Discovery
        if (role == null) {
            role = when (cleanEmail) {
                "admin@gmail.com" -> "admin"
                "principal@gmail.com" -> "principal"
                else -> {
                    try {
                        val driverQuery = db.collection("drivers").whereEqualTo("email", cleanEmail).get().await()
                        if (!driverQuery.isEmpty) {
                            // Sync UID to drivers collection if missing
                            val driverDoc = driverQuery.documents.first()
                            if (driverDoc.getString("uid").isNullOrBlank()) {
                                db.collection("drivers").document(driverDoc.id).update("uid", uid)
                            }
                            "driver"
                        } else "parent"
                    } catch (e: Exception) { "parent" }
                }
            }
        } else if (role == "driver") {
            // Even if role is known, ensure drivers collection has the UID
            try {
                val driverQuery = db.collection("drivers").whereEqualTo("email", cleanEmail).get().await()
                if (!driverQuery.isEmpty) {
                    val driverDoc = driverQuery.documents.first()
                    if (driverDoc.getString("uid").isNullOrBlank()) {
                        db.collection("drivers").document(driverDoc.id).update("uid", uid)
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthRepo", "Failed to sync driver UID: ${e.message}")
            }
        }

        // 3. Sync/Initialize Profile
        // We only write if the document was missing or role was unknown
        // We CRITICALLY only set fullName if it doesn't already exist to protect form-entered data
        try {
            val userData = mutableMapOf<String, Any>()
            var needsSync = false

            if (existingName == null) {
                userData["uid"] = uid
                userData["email"] = cleanEmail
                userData["role"] = role ?: "parent"
                userData["fullName"] = auth.currentUser?.displayName ?: "User Name"
                
                if (role == "admin") userData["employeeId"] = "ADM-2024-001"
                if (role == "principal") userData["employeeId"] = "PRN-2024-001"
                needsSync = true
            } else if (role != null) {
                // If document exists but we want to ensure role is set correctly (e.g. login sync)
                userData["role"] = role
                needsSync = true
            }

            if (needsSync) {
                db.collection("users").document(uid).set(userData, com.google.firebase.firestore.SetOptions.merge())
            }
        } catch (e: Exception) {
            Log.e("AuthRepo", "Firestore sync failed: ${e.message}")
        }

        val resolvedRole = if (role == "user" || role == null) "parent" else role
        syncFcmToken(uid, resolvedRole)
        return resolvedRole
    }

    /**
     * Synchronizes the FCM token with Firestore and registers role topic subscriptions.
     * Ensures token is saved to users/{uid} and drivers/{driverId} (if driver),
     * and subscribes the user exclusively to role_${role}.
     */
    fun syncFcmToken(uid: String? = null, role: String? = null) {
        val targetUid = uid ?: auth.currentUser?.uid ?: return
        FirebaseMessaging.getInstance().token
            .addOnSuccessListener { token ->
                if (!token.isNullOrBlank()) {
                    // 1. Save to users/{uid}
                    db.collection("users").document(targetUid)
                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                        .addOnSuccessListener {
                            Log.d("AuthRepo", "FCM token synced to users/$targetUid")
                        }
                        .addOnFailureListener { e ->
                            Log.w("AuthRepo", "Failed to update users/$targetUid token: ${e.message}")
                        }

                    // 2. If user is a driver, also update matching driver doc in drivers collection
                    val cleanRole = role?.lowercase() ?: "parent"
                    if (cleanRole == "driver") {
                        db.collection("drivers").whereEqualTo("uid", targetUid).get()
                            .addOnSuccessListener { snapshot ->
                                snapshot.documents.forEach { doc ->
                                    db.collection("drivers").document(doc.id)
                                        .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.w("AuthRepo", "Failed to update driver token: ${e.message}")
                            }
                    }

                    // 3. Subscribe exclusively to the user's role topic
                    val allRoles = listOf("admin", "principal", "driver", "parent")
                    allRoles.forEach { r ->
                        if (r == cleanRole) {
                            FirebaseMessaging.getInstance().subscribeToTopic("role_$r")
                                .addOnSuccessListener { Log.d("AuthRepo", "Subscribed to topic role_$r") }
                        } else {
                            FirebaseMessaging.getInstance().unsubscribeFromTopic("role_$r")
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.w("AuthRepo", "Failed to retrieve FCM token: ${e.message}")
            }
    }

    // REMOVED: createAdminAccount and createPrincipalAccount as they were using ghost UIDs

    suspend fun getCurrentUserRole(): String {
        val user = auth.currentUser ?: return "user"
        val email = user.email?.trim()?.lowercase()
        
        // Hardcoded safety for admin and principal email
        if (email == "admin@gmail.com") return "admin"
        if (email == "principal@gmail.com") return "principal"
        
        return try {
            val doc = db.collection("users").document(user.uid).get().await()
            val role = doc.getString("role") ?: "user"
            // If it's 'user', we treat it as 'parent' in our app logic
            if (role == "user") "parent" else role
        } catch (e: Exception) {
            "parent"
        }
    }
}