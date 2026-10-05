package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {
    @Query(
        "SELECT * FROM leaderboard_entries " +
            "WHERE gameId = :gameId AND score > 0 " +
            "ORDER BY score DESC, timestamp ASC LIMIT 10"
    )
    fun getTopScoresForGame(gameId: String): Flow<List<LeaderboardEntry>>

    @Query(
        "SELECT * FROM leaderboard_entries " +
            "WHERE score > 0 " +
            "ORDER BY score DESC, timestamp ASC"
    )
    fun getAllScores(): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard_entries WHERE isSynced = 0 AND score > 0")
    suspend fun getUnsyncedEntries(): List<LeaderboardEntry>

    @Query("SELECT COUNT(*) FROM leaderboard_entries WHERE gameId = :gameId")
    suspend fun getCountForGame(gameId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: LeaderboardEntry)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<LeaderboardEntry>)

    @Query("UPDATE leaderboard_entries SET playerName = :newName, isSynced = 0 WHERE id = :entryId")
    suspend fun updatePlayerNameForEntry(entryId: String, newName: String)

    @Query("UPDATE leaderboard_entries SET isSynced = 1 WHERE id = :entryId")
    suspend fun markAsSynced(entryId: String)
}
