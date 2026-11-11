package com.example.nativegallery.ui

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.sin

enum class StoryAudioMood(val label: String, val baseFreq: Float, val chordIntervals: FloatArray) {
    CalmPiano("Calm Piano", 261.63f, floatArrayOf(1.0f, 1.25f, 1.5f, 1.875f)), // C major 7th / pentatonic
    GoldenSunset("Golden Sunset", 220.0f, floatArrayOf(1.0f, 1.189f, 1.498f, 1.782f)), // A minor 7th
    LoFiChill("Lo-Fi Chill", 196.0f, floatArrayOf(1.0f, 1.26f, 1.498f, 1.682f)), // G sus / dream
    Silent("Muted", 0f, floatArrayOf())
}

/**
 * Procedural offline ambient music generator for Stories using AudioTrack.
 * Generates soft, warm, polyphonic ambient chords without any network or asset files.
 */
@Composable
fun rememberStoryAudioPlayer(mood: StoryAudioMood, isPlaying: Boolean): State<StoryAudioMood> {
    val currentMood = rememberUpdatedState(mood)
    val playingState = rememberUpdatedState(isPlaying)

    LaunchedEffect(mood, isPlaying) {
        if (mood == StoryAudioMood.Silent || !isPlaying) return@LaunchedEffect
        withContext(Dispatchers.IO) {
            val sampleRate = 22050
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            ).coerceAtLeast(4096)

            val track = runCatching {
                AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(sampleRate)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(bufferSize)
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .build()
            }.getOrNull() ?: return@withContext

            try {
                track.play()
                val chunk = ShortArray(1024)
                var phase = 0.0
                var chordTime = 0.0
                var chordIndex = 0

                // Progression steps: Root, 4th, 5th, 6th
                val chordSteps = floatArrayOf(1.0f, 1.334f, 1.5f, 1.122f)

                while (isActive && playingState.value && currentMood.value != StoryAudioMood.Silent) {
                    val root = currentMood.value.baseFreq * chordSteps[chordIndex % chordSteps.size]
                    val intervals = currentMood.value.chordIntervals

                    for (i in chunk.indices) {
                        val t = chordTime / sampleRate.toDouble()
                        // Soft envelope: attack and decay per chord note
                        val cycleProgress = (t % 3.5) / 3.5
                        val envelope = if (cycleProgress < 0.2) cycleProgress / 0.2
                        else (1.0 - cycleProgress).coerceAtLeast(0.15)

                        var sample = 0.0
                        for ((idx, interval) in intervals.withIndex()) {
                            val freq = root * interval
                            val detune = 1.0 + (idx * 0.003) // Subtle chorus detuning
                            sample += sin(2.0 * PI * freq * detune * t) * (0.28 / (idx + 1))
                            // Gentle 2nd harmonic
                            sample += sin(4.0 * PI * freq * t) * 0.08
                        }
                        val value = (sample * envelope * 12000.0).toInt().coerceIn(-32767, 32767)
                        chunk[i] = value.toShort()
                        chordTime += 1.0
                    }

                    if (chordTime >= sampleRate * 3.5) {
                        chordTime = 0.0
                        chordIndex++
                    }

                    track.write(chunk, 0, chunk.size)
                }
            } finally {
                runCatching {
                    track.stop()
                    track.release()
                }
            }
        }
    }

    return currentMood
}

@Composable
fun StoryMusicEqualizerIcon(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Color.White,
    barWidth: Dp = 3.dp,
    height: Dp = 16.dp
) {
    val infiniteTransition = rememberInfiniteTransition(label = "music-eq")
    val bar1 by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(420, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq1"
    )
    val bar2 by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(340, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq2"
    )
    val bar3 by infiniteTransition.animateFloat(
        initialValue = 0.40f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(480, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eq3"
    )

    Canvas(modifier = modifier.size(width = 18.dp, height = height)) {
        val gap = 3.dp.toPx()
        val w = barWidth.toPx()
        val h = size.height

        val b1 = if (isPlaying) bar1 else 0.3f
        val b2 = if (isPlaying) bar2 else 0.45f
        val b3 = if (isPlaying) bar3 else 0.25f

        // Bar 1
        val h1 = h * b1
        drawRoundRect(color, Offset(0f, h - h1), Size(w, h1), CornerRadius(w / 2))
        // Bar 2
        val h2 = h * b2
        drawRoundRect(color, Offset(w + gap, h - h2), Size(w, h2), CornerRadius(w / 2))
        // Bar 3
        val h3 = h * b3
        drawRoundRect(color, Offset((w + gap) * 2, h - h3), Size(w, h3), CornerRadius(w / 2))
    }
}
