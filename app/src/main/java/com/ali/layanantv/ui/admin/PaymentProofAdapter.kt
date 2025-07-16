package com.ali.layanantv.ui.adapter

import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.R
import com.ali.layanantv.data.model.PaymentProof
import com.ali.layanantv.databinding.ItemPaymentProofBinding

class PaymentProofAdapter(
    private var paymentProofs: List<PaymentProof>,
    private val onItemClick: (PaymentProof) -> Unit,
    private val onApproveClick: ((PaymentProof) -> Unit)? = null,
    private val onRejectClick: ((PaymentProof) -> Unit)? = null
) : RecyclerView.Adapter<PaymentProofAdapter.PaymentProofViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentProofViewHolder {
        val binding = ItemPaymentProofBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return PaymentProofViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentProofViewHolder, position: Int) {
        holder.bind(paymentProofs[position])
    }

    override fun getItemCount(): Int = paymentProofs.size

    fun updateData(newPaymentProofs: List<PaymentProof>) {
        paymentProofs = newPaymentProofs
        notifyDataSetChanged()
    }

    inner class PaymentProofViewHolder(
        private val binding: ItemPaymentProofBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(paymentProof: PaymentProof) {
            with(binding) {
                // Basic info
                tvOrderId.text = paymentProof.orderId
                tvPaymentId.text = paymentProof.id
                tvUploadDate.text = paymentProof.getFormattedUploadDate()
                tvStatus.text = paymentProof.getStatusText()

                // Status color
                val statusColor = Color.parseColor(paymentProof.getStatusColor())
                tvStatus.setTextColor(statusColor)

                // Additional notes
                if (paymentProof.additionalNotes.isNotEmpty()) {
                    tvAdditionalNotes.text = paymentProof.additionalNotes
                    tvAdditionalNotes.visibility = View.VISIBLE
                    labelAdditionalNotes.visibility = View.VISIBLE
                } else {
                    tvAdditionalNotes.visibility = View.GONE
                    labelAdditionalNotes.visibility = View.GONE
                }

                // Admin notes (if verified)
                if (paymentProof.adminNotes.isNotEmpty()) {
                    tvAdminNotes.text = paymentProof.adminNotes
                    tvVerifiedBy.text = paymentProof.verifiedByAdmin
                    tvVerificationDate.text = paymentProof.getFormattedVerificationDate() ?: ""

                    layoutAdminNotes.visibility = View.VISIBLE
                } else {
                    layoutAdminNotes.visibility = View.GONE
                }

                // Load image thumbnail
                try {
                    val imageUri = Uri.parse(paymentProof.imageUri)
                    val bitmap = if (paymentProof.imagePath != null) {
                        BitmapFactory.decodeFile(paymentProof.imagePath)
                    } else {
                        val inputStream = itemView.context.contentResolver.openInputStream(imageUri)
                        BitmapFactory.decodeStream(inputStream)
                    }

                    ivThumbnail.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    ivThumbnail.setImageResource(R.drawable.ic_image_placeholder)
                }

                // Admin action buttons
                if (onApproveClick != null && onRejectClick != null) {
                    layoutAdminActions.visibility = if (paymentProof.isPending()) {
                        View.VISIBLE
                    } else {
                        View.GONE
                    }

                    btnApprove.setOnClickListener {
                        onApproveClick.invoke(paymentProof)
                    }

                    btnReject.setOnClickListener {
                        onRejectClick.invoke(paymentProof)
                    }
                } else {
                    layoutAdminActions.visibility = View.GONE
                }

                // Item click
                root.setOnClickListener {
                    onItemClick(paymentProof)
                }
            }
        }
    }
}