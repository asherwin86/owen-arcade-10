package com.example.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "leaderboard_entries",
    indices = [Index(value = ["gameId", "score"])]
)
data class LeaderboardEntry(
    @PrimaryKey val id: String,
    val gameId: String,
    val playerName: String,
    val playerId: String,
    val score: Int,
    val scoreUnit: String = "pts",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "gameId" to gameId,
        "playerName" to playerName,
        "playerId" to playerId,
        "score" to score,
        "scoreUnit" to scoreUnit,
        "timestamp" to timestamp
    )
}
