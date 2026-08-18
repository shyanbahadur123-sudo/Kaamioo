package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kaamio.nepal.data.ChatMessage
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.data.repository.IChatRepository
import com.kaamio.nepal.data.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: IChatRepository,
    private val userRepository: IUserRepository,
    private val firebaseAuth: FirebaseAuth
) : BaseViewModel() {

    private val currentUid: String get() = firebaseAuth.currentUser?.uid ?: ""

    private val _isChatsLoading = MutableStateFlow(true)
    val isChatsLoading = _isChatsLoading.asStateFlow()

    private val _activeChatPartner = MutableStateFlow<UserProfile?>(null)
    val activeChatPartner = _activeChatPartner.asStateFlow()

    val chatHistoryList: StateFlow<List<ChatMessage>> = chatRepository.getCurrentUserMessages()
        .map { messages ->
            _isChatsLoading.value = false
            messages.groupBy { if (it.senderId == currentUid) it.partnerId else it.senderId }
                .map { (partnerId, partnerMessages) ->
                    val latest = partnerMessages.maxByOrNull { it.timestamp } ?: partnerMessages.first()
                    latest.copy(partnerId = partnerId)
                }.sortedByDescending { it.timestamp }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeChatPartnerId = MutableStateFlow<String?>(null)
    val activeChatPartnerId = _activeChatPartnerId.asStateFlow()

    val activeChatMessages: StateFlow<List<ChatMessage>> = _activeChatPartnerId
        .flatMapLatest { partnerId ->
            if (partnerId == null) flowOf(emptyList())
            else chatRepository.getMessagesForPartner(partnerId)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectActiveChat(partnerId: String, name: String = "", avatar: String = "") {
        _activeChatPartnerId.value = partnerId
        // Pre-fill with what we have
        _activeChatPartner.value = UserProfile(name = name, photoUrl = avatar)

        // Mark the partner's incoming messages as read (local + remote).
        viewModelScope.launch {
            try {
                chatRepository.markMessagesRead(partnerId)
            } catch (_: Exception) {}
        }

        // Fetch real info
        viewModelScope.launch {
            try {
                val fullProfile = userRepository.getUserProfile(partnerId)
                if (fullProfile != null) {
                    _activeChatPartner.value = fullProfile
                }
            } catch (_: Exception) {}
        }
    }

    private var sendingMessage = false

    fun sendChatMessage(text: String, partnerName: String, partnerAvatar: String) {
        val partnerId = _activeChatPartnerId.value ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || sendingMessage) return
        sendingMessage = true
        viewModelScope.launch {
            try {
                chatRepository.insertMessage(ChatMessage(
                    id = "msg_${UUID.randomUUID()}",
                    senderId = currentUid,
                    partnerId = partnerId,
                    partnerName = partnerName,
                    partnerAvatar = partnerAvatar,
                    messageText = trimmed
                ))
            } catch (_: Exception) {
                showSnackbar("Failed to send message. Try again.")
            } finally {
                sendingMessage = false
            }
        }
    }

    fun sendChatImage(uri: android.net.Uri, partnerName: String, partnerAvatar: String) {
        val partnerId = _activeChatPartnerId.value ?: return
        if (sendingMessage) return
        sendingMessage = true
        viewModelScope.launch {
            try {
                val url = chatRepository.uploadChatImage(uri)
                chatRepository.insertMessage(ChatMessage(
                    id = "msg_${UUID.randomUUID()}",
                    senderId = currentUid,
                    partnerId = partnerId,
                    partnerName = partnerName,
                    partnerAvatar = partnerAvatar,
                    messageText = "Image",
                    imageUrl = url
                ))
            } catch (_: Exception) {
                showSnackbar("Failed to send image. Try again.")
            } finally {
                sendingMessage = false
            }
        }
    }

    fun updateProposalStatus(messageId: String, status: String) {
        viewModelScope.launch {
            try {
                chatRepository.updateMessageProposalStatus(messageId, status)
            } catch (_: Exception) {
                showSnackbar("Failed to update proposal.")
            }
        }
    }
}
