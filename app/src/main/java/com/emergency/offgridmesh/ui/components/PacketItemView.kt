package com.emergency.offgridmesh.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.NearMe
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergency.offgridmesh.data.model.MeshPacket
import com.emergency.offgridmesh.ui.theme.EmergencyRed
import com.emergency.offgridmesh.ui.theme.EmergencyRedDark
import com.emergency.offgridmesh.ui.theme.MeshBorderDark
import com.emergency.offgridmesh.ui.theme.MeshGreen
import com.emergency.offgridmesh.ui.theme.MeshSurfaceDark
import com.emergency.offgridmesh.ui.theme.MeshSurfaceVariantDark
import com.emergency.offgridmesh.ui.theme.TacticalAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PacketItemView(
    packet: MeshPacket,
    isLocalNode: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSos = packet.packetType == MeshPacket.TYPE_SOS_ALERT
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(packet.timestamp))

    val borderColor = if (isSos) EmergencyRed else MeshBorderDark
    val backgroundColor = if (isSos) EmergencyRedDark.copy(alpha = 0.25f) else MeshSurfaceDark

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isSos) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "SOS",
                        tint = EmergencyRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "EMERGENCY SOS",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmergencyRed,
                        fontWeight = FontWeight.Black
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.NearMe,
                        contentDescription = "Mesh Message",
                        tint = TacticalAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isLocalNode) "YOU" else packet.senderId,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (isLocalNode) MeshGreen else Color.LightGray,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = if (packet.isSyncedToCloud) Icons.Default.CloudDone else Icons.Default.CloudQueue,
                    contentDescription = null,
                    tint = if (packet.isSyncedToCloud) MeshGreen else Color.DarkGray,
                    modifier = Modifier.size(14.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = packet.payload,
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White,
            fontWeight = if (isSos) FontWeight.Bold else FontWeight.Normal
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Relay Hop Trail & TTL info
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val hopCount = packet.getHopCount()
            val hopText = if (hopCount <= 1) "Direct Hop" else "$hopCount Hops (${packet.hopPath.replace(",", " \u2192 ")})"

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MeshSurfaceVariantDark)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            ) {
                Text(
                    text = hopText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.LightGray,
                    fontSize = 9.sp
                )
            }

            Text(
                text = "TTL: ${packet.ttl}",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }

        // Location coordinates link if attached
        if (packet.latitude != null && packet.longitude != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(MeshSurfaceVariantDark)
                    .clickable {
                        val geoUri = Uri.parse("geo:${packet.latitude},${packet.longitude}?q=${packet.latitude},${packet.longitude}(Beacon+Location)")
                        val mapIntent = Intent(Intent.ACTION_VIEW, geoUri).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            context.startActivity(mapIntent)
                        } catch (_: Exception) {}
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = if (isSos) EmergencyRed else MeshGreen,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                val coordText = String.format(Locale.US, "GPS: %.5f, %.5f (Tap to View)", packet.latitude, packet.longitude)
                Text(
                    text = coordText,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp
                )
            }
        }
    }
}
