package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.data.UserProfileDao
import com.kaamio.nepal.data.repository.IUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val userRepository: IUserRepository,
    private val userProfileDao: UserProfileDao
) : BaseViewModel() {

    val userProfile: StateFlow<UserProfile> = userRepository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    private val _isUpdating = MutableStateFlow(false)
    val isUpdating = _isUpdating.asStateFlow()

    private val _trustHistory = MutableStateFlow<List<Pair<Long, Int>>>(emptyList())
    val trustHistory = _trustHistory.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                _trustHistory.value = userRepository.getTrustScoreHistory()
            } catch (_: Exception) {}
        }
    }

    fun updateProfile(updates: Map<String, Any>) {
        viewModelScope.launch {
            try {
                userRepository.updateProfile(updates)
            } catch (_: Exception) {
                showSnackbar("Failed to save profile changes.")
            }
        }
    }

    fun completeOnboarding(role: String, province: String, district: String, experience: String, phoneNumber: String, preferredLanguage: String) {
        viewModelScope.launch {
            _isUpdating.value = true
            try {
                val updates = mapOf(
                    "role" to role,
                    "province" to province,
                    "district" to district,
                    "experience" to experience,
                    "phoneNumber" to phoneNumber,
                    "language" to preferredLanguage,
                    "profileCompletion" to 100,
                    "profileCompleted" to true
                )
                
                // 1. Update Firestore and wait for success
                userRepository.updateProfile(updates)
                
                // 2. Fetch the absolute latest from Firestore to ensure Room is perfectly in sync
                userRepository.getCurrentUser()
                
            } catch (e: Exception) {
                // Log error or show message if needed
            } finally {
                _isUpdating.value = false
            }
        }
    }

    private val _isSubmittingReview = MutableStateFlow(false)
    val isSubmittingReview = _isSubmittingReview.asStateFlow()

    private val _reviewMessage = MutableStateFlow<String?>(null)
    val reviewMessage = _reviewMessage.asStateFlow()

    fun submitReview(reviewedUserId: String, rating: Int, comment: String) {
        viewModelScope.launch {
            _isSubmittingReview.value = true
            _reviewMessage.value = null
            try {
                userRepository.submitReview(reviewedUserId, rating, comment)
                _reviewMessage.value = "Review submitted successfully."
            } catch (e: Exception) {
                _reviewMessage.value = e.localizedMessage ?: "Failed to submit review"
            } finally {
                _isSubmittingReview.value = false
            }
        }
    }

    suspend fun fetchMyReviews(): List<com.kaamio.nepal.data.Review> =
        userRepository.getMyReviews()

    fun requestIdentityVerification() {
        viewModelScope.launch {
            try {
                userRepository.requestIdentityVerification()
                showSnackbar("Verification submitted for review.")
            } catch (e: Exception) {
                showSnackbar(e.localizedMessage ?: "Identity verification failed")
            }
        }
    }

    private val _isSubmittingKyc = MutableStateFlow(false)
    val isSubmittingKyc = _isSubmittingKyc.asStateFlow()

    fun submitKyc(
        documentUri: android.net.Uri?,
        selfieUri: android.net.Uri?,
        fullName: String,
        address: String,
        idType: String,
        idNumber: String
    ) {
        viewModelScope.launch {
            _isSubmittingKyc.value = true
            try {
                userRepository.submitKycVerification(documentUri, selfieUri, fullName, address, idType, idNumber)
                showSnackbar("Identity documents submitted for review.")
            } catch (e: Exception) {
                showSnackbar(e.localizedMessage ?: "Submission failed")
            } finally {
                _isSubmittingKyc.value = false
            }
        }
    }

    fun clearReviewMessage() { _reviewMessage.value = null }
}
