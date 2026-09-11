package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class GameScoreRepository(private val dao: GameScoreDao) {
    val allScores: Flow<Map<String, GameScore>> = dao.getAllScores().map { list ->
        list.associateBy { it.gameId }
    }

    fun getScoreForGame(gameId: String): Flow<GameScore?> = dao.getScoreForGame(gameId)

    suspend fun recordGameFinished(gameId: String, score: Int) {
        val existing = dao.getScoreSync(gameId)
        val currentBest = existing?.highScore ?: 0
        val gamesPlayed = (existing?.gamesPlayed ?: 0) + 1
        val newBest = if (score > currentBest) score else currentBest

        dao.insertOrUpdate(
            GameScore(
                gameId = gameId,
                highScore = newBest,
                gamesPlayed = gamesPlayed,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }
}
