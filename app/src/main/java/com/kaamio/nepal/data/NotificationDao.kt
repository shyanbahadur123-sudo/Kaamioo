package com.kaamio.nepal.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notification_item ORDER BY timestamp DESC")
    fun getAllNotifications(): Flow<List<NotificationItem>>

    @Query("SELECT * FROM notification_item ORDER BY timestamp DESC")
    suspend fun getAllNotificationsSync(): List<NotificationItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotifications(items: List<NotificationItem>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(item: NotificationItem)

    @Query("UPDATE notification_item SET read = 1 WHERE id = :id")
    suspend fun markRead(id: String)

    @Query("DELETE FROM notification_item")
    suspend fun clearAll()
}
