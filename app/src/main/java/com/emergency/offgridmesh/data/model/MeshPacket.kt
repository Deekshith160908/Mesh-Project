package com.emergency.offgridmesh.data.model

import kotlinx.serialization.Serializable

@Serializable
data class MeshPacket(
    val packetId: String,
    val senderId: String,
    val packetType: String,
    val payload: String,
    val latitude: Double?,
    val longitude: Double?,
    val timestamp: Long,
    val ttl: Int = 5,
    val hopPath: String,
    val isSyncedToCloud: Boolean = false
) {
    companion object {
        const val TYPE_SOS_ALERT = "SOS_ALERT"
        const val TYPE_TEXT_MESSAGE = "TEXT_MESSAGE"
        const val TYPE_PING = "PING"
        const val TYPE_ACK = "ACK"
        const val DEFAULT_TTL = 5
    }

    fun isExpired(): Boolean = ttl <= 0

    fun containsHop(nodeId: String): Boolean {
        if (hopPath.isBlank()) return false
        val nodes = hopPath.split(",").map { it.trim() }
        return nodes.contains(nodeId.trim())
    }

    fun getHopCount(): Int {
        if (hopPath.isBlank()) return 0
        return hopPath.split(",").size
    }

    fun createRelayPacket(relayNodeId: String): MeshPacket {
        val updatedHopPath = if (hopPath.isBlank()) {
            relayNodeId
        } else {
            "$hopPath,$relayNodeId"
        }
        return copy(
            ttl = ttl - 1,
            hopPath = updatedHopPath
        )
    }
}
