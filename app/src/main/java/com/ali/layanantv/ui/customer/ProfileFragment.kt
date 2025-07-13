package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ali.layanantv.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

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
        setupUI()
    }

    private fun setupUI() {
        // Setup user profile info
        binding.tvUserName.text = "Ali Rahman"
        binding.tvUserEmail.text = "ali.rahman@email.com"
        binding.tvUserPhone.text = "+62 812-3456-7890"
        binding.tvUserPoints.text = "2500 Point"

        // Setup click listeners
        binding.btnEditProfile.setOnClickListener {
            // TODO: Open edit profile activity
        }

        binding.btnChangePassword.setOnClickListener {
            // TODO: Open change password activity
        }

        binding.btnNotificationSettings.setOnClickListener {
            // TODO: Open notification settings
        }

        binding.btnHelp.setOnClickListener {
            // TODO: Open help/FAQ activity
        }

        binding.btnAbout.setOnClickListener {
            // TODO: Open about app activity
        }

        binding.btnLogout.setOnClickListener {
            // Call logout method from parent activity
            (activity as? CustomerDashboardActivity)?.logout()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}