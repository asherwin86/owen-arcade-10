package com.example.audio

/**
 * Information describing a background music track for a game or arcade area.
 */
data class GameTrackInfo(
    val id: String,
    val name: String,
    val gameTitle: String,
    val genre: String,
    val bpm: Int,
    val emoji: String,
    val styleDescription: String,
    val defaultAiPrompt: String
)

object MusicTrackRegistry {
    const val LOBBY_TRACK_ID = "arcade_lobby"

    val tracks: List<GameTrackInfo> = listOf(
        GameTrackInfo(
            id = LOBBY_TRACK_ID,
            name = "Neon Pixel Lounge",
            gameTitle = "Arcade Lobby",
            genre = "Chiptune Synthwave",
            bpm = 124,
            emoji = "🕹️",
            styleDescription = "Catchy upbeat retro arcade anthem with walking bass, bubbly arpeggios, and melodic synth hooks.",
            defaultAiPrompt = "An upbeat 124 BPM retro 8-bit chiptune arcade lobby anthem with catchy square wave melodies, walking bass, and crisp electronic drum machine percussion."
        ),
        GameTrackInfo(
            id = "snake",
            name = "Slither Grooves",
            gameTitle = "Retro Snake",
            genre = "Fast Electro Chiptune",
            bpm = 132,
            emoji = "🐍",
            styleDescription = "Pumping minor-scale serpentine runs with driving bassline and fast-paced evasion rhythm.",
            defaultAiPrompt = "A fast-paced 132 BPM electro chiptune track in minor key for an arcade snake game with driving syncopated bass, rapid square wave runs, and energetic beats."
        ),
        GameTrackInfo(
            id = "2048",
            name = "Binary Zen",
            gameTitle = "2048 Master",
            genre = "Chill Lo-Fi Puzzle",
            bpm = 102,
            emoji = "🔢",
            styleDescription = "Calm, thoughtful Rhodes chords with soothing harmonic arpeggios and relaxed puzzle groove.",
            defaultAiPrompt = "A chill 102 BPM lo-fi electronic puzzle theme with warm Rhodes electric piano chords, soft triangle wave arpeggios, and gentle ambient beats for strategic thinking."
        ),
        GameTrackInfo(
            id = "minesweeper",
            name = "Minefield Tension",
            gameTitle = "Minesweeper",
            genre = "Suspenseful Cyber Dark",
            bpm = 92,
            emoji = "💣",
            styleDescription = "Ticking clock percussion and dark mystery chords capturing the suspense of bomb defusal.",
            defaultAiPrompt = "A suspenseful 92 BPM dark ambient electronic track with ticking clock percussion, dramatic minor stabs, and subtle futuristic pulses for bomb defusal puzzle."
        ),
        GameTrackInfo(
            id = "brick_breaker",
            name = "Paddle Rush",
            gameTitle = "Brick Breaker",
            genre = "High-Energy Power Synth",
            bpm = 142,
            emoji = "🧱",
            styleDescription = "Heroic soaring arcade power-pop with driving kick drum, galloping bass, and soaring leads.",
            defaultAiPrompt = "A high-energy 142 BPM retro power arcade synth track with four-on-the-floor kick drum, soaring 8-bit leads, and energetic power chords for brick breaking action."
        ),
        GameTrackInfo(
            id = "word_guess",
            name = "Letter Lo-Fi",
            gameTitle = "Word Guess",
            genre = "Cozy Study Lo-Fi",
            bpm = 86,
            emoji = "🔤",
            styleDescription = "Relaxed, warm study beats with soothing jazz chords and space to ponder 5-letter words.",
            defaultAiPrompt = "A cozy 86 BPM lo-fi study beat with mellow jazz chords, subtle vinyl crackle, gentle flute-like melodies, and relaxed thinking groove for word games."
        ),
        GameTrackInfo(
            id = "flappy_bird",
            name = "Aviator Bounce",
            gameTitle = "Tap Flight",
            genre = "Whimsical Ragtime Swing",
            bpm = 136,
            emoji = "🐤",
            styleDescription = "Bouncy, buoyant swing melody with cheerful octave hops, flutter chirps, and lighthearted groove.",
            defaultAiPrompt = "A cheerful 136 BPM whimsical ragtime chiptune swing with bouncy staccato bass, playful flutter trills, and jaunty retro carnival melody for flight games."
        ),
        GameTrackInfo(
            id = "tic_tac_toe",
            name = "Duo Showdown",
            gameTitle = "Tic-Tac-Toe",
            genre = "Playful Strategy Duel",
            bpm = 116,
            emoji = "❌",
            styleDescription = "Call-and-response melodic duel with rhythmic syncopation and lighthearted duel vibes.",
            defaultAiPrompt = "A playful 116 BPM retro tabletop strategy melody with call-and-response square wave synths, quirky bleeps, and clever counterpoint rhythms."
        ),
        GameTrackInfo(
            id = "connect_four",
            name = "Grid Tactician",
            gameTitle = "Connect 4",
            genre = "Funk Chiptune Groove",
            bpm = 112,
            emoji = "🔴",
            styleDescription = "Cool funk-chiptune bassline with slick melodic motifs and syncopated counter-rhythm.",
            defaultAiPrompt = "A groovy 112 BPM funk chiptune track with syncopated slap bass, tight retro drums, and slick minor key synth lines for competitive board game battles."
        ),
        GameTrackInfo(
            id = "memory_cards",
            name = "Echo Chimes",
            gameTitle = "Memory Match",
            genre = "Sparkling Music Box",
            bpm = 96,
            emoji = "🃏",
            styleDescription = "Crystalline, magical music box arpeggios cascading like twinkling stars with gentle bell resonance.",
            defaultAiPrompt = "A dreamy 96 BPM crystalline music box theme with shimmering celestial arpeggios, gentle ambient chime resonance, and peaceful nostalgic melody."
        ),
        GameTrackInfo(
            id = "whack_a_mole",
            name = "Mole Carnival",
            gameTitle = "Whack-A-Mole",
            genre = "Frantic Carnival Polka",
            bpm = 154,
            emoji = "🔨",
            styleDescription = "High-speed comedic carnival polka with rapid chromatic runs, energetic bass gallop, and cartoon trills.",
            defaultAiPrompt = "A frantic 154 BPM high-speed carnival polka chiptune with comical chromatic runs, rapid accordion-style square waves, and frantic energetic arcade tempo."
        ),
        GameTrackInfo(
            id = "owen_tag",
            name = "Neon Cyber Chase",
            gameTitle = "Owen's Tag",
            genre = "Adrenaline Cyber Synth",
            bpm = 146,
            emoji = "🏃",
            styleDescription = "Driving adrenaline-fueled cyber chase synthwave with pounding kick, heavy bass, and tense sweeps.",
            defaultAiPrompt = "An intense 146 BPM cyberpunk chase synthwave track with heavy driving bassline, fast-paced evasion drums, and tense retro futuristic synthesizer hooks."
        )
    )

    fun getTrackForGame(gameId: String?): GameTrackInfo {
        if (gameId == null) return getTrackById(LOBBY_TRACK_ID)
        return tracks.firstOrNull { it.id == gameId } ?: getTrackById(LOBBY_TRACK_ID)
    }

    fun getTrackById(id: String): GameTrackInfo {
        return tracks.firstOrNull { it.id == id } ?: tracks.first()
    }
}
