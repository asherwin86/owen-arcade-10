package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameScoreDao {
    @Query("SELECT * FROM game_scores")
    fun getAllScores(): Flow<List<GameScore>>

    @Query("SELECT * FROM game_scores WHERE gameId = :gameId")
    fun getScoreForGame(gameId: String): Flow<GameScore?>

    @Query("SELECT * FROM game_scores WHERE gameId = :gameId")
    suspend fun getScoreSync(gameId: String): GameScore?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(score: GameScore)
}
