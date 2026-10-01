package com.example.data.remote

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Random
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

enum class SoundPreset(
    val title: String,
    val description: String,
    val icon: String,
    val defaultDurationSec: Float
) {
    AMBIENT_DRONE("Ambient 432Hz Drone", "Deep harmonic calming vibration for focus & meditation", "🧘", 4.0f),
    CHIME_TRIAD("Notification Chime", "C-Major harmonic chime arpeggio", "🔔", 1.8f),
    SCIFI_LASER("Cyberpunk Laser", "High-frequency down-sweep energy beam", "⚡", 0.6f),
    SOOTHING_RAIN("Gentle Rain & White Noise", "Relaxing continuous rain ambiance", "🌧️", 5.0f),
    RETRO_COIN("8-Bit Retro Coin", "Nostalgic arcade square-wave chirp", "🎮", 0.4f),
    BASS_DROP("Sub-Bass Impact", "Deep sub-harmonic bass drop resonance", "💥", 2.0f),
    COSMIC_PAD("Cosmic Synth Pad", "Modulated dual sine wave space texture", "🌌", 4.0f)
}

object AudioSynthesizer {
    private const val SAMPLE_RATE = 44100
    private var currentTrack: AudioTrack? = null
    var isPlaying: Boolean = false
        private set

    suspend fun playSound(preset: SoundPreset, volume: Float = 0.8f, onComplete: () -> Unit = {}): Unit = withContext(Dispatchers.Default) {
        stopSound()
        try {
            val samples = generateSamples(preset)
            playSamples(samples, volume, onComplete)
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) { onComplete() }
        }
    }

    fun stopSound() {
        try {
            isPlaying = false
            currentTrack?.stop()
            currentTrack?.release()
            currentTrack = null
        } catch (_: Exception) {}
    }

    private fun generateSamples(preset: SoundPreset): ShortArray {
        val duration = preset.defaultDurationSec
        val totalSamples = (SAMPLE_RATE * duration).toInt()
        val buffer = ShortArray(totalSamples)
        val random = Random()

        when (preset) {
            SoundPreset.AMBIENT_DRONE -> {
                val f1 = 432.0 // base
                val f2 = 432.0 * 1.5 // fifth
                val f3 = 432.0 * 2.0 // octave
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val envelope = sin(PI * (i.toDouble() / totalSamples)) // fade in/out
                    val sample = (0.5 * sin(2.0 * PI * f1 * t) +
                            0.3 * sin(2.0 * PI * f2 * t) +
                            0.2 * sin(2.0 * PI * f3 * t)) * envelope
                    buffer[i] = (sample * 30000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }

            SoundPreset.CHIME_TRIAD -> {
                // C5 (523Hz), E5 (659Hz), G5 (784Hz), C6 (1046Hz)
                val notes = doubleArrayOf(523.25, 659.25, 783.99, 1046.50)
                val noteDuration = totalSamples / notes.size
                for (i in 0 until totalSamples) {
                    val noteIndex = (i / noteDuration).coerceAtMost(notes.size - 1)
                    val noteTime = (i % noteDuration).toDouble() / SAMPLE_RATE
                    val decay = exp(-noteTime * 4.5)
                    val freq = notes[noteIndex]
                    val sample = sin(2.0 * PI * freq * noteTime) * decay
                    buffer[i] = (sample * 28000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }

            SoundPreset.SCIFI_LASER -> {
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val freq = 2400.0 * (1.0 - progress) * (1.0 - progress) + 120.0
                    val t = i.toDouble() / SAMPLE_RATE
                    val sample = sin(2.0 * PI * freq * t) * (1.0 - progress)
                    buffer[i] = (sample * 31000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }

            SoundPreset.SOOTHING_RAIN -> {
                var lastVal = 0.0
                for (i in 0 until totalSamples) {
                    val white = (random.nextDouble() * 2.0 - 1.0)
                    // Low-pass filter for pink/brown soft rain sound
                    lastVal = (lastVal * 0.94) + (white * 0.06)
                    val envelope = sin(PI * (i.toDouble() / totalSamples))
                    val sample = lastVal * envelope * 2.5
                    buffer[i] = (sample * 26000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }

            SoundPreset.RETRO_COIN -> {
                val f1 = 987.77 // B5
                val f2 = 1318.51 // E6
                val split = totalSamples / 2
                for (i in 0 until totalSamples) {
                    val freq = if (i < split) f1 else f2
                    val t = i.toDouble() / SAMPLE_RATE
                    // Square wave
                    val sine = sin(2.0 * PI * freq * t)
                    val square = if (sine >= 0) 0.6 else -0.6
                    val decay = 1.0 - (i.toDouble() / totalSamples)
                    buffer[i] = (square * decay * 25000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }

            SoundPreset.BASS_DROP -> {
                for (i in 0 until totalSamples) {
                    val progress = i.toDouble() / totalSamples
                    val freq = 160.0 * exp(-progress * 2.8) + 38.0
                    val t = i.toDouble() / SAMPLE_RATE
                    val envelope = 1.0 - (progress * 0.8)
                    val sample = sin(2.0 * PI * freq * t) * envelope
                    buffer[i] = (sample * 32000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }

            SoundPreset.COSMIC_PAD -> {
                val baseFreq = 220.0 // A3
                for (i in 0 until totalSamples) {
                    val t = i.toDouble() / SAMPLE_RATE
                    val lfo = 1.0 + 0.015 * sin(2.0 * PI * 1.5 * t)
                    val s1 = sin(2.0 * PI * (baseFreq * lfo) * t)
                    val s2 = sin(2.0 * PI * (baseFreq * 1.503) * t)
                    val envelope = sin(PI * (i.toDouble() / totalSamples))
                    val sample = (0.5 * s1 + 0.5 * s2) * envelope
                    buffer[i] = (sample * 29000).toInt().coerceIn(-32768, 32767).toShort()
                }
            }
        }
        return buffer
    }

    private suspend fun playSamples(samples: ShortArray, volume: Float, onComplete: () -> Unit) = withContext(Dispatchers.IO) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, samples.size * 2)

        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()

        track.setVolume(volume.coerceIn(0.0f, 1.0f))
        currentTrack = track
        isPlaying = true

        track.play()
        track.write(samples, 0, samples.size)

        // Wait until playback completes or stopped
        val durationMs = (samples.size.toDouble() / SAMPLE_RATE * 1000).toLong()
        kotlinx.coroutines.delay(durationMs + 100)
        isPlaying = false
        withContext(Dispatchers.Main) { onComplete() }
    }
}
