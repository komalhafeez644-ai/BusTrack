package com.example.bustrack_app.sync

import android.content.Context
import android.util.Log
import com.example.bustrack_app.sync.data.SyncDatabase
import com.example.bustrack_app.sync.data.SyncQueueDao
import com.example.bustrack_app.sync.data.SyncQueueEntity
import com.example.bustrack_app.sync.network.NetworkMonitor
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import java.lang.reflect.Type
import java.util.Date
import java.util.concurrent.atomic.AtomicBoolean

object SyncQueueManager {

    private const val TAG = "SyncQueueManager"
    private var appContext: Context? = null
    private var dao: SyncQueueDao? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val isSyncing = AtomicBoolean(false)

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Timestamp::class.java, TimestampTypeAdapter())
        .registerTypeAdapter(Date::class.java, DateTypeAdapter())
        .create()

    fun init(context: Context) {
        appContext = context.applicationContext
        val db = SyncDatabase.getDatabase(context.applicationContext)
        dao = db.syncQueueDao()
        Log.d(TAG, "SyncQueueManager initialized")
    }

    enum class SyncResult {
        SYNCED_ONLINE,
        QUEUED_OFFLINE,
        FAILED
    }

    /**
     * Enqueues a full document set operation with merge semantics.
     */
    fun enqueueSet(
        syncId: String,
        actionType: String,
        targetCollection: String,
        targetDocumentId: String,
        data: Map<String, Any?>,
        immediateSync: Boolean = true,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        enqueueSetWithResult(
            syncId, actionType, targetCollection, targetDocumentId, data, immediateSync
        ) { result ->
            onComplete?.invoke(result != SyncResult.FAILED)
        }
    }

    /**
     * Enqueues a full document set operation and reports exact SyncResult (online vs queued vs failed).
     */
    fun enqueueSetWithResult(
        syncId: String,
        actionType: String,
        targetCollection: String,
        targetDocumentId: String,
        data: Map<String, Any?>,
        immediateSync: Boolean = true,
        onComplete: ((SyncResult) -> Unit)? = null
    ) {
        scope.launch {
            try {
                val payload = serializeMap(data)
                val entity = SyncQueueEntity(
                    syncId = syncId,
                    actionType = actionType,
                    operationType = SyncQueueEntity.OP_SET,
                    targetCollection = targetCollection,
                    targetDocumentId = targetDocumentId,
                    payloadJson = payload,
                    createdAt = System.currentTimeMillis(),
                    status = SyncQueueEntity.STATUS_PENDING,
                    isConflated = false
                )

                dao?.insertOrReplace(entity)

                if (immediateSync && NetworkMonitor.isOnline) {
                    val success = trySyncSingleItem(entity, data)
                    if (success) {
                        dao?.deleteById(syncId)
                        withContext(Dispatchers.Main) { onComplete?.invoke(SyncResult.SYNCED_ONLINE) }
                        return@launch
                    }
                }

                // Successfully saved locally in Room, queued for sync
                withContext(Dispatchers.Main) { onComplete?.invoke(SyncResult.QUEUED_OFFLINE) }
            } catch (e: Exception) {
                Log.e(TAG, "Failed local save for $syncId: ${e.message}", e)
                withContext(Dispatchers.Main) { onComplete?.invoke(SyncResult.FAILED) }
            }
        }
    }

    /**
     * Enqueues a partial document update with conflation support.
     */
    fun enqueueUpdate(
        syncId: String,
        actionType: String,
        targetCollection: String,
        targetDocumentId: String,
        updates: Map<String, Any?>,
        isConflated: Boolean = false,
        immediateSync: Boolean = true,
        onComplete: ((Boolean) -> Unit)? = null
    ) {
        scope.launch {
            val payload = serializeMap(updates)
            val entity = SyncQueueEntity(
                syncId = syncId,
                actionType = actionType,
                operationType = SyncQueueEntity.OP_UPDATE,
                targetCollection = targetCollection,
                targetDocumentId = targetDocumentId,
                payloadJson = payload,
                createdAt = System.currentTimeMillis(),
                status = SyncQueueEntity.STATUS_PENDING,
                isConflated = isConflated
            )

            dao?.insertOrReplace(entity)

            if (immediateSync && NetworkMonitor.isOnline) {
                val success = trySyncSingleItem(entity, updates)
                if (success) {
                    dao?.deleteById(syncId)
                    withContext(Dispatchers.Main) { onComplete?.invoke(true) }
                    return@launch
                }
            }

            withContext(Dispatchers.Main) { onComplete?.invoke(true) }
        }
    }

    private suspend fun trySyncSingleItem(entity: SyncQueueEntity, rawMap: Map<String, Any?>?): Boolean {
        return try {
            val firestore = Firebase.firestore
            val mapToWrite = rawMap ?: deserializeMap(entity.payloadJson)
            val sanitized = sanitizeMapForFirestore(mapToWrite)

            firestore.collection(entity.targetCollection)
                .document(entity.targetDocumentId)
                .set(sanitized, SetOptions.merge())
                .await()

            Log.d(TAG, "Successfully synced item [${entity.syncId}] (${entity.actionType}) to ${entity.targetCollection}/${entity.targetDocumentId}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Direct sync failed for [${entity.syncId}]: ${e.message}")
            false
        }
    }

    /**
     * Drains the pending queue and synchronizes records with Firestore.
     */
    fun processQueue() {
        if (!isSyncing.compareAndSet(false, true)) {
            Log.d(TAG, "Sync already in progress. Skipping duplicate run.")
            return
        }

        scope.launch {
            try {
                val currentDao = dao ?: return@launch
                val pending = currentDao.getPendingItems()
                if (pending.isEmpty()) {
                    Log.d(TAG, "Sync queue is empty.")
                    return@launch
                }

                Log.d(TAG, "Processing ${pending.size} pending sync queue items...")
                val firestore = Firebase.firestore

                for (item in pending) {
                    try {
                        val map = deserializeMap(item.payloadJson)
                        val sanitized = sanitizeMapForFirestore(map)

                        firestore.collection(item.targetCollection)
                            .document(item.targetDocumentId)
                            .set(sanitized, SetOptions.merge())
                            .await()

                        currentDao.deleteById(item.syncId)
                        Log.d(TAG, "Synced and removed queue item: ${item.syncId}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed syncing item ${item.syncId}: ${e.message}")
                        currentDao.update(
                            item.copy(
                                retryCount = item.retryCount + 1,
                                status = if (item.retryCount >= 5) SyncQueueEntity.STATUS_FAILED else SyncQueueEntity.STATUS_PENDING
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in processQueue: ${e.message}", e)
            } finally {
                isSyncing.set(false)
            }
        }
    }

    private fun serializeMap(map: Map<String, Any?>): String {
        return try {
            val jsonMap = mutableMapOf<String, Any?>()
            map.forEach { (k, v) ->
                when (v) {
                    is Timestamp -> {
                        jsonMap[k] = mapOf(
                            "__type" to "Timestamp",
                            "seconds" to v.seconds,
                            "nanoseconds" to v.nanoseconds
                        )
                    }
                    is Date -> {
                        jsonMap[k] = mapOf(
                            "__type" to "Date",
                            "time" to v.time
                        )
                    }
                    else -> jsonMap[k] = v
                }
            }
            gson.toJson(jsonMap)
        } catch (e: Exception) {
            Log.e(TAG, "Error serializing payload map: ${e.message}", e)
            "{}"
        }
    }

    private fun deserializeMap(json: String): Map<String, Any?> {
        return try {
            val type = object : TypeToken<Map<String, Any?>>() {}.type
            val rawMap: Map<String, Any?> = gson.fromJson(json, type) ?: emptyMap()
            rawMap
        } catch (e: Exception) {
            Log.e(TAG, "Error deserializing payload map: ${e.message}", e)
            emptyMap()
        }
    }

    private fun sanitizeMapForFirestore(map: Map<String, Any?>): Map<String, Any?> {
        val sanitized = mutableMapOf<String, Any?>()
        map.forEach { (k, v) ->
            if (v is Map<*, *> && v["__type"] == "Timestamp") {
                val seconds = (v["seconds"] as? Number)?.toLong() ?: 0L
                val nanoseconds = (v["nanoseconds"] as? Number)?.toInt() ?: 0
                sanitized[k] = Timestamp(seconds, nanoseconds)
            } else if (v is Map<*, *> && v["__type"] == "Date") {
                val time = (v["time"] as? Number)?.toLong() ?: 0L
                sanitized[k] = Date(time)
            } else {
                sanitized[k] = v
            }
        }
        return sanitized
    }

    private class TimestampTypeAdapter : JsonSerializer<Timestamp>, JsonDeserializer<Timestamp> {
        override fun serialize(src: Timestamp?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            val obj = JsonObject()
            if (src != null) {
                obj.addProperty("seconds", src.seconds)
                obj.addProperty("nanoseconds", src.nanoseconds)
            }
            return obj
        }

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Timestamp {
            val obj = json?.asJsonObject ?: return Timestamp.now()
            val seconds = obj.get("seconds")?.asLong ?: 0L
            val nanoseconds = obj.get("nanoseconds")?.asInt ?: 0
            return Timestamp(seconds, nanoseconds)
        }
    }

    private class DateTypeAdapter : JsonSerializer<Date>, JsonDeserializer<Date> {
        override fun serialize(src: Date?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
            val obj = JsonObject()
            if (src != null) {
                obj.addProperty("time", src.time)
            }
            return obj
        }

        override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Date {
            val obj = json?.asJsonObject ?: return Date()
            val time = obj.get("time")?.asLong ?: 0L
            return Date(time)
        }
    }
}
