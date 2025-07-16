package com.ali.layanantv.ui.admin

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.R
import com.ali.layanantv.data.model.PaymentProof
import com.ali.layanantv.data.repository.PaymentRepository
import com.ali.layanantv.databinding.ActivityPaymentProofDetailBinding
import kotlinx.coroutines.launch

class PaymentProofDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPaymentProofDetailBinding
    private lateinit var paymentRepository: PaymentRepository
    private var paymentProof: PaymentProof? = null

    private val paymentProofId by lazy {
        intent.getStringExtra(EXTRA_PAYMENT_PROOF_ID) ?: ""
    }

    companion object {
        const val EXTRA_PAYMENT_PROOF_ID = "extra_payment_proof_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentProofDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        paymentRepository = PaymentRepository()

        setupUI()
        setupClickListeners()
        loadPaymentProofDetail()
    }

    private fun setupUI() {
        supportActionBar?.apply {
            title = "Detail Bukti Pembayaran"
            setDisplayHomeAsUpEnabled(true)
        }
    }

    private fun setupClickListeners() {
        binding.btnApprove.setOnClickListener {
            paymentProof?.let { proof ->
                showApprovalDialog(proof)
            }
        }

        binding.btnReject.setOnClickListener {
            paymentProof?.let { proof ->
                showRejectionDialog(proof)
            }
        }

        binding.ivPaymentProof.setOnClickListener {
            paymentProof?.let { proof ->
//                showFullScreenImage(proof)
            }
        }
    }

    private fun loadPaymentProofDetail() {
        lifecycleScope.launch {
            try {
                val proof = paymentRepository.getPaymentProofById(paymentProofId)
                if (proof != null) {
                    paymentProof = proof
                    displayPaymentProofDetail(proof)
                } else {
                    Toast.makeText(this@PaymentProofDetailActivity,
                        "Data bukti pembayaran tidak ditemukan", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentProofDetailActivity,
                    "Gagal memuat data: ${e.message}", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun displayPaymentProofDetail(proof: PaymentProof) {
        with(binding) {
            // Basic information
            tvPaymentId.text = proof.id
            tvOrderId.text = proof.orderId
            tvUploadDate.text = proof.getFormattedUploadDate()
            tvStatus.text = proof.getStatusText()

            // Status color
            val statusColor = Color.parseColor(proof.getStatusColor())
            tvStatus.setTextColor(statusColor)

            // Additional notes from user
            if (proof.additionalNotes.isNotEmpty()) {
                tvAdditionalNotes.text = proof.additionalNotes
                layoutAdditionalNotes.visibility = View.VISIBLE
            } else {
                layoutAdditionalNotes.visibility = View.GONE
            }

            // Admin verification info
            if (proof.adminNotes.isNotEmpty()) {
                tvAdminNotes.text = proof.adminNotes
                tvVerifiedBy.text = proof.verifiedByAdmin
                tvVerificationDate.text = proof.getFormattedVerificationDate() ?: ""
                layoutAdminInfo.visibility = View.VISIBLE
            } else {
                layoutAdminInfo.visibility = View.GONE
            }

            // Load payment proof image
            try {
                val imageUri = Uri.parse(proof.imageUri)
                val bitmap = if (proof.imagePath != null) {
                    BitmapFactory.decodeFile(proof.imagePath)
                } else {
                    val inputStream = contentResolver.openInputStream(imageUri)
                    BitmapFactory.decodeStream(inputStream)
                }

                ivPaymentProof.setImageBitmap(bitmap)
            } catch (e: Exception) {
                ivPaymentProof.setImageResource(R.drawable.ic_image_placeholder)
                Toast.makeText(this@PaymentProofDetailActivity,
                    "Gagal memuat gambar bukti pembayaran", Toast.LENGTH_SHORT).show()
            }

            // Admin action buttons
            if (proof.isPending()) {
                layoutAdminActions.visibility = View.VISIBLE
            } else {
                layoutAdminActions.visibility = View.GONE
            }
        }
    }

    private fun showApprovalDialog(paymentProof: PaymentProof) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_approval_notes, null)
        val etNotes = dialogView.findViewById<android.widget.EditText>(R.id.etNotes)

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
        val etNotes = dialogView.findViewById<android.widget.EditText>(R.id.etNotes)

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
                    Toast.makeText(this@PaymentProofDetailActivity,
                        "Pembayaran berhasil disetujui", Toast.LENGTH_SHORT).show()
                    loadPaymentProofDetail()
                } else {
                    Toast.makeText(this@PaymentProofDetailActivity,
                        "Gagal menyetujui pembayaran", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentProofDetailActivity,
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
                    Toast.makeText(this@PaymentProofDetailActivity,
                        "Pembayaran berhasil ditolak", Toast.LENGTH_SHORT).show()
                    loadPaymentProofDetail()
                } else {
                    Toast.makeText(this@PaymentProofDetailActivity,
                        "Gagal menolak pembayaran", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentProofDetailActivity,
                    "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

//    private fun showFullScreenImage(paymentProof: PaymentProof) {
//        val intent = Intent(this, FullScreenImageActivity::class.java)
//        intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_URI, paymentProof.imageUri)
//        intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_PATH, paymentProof.imagePath)
//        startActivity(intent)
//    }

//    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
//        menuInflater.inflate(R.menu.menu_payment_detail, menu)
//        return true
//    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        return when (item.itemId) {
//            R.id.action_refresh -> {
//                loadPaymentProofDetail()
//                true
//            }
//            R.id.action_delete -> {
//                showDeleteConfirmation()
//                true
//            }
//            android.R.id.home -> {
//                onBackPressed()
//                true
//            }
//            else -> super.onOptionsItemSelected(item)
//        }
//    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Hapus Bukti Pembayaran")
            .setMessage("Apakah Anda yakin ingin menghapus bukti pembayaran ini?")
            .setPositiveButton("Hapus") { _, _ ->
                deletePaymentProof()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deletePaymentProof() {
        lifecycleScope.launch {
            try {
                val success = paymentRepository.deletePaymentProof(paymentProofId)

                if (success) {
                    Toast.makeText(this@PaymentProofDetailActivity,
                        "Bukti pembayaran berhasil dihapus", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@PaymentProofDetailActivity,
                        "Gagal menghapus bukti pembayaran", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@PaymentProofDetailActivity,
                    "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}