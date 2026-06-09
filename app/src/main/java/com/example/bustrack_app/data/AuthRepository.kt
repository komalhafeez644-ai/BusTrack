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
     */
    suspend fun login(email: String, password: String): String? {
        val cleanEmail = email.trim().lowercase()

        return try {
            // 1. Try to Login
            val authResult = auth.signInWithEmailAndPassword(cleanEmail, password).await()
            val uid = authResult.user?.uid

            if (uid != null) {
                val document = db.collection("users").document(uid).get().await()
                
                // Force role for specific emails (to prevent database mismatches)
                val role = if (cleanEmail == "admin@gmail.com") {
                    "admin"
                } else if (cleanEmail == "principal@gmail.com") {
                    "principal"
                } else {
                    document.getString("role") ?: "user"
                }

                // If user exists in Auth but not in Firestore OR if role is mismatched
                if (!document.exists() || document.getString("role") != role) {
                    val userData = mutableMapOf(
                        "uid" to uid,
                        "email" to cleanEmail,
                        "role" to role
                    )
                    // If it's a first time auto-create for admin/principal
                    if (!document.exists()) {
                        userData["fullName"] = if (role == "admin") "System Admin" else if (role == "principal") "Principal Office" else "User"
                        userData["employeeId"] = if (role == "admin") "ADMIN-2024-001" else "PR-CF-2024"
                        userData["department"] = if (role == "admin") "Transport" else "Administration"
                    }
                    db.collection("users").document(uid).set(userData, com.google.firebase.firestore.SetOptions.merge()).await()
                }
                return role
            }
            "Authentication failed"
        } catch (e: Exception) {
            Log.e("AuthRepo", "Login Error: ${e.message}")
            
            // 2. If user doesn't exist in Auth (First time), create it
            // This replaces the old 'pre-created' logic with a 'auto-create on first login'
            if ((cleanEmail == "admin@gmail.com" && password == "admin123") || 
                (cleanEmail == "principal@gmail.com" && password == "principal123")) {
                
                return try {
                    val result = auth.createUserWithEmailAndPassword(cleanEmail, password).await()
                    val newUid = result.user?.uid
                    if (newUid != null) {
                        val role = if (cleanEmail.contains("admin")) "admin" else "principal"
                        val userData = mapOf(
                            "uid" to newUid,
                            "fullName" to if (role == "admin") "System Admin" else "Principal Office",
                            "email" to cleanEmail,
                            "role" to role,
                            "employeeId" to if (role == "admin") "ADMIN-2024-001" else "PR-CF-2024",
                            "department" to if (role == "admin") "Transport" else "Administration",
                            "phone" to if (role == "admin") "+92 300 1234567" else "+92 311 9876543"
                        )
                        db.collection("users").document(newUid).set(userData).await()
                        role
                    } else "Registration failed"
                } catch (e2: Exception) {
                    // If creation fails (e.g. user already exists but password was wrong)
                    e2.localizedMessage ?: "Login failed"
                }
            }
            
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
            doc.getString("role") ?: "user"
        } catch (e: Exception) {
            "user"
        }
    }
}