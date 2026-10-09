package com.example.audio

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pure Kotlin procedural chiptune synthesizer.
 * Generates authentic loopable 16-bit PCM WAV audio for the arcade lobby and all 10+ games.
 * Completely offline, zero dependencies, instantaneous (~10ms) generation with cache reuse.
 */
object ChiptuneSynthesizer {

    private const val SAMPLE_RATE = 22050

    fun getOrCreateTrackFile(context: Context, trackId: String): File {
        val dir = File(context.cacheDir, "bgm_tracks").apply { mkdirs() }
        val targetFile = File(dir, "bgm_${trackId}.wav")
        if (targetFile.exists() && targetFile.length() > 1000) {
            return targetFile
        }

        val trackConfig = getTrackConfig(trackId)
        val pcmData = synthesizeTrack(trackConfig)
        val wavHeader = createWavHeader(pcmData.size, SAMPLE_RATE, 1)

        FileOutputStream(targetFile).use { fos ->
            fos.write(wavHeader)
            fos.write(pcmData)
        }

        return targetFile
    }

    private fun createWavHeader(dataLength: Int, sampleRate: Int, channels: Int): ByteArray {
        val totalLength = dataLength + 36
        val byteRate = sampleRate * channels * 2
        val header = ByteArray(44)
        val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray(Charsets.US_ASCII))
        bb.putInt(totalLength)
        bb.put("WAVE".toByteArray(Charsets.US_ASCII))
        bb.put("fmt ".toByteArray(Charsets.US_ASCII))
        bb.putInt(16) // Subchunk1Size
        bb.putShort(1.toShort()) // AudioFormat (1 = PCM)
        bb.putShort(channels.toShort())
        bb.putInt(sampleRate)
        bb.putInt(byteRate)
        bb.putShort((channels * 2).toShort()) // BlockAlign
        bb.putShort(16.toShort()) // BitsPerSample
        bb.put("data".toByteArray(Charsets.US_ASCII))
        bb.putInt(dataLength)
        return header
    }

    private fun synthesizeTrack(config: TrackSynthesisConfig): ByteArray {
        val samplesPerStep = ((SAMPLE_RATE * 60.0) / (config.bpm * 4.0)).toInt()
        val totalSteps = config.leadNotes.size
        val totalSamples = totalSteps * samplesPerStep

        val buffer = ByteBuffer.allocate(totalSamples * 2).order(ByteOrder.LITTLE_ENDIAN)
        val random = Random(config.seed)

        var leadPhase = 0.0
        var arpPhase = 0.0
        var bassPhase = 0.0
        var kickPhase = 0.0

        for (s in 0 until totalSamples) {
            val step = (s / samplesPerStep).coerceIn(0, totalSteps - 1)
            val stepSample = s % samplesPerStep
            val stepProgress = stepSample.toDouble() / samplesPerStep

            // --- Lead Voice ---
            val leadMidi = config.leadNotes[step]
            var leadSample = 0.0
            if (leadMidi > 0) {
                val baseFreq = 440.0 * 2.0.pow((leadMidi - 69.0) / 12.0)
                val vibrato = if (stepProgress > 0.25) sin(2.0 * PI * 5.5 * stepProgress) * 2.5 else 0.0
                val freq = baseFreq + vibrato
                leadPhase = (leadPhase + freq / SAMPLE_RATE) % 1.0

                val rawLead = when (config.leadWave) {
                    WaveType.SQUARE_50 -> if (leadPhase < 0.5) 1.0 else -1.0
                    WaveType.PULSE_25 -> if (leadPhase < 0.25) 1.0 else -1.0
                    WaveType.PULSE_12 -> if (leadPhase < 0.125) 1.0 else -1.0
                    WaveType.TRIANGLE -> 2.0 * abs(2.0 * (leadPhase - floor(leadPhase + 0.5))) - 1.0
                }

                val env = when {
                    stepProgress < 0.05 -> stepProgress / 0.05
                    stepProgress < 0.80 -> 1.0 - (stepProgress - 0.05) * 0.25
                    else -> (1.0 - stepProgress) / 0.20 * 0.75
                }
                leadSample = rawLead * env.coerceIn(0.0, 1.0)
            }

            // --- Arpeggio Voice ---
            val bar = (step / 16) % config.chords.size
            val chord = config.chords[bar]
            val arpIdx = step % chord.size
            val arpMidi = chord[arpIdx]
            val arpFreq = 440.0 * 2.0.pow((arpMidi - 69.0) / 12.0)
            arpPhase = (arpPhase + arpFreq / SAMPLE_RATE) % 1.0
            val arpRaw = if (config.arpIsTriangle) {
                2.0 * abs(2.0 * (arpPhase - floor(arpPhase + 0.5))) - 1.0
            } else {
                if (arpPhase < 0.25) 0.8 else -0.8
            }
            val arpEnv = (1.0 - stepProgress * 0.5).coerceAtLeast(0.0)
            val arpSample = arpRaw * arpEnv

            // --- Bass Voice ---
            val bassMidi = config.bassNotes[step]
            var bassSample = 0.0
            if (bassMidi > 0) {
                val bassFreq = 440.0 * 2.0.pow((bassMidi - 69.0) / 12.0)
                bassPhase = (bassPhase + bassFreq / SAMPLE_RATE) % 1.0
                val bassRaw = if (config.bassIsSquare) {
                    if (bassPhase < 0.5) 1.0 else -1.0
                } else {
                    2.0 * abs(2.0 * (bassPhase - floor(bassPhase + 0.5))) - 1.0
                }
                val bassEnv = exp(-stepProgress * 3.2)
                bassSample = bassRaw * bassEnv
            }

            // --- Percussion Voice ---
            val stepInBar = step % 16
            val isKickStep = config.kickSteps.contains(stepInBar)
            val isSnareStep = config.snareSteps.contains(stepInBar)
            val isHatStep = config.hatSteps.contains(stepInBar)

            var kickSample = 0.0
            if (isKickStep && stepProgress < 0.25) {
                val kFreq = 140.0 * exp(-stepProgress * 18.0) + 40.0
                kickPhase = (kickPhase + kFreq / SAMPLE_RATE) % 1.0
                kickSample = sin(2.0 * PI * kickPhase) * exp(-stepProgress * 12.0)
            }

            var snareSample = 0.0
            if (isSnareStep && stepProgress < 0.30) {
                val noise = random.nextDouble() * 2.0 - 1.0
                snareSample = noise * exp(-stepProgress * 12.0)
            }

            var hatSample = 0.0
            if (isHatStep && stepProgress < 0.08) {
                val noise = random.nextDouble() * 2.0 - 1.0
                hatSample = noise * exp(-stepProgress * 35.0)
            }

            val drumSample = kickSample * 0.7 + snareSample * 0.45 + hatSample * 0.25

            // --- Master Mix ---
            val mixed = leadSample * config.leadVolume +
                    arpSample * config.arpVolume +
                    bassSample * config.bassVolume +
                    drumSample * config.drumVolume

            val clamped = mixed.coerceIn(-0.95, 0.95)
            val sampleShort = (clamped * 32767.0).toInt().toShort()
            buffer.putShort(sampleShort)
        }

        return buffer.array()
    }

    private enum class WaveType {
        SQUARE_50, PULSE_25, PULSE_12, TRIANGLE
    }

    private data class TrackSynthesisConfig(
        val bpm: Int,
        val seed: Long,
        val leadWave: WaveType,
        val arpIsTriangle: Boolean,
        val bassIsSquare: Boolean,
        val leadVolume: Double = 0.36,
        val arpVolume: Double = 0.24,
        val bassVolume: Double = 0.28,
        val drumVolume: Double = 0.22,
        val kickSteps: Set<Int>,
        val snareSteps: Set<Int>,
        val hatSteps: Set<Int>,
        val chords: List<IntArray>,
        val leadNotes: IntArray,
        val bassNotes: IntArray
    )

    private fun getTrackConfig(trackId: String): TrackSynthesisConfig {
        return when (trackId) {
            "snake" -> buildSnakeConfig()
            "2048" -> build2048Config()
            "minesweeper" -> buildMinesweeperConfig()
            "brick_breaker" -> buildBrickBreakerConfig()
            "word_guess" -> buildWordGuessConfig()
            "flappy_bird" -> buildFlappyBirdConfig()
            "tic_tac_toe" -> buildTicTacToeConfig()
            "connect_four" -> buildConnectFourConfig()
            "memory_cards" -> buildMemoryMatchConfig()
            "whack_a_mole" -> buildWhackAMoleConfig()
            "owen_tag" -> buildOwenTagConfig()
            else -> buildLobbyConfig()
        }
    }

    // 1. ARCADE LOBBY: "Neon Pixel Lounge" (124 BPM, C Major, Upbeat Anthem)
    private fun buildLobbyConfig(): TrackSynthesisConfig {
        // Chords: C (60,64,67,72), Am (57,60,64,69), F (53,57,60,65), G (55,59,62,67)
        val chords = listOf(
            intArrayOf(60, 64, 67, 72),
            intArrayOf(57, 60, 64, 69),
            intArrayOf(53, 57, 60, 65),
            intArrayOf(55, 59, 62, 67)
        )
        // 64 steps (4 bars)
        val lead = intArrayOf(
            72, 0, 72, 74, 76, 0, 76, 79, 76, 0, 74, 0, 72, 0, 67, 0,
            69, 0, 69, 72, 74, 0, 74, 76, 74, 0, 72, 0, 69, 0, 64, 0,
            65, 0, 65, 69, 72, 0, 72, 76, 74, 0, 72, 0, 69, 0, 65, 0,
            67, 0, 71, 74, 76, 0, 79, 0, 76, 0, 74, 72, 74, 0, 72, 0
        )
        val bass = intArrayOf(
            48, 0, 48, 60, 48, 0, 48, 60, 48, 0, 48, 60, 48, 0, 55, 0,
            45, 0, 45, 57, 45, 0, 45, 57, 45, 0, 45, 57, 45, 0, 52, 0,
            41, 0, 41, 53, 41, 0, 41, 53, 41, 0, 41, 53, 41, 0, 48, 0,
            43, 0, 43, 55, 43, 0, 43, 55, 43, 0, 43, 55, 43, 0, 50, 0
        )
        return TrackSynthesisConfig(
            bpm = 124,
            seed = 1001L,
            leadWave = WaveType.SQUARE_50,
            arpIsTriangle = false,
            bassIsSquare = false,
            kickSteps = setOf(0, 8),
            snareSteps = setOf(4, 12),
            hatSteps = setOf(2, 6, 10, 14),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 2. RETRO SNAKE: "Slither Grooves" (132 BPM, E Minor, Fast Serpentine Runs)
    private fun buildSnakeConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(52, 55, 59, 64), // Em
            intArrayOf(48, 52, 55, 60), // C
            intArrayOf(45, 48, 52, 57), // Am
            intArrayOf(47, 51, 54, 59)  // B7
        )
        val lead = intArrayOf(
            64, 67, 71, 74, 76, 74, 71, 67, 64, 0, 67, 0, 71, 74, 76, 0,
            60, 64, 67, 72, 74, 72, 67, 64, 60, 0, 64, 0, 67, 72, 74, 0,
            57, 60, 64, 69, 71, 69, 64, 60, 57, 0, 60, 0, 64, 69, 71, 0,
            59, 63, 66, 71, 74, 71, 66, 63, 59, 0, 63, 66, 71, 74, 76, 74
        )
        val bass = intArrayOf(
            40, 40, 52, 40, 40, 40, 52, 40, 40, 40, 52, 40, 40, 40, 47, 40,
            36, 36, 48, 36, 36, 36, 48, 36, 36, 36, 48, 36, 36, 36, 43, 36,
            33, 33, 45, 33, 33, 33, 45, 33, 33, 33, 45, 33, 33, 33, 40, 33,
            35, 35, 47, 35, 35, 35, 47, 35, 35, 35, 47, 35, 35, 35, 42, 35
        )
        return TrackSynthesisConfig(
            bpm = 132,
            seed = 2002L,
            leadWave = WaveType.PULSE_25,
            arpIsTriangle = true,
            bassIsSquare = true,
            kickSteps = setOf(0, 6, 8, 14),
            snareSteps = setOf(4, 12),
            hatSteps = setOf(0, 2, 4, 6, 8, 10, 12, 14),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 3. 2048 MASTER: "Binary Zen" (102 BPM, D Major, Chill Ambient Puzzle)
    private fun build2048Config(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(50, 54, 57, 62), // Dmaj7
            intArrayOf(47, 50, 54, 59), // Bm7
            intArrayOf(43, 47, 50, 55), // Gmaj7
            intArrayOf(45, 49, 52, 57)  // A7
        )
        val lead = intArrayOf(
            62, 0, 0, 0, 66, 0, 0, 0, 69, 0, 71, 0, 69, 0, 66, 0,
            59, 0, 0, 0, 62, 0, 0, 0, 66, 0, 69, 0, 66, 0, 62, 0,
            55, 0, 0, 0, 59, 0, 0, 0, 62, 0, 66, 0, 62, 0, 59, 0,
            57, 0, 0, 0, 61, 0, 0, 0, 64, 0, 67, 0, 64, 0, 61, 0
        )
        val bass = intArrayOf(
            38, 0, 0, 0, 38, 0, 0, 0, 50, 0, 0, 0, 45, 0, 0, 0,
            35, 0, 0, 0, 35, 0, 0, 0, 47, 0, 0, 0, 42, 0, 0, 0,
            31, 0, 0, 0, 31, 0, 0, 0, 43, 0, 0, 0, 38, 0, 0, 0,
            33, 0, 0, 0, 33, 0, 0, 0, 45, 0, 0, 0, 40, 0, 0, 0
        )
        return TrackSynthesisConfig(
            bpm = 102,
            seed = 3003L,
            leadWave = WaveType.TRIANGLE,
            arpIsTriangle = true,
            bassIsSquare = false,
            kickSteps = setOf(0, 10),
            snareSteps = setOf(8),
            hatSteps = setOf(4, 12),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 4. MINESWEEPER: "Minefield Tension" (92 BPM, D Minor, Ticking Clock Suspense)
    private fun buildMinesweeperConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(50, 53, 57, 62), // Dm
            intArrayOf(46, 50, 53, 58), // Bb
            intArrayOf(43, 46, 50, 55), // Gm
            intArrayOf(45, 49, 52, 57)  // A
        )
        val lead = intArrayOf(
            62, 0, 65, 0, 62, 0, 0, 0, 69, 0, 68, 0, 65, 0, 62, 0,
            58, 0, 62, 0, 58, 0, 0, 0, 65, 0, 64, 0, 62, 0, 58, 0,
            55, 0, 58, 0, 55, 0, 0, 0, 62, 0, 61, 0, 58, 0, 55, 0,
            57, 0, 61, 0, 64, 0, 67, 0, 69, 0, 67, 0, 64, 0, 61, 0
        )
        val bass = intArrayOf(
            38, 0, 38, 0, 38, 0, 38, 0, 38, 0, 38, 0, 38, 0, 38, 0,
            34, 0, 34, 0, 34, 0, 34, 0, 34, 0, 34, 0, 34, 0, 34, 0,
            31, 0, 31, 0, 31, 0, 31, 0, 31, 0, 31, 0, 31, 0, 31, 0,
            33, 0, 33, 0, 33, 0, 33, 0, 33, 0, 33, 0, 33, 0, 33, 0
        )
        return TrackSynthesisConfig(
            bpm = 92,
            seed = 4004L,
            leadWave = WaveType.PULSE_12,
            arpIsTriangle = false,
            bassIsSquare = false,
            kickSteps = setOf(0),
            snareSteps = setOf(8),
            // Ticking clock hi-hat on every 16th note!
            hatSteps = setOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 5. BRICK BREAKER: "Paddle Rush" (142 BPM, A Major, Power-Pop Chiptune)
    private fun buildBrickBreakerConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(57, 61, 64, 69), // A
            intArrayOf(54, 57, 61, 66), // F#m
            intArrayOf(50, 54, 57, 62), // D
            intArrayOf(52, 56, 59, 64)  // E
        )
        val lead = intArrayOf(
            69, 0, 69, 73, 76, 0, 81, 0, 76, 0, 73, 0, 69, 0, 73, 76,
            66, 0, 66, 69, 73, 0, 78, 0, 73, 0, 69, 0, 66, 0, 69, 73,
            62, 0, 62, 66, 69, 0, 74, 0, 69, 0, 66, 0, 62, 0, 66, 69,
            64, 0, 68, 71, 76, 0, 80, 0, 76, 0, 71, 0, 68, 0, 71, 76
        )
        val bass = intArrayOf(
            45, 45, 57, 45, 45, 45, 57, 45, 45, 45, 57, 45, 45, 45, 52, 45,
            42, 42, 54, 42, 42, 42, 54, 42, 42, 42, 54, 42, 42, 42, 49, 42,
            38, 38, 50, 38, 38, 38, 50, 38, 38, 38, 50, 38, 38, 38, 45, 38,
            40, 40, 52, 40, 40, 40, 52, 40, 40, 40, 52, 40, 40, 40, 47, 40
        )
        return TrackSynthesisConfig(
            bpm = 142,
            seed = 5005L,
            leadWave = WaveType.SQUARE_50,
            arpIsTriangle = false,
            bassIsSquare = true,
            kickSteps = setOf(0, 4, 8, 12), // Four-on-the-floor!
            snareSteps = setOf(4, 12),
            hatSteps = setOf(2, 6, 10, 14),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 6. WORD GUESS: "Letter Lo-Fi" (86 BPM, F Major, Cozy Study Beats)
    private fun buildWordGuessConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(53, 57, 60, 64), // Fmaj7
            intArrayOf(50, 53, 57, 60), // Dm7
            intArrayOf(46, 50, 53, 57), // Gm7
            intArrayOf(48, 52, 55, 58)  // C7
        )
        val lead = intArrayOf(
            65, 0, 0, 69, 72, 0, 76, 0, 72, 0, 69, 0, 65, 0, 0, 0,
            62, 0, 0, 65, 69, 0, 72, 0, 69, 0, 65, 0, 62, 0, 0, 0,
            58, 0, 0, 62, 65, 0, 69, 0, 65, 0, 62, 0, 58, 0, 0, 0,
            60, 0, 0, 64, 67, 0, 70, 0, 67, 0, 64, 0, 60, 0, 0, 0
        )
        val bass = intArrayOf(
            41, 0, 0, 0, 41, 0, 0, 0, 53, 0, 0, 0, 48, 0, 0, 0,
            38, 0, 0, 0, 38, 0, 0, 0, 50, 0, 0, 0, 45, 0, 0, 0,
            34, 0, 0, 0, 34, 0, 0, 0, 46, 0, 0, 0, 41, 0, 0, 0,
            36, 0, 0, 0, 36, 0, 0, 0, 48, 0, 0, 0, 43, 0, 0, 0
        )
        return TrackSynthesisConfig(
            bpm = 86,
            seed = 6006L,
            leadWave = WaveType.TRIANGLE,
            arpIsTriangle = true,
            bassIsSquare = false,
            kickSteps = setOf(0, 7),
            snareSteps = setOf(8),
            hatSteps = setOf(2, 4, 10, 12),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 7. TAP FLIGHT: "Aviator Bounce" (136 BPM, G Major, Ragtime Swing)
    private fun buildFlappyBirdConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(55, 59, 62, 67), // G
            intArrayOf(52, 55, 59, 64), // Em
            intArrayOf(48, 52, 55, 60), // C
            intArrayOf(50, 54, 57, 62)  // D
        )
        val lead = intArrayOf(
            67, 0, 71, 0, 74, 76, 74, 71, 67, 0, 74, 0, 71, 0, 67, 0,
            64, 0, 67, 0, 71, 72, 71, 67, 64, 0, 71, 0, 67, 0, 64, 0,
            60, 0, 64, 0, 67, 69, 67, 64, 60, 0, 67, 0, 64, 0, 60, 0,
            62, 0, 66, 0, 69, 71, 69, 66, 62, 0, 69, 0, 71, 74, 76, 74
        )
        val bass = intArrayOf(
            43, 0, 55, 0, 43, 0, 55, 0, 43, 0, 55, 0, 43, 0, 50, 0,
            40, 0, 52, 0, 40, 0, 52, 0, 40, 0, 52, 0, 40, 0, 47, 0,
            36, 0, 48, 0, 36, 0, 48, 0, 36, 0, 48, 0, 36, 0, 43, 0,
            38, 0, 50, 0, 38, 0, 50, 0, 38, 0, 50, 0, 38, 0, 45, 0
        )
        return TrackSynthesisConfig(
            bpm = 136,
            seed = 7007L,
            leadWave = WaveType.PULSE_25,
            arpIsTriangle = false,
            bassIsSquare = false,
            kickSteps = setOf(0, 8),
            snareSteps = setOf(4, 12),
            hatSteps = setOf(2, 6, 10, 14),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 8. TIC-TAC-TOE: "Duo Showdown" (116 BPM, C Major, Playful Strategy Duel)
    private fun buildTicTacToeConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(60, 64, 67), // C
            intArrayOf(55, 59, 62), // G
            intArrayOf(57, 60, 64), // Am
            intArrayOf(53, 57, 60)  // F
        )
        // Call and response phrasing
        val lead = intArrayOf(
            72, 72, 76, 0, 79, 0, 76, 0, 0, 0, 0, 0, 74, 74, 71, 0,
            67, 0, 71, 0, 74, 0, 71, 0, 0, 0, 0, 0, 69, 69, 64, 0,
            69, 69, 72, 0, 76, 0, 72, 0, 0, 0, 0, 0, 74, 72, 69, 0,
            65, 0, 69, 0, 72, 0, 76, 0, 74, 0, 72, 0, 74, 0, 72, 0
        )
        val bass = intArrayOf(
            48, 0, 48, 0, 60, 0, 48, 0, 48, 0, 48, 0, 55, 0, 48, 0,
            43, 0, 43, 0, 55, 0, 43, 0, 43, 0, 43, 0, 50, 0, 43, 0,
            45, 0, 45, 0, 57, 0, 45, 0, 45, 0, 45, 0, 52, 0, 45, 0,
            41, 0, 41, 0, 53, 0, 41, 0, 41, 0, 41, 0, 48, 0, 41, 0
        )
        return TrackSynthesisConfig(
            bpm = 116,
            seed = 8008L,
            leadWave = WaveType.PULSE_12,
            arpIsTriangle = true,
            bassIsSquare = true,
            kickSteps = setOf(0, 8),
            snareSteps = setOf(4, 12),
            hatSteps = setOf(2, 6, 10, 14),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 9. CONNECT 4: "Grid Tactician" (112 BPM, A Minor, Funk Chiptune Groove)
    private fun buildConnectFourConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(57, 60, 64, 69), // Am
            intArrayOf(50, 53, 57, 62), // Dm
            intArrayOf(53, 57, 60, 65), // F
            intArrayOf(52, 56, 59, 64)  // E7
        )
        val lead = intArrayOf(
            69, 0, 69, 72, 0, 76, 74, 72, 69, 0, 72, 0, 69, 0, 64, 0,
            62, 0, 62, 65, 0, 69, 67, 65, 62, 0, 65, 0, 62, 0, 57, 0,
            65, 0, 65, 69, 0, 72, 71, 69, 65, 0, 69, 0, 65, 0, 60, 0,
            64, 0, 68, 71, 0, 74, 72, 71, 68, 0, 71, 0, 74, 76, 74, 71
        )
        val bass = intArrayOf(
            45, 0, 45, 57, 0, 45, 52, 0, 45, 0, 45, 57, 0, 45, 52, 0,
            38, 0, 38, 50, 0, 38, 45, 0, 38, 0, 38, 50, 0, 38, 45, 0,
            41, 0, 41, 53, 0, 41, 48, 0, 41, 0, 41, 53, 0, 41, 48, 0,
            40, 0, 40, 52, 0, 40, 47, 0, 40, 0, 40, 52, 0, 40, 47, 0
        )
        return TrackSynthesisConfig(
            bpm = 112,
            seed = 9009L,
            leadWave = WaveType.SQUARE_50,
            arpIsTriangle = false,
            bassIsSquare = true,
            kickSteps = setOf(0, 6, 10),
            snareSteps = setOf(4, 12),
            hatSteps = setOf(2, 8, 14),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 10. MEMORY MATCH: "Echo Chimes" (96 BPM, E Major, Music Box Arpeggios)
    private fun buildMemoryMatchConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(52, 56, 59, 64, 68), // E
            intArrayOf(49, 52, 56, 61, 64), // C#m
            intArrayOf(45, 49, 52, 57, 61), // A
            intArrayOf(47, 51, 54, 59, 63)  // B
        )
        val lead = intArrayOf(
            76, 0, 80, 0, 83, 0, 88, 0, 83, 0, 80, 0, 76, 0, 71, 0,
            73, 0, 76, 0, 80, 0, 85, 0, 80, 0, 76, 0, 73, 0, 68, 0,
            69, 0, 73, 0, 76, 0, 81, 0, 76, 0, 73, 0, 69, 0, 64, 0,
            71, 0, 75, 0, 78, 0, 83, 0, 78, 0, 75, 0, 71, 0, 76, 0
        )
        val bass = intArrayOf(
            40, 0, 0, 0, 40, 0, 0, 0, 52, 0, 0, 0, 47, 0, 0, 0,
            37, 0, 0, 0, 37, 0, 0, 0, 49, 0, 0, 0, 44, 0, 0, 0,
            33, 0, 0, 0, 33, 0, 0, 0, 45, 0, 0, 0, 40, 0, 0, 0,
            35, 0, 0, 0, 35, 0, 0, 0, 47, 0, 0, 0, 42, 0, 0, 0
        )
        return TrackSynthesisConfig(
            bpm = 96,
            seed = 1010L,
            leadWave = WaveType.TRIANGLE,
            arpIsTriangle = true,
            bassIsSquare = false,
            kickSteps = setOf(0),
            snareSteps = setOf(8),
            hatSteps = setOf(4, 12),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 11. WHACK-A-MOLE: "Mole Carnival" (154 BPM, F Major, Frantic Carnival Polka)
    private fun buildWhackAMoleConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(53, 57, 60, 65), // F
            intArrayOf(48, 52, 55, 60), // C
            intArrayOf(53, 57, 60, 65), // F
            intArrayOf(50, 53, 57, 62)  // Bb / Dm
        )
        val lead = intArrayOf(
            65, 66, 67, 68, 69, 0, 69, 72, 69, 0, 65, 0, 69, 0, 65, 0,
            60, 61, 62, 63, 64, 0, 64, 67, 64, 0, 60, 0, 64, 0, 60, 0,
            65, 66, 67, 68, 69, 0, 69, 72, 69, 0, 65, 0, 69, 0, 72, 0,
            70, 0, 69, 0, 67, 0, 65, 0, 64, 0, 62, 0, 60, 62, 64, 65
        )
        val bass = intArrayOf(
            41, 0, 53, 0, 41, 0, 53, 0, 41, 0, 53, 0, 41, 0, 48, 0,
            36, 0, 48, 0, 36, 0, 48, 0, 36, 0, 48, 0, 36, 0, 43, 0,
            41, 0, 53, 0, 41, 0, 53, 0, 41, 0, 53, 0, 41, 0, 48, 0,
            34, 0, 46, 0, 34, 0, 46, 0, 36, 0, 48, 0, 41, 0, 53, 0
        )
        return TrackSynthesisConfig(
            bpm = 154,
            seed = 1111L,
            leadWave = WaveType.PULSE_25,
            arpIsTriangle = false,
            bassIsSquare = true,
            kickSteps = setOf(0, 8),
            snareSteps = setOf(4, 12),
            hatSteps = setOf(2, 6, 10, 14),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }

    // 12. OWEN'S TAG: "Neon Cyber Chase" (146 BPM, B Minor, Evasion Synthwave)
    private fun buildOwenTagConfig(): TrackSynthesisConfig {
        val chords = listOf(
            intArrayOf(47, 50, 54, 59), // Bm
            intArrayOf(43, 47, 50, 55), // G
            intArrayOf(40, 43, 47, 52), // Em
            intArrayOf(42, 46, 49, 54)  // F#
        )
        val lead = intArrayOf(
            71, 71, 74, 71, 76, 74, 71, 0, 71, 74, 76, 79, 76, 74, 71, 0,
            67, 67, 71, 67, 74, 71, 67, 0, 67, 71, 74, 76, 74, 71, 67, 0,
            64, 64, 67, 64, 71, 67, 64, 0, 64, 67, 71, 74, 71, 67, 64, 0,
            66, 66, 70, 66, 73, 70, 66, 0, 66, 70, 73, 75, 73, 70, 66, 71
        )
        val bass = intArrayOf(
            35, 35, 47, 35, 35, 35, 47, 35, 35, 35, 47, 35, 35, 35, 42, 35,
            31, 31, 43, 31, 31, 31, 43, 31, 31, 31, 43, 31, 31, 31, 38, 31,
            28, 28, 40, 28, 28, 28, 40, 28, 28, 28, 40, 28, 28, 28, 35, 28,
            30, 30, 42, 30, 30, 30, 42, 30, 30, 30, 42, 30, 30, 30, 37, 30
        )
        return TrackSynthesisConfig(
            bpm = 146,
            seed = 1212L,
            leadWave = WaveType.PULSE_12,
            arpIsTriangle = true,
            bassIsSquare = true,
            kickSteps = setOf(0, 4, 8, 12),
            snareSteps = setOf(4, 12),
            hatSteps = setOf(0, 2, 4, 6, 8, 10, 12, 14),
            chords = chords,
            leadNotes = lead,
            bassNotes = bass
        )
    }
}
