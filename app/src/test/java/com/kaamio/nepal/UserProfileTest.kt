package com.kaamio.nepal

import com.kaamio.nepal.data.UserProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UserProfileTest {

    @Test
    fun defaultProfile_hasCorrectDefaults() {
        val profile = UserProfile()
        assertEquals(1, profile.id)
        assertEquals("", profile.name)
        assertEquals("English", profile.language)
        assertFalse(profile.isLoggedIn)
        assertFalse(profile.profileCompleted)
        assertEquals(0, profile.trustScore)
    }

    @Test
    fun profileWithLoggedInState_persistsCorrectly() {
        val profile = UserProfile(
            id = 1,
            name = "Ram Thapa",
            role = "worker",
            isLoggedIn = true,
            profileCompleted = true,
            trustScore = 65,
            language = "Nepali"
        )
        assertTrue(profile.isLoggedIn)
        assertTrue(profile.profileCompleted)
        assertEquals("Ram Thapa", profile.name)
        assertEquals("worker", profile.role)
        assertEquals(65, profile.trustScore)
        assertEquals("Nepali", profile.language)
    }

    @Test
    fun profileCopy_createsModifiedInstance() {
        val original = UserProfile(id = 1, name = "Sita", isLoggedIn = true)
        val modified = original.copy(name = "Sita Sharma", province = "Bagmati")

        assertEquals("Sita", original.name)
        assertEquals("Sita Sharma", modified.name)
        assertEquals("Bagmati", modified.province)
        assertTrue(modified.isLoggedIn)
        assertFalse(modified.profileCompleted)
    }
}
