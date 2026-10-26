package com.nuvio.app

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Desktop-only Live TV content supplied by the macOS app entry point. */
internal class DesktopLiveTvNavigation(
    val isOpen: Boolean,
    val open: () -> Unit,
    val close: () -> Unit,
    val content: @Composable () -> Unit,
)

internal val LocalDesktopLiveTvNavigation = compositionLocalOf<DesktopLiveTvNavigation?> { null }
