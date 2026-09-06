package utils

import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

/**
 * Utility to handle image uploads to Cloudinary (replacing Firebase Storage for better free-tier reliability)
 */
object StorageUtils {

    /**
     * Uploads an image to Cloudinary using the 'BusTrack' unsigned preset and returns the secure HTTPS URL
     */
    fun uploadImage(folder: String, uri: Uri, onResult: (String?) -> Unit) {
        Log.d("Cloudinary", "Starting upload to folder: $folder")

        MediaManager.get().upload(uri)
            .option("folder", folder)
            .unsigned("BusTrack")
            .callback(object : UploadCallback {
                override fun onStart(requestId: String) {
                    Log.d("Cloudinary", "Upload started: $requestId")
                }

                override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                    // Optional: track progress
                }

                override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                    val url = resultData["secure_url"] as? String
                    Log.d("Cloudinary", "Upload successful: $url")
                    onResult(url)
                }

                override fun onError(requestId: String, error: ErrorInfo) {
                    Log.e("Cloudinary", "Upload failed: ${error.description}")
                    onResult(null)
                }

                override fun onReschedule(requestId: String, error: ErrorInfo) {
                    Log.d("Cloudinary", "Upload rescheduled")
                }
            })
            .dispatch()
    }
}