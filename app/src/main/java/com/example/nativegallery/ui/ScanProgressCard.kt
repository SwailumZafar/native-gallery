package com.example.nativegallery.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Stationary tabular glyphs never overlap or fade through each other during a fast count. */
@Composable
internal fun LiveScanCounter(count: Int, total: Int, baseline: Int = 0) {
    val target = count.coerceAtLeast(0).toFloat()
    var displayed by remember(total, baseline) {
        mutableFloatStateOf(baseline.coerceIn(0, count.coerceAtLeast(0)).toFloat())
    }

    LaunchedEffect(target, total, baseline) {
        if (target < displayed || target - displayed > 250f) {
            displayed = target
            return@LaunchedEffect
        }
        while (displayed < target) {
            withFrameNanos {
                val remaining = target - displayed
                if (remaining <= 0.05f) {
                    displayed = target
                } else {
                    val step = maxOf(0.45f, remaining * 0.028f)
                    displayed = (displayed + step).coerceAtMost(target)
                }
            }
        }
    }
    val displayedInt = displayed.toInt()
    Row(Modifier.testTag("live-scan-counter").semantics { stateDescription = "$displayedInt/$total" },
        horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        val numberStyle = MaterialTheme.typography.labelLarge.copy(fontFamily = FontFamily.Monospace, fontWeight = FontWeight.SemiBold)
        val measurer = rememberTextMeasurer()
        val density = LocalDensity.current
        val cellSize = remember(measurer, numberStyle) {
            val sizes = (0..9).map { measurer.measure(it.toString(), numberStyle).size }
            androidx.compose.ui.unit.IntSize(sizes.maxOf { it.width } + 2, sizes.maxOf { it.height })
        }
        val digits = displayedInt.toString().padStart(total.coerceAtLeast(displayedInt).toString().length, ' ')
        Row(Modifier.semantics(mergeDescendants = true) { contentDescription = displayedInt.toString() }) {
            digits.forEach { digit ->
                Box(Modifier.size(with(density) { cellSize.width.toDp() }, with(density) { cellSize.height.toDp() }), contentAlignment = Alignment.Center) {
                    Text(if (digit == ' ') "" else digit.toString(), Modifier.clearAndSetSemantics {},
                        style = numberStyle, color = MaterialTheme.colorScheme.primary, maxLines = 1, softWrap = false)
                }
            }
        }
        Text("/ $total", style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
internal fun ScanProgressCard(title: String, active: Boolean, count: Int, total: Int, modifier: Modifier = Modifier,
    baseline: Int = 0, actionLabel: String? = null, onAction: (() -> Unit)? = null) {
    val target = if (total > 0) count.toFloat().div(total).coerceIn(0f, 1f) else 0f
    val progress = animateFloatAsState(target, tween(180, easing = LinearOutSlowInEasing), label = "scan-track")
    val colors = MaterialTheme.colorScheme
    Surface(modifier.testTag("scan-progress-card"), shape = RoundedCornerShape(18.dp), color = colors.surfaceContainerLow,
        border = BorderStroke(1.dp, colors.outlineVariant.copy(alpha = 0.4f))) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                LiveScanCounter(count, total, baseline)
                if (actionLabel != null && onAction != null) TextButton(onAction, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp),
                    modifier = Modifier.heightIn(min = 36.dp)) { Text(actionLabel) }
            }
            Canvas(Modifier.fillMaxWidth().height(7.dp).semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                contentDescription = if (active) "Scan progress" else "Scan complete"
            }) {
                val radius = size.height / 2f
                drawRoundRect(colors.primary.copy(alpha = 0.09f), cornerRadius = CornerRadius(radius))
                val width = size.width * progress.value
                if (width > 0f) {
                    drawRoundRect(Brush.horizontalGradient(listOf(colors.primary, colors.tertiary)), size = Size(width.coerceAtLeast(size.height), size.height), cornerRadius = CornerRadius(radius))
                    if (active && width >= size.height) drawCircle(colors.onPrimary.copy(alpha = 0.9f), radius = 1.3.dp.toPx(), center = Offset((width - radius).coerceAtLeast(radius), radius))
                }
            }
        }
    }
}
