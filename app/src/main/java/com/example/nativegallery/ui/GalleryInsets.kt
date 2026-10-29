package com.example.nativegallery.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun galleryHeaderTop(): Dp = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp
