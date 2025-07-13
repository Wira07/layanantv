package com.ali.layanantv.ui.customer

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ali.layanantv.R
import com.ali.layanantv.data.repository.AuthRepository
import com.ali.layanantv.databinding.ActivityCustomerDashboardBinding
import com.ali.layanantv.ui.auth.LoginActivity
import com.ali.layanantv.ui.customer.fragments.HistoryFragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomerDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Layanan Channel TV"
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_subscriptions -> {
                    loadFragment(SubscriptionsFragment())
                    true
                }
                R.id.nav_history -> {
                    loadFragment(HistoryFragment())
                    true
                }
                R.id.nav_chat -> {
                    loadFragment(ChatFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    fun logout() {
        AuthRepository().logout()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}