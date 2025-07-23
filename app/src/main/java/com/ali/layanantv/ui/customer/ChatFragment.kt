package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.data.model.ChatRoom
import com.ali.layanantv.data.model.MessageType
import com.ali.layanantv.data.repository.ChatRepository
import com.ali.layanantv.databinding.FragmentChatBinding
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatRepository: ChatRepository
    private var currentChatRoom: ChatRoom? = null
    private var typingJob: Job? = null

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

        chatRepository = ChatRepository()
        setupUI()
        initializeChat()
    }

    private fun setupUI() {
        // Setup RecyclerView for chat messages
        chatAdapter = ChatAdapter()
        binding.rvChatMessages.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatAdapter
        }

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

        // Setup typing indicator
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handleTyping()
            }
        })
    }

    private fun initializeChat() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                // Create or get existing chat room
                currentChatRoom = chatRepository.createOrGetChatRoom()

                if (currentChatRoom != null) {
                    observeMessages()
                    markMessagesAsRead()
                } else {
                    showError("Gagal memuat chat room")
                }
            } catch (e: Exception) {
                showError("Error: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun observeMessages() {
        currentChatRoom?.let { room ->
            lifecycleScope.launch {
                chatRepository.getMessages(room.id).collect { messages ->
                    chatAdapter.submitList(messages)

                    // Scroll to bottom when new messages arrive
                    if (messages.isNotEmpty()) {
                        binding.rvChatMessages.scrollToPosition(messages.size - 1)
                    }

                    // Mark messages as read
                    markMessagesAsRead()
                }
            }
        }
    }

    private fun sendMessage() {
        val message = binding.etMessage.text.toString().trim()
        if (message.isEmpty()) return

        currentChatRoom?.let { room ->
            binding.etMessage.text?.clear()

            lifecycleScope.launch {
                val success = chatRepository.sendMessage(room.id, message,
                    MessageType.TEXT.toString()
                )
                if (!success) {
                    showError("Gagal mengirim pesan")
                }
            }
        }
    }

    private fun handleTyping() {
        currentChatRoom?.let { room ->
            // Cancel previous typing job
            typingJob?.cancel()

            // Set typing status to true
            lifecycleScope.launch {
                chatRepository.setTypingStatus(room.id, true)
            }

            // Set typing status to false after 2 seconds of inactivity
            typingJob = lifecycleScope.launch {
                delay(2000)
                chatRepository.setTypingStatus(room.id, false)
            }
        }
    }

    private fun markMessagesAsRead() {
        currentChatRoom?.let { room ->
            lifecycleScope.launch {
                chatRepository.markMessagesAsRead(room.id, room.userId)
            }
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Stop typing indicator when leaving
        currentChatRoom?.let { room ->
            lifecycleScope.launch {
                chatRepository.setTypingStatus(room.id, false)
            }
        }

        typingJob?.cancel()
        _binding = null
    }
}