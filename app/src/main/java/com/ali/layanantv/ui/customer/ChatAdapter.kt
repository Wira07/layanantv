package com.ali.layanantv.ui.customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.data.model.ChatMessage
import com.ali.layanantv.databinding.ItemChatMessageReceivedBinding
import com.ali.layanantv.databinding.ItemChatMessageSentBinding
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter : ListAdapter<ChatMessage, RecyclerView.ViewHolder>(ChatDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2

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

    override fun getItemViewType(position: Int): Int {
        val message = getItem(position)
        return if (message.senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val binding = ItemChatMessageSentBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                SentMessageViewHolder(binding)
            }
            VIEW_TYPE_RECEIVED -> {
                val binding = ItemChatMessageReceivedBinding.inflate(
                    LayoutInflater.from(parent.context), parent, false
                )
                ReceivedMessageViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = getItem(position)
        when (holder) {
            is SentMessageViewHolder -> holder.bind(message)
            is ReceivedMessageViewHolder -> holder.bind(message)
        }
    }

    class SentMessageViewHolder(private val binding: ItemChatMessageSentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.message
            binding.tvTimestamp.text = formatTimestamp(message.timestamp.toDate())

            // Show read status
            if (message.isRead) {
                binding.ivReadStatus.visibility = View.VISIBLE
            } else {
                binding.ivReadStatus.visibility = View.GONE
            }
        }
    }

    class ReceivedMessageViewHolder(private val binding: ItemChatMessageReceivedBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: ChatMessage) {
            binding.tvMessage.text = message.message
            binding.tvTimestamp.text = formatTimestamp(message.timestamp.toDate())
            binding.tvSenderName.text = message.senderName

            // Show sender role badge
            if (message.senderRole == "ADMIN") {
                binding.tvSenderRole.visibility = View.VISIBLE
                binding.tvSenderRole.text = "Admin"
            } else {
                binding.tvSenderRole.visibility = View.GONE
            }
        }
    }
}

class ChatDiffCallback : DiffUtil.ItemCallback<ChatMessage>() {
    override fun areItemsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: ChatMessage, newItem: ChatMessage): Boolean {
        return oldItem == newItem
    }
}