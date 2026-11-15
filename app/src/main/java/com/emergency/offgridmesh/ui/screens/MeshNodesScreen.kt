package com.emergency.offgridmesh.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CellTower
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergency.offgridmesh.data.model.PeerNode
import com.emergency.offgridmesh.ui.components.MeshStatusBanner
import com.emergency.offgridmesh.ui.theme.MeshBackgroundDark
import com.emergency.offgridmesh.ui.theme.MeshBorderDark
import com.emergency.offgridmesh.ui.theme.MeshGreen
import com.emergency.offgridmesh.ui.theme.MeshSurfaceDark
import com.emergency.offgridmesh.ui.theme.TacticalAmber
import com.emergency.offgridmesh.ui.viewmodel.MeshViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MeshNodesScreen(
    viewModel: MeshViewModel,
    modifier: Modifier = Modifier
) {
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()

    val infiniteTransition = rememberInfiniteTransition(label = "radar_sweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sweep_angle"
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MeshBackgroundDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            MeshStatusBanner(
                localNodeId = viewModel.localNodeId,
                connectedPeersCount = connectedPeers.size,
                isInternetAvailable = isInternetAvailable
            )
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Animated Radar Widget
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(200.dp)
                    .clip(CircleShape)
                    .background(MeshSurfaceDark)
                    .border(2.dp, MeshGreen.copy(alpha = 0.5f), CircleShape)
            ) {
                // Inner concentric radar rings
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .border(1.dp, MeshGreen.copy(alpha = 0.3f), CircleShape)
                )
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .border(1.dp, MeshGreen.copy(alpha = 0.3f), CircleShape)
                )

                // Rotating radar beam
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .rotate(sweepAngle)
                        .background(
                            Brush.sweepGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    MeshGreen.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                // Center node icon
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.Sensors,
                        contentDescription = "Radar",
                        tint = MeshGreen,
                        modifier = Modifier.size(32.dp)
                    )
                    Text(
                        text = "SCANNING",
                        style = MaterialTheme.typography.labelSmall,
                        color = MeshGreen,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // Neighbors List Header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "DISCOVERED DIRECT NEIGHBORS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${connectedPeers.size} ONLINE",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (connectedPeers.isNotEmpty()) MeshGreen else TacticalAmber,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (connectedPeers.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MeshSurfaceDark)
                        .border(1.dp, MeshBorderDark, RoundedCornerShape(8.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No direct peers connected yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Nearby devices with this app will automatically discover each other via Bluetooth LE & Wi-Fi Direct.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        } else {
            items(connectedPeers) { peer ->
                PeerNodeItem(peer = peer)
                Spacer(modifier = Modifier.height(8.dp))
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }

        // Mesh Multi-Hop Guide Card
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MeshSurfaceDark)
                    .border(1.dp, MeshBorderDark, RoundedCornerShape(10.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = TacticalAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "HOW MULTI-HOP EMERGENCY ROUTING WORKS",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Zero cellular or internet required. Nodes bridge directly via Bluetooth & Wi-Fi Direct.\n" +
                            "• Packets leap across devices up to 5 hops (TTL=5), dramatically expanding search-and-rescue coverage.\n" +
                            "• When any single device in the mesh catches cell/Wi-Fi signal, it opportunistically uploads all queued SOS beacons to the cloud gateway.",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
fun PeerNodeItem(peer: PeerNode) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(peer.lastSeenTimestamp))

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MeshSurfaceDark)
            .border(1.dp, MeshBorderDark, RoundedCornerShape(8.dp))
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.CellTower,
                contentDescription = null,
                tint = MeshGreen,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    text = peer.deviceName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Endpoint: ${peer.endpointId}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = Color.Gray,
                    fontSize = 10.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "LINKED",
                style = MaterialTheme.typography.labelSmall,
                color = MeshGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp
            )
            Text(
                text = formattedTime,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 10.sp
            )
        }
    }
}
