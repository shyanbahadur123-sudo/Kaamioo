package com.kaamio.nepal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM course")
    fun getAllCourses(): Flow<List<Course>>

    @Query("SELECT * FROM course")
    suspend fun getAllCoursesSync(): List<Course>

    @Query("SELECT * FROM course WHERE id = :id")
    suspend fun getCourseById(id: String): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourses(courses: List<Course>)

    @Update
    suspend fun updateCourse(course: Course)

    @Query("DELETE FROM course")
    suspend fun clearAll()
}
