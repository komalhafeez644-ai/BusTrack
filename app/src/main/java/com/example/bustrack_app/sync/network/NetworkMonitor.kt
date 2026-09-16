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
                isOnline = true
                SyncQueueManager.processQueue()
            }

            override fun onLost(network: Network) {
                super.onLost(network)
                Log.d(TAG, "Network lost")
                isOnline = checkInitialConnectivity(connectivityManager)
            }

            override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                super.onCapabilitiesChanged(network, capabilities)
                val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                if (hasInternet && !isOnline) {
                    isOnline = true
                    Log.d(TAG, "Validated internet connection available")
                    SyncQueueManager.processQueue()
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
