package com.example.ui.games

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.input.pointer.pointerInput
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
import kotlin.math.cos
import kotlin.math.sin

private data class Brick(
    val row: Int,
    val col: Int,
    val points: Int,
    val color: Color,
    var isDestroyed: Boolean = false
)

@Composable
fun BrickBreakerGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("brick_breaker") }

    var paddleX by remember { mutableFloatStateOf(0.5f) } // 0..1
    var paddleWidthRatio by remember { mutableFloatStateOf(0.24f) }
    var ballX by remember { mutableFloatStateOf(0.5f) }
    var ballY by remember { mutableFloatStateOf(0.7f) }
    var ballVx by remember { mutableFloatStateOf(0.008f) }
    var ballVy by remember { mutableFloatStateOf(-0.012f) }

    var lives by remember { mutableIntStateOf(3) }
    var score by remember { mutableIntStateOf(0) }
    var isBallInPlay by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }

    val rows = 5
    val cols = 6
    val rowColors = listOf(
        Pair(Color(0xFFF43F5E), 30),
        Pair(Color(0xFFFB923C), 25),
        Pair(Color(0xFFFBBF24), 20),
        Pair(Color(0xFF34D399), 15),
        Pair(Color(0xFF38BDF8), 10)
    )

    fun createBricks(): List<Brick> {
        val list = mutableListOf<Brick>()
        for (r in 0 until rows) {
            val (color, pts) = rowColors[r]
            for (c in 0 until cols) {
                list.add(Brick(row = r, col = c, points = pts, color = color))
            }
        }
        return list
    }

    var bricks by remember { mutableStateOf(createBricks()) }

    fun resetBall() {
        ballX = paddleX
        ballY = 0.85f
        ballVx = if (ballVx > 0) 0.008f else -0.008f
        ballVy = -0.012f
        isBallInPlay = false
    }

    fun restartGame() {
        paddleX = 0.5f
        paddleWidthRatio = 0.24f
        lives = 3
        score = 0
        bricks = createBricks()
        isGameOver = false
        isVictory = false
        resetBall()
    }

    // Physics update loop
    LaunchedEffect(isBallInPlay, isGameOver, isVictory) {
        while (isBallInPlay && !isGameOver && !isVictory) {
            delay(16) // ~60fps

            var newX = ballX + ballVx
            var newY = ballY + ballVy

            // Wall collisions
            if (newX <= 0.03f) {
                newX = 0.03f
                ballVx = -ballVx
            } else if (newX >= 0.97f) {
                newX = 0.97f
                ballVx = -ballVx
            }

            if (newY <= 0.03f) {
                newY = 0.03f
                ballVy = -ballVy
            }

            // Paddle collision
            val paddleTop = 0.88f
            val paddleLeft = paddleX - paddleWidthRatio / 2
            val paddleRight = paddleX + paddleWidthRatio / 2

            if (newY in (paddleTop - 0.03f)..(paddleTop + 0.02f) &&
                newX in (paddleLeft - 0.02f)..(paddleRight + 0.02f) &&
                ballVy > 0
            ) {
                // Deflect ball angle based on where it hits paddle
                val hitOffset = (newX - paddleX) / (paddleWidthRatio / 2) // -1 to 1
                val angle = hitOffset * 1.0f // Radians deflection
                val speed = 0.014f
                ballVx = speed * sin(angle).coerceIn(-0.012f, 0.012f)
                ballVy = -speed * cos(angle).coerceAtLeast(0.008f)
                HapticHelper.playScore(context)
            }

            // Brick collision
            val brickAreaTop = 0.08f
            val brickAreaHeight = 0.28f
            val brickHeight = brickAreaHeight / rows
            val brickWidth = 1f / cols

            if (newY in brickAreaTop..(brickAreaTop + brickAreaHeight)) {
                val hitR = ((newY - brickAreaTop) / brickHeight).toInt().coerceIn(0, rows - 1)
                val hitC = (newX / brickWidth).toInt().coerceIn(0, cols - 1)

                val hitBrick = bricks.find { it.row == hitR && it.col == hitC && !it.isDestroyed }
                if (hitBrick != null) {
                    hitBrick.isDestroyed = true
                    score += hitBrick.points
                    ballVy = -ballVy
                    HapticHelper.playScore(context)

                    // Check victory
                    if (bricks.all { it.isDestroyed }) {
                        isVictory = true
                        onRecordScore(score)
                        break
                    }
                }
            }

            // Fall below screen
            if (newY >= 1.0f) {
                lives--
                HapticHelper.playGameOver(context)
                if (lives <= 0) {
                    isGameOver = true
                    onRecordScore(score)
                    break
                } else {
                    resetBall()
                    break
                }
            }

            ballX = newX
            ballY = newY
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

        // Lives indicators & Launch Info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                repeat(3) { i ->
                    Text(
                        text = if (i < lives) "❤️" else "🖤",
                        fontSize = 18.sp,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
            }

            Text(
                text = if (!isBallInPlay) "Tap Launch to Start!" else "Drag paddle or use arrows",
                color = if (!isBallInPlay) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }

        // Game Canvas View
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131126)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val deltaFraction = dragAmount.x / size.width
                            paddleX = (paddleX + deltaFraction).coerceIn(paddleWidthRatio / 2, 1f - paddleWidthRatio / 2)
                            if (!isBallInPlay) {
                                ballX = paddleX
                            }
                        }
                    }
                    .testTag("brick_breaker_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw bricks
                    val brickAreaTopPx = h * 0.08f
                    val brickAreaHeightPx = h * 0.28f
                    val bhPx = brickAreaHeightPx / rows
                    val bwPx = w / cols

                    bricks.forEach { brick ->
                        if (!brick.isDestroyed) {
                            val bx = brick.col * bwPx + 3f
                            val by = brickAreaTopPx + brick.row * bhPx + 3f
                            val bWidth = bwPx - 6f
                            val bHeight = bhPx - 6f

                            drawRoundRect(
                                color = brick.color,
                                topLeft = Offset(bx, by),
                                size = Size(bWidth, bHeight),
                                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                            )
                        }
                    }

                    // Draw paddle
                    val pwPx = w * paddleWidthRatio
                    val phPx = 14.dp.toPx()
                    val pxPx = (paddleX * w) - pwPx / 2
                    val pyPx = h * 0.88f

                    drawRoundRect(
                        color = Color(0xFFEC4899),
                        topLeft = Offset(pxPx, pyPx),
                        size = Size(pwPx, phPx),
                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                    )

                    // Draw ball
                    val ballRadiusPx = 8.dp.toPx()
                    drawCircle(
                        color = Color(0xFF38BDF8),
                        radius = ballRadiusPx,
                        center = Offset(ballX * w, ballY * h)
                    )
                }
            }
        }

        // Bottom Controls: Left/Right Paddle buttons + Launch Button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    paddleX = (paddleX - 0.08f).coerceIn(paddleWidthRatio / 2, 1f - paddleWidthRatio / 2)
                    if (!isBallInPlay) ballX = paddleX
                    HapticHelper.playClick(context)
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                    .testTag("paddle_left")
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Paddle Left", tint = Color.White)
            }

            Button(
                onClick = {
                    if (!isBallInPlay) {
                        isBallInPlay = true
                        HapticHelper.playClick(context)
                    }
                },
                enabled = !isBallInPlay && !isGameOver && !isVictory,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEC4899)
                ),
                modifier = Modifier
                    .height(48.dp)
                    .width(130.dp)
                    .testTag("launch_ball_button")
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Launch")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Launch", fontWeight = FontWeight.Bold)
            }

            IconButton(
                onClick = {
                    paddleX = (paddleX + 0.08f).coerceIn(paddleWidthRatio / 2, 1f - paddleWidthRatio / 2)
                    if (!isBallInPlay) ballX = paddleX
                    HapticHelper.playClick(context)
                },
                modifier = Modifier
                    .size(48.dp)
                    .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                    .testTag("paddle_right")
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Paddle Right", tint = Color.White)
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver || isVictory,
        title = if (isVictory) "All Bricks Smashed!" else "Out of Lives!",
        subtitle = if (isVictory) "You destroyed every brick!" else "Nice effort! Try again!",
        score = score,
        highScore = highScore,
        isWin = isVictory,
        onRestart = { restartGame() },
        onHome = onBack
    )
}
