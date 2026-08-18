package com.kaamio.nepal.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.room.withTransaction
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserProfile::class, JobListing::class, Course::class, ChatMessage::class, CommunityPost::class, Review::class, NotificationItem::class],
    version = 13,
    exportSchema = true
)
abstract class KaamioDatabase : RoomDatabase() {
    abstract fun userProfileDao(): UserProfileDao
    abstract fun jobListingDao(): JobListingDao
    abstract fun courseDao(): CourseDao
    abstract fun chatDao(): ChatDao
    abstract fun communityPostDao(): CommunityPostDao
    abstract fun reviewDao(): ReviewDao
    abstract fun notificationDao(): NotificationDao

    companion object {
        @Volatile
        private var INSTANCE: KaamioDatabase? = null

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN kaamioId TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS community_post (id TEXT NOT NULL PRIMARY KEY, authorId TEXT NOT NULL DEFAULT '', authorName TEXT NOT NULL, authorRole TEXT NOT NULL, authorAvatar TEXT NOT NULL, content TEXT NOT NULL, timestamp INTEGER NOT NULL, likesCount INTEGER NOT NULL DEFAULT 0, commentsCount INTEGER NOT NULL DEFAULT 0, isLiked INTEGER NOT NULL DEFAULT 0)")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN notificationsEnabled INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN privacyEnabled INTEGER NOT NULL DEFAULT 1")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN trustScore INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN isOnline INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN lastLogin INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN isProposal INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN proposalRate TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN proposalDuration TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN proposalStatus TEXT NOT NULL DEFAULT 'PENDING'")
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN gender TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN dateOfBirth TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE job_listing ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_listing ADD COLUMN createdAt INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE job_listing ADD COLUMN budget TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE job_listing ADD COLUMN deadlineDays INTEGER NOT NULL DEFAULT 14")
                db.execSQL("ALTER TABLE job_listing ADD COLUMN milestonesCount INTEGER NOT NULL DEFAULT 4")
                db.execSQL("ALTER TABLE job_listing ADD COLUMN clientRating REAL NOT NULL DEFAULT 4.9")
                db.execSQL("ALTER TABLE job_listing ADD COLUMN preferredSkills TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN fcmToken TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN isPhoneVerified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN isGoogleVerified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN isIdentityVerified INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE course ADD COLUMN modules TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE course ADD COLUMN instructorId TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE course ADD COLUMN price TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN chartType TEXT")
                db.execSQL("ALTER TABLE chat_message ADD COLUMN chartData TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN averageRating REAL NOT NULL DEFAULT 0.0")
                db.execSQL("ALTER TABLE user_profile ADD COLUMN totalReviews INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE chat_message ADD COLUMN imageUrl TEXT")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `review` (`id` TEXT NOT NULL, `reviewedUserId` TEXT NOT NULL, `reviewerId` TEXT NOT NULL, `reviewerName` TEXT NOT NULL, `reviewerPhotoUrl` TEXT NOT NULL, `rating` INTEGER NOT NULL, `comment` TEXT NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `notification_item` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `screen` TEXT NOT NULL, `read` INTEGER NOT NULL, `timestamp` INTEGER NOT NULL, PRIMARY KEY(`id`))"
                )
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_profile ADD COLUMN kycStatus TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE course ADD COLUMN isUnlocked INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE course ADD COLUMN unlockedBy TEXT NOT NULL DEFAULT ''")
            }
        }

        fun getDatabase(context: Context): KaamioDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KaamioDatabase::class.java,
                    "kaamio_database_v13"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                .build()
                INSTANCE = instance
                instance
            }
        }

        suspend fun clearAllData(db: KaamioDatabase) {
            db.withTransaction {
                val local = db.userProfileDao().getUserProfileSync()
                db.userProfileDao().insertProfile(
                    local?.copy(isLoggedIn = false, isOnline = false) ?: UserProfile(isLoggedIn = false)
                )
                db.jobListingDao().clearAll()
                db.courseDao().clearAll()
                db.chatDao().clearAll()
                db.communityPostDao().clearAll()
                db.reviewDao().clearAll()
                db.notificationDao().clearAll()
            }
        }

        suspend fun clearCacheData(db: KaamioDatabase) {
            db.withTransaction {
                db.jobListingDao().clearAll()
                db.courseDao().clearAll()
                db.chatDao().clearAll()
                db.communityPostDao().clearAll()
                db.reviewDao().clearAll()
                db.notificationDao().clearAll()
            }
        }

        suspend fun prepopulateData(db: KaamioDatabase) {
            val existingProfile = db.userProfileDao().getUserProfileSync()
            if (existingProfile == null) {
                db.userProfileDao().insertProfile(UserProfile())
            }
        }
    }
}