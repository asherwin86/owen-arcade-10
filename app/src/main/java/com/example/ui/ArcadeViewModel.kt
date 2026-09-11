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
import com.example.BuildConfig
import com.example.api.RetrofitClient
import com.example.api.GenerateContentRequest
import com.example.api.Content
import com.example.api.Part
import com.example.api.GenerationConfig
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

    private val _isMusicGenerating = MutableStateFlow(false)
    val isMusicGenerating: StateFlow<Boolean> = _isMusicGenerating.asStateFlow()

    private val _generatedMusicBase64 = MutableStateFlow<String?>(null)
    val generatedMusicBase64: StateFlow<String?> = _generatedMusicBase64.asStateFlow()

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
                    contents = listOf(Content(
                        parts = listOf(Part(text = "Generate a 30-second upbeat retro synthwave arcade track suitable for a retro game collection main menu."))
                    )),
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
