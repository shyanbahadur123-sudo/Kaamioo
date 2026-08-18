package com.kaamio.nepal.di

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings
import com.google.firebase.functions.FirebaseFunctions
import com.kaamio.nepal.data.*
import com.kaamio.nepal.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModules {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore {
        val firestore = FirebaseFirestore.getInstance()
        firestore.firestoreSettings = FirebaseFirestoreSettings.Builder()
            .setLocalCacheSettings(PersistentCacheSettings.newBuilder()
                .setSizeBytes(100 * 1024 * 1024) // 100MB
                .build())
            .build()
        return firestore
    }

    @Provides
    @Singleton
    fun provideFirebaseFunctions(): FirebaseFunctions = FirebaseFunctions.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseStorage(): com.google.firebase.storage.FirebaseStorage =
        com.google.firebase.storage.FirebaseStorage.getInstance()

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KaamioDatabase =
        KaamioDatabase.getDatabase(context)

    @Provides
    fun provideUserProfileDao(db: KaamioDatabase): UserProfileDao = db.userProfileDao()

    @Provides
    fun provideJobListingDao(db: KaamioDatabase): JobListingDao = db.jobListingDao()

    @Provides
    fun provideCourseDao(db: KaamioDatabase): CourseDao = db.courseDao()

    @Provides
    fun provideChatDao(db: KaamioDatabase): ChatDao = db.chatDao()

    @Provides
    fun provideCommunityPostDao(db: KaamioDatabase): CommunityPostDao = db.communityPostDao()

    @Provides
    fun provideReviewDao(db: KaamioDatabase): ReviewDao = db.reviewDao()

    @Provides
    fun provideNotificationDao(db: KaamioDatabase): NotificationDao = db.notificationDao()

    @Provides
    @IoDispatcher
    fun provideIoDispatcher() = Dispatchers.IO

    @Provides
    @DefaultDispatcher
    fun provideDefaultDispatcher() = Dispatchers.Default

    @Provides
    @MainDispatcher
    fun provideMainDispatcher() = Dispatchers.Main

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(@IoDispatcher ioDispatcher: kotlinx.coroutines.CoroutineDispatcher): CoroutineScope =
        CoroutineScope(SupervisorJob() + ioDispatcher)

    @Provides
    @Singleton
    fun provideConnectivityObserver(@ApplicationContext context: Context): ConnectivityObserver = ConnectivityObserver(context)

    @Provides
    @Singleton
    fun provideUserRepository(
        userProfileDao: UserProfileDao,
        reviewDao: ReviewDao,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        firebaseFunctions: FirebaseFunctions,
        firebaseStorage: com.google.firebase.storage.FirebaseStorage,
        database: KaamioDatabase,
        @ApplicationScope externalScope: CoroutineScope,
        connectivityObserver: ConnectivityObserver
    ): IUserRepository = UserRepository(
        userProfileDao = userProfileDao,
        reviewDao = reviewDao,
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        firebaseFunctions = firebaseFunctions,
        firebaseStorage = firebaseStorage,
        database = database,
        externalScope = externalScope,
        onConnectivityError = connectivityObserver.onConnectivityChange
    )

    @Provides
    @Singleton
    fun provideListingRepository(
        jobListingDao: JobListingDao,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        @ApplicationScope externalScope: CoroutineScope,
        connectivityObserver: ConnectivityObserver
    ): IListingRepository = ListingRepository(
        jobListingDao = jobListingDao,
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        externalScope = externalScope,
        onConnectivityError = connectivityObserver.onConnectivityChange
    )

    @Provides
    @Singleton
    fun provideEducationRepository(
        courseDao: CourseDao,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        firebaseFunctions: FirebaseFunctions,
        @ApplicationScope externalScope: CoroutineScope,
        connectivityObserver: ConnectivityObserver
    ): IEducationRepository = EducationRepository(
        courseDao = courseDao,
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        firebaseFunctions = firebaseFunctions,
        externalScope = externalScope,
        onConnectivityError = connectivityObserver.onConnectivityChange
    )

    @Provides
    @Singleton
    fun provideCommunityRepository(
        communityPostDao: CommunityPostDao,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        @ApplicationScope externalScope: CoroutineScope,
        connectivityObserver: ConnectivityObserver
    ): ICommunityRepository = CommunityRepository(
        communityPostDao = communityPostDao,
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        externalScope = externalScope,
        onConnectivityError = connectivityObserver.onConnectivityChange
    )

    @Provides
    @Singleton
    fun provideKhaltiPaymentGateway(firebaseFunctions: FirebaseFunctions): com.kaamio.nepal.payment.KhaltiPaymentGateway =
        com.kaamio.nepal.payment.KhaltiPaymentGateway(firebaseFunctions)

    @Provides
    @Singleton
    fun provideESewaPaymentGateway(firebaseFunctions: FirebaseFunctions): com.kaamio.nepal.payment.ESewaPaymentGateway =
        com.kaamio.nepal.payment.ESewaPaymentGateway(firebaseFunctions)

    @Provides
    @Singleton
    fun provideEscrowService(firestore: FirebaseFirestore, firebaseFunctions: FirebaseFunctions): com.kaamio.nepal.payment.EscrowService =
        com.kaamio.nepal.payment.FirestoreEscrowService(firestore, firebaseFunctions)

    @Provides
    @Singleton
    fun provideChatRepository(
        chatDao: ChatDao,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore,
        firebaseFunctions: FirebaseFunctions,
        firebaseStorage: com.google.firebase.storage.FirebaseStorage,
        @ApplicationScope externalScope: CoroutineScope,
        connectivityObserver: ConnectivityObserver
    ): IChatRepository = ChatRepository(
        chatDao = chatDao,
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        firebaseFunctions = firebaseFunctions,
        firebaseStorage = firebaseStorage,
        externalScope = externalScope,
        onConnectivityError = connectivityObserver.onConnectivityChange
    )

    @Provides
    @Singleton
    fun provideNotificationRepository(
        notificationDao: NotificationDao,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): INotificationRepository = NotificationRepository(
        notificationDao = notificationDao,
        firebaseAuth = firebaseAuth,
        firestore = firestore
    )
}