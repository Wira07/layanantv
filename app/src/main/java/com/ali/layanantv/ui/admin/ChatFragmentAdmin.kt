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
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.isActive

class ChatFragmentAdmin : Fragment() {
    private var _binding: FragmentChatAdminBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatAdapter: ChatAdapter
    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private lateinit var chatRepository: ChatRepository
    private var currentChatRoom: ChatRoom? = null
    private var typingJob: Job? = null
    private var searchJob: Job? = null
    private var loadingJob: Job? = null
    private var messagesJob: Job? = null


    companion object {
        private const val LOADING_TIMEOUT = 10000L // 10 seconds timeout
        private const val TYPING_DELAY = 2000L
        private const val SEARCH_DEBOUNCE = 500L
    }

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

//    private fun loadChatRooms() {
//        // Cancel any existing loading job
//        loadingJob?.cancel()
//
//        // Check if fragment is still active
//        if (!isAdded || _binding == null) return
//
//        showLoading(true)
//
//        loadingJob = lifecycleScope.launch {
//            try {
//                // Check if coroutine is still active
//                if (!isActive) return@launch
//
//                // Add timeout to prevent endless loading
//                withTimeout(LOADING_TIMEOUT) {
//                    chatRepository.getChatRoomsFlow().collect { chatRooms ->
//                        // Check if fragment is still active and attached
//                        if (!isActive || !isAdded || _binding == null) return@collect
//
//                        // Hide loading as soon as we get data
//                        showLoading(false)
//
//                        chatRoomAdapter.submitList(chatRooms)
//
//                        // Update UI based on chat rooms availability
//                        if (chatRooms.isEmpty()) {
//                            binding.tvEmptyState.visibility = View.VISIBLE
//                            binding.rvChatRooms.visibility = View.GONE
//                        } else {
//                            binding.tvEmptyState.visibility = View.GONE
//                            binding.rvChatRooms.visibility = View.VISIBLE
//                        }
//                    }
//                }
//            } catch (e: TimeoutCancellationException) {
//                if (!isActive || !isAdded || _binding == null) return@launch
//                showLoading(false)
//                showError("Loading timeout. Please try again.")
//                // Show empty state on timeout
//                binding.tvEmptyState.visibility = View.VISIBLE
//                binding.rvChatRooms.visibility = View.GONE
//            } catch (e: Exception) {
//                if (!isActive || !isAdded || _binding == null) return@launch
//                showLoading(false)
//                // Only show error if it's not a cancellation
//                if (e.message?.contains("cancelled", ignoreCase = true) != true) {
//                    showError("Error loading chat rooms: ${e.message}")
//                }
//                // Show empty state on error
//                binding.tvEmptyState.visibility = View.VISIBLE
//                binding.rvChatRooms.visibility = View.GONE
//            }
//        }
//
//        // Additional safety: Hide loading after maximum time regardless
//        lifecycleScope.launch {
//            delay(LOADING_TIMEOUT)
//            if (isActive && isAdded && _binding != null && binding.progressBar.visibility == View.VISIBLE) {
//                showLoading(false)
//            }
//        }
//    }

    private fun loadChatRooms() {
        loadingJob?.cancel()

        if (!isAdded || _binding == null) return

        showLoading(true)
        var receivedData = false

        loadingJob = lifecycleScope.launch {
            chatRepository.getChatRoomsFlow().collect { chatRooms ->
                if (!isActive || !isAdded || _binding == null) return@collect

                showLoading(false)
                receivedData = true

                chatRoomAdapter.submitList(chatRooms)

                binding.tvEmptyState.visibility =
                    if (chatRooms.isEmpty()) View.VISIBLE else View.GONE
                binding.rvChatRooms.visibility =
                    if (chatRooms.isEmpty()) View.GONE else View.VISIBLE
            }
        }

        // Timeout fallback
        lifecycleScope.launch {
            delay(LOADING_TIMEOUT)
            if (!receivedData && isAdded && _binding != null) {
                showLoading(false)
                showError("Loading timeout. Please try again.")
                binding.tvEmptyState.visibility = View.VISIBLE
                binding.rvChatRooms.visibility = View.GONE
            }
        }
    }

    private fun selectChatRoom(chatRoom: ChatRoom) {
        currentChatRoom = chatRoom

        // Update UI
        binding.tvChatWithUser.text = "Chat dengan ${chatRoom.userName}"
        binding.btnCloseChat.visibility = View.VISIBLE
        showChatInterface(true)

        // Load messages for this chat room
        observeMessages()

        // Mark messages as read
        markMessagesAsRead()
    }

