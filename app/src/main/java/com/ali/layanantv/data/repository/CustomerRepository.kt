package com.ali.layanantv.data.repository

import com.ali.layanantv.data.model.Channel
import com.ali.layanantv.data.model.CustomerDashboardStats
import com.ali.layanantv.data.model.Order
import com.ali.layanantv.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await

class CustomerRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Get all active channels
    suspend fun getActiveChannels(): List<Channel> {
        return try {
            val snapshot = firestore.collection("channels")
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Channel::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get channels by category
    suspend fun getChannelsByCategory(category: String): List<Channel> {
        return try {
            val snapshot = firestore.collection("channels")
                .whereEqualTo("isActive", true)
                .whereEqualTo("category", category)
                .orderBy("name")
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Channel::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // Get channel by ID
    suspend fun getChannelById(channelId: String): Channel? {
        return try {
            val snapshot = firestore.collection("channels")
                .document(channelId)
                .get()
                .await()
            snapshot.toObject(Channel::class.java)?.copy(id = snapshot.id)
        } catch (e: Exception) {
            null
        }
    }

    // Create new order
    suspend fun createOrder(order: Order): String {
        val docRef = firestore.collection("orders").add(order).await()
        return docRef.id
    }

    // FIXED: Get user's orders dengan error handling yang lebih baik
    suspend fun getUserOrders(): List<Order> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            // Log error untuk debugging
            e.printStackTrace()
            emptyList()
        }
    }

    // FIXED: Get user's active subscriptions - menggunakan collection subscriptions yang sebenarnya
    suspend fun getUserSubscriptions(): List<Order> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            // Pertama, coba ambil dari collection subscriptions
            val subscriptionsSnapshot = firestore.collection("subscriptions")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            // Jika ada subscriptions, konversi ke Order format untuk kompatibilitas
            if (subscriptionsSnapshot.documents.isNotEmpty()) {
                subscriptionsSnapshot.documents.mapNotNull { doc ->
                    val data = doc.data
                    if (data != null) {
                        Order(
                            id = doc.id,
                            userId = data["userId"] as? String ?: "",
                            channelId = data["channelId"] as? String ?: "",
                            channelName = data["channelName"] as? String ?: "",
                            subscriptionType = data["subscriptionType"] as? String ?: "",
                            status = "completed",
                            createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                            updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                        )
                    } else null
                }
            } else {
                // Fallback ke orders yang completed
                val ordersSnapshot = firestore.collection("orders")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereEqualTo("status", "completed")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .get()
                    .await()

                ordersSnapshot.documents.mapNotNull { doc ->
                    doc.toObject(Order::class.java)?.copy(id = doc.id)
                }
            }
        } catch (e: Exception) {
            // Log error untuk debugging
            e.printStackTrace()
            emptyList()
        }
    }

    // Get current user info
    suspend fun getCurrentUser(): User? {
        val currentUser = auth.currentUser ?: return null

        return try {
            val snapshot = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            snapshot.toObject(User::class.java)?.copy(uid = snapshot.id)
        } catch (e: Exception) {
            null
        }
    }

    // Update user profile
    suspend fun updateUserProfile(user: User) {
        val currentUser = auth.currentUser ?: return

        try {
            firestore.collection("users")
                .document(currentUser.uid)
                .set(user)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // FIXED: Get user points dengan error handling yang lebih baik
    suspend fun getUserPoints(): Int {
        val currentUser = auth.currentUser ?: return 0

        return try {
            val snapshot = firestore.collection("users")
                .document(currentUser.uid)
                .get()
                .await()

            snapshot.getLong("points")?.toInt() ?: 2500
        } catch (e: Exception) {
            2500 // Default points jika error
        }
    }

    // FIXED: Get available channels dengan error handling yang lebih baik
    suspend fun getAvailableChannels(): List<Channel> {
        return try {
            val channelsSnapshot = firestore.collection("channels")
                .whereEqualTo("isActive", true)
                .orderBy("name")
                .get()
                .await()

            channelsSnapshot.documents.mapNotNull { doc ->
                doc.toObject(Channel::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // FIXED: Get dashboard stats dengan error handling yang lebih baik
    suspend fun getCustomerDashboardStats(): CustomerDashboardStats {
        val currentUser = auth.currentUser ?: return CustomerDashboardStats()

        return try {
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

            CustomerDashboardStats(
                totalOrders = totalOrders,
                activeSubscriptions = activeSubscriptions,
                pendingOrders = pendingOrders
            )
        } catch (e: Exception) {
            e.printStackTrace()
            CustomerDashboardStats()
        }
    }

    // TAMBAHAN: Method untuk mendapatkan order history dengan pagination
    suspend fun getOrderHistory(limit: Int = 10, lastOrderId: String? = null): List<Order> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            var query = firestore.collection("orders")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            // Jika ada lastOrderId, mulai dari setelah dokumen tersebut
            if (lastOrderId != null) {
                val lastDoc = firestore.collection("orders")
                    .document(lastOrderId)
                    .get()
                    .await()
                query = query.startAfter(lastDoc)
            }

            val snapshot = query.get().await()

            snapshot.documents.mapNotNull { doc ->
                doc.toObject(Order::class.java)?.copy(id = doc.id)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // TAMBAHAN: Method untuk mendapatkan subscription history
    suspend fun getSubscriptionHistory(): List<Order> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            val snapshot = firestore.collection("subscriptions")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data
                if (data != null) {
                    Order(
                        id = doc.id,
                        userId = data["userId"] as? String ?: "",
                        channelId = data["channelId"] as? String ?: "",
                        channelName = data["channelName"] as? String ?: "",
                        subscriptionType = data["subscriptionType"] as? String ?: "",
                        status = if (data["isActive"] as? Boolean == true) "active" else "expired",
                        createdAt = data["createdAt"] as? Timestamp ?: Timestamp.now(),
                        updatedAt = data["updatedAt"] as? Timestamp ?: Timestamp.now()
                    )
                } else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // TAMBAHAN: Method untuk check apakah user sudah subscribe ke channel tertentu
    suspend fun isUserSubscribedToChannel(channelId: String): Boolean {
        val currentUser = auth.currentUser ?: return false

        return try {
            val snapshot = firestore.collection("subscriptions")
                .whereEqualTo("userId", currentUser.uid)
                .whereEqualTo("channelId", channelId)
                .whereEqualTo("isActive", true)
                .get()
                .await()

            snapshot.documents.isNotEmpty()
        } catch (e: Exception) {
            false
        }
    }

    // TAMBAHAN: Method untuk mendapatkan notification user
    suspend fun getUserNotifications(): List<Map<String, Any>> {
        val currentUser = auth.currentUser ?: return emptyList()

        return try {
            val snapshot = firestore.collection("notifications")
                .whereEqualTo("userId", currentUser.uid)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                doc.data
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}