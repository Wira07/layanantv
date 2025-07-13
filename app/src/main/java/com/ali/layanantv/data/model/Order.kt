package com.ali.layanantv.data.model

import com.google.firebase.Timestamp

data class Order(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val channelId: String = "",
    val channelName: String = "",
    val subscriptionType: String = "", // monthly, yearly
    val totalAmount: Double = 0.0,
    val status: String = "pending", // pending, confirmed, completed, cancelled
    val paymentMethod: String = "",
    val paymentVerified: Boolean = false,
    val paymentProofUrl: String = "",
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)