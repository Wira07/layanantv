package com.ali.layanantv.data.model

import com.google.firebase.Timestamp

data class ChatRoom(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val adminId: String = "",
    val adminName: String = "",
    val status: String = "ACTIVE", // ACTIVE, CLOSED
    val lastMessage: String = "",
    val lastMessageTime: Timestamp = Timestamp.now(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val unreadCount: Int = 0,
    val isUserTyping: Boolean = false,
    val isAdminTyping: Boolean = false
)