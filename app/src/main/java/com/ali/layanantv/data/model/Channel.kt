package com.ali.layanantv.data.model

import com.google.firebase.Timestamp

data class Channel(
    val id: String = "",
    val name: String = "",
    val description: String = "",
    val logoUrl: String = "",
    val logoBase64: String = "",
    val price: Double = 0.0,
    val category: String = "",
    val isActive: Boolean = true,
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now()
)