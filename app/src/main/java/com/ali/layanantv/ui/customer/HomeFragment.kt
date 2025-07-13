package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ali.layanantv.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Setup welcome message with user points
        binding.tvWelcome.text = "Selamat datang kembali!"
        binding.tvUserPoints.text = "Point Anda: 2500"

        // Setup click listeners for main features
        binding.btnBrowseChannels.setOnClickListener {
            // TODO: Navigate to channel browser
        }

        binding.btnMySubscriptions.setOnClickListener {
            // TODO: Navigate to subscriptions
        }

        binding.btnPurchaseHistory.setOnClickListener {
            // TODO: Navigate to purchase history
        }

        binding.btnRedeemPoints.setOnClickListener {
            // TODO: Open points redemption dialog
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}