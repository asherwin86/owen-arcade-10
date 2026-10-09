package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.audio.ChiptuneSynthesizer
import com.example.audio.MusicTrackRegistry
import com.example.data.LeaderboardEntry
import com.example.model.GameRegistry
import com.example.update.AppReleaseInfo
import com.example.update.AppUpdateManager
import com.example.update.UpdateChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
    fun `leaderboard entry maps to firestore fields for all games including owen_tag`() {
        assertEquals(11, GameRegistry.games.size)
        assertTrue(GameRegistry.games.any { it.id == "owen_tag" })
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

    @Test
    fun `all arcade lobby and game tracks are registered and synthesize valid wav files`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val lobbyTrack = MusicTrackRegistry.getTrackForGame(null)
        assertEquals("arcade_lobby", lobbyTrack.id)
        val lobbyFile = ChiptuneSynthesizer.getOrCreateTrackFile(context, lobbyTrack.id)
        assertTrue(lobbyFile.exists() && lobbyFile.length() > 44)

        for (game in GameRegistry.games) {
            val track = MusicTrackRegistry.getTrackForGame(game.id)
            assertNotNull(track)
            val wavFile = ChiptuneSynthesizer.getOrCreateTrackFile(context, game.id)
            assertTrue("Expected WAV file for ${game.id}", wavFile.exists() && wavFile.length() > 44)
        }
    }

    @Test
    fun `app update manager enables auto update by default and persists channel settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = AppUpdateManager(context)
        assertTrue(manager.settings.value.autoCheckOnLaunch)
        assertTrue(manager.settings.value.autoInstallOtaPatches)
        assertTrue(manager.settings.value.backgroundAutoSync)

        manager.updateSettings { it.copy(channel = UpdateChannel.BETA) }
        assertEquals(UpdateChannel.BETA, manager.settings.value.channel)

        val otaRelease = AppReleaseInfo(
            versionName = "1.3.0",
            versionCode = 4,
            otaRevision = 9,
            releaseTitle = "Test Live OTA",
            releaseNotes = listOf("New arcade balancing")
        )
        manager.applyOtaUpdateNow(otaRelease, wasAutomatic = true)
        manager.release()
    }
}
