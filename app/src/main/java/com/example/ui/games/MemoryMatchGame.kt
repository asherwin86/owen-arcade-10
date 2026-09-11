package com.example.ui.games

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.graphicsLayer
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

private data class MemoryCard(
    val id: Int,
    val symbol: String,
    var isFaceUp: Boolean = false,
    var isMatched: Boolean = false
)

private val ICONS = listOf("🚀", "💎", "🍕", "🎸", "🦊", "⚡", "🍀", "👑")

@Composable
fun MemoryMatchGame(
    highScore: Int,
    onRecordScore: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val gameInfo = remember { GameRegistry.getGame("memory_cards") }

    fun generateCards(): List<MemoryCard> {
        val pairs = (ICONS + ICONS).shuffled()
        return pairs.mapIndexed { index, symbol ->
            MemoryCard(id = index, symbol = symbol)
        }
    }

    var cards by remember { mutableStateOf(generateCards()) }
    var firstFlippedIndex by remember { mutableStateOf<Int?>(null) }
    var secondFlippedIndex by remember { mutableStateOf<Int?>(null) }
    var moves by remember { mutableIntStateOf(0) }
    var pairsMatched by remember { mutableIntStateOf(0) }
    var isChecking by remember { mutableStateOf(false) }
    var isVictory by remember { mutableStateOf(false) }

    fun initGame() {
        cards = generateCards()
        firstFlippedIndex = null
        secondFlippedIndex = null
        moves = 0
        pairsMatched = 0
        isChecking = false
        isVictory = false
    }

    // Checking 2 flipped cards
    LaunchedEffect(firstFlippedIndex, secondFlippedIndex) {
        val first = firstFlippedIndex
        val second = secondFlippedIndex

        if (first != null && second != null) {
            isChecking = true
            delay(700)

            val updated = cards.map { it.copy() }
            if (updated[first].symbol == updated[second].symbol) {
                updated[first].isMatched = true
                updated[second].isMatched = true
                pairsMatched++
                HapticHelper.playScore(context)

                if (pairsMatched == 8) {
                    isVictory = true
                    val finalScore = (1000 - moves * 20).coerceAtLeast(100)
                    onRecordScore(finalScore)
                }
            } else {
                updated[first].isFaceUp = false
                updated[second].isFaceUp = false
                HapticHelper.playClick(context)
            }

            cards = updated
            firstFlippedIndex = null
            secondFlippedIndex = null
            isChecking = false
        }
    }

    fun onCardClick(index: Int) {
        if (isChecking || cards[index].isFaceUp || cards[index].isMatched) return

        val updated = cards.map { it.copy() }
        updated[index].isFaceUp = true
        cards = updated
        HapticHelper.playClick(context)

        if (firstFlippedIndex == null) {
            firstFlippedIndex = index
        } else if (secondFlippedIndex == null) {
            secondFlippedIndex = index
            moves++
        }
    }

    val currentScore = (1000 - moves * 20).coerceAtLeast(100)

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

        // Matches and Moves stats
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
                    Text("PAIRS MATCHED", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text("$pairsMatched / 8", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF34D399))
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text("MOVES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Text("$moves", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF38BDF8))
                }
            }
        }

        // 4x4 Grid
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF141226)),
                elevation = CardDefaults.cardElevation(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (row in 0 until 4) {
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            for (col in 0 until 4) {
                                val index = row * 4 + col
                                val card = cards[index]
                                val rotation by animateFloatAsState(
                                    targetValue = if (card.isFaceUp || card.isMatched) 180f else 0f,
                                    animationSpec = tween(durationMillis = 300),
                                    label = "card_flip"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxSize()
                                        .graphicsLayer {
                                            rotationY = rotation
                                            cameraDistance = 12f * density
                                        }
                                        .background(
                                            when {
                                                card.isMatched -> Color(0xFF10B981).copy(alpha = 0.3f)
                                                card.isFaceUp -> Color(0xFF2A274E)
                                                else -> Color(0xFF4338CA)
                                            },
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { onCardClick(index) }
                                        .testTag("memory_card_$index"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (rotation > 90f) {
                                        Text(
                                            text = card.symbol,
                                            fontSize = 28.sp,
                                            modifier = Modifier.graphicsLayer { rotationY = 180f }
                                        )
                                    } else {
                                        Text("❓", fontSize = 20.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    GameOverDialog(
        isOpen = isVictory,
        title = "Pairs Completed!",
        subtitle = "Completed in $moves moves!",
        score = currentScore,
        highScore = highScore,
        isWin = true,
        onRestart = { initGame() },
        onHome = onBack
    )
}
