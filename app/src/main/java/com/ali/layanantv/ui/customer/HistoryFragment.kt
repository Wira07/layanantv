package com.ali.layanantv.ui.customer.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Setup RecyclerView for purchase history
        binding.rvPurchaseHistory.layoutManager = LinearLayoutManager(context)

        // Setup filter buttons
        binding.btnFilterAll.setOnClickListener {
            // TODO: Show all purchases
        }

        binding.btnFilterActive.setOnClickListener {
            // TODO: Show active subscriptions only
        }

        binding.btnFilterExpired.setOnClickListener {
            // TODO: Show expired subscriptions only
        }

        // TODO: Setup adapter for purchase history
        // binding.rvPurchaseHistory.adapter = historyAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}