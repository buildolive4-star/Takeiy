package com.example.firebase

import android.content.Context
import androidx.annotation.Keep
import com.example.R
import com.example.data.*
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Keep
@IgnoreExtraProperties
data class CloudUserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userName: String = "Rohit Sharma",
    val userPhone: String = "+91 98765 43210",
    val userEmail: String = "",
    val activeAddressLabel: String = "Sector 62, Noida",
    val activeAddressLine: String = "Sector 62, Noida • Electronic City",
    val walletBalance: Double = 2450.0,
    val rewardPoints: Long = 1280L,
    val activePanelName: String = "CUSTOMER",
    val driverOnline: Boolean = true,
    val deliveryPartnerOnline: Boolean = true,
    val language: String = "English (India)",
    val notificationsEnabled: Boolean = true,
    val avatarUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudServiceOrder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val orderType: String = "FOOD",
    val title: String = "",
    val subtitle: String = "",
    val status: String = "PENDING",
    val amount: Double = 0.0,
    val paymentMethod: String = "Wallet",
    val pickupLocation: String = "",
    val dropLocation: String = "",
    val otp: String = "1234",
    val partnerName: String = "Take It Partner",
    val rating: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudWalletTransaction(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val txType: String = "CREDIT",
    val amount: Double = 0.0,
    val method: String = "Wallet",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudAppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val body: String = "",
    val category: String = "ORDER",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudMenuItem(
    @DocumentId val id: String = "",
    val userId: String = "",
    val restaurantId: String = "r1",
    val category: String = "Chef Specials",
    val name: String = "",
    val description: String = "",
    val price: Long = 149L,
    val vegFlag: Boolean = true,
    val availableFlag: Boolean = true,
    val bestsellerFlag: Boolean = false,
    val rating: Double = 4.6,
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudGroceryProduct(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "Daily Essentials",
    val unit: String = "1 Pack",
    val price: Long = 49L,
    val mrp: Long = 69L,
    val stockCount: Long = 20L,
    val badge: String = "Fresh",
    val imageUrl: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudSavedAddress(
    @DocumentId val id: String = "",
    val userId: String = "",
    val label: String = "",
    val fullAddress: String = "",
    val landmark: String = "",
    val latitude: Double = 28.6279,
    val longitude: Double = 77.3649,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

class TakeItCloudRepository(private val db: FirebaseFirestore) {

    // CRITICAL: Always resolve custom provisioned database ID from firebase_applet_config.xml
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        val uid = auth.currentUser?.uid
        if (uid.isNullOrBlank()) {
            throw IllegalStateException("User must be signed in with Google before accessing Firestore.")
        }
        return uid
    }

    private fun sanitizeDocId(raw: String): String {
        val cleaned = raw.replace(Regex("[^a-zA-Z0-9_\\-]"), "_").take(120)
        return cleaned.ifBlank { "doc_${System.currentTimeMillis()}" }
    }

    private fun <T> Flow<T>.wrapFirestoreFlow(uid: String, path: String, op: OperationType): Flow<T> {
        return this.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val toLog = firestoreError ?: (error as? Exception)
            if (toLog != null && auth.currentUser?.uid == uid) {
                handleFirestoreError(toLog, op, path)
            }
            throw error
        }
    }

    // --- REALTIME SNAPSHOT OBSERVERS ---

    fun observeUserProfile(uid: String): Flow<CloudUserProfile?> = flow {
        require(uid.isNotBlank()) { "UID cannot be blank" }
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap ->
                    if (!snap.exists()) null
                    else snap.toObject(CloudUserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                }
                .wrapFirestoreFlow(uid, path, OperationType.GET)
        )
    }

    fun observeOrders(uid: String): Flow<List<OrderEntity>> = flow {
        require(uid.isNotBlank()) { "UID cannot be blank" }
        val path = "users/$uid/orders"
        emitAll(
            db.collection("users").document(uid).collection("orders")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudServiceOrder::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).map { doc ->
                        OrderEntity(
                            id = doc.id,
                            type = doc.orderType,
                            title = doc.title,
                            subtitle = doc.subtitle,
                            status = doc.status,
                            amount = doc.amount,
                            paymentMethod = doc.paymentMethod,
                            pickupLocation = doc.pickupLocation,
                            dropLocation = doc.dropLocation,
                            otp = doc.otp,
                            partnerName = doc.partnerName,
                            timestamp = doc.createdAt?.toDate()?.time ?: System.currentTimeMillis(),
                            rating = doc.rating.toInt()
                        )
                    }
                }
                .wrapFirestoreFlow(uid, path, OperationType.LIST)
        )
    }

    fun observeTransactions(uid: String): Flow<List<WalletTxEntity>> = flow {
        require(uid.isNotBlank()) { "UID cannot be blank" }
        val path = "users/$uid/wallet_transactions"
        emitAll(
            db.collection("users").document(uid).collection("wallet_transactions")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudWalletTransaction::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).map { doc ->
                        WalletTxEntity(
                            id = doc.id,
                            title = doc.title,
                            txType = doc.txType,
                            amount = doc.amount,
                            method = doc.method,
                            timestamp = doc.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                }
                .wrapFirestoreFlow(uid, path, OperationType.LIST)
        )
    }

    fun observeNotifications(uid: String): Flow<List<NotificationEntity>> = flow {
        require(uid.isNotBlank()) { "UID cannot be blank" }
        val path = "users/$uid/notifications"
        emitAll(
            db.collection("users").document(uid).collection("notifications")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .snapshots()
                .map { snap ->
                    snap.toObjects(CloudAppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE).map { doc ->
                        NotificationEntity(
                            id = doc.id,
                            category = doc.category,
                            title = doc.title,
                            message = doc.body,
                            isRead = doc.readFlag,
                            timestamp = doc.createdAt?.toDate()?.time ?: System.currentTimeMillis()
                        )
                    }
                }
                .wrapFirestoreFlow(uid, path, OperationType.LIST)
        )
    }

    fun observeMenuItemsWithImages(uid: String): Flow<Pair<List<MenuItemEntity>, Map<String, String>>> = flow {
        require(uid.isNotBlank()) { "UID cannot be blank" }
        val path = "users/$uid/menu_items"
        emitAll(
            db.collection("users").document(uid).collection("menu_items")
                .snapshots()
                .map { snap ->
                    val docs = snap.toObjects(CloudMenuItem::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val entities = docs.map { doc ->
                        MenuItemEntity(
                            id = doc.id,
                            restaurantId = doc.restaurantId,
                            category = doc.category,
                            name = doc.name,
                            description = doc.description,
                            price = doc.price.toInt(),
                            isVeg = doc.vegFlag,
                            isAvailable = doc.availableFlag,
                            isBestseller = doc.bestsellerFlag,
                            rating = doc.rating
                        )
                    }
                    val images = docs.filter { it.imageUrl.startsWith("https://") }.associate { it.id to it.imageUrl }
                    entities to images
                }
                .wrapFirestoreFlow(uid, path, OperationType.LIST)
        )
    }

    fun observeGroceryProductsWithImages(uid: String): Flow<Pair<List<GroceryProductEntity>, Map<String, String>>> = flow {
        require(uid.isNotBlank()) { "UID cannot be blank" }
        val path = "users/$uid/grocery_products"
        emitAll(
            db.collection("users").document(uid).collection("grocery_products")
                .snapshots()
                .map { snap ->
                    val docs = snap.toObjects(CloudGroceryProduct::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    val entities = docs.map { doc ->
                        GroceryProductEntity(
                            id = doc.id,
                            name = doc.name,
                            category = doc.category,
                            unit = doc.unit,
                            mrp = doc.mrp.toInt(),
                            sellingPrice = doc.price.toInt(),
                            stockCount = doc.stockCount.toInt(),
                            deliveryMins = 10,
                            isFeatured = doc.badge.isNotBlank()
                        )
                    }
                    val images = docs.filter { it.imageUrl.startsWith("https://") }.associate { it.id to it.imageUrl }
                    entities to images
                }
                .wrapFirestoreFlow(uid, path, OperationType.LIST)
        )
    }

    // --- CRASH-SAFE CLOUD MUTATIONS (Result<Unit>) ---

    suspend fun ensureProfileInitialized(
        displayName: String,
        email: String
    ): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val safeName = displayName.trim().take(80).ifBlank { "Take It Member" }
                val safeEmail = email.trim().take(120)
                val payload = mapOf(
                    "userId" to uid,
                    "userName" to safeName,
                    "userPhone" to "+91 98765 43210",
                    "userEmail" to safeEmail,
                    "activeAddressLabel" to "Sector 62, Noida",
                    "activeAddressLine" to "Sector 62, Noida • Electronic City",
                    "walletBalance" to 2450.0,
                    "rewardPoints" to 1280L,
                    "activePanelName" to "CUSTOMER",
                    "driverOnline" to true,
                    "deliveryPartnerOnline" to true,
                    "language" to "English (India)",
                    "notificationsEnabled" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateProfileFields(fields: Map<String, Any?>): Result<Unit> {
        var docPath = "users/unauthenticated"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid)
            docPath = docRef.path
            val cleanFields = fields
                .filterValues { it != null }
                .mapValues { it.value as Any } + ("updatedAt" to FieldValue.serverTimestamp())
            docRef.update(cleanFields).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun createOrder(order: OrderEntity): Result<Unit> {
        var docPath = "users/unauthenticated/orders"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(order.id)
            val docRef = db.collection("users").document(uid).collection("orders").document(docId)
            docPath = docRef.path
            val validType = order.type.uppercase().takeIf {
                it in setOf("RIDE", "FOOD", "GROCERY", "PARCEL", "TRANSPORT", "SHOPPING")
            } ?: "FOOD"
            val validStatus = order.status.uppercase().takeIf {
                it in setOf("PENDING", "ACCEPTED", "PREPARING", "ON_THE_WAY", "COMPLETED", "CANCELLED", "REFUNDED")
            } ?: "PENDING"
            val payload = mapOf(
                "userId" to uid,
                "orderType" to validType,
                "title" to order.title.trim().take(140).ifBlank { "Take It Order" },
                "subtitle" to order.subtitle.trim().take(240),
                "status" to validStatus,
                "amount" to order.amount.coerceIn(0.0, 500000.0),
                "paymentMethod" to order.paymentMethod.trim().take(40).ifBlank { "Wallet" },
                "pickupLocation" to order.pickupLocation.trim().take(240),
                "dropLocation" to order.dropLocation.trim().take(240),
                "otp" to order.otp.trim().take(12).ifBlank { "1234" },
                "partnerName" to order.partnerName.trim().take(80),
                "rating" to order.rating.coerceIn(0, 5).toLong(),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> {
        var docPath = "users/unauthenticated/orders/$orderId"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(orderId)
            val docRef = db.collection("users").document(uid).collection("orders").document(docId)
            docPath = docRef.path
            val safeStatus = newStatus.uppercase().takeIf {
                it in setOf("PENDING", "ACCEPTED", "PREPARING", "ON_THE_WAY", "COMPLETED", "CANCELLED", "REFUNDED")
            } ?: "ON_THE_WAY"
            docRef.update(
                mapOf(
                    "status" to safeStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun rateOrder(orderId: String, stars: Int): Result<Unit> {
        var docPath = "users/unauthenticated/orders/$orderId"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(orderId)
            val docRef = db.collection("users").document(uid).collection("orders").document(docId)
            docPath = docRef.path
            docRef.update(
                mapOf(
                    "rating" to stars.coerceIn(1, 5).toLong(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun recordWalletTransaction(tx: WalletTxEntity): Result<Unit> {
        var docPath = "users/unauthenticated/wallet_transactions"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(tx.id)
            val docRef = db.collection("users").document(uid).collection("wallet_transactions").document(docId)
            docPath = docRef.path
            val safeType = tx.txType.uppercase().takeIf {
                it in setOf("CREDIT", "DEBIT", "CASHBACK", "REFUND", "PROMO")
            } ?: "CREDIT"
            val payload = mapOf(
                "userId" to uid,
                "title" to tx.title.trim().take(140).ifBlank { "Wallet Transaction" },
                "txType" to safeType,
                "amount" to tx.amount.coerceIn(0.0, 500000.0),
                "method" to tx.method.trim().take(60).ifBlank { "Wallet" },
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun pushNotification(id: String, category: String, title: String, body: String): Result<Unit> {
        var docPath = "users/unauthenticated/notifications"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(id)
            val docRef = db.collection("users").document(uid).collection("notifications").document(docId)
            docPath = docRef.path
            val payload = mapOf(
                "userId" to uid,
                "title" to title.trim().take(120).ifBlank { "Take It Update" },
                "body" to body.trim().take(400).ifBlank { "Notification update" },
                "category" to category.trim().take(40).ifBlank { "ORDER" },
                "readFlag" to false,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docPath)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(ids: List<String>): Result<Unit> {
        var docPath = "users/unauthenticated/notifications"
        return try {
            val uid = requireUserId()
            val batch = db.batch()
            ids.forEach { rawId ->
                val ref = db.collection("users").document(uid).collection("notifications").document(sanitizeDocId(rawId))
                docPath = ref.path
                batch.update(ref, mapOf("readFlag" to true, "updatedAt" to FieldValue.serverTimestamp()))
            }
            if (ids.isNotEmpty()) batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docPath)
            Result.failure(e)
        }
    }

    private fun sanitizeHttpsUrl(url: String?): String {
        val trimmed = url?.trim().orEmpty()
        return if (trimmed.startsWith("https://") && trimmed.length <= 500) trimmed else ""
    }

    suspend fun upsertMenuItem(item: MenuItemEntity, imageUrl: String? = null): Result<Unit> {
        var docPath = "users/unauthenticated/menu_items"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(item.id)
            val docRef = db.collection("users").document(uid).collection("menu_items").document(docId)
            docPath = docRef.path
            val safeImg = sanitizeHttpsUrl(imageUrl)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "restaurantId" to item.restaurantId.take(60),
                    "category" to item.category.take(60),
                    "name" to item.name.take(120),
                    "description" to item.description.take(300),
                    "price" to item.price.coerceIn(1, 50000).toLong(),
                    "vegFlag" to item.isVeg,
                    "availableFlag" to item.isAvailable,
                    "bestsellerFlag" to item.isBestseller,
                    "rating" to item.rating.coerceIn(0.0, 5.0),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeImg.isNotEmpty()) updates["imageUrl"] = safeImg
                docRef.update(updates).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "restaurantId" to item.restaurantId.take(60),
                        "category" to item.category.take(60),
                        "name" to item.name.take(120),
                        "description" to item.description.take(300),
                        "price" to item.price.coerceIn(1, 50000).toLong(),
                        "vegFlag" to item.isVeg,
                        "availableFlag" to item.isAvailable,
                        "bestsellerFlag" to item.isBestseller,
                        "rating" to item.rating.coerceIn(0.0, 5.0),
                        "imageUrl" to safeImg,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun upsertGroceryProduct(product: GroceryProductEntity, imageUrl: String? = null): Result<Unit> {
        var docPath = "users/unauthenticated/grocery_products"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId(product.id)
            val docRef = db.collection("users").document(uid).collection("grocery_products").document(docId)
            docPath = docRef.path
            val safeImg = sanitizeHttpsUrl(imageUrl)
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to product.name.take(120),
                    "category" to product.category.take(60),
                    "unit" to product.unit.take(40),
                    "price" to product.sellingPrice.coerceIn(1, 50000).toLong(),
                    "mrp" to product.mrp.coerceIn(1, 60000).toLong(),
                    "stockCount" to product.stockCount.coerceIn(0, 100000).toLong(),
                    "badge" to (if (product.isFeatured) "Fresh" else ""),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (safeImg.isNotEmpty()) updates["imageUrl"] = safeImg
                docRef.update(updates).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "name" to product.name.take(120),
                        "category" to product.category.take(60),
                        "unit" to product.unit.take(40),
                        "price" to product.sellingPrice.coerceIn(1, 50000).toLong(),
                        "mrp" to product.mrp.coerceIn(1, 60000).toLong(),
                        "stockCount" to product.stockCount.coerceIn(0, 100000).toLong(),
                        "badge" to (if (product.isFeatured) "Fresh" else ""),
                        "imageUrl" to safeImg,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun saveGpsAddress(label: String, fullAddress: String, landmark: String, lat: Double, lng: Double): Result<Unit> {
        var docPath = "users/unauthenticated/addresses"
        return try {
            val uid = requireUserId()
            val docId = sanitizeDocId("gps_${label.lowercase()}")
            val docRef = db.collection("users").document(uid).collection("addresses").document(docId)
            docPath = docRef.path
            val existingSnap = docRef.get().await()
            if (existingSnap.exists()) {
                docRef.update(
                    mapOf(
                        "label" to label.take(60),
                        "fullAddress" to fullAddress.take(240),
                        "landmark" to landmark.take(160),
                        "latitude" to lat.coerceIn(-90.0, 90.0),
                        "longitude" to lng.coerceIn(-180.0, 180.0),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.set(
                    mapOf(
                        "userId" to uid,
                        "label" to label.take(60),
                        "fullAddress" to fullAddress.take(240),
                        "landmark" to landmark.take(160),
                        "latitude" to lat.coerceIn(-90.0, 90.0),
                        "longitude" to lng.coerceIn(-180.0, 180.0),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }
}
