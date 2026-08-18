package com.kaamio.nepal.data.repository

import com.kaamio.nepal.data.Course
import com.kaamio.nepal.data.CourseDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class EducationRepository(
    private val courseDao: CourseDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseFunctions: FirebaseFunctions,
    private val externalScope: CoroutineScope,
    private val onConnectivityError: (Boolean) -> Unit = {}
) : IEducationRepository {
    override val allCourses: Flow<List<Course>> = courseDao.getAllCourses()
    private var coursesListener: ListenerRegistration? = null
    private var coursesLastDoc: com.google.firebase.firestore.DocumentSnapshot? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    companion object {
        private const val PAGE_SIZE = 30L
    }

    private fun applyUnlockState(courses: List<Course>): List<Course> {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        return courses.map { course ->
            val unlockedBy = course.unlockedBy.split(',').map { it.trim() }.filter { it.isNotBlank() }
            course.copy(isUnlocked = unlockedBy.contains(uid) || course.isUnlocked)
        }
    }

    init {
        val listener = FirebaseAuth.AuthStateListener {
            startObservingFirestoreCourses()
        }
        authStateListener = listener
        firebaseAuth.addAuthStateListener(listener)
        startObservingFirestoreCourses()
    }

    private fun startObservingFirestoreCourses() {
        coursesListener?.remove()
        // Do not subscribe while signed out: the query is permission-denied and the
        // error would flip the global offline flag. Re-subscribed on auth change.
        val uid = firebaseAuth.currentUser?.uid ?: return
        coursesListener = firestore.collection("courses")
            .orderBy("rating", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onConnectivityError(true)
                    return@addSnapshotListener
                }
                onConnectivityError(false)
                
                externalScope.launch(Dispatchers.IO) {
                    snapshots?.let { docs ->
                        coursesLastDoc = docs.documents.lastOrNull()
                        val localCourses = courseDao.getAllCoursesSync()
                        val courses = docs.mapNotNull { doc ->
                            val local = localCourses.find { it.id == doc.id }
                            val remote = Course.fromDocument(doc.id, doc.data)
                            remote.copy(isBookmarked = local?.isBookmarked ?: false)
                        }
                        courseDao.insertCourses(applyUnlockState(courses))
                    }
                }
            }
    }

    override suspend fun loadMoreCourses(): Boolean {
        val cursor = coursesLastDoc ?: return false
        val next = firestore.collection("courses")
            .orderBy("rating", Query.Direction.DESCENDING)
            .startAfter(cursor)
            .limit(PAGE_SIZE)
            .get().await()
        if (next.isEmpty) return false
        coursesLastDoc = next.documents.lastOrNull()
        val localCourses = courseDao.getAllCoursesSync()
        val courses = next.mapNotNull { doc ->
            val local = localCourses.find { it.id == doc.id }
            val remote = Course.fromDocument(doc.id, doc.data)
            remote.copy(isBookmarked = local?.isBookmarked ?: false)
        }
        withContext(Dispatchers.IO) { courseDao.insertCourses(applyUnlockState(courses)) }
        return true
    }

    override suspend fun unlockCourse(courseId: String, transactionId: String?): Boolean = runCatching {
        firebaseFunctions
            .getHttpsCallable("course-unlock")
            .call(hashMapOf("courseId" to courseId, "transactionId" to transactionId))
            .await()
        val course = courseDao.getCourseById(courseId)
        if (course != null) {
            courseDao.updateCourse(course.copy(isUnlocked = true))
        }
        true
    }.getOrDefault(false)

    override suspend fun createCourse(course: Course) {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        val finalCourse = course.copy(
            id = if (course.id.isBlank()) "c_${System.currentTimeMillis()}" else course.id,
            instructorId = uid
        )
        
        // 1. Add to Firestore (explicit doc id so the stored id matches the doc id)
        firestore.collection("courses").document(finalCourse.id).set(finalCourse.toFirestoreMap()).await()
        
        // 2. Insert into Room
        withContext(Dispatchers.IO) {
            courseDao.insertCourse(finalCourse)
        }
    }

    override suspend fun updateLocalCourse(course: Course) {
        courseDao.updateCourse(course)
    }

    override suspend fun refreshCourses() {
        try {
            val snapshots = firestore.collection("courses")
                .orderBy("rating", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)
                .get().await()

            coursesLastDoc = snapshots.documents.lastOrNull()
            val localCourses = courseDao.getAllCoursesSync()
            val courses = snapshots.mapNotNull { doc ->
                val local = localCourses.find { it.id == doc.id }
                val remote = Course.fromDocument(doc.id, doc.data)
                remote.copy(isBookmarked = local?.isBookmarked ?: false)
            }
            withContext(Dispatchers.IO) {
                courseDao.insertCourses(applyUnlockState(courses))
            }
        } catch (_: Exception) {}
    }

    override suspend fun getAllCoursesSync(): List<Course> {
        return courseDao.getAllCoursesSync()
    }

    override fun cleanup() {
        coursesListener?.remove()
        coursesListener = null
        authStateListener?.let { firebaseAuth.removeAuthStateListener(it) }
        authStateListener = null
    }
}
