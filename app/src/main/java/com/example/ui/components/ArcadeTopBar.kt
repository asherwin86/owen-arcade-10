package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.LocalMusicManager
import com.example.model.GameInfo
import com.example.ui.LocalLeaderboardController
import com.example.ui.theme.ArcadeTheme

@Composable
fun ArcadeGameHeader(
    game: GameInfo,
    score: Int,
    highScore: Int,
    onBack: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val context = LocalContext.current
    val leaderboardController = LocalLeaderboardController.current
    val globalTopScore = leaderboardController.topScoresByGame[game.id]?.firstOrNull()?.score ?: 0
    val displayedHigh = maxOf(highScore, globalTopScore)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(42.dp)
                        .background(colors.surfaceCard, CircleShape)
                        .border(1.dp, colors.border, CircleShape)
                        .testTag("game_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to Hub",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = game.iconEmoji,
                            fontSize = 20.sp,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Text(
                            text = game.title,
                            style = textStyles.gameHeaderTitle.copy(fontSize = 16.sp),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = game.subtitle.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(game.accentColorHex),
                            letterSpacing = 1.2.sp
                        )
                        val musicManager = LocalMusicManager.current
                        if (musicManager != null) {
                            val trackName = musicManager.getTrackTitleForGame(game.id)
                            Text(
                                text = "• 🎵 $trackName",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = colors.neonCyan.copy(alpha = 0.85f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Retro arcade HUD score badge
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.padding(end = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "1UP",
                                style = textStyles.hudLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$score",
                                style = textStyles.hudScore,
                                color = colors.neonCyan
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(22.dp)
                                .background(colors.border)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "TOP",
                                style = textStyles.hudLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$displayedHigh",
                                style = textStyles.hudScore,
                                color = colors.scoreGold
                            )
                        }
                    }
                }

                IconButton(
                    onClick = {
                        HapticHelper.playClick(context)
                        leaderboardController.onOpenGameLeaderboardModal(game.id)
                    },
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(40.dp)
                        .background(colors.surfaceCard, CircleShape)
                        .border(1.dp, colors.scoreGold.copy(alpha = 0.6f), CircleShape)
                        .testTag("game_leaderboard_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = "Top 10 Leaderboard",
                        tint = colors.scoreGold,
                        modifier = Modifier.size(18.dp)
                    )
                }

                val musicManager = LocalMusicManager.current
                if (musicManager != null) {
                    val isMuted by musicManager.isMuted.collectAsState()
                    IconButton(
                        onClick = {
                            HapticHelper.playClick(context)
                            musicManager.toggleMute()
                        },
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(40.dp)
                            .background(colors.surfaceCard, CircleShape)
                            .border(
                                1.dp,
                                if (isMuted) Color(0xFFEF4444).copy(alpha = 0.6f) else colors.neonCyan.copy(alpha = 0.6f),
                                CircleShape
                            )
                            .testTag("game_music_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute BGM" else "Mute BGM",
                            tint = if (isMuted) Color(0xFFEF4444) else colors.neonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onRestart,
                    modifier = Modifier
                        .size(40.dp)
                        .background(colors.surfaceCard, CircleShape)
                        .border(1.dp, colors.border, CircleShape)
                        .testTag("game_restart_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Restart Game",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
