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
    private var listeners: List<ListenerRegistration> = emptyList()

    private val _alerts = MutableLiveData<List<TransportAlert>>()
    val alerts: LiveData<List<TransportAlert>> get() = _alerts

    init {
        listenToRealAlerts()
    }

    private fun listenToRealAlerts() {
        val uid = Firebase.auth.currentUser?.uid ?: return
        // Reused by both Admin and Principal (Task 3/5) - resolve the actual signed-in
        // role instead of hardcoding "admin", so the exact same screen/ViewModel serves
        // both without a second implementation.
        viewModelScope.launch {
            val role = com.example.bustrack_app.data.AuthRepository().getCurrentUserRole()
            listeners = FirebaseRepository.listenToNotifications(uid, role) { notifications ->
                allAlerts = notifications.map { it.toTransportAlert() }
                _alerts.value = allAlerts
                
                // Track unseen count for badges (optional: could expose as LiveData)
                _unseenCount.value = notifications.count { !it.isRead }
            }
        }
    }

    private val _unseenCount = MutableLiveData<Int>()
    val unseenCount: LiveData<Int> get() = _unseenCount

    private fun com.example.bustrack_app.models.NotificationModel.toTransportAlert(): TransportAlert {
        val (tag, icon) = when (this.type) {
            "TRACKING_REQUEST" -> "IMPORTANT" to android.R.drawable.stat_sys_warning
            "ATTENDANCE" -> "CRITICAL" to android.R.drawable.stat_notify_error
            "BROADCAST" -> "GENERAL" to android.R.drawable.ic_menu_manage
            else -> "GENERAL" to android.R.drawable.ic_dialog_info
        }
        val subtitleWithTime = "${this.message}\n${FormUtils.timeAgo(this.timestamp)}"
        return TransportAlert(this.title, subtitleWithTime, tag, icon, this.id)
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
