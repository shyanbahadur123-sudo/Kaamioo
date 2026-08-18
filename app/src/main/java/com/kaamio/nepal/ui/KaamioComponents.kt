@file:OptIn(ExperimentalMaterial3Api::class)

package com.kaamio.nepal.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaamio.nepal.R
import com.kaamio.nepal.ui.theme.*

@Composable
fun KaamioButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = LocalKaamioTheme.current.textPrimary,
    contentColor: Color = LocalKaamioTheme.current.background,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    height: Dp = 64.dp,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "scale"
    )

    Button(
        onClick = onClick,
        modifier = modifier
            .height(height)
            .graphicsLayer { scaleX = scale; scaleY = scale },
        enabled = enabled && !isLoading,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor, 
            contentColor = contentColor,
            disabledContainerColor = containerColor.copy(alpha = 0.5f),
            disabledContentColor = contentColor.copy(alpha = 0.5f)
        ),
        interactionSource = interactionSource,
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 6.dp,
            pressedElevation = 0.dp,
            disabledElevation = 0.dp
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = contentColor, strokeWidth = 2.5.dp)
        } else {
            Text(
                text = text.uppercase(), 
                style = Typography.labelLarge, 
                fontWeight = FontWeight.Black, 
                letterSpacing = 1.5.sp
            )
        }
    }
}

@Composable
fun KaamioSecondaryButton(text: String, icon: ImageVector? = null, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val theme = LocalKaamioTheme.current
    OutlinedButton(
        onClick = onClick, enabled = enabled, modifier = modifier.height(56.dp),
        shape = RoundedCornerShape(24.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = theme.textPrimary),
        border = BorderStroke(1.dp, theme.border)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) { Icon(icon, null, modifier = Modifier.size(20.dp)); Spacer(modifier = Modifier.width(8.dp)) }
            Text(text.uppercase(), style = Typography.labelLarge, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
        }
    }
}

@Composable
fun KaamioCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    backgroundColor: Color = LocalKaamioTheme.current.card,
    padding: Dp = 20.dp,
    elevation: Dp = 0.dp,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val theme = LocalKaamioTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "card_scale"
    )

    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(
                elevation = if (isPressed && onClick != null) 2.dp else elevation,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.2f),
                spotColor = Color.Black.copy(alpha = 0.1f)
            )
            .clip(shape)
            .background(backgroundColor)
            .border(1.dp, theme.border.copy(alpha = if(theme.isDark) 0.3f else 0.1f), shape)
            .then(if (onClick != null) Modifier.clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ) else Modifier)
            .padding(padding),
        content = content
    )
}

@Composable
fun PremiumSectionHeader(title: String, actionLabel: String? = null, onAction: (() -> Unit)? = null, horizontalPadding: Dp = 32.dp) {
    val theme = LocalKaamioTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = horizontalPadding),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        Text(title.uppercase(), style = Typography.labelLarge, color = theme.textSecondary, letterSpacing = 2.sp)
        if (actionLabel != null && onAction != null) {
            Text(actionLabel, style = Typography.labelLarge, color = theme.textPrimary, 
                modifier = Modifier.clickable { onAction() }.padding(bottom = 2.dp), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun KaamioRoleCard(title: String, subtitle: String, icon: ImageVector, isSelected: Boolean, onClick: () -> Unit) {
    val theme = LocalKaamioTheme.current
    KaamioCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        backgroundColor = if (isSelected) theme.surface else theme.card,
        elevation = if (isSelected) 6.dp else 0.dp
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)).background(if (isSelected) theme.accent else theme.surface), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (isSelected) theme.onAccent else theme.textPrimary, modifier = Modifier.size(24.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = Typography.titleMedium, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                if (subtitle.isNotEmpty()) Text(subtitle, style = Typography.bodySmall, color = theme.textSecondary)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, null, tint = theme.accent)
            }
        }
    }
}

