package com.ali.layanantv.ui.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.repository.AdminRepository
import com.ali.layanantv.databinding.ActivityChannelManagementBinding
import com.ali.layanantv.databinding.DialogAddChannelBinding
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

class ChannelManagementActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelManagementBinding
    private lateinit var adminRepository: AdminRepository
    private lateinit var channelAdapter: ChannelAdapter
    private var selectedImageUri: Uri? = null
    private var editingChannel: Channel? = null
    private var currentDialog: AlertDialog? = null
    private var currentDialogBinding: DialogAddChannelBinding? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            selectedImageUri = result.data?.data
            // Show preview in dialog if it's open
            currentDialogBinding?.let { dialogBinding ->
                selectedImageUri?.let { uri ->
                    Glide.with(this)
                        .load(uri)
                        .into(dialogBinding.ivChannelLogo)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adminRepository = AdminRepository()
        setupUI()
        loadChannels()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Kelola Channel TV"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView
        channelAdapter = ChannelAdapter(
            onEditClick = { channel ->
                showAddEditChannelDialog(channel)
            },
            onDeleteClick = { channel ->
                showDeleteConfirmationDialog(channel)
            }
        )

        binding.rvChannels.apply {
            layoutManager = LinearLayoutManager(this@ChannelManagementActivity)
            adapter = channelAdapter
        }

        // FAB for adding new channel
        binding.fabAddChannel.setOnClickListener {
            showAddEditChannelDialog()
        }
    }

    private fun loadChannels() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val channels = adminRepository.getAllChannels()
                channelAdapter.submitList(channels)
                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ChannelManagementActivity, "Error loading channels: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddEditChannelDialog(channel: Channel? = null) {
        val dialogBinding = DialogAddChannelBinding.inflate(LayoutInflater.from(this))
        editingChannel = channel
        selectedImageUri = null
        currentDialogBinding = dialogBinding

        // Pre-fill data if editing
        channel?.let {
            dialogBinding.etChannelName.setText(it.name)
            dialogBinding.etChannelDescription.setText(it.description)
            dialogBinding.etChannelPrice.setText(it.price.toString())
            dialogBinding.etChannelCategory.setText(it.category)
            dialogBinding.switchChannelActive.isChecked = it.isActive

            // Load existing image - prioritize Base64 over URL
            if (it.logoBase64?.isNotEmpty() == true) {
                // If Base64 exists, use it
                val base64String = if (it.logoBase64.startsWith("data:image")) {
                    it.logoBase64
                } else {
                    "data:image/jpeg;base64,${it.logoBase64}"
                }
                Glide.with(this)
                    .load(base64String)
                    .into(dialogBinding.ivChannelLogo)
            } else if (it.logoUrl.isNotEmpty()) {
                // Fallback to URL
                Glide.with(this)
                    .load(it.logoUrl)
                    .into(dialogBinding.ivChannelLogo)
            }
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (channel == null) "Tambah Channel" else "Edit Channel")
            .setView(dialogBinding.root)
            .setPositiveButton("Simpan", null)
            .setNegativeButton("Batal", null)
            .create()

        currentDialog = dialog

        dialogBinding.btnSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            imagePickerLauncher.launch(intent)
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                saveChannel(dialogBinding, dialog)
            }
        }

        dialog.setOnDismissListener {
            currentDialog = null
            currentDialogBinding = null
        }

        dialog.show()
    }

    private fun saveChannel(dialogBinding: DialogAddChannelBinding, dialog: AlertDialog) {
        val name = dialogBinding.etChannelName.text.toString().trim()
        val description = dialogBinding.etChannelDescription.text.toString().trim()
        val priceStr = dialogBinding.etChannelPrice.text.toString().trim()
        val category = dialogBinding.etChannelCategory.text.toString().trim()
        val isActive = dialogBinding.switchChannelActive.isChecked

        if (name.isEmpty() || description.isEmpty() || priceStr.isEmpty() || category.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceStr.toDoubleOrNull()
        if (price == null || price <= 0) {
            Toast.makeText(this, "Harga harus berupa angka yang valid", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                dialog.dismiss()
                binding.progressBar.visibility = View.VISIBLE

                var logoUrl = editingChannel?.logoUrl ?: ""
                var logoBase64 = editingChannel?.logoBase64 ?: ""

                // Upload new image if selected
                if (selectedImageUri != null) {
                    val channelId = editingChannel?.id ?: System.currentTimeMillis().toString()
                    // Fixed: Correct parameter order (Context, Uri, String)
                    logoBase64 = adminRepository.uploadChannelLogo(this@ChannelManagementActivity, selectedImageUri!!, channelId)
                    logoUrl = "" // Clear URL when using Base64
                }

                val channel = Channel(
                    id = editingChannel?.id ?: "",
                    name = name,
                    description = description,
                    logoUrl = logoUrl,
                    logoBase64 = logoBase64,
                    price = price,
                    category = category,
                    isActive = isActive,
                    createdAt = editingChannel?.createdAt ?: Timestamp.now(),
                    updatedAt = Timestamp.now()
                )

                if (editingChannel == null) {
                    // Add new channel
                    adminRepository.addChannel(channel)
                    Toast.makeText(this@ChannelManagementActivity, "Channel berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                } else {
                    // Update existing channel
                    adminRepository.updateChannel(editingChannel!!.id, channel)
                    Toast.makeText(this@ChannelManagementActivity, "Channel berhasil diupdate", Toast.LENGTH_SHORT).show()
                }

                loadChannels()

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ChannelManagementActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteConfirmationDialog(channel: Channel) {
        AlertDialog.Builder(this)
            .setTitle("Hapus Channel")
            .setMessage("Apakah Anda yakin ingin menghapus channel ${channel.name}?")
            .setPositiveButton("Hapus") { _, _ ->
                deleteChannel(channel)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteChannel(channel: Channel) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                adminRepository.deleteChannel(channel.id)
                Toast.makeText(this@ChannelManagementActivity, "Channel berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadChannels()
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ChannelManagementActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}