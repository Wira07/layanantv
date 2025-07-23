// PaymentActivity.kt
package com.ali.layanantv.ui.customer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.databinding.ActivityPaymentBinding
import com.ali.layanantv.data.repository.CustomerRepository
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.data.model.PaymentProof
import com.ali.layanantv.data.repository.PaymentRepository
import com.ali.layanantv.ui.customer.QrisPaymentProofActivity.Companion.EXTRA_CHANNEL_NAME
import com.ali.layanantv.ui.customer.QrisPaymentProofActivity.Companion.EXTRA_ORDER_ID
import com.ali.layanantv.ui.customer.QrisPaymentProofActivity.Companion.EXTRA_PAYMENT_AMOUNT
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class PaymentActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentBinding
    private lateinit var customerRepository: CustomerRepository
    private lateinit var paymentRepository: PaymentRepository
    private var selectedChannel: Channel? = null
    private var originalPrice: Double = 0.0
    private var userPoints: Int = 0
    private var pointsToUse: Int = 0
    private var finalPrice: Double = 0.0
    private var subscriptionType: String = "1_month"

    private var selectedImageUri: Uri? = null
    private var selectedImageFile: File? = null

    private val orderId by lazy { intent.getStringExtra(EXTRA_ORDER_ID) ?: "" }
    private val paymentAmount by lazy { intent.getStringExtra(EXTRA_PAYMENT_AMOUNT) ?: "" }
    private val channelName by lazy { intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "" }

    companion object {
        const val EXTRA_CHANNEL_ID = "channel_id"
        const val EXTRA_SUBSCRIPTION_TYPE = "subscription_type"
        const val MIN_POINTS_USAGE = 100
        const val POINT_TO_RUPIAH = 1 // 1 point = Rp 1
        const val MAX_DISCOUNT_PERCENTAGE = 0.5 // 50%
    }

    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT)
                .show()
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(
                this,
                "Izin penyimpanan diperlukan untuk memilih gambar",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // Activity Result Launchers
    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri?.let { uri ->
                displaySelectedImage(uri)
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displaySelectedImage(uri)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        customerRepository = CustomerRepository()
        paymentRepository = PaymentRepository()

        // Get data dari intent
        val channelId = intent.getStringExtra(EXTRA_CHANNEL_ID) ?: ""
        subscriptionType = intent.getStringExtra(EXTRA_SUBSCRIPTION_TYPE) ?: "1_month"

        setupUI()
        loadChannelData(channelId)
        loadUserPoints()
    }

    private fun showImagePickerDialog() {
        val options = arrayOf(
            "Ambil Foto",
            "Pilih dari Galeri",
            "Batal"
        )

        AlertDialog.Builder(this)
            .setTitle("Pilih Sumber Gambar")
            .setItems(options) { dialog, which ->
                when (which) {
                    0 -> {
                        if (checkCameraPermission()) {
                            openCamera()
                        } else {
                            requestCameraPermission()
                        }
                    }

                    1 -> {
                        if (checkStoragePermission()) {
                            openGallery()
                        } else {
                            requestStoragePermission()
                        }
                    }

                    2 -> dialog.dismiss()
                }
            }
            .show()
    }

    private fun checkCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkStoragePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ uses READ_MEDIA_IMAGES
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Below Android 13 uses READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestCameraPermission() {
        when {
            checkCameraPermission() -> {
                openCamera()
            }

            shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) -> {
                // Show explanation dialog
                AlertDialog.Builder(this)
                    .setTitle("Izin Kamera Diperlukan")
                    .setMessage("Aplikasi memerlukan izin kamera untuk mengambil foto bukti pembayaran.")
                    .setPositiveButton("Berikan Izin") { _, _ ->
                        requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            else -> {
                requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun requestStoragePermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            checkStoragePermission() -> {
                openGallery()
            }

            shouldShowRequestPermissionRationale(permission) -> {
                // Show explanation dialog
                AlertDialog.Builder(this)
                    .setTitle("Izin Penyimpanan Diperlukan")
                    .setMessage("Aplikasi memerlukan izin untuk mengakses galeri guna memilih foto bukti pembayaran.")
                    .setPositiveButton("Berikan Izin") { _, _ ->
                        requestStoragePermissionLauncher.launch(permission)
                    }
                    .setNegativeButton("Batal", null)
                    .show()
            }

            else -> {
                requestStoragePermissionLauncher.launch(permission)
            }
        }
    }

    private fun openCamera() {
        try {
            val imageFile = createImageFile()
            selectedImageFile = imageFile

            val imageUri = FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                imageFile
            )
            selectedImageUri = imageUri

            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

            // Grant URI permission for camera app
            val resolvedIntentActivities = packageManager.queryIntentActivities(
                takePictureIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolvedIntentInfo in resolvedIntentActivities) {
                val packageName = resolvedIntentInfo.activityInfo.packageName
                grantUriPermission(
                    packageName,
                    imageUri,
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            takePictureLauncher.launch(takePictureIntent)
        } catch (e: IOException) {
            Toast.makeText(this, "Gagal membuat file gambar: ${e.message}", Toast.LENGTH_SHORT)
                .show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"

            val chooserIntent = Intent.createChooser(intent, "Pilih Gambar")

            val alternativeIntents = arrayListOf<Intent>()

            // Add Documents UI intent
            val documentsIntent = Intent(Intent.ACTION_GET_CONTENT)
            documentsIntent.type = "image/*"
            documentsIntent.addCategory(Intent.CATEGORY_OPENABLE)
            alternativeIntents.add(documentsIntent)

            if (alternativeIntents.isNotEmpty()) {
                chooserIntent.putExtra(
                    Intent.EXTRA_INITIAL_INTENTS,
                    alternativeIntents.toTypedArray()
                )
            }

            pickImageLauncher.launch(chooserIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka galeri: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFileName = "JPEG_${timeStamp}_payment_proof"
        val storageDir = getExternalFilesDir("Pictures")
            ?: File(filesDir, "Pictures").apply { mkdirs() }

        return File.createTempFile(
            imageFileName,
            ".jpg",
            storageDir
        )
    }

    private fun displaySelectedImage(uri: Uri) {
        try {
            Glide.with(this)
                .load(uri)
                .into(binding.ivPreview)

            binding.cvImagePreview.visibility = View.VISIBLE
            binding.llFileInfo.visibility = View.VISIBLE
            binding.uploadArea.visibility = View.GONE
            binding.btnPayment.isEnabled = true

            val fileName = getFileName(uri)
            val fileSize = getFileSize(uri)

            binding.tvFileName.text = fileName
            binding.tvFileSize.text = formatFileSize(fileSize)

            Toast.makeText(this, "Gambar berhasil dipilih", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupUI() {
        binding.apply {
            binding.cvImagePreview.visibility = View.GONE
            btnBack.setOnClickListener { finish() }
            uploadArea.setOnClickListener { showImagePickerDialog() }
            btnPayment.isEnabled = false

            btnPayment.setOnClickListener {
                if (selectedImageUri != null) {
                    uploadProofAndPay()
                } else {
                    Toast.makeText(
                        this@PaymentActivity,
                        "Silakan unggah bukti pembayaran",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            switchUsePoints.setOnCheckedChangeListener { _, isChecked ->
                layoutPointUsage.visibility = if (isChecked) View.VISIBLE else View.GONE
                if (!isChecked) {
                    pointsToUse = 0
                    binding.etPointsToUse.text?.clear()
                    calculateFinalPrice()
                }
            }

            setupSubscriptionTypeSelection()
            setupPointUsageInput()
        }
    }

    private fun setupSubscriptionTypeSelection() {
        binding.apply {
            // Default selection
            updateSubscriptionSelection("1_month")

            btnSubscription1Month.setOnClickListener {
                updateSubscriptionSelection("1_month")
            }
        }
    }

    private fun updateSubscriptionSelection(type: String) {
        subscriptionType = type

        // Update UI selection state
        binding.apply {
            btnSubscription1Month.isSelected = type == "1_month"
        }

        // Recalculate price
        calculatePrice()
    }

    private fun setupPointUsageInput() {
        binding.etPointsToUse.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val input = s.toString()
                if (input.isNotEmpty()) {
                    try {
                        val points = input.toInt()
                        validateAndUpdatePointUsage(points)
                    } catch (e: NumberFormatException) {
                        binding.tvPointsError.text = "Format angka tidak valid"
                        binding.tvPointsError.visibility = View.VISIBLE
                    }
                } else {
                    pointsToUse = 0
                    calculateFinalPrice()
                }
            }
        })
    }

    private fun validateAndUpdatePointUsage(points: Int) {
        binding.apply {
            tvPointsError.visibility = android.view.View.GONE

            when {
                points < MIN_POINTS_USAGE -> {
                    tvPointsError.text = "Minimal penggunaan $MIN_POINTS_USAGE point"
                    tvPointsError.visibility = android.view.View.VISIBLE
                    pointsToUse = 0
                }

                points > userPoints -> {
                    tvPointsError.text = "Point tidak mencukupi. Maksimal: $userPoints point"
                    tvPointsError.visibility = android.view.View.VISIBLE
                    pointsToUse = userPoints
                }

                points > (originalPrice * MAX_DISCOUNT_PERCENTAGE).toInt() -> {
                    val maxPoints = (originalPrice * MAX_DISCOUNT_PERCENTAGE).toInt()
                    tvPointsError.text = "Maksimal penggunaan: $maxPoints point (50% dari harga)"
                    tvPointsError.visibility = android.view.View.VISIBLE
                    pointsToUse = maxPoints
                }

                else -> {
                    pointsToUse = points
                }
            }

            calculateFinalPrice()
        }
    }

    private fun togglePointUsage(isEnabled: Boolean) {
        binding.apply {
            layoutPointUsage.visibility =
                if (isEnabled) android.view.View.VISIBLE else android.view.View.GONE

            if (!isEnabled) {
                pointsToUse = 0
                etPointsToUse.text?.clear()
                calculateFinalPrice()
            }
        }
    }

    private fun loadChannelData(channelId: String) {
        lifecycleScope.launch {
            try {
                selectedChannel = customerRepository.getChannelById(channelId)
                selectedChannel?.let { channel ->
                    binding.tvChannelName.text = channel.name
                    binding.tvChannelDescription.text = channel.description
                    calculatePrice()
                } ?: run {
                    Toast.makeText(
                        this@PaymentActivity,
                        "Channel tidak ditemukan",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PaymentActivity,
                    "Error loading channel: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun loadUserPoints() {
        lifecycleScope.launch {
            try {
                val currentUser = FirebaseAuth.getInstance().currentUser
                if (currentUser == null) {
                    userPoints = 0
                    binding.tvAvailablePoints.text = "Point tersedia: 0"
                    return@launch
                }

                userPoints = customerRepository.getUserPoints(currentUser.uid)
                binding.apply {
                    tvAvailablePoints.text = "Point tersedia: ${formatNumber(userPoints)}"
                    tvPointsInfo.text =
                        "1 Point = Rp 1 • Min: $MIN_POINTS_USAGE • Max: 50% dari harga"
                }
            } catch (e: Exception) {
                userPoints = 0
                binding.tvAvailablePoints.text = "Point tersedia: 0"
            }
        }
    }

    private fun calculatePrice() {
        selectedChannel?.let { channel ->
            originalPrice = when (subscriptionType) {
                "1_month" -> channel.price
                "3_month" -> channel.price * 3 * 0.95 // 5% discount
                "12_month" -> channel.price * 12 * 0.85 // 15% discount
                else -> channel.price
            }

            calculateFinalPrice()
        }
    }

    private fun calculateFinalPrice() {
        val pointDiscount = pointsToUse * POINT_TO_RUPIAH
        finalPrice = originalPrice - pointDiscount

        binding.apply {
            tvOriginalPrice.text = "Harga Asli: ${formatCurrency(originalPrice)}"

            if (pointsToUse > 0) {
                tvPointDiscount.text = "Diskon Point: -${formatCurrency(pointDiscount.toDouble())}"
                tvPointDiscount.visibility = android.view.View.VISIBLE
                tvPointsUsed.text = "Menggunakan $pointsToUse point"
                tvPointsUsed.visibility = android.view.View.VISIBLE
            } else {
                tvPointDiscount.visibility = android.view.View.GONE
                tvPointsUsed.visibility = android.view.View.GONE
            }

            tvFinalPrice.text = formatCurrency(finalPrice)
            btnPayment.text = "Bayar ${formatCurrency(finalPrice)}"
        }
    }

    private fun uploadProofAndPay() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val channel = selectedChannel ?: return
        val additionalNotes = binding.etAdditionalNotes.text.toString().trim()

        binding.btnPayment.isEnabled = false
        binding.btnPayment.text = "Mengunggah..."

        lifecycleScope.launch {
            try {
                val cloudinaryUrl =
                    customerRepository.uploadToCloudinary(this@PaymentActivity, selectedImageUri!!)

                // Simpan objek PaymentProof (opsional)
                val paymentProof = PaymentProof(
                    orderId = orderId,
                    imageUri = selectedImageUri.toString(),
                    imagePath = selectedImageFile?.absolutePath,
                    additionalNotes = additionalNotes,
                    uploadTimestamp = System.currentTimeMillis(),
                    status = "pending_verification"
                )
                val proofSuccess = paymentRepository.submitPaymentProof(paymentProof)

                if (cloudinaryUrl != null) {
                    val order = Order(
                        userId = currentUser.uid,
                        channelId = channel.id,
                        channelName = channel.name,
                        subscriptionType = subscriptionType,
                        originalAmount = originalPrice,
                        pointsUsed = pointsToUse,
                        pointDiscount = pointsToUse * POINT_TO_RUPIAH.toDouble(),
                        totalAmount = finalPrice,
                        proofImageUrl = cloudinaryUrl,
                        notes = additionalNotes,
                        status = "pending",
                        createdAt = Timestamp.now(),
                        updatedAt = Timestamp.now()
                    )

                    val orderIdResult = customerRepository.createOrder(order)
                    customerRepository.createSubscription(order.copy(id = orderIdResult))
                    if (pointsToUse > 0) customerRepository.deductUserPoints(pointsToUse)
                    customerRepository.checkAndAddRewardPoints(currentUser.uid)

                    showSuccessDialog()
                } else {
                    Toast.makeText(
                        this@PaymentActivity,
                        "Gagal menyimpan data pembayaran",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } catch (e: Exception) {
                Toast.makeText(
                    this@PaymentActivity,
                    "Terjadi kesalahan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            } finally {
                restorePaymentButton()
            }
        }
    }

    private fun restorePaymentButton() {
        binding.btnPayment.isEnabled = true
        binding.btnPayment.text = "Bayar ${formatCurrency(finalPrice)}"
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Pembayaran Berhasil")
            .setMessage("Pembelian berhasil, kamu dapat 1000 poin.\n\nAdmin akan memverifikasi pembayaran Anda dalam 1x24 jam. Anda akan mendapat notifikasi setelah verifikasi selesai.")
            .setCancelable(false)
            .setPositiveButton("Lihat Point") { dialog, _ ->
                dialog.dismiss()
                finish() // menutup PaymentActivity atau pindah halaman
            }
            .show()
    }


    private fun getFileName(uri: Uri): String {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val displayNameIndex = it.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                if (displayNameIndex != -1) {
                    return it.getString(displayNameIndex) ?: "payment_proof.jpg"
                }
            }
        }
        return "payment_proof.jpg"
    }

    private fun getFileSize(uri: Uri): Long {
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(MediaStore.Images.Media.SIZE)
                if (sizeIndex != -1) {
                    return it.getLong(sizeIndex)
                }
            }
        }
        return 0L
    }

    private fun formatFileSize(size: Long): String {
        val kb = size / 1024.0
        val mb = kb / 1024.0

        return when {
            mb >= 1 -> String.format("%.1f MB", mb)
            kb >= 1 -> String.format("%.1f KB", kb)
            else -> "$size B"
        }
    }

    private fun formatCurrency(amount: Double): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        return formatter.format(amount)
    }

    private fun formatNumber(number: Int): String {
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(number)
    }
}