@Composable
fun KaamioTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    label: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    isError: Boolean = false,
    errorMessage: String? = null,
    enabled: Boolean = true,
    isPassword: Boolean = false
) {
    val theme = LocalKaamioTheme.current
    var passwordVisible by remember { mutableStateOf(false) }
    val effectiveTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else visualTransformation
    
    Column(modifier = modifier) {
        if (label != null) {
            Text(text = label.uppercase(), style = Typography.labelSmall, color = theme.textSecondary, 
                modifier = Modifier.padding(start = 12.dp, bottom = 8.dp), letterSpacing = 1.sp)
        }
        OutlinedTextField(
            value = value, onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp),
            enabled = enabled,
            placeholder = { Text(text = placeholder, style = Typography.bodyLarge, color = theme.textTertiary) },
            leadingIcon = leadingIcon?.let { { Icon(it, null, tint = theme.textSecondary, modifier = Modifier.size(22.dp)) } },
            trailingIcon = if (isPassword) { { IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(if (passwordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, null, tint = theme.textSecondary) } } } else trailingIcon,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = theme.textPrimary, unfocusedTextColor = theme.textPrimary,
                focusedContainerColor = theme.surface, unfocusedContainerColor = theme.surface,
                focusedBorderColor = theme.textPrimary.copy(alpha = 0.5f), unfocusedBorderColor = theme.border,
                cursorColor = theme.textPrimary
            ),
            isError = isError, keyboardOptions = keyboardOptions,
            visualTransformation = effectiveTransformation, singleLine = true, textStyle = Typography.bodyLarge
        )
        if (isError && !errorMessage.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = errorMessage,
                style = Typography.bodySmall,
                color = LocalKaamioTheme.current.error,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

@Composable
fun KaamioBottomNav(currentRoute: String, gateway: String = "work", onNavigate: (Screen) -> Unit) {
    val theme = LocalKaamioTheme.current
    
    val navItems = remember(gateway) {
        if (gateway == "education") {
            listOf(
                BottomNavItem(Screen.Home, R.string.nav_home, Icons.Outlined.Home, Icons.Outlined.Home),
                BottomNavItem(Screen.Learn, R.string.nav_learn, Icons.Outlined.School, Icons.Outlined.School),
                BottomNavItem(Screen.TeacherDashboard, R.string.educator_hub_title, Icons.Outlined.CoPresent, Icons.Outlined.CoPresent),
                BottomNavItem(Screen.Profile, R.string.nav_profile, Icons.Outlined.Person, Icons.Outlined.Person)
            )
        } else {
            listOf(
                BottomNavItem(Screen.Home, R.string.nav_home, Icons.Outlined.Home, Icons.Outlined.Home),
                BottomNavItem(Screen.Market, R.string.nav_market, Icons.Outlined.BusinessCenter, Icons.Outlined.BusinessCenter),
                BottomNavItem(Screen.Chat, R.string.nav_messages, Icons.Outlined.ChatBubbleOutline, Icons.Outlined.ChatBubbleOutline),
                BottomNavItem(Screen.MyActivities, R.string.my_work_title, Icons.Outlined.Assignment, Icons.Outlined.Assignment),
                BottomNavItem(Screen.Profile, R.string.nav_profile, Icons.Outlined.Person, Icons.Outlined.Person)
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .height(84.dp)
            .shadow(16.dp, RoundedCornerShape(42.dp), ambientColor = Color.Black, spotColor = theme.accent.copy(alpha = 0.2f))
            .clip(RoundedCornerShape(42.dp))
            .background(theme.surface.copy(alpha = 0.95f))
            .border(1.dp, theme.textPrimary.copy(alpha = 0.08f), RoundedCornerShape(42.dp)),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEach { item ->
                val selected = currentRoute == item.screen.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigate(item.screen) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .graphicsLayer { scaleX = if(selected) 1.05f else 1f; scaleY = if(selected) 1.05f else 1f }
                            .clip(CircleShape)
                            .background(if (selected) theme.accent else Color.Transparent)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
                            contentDescription = stringResource(item.labelRes),
                            tint = if (selected) theme.onAccent else theme.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    if (selected) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(item.labelRes),
                            style = Typography.labelSmall,
                            color = theme.accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

private data class BottomNavItem(val screen: Screen, val labelRes: Int, val selectedIcon: ImageVector, val unselectedIcon: ImageVector)

@Composable
fun KaamioBackButton(onClick: () -> Unit) {
    val theme = LocalKaamioTheme.current
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp).clip(CircleShape).background(theme.card)) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back), tint = theme.textPrimary, modifier = Modifier.size(22.dp))
    }
}

@Composable
fun KaamioOrDivider(text: String, modifier: Modifier = Modifier) {
    val theme = LocalKaamioTheme.current
    Row(modifier = modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        HorizontalDivider(modifier = Modifier.weight(1f), color = theme.divider)
        Text(text, color = theme.textSecondary, style = Typography.labelSmall, modifier = Modifier.padding(horizontal = 16.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = theme.divider)
    }
}

@Composable
fun KaamioErrorBanner(error: String, modifier: Modifier = Modifier) {
    if (error.isBlank()) return
    val theme = LocalKaamioTheme.current
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.error.copy(alpha = 0.12f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Error, null, tint = theme.error)
        Spacer(modifier = Modifier.width(12.dp))
        Text(error, color = theme.error, style = Typography.bodyMedium)
    }
}

@Composable
fun KaamioSuccessBanner(message: String, modifier: Modifier = Modifier) {
    val theme = LocalKaamioTheme.current
    if (message.isBlank()) return
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(theme.success.copy(alpha = 0.12f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.CheckCircle, null, tint = theme.success)
        Spacer(modifier = Modifier.width(12.dp))
        Text(message, color = theme.success, style = Typography.bodyMedium)
    }
}

@Composable
fun ConnectivityBanner() {
    Box(modifier = Modifier.fillMaxWidth().background(KaamioColors.Error).padding(horizontal = 32.dp, vertical = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.WifiOff, null, tint = Color.White, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(stringResource(R.string.banner_no_connection), color = Color.White, style = Typography.labelSmall)
        }
    }
}

@Composable
fun KaamioAlertDialog(
    title: String, text: String, confirmText: String, dismissText: String, onConfirm: () -> Unit, onDismiss: () -> Unit, isLoading: Boolean = false
) {
    val theme = LocalKaamioTheme.current
    AlertDialog(
        onDismissRequest = { if (!isLoading) onDismiss() },
        title = { Text(title, style = Typography.headlineSmall, color = theme.textPrimary, fontWeight = FontWeight.Bold) },
        text = { Text(text, style = Typography.bodyLarge, color = theme.textSecondary) },
        containerColor = theme.card,
        shape = RoundedCornerShape(28.dp),
        confirmButton = {
            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = theme.textPrimary)
            else TextButton(onClick = onConfirm) { Text(confirmText, color = theme.textPrimary, fontWeight = FontWeight.Bold) }
        },
        dismissButton = {
            if (dismissText.isNotBlank()) TextButton(onClick = onDismiss) { Text(dismissText, color = theme.textSecondary) }
        }
    )
}

