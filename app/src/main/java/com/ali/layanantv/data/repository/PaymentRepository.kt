package com.ali.layanantv.data.repository

import android.content.Context
import android.net.Uri
import com.ali.layanantv.data.model.PaymentProof
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

class PaymentRepository {

    companion object {
        // Use a singleton pattern to ensure data persistence across activities
        private val paymentProofs = mutableListOf<PaymentProof>()

        // Add some sample data for testing (remove in production)
        init {
            // Add sample payment proofs for testing
            paymentProofs.add(
                PaymentProof(
                    id = "proof_sample_001",
                    orderId = "ORDER_001",
                    userId = "user_001",
                    imageUri = "content://sample/image1.jpg",
                    imagePath = "/storage/sample/image1.jpg",
                    additionalNotes = "Pembayaran melalui QRIS BCA",
                    uploadTimestamp = System.currentTimeMillis() - 3600000, // 1 hour ago
                    status = "pending_verification"
                )
            )

            paymentProofs.add(
                PaymentProof(
                    id = "proof_sample_002",
                    orderId = "ORDER_002",
                    userId = "user_002",
                    imageUri = "content://sample/image2.jpg",
                    imagePath = "/storage/sample/image2.jpg",
                    additionalNotes = "Transfer via mobile banking",
                    uploadTimestamp = System.currentTimeMillis() - 7200000, // 2 hours ago
                    status = "approved",
                    adminNotes = "Pembayaran valid dan sesuai",
                    verificationTimestamp = System.currentTimeMillis() - 3600000,
                    verifiedByAdmin = "Admin"
                )
            )
        }
    }

    suspend fun submitPaymentProof(paymentProof: PaymentProof): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // Simulate network delay
                kotlinx.coroutines.delay(1000)

                // Add to storage
                paymentProofs.add(paymentProof)

                // Log for debugging
                println("PaymentRepository: Payment proof submitted successfully")
                println("PaymentRepository: Total proofs: ${paymentProofs.size}")
                println("PaymentRepository: Latest proof ID: ${paymentProof.id}")

                // Return success
                true
            } catch (e: Exception) {
                println("PaymentRepository: Error submitting proof: ${e.message}")
                false
            }
        }
    }

    suspend fun getAllPaymentProofs(): List<PaymentProof> {
        return withContext(Dispatchers.IO) {
            // Simulate network delay
            kotlinx.coroutines.delay(500)

            println("PaymentRepository: Getting all payment proofs, count: ${paymentProofs.size}")

            paymentProofs.sortedByDescending { it.uploadTimestamp }
        }
    }

    suspend fun getPaymentProofsByStatus(status: String): List<PaymentProof> {
        return withContext(Dispatchers.IO) {
            // Simulate network delay
            kotlinx.coroutines.delay(500)

            val filtered = paymentProofs.filter { it.status == status }
                .sortedByDescending { it.uploadTimestamp }

            println("PaymentRepository: Getting proofs by status '$status', count: ${filtered.size}")

            filtered
        }
    }

    suspend fun getPaymentProofById(id: String): PaymentProof? {
        return withContext(Dispatchers.IO) {
            val proof = paymentProofs.find { it.id == id }
            println("PaymentRepository: Getting proof by ID '$id', found: ${proof != null}")
            proof
        }
    }

    suspend fun updatePaymentProofStatus(
        id: String,
        status: String,
        adminNotes: String = "",
        verifiedByAdmin: String = ""
    ): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val index = paymentProofs.indexOfFirst { it.id == id }
                if (index != -1) {
                    val updatedProof = paymentProofs[index].copy(
                        status = status,
                        adminNotes = adminNotes,
                        verificationTimestamp = System.currentTimeMillis(),
                        verifiedByAdmin = verifiedByAdmin
                    )
                    paymentProofs[index] = updatedProof

                    println("PaymentRepository: Updated proof status - ID: $id, Status: $status")
                    true
                } else {
                    println("PaymentRepository: Proof not found for ID: $id")
                    false
                }
            } catch (e: Exception) {
                println("PaymentRepository: Error updating proof status: ${e.message}")
                false
            }
        }
    }

    suspend fun deletePaymentProof(id: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val removed = paymentProofs.removeIf { it.id == id }
                println("PaymentRepository: Deleted proof ID: $id, success: $removed")
                removed
            } catch (e: Exception) {
                println("PaymentRepository: Error deleting proof: ${e.message}")
                false
            }
        }
    }

    suspend fun getPaymentProofsByOrderId(orderId: String): List<PaymentProof> {
        return withContext(Dispatchers.IO) {
            val filtered = paymentProofs.filter { it.orderId == orderId }
                .sortedByDescending { it.uploadTimestamp }

            println("PaymentRepository: Getting proofs by order ID '$orderId', count: ${filtered.size}")

            filtered
        }
    }

    // Helper method to copy file to internal storage
    private suspend fun copyFileToInternalStorage(context: Context, uri: Uri): String? {
        return withContext(Dispatchers.IO) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val fileName = "payment_proof_${System.currentTimeMillis()}.jpg"
                val outputFile = File(context.filesDir, fileName)

                inputStream?.use { input ->
                    FileOutputStream(outputFile).use { output ->
                        input.copyTo(output)
                    }
                }

                outputFile.absolutePath
            } catch (e: Exception) {
                println("PaymentRepository: Error copying file: ${e.message}")
                null
            }
        }
    }

    // Get statistics for admin dashboard
    suspend fun getPaymentStatistics(): PaymentStatistics {
        return withContext(Dispatchers.IO) {
            val total = paymentProofs.size
            val pending = paymentProofs.count { it.status == "pending_verification" }
            val approved = paymentProofs.count { it.status == "approved" }
            val rejected = paymentProofs.count { it.status == "rejected" }

            println("PaymentRepository: Statistics - Total: $total, Pending: $pending, Approved: $approved, Rejected: $rejected")

            PaymentStatistics(
                totalPayments = total,
                pendingPayments = pending,
                approvedPayments = approved,
                rejectedPayments = rejected
            )
        }
    }
}

data class PaymentStatistics(
    val totalPayments: Int,
    val pendingPayments: Int,
    val approvedPayments: Int,
    val rejectedPayments: Int
)