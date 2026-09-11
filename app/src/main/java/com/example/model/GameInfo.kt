package com.example.model

enum class GameCategory(val displayName: String) {
    ALL("All Games"),
    ARCADE("Retro & Arcade"),
    PUZZLE("Puzzles & Brain"),
    STRATEGY("Classic Strategy")
}

data class GameInfo(
    val id: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val category: GameCategory,
    val iconEmoji: String,
    val accentColorHex: Long,
    val scoreUnit: String = "pts"
)

object GameRegistry {
    val games: List<GameInfo> = listOf(
        GameInfo(
            id = "snake",
            title = "Retro Snake",
            subtitle = "Classic Arcade",
            description = "Guide the hungry snake to eat fruit and grow without hitting walls or tail.",
            category = GameCategory.ARCADE,
            iconEmoji = "🐍",
            accentColorHex = 0xFF10B981,
            scoreUnit = "pts"
        ),
        GameInfo(
            id = "2048",
            title = "2048 Master",
            subtitle = "Number Puzzle",
            description = "Swipe and merge identical tiles together to reach the glorious 2048 tile!",
            category = GameCategory.PUZZLE,
            iconEmoji = "🔢",
            accentColorHex = 0xFFF59E0B,
            scoreUnit = "pts"
        ),
        GameInfo(
            id = "minesweeper",
            title = "Minesweeper",
            subtitle = "Grid Deduction",
            description = "Uncover safe tiles, flag hidden explosives, and clear the minefield.",
            category = GameCategory.PUZZLE,
            iconEmoji = "💣",
            accentColorHex = 0xFFEF4444,
            scoreUnit = "safe"
        ),
        GameInfo(
            id = "brick_breaker",
            title = "Brick Breaker",
            subtitle = "Paddle Action",
            description = "Bounce the energy sphere off your paddle to shatter every brick row.",
            category = GameCategory.ARCADE,
            iconEmoji = "🧱",
            accentColorHex = 0xFFEC4899,
            scoreUnit = "pts"
        ),
        GameInfo(
            id = "word_guess",
            title = "Word Guess",
            subtitle = "Daily Wordle Style",
            description = "Deduce the hidden 5-letter word within 6 tries using color-coded hints.",
            category = GameCategory.PUZZLE,
            iconEmoji = "🔤",
            accentColorHex = 0xFF8B5CF6,
            scoreUnit = "wins"
        ),
        GameInfo(
            id = "flappy_bird",
            title = "Tap Flight",
            subtitle = "Reflex Aviator",
            description = "Tap with rhythm to flap wings and navigate safely between pipes.",
            category = GameCategory.ARCADE,
            iconEmoji = "🐤",
            accentColorHex = 0xFF06B6D4,
            scoreUnit = "pipes"
        ),
        GameInfo(
            id = "tic_tac_toe",
            title = "Tic-Tac-Toe",
            subtitle = "3-in-a-Row",
            description = "Challenge smart computer AI or play pass-and-play with a friend.",
            category = GameCategory.STRATEGY,
            iconEmoji = "❌",
            accentColorHex = 0xFF3B82F6,
            scoreUnit = "wins"
        ),
        GameInfo(
            id = "connect_four",
            title = "Connect 4",
            subtitle = "Drop Strategy",
            description = "Drop discs into slots and be the first to align four of your color.",
            category = GameCategory.STRATEGY,
            iconEmoji = "🔴",
            accentColorHex = 0xFFF43F5E,
            scoreUnit = "wins"
        ),
        GameInfo(
            id = "memory_cards",
            title = "Memory Match",
            subtitle = "Card Recall",
            description = "Flip cards over and pair matching symbols with the fewest attempts.",
            category = GameCategory.PUZZLE,
            iconEmoji = "🃏",
            accentColorHex = 0xFF14B8A6,
            scoreUnit = "pts"
        ),
        GameInfo(
            id = "whack_a_mole",
            title = "Whack-A-Mole",
            subtitle = "Speed Reaction",
            description = "Tap moles as they peek from burrows before the 30-second timer runs out!",
            category = GameCategory.ARCADE,
            iconEmoji = "🔨",
            accentColorHex = 0xFFD97706,
            scoreUnit = "pts"
        )
    )

    fun getGame(id: String): GameInfo = games.firstOrNull { it.id == id } ?: games[0]
}
