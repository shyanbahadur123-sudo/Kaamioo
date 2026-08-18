package com.kaamio.nepal.data.repository

import android.util.Log
import android.util.Patterns
import com.kaamio.nepal.data.KaamioDatabase
import com.kaamio.nepal.data.Review
import com.kaamio.nepal.data.ReviewDao
import com.kaamio.nepal.data.UserProfile
import com.kaamio.nepal.data.UserProfileDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.userProfileChangeRequest
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicReference

class UserRepository(
    private val userProfileDao: UserProfileDao,
    private val reviewDao: ReviewDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseFunctions: FirebaseFunctions,
    private val firebaseStorage: FirebaseStorage,
    private val database: KaamioDatabase,
    private val externalScope: CoroutineScope,
    private val onConnectivityError: (Boolean) -> Unit = {}
) : IUserRepository {

    private fun sanitizeEmail(email: String): String = email.trim().lowercase()

    private fun validateEmail(email: String) {
        val normalized = sanitizeEmail(email)
        require(Patterns.EMAIL_ADDRESS.matcher(normalized).matches()) { "Please enter a valid email address" }
    }

    private fun validatePassword(pass: String) {
        require(pass.length >= 8) { "Password must be at least 8 characters" }
        require(pass.any { it.isLetter() }) { "Password must contain at least one letter" }
        require(pass.any { it.isDigit() }) { "Password must contain at least one number" }
    }

    private fun sanitizeDisplayName(name: String): String = name.trim().take(80)

    override val userProfile: Flow<UserProfile?> = userProfileDao.getUserProfile()

    private var profileListener: ListenerRegistration? = null
    private var authListener: FirebaseAuth.AuthStateListener? = null
    private var isCleanedUp = false
    private val profileCreateInFlight = AtomicReference<String?>(null)

    init {
        setupAuthListener()
    }

    // Email/password users must complete email verification before they are
    // treated as signed in. Google/phone providers are trusted immediately.
    private fun isEmailVerifiedOrNonEmail(user: FirebaseUser): Boolean {
        val providers = user.providerData.mapNotNull { it.providerId }
        return !(providers.contains("password") && !user.isEmailVerified)
    }

    private fun setupAuthListener() {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            if (isCleanedUp) return@AuthStateListener
            val user = auth.currentUser
            if (user != null && isEmailVerifiedOrNonEmail(user)) {
                startObservingFirestoreProfile(user.uid)
            } else {
                profileListener?.remove()
                profileListener = null
                externalScope.launch(Dispatchers.IO) {
                    userProfileDao.insertProfile(UserProfile(id = 1, isLoggedIn = false))
                }
            }
        }
        authListener = listener
        firebaseAuth.addAuthStateListener(listener)
    }

    private suspend fun markOnline() {
        try {
            firestore.collection("users")
                .document(firebaseAuth.currentUser?.uid ?: return)
                .set(mapOf("isOnline" to true), SetOptions.merge()).await()
        } catch (_: Exception) { }
    }

    private fun startObservingFirestoreProfile(uid: String) {
        profileListener?.remove()
        externalScope.launch { markOnline() }
        profileListener = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onConnectivityError(true)
                    return@addSnapshotListener
                }
                onConnectivityError(false)

                if (snapshot == null || !snapshot.exists()) {
                    val user = firebaseAuth.currentUser
                    if (user != null) {
                        externalScope.launch {
                            try { createDefaultProfile(user) }
                            catch (ex: Exception) { Log.e("KaamioLog", "Failed to create default profile", ex) }
                        }
                    }
                    return@addSnapshotListener
                }

                externalScope.launch(Dispatchers.Default) {
                    val data = snapshot.data ?: return@launch
                    val profile = UserProfile.fromDocument(snapshot.id, data)
                    withContext(Dispatchers.IO) {
                        userProfileDao.insertProfile(profile.copy(id = 1, isLoggedIn = true))
                    }
                }
            }
    }

    private suspend fun createDefaultProfile(user: FirebaseUser, name: String? = null, agreedToTerms: Boolean = true) {
        // De-duplicate concurrent creation (explicit call + AuthStateListener both fire).
        if (!profileCreateInFlight.compareAndSet(null, user.uid)) return
        try {
            doCreateDefaultProfile(user, name, agreedToTerms)
        } finally {
            profileCreateInFlight.set(null)
        }
    }

    private suspend fun doCreateDefaultProfile(user: FirebaseUser, name: String? = null, agreedToTerms: Boolean = true) {
        val uid = user.uid
        val displayName = name ?: user.displayName ?: ""
        val email = user.email ?: ""
        val phoneNumber = user.phoneNumber ?: ""
        val photoUrl = user.photoUrl?.toString() ?: ""
        
        val initialProfile = UserProfile(
            id = 1,
            name = displayName,
            email = email,
            phoneNumber = phoneNumber,
            photoUrl = photoUrl,
            isLoggedIn = true,
            privacyEnabled = agreedToTerms,
            createdAt = System.currentTimeMillis(),
            lastLogin = System.currentTimeMillis()
        )
        
        // Create in Firestore. "uid" must be present so the create rule
        // (request.resource.data.uid == uid) is satisfied for brand-new users.
        firestore.collection("users").document(uid)
            .set(initialProfile.toFirestoreMap() + mapOf("uid" to uid), SetOptions.merge()).await()
            
        // Cache in Room
        withContext(Dispatchers.IO) {
            userProfileDao.insertProfile(initialProfile)
        }
        
        // Call cloud function to ensure everything is set up on server side
        try {
            val data = hashMapOf(
                "displayName" to displayName,
                "email" to email,
                "phoneNumber" to phoneNumber,
                "photoURL" to photoUrl,
                "agreedToTerms" to agreedToTerms
            )
            firebaseFunctions
                .getHttpsCallable("ensureUserProfile")
                .call(data)
                .await()
        } catch (_: Exception) {}
    }

    private suspend fun updateLastLogin() {
        try {
            firebaseFunctions
                .getHttpsCallable("updateLastLogin")
                .call()
                .await()
        } catch (_: Exception) { }
        
        val uid = firebaseAuth.currentUser?.uid
        if (uid != null) {
            val now = System.currentTimeMillis()
            try {
                firestore.collection("users").document(uid).update("lastLogin", now).await()
            } catch (e: Exception) {
                // Firestore has no write-through rules for lastLogin on a brand-new
                // user doc; the cloud function already handled it. Non-fatal.
                Log.w("KaamioLog", "updateLastLogin Firestore write skipped", e)
            }
            val local = userProfileDao.getUserProfileSync()
            if (local != null) {
                userProfileDao.updateProfile(local.copy(lastLogin = now))
            }
        }
    }

    // --- Authentication Actions ---

    override suspend fun signInWithGoogle(credential: AuthCredential) {
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user ?: throw Exception("Google Sign-In failed: User is null")
        
        val doc = firestore.collection("users").document(user.uid).get().await()
        if (!doc.exists()) {
            createDefaultProfile(user)
        } else {
            val profile = UserProfile.fromDocument(user.uid, doc.data ?: emptyMap())
            withContext(Dispatchers.IO) {
                userProfileDao.insertProfile(profile.copy(id = 1, isLoggedIn = true))
            }
        }
        updateLastLogin()
    }

    override suspend fun signInWithEmail(email: String, pass: String) {
        val normalizedEmail = sanitizeEmail(email)
        validateEmail(normalizedEmail)
        require(pass.isNotEmpty()) { "Please enter your password" }
        val result = firebaseAuth.signInWithEmailAndPassword(normalizedEmail, pass).await()
        val user = result.user ?: throw Exception("Invalid email or password")
        if (!user.isEmailVerified) {
            // Re-send verification and block access to a hard-to-use unverified account.
            // Sign out so the AuthStateListener does not keep a logged-in Room profile.
            firebaseAuth.signOut()
            try { user.sendEmailVerification().await() } catch (_: Exception) {}
            throw Exception("Please verify your email first. A new verification link was sent to $normalizedEmail.")
        }
        markOnline()
        updateLastLogin()
        getCurrentUser() // Sync to Room
    }

    override suspend fun signUpWithEmail(email: String, pass: String, name: String, agreedToTerms: Boolean) {
        val normalizedEmail = sanitizeEmail(email)
        val safeDisplayName = sanitizeDisplayName(name)
        validateEmail(normalizedEmail)
        validatePassword(pass)
        require(safeDisplayName.length >= 2) { "Name must be at least 2 characters" }

        val result = firebaseAuth.createUserWithEmailAndPassword(normalizedEmail, pass).await()
        val user = result.user ?: throw Exception("Sign up failed")

        val profileUpdates = userProfileChangeRequest {
            displayName = safeDisplayName
        }
        user.updateProfile(profileUpdates).await()
        
        createDefaultProfile(user, safeDisplayName, agreedToTerms)
        
        try { user.sendEmailVerification().await() }
        catch (_: Exception) { }
    }

    override suspend fun sendPasswordReset(email: String) {
        val normalizedEmail = sanitizeEmail(email)
        validateEmail(normalizedEmail)
        firebaseAuth.sendPasswordResetEmail(normalizedEmail).await()
    }

    override suspend fun sendEmailVerification() {
        firebaseAuth.currentUser?.sendEmailVerification()?.await()
    }

    override suspend fun reloadAndCheckEmailVerified(): Boolean {
        val user = firebaseAuth.currentUser ?: return false
        user.reload().await()
        return user.isEmailVerified
    }

    override suspend fun signInWithPhoneCredential(credential: AuthCredential) {
        val result = firebaseAuth.signInWithCredential(credential).await()
        val user = result.user ?: throw Exception("Phone Sign-In failed")
        val doc = firestore.collection("users").document(user.uid).get().await()
        if (!doc.exists()) createDefaultProfile(user)
        else {
            updateLastLogin()
            getCurrentUser() // Sync to Room
        }
    }

    override fun verifyPhoneNumber(
        phoneNumber: String,
        activity: android.app.Activity,
        callbacks: com.google.firebase.auth.PhoneAuthProvider.OnVerificationStateChangedCallbacks,
        resendToken: com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken?
    ) {
        val optionsBuilder = com.google.firebase.auth.PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            
        if (resendToken != null) {
            optionsBuilder.setForceResendingToken(resendToken)
        }
        
        com.google.firebase.auth.PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    override suspend fun updateProfile(updates: Map<String, Any>) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        
        // Update Firestore
        firestore.collection("users").document(uid)
            .set(updates, SetOptions.merge()).await()
            
        // Trigger Trust Score recalculation after profile update
        try {
            firebaseFunctions.getHttpsCallable("calculateTrustScore").call().await()
        } catch (_: Exception) {}

        // Update Room
        getCurrentUser()
    }

    override suspend fun setFcmToken(token: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).update("fcmToken", token).await()
        val local = userProfileDao.getUserProfileSync()
        if (local != null) {
            userProfileDao.updateProfile(local.copy(fcmToken = token))
        }
    }

    override suspend fun syncFcmTokenIfNeeded() {
        try {
            val uid = firebaseAuth.currentUser?.uid ?: return
            val local = userProfileDao.getUserProfileSync() ?: return
            if (!local.fcmToken.isNullOrBlank()) return
            val token = com.google.firebase.messaging.FirebaseMessaging.getInstance().token.await()
            if (token.isNotBlank()) {
                setFcmToken(token)
                Log.d("KaamioLog", "FCM token synced for $uid")
            }
        } catch (e: Exception) {
            Log.w("KaamioLog", "FCM token sync failed", e)
        }
    }

    override suspend fun clearFcmToken() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        try {
            firestore.collection("users").document(uid).update("fcmToken", "")
        } catch (_: Exception) {}
        val local = userProfileDao.getUserProfileSync()
        if (local != null) {
            userProfileDao.updateProfile(local.copy(fcmToken = ""))
        }
    }

    override suspend fun getCurrentUser(): UserProfile? {
        val uid = firebaseAuth.currentUser?.uid ?: return null
        val doc = firestore.collection("users").document(uid).get().await()
        if (doc.exists()) {
            val profile = UserProfile.fromDocument(uid, doc.data ?: emptyMap())
            withContext(Dispatchers.IO) {
                userProfileDao.insertProfile(profile.copy(id = 1, isLoggedIn = true))
            }
            return profile
        }
        return null
    }

    override suspend fun getUserProfile(uid: String): UserProfile? {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            if (doc.exists()) {
                UserProfile.fromDocument(uid, doc.data ?: emptyMap())
            } else null
        } catch (_: Exception) { null }
    }

    override suspend fun getReviewsForUser(uid: String): List<Review> {
        return try {
            val snapshots = firestore.collection("reviews")
                .whereEqualTo("reviewedUserId", uid)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(100)
                .get().await()
            val reviews = snapshots.mapNotNull { doc -> Review.fromDocument(doc.id, doc.data) }
            // Cache offline so review history survives a connection loss.
            withContext(Dispatchers.IO) { reviewDao.insertReviews(reviews) }
            reviews
        } catch (_: Exception) {
            // Fall back to the locally cached copy when offline.
            reviewDao.getReviewsForUserSync(uid)
        }
    }

    override suspend fun getMyReviews(): List<Review> {
        val uid = firebaseAuth.currentUser?.uid ?: return emptyList()
        return getReviewsForUser(uid)
    }

    override suspend fun submitReview(reviewedUserId: String, rating: Int, comment: String) {
        val reviewerId = firebaseAuth.currentUser?.uid ?: return
        require(rating in 1..5) { "Rating must be between 1 and 5" }
        val safeComment = comment.trim().take(500)

        val review = Review(
            id = "review_${System.currentTimeMillis()}",
            reviewedUserId = reviewedUserId,
            reviewerId = reviewerId,
            reviewerName = firebaseAuth.currentUser?.displayName ?: "",
            reviewerPhotoUrl = firebaseAuth.currentUser?.photoUrl?.toString() ?: "",
            rating = rating,
            comment = safeComment
        )

        firestore.collection("reviews").document(review.id).set(review.toFirestoreMap()).await()

        // Aggregate rating is recomputed server-side by the onReviewWritten
        // Cloud Function. Clients can no longer write averageRating/totalReviews
        // (removed from the users update whitelist for anti-forgery).
        try {
            firebaseFunctions.getHttpsCallable("recomputeTrustScore")
                .call(mapOf("targetUid" to reviewedUserId))
                .await()
        } catch (_: Exception) {}
    }

    override suspend fun requestIdentityVerification() {
        val uid = firebaseAuth.currentUser?.uid ?: return
        // Real KYC: record the submission as pending and hand off to the
        // verified cloud function; the client can never self-verify.
        firestore.collection("kyc").document(uid).set(
            mapOf(
                "uid" to uid,
                "status" to "pending",
                "requestedAt" to System.currentTimeMillis()
            )
        ).await()
        try {
            firebaseFunctions.getHttpsCallable("recomputeTrustScore").call().await()
        } catch (_: Exception) {}
        getCurrentUser()
    }

    override suspend fun submitKycVerification(
        documentUri: android.net.Uri?,
        selfieUri: android.net.Uri?,
        fullName: String,
        address: String,
        idType: String,
        idNumber: String
    ) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val uploads = mutableMapOf<String, String>()
        if (documentUri != null) {
            val ref = firebaseStorage.reference.child("kyc/${uid}/document_${System.currentTimeMillis()}.jpg")
            ref.putFile(documentUri).await()
            uploads["documentUrl"] = ref.downloadUrl.await().toString()
        }
        if (selfieUri != null) {
            val ref = firebaseStorage.reference.child("kyc/${uid}/selfie_${System.currentTimeMillis()}.jpg")
            ref.putFile(selfieUri).await()
            uploads["selfieUrl"] = ref.downloadUrl.await().toString()
        }

        firestore.collection("kyc").document(uid).set(
            mapOf(
                "uid" to uid,
                "fullName" to fullName.take(120),
                "address" to address.take(160),
                "idType" to idType.take(40),
                "idNumber" to idNumber.take(40),
                "status" to "pending",
                "submittedAt" to System.currentTimeMillis()
            ) + uploads
        ).await()
        getCurrentUser()
    }

    override suspend fun getTrustScoreHistory(): List<Pair<Long, Int>> {
        val uid = firebaseAuth.currentUser?.uid ?: return emptyList()
        return try {
            val snapshots = firestore.collection("trust_scores").document(uid)
                .collection("history")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get().await()
            snapshots.map { doc ->
                (doc.getLong("timestamp") ?: 0L) to (doc.getLong("score")?.toInt() ?: 0)
            }
        } catch (_: Exception) { emptyList() }
    }

    override suspend fun logout() {
        val uid = firebaseAuth.currentUser?.uid
        // Stop receiving push notifications for the old account on shared devices.
        try { clearFcmToken() } catch (_: Exception) {}
        // Perform local sign out immediately to ensure responsiveness
        firebaseAuth.signOut()
        
        withContext(Dispatchers.IO) {
            // Wipe all cached data so a previously signed-in user's listings,
            // chats, posts, courses, and reviews never leak to the next user.
            try { KaamioDatabase.clearAllData(database) } catch(_: Exception) {}
            
            val local = userProfileDao.getUserProfileSync()
            if (local != null) {
                userProfileDao.updateProfile(local.copy(isLoggedIn = false, isOnline = false))
            } else {
                userProfileDao.insertProfile(UserProfile(id = 1, isLoggedIn = false))
            }
        }

        // Try to update Firestore status in background, don't block if database is missing/offline
        if (uid != null) {
            externalScope.launch {
                try {
                    firestore.collection("users").document(uid)
                        .update("isOnline", false).await()
                } catch (_: Exception) { }
            }
        }
    }

    override fun cleanup() {
        isCleanedUp = true
        profileListener?.remove()
        profileListener = null
        authListener?.let { firebaseAuth.removeAuthStateListener(it) }
        authListener = null
    }
}
