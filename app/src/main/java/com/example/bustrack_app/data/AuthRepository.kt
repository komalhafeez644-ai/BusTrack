package com.example.bustrack_app.data

import android.util.Log
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthRepository {

    private val auth = Firebase.auth
    private val db = Firebase.firestore

    /**
     * Tries to login and returns Role (String) or Error Message (String).
     * Strictly uses signInWithEmailAndPassword for all users.
     */
    suspend fun login(email: String, password: String): String? {
        val cleanEmail = email.trim().lowercase()

        return try {
            // 1. Try to Login using ONLY signIn
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val uid = authResult.user?.uid

            if (uid != null) {
                var role: String? = null
                
                // Try to fetch role from Firestore, but don't crash if permissions are missing
                try {
                    val document = db.collection("users").document(uid).get().await()
                    role = document.getString("role")
                } catch (e: Exception) {
                    Log.w("AuthRepo", "Firestore read failed: ${e.message}")
                }

                // Hardcoded fallback if Firestore failed or role is missing
                if (role == null) {
                    role = when (cleanEmail) {
                        "admin@gmail.com" -> "admin"
                        "principal@gmail.com" -> "principal"
                        else -> {
                            // Try to check driver collection if possible
                            try {
                                val driverQuery = db.collection("drivers").whereEqualTo("email", cleanEmail).get().await()
                                if (!driverQuery.isEmpty) "driver" else "parent"
                            } catch (e: Exception) { "parent" }
                        }
                    }

                    // Attempt to sync missing document safely
                    try {
                        val userData = mapOf("uid" to uid, "email" to cleanEmail, "role" to role)
                        db.collection("users").document(uid).set(userData, com.google.firebase.firestore.SetOptions.merge())
                    } catch (e: Exception) {
                        Log.e("AuthRepo", "Firestore sync failed: ${e.message}")
                    }
                }

                return if (role == "user") "parent" else role
            }
            "Authentication failed"
        } catch (e: Exception) {
            Log.e("AuthRepo", "Login Error: ${e.message}")
            e.localizedMessage ?: "Invalid email or password"
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