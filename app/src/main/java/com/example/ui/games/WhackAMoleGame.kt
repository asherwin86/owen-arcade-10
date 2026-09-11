package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameRegistry
import com.example.ui.components.ArcadeGameHeader
import com.example.ui.components.GameOverDialog
import com.example.ui.components.HapticHelper
import kotlinx.coroutines.delay
import kotlin.random.Random

private enum class MoleType(val emoji: String, val points: Int) {
    REGULAR("🦔", 1),
    GOLDEN("👑", 3),
    BOMB("💣", -2)
}

private data class HoleState(
    val activeMole: MoleType? = null,
    val isHit: Boolean = false
)

@Composable
fun WhackAMoleGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("whack_a_mole") }

    var holes by remember { mutableStateOf(List(9) { HoleState() }) }
    var score by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var combo by remember { mutableIntStateOf(1) }
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }

    fun restartGame() {
        holes = List(9) { HoleState() }
        score = 0
        timeLeft = 30
        combo = 1
        isPlaying = true
        isGameOver = false
    }

    // Timer countdown
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (timeLeft > 0) {
                delay(1000)
                timeLeft--
            }
            isPlaying = false
            isGameOver = true
            HapticHelper.playGameOver(context)
            onRecordScore(score)
        }
    }

    // Mole popping loop
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            val randomInterval = Random.nextLong(400, 750)
            delay(randomInterval)

            // Pick 1 or 2 random holes
            val randomHole = Random.nextInt(9)
            val moleKind = when {
                Random.nextFloat() < 0.20f -> MoleType.BOMB
                Random.nextFloat() < 0.25f -> MoleType.GOLDEN
                else -> MoleType.REGULAR
            }

            val updated = holes.toMutableList()
            updated[randomHole] = HoleState(activeMole = moleKind, isHit = false)
            holes = updated

            // Stay visible for 700-1000ms
            delay(Random.nextLong(650, 950))

            val cleared = holes.toMutableList()
            if (cleared[randomHole].activeMole == moleKind && !cleared[randomHole].isHit) {
                cleared[randomHole] = HoleState(activeMole = null)
                holes = cleared
            }
        }
    }

    fun onHoleClick(index: Int) {
        if (!isPlaying || isGameOver) return
        val hole = holes[index]

        if (hole.activeMole != null && !hole.isHit) {
            val pts = hole.activeMole.points
            if (pts > 0) {
                val gained = pts * combo
                score += gained
                combo = (combo + 1).coerceAtMost(4)
                HapticHelper.playScore(context)
            } else {
                score = (score + pts).coerceAtLeast(0)
                combo = 1
                HapticHelper.playGameOver(context)
            }

            val updated = holes.toMutableList()
            updated[index] = hole.copy(isHit = true)
            holes = updated
        } else {
            // Tapped empty hole
            combo = 1
            HapticHelper.playClick(context)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0C20))
            .navigationBarsPadding()
    ) {
        ArcadeGameHeader(
            game = gameInfo,
            score = score,
            highScore = maxOf(score, highScore),
            onBack = onBack,
            onRestart = { restartGame() }
        )

        // Status Card: Timer & Combo Multiplier
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("TIME LEFT", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text("${timeLeft}s", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = if (timeLeft <= 5) Color(0xFFEF4444) else Color(0xFF38BDF8))
                }

                Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (combo > 1) Color(0xFFF59E0B) else Color(0xFF334155)
                    )
                ) {
                    Text(
                        text = "${combo}x COMBO",
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 3x3 Holes Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151325)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val hole = holes[index]

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(Color(0xFF26233F), CircleShape)
                                        .clickable { onHoleClick(index) }
                                        .testTag("mole_hole_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Mole hole rim
                                    Box(
                                        modifier = Modifier
                                            .size(68.dp)
                                            .background(Color(0xFF19172B), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (hole.isHit) {
                                            Text("💥", fontSize = 32.sp)
                                        } else if (hole.activeMole != null) {
                                            Text(hole.activeMole.emoji, fontSize = 34.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isPlaying && !isGameOver) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xBB000000), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Button(
                        onClick = { restartGame() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .height(52.dp)
                            .testTag("start_whack_button")
                    ) {
                        Text("🔨 Start Whacking!", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver,
        title = "Time's Up!",
        subtitle = "You scored $score points in 30s!",
        score = score,
        highScore = highScore,
        scoreUnit = "pts",
        onRestart = { restartGame() },
        onHome = onBack
    )
}
