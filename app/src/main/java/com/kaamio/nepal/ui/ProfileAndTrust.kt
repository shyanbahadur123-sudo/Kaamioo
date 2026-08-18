package com.kaamio.nepal.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kaamio.nepal.BuildConfig
import com.kaamio.nepal.R
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.ui.theme.*

@Composable
fun ProfileScreen(homeViewModel: HomeViewModel, profileViewModel: ProfileViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    var showPrivacyDialog by remember { mutableStateOf(false) }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(theme.surface)
                        .border(1.dp, theme.border, CircleShape)
                        .clickable { homeViewModel.navigateTo(Screen.Settings) },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.Settings, stringResource(R.string.cd_open_settings), tint = theme.textPrimary, modifier = Modifier.size(24.dp))
                }
            }
        },
        containerColor = theme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 120.dp)
        ) {
            // Editorial Header
            Box(modifier = Modifier.fillMaxWidth().height(220.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(120.dp)) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .align(Alignment.Center)
                                .clip(CircleShape)
                                .background(theme.surface)
                                .border(2.dp, theme.border, CircleShape)
                        ) {
                            if (profile.photoUrl.isNotEmpty()) {
                                AsyncImage(model = profile.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                Icon(Icons.Default.Person, null, modifier = Modifier.size(52.dp).align(Alignment.Center), tint = theme.textTertiary)
                            }
                        }
                        if (profile.verificationLevel >= 2) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .align(Alignment.BottomEnd)
                                    .clip(CircleShape)
                                    .background(theme.accent)
                                    .border(2.dp, theme.background, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Check, null, tint = theme.onAccent, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(
                        text = profile.name.ifEmpty { stringResource(R.string.profile_title) }, 
                        style = Typography.displaySmall, 
                        color = theme.textPrimary, 
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-1).sp
                    )
                    if (profile.kaamioId.isNotEmpty()) {
                        Text(
                            text = "PROFESSIONAL ID: ${profile.kaamioId.uppercase()}", 
                            style = Typography.labelSmall, 
                            color = theme.textSecondary,
                            letterSpacing = 1.5.sp
                        )
                    }
                }
            }

            // Wallet-style Trust Card
            KaamioCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), backgroundColor = theme.card, elevation = 4.dp) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(8.dp)) {
                    val animatedScore by animateFloatAsState(targetValue = profile.trustScore / 100f, animationSpec = tween(1200), label = "score")
                    Box(modifier = Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawArc(color = theme.border, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                            drawArc(color = theme.accent, startAngle = -90f, sweepAngle = animatedScore * 360f, useCenter = false, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                        }
                        Text("${profile.trustScore}%", style = Typography.labelLarge, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(20.dp))
                    Column {
                        Text(stringResource(R.string.trust_index_label), style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
                        Text(verificationTitle(profile.verificationLevel), style = Typography.titleMedium, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            PremiumSectionHeader(stringResource(R.string.section_summary))
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(stringResource(R.string.stat_jobs), "${profile.completedJobsCount}", Modifier.weight(1f))
                StatCard(stringResource(R.string.section_reviews), "${profile.totalReviews}", Modifier.weight(1f))
                StatCard("Rating", String.format("%.1f", profile.averageRating), Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(40.dp))
            PremiumSectionHeader("ACCOUNT CONTROLS")
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 32.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                AccountActionRow(stringResource(R.string.action_edit_profile), Icons.Outlined.Edit) { homeViewModel.navigateTo(Screen.EditProfile) }
                AccountActionRow(stringResource(R.string.trust_ledger_title), Icons.Outlined.Shield) { homeViewModel.navigateTo(Screen.TrustLedger) }
                AccountActionRow("Privacy Policy", Icons.Outlined.PrivacyTip) { showPrivacyDialog = true }
                Spacer(modifier = Modifier.height(8.dp))
                KaamioButton(stringResource(R.string.btn_sign_out), { homeViewModel.logout() }, modifier = Modifier.fillMaxWidth(), containerColor = theme.surface, contentColor = theme.textPrimary)
            }
        }
    }

    if (showPrivacyDialog) {
        AlertDialog(
            onDismissRequest = { showPrivacyDialog = false },
            title = { Text("Privacy Policy", style = Typography.titleLarge) },
            text = { Text("Your profile, trust score, reviews and chat history are stored securely. By default your profile is visible to verified clients and professionals; enable Profile Privacy in Settings to restrict who can view your full details. Payments are processed via Khalti/eSewa escrow and are never stored on-device.") },
            confirmButton = {
                TextButton(onClick = { showPrivacyDialog = false }) { Text("Close", color = theme.textPrimary) }
            },
            containerColor = theme.surface,
            titleContentColor = theme.textPrimary,
            textContentColor = theme.textSecondary
        )
    }
}

@Composable
fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    val theme = LocalKaamioTheme.current
    KaamioCard(modifier = modifier, padding = 16.dp, elevation = 4.dp) {
        Text(value, style = Typography.headlineLarge, color = theme.textPrimary, fontWeight = FontWeight.ExtraBold)
        Text(label.uppercase(), style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 1.sp)
    }
}

@Composable
fun EditProfileScreen(homeViewModel: HomeViewModel, profileViewModel: ProfileViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    var name by remember { mutableStateOf(profile.name) }
    var province by remember { mutableStateOf(profile.province) }
    var district by remember { mutableStateOf(profile.district) }
    
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KaamioBackButton { homeViewModel.navigateTo(Screen.Profile) }
                Spacer(modifier = Modifier.width(20.dp))
                Text(stringResource(R.string.edit_profile_title), style = Typography.displaySmall, color = theme.textPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = theme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            KaamioTextField(value = name, onValueChange = { name = it }, label = stringResource(R.string.label_full_name))
            KaamioTextField(value = province, onValueChange = { province = it }, label = stringResource(R.string.label_province))
            KaamioTextField(value = district, onValueChange = { district = it }, label = stringResource(R.string.label_district))
            
            Spacer(modifier = Modifier.height(24.dp))
            KaamioButton(stringResource(R.string.edit_profile_save), { 
                profileViewModel.updateProfile(mapOf("displayName" to name, "province" to province, "district" to district))
                homeViewModel.navigateTo(Screen.Profile)
            }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun SettingsScreen(homeViewModel: HomeViewModel, settingsViewModel: SettingsViewModel, globalViewModel: GlobalViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    val isDarkMode by settingsViewModel.isDarkMode.collectAsStateWithLifecycle()
    val currentLanguage by settingsViewModel.currentLanguage.collectAsStateWithLifecycle()
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KaamioBackButton { homeViewModel.navigateTo(Screen.Profile) }
                Spacer(modifier = Modifier.width(20.dp))
                Text(stringResource(R.string.settings_title), style = Typography.displaySmall, color = theme.textPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = theme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // APPEARANCE
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.settings_theme_title).uppercase(), style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
            
            SettingsToggleCard(
                label = stringResource(R.string.settings_dark_mode_label),
                icon = if (isDarkMode) Icons.Outlined.DarkMode else Icons.Outlined.LightMode,
                checked = isDarkMode,
                onCheckedChange = { settingsViewModel.setDarkMode(it) }
            )

            // PREFERENCES
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.label_language).uppercase(), style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
            
            AccountActionRow(stringResource(if(currentLanguage == "en") R.string.lang_english else R.string.lang_nepali), Icons.Outlined.Language) {
                settingsViewModel.setLanguage(if (currentLanguage == "en") "ne" else "en")
            }

            // NOTIFICATIONS
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.notifications_title).uppercase(), style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
            
            SettingsToggleCard(
                label = stringResource(R.string.toggle_notifications),
                icon = Icons.Outlined.NotificationsActive,
                checked = profile.notificationsEnabled,
                onCheckedChange = { settingsViewModel.updateSettingsPreference("notificationsEnabled", it, profile) }
            )

            // PRIVACY & SECURITY
            Spacer(modifier = Modifier.height(8.dp))
            Text("VISIBILITY", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
            
            SettingsToggleCard(
                label = "Profile Visibility",
                icon = Icons.Outlined.Visibility,
                checked = profile.privacyEnabled,
                onCheckedChange = { settingsViewModel.updateSettingsPreference("privacyEnabled", it, profile) }
            )
            
            AccountActionRow("About Kaamio", Icons.Outlined.Info) { showAboutDialog = true }

            // SYSTEM
            Spacer(modifier = Modifier.height(8.dp))
            Text("SYSTEM", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
            
            AccountActionRow("Clear Local Cache", Icons.Outlined.DeleteSweep) { showClearCacheDialog = true }
            
            Spacer(modifier = Modifier.height(24.dp))
            KaamioButton(stringResource(R.string.btn_sign_out), { homeViewModel.logout() }, modifier = Modifier.fillMaxWidth(), containerColor = theme.surface, contentColor = theme.textPrimary)
            
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Local Cache?", style = Typography.titleLarge) },
            text = { Text("This deletes cached listings, courses, chats, posts, reviews and notifications on this device. You can re-fetch them from the network. Your channel and account are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    settingsViewModel.clearLocalCache()
                }) { Text("Clear", color = theme.textPrimary) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("Cancel") }
            },
            containerColor = theme.surface,
            titleContentColor = theme.textPrimary,
            textContentColor = theme.textSecondary
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("Kaamio", style = Typography.titleLarge) },
            text = { Text("Version ${BuildConfig.VERSION_NAME}\n\nKaamio connects clients with verified Nepali service channel professionals. Payments are held in escrow until work is completed to satisfaction.") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) { Text("Close", color = theme.textPrimary) }
            },
            containerColor = theme.surface,
            titleContentColor = theme.textPrimary,
            textContentColor = theme.textSecondary
        )
    }
}

