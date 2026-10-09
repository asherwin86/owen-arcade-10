package com.example.ui

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.FirestoreConnectionState
import com.example.data.GameScore
import com.example.data.LeaderboardEntry
import com.example.model.GameInfo
import com.example.model.GameRegistry
import com.example.ui.components.FirestoreStatusBadge
import com.example.ui.components.HapticHelper
import com.example.ui.components.LeaderboardEntryRow
import com.example.ui.theme.ArcadeTheme

@Composable
fun GlobalLeaderboardScreen(
    initialGameId: String?,
    topScoresByGame: Map<String, List<LeaderboardEntry>>,
    localHighScores: Map<String, GameScore>,
    currentPlayerId: String,
    currentPlayerName: String,
    connectionState: FirestoreConnectionState,
    onBack: () -> Unit,
    onPlayGame: (String) -> Unit,
    onEditCallsign: () -> Unit,
    onOpenFirebaseConfig: () -> Unit,
    onRefresh: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    // null means "ALL 10 CHAMPIONS" overview tab; otherwise specific gameId
    var selectedTabGameId by remember(initialGameId) {
        mutableStateOf(initialGameId ?: GameRegistry.games.first().id)
    }
    var showAllChampionsOverview by remember(initialGameId) {
        mutableStateOf(false)
    }

    val userTop10Count = remember(topScoresByGame, currentPlayerId) {
        topScoresByGame.values.sumOf { list ->
            list.count { it.playerId == currentPlayerId }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .testTag("global_leaderboard_screen")
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        HapticHelper.playClick(context)
                        onBack()
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.surfaceCard, CircleShape)
                        .border(1.dp, colors.border, CircleShape)
                        .testTag("leaderboard_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = colors.scoreGold,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "HALL OF FAME",
                            style = textStyles.gameHeaderTitle.copy(fontSize = 18.sp),
                            color = colors.scoreGold
                        )
                    }
                    FirestoreStatusBadge(connectionState = connectionState)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = {
                        HapticHelper.playClick(context)
                        onRefresh()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceCard, CircleShape)
                        .border(1.dp, colors.border, CircleShape)
                        .testTag("refresh_leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync Firestore",
                        tint = colors.neonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    onClick = {
                        HapticHelper.playClick(context)
                        onOpenFirebaseConfig()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceCard, CircleShape)
                        .border(1.dp, colors.neonPurple.copy(alpha = 0.7f), CircleShape)
                        .testTag("firebase_config_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudQueue,
                        contentDescription = "Firebase Setup",
                        tint = colors.neonPurple,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Player Callsign & Summary Card
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        HapticHelper.playClick(context)
                        onEditCallsign()
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("edit_callsign_button"),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(colors.neonCyan.copy(alpha = 0.15f), CircleShape)
                            .border(1.dp, colors.neonCyan, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🕹️", fontSize = 16.sp)
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "CALLSIGN: ",
                                style = textStyles.hudLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentPlayerName,
                                style = textStyles.hudLabel.copy(fontSize = 12.sp),
                                color = colors.neonCyan
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Callsign",
                                tint = colors.neonCyan,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "$userTop10Count Top-10 entries across ${GameRegistry.games.size} games",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .background(colors.scoreGold.copy(alpha = 0.15f), RoundedCornerShape(10.dp))
                        .border(1.dp, colors.scoreGold.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "TOP 10",
                        style = textStyles.hudLabel,
                        color = colors.scoreGold
                    )
                }
            }
        }

        // Game Selector Carousel (All Champions + Individual Games)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                FilterChip(
                    selected = showAllChampionsOverview,
                    onClick = {
                        HapticHelper.playClick(context)
                        showAllChampionsOverview = true
                    },
                    label = {
                        Text(
                            text = "👑 ALL CHAMPIONS",
                            style = textStyles.hudLabel,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.scoreGold,
                        selectedLabelColor = Color(0xFF281D00),
                        containerColor = colors.surfaceCard,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = showAllChampionsOverview,
                        borderColor = if (showAllChampionsOverview) colors.scoreGold else colors.border
                    ),
                    modifier = Modifier.testTag("leaderboard_tab_all")
                )
            }

            items(GameRegistry.games, key = { it.id }) { game ->
                val isSelected = !showAllChampionsOverview && selectedTabGameId == game.id
                val gameColor = Color(game.accentColorHex)
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        HapticHelper.playClick(context)
                        showAllChampionsOverview = false
                        selectedTabGameId = game.id
                    },
                    label = {
                        Text(
                            text = "${game.iconEmoji} ${game.title.uppercase()}",
                            style = textStyles.hudLabel,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = gameColor,
                        selectedLabelColor = Color.White,
                        containerColor = colors.surfaceCard,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) gameColor else colors.border
                    ),
                    modifier = Modifier.testTag("leaderboard_tab_${game.id}")
                )
            }
        }

        if (showAllChampionsOverview) {
            AllGamesChampionsList(
                topScoresByGame = topScoresByGame,
                localHighScores = localHighScores,
                onSelectGameLeaderboard = { gameId ->
                    HapticHelper.playClick(context)
                    selectedTabGameId = gameId
                    showAllChampionsOverview = false
                },
                onPlayGame = { gameId ->
                    HapticHelper.playClick(context)
                    onPlayGame(gameId)
                },
                modifier = Modifier.weight(1f)
            )
        } else {
            val activeGame = remember(selectedTabGameId) {
                GameRegistry.getGame(selectedTabGameId)
            }
            val activeEntries = topScoresByGame[activeGame.id] ?: emptyList()
            val personalBest = localHighScores[activeGame.id]?.highScore ?: 0

            SingleGameTop10Content(
                game = activeGame,
                entries = activeEntries,
                personalBest = personalBest,
                currentPlayerId = currentPlayerId,
                onPlayGame = {
                    HapticHelper.playClick(context)
                    onPlayGame(activeGame.id)
                },
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun SingleGameTop10Content(
    game: GameInfo,
    entries: List<LeaderboardEntry>,
    personalBest: Int,
    currentPlayerId: String,
    onPlayGame: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val accentColor = Color(game.accentColorHex)

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Game Hero Card
        item {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    accentColor.copy(alpha = 0.22f),
                                    colors.surfaceCard
                                )
                            )
                        )
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .background(accentColor.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
                                .border(1.dp, accentColor, RoundedCornerShape(16.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = game.iconEmoji, fontSize = 28.sp)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Text(
                                text = game.title.uppercase(),
                                style = textStyles.gameHeaderTitle,
                                color = Color.White
                            )
                            Text(
                                text = "TOP 10 GLOBAL HIGH SCORES (${game.scoreUnit.uppercase()})",
                                style = textStyles.hudLabel.copy(fontSize = 9.sp),
                                color = accentColor
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (personalBest > 0) {
                                    "Your Personal Best: $personalBest ${game.scoreUnit}"
                                } else {
                                    "No personal score recorded yet"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = onPlayGame,
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                        modifier = Modifier.testTag("leaderboard_play_${game.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Play ${game.title}",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("PLAY", style = textStyles.hudLabel)
                    }
                }
            }
        }

        // Top 3 Podium if at least 1 score exists
        if (entries.isNotEmpty()) {
            item {
                TopThreePodium(
                    entries = entries.take(3),
                    accentColor = accentColor
                )
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "RANK & PLAYER",
                        style = textStyles.hudLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "HIGH SCORE (${entries.size}/10)",
                        style = textStyles.hudLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            itemsIndexed(entries, key = { _, item -> item.id }) { index, entry ->
                LeaderboardEntryRow(
                    rank = index + 1,
                    entry = entry,
                    isCurrentUser = entry.playerId == currentPlayerId,
                    accentColor = accentColor
                )
            }
        } else {
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = BorderStroke(1.dp, colors.border),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🏆", fontSize = 46.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "NO HIGH SCORES YET FOR ${game.title.uppercase()}",
                            style = textStyles.gameCardTitle,
                            color = colors.scoreGold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Complete a round of ${game.title} to post the first score to the Firebase Firestore Top 10 Leaderboard!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Button(
                            onClick = onPlayGame,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.neonCyan,
                                contentColor = Color(0xFF003730)
                            ),
                            modifier = Modifier.testTag("empty_state_play_button")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "PLAY ${game.title.uppercase()} NOW",
                                style = textStyles.hudLabel
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopThreePodium(
    entries: List<LeaderboardEntry>,
    accentColor: Color
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    val first = entries.getOrNull(0)
    val second = entries.getOrNull(1)
    val third = entries.getOrNull(2)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            // 2nd Place
            PodiumColumn(
                rank = 2,
                medal = "🥈",
                entry = second,
                podiumColor = Color(0xFF94A3B8),
                barHeight = 68.dp,
                modifier = Modifier.weight(1f)
            )

            // 1st Place
            PodiumColumn(
                rank = 1,
                medal = "🥇",
                entry = first,
                podiumColor = colors.scoreGold,
                barHeight = 90.dp,
                modifier = Modifier.weight(1.15f)
            )

            // 3rd Place
            PodiumColumn(
                rank = 3,
                medal = "🥉",
                entry = third,
                podiumColor = Color(0xFFD97706),
                barHeight = 54.dp,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun PodiumColumn(
    rank: Int,
    medal: String,
    entry: LeaderboardEntry?,
    podiumColor: Color,
    barHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val textStyles = ArcadeTheme.textStyles

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = medal, fontSize = if (rank == 1) 26.sp else 20.sp)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry?.playerName ?: "---",
            style = textStyles.hudLabel.copy(fontSize = 11.sp),
            color = if (entry != null) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = if (entry != null) "${entry.score} ${entry.scoreUnit}" else "OPEN",
            style = textStyles.hudScore.copy(fontSize = if (rank == 1) 15.sp else 13.sp),
            color = podiumColor,
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(barHeight)
                .background(
                    podiumColor.copy(alpha = 0.22f),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .border(
                    1.dp,
                    podiumColor.copy(alpha = 0.6f),
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "#$rank",
                style = textStyles.gameHeaderTitle.copy(fontSize = if (rank == 1) 20.sp else 16.sp),
                color = podiumColor
            )
        }
    }
}

@Composable
private fun AllGamesChampionsList(
    topScoresByGame: Map<String, List<LeaderboardEntry>>,
    localHighScores: Map<String, GameScore>,
    onSelectGameLeaderboard: (String) -> Unit,
    onPlayGame: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "ALL ${GameRegistry.games.size} GAMES • #1 GLOBAL RECORD HOLDERS",
                style = textStyles.hudLabel,
                color = colors.scoreGold,
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        items(GameRegistry.games, key = { it.id }) { game ->
            val topList = topScoresByGame[game.id] ?: emptyList()
            val champion = topList.firstOrNull()
            val localBest = localHighScores[game.id]?.highScore ?: 0
            val gameColor = Color(game.accentColorHex)

            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                border = BorderStroke(1.dp, gameColor.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectGameLeaderboard(game.id) }
                    .testTag("champion_card_${game.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(gameColor.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                                .border(1.dp, gameColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = game.iconEmoji, fontSize = 24.sp)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = game.title,
                                style = textStyles.gameCardTitle,
                                color = Color.White
                            )
                            if (champion != null) {
                                Text(
                                    text = "🥇 #1 ${champion.playerName} • ${champion.score} ${game.scoreUnit.uppercase()}",
                                    style = textStyles.hudLabel.copy(fontSize = 11.sp),
                                    color = colors.scoreGold
                                )
                            } else {
                                Text(
                                    text = "NO GLOBAL RECORD YET • TAP TO VIEW",
                                    style = textStyles.hudLabel.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (localBest > 0) {
                                Text(
                                    text = "Your Best: $localBest ${game.scoreUnit} • ${topList.size}/10 Ranked",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = colors.neonCyan
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(colors.surfaceElevated, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "TOP 10",
                                style = textStyles.hudLabel.copy(fontSize = 9.sp),
                                color = colors.neonCyan
                            )
                        }

                        IconButton(
                            onClick = { onPlayGame(game.id) },
                            modifier = Modifier
                                .size(36.dp)
                                .background(gameColor, CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play ${game.title}",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
