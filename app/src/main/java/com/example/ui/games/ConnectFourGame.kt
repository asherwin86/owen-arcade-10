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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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

private const val COLS = 7
private const val ROWS = 6

@Composable
fun ConnectFourGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("connect_four") }

    // 0 = empty, 1 = Red (Player 1), 2 = Yellow (Player 2 / AI)
    var board by remember { mutableStateOf(Array(ROWS) { IntArray(COLS) }) }
    var isRedTurn by remember { mutableStateOf(true) }
    var vsAI by remember { mutableStateOf(true) }
    var redWins by remember { mutableIntStateOf(0) }
    var yellowWins by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<Int?>(null) } // 1, 2, or 0 (draw)
    var winningSlots by remember { mutableStateOf<Set<Pair<Int, Int>>>(emptySet()) }

    fun checkWin(b: Array<IntArray>): Pair<Int?, Set<Pair<Int, Int>>> {
        // Horizontal
        for (r in 0 until ROWS) {
            for (c in 0..COLS - 4) {
                val p = b[r][c]
                if (p != 0 && p == b[r][c + 1] && p == b[r][c + 2] && p == b[r][c + 3]) {
                    return Pair(p, setOf(Pair(r, c), Pair(r, c + 1), Pair(r, c + 2), Pair(r, c + 3)))
                }
            }
        }
        // Vertical
        for (r in 0..ROWS - 4) {
            for (c in 0 until COLS) {
                val p = b[r][c]
                if (p != 0 && p == b[r + 1][c] && p == b[r + 2][c] && p == b[r + 3][c]) {
                    return Pair(p, setOf(Pair(r, c), Pair(r + 1, c), Pair(r + 2, c), Pair(r + 3, c)))
                }
            }
        }
        // Diagonal Down-Right
        for (r in 0..ROWS - 4) {
            for (c in 0..COLS - 4) {
                val p = b[r][c]
                if (p != 0 && p == b[r + 1][c + 1] && p == b[r + 2][c + 2] && p == b[r + 3][c + 3]) {
                    return Pair(p, setOf(Pair(r, c), Pair(r + 1, c + 1), Pair(r + 2, c + 2), Pair(r + 3, c + 3)))
                }
            }
        }
        // Diagonal Up-Right
        for (r in 3 until ROWS) {
            for (c in 0..COLS - 4) {
                val p = b[r][c]
                if (p != 0 && p == b[r - 1][c + 1] && p == b[r - 2][c + 2] && p == b[r - 3][c + 3]) {
                    return Pair(p, setOf(Pair(r, c), Pair(r - 1, c + 1), Pair(r - 2, c + 2), Pair(r - 3, c + 3)))
                }
            }
        }
        // Check Draw
        val isFull = b[0].all { it != 0 }
        if (isFull) return Pair(0, emptySet())

        return Pair(null, emptySet())
    }

    fun initGame() {
        board = Array(ROWS) { IntArray(COLS) }
        isRedTurn = true
        isGameOver = false
        winner = null
        winningSlots = emptySet()
    }

    fun dropDisc(col: Int, player: Int): Boolean {
        // Find bottom-most empty slot in col
        for (r in ROWS - 1 downTo 0) {
            if (board[r][col] == 0) {
                val updated = Array(ROWS) { rIdx -> board[rIdx].clone() }
                updated[r][col] = player
                board = updated

                val (win, slots) = checkWin(updated)
                if (win != null) {
                    winner = win
                    winningSlots = slots
                    isGameOver = true
                    if (win == 1) {
                        redWins++
                        onRecordScore(redWins)
                        HapticHelper.playScore(context)
                    } else if (win == 2) {
                        yellowWins++
                        HapticHelper.playGameOver(context)
                    }
                } else {
                    isRedTurn = !isRedTurn
                }
                return true
            }
        }
        return false
    }

    // AI Move logic
    LaunchedEffect(isRedTurn, vsAI, isGameOver) {
        if (!isRedTurn && vsAI && !isGameOver) {
            delay(500)
            val availableCols = (0 until COLS).filter { board[0][it] == 0 }
            if (availableCols.isNotEmpty()) {
                // 1. Check if AI can win immediately
                var chosenCol = availableCols.firstOrNull { col ->
                    val testB = Array(ROWS) { r -> board[r].clone() }
                    for (r in ROWS - 1 downTo 0) {
                        if (testB[r][col] == 0) {
                            testB[r][col] = 2
                            break
                        }
                    }
                    checkWin(testB).first == 2
                }

                // 2. Block Red from winning
                if (chosenCol == null) {
                    chosenCol = availableCols.firstOrNull { col ->
                        val testB = Array(ROWS) { r -> board[r].clone() }
                        for (r in ROWS - 1 downTo 0) {
                            if (testB[r][col] == 0) {
                                testB[r][col] = 1
                                break
                            }
                        }
                        checkWin(testB).first == 1
                    }
                }

                // 3. Prefer center column
                if (chosenCol == null && availableCols.contains(3)) {
                    chosenCol = 3
                }

                // 4. Random choice
                if (chosenCol == null) {
                    chosenCol = availableCols[Random.nextInt(availableCols.size)]
                }

                dropDisc(chosenCol, 2)
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
            score = redWins,
            highScore = maxOf(redWins, highScore),
            onBack = onBack,
            onRestart = { initGame() }
        )

        // Game Mode Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B2E))
            ) {
                Row(modifier = Modifier.padding(4.dp)) {
                    Box(
                        modifier = Modifier
                            .background(if (vsAI) Color(0xFF6366F1) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable {
                                vsAI = true
                                initGame()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("🤖 vs AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Box(
                        modifier = Modifier
                            .background(if (!vsAI) Color(0xFF6366F1) else Color.Transparent, RoundedCornerShape(8.dp))
                            .clickable {
                                vsAI = false
                                initGame()
                            }
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text("👥 2 Players", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Player Turn Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp).background(Color(0xFFEF4444), CircleShape))
                Text(
                    " Red: $redWins",
                    color = Color.White,
                    fontWeight = if (isRedTurn) FontWeight.ExtraBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }

            Text(
                text = when {
                    isGameOver -> if (winner == 0) "Draw!" else if (winner == 1) "Red Wins!" else "Yellow Wins!"
                    isRedTurn -> "🔴 Red's Turn"
                    vsAI -> "🤖 AI Thinking..."
                    else -> "🟡 Yellow's Turn"
                },
                color = if (isRedTurn) Color(0xFFEF4444) else Color(0xFFFBBF24),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(14.dp).background(Color(0xFFFBBF24), CircleShape))
                Text(
                    " ${if (vsAI) "AI" else "Yellow"}: $yellowWins",
                    color = Color.White,
                    fontWeight = if (!isRedTurn) FontWeight.ExtraBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }

        // 7x6 Board
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E3A8A)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(COLS.toFloat() / ROWS.toFloat())
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                ) {
                    for (c in 0 until COLS) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize()
                                .clickable(enabled = !isGameOver && (isRedTurn || !vsAI)) {
                                    val success = dropDisc(c, if (isRedTurn) 1 else 2)
                                    if (success) HapticHelper.playClick(context)
                                }
                                .testTag("col_$c"),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (r in 0 until ROWS) {
                                val slotVal = board[r][c]
                                val isWinSlot = winningSlots.contains(Pair(r, c))

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth()
                                        .aspectRatio(1f)
                                        .background(
                                            when (slotVal) {
                                                1 -> Color(0xFFEF4444)
                                                2 -> Color(0xFFFBBF24)
                                                else -> Color(0xFF0F172A)
                                            },
                                            CircleShape
                                        )
                                        .then(
                                            if (isWinSlot) {
                                                Modifier.background(Color.White.copy(alpha = 0.3f), CircleShape)
                                            } else Modifier
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isWinSlot) {
                                        Text("⭐", fontSize = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "Tap any column to drop a disc",
            color = Color(0xFF94A3B8),
            fontSize = 12.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 12.dp)
        )
    }

    GameOverDialog(
        isOpen = isGameOver,
        title = if (winner == 1) "Red Connected 4!" else if (winner == 2) "Yellow Connected 4!" else "Grid is Full!",
        subtitle = if (winner == 1) "Superb strategy victory!" else if (winner == 2) "Yellow takes the round!" else "Tied game!",
        score = redWins,
        highScore = highScore,
        scoreUnit = "wins",
        isWin = winner == 1,
        onRestart = { initGame() },
        onHome = onBack
    )
}
