package com.example.ui.games

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
import androidx.compose.material3.Icon
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

private const val COLS = 8
private const val ROWS = 10
private const val TOTAL_MINES = 12

private data class Cell(
    val r: Int,
    val c: Int,
    var isMine: Boolean = false,
    var isRevealed: Boolean = false,
    var isFlagged: Boolean = false,
    var neighborMines: Int = 0
)

@Composable
fun MinesweeperGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("minesweeper") }

    var grid by remember {
        mutableStateOf(List(ROWS) { r -> List(COLS) { c -> Cell(r, c) } })
    }
    var firstClickDone by remember { mutableStateOf(false) }
    var flagMode by remember { mutableStateOf(false) }
    var flagsCount by remember { mutableIntStateOf(0) }
    var timerSeconds by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var faceEmoji by remember { mutableStateOf("😊") }

    fun initGame() {
        grid = List(ROWS) { r -> List(COLS) { c -> Cell(r, c) } }
        firstClickDone = false
        flagsCount = 0
        timerSeconds = 0
        isGameOver = false
        isVictory = false
        faceEmoji = "😊"
    }

    LaunchedEffect(firstClickDone, isGameOver, isVictory) {
        if (firstClickDone && !isGameOver && !isVictory) {
            while (true) {
                delay(1000)
                timerSeconds++
            }
        }
    }

    fun placeMinesAndNeighbors(firstR: Int, firstC: Int) {
        val allCoords = mutableListOf<Pair<Int, Int>>()
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                if (r != firstR || c != firstC) {
                    allCoords.add(Pair(r, c))
                }
            }
        }
        allCoords.shuffle()
        val mineCoords = allCoords.take(TOTAL_MINES).toSet()

        val newGrid = List(ROWS) { r ->
            List(COLS) { c ->
                val isM = mineCoords.contains(Pair(r, c))
                Cell(r, c, isMine = isM)
            }
        }

        // Count neighbors
        for (r in 0 until ROWS) {
            for (c in 0 until COLS) {
                if (!newGrid[r][c].isMine) {
                    var count = 0
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            val nr = r + dr
                            val nc = c + dc
                            if (nr in 0 until ROWS && nc in 0 until COLS && newGrid[nr][nc].isMine) {
                                count++
                            }
                        }
                    }
                    newGrid[r][c].neighborMines = count
                }
            }
        }
        grid = newGrid
    }

    fun revealCell(r: Int, c: Int) {
        val target = grid[r][c]
        if (target.isRevealed || target.isFlagged) return

        if (!firstClickDone) {
            placeMinesAndNeighbors(r, c)
            firstClickDone = true
        }

        val updated = grid.map { row -> row.map { it.copy() } }

        if (updated[r][c].isMine) {
            // Hit mine! Reveal all mines
            for (row in updated) {
                for (cell in row) {
                    if (cell.isMine) cell.isRevealed = true
                }
            }
            grid = updated
            faceEmoji = "💥"
            isGameOver = true
            HapticHelper.playGameOver(context)
            return
        }

        // Flood reveal
        val queue = ArrayDeque<Pair<Int, Int>>()
        queue.add(Pair(r, c))
        updated[r][c].isRevealed = true

        while (queue.isNotEmpty()) {
            val (currR, currC) = queue.removeFirst()
            val cell = updated[currR][currC]

            if (cell.neighborMines == 0) {
                for (dr in -1..1) {
                    for (dc in -1..1) {
                        val nr = currR + dr
                        val nc = currC + dc
                        if (nr in 0 until ROWS && nc in 0 until COLS) {
                            val neighbor = updated[nr][nc]
                            if (!neighbor.isRevealed && !neighbor.isMine && !neighbor.isFlagged) {
                                neighbor.isRevealed = true
                                if (neighbor.neighborMines == 0) {
                                    queue.add(Pair(nr, nc))
                                }
                            }
                        }
                    }
                }
            }
        }

        grid = updated
        HapticHelper.playScore(context)

        // Check victory
        var revealedCount = 0
        for (row in updated) {
            for (cell in row) {
                if (cell.isRevealed && !cell.isMine) revealedCount++
            }
        }

        if (revealedCount == (ROWS * COLS - TOTAL_MINES)) {
            isVictory = true
            faceEmoji = "😎"
            HapticHelper.playScore(context)
            val finalScore = (1000 - timerSeconds * 2).coerceAtLeast(100)
            onRecordScore(finalScore)
        }
    }

    fun toggleFlag(r: Int, c: Int) {
        val target = grid[r][c]
        if (target.isRevealed) return
        val updated = grid.map { row -> row.map { it.copy() } }
        val newFlag = !updated[r][c].isFlagged
        updated[r][c].isFlagged = newFlag
        grid = updated
        flagsCount += if (newFlag) 1 else -1
        HapticHelper.playClick(context)
    }

    val revealedSafeCount = grid.sumOf { row -> row.count { it.isRevealed && !it.isMine } }
    val currentScore = if (isVictory) (1000 - timerSeconds * 2).coerceAtLeast(100) else revealedSafeCount

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0C20))
            .navigationBarsPadding()
    ) {
        ArcadeGameHeader(
            game = gameInfo,
            score = currentScore,
            highScore = maxOf(currentScore, highScore),
            onBack = onBack,
            onRestart = { initGame() }
        )

        // Status bar: Mines Left, Emoji Face, Timer
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💣", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${(TOTAL_MINES - flagsCount).coerceAtLeast(0)}",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444),
                        fontSize = 18.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF2D2A4A), CircleShape)
                        .clickable { initGame() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(faceEmoji, fontSize = 20.sp)
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("⏱️", fontSize = 18.sp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "${timerSeconds}s",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF38BDF8),
                        fontSize = 18.sp
                    )
                }
            }
        }

        // Minesweeper Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF161426)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COLS.toFloat() / ROWS.toFloat())
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    for (r in 0 until ROWS) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            for (c in 0 until COLS) {
                                val cell = grid[r][c]
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(
                                            when {
                                                cell.isRevealed && cell.isMine -> Color(0xFFDC2626)
                                                cell.isRevealed -> Color(0xFF2A2744)
                                                else -> Color(0xFF3B3765)
                                            },
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable(enabled = !isGameOver && !isVictory) {
                                            if (flagMode) {
                                                toggleFlag(r, c)
                                            } else {
                                                revealCell(r, c)
                                            }
                                        }
                                        .testTag("cell_${r}_$c"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (cell.isRevealed) {
                                        if (cell.isMine) {
                                            Text("💣", fontSize = 16.sp)
                                        } else if (cell.neighborMines > 0) {
                                            Text(
                                                text = "${cell.neighborMines}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = getNumberColor(cell.neighborMines)
                                            )
                                        }
                                    } else if (cell.isFlagged) {
                                        Text("🚩", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Mode Switcher Button (Dig vs Flag)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    flagMode = false
                    HapticHelper.playClick(context)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("dig_mode_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (!flagMode) Color(0xFF6366F1) else Color(0xFF1E1B2E)
                )
            ) {
                Text("⛏️ Dig / Reveal", color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = {
                    flagMode = true
                    HapticHelper.playClick(context)
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("flag_mode_button"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (flagMode) Color(0xFFEF4444) else Color(0xFF1E1B2E)
                )
            ) {
                Text("🚩 Flag Mode", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver || isVictory,
        title = if (isVictory) "Minefield Cleared!" else "Detonated!",
        subtitle = if (isVictory) "Cleared in ${timerSeconds}s! Fantastic work!" else "You hit a hidden mine.",
        score = currentScore,
        highScore = highScore,
        scoreUnit = if (isVictory) "pts" else "safe",
        isWin = isVictory,
        onRestart = { initGame() },
        onHome = onBack
    )
}

private fun getNumberColor(number: Int): Color {
    return when (number) {
        1 -> Color(0xFF60A5FA)
        2 -> Color(0xFF4ADE80)
        3 -> Color(0xFFF87171)
        4 -> Color(0xFFA78BFA)
        5 -> Color(0xFFFB923C)
        6 -> Color(0xFF38BDF8)
        7 -> Color(0xFFF472B6)
        else -> Color(0xFF94A3B8)
    }
}
