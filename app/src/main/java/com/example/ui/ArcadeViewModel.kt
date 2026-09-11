package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.GameScore
import com.example.data.GameScoreRepository
import com.example.model.GameCategory
import com.example.model.GameInfo
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

    init {
        val dao = AppDatabase.getDatabase(application).gameScoreDao()
        repository = GameScoreRepository(dao)
    }

    val highScores: StateFlow<Map<String, GameScore>> = repository.allScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        viewModelScope.launch {
            delay(2000) // Simulate asset loading
            _isLoading.value = false
        }
    }

    private val _selectedGameId = MutableStateFlow<String?>(null)
    val selectedGameId: StateFlow<String?> = _selectedGameId.asStateFlow()

    private val _selectedCategory = MutableStateFlow(GameCategory.ALL)
    val selectedCategory: StateFlow<GameCategory> = _selectedCategory.asStateFlow()

    fun selectGame(gameId: String?) {
        _selectedGameId.value = gameId
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
        }
    }
}
