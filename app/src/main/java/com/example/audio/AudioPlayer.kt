package com.example.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null

    fun playBase64Audio(base64Data: String) {
        stop() // Stop existing playback
        
        try {
            val audioBytes = Base64.decode(base64Data, Base64.DEFAULT)
            
            // Create a temp file to hold the audio
            val tempFile = File.createTempFile("temp_music", ".wav", context.cacheDir)
            tempFile.deleteOnExit()
            
            FileOutputStream(tempFile).use { fos ->
                fos.write(audioBytes)
            }
            
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
}
