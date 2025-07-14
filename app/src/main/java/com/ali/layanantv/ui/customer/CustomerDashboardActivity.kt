package com.ali.layanantv.ui.customer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.ali.layanantv.R
import com.ali.layanantv.databinding.ActivityCustomerDashboardBinding

class CustomerDashboardActivity : AppCompatActivity() {
    private lateinit var binding: ActivityCustomerDashboardBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCustomerDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        // Set default fragment
        if (savedInstanceState == null) {
            replaceFragment(HomeFragment())
        }
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    replaceFragment(HomeFragment())
                    true
                }
                R.id.nav_subscriptions -> {
                    replaceFragment(SubscriptionsFragment())
                    true
                }
                R.id.nav_history -> {
                    replaceFragment(HistoryFragment())
                    true
                }
                R.id.nav_profile -> {
                    replaceFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Method untuk navigasi dari HomeFragment ke SubscriptionsFragment
    fun navigateToSubscriptions() {
        binding.bottomNavigation.selectedItemId = R.id.nav_subscriptions
    }

    // Method untuk navigasi dari HomeFragment ke HistoryFragment
    fun navigateToHistory() {
        binding.bottomNavigation.selectedItemId = R.id.nav_history
    }
}