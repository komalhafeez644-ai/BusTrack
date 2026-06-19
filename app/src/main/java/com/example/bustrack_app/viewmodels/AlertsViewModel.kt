package com.example.bustrack_app.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.bustrack_app.models.TransportAlert

class AlertsViewModel : ViewModel() {

    private val allAlerts = mutableListOf<TransportAlert>()

    private val _alerts = MutableLiveData<List<TransportAlert>>()
    val alerts: LiveData<List<TransportAlert>> get() = _alerts

    init {
        loadTransportAlerts()
    }

    // ✅ Load data once (master list)
    private fun loadTransportAlerts() {

        allAlerts.clear()

        allAlerts.addAll(
            listOf(
                TransportAlert(
                    "Emergency Bus Breakdown",
                    "Bus-02 stopped near DHA Phase 2.\nEngine failure reported by driver.",
                    "CRITICAL",
                    android.R.drawable.stat_notify_error
                ),
                TransportAlert(
                    "Students Need Assignment",
                    "3 new enrollments in Sector G-11\nrequire bus seating assignment.",
                    "IMPORTANT",
                    android.R.drawable.stat_sys_warning
                ),
                TransportAlert(
                    "Route-04 Routine Maintenance",
                    "Bus-04 is sent for monthly oil change.\nBackup bus assigned successfully.",
                    "GENERAL",
                    android.R.drawable.ic_menu_manage
                ),
                TransportAlert(
                    "Bus Route-03 Delayed",
                    "Heavy traffic near Bahria Town.\nEstimated 15 mins late.",
                    "CRITICAL",
                    android.R.drawable.stat_notify_error
                )
            )
        )

        _alerts.value = allAlerts.toList()
    }

    // ✅ Show all
    fun loadAll() {
        _alerts.value = allAlerts.toList()
    }

    // ✅ Filter by type (chips ke liye)
    fun filterByType(type: String) {

        _alerts.value = if (type == "ALL") {
            allAlerts.toList()
        } else {
            allAlerts.filter {
                it.type.equals(type, true)
            }
        }
    }
}