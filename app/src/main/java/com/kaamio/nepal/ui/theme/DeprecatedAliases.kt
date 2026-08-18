package com.kaamio.nepal.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Theme-dependent backward-compatible aliases
// These exist so existing screens compile without changes.
// New code should use LocalKaamioTheme.current directly.

val KaamioBackground: Color
    @Composable get() = LocalKaamioTheme.current.background

val KaamioSurface: Color
    @Composable get() = LocalKaamioTheme.current.surface

val KaamioCard: Color
    @Composable get() = LocalKaamioTheme.current.card

val KaamioTextPrimary: Color
    @Composable get() = LocalKaamioTheme.current.textPrimary

val KaamioTextSecondary: Color
    @Composable get() = LocalKaamioTheme.current.textSecondary

val KaamioTextTertiary: Color
    @Composable get() = LocalKaamioTheme.current.textTertiary

val KaamioBorder: Color
    @Composable get() = LocalKaamioTheme.current.border

val KaamioDivider: Color
    @Composable get() = LocalKaamioTheme.current.divider

val KaamioOnPrimary: Color
    @Composable get() = LocalKaamioTheme.current.onPrimary
