package com.kaamio.nepal.data.repository

import com.kaamio.nepal.data.CommunityPost
import kotlinx.coroutines.flow.Flow

interface ICommunityRepository {
    val allPosts: Flow<List<CommunityPost>>
    suspend fun refreshPosts()
    suspend fun loadMorePosts(): Boolean
    suspend fun insertPost(post: CommunityPost)
    suspend fun likePost(postId: String)
    suspend fun updatePost(post: CommunityPost)
    fun cleanup()
}