package com.kaamio.nepal.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CourseTest {

    private fun baseCourse(id: String, price: String = "", modules: String = ""): Course =
        Course(
            id = id,
            title = "Test Course",
            instructor = "Instructor",
            duration = "4h",
            rating = 4.5f,
            studentsCount = "100",
            category = "Coding",
            thumbnailUrl = "",
            price = price,
            modules = modules
        )

    @Test
    fun freeCourse_isNotPremium() {
        assertFalse(baseCourse("c1").isPremium)
    }

    @Test
    fun zeroPrice_isNotPremium() {
        assertFalse(baseCourse("c2", price = "0").isPremium)
    }

    @Test
    fun freeLabel_isNotPremium() {
        assertFalse(baseCourse("c3", price = "Free").isPremium)
    }

    @Test
    fun pricedCourse_isPremium() {
        assertTrue(baseCourse("c4", price = "Rs. 499").isPremium)
    }

    @Test
    fun moduleList_splitsOnPipe() {
        val course = baseCourse("c5", modules = "Intro | Core Concepts | Final Project")
        assertEquals(listOf("Intro", "Core Concepts", "Final Project"), course.moduleList)
    }

    @Test
    fun moduleList_splitsOnNewlineAndFiltersBlank() {
        val course = baseCourse("c6", modules = "Intro\n\nPractice\n")
        assertEquals(listOf("Intro", "Practice"), course.moduleList)
    }

    @Test
    fun emptyModules_fallbackToEmptyList() {
        assertTrue(baseCourse("c7").moduleList.isEmpty())
    }
}
