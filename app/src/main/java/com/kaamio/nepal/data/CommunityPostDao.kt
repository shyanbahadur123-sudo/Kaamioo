package com.kaamio.nepal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CommunityPostDao {
    @Query("SELECT * FROM community_post ORDER BY timestamp DESC")
    fun getAllPosts(): Flow<List<CommunityPost>>

    @Query("SELECT * FROM community_post ORDER BY timestamp DESC")
    suspend fun getAllPostsSync(): List<CommunityPost>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<CommunityPost>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: CommunityPost)

    @Update
    suspend fun updatePost(post: CommunityPost)

    @Query("UPDATE community_post SET isLiked = :isLiked, likesCount = :likesCount WHERE id = :postId")
    suspend fun updatePostLike(postId: String, isLiked: Boolean, likesCount: Int)

    @Query("DELETE FROM community_post")
    suspend fun clearAll()
}
