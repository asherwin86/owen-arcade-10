package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ArcadeHomeScreen
import com.example.audio.AudioPlayer
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.example.ui.ArcadeViewModel
import com.example.ui.components.ArcadeLoadingOverlay
import com.example.ui.games.BrickBreakerGame
import com.example.ui.games.ConnectFourGame
import com.example.ui.games.FlappyBirdGame
import com.example.ui.games.Game2048
import com.example.ui.games.MemoryMatchGame
import com.example.ui.games.MinesweeperGame
import com.example.ui.games.SnakeGame
import com.example.ui.games.TicTacToeGame
import com.example.ui.games.WhackAMoleGame
import com.example.ui.games.WordGuessGame
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ArcadeApp()
            }
        }
    }
}

@Composable
fun ArcadeApp(arcadeViewModel: ArcadeViewModel = viewModel()) {
    val isLoading by arcadeViewModel.isLoading.collectAsStateWithLifecycle()
    val selectedGameId by arcadeViewModel.selectedGameId.collectAsStateWithLifecycle()
    val highScores by arcadeViewModel.highScores.collectAsStateWithLifecycle()
    val selectedCategory by arcadeViewModel.selectedCategory.collectAsStateWithLifecycle()
    
    val isMusicGenerating by arcadeViewModel.isMusicGenerating.collectAsStateWithLifecycle()
    val generatedMusicBase64 by arcadeViewModel.generatedMusicBase64.collectAsStateWithLifecycle()
    val context = LocalContext.current
    
    val audioPlayer = remember { AudioPlayer(context) }
    
    DisposableEffect(Unit) {
        onDispose {
            audioPlayer.stop()
        }
    }
    
    LaunchedEffect(generatedMusicBase64) {
        generatedMusicBase64?.let { base64 ->
            audioPlayer.playBase64Audio(base64)
        }
    }

    if (isLoading) {
        ArcadeLoadingOverlay()
        return
    }

    BackHandler(enabled = selectedGameId != null) {
        arcadeViewModel.selectGame(null)
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        when (selectedGameId) {
            "snake" -> {
                val best = highScores["snake"]?.highScore ?: 0
                SnakeGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("snake", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "2048" -> {
                val best = highScores["2048"]?.highScore ?: 0
                Game2048(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("2048", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "minesweeper" -> {
                val best = highScores["minesweeper"]?.highScore ?: 0
                MinesweeperGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("minesweeper", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "brick_breaker" -> {
                val best = highScores["brick_breaker"]?.highScore ?: 0
                BrickBreakerGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("brick_breaker", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "word_guess" -> {
                val best = highScores["word_guess"]?.highScore ?: 0
                WordGuessGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("word_guess", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "tic_tac_toe" -> {
                val best = highScores["tic_tac_toe"]?.highScore ?: 0
                TicTacToeGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("tic_tac_toe", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "flappy_bird" -> {
                val best = highScores["flappy_bird"]?.highScore ?: 0
                FlappyBirdGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("flappy_bird", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "memory_cards" -> {
                val best = highScores["memory_cards"]?.highScore ?: 0
                MemoryMatchGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("memory_cards", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "connect_four" -> {
                val best = highScores["connect_four"]?.highScore ?: 0
                ConnectFourGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("connect_four", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            "whack_a_mole" -> {
                val best = highScores["whack_a_mole"]?.highScore ?: 0
                WhackAMoleGame(
                    highScore = best,
                    onRecordScore = { arcadeViewModel.recordScore("whack_a_mole", it) },
                    onBack = { arcadeViewModel.selectGame(null) }
                )
            }
            else -> {
                ArcadeHomeScreen(
                    highScores = highScores,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { arcadeViewModel.setCategory(it) },
                    onGameSelected = { arcadeViewModel.selectGame(it) },
                    onPlayRandom = { arcadeViewModel.playRandomGame() },
                    onGenerateMusic = { arcadeViewModel.generateHomeMusic() }
                )
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MyApplicationTheme { Greeting("Android") }
}
