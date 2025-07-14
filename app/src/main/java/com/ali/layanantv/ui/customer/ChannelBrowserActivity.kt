package com.ali.layanantv.ui.customer

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.databinding.ActivityChannelBrowserBinding
import kotlinx.coroutines.launch

class ChannelBrowserActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChannelBrowserBinding
    private lateinit var customerRepository: CustomerRepository
    private lateinit var channelBrowserAdapter: ChannelBrowserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChannelBrowserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerRepository = CustomerRepository()
        setupUI()
        loadChannels()
    }

    private fun setupUI() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pilih Channel"

        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        // Setup RecyclerView
        channelBrowserAdapter = ChannelBrowserAdapter { channel ->
            subscribeToChannel(channel)
        }

        binding.rvChannels.apply {
            layoutManager = GridLayoutManager(this@ChannelBrowserActivity, 2)
            adapter = channelBrowserAdapter
        }
    }

    private fun loadChannels() {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE
                val channels = customerRepository.getAvailableChannels()

                if (channels.isEmpty()) {
                    binding.layoutEmpty.visibility = View.VISIBLE
                    binding.rvChannels.visibility = View.GONE
                } else {
                    binding.layoutEmpty.visibility = View.GONE
                    binding.rvChannels.visibility = View.VISIBLE
                    channelBrowserAdapter.submitList(channels)
                }

                binding.progressBar.visibility = View.GONE
            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this@ChannelBrowserActivity, "Error loading channels: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun subscribeToChannel(channel: Channel) {
        // TODO: Implement subscription logic
        Toast.makeText(this, "Berlangganan ${channel.name}", Toast.LENGTH_SHORT).show()
    }
}