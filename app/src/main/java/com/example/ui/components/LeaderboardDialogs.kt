package com.example.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.FirebaseCustomConfig
import com.example.data.FirestoreConnectionState
import com.example.data.LeaderboardEntry
import com.example.model.GameRegistry
import com.example.ui.theme.ArcadeTheme

@Composable
fun GameLeaderboardModalDialog(
    gameId: String?,
    entries: List<LeaderboardEntry>,
    currentPlayerId: String,
    currentPlayerName: String,
    connectionState: FirestoreConnectionState,
    onDismiss: () -> Unit,
    onOpenFullHub: (String) -> Unit,
    onEditCallsign: () -> Unit,
    onRefresh: () -> Unit
) {
    if (gameId == null) return
    val game = remember(gameId) { GameRegistry.getGame(gameId) }
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.5.dp, Color(game.accentColorHex).copy(alpha = 0.6f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("game_leaderboard_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .background(
                                    Color(game.accentColorHex).copy(alpha = 0.2f),
                                    RoundedCornerShape(12.dp)
                                )
                                .border(
                                    1.dp,
                                    Color(game.accentColorHex),
                                    RoundedCornerShape(12.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = game.iconEmoji, fontSize = 22.sp)
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "${game.title.uppercase()} TOP 10",
                                style = textStyles.gameHeaderTitle.copy(fontSize = 15.sp),
                                color = Color.White
                            )
                            FirestoreStatusBadge(connectionState = connectionState)
                        }
                    }

                    Row {
                        IconButton(
                            onClick = {
                                HapticHelper.playClick(context)
                                onRefresh()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Refresh Top 10",
                                tint = colors.neonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        IconButton(
                            onClick = {
                                HapticHelper.playClick(context)
                                onDismiss()
                            },
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("close_leaderboard_modal")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Player Callsign bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                        .clickable {
                            HapticHelper.playClick(context)
                            onEditCallsign()
                        }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("🕹️ CALLSIGN: ", style = textStyles.hudLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = currentPlayerName,
                            style = textStyles.hudLabel.copy(fontSize = 12.sp),
                            color = colors.neonCyan
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Callsign",
                            tint = colors.neonCyan,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EDIT", style = textStyles.hudLabel, color = colors.neonCyan)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (entries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🏆", fontSize = 36.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "NO SCORES RECORDED YET",
                                style = textStyles.hudLabel.copy(fontSize = 12.sp),
                                color = colors.scoreGold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Finish a round of ${game.title} to claim #1!",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 310.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        itemsIndexed(entries, key = { _, item -> item.id }) { index, entry ->
                            LeaderboardEntryRow(
                                rank = index + 1,
                                entry = entry,
                                isCurrentUser = entry.playerId == currentPlayerId,
                                accentColor = Color(game.accentColorHex)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            HapticHelper.playClick(context)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                    ) {
                        Text("Close", style = textStyles.hudLabel, color = Color.White)
                    }

                    Button(
                        onClick = {
                            HapticHelper.playClick(context)
                            onDismiss()
                            onOpenFullHub(game.id)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.neonCyan,
                            contentColor = Color(0xFF003730)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .testTag("modal_open_all_leaderboards")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("All 10 Games", style = textStyles.hudLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun LeaderboardEntryRow(
    rank: Int,
    entry: LeaderboardEntry,
    isCurrentUser: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    val rankColor = when (rank) {
        1 -> Color(0xFFFBBF24) // Gold
        2 -> Color(0xFFE2E8F0) // Silver
        3 -> Color(0xFFD97706) // Bronze
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val medalEmoji = when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> null
    }

    val relativeTime = remember(entry.timestamp) {
        DateUtils.getRelativeTimeSpanString(
            entry.timestamp,
            System.currentTimeMillis(),
            DateUtils.MINUTE_IN_MILLIS,
            DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentUser) {
                accentColor.copy(alpha = 0.16f)
            } else {
                colors.surfaceElevated
            }
        ),
        border = BorderStroke(
            width = if (rank <= 3 || isCurrentUser) 1.dp else 0.5.dp,
            color = when {
                isCurrentUser -> colors.neonCyan
                rank == 1 -> colors.scoreGold.copy(alpha = 0.7f)
                rank == 2 -> Color(0xFF94A3B8).copy(alpha = 0.5f)
                rank == 3 -> Color(0xFFD97706).copy(alpha = 0.5f)
                else -> colors.border
            }
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("leaderboard_row_$rank")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (rank <= 3) rankColor.copy(alpha = 0.2f) else Color(0xFF141226),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (medalEmoji != null) {
                        Text(text = medalEmoji, fontSize = 15.sp)
                    } else {
                        Text(
                            text = "#$rank",
                            style = textStyles.hudLabel,
                            color = rankColor,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = entry.playerName,
                            style = textStyles.gameCardTitle.copy(fontSize = 14.sp),
                            color = if (isCurrentUser) colors.neonCyan else Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .background(colors.neonCyan.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "YOU",
                                    style = textStyles.hudLabel.copy(fontSize = 8.sp),
                                    color = colors.neonCyan
                                )
                            }
                        }
                    }
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 10.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score}",
                    style = textStyles.hudScore.copy(fontSize = 17.sp),
                    color = if (rank == 1) colors.scoreGold else colors.neonCyan
                )
                Text(
                    text = entry.scoreUnit.uppercase(),
                    style = textStyles.hudLabel.copy(fontSize = 9.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FirestoreStatusBadge(
    connectionState: FirestoreConnectionState,
    modifier: Modifier = Modifier
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    val (icon, label, tint) = when (connectionState) {
        is FirestoreConnectionState.Connected -> Triple(
            Icons.Default.CloudDone,
            "FIRESTORE LIVE (${connectionState.projectId})",
            colors.neonGreen
        )
        is FirestoreConnectionState.Syncing -> Triple(
            Icons.Default.CloudSync,
            "SYNCING FIRESTORE...",
            colors.neonYellow
        )
        is FirestoreConnectionState.LocalOnly -> Triple(
            Icons.Default.CloudQueue,
            "FIRESTORE READY • OFFLINE CACHE",
            colors.neonCyan
        )
        is FirestoreConnectionState.Error -> Triple(
            Icons.Default.CloudQueue,
            "LOCAL CACHE MODE",
            colors.neonOrange
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = tint,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = textStyles.hudLabel.copy(fontSize = 9.sp),
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun EditCallsignDialog(
    isOpen: Boolean,
    currentName: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    if (!isOpen) return
    var nameInput by remember(currentName) { mutableStateOf(currentName) }
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val presets = listOf("PLAYER 1", "ACE", "CYBER", "NEON", "PIXEL", "RETRO")

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.5.dp, colors.neonCyan),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("edit_callsign_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🕹️ ARCADE CALLSIGN",
                    style = textStyles.gameHeaderTitle,
                    color = colors.neonCyan
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Your callsign is displayed on the Global Top 10 Leaderboards across all 10 games.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { if (it.length <= 14) nameInput = it.uppercase() },
                    label = { Text("CALLSIGN (MAX 14 CHARS)", style = textStyles.hudLabel) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonCyan,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("callsign_input")
                )

                Spacer(modifier = Modifier.height(10.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(presets) { preset ->
                        Box(
                            modifier = Modifier
                                .background(colors.surfaceElevated, RoundedCornerShape(8.dp))
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .clickable { nameInput = preset }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(preset, style = textStyles.hudLabel, color = colors.scoreGold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Cancel", style = textStyles.hudLabel, color = Color.White)
                    }
                    Button(
                        onClick = {
                            onSave(nameInput)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.neonCyan,
                            contentColor = Color(0xFF003730)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_callsign_button")
                    ) {
                        Text("Save Callsign", style = textStyles.hudLabel)
                    }
                }
            }
        }
    }
}

@Composable
fun FirebaseConfigDialog(
    isOpen: Boolean,
    initialConfig: FirebaseCustomConfig,
    connectionState: FirestoreConnectionState,
    onDismiss: () -> Unit,
    onSaveConfig: (projectId: String, appId: String, apiKey: String) -> Unit
) {
    if (!isOpen) return
    var projectId by remember(initialConfig) { mutableStateOf(initialConfig.projectId) }
    var appId by remember(initialConfig) { mutableStateOf(initialConfig.applicationId) }
    var apiKey by remember(initialConfig) { mutableStateOf(initialConfig.apiKey) }

    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.5.dp, colors.neonPurple),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("firebase_config_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = null,
                            tint = colors.neonCyan
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "FIREBASE FIRESTORE SETUP",
                            style = textStyles.gameHeaderTitle.copy(fontSize = 15.sp),
                            color = Color.White
                        )
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                FirestoreStatusBadge(connectionState = connectionState)
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Connect your Firebase Cloud Firestore database via app/google-services.json, AI Studio Secrets (.env), or enter your Firebase Project credentials below:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = projectId,
                    onValueChange = { projectId = it },
                    label = { Text("FIREBASE PROJECT ID", style = textStyles.hudLabel) },
                    placeholder = { Text("e.g. arcade-10-global", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonCyan,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("fb_project_id_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = appId,
                    onValueChange = { appId = it },
                    label = { Text("FIREBASE APP ID (mobilesdk_app_id)", style = textStyles.hudLabel) },
                    placeholder = { Text("e.g. 1:123456789:android:abcdef", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonCyan,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("fb_app_id_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { apiKey = it },
                    label = { Text("FIREBASE WEB / ANDROID API KEY", style = textStyles.hudLabel) },
                    placeholder = { Text("AIzaSy...", fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.neonCyan,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("fb_api_key_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier.weight(1f).height(46.dp)
                    ) {
                        Text("Close", style = textStyles.hudLabel, color = Color.White)
                    }

                    Button(
                        onClick = {
                            onSaveConfig(projectId, appId, apiKey)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.neonCyan,
                            contentColor = Color(0xFF003730)
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .testTag("save_firebase_config_button")
                    ) {
                        Text("Connect & Sync", style = textStyles.hudLabel)
                    }
                }
            }
        }
    }
}
