package com.kaamio.nepal.data.repository

import com.google.firebase.auth.AuthCredential
import com.kaamio.nepal.data.UserProfile
import kotlinx.coroutines.flow.Flow

interface IUserRepository {
    val userProfile: Flow<UserProfile?>
    suspend fun signInWithGoogle(credential: AuthCredential)
    suspend fun signInWithEmail(email: String, pass: String)
    suspend fun signUpWithEmail(email: String, pass: String, name: String, agreedToTerms: Boolean = true)
    suspend fun sendPasswordReset(email: String)
    suspend fun sendEmailVerification()
    suspend fun reloadAndCheckEmailVerified(): Boolean
    suspend fun signInWithPhoneCredential(credential: AuthCredential)
    fun verifyPhoneNumber(phoneNumber: String, activity: android.app.Activity, callbacks: com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks, resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken? = null)
    suspend fun updateProfile(updates: Map<String, Any>)
    suspend fun setFcmToken(token: String)
    suspend fun syncFcmTokenIfNeeded()
    suspend fun clearFcmToken()
    suspend fun getCurrentUser(): UserProfile?
    suspend fun getUserProfile(uid: String): UserProfile?
    suspend fun getReviewsForUser(uid: String): List<com.kaamio.nepal.data.Review>
    suspend fun getMyReviews(): List<com.kaamio.nepal.data.Review>
    suspend fun submitReview(reviewedUserId: String, rating: Int, comment: String)
    suspend fun requestIdentityVerification()
    suspend fun submitKycVerification(
        documentUri: android.net.Uri?,
        selfieUri: android.net.Uri?,
        fullName: String,
        address: String,
        idType: String,
        idNumber: String
    )
    suspend fun getTrustScoreHistory(): List<Pair<Long, Int>>
    suspend fun logout()
    fun cleanup()
}
