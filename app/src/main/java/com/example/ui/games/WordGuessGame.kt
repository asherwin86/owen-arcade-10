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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.GameRegistry
import com.example.ui.components.ArcadeGameHeader
import com.example.ui.components.GameOverDialog
import com.example.ui.components.HapticHelper
import kotlin.random.Random

private val WORD_LIST = listOf(
    "APPLE", "BRAVE", "CLOUD", "DREAM", "EAGLE",
    "FLAME", "GHOST", "HEART", "LIGHT", "MUSIC",
    "OCEAN", "PIANO", "QUEEN", "RIVER", "SMILE",
    "TIGER", "WATER", "MAGIC", "STARS", "SPACE",
    "STORM", "POWER", "TRAIN", "HOUSE", "EARTH",
    "BEACH", "CHAIR", "PLANT", "SUGAR", "SWEET",
    "NIGHT", "STONE", "SHINE", "PRIDE", "SOLAR"
)

private enum class LetterStatus {
    EMPTY,
    TYPED,
    CORRECT, // Green
    PRESENT, // Yellow
    ABSENT   // Gray
}

private data class GuessLetter(
    val char: Char,
    val status: LetterStatus = LetterStatus.EMPTY
)

@Composable
fun WordGuessGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("word_guess") }

    var targetWord by remember { mutableStateOf(WORD_LIST[Random.nextInt(WORD_LIST.size)]) }
    var guesses by remember { mutableStateOf(List(6) { "" }) }
    var currentAttempt by remember { mutableIntStateOf(0) }
    var isGameOver by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }
    var totalWins by remember { mutableIntStateOf(highScore) }

    fun initGame() {
        targetWord = WORD_LIST[Random.nextInt(WORD_LIST.size)]
        guesses = List(6) { "" }
        currentAttempt = 0
        isGameOver = false
        isVictory = false
    }

    // Key status map (for coloring keyboard)
    val keyStatusMap = remember(guesses, currentAttempt) {
        val map = mutableMapOf<Char, LetterStatus>()
        for (i in 0 until currentAttempt) {
            val guess = guesses[i]
            for (j in guess.indices) {
                val c = guess[j]
                val currentStatus = map[c]
                val newStatus = when {
                    targetWord[j] == c -> LetterStatus.CORRECT
                    targetWord.contains(c) -> LetterStatus.PRESENT
                    else -> LetterStatus.ABSENT
                }
                // Upgrade status if higher priority (CORRECT > PRESENT > ABSENT)
                if (currentStatus != LetterStatus.CORRECT) {
                    map[c] = newStatus
                }
            }
        }
        map
    }

    fun onKeyPress(key: String) {
        if (isGameOver || isVictory) return
        val currentGuess = guesses[currentAttempt]

        when (key) {
            "ENTER" -> {
                if (currentGuess.length == 5) {
                    HapticHelper.playScore(context)
                    if (currentGuess == targetWord) {
                        isVictory = true
                        totalWins++
                        onRecordScore(totalWins)
                    } else if (currentAttempt >= 5) {
                        isGameOver = true
                        HapticHelper.playGameOver(context)
                    } else {
                        currentAttempt++
                    }
                }
            }
            "DEL" -> {
                if (currentGuess.isNotEmpty()) {
                    val updated = guesses.toMutableList()
                    updated[currentAttempt] = currentGuess.dropLast(1)
                    guesses = updated
                    HapticHelper.playClick(context)
                }
            }
            else -> {
                if (currentGuess.length < 5) {
                    val updated = guesses.toMutableList()
                    updated[currentAttempt] = currentGuess + key
                    guesses = updated
                    HapticHelper.playClick(context)
                }
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
            score = totalWins,
            highScore = maxOf(totalWins, highScore),
            onBack = onBack,
            onRestart = { initGame() }
        )

        // 6x5 Grid
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            for (row in 0 until 6) {
                val guess = guesses[row]
                val isSubmitted = row < currentAttempt || (row == currentAttempt && (isVictory || isGameOver))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (col in 0 until 5) {
                        val char = if (col < guess.length) guess[col] else ' '
                        val status = if (isSubmitted && col < guess.length) {
                            when {
                                targetWord[col] == char -> LetterStatus.CORRECT
                                targetWord.contains(char) -> LetterStatus.PRESENT
                                else -> LetterStatus.ABSENT
                            }
                        } else if (char != ' ') {
                            LetterStatus.TYPED
                        } else {
                            LetterStatus.EMPTY
                        }

                        val bgColor = when (status) {
                            LetterStatus.CORRECT -> Color(0xFF10B981) // Green
                            LetterStatus.PRESENT -> Color(0xFFEAB308) // Yellow
                            LetterStatus.ABSENT -> Color(0xFF334155)  // Dark gray
                            LetterStatus.TYPED -> Color(0xFF1E1B2E)
                            LetterStatus.EMPTY -> Color(0xFF141224)
                        }

                        val borderColor = when (status) {
                            LetterStatus.TYPED -> Color(0xFF818CF8)
                            LetterStatus.EMPTY -> Color(0xFF2E2B4A)
                            else -> Color.Transparent
                        }

                        Card(
                            shape = RoundedCornerShape(8.dp),
                            colors = CardDefaults.cardColors(containerColor = bgColor),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, borderColor),
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (char != ' ') "$char" else "",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }

        // On-screen QWERTY Keyboard
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val rows = listOf(
                listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
                listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
                listOf("ENTER", "Z", "X", "C", "V", "B", "N", "M", "DEL")
            )

            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isWide = key == "ENTER" || key == "DEL"
                        val char = key.firstOrNull()
                        val status = if (!isWide && char != null) keyStatusMap[char] else null

                        val keyBg = when (status) {
                            LetterStatus.CORRECT -> Color(0xFF10B981)
                            LetterStatus.PRESENT -> Color(0xFFEAB308)
                            LetterStatus.ABSENT -> Color(0xFF334155)
                            else -> Color(0xFF1E1B2E)
                        }

                        Box(
                            modifier = Modifier
                                .weight(if (isWide) 1.5f else 1f)
                                .height(44.dp)
                                .background(keyBg, RoundedCornerShape(6.dp))
                                .clickable { onKeyPress(key) }
                                .testTag("key_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (key == "DEL") "⌫" else key,
                                fontSize = if (isWide) 11.sp else 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }

    GameOverDialog(
        isOpen = isGameOver || isVictory,
        title = if (isVictory) "Brilliant! You Guessed It!" else "Out of Guesses!",
        subtitle = "The secret word was: $targetWord",
        score = totalWins,
        highScore = highScore,
        scoreUnit = "wins",
        isWin = isVictory,
        onRestart = { initGame() },
        onHome = onBack
    )
}
