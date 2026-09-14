package utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {

    const val NOTIFICATION_PERMISSION_REQUEST_CODE = 1001
    private const val PREFS_NAME = "notification_permission_prefs"
    private const val KEY_PERMISSION_ASKED = "has_asked_notification_permission"

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun requestNotificationPermission(activity: AppCompatActivity, forcePrompt: Boolean = false) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (hasNotificationPermission(activity)) return

            val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val alreadyAsked = prefs.getBoolean(KEY_PERMISSION_ASKED, false)

            // Prompt once unless forcePrompt is requested or rationale should be shown
            if (!alreadyAsked || forcePrompt || ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.POST_NOTIFICATIONS)) {
                prefs.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_REQUEST_CODE
                )
            }
        }
    }
}
