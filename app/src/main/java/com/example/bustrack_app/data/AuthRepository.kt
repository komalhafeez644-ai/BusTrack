package com.example.bustrack_app.data

import android.util.Log
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
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
        
        // Try to fetch role from Firestore
        try {
            val document = db.collection("users").document(uid).get().await()
            role = document.getString("role")
        } catch (e: Exception) {
            Log.w("AuthRepo", "Firestore read failed: ${e.message}")
        }

        // Fallback if role is missing
        if (role == null) {
            role = when (cleanEmail) {
                "admin@gmail.com" -> "admin"
                "principal@gmail.com" -> "principal"
                else -> {
                    try {
                        val driverQuery = db.collection("drivers").whereEqualTo("email", cleanEmail).get().await()
                        if (!driverQuery.isEmpty) "driver" else "parent"
                    } catch (e: Exception) { "parent" }
                }
            }

            // Sync missing document
            try {
                val userData = mutableMapOf<String, Any>(
                    "uid" to uid, 
                    "email" to cleanEmail, 
                    "role" to role,
                    "fullName" to (auth.currentUser?.displayName ?: "User Name")
                )
                
                // Set default Employee ID for pre-created accounts
                if (role == "admin") userData["employeeId"] = "ADM-2024-001"
                if (role == "principal") userData["employeeId"] = "PRN-2024-001"

                db.collection("users").document(uid).set(userData, com.google.firebase.firestore.SetOptions.merge())
            } catch (e: Exception) {
                Log.e("AuthRepo", "Firestore sync failed: ${e.message}")
            }
        }

        return if (role == "user") "parent" else role
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