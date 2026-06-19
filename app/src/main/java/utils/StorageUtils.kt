package utils

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.ktx.storage
import com.google.firebase.ktx.Firebase
import java.util.UUID

/**
 * Utility to handle image uploads to Firebase Storage
 */
object StorageUtils {

    private val storage = Firebase.storage.reference

    /**
     * Uploads an image to a specific folder and returns the download URL
     */
    fun uploadImage(folder: String, uri: Uri, onResult: (String?) -> Unit) {
        val fileName = "${UUID.randomUUID()}.jpg"
        val ref = storage.child("$folder/$fileName")

        Log.d("StorageUtils", "Starting upload to: $folder/$fileName")

        ref.putFile(uri)
            .addOnSuccessListener { taskSnapshot ->
                Log.d("StorageUtils", "Upload successful, getting download URL...")
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    Log.d("StorageUtils", "URL obtained: $downloadUri")
                    onResult(downloadUri.toString())
                }.addOnFailureListener { e ->
                    Log.e("StorageUtils", "Failed to get download URL", e)
                    onResult(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("StorageUtils", "Upload failed: ${e.message}", e)
                onResult(null)
            }
    }
}