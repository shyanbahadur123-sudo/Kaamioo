package com.kaamio.nepal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kaamio.nepal.data.NotificationItem
import com.kaamio.nepal.ui.theme.*

private fun notificationTargetScreen(screen: String): Screen = when (screen) {
    "market" -> Screen.Market
    "learn" -> Screen.Learn
    "community" -> Screen.Community
    "chat" -> Screen.Chat
    "profile" -> Screen.Profile
    "trust_ledger" -> Screen.TrustLedger
    "settings" -> Screen.Settings
    "my_activities" -> Screen.MyActivities
    else -> Screen.Home
}

@Composable
fun NotificationsScreen(homeViewModel: HomeViewModel, viewModel: NotificationsViewModel) {
    val theme = LocalKaamioTheme.current
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(32.dp), verticalAlignment = Alignment.CenterVertically) {
                KaamioBackButton { homeViewModel.navigateTo(Screen.Home) }
                Spacer(modifier = Modifier.width(20.dp))
                Text("Notifications", style = Typography.displaySmall, fontWeight = FontWeight.Bold, color = theme.textPrimary)
            }

            when {
                isLoading && notifications.isEmpty() -> {
                    KaamioListSkeleton(count = 5, itemHeight = 72.dp)
                }
                notifications.isEmpty() -> {
                    KaamioEmptyState(
                        icon = Icons.Outlined.Notifications,
                        title = "You're all caught up",
                        subtitle = "No notifications yet."
                    )
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(horizontal = 32.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(notifications, key = { it.id }) { item ->
                            NotificationCard(
                                item = item,
                                onClick = {
                                    viewModel.markRead(item.id)
                                    homeViewModel.navigateTo(notificationTargetScreen(item.screen))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationCard(item: NotificationItem, onClick: () -> Unit) {
    val theme = LocalKaamioTheme.current
    KaamioCard(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), padding = 16.dp, elevation = 2.dp) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape)
                    .background(if (item.read) theme.surface else theme.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Info, null, tint = if (item.read) theme.textPrimary else theme.onAccent, modifier = Modifier.size(22.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    item.title,
                    style = Typography.bodyLarge,
                    color = theme.textPrimary,
                    fontWeight = if (item.read) FontWeight.Medium else FontWeight.Bold
                )
                Text(item.body, style = Typography.bodyMedium, color = theme.textSecondary)
            }
            if (!item.read) {
                Spacer(modifier = Modifier.width(12.dp))
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(theme.accent))
            }
        }
    }
}