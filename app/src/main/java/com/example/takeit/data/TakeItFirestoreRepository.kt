package com.example.takeit.data

import android.content.Context
import android.util.Log
import com.example.R
import com.example.takeit.model.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class TakeItFirestoreRepository(context: Context) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(
        context.getString(R.string.firestore_database_id)
    )

    private fun requireUid(): String {
        val uid = Firebase.auth.currentUser?.uid
        check(!uid.isNullOrBlank()) { "Please sign in with Google to continue." }
        return uid
    }

    private fun cleanId(prefix: String): String {
        val token = UUID.randomUUID().toString().replace("-", "").take(12)
        return "${prefix}_$token"
    }

    private fun logError(op: String, path: String, e: Exception) {
        Log.e("TakeItFirestore", "Firestore $op failed at $path: ${e.message}", e)
    }

    private inline fun <reified T> DocumentSnapshot.toSafeObject(): T? {
        return try {
            toObject(T::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
        } catch (e: Exception) {
            Log.w("TakeItFirestore", "Deserialization skipped for ${reference.path}: ${e.message}")
            null
        }
    }

    // --- LIVE OBSERVERS ---

    fun observeConfig(): Flow<TakeItAppConfig> =
        db.collection("takeit_config").document("live")
            .snapshots(MetadataChanges.INCLUDE)
            .map { snap -> snap.toSafeObject<TakeItAppConfig>() ?: TakeItAppConfig() }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(TakeItAppConfig())
            }

    fun observeRestaurants(): Flow<List<Restaurant>> =
        db.collection("takeit_restaurants")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<Restaurant>() }
                    .sortedWith(compareByDescending<Restaurant> { it.featuredFlag }.thenByDescending { it.rating })
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeMenuItems(): Flow<List<MenuItem>> =
        db.collection("takeit_menu_items")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<MenuItem>() }
                    .sortedWith(compareByDescending<MenuItem> { it.bestsellerFlag }.thenBy { it.price })
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeCoupons(): Flow<List<PromoCoupon>> =
        db.collection("takeit_coupons")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<PromoCoupon>() }
                    .sortedByDescending { it.discountValue }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeMyOrders(uid: String): Flow<List<FoodOrder>> =
        db.collection("takeit_orders")
            .whereEqualTo("userId", uid)
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<FoodOrder>() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeAllOrders(): Flow<List<FoodOrder>> =
        db.collection("takeit_orders")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<FoodOrder>() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeMyProfile(uid: String): Flow<TakeItUserProfile?> =
        db.collection("takeit_users").document(uid)
            .snapshots(MetadataChanges.INCLUDE)
            .map { it.toSafeObject<TakeItUserProfile>() }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(null)
            }

    fun observeAllUsers(): Flow<List<TakeItUserProfile>> =
        db.collection("takeit_users")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<TakeItUserProfile>() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeAddresses(uid: String): Flow<List<SavedAddress>> =
        db.collection("takeit_users").document(uid).collection("addresses")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<SavedAddress>() }
                    .sortedByDescending { it.updatedAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeWalletTransactions(uid: String): Flow<List<WalletTx>> =
        db.collection("takeit_users").document(uid).collection("wallet_transactions")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<WalletTx>() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeAllReviews(): Flow<List<RestaurantReview>> =
        db.collection("takeit_reviews")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<RestaurantReview>() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeMyTickets(uid: String): Flow<List<SupportTicket>> =
        db.collection("takeit_support_tickets")
            .whereEqualTo("userId", uid)
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<SupportTicket>() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeAllTickets(): Flow<List<SupportTicket>> =
        db.collection("takeit_support_tickets")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<SupportTicket>() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    fun observeCoAdmins(): Flow<List<CoAdminAccount>> =
        db.collection("takeit_co_admins")
            .snapshots(MetadataChanges.INCLUDE)
            .map { qs ->
                qs.documents.mapNotNull { it.toSafeObject<CoAdminAccount>() }
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                emit(emptyList())
            }

    // --- AUTH & CO-ADMIN LOOKUP ---

    suspend fun fetchMyCoAdminRecord(email: String): Result<CoAdminAccount?> {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isBlank()) return Result.success(null)
        return try {
            val snap = db.collection("takeit_co_admins").document(cleanEmail).get().await()
            Result.success(snap.toSafeObject<CoAdminAccount>())
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("get", "takeit_co_admins/$cleanEmail", e)
            Result.failure(e)
        }
    }

    suspend fun ensureUserProfile(uid: String, name: String, email: String): Result<Unit> {
        return try {
            val ref = db.collection("takeit_users").document(uid)
            val snap = ref.get().await()
            if (!snap.exists()) {
                val safeName = name.ifBlank { email.substringBefore("@").ifBlank { "Take It Foodie" } }.take(80)
                val payload = mapOf(
                    "userId" to uid,
                    "name" to safeName,
                    "email" to email.take(120),
                    "phone" to "",
                    "avatarUrl" to "",
                    "activeAddressLabel" to "Home",
                    "activeAddressLine" to "Greater Noida, Sector 62",
                    "walletBalance" to 0.0,
                    "goldMemberFlag" to true,
                    "suspendedFlag" to false,
                    "favoriteRestaurantIds" to emptyList<String>(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                ref.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("ensureUserProfile", "takeit_users/$uid", e)
            Result.failure(e)
        }
    }

    // --- CUSTOMER MUTATIONS ---

    suspend fun updateMyProfile(
        name: String? = null,
        phone: String? = null,
        avatarUrl: String? = null,
        activeAddressLabel: String? = null,
        activeAddressLine: String? = null,
        favoriteIds: List<String>? = null
    ): Result<Unit> {
        val uid = requireUid()
        return try {
            val updates = mutableMapOf<String, Any>("updatedAt" to FieldValue.serverTimestamp())
            name?.takeIf { it.isNotBlank() }?.let { updates["name"] = it.trim().take(80) }
            phone?.let { updates["phone"] = it.trim().take(30) }
            avatarUrl?.let { updates["avatarUrl"] = it.trim().take(500) }
            activeAddressLabel?.let { updates["activeAddressLabel"] = it.trim().take(60) }
            activeAddressLine?.let { updates["activeAddressLine"] = it.trim().take(260) }
            favoriteIds?.let { updates["favoriteRestaurantIds"] = it.take(100) }
            db.collection("takeit_users").document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("update", "takeit_users/$uid", e)
            Result.failure(e)
        }
    }

    suspend fun saveAddress(label: String, fullAddress: String, landmark: String): Result<Unit> {
        val uid = requireUid()
        val id = cleanId("addr")
        return try {
            val payload = mapOf(
                "userId" to uid,
                "label" to label.trim().ifBlank { "Home" }.take(60),
                "fullAddress" to fullAddress.trim().take(250),
                "landmark" to landmark.trim().take(150),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("takeit_users").document(uid).collection("addresses").document(id).set(payload).await()
            updateMyProfile(activeAddressLabel = label, activeAddressLine = fullAddress)
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("create", "takeit_users/$uid/addresses/$id", e)
            Result.failure(e)
        }
    }

    suspend fun deleteAddress(addressId: String): Result<Unit> {
        val uid = requireUid()
        return try {
            db.collection("takeit_users").document(uid).collection("addresses").document(addressId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("delete", "takeit_users/$uid/addresses/$addressId", e)
            Result.failure(e)
        }
    }

    suspend fun recordWalletTopUp(amount: Double, method: String, title: String): Result<Unit> {
        val uid = requireUid()
        val txId = cleanId("tx")
        return try {
            val userRef = db.collection("takeit_users").document(uid)
            val txRef = userRef.collection("wallet_transactions").document(txId)
            db.runBatch { batch ->
                batch.update(userRef, mapOf(
                    "walletBalance" to FieldValue.increment(amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                batch.set(txRef, mapOf(
                    "userId" to uid,
                    "title" to title.take(120),
                    "txType" to "CREDIT",
                    "amount" to amount,
                    "method" to method.take(50),
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("walletTopUp", "takeit_users/$uid", e)
            Result.failure(e)
        }
    }

    suspend fun placeOrder(
        order: FoodOrder,
        deductWalletAmount: Double = 0.0
    ): Result<String> {
        val uid = requireUid()
        val id = cleanId("ord")
        return try {
            val orderRef = db.collection("takeit_orders").document(id)
            val payload = mapOf(
                "orderNumber" to order.orderNumber.take(35),
                "userId" to uid,
                "customerName" to order.customerName.ifBlank { "Customer" }.take(80),
                "customerEmail" to order.customerEmail.take(120),
                "customerPhone" to order.customerPhone.take(30),
                "restaurantId" to order.restaurantId.take(120),
                "restaurantName" to order.restaurantName.take(100),
                "itemsSummary" to order.itemsSummary.take(480),
                "itemTotal" to order.itemTotal.coerceAtLeast(0),
                "deliveryFee" to order.deliveryFee.coerceAtLeast(0),
                "platformFee" to order.platformFee.coerceAtLeast(0),
                "gstAmount" to order.gstAmount.coerceAtLeast(0),
                "discountAmount" to order.discountAmount.coerceAtLeast(0),
                "couponCode" to order.couponCode.take(35),
                "totalAmount" to order.totalAmount.coerceAtLeast(0),
                "paymentMethod" to order.paymentMethod,
                "paymentStatus" to order.paymentStatus,
                "paymentRef" to order.paymentRef.take(100),
                "deliveryAddress" to order.deliveryAddress.ifBlank { "Greater Noida" }.take(280),
                "status" to "PLACED",
                "riderName" to "Assigning Rider...",
                "riderPhone" to "",
                "riderVehicle" to "",
                "etaMinutes" to 25,
                "otp" to order.otp.take(8),
                "rating" to 0,
                "reviewText" to "",
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.runBatch { batch ->
                batch.set(orderRef, payload)
                if (deductWalletAmount > 0.0) {
                    val userRef = db.collection("takeit_users").document(uid)
                    val txRef = userRef.collection("wallet_transactions").document(cleanId("tx"))
                    batch.update(userRef, mapOf(
                        "walletBalance" to FieldValue.increment(-deductWalletAmount),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                    batch.set(txRef, mapOf(
                        "userId" to uid,
                        "title" to "Order ${order.orderNumber} (${order.restaurantName})".take(120),
                        "txType" to "DEBIT",
                        "amount" to deductWalletAmount,
                        "method" to "WALLET",
                        "createdAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()
            Result.success(id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("placeOrder", "takeit_orders/$id", e)
            Result.failure(e)
        }
    }

    suspend fun cancelMyPlacedOrder(orderId: String): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_orders").document(orderId).update(
                mapOf("status" to "CANCELLED", "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("cancelOrder", "takeit_orders/$orderId", e)
            Result.failure(e)
        }
    }

    suspend fun submitOrderReview(
        orderId: String,
        restaurantId: String,
        userName: String,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val uid = requireUid()
        val revId = cleanId("rev")
        return try {
            val safeRating = rating.coerceIn(1, 5)
            val safeComment = comment.trim().ifBlank { "Great food & fast delivery!" }.take(380)
            db.collection("takeit_orders").document(orderId).update(
                mapOf(
                    "rating" to safeRating,
                    "reviewText" to safeComment,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            db.collection("takeit_reviews").document(revId).set(
                mapOf(
                    "userId" to uid,
                    "userName" to userName.ifBlank { "Take It Foodie" }.take(80),
                    "restaurantId" to restaurantId,
                    "orderId" to orderId,
                    "rating" to safeRating,
                    "comment" to safeComment,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("submitOrderReview", "takeit_reviews/$revId", e)
            Result.failure(e)
        }
    }

    suspend fun createSupportTicket(userName: String, userEmail: String, subject: String, message: String): Result<Unit> {
        val uid = requireUid()
        val id = cleanId("tkt")
        return try {
            db.collection("takeit_support_tickets").document(id).set(
                mapOf(
                    "userId" to uid,
                    "userName" to userName.ifBlank { "Customer" }.take(80),
                    "userEmail" to userEmail.take(120),
                    "subject" to subject.trim().take(120),
                    "message" to message.trim().take(580),
                    "status" to "OPEN",
                    "adminReply" to "",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("createSupportTicket", "takeit_support_tickets/$id", e)
            Result.failure(e)
        }
    }

    // --- ADMIN & CO-ADMIN MUTATIONS ---

    suspend fun saveAppConfig(config: TakeItAppConfig): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_config").document("live").set(
                mapOf(
                    "appName" to config.appName.ifBlank { "Take It" }.take(50),
                    "cityName" to config.cityName.ifBlank { "Greater Noida" }.take(70),
                    "heroBannerTitle" to config.heroBannerTitle.take(110),
                    "heroBannerSubtitle" to config.heroBannerSubtitle.take(170),
                    "heroBannerCode" to config.heroBannerCode.uppercase().take(35),
                    "lateNightTitle" to config.lateNightTitle.take(110),
                    "lateNightSubtitle" to config.lateNightSubtitle.take(170),
                    "deliveryFee" to config.deliveryFee.coerceIn(0, 500),
                    "platformFee" to config.platformFee.coerceIn(0, 200),
                    "gstPercent" to config.gstPercent.coerceIn(0, 28),
                    "freeDeliveryAbove" to config.freeDeliveryAbove.coerceIn(0, 50000),
                    "maintenanceMode" to config.maintenanceMode,
                    "acceptingOrders" to config.acceptingOrders,
                    "supportPhone" to config.supportPhone.take(35),
                    "supportEmail" to config.supportEmail.take(110),
                    "announcementText" to config.announcementText.take(280),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("saveAppConfig", "takeit_config/live", e)
            Result.failure(e)
        }
    }

    suspend fun saveRestaurant(restaurant: Restaurant): Result<String> {
        requireUid()
        val isNew = restaurant.id.isBlank()
        val id = if (isNew) cleanId("rest") else restaurant.id
        return try {
            val ref = db.collection("takeit_restaurants").document(id)
            if (isNew) {
                ref.set(
                    mapOf(
                        "name" to restaurant.name.trim().take(90),
                        "cuisine" to restaurant.cuisine.trim().ifBlank { "Multi Cuisine" }.take(110),
                        "category" to restaurant.category.trim().ifBlank { "Pizza" }.take(50),
                        "rating" to restaurant.rating.coerceIn(0.0, 5.0),
                        "ratingCount" to restaurant.ratingCount.ifBlank { "New" }.take(35),
                        "deliveryTime" to restaurant.deliveryTime.ifBlank { "30 min" }.take(35),
                        "priceForTwo" to restaurant.priceForTwo.coerceIn(50, 50000),
                        "offerTag" to restaurant.offerTag.take(35),
                        "offerCode" to restaurant.offerCode.uppercase().take(35),
                        "address" to restaurant.address.take(220),
                        "imageUrl" to restaurant.imageUrl.trim().take(550),
                        "pureVegFlag" to restaurant.pureVegFlag,
                        "freeDeliveryFlag" to restaurant.freeDeliveryFlag,
                        "verifiedFlag" to restaurant.verifiedFlag,
                        "openFlag" to restaurant.openFlag,
                        "featuredFlag" to restaurant.featuredFlag,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "name" to restaurant.name.trim().take(90),
                        "cuisine" to restaurant.cuisine.trim().ifBlank { "Multi Cuisine" }.take(110),
                        "category" to restaurant.category.trim().ifBlank { "Pizza" }.take(50),
                        "rating" to restaurant.rating.coerceIn(0.0, 5.0),
                        "ratingCount" to restaurant.ratingCount.ifBlank { "New" }.take(35),
                        "deliveryTime" to restaurant.deliveryTime.ifBlank { "30 min" }.take(35),
                        "priceForTwo" to restaurant.priceForTwo.coerceIn(50, 50000),
                        "offerTag" to restaurant.offerTag.take(35),
                        "offerCode" to restaurant.offerCode.uppercase().take(35),
                        "address" to restaurant.address.take(220),
                        "imageUrl" to restaurant.imageUrl.trim().take(550),
                        "pureVegFlag" to restaurant.pureVegFlag,
                        "freeDeliveryFlag" to restaurant.freeDeliveryFlag,
                        "verifiedFlag" to restaurant.verifiedFlag,
                        "openFlag" to restaurant.openFlag,
                        "featuredFlag" to restaurant.featuredFlag,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("saveRestaurant", "takeit_restaurants/$id", e)
            Result.failure(e)
        }
    }

    suspend fun toggleRestaurantOpen(restaurantId: String, open: Boolean): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_restaurants").document(restaurantId).update(
                mapOf("openFlag" to open, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("toggleRestaurantOpen", "takeit_restaurants/$restaurantId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteRestaurant(restaurantId: String): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_restaurants").document(restaurantId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("deleteRestaurant", "takeit_restaurants/$restaurantId", e)
            Result.failure(e)
        }
    }

    suspend fun saveMenuItem(item: MenuItem): Result<String> {
        requireUid()
        val isNew = item.id.isBlank()
        val id = if (isNew) cleanId("dish") else item.id
        return try {
            val ref = db.collection("takeit_menu_items").document(id)
            if (isNew) {
                ref.set(
                    mapOf(
                        "restaurantId" to item.restaurantId,
                        "restaurantName" to item.restaurantName.take(90),
                        "name" to item.name.trim().take(110),
                        "description" to item.description.trim().take(280),
                        "category" to item.category.trim().ifBlank { "Recommended" }.take(50),
                        "price" to item.price.coerceIn(1, 50000),
                        "originalPrice" to item.originalPrice.coerceIn(0, 60000),
                        "vegFlag" to item.vegFlag,
                        "availableFlag" to item.availableFlag,
                        "bestsellerFlag" to item.bestsellerFlag,
                        "rating" to item.rating.coerceIn(0.0, 5.0),
                        "imageUrl" to item.imageUrl.trim().take(550),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "restaurantId" to item.restaurantId,
                        "restaurantName" to item.restaurantName.take(90),
                        "name" to item.name.trim().take(110),
                        "description" to item.description.trim().take(280),
                        "category" to item.category.trim().ifBlank { "Recommended" }.take(50),
                        "price" to item.price.coerceIn(1, 50000),
                        "originalPrice" to item.originalPrice.coerceIn(0, 60000),
                        "vegFlag" to item.vegFlag,
                        "availableFlag" to item.availableFlag,
                        "bestsellerFlag" to item.bestsellerFlag,
                        "rating" to item.rating.coerceIn(0.0, 5.0),
                        "imageUrl" to item.imageUrl.trim().take(550),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("saveMenuItem", "takeit_menu_items/$id", e)
            Result.failure(e)
        }
    }

    suspend fun toggleMenuItemAvailable(itemId: String, available: Boolean): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_menu_items").document(itemId).update(
                mapOf("availableFlag" to available, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("toggleMenuItem", "takeit_menu_items/$itemId", e)
            Result.failure(e)
        }
    }

    suspend fun deleteMenuItem(itemId: String): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_menu_items").document(itemId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("deleteMenuItem", "takeit_menu_items/$itemId", e)
            Result.failure(e)
        }
    }

    suspend fun saveCoupon(coupon: PromoCoupon): Result<String> {
        requireUid()
        val isNew = coupon.id.isBlank()
        val id = if (isNew) cleanId("cpn") else coupon.id
        return try {
            val ref = db.collection("takeit_coupons").document(id)
            if (isNew) {
                ref.set(
                    mapOf(
                        "code" to coupon.code.trim().uppercase().take(28),
                        "title" to coupon.title.trim().take(90),
                        "description" to coupon.description.trim().take(220),
                        "discountType" to if (coupon.discountType == "FLAT") "FLAT" else "PERCENT",
                        "discountValue" to coupon.discountValue.coerceIn(1, 10000),
                        "maxDiscount" to coupon.maxDiscount.coerceIn(0, 10000),
                        "minOrderAmount" to coupon.minOrderAmount.coerceIn(0, 50000),
                        "activeFlag" to coupon.activeFlag,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "code" to coupon.code.trim().uppercase().take(28),
                        "title" to coupon.title.trim().take(90),
                        "description" to coupon.description.trim().take(220),
                        "discountType" to if (coupon.discountType == "FLAT") "FLAT" else "PERCENT",
                        "discountValue" to coupon.discountValue.coerceIn(1, 10000),
                        "maxDiscount" to coupon.maxDiscount.coerceIn(0, 10000),
                        "minOrderAmount" to coupon.minOrderAmount.coerceIn(0, 50000),
                        "activeFlag" to coupon.activeFlag,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(id)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("saveCoupon", "takeit_coupons/$id", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCoupon(couponId: String): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_coupons").document(couponId).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("deleteCoupon", "takeit_coupons/$couponId", e)
            Result.failure(e)
        }
    }

    suspend fun adminUpdateOrder(
        orderId: String,
        status: String,
        riderName: String? = null,
        riderPhone: String? = null,
        riderVehicle: String? = null,
        etaMinutes: Int? = null,
        paymentStatus: String? = null
    ): Result<Unit> {
        requireUid()
        return try {
            val updates = mutableMapOf<String, Any>(
                "status" to status,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            riderName?.let { updates["riderName"] = it.trim().take(70) }
            riderPhone?.let { updates["riderPhone"] = it.trim().take(25) }
            riderVehicle?.let { updates["riderVehicle"] = it.trim().take(35) }
            etaMinutes?.let { updates["etaMinutes"] = it.coerceIn(0, 240) }
            paymentStatus?.let { updates["paymentStatus"] = it }
            db.collection("takeit_orders").document(orderId).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("adminUpdateOrder", "takeit_orders/$orderId", e)
            Result.failure(e)
        }
    }

    suspend fun adminRefundOrderToWallet(order: FoodOrder): Result<Unit> {
        requireUid()
        return try {
            val orderRef = db.collection("takeit_orders").document(order.id)
            val userRef = db.collection("takeit_users").document(order.userId)
            val txRef = userRef.collection("wallet_transactions").document(cleanId("ref"))
            db.runBatch { batch ->
                batch.update(orderRef, mapOf(
                    "status" to "REFUNDED",
                    "paymentStatus" to "REFUNDED",
                    "etaMinutes" to 0,
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                if (order.totalAmount > 0) {
                    batch.update(userRef, mapOf(
                        "walletBalance" to FieldValue.increment(order.totalAmount.toDouble()),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ))
                    batch.set(txRef, mapOf(
                        "userId" to order.userId,
                        "title" to "Refund for ${order.orderNumber}".take(120),
                        "txType" to "REFUND",
                        "amount" to order.totalAmount.toDouble(),
                        "method" to "ADMIN_REFUND",
                        "createdAt" to FieldValue.serverTimestamp()
                    ))
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("adminRefundOrder", "takeit_orders/${order.id}", e)
            Result.failure(e)
        }
    }

    suspend fun adminCreditCustomerWallet(userId: String, amount: Double, reason: String): Result<Unit> {
        requireUid()
        return try {
            val userRef = db.collection("takeit_users").document(userId)
            val txRef = userRef.collection("wallet_transactions").document(cleanId("bonus"))
            db.runBatch { batch ->
                batch.update(userRef, mapOf(
                    "walletBalance" to FieldValue.increment(amount),
                    "updatedAt" to FieldValue.serverTimestamp()
                ))
                batch.set(txRef, mapOf(
                    "userId" to userId,
                    "title" to reason.ifBlank { "Admin Wallet Credit" }.take(120),
                    "txType" to "PROMO",
                    "amount" to amount,
                    "method" to "ADMIN_BONUS",
                    "createdAt" to FieldValue.serverTimestamp()
                ))
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("adminCreditWallet", "takeit_users/$userId", e)
            Result.failure(e)
        }
    }

    suspend fun adminToggleSuspendUser(userId: String, suspended: Boolean): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_users").document(userId).update(
                mapOf("suspendedFlag" to suspended, "updatedAt" to FieldValue.serverTimestamp())
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("adminSuspendUser", "takeit_users/$userId", e)
            Result.failure(e)
        }
    }

    suspend fun adminReplyTicket(ticketId: String, reply: String, status: String): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_support_tickets").document(ticketId).update(
                mapOf(
                    "adminReply" to reply.trim().take(580),
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("adminReplyTicket", "takeit_support_tickets/$ticketId", e)
            Result.failure(e)
        }
    }

    // --- MAIN ADMIN ONLY: CO-ADMIN PASSWORD MANAGEMENT ---

    suspend fun saveCoAdmin(account: CoAdminAccount): Result<Unit> {
        requireUid()
        val emailKey = account.email.trim().lowercase()
        return try {
            val ref = db.collection("takeit_co_admins").document(emailKey)
            val exists = ref.get().await().exists()
            if (!exists) {
                ref.set(
                    mapOf(
                        "email" to emailKey,
                        "name" to account.name.trim().take(75),
                        "roleTitle" to account.roleTitle.trim().ifBlank { "Co-Admin" }.take(55),
                        "passcode" to account.passcode.trim().take(38),
                        "activeFlag" to account.activeFlag,
                        "canManageOrders" to account.canManageOrders,
                        "canManageRestaurants" to account.canManageRestaurants,
                        "canManageCoupons" to account.canManageCoupons,
                        "canManageUsers" to account.canManageUsers,
                        "canManageSettings" to account.canManageSettings,
                        "createdBy" to MAIN_ADMIN_EMAIL,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                ref.update(
                    mapOf(
                        "name" to account.name.trim().take(75),
                        "roleTitle" to account.roleTitle.trim().ifBlank { "Co-Admin" }.take(55),
                        "passcode" to account.passcode.trim().take(38),
                        "activeFlag" to account.activeFlag,
                        "canManageOrders" to account.canManageOrders,
                        "canManageRestaurants" to account.canManageRestaurants,
                        "canManageCoupons" to account.canManageCoupons,
                        "canManageUsers" to account.canManageUsers,
                        "canManageSettings" to account.canManageSettings,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("saveCoAdmin", "takeit_co_admins/$emailKey", e)
            Result.failure(e)
        }
    }

    suspend fun deleteCoAdmin(emailKey: String): Result<Unit> {
        requireUid()
        return try {
            db.collection("takeit_co_admins").document(emailKey.trim().lowercase()).delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("deleteCoAdmin", "takeit_co_admins/$emailKey", e)
            Result.failure(e)
        }
    }

    /**
     * One-tap starter catalog population by Main Admin so the live Firestore store starts rich & immediately usable.
     */
    suspend fun seedStarterCatalogIfEmpty(force: Boolean = false): Result<Int> {
        requireUid()
        return try {
            val existing = db.collection("takeit_restaurants").limit(1).get().await()
            if (!force && !existing.isEmpty) return Result.success(0)

            saveAppConfig(TakeItAppConfig())

            val starterRestaurants = listOf(
                Restaurant(id = "rest_pizza_world", name = "Pizza World", cuisine = "Italian, Wood-Fired Pizza, Sides", category = "Pizza", rating = 4.5, ratingCount = "10K+", deliveryTime = "28 min", priceForTwo = 200, offerTag = "50% OFF", offerCode = "FOOD50", address = "Alpha 1, Greater Noida", pureVegFlag = true, freeDeliveryFlag = true, verifiedFlag = true, openFlag = true, featuredFlag = true),
                Restaurant(id = "rest_burger_hub", name = "Burger Hub", cuisine = "Gourmet Burgers, Crispy Fries, Shakes", category = "Burger", rating = 4.4, ratingCount = "8K+", deliveryTime = "24 min", priceForTwo = 180, offerTag = "40% OFF", offerCode = "TAKEIT40", address = "Sector 62, Greater Noida", pureVegFlag = false, freeDeliveryFlag = false, verifiedFlag = true, openFlag = true, featuredFlag = true),
                Restaurant(id = "rest_biryani_house", name = "Biryani House", cuisine = "Hyderabadi Dum Biryani, Kebabs", category = "Biryani", rating = 4.6, ratingCount = "12K+", deliveryTime = "35 min", priceForTwo = 250, offerTag = "30% OFF", offerCode = "BIRYANI30", address = "Pari Chowk, Greater Noida", pureVegFlag = false, freeDeliveryFlag = true, verifiedFlag = true, openFlag = true, featuredFlag = true),
                Restaurant(id = "rest_chinese_corner", name = "Chinese Corner", cuisine = "Hakka Noodles, Manchurian, Momos", category = "Chinese", rating = 4.3, ratingCount = "6K+", deliveryTime = "26 min", priceForTwo = 160, offerTag = "₹75 OFF", offerCode = "TAKEIT75", address = "Knowledge Park, Greater Noida", pureVegFlag = true, freeDeliveryFlag = false, verifiedFlag = true, openFlag = true, featuredFlag = false),
                Restaurant(id = "rest_south_spice", name = "South Spice Kitchen", cuisine = "Masala Dosa, Idli, Filter Coffee", category = "South Indian", rating = 4.7, ratingCount = "9K+", deliveryTime = "22 min", priceForTwo = 150, offerTag = "FREE DEL", offerCode = "FREEDEL", address = "Gamma 2, Greater Noida", pureVegFlag = true, freeDeliveryFlag = true, verifiedFlag = true, openFlag = true, featuredFlag = false),
                Restaurant(id = "rest_sweet_cravings", name = "Sweet Cravings Bakery", cuisine = "Belgian Waffles, Brownies, Shakes", category = "Desserts", rating = 4.8, ratingCount = "5K+", deliveryTime = "20 min", priceForTwo = 220, offerTag = "25% OFF", offerCode = "FOOD50", address = "Beta 1, Greater Noida", pureVegFlag = true, freeDeliveryFlag = true, verifiedFlag = true, openFlag = true, featuredFlag = false)
            )

            starterRestaurants.forEach { r ->
                db.collection("takeit_restaurants").document(r.id).set(
                    mapOf(
                        "name" to r.name, "cuisine" to r.cuisine, "category" to r.category,
                        "rating" to r.rating, "ratingCount" to r.ratingCount, "deliveryTime" to r.deliveryTime,
                        "priceForTwo" to r.priceForTwo, "offerTag" to r.offerTag, "offerCode" to r.offerCode,
                        "address" to r.address, "imageUrl" to "",
                        "pureVegFlag" to r.pureVegFlag, "freeDeliveryFlag" to r.freeDeliveryFlag,
                        "verifiedFlag" to r.verifiedFlag, "openFlag" to r.openFlag, "featuredFlag" to r.featuredFlag,
                        "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            val starterDishes = listOf(
                MenuItem(id = "dish_margherita", restaurantId = "rest_pizza_world", restaurantName = "Pizza World", name = "Margherita Pizza", description = "Classic hand-tossed pizza with fresh mozzarella & basil", category = "Recommended", price = 149, originalPrice = 219, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.6),
                MenuItem(id = "dish_farmhouse", restaurantId = "rest_pizza_world", restaurantName = "Pizza World", name = "Farmhouse Pizza", description = "Loaded with crisp capsicum, onion, sweet corn & mushrooms", category = "Recommended", price = 199, originalPrice = 279, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.7),
                MenuItem(id = "dish_paneer_tikka", restaurantId = "rest_pizza_world", restaurantName = "Pizza World", name = "Paneer Tikka Pizza", description = "Smoky tandoori paneer chunks with mint mayo swirl", category = "Recommended", price = 219, originalPrice = 299, vegFlag = true, availableFlag = true, bestsellerFlag = false, rating = 4.5),
                MenuItem(id = "dish_garlic_bread", restaurantId = "rest_pizza_world", restaurantName = "Pizza World", name = "Stuffed Garlic Bread (4 Pcs)", description = "Warm buttery garlic bread stuffed with molten cheese & jalapeños", category = "Sides", price = 99, originalPrice = 139, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.6),

                MenuItem(id = "dish_double_cheese", restaurantId = "rest_burger_hub", restaurantName = "Burger Hub", name = "Double Smash Cheese Burger", description = "Double crispy patty with melted cheddar & smoky chipotle sauce", category = "Recommended", price = 149, originalPrice = 199, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.5),
                MenuItem(id = "dish_crispy_chicken", restaurantId = "rest_burger_hub", restaurantName = "Burger Hub", name = "Fiery Chicken Crunch Burger", description = "Southern-fried spicy chicken fillet with lettuce & garlic mayo", category = "Recommended", price = 179, originalPrice = 229, vegFlag = false, availableFlag = true, bestsellerFlag = true, rating = 4.6),
                MenuItem(id = "dish_peri_fries", restaurantId = "rest_burger_hub", restaurantName = "Burger Hub", name = "Peri-Peri Loaded Fries", description = "Crispy golden fries tossed in tangy peri-peri & cheese dip", category = "Sides", price = 109, originalPrice = 149, vegFlag = true, availableFlag = true, bestsellerFlag = false, rating = 4.4),

                MenuItem(id = "dish_hyd_chicken", restaurantId = "rest_biryani_house", restaurantName = "Biryani House", name = "Hyderabadi Chicken Dum Biryani", description = "Slow-cooked saffron basmati rice with tender spiced chicken & raita", category = "Recommended", price = 249, originalPrice = 320, vegFlag = false, availableFlag = true, bestsellerFlag = true, rating = 4.8),
                MenuItem(id = "dish_paneer_biryani", restaurantId = "rest_biryani_house", restaurantName = "Biryani House", name = "Royal Paneer Subz Biryani", description = "Aromatic dum biryani with marinated paneer & caramelized onions", category = "Recommended", price = 219, originalPrice = 280, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.6),

                MenuItem(id = "dish_hakka_noodles", restaurantId = "rest_chinese_corner", restaurantName = "Chinese Corner", name = "Wok-Tossed Veg Hakka Noodles", description = "Smoky street-style noodles with crunchy bell peppers & sauces", category = "Recommended", price = 139, originalPrice = 180, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.4),
                MenuItem(id = "dish_chilli_paneer", restaurantId = "rest_chinese_corner", restaurantName = "Chinese Corner", name = "Crispy Chilli Paneer Gravy", description = "Soft paneer cubes tossed in spicy soy-garlic chilli glaze", category = "Recommended", price = 189, originalPrice = 240, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.5),

                MenuItem(id = "dish_masala_dosa", restaurantId = "rest_south_spice", restaurantName = "South Spice Kitchen", name = "Ghee Roast Masala Dosa", description = "Golden crispy dosa with spiced potato masala, sambar & 3 chutneys", category = "Recommended", price = 129, originalPrice = 169, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.8),
                MenuItem(id = "dish_idli_vada", restaurantId = "rest_south_spice", restaurantName = "South Spice Kitchen", name = "Steamed Idli & Medu Vada Combo", description = "Fluffy idlis & crisp vadas served with hot piping sambar", category = "Combos", price = 109, originalPrice = 140, vegFlag = true, availableFlag = true, bestsellerFlag = false, rating = 4.7),

                MenuItem(id = "dish_choco_lava", restaurantId = "rest_sweet_cravings", restaurantName = "Sweet Cravings Bakery", name = "Molten Choco Lava Cake", description = "Warm dark chocolate cake with gooey Belgian chocolate core", category = "Desserts", price = 119, originalPrice = 159, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.9),
                MenuItem(id = "dish_nutella_waffle", restaurantId = "rest_sweet_cravings", restaurantName = "Sweet Cravings Bakery", name = "Triple Chocolate Nutella Waffle", description = "Freshly baked Belgian waffle drizzled with Nutella & choco chips", category = "Desserts", price = 169, originalPrice = 219, vegFlag = true, availableFlag = true, bestsellerFlag = true, rating = 4.8)
            )

            starterDishes.forEach { d ->
                db.collection("takeit_menu_items").document(d.id).set(
                    mapOf(
                        "restaurantId" to d.restaurantId, "restaurantName" to d.restaurantName,
                        "name" to d.name, "description" to d.description, "category" to d.category,
                        "price" to d.price, "originalPrice" to d.originalPrice,
                        "vegFlag" to d.vegFlag, "availableFlag" to d.availableFlag,
                        "bestsellerFlag" to d.bestsellerFlag, "rating" to d.rating, "imageUrl" to "",
                        "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            val starterCoupons = listOf(
                PromoCoupon(id = "cpn_food50", code = "FOOD50", title = "Flat 50% OFF", description = "Save up to ₹100 on orders above ₹149", discountType = "PERCENT", discountValue = 50, maxDiscount = 100, minOrderAmount = 149, activeFlag = true),
                PromoCoupon(id = "cpn_takeit75", code = "TAKEIT75", title = "Flat ₹75 OFF", description = "Instant ₹75 discount on orders above ₹249", discountType = "FLAT", discountValue = 75, maxDiscount = 75, minOrderAmount = 249, activeFlag = true),
                PromoCoupon(id = "cpn_welcome100", code = "WELCOME100", title = "Flat ₹100 OFF", description = "Special feast offer on orders above ₹399", discountType = "FLAT", discountValue = 100, maxDiscount = 100, minOrderAmount = 399, activeFlag = true)
            )

            starterCoupons.forEach { c ->
                db.collection("takeit_coupons").document(c.id).set(
                    mapOf(
                        "code" to c.code, "title" to c.title, "description" to c.description,
                        "discountType" to c.discountType, "discountValue" to c.discountValue,
                        "maxDiscount" to c.maxDiscount, "minOrderAmount" to c.minOrderAmount,
                        "activeFlag" to c.activeFlag,
                        "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()
                    ),
                    SetOptions.merge()
                ).await()
            }

            Result.success(starterRestaurants.size)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            logError("seedStarterCatalog", "takeit_restaurants", e)
            Result.failure(e)
        }
    }
}
