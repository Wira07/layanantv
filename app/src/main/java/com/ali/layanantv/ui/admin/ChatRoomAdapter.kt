package com.ali.layanantv.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.data.model.ChatRoom
import com.ali.layanantv.databinding.ItemChatRoomAdminBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomAdapter(
    private val onItemClick: (ChatRoom) -> Unit
) : ListAdapter<ChatRoom, ChatRoomAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomAdminBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ChatRoomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ChatRoomViewHolder(
        private val binding: ItemChatRoomAdminBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoom) {
            binding.apply {
                tvUserName.text = chatRoom.userName
                tvLastMessage.text = when {
                    chatRoom.lastMessage.isNotEmpty() -> chatRoom.lastMessage
                    else -> "Belum ada pesan"
                }
                tvTimestamp.text = formatTimestamp(chatRoom.lastMessageTime.toDate())

                // Show unread count
                if (chatRoom.unreadCount > 0) {
                    tvUnreadCount.visibility = View.VISIBLE
                    tvUnreadCount.text = chatRoom.unreadCount.toString()
                } else {
                    tvUnreadCount.visibility = View.GONE
                }

                // Set click listener
                root.setOnClickListener {
                    onItemClick(chatRoom)
                }
            }
        }
    }

    private fun formatTimestamp(date: Date): String {
        val now = Date()
        val diff = now.time - date.time

        return when {
            diff < 60000 -> "Baru saja" // Less than 1 minute
            diff < 3600000 -> "${diff / 60000} menit yang lalu" // Less than 1 hour
            diff < 86400000 -> SimpleDateFormat("HH:mm", Locale.getDefault()).format(date) // Same day
            else -> SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(date) // Different day
        }
    }
}

class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
    override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
        return oldItem == newItem
    }
}