@Composable
fun SettingsToggleCard(label: String, icon: ImageVector, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val theme = LocalKaamioTheme.current
    KaamioCard(modifier = Modifier.fillMaxWidth(), padding = 8.dp, elevation = 4.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(theme.surface), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = theme.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = Typography.bodyLarge, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = theme.accent,
                    uncheckedThumbColor = theme.textSecondary,
                    uncheckedTrackColor = theme.surface
                )
            )
        }
    }
}

@Composable
fun AccountActionRow(label: String, icon: ImageVector, onClick: () -> Unit) {
    val theme = LocalKaamioTheme.current
    KaamioCard(modifier = Modifier.fillMaxWidth(), onClick = onClick, padding = 16.dp, elevation = 4.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(theme.surface), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = theme.textPrimary, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, style = Typography.bodyLarge, color = theme.textPrimary, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, null, tint = theme.textTertiary)
        }
    }
}

@Composable
fun TrustLedgerScreen(homeViewModel: HomeViewModel, profileViewModel: ProfileViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    val trustHistory by profileViewModel.trustHistory.collectAsStateWithLifecycle()
    val history = trustHistory.sortedBy { it.first }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 40.dp)) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                KaamioBackButton { homeViewModel.navigateTo(Screen.Profile) }
                Spacer(modifier = Modifier.width(20.dp))
                Text(stringResource(R.string.trust_ledger_title), style = Typography.displaySmall, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }

            KaamioCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), elevation = 4.dp) {
                Text(stringResource(R.string.section_trajectory), style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(24.dp))
                if (history.isEmpty()) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp)) {
                        Icon(Icons.Default.Timeline, null, tint = theme.textTertiary, modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No trust history yet", style = Typography.titleMedium, color = theme.textPrimary)
                        Text("Complete your profile and finish your first contract to build a track record.", style = Typography.bodyMedium, color = theme.textSecondary)
                    }
                } else {
                    Box(modifier = Modifier.fillMaxWidth().height(160.dp).padding(8.dp)) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val scores = history.map { it.second.toFloat() }.toFloatArray()
                            val min = scores.minOrNull()?.coerceAtLeast(0f) ?: 0f
                            val max = scores.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                            val span = (max - min).coerceAtLeast(1f)
                            val normalized = scores.map { 1f - ((it - min) / span).coerceIn(0f, 1f) }
                            val path = Path().apply {
                                moveTo(0f, size.height * normalized[0])
                                normalized.forEachIndexed { i, p ->
                                    if (i > 0) lineTo(size.width * (i / (normalized.size - 1f)), size.height * p)
                                }
                            }
                            drawPath(path, color = theme.accent, style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            PremiumSectionHeader("TRUST MILESTONES")
            Spacer(modifier = Modifier.height(16.dp))
            if (history.isEmpty()) {
                KaamioCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp), elevation = 4.dp) {
                    Text(
                        "Your trust score is recalculated automatically as you verify your phone, complete KYC, and finish contracts. Milestones will appear here.",
                        style = Typography.bodyMedium,
                        color = theme.textSecondary
                    )
                }
            } else {
                history.forEach { (timestamp, score) ->
                    KaamioCard(modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp), elevation = 4.dp) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(theme.surface), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.VerifiedUser, null, tint = theme.success, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text("Trust score updated", style = Typography.bodyLarge, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                                Text("${score}% • ${formatRelativeDate(timestamp)}", style = Typography.labelSmall, color = theme.textSecondary)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatRelativeDate(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val days = diff / (24 * 60 * 60 * 1000)
    return when {
        days <= 0 -> "today"
        days == 1L -> "yesterday"
        else -> "$days days ago"
    }
}

private fun verificationTitle(level: Int): String = when (level) {
    3 -> "Identity Verified"
    2 -> "Account Verified"
    1 -> "Phone Verified"
    else -> "Getting Started"
}
