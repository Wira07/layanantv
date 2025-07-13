package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.databinding.FragmentSubscriptionsBinding

class SubscriptionsFragment : Fragment() {
    private var _binding: FragmentSubscriptionsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSubscriptionsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Setup RecyclerView for subscriptions
        binding.rvSubscriptions.layoutManager = LinearLayoutManager(context)

        // Setup click listeners
        binding.btnBrowseChannels.setOnClickListener {
            // TODO: Navigate to channel browser
        }

        binding.btnRenewAll.setOnClickListener {
            // TODO: Renew all subscriptions
        }

        // TODO: Setup adapter for subscriptions list
        // binding.rvSubscriptions.adapter = subscriptionsAdapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}