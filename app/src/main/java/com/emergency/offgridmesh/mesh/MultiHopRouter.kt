package com.emergency.offgridmesh.mesh

import android.content.Context
import android.util.Log
import com.emergency.offgridmesh.alert.EmergencyAlertManager
import com.emergency.offgridmesh.data.model.MeshEvent
import com.emergency.offgridmesh.data.model.MeshPacket
import com.emergency.offgridmesh.data.repository.MeshRepository
import com.emergency.offgridmesh.location.LocationProvider
import com.emergency.offgridmesh.util.DeviceIdProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.UUID

class MultiHopRouter(
    private val context: Context,
    private val repository: MeshRepository,
    private val locationProvider: LocationProvider,
    private val alertManager: EmergencyAlertManager
) {

    companion object {
        private const val TAG = "MultiHopRouter"
        private const val MAX_SEEN_CACHE_SIZE = 500
    }

    private val myDeviceId: String = DeviceIdProvider.getDeviceId(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    // In-memory LRU set for fast dedup before DB query
    private val processedPacketIds = Collections.synchronizedSet(
        object : LinkedHashMap<String, Boolean>(MAX_SEEN_CACHE_SIZE, 0.75f, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Boolean>?): Boolean {
                return size > MAX_SEEN_CACHE_SIZE
            }
        }.keySet()
    )

    private val _meshEvents = MutableSharedFlow<MeshEvent>(extraBufferCapacity = 64)
    val meshEvents: SharedFlow<MeshEvent> = _meshEvents.asSharedFlow()

    @Volatile
    var nearbyMeshManager: NearbyMeshManager? = null

    fun handleIncomingPayload(bytes: ByteArray, fromEndpointId: String) {
        scope.launch {
            val packet = PacketSerializer.fromBytes(bytes)
            if (packet == null) {
                Log.w(TAG, "Dropped packet: Unable to parse payload bytes.")
                return@launch
            }

            // 1. Deduplication check
            if (processedPacketIds.contains(packet.packetId) || repository.hasPacket(packet.packetId)) {
                Log.d(TAG, "Dropped duplicate packet: ${packet.packetId}")
                return@launch
            }
            processedPacketIds.add(packet.packetId)

            // 2. Loop prevention check
            if (packet.senderId == myDeviceId || packet.containsHop(myDeviceId)) {
                Log.d(TAG, "Dropped packet to prevent routing loop: ${packet.packetId} (Path: ${packet.hopPath})")
                return@launch
            }

            // 3. TTL Expiration check
            if (packet.isExpired()) {
                Log.d(TAG, "Dropped expired packet: ${packet.packetId} (TTL reached 0)")
                return@launch
            }

            Log.i(
                TAG,
                "Valid packet received: ID=${packet.packetId}, Type=${packet.packetType}, HopCount=${packet.getHopCount()}, Sender=${packet.senderId}"
            )

            // 4. Save to persistent local Room DB
            repository.savePacket(packet)

            // 5. Check if it's an emergency SOS alert
            if (packet.packetType == MeshPacket.TYPE_SOS_ALERT) {
                Log.w(TAG, "EMERGENCY SOS ALERT RECEIVED FROM ${packet.senderId}: ${packet.payload}")
                alertManager.triggerEmergencyAlert(enableSiren = true, enableTorchStrobe = true)
                _meshEvents.emit(MeshEvent.SosAlertTriggered(packet))
            }

            _meshEvents.emit(MeshEvent.PacketReceived(packet, fromEndpointId))

            // 6. Multi-Hop Forwarding / Flooding to other mesh nodes
            if (packet.ttl > 1) {
                val relayPacket = packet.createRelayPacket(myDeviceId)
                val relayBytes = PacketSerializer.toBytes(relayPacket)
                val relayedToCount = nearbyMeshManager?.broadcastBytes(relayBytes, excludeEndpointId = fromEndpointId) ?: 0
                Log.i(
                    TAG,
                    "Relayed packet ${relayPacket.packetId} (Remaining TTL=${relayPacket.ttl}) to $relayedToCount neighbor nodes."
                )
                _meshEvents.emit(MeshEvent.PacketRelayed(relayPacket, relayPacket.getHopCount()))
            } else {
                Log.d(TAG, "Packet reached final hop (TTL=1, not relayed further).")
            }
        }
    }

    suspend fun createAndBroadcastSos(customMessage: String): MeshPacket {
        val location = locationProvider.getCurrentLocation()
        val packet = MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = myDeviceId,
            packetType = MeshPacket.TYPE_SOS_ALERT,
            payload = customMessage.ifBlank { "EMERGENCY DISTRESS BEACON - NEED IMMEDIATE RESCUE" },
            latitude = location?.latitude,
            longitude = location?.longitude,
            timestamp = System.currentTimeMillis(),
            ttl = MeshPacket.DEFAULT_TTL,
            hopPath = myDeviceId,
            isSyncedToCloud = false
        )

        processedPacketIds.add(packet.packetId)
        repository.savePacket(packet)

        val bytes = PacketSerializer.toBytes(packet)
        nearbyMeshManager?.broadcastBytes(bytes)

        _meshEvents.emit(MeshEvent.SosAlertTriggered(packet))
        return packet
    }

    suspend fun createAndBroadcastTextMessage(messageText: String): MeshPacket {
        val location = locationProvider.lastKnownLocation ?: locationProvider.getCurrentLocation()
        val packet = MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = myDeviceId,
            packetType = MeshPacket.TYPE_TEXT_MESSAGE,
            payload = messageText,
            latitude = location?.latitude,
            longitude = location?.longitude,
            timestamp = System.currentTimeMillis(),
            ttl = MeshPacket.DEFAULT_TTL,
            hopPath = myDeviceId,
            isSyncedToCloud = false
        )

        processedPacketIds.add(packet.packetId)
        repository.savePacket(packet)

        val bytes = PacketSerializer.toBytes(packet)
        nearbyMeshManager?.broadcastBytes(bytes)

        return packet
    }
}
