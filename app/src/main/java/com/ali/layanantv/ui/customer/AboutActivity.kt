package com.ali.layanantv.ui.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.ali.layanantv.R
import com.ali.layanantv.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        // Setup toolbar
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        // Set app version
        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            binding.tvVersion.text = "Versi ${packageInfo.versionName}"
        } catch (e: Exception) {
            binding.tvVersion.text = "Versi 1.0.0"
        }
    }

    private fun setupClickListeners() {
        // Contact us
        binding.layoutContact.setOnClickListener {
            val intent = Intent(Intent.ACTION_DIAL)
            intent.data = Uri.parse("tel:+628123456789")
            startActivity(intent)
        }

        // Email
        binding.layoutEmail.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:support@layanantv.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "Pertanyaan Layanan TV")
            startActivity(intent)
        }

        // Website
        binding.layoutWebsite.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.layanantv.com")
            startActivity(intent)
        }

        // Privacy Policy
        binding.layoutPrivacy.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.layanantv.com/privacy")
            startActivity(intent)
        }

        // Terms of Service
        binding.layoutTerms.setOnClickListener {
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://www.layanantv.com/terms")
            startActivity(intent)
        }

        // Rate app
        binding.layoutRate.setOnClickListener {
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("market://details?id=$packageName")
                startActivity(intent)
            } catch (e: Exception) {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                startActivity(intent)
            }
        }
    }
}