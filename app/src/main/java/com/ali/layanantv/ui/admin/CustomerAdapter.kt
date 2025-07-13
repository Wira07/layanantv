package com.ali.layanantv.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.data.model.User
import com.ali.layanantv.databinding.ItemCustomerBinding

class CustomerAdapter(
    private val onItemClick: (User) -> Unit,
    private val onToggleStatus: (User) -> Unit
) : ListAdapter<User, CustomerAdapter.CustomerViewHolder>(CustomerDiffCallback()) {

    inner class CustomerViewHolder(private val binding: ItemCustomerBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvCustomerName.text = user.name
                tvCustomerEmail.text = user.email
                tvCustomerPhone.text = user.phoneNumber ?: "-"
                switchStatus.isChecked = user.isActive

                // Set click listener for the whole item
                root.setOnClickListener {
                    onItemClick(user)
                }

                // Set click listener for the switch
                switchStatus.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != user.isActive) {
                        onToggleStatus(user)
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomerViewHolder {
        val binding = ItemCustomerBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CustomerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CustomerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class CustomerDiffCallback : DiffUtil.ItemCallback<User>() {
    override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem.uid == newItem.uid
    }

    override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
        return oldItem == newItem
    }
}