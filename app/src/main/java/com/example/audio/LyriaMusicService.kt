package com.example.audio

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.api.Content
import com.example.api.GenerateContentRequest
import com.example.api.GenerationConfig
import com.example.api.Part
import com.example.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

data class GeneratedMusicTrack(
    val id: String,
    val title: String,
    val gameId: String,
    val modelUsed: String,
    val prompt: String,
    val file: File,
    val mimeType: String,
    val timestampMs: Long = System.currentTimeMillis()
)

object LyriaMusicService {
    private const val TAG = "LyriaMusicService"

    // Supported Lyria Models as requested
    const val MODEL_CLIP = "lyria-3-clip-preview" // Short clips up to 30s
    const val MODEL_PRO = "lyria-3-pro-preview"   // Full-length tracks

    // Supported Prompt Enhancement model
    const val MODEL_GEMINI_FLASH = "gemini-flash-latest"

    /**
     * Generates custom music using Google's Lyria audio generation models.
     */
    suspend fun generateTrack(
        context: Context,
        gameId: String,
        prompt: String,
        modelName: String = MODEL_CLIP,
        customTitle: String? = null
    ): Result<GeneratedMusicTrack> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY

        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Missing Gemini API Key. Please configure GEMINI_API_KEY in the Secrets panel in AI Studio.")
            )
        }

        try {
            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt.trim())
                        )
                    )
                ),
                generationConfig = GenerationConfig(
                    responseModalities = listOf("AUDIO")
                )
            )

            Log.d(TAG, "Requesting music generation with model: $modelName, prompt: $prompt")
            val response = RetrofitClient.service.generateContent(
                model = modelName,
                apiKey = apiKey,
                request = request
            )

            val part = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()
            val inlineData = part?.inlineData

            if (inlineData == null || inlineData.data.isBlank()) {
                val textResponse = part?.text ?: "No audio candidate returned"
                return@withContext Result.failure(
                    IllegalStateException("Model responded without audio data: $textResponse")
                )
            }

            val audioBytes = Base64.decode(inlineData.data, Base64.DEFAULT)
            if (audioBytes == null || audioBytes.isEmpty()) {
                return@withContext Result.failure(
                    IllegalStateException("Failed to decode base64 audio stream.")
                )
            }

            val musicDir = File(context.filesDir, "custom_music").apply { mkdirs() }
            val trackId = "lyria_${gameId}_${System.currentTimeMillis()}"
            val extension = when {
                inlineData.mimeType.contains("mp3", ignoreCase = true) -> ".mp3"
                inlineData.mimeType.contains("aac", ignoreCase = true) -> ".aac"
                else -> ".wav"
            }
            val targetFile = File(musicDir, "$trackId$extension")

            saveAudioBytes(targetFile, audioBytes, inlineData.mimeType)

            val gameInfo = MusicTrackRegistry.getTrackForGame(gameId)
            val title = customTitle ?: "AI: ${gameInfo.name} (${if (modelName == MODEL_CLIP) "Clip" else "Pro"})"

            val track = GeneratedMusicTrack(
                id = trackId,
                title = title,
                gameId = gameId,
                modelUsed = modelName,
                prompt = prompt,
                file = targetFile,
                mimeType = inlineData.mimeType
            )

            Result.success(track)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating music with Lyria", e)
            Result.failure(e)
        }
    }

    /**
     * Enhances a music prompt using gemini-flash-latest to generate rich, evocative music descriptions.
     */
    suspend fun enhanceMusicPrompt(
        gameTitle: String,
        currentPrompt: String
    ): Result<String> = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext Result.failure(
                IllegalStateException("Missing Gemini API Key.")
            )
        }

        try {
            val systemInstruction = "You are a professional video game music composer and sound designer. " +
                    "Write a concise, vivid 1-2 sentence music generation prompt for Google Lyria to produce an authentic retro arcade background track."

            val promptText = "Enhance this music prompt for the arcade game '$gameTitle':\n\"$currentPrompt\"\n" +
                    "Focus on instruments (square waves, synth pads, drum machine), BPM, key, and mood. Output only the prompt text, no quotes or intro."

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = "$systemInstruction\n\n$promptText")
                        )
                    )
                )
            )

            val response = RetrofitClient.service.generateContent(
                model = MODEL_GEMINI_FLASH,
                apiKey = apiKey,
                request = request
            )

            val enhancedText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
            if (!enhancedText.isNullOrBlank()) {
                Result.success(enhancedText.removeSurrounding("\""))
            } else {
                Result.failure(IllegalStateException("No enhanced prompt generated"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error enhancing prompt with Gemini", e)
            Result.failure(e)
        }
    }

    private fun saveAudioBytes(file: File, bytes: ByteArray, mimeType: String) {
        // If it's already a container format (RIFF WAV, ID3 MP3), write directly
        val isRiff = bytes.size >= 4 && bytes[0] == 'R'.code.toByte() && bytes[1] == 'I'.code.toByte()
        val isMp3 = bytes.size >= 3 && ((bytes[0] == 'I'.code.toByte() && bytes[1] == 'D'.code.toByte()) || (bytes[0] == 0xFF.toByte()))

        if (isRiff || isMp3 || !mimeType.contains("pcm", ignoreCase = true)) {
            FileOutputStream(file).use { it.write(bytes) }
        } else {
            // Raw PCM 16-bit: wrap with standard WAV header (assuming 24000Hz mono or 44100Hz)
            val sampleRate = 24000
            val channels = 1
            val totalLength = bytes.size + 36
            val byteRate = sampleRate * channels * 2
            val header = ByteArray(44)
            val bb = ByteBuffer.wrap(header).order(ByteOrder.LITTLE_ENDIAN)
            bb.put("RIFF".toByteArray(Charsets.US_ASCII))
            bb.putInt(totalLength)
            bb.put("WAVE".toByteArray(Charsets.US_ASCII))
            bb.put("fmt ".toByteArray(Charsets.US_ASCII))
            bb.putInt(16)
            bb.putShort(1.toShort())
            bb.putShort(channels.toShort())
            bb.putInt(sampleRate)
            bb.putInt(byteRate)
            bb.putShort((channels * 2).toShort())
            bb.putShort(16.toShort())
            bb.put("data".toByteArray(Charsets.US_ASCII))
            bb.putInt(bytes.size)

            FileOutputStream(file).use {
                it.write(header)
                it.write(bytes)
            }
        }
    }
}
