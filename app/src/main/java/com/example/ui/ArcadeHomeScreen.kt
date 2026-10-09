package com.example.ui

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.example.audio.ArcadeMusicManager
import com.example.ui.music.ArcadeMusicBar
import com.example.ui.update.AutoUpdateNotificationBanner
import com.example.ui.update.AutoUpdateStatusChip
import com.example.update.AppUpdateManager
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.GameScore
import com.example.data.LeaderboardEntry
import com.example.model.GameCategory
import com.example.model.GameInfo
import com.example.model.GameRegistry
import com.example.ui.components.FirestoreStatusBadge
import com.example.ui.components.HapticHelper
import com.example.ui.theme.ArcadeTheme

@Composable
fun ArcadeHomeScreen(
    highScores: Map<String, GameScore>,
    selectedCategory: GameCategory,
    onCategorySelected: (GameCategory) -> Unit,
    onGameSelected: (String) -> Unit,
    onPlayRandom: () -> Unit,
    onGenerateMusic: () -> Unit,
    musicManager: ArcadeMusicManager? = null,
    updateManager: AppUpdateManager? = null,
    onOpenUpdateCenter: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val leaderboardController = LocalLeaderboardController.current

    val filteredGames = GameRegistry.games.filter {
        selectedCategory == GameCategory.ALL || it.category == selectedCategory
    }

    val totalPlayed = highScores.values.sumOf { it.gamesPlayed }
    val totalGlobalEntries = leaderboardController.topScoresByGame.values.sumOf { it.size }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // App Header Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Generated icon thumbnail
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surfaceElevated)
                        .border(1.dp, colors.neonCyan.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_arcade_logo),
                        contentDescription = "Arcade Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "ARCADE 10",
                        style = textStyles.gameHeaderTitle.copy(
                            fontSize = 19.sp,
                            color = colors.neonCyan
                        )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FirestoreStatusBadge(connectionState = leaderboardController.connectionState)
                        if (updateManager != null) {
                            AutoUpdateStatusChip(
                                updateManager = updateManager,
                                onClick = {
                                    HapticHelper.playClick(context)
                                    onOpenUpdateCenter()
                                }
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Auto-Update Center Button
                IconButton(
                    onClick = {
                        HapticHelper.playClick(context)
                        onOpenUpdateCenter()
                    },
                    modifier = Modifier
                        .background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                        .border(1.dp, colors.neonCyan.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .size(36.dp)
                        .testTag("open_update_center_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = "Auto-Update Center",
                        tint = colors.neonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Global Leaderboards Hall of Fame Button
                IconButton(
                    onClick = {
                        HapticHelper.playClick(context)
                        leaderboardController.onOpenGlobalLeaderboardScreen(null)
                    },
                    modifier = Modifier
                        .background(colors.scoreGold, RoundedCornerShape(12.dp))
                        .size(36.dp)
                        .testTag("open_leaderboards_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Global Leaderboards",
                        tint = Color(0xFF281D00),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Generate Music Button
                IconButton(
                    onClick = {
                        HapticHelper.playClick(context)
                        onGenerateMusic()
                    },
                    modifier = Modifier
                        .background(colors.neonPurple, RoundedCornerShape(12.dp))
                        .size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = "Generate Music",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Quick Play Random button
                Button(
                    onClick = {
                        HapticHelper.playClick(context)
                        onPlayRandom()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.neonCyan,
                        contentColor = Color(0xFF003730)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                    modifier = Modifier.testTag("random_game_button").height(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Casino,
                        contentDescription = "Random Game",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Surprise", style = textStyles.hudLabel, fontSize = 11.sp)
                }
            }
        }

        // Auto-Update / Live Broadcast Banner
        if (updateManager != null) {
            AutoUpdateNotificationBanner(
                updateManager = updateManager,
                onOpenUpdateCenter = onOpenUpdateCenter,
                onPlayFeaturedGame = onGameSelected,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        // Background Music Controller Bar
        if (musicManager != null) {
            val currentTrackTitle by musicManager.currentTrackTitle.collectAsState()
            val currentTrackSub by musicManager.currentTrackSub.collectAsState()
            val isPlaying by musicManager.isPlaying.collectAsState()
            val isMuted by musicManager.isMuted.collectAsState()
            val volume by musicManager.volume.collectAsState()
            val isAiGenerated by musicManager.isAiGenerated.collectAsState()

            ArcadeMusicBar(
                musicManager = musicManager,
                currentTrackTitle = currentTrackTitle,
                currentTrackSub = currentTrackSub,
                isPlaying = isPlaying,
                isMuted = isMuted,
                volume = volume,
                isAiGenerated = isAiGenerated,
                onOpenMusicStudio = onGenerateMusic,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        // Global Leaderboard & Stats Banner (always visible so player can access Hall of Fame or Callsign)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
            border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp)
                .clickable {
                    HapticHelper.playClick(context)
                    leaderboardController.onOpenGlobalLeaderboardScreen(null)
                }
                .testTag("home_leaderboard_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("🏆", fontSize = 16.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "GLOBAL TOP 10 HALL OF FAME",
                            style = textStyles.hudLabel,
                            color = colors.scoreGold
                        )
                        Text(
                            text = "🕹️ ${leaderboardController.playerName} • PLAYED: $totalPlayed",
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Trophy",
                        tint = colors.neonCyan,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$totalGlobalEntries RANKED →",
                        style = textStyles.hudLabel,
                        color = colors.neonCyan
                    )
                }
            }
        }

        // Category Filter Tabs
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(GameCategory.entries) { category ->
                val isSelected = category == selectedCategory
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        HapticHelper.playClick(context)
                        onCategorySelected(category)
                    },
                    label = {
                        Text(
                            text = category.displayName.uppercase(),
                            style = textStyles.hudLabel,
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.neonPink,
                        selectedLabelColor = Color.White,
                        containerColor = colors.surfaceCard,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) colors.neonPink else colors.border
                    )
                )
            }
        }

        // Games List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            items(filteredGames, key = { it.id }) { game ->
                val scoreData = highScores[game.id]
                val gameTop10 = leaderboardController.topScoresByGame[game.id] ?: emptyList()
                GameCard(
                    game = game,
                    scoreData = scoreData,
                    topEntry = gameTop10.firstOrNull(),
                    onClick = {
                        HapticHelper.playClick(context)
                        onGameSelected(game.id)
                    },
                    onOpenLeaderboard = {
                        HapticHelper.playClick(context)
                        leaderboardController.onOpenGlobalLeaderboardScreen(game.id)
                    }
                )
            }
        }
    }
}

@Composable
private fun GameCard(
    game: GameInfo,
    scoreData: GameScore?,
    topEntry: LeaderboardEntry?,
    onClick: () -> Unit,
    onOpenLeaderboard: () -> Unit
) {
    val bestScore = scoreData?.highScore ?: 0
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("game_card_${game.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Big Emoji Badge with neon halo
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(Color(game.accentColorHex).copy(alpha = 0.18f), RoundedCornerShape(18.dp))
                    .border(1.dp, Color(game.accentColorHex).copy(alpha = 0.4f), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = game.iconEmoji,
                    fontSize = 32.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Game Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = game.title,
                        style = textStyles.gameCardTitle,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Category badge
                    Box(
                        modifier = Modifier
                            .background(Color(game.accentColorHex).copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = game.subtitle.uppercase(),
                            style = textStyles.hudLabel,
                            fontSize = 9.sp,
                            color = Color(game.accentColorHex)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = game.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                // High score & Global #1 badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (bestScore > 0) {
                        Text(
                            text = "★ YOU: $bestScore ${game.scoreUnit.uppercase()}",
                            style = textStyles.hudLabel,
                            color = colors.scoreGold
                        )
                    } else {
                        Text(
                            text = "READY TO PLAY",
                            style = textStyles.hudLabel,
                            color = colors.neonCyan.copy(alpha = 0.8f)
                        )
                    }

                    if (topEntry != null) {
                        Text(
                            text = "🥇 #1: ${topEntry.score} (${topEntry.playerName})",
                            style = textStyles.hudLabel.copy(fontSize = 9.sp),
                            color = colors.neonCyan,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Play Button Icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.neonCyan, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF003730),
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Game Top 10 Leaderboard Button
                IconButton(
                    onClick = onOpenLeaderboard,
                    modifier = Modifier
                        .size(34.dp)
                        .background(colors.surfaceElevated, CircleShape)
                        .border(1.dp, colors.scoreGold.copy(alpha = 0.6f), CircleShape)
                        .testTag("card_leaderboard_${game.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "${game.title} Top 10 Leaderboard",
                        tint = colors.scoreGold,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
