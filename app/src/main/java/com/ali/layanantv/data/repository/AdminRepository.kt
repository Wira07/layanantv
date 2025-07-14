package com.ali.layanantv.data.repository

import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.model.User
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.data.model.DashboardStats
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import android.util.Base64
import java.io.ByteArrayOutputStream
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.content.Context

class AdminRepository {
    private val firestore = FirebaseFirestore.getInstance()

    suspend fun getDashboardStats(): DashboardStats {
        val usersSnapshot = firestore.collection("users").get().await()
        val channelsSnapshot = firestore.collection("channels").get().await()
        val ordersSnapshot = firestore.collection("orders").get().await()

        val totalUsers = usersSnapshot.documents.size
        val activeChannels = channelsSnapshot.documents.size
        val totalOrders = ordersSnapshot.documents.size

        // Calculate total revenue from completed orders
        var totalRevenue = 0.0
        var activeSubscriptions = 0
        var pendingOrders = 0

        for (doc in ordersSnapshot.documents) {
            val order = doc.toObject(Order::class.java)
            if (order?.status == "completed") {
                totalRevenue += order.totalAmount
                activeSubscriptions++ // Count completed orders as active subscriptions
            } else if (order?.status == "pending") {
                pendingOrders++
            }
        }

        return DashboardStats(
            totalUsers = totalUsers,
            activeChannels = activeChannels,
            totalOrders = totalOrders,
            totalRevenue = totalRevenue,
            activeSubscriptions = activeSubscriptions,
            pendingOrders = pendingOrders
        )
    }

