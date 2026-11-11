package com.example.nativegallery.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.nativegallery.ui.components.DisplayedBitmapCache
import com.example.nativegallery.ui.components.GalleryImage
import com.example.nativegallery.ui.components.ImageLoadQuality
import com.example.nativegallery.ui.components.mediaThumbnailCacheRevision
import kotlinx.coroutines.delay

@Composable
internal fun MomentStory(card: MomentCard, onDismiss: () -> Unit) {
    val frames = remember(card.id, card.highlights, card.media) {
        val photos = (card.highlights.ifEmpty { card.media }).filterNot { it.isVideo }
        val chosen = photos.ifEmpty { card.media.take(12) }
        val cover = card.media.firstOrNull { it.id == card.cover.id && !it.isVideo }
        (listOfNotNull(cover) + chosen).distinctBy { it.id }.take(12)
    }
    if (frames.isEmpty()) { LaunchedEffect(Unit) { onDismiss() }; return }
    var index by rememberSaveable(card.id) { mutableIntStateOf(0) }
    index = index.coerceIn(frames.indices)
    var paused by rememberSaveable(card.id) { mutableStateOf(false) }
    var held by remember { mutableStateOf(false) }
    var audioMood by rememberSaveable(card.id) { mutableStateOf(StoryAudioMood.CalmPiano) }
    var audioMenuOpen by remember { mutableStateOf(false) }
    val elapsed = remember(card.id, index) { mutableFloatStateOf(0f) }
    val lifecycle by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsStateWithLifecycle()
    val isMusicPlaying = !paused && !held && audioMood != StoryAudioMood.Silent && lifecycle.isAtLeast(Lifecycle.State.RESUMED)
    rememberStoryAudioPlayer(mood = audioMood, isPlaying = isMusicPlaying)

    val frame = frames[index]
    var loaded by remember(frame.id) { mutableStateOf(frame.contentUri == null) }
    LaunchedEffect(frame.id) {
        repeat(80) { if (DisplayedBitmapCache.get(frame.contentUri) != null) { loaded = true; return@LaunchedEffect }; delay(50) }
        loaded = true // Unreadable items must not trap the story's navigation.
    }
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(card.id, index, paused, held, loaded, lifecycle) {
        if (paused || held || !loaded || !lifecycle.isAtLeast(Lifecycle.State.RESUMED)) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (elapsed.floatValue < 1f) {
            val now = withFrameNanos { it }
            elapsed.floatValue = (elapsed.floatValue + (now - last).coerceAtMost(80_000_000L) / 5_000_000_000f).coerceAtMost(1f)
            last = now
        }
        if (index < frames.lastIndex) index++ else dismiss()
    }
    fun next() { if (index < frames.lastIndex) index++ else onDismiss() }
    fun previous() { if (index > 0) index-- else elapsed.floatValue = 0f }
    UsefulFeatureDialog(onDismiss) {
        Box(Modifier.fillMaxSize().background(Color.Black).testTag("moment-story")) {
            AnimatedContent(frame, Modifier.fillMaxSize().clipToBounds(), contentKey = { it.id }, label = "story-photo",
                transitionSpec = { fadeIn(tween(240)) togetherWith fadeOut(tween(180)) }) { photo ->
                GalleryImage(photo.imageRes, photo.contentUri, "Moment story photo", Modifier.fillMaxSize(), cornerRadius = 0.dp,
                    contentScale = ContentScale.Fit, thumbnailSize = 1600, loadQuality = ImageLoadQuality.HighQuality,
                    backgroundColor = Color.Black, fallbackImageUri = photo.previewUri, isVideo = photo.isVideo,
                    cacheRevision = mediaThumbnailCacheRevision(photo))
            }
            Box(Modifier.fillMaxSize().pointerInput(index) {
                detectTapGestures(onPress = { held = true; try { tryAwaitRelease() } finally { held = false } },
                    onTap = { if (it.x < size.width * 0.33f) previous() else next() })
            })
            Box(Modifier.fillMaxWidth().height(210.dp).align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.85f), Color.Transparent))))
            Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
                Canvas(Modifier.fillMaxWidth().height(3.dp)) {
                    val gap = 4.dp.toPx()
                    val width = ((size.width - gap * (frames.size - 1)) / frames.size).coerceAtLeast(1f)
                    repeat(frames.size) { position ->
                        val left = position * (width + gap)
                        drawRoundRect(Color.White.copy(alpha = 0.25f), Offset(left, 0f), Size(width, size.height), CornerRadius(size.height))
                        val amount = if (position < index) 1f else if (position == index) elapsed.floatValue else 0f
                        if (amount > 0f) drawRoundRect(Color.White, Offset(left, 0f), Size(width * amount, size.height), CornerRadius(size.height))
                    }
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(card.name, color = Color.White, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                        Text("${index + 1} of ${frames.size} · Hold to pause", color = Color.White.copy(alpha = 0.75f), style = MaterialTheme.typography.bodySmall)
                    }
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.16f),
                            modifier = Modifier.clickable { audioMenuOpen = true }
                        ) {
                            Row(
                                Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (audioMood == StoryAudioMood.Silent) {
                                    Icon(Icons.Filled.VolumeOff, "Sound muted", tint = Color.White, modifier = Modifier.size(16.dp))
                                } else {
                                    StoryMusicEqualizerIcon(isPlaying = isMusicPlaying, color = Color.White)
                                }
                                Text(audioMood.label, color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                        DropdownMenu(expanded = audioMenuOpen, onDismissRequest = { audioMenuOpen = false }) {
                            for (mood in StoryAudioMood.entries) {
                                DropdownMenuItem(
                                    text = { Text(mood.label) },
                                    leadingIcon = {
                                        Icon(
                                            if (mood == StoryAudioMood.Silent) Icons.Filled.VolumeOff else Icons.Filled.MusicNote,
                                            contentDescription = null
                                        )
                                    },
                                    trailingIcon = {
                                        if (mood == audioMood) Icon(Icons.Filled.Check, null)
                                    },
                                    onClick = {
                                        audioMood = mood
                                        audioMenuOpen = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton({ paused = !paused }) { Icon(if (paused) Icons.Filled.PlayArrow else Icons.Filled.Pause,
                        if (paused) "Resume story" else "Pause story", tint = Color.White) }
                    IconButton(onDismiss) { Icon(Icons.Filled.Close, "Close story", tint = Color.White) }
                }
            }
            Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.9f))))
                .navigationBarsPadding().padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(::previous, enabled = index > 0) { Icon(Icons.Filled.ChevronLeft, "Previous story photo", tint = Color.White) }
                    Button(onDismiss, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("View all photos") }
                    IconButton(::next) { Icon(Icons.Filled.ChevronRight, "Next story photo", tint = Color.White) }
                }
            }
        }
    }
}
