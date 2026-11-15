package com.emergency.offgridmesh.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emergency.offgridmesh.ui.theme.MeshBorderDark
import com.emergency.offgridmesh.ui.theme.MeshGreen
import com.emergency.offgridmesh.ui.theme.MeshSurfaceDark
import com.emergency.offgridmesh.ui.theme.TacticalAmber

@Composable
fun MeshStatusBanner(
    localNodeId: String,
    connectedPeersCount: Int,
    isInternetAvailable: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MeshSurfaceDark)
            .border(1.dp, MeshBorderDark, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Column {
            Text(
                text = "MY NODE ID",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 9.sp
            )
            Text(
                text = localNodeId,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (connectedPeersCount > 0) MeshGreen else TacticalAmber)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = "MESH PEERS",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    fontSize = 9.sp
                )
                Text(
                    text = if (connectedPeersCount == 1) "1 Node" else "$connectedPeersCount Nodes",
                    style = MaterialTheme.typography.bodyMedium,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = if (connectedPeersCount > 0) MeshGreen else TacticalAmber
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "GATEWAY",
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                fontSize = 9.sp
            )
            Text(
                text = if (isInternetAvailable) "UPLINK ON" else "OFF-GRID",
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isInternetAvailable) MeshGreen else TacticalAmber
            )
        }
    }
}
