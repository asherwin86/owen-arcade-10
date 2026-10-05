package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import com.example.model.GameRegistry
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.UUID
import kotlin.coroutines.resume

sealed class FirestoreConnectionState {
    data object Syncing : FirestoreConnectionState()
    data class Connected(val projectId: String) : FirestoreConnectionState()
    data object LocalOnly : FirestoreConnectionState()
    data class Error(val message: String) : FirestoreConnectionState()
}

data class FirebaseCustomConfig(
    val projectId: String,
    val applicationId: String,
    val apiKey: String
)

class FirestoreLeaderboardRepository(
    private val context: Context,
    private val leaderboardDao: LeaderboardDao,
    private val gameScoreDao: GameScoreDao
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs: SharedPreferences =
        context.getSharedPreferences("arcade_10_leaderboard_prefs", Context.MODE_PRIVATE)

    private var firestore: FirebaseFirestore? = null
    private val snapshotListeners = mutableListOf<ListenerRegistration>()

    private val _connectionState = MutableStateFlow<FirestoreConnectionState>(FirestoreConnectionState.LocalOnly)
    val connectionState: StateFlow<FirestoreConnectionState> = _connectionState.asStateFlow()

    private val _playerName = MutableStateFlow(loadPlayerName())
    val playerName: StateFlow<String> = _playerName.asStateFlow()

    val playerId: String = getOrCreatePlayerId()

    val topScoresByGame: Flow<Map<String, List<LeaderboardEntry>>> =
        leaderboardDao.getAllScores().map { entries ->
            val grouped = entries.groupBy { it.gameId }
            GameRegistry.games.associate { game ->
                val top10 = (grouped[game.id] ?: emptyList())
                    .filter { it.score > 0 }
                    .sortedWith(
                        compareByDescending<LeaderboardEntry> { it.score }
                            .thenBy { it.timestamp }
                    )
                    .take(10)
                game.id to top10
            }
        }

    init {
        scope.launch {
            migrateExistingPersonalBests()
            initializeFirestoreAndSync()
        }
    }

    private fun getOrCreatePlayerId(): String {
        val existing = prefs.getString(KEY_PLAYER_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val generated = "p_" + UUID.randomUUID().toString().replace("-", "").take(10)
        prefs.edit().putString(KEY_PLAYER_ID, generated).apply()
        return generated
    }

    private fun loadPlayerName(): String {
        return prefs.getString(KEY_PLAYER_NAME, "PLAYER 1")?.takeIf { it.isNotBlank() } ?: "PLAYER 1"
    }

    fun updatePlayerName(newName: String, lastEntryId: String? = null, lastGameId: String? = null) {
        val sanitized = newName.trim().uppercase().take(14).ifBlank { "PLAYER 1" }
        prefs.edit().putString(KEY_PLAYER_NAME, sanitized).apply()
        _playerName.value = sanitized

        if (!lastEntryId.isNullOrBlank() && !lastGameId.isNullOrBlank()) {
            scope.launch {
                leaderboardDao.updatePlayerNameForEntry(lastEntryId, sanitized)
                val db = firestore ?: return@launch
                try {
                    db.collection(COLLECTION_LEADERBOARDS)
                        .document(lastGameId)
                        .collection(SUBCOLLECTION_SCORES)
                        .document(lastEntryId)
                        .update("playerName", sanitized)
                        .addOnSuccessListener {
                            scope.launch { leaderboardDao.markAsSynced(lastEntryId) }
                        }
                } catch (_: Exception) {
                }
            }
        }
    }

    fun getSavedFirebaseConfig(): FirebaseCustomConfig {
        val savedProject = prefs.getString(KEY_FB_PROJECT_ID, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.FIREBASE_PROJECT_ID.takeIf { it.isNotBlank() && !it.startsWith("MY_") }
            ?: ""
        val savedAppId = prefs.getString(KEY_FB_APP_ID, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.FIREBASE_APPLICATION_ID.takeIf { it.isNotBlank() && !it.startsWith("MY_") }
            ?: ""
        val savedApiKey = prefs.getString(KEY_FB_API_KEY, null)?.takeIf { it.isNotBlank() }
            ?: BuildConfig.FIREBASE_API_KEY.takeIf { it.isNotBlank() && !it.startsWith("MY_") }
            ?: ""
        return FirebaseCustomConfig(
            projectId = savedProject,
            applicationId = savedAppId,
            apiKey = savedApiKey
        )
    }

    fun saveFirebaseConfigAndReconnect(projectId: String, applicationId: String, apiKey: String) {
        prefs.edit()
            .putString(KEY_FB_PROJECT_ID, projectId.trim())
            .putString(KEY_FB_APP_ID, applicationId.trim())
            .putString(KEY_FB_API_KEY, apiKey.trim())
            .apply()
        scope.launch {
            initializeFirestoreAndSync(forceReinit = true)
        }
    }

    private suspend fun migrateExistingPersonalBests() {
        for (game in GameRegistry.games) {
            val existingScore = gameScoreDao.getScoreSync(game.id)
            if (existingScore != null && existingScore.highScore > 0) {
                val count = leaderboardDao.getCountForGame(game.id)
                if (count == 0) {
                    val entry = LeaderboardEntry(
                        id = "${game.id}_${playerId}_best",
                        gameId = game.id,
                        playerName = _playerName.value,
                        playerId = playerId,
                        score = existingScore.highScore,
                        scoreUnit = game.scoreUnit,
                        timestamp = existingScore.lastUpdated,
                        isSynced = false
                    )
                    leaderboardDao.insertEntry(entry)
                }
            }
        }
    }

    fun initializeFirestoreAndSync(forceReinit: Boolean = false) {
        scope.launch {
            _connectionState.value = FirestoreConnectionState.Syncing
            val db = resolveFirestoreInstance(forceReinit)
            if (db == null) {
                _connectionState.value = FirestoreConnectionState.LocalOnly
                return@launch
            }
            firestore = db
            val projId = db.app.options.projectId ?: "firebase-cloud"
            _connectionState.value = FirestoreConnectionState.Connected(projId)
            attachRealtimeListeners(db)
            syncUnsyncedEntries(db)
            refreshAllGamesFromFirestore(db)
        }
    }

    private fun resolveFirestoreInstance(forceReinit: Boolean): FirebaseFirestore? {
        return try {
            val customConfig = getSavedFirebaseConfig()
            val hasCustomCredentials = customConfig.projectId.isNotBlank() &&
                customConfig.applicationId.isNotBlank() &&
                customConfig.apiKey.isNotBlank()

            val app: FirebaseApp? = if (forceReinit && hasCustomCredentials) {
                val appName = "arcade10_firestore_${System.currentTimeMillis()}"
                val options = FirebaseOptions.Builder()
                    .setProjectId(customConfig.projectId)
                    .setApplicationId(customConfig.applicationId)
                    .setApiKey(customConfig.apiKey)
                    .build()
                FirebaseApp.initializeApp(context, options, appName)
            } else {
                val existingApps = FirebaseApp.getApps(context)
                if (existingApps.isNotEmpty()) {
                    existingApps.last()
                } else {
                    val defaultInit = FirebaseApp.initializeApp(context)
                    if (defaultInit != null) {
                        defaultInit
                    } else if (hasCustomCredentials) {
                        val options = FirebaseOptions.Builder()
                            .setProjectId(customConfig.projectId)
                            .setApplicationId(customConfig.applicationId)
                            .setApiKey(customConfig.apiKey)
                            .build()
                        FirebaseApp.initializeApp(context, options)
                    } else {
                        null
                    }
                }
            }

            if (app == null) return null

            val db = FirebaseFirestore.getInstance(app)
            try {
                val settings = FirebaseFirestoreSettings.Builder()
                    .setLocalCacheSettings(PersistentCacheSettings.newBuilder().build())
                    .build()
                db.firestoreSettings = settings
            } catch (_: Exception) {
                // Settings may already be locked if instance was previously used
            }
            db
        } catch (e: Exception) {
            _connectionState.value = FirestoreConnectionState.Error(
                e.localizedMessage ?: "Failed to initialize Firebase Firestore"
            )
            null
        }
    }

    private fun attachRealtimeListeners(db: FirebaseFirestore) {
        snapshotListeners.forEach { runCatching { it.remove() } }
        snapshotListeners.clear()

        for (game in GameRegistry.games) {
            val registration = db.collection(COLLECTION_LEADERBOARDS)
                .document(game.id)
                .collection(SUBCOLLECTION_SCORES)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val entries = snapshot.documents.mapNotNull { doc ->
                            val scoreVal = doc.getLong("score")?.toInt() ?: return@mapNotNull null
                            if (scoreVal <= 0) return@mapNotNull null
                            LeaderboardEntry(
                                id = doc.id,
                                gameId = doc.getString("gameId") ?: game.id,
                                playerName = doc.getString("playerName") ?: "PLAYER 1",
                                playerId = doc.getString("playerId") ?: "",
                                score = scoreVal,
                                scoreUnit = doc.getString("scoreUnit") ?: game.scoreUnit,
                                timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                                isSynced = true
                            )
                        }
                        if (entries.isNotEmpty()) {
                            scope.launch {
                                leaderboardDao.insertEntries(entries)
                            }
                        }
                    }
                }
            snapshotListeners.add(registration)
        }
    }

    suspend fun submitScore(gameId: String, score: Int): String? {
        if (score <= 0) return null
        val game = GameRegistry.getGame(gameId)
        val entryId = "${gameId}_${playerId}_${System.currentTimeMillis()}"
        val entry = LeaderboardEntry(
            id = entryId,
            gameId = gameId,
            playerName = _playerName.value,
            playerId = playerId,
            score = score,
            scoreUnit = game.scoreUnit,
            timestamp = System.currentTimeMillis(),
            isSynced = false
        )

        leaderboardDao.insertEntry(entry)

        val db = firestore
        if (db != null) {
            pushEntryToFirestore(db, entry, game.title)
        }
        return entryId
    }

    private suspend fun pushEntryToFirestore(
        db: FirebaseFirestore,
        entry: LeaderboardEntry,
        gameTitle: String
    ): Boolean = suspendCancellableCoroutine { cont ->
        try {
            val gameDocRef = db.collection(COLLECTION_LEADERBOARDS).document(entry.gameId)
            val scoreDocRef = gameDocRef.collection(SUBCOLLECTION_SCORES).document(entry.id)

            gameDocRef.set(
                mapOf(
                    "gameId" to entry.gameId,
                    "gameTitle" to gameTitle,
                    "scoreUnit" to entry.scoreUnit,
                    "lastUpdated" to entry.timestamp
                ),
                SetOptions.merge()
            )

            scoreDocRef.set(entry.toFirestoreMap())
                .addOnSuccessListener {
                    scope.launch {
                        leaderboardDao.markAsSynced(entry.id)
                    }
                    if (cont.isActive) cont.resume(true)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(false)
                }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(false)
        }
    }

    fun refreshAllLeaderboards() {
        scope.launch {
            val db = firestore ?: resolveFirestoreInstance(forceReinit = false)
            if (db == null) {
                _connectionState.value = FirestoreConnectionState.LocalOnly
                return@launch
            }
            firestore = db
            syncUnsyncedEntries(db)
            refreshAllGamesFromFirestore(db)
        }
    }

    private suspend fun syncUnsyncedEntries(db: FirebaseFirestore) {
        val unsynced = leaderboardDao.getUnsyncedEntries()
        for (entry in unsynced) {
            val game = GameRegistry.getGame(entry.gameId)
            pushEntryToFirestore(db, entry, game.title)
        }
    }

    private suspend fun refreshAllGamesFromFirestore(db: FirebaseFirestore) {
        val projId = db.app.options.projectId ?: "firebase-cloud"
        for (game in GameRegistry.games) {
            fetchTop10ForGame(db, game.id, game.scoreUnit)
        }
        _connectionState.value = FirestoreConnectionState.Connected(projId)
    }

    private suspend fun fetchTop10ForGame(
        db: FirebaseFirestore,
        gameId: String,
        defaultUnit: String
    ): List<LeaderboardEntry> = suspendCancellableCoroutine { cont ->
        try {
            db.collection(COLLECTION_LEADERBOARDS)
                .document(gameId)
                .collection(SUBCOLLECTION_SCORES)
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener { snapshot ->
                    val entries = snapshot.documents.mapNotNull { doc ->
                        val scoreVal = doc.getLong("score")?.toInt() ?: return@mapNotNull null
                        if (scoreVal <= 0) return@mapNotNull null
                        LeaderboardEntry(
                            id = doc.id,
                            gameId = doc.getString("gameId") ?: gameId,
                            playerName = doc.getString("playerName") ?: "PLAYER 1",
                            playerId = doc.getString("playerId") ?: "",
                            score = scoreVal,
                            scoreUnit = doc.getString("scoreUnit") ?: defaultUnit,
                            timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            isSynced = true
                        )
                    }
                    if (entries.isNotEmpty()) {
                        scope.launch { leaderboardDao.insertEntries(entries) }
                    }
                    if (cont.isActive) cont.resume(entries)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(emptyList())
                }
        } catch (_: Exception) {
            if (cont.isActive) cont.resume(emptyList())
        }
    }

    companion object {
        private const val COLLECTION_LEADERBOARDS = "leaderboards"
        private const val SUBCOLLECTION_SCORES = "scores"
        private const val KEY_PLAYER_ID = "player_id"
        private const val KEY_PLAYER_NAME = "player_name"
        private const val KEY_FB_PROJECT_ID = "fb_project_id"
        private const val KEY_FB_APP_ID = "fb_app_id"
        private const val KEY_FB_API_KEY = "fb_api_key"
    }
}
