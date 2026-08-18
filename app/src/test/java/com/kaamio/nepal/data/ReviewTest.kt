package com.kaamio.nepal.data

import com.kaamio.nepal.payment.EscrowStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewTest {

    @Test
    fun toFirestoreMap_containsAllFields() {
        val review = Review(
            id = "r1",
            reviewedUserId = "userA",
            reviewerId = "userB",
            reviewerName = "Ram",
            reviewerPhotoUrl = "https://example.com/photo.jpg",
            rating = 4,
            comment = "Great work",
            timestamp = 12345L
        )
        val map = review.toFirestoreMap()
        assertEquals("userA", map["reviewedUserId"])
        assertEquals("userB", map["reviewerId"])
        assertEquals("Ram", map["reviewerName"])
        assertEquals(4, map["rating"])
        assertEquals("Great work", map["comment"])
        assertEquals(12345L, map["timestamp"])
    }

    @Test
    fun fromDocument_parsesCorrectly() {
        val data = mapOf(
            "reviewedUserId" to "userA",
            "reviewerId" to "userB",
            "reviewerName" to "Sita",
            "rating" to 5L,
            "comment" to "Excellent",
            "timestamp" to 999L
        )
        val review = Review.fromDocument("r2", data)
        assertEquals("r2", review.id)
        assertEquals("userA", review.reviewedUserId)
        assertEquals("userB", review.reviewerId)
        assertEquals("Sita", review.reviewerName)
        assertEquals(5, review.rating)
        assertEquals("Excellent", review.comment)
        assertEquals(999L, review.timestamp)
    }

    @Test
    fun fromDocument_defaultsOnMissingFields() {
        val review = Review.fromDocument("r3", emptyMap())
        assertEquals("r3", review.id)
        assertEquals("", review.reviewedUserId)
        assertEquals(5, review.rating)
        assertTrue(review.timestamp > 0)
    }

    @Test
    fun fromDocument_handlesIntRating() {
        val data = mapOf("rating" to 3)
        val review = Review.fromDocument("r4", data)
        assertEquals(3, review.rating)
    }

    @Test
    fun escrowStatus_valuesAreComplete() {
        assertEquals(7, EscrowStatus.values().size)
        val expected = listOf(
            EscrowStatus.PENDING_FUNDING,
            EscrowStatus.FUNDED,
            EscrowStatus.IN_PROGRESS,
            EscrowStatus.COMPLETED,
            EscrowStatus.RELEASED,
            EscrowStatus.REFUNDED,
            EscrowStatus.DISPUTED
        )
        assertTrue(expected.containsAll(EscrowStatus.values().toList()))
    }

    @Test
    fun escrowStatus_parsesFromName() {
        assertEquals(EscrowStatus.FUNDED, EscrowStatus.valueOf("FUNDED"))
        assertEquals(EscrowStatus.RELEASED, EscrowStatus.valueOf("RELEASED"))
    }

    @Test
    fun unknownStatus_throws() {
        assertTrue(runCatching { EscrowStatus.valueOf("UNKNOWN") }.exceptionOrNull() != null)
    }
}
