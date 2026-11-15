package com.emergency.offgridmesh.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MeshPacketDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPacket(packet: MeshPacketEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdatePacket(packet: MeshPacketEntity): Long

    @Query("SELECT COUNT(*) FROM mesh_packets WHERE packetId = :packetId")
    suspend fun hasPacket(packetId: String): Int

    @Query("SELECT * FROM mesh_packets WHERE packetId = :packetId LIMIT 1")
    suspend fun getPacketById(packetId: String): MeshPacketEntity?

    @Query("SELECT * FROM mesh_packets ORDER BY timestamp DESC")
    fun getAllPackets(): Flow<List<MeshPacketEntity>>

    @Query("SELECT * FROM mesh_packets WHERE packetType = 'SOS_ALERT' ORDER BY timestamp DESC")
    fun getSosAlerts(): Flow<List<MeshPacketEntity>>

    @Query("SELECT * FROM mesh_packets WHERE packetType = 'TEXT_MESSAGE' ORDER BY timestamp DESC")
    fun getTextMessages(): Flow<List<MeshPacketEntity>>

    @Query("SELECT * FROM mesh_packets WHERE isSyncedToCloud = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedPackets(): List<MeshPacketEntity>

    @Query("UPDATE mesh_packets SET isSyncedToCloud = 1 WHERE packetId IN (:packetIds)")
    suspend fun markPacketsSynced(packetIds: List<String>)

    @Query("DELETE FROM mesh_packets WHERE timestamp < :olderThanTimestamp AND packetType != 'SOS_ALERT'")
    suspend fun deleteOldNonSosPackets(olderThanTimestamp: Long): Int

    @Query("DELETE FROM mesh_packets")
    suspend fun clearAll()
}
