package com.kaamio.nepal.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val role: String = "",
    val completionProgress: Int = 0,
    val trustScore: Int = 0,
    val about: String = "",
    val language: String = "English",
    val isDarkMode: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val privacyEnabled: Boolean = true,
    val completedJobsCount: Int = 0,
    val verifiedSkillsCount: Int = 0,
    val endorsementsCount: Int = 0,
    val teacherEarnings: Int = 0,
    val teacherStudents: Int = 0,
    val teacherCourses: Int = 0,
    val teacherReviews: Int = 0,
    val teacherRating: Float = 0.0f,
    val kaamioId: String = "",
    val phoneNumber: String = "",
    val email: String = "",
    val photoUrl: String = "",
    val gender: String = "",
    val dateOfBirth: String = "",
    val province: String = "",
    val district: String = "",
    val municipality: String = "",
    val userTypes: String = "",
    val skills: String = "",
    val experience: String = "",
    val verificationLevel: Int = 0,
    val isGoogleLinked: Boolean = false,
    val isLoggedIn: Boolean = false,
    val verified: Boolean = false,
    val profileCompleted: Boolean = false,
    val createdAt: Long = 0L,
    val lastLogin: Long = 0L,
    val isOnline: Boolean = false,
    val fcmToken: String = "",
    val isPhoneVerified: Boolean = false,
    val isGoogleVerified: Boolean = false,
    val isIdentityVerified: Boolean = false,
    val kycStatus: String = "",
    val averageRating: Float = 0.0f,
    val totalReviews: Int = 0
) {
    fun toFirestoreMap(): Map<String, Any> = mapOf(
        "kaamioId" to kaamioId,
        "displayName" to name,
        "email" to email,
        "phoneNumber" to phoneNumber,
        "photoURL" to photoUrl,
        "province" to province,
        "district" to district,
        "municipality" to municipality,
        "role" to role,
        "userTypes" to userTypes,
        "skills" to skills,
        "experience" to experience,
        "profileCompleted" to profileCompleted,
        "trustScore" to trustScore,
        "isPhoneVerified" to isPhoneVerified,
        "isGoogleVerified" to isGoogleVerified,
        "isIdentityVerified" to isIdentityVerified,
        "notificationsEnabled" to notificationsEnabled,
        "privacyEnabled" to privacyEnabled,
        "gender" to gender,
        "dateOfBirth" to dateOfBirth,
        "about" to about,
        "fcmToken" to fcmToken,
        "isOnline" to isOnline,
        "createdAt" to createdAt,
        "lastLogin" to lastLogin,
        "language" to language,
        "averageRating" to averageRating,
        "totalReviews" to totalReviews,
        "kycStatus" to kycStatus
    )

    companion object {
        fun fromDocument(id: String, data: Map<String, Any>): UserProfile {
            val completionProgress = try {
                val isCompleted = data["profileCompleted"] as? Boolean ?: false
                if (isCompleted) 100 else (data["profileCompletion"] as? Long)?.toInt() ?: 20
            } catch (_: Exception) { 20 }

            val isPhoneVerified = data["isPhoneVerified"] as? Boolean ?: false
            val isGoogleVerified = data["isGoogleVerified"] as? Boolean ?: false
            val isIdentityVerified = data["isIdentityVerified"] as? Boolean ?: false
            val verified = data["verified"] as? Boolean ?: false

            val verificationLevel = when {
                isIdentityVerified -> 3
                isGoogleVerified || verified -> 2
                isPhoneVerified -> 1
                else -> 0
            }

            return UserProfile(
                id = 1, // Fixed ID for local profile
                name = data["displayName"] as? String ?: data["fullName"] as? String ?: "",
                role = data["role"] as? String ?: "Specialist",
                completionProgress = completionProgress,
                trustScore = (data["trustScore"] as? Long)?.toInt() ?: 50,
                kaamioId = data["kaamioId"] as? String ?: "",
                phoneNumber = data["phoneNumber"] as? String ?: "",
                email = data["email"] as? String ?: "",
                photoUrl = data["photoURL"] as? String ?: "",
                province = data["province"] as? String ?: "",
                district = data["district"] as? String ?: "",
                municipality = data["municipality"] as? String ?: "",
                userTypes = data["userTypes"] as? String ?: "",
                skills = data["skills"] as? String ?: "",
                experience = data["experience"] as? String ?: "",
                verificationLevel = verificationLevel,
                isGoogleLinked = isGoogleVerified,
                isLoggedIn = true,
                language = data["language"] as? String ?: "English",
                verified = verified,
                profileCompleted = completionProgress == 100,
                notificationsEnabled = data["notificationsEnabled"] as? Boolean ?: true,
                privacyEnabled = data["privacyEnabled"] as? Boolean ?: true,
                gender = data["gender"] as? String ?: "",
                dateOfBirth = data["dateOfBirth"] as? String ?: "",
                about = data["about"] as? String ?: "",
                fcmToken = data["fcmToken"] as? String ?: "",
                isOnline = data["isOnline"] as? Boolean ?: false,
                createdAt = data["createdAt"] as? Long ?: 0L,
                lastLogin = data["lastLogin"] as? Long ?: 0L,
                isPhoneVerified = isPhoneVerified,
                isGoogleVerified = isGoogleVerified,
                isIdentityVerified = isIdentityVerified,
                kycStatus = data["kycStatus"] as? String ?: "",
                averageRating = (data["averageRating"] as? Double)?.toFloat() ?: (data["averageRating"] as? Long)?.toFloat() ?: 0.0f,
                totalReviews = (data["totalReviews"] as? Long)?.toInt() ?: 0
            )
        }
    }
}
