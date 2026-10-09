package com.example.ui.music

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.filled.VolumeMute
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.audio.ArcadeMusicManager

/**
 * Animated retro equalizer visualizer bars.
 */
@Composable
fun RetroEqualizerBars(
    isPlaying: Boolean,
    color: Color = Color(0xFF00FFCC),
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "equalizer")

    val h1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar1"
    )
    val h2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.20f,
        animationSpec = infiniteRepeatable(
            animation = tween(310, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar2"
    )
    val h3 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(490, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar3"
    )
    val h4 by infiniteTransition.animateFloat(
        initialValue = 0.90f,
        targetValue = 0.30f,
        animationSpec = infiniteRepeatable(
            animation = tween(360, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar4"
    )

    Row(
        modifier = modifier.height(18.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        val bars = listOf(h1, h2, h3, h4)
        bars.forEach { fraction ->
            val scale = if (isPlaying) fraction else 0.25f
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height((18 * scale).dp.coerceAtLeast(3.dp))
                    .clip(RoundedCornerShape(1.dp))
                    .background(color)
            )
        }
    }
}

/**
 * High-visibility music controller bar displayed on Arcade Home Screen.
 */
@Composable
fun ArcadeMusicBar(
    musicManager: ArcadeMusicManager,
    currentTrackTitle: String,
    currentTrackSub: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    volume: Float,
    isAiGenerated: Boolean,
    onOpenMusicStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVolumeSlider by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(
                1.dp,
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF8B5CF6).copy(alpha = 0.5f),
                        Color(0xFF00FFCC).copy(alpha = 0.5f)
                    )
                ),
                RoundedCornerShape(16.dp)
            )
            .testTag("arcade_music_bar"),
        color = Color(0xFF131126).copy(alpha = 0.92f),
        tonalElevation = 6.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left: Equalizer + Track Info
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenMusicStudio() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                if (isAiGenerated) Color(0xFF8B5CF6).copy(alpha = 0.25f)
                                else Color(0xFF00FFCC).copy(alpha = 0.2f),
                                CircleShape
                            )
                            .border(
                                1.dp,
                                if (isAiGenerated) Color(0xFFA855F7) else Color(0xFF00FFCC),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPlaying && !isMuted) {
                            RetroEqualizerBars(
                                isPlaying = true,
                                color = if (isAiGenerated) Color(0xFFA855F7) else Color(0xFF00FFCC)
                            )
                        } else {
                            Icon(
                                imageVector = if (isAiGenerated) Icons.Default.AutoAwesome else Icons.Default.MusicNote,
                                contentDescription = "Music",
                                tint = if (isAiGenerated) Color(0xFFA855F7) else Color(0xFF00FFCC),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = currentTrackTitle,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                ),
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // AI / Chiptune badge
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        if (isAiGenerated) Color(0xFF8B5CF6).copy(alpha = 0.35f)
                                        else Color(0xFF10B981).copy(alpha = 0.3f)
                                    )
                                    .padding(horizontal = 5.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = if (isAiGenerated) "LYRIA AI" else "8-BIT BGM",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (isAiGenerated) Color(0xFFD8B4FE) else Color(0xFF6EE7B7),
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }

                        Text(
                            text = currentTrackSub,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                            color = Color(0xFF94A3B8),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(6.dp))

                // Right controls: Play/Pause, Mute/Unmute, Studio
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Play / Pause
                    IconButton(
                        onClick = { musicManager.togglePlayPause() },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("music_play_pause_button")
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause Music" else "Play Music",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Mute / Unmute & Volume expander
                    IconButton(
                        onClick = {
                            if (isMuted) {
                                musicManager.toggleMute()
                            } else {
                                showVolumeSlider = !showVolumeSlider
                            }
                        },
                        modifier = Modifier
                            .size(32.dp)
                            .testTag("music_volume_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = if (isMuted) "Unmute Music" else "Volume",
                            tint = if (isMuted) Color(0xFFEF4444) else Color(0xFF00FFCC),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Open Music Studio / Jukebox
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color(0xFF8B5CF6), Color(0xFFEC4899))
                                )
                            )
                            .clickable { onOpenMusicStudio() }
                            .padding(horizontal = 9.dp, vertical = 6.dp)
                            .testTag("open_music_studio_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = "STUDIO",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            // Collapsible Volume Slider
            if (showVolumeSlider) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = { musicManager.toggleMute() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = null,
                            tint = if (isMuted) Color(0xFFEF4444) else Color(0xFF00FFCC),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Slider(
                        value = if (isMuted) 0f else volume,
                        onValueChange = { musicManager.setVolume(it) },
                        valueRange = 0f..1f,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00FFCC),
                            activeTrackColor = Color(0xFF00FFCC),
                            inactiveTrackColor = Color(0xFF334155)
                        )
                    )

                    Text(
                        text = if (isMuted) "MUTED" else "${(volume * 100).toInt()}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

/**
 * Compact floating music pill for in-game screens.
 * Allows quick muting/unmuting, track inspection, and volume adjustment without interrupting games.
 */
@Composable
fun MiniGameMusicPill(
    musicManager: ArcadeMusicManager,
    currentTrackTitle: String,
    isPlaying: Boolean,
    isMuted: Boolean,
    onOpenMusicStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, Color(0xFF334155), RoundedCornerShape(20.dp))
            .clickable { onOpenMusicStudio() }
            .testTag("in_game_music_pill"),
        color = Color(0xFF0F172A).copy(alpha = 0.88f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            RetroEqualizerBars(
                isPlaying = isPlaying && !isMuted,
                color = if (isMuted) Color(0xFF64748B) else Color(0xFF00FFCC)
            )

            Text(
                text = currentTrackTitle,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = if (isMuted) Color(0xFF64748B) else Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            IconButton(
                onClick = { musicManager.toggleMute() },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = if (isMuted) Icons.AutoMirrored.Filled.VolumeMute else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = if (isMuted) "Unmute" else "Mute",
                    tint = if (isMuted) Color(0xFFEF4444) else Color(0xFF00FFCC),
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}