// ---------------------------------------------------------------------------
// Premium shared primitives
// ---------------------------------------------------------------------------

@Composable
fun KaamioScreenHeader(title: String, onBack: (() -> Unit)? = null, trailing: @Composable (() -> Unit)? = null) {
    val theme = LocalKaamioTheme.current
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 32.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            KaamioBackButton(onClick = onBack)
            Spacer(modifier = Modifier.width(20.dp))
        }
        Text(title, style = Typography.displaySmall, fontWeight = FontWeight.Bold, color = theme.textPrimary, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun KaamioEmptyState(
    icon: ImageVector = Icons.Outlined.Inbox,
    title: String,
    subtitle: String? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val theme = LocalKaamioTheme.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(28.dp)).background(theme.surface).border(1.dp, theme.border, RoundedCornerShape(28.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = theme.textTertiary, modifier = Modifier.size(40.dp))
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(title, style = Typography.titleLarge, color = theme.textPrimary, textAlign = TextAlign.Center)
        if (!subtitle.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(subtitle, style = Typography.bodyMedium, color = theme.textSecondary, textAlign = TextAlign.Center)
        }
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(24.dp))
            KaamioButton(actionLabel, onAction, height = 48.dp, shape = RoundedCornerShape(20.dp))
        }
    }
}

@Composable
fun ShimmerBox(modifier: Modifier = Modifier, shape: RoundedCornerShape = RoundedCornerShape(20.dp)) {
    Box(modifier = modifier.shimmer(shape))
}

@Composable
fun KaamioIconContainer(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    containerColor: Color = LocalKaamioTheme.current.surface,
    tint: Color = LocalKaamioTheme.current.textPrimary,
    cornerRadius: Dp = 14.dp
) {
    Box(
        modifier = modifier.size(size).clip(RoundedCornerShape(cornerRadius)).background(containerColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun KaamioChip(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val theme = LocalKaamioTheme.current
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) theme.accent else theme.surface,
        contentColor = if (isSelected) theme.onAccent else theme.textPrimary,
        border = BorderStroke(1.dp, if (isSelected) theme.accent else theme.border),
        modifier = Modifier.pressScale(interactionSource)
    ) {
        Text(
            text = text,
            style = Typography.labelLarge,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        )
    }
}

@Composable
fun KaamioBadge(text: String, color: Color = LocalKaamioTheme.current.accent) {
    Box(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(color.copy(alpha = 0.15f)).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(text, style = Typography.labelSmall, color = color, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

@Composable
fun KaamioStatusBanner(text: String, icon: ImageVector, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(color.copy(alpha = 0.12f)).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, color = color, style = Typography.bodyMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun KaamioListSkeleton(count: Int = 4, itemHeight: Dp = 88.dp, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(count) {
            ShimmerBox(
                modifier = Modifier.fillMaxWidth().height(itemHeight)
            )
        }
    }
}