    // Channel Management
    suspend fun getAllChannels(): List<Channel> {
        val snapshot = firestore.collection("channels")
            .orderBy("name")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Channel::class.java)?.let { channel ->
                Channel(
                    id = doc.id,
                    name = channel.name,
                    description = channel.description,
                    logoUrl = channel.logoUrl,
                    logoBase64 = channel.logoBase64,
                    price = channel.price,
                    category = channel.category,
                    isActive = channel.isActive,
                    createdAt = channel.createdAt,
                    updatedAt = channel.updatedAt
                )
            }
        }
    }

    suspend fun addChannel(channel: Channel): String {
        val docRef = firestore.collection("channels").add(channel).await()
        return docRef.id
    }

    suspend fun updateChannel(channelId: String, channel: Channel) {
        firestore.collection("channels")
            .document(channelId)
            .set(channel)
            .await()
    }

    suspend fun deleteChannel(channelId: String) {
        firestore.collection("channels")
            .document(channelId)
            .delete()
            .await()
    }

    // Menggunakan Base64 untuk menyimpan logo channel di Firestore
    suspend fun uploadChannelLogo(context: Context, uri: Uri, channelId: String): String {
        try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Resize bitmap untuk menghemat space (maksimal 500x500)
            val resizedBitmap = resizeBitmap(bitmap, 500, 500)

            // Convert ke Base64
            val base64String = bitmapToBase64(resizedBitmap)

            // Simpan ke Firestore sebagai field di document channel
            firestore.collection("channels")
                .document(channelId)
                .update("logoBase64", base64String)
                .await()

            return base64String

        } catch (e: Exception) {
            throw Exception("Gagal upload logo: ${e.message}")
        }
    }

    // Alternative: Simpan sebagai URL eksternal (misal dari web)
    suspend fun setChannelLogoUrl(channelId: String, logoUrl: String) {
        firestore.collection("channels")
            .document(channelId)
            .update("logoUrl", logoUrl)
            .await()
    }

    // Helper function untuk resize bitmap
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val ratioBitmap = width.toFloat() / height.toFloat()
        val ratioMax = maxWidth.toFloat() / maxHeight.toFloat()

        var finalWidth = maxWidth
        var finalHeight = maxHeight

        if (ratioMax > ratioBitmap) {
            finalWidth = (maxHeight.toFloat() * ratioBitmap).toInt()
        } else {
            finalHeight = (maxWidth.toFloat() / ratioBitmap).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, finalWidth, finalHeight, true)
    }

    // Helper function untuk convert bitmap ke Base64
    private fun bitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    // Customer Management
    suspend fun getAllUsers(): List<User> {
        val snapshot = firestore.collection("users")
            .orderBy("email")
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(User::class.java)?.let { user ->
                User(
                    uid = doc.id,
                    email = user.email,
                    name = user.name,
                    phoneNumber = user.phoneNumber,
                    role = user.role,
                    createdAt = user.createdAt,
                    isActive = user.isActive
                )
            }
        }
    }

    suspend fun getUserById(userId: String): User? {
        val snapshot = firestore.collection("users")
            .document(userId)
            .get()
            .await()

        return snapshot.toObject(User::class.java)?.let { user ->
            User(
                uid = snapshot.id,
                email = user.email,
                name = user.name,
                phoneNumber = user.phoneNumber,
                role = user.role,
                createdAt = user.createdAt,
                isActive = user.isActive
            )
        }
    }

    suspend fun updateUserStatus(userId: String, isActive: Boolean) {
        firestore.collection("users")
            .document(userId)
            .update("isActive", isActive)
            .await()
    }

    // Order Management
    suspend fun getAllOrders(): List<Order> {
        val snapshot = firestore.collection("orders")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Order::class.java)?.let { order ->
                Order(
                    id = doc.id,
                    userId = order.userId,
                    userName = order.userName,
                    userEmail = order.userEmail,
                    channelId = order.channelId,
                    channelName = order.channelName,
                    subscriptionType = order.subscriptionType,
                    totalAmount = order.totalAmount,
                    status = order.status,
                    paymentMethod = order.paymentMethod,
                    paymentVerified = order.paymentVerified,
                    paymentProofUrl = order.paymentProofUrl,
                    notes = order.notes,
                    createdAt = order.createdAt,
                    updatedAt = order.updatedAt
                )
            }
        }
    }

    suspend fun getOrderById(orderId: String): Order? {
        val snapshot = firestore.collection("orders")
            .document(orderId)
            .get()
            .await()

        return snapshot.toObject(Order::class.java)?.let { order ->
            Order(
                id = snapshot.id,
                userId = order.userId,
                userName = order.userName,
                userEmail = order.userEmail,
                channelId = order.channelId,
                channelName = order.channelName,
                subscriptionType = order.subscriptionType,
                totalAmount = order.totalAmount,
                status = order.status,
                paymentMethod = order.paymentMethod,
                paymentVerified = order.paymentVerified,
                paymentProofUrl = order.paymentProofUrl,
                notes = order.notes,
                createdAt = order.createdAt,
                updatedAt = order.updatedAt
            )
        }
    }

    suspend fun updateOrderStatus(orderId: String, status: String) {
        firestore.collection("orders")
            .document(orderId)
            .update("status", status)
            .await()
    }

    suspend fun verifyPayment(orderId: String, isVerified: Boolean) {
        firestore.collection("orders")
            .document(orderId)
            .update("paymentVerified", isVerified)
            .await()
    }

    suspend fun getOrdersByUserId(userId: String): List<Order> {
        val snapshot = firestore.collection("orders")
            .whereEqualTo("userId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(Order::class.java)?.let { order ->
                Order(
                    id = doc.id,
                    userId = order.userId,
                    userName = order.userName,
                    userEmail = order.userEmail,
                    channelId = order.channelId,
                    channelName = order.channelName,
                    subscriptionType = order.subscriptionType,
                    totalAmount = order.totalAmount,
                    status = order.status,
                    paymentMethod = order.paymentMethod,
                    paymentVerified = order.paymentVerified,
                    paymentProofUrl = order.paymentProofUrl,
                    notes = order.notes,
                    createdAt = order.createdAt,
                    updatedAt = order.updatedAt
                )
            }
        }
    }
}