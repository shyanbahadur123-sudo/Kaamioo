package com.kaamio.nepal.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

object KaamioColors {
    // Core Palette (Apple Premium Style)
    val PureBlack = Color(0xFF000000)
    val SoftBlack = Color(0xFF0A0A0A)
    val ElevatedSurface = Color(0xFF111111)
    val CardSurface = Color(0xFF151515)
    
    val White = Color(0xFFFFFFFF)
    val GrayLight = Color(0xFFC7C7C7)
    val GrayMedium = Color(0xFF8A8A8A)
    val GrayDark = Color(0xFF5A5A5A)
    val Divider = Color(0xFF1F1F1F)
    val Border = Color(0xFF2A2A2A)

    // Accents from Ref Image
    val FindJobs = Color(0xFF33A1FF)
    val HireWorkers = Color(0xFFAF52DE)
    val LearnSkills = Color(0xFF30D158)
    val TeachSkills = Color(0xFFFF3B30)
    val FreelanceGigs = Color(0xFFFFD60A)
    val TrustLedger = Color(0xFF8E8E93)
    
    // Semantic Aliases
    val Background = PureBlack
    val Surface = SoftBlack
    val Card = CardSurface
    val Success = LearnSkills
    val Error = Color(0xFFFF453A)
    val TextPrimary = White
    val TextSecondary = GrayLight
    val TextTertiary = GrayMedium
}

// Global Aliases for Compatibility (These should ideally be avoided in favor of theme access)
val KaamioPrimary = KaamioColors.FindJobs
val KaamioSuccess = KaamioColors.Success
val KaamioError = KaamioColors.Error
val KaamioWhite = Color.White

@Immutable
data class KaamioTheme(
    val background: Color,
    val surface: Color,
    val card: Color,
    val border: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val onPrimary: Color,
    val accent: Color,
    val onAccent: Color,
    val success: Color,
    val error: Color,
    val warn: Color,
    val isDark: Boolean
) {
    companion object {
        val Dark = KaamioTheme(
            background = KaamioColors.PureBlack,
            surface = KaamioColors.SoftBlack,
            card = KaamioColors.CardSurface,
            border = KaamioColors.Border,
            divider = KaamioColors.Divider,
            textPrimary = Color.White,
            textSecondary = KaamioColors.GrayLight,
            textTertiary = KaamioColors.GrayMedium,
            onPrimary = Color.Black,
            accent = KaamioColors.FindJobs,
            onAccent = Color.Black,
            success = KaamioColors.LearnSkills,
            error = KaamioColors.Error,
            warn = KaamioColors.FreelanceGigs,
            isDark = true
        )

        val Light = KaamioTheme(
            background = Color(0xFFF6F6F9),
            surface = Color(0xFFFFFFFF),
            card = Color(0xFFF2F2F7),
            border = Color(0xFFE4E4EC),
            divider = Color(0xFFECECF2),
            textPrimary = Color(0xFF0A0A0F),
            textSecondary = Color(0xFF5D5D66),
            textTertiary = Color(0xFF9A9AA4),
            onPrimary = Color.White,
            accent = Color(0xFF0B7BD4),
            onAccent = Color.White,
            success = Color(0xFF1B8A3C),
            error = Color(0xFFD9281D),
            warn = Color(0xFFB28B00),
            isDark = false
        )
    }
}

@Composable
fun currentKaamioTheme(): KaamioTheme = LocalKaamioTheme.current
