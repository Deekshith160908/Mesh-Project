package com.emergency.offgridmesh

import com.emergency.offgridmesh.data.model.MeshPacket
import com.emergency.offgridmesh.mesh.PacketSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MeshPacketTest {

    @Test
    fun testPacketSerializationAndDeserialization() {
        val original = MeshPacket(
            packetId = UUID.randomUUID().toString(),
            senderId = "NODE-TEST-1",
            packetType = MeshPacket.TYPE_SOS_ALERT,
            payload = "EMERGENCY TRAPPED IN SHELTER",
            latitude = 37.774929,
            longitude = -122.419416,
            timestamp = System.currentTimeMillis(),
            ttl = 5,
            hopPath = "NODE-TEST-1",
            isSyncedToCloud = false
        )

        val bytes = PacketSerializer.toBytes(original)
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val deserialized = PacketSerializer.fromBytes(bytes)
        assertNotNull(deserialized)
        assertEquals(original.packetId, deserialized?.packetId)
        assertEquals(original.senderId, deserialized?.senderId)
        assertEquals(original.packetType, deserialized?.packetType)
        assertEquals(original.payload, deserialized?.payload)
        assertEquals(original.latitude, deserialized?.latitude)
        assertEquals(original.longitude, deserialized?.longitude)
        assertEquals(original.ttl, deserialized?.ttl)
        assertEquals(original.hopPath, deserialized?.hopPath)
    }

    @Test
    fun testLoopPreventionAndHopTracking() {
        val packet = MeshPacket(
            packetId = "packet-123",
            senderId = "NODE-A",
            packetType = MeshPacket.TYPE_TEXT_MESSAGE,
            payload = "Hello mesh",
            latitude = null,
            longitude = null,
            timestamp = 1000L,
            ttl = 5,
            hopPath = "NODE-A,NODE-B"
        )

        assertTrue(packet.containsHop("NODE-A"))
        assertTrue(packet.containsHop("NODE-B"))
        assertFalse(packet.containsHop("NODE-C"))
        assertEquals(2, packet.getHopCount())

        val relayed = packet.createRelayPacket("NODE-C")
        assertEquals(4, relayed.ttl)
        assertEquals("NODE-A,NODE-B,NODE-C", relayed.hopPath)
        assertTrue(relayed.containsHop("NODE-C"))
        assertEquals(3, relayed.getHopCount())
    }

    @Test
    fun testTtlExpiration() {
        val activePacket = MeshPacket(
            packetId = "packet-active",
            senderId = "NODE-1",
            packetType = MeshPacket.TYPE_SOS_ALERT,
            payload = "Active SOS",
            latitude = 0.0,
            longitude = 0.0,
            timestamp = 1000L,
            ttl = 1,
            hopPath = "NODE-1"
        )
        assertFalse(activePacket.isExpired())

        val expiredPacket = activePacket.copy(ttl = 0)
        assertTrue(expiredPacket.isExpired())
    }
}
