package com.ali.layanantv.data.repository

import android.util.Log
import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.model.CustomerDashboardStats
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.firestore.FieldValue
import kotlinx.coroutines.tasks.await

class CustomerRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "CustomerRepository"
    }

    // Method untuk mengurangi points user setelah digunakan untuk pembayaran
    suspend fun deductUserPoints(pointsToDeduct: Int): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            Log.d(TAG, "Deducting $pointsToDeduct points from user: ${currentUser.uid}")

            // Ambil data user terlebih dahulu untuk validasi
            val userSnapshot = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            val currentPoints = userSnapshot.getLong("points")?.toInt() ?: 0

            if (currentPoints < pointsToDeduct) {
                Log.e(TAG, "Insufficient points. Current: $currentPoints, Required: $pointsToDeduct")
                return false
            }

            // Kurangi points menggunakan FieldValue.increment untuk atomic operation
            firestore.collection("users")
                .document(currentUser.uid)
                .update(
                    mapOf(
                        "points" to FieldValue.increment(-pointsToDeduct.toLong()),
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Log.d(TAG, "Successfully deducted $pointsToDeduct points from user")

            // Tambahkan record ke point transaction history
            recordPointTransaction(currentUser.uid, pointsToDeduct, "deduction", "Used for payment")

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error deducting user points: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    // Method untuk menambah points user (untuk reward, refund, dll)
    suspend fun addUserPoints(pointsToAdd: Int, reason: String = "Reward"): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            Log.d(TAG, "Adding $pointsToAdd points to user: ${currentUser.uid}")

            // Tambah points menggunakan FieldValue.increment untuk atomic operation
            firestore.collection("users")
                .document(currentUser.uid)
                .update(
                    mapOf(
                        "points" to FieldValue.increment(pointsToAdd.toLong()),
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()

            Log.d(TAG, "Successfully added $pointsToAdd points to user")

            // Tambahkan record ke point transaction history
            recordPointTransaction(currentUser.uid, pointsToAdd, "addition", reason)

            true
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user points: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    // Method untuk mencatat transaksi point
    private suspend fun recordPointTransaction(userId: String, points: Int, type: String, reason: String) {
        try {
            Log.d(TAG, "Recording point transaction: user=$userId, points=$points, type=$type")

            val transaction = hashMapOf(
                "userId" to userId,
                "points" to points,
                "type" to type, // "addition" or "deduction"
                "reason" to reason,
                "createdAt" to Timestamp.now()
            )

            firestore.collection("point_transactions")
                .add(transaction)
                .await()

            Log.d(TAG, "Point transaction recorded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error recording point transaction: ${e.message}", e)
            // Tidak throw error karena ini bukan critical operation
        }
    }

    // Method untuk mendapatkan riwayat transaksi point user
    suspend fun getPointTransactionHistory(limit: Int = 20): List<Map<String, Any>> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            Log.d(TAG, "Getting point transaction history for user: ${currentUser.uid}")

            val snapshot = firestore.collection("point_transactions")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val transactions = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    data.plus("id" to doc.id)
                } else null
            }

            Log.d(TAG, "Found ${transactions.size} point transactions")
            transactions
        } catch (e: Exception) {
            Log.e(TAG, "Error getting point transaction history: ${e.message}", e)
            emptyList()
        }
    }


    // Get all active channels
    suspend fun getActiveChannels(): List<Channel> {
        return try {
            Log.d(TAG, "Getting active channels")
            val snapshot = firestore.collection("channels")
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .await()

            val channels = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Channel::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Found ${channels.size} active channels")
            channels
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active channels: ${e.message}", e)
            emptyList()
        }
    }

    // Get channels by category
    suspend fun getChannelsByCategory(category: String): List<Channel> {
        return try {
            Log.d(TAG, "Getting channels by category: $category")
            val snapshot = firestore.collection("channels")
                .whereEqualTo("isActive", true)
                .whereEqualTo("category", category)
                .orderBy("name")
                .get()
                .await()

            val channels = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Channel::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Found ${channels.size} channels in category: $category")
            channels
        } catch (e: Exception) {
            Log.e(TAG, "Error getting channels by category: ${e.message}", e)
            emptyList()
        }
    }

    // Get channel by ID
    suspend fun getChannelById(channelId: String): Channel? {
        return try {
            Log.d(TAG, "Getting channel by ID: $channelId")
            val snapshot = firestore.collection("channels")
                .document(channelId)
                .get()
                .await()

            val channel = snapshot.toObject(Channel::class.java)?.copy(id = snapshot.id)
            Log.d(TAG, "Found channel: ${channel?.name}")
            channel
        } catch (e: Exception) {
            Log.e(TAG, "Error getting channel by ID: ${e.message}", e)
            null
        }
    }

    // Create new order
    suspend fun createOrder(order: Order): String {
        try {
            Log.d(TAG, "Creating order for channel: ${order.channelName}")

            // Validasi user authentication
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not authenticated")
                throw SecurityException("User not authenticated")
            }

            // Validasi bahwa order.userId sama dengan current user
            if (order.userId != currentUser.uid) {
                Log.e(TAG, "User ID mismatch: order.userId=${order.userId}, currentUser.uid=${currentUser.uid}")
                throw SecurityException("User ID mismatch")
            }

            // Pastikan order memiliki semua field yang diperlukan
            val orderData = hashMapOf(
                "userId" to order.userId,
                "channelId" to order.channelId,
                "channelName" to order.channelName,
                "subscriptionType" to order.subscriptionType,
                "totalAmount" to order.totalAmount,
                "status" to "pending", // Sesuai dengan validasi di rules: isValidOrderStatus
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )

            // Tambahkan field opsional jika ada
            if (order.paymentMethod.isNotEmpty()) {
                orderData["paymentMethod"] = order.paymentMethod
            }

            Log.d(TAG, "Creating order with data: $orderData")

            val docRef = firestore.collection("orders").add(orderData).await()
            Log.d(TAG, "Order created with ID: ${docRef.id}")

            return docRef.id
        } catch (e: Exception) {
            Log.e(TAG, "Error creating order: ${e.message}", e)

            when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    throw SecurityException("Permission denied: Make sure you are authenticated and have proper permissions")
                }
                e.message?.contains("INVALID_ARGUMENT") == true -> {
                    throw IllegalArgumentException("Invalid order data: ${e.message}")
                }
                e.message?.contains("UNAUTHENTICATED") == true -> {
                    throw SecurityException("User not authenticated")
                }
                e is SecurityException -> {
                    throw e
                }
                else -> {
                    throw Exception("Failed to create order: ${e.message}")
                }
            }
        }
    }

    // Create subscription record when order is completed
    // Add this improved createSubscription method to CustomerRepository
    suspend fun createSubscription(order: Order) {
        try {
            Log.d(TAG, "Creating subscription for order: ${order.id}")
            Log.d(TAG, "Order details: userId=${order.userId}, channelId=${order.channelId}, channelName=${order.channelName}, status=${order.status}")

            // Validasi user authentication
            val currentUser = auth.currentUser
            if (currentUser == null) {
                Log.e(TAG, "User not authenticated")
                throw SecurityException("User not authenticated")
            }

            // Validasi bahwa order.userId sama dengan current user
            if (order.userId != currentUser.uid) {
                Log.e(TAG, "User ID mismatch: order.userId=${order.userId}, currentUser.uid=${currentUser.uid}")
                throw SecurityException("User ID mismatch")
            }

            // Pastikan order memiliki data yang lengkap
            if (order.userId.isEmpty() || order.channelId.isEmpty() || order.channelName.isNullOrEmpty()) {
                Log.e(TAG, "Order data incomplete: userId=${order.userId}, channelId=${order.channelId}, channelName=${order.channelName}")
                throw IllegalArgumentException("Order data is incomplete")
            }

            // Validasi subscriptionType
            if (order.subscriptionType.isEmpty()) {
                Log.e(TAG, "Subscription type is empty")
                throw IllegalArgumentException("Subscription type is required")
            }

            // Check if subscription already exists
            val existingSubscription = firestore.collection("subscriptions")
                .whereEqualTo("userId", order.userId)
                .whereEqualTo("channelId", order.channelId)
                .whereEqualTo("status", "active")
                .get()
                .await()

            if (existingSubscription.documents.isNotEmpty()) {
                Log.w(TAG, "Active subscription already exists for user ${order.userId} and channel ${order.channelId}")

                // Update existing subscription instead of creating new one
                val existingDoc = existingSubscription.documents.first()
                val updateData = mapOf(
                    "orderId" to order.id,
                    "totalAmount" to order.totalAmount,
                    "subscriptionType" to order.subscriptionType,
                    "updatedAt" to Timestamp.now()
                )

                firestore.collection("subscriptions")
                    .document(existingDoc.id)
                    .update(updateData)
                    .await()

                Log.d(TAG, "Updated existing subscription with new order data")
                return
            }

            // Create new subscription dengan semua field yang diperlukan sesuai rules
            val subscription = hashMapOf(
                "userId" to order.userId,
                "channelId" to order.channelId,
                "channelName" to order.channelName,
                "subscriptionType" to order.subscriptionType,
                "totalAmount" to order.totalAmount,
                "status" to "active", // Sesuai dengan validasi di rules: isValidSubscriptionStatus
                "createdAt" to Timestamp.now()
            )

            // Tambahkan field opsional jika ada
            if (order.id.isNotEmpty()) {
                subscription["orderId"] = order.id
            }

            Log.d(TAG, "Creating subscription with data: $subscription")

            // Buat subscription baru
            val docRef = firestore.collection("subscriptions").add(subscription).await()
            Log.d(TAG, "Subscription created successfully with ID: ${docRef.id}")

            // Update order status hanya jika order.id tidak kosong
            if (order.id.isNotEmpty()) {
                try {
                    val updateData = mapOf(
                        "status" to "completed",
                        "updatedAt" to Timestamp.now()
                    )

                    firestore.collection("orders")
                        .document(order.id)
                        .update(updateData)
                        .await()
                    Log.d(TAG, "Order status updated to completed")
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to update order status, but subscription was created successfully: ${e.message}")
                    // Tidak throw error karena subscription sudah berhasil dibuat
                }
            }

            Log.d(TAG, "Successfully created subscription")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating subscription: ${e.message}", e)

            // Berikan error message yang lebih spesifik
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    throw SecurityException("Permission denied: Make sure you are authenticated and have proper permissions")
                }
                e.message?.contains("INVALID_ARGUMENT") == true -> {
                    throw IllegalArgumentException("Invalid subscription data: ${e.message}")
                }
                e.message?.contains("UNAUTHENTICATED") == true -> {
                    throw SecurityException("User not authenticated")
                }
                e is SecurityException -> {
                    throw e
                }
                e is IllegalArgumentException -> {
                    throw e
                }
                else -> {
                    throw Exception("Failed to create subscription: ${e.message}")
                }
            }
        }
    }

    // Get user's orders dengan error handling yang lebih baik
    suspend fun getUserOrders(): List<Order> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            Log.d(TAG, "Getting user orders for: ${currentUser.uid}")
            val snapshot = firestore.collection("orders")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val orders = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Found ${orders.size} orders for user")
            orders
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user orders: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Get user's active subscriptions dengan data channel yang lengkap
    suspend fun getUserSubscriptions(): List<Order> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            Log.d(TAG, "Getting user subscriptions for: ${currentUser.uid}")
            // Pertama, coba ambil dari collection subscriptions
            val subscriptionsSnapshot = firestore.collection("subscriptions")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("isActive", true)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            // Jika ada subscriptions, konversi ke Order format dengan data channel lengkap
            if (subscriptionsSnapshot.documents.isNotEmpty()) {
                val subscriptions = subscriptionsSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        val channelId = data["channelId"] as? String ?: ""
                        val channelName = data["channelName"] as? String ?: ""

                        // Ambil data channel yang lebih lengkap jika diperlukan
                        val channel = if (channelId.isNotEmpty()) {
                            getChannelById(channelId)
                        } else null

                        Order(
                            id = doc.id,
                            userId = data["userId"] as? String ?: "",
                            channelId = channelId,
                            channelName = channel?.name ?: channelName,
                            subscriptionType = data["subscriptionType"] as? String ?: "",
                            totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                            status = "completed",
                            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                            updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                        )
                    } else null
                }

                Log.d(TAG, "Found ${subscriptions.size} active subscriptions")
                subscriptions
            } else {
                // Fallback ke orders yang completed
                val ordersSnapshot = firestore.collection("orders")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("status", "completed")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                val orders = ordersSnapshot.documents.mapNotNull { doc ->
                    val order = doc.toObject(Order::class.java)?.copy(id = doc.id)
                    order?.let {
                        // Pastikan data channel terbaru
                        val channel = getChannelById(it.channelId)
                        it.copy(channelName = channel?.name ?: it.channelName)
                    }
                }

                Log.d(TAG, "Found ${orders.size} completed orders as fallback")
                orders
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user subscriptions: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Get current user info
    suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser ?: return null

        return try {
            Log.d(TAG, "Getting current user info")
            val snapshot = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting current user: ${e.message}", e)
            null
        }
    }

    // Update user profile - Fixed method with individual parameters
    suspend fun updateUserProfile(
        name: String,
        email: String,
        phoneNumber: String,
        profilePhoto: String?
    ): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            Log.d(TAG, "Updating user profile")
            val updateData = hashMapOf<String, Any>(
                "name" to name,
                "email" to email,
                "phoneNumber" to phoneNumber,
                "updatedAt" to Timestamp.now()
            )

            profilePhoto?.let {
                updateData["profilePhoto"] = it
            }

            firestore.collection("users")
                .document(currentUser.uid)
                .update(updateData)
                .await()

            Log.d(TAG, "User profile updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    // Overloaded method for User object (keeping backward compatibility)
    suspend fun updateUserProfile(user: User): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            Log.d(TAG, "Updating user profile with User object")
            firestore.collection("users")
                .document(currentUser.uid)
                .set(user)
                .await()
            Log.d(TAG, "User profile updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error updating user profile: ${e.message}", e)
            e.printStackTrace()
            false
        }
    }

    // Get user points dengan error handling yang lebih baik
    suspend fun getUserPoints(): Int {
        val currentUser = auth.currentUser ?: return 0

        return try {
            Log.d(TAG, "Getting user points")
            val snapshot = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            val points = snapshot.getLong("points")?.toInt() ?: 2500
            Log.d(TAG, "User points: $points")
            points
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user points: ${e.message}", e)
            2500 // Default points jika error
        }
    }

    // PERBAIKAN: Method ini sekarang sama dengan getActiveChannels() untuk konsistensi
    suspend fun getAvailableChannels(): List<Channel> {
        return try {
            Log.d(TAG, "Getting available channels for channel browser")
            val snapshot = firestore.collection("channels")
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .await()

            val channels = snapshot.documents.mapNotNull { doc ->
                try {
                    val channel = doc.toObject(Channel::class.java)?.copy(id = doc.id)
                    Log.d(TAG, "Processing channel: ${channel?.name} (ID: ${channel?.id})")
                    channel
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing channel document: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Successfully retrieved ${channels.size} available channels")

            // Log setiap channel untuk debugging
            channels.forEachIndexed { index, channel ->
                Log.d(TAG, "Channel $index: ${channel.name} (ID: ${channel.id}, Active: ${channel.isActive}, Price: ${channel.price})")
            }

            channels
        } catch (e: Exception) {
            Log.e(TAG, "Error getting available channels: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Get dashboard stats dengan error handling yang lebih baik
    suspend fun getCustomerDashboardStats(): CustomerDashboardStats {
        val currentUser = auth.currentUser ?: return CustomerDashboardStats()

        return try {
            Log.d(TAG, "Getting customer dashboard stats")
            val ordersSnapshot = firestore.collection("orders")
                .whereEqualTo("userId", currentUser.uid)
                .get()
                .await()

            val totalOrders = ordersSnapshot.documents.size
            val activeSubscriptions = ordersSnapshot.documents.count { doc ->
                doc.toObject(Order::class.java)?.status == "completed"
            }
            val pendingOrders = ordersSnapshot.documents.count { doc ->
                doc.toObject(Order::class.java)?.status == "pending"
            }

            val stats = CustomerDashboardStats(
                totalOrders = totalOrders,
                activeSubscriptions = activeSubscriptions,
                pendingOrders = pendingOrders
            )

            Log.d(TAG, "Dashboard stats: $stats")
            stats
        } catch (e: Exception) {
            Log.e(TAG, "Error getting dashboard stats: ${e.message}", e)
            e.printStackTrace()
            CustomerDashboardStats()
        }
    }


    // Method untuk mendapatkan order history dengan pagination
    suspend fun getOrderHistory(limit: Int = 10, lastOrderId: String? = null): List<Order> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            Log.d(TAG, "Getting order history with limit: $limit")
            var query = firestore.collection("orders")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (lastOrderId != null) {
                val lastDoc = firestore.collection("orders")
                    .document(lastOrderId)
                    .get()
                    .await()
                query = query.startAfter(lastDoc)
            }

            val snapshot = query.get().await()

            val orders = snapshot.documents.mapNotNull { doc ->
                val order = doc.toObject(Order::class.java)?.copy(id = doc.id)
                order?.let {
                    // Pastikan data channel terbaru
                    val channel = getChannelById(it.channelId)
                    it.copy(channelName = channel?.name ?: it.channelName)
                }
            }

            Log.d(TAG, "Found ${orders.size} orders in history")
            orders
        } catch (e: Exception) {
            Log.e(TAG, "Error getting order history: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Method untuk mendapatkan subscription history
    suspend fun getSubscriptionHistory(): List<Order> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            Log.d(TAG, "Getting subscription history")
            val snapshot = firestore.collection("subscriptions")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val subscriptions = snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    val channelId = data["channelId"] as? String ?: ""
                    val channelName = data["channelName"] as? String ?: ""

                    // Ambil data channel yang lebih lengkap
                    val channel = if (channelId.isNotEmpty()) {
                        getChannelById(channelId)
                    } else null

                    Order(
                        id = doc.id,
                        userId = data["userId"] as? String ?: "",
                        channelId = channelId,
                        channelName = channel?.name ?: channelName,
                        subscriptionType = data["subscriptionType"] as? String ?: "",
                        totalAmount = (data["totalAmount"] as? Number)?.toDouble() ?: 0.0,
                        status = if (data["isActive"] as? Boolean == true) "active" else "expired",
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                    )
                } else null
            }

            Log.d(TAG, "Found ${subscriptions.size} subscription history records")
            subscriptions
        } catch (e: Exception) {
            Log.e(TAG, "Error getting subscription history: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Method untuk check apakah user sudah subscribe ke channel tertentu
    suspend fun isUserSubscribedToChannel(channelId: String): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            Log.d(TAG, "Checking if user is subscribed to channel: $channelId")
            val snapshot = firestore.collection("subscriptions")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("channelId", channelId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            val isSubscribed = snapshot.documents.isNotEmpty()
            Log.d(TAG, "User subscription status for channel $channelId: $isSubscribed")
            isSubscribed
        } catch (e: Exception) {
            Log.e(TAG, "Error checking user subscription: ${e.message}", e)
            false
        }
    }

    // Method untuk cancel subscription - PERBAIKAN
    suspend fun cancelSubscription(subscriptionId: String) {
        try {
            Log.d(TAG, "Cancelling subscription: $subscriptionId")

            // Coba update di collection subscriptions
            val subscriptionDoc = firestore.collection("subscriptions")
                .document(subscriptionId)
                .get()
                .await()

            if (subscriptionDoc.exists()) {
                firestore.collection("subscriptions")
                    .document(subscriptionId)
                    .update(
                        mapOf(
                            "isActive" to false,
                            "status" to "cancelled",
                            "updatedAt" to Timestamp.now()
                        )
                    )
                    .await()
                Log.d(TAG, "Subscription updated in subscriptions collection")
            }

            // Coba update di collection orders juga
            val orderDoc = firestore.collection("orders")
                .document(subscriptionId)
                .get()
                .await()

            if (orderDoc.exists()) {
                firestore.collection("orders")
                    .document(subscriptionId)
                    .update(
                        mapOf(
                            "status" to "cancelled",
                            "updatedAt" to Timestamp.now()
                        )
                    )
                    .await()
                Log.d(TAG, "Order updated in orders collection")
            }

            // Jika tidak ada di kedua collection, coba cari berdasarkan ID
            if (!subscriptionDoc.exists() && !orderDoc.exists()) {
                Log.w(TAG, "Subscription/Order not found with ID: $subscriptionId")

                // Coba cari di orders berdasarkan user dan channel
                val currentUser = auth.currentUser
                if (currentUser != null) {
                    val ordersQuery = firestore.collection("orders")
                        .whereEqualTo("userId", currentUser.uid)
                        .whereEqualTo("id", subscriptionId)
                        .get()
                        .await()

                    if (ordersQuery.documents.isNotEmpty()) {
                        val orderToCancel = ordersQuery.documents.first()
                        firestore.collection("orders")
                            .document(orderToCancel.id)
                            .update(
                                mapOf(
                                    "status" to "cancelled",
                                    "updatedAt" to Timestamp.now()
                                )
                            )
                            .await()
                        Log.d(TAG, "Found and cancelled order via query")
                    }
                }
            }

            Log.d(TAG, "Subscription cancellation completed successfully")

        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling subscription: ${e.message}", e)
            // Throw exception dengan pesan yang lebih informatif
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    throw Exception("PERMISSION_DENIED")
                }
                e.message?.contains("NOT_FOUND") == true -> {
                    throw Exception("SUBSCRIPTION_NOT_FOUND")
                }
                e.message?.contains("UNAVAILABLE") == true -> {
                    throw Exception("SERVICE_UNAVAILABLE")
                }
                else -> {
                    throw Exception("CANCELLATION_FAILED: ${e.message}")
                }
            }
        }
    }

    // Method untuk renew subscription
    suspend fun renewSubscription(order: Order): String {
        Log.d(TAG, "Renewing subscription for channel: ${order.channelName}")
        val renewedOrder = order.copy(
            id = "",
            status = "pending",
            createdAt = Timestamp.now(),
            updatedAt = Timestamp.now()
        )
        return createOrder(renewedOrder)
    }

    // Method untuk refresh channels - memastikan data terbaru dari Firestore
    suspend fun refreshChannels(): List<Channel> {
        return try {
            Log.d(TAG, "Refreshing channels data from Firestore")
            // Langsung query ke Firestore tanpa cache
            val snapshot = firestore.collection("channels")
                .orderBy("name")
                .get()
                .await()

            val allChannels = snapshot.documents.mapNotNull { doc ->
                try {
                    val channel = doc.toObject(Channel::class.java)?.copy(id = doc.id)
                    Log.d(TAG, "Refreshed channel: ${channel?.name} (Active: ${channel?.isActive})")
                    channel
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing channel document: ${doc.id}", e)
                    null
                }
            }

            Log.d(TAG, "Refreshed ${allChannels.size} channels from Firestore")
            allChannels
        } catch (e: Exception) {
            Log.e(TAG, "Error refreshing channels: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Method untuk change password
    suspend fun changePassword(currentPassword: String, newPassword: String): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            Log.d(TAG, "Starting password change process")

            // Re-authenticate user with current password
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
            currentUser.reauthenticate(credential).await()

            Log.d(TAG, "User re-authenticated successfully")

            // Update password
            currentUser.updatePassword(newPassword).await()

            Log.d(TAG, "Password updated successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error changing password: ${e.message}", e)
            false
        }
    }

    // Method untuk validate current password
    suspend fun validateCurrentPassword(currentPassword: String): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            Log.d(TAG, "Validating current password")
            val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)
            currentUser.reauthenticate(credential).await()

            Log.d(TAG, "Current password is valid")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Current password validation failed: ${e.message}", e)
            false
        }
    }

    // Method untuk reset password via email
    suspend fun sendPasswordResetEmail(email: String): Boolean {
        return try {
            Log.d(TAG, "Sending password reset email to: $email")
            auth.sendPasswordResetEmail(email).await()
            Log.d(TAG, "Password reset email sent successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error sending password reset email: ${e.message}", e)
            false
        }
    }

    // Method untuk mendapatkan semua channel (termasuk yang tidak aktif) untuk debugging
    suspend fun getAllChannels(): List<Channel> {
        return try {
            Log.d(TAG, "Getting all channels (including inactive)")
            val snapshot = firestore.collection("channels")
                .orderBy("name")
                .get()
                .await()

            val channels = snapshot.documents.mapNotNull { doc ->
                doc.toObject(Channel::class.java)?.copy(id = doc.id)
            }

            Log.d(TAG, "Found ${channels.size} total channels")
            channels.forEach { channel ->
                Log.d(TAG, "Channel: ${channel.name} (ID: ${channel.id}, Active: ${channel.isActive})")
            }

            channels
        } catch (e: Exception) {
            Log.e(TAG, "Error getting all channels: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }
}