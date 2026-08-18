package com.kaamio.nepal.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "notification_item")
data class NotificationItem(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val screen: String = "home",
    val read: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "title" to title,
        "body" to body,
        "screen" to screen,
        "read" to read,
        "timestamp" to timestamp
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): NotificationItem = NotificationItem(
            id = id,
            title = data["title"] as? String ?: "",
            body = data["body"] as? String ?: "",
            screen = data["screen"] as? String ?: "home",
            read = data["read"] as? Boolean ?: false,
            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()
        )
    }
}
