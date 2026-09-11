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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.abs
import kotlin.random.Random

private const val GRID_WIDTH = 18
private const val GRID_HEIGHT = 20

private enum class Direction { UP, DOWN, LEFT, RIGHT }

private data class Point(val x: Int, val y: Int)

@Composable
fun SnakeGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("snake") }

    var snake by remember {
        mutableStateOf(
            listOf(
                Point(8, 10),
                Point(8, 11),
                Point(8, 12)
            )
        )
    }
    var direction by remember { mutableStateOf(Direction.UP) }
    var nextDirection by remember { mutableStateOf(Direction.UP) }
    var food by remember { mutableStateOf(Point(5, 5)) }
    var bonusFood by remember { mutableStateOf<Point?>(null) }
    var bonusTimer by remember { mutableIntStateOf(0) }
    var score by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    fun spawnFood(currentSnake: List<Point>): Point {
        val occupied = currentSnake.toSet()
        val emptyPoints = mutableListOf<Point>()
        for (x in 0 until GRID_WIDTH) {
            for (y in 0 until GRID_HEIGHT) {
                val p = Point(x, y)
                if (!occupied.contains(p)) emptyPoints.add(p)
            }
        }
        return if (emptyPoints.isNotEmpty()) emptyPoints[Random.nextInt(emptyPoints.size)] else Point(0, 0)
    }

    fun restartGame() {
        snake = listOf(Point(8, 10), Point(8, 11), Point(8, 12))
        direction = Direction.UP
        nextDirection = Direction.UP
        score = 0
        bonusFood = null
        bonusTimer = 0
        food = spawnFood(snake)
        isGameOver = false
        isPaused = false
    }

    // Game loop
    LaunchedEffect(isGameOver, isPaused) {
        while (!isGameOver && !isPaused) {
            val speed = (180 - (score * 2)).coerceAtLeast(80).toLong()
            delay(speed)

            direction = nextDirection
            val head = snake.first()
            val newHead = when (direction) {
                Direction.UP -> Point(head.x, head.y - 1)
                Direction.DOWN -> Point(head.x, head.y + 1)
                Direction.LEFT -> Point(head.x - 1, head.y)
                Direction.RIGHT -> Point(head.x + 1, head.y)
            }

            // Wall or self-collision
            if (newHead.x !in 0 until GRID_WIDTH ||
                newHead.y !in 0 until GRID_HEIGHT ||
                snake.contains(newHead)
            ) {
                HapticHelper.playGameOver(context)
                isGameOver = true
                onRecordScore(score)
                break
            }

            val newSnake = mutableListOf(newHead)
            var ate = false

            if (newHead == food) {
                ate = true
                score += 10
                HapticHelper.playScore(context)
                food = spawnFood(snake + newHead)

                // 20% chance to spawn bonus star
                if (bonusFood == null && Random.nextFloat() < 0.3f) {
                    bonusFood = spawnFood(snake + newHead + food)
                    bonusTimer = 40
                }
            } else if (newHead == bonusFood) {
                ate = true
                score += 30
                HapticHelper.playScore(context)
                bonusFood = null
                bonusTimer = 0
            }

            if (ate) {
                newSnake.addAll(snake)
            } else {
                newSnake.addAll(snake.dropLast(1))
            }
            snake = newSnake

            if (bonusTimer > 0) {
                bonusTimer--
                if (bonusTimer <= 0) bonusFood = null
            }
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

        // Game canvas arena with drag gestures
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF131127)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(GRID_WIDTH.toFloat() / GRID_HEIGHT.toFloat())
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {},
                            onDragCancel = {},
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val (dx, dy) = dragAmount
                                if (abs(dx) > abs(dy) && abs(dx) > 10) {
                                    if (dx > 0 && direction != Direction.LEFT) {
                                        nextDirection = Direction.RIGHT
                                    } else if (dx < 0 && direction != Direction.RIGHT) {
                                        nextDirection = Direction.LEFT
                                    }
                                } else if (abs(dy) > 10) {
                                    if (dy > 0 && direction != Direction.UP) {
                                        nextDirection = Direction.DOWN
                                    } else if (dy < 0 && direction != Direction.DOWN) {
                                        nextDirection = Direction.UP
                                    }
                                }
                            }
                        )
                    }
                    .testTag("snake_canvas")
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellWidth = size.width / GRID_WIDTH
                    val cellHeight = size.height / GRID_HEIGHT

                    // Draw subtle grid dots
                    for (gx in 0 until GRID_WIDTH) {
                        for (gy in 0 until GRID_HEIGHT) {
                            drawCircle(
                                color = Color(0x15FFFFFF),
                                radius = 1.dp.toPx(),
                                center = Offset((gx + 0.5f) * cellWidth, (gy + 0.5f) * cellHeight)
                            )
                        }
                    }

                    // Draw regular food (Apple)
                    drawRoundRect(
                        color = Color(0xFFEF4444),
                        topLeft = Offset(food.x * cellWidth + cellWidth * 0.15f, food.y * cellHeight + cellHeight * 0.15f),
                        size = Size(cellWidth * 0.7f, cellHeight * 0.7f),
                        cornerRadius = CornerRadius(cellWidth * 0.35f, cellHeight * 0.35f)
                    )

                    // Draw bonus food (Golden Star)
                    bonusFood?.let { bonus ->
                        drawRoundRect(
                            color = Color(0xFFFBBF24),
                            topLeft = Offset(bonus.x * cellWidth + cellWidth * 0.1f, bonus.y * cellHeight + cellHeight * 0.1f),
                            size = Size(cellWidth * 0.8f, cellHeight * 0.8f),
                            cornerRadius = CornerRadius(cellWidth * 0.4f, cellHeight * 0.4f)
                        )
                    }

                    // Draw snake body & head
                    snake.forEachIndexed { index, point ->
                        val isHead = index == 0
                        val color = if (isHead) Color(0xFF34D399) else Color(0xFF059669)
                        val padding = if (isHead) 0.05f else 0.1f
                        val corner = if (isHead) cellWidth * 0.4f else cellWidth * 0.25f

                        drawRoundRect(
                            color = color,
                            topLeft = Offset(
                                point.x * cellWidth + cellWidth * padding,
                                point.y * cellHeight + cellHeight * padding
                            ),
                            size = Size(cellWidth * (1f - 2 * padding), cellHeight * (1f - 2 * padding)),
                            cornerRadius = CornerRadius(corner, corner)
                        )

                        // Head eyes
                        if (isHead) {
                            val eyeColor = Color(0xFF064E3B)
                            val eyeRadius = cellWidth * 0.1f
                            val center = Offset(
                                (point.x + 0.5f) * cellWidth,
                                (point.y + 0.5f) * cellHeight
                            )
                            drawCircle(eyeColor, eyeRadius, center)
                        }
                    }
                }
            }
        }

        // On-screen tactile controls (D-Pad + Pause)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Speed / Length info
            Column {
                Text(
                    text = "Length: ${snake.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = if (bonusFood != null) "⭐ Bonus active!" else "Swipe or use D-Pad",
                    color = if (bonusFood != null) Color(0xFFFBBF24) else Color(0xFF94A3B8),
                    fontSize = 12.sp
                )
            }

            // Pause toggle
            IconButton(
                onClick = { isPaused = !isPaused },
                modifier = Modifier
                    .size(44.dp)
                    .background(Color(0xFF1E1B2E), CircleShape)
                    .testTag("snake_pause_button")
            ) {
                Icon(
                    imageVector = if (isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                    contentDescription = "Pause",
                    tint = Color.White
                )
            }

            // Sleek mini D-Pad
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = {
                        if (direction != Direction.DOWN) nextDirection = Direction.UP
                        HapticHelper.playClick(context)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                        .testTag("dpad_up")
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White)
                }
                Row {
                    IconButton(
                        onClick = {
                            if (direction != Direction.RIGHT) nextDirection = Direction.LEFT
                            HapticHelper.playClick(context)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                            .testTag("dpad_left")
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.size(44.dp))
                    IconButton(
                        onClick = {
                            if (direction != Direction.LEFT) nextDirection = Direction.RIGHT
                            HapticHelper.playClick(context)
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                            .testTag("dpad_right")
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = Color.White)
                    }
                }
                IconButton(
                    onClick = {
                        if (direction != Direction.UP) nextDirection = Direction.DOWN
                        HapticHelper.playClick(context)
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                        .testTag("dpad_down")
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
                }
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver,
        title = "Game Over!",
        subtitle = "You collected $score points!",
        score = score,
        highScore = highScore,
        scoreUnit = "pts",
        onRestart = { restartGame() },
        onHome = onBack
    )
}
