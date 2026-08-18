package com.kaamio.nepal.data.repository

import com.kaamio.nepal.data.ChatMessage
import kotlinx.coroutines.flow.Flow

interface IChatRepository {
    val allMessages: Flow<List<ChatMessage>>
    fun getMessagesForPartner(partnerId: String): Flow<List<ChatMessage>>
    fun getCurrentUserMessages(): Flow<List<ChatMessage>>
    suspend fun updateMessageProposalStatus(messageId: String, status: String)
    suspend fun markMessagesRead(partnerId: String)
    suspend fun insertMessage(message: ChatMessage)
    suspend fun uploadChatImage(uri: android.net.Uri): String
    fun cleanup()
}
