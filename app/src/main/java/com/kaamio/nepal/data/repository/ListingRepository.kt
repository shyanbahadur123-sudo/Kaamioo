package com.kaamio.nepal.data.repository

import com.kaamio.nepal.data.JobListing
import com.kaamio.nepal.data.JobListingDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ListingRepository(
    private val jobListingDao: JobListingDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val externalScope: CoroutineScope,
    private val onConnectivityError: (Boolean) -> Unit = {}
) : IListingRepository {

    companion object {
        private const val PAGE_SIZE = 30L
    }

    override val allJobs: Flow<List<JobListing>> = jobListingDao.getAllJobs()
    private var listingsListener: ListenerRegistration? = null
    private var listingsLastDoc: com.google.firebase.firestore.DocumentSnapshot? = null
    private var isCleanedUp = false
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    init {
        val listener = FirebaseAuth.AuthStateListener {
            if (!isCleanedUp) startObservingFirestoreListings()
        }
        authStateListener = listener
        firebaseAuth.addAuthStateListener(listener)
        startObservingFirestoreListings()
    }

    private fun startObservingFirestoreListings() {
        listingsListener?.remove()
        // Do not subscribe while signed out: the query is permission-denied and the
        // error would flip the global offline flag. Re-subscribed on auth change.
        val uid = firebaseAuth.currentUser?.uid ?: return
        listingsListener = firestore.collection("listings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onConnectivityError(true)
                    return@addSnapshotListener
                }
                onConnectivityError(false)

                externalScope.launch(Dispatchers.IO) {
                    snapshots?.let { docs ->
                        listingsLastDoc = docs.documents.lastOrNull()
                        val localJobs = jobListingDao.getAllJobsSync()

                        val jobListings = docs.mapNotNull { doc ->
                            val data = doc.data
                            val existingLocal = localJobs.find { it.id == doc.id }
                            val remoteJob = JobListing.fromFirestoreSnapshot(doc.id, data)
                            remoteJob.copy(
                                isApplied = existingLocal?.isApplied ?: false,
                                isBookmarked = existingLocal?.isBookmarked ?: false
                            )
                        }
                        jobListingDao.insertJobs(jobListings)
                    }
                }
            }
    }

    override suspend fun loadMoreListings(): Boolean {
        val cursor = listingsLastDoc ?: return false
        val next = firestore.collection("listings")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .startAfter(cursor)
            .limit(PAGE_SIZE)
            .get().await()
        if (next.isEmpty) return false
        listingsLastDoc = next.documents.lastOrNull()
        val localJobs = jobListingDao.getAllJobsSync()
        val jobListings = next.mapNotNull { doc ->
            val existingLocal = localJobs.find { it.id == doc.id }
            val remoteJob = JobListing.fromFirestoreSnapshot(doc.id, doc.data)
            remoteJob.copy(
                isApplied = existingLocal?.isApplied ?: false,
                isBookmarked = existingLocal?.isBookmarked ?: false
            )
        }
        withContext(Dispatchers.IO) { jobListingDao.insertJobs(jobListings) }
        return true
    }

    override suspend fun postListing(job: JobListing) {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        val finalJob = job.copy(
            id = if (job.id.isBlank()) "jl_${System.currentTimeMillis()}_${(0..9999).random()}" else job.id,
            ownerId = uid,
            createdAt = System.currentTimeMillis()
        )
        
        // 1. Add to Firestore with an explicit doc id so the stored id always
        // matches the document id (no stale "id": "" field in the doc).
        firestore.collection("listings").document(finalJob.id).set(finalJob.toFirestoreMap()).await()
        
        // 2. Insert into Room
        withContext(Dispatchers.IO) {
            jobListingDao.insertJob(finalJob)
        }
    }

    override suspend fun applyToJob(jobId: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val localJob = jobListingDao.getJobById(jobId)
        val ownerId = localJob?.ownerId ?: runCatching {
            firestore.collection("listings").document(jobId).get().await()
                .getString("ownerId") ?: ""
        }.getOrDefault("")

        val application = hashMapOf(
            "jobId" to jobId,
            "applicantId" to uid,
            "ownerId" to ownerId,
            "appliedAt" to System.currentTimeMillis(),
            "status" to "pending"
        )

        // 1. Write to Firestore applications
        firestore.collection("applications").document("${jobId}_$uid").set(application).await()

        // 2. Update local Room
        if (localJob != null) {
            jobListingDao.updateJob(localJob.copy(isApplied = true))
        }
    }

    override suspend fun bookmarkJob(jobId: String, bookmarked: Boolean) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        
        // 1. Write to save_jobs Firestore
        val docId = "${jobId}_$uid"
        if (bookmarked) {
            val data = hashMapOf(
                "jobId" to jobId,
                "userId" to uid,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("save_jobs").document(docId).set(data).await()
        } else {
            firestore.collection("save_jobs").document(docId).delete().await()
        }
        
        // 2. Update local Room
        val localJob = jobListingDao.getJobById(jobId)
        if (localJob != null) {
            jobListingDao.updateJob(localJob.copy(isBookmarked = bookmarked))
        }
    }

    override suspend fun syncBookmarks() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val snapshots = firestore.collection("save_jobs").whereEqualTo("userId", uid).get().await()
        val bookmarkedIds = snapshots.mapNotNull { it.getString("jobId") }.toSet()
        
        val localJobs = jobListingDao.getAllJobsSync()
        withContext(Dispatchers.IO) {
            localJobs.forEach { job ->
                val isBookmarkedRemote = bookmarkedIds.contains(job.id)
                if (job.isBookmarked != isBookmarkedRemote) {
                    jobListingDao.updateJob(job.copy(isBookmarked = isBookmarkedRemote))
                }
            }
        }
    }

    override suspend fun syncApplications() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val snapshots = firestore.collection("applications").whereEqualTo("applicantId", uid).get().await()
        val appliedIds = snapshots.mapNotNull { it.getString("jobId") }.toSet()
        
        val localJobs = jobListingDao.getAllJobsSync()
        withContext(Dispatchers.IO) {
            localJobs.forEach { job ->
                val isAppliedRemote = appliedIds.contains(job.id)
                if (job.isApplied != isAppliedRemote) {
                    jobListingDao.updateJob(job.copy(isApplied = isAppliedRemote))
                }
            }
        }
    }

    override suspend fun refreshJobs() {
        try {
            val uid = firebaseAuth.currentUser?.uid ?: return
            val snapshots = firestore.collection("listings")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get().await()

            listingsLastDoc = snapshots.documents.lastOrNull()
            val localJobs = jobListingDao.getAllJobsSync()
            val jobListings = snapshots.mapNotNull { doc ->
                val existingLocal = localJobs.find { it.id == doc.id }
                val remoteJob = JobListing.fromFirestoreSnapshot(doc.id, doc.data)
                remoteJob.copy(
                    isApplied = existingLocal?.isApplied ?: false,
                    isBookmarked = existingLocal?.isBookmarked ?: false
                )
            }
            withContext(Dispatchers.IO) {
                jobListingDao.insertJobs(jobListings)
            }
        } catch (_: Exception) {}
    }

    override suspend fun updateLocalJob(job: JobListing) {
        jobListingDao.updateJob(job)
    }

    override fun cleanup() {
        isCleanedUp = true
        listingsListener?.remove()
        listingsListener = null
        authStateListener?.let { firebaseAuth.removeAuthStateListener(it) }
        authStateListener = null
    }
}
