package com.kaamio.nepal.data.repository

import com.kaamio.nepal.data.CommunityPost
import com.kaamio.nepal.data.CommunityPostDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CommunityRepository(
    private val communityPostDao: CommunityPostDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val externalScope: CoroutineScope,
    private val onConnectivityError: (Boolean) -> Unit = {}
) : ICommunityRepository {
    override val allPosts: Flow<List<CommunityPost>> = communityPostDao.getAllPosts()
    private var communityListener: ListenerRegistration? = null
    private var postsLastDoc: com.google.firebase.firestore.DocumentSnapshot? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    companion object {
        private const val PAGE_SIZE = 30L
    }

    init {
        val listener = FirebaseAuth.AuthStateListener {
            startObservingFirestorePosts()
        }
        authStateListener = listener
        firebaseAuth.addAuthStateListener(listener)
        startObservingFirestorePosts()
    }

    private fun startObservingFirestorePosts() {
        communityListener?.remove()
        // Do not subscribe while signed out: the query is permission-denied and the
        // error would flip the global offline flag. Re-subscribed on auth change.
        val uid = firebaseAuth.currentUser?.uid ?: return
        communityListener = firestore.collection("community")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(PAGE_SIZE)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onConnectivityError(true)
                    return@addSnapshotListener
                }
                onConnectivityError(false)

                externalScope.launch(Dispatchers.IO) {
                    snapshots?.let { docs ->
                        postsLastDoc = docs.documents.lastOrNull()
                        val existingLocal = communityPostDao.getAllPostsSync()
                        val posts = docs.mapNotNull { doc ->
                            val data = doc.data
                            val local = existingLocal.find { it.id == doc.id }
                            val remote = CommunityPost.fromDocument(doc.id, data)
                            remote.copy(isLiked = local?.isLiked ?: false)
                        }
                        communityPostDao.insertPosts(posts)
                    }
                }
            }
    }

    override suspend fun loadMorePosts(): Boolean {
        val cursor = postsLastDoc ?: return false
        val next = firestore.collection("community")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .startAfter(cursor)
            .limit(PAGE_SIZE)
            .get().await()
        if (next.isEmpty) return false
        postsLastDoc = next.documents.lastOrNull()
        val existingLocal = communityPostDao.getAllPostsSync()
        val posts = next.mapNotNull { doc ->
            val local = existingLocal.find { it.id == doc.id }
            val remote = CommunityPost.fromDocument(doc.id, doc.data)
            remote.copy(isLiked = local?.isLiked ?: false)
        }
        withContext(Dispatchers.IO) { communityPostDao.insertPosts(posts) }
        return true
    }

    override suspend fun refreshPosts() {
        startObservingFirestorePosts()
    }

    override suspend fun insertPost(post: CommunityPost) {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        val finalPost = post.copy(
            id = if (post.id.isBlank()) "cp_${System.currentTimeMillis()}_${(0..9999).random()}" else post.id,
            authorId = uid,
            timestamp = System.currentTimeMillis()
        )
        
        // 1. Add to Firestore with an explicit doc id so the stored id always
        // matches the document id (no stale "id": "" field in the doc).
        firestore.collection("community").document(finalPost.id).set(finalPost.toFirestoreMap()).await()
        
        // 2. Insert into Room
        withContext(Dispatchers.IO) {
            communityPostDao.insertPost(finalPost)
        }
    }

    override suspend fun likePost(postId: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val postRef = firestore.collection("community").document(postId)

        val result = firestore.runTransaction { transaction ->
            val snapshot = transaction.get(postRef)
            val likedBy = snapshot.get("likedBy") as? List<*> ?: emptyList<Any>()
            val alreadyLiked = likedBy.contains(uid)

            if (alreadyLiked) {
                transaction.update(postRef, "likesCount", FieldValue.increment(-1))
                transaction.update(postRef, "likedBy", FieldValue.arrayRemove(uid))
            } else {
                transaction.update(postRef, "likesCount", FieldValue.increment(1))
                transaction.update(postRef, "likedBy", FieldValue.arrayUnion(uid))
            }
            !alreadyLiked // new liked status
        }.await()

        // Sync to Room
        val snapshot = postRef.get().await()
        val newLikesCount = snapshot.getLong("likesCount")?.toInt() ?: 0
        withContext(Dispatchers.IO) {
            communityPostDao.updatePostLike(postId, result, newLikesCount)
        }
    }

    override suspend fun updatePost(post: CommunityPost) {
        communityPostDao.updatePost(post)
    }

    override fun cleanup() {
        communityListener?.remove()
        communityListener = null
        authStateListener?.let { firebaseAuth.removeAuthStateListener(it) }
        authStateListener = null
    }
}
