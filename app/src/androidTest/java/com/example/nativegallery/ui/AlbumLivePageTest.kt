package com.example.nativegallery.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlbumLivePageTest {
    @get:Rule val rule = createComposeRule()

    @Test fun livePageSurvivesOpeningRestAndClosingWithoutChangingItsLayout() {
        val moving = mutableStateOf(true)
        val expansion = mutableStateOf(0f)
        val tokens = mutableListOf<Any>()
        val sizes = mutableListOf<IntSize>()
        var mounts = 0
        var disposals = 0
        rule.setContent {
            MaterialTheme {
                val density = LocalDensity.current
                val width = with(density) { 120.dp.toPx() }
                val height = with(density) { 240.dp.toPx() }
                Box(Modifier.size(120.dp, 240.dp)) {
                    AlbumPageSurface(width, height, Rect(20f, 90f, 80f, 150f), 18f,
                        moving.value, { expansion.value }) {
                        val token = remember { Any() }
                        DisposableEffect(Unit) { mounts++; tokens += token; onDispose { disposals++ } }
                        Box(Modifier.fillMaxSize().onSizeChanged { sizes += it }.background(Color.Red))
                    }
                }
            }
        }
        for ((active, p) in listOf(true to .3f, true to .8f, false to 1f,
            true to 1f, true to .6f, true to .1f, true to 0f)) {
            rule.runOnIdle { moving.value = active; expansion.value = p }
            rule.waitForIdle()
        }
        rule.runOnIdle {
            assertEquals(1, mounts)
            assertEquals(0, disposals)
            assertEquals(1, tokens.size)
            assertEquals(1, sizes.distinct().size)
            assertTrue(sizes.first().width > 0)
        }
    }
}
