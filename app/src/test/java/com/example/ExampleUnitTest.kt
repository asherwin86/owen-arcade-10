package com.example

import com.example.audio.MusicTrackRegistry
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying audio and music track configurations.
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testMusicTrackRegistry_containsAllGamesAndLobby() {
    val tracks = MusicTrackRegistry.tracks
    assertTrue("Should have at least 12 soundtrack themes", tracks.size >= 12)

    val trackIds = tracks.map { it.id }.toSet()
    assertTrue(trackIds.contains(MusicTrackRegistry.LOBBY_TRACK_ID))
    assertTrue(trackIds.contains("snake"))
    assertTrue(trackIds.contains("2048"))
    assertTrue(trackIds.contains("minesweeper"))
    assertTrue(trackIds.contains("brick_breaker"))
    assertTrue(trackIds.contains("word_guess"))
    assertTrue(trackIds.contains("flappy_bird"))
    assertTrue(trackIds.contains("tic_tac_toe"))
    assertTrue(trackIds.contains("connect_four"))
    assertTrue(trackIds.contains("memory_cards"))
    assertTrue(trackIds.contains("whack_a_mole"))
    assertTrue(trackIds.contains("owen_tag"))

    tracks.forEach { track ->
      assertTrue("Track ${track.id} name should not be blank", track.name.isNotBlank())
      assertTrue("Track ${track.id} BPM should be positive", track.bpm in 60..200)
      assertTrue("Track ${track.id} default AI prompt should not be blank", track.defaultAiPrompt.isNotBlank())
    }
  }

  @Test
  fun testGetTrackForGame_fallbackToLobby() {
    val nullTrack = MusicTrackRegistry.getTrackForGame(null)
    assertEquals(MusicTrackRegistry.LOBBY_TRACK_ID, nullTrack.id)

    val snakeTrack = MusicTrackRegistry.getTrackForGame("snake")
    assertEquals("snake", snakeTrack.id)
    assertEquals("Slither Grooves", snakeTrack.name)
  }
}

