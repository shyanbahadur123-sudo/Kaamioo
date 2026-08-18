package com.kaamio.nepal.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey val id: String,
    val senderId: String,
    val partnerId: String,
    val partnerName: String,
    val partnerAvatar: String,
    val messageText: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = true,
    val isProposal: Boolean = false,
    val proposalRate: String = "",
    val proposalDuration: String = "",
    val proposalStatus: String = "PENDING",
    val chartType: String? = null,
    val chartData: String? = null,
    val imageUrl: String? = null
) {
    fun toFirestoreMap(): Map<String, Any?> = mapOf(
        "senderId" to senderId,
        "partnerId" to partnerId,
        "partnerName" to partnerName,
        "partnerAvatar" to partnerAvatar,
        "messageText" to messageText,
        "timestamp" to timestamp,
        "isRead" to isRead,
        "isProposal" to isProposal,
        "proposalRate" to proposalRate,
        "proposalDuration" to proposalDuration,
        "proposalStatus" to proposalStatus,
        "chartType" to chartType,
        "chartData" to chartData,
        "imageUrl" to imageUrl
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): ChatMessage = ChatMessage(
            id = id,
            senderId = data["senderId"] as? String ?: "",
            partnerId = data["partnerId"] as? String ?: "",
            partnerName = data["partnerName"] as? String ?: "",
            partnerAvatar = data["partnerAvatar"] as? String ?: "",
            messageText = data["messageText"] as? String ?: "",
            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
            isRead = data["isRead"] as? Boolean ?: true,
            isProposal = data["isProposal"] as? Boolean ?: false,
            proposalRate = data["proposalRate"] as? String ?: "",
            proposalDuration = data["proposalDuration"] as? String ?: "",
            proposalStatus = data["proposalStatus"] as? String ?: "PENDING",
            chartType = data["chartType"] as? String,
            chartData = data["chartData"] as? String,
            imageUrl = data["imageUrl"] as? String
        )
    }
}
