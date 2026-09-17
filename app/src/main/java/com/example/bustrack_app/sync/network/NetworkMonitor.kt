package com.example.bustrack_app.sync.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import com.example.bustrack_app.sync.SyncQueueManager

object NetworkMonitor {

    private const val TAG = "NetworkMonitor"
    private var isMonitoring = false
    @Volatile
    var isOnline: Boolean = false
        private set

    private val listeners = java.util.concurrent.CopyOnWriteArrayList<(Boolean) -> Unit>()
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    fun addListener(listener: (Boolean) -> Unit) {
        listeners.add(listener)
        mainHandler.post { listener(isOnline) }
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        listeners.remove(listener)
    }

    private fun notifyListeners(online: Boolean) {
        mainHandler.post {
            for (l in listeners) {
                try {
                    l(online)
                } catch (e: Exception) {
                    Log.e(TAG, "Error in network listener: ${e.message}", e)
                }
            }
        }
    }

    fun startMonitoring(context: Context) {
        if (isMonitoring) return
        val appContext = context.applicationContext
        val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return

        isOnline = checkInitialConnectivity(connectivityManager)
        Log.d(TAG, "Initial network status: isOnline = $isOnline")

        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)
                Log.d(TAG, "Network became available")
                val wasOnline = isOnline
                isOnline = true
                if (!wasOnline) {
                    notifyListeners(true)
                }
                SyncQueueManager.processQueue()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "Network lost")
                val wasOnline = isOnline
                isOnline = checkInitialConnectivity(connectivityManager)
                if (wasOnline != isOnline) {
                    notifyListeners(isOnline)
                }
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, capabilities)
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val wasOnline = isOnline
                if (hasInternet && !isOnline) {
                    isOnline = true
                    Log.d(TAG, "Validated internet connection available")
                    notifyListeners(true)
                    SyncQueueManager.processQueue()
                } else if (!hasInternet && isOnline) {
                    isOnline = false
                    notifyListeners(false)
                }
            }
        }

        try {
            val builder = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            connectivityManager.registerNetworkCallback(builder.build(), networkCallback)
            isMonitoring = true
        } catch (e: Exception) {
            Log.e(TAG, "Error registering network callback: ${e.message}", e)
        }
    }

    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        return checkInitialConnectivity(connectivityManager)
    }

    private fun checkInitialConnectivity(connectivityManager: ConnectivityManager): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
