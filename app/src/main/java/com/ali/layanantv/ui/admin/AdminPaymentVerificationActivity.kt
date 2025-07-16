package com.ali.layanantv.ui.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.ali.layanantv.R
import com.ali.layanantv.data.model.PaymentProof
import com.ali.layanantv.data.repository.PaymentRepository
import com.ali.layanantv.data.repository.PaymentStatistics
import com.ali.layanantv.databinding.ActivityAdminPaymentVerificationBinding
import com.ali.layanantv.ui.adapter.PaymentProofAdapter
import kotlinx.coroutines.launch

class AdminPaymentVerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminPaymentVerificationBinding
    private lateinit var paymentRepository: PaymentRepository
    private lateinit var paymentProofAdapter: PaymentProofAdapter

    private var currentFilter = "all" // all, pending, approved, rejected
    private var paymentProofs = mutableListOf<PaymentProof>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminPaymentVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        paymentRepository = PaymentRepository()

        setupUI()
        setupRecyclerView()
        setupClickListeners()
        loadPaymentProofs()
        loadStatistics()
    }

    private fun setupUI() {
        supportActionBar?.apply {
            title = "Verifikasi Pembayaran"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupRecyclerView() {
        paymentProofAdapter = PaymentProofAdapter(
            paymentProofs = paymentProofs,
            onItemClick = { paymentProof ->
                showPaymentProofDetail(paymentProof)
            },
            onApproveClick = { paymentProof ->
                showApprovalDialog(paymentProof)
            },
            onRejectClick = { paymentProof ->
                showRejectionDialog(paymentProof)
            }
        )

        binding.rvPaymentProofs.apply {
            layoutManager = LinearLayoutManager(this@AdminPaymentVerificationActivity)
            adapter = paymentProofAdapter
        }
    }

    private fun setupClickListeners() {
        binding.chipAll.setOnClickListener {
            filterPayments("all")
        }

        binding.chipPending.setOnClickListener {
            filterPayments("pending_verification")
        }

        binding.chipApproved.setOnClickListener {
            filterPayments("approved")
        }

        binding.chipRejected.setOnClickListener {
            filterPayments("rejected")
        }

        binding.swipeRefresh.setOnRefreshListener {
            loadPaymentProofs()
            loadStatistics()
        }
    }

    private fun loadPaymentProofs() {
        binding.swipeRefresh.isRefreshing = true

        lifecycleScope.launch {
            try {
                val proofs = paymentRepository.getAllPaymentProofs()
                paymentProofs.clear()
                paymentProofs.addAll(proofs)

                filterPayments(currentFilter)

                binding.swipeRefresh.isRefreshing = false

                if (proofs.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                }

            } catch (e: Exception) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(this@AdminPaymentVerificationActivity,
                    "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadStatistics() {
        lifecycleScope.launch {
            try {
                val stats = paymentRepository.getPaymentStatistics()
                updateStatisticsUI(stats)
            } catch (e: Exception) {
                // Handle error silently for statistics
            }
        }
    }

    private fun updateStatisticsUI(stats: PaymentStatistics) {
        binding.tvTotalPayments.text = stats.totalPayments.toString()
        binding.tvPendingPayments.text = stats.pendingPayments.toString()
        binding.tvApprovedPayments.text = stats.approvedPayments.toString()
        binding.tvRejectedPayments.text = stats.rejectedPayments.toString()
    }

    private fun filterPayments(filter: String) {
        currentFilter = filter

        val filteredProofs = when (filter) {
            "pending_verification" -> paymentProofs.filter { it.status == "pending_verification" }
            "approved" -> paymentProofs.filter { it.status == "approved" }
            "rejected" -> paymentProofs.filter { it.status == "rejected" }
            else -> paymentProofs
        }

        paymentProofAdapter.updateData(filteredProofs)

        // Update chip selection
        binding.chipAll.isChecked = filter == "all"
        binding.chipPending.isChecked = filter == "pending_verification"
        binding.chipApproved.isChecked = filter == "approved"
        binding.chipRejected.isChecked = filter == "rejected"

        if (filteredProofs.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    private fun showPaymentProofDetail(paymentProof: PaymentProof) {
        val intent = Intent(this, PaymentProofDetailActivity::class.java)
        intent.putExtra(PaymentProofDetailActivity.EXTRA_PAYMENT_PROOF_ID, paymentProof.id)
        startActivity(intent)
    }

    private fun showApprovalDialog(paymentProof: PaymentProof) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_approval_notes, null)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        AlertDialog.Builder(this)
            .setTitle("Setujui Pembayaran")
            .setMessage("Apakah Anda yakin ingin menyetujui pembayaran ini?")
            .setView(dialogView)
            .setPositiveButton("Setujui") { _, _ ->
                val notes = etNotes.text.toString().trim()
                approvePayment(paymentProof, notes)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun showRejectionDialog(paymentProof: PaymentProof) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_rejection_notes, null)
        val etNotes = dialogView.findViewById<EditText>(R.id.etNotes)

        AlertDialog.Builder(this)
            .setTitle("Tolak Pembayaran")
            .setMessage("Berikan alasan penolakan pembayaran:")
            .setView(dialogView)
            .setPositiveButton("Tolak") { _, _ ->
                val notes = etNotes.text.toString().trim()
                if (notes.isNotEmpty()) {
                    rejectPayment(paymentProof, notes)
                } else {
                    Toast.makeText(this, "Alasan penolakan wajib diisi", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun approvePayment(paymentProof: PaymentProof, notes: String) {
        lifecycleScope.launch {
            try {
                val success = paymentRepository.updatePaymentProofStatus(
                    id = paymentProof.id,
                    status = "approved",
                    adminNotes = notes,
                    verifiedByAdmin = "Admin" // In real app, get from user session
                )

                if (success) {
                    Toast.makeText(this@AdminPaymentVerificationActivity,
                        "Pembayaran berhasil disetujui", Toast.LENGTH_SHORT).show()
                    loadPaymentProofs()
                    loadStatistics()
                } else {
                    Toast.makeText(this@AdminPaymentVerificationActivity,
                        "Gagal menyetujui pembayaran", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminPaymentVerificationActivity,
                    "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun rejectPayment(paymentProof: PaymentProof, notes: String) {
        lifecycleScope.launch {
            try {
                val success = paymentRepository.updatePaymentProofStatus(
                    id = paymentProof.id,
                    status = "rejected",
                    adminNotes = notes,
                    verifiedByAdmin = "Admin" // In real app, get from user session
                )

                if (success) {
                    Toast.makeText(this@AdminPaymentVerificationActivity,
                        "Pembayaran berhasil ditolak", Toast.LENGTH_SHORT).show()
                    loadPaymentProofs()
                    loadStatistics()
                } else {
                    Toast.makeText(this@AdminPaymentVerificationActivity,
                        "Gagal menolak pembayaran", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminPaymentVerificationActivity,
                    "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyState() {
        binding.rvPaymentProofs.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE

        val message = when (currentFilter) {
            "pending_verification" -> "Tidak ada pembayaran yang menunggu verifikasi"
            "approved" -> "Tidak ada pembayaran yang disetujui"
            "rejected" -> "Tidak ada pembayaran yang ditolak"
            else -> "Tidak ada data pembayaran"
        }

        binding.tvEmptyMessage.text = message
    }

    private fun hideEmptyState() {
        binding.rvPaymentProofs.visibility = View.VISIBLE
        binding.layoutEmpty.visibility = View.GONE
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_admin_payment, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                loadPaymentProofs()
                loadStatistics()
                true
            }
            R.id.action_search -> {
                showSearchDialog()
                true
            }
            android.R.id.home -> {
                onBackPressed()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSearchDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_search_payment, null)
        val etSearch = dialogView.findViewById<EditText>(R.id.etSearch)

        AlertDialog.Builder(this)
            .setTitle("Cari Pembayaran")
            .setView(dialogView)
            .setPositiveButton("Cari") { _, _ ->
                val query = etSearch.text.toString().trim()
                if (query.isNotEmpty()) {
                    searchPayments(query)
                }
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun searchPayments(query: String) {
        val filteredProofs = paymentProofs.filter { proof ->
            proof.orderId.contains(query, ignoreCase = true) ||
                    proof.id.contains(query, ignoreCase = true) ||
                    proof.additionalNotes.contains(query, ignoreCase = true)
        }

        paymentProofAdapter.updateData(filteredProofs)

        if (filteredProofs.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
        }
    }

    override fun onResume() {
        super.onResume()
        loadPaymentProofs()
        loadStatistics()
    }
}