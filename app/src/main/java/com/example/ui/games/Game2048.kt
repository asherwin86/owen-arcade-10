package com.example.ui.games

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import kotlin.math.abs
import kotlin.random.Random

private const val GRID_SIZE = 4

@Composable
fun Game2048(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("2048") }

    var board by remember { mutableStateOf(Array(GRID_SIZE) { IntArray(GRID_SIZE) }) }
    var previousBoard by remember { mutableStateOf<Array<IntArray>?>(null) }
    var score by remember { mutableIntStateOf(0) }
    var previousScore by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var hasReached2048 by remember { mutableStateOf(false) }

    fun copyBoard(source: Array<IntArray>): Array<IntArray> =
        Array(GRID_SIZE) { r -> source[r].clone() }

    fun addRandomTile(targetBoard: Array<IntArray>): Boolean {
        val emptySlots = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (targetBoard[r][c] == 0) emptySlots.add(Pair(r, c))
            }
        }
        if (emptySlots.isEmpty()) return false
        val (r, c) = emptySlots[Random.nextInt(emptySlots.size)]
        targetBoard[r][c] = if (Random.nextFloat() < 0.9f) 2 else 4
        return true
    }

    fun isMovePossible(b: Array<IntArray>): Boolean {
        for (r in 0 until GRID_SIZE) {
            for (c in 0 until GRID_SIZE) {
                if (b[r][c] == 0) return true
                if (c + 1 < GRID_SIZE && b[r][c] == b[r][c + 1]) return true
                if (r + 1 < GRID_SIZE && b[r][c] == b[r + 1][c]) return true
            }
        }
        return false
    }

    fun initGame() {
        val newBoard = Array(GRID_SIZE) { IntArray(GRID_SIZE) }
        addRandomTile(newBoard)
        addRandomTile(newBoard)
        board = newBoard
        previousBoard = null
        score = 0
        previousScore = 0
        isGameOver = false
        hasReached2048 = false
    }

    remember {
        initGame()
        true
    }

    fun move(direction: Int) { // 0: Up, 1: Right, 2: Down, 3: Left
        val currentCopy = copyBoard(board)
        var moved = false
        var gainedScore = 0

        val nextBoard = copyBoard(board)

        // Slide logic helper
        fun processRow(row: IntArray): Pair<IntArray, Int> {
            val nonZeros = row.filter { it != 0 }.toMutableList()
            val result = IntArray(GRID_SIZE)
            var points = 0
            var writeIndex = 0
            var i = 0
            while (i < nonZeros.size) {
                if (i + 1 < nonZeros.size && nonZeros[i] == nonZeros[i + 1]) {
                    val merged = nonZeros[i] * 2
                    result[writeIndex++] = merged
                    points += merged
                    if (merged == 2048) hasReached2048 = true
                    i += 2
                } else {
                    result[writeIndex++] = nonZeros[i]
                    i += 1
                }
            }
            return Pair(result, points)
        }

        when (direction) {
            0 -> { // UP
                for (c in 0 until GRID_SIZE) {
                    val col = IntArray(GRID_SIZE) { r -> nextBoard[r][c] }
                    val (newCol, pts) = processRow(col)
                    gainedScore += pts
                    for (r in 0 until GRID_SIZE) {
                        if (nextBoard[r][c] != newCol[r]) moved = true
                        nextBoard[r][c] = newCol[r]
                    }
                }
            }
            1 -> { // RIGHT
                for (r in 0 until GRID_SIZE) {
                    val row = IntArray(GRID_SIZE) { c -> nextBoard[r][GRID_SIZE - 1 - c] }
                    val (newRow, pts) = processRow(row)
                    gainedScore += pts
                    for (c in 0 until GRID_SIZE) {
                        val targetCol = GRID_SIZE - 1 - c
                        if (nextBoard[r][targetCol] != newRow[c]) moved = true
                        nextBoard[r][targetCol] = newRow[c]
                    }
                }
            }
            2 -> { // DOWN
                for (c in 0 until GRID_SIZE) {
                    val col = IntArray(GRID_SIZE) { r -> nextBoard[GRID_SIZE - 1 - r][c] }
                    val (newCol, pts) = processRow(col)
                    gainedScore += pts
                    for (r in 0 until GRID_SIZE) {
                        val targetRow = GRID_SIZE - 1 - r
                        if (nextBoard[targetRow][c] != newCol[r]) moved = true
                        nextBoard[targetRow][c] = newCol[r]
                    }
                }
            }
            3 -> { // LEFT
                for (r in 0 until GRID_SIZE) {
                    val row = nextBoard[r].clone()
                    val (newRow, pts) = processRow(row)
                    gainedScore += pts
                    for (c in 0 until GRID_SIZE) {
                        if (nextBoard[r][c] != newRow[c]) moved = true
                        nextBoard[r][c] = newRow[c]
                    }
                }
            }
        }

        if (moved) {
            previousBoard = currentCopy
            previousScore = score
            addRandomTile(nextBoard)
            board = nextBoard
            score += gainedScore
            HapticHelper.playScore(context)

            if (!isMovePossible(board)) {
                isGameOver = true
                HapticHelper.playGameOver(context)
                onRecordScore(score)
            }
        }
    }

    fun undoMove() {
        previousBoard?.let {
            board = copyBoard(it)
            score = previousScore
            previousBoard = null
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
            onRestart = { initGame() }
        )

        // 4x4 Grid Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .pointerInput(Unit) {
                        var totalDx = 0f
                        var totalDy = 0f
                        detectDragGestures(
                            onDragStart = {
                                totalDx = 0f
                                totalDy = 0f
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                totalDx += dragAmount.x
                                totalDy += dragAmount.y
                            },
                            onDragEnd = {
                                if (abs(totalDx) > abs(totalDy) && abs(totalDx) > 40) {
                                    if (totalDx > 0) move(1) else move(3)
                                } else if (abs(totalDy) > 40) {
                                    if (totalDy > 0) move(2) else move(0)
                                }
                            }
                        )
                    }
                    .testTag("2048_board")
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (r in 0 until GRID_SIZE) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (c in 0 until GRID_SIZE) {
                                val value = board[r][c]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(
                                            getTileColor(value),
                                            RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (value > 0) {
                                        Text(
                                            text = "$value",
                                            fontSize = when {
                                                value >= 1024 -> 18.sp
                                                value >= 128 -> 22.sp
                                                else -> 26.sp
                                            },
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (value <= 4) Color(0xFF1E1B2E) else Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Bottom Controls: Undo & Directional Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Undo button
            IconButton(
                onClick = { undoMove() },
                enabled = previousBoard != null,
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (previousBoard != null) Color(0xFF6366F1) else Color(0xFF1E1B2E),
                        CircleShape
                    )
                    .testTag("2048_undo")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Undo,
                    contentDescription = "Undo",
                    tint = if (previousBoard != null) Color.White else Color(0xFF64748B)
                )
            }

            // D-Pad
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(
                    onClick = { move(0) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                        .testTag("2048_up")
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Up", tint = Color.White)
                }
                Row {
                    IconButton(
                        onClick = { move(3) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                            .testTag("2048_left")
                    ) {
                        Icon(Icons.Default.KeyboardArrowLeft, contentDescription = "Left", tint = Color.White)
                    }
                    Spacer(modifier = Modifier.size(44.dp))
                    IconButton(
                        onClick = { move(1) },
                        modifier = Modifier
                            .size(44.dp)
                            .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                            .testTag("2048_right")
                    ) {
                        Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Right", tint = Color.White)
                    }
                }
                IconButton(
                    onClick = { move(2) },
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFF1E1B2E), RoundedCornerShape(12.dp))
                        .testTag("2048_down")
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Down", tint = Color.White)
                }
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver,
        title = if (hasReached2048) "Victory! 2048!" else "Game Over",
        subtitle = if (hasReached2048) "You reached 2048!" else "No more valid moves!",
        score = score,
        highScore = highScore,
        isWin = hasReached2048,
        onRestart = { initGame() },
        onHome = onBack
    )
}

private fun getTileColor(value: Int): Color {
    return when (value) {
        0 -> Color(0xFF28253E)
        2 -> Color(0xFFEEE4DA)
        4 -> Color(0xFFEDE0C8)
        8 -> Color(0xFFF2B179)
        16 -> Color(0xFFF59563)
        32 -> Color(0xFFF67C5F)
        64 -> Color(0xFFF65E3B)
        128 -> Color(0xFFEDCF72)
        256 -> Color(0xFFEDCC61)
        512 -> Color(0xFFEDC850)
        1024 -> Color(0xFFEDC53F)
        2048 -> Color(0xFFEDC22E)
        else -> Color(0xFF3C3A32)
    }
}
