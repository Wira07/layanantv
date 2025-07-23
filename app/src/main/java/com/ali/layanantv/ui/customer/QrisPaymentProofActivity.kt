package com.ali.layanantv.ui.customer

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import com.ali.layanantv.data.repository.PaymentRepository
import com.ali.layanantv.databinding.ActivityQrisPaymentProofBinding
import com.ali.layanantv.data.model.PaymentProof
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class QrisPaymentProofActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrisPaymentProofBinding
    private lateinit var paymentRepository: PaymentRepository

    private var selectedImageUri: Uri? = null
    private var selectedImageFile: File? = null

    private val orderId by lazy { intent.getStringExtra(EXTRA_ORDER_ID) ?: "" }
    private val paymentAmount by lazy { intent.getStringExtra(EXTRA_PAYMENT_AMOUNT) ?: "" }
    private val channelName by lazy { intent.getStringExtra(EXTRA_CHANNEL_NAME) ?: "" }

    companion object {
        const val EXTRA_ORDER_ID = "extra_order_id"
        const val EXTRA_PAYMENT_AMOUNT = "extra_payment_amount"
        const val EXTRA_CHANNEL_NAME = "extra_channel_name"
    }

    // Modern permission requests using ActivityResultContracts
    private val requestCameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(this, "Izin kamera diperlukan untuk mengambil foto", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestStoragePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(this, "Izin penyimpanan diperlukan untuk memilih gambar", Toast.LENGTH_SHORT).show()
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
        binding = ActivityQrisPaymentProofBinding.inflate(layoutInflater)
        setContentView(binding.root)

        paymentRepository = PaymentRepository()

        setupUI()
        setupClickListeners()
        loadPaymentData()
    }

    private fun setupUI() {
        // Setup toolbar
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Upload Bukti Pembayaran"
        }

        // Initially hide preview elements
        binding.cvImagePreview.visibility = View.GONE
        binding.llFileInfo.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.uploadArea.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnRemoveImage.setOnClickListener {
            removeSelectedImage()
        }

        binding.btnSubmitProof.setOnClickListener {
            submitPaymentProof()
        }
    }

    private fun loadPaymentData() {
        binding.tvOrderId.text = orderId
        binding.tvPaymentAmount.text = paymentAmount
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
            Toast.makeText(this, "Gagal membuat file gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal membuka kamera: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openGallery() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            intent.type = "image/*"

            // Add alternative intent for file managers
            val chooserIntent = Intent.createChooser(intent, "Pilih Gambar")

            // Add alternative intents
            val alternativeIntents = arrayListOf<Intent>()

            // Add Documents UI intent
            val documentsIntent = Intent(Intent.ACTION_GET_CONTENT)
            documentsIntent.type = "image/*"
            documentsIntent.addCategory(Intent.CATEGORY_OPENABLE)
            alternativeIntents.add(documentsIntent)

            if (alternativeIntents.isNotEmpty()) {
                chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, alternativeIntents.toTypedArray())
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
            // Load and display the image
            val bitmap = if (selectedImageFile != null && selectedImageFile!!.exists()) {
                BitmapFactory.decodeFile(selectedImageFile!!.absolutePath)
            } else {
                val inputStream = contentResolver.openInputStream(uri)
                BitmapFactory.decodeStream(inputStream)
            }

            if (bitmap != null) {
                binding.ivPreview.setImageBitmap(bitmap)

                // Show preview elements
                binding.cvImagePreview.visibility = View.VISIBLE
                binding.llFileInfo.visibility = View.VISIBLE

                // Hide upload area
                binding.uploadArea.visibility = View.GONE

                // Update file info
                val fileName = getFileName(uri)
                val fileSize = getFileSize(uri)

                binding.tvFileName.text = fileName
                binding.tvFileSize.text = formatFileSize(fileSize)

                Toast.makeText(this, "Gambar berhasil dipilih", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Gagal memuat gambar", Toast.LENGTH_SHORT).show()
            }

        } catch (e: Exception) {
            Toast.makeText(this, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun removeSelectedImage() {
        selectedImageUri = null
        selectedImageFile = null

        // Hide preview elements
        binding.cvImagePreview.visibility = View.GONE
        binding.llFileInfo.visibility = View.GONE

        // Show upload area
        binding.uploadArea.visibility = View.VISIBLE

        Toast.makeText(this, "Gambar dihapus", Toast.LENGTH_SHORT).show()
    }

    private fun submitPaymentProof() {
        if (selectedImageUri == null) {
            Toast.makeText(this, "Pilih gambar terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        val additionalNotes = binding.etAdditionalNotes.text.toString().trim()

        // Show loading state
        binding.btnSubmitProof.isEnabled = false

        lifecycleScope.launch {
            try {
                val paymentProof = PaymentProof(
                    orderId = orderId,
                    imageUri = selectedImageUri.toString(),
                    imagePath = selectedImageFile?.absolutePath,
                    additionalNotes = additionalNotes,
                    uploadTimestamp = System.currentTimeMillis(),
                    status = "pending_verification"
                )

                val success = paymentRepository.submitPaymentProof(paymentProof)

                if (success) {
                    showSuccessDialog()
                } else {
                    Toast.makeText(this@QrisPaymentProofActivity, "Gagal mengirim bukti pembayaran", Toast.LENGTH_SHORT).show()
                    binding.btnSubmitProof.isEnabled = true
                }

            } catch (e: Exception) {
                Toast.makeText(this@QrisPaymentProofActivity, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnSubmitProof.isEnabled = true
            }
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(this)
            .setTitle("Berhasil!")
            .setMessage("Bukti pembayaran berhasil dikirim.\n\nAdmin akan memverifikasi pembayaran Anda dalam 1x24 jam. Anda akan mendapat notifikasi setelah verifikasi selesai.")
            .setPositiveButton("OK") { _, _ ->
                finish()
            }
            .setCancelable(false)
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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}