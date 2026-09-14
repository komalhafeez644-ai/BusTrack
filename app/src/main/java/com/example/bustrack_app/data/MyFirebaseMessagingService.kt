package com.example.bustrack_app.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.bustrack_app.R
import com.example.bustrack_app.SplashActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.auth.ktx.auth

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        updateTokenInFirestore(token)
    }

    private fun updateTokenInFirestore(token: String) {
        val uid = Firebase.auth.currentUser?.uid
        if (uid != null) {
            val db = Firebase.firestore
            // 1. Update in users collection
            db.collection("users").document(uid)
                .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())

            // 2. Also check and update drivers collection if this user is a driver
            db.collection("drivers").whereEqualTo("uid", uid).get()
                .addOnSuccessListener { snapshot ->
                    snapshot.documents.forEach { doc ->
                        db.collection("drivers").document(doc.id)
                            .set(mapOf("fcmToken" to token), com.google.firebase.firestore.SetOptions.merge())
                    }
                }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val title = remoteMessage.notification?.title ?: remoteMessage.data["title"] ?: "BusTrack Update"
        val message = remoteMessage.notification?.body ?: remoteMessage.data["message"] ?: ""
        val type = remoteMessage.data["type"] ?: "GENERAL"
        val relatedId = remoteMessage.data["relatedId"] ?: ""
        val notificationId = remoteMessage.data["notificationId"] ?: ""
        val recipientRole = remoteMessage.data["recipientRole"] ?: ""

        sendNotification(title, message, type, relatedId, notificationId, recipientRole)
    }

    private fun sendNotification(
        title: String,
        messageBody: String,
        type: String,
        relatedId: String,
        notificationId: String,
        recipientRole: String
    ) {
        val intent = Intent(this, SplashActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            putExtra("NOTIFICATION_ID", notificationId)
            putExtra("NOTIFICATION_TYPE", type)
            putExtra("RELATED_ID", relatedId)
            putExtra("RECIPIENT_ROLE", recipientRole)
        }

        val notifIntId = if (notificationId.isNotBlank()) notificationId.hashCode() else System.currentTimeMillis().toInt()
        val pendingIntent = PendingIntent.getActivity(
            this,
            notifIntId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = "bus_track_notifications"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "BusTrack Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time notifications for bus tracking, duty status, trip updates, and student attendance"
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(messageBody))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        notificationManager.notify(notifIntId, notificationBuilder.build())
    }
}
