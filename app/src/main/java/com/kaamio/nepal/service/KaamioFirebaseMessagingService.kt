package com.kaamio.nepal.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.kaamio.nepal.MainActivity

class KaamioFirebaseMessagingService : FirebaseMessagingService() {

    @Suppress("DEPRECATION")
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "New FCM token: $token")
        sendTokenToFirestore(token)
    }

    private fun sendTokenToFirestore(token: String) {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(user.uid)
                .update("fcmToken", token)
                .addOnSuccessListener { Log.d(TAG, "FCM token synced to Firestore") }
                .addOnFailureListener { e -> Log.w(TAG, "Failed to sync FCM token", e) }
        } catch (e: Exception) {
            Log.w(TAG, "Error syncing FCM token", e)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        val title = message.notification?.title
            ?: message.data["title"]
            ?: getString(com.kaamio.nepal.R.string.app_name)

        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        val channelId = message.data["channel"] ?: DEFAULT_CHANNEL_ID

        persistNotification(title, body, message.data)
        showNotification(title, body, channelId, message.data)
    }

    private fun persistNotification(title: String, body: String, data: Map<String, String>) {
        try {
            val user = FirebaseAuth.getInstance().currentUser ?: return
            val docId = "notif_${System.currentTimeMillis()}_${(1000..9999).random()}"
            val notificationMap = hashMapOf<String, Any>(
                "recipientId" to user.uid,
                "title" to title,
                "body" to body,
                "screen" to (data["screen"] ?: "home"),
                "read" to false,
                "timestamp" to System.currentTimeMillis()
            )
            FirebaseFirestore.getInstance()
                .collection("notifications")
                .document(docId)
                .set(notificationMap)
                .addOnFailureListener { e -> Log.w(TAG, "Failed to persist notification", e) }
        } catch (e: Exception) {
            Log.w(TAG, "Error persisting notification", e)
        }
    }

    fun getFcmTokenAndSync() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token -> sendTokenToFirestore(token) }
                .addOnFailureListener { e -> Log.w(TAG, "Failed to get FCM token", e) }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting FCM token", e)
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        channelId: String,
        data: Map<String, String>
    ) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
            if (data["screen"].isNullOrBlank()) {
                putExtra("screen", "home")
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        createNotificationChannel(channelId)

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(com.kaamio.nepal.R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            NotificationManagerCompat.from(this).notify(System.currentTimeMillis().toInt(), notification)
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted", e)
        }
    }

    private fun createNotificationChannel(channelId: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = when (channelId) {
                CHANNEL_MESSAGES -> "Messages"
                CHANNEL_JOBS -> "Job Alerts"
                CHANNEL_PAYMENTS -> "Payment Updates"
                CHANNEL_COMMUNITY -> "Community"
                else -> "General"
            }
            val descriptionText = when (channelId) {
                CHANNEL_MESSAGES -> "Chat messages and proposal updates"
                CHANNEL_JOBS -> "New job listings and application status"
                CHANNEL_PAYMENTS -> "Payment confirmations and escrow updates"
                CHANNEL_COMMUNITY -> "Community activity and course updates"
                else -> "General notifications"
            }
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        private const val TAG = "KaamioFCM"
        private const val DEFAULT_CHANNEL_ID = "kaamio_general"
        const val CHANNEL_MESSAGES = "kaamio_messages"
        const val CHANNEL_JOBS = "kaamio_jobs"
        const val CHANNEL_PAYMENTS = "kaamio_payments"
        const val CHANNEL_COMMUNITY = "kaamio_community"
    }
}
