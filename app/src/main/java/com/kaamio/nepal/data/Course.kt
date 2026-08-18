package com.kaamio.nepal.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "course")
data class Course(
    @PrimaryKey val id: String,
    val title: String,
    val instructor: String,
    val duration: String,
    val rating: Float,
    val studentsCount: String,
    val progress: Int = 0,
    val category: String,
    val thumbnailUrl: String,
    val description: String = "",
    val isBookmarked: Boolean = false,
    val isVerifiedInstructor: Boolean = true,
    val modules: String = "",
    val instructorId: String = "",
    val price: String = "",
    val isUnlocked: Boolean = false,
    val unlockedBy: String = ""
) {
    val isPremium: Boolean
        get() = price.isNotBlank() && price.trim() != "0" && !price.contains("free", ignoreCase = true)

    val moduleList: List<String>
        get() = modules.split('|', '\n').map { it.trim() }.filter { it.isNotBlank() }

    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "title" to title,
        "instructor" to instructor,
        "duration" to duration,
        "rating" to rating,
        "studentsCount" to studentsCount,
        "progress" to progress,
        "category" to category,
        "thumbnailUrl" to thumbnailUrl,
        "description" to description,
        "isBookmarked" to isBookmarked,
        "isVerifiedInstructor" to isVerifiedInstructor,
        "modules" to modules,
        "instructorId" to instructorId,
        "price" to price,
        "unlockedBy" to unlockedBy.split(',').map { it.trim() }.filter { it.isNotBlank() }
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): Course = Course(
            id = id,
            title = data["title"] as? String ?: "",
            instructor = data["instructor"] as? String ?: "",
            duration = data["duration"] as? String ?: "",
            rating = (data["rating"] as? Double)?.toFloat() ?: (data["rating"] as? Long)?.toFloat() ?: 0f,
            studentsCount = data["studentsCount"] as? String ?: "0",
            progress = (data["progress"] as? Long)?.toInt() ?: 0,
            category = data["category"] as? String ?: "",
            thumbnailUrl = data["thumbnailUrl"] as? String ?: "",
            description = data["description"] as? String ?: "",
            isBookmarked = data["isBookmarked"] as? Boolean ?: false,
            isVerifiedInstructor = data["isVerifiedInstructor"] as? Boolean ?: true,
            modules = data["modules"] as? String ?: "",
            instructorId = data["instructorId"] as? String ?: "",
            price = data["price"] as? String ?: "",
            unlockedBy = (data["unlockedBy"] as? List<*>)?.joinToString(",") ?: ""
        )
    }
}
