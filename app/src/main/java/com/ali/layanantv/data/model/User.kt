package com.ali.layanantv.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val phoneNumber: String? = null,
    val points: Int = 0,
    val role: String = Role.CUSTOMER.name,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)