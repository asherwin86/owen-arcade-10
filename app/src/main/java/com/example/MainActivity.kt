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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.audio.ArcadeMusicManager
import com.example.audio.LocalMusicManager
import com.example.ui.ArcadeHomeScreen
import com.example.ui.ArcadeViewModel
import com.example.ui.GlobalLeaderboardScreen
import com.example.ui.LeaderboardUiController
import com.example.ui.LocalLeaderboardController
import com.example.ui.components.ArcadeLoadingOverlay
import com.example.ui.components.EditCallsignDialog
import com.example.ui.components.FirebaseConfigDialog
import com.example.ui.components.GameLeaderboardModalDialog
import com.example.ui.music.MusicGenerationDialog
import com.example.ui.update.AppUpdateDialog
import com.example.update.AppUpdateManager
import com.example.update.LocalAppUpdateManager
import com.example.ui.games.BrickBreakerGame
import com.example.ui.games.ConnectFourGame
import com.example.ui.games.FlappyBirdGame
import com.example.ui.games.Game2048
import com.example.ui.games.MemoryMatchGame
import com.example.ui.games.MinesweeperGame
import com.example.ui.games.OwenTagGame
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
    val topScoresByGame by arcadeViewModel.topScoresByGame.collectAsStateWithLifecycle()
    val firestoreState by arcadeViewModel.firestoreConnectionState.collectAsStateWithLifecycle()
    val playerName by arcadeViewModel.playerName.collectAsStateWithLifecycle()
    val selectedCategory by arcadeViewModel.selectedCategory.collectAsStateWithLifecycle()

    val isGlobalLeaderboardOpen by arcadeViewModel.isGlobalLeaderboardOpen.collectAsStateWithLifecycle()
    val leaderboardFocusGameId by arcadeViewModel.leaderboardFocusGameId.collectAsStateWithLifecycle()
    val modalLeaderboardGameId by arcadeViewModel.modalLeaderboardGameId.collectAsStateWithLifecycle()
    val isEditCallsignOpen by arcadeViewModel.isEditCallsignOpen.collectAsStateWithLifecycle()
    val isFirebaseConfigOpen by arcadeViewModel.isFirebaseConfigOpen.collectAsStateWithLifecycle()

    val isMusicGenerating by arcadeViewModel.isMusicGenerating.collectAsStateWithLifecycle()
    val generatedMusicBase64 by arcadeViewModel.generatedMusicBase64.collectAsStateWithLifecycle()
    val isMusicStudioOpen by arcadeViewModel.isMusicStudioOpen.collectAsStateWithLifecycle()
    val musicStudioTargetGameId by arcadeViewModel.musicStudioTargetGameId.collectAsStateWithLifecycle()
    val isUpdateDialogOpen by arcadeViewModel.isUpdateDialogOpen.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val arcadeMusicManager = remember { ArcadeMusicManager(context.applicationContext) }
    val appUpdateManager = remember {
        AppUpdateManager(
            context = context.applicationContext,
            onPeriodicDataSync = { arcadeViewModel.refreshLeaderboards() }
        )
    }

    // Re-attach Firestore release listener when Firebase connection state changes
    LaunchedEffect(firestoreState) {
        appUpdateManager.attachFirestoreReleaseListener()
    }

    // Sync active game ID with background music
    LaunchedEffect(selectedGameId) {
        arcadeMusicManager.playForGame(selectedGameId)
    }

    // Android Activity Lifecycle handling
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> arcadeMusicManager.pause()
                Lifecycle.Event.ON_RESUME -> arcadeMusicManager.resume()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            arcadeMusicManager.release()
            appUpdateManager.release()
        }
    }

    if (isLoading) {
        ArcadeLoadingOverlay()
        return
    }

    val controller = LeaderboardUiController(
        activeGameId = selectedGameId,
        playerId = arcadeViewModel.playerId,
        playerName = playerName,
        connectionState = firestoreState,
        topScoresByGame = topScoresByGame,
        onOpenGameLeaderboardModal = { gameId -> arcadeViewModel.openGameLeaderboardModal(gameId) },
        onOpenGlobalLeaderboardScreen = { gameId ->
            arcadeViewModel.selectGame(null)
            arcadeViewModel.openGlobalLeaderboard(gameId)
        },
        onOpenEditCallsign = { arcadeViewModel.setEditCallsignOpen(true) },
        onOpenFirebaseConfig = { arcadeViewModel.setFirebaseConfigOpen(true) },
        onRefreshLeaderboards = { arcadeViewModel.refreshLeaderboards() },
        onUpdatePlayerName = { newName -> arcadeViewModel.updatePlayerName(newName) },
        onSaveFirebaseConfig = { proj, app, key -> arcadeViewModel.saveFirebaseConfig(proj, app, key) },
        getSavedFirebaseConfig = { arcadeViewModel.getSavedFirebaseConfig() }
    )

    BackHandler(enabled = selectedGameId != null || isGlobalLeaderboardOpen) {
        if (selectedGameId != null) {
            arcadeViewModel.selectGame(null)
        } else if (isGlobalLeaderboardOpen) {
            arcadeViewModel.closeGlobalLeaderboard()
        }
    }

    CompositionLocalProvider(
        LocalLeaderboardController provides controller,
        LocalMusicManager provides arcadeMusicManager,
        LocalAppUpdateManager provides appUpdateManager
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            if (isGlobalLeaderboardOpen && selectedGameId == null) {
                GlobalLeaderboardScreen(
                    initialGameId = leaderboardFocusGameId,
                    topScoresByGame = topScoresByGame,
                    localHighScores = highScores,
                    currentPlayerId = arcadeViewModel.playerId,
                    currentPlayerName = playerName,
                    connectionState = firestoreState,
                    onBack = { arcadeViewModel.closeGlobalLeaderboard() },
                    onPlayGame = { gameId -> arcadeViewModel.selectGame(gameId) },
                    onEditCallsign = { arcadeViewModel.setEditCallsignOpen(true) },
                    onOpenFirebaseConfig = { arcadeViewModel.setFirebaseConfigOpen(true) },
                    onRefresh = { arcadeViewModel.refreshLeaderboards() }
                )
            } else {
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
                    "owen_tag" -> {
                        val best = highScores["owen_tag"]?.highScore ?: 0
                        OwenTagGame(
                            highScore = best,
                            onRecordScore = { arcadeViewModel.recordScore("owen_tag", it) },
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
                            onGenerateMusic = { arcadeViewModel.openMusicStudio(null) },
                            musicManager = arcadeMusicManager,
                            updateManager = appUpdateManager,
                            onOpenUpdateCenter = { arcadeViewModel.setUpdateDialogOpen(true) }
                        )
                    }
                }
            }
        }

        // Modal dialogs accessible from anywhere
        AppUpdateDialog(
            isOpen = isUpdateDialogOpen,
            updateManager = appUpdateManager,
            onDismiss = { arcadeViewModel.setUpdateDialogOpen(false) }
        )

        MusicGenerationDialog(
            isOpen = isMusicStudioOpen,
            musicManager = arcadeMusicManager,
            initialTargetGameId = musicStudioTargetGameId ?: selectedGameId,
            onDismiss = { arcadeViewModel.closeMusicStudio() }
        )

        GameLeaderboardModalDialog(
            gameId = modalLeaderboardGameId,
            entries = modalLeaderboardGameId?.let { topScoresByGame[it] } ?: emptyList(),
            currentPlayerId = arcadeViewModel.playerId,
            currentPlayerName = playerName,
            connectionState = firestoreState,
            onDismiss = { arcadeViewModel.openGameLeaderboardModal(null) },
            onOpenFullHub = { gameId ->
                arcadeViewModel.selectGame(null)
                arcadeViewModel.openGlobalLeaderboard(gameId)
            },
            onEditCallsign = { arcadeViewModel.setEditCallsignOpen(true) },
            onRefresh = { arcadeViewModel.refreshLeaderboards() }
        )

        EditCallsignDialog(
            isOpen = isEditCallsignOpen,
            currentName = playerName,
            onDismiss = { arcadeViewModel.setEditCallsignOpen(false) },
            onSave = { newName -> arcadeViewModel.updatePlayerName(newName) }
        )

        FirebaseConfigDialog(
            isOpen = isFirebaseConfigOpen,
            initialConfig = arcadeViewModel.getSavedFirebaseConfig(),
            connectionState = firestoreState,
            onDismiss = { arcadeViewModel.setFirebaseConfigOpen(false) },
            onSaveConfig = { proj, app, key -> arcadeViewModel.saveFirebaseConfig(proj, app, key) }
        )
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
