package com.emergency.offgridmesh.mesh

import android.content.Context
import android.util.Log
import com.emergency.offgridmesh.data.model.PeerNode
import com.emergency.offgridmesh.util.DeviceIdProvider
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.AdvertisingOptions
import com.google.android.gms.nearby.connection.ConnectionInfo
import com.google.android.gms.nearby.connection.ConnectionLifecycleCallback
import com.google.android.gms.nearby.connection.ConnectionResolution
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes
import com.google.android.gms.nearby.connection.DiscoveredEndpointInfo
import com.google.android.gms.nearby.connection.DiscoveryOptions
import com.google.android.gms.nearby.connection.EndpointDiscoveryCallback
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import com.google.android.gms.nearby.connection.Strategy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class NearbyMeshManager(
    private val context: Context,
    private val onPayloadReceivedListener: (bytes: ByteArray, fromEndpointId: String) -> Unit,
    private val onPeerStatusChangedListener: (endpointId: String, name: String, isConnected: Boolean) -> Unit
) {

    companion object {
        private const val TAG = "NearbyMeshManager"
        const val MESH_SERVICE_ID = "com.emergency.offgridmesh.SERVICE"
        val MESH_STRATEGY: Strategy = Strategy.P2P_CLUSTER
    }

    private val connectionsClient: ConnectionsClient = Nearby.getConnectionsClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val connectedEndpoints = ConcurrentHashMap<String, String>() // endpointId -> name
    private val connectingEndpoints = ConcurrentHashMap.newKeySet<String>()

    private val _isAdvertising = MutableStateFlow(false)
    val isAdvertising: StateFlow<Boolean> = _isAdvertising.asStateFlow()

    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()

    private val _connectedPeers = MutableStateFlow<List<PeerNode>>(emptyList())
    val connectedPeers: StateFlow<List<PeerNode>> = _connectedPeers.asStateFlow()

    private val payloadCallback = object : PayloadCallback() {
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES) {
                val bytes = payload.asBytes()
                if (bytes != null) {
                    Log.d(TAG, "Payload received from $endpointId: ${bytes.size} bytes")
                    onPayloadReceivedListener(bytes, endpointId)
                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
            if (update.status == PayloadTransferUpdate.Status.SUCCESS) {
                Log.d(TAG, "Payload transfer to/from $endpointId completed successfully.")
            } else if (update.status == PayloadTransferUpdate.Status.FAILURE) {
                Log.w(TAG, "Payload transfer failure with endpoint: $endpointId")
            }
        }
    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, connectionInfo: ConnectionInfo) {
            val peerName = connectionInfo.endpointName
            Log.i(TAG, "Connection initiated from $endpointId ($peerName). Auto-accepting...")
            connectionsClient.acceptConnection(endpointId, payloadCallback)
                .addOnSuccessListener {
                    Log.d(TAG, "Successfully accepted connection with $endpointId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to accept connection with $endpointId: ${e.message}")
                    connectingEndpoints.remove(endpointId)
                }
        }

        override fun onConnectionResult(endpointId: String, resolution: ConnectionResolution) {
            connectingEndpoints.remove(endpointId)
            when (resolution.status.statusCode) {
                ConnectionsStatusCodes.STATUS_OK -> {
                    Log.i(TAG, "Connection ESTABLISHED with $endpointId")
                    val peerName = "Peer-${endpointId.take(4)}"
                    connectedEndpoints[endpointId] = peerName
                    updatePeersState()
                    onPeerStatusChangedListener(endpointId, peerName, true)
                }
                ConnectionsStatusCodes.STATUS_CONNECTION_REJECTED -> {
                    Log.w(TAG, "Connection REJECTED by $endpointId")
                }
                ConnectionsStatusCodes.STATUS_ERROR -> {
                    Log.e(TAG, "Connection ERROR with $endpointId")
                }
                else -> {
                    Log.w(TAG, "Connection result unknown code: ${resolution.status.statusCode}")
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            Log.i(TAG, "Disconnected from $endpointId")
            val peerName = connectedEndpoints.remove(endpointId) ?: "Unknown"
            connectingEndpoints.remove(endpointId)
            updatePeersState()
            onPeerStatusChangedListener(endpointId, peerName, false)
        }
    }

    private val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            val peerName = info.endpointName
            Log.i(TAG, "Discovered endpoint: $endpointId ($peerName)")

            if (!connectedEndpoints.containsKey(endpointId) && !connectingEndpoints.contains(endpointId)) {
                connectingEndpoints.add(endpointId)
                val localAlias = DeviceIdProvider.getNodeAlias(context)
                Log.d(TAG, "Requesting connection to $endpointId with alias $localAlias")
                connectionsClient.requestConnection(localAlias, endpointId, connectionLifecycleCallback)
                    .addOnSuccessListener {
                        Log.d(TAG, "Requested connection to $endpointId")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Failed to request connection to $endpointId: ${e.message}")
                        connectingEndpoints.remove(endpointId)
                    }
            }
        }

        override fun onEndpointLost(endpointId: String) {
            Log.d(TAG, "Lost discovered endpoint: $endpointId")
            connectingEndpoints.remove(endpointId)
        }
    }

    fun startMesh() {
        startAdvertising()
        startDiscovery()
    }

    fun stopMesh() {
        stopAdvertising()
        stopDiscovery()
        try {
            connectionsClient.stopAllEndpoints()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping all endpoints: ${e.message}")
        }
        connectedEndpoints.clear()
        connectingEndpoints.clear()
        updatePeersState()
    }

    private fun startAdvertising() {
        val localAlias = DeviceIdProvider.getNodeAlias(context)
        val advertisingOptions = AdvertisingOptions.Builder()
            .setStrategy(MESH_STRATEGY)
            .setLowPower(false)
            .build()

        connectionsClient.startAdvertising(
            localAlias,
            MESH_SERVICE_ID,
            connectionLifecycleCallback,
            advertisingOptions
        ).addOnSuccessListener {
            Log.i(TAG, "Started advertising as $localAlias")
            _isAdvertising.value = true
        }.addOnFailureListener { e ->
            Log.e(TAG, "Advertising start failed: ${e.message}")
            _isAdvertising.value = false
        }
    }

    private fun stopAdvertising() {
        try {
            connectionsClient.stopAdvertising()
            _isAdvertising.value = false
            Log.i(TAG, "Stopped advertising.")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping advertising: ${e.message}")
        }
    }

    private fun startDiscovery() {
        val discoveryOptions = DiscoveryOptions.Builder()
            .setStrategy(MESH_STRATEGY)
            .setLowPower(false)
            .build()

        connectionsClient.startDiscovery(
            MESH_SERVICE_ID,
            endpointDiscoveryCallback,
            discoveryOptions
        ).addOnSuccessListener {
            Log.i(TAG, "Started discovery on service: $MESH_SERVICE_ID")
            _isDiscovering.value = true
        }.addOnFailureListener { e ->
            Log.e(TAG, "Discovery start failed: ${e.message}")
            _isDiscovering.value = false
        }
    }

    private fun stopDiscovery() {
        try {
            connectionsClient.stopDiscovery()
            _isDiscovering.value = false
            Log.i(TAG, "Stopped discovery.")
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping discovery: ${e.message}")
        }
    }

    fun broadcastBytes(bytes: ByteArray, excludeEndpointId: String? = null): Int {
        val targetEndpoints = connectedEndpoints.keys.filter { it != excludeEndpointId }
        if (targetEndpoints.isEmpty()) {
            Log.d(TAG, "No connected endpoints to broadcast bytes.")
            return 0
        }

        val payload = Payload.fromBytes(bytes)
        try {
            connectionsClient.sendPayload(targetEndpoints, payload)
                .addOnSuccessListener {
                    Log.d(TAG, "Broadcast sent to ${targetEndpoints.size} endpoints.")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Broadcast failed: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during broadcast: ${e.message}")
        }
        return targetEndpoints.size
    }

    private fun updatePeersState() {
        val peersList = connectedEndpoints.map { (id, name) ->
            PeerNode(
                endpointId = id,
                deviceName = name,
                isConnected = true,
                lastSeenTimestamp = System.currentTimeMillis(),
                directHop = true
            )
        }
        _connectedPeers.value = peersList
    }

    fun getConnectedEndpointsCount(): Int = connectedEndpoints.size
}
