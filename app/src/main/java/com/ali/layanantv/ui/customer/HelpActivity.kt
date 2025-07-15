package com.ali.layanantv.ui.customer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.R
import com.ali.layanantv.databinding.ActivityHelpBinding

class HelpActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHelpBinding
    private lateinit var helpAdapter: HelpAdapter
    private val helpItems = mutableListOf<HelpItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityHelpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupRecyclerView()
        loadHelpItems()

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun setupUI() {
        // Setup toolbar
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Pusat Bantuan"
        }

        // Setup contact buttons
        binding.btnContactWhatsapp.setOnClickListener {
            openWhatsApp()
        }

        binding.btnContactEmail.setOnClickListener {
            openEmail()
        }

        binding.btnContactPhone.setOnClickListener {
            openPhone()
        }
    }

    private fun setupRecyclerView() {
        helpAdapter = HelpAdapter(helpItems) { helpItem ->
            // Handle item click - expand/collapse
            helpItem.isExpanded = !helpItem.isExpanded
            helpAdapter.notifyItemChanged(helpItems.indexOf(helpItem))
        }

        binding.recyclerViewHelp.apply {
            layoutManager = LinearLayoutManager(this@HelpActivity)
            adapter = helpAdapter
        }
    }

    private fun loadHelpItems() {
        helpItems.clear()
        helpItems.addAll(getHelpData())
        helpAdapter.notifyDataSetChanged()
    }

    private fun getHelpData(): List<HelpItem> {
        return listOf(
            HelpItem(
                "🔐 Akun & Login",
                "Masalah terkait akun dan masuk aplikasi",
                listOf(
                    HelpSubItem("Lupa password", "Gunakan fitur 'Lupa Password' di halaman login atau hubungi customer service"),
                    HelpSubItem("Tidak bisa login", "Pastikan email dan password benar. Coba reset password jika perlu"),
                    HelpSubItem("Akun terkunci", "Hubungi customer service untuk membuka kunci akun"),
                    HelpSubItem("Ganti password", "Masuk ke Profil > Keamanan > Ganti Password")
                )
            ),
            HelpItem(
                "📺 Langganan Channel",
                "Informasi tentang berlangganan channel TV",
                listOf(
                    HelpSubItem("Cara berlangganan", "Pilih channel di halaman utama > Klik 'Berlangganan' > Pilih paket > Bayar"),
                    HelpSubItem("Jenis paket langganan", "Tersedia paket bulanan, 3 bulan, 6 bulan, dan tahunan"),
                    HelpSubItem("Pembatalan langganan", "Masuk ke Profil > Langganan Saya > Pilih channel > Batalkan"),
                    HelpSubItem("Perpanjang langganan", "Langganan akan diperpanjang otomatis atau manual melalui aplikasi")
                )
            ),
            HelpItem(
                "💳 Pembayaran",
                "Cara pembayaran dan masalah transaksi",
                listOf(
                    HelpSubItem("Metode pembayaran", "Kami menerima transfer bank, e-wallet, dan kartu kredit"),
                    HelpSubItem("Pembayaran gagal", "Cek saldo rekening atau hubungi bank/penyedia e-wallet Anda"),
                    HelpSubItem("Refund", "Refund diproses 3-7 hari kerja setelah persetujuan"),
                    HelpSubItem("Bukti pembayaran", "Simpan bukti pembayaran untuk keperluan verifikasi")
                )
            ),
            HelpItem(
                "📱 Penggunaan Aplikasi",
                "Panduan menggunakan fitur-fitur aplikasi",
                listOf(
                    HelpSubItem("Navigasi aplikasi", "Gunakan menu di bawah untuk berpindah antar halaman"),
                    HelpSubItem("Cari channel", "Gunakan fitur pencarian di halaman utama"),
                    HelpSubItem("Riwayat pesanan", "Lihat riwayat di menu Profil > Riwayat Pesanan"),
                    HelpSubItem("Update aplikasi", "Pastikan selalu menggunakan versi terbaru dari Play Store")
                )
            ),
            HelpItem(
                "🔧 Masalah Teknis",
                "Solusi untuk masalah teknis yang umum terjadi",
                listOf(
                    HelpSubItem("Aplikasi lambat", "Tutup aplikasi lain yang tidak digunakan dan restart aplikasi"),
                    HelpSubItem("Tidak bisa streaming", "Periksa koneksi internet dan coba refresh aplikasi"),
                    HelpSubItem("Video buffering", "Gunakan koneksi WiFi yang stabil atau kurangi kualitas video"),
                    HelpSubItem("Crash aplikasi", "Restart aplikasi atau reinstall jika masalah berlanjut")
                )
            ),
            HelpItem(
                "🎯 Fitur Khusus",
                "Informasi tentang fitur-fitur khusus aplikasi",
                listOf(
                    HelpSubItem("Sistem poin", "Dapatkan poin setiap berlangganan dan tukar dengan diskon"),
                    HelpSubItem("Notifikasi", "Aktifkan notifikasi untuk info promo dan pengingat pembayaran"),
                    HelpSubItem("Favorit channel", "Tandai channel favorit untuk akses cepat"),
                    HelpSubItem("Parental control", "Atur pembatasan konten untuk anak-anak")
                )
            ),
            HelpItem(
                "📞 Kontak & Dukungan",
                "Cara menghubungi tim customer service",
                listOf(
                    HelpSubItem("Jam operasional", "Senin-Jumat: 08:00-17:00 WIB, Sabtu: 08:00-12:00 WIB"),
                    HelpSubItem("WhatsApp", "Chat langsung melalui WhatsApp di +62 812-3456-7890"),
                    HelpSubItem("Email", "Kirim email ke support@layanantv.com"),
                    HelpSubItem("Telepon", "Hubungi (021) 1234-5678 untuk bantuan langsung")
                )
            )
        )
    }

    private fun openWhatsApp() {
        try {
            val phoneNumber = "+6281234567890" // Ganti dengan nomor WhatsApp yang sesuai
            val message = "Halo, saya butuh bantuan mengenai aplikasi Layanan TV"
            val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://wa.me/$phoneNumber?text=${Uri.encode(message)}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "WhatsApp tidak terinstall", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEmail() {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@layanantv.com")
                putExtra(Intent.EXTRA_SUBJECT, "Bantuan Aplikasi Layanan TV")
                putExtra(Intent.EXTRA_TEXT, "Halo, saya butuh bantuan mengenai:\n\n")
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak ada aplikasi email yang tersedia", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openPhone() {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:02112345678") // Ganti dengan nomor telepon yang sesuai
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak dapat membuka dialer", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}

// Data classes for help items
data class HelpItem(
    val title: String,
    val description: String,
    val subItems: List<HelpSubItem>,
    var isExpanded: Boolean = false
)

data class HelpSubItem(
    val question: String,
    val answer: String
)