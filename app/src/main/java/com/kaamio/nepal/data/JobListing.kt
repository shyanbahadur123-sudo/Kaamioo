package com.kaamio.nepal.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "job_listing")
data class JobListing(
    @PrimaryKey val id: String,
    val ownerId: String = "",
    val title: String,
    val company: String,
    val logoUrl: String,
    val salary: String,
    val location: String,
    val isRemote: Boolean,
    val type: String,
    val isApplied: Boolean = false,
    val isBookmarked: Boolean = false,
    val isVerifiedCompany: Boolean = false,
    val category: String,
    val trustScore: Int = 0,
    val budget: String = "",
    val deadlineDays: Int = 14,
    val milestonesCount: Int = 4,
    val clientRating: Float = 0f,
    val preferredSkills: String = "",
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "id" to id,
        "ownerId" to ownerId,
        "title" to title,
        "company" to company,
        "logoUrl" to logoUrl,
        "salary" to salary,
        "location" to location,
        "isRemote" to isRemote,
        "type" to type,
        "isApplied" to isApplied,
        "isBookmarked" to isBookmarked,
        "isVerifiedCompany" to isVerifiedCompany,
        "category" to category,
        "trustScore" to trustScore,
        "budget" to budget,
        "deadlineDays" to deadlineDays,
        "milestonesCount" to milestonesCount,
        "clientRating" to clientRating,
        "preferredSkills" to preferredSkills,
        "description" to description,
        "createdAt" to createdAt
    )

    companion object {
        fun fromFirestoreSnapshot(id: String, data: Map<String, Any>): JobListing = JobListing(
            id = id,
            ownerId = data["ownerId"] as? String ?: "",
            title = data["title"] as? String ?: "",
            company = data["company"] as? String ?: "",
            logoUrl = data["logoUrl"] as? String ?: "",
            salary = data["salary"] as? String ?: "",
            location = data["location"] as? String ?: "",
            isRemote = data["isRemote"] as? Boolean ?: false,
            type = data["type"] as? String ?: "",
            isApplied = data["isApplied"] as? Boolean ?: false,
            isBookmarked = data["isBookmarked"] as? Boolean ?: false,
            isVerifiedCompany = data["isVerifiedCompany"] as? Boolean ?: false,
            category = data["category"] as? String ?: "",
            trustScore = (data["trustScore"] as? Long)?.toInt() ?: 0,
            budget = data["budget"] as? String ?: "",
            deadlineDays = (data["deadlineDays"] as? Long)?.toInt() ?: 14,
            milestonesCount = (data["milestonesCount"] as? Long)?.toInt() ?: 4,
            clientRating = (data["clientRating"] as? Double)?.toFloat() ?: (data["clientRating"] as? Long)?.toFloat() ?: 0f,
            preferredSkills = data["preferredSkills"] as? String ?: "",
            description = data["description"] as? String ?: "",
            createdAt = data["createdAt"] as? Long ?: System.currentTimeMillis()
        )
    }
}
