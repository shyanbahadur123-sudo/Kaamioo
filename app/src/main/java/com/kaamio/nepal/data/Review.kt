package com.kaamio.nepal.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "review")
data class Review(
    @PrimaryKey val id: String,
    val reviewedUserId: String,
    val reviewerId: String,
    val reviewerName: String = "",
    val reviewerPhotoUrl: String = "",
    val rating: Int = 5,
    val comment: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "reviewedUserId" to reviewedUserId,
        "reviewerId" to reviewerId,
        "reviewerName" to reviewerName,
        "reviewerPhotoUrl" to reviewerPhotoUrl,
        "rating" to rating,
        "comment" to comment,
        "timestamp" to timestamp
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): Review = Review(
            id = id,
            reviewedUserId = data["reviewedUserId"] as? String ?: "",
            reviewerId = data["reviewerId"] as? String ?: "",
            reviewerName = data["reviewerName"] as? String ?: "",
            reviewerPhotoUrl = data["reviewerPhotoUrl"] as? String ?: "",
            rating = (data["rating"] as? Long)?.toInt() ?: (data["rating"] as? Int) ?: 5,
            comment = data["comment"] as? String ?: "",
            timestamp = data["timestamp"] as? Long ?: System.currentTimeMillis()
        )
    }
}
