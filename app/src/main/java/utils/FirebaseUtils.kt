package utils

import com.example.bustrack_app.models.*
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.toObject

/**
 * Helper methods to map Firestore documents to Kotlin objects easily.
 */
object FirebaseUtils {

    // --- STUDENT MAPPING ---
    fun DocumentSnapshot.toStudent(): StudentModel? {
        return this.toObject<StudentModel>()?.copy() // You can manually copy the ID if needed
    }

    // --- DRIVER MAPPING ---
    fun DocumentSnapshot.toDriver(): DriverModel? {
        return this.toObject<DriverModel>()
    }

    // --- BUS MAPPING ---
    fun DocumentSnapshot.toBus(): BusModel? {
        return this.toObject<BusModel>()
    }

    // --- ROUTE MAPPING ---
    fun DocumentSnapshot.toRoute(): RouteModel? {
        return this.toObject<RouteModel>()
    }

    // --- APPLICATION MAPPING ---
    fun DocumentSnapshot.toApplication(): ApplicationModel? {
        return this.toObject<ApplicationModel>()
    }

    /**
     * Usage Example:
     * val student = document.toStudent()
     * val bus = document.toBus()
     */
}