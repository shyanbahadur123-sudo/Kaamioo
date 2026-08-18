package com.kaamio.nepal.ui

import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.kaamio.nepal.data.CommunityPost
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.data.repository.ICommunityRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class CommunityViewModel @Inject constructor(
    private val communityRepository: ICommunityRepository,
    private val firebaseAuth: FirebaseAuth
) : BaseViewModel() {

    val communityPosts: StateFlow<List<CommunityPost>> = communityRepository.allPosts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun postToCommunity(content: String, profile: UserProfile) {
        if (content.isBlank()) return
        viewModelScope.launch {
            try {
                val post = CommunityPost(
                    id = "post_${UUID.randomUUID()}",
                    authorId = firebaseAuth.currentUser?.uid ?: profile.kaamioId,
                    authorName = profile.name.ifEmpty { "User" },
                    authorRole = profile.role.ifEmpty { "Specialist" },
                    authorAvatar = profile.photoUrl.ifEmpty { "" },
                    content = content.trim(),
                    timestamp = System.currentTimeMillis()
                )
                communityRepository.insertPost(post)
            } catch (_: Exception) {
                showSnackbar("Failed to share post. Check your connection.")
            }
        }
    }

    fun likePost(postId: String) {
        viewModelScope.launch {
            try {
                communityRepository.likePost(postId)
            } catch (_: Exception) {
                showSnackbar("Could not update like.")
            }
        }
    }
}