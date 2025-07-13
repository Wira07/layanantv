package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.databinding.FragmentChatBinding

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupUI()
    }

    private fun setupUI() {
        // Setup RecyclerView for chat messages
        binding.rvChatMessages.layoutManager = LinearLayoutManager(context)

        // Setup send button
        binding.btnSendMessage.setOnClickListener {
            sendMessage()
        }

        // Setup quick reply buttons
        binding.btnQuickReply1.setOnClickListener {
            binding.etMessage.setText("Saya mengalami masalah dengan channel")
        }

        binding.btnQuickReply2.setOnClickListener {
            binding.etMessage.setText("Bagaimana cara perpanjang langganan?")
        }

        binding.btnQuickReply3.setOnClickListener {
            binding.etMessage.setText("Saya ingin membatalkan langganan")
        }

        // TODO: Setup chat adapter
        // binding.rvChatMessages.adapter = chatAdapter
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        if (message.isNotEmpty()) {
            // TODO: Send message to customer service
            binding.etMessage.text?.clear()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}