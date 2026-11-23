package com.example.takeit.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.network.ImgBbUploadService
import com.example.network.TakeItAdminTransaction
import com.example.network.TakeItPaymentGateway
import com.example.takeit.data.TakeItFirestoreRepository
import com.example.takeit.model.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

data class TakeItUiState(
    val uid: String = "",
    val email: String = "",
    val displayName: String = "",
    val isAttached: Boolean = false,
    val appVariant: TakeItAppVariant = TakeItAppVariant.fromBuildConfig(),
    // Admin / Co-Admin Auth State
    val adminRole: AdminRole = AdminRole.NONE,
    val activeCoAdmin: CoAdminAccount? = null,
    val isInAdminMode: Boolean = TakeItAppVariant.fromBuildConfig() == TakeItAppVariant.ADMIN,
    val coAdminPasscodeVerified: Boolean = false,
    val coAdminAuthError: String? = null,
    val isCheckingCoAdmin: Boolean = false,
    // Live Synced Data
    val config: TakeItAppConfig = TakeItAppConfig(),
    val profile: TakeItUserProfile = TakeItUserProfile(),
    val restaurants: List<Restaurant> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val coupons: List<PromoCoupon> = emptyList(),
    val myOrders: List<FoodOrder> = emptyList(),
    val allOrders: List<FoodOrder> = emptyList(),
    val allUsers: List<TakeItUserProfile> = emptyList(),
    val addresses: List<SavedAddress> = emptyList(),
    val walletTx: List<WalletTx> = emptyList(),
    val reviews: List<RestaurantReview> = emptyList(),
    val myTickets: List<SupportTicket> = emptyList(),
    val allTickets: List<SupportTicket> = emptyList(),
    val coAdmins: List<CoAdminAccount> = emptyList(),
    // Razorpay Admin Ledger (Isolated TakeIt-only)
    val razorpayTransactions: List<TakeItAdminTransaction> = emptyList(),
    val razorpayTotalRevenue: Int = 0,
    val isLoadingLedger: Boolean = false,
    val ledgerError: String? = null,
    // Customer Cart & Navigation
    val customerTab: CustomerTab = CustomerTab.HOME,
    val customerBackStack: List<CustomerScreen> = listOf(CustomerScreen.MainTabs),
    val adminTab: AdminTab = AdminTab.OVERVIEW,
    val cart: List<CartItem> = emptyList(),
    val appliedCoupon: PromoCoupon? = null,
    val searchQuery: String = "",
    val isUploadingPhoto: Boolean = false,
    val snackbarMessage: String? = null
) {
    val currentCustomerScreen: CustomerScreen get() = customerBackStack.lastOrNull() ?: CustomerScreen.MainTabs
    val isMainAdmin: Boolean get() = email.trim().equals(MAIN_ADMIN_EMAIL, ignoreCase = true)
    val hasUnlockedAdminAccess: Boolean
        get() = isMainAdmin || (adminRole == AdminRole.CO_ADMIN && coAdminPasscodeVerified && activeCoAdmin?.activeFlag == true)

    val canManageOrders: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageOrders == true)
    val canManageRestaurants: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageRestaurants == true)
    val canManageCoupons: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageCoupons == true)
    val canManageUsers: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageUsers == true)
    val canManageSettings: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageSettings == true)

    val cartItemCount: Int get() = cart.sumOf { it.quantity }
    val cartItemTotal: Int get() = cart.sumOf { it.menuItem.price * it.quantity }
    val cartRestaurantId: String get() = cart.firstOrNull()?.menuItem?.restaurantId.orEmpty()
    val cartRestaurantName: String get() = cart.firstOrNull()?.menuItem?.restaurantName.orEmpty()

    val calculatedDeliveryFee: Int
        get() = when {
            cart.isEmpty() -> 0
            config.freeDeliveryAbove > 0 && cartItemTotal >= config.freeDeliveryAbove -> 0
            else -> config.deliveryFee
        }

    val calculatedPlatformFee: Int get() = if (cart.isEmpty()) 0 else config.platformFee
    val calculatedGst: Int get() = if (cart.isEmpty()) 0 else ((cartItemTotal * config.gstPercent) / 100.0).roundToInt()

    val calculatedDiscount: Int
        get() {
            val c = appliedCoupon ?: return 0
            if (!c.activeFlag || cartItemTotal < c.minOrderAmount) return 0
            return if (c.discountType == "FLAT") {
                min(c.discountValue, cartItemTotal)
            } else {
                val pct = ((cartItemTotal * c.discountValue) / 100.0).roundToInt()
                if (c.maxDiscount > 0) min(pct, c.maxDiscount) else pct
            }
        }

    val cartTotalPayable: Int
        get() = (cartItemTotal + calculatedDeliveryFee + calculatedPlatformFee + calculatedGst - calculatedDiscount).coerceAtLeast(0)
}

class TakeItViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = TakeItFirestoreRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(TakeItUiState())
    val uiState: StateFlow<TakeItUiState> = _uiState.asStateFlow()

    private val sessionJobs = mutableListOf<Job>()
    private val adminJobs = mutableListOf<Job>()

    fun attachSession(uid: String, displayName: String, email: String) {
        if (_uiState.value.isAttached && _uiState.value.uid == uid) return
        detachSession()

        val cleanEmail = email.trim().lowercase()
        val isMain = cleanEmail == MAIN_ADMIN_EMAIL

        _uiState.update {
            it.copy(
                uid = uid,
                email = cleanEmail,
                displayName = displayName.ifBlank { cleanEmail.substringBefore("@").ifBlank { "Take It User" } },
                isAttached = true,
                adminRole = if (isMain) AdminRole.MAIN_ADMIN else AdminRole.NONE,
                coAdminPasscodeVerified = isMain,
                isInAdminMode = it.appVariant == TakeItAppVariant.ADMIN
            )
        }

        // Core Customer & Shared Live Streams
        sessionJobs += viewModelScope.launch {
            repo.ensureUserProfile(uid, displayName, cleanEmail)
            if (isMain) {
                repo.seedStarterCatalogIfEmpty(force = false)
            }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeConfig().collect { cfg -> _uiState.update { it.copy(config = cfg) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeRestaurants().collect { list -> _uiState.update { it.copy(restaurants = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeMenuItems().collect { list -> _uiState.update { it.copy(menuItems = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeCoupons().collect { list -> _uiState.update { it.copy(coupons = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeMyProfile(uid).collect { prof ->
                if (prof != null) _uiState.update { it.copy(profile = prof) }
            }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeMyOrders(uid).collect { list -> _uiState.update { it.copy(myOrders = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeAddresses(uid).collect { list -> _uiState.update { it.copy(addresses = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeWalletTransactions(uid).collect { list -> _uiState.update { it.copy(walletTx = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeAllReviews().collect { list -> _uiState.update { it.copy(reviews = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeMyTickets(uid).collect { list -> _uiState.update { it.copy(myTickets = list) } }
        }

        if (isMain) {
            attachAdminStreams(isMainAdmin = true)
        } else {
            // Silently check if this Gmail is registered as a Co-Admin
            sessionJobs += viewModelScope.launch {
                val res = repo.fetchMyCoAdminRecord(cleanEmail)
                val record = res.getOrNull()
                if (record != null && record.activeFlag) {
                    _uiState.update { it.copy(adminRole = AdminRole.CO_ADMIN, activeCoAdmin = record) }
                }
            }
        }
    }

    private fun attachAdminStreams(isMainAdmin: Boolean) {
        adminJobs.forEach { it.cancel() }
        adminJobs.clear()

        adminJobs += viewModelScope.launch {
            repo.observeAllOrders().collect { list -> _uiState.update { it.copy(allOrders = list) } }
        }
        adminJobs += viewModelScope.launch {
            repo.observeAllUsers().collect { list -> _uiState.update { it.copy(allUsers = list) } }
        }
        adminJobs += viewModelScope.launch {
            repo.observeAllTickets().collect { list -> _uiState.update { it.copy(allTickets = list) } }
        }
        if (isMainAdmin) {
            adminJobs += viewModelScope.launch {
                repo.observeCoAdmins().collect { list -> _uiState.update { it.copy(coAdmins = list) } }
            }
        }
        refreshRazorpayLedger()
    }

    fun detachSession() {
        sessionJobs.forEach { it.cancel() }
        sessionJobs.clear()
        adminJobs.forEach { it.cancel() }
        adminJobs.clear()
        _uiState.value = TakeItUiState()
    }

    // --- NAVIGATION & MODE SWITCHING ---

    fun selectCustomerTab(tab: CustomerTab) {
        _uiState.update { it.copy(customerTab = tab, customerBackStack = listOf(CustomerScreen.MainTabs)) }
    }

    fun navigateCustomer(screen: CustomerScreen) {
        _uiState.update { it.copy(customerBackStack = it.customerBackStack + screen) }
    }

    fun navigateCustomerBack() {
        _uiState.update { state ->
            if (state.customerBackStack.size > 1) {
                state.copy(customerBackStack = state.customerBackStack.dropLast(1))
            } else {
                state
            }
        }
    }

    fun resetCustomerHome() {
        _uiState.update { it.copy(customerTab = CustomerTab.HOME, customerBackStack = listOf(CustomerScreen.MainTabs)) }
    }

    fun selectAdminTab(tab: AdminTab) {
        _uiState.update { it.copy(adminTab = tab) }
    }

    fun enterAdminMode() {
        _uiState.update { it.copy(isInAdminMode = true) }
        if (_uiState.value.hasUnlockedAdminAccess) {
            attachAdminStreams(_uiState.value.isMainAdmin)
        }
    }

    fun exitAdminMode() {
        _uiState.update { it.copy(isInAdminMode = false) }
    }

    fun verifyCoAdminPasscode(passcodeInput: String) {
        val state = _uiState.value
        val email = state.email.trim().lowercase()
        if (email == MAIN_ADMIN_EMAIL) {
            _uiState.update { it.copy(coAdminPasscodeVerified = true, coAdminAuthError = null) }
            attachAdminStreams(isMainAdmin = true)
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingCoAdmin = true, coAdminAuthError = null) }
            val result = repo.fetchMyCoAdminRecord(email)
            val record = result.getOrNull()
            when {
                record == null -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCoAdmin = false,
                            coAdminAuthError = "Access denied. Only Main Admin ($MAIN_ADMIN_EMAIL) or authorized Co-Admins can enter."
                        )
                    }
                }
                !record.activeFlag -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCoAdmin = false,
                            coAdminAuthError = "Your Co-Admin access has been suspended by Main Admin."
                        )
                    }
                }
                record.passcode != passcodeInput.trim() -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCoAdmin = false,
                            coAdminAuthError = "Incorrect Co-Admin password. Ask Main Admin ($MAIN_ADMIN_EMAIL) for your password."
                        )
                    }
                }
                else -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCoAdmin = false,
                            adminRole = AdminRole.CO_ADMIN,
                            activeCoAdmin = record,
                            coAdminPasscodeVerified = true,
                            coAdminAuthError = null
                        )
                    }
                    attachAdminStreams(isMainAdmin = false)
                    showMessage("Welcome Co-Admin ${record.name} (${record.roleTitle})")
                }
            }
        }
    }

    // --- CUSTOMER CART & ORDERING ---

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun addToCart(item: MenuItem) {
        _uiState.update { state ->
            // If adding from a different restaurant, replace cart cleanly with notification
            val sameRest = state.cart.isEmpty() || state.cart.first().menuItem.restaurantId == item.restaurantId
            val baseCart = if (sameRest) state.cart else emptyList()
            val idx = baseCart.indexOfFirst { it.menuItem.id == item.id }
            val updated = if (idx >= 0) {
                baseCart.mapIndexed { i, ci -> if (i == idx) ci.copy(quantity = ci.quantity + 1) else ci }
            } else {
                baseCart + CartItem(item, 1)
            }
            state.copy(
                cart = updated,
                appliedCoupon = if (sameRest) state.appliedCoupon else null,
                snackbarMessage = if (!sameRest) "Started new cart for ${item.restaurantName}" else state.snackbarMessage
            )
        }
    }

    fun updateCartQuantity(itemId: String, delta: Int) {
        _uiState.update { state ->
            val updated = state.cart.mapNotNull { ci ->
                if (ci.menuItem.id == itemId) {
                    val next = ci.quantity + delta
                    if (next <= 0) null else ci.copy(quantity = next)
                } else ci
            }
            state.copy(
                cart = updated,
                appliedCoupon = if (updated.isEmpty()) null else state.appliedCoupon
            )
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cart = emptyList(), appliedCoupon = null) }
    }

    fun applyCoupon(coupon: PromoCoupon) {
        val total = _uiState.value.cartItemTotal
        if (!coupon.activeFlag) {
            showMessage("This coupon is no longer active.")
            return
        }
        if (total < coupon.minOrderAmount) {
            showMessage("Add ₹${coupon.minOrderAmount - total} more to use ${coupon.code}")
            return
        }
        _uiState.update { it.copy(appliedCoupon = coupon, snackbarMessage = "Coupon ${coupon.code} applied!") }
    }

    fun removeCoupon() {
        _uiState.update { it.copy(appliedCoupon = null) }
    }

    fun toggleFavoriteRestaurant(restaurantId: String) {
        val current = _uiState.value.profile.favoriteRestaurantIds
        val updated = if (current.contains(restaurantId)) current - restaurantId else current + restaurantId
        viewModelScope.launch {
            repo.updateMyProfile(favoriteIds = updated)
        }
    }

    fun saveAddress(label: String, fullAddress: String, landmark: String) {
        if (fullAddress.isBlank()) {
            showMessage("Please enter full address")
            return
        }
        viewModelScope.launch {
            repo.saveAddress(label, fullAddress, landmark)
                .onSuccess { showMessage("Address saved & selected!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to save address") }
        }
    }

    fun selectActiveAddress(label: String, fullAddress: String) {
        viewModelScope.launch {
            repo.updateMyProfile(activeAddressLabel = label, activeAddressLine = fullAddress)
                .onSuccess { showMessage("Delivering to $label") }
        }
    }

    fun deleteAddress(addressId: String) {
        viewModelScope.launch {
            repo.deleteAddress(addressId)
                .onSuccess { showMessage("Address removed") }
        }
    }

    fun updateCustomerPhoneAndName(name: String, phone: String) {
        viewModelScope.launch {
            repo.updateMyProfile(name = name, phone = phone)
                .onSuccess { showMessage("Profile updated!") }
                .onFailure { showMessage(it.localizedMessage ?: "Update failed") }
        }
    }

    fun recordWalletTopUp(amount: Double, method: String, title: String) {
        viewModelScope.launch {
            repo.recordWalletTopUp(amount, method, title)
                .onSuccess { showMessage("₹${amount.toInt()} added to Take It Wallet!") }
                .onFailure { showMessage(it.localizedMessage ?: "Wallet update failed") }
        }
    }

    fun placeFoodOrder(
        paymentMethod: String,
        paymentStatus: String,
        paymentRef: String,
        onOrderCreated: (String) -> Unit
    ) {
        val state = _uiState.value
        if (state.cart.isEmpty()) {
            showMessage("Your cart is empty")
            return
        }
        if (state.profile.suspendedFlag) {
            showMessage("Account suspended by Admin. Contact support.")
            return
        }
        if (!state.config.acceptingOrders || state.config.maintenanceMode) {
            showMessage("Orders are temporarily paused by Take It Admin.")
            return
        }
        val deductWallet = if (paymentMethod == "WALLET") state.cartTotalPayable.toDouble() else 0.0
        if (deductWallet > state.profile.walletBalance) {
            showMessage("Insufficient Wallet balance. Top up or pay via Razorpay / COD.")
            return
        }

        val orderNum = "TK-${Random.nextInt(10000, 99999)}"
        val otp = Random.nextInt(1000, 9999).toString()
        val summary = state.cart.joinToString(", ") { "${it.quantity}x ${it.menuItem.name}" }
        val deliveryAddr = listOf(state.profile.activeAddressLabel, state.profile.activeAddressLine)
            .filter { it.isNotBlank() }.joinToString(" • ").ifBlank { state.config.cityName }

        val order = FoodOrder(
            orderNumber = orderNum,
            userId = state.uid,
            customerName = state.profile.name.ifBlank { state.displayName },
            customerEmail = state.email,
            customerPhone = state.profile.phone,
            restaurantId = state.cartRestaurantId,
            restaurantName = state.cartRestaurantName,
            itemsSummary = summary,
            itemTotal = state.cartItemTotal,
            deliveryFee = state.calculatedDeliveryFee,
            platformFee = state.calculatedPlatformFee,
            gstAmount = state.calculatedGst,
            discountAmount = state.calculatedDiscount,
            couponCode = state.appliedCoupon?.code.orEmpty(),
            totalAmount = state.cartTotalPayable,
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            paymentRef = paymentRef,
            deliveryAddress = deliveryAddr,
            status = "PLACED",
            otp = otp
        )

        viewModelScope.launch {
            repo.placeOrder(order, deductWallet)
                .onSuccess { newOrderId ->
                    clearCart()
                    showMessage("Order $orderNum placed! Live tracking started.")
                    onOrderCreated(newOrderId)
                }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to place order") }
        }
    }

    fun cancelMyOrder(orderId: String) {
        viewModelScope.launch {
            repo.cancelMyPlacedOrder(orderId)
                .onSuccess { showMessage("Order cancelled.") }
                .onFailure { showMessage("Order already confirmed by kitchen and cannot be cancelled.") }
        }
    }

    fun submitOrderReview(orderId: String, restaurantId: String, rating: Int, comment: String) {
        val name = _uiState.value.profile.name.ifBlank { _uiState.value.displayName }
        viewModelScope.launch {
            repo.submitOrderReview(orderId, restaurantId, name, rating, comment)
                .onSuccess { showMessage("Thank you! Your $rating★ review is live.") }
                .onFailure { showMessage(it.localizedMessage ?: "Could not submit review") }
        }
    }

    fun createSupportTicket(subject: String, message: String) {
        if (subject.isBlank() || message.isBlank()) {
            showMessage("Please enter subject and message")
            return
        }
        val state = _uiState.value
        viewModelScope.launch {
            repo.createSupportTicket(state.profile.name.ifBlank { state.displayName }, state.email, subject, message)
                .onSuccess { showMessage("Support ticket raised! Admin will reply shortly.") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to raise ticket") }
        }
    }

    // --- ADMIN & CO-ADMIN COMMAND CENTER ACTIONS ---

    fun seedStarterCatalog() {
        viewModelScope.launch {
            repo.seedStarterCatalogIfEmpty(force = true)
                .onSuccess { showMessage("Starter restaurants, menu & coupons synced to Cloud!") }
                .onFailure { showMessage(it.localizedMessage ?: "Seed failed") }
        }
    }

    fun saveAppConfig(config: TakeItAppConfig) {
        viewModelScope.launch {
            repo.saveAppConfig(config)
                .onSuccess { showMessage("Live app settings pushed to all Customer apps!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to update settings") }
        }
    }

    fun saveRestaurant(restaurant: Restaurant) {
        viewModelScope.launch {
            repo.saveRestaurant(restaurant)
                .onSuccess { showMessage("Restaurant '${restaurant.name}' synced live!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to save restaurant") }
        }
    }

    fun toggleRestaurantOpen(restaurantId: String, open: Boolean) {
        viewModelScope.launch {
            repo.toggleRestaurantOpen(restaurantId, open)
                .onSuccess { showMessage(if (open) "Restaurant is now OPEN" else "Restaurant marked CLOSED") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to update status") }
        }
    }

    fun deleteRestaurant(restaurantId: String) {
        viewModelScope.launch {
            repo.deleteRestaurant(restaurantId)
                .onSuccess { showMessage("Restaurant removed from live app.") }
                .onFailure { showMessage(it.localizedMessage ?: "Delete failed") }
        }
    }

    fun saveMenuItem(item: MenuItem) {
        viewModelScope.launch {
            repo.saveMenuItem(item)
                .onSuccess { showMessage("Dish '${item.name}' saved live!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to save dish") }
        }
    }

    fun toggleMenuItemAvailable(itemId: String, available: Boolean) {
        viewModelScope.launch {
            repo.toggleMenuItemAvailable(itemId, available)
                .onSuccess { showMessage(if (available) "Dish marked In Stock" else "Dish marked Sold Out") }
        }
    }

    fun deleteMenuItem(itemId: String) {
        viewModelScope.launch {
            repo.deleteMenuItem(itemId)
                .onSuccess { showMessage("Dish deleted.") }
        }
    }

    fun saveCoupon(coupon: PromoCoupon) {
        viewModelScope.launch {
            repo.saveCoupon(coupon)
                .onSuccess { showMessage("Coupon ${coupon.code.uppercase()} is live!") }
                .onFailure { showMessage(it.localizedMessage ?: "Coupon save failed") }
        }
    }

    fun deleteCoupon(couponId: String) {
        viewModelScope.launch {
            repo.deleteCoupon(couponId)
                .onSuccess { showMessage("Coupon deleted.") }
        }
    }

    fun adminUpdateOrderStatus(
        orderId: String,
        status: String,
        riderName: String? = null,
        riderPhone: String? = null,
        riderVehicle: String? = null,
        etaMinutes: Int? = null
    ) {
        viewModelScope.launch {
            val payStatus = if (status == "DELIVERED") "PAID" else null
            repo.adminUpdateOrder(orderId, status, riderName, riderPhone, riderVehicle, etaMinutes, payStatus)
                .onSuccess { showMessage("Order updated to $status — synced live to customer!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to update order") }
        }
    }

    fun adminRefundOrder(order: FoodOrder) {
        viewModelScope.launch {
            repo.adminRefundOrderToWallet(order)
                .onSuccess { showMessage("₹${order.totalAmount} refunded to customer wallet!") }
                .onFailure { showMessage(it.localizedMessage ?: "Refund failed") }
        }
    }

    fun adminCreditCustomerWallet(userId: String, amount: Double, reason: String) {
        viewModelScope.launch {
            repo.adminCreditCustomerWallet(userId, amount, reason)
                .onSuccess { showMessage("Credited ₹${amount.toInt()} to customer wallet!") }
                .onFailure { showMessage(it.localizedMessage ?: "Credit failed") }
        }
    }

    fun adminToggleSuspendUser(userId: String, suspended: Boolean) {
        viewModelScope.launch {
            repo.adminToggleSuspendUser(userId, suspended)
                .onSuccess { showMessage(if (suspended) "User suspended" else "User reactivated") }
                .onFailure { showMessage(it.localizedMessage ?: "Action failed") }
        }
    }

    fun adminReplyTicket(ticketId: String, reply: String, status: String) {
        viewModelScope.launch {
            repo.adminReplyTicket(ticketId, reply, status)
                .onSuccess { showMessage("Reply sent to customer!") }
                .onFailure { showMessage(it.localizedMessage ?: "Reply failed") }
        }
    }

    fun saveCoAdmin(account: CoAdminAccount) {
        if (!_uiState.value.isMainAdmin) {
            showMessage("Only Main Admin ($MAIN_ADMIN_EMAIL) can manage Co-Admins.")
            return
        }
        viewModelScope.launch {
            repo.saveCoAdmin(account)
                .onSuccess { showMessage("Co-Admin ${account.name} saved! Share password with ${account.email}") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to save Co-Admin") }
        }
    }

    fun deleteCoAdmin(email: String) {
        if (!_uiState.value.isMainAdmin) return
        viewModelScope.launch {
            repo.deleteCoAdmin(email)
                .onSuccess { showMessage("Co-Admin removed.") }
                .onFailure { showMessage(it.localizedMessage ?: "Delete failed") }
        }
    }

    fun uploadImageToImgBb(uri: Uri, prefix: String, onUploaded: (String) -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUploadingPhoto = true) }
            val result = ImgBbUploadService.uploadImageUri(getApplication(), uri, prefix)
            _uiState.update { it.copy(isUploadingPhoto = false) }
            result.onSuccess { url ->
                showMessage("Photo uploaded to Cloud!")
                onUploaded(url)
            }.onFailure { err ->
                showMessage(err.localizedMessage ?: "Photo upload failed")
            }
        }
    }

    fun refreshRazorpayLedger() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingLedger = true, ledgerError = null) }
            TakeItPaymentGateway.fetchAdminTransactions()
                .onSuccess { res ->
                    _uiState.update {
                        it.copy(
                            isLoadingLedger = false,
                            razorpayTransactions = res.transactions,
                            razorpayTotalRevenue = res.totalRevenue,
                            ledgerError = null
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isLoadingLedger = false,
                            ledgerError = err.localizedMessage ?: "Could not reach jaiambey.shop"
                        )
                    }
                }
        }
    }

    fun showMessage(msg: String) {
        _uiState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearMessage() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }
}
