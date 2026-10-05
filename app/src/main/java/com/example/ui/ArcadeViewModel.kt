package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import com.example.data.AppDatabase
import com.example.data.FirebaseCustomConfig
import com.example.data.FirestoreConnectionState
import com.example.data.FirestoreLeaderboardRepository
import com.example.data.GameScore
import com.example.data.GameScoreRepository
import com.example.data.LeaderboardEntry
import com.example.model.GameCategory
import com.example.model.GameRegistry
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArcadeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: GameScoreRepository
    private val leaderboardRepository: FirestoreLeaderboardRepository

    private var lastSubmittedEntryId: String? = null
    private var lastSubmittedGameId: String? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = GameScoreRepository(db.gameScoreDao())
        leaderboardRepository = FirestoreLeaderboardRepository(
            context = application,
            leaderboardDao = db.leaderboardDao(),
            gameScoreDao = db.gameScoreDao()
        )
    }

    val highScores: StateFlow<Map<String, GameScore>> = repository.allScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val topScoresByGame: StateFlow<Map<String, List<LeaderboardEntry>>> =
        leaderboardRepository.topScoresByGame.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    val firestoreConnectionState: StateFlow<FirestoreConnectionState> =
        leaderboardRepository.connectionState

    val playerName: StateFlow<String> = leaderboardRepository.playerName
    val playerId: String get() = leaderboardRepository.playerId

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isMusicGenerating = MutableStateFlow(false)
    val isMusicGenerating: StateFlow<Boolean> = _isMusicGenerating.asStateFlow()

    private val _generatedMusicBase64 = MutableStateFlow<String?>(null)
    val generatedMusicBase64: StateFlow<String?> = _generatedMusicBase64.asStateFlow()

    private val _isGlobalLeaderboardOpen = MutableStateFlow(false)
    val isGlobalLeaderboardOpen: StateFlow<Boolean> = _isGlobalLeaderboardOpen.asStateFlow()

    private val _leaderboardFocusGameId = MutableStateFlow<String?>(null)
    val leaderboardFocusGameId: StateFlow<String?> = _leaderboardFocusGameId.asStateFlow()

    private val _modalLeaderboardGameId = MutableStateFlow<String?>(null)
    val modalLeaderboardGameId: StateFlow<String?> = _modalLeaderboardGameId.asStateFlow()

    private val _isEditCallsignOpen = MutableStateFlow(false)
    val isEditCallsignOpen: StateFlow<Boolean> = _isEditCallsignOpen.asStateFlow()

    private val _isFirebaseConfigOpen = MutableStateFlow(false)
    val isFirebaseConfigOpen: StateFlow<Boolean> = _isFirebaseConfigOpen.asStateFlow()

    init {
        viewModelScope.launch {
            delay(2000) // Simulate asset loading
            _isLoading.value = false
        }
    }

    fun generateHomeMusic() {
        if (_isMusicGenerating.value) return
        _isMusicGenerating.value = true

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                val request = GenerateContentRequest(
                    contents = listOf(
                        Content(
                            parts = listOf(
                                Part(
                                    text = "Generate a 30-second upbeat retro synthwave arcade track suitable for a retro game collection main menu."
                                )
                            )
                        )
                    ),
                    generationConfig = GenerationConfig(
                        responseModalities = listOf("AUDIO")
                    )
                )

                // Use lyria-3-clip-preview for short clips
                val response = RetrofitClient.service.generateContent(
                    model = "lyria-3-clip-preview",
                    apiKey = apiKey,
                    request = request
                )

                val base64Data = response.candidates?.firstOrNull()
                    ?.content?.parts?.firstOrNull()
                    ?.inlineData?.data

                _generatedMusicBase64.value = base64Data
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isMusicGenerating.value = false
            }
        }
    }

    private val _selectedGameId = MutableStateFlow<String?>(null)
    val selectedGameId: StateFlow<String?> = _selectedGameId.asStateFlow()

    private val _selectedCategory = MutableStateFlow(GameCategory.ALL)
    val selectedCategory: StateFlow<GameCategory> = _selectedCategory.asStateFlow()

    fun selectGame(gameId: String?) {
        _selectedGameId.value = gameId
        if (gameId != null) {
            _isGlobalLeaderboardOpen.value = false
            _modalLeaderboardGameId.value = null
        }
    }

    fun setCategory(category: GameCategory) {
        _selectedCategory.value = category
    }

    fun playRandomGame() {
        val randomGame = GameRegistry.games.random()
        selectGame(randomGame.id)
    }

    fun recordScore(gameId: String, score: Int) {
        viewModelScope.launch {
            repository.recordGameFinished(gameId, score)
            if (score > 0) {
                val entryId = leaderboardRepository.submitScore(gameId, score)
                lastSubmittedEntryId = entryId
                lastSubmittedGameId = gameId
            }
        }
    }

    fun openGlobalLeaderboard(focusGameId: String? = null) {
        _leaderboardFocusGameId.value = focusGameId
        _isGlobalLeaderboardOpen.value = true
    }

    fun closeGlobalLeaderboard() {
        _isGlobalLeaderboardOpen.value = false
    }

    fun openGameLeaderboardModal(gameId: String?) {
        _modalLeaderboardGameId.value = gameId
    }

    fun setEditCallsignOpen(open: Boolean) {
        _isEditCallsignOpen.value = open
    }

    fun setFirebaseConfigOpen(open: Boolean) {
        _isFirebaseConfigOpen.value = open
    }

    fun updatePlayerName(newName: String) {
        leaderboardRepository.updatePlayerName(
            newName = newName,
            lastEntryId = lastSubmittedEntryId,
            lastGameId = lastSubmittedGameId
        )
    }

    fun refreshLeaderboards() {
        leaderboardRepository.refreshAllLeaderboards()
    }

    fun getSavedFirebaseConfig(): FirebaseCustomConfig {
        return leaderboardRepository.getSavedFirebaseConfig()
    }

    fun saveFirebaseConfig(projectId: String, appId: String, apiKey: String) {
        leaderboardRepository.saveFirebaseConfigAndReconnect(projectId, appId, apiKey)
    }
}
