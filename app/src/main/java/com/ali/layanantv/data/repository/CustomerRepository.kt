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
import kotlinx.coroutines.tasks.await

class CustomerRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    companion object {
        private const val TAG = "CustomerRepository"
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
        Log.d(TAG, "Creating order for channel: ${order.channelName}")
        val docRef = firestore.collection("orders").add(order).await()
        Log.d(TAG, "Order created with ID: ${docRef.id}")
        return docRef.id
    }

    // Create subscription record when order is completed
    // Add this improved createSubscription method to CustomerRepository
    suspend fun createSubscription(order: Order) {
        try {
            Log.d(TAG, "Creating subscription for order: ${order.id}")
            Log.d(TAG, "Order details: userId=${order.userId}, channelId=${order.channelId}, channelName=${order.channelName}, status=${order.status}")

            // Pastikan order memiliki data yang lengkap
            if (order.userId.isEmpty() || order.channelId.isEmpty() || order.channelName.isNullOrEmpty()) {
                Log.e(TAG, "Order data incomplete: userId=${order.userId}, channelId=${order.channelId}, channelName=${order.channelName}")
                throw IllegalArgumentException("Order data is incomplete")
            }

            // Check if subscription already exists
            val existingSubscription = firestore.collection("subscriptions")
                .whereEqualTo("userId", order.userId)
                .whereEqualTo("channelId", order.channelId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            if (existingSubscription.documents.isNotEmpty()) {
                Log.w(TAG, "Subscription already exists for user ${order.userId} and channel ${order.channelId}")

                // Update existing subscription instead of creating new one
                val existingDoc = existingSubscription.documents.first()
                firestore.collection("subscriptions")
                    .document(existingDoc.id)
                    .update(
                        mapOf(
                            "orderId" to order.id,
                            "totalAmount" to order.totalAmount,
                            "subscriptionType" to order.subscriptionType,
                            "updatedAt" to Timestamp.now()
                        )
                    )
                    .await()

                Log.d(TAG, "Updated existing subscription with new order data")
                return
            }

            // Create new subscription
            val subscription = hashMapOf(
                "userId" to order.userId,
                "channelId" to order.channelId,
                "channelName" to order.channelName,
                "subscriptionType" to order.subscriptionType,
                "totalAmount" to order.totalAmount,
                "orderId" to order.id,
                "isActive" to true,
                "status" to "active",
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )

            val docRef = firestore.collection("subscriptions").add(subscription).await()
            Log.d(TAG, "Subscription created successfully with ID: ${docRef.id}")

            // Update order status to ensure it's completed
            if (order.id.isNotEmpty()) {
                val updateData = mapOf(
                    "status" to "completed",
                    "paymentVerified" to true,
                    "updatedAt" to Timestamp.now()
                )

                firestore.collection("orders")
                    .document(order.id)
                    .update(updateData)
                    .await()
                Log.d(TAG, "Order status updated to completed with paymentVerified=true")
            }

            Log.d(TAG, "Successfully created subscription and updated order")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating subscription: ${e.message}", e)
            throw e // Re-throw to be handled by caller
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

    // Method untuk mendapatkan notification user
    suspend fun getUserNotifications(): List<Map<String, Any>> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            Log.d(TAG, "Getting user notifications")
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val notifications = snapshot.documents.mapNotNull { doc ->
                doc.data
            }

            Log.d(TAG, "Found ${notifications.size} notifications")
            notifications
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user notifications: ${e.message}", e)
            e.printStackTrace()
            emptyList()
        }
    }

    // Method untuk cancel subscription
    suspend fun cancelSubscription(subscriptionId: String) {
        try {
            Log.d(TAG, "Cancelling subscription: $subscriptionId")
            firestore.collection("subscriptions")
                .document(subscriptionId)
                .update("isActive", false, "updatedAt", Timestamp.now())
                .await()
            Log.d(TAG, "Subscription cancelled successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling subscription: ${e.message}", e)
            e.printStackTrace()
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