package com.example.bustrack_app.sync.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_queue")
data class SyncQueueEntity(
    @PrimaryKey
    val syncId: String,
    val actionType: String,
    val operationType: String, // "SET" or "UPDATE"
    val targetCollection: String,
    val targetDocumentId: String,
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val status: String = STATUS_PENDING,
    val isConflated: Boolean = false
) {
    companion object {
        const val ACTION_LOCATION_LATEST = "LOCATION_LATEST"
        const val ACTION_DUTY_STATUS = "DUTY_STATUS"
        const val ACTION_ATTENDANCE = "ATTENDANCE"
        const val ACTION_DRIVER_ALERT = "DRIVER_ALERT"
        const val ACTION_NOTIFICATION = "NOTIFICATION"

        const val STATUS_PENDING = "PENDING"
        const val STATUS_SYNCING = "SYNCING"
        const val STATUS_SYNCED = "SYNCED"
        const val STATUS_FAILED = "FAILED"

        const val OP_SET = "SET"
        const val OP_UPDATE = "UPDATE"
    }
}
