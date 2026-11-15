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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergency.offgridmesh.ui.components.EmergencyButton
import com.emergency.offgridmesh.ui.components.GpsCoordinatesCard
import com.emergency.offgridmesh.ui.components.MeshStatusBanner
import com.emergency.offgridmesh.ui.components.PacketItemView
import com.emergency.offgridmesh.ui.theme.EmergencyRed
import com.emergency.offgridmesh.ui.theme.EmergencyRedDark
import com.emergency.offgridmesh.ui.theme.MeshBackgroundDark
import com.emergency.offgridmesh.ui.theme.TacticalAmber
import com.emergency.offgridmesh.ui.viewmodel.MeshViewModel

@Composable
fun SosScreen(
    viewModel: MeshViewModel,
    modifier: Modifier = Modifier
) {
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()
    val locationData by viewModel.currentLocation.collectAsState()
    val sosCountdown by viewModel.sosCountdown.collectAsState()
    val isAlarmActive by viewModel.isAlarmActive.collectAsState()
    val sosAlerts by viewModel.sosAlerts.collectAsState()

    val distressPresets = listOf(
        "GENERAL DISTRESS",
        "MEDICAL CRITICAL",
        "TRAPPED IN RUBBLE",
        "FLOOD / WATER SURGE",
        "FIRE EVACUATION"
    )
    var selectedPreset by remember { mutableStateOf(distressPresets[0]) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MeshBackgroundDark)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Banner
        item {
            MeshStatusBanner(
                localNodeId = viewModel.localNodeId,
                connectedPeersCount = connectedPeers.size,
                isInternetAvailable = isInternetAvailable
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // GPS Coordinates
        item {
            GpsCoordinatesCard(
                locationData = locationData,
                onRefresh = { viewModel.refreshLocation() }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Distress Preset Selector
        item {
            Text(
                text = "EMERGENCY BEACON CATEGORY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(distressPresets) { preset ->
                    val isSelected = preset == selectedPreset
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedPreset = preset },
                        label = {
                            Text(
                                text = preset,
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmergencyRed,
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E222A),
                            labelColor = Color.LightGray
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }

        // The Big Emergency Button
        item {
            EmergencyButton(
                countdown = sosCountdown,
                onPress = {
                    viewModel.startSosCountdown(selectedPreset)
                },
                onCancel = {
                    viewModel.cancelSosCountdown()
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Silence Alarm Button (Visible when Alarm is active)
        if (isAlarmActive) {
            item {
                Button(
                    onClick = { viewModel.stopAlarm() },
                    colors = ButtonDefaults.buttonColors(containerColor = TacticalAmber),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SILENCE SIREN & STROBE",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Active Emergency SOS Beacons Feed
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = EmergencyRed,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "RECEIVED DISTRESS BEACONS (${sosAlerts.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (sosAlerts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No distress signals detected in local mesh range.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            items(sosAlerts) { packet ->
                PacketItemView(
                    packet = packet,
                    isLocalNode = packet.senderId == viewModel.localNodeId
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
