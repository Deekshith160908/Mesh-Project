package com.emergency.offgridmesh.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.emergency.offgridmesh.data.model.MeshPacket

@Entity(
    tableName = "mesh_packets",
    indices = [
        Index(value = ["packetType"]),
        Index(value = ["timestamp"]),
        Index(value = ["isSyncedToCloud"])
    ]
)
data class MeshPacketEntity(
    @PrimaryKey
    val packetId: String,
    val senderId: String,
    val packetType: String,
    val payload: String,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long,
    val ttl: Int,
    val hopPath: String,
    val isSyncedToCloud: Boolean
) {
    fun toDomain(): MeshPacket {
        return MeshPacket(
            packetId = packetId,
            senderId = senderId,
            packetType = packetType,
            payload = payload,
            latitude = latitude,
            longitude = longitude,
            timestamp = timestamp,
            ttl = ttl,
            hopPath = hopPath,
            isSyncedToCloud = isSyncedToCloud
        )
    }

    companion object {
        fun fromDomain(packet: MeshPacket): MeshPacketEntity {
            return MeshPacketEntity(
                packetId = packet.packetId,
                senderId = packet.senderId,
                packetType = packet.packetType,
                payload = packet.payload,
                latitude = packet.latitude,
                longitude = packet.longitude,
                timestamp = packet.timestamp,
                ttl = packet.ttl,
                hopPath = packet.hopPath,
                isSyncedToCloud = packet.isSyncedToCloud
            )
        }
    }
}
