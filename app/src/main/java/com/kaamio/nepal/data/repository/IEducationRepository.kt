package com.kaamio.nepal.data.repository

import com.kaamio.nepal.data.Course
import kotlinx.coroutines.flow.Flow

interface IEducationRepository {
    val allCourses: Flow<List<Course>>
    suspend fun createCourse(course: Course)
    suspend fun updateLocalCourse(course: Course)
    suspend fun getAllCoursesSync(): List<Course>
    suspend fun refreshCourses()
    suspend fun loadMoreCourses(): Boolean
    suspend fun unlockCourse(courseId: String, transactionId: String?): Boolean
    fun cleanup()
}
