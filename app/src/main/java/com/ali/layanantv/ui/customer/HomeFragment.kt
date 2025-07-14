package com.ali.layanantv.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.R
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.databinding.FragmentHomeBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null

    // Safe getter for binding with null check
    private val binding: FragmentHomeBinding?
        get() = _binding

    private lateinit var customerRepository: CustomerRepository
    private var dashboardJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return _binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customerRepository = CustomerRepository()
        setupUI()
        loadDashboardData()
    }

    private fun setupUI() {
        binding?.let { binding ->
            // Setup click listeners for main features
            binding.btnBrowseChannels.setOnClickListener {
                // TODO: Create ChannelBrowserActivity or navigate to channels
                // startActivity(Intent(requireContext(), ChannelBrowserActivity::class.java))
            }

            binding.btnMySubscriptions.setOnClickListener {
                // Navigate to subscriptions tab
                (activity as? CustomerDashboardActivity)?.navigateToSubscriptions()
            }

            binding.btnPurchaseHistory.setOnClickListener {
                // Navigate to history tab
                (activity as? CustomerDashboardActivity)?.navigateToHistory()
            }

            binding.btnRedeemPoints.setOnClickListener {
                // TODO: Open points redemption dialog
                showPointsRedemptionDialog()
            }
        }
    }

    private fun loadDashboardData() {
        // Cancel previous job if exists
        dashboardJob?.cancel()

        dashboardJob = lifecycleScope.launch {
            try {
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@launch

                val user = customerRepository.getCurrentUser()
                val userPoints = customerRepository.getUserPoints()
                val dashboardStats = customerRepository.getCustomerDashboardStats()

                // Use safe binding access
                binding?.let { binding ->
                    binding.tvWelcome.text = "Selamat datang, ${user?.name ?: "User"}!"
                    binding.tvUserPoints.text = userPoints.toString()

                    // Update dashboard stats - these TextViews need to be added to your layout
                    // binding.tvTotalOrders.text = dashboardStats.totalOrders.toString()
                    // binding.tvActiveSubscriptions.text = dashboardStats.activeSubscriptions.toString()
                    // binding.tvPendingOrders.text = dashboardStats.pendingOrders.toString()
                }

            } catch (e: Exception) {
                // Use safe binding access for error handling
                binding?.let { binding ->
                    binding.tvWelcome.text = "Selamat datang kembali!"
                    binding.tvUserPoints.text = "2,500" // Default value as shown in your layout
                }
            }
        }
    }

    private fun showPointsRedemptionDialog() {
        // Check if fragment is still attached
        if (!isAdded) return

        context?.let { ctx ->
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("Tukar Point")
                .setMessage("Fitur tukar point akan segera hadir!")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Cancel any running coroutines
        dashboardJob?.cancel()
        dashboardJob = null
        _binding = null
    }
}