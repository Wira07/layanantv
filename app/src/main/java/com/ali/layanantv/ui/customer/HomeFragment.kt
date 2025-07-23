package com.ali.layanantv.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
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

    // Activity Result Launcher untuk menangani result dari ChannelBrowserActivity
    private val channelBrowserLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            // Jika berhasil berlangganan, pindah ke SubscriptionsFragment
            (activity as? CustomerDashboardActivity)?.navigateToSubscriptions()
        }
    }

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
                val intent = Intent(requireContext(), ChannelBrowserActivity::class.java)
                channelBrowserLauncher.launch(intent)
            }

            binding.btnMySubscriptions.setOnClickListener {
                // Navigate to subscriptions tab
                (activity as? CustomerDashboardActivity)?.navigateToSubscriptions()
            }

            binding.btnPurchaseHistory.setOnClickListener {
                // Navigate to history tab
                (activity as? CustomerDashboardActivity)?.navigateToHistory()
            }

            // NEW: Upload bukti pembayaran QRIS
//            binding.btnUploadPaymentProof.setOnClickListener {
//                navigateToQrisPaymentProof()
//            }

            // Point card click listener - show point info instead of redeem
            binding.pointsCard.setOnClickListener {
                showPointsInfoDialog()
            }

            // Add click listener for "Mulai Berlangganan" button in promo section
            setupPromoSectionClickListener()
        }
    }

    private fun setupPromoSectionClickListener() {
        binding?.let { binding ->
            // Add click listener to the "Mulai Berlangganan" button
            val startSubscriptionButton = binding.root.findViewById<View>(R.id.btn_start_subscription)
            startSubscriptionButton?.setOnClickListener {
                // Navigate to ChannelBrowserActivity dan tunggu hasilnya
                val intent = Intent(requireContext(), ChannelBrowserActivity::class.java)
                channelBrowserLauncher.launch(intent)
            }

            // Alternative: Make entire promo card clickable if button is not available
            val promoCard = binding.root.findViewById<androidx.cardview.widget.CardView>(R.id.promo_card)
            promoCard?.setOnClickListener {
                // Navigate to ChannelBrowserActivity dan tunggu hasilnya
                val intent = Intent(requireContext(), ChannelBrowserActivity::class.java)
                channelBrowserLauncher.launch(intent)
            }
        }
    }

    private fun navigateToPaymentActivity(channelId: String = "default_channel_id") {
        val intent = Intent(requireContext(), PaymentActivity::class.java).apply {
            putExtra(PaymentActivity.EXTRA_CHANNEL_ID, channelId)
            putExtra(PaymentActivity.EXTRA_SUBSCRIPTION_TYPE, "1_month")
        }
        startActivity(intent)
    }

    // NEW: Method untuk navigasi ke Payment dengan callback
    private fun navigateToPaymentActivityWithCallback(channelId: String = "default_channel_id") {
        val intent = Intent(requireContext(), PaymentActivity::class.java).apply {
            putExtra(PaymentActivity.EXTRA_CHANNEL_ID, channelId)
            putExtra(PaymentActivity.EXTRA_SUBSCRIPTION_TYPE, "1_month")
        }

        // Gunakan launcher untuk menangani hasil payment
        val paymentLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                // Jika payment berhasil, pindah ke SubscriptionsFragment
                (activity as? CustomerDashboardActivity)?.navigateToSubscriptions()
            }
        }

        paymentLauncher.launch(intent)
    }

    // NEW: Method untuk navigasi ke QRIS Payment Proof
    private fun navigateToQrisPaymentProof() {
        val intent = Intent(requireContext(), QrisPaymentProofActivity::class.java).apply {
            // Contoh data dummy, sesuaikan dengan kebutuhan Anda
            putExtra(QrisPaymentProofActivity.EXTRA_ORDER_ID, "ORD-2024-001")
            putExtra(QrisPaymentProofActivity.EXTRA_PAYMENT_AMOUNT, "Rp 150.000")
            putExtra(QrisPaymentProofActivity.EXTRA_CHANNEL_NAME, "Premium Sports")
        }
        startActivity(intent)
    }

    // NEW: Method untuk navigasi dengan parameter dinamis
    private fun navigateToQrisPaymentProofWithOrder(orderId: String, amount: String, channelName: String) {
        val intent = Intent(requireContext(), QrisPaymentProofActivity::class.java).apply {
            putExtra(QrisPaymentProofActivity.EXTRA_ORDER_ID, orderId)
            putExtra(QrisPaymentProofActivity.EXTRA_PAYMENT_AMOUNT, amount)
            putExtra(QrisPaymentProofActivity.EXTRA_CHANNEL_NAME, channelName)
        }
        startActivity(intent)
    }

    private fun loadDashboardData() {
        // Cancel previous job if exists
        dashboardJob?.cancel()

        dashboardJob = lifecycleScope.launch {
            try {
                // Check if fragment is still attached and binding is not null
                if (!isAdded || binding == null) return@launch

                val user = customerRepository.getCurrentUser()
                val userPoints = customerRepository.getUserPoints(user?.uid ?: "")
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

    private fun showPointsInfoDialog() {
        // Check if fragment is still attached
        if (!isAdded) return

        context?.let { ctx ->
            lifecycleScope.launch {
                val currentUser = customerRepository.getCurrentUser()
                val userPoints = customerRepository.getUserPoints(currentUser?.uid ?: "")

                androidx.appcompat.app.AlertDialog.Builder(ctx)
                    .setTitle("Point Rewards Anda")
                    .setMessage("Anda memiliki $userPoints point.\n\nPoint dapat digunakan untuk mengurangi harga pembayaran:\n• 1 Point = Rp 1\n• Minimal penggunaan: 100 point\n• Maksimal penggunaan: 50% dari total harga\n\nGunakan point Anda saat melakukan pembayaran langganan!")
                    .setPositiveButton("Mengerti", null)
                    .setNegativeButton("Mulai Berlangganan") { _, _ ->
                        // Navigate to channel browser dengan callback
                        val intent = Intent(requireContext(), ChannelBrowserActivity::class.java)
                        channelBrowserLauncher.launch(intent)
                    }
                    .show()
            }
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