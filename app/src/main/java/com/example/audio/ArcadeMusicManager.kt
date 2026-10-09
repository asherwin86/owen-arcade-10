package com.example.audio

import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.util.Log
import androidx.compose.runtime.compositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

val LocalMusicManager = compositionLocalOf<ArcadeMusicManager?> { null }

/**
 * Global Background Music Manager for Arcade 10.
 * Automatically adapts background music to the Arcade Lobby and each of the 10+ games.
 * Supports built-in authentic 8-bit chiptune tracks and AI-generated Lyria tracks.
 */
class ArcadeMusicManager(private val context: Context) {

    private val tag = "ArcadeMusicManager"
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val prefs: SharedPreferences = context.getSharedPreferences("arcade_music_prefs", Context.MODE_PRIVATE)

    private var mediaPlayer: MediaPlayer? = null
    private var previewPlayer: MediaPlayer? = null

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isMuted = MutableStateFlow(prefs.getBoolean("music_muted", false))
    val isMuted: StateFlow<Boolean> = _isMuted.asStateFlow()

    private val _volume = MutableStateFlow(prefs.getFloat("music_volume", 0.75f))
    val volume: StateFlow<Float> = _volume.asStateFlow()

    private val _currentTrackTitle = MutableStateFlow("Neon Pixel Lounge")
    val currentTrackTitle: StateFlow<String> = _currentTrackTitle.asStateFlow()

    private val _currentTrackSub = MutableStateFlow("Arcade Lobby • 124 BPM")
    val currentTrackSub: StateFlow<String> = _currentTrackSub.asStateFlow()

    private val _isAiGenerated = MutableStateFlow(false)
    val isAiGenerated: StateFlow<Boolean> = _isAiGenerated.asStateFlow()

    private val _activeGameId = MutableStateFlow<String?>(null)
    val activeGameId: StateFlow<String?> = _activeGameId.asStateFlow()

    private val _previewPlayingFile = MutableStateFlow<String?>(null)
    val previewPlayingFile: StateFlow<String?> = _previewPlayingFile.asStateFlow()

    private var currentlyPlayingPath: String? = null

    init {
        // Start playing lobby music on initialization
        playForGame(null)
    }

    /**
     * Transition background music to match the current screen (game or lobby).
     * @param gameId null for Arcade Lobby, or game ID (e.g., "snake", "2048", etc.)
     */
    fun playForGame(gameId: String?) {
        _activeGameId.value = gameId
        val targetKey = gameId ?: MusicTrackRegistry.LOBBY_TRACK_ID
        val trackInfo = MusicTrackRegistry.getTrackForGame(gameId)

        scope.launch {
            // Check if there is an AI track assigned to this game/lobby
            val customPath = prefs.getString("bgm_custom_$targetKey", null)
            val customFile = customPath?.let { File(it) }

            val (fileToPlay, title, sub, isAi) = if (customFile != null && customFile.exists()) {
                val customTitle = prefs.getString("bgm_title_$targetKey", "Custom AI Track") ?: "Custom AI Track"
                val modelUsed = prefs.getString("bgm_model_$targetKey", "Lyria") ?: "Lyria"
                FourTuple(customFile, customTitle, "${trackInfo.gameTitle} • $modelUsed", true)
            } else {
                val synthesizedFile = withContext(Dispatchers.IO) {
                    ChiptuneSynthesizer.getOrCreateTrackFile(context, targetKey)
                }
                FourTuple(synthesizedFile, trackInfo.name, "${trackInfo.gameTitle} • ${trackInfo.genre}", false)
            }

            _currentTrackTitle.value = title
            _currentTrackSub.value = sub
            _isAiGenerated.value = isAi

            // If already playing this exact file, do nothing
            if (fileToPlay.absolutePath == currentlyPlayingPath && mediaPlayer?.isPlaying == true) {
                return@launch
            }

            playAudioFile(fileToPlay)
        }
    }

    private fun playAudioFile(file: File) {
        try {
            stopPreviewInternal()
            stopCurrentMediaPlayer()

            val player = MediaPlayer()
            player.setDataSource(file.absolutePath)
            player.isLooping = true
            player.prepare()

            val effectiveVol = if (_isMuted.value) 0f else _volume.value
            player.setVolume(effectiveVol, effectiveVol)
            player.start()

            mediaPlayer = player
            currentlyPlayingPath = file.absolutePath
            _isPlaying.value = true
        } catch (e: Exception) {
            Log.e(tag, "Error playing audio file: ${file.absolutePath}", e)
            _isPlaying.value = false
        }
    }

    fun toggleMute() {
        val newMuted = !_isMuted.value
        _isMuted.value = newMuted
        prefs.edit().putBoolean("music_muted", newMuted).apply()
        applyVolume()
    }

