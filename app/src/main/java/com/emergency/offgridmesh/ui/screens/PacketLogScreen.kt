package com.emergency.offgridmesh.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergency.offgridmesh.ui.components.PacketItemView
import com.emergency.offgridmesh.ui.theme.EmergencyRed
import com.emergency.offgridmesh.ui.theme.MeshBackgroundDark
import com.emergency.offgridmesh.ui.theme.MeshGreen
import com.emergency.offgridmesh.ui.theme.TacticalAmber
import com.emergency.offgridmesh.ui.viewmodel.MeshViewModel

@Composable
fun PacketLogScreen(
    viewModel: MeshViewModel,
    modifier: Modifier = Modifier
) {
    val packets by viewModel.allPackets.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val syncedCount by viewModel.syncedPacketsCount.collectAsState()

    val unsyncedCount = packets.count { !it.isSyncedToCloud }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeshBackgroundDark)
            .padding(16.dp)
    ) {
        Text(
            text = "MULTI-HOP PACKET LOG",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Total Packets: ${packets.size} | Cloud Synced: $syncedCount | Pending Uplink: $unsyncedCount",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray,
            fontSize = 11.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Action Buttons Row
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { viewModel.triggerCloudSync() },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isInternetAvailable) MeshGreen else Color.DarkGray
                ),
                shape = RoundedCornerShape(8.dp),
                enabled = isInternetAvailable,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CloudSync,
                    contentDescription = null,
                    tint = if (isInternetAvailable) Color.Black else Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "SYNC UPLINK",
                    color = if (isInternetAvailable) Color.Black else Color.Gray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { viewModel.clearHistory() },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmergencyRed),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.DeleteSweep,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "CLEAR LOG", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (packets.isEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = "No packet transmissions recorded in database.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(packets, key = { it.packetId }) { packet ->
                    PacketItemView(
                        packet = packet,
                        isLocalNode = packet.senderId == viewModel.localNodeId
                    )
                }
            }
        }
    }
}
