package com.ali.layanantv.data.model

data class User(
    val uid: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = Role.CUSTOMER.name,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)