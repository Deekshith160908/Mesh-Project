package com.emergency.offgridmesh.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.emergency.offgridmesh.data.model.PeerNode

@Entity(tableName = "peer_nodes")
data class PeerNodeEntity(
    @PrimaryKey
    val endpointId: String,
    val deviceName: String,
    val isConnected: Boolean,
    val lastSeenTimestamp: Long,
    val directHop: Boolean,
    val rssi: Int
) {
    fun toDomain(): PeerNode {
        return PeerNode(
            endpointId = endpointId,
            deviceName = deviceName,
            isConnected = isConnected,
            lastSeenTimestamp = lastSeenTimestamp,
            directHop = directHop,
            rssi = rssi
        )
    }

    companion object {
        fun fromDomain(peer: PeerNode): PeerNodeEntity {
            return PeerNodeEntity(
                endpointId = peer.endpointId,
                deviceName = peer.deviceName,
                isConnected = peer.isConnected,
                lastSeenTimestamp = peer.lastSeenTimestamp,
                directHop = peer.directHop,
                rssi = peer.rssi
            )
        }
    }
}
