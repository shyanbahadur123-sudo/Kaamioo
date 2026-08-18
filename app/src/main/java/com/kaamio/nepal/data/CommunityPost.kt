package com.kaamio.nepal.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "community_post")
data class CommunityPost(
    @PrimaryKey val id: String,
    val authorId: String = "",
    val authorName: String,
    val authorRole: String,
    val authorAvatar: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    val isLiked: Boolean = false
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "authorId" to authorId,
        "authorName" to authorName,
        "authorRole" to authorRole,
        "authorAvatar" to authorAvatar,
        "content" to content,
        "timestamp" to timestamp,
        "likesCount" to likesCount,
        "commentsCount" to commentsCount,
        "isLiked" to isLiked,
        "likedBy" to emptyList<String>()
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): CommunityPost = CommunityPost(
            id = id,
            authorId = data["authorId"] as? String ?: "",
            authorName = data["authorName"] as? String ?: "",
            authorRole = data["authorRole"] as? String ?: "",
            authorAvatar = data["authorAvatar"] as? String ?: "",
            content = data["content"] as? String ?: "",
            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis(),
            likesCount = (data["likesCount"] as? Long)?.toInt() ?: 0,
            commentsCount = (data["commentsCount"] as? Long)?.toInt() ?: 0,
            isLiked = data["isLiked"] as? Boolean ?: false
        )
    }
}
