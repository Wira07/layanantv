package com.ali.layanantv.data.repository

import android.util.Log
import com.ali.layanantv.data.model.ChatMessage
import com.ali.layanantv.data.model.ChatRoom
import com.ali.layanantv.data.model.ChatStatus
import com.ali.layanantv.data.model.MessageType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ChatRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "ChatRepository"
        private const val COLLECTION_CHAT_ROOMS = "chat_rooms"
        private const val COLLECTION_CHAT_MESSAGES = "chat_messages"
    }

    // Create or get existing chat room for customer
    suspend fun createOrGetChatRoom(): ChatRoom? {
        val currentUser = auth.currentUser ?: return null

        return try {
            Log.d(TAG, "Creating or getting chat room for user: ${currentUser.uid}")

            // Check if chat room already exists
            val existingRoom = firestore.collection(COLLECTION_CHAT_ROOMS)
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("status", ChatStatus.ACTIVE.name)
                .get()
                .await()

            if (existingRoom.documents.isNotEmpty()) {
                val room = existingRoom.documents.first().toObject(ChatRoom::class.java)
                    ?.copy(id = existingRoom.documents.first().id)
                Log.d(TAG, "Found existing chat room: ${room?.id}")
                return room
            }

            // Create new chat room
            val newRoom = ChatRoom(
                userId = currentUser.uid,
                userName = currentUser.displayName ?: "Customer",
                userEmail = currentUser.email ?: "",
                status = ChatStatus.ACTIVE.name,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )

            val docRef = firestore.collection(COLLECTION_CHAT_ROOMS).add(newRoom).await()
            val createdRoom = newRoom.copy(id = docRef.id)

            Log.d(TAG, "Created new chat room: ${createdRoom.id}")
            createdRoom
        } catch (e: Exception) {
            Log.e(TAG, "Error creating chat room: ${e.message}", e)
            null
        }
    }

    // Get all chat rooms for admin
    suspend fun getAllChatRooms(): List<ChatRoom> {
        return try {
            Log.d(TAG, "Getting all chat rooms for admin")
            val snapshot = firestore.collection(COLLECTION_CHAT_ROOMS)
                .orderBy("lastMessageTime", Query.Direction.DESCENDING)
                .get()
                .await()

            val rooms = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Found ${rooms.size} chat rooms")
            rooms
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat rooms: ${e.message}", e)
            emptyList()
        }
    }

    // Get chat room by ID
    suspend fun getChatRoomById(roomId: String): ChatRoom? {
        return try {
            Log.d(TAG, "Getting chat room by ID: $roomId")
            val snapshot = firestore.collection(COLLECTION_CHAT_ROOMS)
                .document(roomId)
                .get()
                .await()

            snapshot.toObject(ChatRoom::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting chat room: ${e.message}", e)
            null
        }
    }

    // Send message
    suspend fun sendMessage(chatRoomId: String, message: String, messageType: MessageType = MessageType.TEXT): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            Log.d(TAG, "Sending message to room: $chatRoomId")

            // Get user role
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userRole = userDoc.getString("role") ?: "CUSTOMER"

            val chatMessage = ChatMessage(
                chatRoomId = chatRoomId,
                senderId = currentUser.uid,
                senderName = currentUser.displayName ?: "User",
                senderRole = userRole,
                message = message,
                messageType = messageType.name,
                timestamp = Timestamp.now()
            )

            // Add message to subcollection
            firestore.collection(COLLECTION_CHAT_ROOMS)
                .document(chatRoomId)
                .collection(COLLECTION_CHAT_MESSAGES)
                .add(chatMessage)
                .await()

            // Update chat room with last message
            firestore.collection(COLLECTION_CHAT_ROOMS)
                .document(chatRoomId)
                .update(
                    "lastMessage", message,
                    "lastMessageTime", Timestamp.now(),
                    "updatedAt", Timestamp.now()
                )
                .await()

            Log.d(TAG, "Message sent successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending message: ${e.message}", e)
            false
        }
    }

    // Get messages for a chat room with real-time updates
    fun getMessages(chatRoomId: String): Flow<List<ChatMessage>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_CHAT_ROOMS)
            .document(chatRoomId)
            .collection(COLLECTION_CHAT_MESSAGES)
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to messages: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(messages)
            }

        awaitClose { listener.remove() }
    }

    // Get chat rooms with real-time updates for admin
    fun getChatRoomsFlow(): Flow<List<ChatRoom>> = callbackFlow {
        val listener = firestore.collection(COLLECTION_CHAT_ROOMS)
            .orderBy("lastMessageTime", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to chat rooms: ${error.message}", error)
                    close(error)
                    return@addSnapshotListener
                }

                val rooms = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(ChatRoom::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(rooms)
            }

        awaitClose { listener.remove() }
    }

    // Mark messages as read
    suspend fun markMessagesAsRead(chatRoomId: String, userId: String) {
        try {
            Log.d(TAG, "Marking messages as read for room: $chatRoomId")

            val messages = firestore.collection(COLLECTION_CHAT_ROOMS)
                .document(chatRoomId)
                .collection(COLLECTION_CHAT_MESSAGES)
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", userId)
                .get()
                .await()

            val batch = firestore.batch()
            messages.documents.forEach { doc ->
                batch.update(doc.reference, "isRead", true)
            }
            batch.commit().await()

            Log.d(TAG, "Messages marked as read")
        } catch (e: Exception) {
            Log.e(TAG, "Error marking messages as read: ${e.message}", e)
        }
    }

    // Close chat room
    suspend fun closeChatRoom(chatRoomId: String) {
        try {
            Log.d(TAG, "Closing chat room: $chatRoomId")
            firestore.collection(COLLECTION_CHAT_ROOMS)
                .document(chatRoomId)
                .update(
                    "status", ChatStatus.CLOSED.name,
                    "updatedAt", Timestamp.now()
                )
                .await()
            Log.d(TAG, "Chat room closed")
        } catch (e: Exception) {
            Log.e(TAG, "Error closing chat room: ${e.message}", e)
        }
    }

    // Set typing status
    suspend fun setTypingStatus(chatRoomId: String, isUserTyping: Boolean) {
        val currentUser = auth.currentUser ?: return

        try {
            val userDoc = firestore.collection("users").document(currentUser.uid).get().await()
            val userRole = userDoc.getString("role") ?: "CUSTOMER"

            val updateField = if (userRole == "ADMIN") "isAdminTyping" else "isUserTyping"

            firestore.collection(COLLECTION_CHAT_ROOMS)
                .document(chatRoomId)
                .update(updateField, isUserTyping)
                .await()
        } catch (e: Exception) {
            Log.e(TAG, "Error setting typing status: ${e.message}", e)
        }
    }

    // Get unread message count
    suspend fun getUnreadMessageCount(chatRoomId: String): Int {
        val currentUser = auth.currentUser ?: return 0

        return try {
            val snapshot = firestore.collection(COLLECTION_CHAT_ROOMS)
                .document(chatRoomId)
                .collection(COLLECTION_CHAT_MESSAGES)
                .whereEqualTo("isRead", false)
                .whereNotEqualTo("senderId", currentUser.uid)
                .get()
                .await()

            snapshot.documents.size
        } catch (e: Exception) {
            Log.e(TAG, "Error getting unread count: ${e.message}", e)
            0
        }
    }

    // Search messages
    suspend fun searchMessages(chatRoomId: String, query: String): List<ChatMessage> {
        return try {
            Log.d(TAG, "Searching messages in room: $chatRoomId with query: $query")
            val snapshot = firestore.collection(COLLECTION_CHAT_ROOMS)
                .document(chatRoomId)
                .collection(COLLECTION_CHAT_MESSAGES)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val messages = snapshot.documents.mapNotNull { doc ->
                doc.toObject(ChatMessage::class.java)?.copy(id = doc.id)
            }

            // Filter messages that contain the query
            messages.filter { it.message.contains(query, ignoreCase = true) }
        } catch (e: Exception) {
            Log.e(TAG, "Error searching messages: ${e.message}", e)
            emptyList()
        }
    }
}