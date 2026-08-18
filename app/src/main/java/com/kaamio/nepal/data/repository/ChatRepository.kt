package com.kaamio.nepal.data.repository

import com.kaamio.nepal.data.ChatMessage
import com.kaamio.nepal.data.ChatDao
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class ChatRepository(
    private val chatDao: ChatDao,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseFunctions: FirebaseFunctions,
    private val firebaseStorage: FirebaseStorage,
    private val externalScope: CoroutineScope,
    private val onConnectivityError: (Boolean) -> Unit = {}
) : IChatRepository {
    override val allMessages: Flow<List<ChatMessage>> = chatDao.getAllMessages()
    private var chatsListener: ListenerRegistration? = null
    private var authStateListener: FirebaseAuth.AuthStateListener? = null

    override fun getMessagesForPartner(partnerId: String): Flow<List<ChatMessage>> {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        return chatDao.getMessagesForPartner(partnerId).map { messages ->
            messages.filter { it.senderId == uid || it.partnerId == uid }
        }
    }

    override fun getCurrentUserMessages(): Flow<List<ChatMessage>> {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        return chatDao.getAllMessages().map { messages ->
            messages.filter { it.senderId == uid || it.partnerId == uid }
        }
    }

    init {
        val listener = FirebaseAuth.AuthStateListener {
            startObservingFirestoreMessages()
        }
        authStateListener = listener
        firebaseAuth.addAuthStateListener(listener)
        startObservingFirestoreMessages()
    }

    private fun startObservingFirestoreMessages() {
        chatsListener?.remove()
        val uid = firebaseAuth.currentUser?.uid ?: return
        
        chatsListener = firestore.collection("chats")
            .whereArrayContains("participantIds", uid)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(500)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    onConnectivityError(true)
                    return@addSnapshotListener
                }
                onConnectivityError(false)

                externalScope.launch(Dispatchers.IO) {
                    snapshots?.let { docs ->
                        val messages = docs.mapNotNull { doc ->
                            ChatMessage.fromDocument(doc.id, doc.data)
                        }
                        chatDao.insertMessages(messages)
                    }
                }
            }
    }

    override suspend fun updateMessageProposalStatus(messageId: String, status: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val result = firebaseFunctions
            .getHttpsCallable("chat-updateProposal")
            .call(hashMapOf("messageId" to messageId, "status" to status))
            .await()

        withContext(Dispatchers.IO) {
            val localMessages = chatDao.getAllMessagesSync()
            val target = localMessages.find { it.id == messageId }
            if (target != null) {
                chatDao.updateMessage(target.copy(proposalStatus = (result.data as? Map<*, *>)?.get("proposalStatus") as? String ?: status))
            }
        }
    }

    override suspend fun insertMessage(message: ChatMessage) {
        val uid = firebaseAuth.currentUser?.uid ?: ""
        val finalMessage = message.copy(senderId = uid, timestamp = System.currentTimeMillis())
        
        // 1. Add to Firestore
        val messageMap = finalMessage.toFirestoreMap().toMutableMap()
        messageMap["participantIds"] = listOf(finalMessage.senderId, finalMessage.partnerId)
        
        firestore.collection("chats").document(finalMessage.id).set(messageMap).await()
        
        // 2. Insert into Room
        withContext(Dispatchers.IO) {
            chatDao.insertMessage(finalMessage)
        }
    }

    override suspend fun markMessagesRead(partnerId: String) {
        val uid = firebaseAuth.currentUser?.uid ?: return
        val unread = withContext(Dispatchers.IO) {
            chatDao.getMessagesForPartner(partnerId).first()
                .filter { it.senderId == partnerId && it.isRead == false }
        }
        unread.forEach { message ->
            // Local
            withContext(Dispatchers.IO) {
                chatDao.updateMessage(message.copy(isRead = true))
            }
            // Remote (rules permit participants to update isRead only)
            try {
                firestore.collection("chats").document(message.id).update("isRead", true).await()
            } catch (_: Exception) {}
        }
    }

    override suspend fun uploadChatImage(uri: android.net.Uri): String {
        val uid = firebaseAuth.currentUser?.uid ?: throw Exception("Not signed in")
        val filename = "chat_${uid}_${System.currentTimeMillis()}.jpg"
        val ref = firebaseStorage.reference.child("chat_images/$filename")
        ref.putFile(uri).await()
        return ref.downloadUrl.await().toString()
    }

    override fun cleanup() {
        chatsListener?.remove()
        chatsListener = null
        authStateListener?.let { firebaseAuth.removeAuthStateListener(it) }
        authStateListener = null
    }
}