    fun setVolume(newVolume: Float) {
        val clamped = newVolume.coerceIn(0f, 1f)
        _volume.value = clamped
        prefs.edit().putFloat("music_volume", clamped).apply()
        if (_isMuted.value && clamped > 0f) {
            _isMuted.value = false
            prefs.edit().putBoolean("music_muted", false).apply()
        }
        applyVolume()
    }

    private fun applyVolume() {
        val vol = if (_isMuted.value) 0f else _volume.value
        try {
            mediaPlayer?.setVolume(vol, vol)
            previewPlayer?.setVolume(vol, vol)
        } catch (e: Exception) {
            Log.e(tag, "Error setting volume", e)
        }
    }

    fun pause() {
        try {
            if (mediaPlayer?.isPlaying == true) {
                mediaPlayer?.pause()
                _isPlaying.value = false
            }
        } catch (e: Exception) {
            Log.e(tag, "Error pausing media player", e)
        }
    }

    fun resume() {
        try {
            if (mediaPlayer != null && !_isPlaying.value) {
                mediaPlayer?.start()
                _isPlaying.value = true
            } else if (mediaPlayer == null) {
                playForGame(_activeGameId.value)
            }
        } catch (e: Exception) {
            Log.e(tag, "Error resuming media player", e)
        }
    }

    fun togglePlayPause() {
        if (_isPlaying.value) {
            pause()
        } else {
            resume()
        }
    }

    /**
     * Preview an audio file (e.g. inside the Music Studio dialog)
     */
    fun previewAudio(file: File) {
        scope.launch {
            try {
                // If this file is already previewing, pause/stop it
                if (_previewPlayingFile.value == file.absolutePath) {
                    stopPreview()
                    return@launch
                }

                // Pause background music during preview
                mediaPlayer?.pause()

                stopPreviewInternal()
                val player = MediaPlayer()
                player.setDataSource(file.absolutePath)
                player.prepare()
                val vol = if (_isMuted.value) 0f else _volume.value
                player.setVolume(vol, vol)
                player.setOnCompletionListener {
                    _previewPlayingFile.value = null
                    // Resume bg music
                    if (_isPlaying.value) {
                        mediaPlayer?.start()
                    }
                }
                player.start()
                previewPlayer = player
                _previewPlayingFile.value = file.absolutePath
            } catch (e: Exception) {
                Log.e(tag, "Error previewing audio file", e)
                stopPreview()
            }
        }
    }

    fun stopPreview() {
        stopPreviewInternal()
        _previewPlayingFile.value = null
        if (_isPlaying.value) {
            try {
                mediaPlayer?.start()
            } catch (e: Exception) {
                Log.e(tag, "Error resuming background player", e)
            }
        }
    }

    private fun stopPreviewInternal() {
        try {
            previewPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error stopping preview player", e)
        }
        previewPlayer = null
    }

    /**
     * Assigns a custom Lyria track as the active BGM for a game or arcade lobby.
     */
    fun assignCustomTrack(gameId: String, track: GeneratedMusicTrack) {
        val targetKey = gameId
        prefs.edit()
            .putString("bgm_custom_$targetKey", track.file.absolutePath)
            .putString("bgm_title_$targetKey", track.title)
            .putString("bgm_model_$targetKey", track.modelUsed)
            .apply()

        // If the updated game is currently active, transition to it immediately!
        val currentKey = _activeGameId.value ?: MusicTrackRegistry.LOBBY_TRACK_ID
        if (currentKey == targetKey) {
            playForGame(_activeGameId.value)
        }
    }

    /**
     * Resets a game or arcade lobby to its authentic built-in 8-bit chiptune theme.
     */
    fun resetToDefaultChiptune(gameId: String) {
        val targetKey = gameId
        prefs.edit()
            .remove("bgm_custom_$targetKey")
            .remove("bgm_title_$targetKey")
            .remove("bgm_model_$targetKey")
            .apply()

        val currentKey = _activeGameId.value ?: MusicTrackRegistry.LOBBY_TRACK_ID
        if (currentKey == targetKey) {
            currentlyPlayingPath = null // Force reload
            playForGame(_activeGameId.value)
        }
    }

    fun isGameUsingAiTrack(gameId: String): Boolean {
        val path = prefs.getString("bgm_custom_$gameId", null) ?: return false
        return File(path).exists()
    }

    fun getTrackTitleForGame(gameId: String): String {
        val customTitle = prefs.getString("bgm_title_$gameId", null)
        if (customTitle != null && isGameUsingAiTrack(gameId)) return customTitle
        return MusicTrackRegistry.getTrackForGame(if (gameId == MusicTrackRegistry.LOBBY_TRACK_ID) null else gameId).name
    }

    private fun stopCurrentMediaPlayer() {
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (e: Exception) {
            Log.e(tag, "Error releasing media player", e)
        }
        mediaPlayer = null
        currentlyPlayingPath = null
    }

    fun release() {
        stopPreviewInternal()
        stopCurrentMediaPlayer()
        _isPlaying.value = false
    }

    private data class FourTuple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
