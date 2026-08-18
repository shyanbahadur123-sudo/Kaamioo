package com.kaamio.nepal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.kaamio.nepal.R
import com.kaamio.nepal.data.ChatMessage
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MessagesScreen(homeViewModel: HomeViewModel, chatViewModel: ChatViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    val inboxList by chatViewModel.chatHistoryList.collectAsStateWithLifecycle()
    val isChatsLoading by chatViewModel.isChatsLoading.collectAsStateWithLifecycle()
    var searchActive by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val filteredInbox = if (searchQuery.isBlank()) inboxList else inboxList.filter { it.partnerName.contains(searchQuery, ignoreCase = true) }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 24.dp, vertical = 24.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Messages", style = Typography.displaySmall, fontWeight = FontWeight.ExtraBold, color = theme.textPrimary)
                    
                    IconButton(
                        onClick = { searchActive = !searchActive },
                        modifier = Modifier.size(44.dp).clip(CircleShape).background(theme.surface).border(1.dp, theme.border, CircleShape)
                    ) {
                        Icon(Icons.Default.Search, null, tint = theme.textPrimary, modifier = Modifier.size(20.dp))
                    }
                }
                if (searchActive) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search conversations", color = theme.textTertiary) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        textStyle = Typography.bodyMedium.copy(color = theme.textPrimary),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = theme.accent,
                            unfocusedBorderColor = theme.border,
                            cursorColor = theme.accent
                        ),
                        trailingIcon = {
                            IconButton(onClick = { searchQuery = ""; searchActive = false }) {
                                Icon(Icons.Default.Close, null, tint = theme.textSecondary)
                            }
                        }
                    )
                }
            }

            if (isChatsLoading && inboxList.isEmpty()) {
                LazyColumn(contentPadding = PaddingValues(horizontal = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(4) {
                        ShimmerBox(modifier = Modifier.fillMaxWidth().height(88.dp))
                    }
                }
            } else if (inboxList.isEmpty()) {
                KaamioEmptyState(
                    icon = Icons.Outlined.ChatBubbleOutline,
                    title = "No messages yet",
                    subtitle = "Start a negotiation to see your chats here"
                )
            } else if (filteredInbox.isEmpty()) {
                Column(modifier = Modifier.fillMaxSize().padding(40.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Outlined.Search, null, tint = theme.textTertiary, modifier = Modifier.size(64.dp))
                    Spacer(modifier = Modifier.height(24.dp))
                    Text("No conversations match \"$searchQuery\"", style = Typography.titleMedium, color = theme.textPrimary, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text("ACTIVE DISCUSSIONS", style = Typography.labelSmall, color = theme.textSecondary, letterSpacing = 2.sp, modifier = Modifier.padding(bottom = 8.dp))
                    }
                    items(filteredInbox, key = { it.id }) { chat ->
                        LuxuriousInboxItem(chat) {
                            chatViewModel.selectActiveChat(chat.partnerId, chat.partnerName, chat.partnerAvatar)
                            homeViewModel.navigateTo(Screen.Negotiation)
                        }
                    }
                    item { Spacer(modifier = Modifier.height(140.dp)) }
                }
            }
        }
    }
}

@Composable
fun LuxuriousInboxItem(chat: ChatMessage, onClick: () -> Unit) {
    val theme = LocalKaamioTheme.current
    val timeFormatter = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val isUnread = !chat.isRead

    KaamioCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        padding = 16.dp,
        elevation = if (isUnread) 4.dp else 0.dp,
        backgroundColor = if (isUnread) theme.surface else theme.card
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(60.dp)) {
                Box(modifier = Modifier.size(56.dp).align(Alignment.Center).clip(CircleShape).background(theme.surface)) {
                    if (chat.partnerAvatar.isNotEmpty()) {
                        AsyncImage(model = chat.partnerAvatar, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(24.dp).align(Alignment.Center), tint = theme.textTertiary)
                    }
                }
                if (isUnread) {
                    Box(modifier = Modifier.size(12.dp).align(Alignment.TopEnd).clip(CircleShape).background(theme.accent).border(2.dp, theme.card, CircleShape))
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(chat.partnerName, style = Typography.titleMedium, color = theme.textPrimary, fontWeight = if(isUnread) FontWeight.ExtraBold else FontWeight.Bold)
                    Text(timeFormatter.format(Date(chat.timestamp)), style = Typography.labelSmall, color = theme.textSecondary)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = chat.messageText,
                    style = Typography.bodyMedium,
                    color = if (isUnread) theme.textPrimary else theme.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun NegotiationChatScreen(homeViewModel: HomeViewModel, chatViewModel: ChatViewModel, profile: UserProfile) {
    val theme = LocalKaamioTheme.current
    val messagesList by chatViewModel.activeChatMessages.collectAsStateWithLifecycle()
    val partnerProfile by chatViewModel.activeChatPartner.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var inputText by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize().background(theme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 32.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KaamioBackButton { homeViewModel.navigateTo(Screen.Chat) }
                Spacer(modifier = Modifier.width(16.dp))
                Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(theme.surface)) {
                    AsyncImage(model = partnerProfile?.photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(partnerProfile?.name ?: "Professional", style = Typography.bodyLarge, color = theme.textPrimary, fontWeight = FontWeight.Bold)
                    Text("Always secure with Kaamio Escrow", style = Typography.labelSmall, color = theme.success)
                }
            }

            HorizontalDivider(color = theme.divider)

            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(vertical = 24.dp)
            ) {
                items(messagesList, key = { it.id }) { msg ->
                    val isMe = msg.senderId == FirebaseAuth.getInstance().currentUser?.uid
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart) {
                        ChatBubble(msg.messageText, isMe, msg.timestamp)
                    }
                }
            }

            LaunchedEffect(messagesList.size) {
                if (messagesList.isNotEmpty()) listState.animateScrollToItem(messagesList.lastIndex)
            }

            Surface(
                modifier = Modifier.fillMaxWidth().imePadding(),
                color = theme.surface,
                tonalElevation = 8.dp
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    TextField(
                        value = inputText, onValueChange = { inputText = it },
                        placeholder = { Text(stringResource(R.string.chat_secure_message_hint), color = theme.textTertiary) },
                        modifier = Modifier.weight(1f).height(56.dp).clip(RoundedCornerShape(28.dp)),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = theme.card, unfocusedContainerColor = theme.card,
                            focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                            focusedTextColor = theme.textPrimary
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = { if(inputText.isNotBlank()) { chatViewModel.sendChatMessage(inputText, partnerProfile?.name ?: "", partnerProfile?.photoUrl ?: ""); inputText = "" } },
                        modifier = Modifier.size(56.dp).clip(CircleShape).background(theme.accent)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = stringResource(R.string.cd_send_message), tint = theme.onAccent)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isMe: Boolean, timestamp: Long = 0L) {
    val theme = LocalKaamioTheme.current
    val shape = RoundedCornerShape(
        topStart = 20.dp, topEnd = 20.dp,
        bottomStart = if (isMe) 20.dp else 4.dp, bottomEnd = if (isMe) 4.dp else 20.dp
    )
    Column(
        modifier = Modifier.widthIn(max = 280.dp).clip(shape)
            .background(if (isMe) theme.accent else theme.card)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text(text, color = if (isMe) theme.onAccent else theme.textPrimary, style = Typography.bodyLarge)
        if (timestamp > 0L) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp)),
                color = if (isMe) theme.onAccent.copy(alpha = 0.6f) else theme.textTertiary,
                style = Typography.labelSmall,
                fontSize = 10.sp
            )
        }
    }
}