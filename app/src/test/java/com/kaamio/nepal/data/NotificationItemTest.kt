package com.kaamio.nepal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationItemTest {

    @Test
    fun fromDocument_parsesCorrectly() {
        val data = mapOf(
            "title" to "New Job Match",
            "body" to "A new plumbing job is available",
            "screen" to "market",
            "read" to false,
            "timestamp" to 12345L
        )
        val item = NotificationItem.fromDocument("n1", data)
        assertEquals("n1", item.id)
        assertEquals("New Job Match", item.title)
        assertEquals("market", item.screen)
        assertFalse(item.read)
        assertEquals(12345L, item.timestamp)
    }

    @Test
    fun fromDocument_defaultsOnMissingFields() {
        val item = NotificationItem.fromDocument("n2", emptyMap())
        assertEquals("n2", item.id)
        assertEquals("", item.title)
        assertEquals("home", item.screen)
        assertFalse(item.read)
        assertTrue(item.timestamp > 0)
    }

    @Test
    fun toFirestoreMap_containsExpectedKeys() {
        val item = NotificationItem(
            id = "n3",
            title = "Payment Released",
            body = "Funds released",
            screen = "chat",
            read = true,
            timestamp = 1L
        )
        val map = item.toFirestoreMap()
        assertEquals("Payment Released", map["title"])
        assertEquals("chat", map["screen"])
        assertEquals(true, map["read"])
        assertEquals(1L, map["timestamp"])
    }
}
