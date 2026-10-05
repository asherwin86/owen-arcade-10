package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.LeaderboardEntry
import com.example.model.GameRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Arcade 10", appName)
    }

    @Test
    fun `leaderboard entry maps to firestore fields for all 10 games`() {
        assertEquals(10, GameRegistry.games.size)
        for (game in GameRegistry.games) {
            val entry = LeaderboardEntry(
                id = "${game.id}_test",
                gameId = game.id,
                playerName = "ACE",
                playerId = "p_123",
                score = 250,
                scoreUnit = game.scoreUnit,
                timestamp = 1700000000L
            )
            val map = entry.toFirestoreMap()
            assertEquals(game.id, map["gameId"])
            assertEquals("ACE", map["playerName"])
            assertEquals(250, map["score"])
            assertTrue(map.containsKey("timestamp"))
        }
    }
}
