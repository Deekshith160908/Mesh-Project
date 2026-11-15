package com.emergency.offgridmesh.mesh

import android.util.Log
import com.emergency.offgridmesh.data.model.MeshPacket
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.nio.charset.StandardCharsets

object PacketSerializer {

    private const val TAG = "PacketSerializer"

    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    fun toBytes(packet: MeshPacket): ByteArray {
        return try {
            val jsonString = jsonConfig.encodeToString(packet)
            jsonString.toByteArray(StandardCharsets.UTF_8)
        } catch (e: Exception) {
            Log.e(TAG, "Kotlinx JSON encoding failed, using JSONObject fallback: ${e.message}")
            toFallbackJson(packet).toByteArray(StandardCharsets.UTF_8)
        }
    }

    fun fromBytes(bytes: ByteArray): MeshPacket? {
        if (bytes.isEmpty()) return null
        val jsonString = String(bytes, StandardCharsets.UTF_8)
        return try {
            jsonConfig.decodeFromString<MeshPacket>(jsonString)
        } catch (e: Exception) {
            Log.e(TAG, "Kotlinx JSON decoding failed, attempting JSONObject fallback: ${e.message}")
            fromFallbackJson(jsonString)
        }
    }

    private fun toFallbackJson(packet: MeshPacket): String {
        val obj = JSONObject()
        obj.put("packetId", packet.packetId)
        obj.put("senderId", packet.senderId)
        obj.put("packetType", packet.packetType)
        obj.put("payload", packet.payload)
        if (packet.latitude != null) obj.put("latitude", packet.latitude) else obj.put("latitude", JSONObject.NULL)
        if (packet.longitude != null) obj.put("longitude", packet.longitude) else obj.put("longitude", JSONObject.NULL)
        obj.put("timestamp", packet.timestamp)
        obj.put("ttl", packet.ttl)
        obj.put("hopPath", packet.hopPath)
        obj.put("isSyncedToCloud", packet.isSyncedToCloud)
        return obj.toString()
    }

    private fun fromFallbackJson(jsonStr: String): MeshPacket? {
        return try {
            val obj = JSONObject(jsonStr)
            val lat = if (obj.isNull("latitude")) null else obj.optDouble("latitude")
            val lon = if (obj.isNull("longitude")) null else obj.optDouble("longitude")
            MeshPacket(
                packetId = obj.getString("packetId"),
                senderId = obj.getString("senderId"),
                packetType = obj.getString("packetType"),
                payload = obj.optString("payload", ""),
                latitude = lat,
                longitude = lon,
                timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                ttl = obj.optInt("ttl", 5),
                hopPath = obj.optString("hopPath", ""),
                isSyncedToCloud = obj.optBoolean("isSyncedToCloud", false)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Fallback JSON parsing also failed: ${e.message}")
            null
        }
    }
}
