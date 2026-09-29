package utils

import android.net.Uri
import android.content.Context
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
    fun uploadImage(
        folder: String,
        uri: Uri,
        immediateContext: Context? = null,
        onResult: (String?) -> Unit
    ) {
        Log.d("Cloudinary", "Starting upload to folder: $folder")

        val request = MediaManager.get().upload(uri)
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
                    val url = (resultData["secure_url"] as? String)?.takeIf { it.startsWith("https://") }
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

        // A foreground profile edit should not wait through the background queue's
        // retry backoff (2 minutes by default) before the user receives a result.
        if (immediateContext != null) request.startNow(immediateContext) else request.dispatch()
    }
}
