package com.kaamio.nepal.data.repository

import com.kaamio.nepal.data.JobListing
import kotlinx.coroutines.flow.Flow

interface IListingRepository {
    val allJobs: Flow<List<JobListing>>
    suspend fun postListing(job: JobListing)
    suspend fun applyToJob(jobId: String)
    suspend fun bookmarkJob(jobId: String, bookmarked: Boolean)
    suspend fun syncBookmarks()
    suspend fun syncApplications()
    suspend fun refreshJobs()
    suspend fun loadMoreListings(): Boolean
    suspend fun updateLocalJob(job: JobListing)
    fun cleanup()
}