    private fun observeMessages() {
        messagesJob?.cancel()

        currentChatRoom?.let { room ->
            lifecycleScope.launch {
                try {
                    if (!isActive || !isAdded || _binding == null) return@launch

                    chatRepository.getMessages(room.id).collect { messages ->
                        // Check if fragment is still active and attached
                        if (!isActive || !isAdded || _binding == null) return@collect

                        chatAdapter.submitList(messages)

                        // Scroll to bottom when new messages arrive
                        if (messages.isNotEmpty()) {
                            binding.rvChatMessages.post {
                                if (isAdded && _binding != null) {
                                    binding.rvChatMessages.scrollToPosition(messages.size - 1)
                                }
                            }
                        }

                        // Mark messages as read
                        markMessagesAsRead()
                    }
                } catch (e: Exception) {
                    if (!isActive || !isAdded || _binding == null) return@launch
                    // Only show error if it's not a cancellation
                    if (e.message?.contains("cancelled", ignoreCase = true) != true) {
                        showError("Error loading messages: ${e.message}")
                    }
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
                try {
                    if (!isActive || !isAdded || _binding == null) return@launch

                    val success = chatRepository.sendMessage(
                        room.id, message,
                        MessageType.TEXT.toString()
                    )
                    if (!success && isActive && isAdded && _binding != null) {
                        showError("Gagal mengirim pesan")
                    }
                } catch (e: Exception) {
                    if (!isActive || !isAdded || _binding == null) return@launch
                    // Only show error if it's not a cancellation
                    if (e.message?.contains("cancelled", ignoreCase = true) != true) {
                        showError("Error sending message: ${e.message}")
                    }
                }
            }
        }
    }

    private fun closeChatRoom() {
        currentChatRoom?.let { room ->
            lifecycleScope.launch {
                try {
                    if (!isActive || !isAdded || _binding == null) return@launch

                    chatRepository.closeChatRoom(room.id)

                    // Reset UI
                    currentChatRoom = null
                    binding.btnCloseChat.visibility = View.GONE
                    showChatInterface(false)
//                    chatAdapter.submitList(emptyList())

                    if (isActive && isAdded && _binding != null) {
                        Toast.makeText(context, "Chat room ditutup", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    if (!isActive || !isAdded || _binding == null) return@launch
                    // Only show error if it's not a cancellation
                    if (e.message?.contains("cancelled", ignoreCase = true) != true) {
                        showError("Error closing chat room: ${e.message}")
                    }
                }
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
            try {
                delay(SEARCH_DEBOUNCE) // Debounce search

                if (!isActive || !isAdded || _binding == null) return@launch

                currentChatRoom?.let { room ->
                    val searchResults = chatRepository.searchMessages(room.id, query)
                    if (isActive && isAdded && _binding != null) {
                        chatAdapter.submitList(searchResults)
                    }
                }
            } catch (e: Exception) {
                if (!isActive || !isAdded || _binding == null) return@launch
                // Only show error if it's not a cancellation
                if (e.message?.contains("cancelled", ignoreCase = true) != true) {
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
                try {
                    if (!isActive || !isAdded || _binding == null) return@launch
                    chatRepository.setTypingStatus(room.id, true)
                } catch (e: Exception) {
                    // Silently handle typing status errors
                }
            }

            // Set typing status to false after 2 seconds of inactivity
            typingJob = lifecycleScope.launch {
                try {
                    delay(TYPING_DELAY)
                    if (!isActive || !isAdded || _binding == null) return@launch
                    chatRepository.setTypingStatus(room.id, false)
                } catch (e: Exception) {
                    // Silently handle typing status errors
                }
            }
        }
    }

    private fun markMessagesAsRead() {
        currentChatRoom?.let { room ->
            lifecycleScope.launch {
                try {
                    if (!isActive || !isAdded || _binding == null) return@launch
                    // Admin marks messages as read with current user ID
                    com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid?.let { adminId ->
                        chatRepository.markMessagesAsRead(room.id, adminId)
                    }
                } catch (e: Exception) {
                    // Silently handle read status errors
                }
            }
        }
    }

    private fun showChatInterface(show: Boolean) {
        if (_binding == null) return
        binding.layoutChatInterface.visibility = if (show) View.VISIBLE else View.GONE
        binding.layoutChatRoomsList.visibility = if (show) View.GONE else View.VISIBLE
    }

    private fun showLoading(show: Boolean) {
        if (_binding == null) return
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }

    private fun showError(message: String) {
        if (_binding == null || !isAdded) return
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Stop typing indicator when leaving
        currentChatRoom?.let { room ->
            lifecycleScope.launch {
                try {
                    chatRepository.setTypingStatus(room.id, false)
                } catch (e: Exception) {
                    // Silently handle typing status errors
                }
            }
        }

        // Cancel all jobs
        typingJob?.cancel()
        searchJob?.cancel()
        loadingJob?.cancel()
        messagesJob?.cancel()
        _binding = null
    }
}