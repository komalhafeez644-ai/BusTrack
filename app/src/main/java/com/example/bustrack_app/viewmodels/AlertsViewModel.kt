package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bustrack_app.R
import com.example.bustrack_app.data.FirebaseRepository
import com.example.bustrack_app.models.TransportAlert
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import utils.FormUtils

/**
 * Admin's Transport Alerts / notifications inbox. Was 100% hardcoded mock data
 * (loadTransportAlerts() built a fixed list) - now a live Firestore feed of real
 * notifications addressed to this admin (by uid) or to the "admin" role broadcast.
 */
class AlertsViewModel : ViewModel() {

    private var allAlerts: List<TransportAlert> = emptyList()
    private var unreadNotificationIds: List<String> = emptyList()
    private var listeners: List<ListenerRegistration> = emptyList()

    private val _alerts = MutableLiveData<List<TransportAlert>>()
    val alerts: LiveData<List<TransportAlert>> get() = _alerts

    init {
        listenToRealAlerts()
    }

    private fun listenToRealAlerts() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        // Resolve the signed-in user's role for Admin and Principal views.
        // role instead of hardcoding "admin", so the exact same screen/ViewModel serves
        // both without a second implementation.
        viewModelScope.launch {
            val role = com.example.bustrack_app.data.AuthRepository().getCurrentUserRole()
            listeners = FirebaseRepository.listenToNotifications(uid, role) { notifications ->
                allAlerts = notifications.map { it.toTransportAlert() }
                _alerts.value = allAlerts
                unreadNotificationIds = notifications.filter { !it.isRead }.map { it.id }.filter { it.isNotBlank() }
                _unseenCount.postValue(unreadNotificationIds.size)
            }
        }
    }

    private val _unseenCount = MutableLiveData<Int>()
    val unseenCount: LiveData<Int> get() = _unseenCount

    fun markAllAsRead(onComplete: (Boolean) -> Unit) {
        FirebaseRepository.markAllNotificationsRead(unreadNotificationIds, onComplete)
    }

    private fun com.example.bustrack_app.models.NotificationModel.toTransportAlert(): TransportAlert {
        val (tag, icon) = when {
            this.type == com.example.bustrack_app.models.NotificationModel.TYPE_DRIVER_ALERT || this.alertType.isNotBlank() -> {
                when (this.alertType) {
                    "Accident", "Bus Breakdown", "Student Emergency" ->
                        "CRITICAL" to R.drawable.notification_active
                    "Road Block", "Heavy Traffic", "Fuel Issue", "Bad Weather", "Police Check", "Wrong Route" ->
                        "IMPORTANT" to R.drawable.notification_active
                    else ->
                        "GENERAL" to R.drawable.notification_active
                }
            }
            this.type == "TRACKING_REQUEST" -> "IMPORTANT" to android.R.drawable.stat_sys_warning
            this.type == "ATTENDANCE" || this.type == com.example.bustrack_app.models.NotificationModel.TYPE_EMERGENCY ->
                "CRITICAL" to android.R.drawable.stat_notify_error
            this.type == com.example.bustrack_app.models.NotificationModel.TYPE_IMPORTANT ->
                "IMPORTANT" to android.R.drawable.stat_sys_warning
            this.type == "BROADCAST" || this.type == com.example.bustrack_app.models.NotificationModel.TYPE_ADMIN_BROADCAST ->
                "GENERAL" to android.R.drawable.ic_menu_manage
            else -> "GENERAL" to android.R.drawable.ic_dialog_info
        }
        return TransportAlert(
            title = this.title,
            subtitle = this.message,
            type = tag,
            iconResId = icon,
            id = this.id,
            timeText = FormUtils.timeAgo(this.timestamp),
            notificationKind = this.type,
            relatedId = this.relatedId,
            parentPhone = this.parentPhone
        )
    }

    // Show all
    fun loadAll() {
        _alerts.value = allAlerts
    }

    // Filter by type (chips ke liye)
    fun filterByType(type: String) {
        _alerts.value = if (type == "ALL") {
            allAlerts
        } else {
            allAlerts.filter { it.type.equals(type, true) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        listeners.forEach { it.remove() }
    }
}
