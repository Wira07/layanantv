package com.ali.layanantv.data.model

import java.text.SimpleDateFormat
import java.util.*

data class PaymentProof(
    val id: String = generateId(),
    val orderId: String,
    val userId: String = "", // Will be set by repository
    val imageUri: String,
    val imagePath: String? = null,
    val additionalNotes: String = "",
    val uploadTimestamp: Long = System.currentTimeMillis(),
    val status: String = "pending_verification", // pending_verification, approved, rejected
    val adminNotes: String = "",
    val verificationTimestamp: Long? = null,
    val verifiedByAdmin: String = ""
) {

    companion object {
        private fun generateId(): String {
            return "proof_${System.currentTimeMillis()}_${(1000..9999).random()}"
        }
    }

    fun getFormattedUploadDate(): String {
        val date = Date(uploadTimestamp)
        val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
        return formatter.format(date)
    }

    fun getFormattedVerificationDate(): String? {
        return verificationTimestamp?.let { timestamp ->
            val date = Date(timestamp)
            val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID"))
            formatter.format(date)
        }
    }

    fun getStatusText(): String {
        return when (status) {
            "pending_verification" -> "Menunggu Verifikasi"
            "approved" -> "Disetujui"
            "rejected" -> "Ditolak"
            else -> "Status Tidak Diketahui"
        }
    }

    fun getStatusColor(): String {
        return when (status) {
            "pending_verification" -> "#FFA500" // Orange
            "approved" -> "#4CAF50" // Green
            "rejected" -> "#F44336" // Red
            else -> "#757575" // Gray
        }
    }

    fun isApproved(): Boolean = status == "approved"
    fun isRejected(): Boolean = status == "rejected"
    fun isPending(): Boolean = status == "pending_verification"
}