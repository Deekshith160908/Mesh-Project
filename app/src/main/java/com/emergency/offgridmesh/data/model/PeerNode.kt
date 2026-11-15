package com.emergency.offgridmesh.data.model

import kotlinx.serialization.Serializable

@Serializable
data class PeerNode(
    val endpointId: String,
    val deviceName: String,
    val isConnected: Boolean,
    val lastSeenTimestamp: Long,
    val directHop: Boolean = true,
    val rssi: Int = 0
)
