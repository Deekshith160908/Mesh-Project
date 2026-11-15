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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergency.offgridmesh.ui.components.MeshStatusBanner
import com.emergency.offgridmesh.ui.components.PacketItemView
import com.emergency.offgridmesh.ui.theme.MeshBackgroundDark
import com.emergency.offgridmesh.ui.theme.MeshBorderDark
import com.emergency.offgridmesh.ui.theme.MeshGreen
import com.emergency.offgridmesh.ui.theme.MeshSurfaceDark
import com.emergency.offgridmesh.ui.theme.TacticalAmber
import com.emergency.offgridmesh.ui.viewmodel.MeshViewModel

@Composable
fun MeshChatScreen(
    viewModel: MeshViewModel,
    modifier: Modifier = Modifier
) {
    val messages by viewModel.textMessages.collectAsState()
    val connectedPeers by viewModel.connectedPeers.collectAsState()
    val isInternetAvailable by viewModel.isInternetAvailable.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    val quickDisasterPhrases = listOf(
        "I am safe and uninjured.",
        "Need clean drinking water.",
        "Need non-perishable food.",
        "Road blocked by debris.",
        "Sheltered in place.",
        "Cellular signal down here."
    )

    // Scroll to bottom when new messages arrive
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MeshBackgroundDark)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        MeshStatusBanner(
            localNodeId = viewModel.localNodeId,
            connectedPeersCount = connectedPeers.size,
            isInternetAvailable = isInternetAvailable
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick Preset Phrases
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(quickDisasterPhrases) { phrase ->
                AssistChip(
                    onClick = {
                        viewModel.sendTextMessage(phrase)
                    },
                    label = { Text(text = phrase, fontSize = 11.sp) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MeshSurfaceDark,
                        labelColor = Color.LightGray
                    ),
                    border = AssistChipDefaults.assistChipBorder(borderColor = MeshBorderDark)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Chat Stream (reverse layout: newest at top or scrollable)
        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Text(
                        text = "No mesh messages yet.\nType below or tap a quick phrase to broadcast.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(messages, key = { it.packetId }) { packet ->
                        PacketItemView(
                            packet = packet,
                            isLocalNode = packet.senderId == viewModel.localNodeId
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                placeholder = {
                    Text(
                        text = "Broadcast mesh message...",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                },
                singleLine = false,
                maxLines = 3,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = TacticalAmber,
                    unfocusedBorderColor = MeshBorderDark,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = MeshSurfaceDark,
                    unfocusedContainerColor = MeshSurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(
                onClick = {
                    if (inputText.isNotBlank()) {
                        viewModel.sendTextMessage(inputText)
                        inputText = ""
                    }
                },
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (inputText.isNotBlank()) MeshGreen else MeshSurfaceDark)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (inputText.isNotBlank()) Color.Black else Color.Gray,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
