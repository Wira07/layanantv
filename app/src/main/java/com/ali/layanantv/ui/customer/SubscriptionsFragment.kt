package com.ali.layanantv.ui.customer

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.databinding.FragmentSubscriptionsBinding
import kotlinx.coroutines.launch

import com.ali.layanantv.data.model.Order

class SubscriptionsFragment : Fragment() {
    private var _binding: FragmentSubscriptionsBinding? = null

    // Gunakan safe binding dengan null check
    private val binding: FragmentSubscriptionsBinding?
        get() = _binding

    private lateinit var customerRepository: CustomerRepository
    private lateinit var subscriptionsAdapter: SubscriptionsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        customerRepository = CustomerRepository()
        setupUI()
        loadSubscriptions()
    }

    private fun setupUI() {
        // Safety check untuk binding
        val safeBinding = binding ?: return

        // Setup RecyclerView for subscriptions
        subscriptionsAdapter = SubscriptionsAdapter(
            onRenewClick = { subscription ->
                renewSubscription(subscription)
            },
            onCancelClick = { subscription ->
                cancelSubscription(subscription)
            }
        )

        safeBinding.rvSubscriptions.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = subscriptionsAdapter
        }

        // Setup click listeners
        safeBinding.btnBrowseChannels.setOnClickListener {
            // Safety check sebelum start activity
            if (isAdded && context != null) {
                startActivity(Intent(requireContext(), ChannelBrowserActivity::class.java))
            }
        }

        safeBinding.btnRenewAll.setOnClickListener {
            renewAllSubscriptions()
        }

        // Setup swipe refresh
        safeBinding.swipeRefresh.setOnRefreshListener {
            loadSubscriptions()
        }
    }

    private fun loadSubscriptions() {
        // Gunakan viewLifecycleOwner untuk auto-cancel saat fragment destroy
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                // Safety check untuk binding dan lifecycle
                val safeBinding = binding
                if (safeBinding == null || !isAdded) return@launch

                if (!safeBinding.swipeRefresh.isRefreshing) {
                    safeBinding.progressBar.visibility = View.VISIBLE
                }

                val subscriptions = customerRepository.getUserSubscriptions()

                // Check lagi sebelum update UI
                if (binding == null || !isAdded) return@launch

                binding?.let { b ->
                    if (subscriptions.isEmpty()) {
                        b.emptyState.visibility = View.VISIBLE
                        b.rvSubscriptions.visibility = View.GONE
                    } else {
                        b.emptyState.visibility = View.GONE
                        b.rvSubscriptions.visibility = View.VISIBLE
                        subscriptionsAdapter.submitList(subscriptions)
                    }

                    b.progressBar.visibility = View.GONE
                    b.swipeRefresh.isRefreshing = false
                }

            } catch (e: Exception) {
                // Safety check untuk error handling
                if (isAdded && binding != null) {
                    binding?.let { b ->
                        b.progressBar.visibility = View.GONE
                        b.swipeRefresh.isRefreshing = false
                    }

                    // Check context sebelum show toast
                    context?.let {
                        Toast.makeText(it, "Error loading subscriptions: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun renewSubscription(subscription: Order) {
        // Safety check
        if (!isAdded || context == null) return

        // TODO: Implement renewal logic
        Toast.makeText(context, "Renewing subscription for ${subscription.channelName}", Toast.LENGTH_SHORT).show()
    }

    private fun cancelSubscription(subscription: Order) {
        // Safety check
        if (!isAdded || context == null) return

        // TODO: Implement cancellation logic
        context?.let { ctx ->
            androidx.appcompat.app.AlertDialog.Builder(ctx)
                .setTitle("Batalkan Langganan")
                .setMessage("Apakah Anda yakin ingin membatalkan langganan ${subscription.channelName}?")
                .setPositiveButton("Ya") { _, _ ->
                    // Implement cancellation
                    if (isAdded && context != null) {
                        Toast.makeText(context, "Langganan dibatalkan", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Tidak", null)
                .show()
        }
    }

    private fun renewAllSubscriptions() {
        // Safety check
        if (!isAdded || context == null) return

        Toast.makeText(context, "Renewing all subscriptions", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}