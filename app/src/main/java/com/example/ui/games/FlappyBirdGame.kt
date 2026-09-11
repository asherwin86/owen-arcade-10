package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

private data class Pipe(
    var x: Float, // 0..1
    val gapY: Float, // 0.2..0.8
    val gapHeight: Float = 0.28f,
    var isPassed: Boolean = false
)

@Composable
fun FlappyBirdGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("flappy_bird") }

    var birdY by remember { mutableFloatStateOf(0.45f) }
    var birdVelocity by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }

    var pipes by remember {
        mutableStateOf(
            listOf(
                Pipe(x = 1.0f, gapY = 0.4f),
                Pipe(x = 1.6f, gapY = 0.55f)
            )
        )
    }

    fun restartGame() {
        birdY = 0.45f
        birdVelocity = 0f
        isPlaying = false
        isGameOver = false
        score = 0
        pipes = listOf(
            Pipe(x = 1.0f, gapY = 0.4f),
            Pipe(x = 1.6f, gapY = 0.55f)
        )
    }

    fun flap() {
        if (!isPlaying && !isGameOver) {
            isPlaying = true
        }
        if (isPlaying && !isGameOver) {
            birdVelocity = -0.016f
            HapticHelper.playClick(context)
        }
    }

    // Game physics loop
    LaunchedEffect(isPlaying, isGameOver) {
        val gravity = 0.0009f
        val pipeSpeed = 0.007f

        while (isPlaying && !isGameOver) {
            delay(16) // ~60 FPS

            birdVelocity += gravity
            birdY += birdVelocity

            // Ground or Ceiling collision
            if (birdY <= 0.04f || birdY >= 0.92f) {
                isGameOver = true
                isPlaying = false
                HapticHelper.playGameOver(context)
                onRecordScore(score)
                break
            }

            // Move pipes
            val birdX = 0.25f
            val birdRadius = 0.035f
            val pipeWidth = 0.14f

            val updatedPipes = pipes.map { pipe ->
                val newX = pipe.x - pipeSpeed
                var passed = pipe.isPassed

                // Score check
                if (!passed && newX + pipeWidth < birdX) {
                    passed = true
                    score++
                    HapticHelper.playScore(context)
                }

                // Pipe Collision check
                val inPipeX = (birdX + birdRadius > newX) && (birdX - birdRadius < newX + pipeWidth)
                val topPipeBottom = pipe.gapY - pipe.gapHeight / 2
                val bottomPipeTop = pipe.gapY + pipe.gapHeight / 2

                if (inPipeX) {
                    if (birdY - birdRadius < topPipeBottom || birdY + birdRadius > bottomPipeTop) {
                        isGameOver = true
                        isPlaying = false
                        HapticHelper.playGameOver(context)
                        onRecordScore(score)
                    }
                }

                pipe.copy(x = newX, isPassed = passed)
            }

            if (isGameOver) break

            // Recycle pipes that go off-screen
            val finalPipes = updatedPipes.map { p ->
                if (p.x < -0.2f) {
                    val randomGap = Random.nextFloat() * 0.4f + 0.3f // 0.3..0.7
                    Pipe(x = 1.0f + 0.1f, gapY = randomGap, isPassed = false)
                } else p
            }

            pipes = finalPipes
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

        // Arena Canvas
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0B192C)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.68f)
                    .clickable { flap() }
                    .testTag("flappy_canvas")
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height

                        // Draw Sky stars
                        drawCircle(Color(0x33FFFFFF), 2.dp.toPx(), Offset(w * 0.2f, h * 0.15f))
                        drawCircle(Color(0x33FFFFFF), 3.dp.toPx(), Offset(w * 0.7f, h * 0.25f))
                        drawCircle(Color(0x33FFFFFF), 2.dp.toPx(), Offset(w * 0.85f, h * 0.1f))

                        // Draw Pipes
                        val pipeWidthPx = w * 0.14f
                        pipes.forEach { pipe ->
                            val px = pipe.x * w
                            val topPipeHeight = (pipe.gapY - pipe.gapHeight / 2) * h
                            val bottomPipeStart = (pipe.gapY + pipe.gapHeight / 2) * h

                            // Top pipe
                            drawRoundRect(
                                color = Color(0xFF10B981),
                                topLeft = Offset(px, 0f),
                                size = Size(pipeWidthPx, topPipeHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            // Top pipe lip
                            drawRoundRect(
                                color = Color(0xFF059669),
                                topLeft = Offset(px - 4f, topPipeHeight - 20f),
                                size = Size(pipeWidthPx + 8f, 20f),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )

                            // Bottom pipe
                            drawRoundRect(
                                color = Color(0xFF10B981),
                                topLeft = Offset(px, bottomPipeStart),
                                size = Size(pipeWidthPx, h - bottomPipeStart),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                            // Bottom pipe lip
                            drawRoundRect(
                                color = Color(0xFF059669),
                                topLeft = Offset(px - 4f, bottomPipeStart),
                                size = Size(pipeWidthPx + 8f, 20f),
                                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            )
                        }

                        // Draw Ground
                        drawRect(
                            color = Color(0xFF1E293B),
                            topLeft = Offset(0f, h * 0.92f),
                            size = Size(w, h * 0.08f)
                        )
                        drawLine(
                            color = Color(0xFF38BDF8),
                            start = Offset(0f, h * 0.92f),
                            end = Offset(w, h * 0.92f),
                            strokeWidth = 3.dp.toPx()
                        )

                        // Draw Bird
                        val bx = w * 0.25f
                        val by = birdY * h
                        val birdRadiusPx = w * 0.035f

                        // Bird body
                        drawCircle(
                            color = Color(0xFFFBBF24),
                            radius = birdRadiusPx,
                            center = Offset(bx, by)
                        )
                        // Bird wing
                        drawCircle(
                            color = Color(0xFFF59E0B),
                            radius = birdRadiusPx * 0.55f,
                            center = Offset(bx - birdRadiusPx * 0.3f, by)
                        )
                        // Eye
                        drawCircle(
                            color = Color.White,
                            radius = birdRadiusPx * 0.35f,
                            center = Offset(bx + birdRadiusPx * 0.4f, by - birdRadiusPx * 0.25f)
                        )
                        drawCircle(
                            color = Color.Black,
                            radius = birdRadiusPx * 0.18f,
                            center = Offset(bx + birdRadiusPx * 0.45f, by - birdRadiusPx * 0.25f)
                        )
                        // Beak
                        drawRoundRect(
                            color = Color(0xFFEA580C),
                            topLeft = Offset(bx + birdRadiusPx * 0.8f, by - birdRadiusPx * 0.15f),
                            size = Size(birdRadiusPx * 0.6f, birdRadiusPx * 0.35f),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }

                    if (!isPlaying && !isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0x88000000)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("🐤", fontSize = 48.sp)
                                Text(
                                    "Tap Anywhere to Flap!",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver,
        title = "Flight Terminated!",
        subtitle = "You cleared $score pipes!",
        score = score,
        highScore = highScore,
        scoreUnit = "pipes",
        onRestart = { restartGame() },
        onHome = onBack
    )
}
