package com.ali.layanantv.ui.admin

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
import com.ali.layanantv.databinding.FragmentChatAdminBinding
import com.ali.layanantv.ui.customer.ChatAdapter
import com.ali.layanantv.ui.adapter.ChatRoomAdapter
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class ChatFragmentAdmin : Fragment() {
    private var _binding: FragmentChatAdminBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private lateinit var chatRepository: ChatRepository
    private var currentChatRoom: ChatRoom? = null
    private var typingJob: Job? = null
    private var searchJob: Job? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatAdminBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        chatRepository = ChatRepository()
        setupUI()
        loadChatRooms()
    }

    private fun setupUI() {
        // Setup RecyclerView for chat rooms list
        chatRoomAdapter = ChatRoomAdapter { chatRoom ->
            selectChatRoom(chatRoom)
        }
        binding.rvChatRooms.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatRoomAdapter
        }

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

        // Setup close chat button
        binding.btnCloseChat.setOnClickListener {
            closeChatRoom()
        }

        // Setup search
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handleSearch(s.toString())
            }
        })

        // Setup typing indicator
        binding.etMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                handleTyping()
            }
        })

        // Setup quick reply buttons
        binding.btnQuickReply1.setOnClickListener {
            binding.etMessage.setText("Halo, ada yang bisa saya bantu?")
        }

        binding.btnQuickReply2.setOnClickListener {
            binding.etMessage.setText("Terima kasih telah menghubungi customer service")
        }

        binding.btnQuickReply3.setOnClickListener {
            binding.etMessage.setText("Apakah ada pertanyaan lain yang bisa saya bantu?")
        }

        // Initially hide chat interface
        showChatInterface(false)
    }

    private fun loadChatRooms() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                chatRepository.getChatRoomsFlow().collect { chatRooms ->
                    chatRoomAdapter.submitList(chatRooms)

                    // Update UI based on chat rooms availability
                    if (chatRooms.isEmpty()) {
                        binding.tvEmptyState.visibility = View.VISIBLE
                        binding.rvChatRooms.visibility = View.GONE
                    } else {
                        binding.tvEmptyState.visibility = View.GONE
                        binding.rvChatRooms.visibility = View.VISIBLE
                    }
                }
            } catch (e: Exception) {
                showError("Error loading chat rooms: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun selectChatRoom(chatRoom: ChatRoom) {
        currentChatRoom = chatRoom

        // Update UI
        binding.tvChatWithUser.text = "Chat dengan ${chatRoom.userName}"
        showChatInterface(true)

        // Load messages for this chat room
        observeMessages()

        // Mark messages as read
        markMessagesAsRead()
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
                val success = chatRepository.sendMessage(room.id, message, MessageType.TEXT)
                if (!success) {
                    showError("Gagal mengirim pesan")
                }
            }
        }
    }

    private fun closeChatRoom() {
        currentChatRoom?.let { room ->
            lifecycleScope.launch {
                chatRepository.closeChatRoom(room.id)

                // Reset UI
                currentChatRoom = null
                showChatInterface(false)
                chatAdapter.submitList(emptyList())

                Toast.makeText(context, "Chat room ditutup", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun handleSearch(query: String) {
        searchJob?.cancel()

        if (query.isEmpty()) {
            loadChatRooms()
            return
        }

        searchJob = lifecycleScope.launch {
            delay(500) // Debounce search

            currentChatRoom?.let { room ->
                try {
                    val searchResults = chatRepository.searchMessages(room.id, query)
                    chatAdapter.submitList(searchResults)
                } catch (e: Exception) {
                    showError("Error searching messages: ${e.message}")
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
                // Admin marks messages as read with current user ID
                com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { adminId ->
                    chatRepository.markMessagesAsRead(room.id, adminId)
                }
            }
        }
    }

    private fun showChatInterface(show: Boolean) {
        binding.layoutChatInterface.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutChatRoomsList.visibility = if (show) View.GONE else View.VISIBLE
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
        searchJob?.cancel()
        _binding = null
    }
}