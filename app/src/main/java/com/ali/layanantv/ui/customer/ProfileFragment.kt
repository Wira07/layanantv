package com.ali.layanantv.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.databinding.FragmentProfileBinding
import com.ali.layanantv.ui.auth.LoginActivity
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var customerRepository: CustomerRepository

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customerRepository = CustomerRepository()
        setupUI()
        loadUserProfile()
    }

    private fun setupUI() {
        // Setup click listeners
        binding.btnEditProfile.setOnClickListener {
            try {
                val intent = Intent(requireContext(), EditProfileActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Edit Profile feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnChangePassword.setOnClickListener {
            try {
                val intent = Intent(requireContext(), ChangePasswordActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Change Password feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnHelp.setOnClickListener {
            try {
                val intent = Intent(requireContext(), HelpActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Help feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnAbout.setOnClickListener {
            try {
                val intent = Intent(requireContext(), AboutActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "About feature coming soon", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun loadUserProfile() {
        lifecycleScope.launch {
            try {
                val user = customerRepository.getCurrentUser()
                if (user != null) {
                    val userPoints = customerRepository.getUserPoints(user.uid)

                    binding.tvUserName.text = user.name
                    binding.tvUserEmail.text = user.email
                    binding.tvUserPhone.text = user.phoneNumber ?: "-"
                    binding.tvUserPoints.text = "$userPoints Points"
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Error loading profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showLogoutDialog() {
        context?.let { ctx ->
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Ya") { _, _ ->
                    performLogout()
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    private fun performLogout() {
        lifecycleScope.launch {
            try {
                // Sign out from Firebase
                FirebaseAuth.getInstance().signOut()

                // Navigate to login screen
                val intent = Intent(requireContext(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)

                // Finish current activity
                activity?.finish()
            } catch (e: Exception) {
                Toast.makeText(context, "Error during logout: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadUserProfile()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}