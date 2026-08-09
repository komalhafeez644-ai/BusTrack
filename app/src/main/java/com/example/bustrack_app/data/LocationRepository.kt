package com.example.bustrack_app.data

import android.util.Log
import com.example.bustrack_app.models.LocationModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object LocationRepository {

    private val db = FirebaseFirestore.getInstance()
    private const val COLLECTION_NAME = "locations"

    suspend fun getLocations(): List<LocationModel> {
        return try {
            val snapshot = db.collection(COLLECTION_NAME).get().await()
            snapshot.toObjects(LocationModel::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun searchLocations(query: String): List<LocationModel> {
        return try {
            // Basic prefix search
            val snapshot = db.collection(COLLECTION_NAME)
                .whereGreaterThanOrEqualTo("name", query)
                .whereLessThanOrEqualTo("name", query + "\uf8ff")
                .get().await()
            snapshot.toObjects(LocationModel::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun seedInitialLocations() {
        val famousAreas = listOf(
            LocationModel("1", "Morgah", 33.55460, 73.08935),
            LocationModel("2", "Army Housing Morgah", 33.53862, 73.08212),
            LocationModel("3", "DHA Phase 1", 33.556, 73.075),
            LocationModel("4", "Sector B1", 33.559, 73.076),
            LocationModel("5", "Askari 14", 33.548, 73.065),
            LocationModel("6", "Gulrez Housing", 33.565, 73.075),
            LocationModel("7", "Gulistan Colony", 33.57071, 73.09205),
            LocationModel("8", "Chaklala", 33.60821, 73.09068),
            LocationModel("9", "Chaklala Scheme 2", 33.56711, 73.10803),
            LocationModel("10", "Chaklala Scheme 3", 33.58786, 73.08724),
            LocationModel("11", "Jinnah Park", 33.570, 73.102),
            LocationModel("12", "Rawalpindi Cantonment", 33.590, 73.055),
            LocationModel("13", "Saddar", 33.59541, 73.05125),
            LocationModel("14", "Westridge", 33.61013, 73.02195),
            LocationModel("15", "Westridge 1", 33.61120, 73.01539),
            LocationModel("16", "Westridge 2", 33.61757, 73.02769),
            LocationModel("17", "Westridge 3", 33.61941, 73.02003),
            LocationModel("18", "Pirwadhai", 33.63219, 73.03938),
            LocationModel("19", "Dhok Hassu", 33.62660, 73.03440),
            LocationModel("20", "Raja Bazaar", 33.61231, 73.05634),
            LocationModel("21", "Committee Chowk", 33.61230, 73.06620),
            LocationModel("22", "Satellite Town", 33.63840, 73.06969),
            LocationModel("23", "Shakrial", 33.63912, 73.09977),
            LocationModel("24", "Sohan Colony", 33.63300, 73.10810),
            LocationModel("25", "Muslim Town", 33.63123, 73.09294),
            LocationModel("26", "Khurram Colony", 33.626, 73.086),
            LocationModel("27", "Mohanpura", 33.615, 73.065),
            LocationModel("28", "Chakra", 33.58925, 72.97978),
            LocationModel("29", "Adiala", 33.45752, 72.99559),
            LocationModel("30", "Bahria Town", 33.497, 73.075),
            LocationModel("31", "Bahria Town Phase 8", 33.49574, 73.07953),
            LocationModel("32", "Rawat", 33.508, 73.175),
            LocationModel("33", "Commercial Market", 33.638, 73.067),
            LocationModel("34", "6th Road", 33.642, 73.074),
            LocationModel("35", "Chandni Chowk", 33.646, 73.073),
            LocationModel("36", "Waris Khan", 33.618, 73.065),
            LocationModel("37", "Airport Road", 33.600, 73.105),
            LocationModel("38", "Chakri", 33.59, 72.98),
            LocationModel("39", "Kahuta Road", 33.54, 73.12),
            LocationModel("40", "Peshawar Morr", 33.68453, 73.04756),
            LocationModel("41", "G-9/1 Islamabad", 33.688, 73.040),
            LocationModel("42", "G-9/2 Islamabad", 33.684, 73.033),
            LocationModel("43", "G-9/3 Islamabad", 33.680, 73.036),
            LocationModel("44", "G-9/4 Islamabad", 33.676, 73.042),
            LocationModel("45", "Karachi Company (G-9 Markaz)", 33.6811, 73.0232),
            LocationModel("46", "H-9 Islamabad", 33.67276, 73.04437),
            LocationModel("47", "G-10 Islamabad", 33.67772, 73.01870),
            LocationModel("48", "G-11 Islamabad", 33.66872, 72.99874),
            LocationModel("49", "Peshawar Morr Bus Stop", 33.68470, 73.04688),
            LocationModel("50", "Metro Bus Terminal Peshawar Mor", 33.68503, 73.04723),
            LocationModel("51", "Kashmir Highway Station", 33.68461, 73.04748),
            LocationModel("52", "Kashmir Highway Station Platform 2", 33.68467, 73.04760),
            LocationModel("53", "Faiz Ahmed Faiz Station", 33.67536, 73.05453),
            LocationModel("54", "Faiz Ahmed Faiz Station Opposite", 33.67542, 73.05467),
            LocationModel("55", "Metro Bus Terminal Faiz Ahmed Faiz", 33.67555, 73.05448),
            LocationModel("56", "Chamman Metro Station", 33.69174, 73.04348),
            LocationModel("57", "Metro Bus Terminal Chaman", 33.68981, 73.04304),
            LocationModel("58", "Chaman Station", 33.68981, 73.04304),
            LocationModel("59", "Chaman Stop", 33.69200, 73.04300),
            LocationModel("60", "Dhaman Sayedan Morr Bus Stop", 33.55639, 73.06039),
            LocationModel("61", "Askari 7 Bus Stop", 33.55639, 73.06039),
            LocationModel("62", "Ali Town Bus Stop", 33.55639, 73.06039),
            LocationModel("63", "Dhok Rajgaan Bus Stop", 33.55639, 73.06039),
            LocationModel("64", "Zia-ul-Haq Park Bus Stop", 33.55639, 73.06039),
            LocationModel("65", "Mubarik Lane, Adiala Road", 33.5563874, 73.0603909),
            LocationModel("66", "FG Post Graduate College for Women", 33.5977, 73.0478)
        )

        for (area in famousAreas) {
            db.collection(COLLECTION_NAME).document(area.id).set(area)
        }
        Log.d("LocationRepo", "Seeded ${famousAreas.size} locations into Firestore")
    }
}
