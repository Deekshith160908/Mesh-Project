package com.emergency.offgridmesh.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PeerNodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertPeer(peer: PeerNodeEntity)

    @Query("SELECT * FROM peer_nodes WHERE isConnected = 1 ORDER BY lastSeenTimestamp DESC")
    fun getConnectedPeers(): Flow<List<PeerNodeEntity>>

    @Query("SELECT * FROM peer_nodes ORDER BY lastSeenTimestamp DESC")
    fun getAllPeers(): Flow<List<PeerNodeEntity>>

    @Query("UPDATE peer_nodes SET isConnected = 0 WHERE endpointId = :endpointId")
    suspend fun markDisconnected(endpointId: String)

    @Query("UPDATE peer_nodes SET isConnected = 0")
    suspend fun markAllDisconnected()

    @Query("DELETE FROM peer_nodes WHERE endpointId = :endpointId")
    suspend fun deletePeer(endpointId: String)
}
