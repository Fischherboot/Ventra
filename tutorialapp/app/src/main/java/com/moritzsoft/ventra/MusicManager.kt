package com.moritzsoft.ventra

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin

/**
 * Erzeugt und spielt eine einfache Melodie für den Lautstärke-Test.
 * Keine externe Datei nötig – die Töne werden als Sinuswellen berechnet.
 */
class MusicManager {

    companion object {
        private const val SAMPLE_RATE = 44100
        private const val FADE_DURATION_MS = 2000L
        private const val FADE_STEP_MS = 50L
        // Sehr leise Lautstärke – soll sanft im Hintergrund spielen
        private const val AMPLITUDE = 0.08
    }

    private var audioTrack: AudioTrack? = null
    private var isPlaying = false
    private var fadeThread: Thread? = null

    /**
     * Erzeugt die PCM-Daten einer einfachen, angenehmen Melodie.
     */
    private fun generateMelody(): ShortArray {
        val c5 = 523.25
        val d5 = 587.33
        val e5 = 659.25
        val g5 = 783.99
        val a5 = 880.00
        val c6 = 1046.50

        val melody = listOf(
            c5 to 400, e5 to 400, g5 to 400, c6 to 600,
            0.0 to 200,
            g5 to 400, e5 to 400, c5 to 600,
            0.0 to 300,
            d5 to 400, g5 to 400, a5 to 400, g5 to 600,
            0.0 to 200,
            e5 to 400, d5 to 400, c5 to 600,
            0.0 to 400,
            c5 to 400, e5 to 400, g5 to 400, c6 to 600,
            0.0 to 200,
            g5 to 400, e5 to 400, c5 to 600,
            0.0 to 300,
            e5 to 300, e5 to 300, d5 to 300, c5 to 300, d5 to 400, e5 to 400, c5 to 800,
            0.0 to 500
        )

        val totalSamples = melody.sumOf { (it.second * SAMPLE_RATE) / 1000 }
        val buffer = ShortArray(totalSamples)

        var offset = 0
        for ((freq, durationMs) in melody) {
            val numSamples = (durationMs * SAMPLE_RATE) / 1000
            val attackSamples = (numSamples * 0.05).toInt()
            val releaseSamples = (numSamples * 0.15).toInt()

            for (i in 0 until numSamples) {
                var sample = if (freq > 0) {
                    sin(2.0 * PI * freq * i / SAMPLE_RATE)
                } else {
                    0.0
                }

                val envelope = when {
                    i < attackSamples -> i.toDouble() / attackSamples
                    i > numSamples - releaseSamples -> (numSamples - i).toDouble() / releaseSamples
                    else -> 1.0
                }
                sample *= envelope * AMPLITUDE

                if (offset + i < buffer.size) {
                    buffer[offset + i] = (sample * Short.MAX_VALUE).toInt().toShort()
                }
            }
            offset += numSamples
        }

        return buffer
    }

    /**
     * Startet die Musikwiedergabe mit Fade-In.
     */
    fun startWithFadeIn() {
        if (isPlaying) return

        val melodyData = generateMelody()
        val bufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(melodyData.size * 2)

        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack?.write(melodyData, 0, melodyData.size)
        audioTrack?.setLoopPoints(0, melodyData.size, -1)
        audioTrack?.setVolume(0f)
        audioTrack?.play()
        isPlaying = true

        fadeThread = Thread {
            val steps = (FADE_DURATION_MS / FADE_STEP_MS).toInt()
            for (i in 1..steps) {
                if (!isPlaying) break
                val volume = i.toFloat() / steps
                try {
                    audioTrack?.setVolume(volume)
                    Thread.sleep(FADE_STEP_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }
        }.also { it.start() }
    }

    /**
     * Stoppt die Musik mit Fade-Out.
     */
    fun stopWithFadeOut(onComplete: (() -> Unit)? = null) {
        if (!isPlaying) {
            onComplete?.invoke()
            return
        }

        fadeThread?.interrupt()

        fadeThread = Thread {
            val steps = (FADE_DURATION_MS / FADE_STEP_MS).toInt()
            val currentVolume = 1.0f

            for (i in steps downTo 0) {
                if (!isPlaying) break
                val volume = (i.toFloat() / steps) * currentVolume
                try {
                    audioTrack?.setVolume(volume)
                    Thread.sleep(FADE_STEP_MS)
                } catch (_: InterruptedException) {
                    break
                }
            }

            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
            isPlaying = false
            onComplete?.invoke()
        }.also { it.start() }
    }

    /**
     * Sofort stoppen (z.B. bei onDestroy).
     */
    fun release() {
        isPlaying = false
        fadeThread?.interrupt()
        try {
            audioTrack?.stop()
        } catch (_: Exception) { }
        audioTrack?.release()
        audioTrack = null
    }
}
