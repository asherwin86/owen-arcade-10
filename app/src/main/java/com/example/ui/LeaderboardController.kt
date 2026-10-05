package com.example.ui

import androidx.compose.runtime.compositionLocalOf
import com.example.data.FirebaseCustomConfig
import com.example.data.FirestoreConnectionState
import com.example.data.LeaderboardEntry

data class LeaderboardUiController(
    val activeGameId: String? = null,
    val playerId: String = "",
    val playerName: String = "PLAYER 1",
    val connectionState: FirestoreConnectionState = FirestoreConnectionState.LocalOnly,
    val topScoresByGame: Map<String, List<LeaderboardEntry>> = emptyMap(),
    val onOpenGameLeaderboardModal: (String) -> Unit = {},
    val onOpenGlobalLeaderboardScreen: (String?) -> Unit = {},
    val onOpenEditCallsign: () -> Unit = {},
    val onOpenFirebaseConfig: () -> Unit = {},
    val onRefreshLeaderboards: () -> Unit = {},
    val onUpdatePlayerName: (String) -> Unit = {},
    val onSaveFirebaseConfig: (String, String, String) -> Unit = { _, _, _ -> },
    val getSavedFirebaseConfig: () -> FirebaseCustomConfig = {
        FirebaseCustomConfig("", "", "")
    }
)

val LocalLeaderboardController = compositionLocalOf { LeaderboardUiController() }
