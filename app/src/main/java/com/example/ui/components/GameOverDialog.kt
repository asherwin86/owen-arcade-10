package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.LocalLeaderboardController
import com.example.ui.theme.ArcadeTheme

object HapticHelper {
    fun playClick(context: Context) {
        vibrate(context, 20)
    }

    fun playScore(context: Context) {
        vibrate(context, 40)
    }

    fun playGameOver(context: Context) {
        vibrate(context, 120)
    }
    
    fun playBump(context: Context) {
        vibrate(context, 10)
    }
    
    fun playError(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateWaveform(context, longArrayOf(0, 30, 40, 30), intArrayOf(0, 255, 0, 255))
        } else {
            vibrate(context, 60)
        }
    }
    
    fun playSuccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrateWaveform(context, longArrayOf(0, 20, 30, 40), intArrayOf(0, 100, 0, 255))
        } else {
            vibrate(context, 50)
        }
    }

    private fun getVibrator(context: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun vibrate(context: Context, millis: Long) {
        try {
            val vibrator = getVibrator(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(millis)
            }
        } catch (_: Exception) {}
    }
    
    @androidx.annotation.RequiresApi(Build.VERSION_CODES.O)
    private fun vibrateWaveform(context: Context, timings: LongArray, amplitudes: IntArray) {
        try {
            val vibrator = getVibrator(context)
            vibrator?.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
        } catch (_: Exception) {}
    }
}

@Composable
fun GameOverDialog(
    isOpen: Boolean,
    title: String,
    subtitle: String = "",
    score: Int,
    highScore: Int,
    scoreUnit: String = "pts",
    isWin: Boolean = false,
    onRestart: () -> Unit,
    onHome: () -> Unit
) {
    if (!isOpen) return

    val isNewHigh = score > highScore && score > 0
    val colors = ArcadeTheme.colors
    val textStyles = ArcadeTheme.textStyles
    val leaderboardController = LocalLeaderboardController.current
    val activeGameId = leaderboardController.activeGameId
    val gameTop10 = activeGameId?.let { leaderboardController.topScoresByGame[it] } ?: emptyList()

    // Calculate rank in top 10 if score > 0
    val projectedRank = if (score > 0) {
        val higherCount = gameTop10.count { it.score > score }
        val rank = higherCount + 1
        if (rank <= 10) rank else null
    } else null

    Dialog(onDismissRequest = {}) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = colors.surfaceCard
            ),
            border = androidx.compose.foundation.BorderStroke(1.5.dp, colors.border),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .testTag("game_over_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(
                            if (isWin) colors.neonGreen.copy(alpha = 0.2f)
                            else colors.neonPink.copy(alpha = 0.2f),
                            CircleShape
                        )
                        .border(
                            1.5.dp,
                            if (isWin) colors.neonGreen else colors.neonPink,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isWin) "🏆" else "💥",
                        fontSize = 32.sp
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = title.uppercase(),
                    style = textStyles.gameHeaderTitle.copy(
                        color = if (isWin) colors.neonGreen else colors.neonPink,
                        fontSize = 20.sp
                    ),
                    textAlign = TextAlign.Center
                )

                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surfaceElevated),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "SCORE",
                                style = textStyles.hudLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$score $scoreUnit",
                                style = textStyles.hudScore.copy(fontSize = 20.sp),
                                color = colors.neonCyan
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(38.dp)
                                .background(colors.border)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "BEST",
                                style = textStyles.hudLabel,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "${maxOf(score, highScore)} $scoreUnit",
                                style = textStyles.hudScore.copy(fontSize = 20.sp),
                                color = colors.scoreGold
                            )
                        }
                    }
                }

                if (isNewHigh || projectedRank != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = colors.scoreGold.copy(alpha = 0.18f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.scoreGold)
                    ) {
                        Text(
                            text = when {
                                isNewHigh && projectedRank != null -> "★ NEW HIGH SCORE • GLOBAL RANK #$projectedRank ★"
                                projectedRank != null -> "🏆 TOP 10 LEADERBOARD • RANK #$projectedRank"
                                else -> "★ NEW HIGH SCORE! ★"
                            },
                            color = colors.scoreGold,
                            style = textStyles.hudLabel,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Player Callsign bar for leaderboard entry
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(colors.surfaceElevated, RoundedCornerShape(12.dp))
                        .clickable { leaderboardController.onOpenEditCallsign() }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                        .testTag("dialog_edit_callsign"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "🕹️ CALLSIGN: ",
                            style = textStyles.hudLabel,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = leaderboardController.playerName,
                            style = textStyles.hudLabel,
                            color = colors.neonCyan
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Change Callsign",
                            tint = colors.neonCyan,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("EDIT", style = textStyles.hudLabel, color = colors.neonCyan)
                    }
                }

                if (activeGameId != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { leaderboardController.onOpenGameLeaderboardModal(activeGameId) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dialog_leaderboard_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.scoreGold.copy(alpha = 0.7f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Top 10 Leaderboard",
                            tint = colors.scoreGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "VIEW TOP 10 LEADERBOARD",
                            color = colors.scoreGold,
                            style = textStyles.hudLabel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onHome,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dialog_home_button"),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Menu",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Hub", color = MaterialTheme.colorScheme.onSurface, style = textStyles.hudLabel)
                    }

                    Button(
                        onClick = onRestart,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("dialog_restart_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.neonCyan,
                            contentColor = Color(0xFF003730)
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Play Again"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retry", fontWeight = FontWeight.Bold, style = textStyles.hudLabel)
                    }
                }
            }
        }
    }
}
