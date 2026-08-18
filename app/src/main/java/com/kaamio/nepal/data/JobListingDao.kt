package com.kaamio.nepal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface JobListingDao {
    @Query("SELECT * FROM job_listing")
    fun getAllJobs(): Flow<List<JobListing>>

    @Query("SELECT * FROM job_listing")
    suspend fun getAllJobsSync(): List<JobListing>

    @Query("SELECT * FROM job_listing WHERE isApplied = 1 ORDER BY createdAt DESC")
    fun getAppliedJobs(): Flow<List<JobListing>>

    @Query("SELECT * FROM job_listing WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    fun getMyListings(ownerId: String): Flow<List<JobListing>>

    @Query("SELECT * FROM job_listing WHERE ownerId = :ownerId ORDER BY createdAt DESC")
    suspend fun getMyListingsSync(ownerId: String): List<JobListing>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJob(job: JobListing)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJobs(jobs: List<JobListing>)

    @Update
    suspend fun updateJob(job: JobListing)

    @Query("SELECT * FROM job_listing WHERE id = :id")
    suspend fun getJobById(id: String): JobListing?

    @Query("DELETE FROM job_listing")
    suspend fun clearAll()
}