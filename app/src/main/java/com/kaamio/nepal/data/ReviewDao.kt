package com.kaamio.nepal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ReviewDao {
    @Query("SELECT * FROM review WHERE reviewedUserId = :userId ORDER BY timestamp DESC")
    fun getReviewsForUser(userId: String): Flow<List<Review>>

    @Query("SELECT * FROM review WHERE reviewedUserId = :userId ORDER BY timestamp DESC")
    suspend fun getReviewsForUserSync(userId: String): List<Review>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReviews(reviews: List<Review>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: Review)

    @Query("DELETE FROM review")
    suspend fun clearAll()
}
