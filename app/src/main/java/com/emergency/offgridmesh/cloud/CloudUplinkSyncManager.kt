package com.emergency.offgridmesh.cloud

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import com.emergency.offgridmesh.data.local.AppDatabase
import com.emergency.offgridmesh.mesh.PacketSerializer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class CloudUplinkSyncManager(
    private val context: Context,
    private val database: AppDatabase
) {

    companion object {
        private const val TAG = "CloudUplinkSyncManager"
        private const val GATEWAY_URL = "https://emergency-mesh-gateway.firebaseapp.com/api/v1/relay/packet"
        private const val TIMEOUT_MS = 10000
    }

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager

    private val scope = CoroutineScope(Dispatchers.IO)

    private val _isInternetAvailable = MutableStateFlow(false)
    val isInternetAvailable: StateFlow<Boolean> = _isInternetAvailable.asStateFlow()

    private val _lastSyncTimestamp = MutableStateFlow(0L)
    val lastSyncTimestamp: StateFlow<Long> = _lastSyncTimestamp.asStateFlow()

    private val _syncedPacketsCount = MutableStateFlow(0)
    val syncedPacketsCount: StateFlow<Int> = _syncedPacketsCount.asStateFlow()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            Log.i(TAG, "Network connection restored. Opportunistic cloud uplink ready.")
            _isInternetAvailable.value = true
            triggerOpportunisticSync()
        }

        override fun onLost(network: Network) {
            super.onLost(network)
            Log.i(TAG, "Network connection lost. Device operating in pure off-grid mesh mode.")
            _isInternetAvailable.value = false
        }
    }

    fun startMonitoring() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        try {
            connectivityManager?.registerNetworkCallback(request, networkCallback)
            checkInitialConnectivity()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register network callback: ${e.message}")
        }
    }

    fun stopMonitoring() {
        try {
            connectivityManager?.unregisterNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to unregister network callback: ${e.message}")
        }
    }

    private fun checkInitialConnectivity() {
        val cm = connectivityManager ?: return
        val activeNetwork = cm.activeNetwork ?: return
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return
        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        _isInternetAvailable.value = hasInternet
        if (hasInternet) {
            triggerOpportunisticSync()
        }
    }

    fun triggerOpportunisticSync() {
        scope.launch {
            syncUnsyncedPackets()
        }
    }

    suspend fun syncUnsyncedPackets(): Int = withContext(Dispatchers.IO) {
        if (!_isInternetAvailable.value) {
            Log.d(TAG, "Sync aborted: No internet uplink available.")
            return@withContext 0
        }

        val unsyncedEntities = database.meshPacketDao().getUnsyncedPackets()
        if (unsyncedEntities.isEmpty()) {
            Log.d(TAG, "No pending unsynced packets.")
            return@withContext 0
        }

        Log.i(TAG, "Attempting cloud upload of ${unsyncedEntities.size} packets...")
        val syncedIds = mutableListOf<String>()

        for (entity in unsyncedEntities) {
            val domainPacket = entity.toDomain()
            val success = uploadPacketToGateway(domainPacket)
            if (success) {
                syncedIds.add(entity.packetId)
            }
        }

        if (syncedIds.isNotEmpty()) {
            database.meshPacketDao().markPacketsSynced(syncedIds)
            _syncedPacketsCount.value += syncedIds.size
            _lastSyncTimestamp.value = System.currentTimeMillis()
            Log.i(TAG, "Successfully synced ${syncedIds.size} emergency packets to cloud gateway.")
        }

        return@withContext syncedIds.size
    }

    private fun uploadPacketToGateway(packet: com.emergency.offgridmesh.data.model.MeshPacket): Boolean {
        var connection: HttpURLConnection? = null
        return try {
            val jsonBytes = PacketSerializer.toBytes(packet)
            val jsonString = String(jsonBytes, Charsets.UTF_8)

            val url = URL(GATEWAY_URL)
            connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = TIMEOUT_MS
                readTimeout = TIMEOUT_MS
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Accept", "application/json")
            }

            OutputStreamWriter(connection.outputStream, Charsets.UTF_8).use { writer ->
                writer.write(jsonString)
                writer.flush()
            }

            val responseCode = connection.responseCode
            // HTTP 200 or 201 or 202 means gateway received packet successfully
            val isSuccess = responseCode in 200..299
            if (!isSuccess) {
                Log.w(TAG, "Gateway returned HTTP status: $responseCode for packet: ${packet.packetId}")
            }
            isSuccess
        } catch (e: Exception) {
            Log.w(TAG, "Cloud gateway upload simulated or network error: ${e.message}. Packet remains queued for retry.")
            // In off-grid disaster tests without a live server, we consider packets acknowledged if connection was attempted
            // but return false so they persist until valid 200 OK.
            false
        } finally {
            connection?.disconnect()
        }
    }
}
