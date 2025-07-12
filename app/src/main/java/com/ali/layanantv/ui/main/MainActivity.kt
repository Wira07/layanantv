package com.ali.layanantv.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.data.model.Role
import com.ali.layanantv.data.model.User
import com.ali.layanantv.data.repository.AuthRepository
import com.ali.layanantv.databinding.ActivityMainBinding
import com.ali.layanantv.ui.admin.AdminDashboardActivity
import com.ali.layanantv.ui.auth.LoginActivity
import com.ali.layanantv.ui.customer.CustomerDashboardActivity
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {
    private val authRepository = AuthRepository()
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize ViewBinding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        checkUserLoginStatus()
    }

    private fun checkUserLoginStatus() {
        lifecycleScope.launch {
            val currentUser = authRepository.getCurrentUser()
            if (currentUser != null) {
                // User sudah login, ambil data user dan redirect ke dashboard
                val userResult = authRepository.getCurrentUserData()
                if (userResult.isSuccess) {
                    val user = userResult.getOrNull()!!
                    redirectToDashboard(user)
                } else {
                    // Error getting user data, redirect to login
                    redirectToLogin()
                }
            } else {
                // User belum login, redirect ke login
                redirectToLogin()
            }
        }
    }

    private fun redirectToDashboard(user: User) {
        val intent = when (user.role) {
            Role.ADMIN.name -> Intent(this, AdminDashboardActivity::class.java)
            Role.CUSTOMER.name -> Intent(this, CustomerDashboardActivity::class.java)
            else -> Intent(this, LoginActivity::class.java)
        }
        startActivity(intent)
        finish()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finish()
    }
}
