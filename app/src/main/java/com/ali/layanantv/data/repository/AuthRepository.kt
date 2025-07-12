package com.ali.layanantv.data.repository

import com.ali.layanantv.data.firebase.FirebaseManager
import com.ali.layanantv.data.model.Role
import com.ali.layanantv.data.model.User
import com.ali.layanantv.utils.ValidationUtils
import com.google.firebase.auth.FirebaseUser

class AuthRepository {
    private val firebaseManager = FirebaseManager()

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseManager.signInWithEmailAndPassword(email, password)
            if (authResult.isSuccess) {
                val firebaseUser = authResult.getOrNull()!!
                val userResult = firebaseManager.getUserFromFirestore(firebaseUser.uid)
                userResult
            } else {
                Result.failure(authResult.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(name: String, email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseManager.createUserWithEmailAndPassword(email, password)
            if (authResult.isSuccess) {
                val firebaseUser = authResult.getOrNull()!!
                val role = if (ValidationUtils.isAdminEmail(email)) Role.ADMIN else Role.CUSTOMER

                val user = User(
                    uid = firebaseUser.uid,
                    email = email,
                    name = name,
                    role = role.name
                )

                val saveResult = firebaseManager.saveUserToFirestore(user)
                if (saveResult.isSuccess) {
                    Result.success(user)
                } else {
                    Result.failure(saveResult.exceptionOrNull()!!)
                }
            } else {
                Result.failure(authResult.exceptionOrNull()!!)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? = firebaseManager.getCurrentUser()

    suspend fun getCurrentUserData(): Result<User> {
        val currentUser = getCurrentUser()
        return if (currentUser != null) {
            firebaseManager.getUserFromFirestore(currentUser.uid)
        } else {
            Result.failure(Exception("No user logged in"))
        }
    }

    fun logout() {
        firebaseManager.signOut()
    }
}