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

enum class TTTOpponent { AI, TWO_PLAYER }

@Composable
fun TicTacToeGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("tic_tac_toe") }

    var board by remember { mutableStateOf(List(9) { "" }) }
    var isXTurn by remember { mutableStateOf(true) }
    var opponentMode by remember { mutableStateOf(TTTOpponent.AI) }
    var xWins by remember { mutableIntStateOf(0) }
    var oWins by remember { mutableIntStateOf(0) }
    var draws by remember { mutableIntStateOf(0) }

    var winningLine by remember { mutableStateOf<List<Int>?>(null) }
    var isGameOver by remember { mutableStateOf(false) }
    var winner by remember { mutableStateOf<String?>(null) }

    fun checkWinner(b: List<String>): Pair<String?, List<Int>?> {
        val lines = listOf(
            listOf(0, 1, 2), listOf(3, 4, 5), listOf(6, 7, 8),
            listOf(0, 3, 6), listOf(1, 4, 7), listOf(2, 5, 8),
            listOf(0, 4, 8), listOf(2, 4, 6)
        )
        for (line in lines) {
            val (i1, i2, i3) = line
            if (b[i1].isNotEmpty() && b[i1] == b[i2] && b[i2] == b[i3]) {
                return Pair(b[i1], line)
            }
        }
        if (b.all { it.isNotEmpty() }) {
            return Pair("DRAW", null)
        }
        return Pair(null, null)
    }

    fun initRound() {
        board = List(9) { "" }
        isXTurn = true
        winningLine = null
        isGameOver = false
        winner = null
    }

    fun makeAIMove() {
        val emptySlots = board.indices.filter { board[it].isEmpty() }
        if (emptySlots.isEmpty()) return

        // 1. Check if AI can win on next move
        for (slot in emptySlots) {
            val copy = board.toMutableList()
            copy[slot] = "O"
            if (checkWinner(copy).first == "O") {
                val updated = board.toMutableList()
                updated[slot] = "O"
                board = updated
                return
            }
        }

        // 2. Block player X from winning
        for (slot in emptySlots) {
            val copy = board.toMutableList()
            copy[slot] = "X"
            if (checkWinner(copy).first == "X") {
                val updated = board.toMutableList()
                updated[slot] = "O"
                board = updated
                return
            }
        }

        // 3. Take center if available
        if (board[4].isEmpty()) {
            val updated = board.toMutableList()
            updated[4] = "O"
            board = updated
            return
        }

        // 4. Random empty slot
        val choice = emptySlots[Random.nextInt(emptySlots.size)]
        val updated = board.toMutableList()
        updated[choice] = "O"
        board = updated
    }

    // AI move effect
    LaunchedEffect(isXTurn, opponentMode, isGameOver) {
        if (!isXTurn && opponentMode == TTTOpponent.AI && !isGameOver) {
            delay(400)
            makeAIMove()
            val (win, line) = checkWinner(board)
            if (win != null) {
                winner = win
                winningLine = line
                isGameOver = true
                if (win == "O") oWins++ else if (win == "DRAW") draws++
                HapticHelper.playGameOver(context)
            } else {
                isXTurn = true
            }
        }
    }

    fun onCellClick(index: Int) {
        if (board[index].isNotEmpty() || isGameOver) return
        if (opponentMode == TTTOpponent.AI && !isXTurn) return

        val updated = board.toMutableList()
        val mark = if (isXTurn) "X" else "O"
        updated[index] = mark
        board = updated
        HapticHelper.playClick(context)

        val (win, line) = checkWinner(updated)
        if (win != null) {
            winner = win
            winningLine = line
            isGameOver = true
            if (win == "X") {
                xWins++
                onRecordScore(xWins)
                HapticHelper.playScore(context)
            } else if (win == "O") {
                oWins++
                HapticHelper.playGameOver(context)
            } else {
                draws++
            }
        } else {
            isXTurn = !isXTurn
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
            score = xWins,
            highScore = maxOf(xWins, highScore),
            onBack = onBack,
            onRestart = { initRound() }
        )

        // Opponent Mode Toggle
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
                            .background(
                                if (opponentMode == TTTOpponent.AI) Color(0xFF6366F1) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                opponentMode = TTTOpponent.AI
                                initRound()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("🤖 vs AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                if (opponentMode == TTTOpponent.TWO_PLAYER) Color(0xFF6366F1) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable {
                                opponentMode = TTTOpponent.TWO_PLAYER
                                initRound()
                            }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("👥 2 Players", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }

        // Tally Card (X Wins, Draws, O Wins)
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161427)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("PLAYER X", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                    Text("$xWins", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("DRAWS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text("$draws", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (opponentMode == TTTOpponent.AI) "AI (O)" else "PLAYER O",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFF43F5E)
                    )
                    Text("$oWins", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                }
            }
        }

        // Current Turn Indicator
        Text(
            text = when {
                isGameOver -> if (winner == "DRAW") "Game Tied!" else "$winner Won!"
                isXTurn -> "Player X's Turn"
                opponentMode == TTTOpponent.AI -> "AI Thinking..."
                else -> "Player O's Turn"
            },
            color = if (isXTurn) Color(0xFF38BDF8) else Color(0xFFF43F5E),
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(vertical = 8.dp)
        )

        // 3x3 Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1A172F)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until 3) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 3) {
                                val index = row * 3 + col
                                val value = board[index]
                                val isWinningCell = winningLine?.contains(index) == true

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .background(
                                            if (isWinningCell) Color(0xFF10B981).copy(alpha = 0.4f)
                                            else Color(0xFF262340),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onCellClick(index) }
                                        .testTag("ttt_cell_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = value,
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (value == "X") Color(0xFF38BDF8) else Color(0xFFF43F5E)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver,
        title = if (winner == "DRAW") "Game Tied!" else "$winner Takes the Round!",
        subtitle = if (winner == "X") "Congratulations Player X!" else if (winner == "O") "Player O wins this game!" else "Evenly matched!",
        score = xWins,
        highScore = highScore,
        scoreUnit = "wins",
        isWin = winner == "X",
        onRestart = { initRound() },
        onHome = onBack
    )
}
