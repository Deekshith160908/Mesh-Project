package com.emergency.offgridmesh.data.repository

import com.emergency.offgridmesh.data.local.AppDatabase
import com.emergency.offgridmesh.data.local.MeshPacketEntity
import com.emergency.offgridmesh.data.local.PeerNodeEntity
import com.emergency.offgridmesh.data.model.MeshPacket
import com.emergency.offgridmesh.data.model.PeerNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MeshRepository(private val database: AppDatabase) {

    private val packetDao = database.meshPacketDao()
    private val peerDao = database.peerNodeDao()

    val allPackets: Flow<List<MeshPacket>> = packetDao.getAllPackets().map { entities ->
        entities.map { it.toDomain() }
    }

    val sosAlerts: Flow<List<MeshPacket>> = packetDao.getSosAlerts().map { entities ->
        entities.map { it.toDomain() }
    }

    val textMessages: Flow<List<MeshPacket>> = packetDao.getTextMessages().map { entities ->
        entities.map { it.toDomain() }
    }

    val connectedPeers: Flow<List<PeerNode>> = peerDao.getConnectedPeers().map { entities ->
        entities.map { it.toDomain() }
    }

    val allPeers: Flow<List<PeerNode>> = peerDao.getAllPeers().map { entities ->
        entities.map { it.toDomain() }
    }

    suspend fun savePacket(packet: MeshPacket): Boolean {
        val entity = MeshPacketEntity.fromDomain(packet)
        val rowId = packetDao.insertPacket(entity)
        return rowId != -1L
    }

    suspend fun hasPacket(packetId: String): Boolean {
        return packetDao.hasPacket(packetId) > 0
    }

    suspend fun getPacket(packetId: String): MeshPacket? {
        return packetDao.getPacketById(packetId)?.toDomain()
    }

    suspend fun upsertPeer(peer: PeerNode) {
        peerDao.upsertPeer(PeerNodeEntity.fromDomain(peer))
    }

    suspend fun markPeerDisconnected(endpointId: String) {
        peerDao.markDisconnected(endpointId)
    }

    suspend fun markAllPeersDisconnected() {
        peerDao.markAllDisconnected()
    }

    suspend fun getUnsyncedPackets(): List<MeshPacket> {
        return packetDao.getUnsyncedPackets().map { it.toDomain() }
    }

    suspend fun markPacketsSynced(packetIds: List<String>) {
        packetDao.markPacketsSynced(packetIds)
    }

    suspend fun deleteOldPackets(thresholdMillis: Long): Int {
        return packetDao.deleteOldNonSosPackets(thresholdMillis)
    }

    suspend fun clearAllPackets() {
        packetDao.clearAll()
    }
}
