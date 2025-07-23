package com.ali.layanantv.data.model

import com.google.firebase.Timestamp

data class Order(
    val id: String ="",
    val userId: String = "",
    val userEmail: String ?= null,
    val userName: String ?= null,
    val channelId: String = "",
    val channelName: String ?= null,
    val subscriptionType: String = "",
    val originalAmount: Double = 0.0,
    val pointsUsed: Int = 0,
    val pointDiscount: Double = 0.0,
    val totalAmount: Double= 0.0,
    val status: String = "",
    val paymentVerified: Boolean = false,
    val paymentMethod: String ?= null,
    val proofImageUrl: String ?= null,
    val notes: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)
