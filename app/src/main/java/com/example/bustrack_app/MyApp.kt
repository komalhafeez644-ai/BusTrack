package com.example.bustrack_app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.cloudinary.android.MediaManager
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        OneSignal.initWithContext(this, "59d52980-a287-4a44-bc2e-7c34e565360c")
        FirebaseAuth.getInstance().addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid.isNullOrBlank()) {
                OneSignal.logout()
            } else {
                OneSignal.login(uid)
            }
        }

        createNotificationChannel()

        val config = HashMap<String, String>()
        config["cloud_name"] = "zhi36daa"
        config["api_key"] = "579148119496887"
        config["upload_preset"] = "BusTrack"

        MediaManager.init(this, config)

        com.example.bustrack_app.sync.SyncQueueManager.init(this)
        com.example.bustrack_app.sync.network.NetworkMonitor.startMonitoring(this)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "bus_track_notifications"
            val channelName = "BusTrack Notifications"
            val channelDescription = "Real-time notifications for bus tracking, duty status, trip updates, and student attendance"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = channelDescription
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
