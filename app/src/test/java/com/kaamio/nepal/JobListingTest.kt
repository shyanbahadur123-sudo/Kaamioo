package com.kaamio.nepal

import com.kaamio.nepal.data.JobListing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class JobListingTest {

    @Test
    fun defaultJob_hasCorrectDefaults() {
        val job = JobListing(
            id = "test1",
            title = "Test Job",
            company = "Test Co",
            logoUrl = "",
            salary = "NPR 1000",
            location = "Kathmandu",
            isRemote = false,
            type = "Full-time",
            category = "Tech"
        )
        assertEquals("test1", job.id)
        assertFalse(job.isApplied)
        assertFalse(job.isBookmarked)
        assertFalse(job.isVerifiedCompany)
    }

    @Test
    fun jobFiltering_remoteCheck() {
        val remoteJob = JobListing(
            id = "r1", title = "Remote Dev", company = "Co", logoUrl = "",
            salary = "", location = "Remote", isRemote = true, type = "Freelance", category = "Tech"
        )
        val onsiteJob = JobListing(
            id = "o1", title = "Office Dev", company = "Co", logoUrl = "",
            salary = "", location = "Kathmandu", isRemote = false, type = "Full-time", category = "Tech"
        )
        assertTrue(remoteJob.isRemote)
        assertFalse(onsiteJob.isRemote)
    }
}
