package com.kaamio.nepal.ui.theme

import android.app.Activity
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// Apple-inspired Spacing Grid (8pt)
object KaamioSpacing {
    val micro = 8.dp
    val internal = 12.dp
    val cardPadding = 16.dp
    val section = 24.dp
    val pageHorizontal = 32.dp
    val pageVertical = 32.dp
    
    val buttonHeight = 56.dp
    val chipHeight = 44.dp
    val iconContainer = 48.dp

    // Compatibility aliases
    val xxs = 4.dp; val xs = 8.dp; val sm = 12.dp; val md = 16.dp
    val lg = 20.dp; val xl = 24.dp; val xxl = 32.dp
}

val KaamioShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val LocalKaamioTheme = staticCompositionLocalOf { KaamioTheme.Dark }

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val kaamioTheme = if (darkTheme) KaamioTheme.Dark else KaamioTheme.Light
    val colorScheme = if (darkTheme) {
        darkColorScheme(
            primary = kaamioTheme.textPrimary,
            onPrimary = kaamioTheme.background,
            secondary = kaamioTheme.surface,
            onSecondary = kaamioTheme.textPrimary,
            tertiary = kaamioTheme.textTertiary,
            background = kaamioTheme.background,
            surface = kaamioTheme.surface,
            onSurface = kaamioTheme.textPrimary,
            error = kaamioTheme.error,
            onError = Color.White,
        )
    } else {
        lightColorScheme(
            primary = kaamioTheme.textPrimary,
            onPrimary = kaamioTheme.background,
            secondary = kaamioTheme.surface,
            onSecondary = kaamioTheme.textPrimary,
            tertiary = kaamioTheme.textTertiary,
            background = kaamioTheme.background,
            surface = kaamioTheme.surface,
            onSurface = kaamioTheme.textPrimary,
            error = kaamioTheme.error,
            onError = Color.White,
        )
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT
            window.navigationBarColor = android.graphics.Color.TRANSPARENT
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalKaamioTheme provides kaamioTheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = KaamioShapes,
            content = content
        )
    }
}

fun Modifier.premiumPress() = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "press_scale"
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
}

fun Modifier.bounceClick(onClick: () -> Unit) = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val animScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "press_scale"
    )
    this.graphicsLayer { scaleX = animScale; scaleY = animScale }
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

// Scale-only press animation for components that already own a click handler
// (e.g. Surface(onClick)). Never adds its own clickable, so the callback is
// never invoked twice. Uses the mandated spring physics at 0.96f.
fun Modifier.pressScale(interactionSource: MutableInteractionSource, pressedScale: Float = 0.96f) = composed {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "press_scale"
    )
    this.graphicsLayer { scaleX = scale; scaleY = scale }
}

fun Modifier.shimmer(shape: Shape = RoundedCornerShape(16.dp)) = composed {
    val theme = LocalKaamioTheme.current
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = -300f, targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Restart
        ), label = "shimmer_translate"
    )
    val base = if (theme.isDark) Color(0xFF111111) else Color(0xFFE8E8EE)
    val highlight = if (theme.isDark) Color.White.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.7f)
    val brush = remember(translateAnim) {
        Brush.linearGradient(
            colors = listOf(Color.White.copy(alpha = 0f), highlight, Color.White.copy(alpha = 0f)),
            start = Offset.Zero, end = Offset(translateAnim, translateAnim)
        )
    }
    this.background(base, shape).drawWithContent { drawContent(); drawRect(brush = brush) }
}

fun Modifier.premiumShadow(elevation: Dp = 4.dp, shape: Shape = RoundedCornerShape(24.dp)) = composed {
    this.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = Color.Black.copy(alpha = 0.5f),
        spotColor = Color.Black.copy(alpha = 0.3f)
    )
}
