package com.example.bustrack_app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.cloudinary.android.MediaManager

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        val config = HashMap<String, String>()
        config["cloud_name"] = "vomxnqzr"
        config["upload_preset"] = "bus_track_images"

        MediaManager.init(this, config)

        // Initialize Offline Sync Engine
        com.example.bustrack_app.sync.SyncQueueManager.init(this)
        com.example.bustrack_app.sync.network.NetworkMonitor.startMonitoring(this)
        com.example.bustrack_app.sync.worker.SyncRetryWorker.schedule(this)
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
