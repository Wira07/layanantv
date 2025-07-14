package com.ali.layanantv.data.model

import com.google.firebase.Timestamp

data class ChatMessage(
    val id: String = "",
    val chatRoomId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val senderRole: String = "", // CUSTOMER, ADMIN
    val message: String = "",
    val messageType: String = "TEXT", // TEXT, IMAGE, FILE
    val timestamp: Timestamp = Timestamp.now(),
    val isRead: Boolean = false
)