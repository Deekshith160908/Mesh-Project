package com.emergency.offgridmesh.data.model

sealed class MeshEvent {
    data class PacketReceived(val packet: MeshPacket, val fromEndpointId: String) : MeshEvent()
    data class PacketRelayed(val packet: MeshPacket, val relayHopCount: Int) : MeshEvent()
    data class PeerConnected(val endpointId: String, val deviceName: String) : MeshEvent()
    data class PeerDisconnected(val endpointId: String) : MeshEvent()
    data class SosAlertTriggered(val packet: MeshPacket) : MeshEvent()
    data class CloudSyncCompleted(val syncedCount: Int) : MeshEvent()
    data class NetworkStatusChanged(val isOnline: Boolean, val activePeerCount: Int) : MeshEvent()
}
