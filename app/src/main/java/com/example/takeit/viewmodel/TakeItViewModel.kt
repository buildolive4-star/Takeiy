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
    val isInDeliveryMode: Boolean = TakeItAppVariant.fromBuildConfig() == TakeItAppVariant.DELIVERY,
    val deliveryPartnerOnline: Boolean = true,
    val deliveryTab: DeliveryTab = DeliveryTab.TASKS,
    val coAdminPasscodeVerified: Boolean = false,
    val coAdminAuthError: String? = null,
    val isCheckingCoAdmin: Boolean = false,
    // Live Synced Data
    val config: TakeItAppConfig = TakeItAppConfig(),
    val profile: TakeItUserProfile = TakeItUserProfile(),
    val activeLatitude: Double = 28.4812,
    val activeLongitude: Double = 77.5125,
    val categories: List<FoodCategory> = emptyList(),
    val heroBanners: List<HeroBannerSlide> = emptyList(),
    val restaurants: List<Restaurant> = emptyList(),
    val menuItems: List<MenuItem> = emptyList(),
    val coupons: List<PromoCoupon> = emptyList(),
    val myOrders: List<FoodOrder> = emptyList(),
    val allOrders: List<FoodOrder> = emptyList(),
    val myRides: List<RideBooking> = emptyList(),
    val allRides: List<RideBooking> = emptyList(),
    val myParcels: List<ParcelOrder> = emptyList(),
    val allParcels: List<ParcelOrder> = emptyList(),
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
    // Customer / Ride / Transport Navigation
    val customerTab: CustomerTab = CustomerTab.HOME,
    val rideTab: RideTab = RideTab.BOOK,
    val transportTab: TransportTab = TransportTab.SEND,
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
    val isRestaurantOwner: Boolean
        get() = adminRole == AdminRole.RESTAURANT_OWNER ||
            activeCoAdmin?.roleType?.equals(STAFF_ROLE_RESTAURANT_OWNER, ignoreCase = true) == true
    val isDeliveryPartner: Boolean
        get() = adminRole == AdminRole.DELIVERY_PARTNER ||
            activeCoAdmin?.roleType?.equals(STAFF_ROLE_DELIVERY_PARTNER, ignoreCase = true) == true
    val assignedRestaurantId: String get() = activeCoAdmin?.assignedRestaurantId.orEmpty()

    val hasUnlockedAdminAccess: Boolean
        get() = isMainAdmin || (
            (adminRole == AdminRole.CO_ADMIN || adminRole == AdminRole.RESTAURANT_OWNER) &&
                coAdminPasscodeVerified &&
                activeCoAdmin?.activeFlag == true
            )

    val hasUnlockedDeliveryAccess: Boolean
        get() = isMainAdmin || (
            isDeliveryPartner &&
                coAdminPasscodeVerified &&
                activeCoAdmin?.activeFlag == true
            )

    val canManageOrders: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageOrders == true)
    val canManageRestaurants: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageRestaurants == true)
    val canManageCoupons: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageCoupons == true)
    val canManageUsers: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageUsers == true)
    val canManageSettings: Boolean get() = isMainAdmin || (hasUnlockedAdminAccess && activeCoAdmin?.canManageSettings == true)
    val canManageStaff: Boolean get() = isMainAdmin || (adminRole == AdminRole.CO_ADMIN && hasUnlockedAdminAccess)

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

    private val initialVariant = TakeItAppVariant.fromContext(application.applicationContext)
    private val repo = TakeItFirestoreRepository(application.applicationContext)
    private val _uiState = MutableStateFlow(
        TakeItUiState(
            appVariant = initialVariant,
            isInAdminMode = initialVariant == TakeItAppVariant.ADMIN,
            isInDeliveryMode = initialVariant == TakeItAppVariant.DELIVERY
        )
    )
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
                isInAdminMode = it.appVariant == TakeItAppVariant.ADMIN,
                isInDeliveryMode = it.appVariant == TakeItAppVariant.DELIVERY
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
            repo.observeCategories().collect { list -> _uiState.update { it.copy(categories = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeHeroBanners().collect { list -> _uiState.update { it.copy(heroBanners = list) } }
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
            repo.observeMyRides(uid).collect { list -> _uiState.update { it.copy(myRides = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeMyParcels(uid).collect { list -> _uiState.update { it.copy(myParcels = list) } }
        }
        sessionJobs += viewModelScope.launch {
            repo.observeAddresses(uid).collect { list ->
                _uiState.update { state ->
                    val matching = list.firstOrNull { addr ->
                        addr.label.equals(state.profile.activeAddressLabel, ignoreCase = true) && addr.latitude != 0.0
                    } ?: list.firstOrNull { it.latitude != 0.0 }
                    state.copy(
                        addresses = list,
                        activeLatitude = matching?.latitude ?: state.activeLatitude,
                        activeLongitude = matching?.longitude ?: state.activeLongitude
                    )
                }
            }
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
            attachAdminStreams(role = AdminRole.MAIN_ADMIN, assignedRestId = "")
        } else {
            // Silently check if this Gmail is registered as Co-Admin, Restaurant Owner, or Delivery Partner
            sessionJobs += viewModelScope.launch {
                val res = repo.fetchMyCoAdminRecord(cleanEmail)
                val record = res.getOrNull()
                if (record != null && record.activeFlag) {
                    val mappedRole = when (record.roleType.uppercase()) {
                        STAFF_ROLE_RESTAURANT_OWNER -> AdminRole.RESTAURANT_OWNER
                        STAFF_ROLE_DELIVERY_PARTNER -> AdminRole.DELIVERY_PARTNER
                        else -> AdminRole.CO_ADMIN
                    }
                    _uiState.update { it.copy(adminRole = mappedRole, activeCoAdmin = record) }
                }
            }
        }
    }

    private fun attachAdminStreams(role: AdminRole, assignedRestId: String) {
        adminJobs.forEach { it.cancel() }
        adminJobs.clear()

        when (role) {
            AdminRole.RESTAURANT_OWNER -> {
                if (assignedRestId.isNotBlank()) {
                    adminJobs += viewModelScope.launch {
                        repo.observeOrdersForRestaurant(assignedRestId).collect { list ->
                            _uiState.update { it.copy(allOrders = list) }
                        }
                    }
                }
            }
            AdminRole.DELIVERY_PARTNER -> {
                adminJobs += viewModelScope.launch {
                    repo.observeAllOrders().collect { list -> _uiState.update { it.copy(allOrders = list) } }
                }
                adminJobs += viewModelScope.launch {
                    repo.observeAllRides().collect { list -> _uiState.update { it.copy(allRides = list) } }
                }
                adminJobs += viewModelScope.launch {
                    repo.observeAllParcels().collect { list -> _uiState.update { it.copy(allParcels = list) } }
                }
            }
            AdminRole.MAIN_ADMIN, AdminRole.CO_ADMIN -> {
                adminJobs += viewModelScope.launch {
                    repo.observeAllOrders().collect { list -> _uiState.update { it.copy(allOrders = list) } }
                }
                adminJobs += viewModelScope.launch {
                    repo.observeAllRides().collect { list -> _uiState.update { it.copy(allRides = list) } }
                }
                adminJobs += viewModelScope.launch {
                    repo.observeAllParcels().collect { list -> _uiState.update { it.copy(allParcels = list) } }
                }
                adminJobs += viewModelScope.launch {
                    repo.observeAllUsers().collect { list -> _uiState.update { it.copy(allUsers = list) } }
                }
                adminJobs += viewModelScope.launch {
                    repo.observeAllTickets().collect { list -> _uiState.update { it.copy(allTickets = list) } }
                }
                adminJobs += viewModelScope.launch {
                    repo.observeCoAdmins().collect { list -> _uiState.update { it.copy(coAdmins = list) } }
                }
                refreshRazorpayLedger()
            }
            AdminRole.NONE -> Unit
        }
    }

    fun detachSession() {
        sessionJobs.forEach { it.cancel() }
        sessionJobs.clear()
        adminJobs.forEach { it.cancel() }
        adminJobs.clear()
        _uiState.value = TakeItUiState(
            appVariant = initialVariant,
            isInAdminMode = initialVariant == TakeItAppVariant.ADMIN,
            isInDeliveryMode = initialVariant == TakeItAppVariant.DELIVERY
        )
    }

    // --- NAVIGATION & MODE SWITCHING ---

    fun selectCustomerTab(tab: CustomerTab) {
        _uiState.update { it.copy(customerTab = tab, customerBackStack = listOf(CustomerScreen.MainTabs)) }
    }

    fun selectRideTab(tab: RideTab) {
        _uiState.update { it.copy(rideTab = tab) }
    }

    fun selectTransportTab(tab: TransportTab) {
        _uiState.update { it.copy(transportTab = tab) }
    }

    fun selectDeliveryTab(tab: DeliveryTab) {
        _uiState.update { it.copy(deliveryTab = tab) }
    }

    fun toggleDeliveryOnline(online: Boolean) {
        _uiState.update {
            it.copy(
                deliveryPartnerOnline = online,
                snackbarMessage = if (online) "You are ONLINE — accepting live delivery jobs" else "You are OFFLINE"
            )
        }
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
        _uiState.update { it.copy(isInAdminMode = true, isInDeliveryMode = false) }
        val s = _uiState.value
        if (s.hasUnlockedAdminAccess) {
            attachAdminStreams(s.adminRole, s.assignedRestaurantId)
        }
    }

    fun exitAdminMode() {
        _uiState.update { it.copy(isInAdminMode = false) }
    }

    fun enterDeliveryMode() {
        _uiState.update { it.copy(isInDeliveryMode = true, isInAdminMode = false) }
        val s = _uiState.value
        if (s.hasUnlockedDeliveryAccess) {
            attachAdminStreams(if (s.isMainAdmin) AdminRole.MAIN_ADMIN else AdminRole.DELIVERY_PARTNER, "")
        }
    }

    fun exitDeliveryMode() {
        _uiState.update { it.copy(isInDeliveryMode = false) }
    }

    fun verifyCoAdminPasscode(passcodeInput: String) {
        val state = _uiState.value
        val email = state.email.trim().lowercase()
        if (email == MAIN_ADMIN_EMAIL) {
            _uiState.update { it.copy(coAdminPasscodeVerified = true, coAdminAuthError = null) }
            attachAdminStreams(AdminRole.MAIN_ADMIN, "")
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
                            coAdminAuthError = "Access denied. Only Main Admin ($MAIN_ADMIN_EMAIL) or registered Partners/Staff can enter."
                        )
                    }
                }
                !record.activeFlag -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCoAdmin = false,
                            coAdminAuthError = "Your account access has been suspended by Admin."
                        )
                    }
                }
                record.passcode != passcodeInput.trim() -> {
                    _uiState.update {
                        it.copy(
                            isCheckingCoAdmin = false,
                            coAdminAuthError = "Incorrect password. Ask Admin ($MAIN_ADMIN_EMAIL) or your Co-Admin for your passcode."
                        )
                    }
                }
                else -> {
                    val mappedRole = when (record.roleType.uppercase()) {
                        STAFF_ROLE_RESTAURANT_OWNER -> AdminRole.RESTAURANT_OWNER
                        STAFF_ROLE_DELIVERY_PARTNER -> AdminRole.DELIVERY_PARTNER
                        else -> AdminRole.CO_ADMIN
                    }
                    _uiState.update {
                        it.copy(
                            isCheckingCoAdmin = false,
                            adminRole = mappedRole,
                            activeCoAdmin = record,
                            coAdminPasscodeVerified = true,
                            coAdminAuthError = null,
                            isInDeliveryMode = it.isInDeliveryMode || (mappedRole == AdminRole.DELIVERY_PARTNER && it.appVariant != TakeItAppVariant.ADMIN)
                        )
                    }
                    attachAdminStreams(mappedRole, record.assignedRestaurantId)
                    showMessage("Welcome ${record.name} (${record.roleTitle})")
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

    fun saveStructuredAddress(address: SavedAddress) {
        if (address.fullAddress.isBlank()) {
            showMessage("Please enter complete address")
            return
        }
        viewModelScope.launch {
            repo.saveAddress(address)
                .onSuccess {
                    if (address.latitude != 0.0 && address.longitude != 0.0) {
                        _uiState.update { it.copy(activeLatitude = address.latitude, activeLongitude = address.longitude) }
                    }
                    showMessage("Address saved & selected!")
                }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to save address") }
        }
    }

    fun selectActiveAddress(address: SavedAddress) {
        val line = listOf(address.fullAddress, address.areaStreet, address.city)
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString(", ")
        viewModelScope.launch {
            repo.updateMyProfile(activeAddressLabel = address.label, activeAddressLine = line)
                .onSuccess {
                    if (address.latitude != 0.0 && address.longitude != 0.0) {
                        _uiState.update { it.copy(activeLatitude = address.latitude, activeLongitude = address.longitude) }
                    }
                    showMessage("Delivering to ${address.label}")
                }
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

        val matchedRestaurant = state.restaurants.firstOrNull { it.id == state.cartRestaurantId }
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
            deliveryLat = state.activeLatitude,
            deliveryLng = state.activeLongitude,
            pickupLat = matchedRestaurant?.latitude ?: 28.4744,
            pickupLng = matchedRestaurant?.longitude ?: 77.5040,
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

    fun saveCategory(category: FoodCategory) {
        if (category.name.isBlank()) {
            showMessage("Category name cannot be empty")
            return
        }
        viewModelScope.launch {
            repo.saveCategory(category)
                .onSuccess { showMessage("Category '${category.name}' is live!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to save category") }
        }
    }

    fun deleteCategory(categoryId: String) {
        viewModelScope.launch {
            repo.deleteCategory(categoryId)
                .onSuccess { showMessage("Category removed.") }
        }
    }

    fun saveHeroBanner(banner: HeroBannerSlide) {
        viewModelScope.launch {
            repo.saveHeroBanner(banner)
                .onSuccess { showMessage("Home carousel banner synced live!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to save banner") }
        }
    }

    fun deleteHeroBanner(bannerId: String) {
        viewModelScope.launch {
            repo.deleteHeroBanner(bannerId)
                .onSuccess { showMessage("Hero banner removed.") }
        }
    }

    fun saveCoAdmin(account: CoAdminAccount) {
        val state = _uiState.value
        val role = account.roleType.uppercase()
        val allowed = state.isMainAdmin ||
            (state.canManageStaff && (role == STAFF_ROLE_RESTAURANT_OWNER || role == STAFF_ROLE_DELIVERY_PARTNER))
        if (!allowed) {
            showMessage("Only Main Admin ($MAIN_ADMIN_EMAIL) can manage Co-Admins. Co-Admins can add Restaurant Owners & Delivery Partners.")
            return
        }
        viewModelScope.launch {
            repo.saveCoAdmin(account)
                .onSuccess { showMessage("${account.name} (${account.roleTitle}) saved! Share password with ${account.email}") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to save account") }
        }
    }

    fun deleteCoAdmin(account: CoAdminAccount) {
        val state = _uiState.value
        val allowed = state.isMainAdmin ||
            (state.canManageStaff && account.roleType in listOf(STAFF_ROLE_RESTAURANT_OWNER, STAFF_ROLE_DELIVERY_PARTNER))
        if (!allowed) {
            showMessage("Only Main Admin can remove Co-Admins.")
            return
        }
        viewModelScope.launch {
            repo.deleteCoAdmin(account.email)
                .onSuccess { showMessage("Account removed.") }
                .onFailure { showMessage(it.localizedMessage ?: "Delete failed") }
        }
    }

    // --- DELIVERY PARTNER WORKFLOWS ---

    fun deliveryClaimOrder(order: FoodOrder) {
        val state = _uiState.value
        val pName = state.activeCoAdmin?.name?.ifBlank { state.displayName } ?: state.displayName
        val pPhone = state.activeCoAdmin?.phone?.ifBlank { state.profile.phone } ?: state.profile.phone
        val pVeh = state.activeCoAdmin?.vehicleNumber?.ifBlank { "Take It Bike" } ?: "Take It Bike"
        adminUpdateOrderStatus(
            orderId = order.id,
            status = "OUT_FOR_DELIVERY",
            riderName = pName,
            riderPhone = pPhone,
            riderVehicle = pVeh,
            etaMinutes = order.etaMinutes.coerceIn(10, 45)
        )
    }

    fun deliveryCompleteOrderWithOtp(order: FoodOrder, enteredOtp: String, onSuccess: () -> Unit) {
        if (enteredOtp.trim() != order.otp.trim()) {
            showMessage("Wrong Customer OTP! Ask customer for the 4-digit OTP shown in their app.")
            return
        }
        adminUpdateOrderStatus(orderId = order.id, status = "DELIVERED", etaMinutes = 0)
        onSuccess()
    }

    fun deliveryClaimRide(ride: RideBooking) {
        val state = _uiState.value
        val cName = state.activeCoAdmin?.name?.ifBlank { state.displayName } ?: state.displayName
        val cPhone = state.activeCoAdmin?.phone?.ifBlank { state.profile.phone } ?: state.profile.phone
        val cVeh = state.activeCoAdmin?.vehicleNumber?.ifBlank { "UP16 TK 2026" } ?: "UP16 TK 2026"
        adminUpdateRide(ride.id, "CAPTAIN_ASSIGNED", cName, cPhone, cVeh, 4)
    }

    fun deliveryCompleteRideWithOtp(ride: RideBooking, enteredOtp: String, onSuccess: () -> Unit) {
        if (enteredOtp.trim() != ride.otp.trim()) {
            showMessage("Wrong Ride OTP! Ask rider for 4-digit OTP.")
            return
        }
        adminUpdateRide(ride.id, "COMPLETED", etaMinutes = 0)
        onSuccess()
    }

    fun deliveryClaimParcel(parcel: ParcelOrder) {
        val state = _uiState.value
        val pName = state.activeCoAdmin?.name?.ifBlank { state.displayName } ?: state.displayName
        val pPhone = state.activeCoAdmin?.phone?.ifBlank { state.profile.phone } ?: state.profile.phone
        val pVeh = state.activeCoAdmin?.vehicleNumber?.ifBlank { "UP16 CR 1102" } ?: "UP16 CR 1102"
        adminUpdateParcel(parcel.id, "PARTNER_ASSIGNED", pName, pPhone, pVeh, 20)
    }

    fun deliveryCompleteParcelWithOtp(parcel: ParcelOrder, enteredOtp: String, onSuccess: () -> Unit) {
        if (enteredOtp.trim() != parcel.otp.trim()) {
            showMessage("Wrong Delivery OTP! Ask recipient for 4-digit OTP.")
            return
        }
        adminUpdateParcel(parcel.id, "DELIVERED", etaMinutes = 0)
        onSuccess()
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

    // --- RAPIDO RIDE & PARCEL TRANSPORT ACTIONS ---

    fun bookRide(
        vehicleType: String,
        pickup: String,
        drop: String,
        distanceKm: Int,
        fare: Int,
        paymentMethod: String,
        paymentStatus: String,
        paymentRef: String,
        onBooked: () -> Unit = {}
    ) {
        val state = _uiState.value
        if (pickup.isBlank() || drop.isBlank()) {
            showMessage("Enter both pickup and drop locations")
            return
        }
        if (state.profile.suspendedFlag) {
            showMessage("Account suspended by Admin. Contact support.")
            return
        }
        val deductWallet = if (paymentMethod == "WALLET") fare.toDouble() else 0.0
        if (deductWallet > state.profile.walletBalance) {
            showMessage("Insufficient Wallet balance. Top up or pay via Razorpay / Cash.")
            return
        }
        val rideNum = "TKR-${Random.nextInt(10000, 99999)}"
        val otp = Random.nextInt(1000, 9999).toString()
        val ride = RideBooking(
            rideNumber = rideNum,
            userId = state.uid,
            customerName = state.profile.name.ifBlank { state.displayName },
            customerEmail = state.email,
            customerPhone = state.profile.phone,
            vehicleType = vehicleType,
            pickupAddress = pickup.trim(),
            dropAddress = drop.trim(),
            distanceKm = distanceKm.coerceAtLeast(1),
            totalFare = fare.coerceAtLeast(1),
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            paymentRef = paymentRef,
            otp = otp
        )
        viewModelScope.launch {
            repo.bookRide(ride, deductWallet)
                .onSuccess {
                    showMessage("Ride $rideNum requested! Finding nearby Captain...")
                    onBooked()
                }
                .onFailure { showMessage(it.localizedMessage ?: "Ride booking failed") }
        }
    }

    fun cancelMyRide(rideId: String) {
        viewModelScope.launch {
            repo.cancelMyRide(rideId)
                .onSuccess { showMessage("Ride cancelled.") }
                .onFailure { showMessage("Captain already assigned.") }
        }
    }

    fun rateMyRide(rideId: String, rating: Int) {
        viewModelScope.launch {
            repo.rateMyRide(rideId, rating)
                .onSuccess { showMessage("Rated Captain $rating★!") }
        }
    }

    fun adminUpdateRide(
        rideId: String,
        status: String,
        captainName: String? = null,
        captainPhone: String? = null,
        vehicleNumber: String? = null,
        etaMinutes: Int? = null
    ) {
        viewModelScope.launch {
            repo.adminUpdateRide(rideId, status, captainName, captainPhone, vehicleNumber, etaMinutes)
                .onSuccess { showMessage("Ride updated to $status live!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to update ride") }
        }
    }

    fun bookParcel(
        vehicleType: String,
        pickup: String,
        drop: String,
        receiverName: String,
        receiverPhone: String,
        packageType: String,
        weightKg: Int,
        fragile: Boolean,
        fare: Int,
        paymentMethod: String,
        paymentStatus: String,
        paymentRef: String,
        onBooked: () -> Unit = {}
    ) {
        val state = _uiState.value
        if (pickup.isBlank() || drop.isBlank() || receiverName.isBlank() || receiverPhone.isBlank()) {
            showMessage("Enter pickup, drop, receiver name & phone")
            return
        }
        if (state.profile.suspendedFlag) {
            showMessage("Account suspended by Admin.")
            return
        }
        val deductWallet = if (paymentMethod == "WALLET") fare.toDouble() else 0.0
        if (deductWallet > state.profile.walletBalance) {
            showMessage("Insufficient Wallet balance.")
            return
        }
        val parcelNum = "TKP-${Random.nextInt(10000, 99999)}"
        val otp = Random.nextInt(1000, 9999).toString()
        val parcel = ParcelOrder(
            parcelNumber = parcelNum,
            userId = state.uid,
            senderName = state.profile.name.ifBlank { state.displayName },
            senderEmail = state.email,
            senderPhone = state.profile.phone,
            receiverName = receiverName.trim(),
            receiverPhone = receiverPhone.trim(),
            pickupAddress = pickup.trim(),
            dropAddress = drop.trim(),
            vehicleType = vehicleType,
            packageType = packageType.ifBlank { "Parcel Box" },
            weightKg = weightKg.coerceAtLeast(1),
            fragileFlag = fragile,
            totalFare = fare.coerceAtLeast(1),
            paymentMethod = paymentMethod,
            paymentStatus = paymentStatus,
            paymentRef = paymentRef,
            otp = otp
        )
        viewModelScope.launch {
            repo.bookParcel(parcel, deductWallet)
                .onSuccess {
                    showMessage("Shipment $parcelNum booked! Tracking live.")
                    onBooked()
                }
                .onFailure { showMessage(it.localizedMessage ?: "Booking failed") }
        }
    }

    fun cancelMyParcel(parcelId: String) {
        viewModelScope.launch {
            repo.cancelMyParcel(parcelId)
                .onSuccess { showMessage("Shipment cancelled.") }
                .onFailure { showMessage("Partner already assigned.") }
        }
    }

    fun rateMyParcel(parcelId: String, rating: Int) {
        viewModelScope.launch {
            repo.rateMyParcel(parcelId, rating)
                .onSuccess { showMessage("Rated delivery $rating★!") }
        }
    }

    fun adminUpdateParcel(
        parcelId: String,
        status: String,
        partnerName: String? = null,
        partnerPhone: String? = null,
        vehicleNumber: String? = null,
        etaMinutes: Int? = null
    ) {
        viewModelScope.launch {
            repo.adminUpdateParcel(parcelId, status, partnerName, partnerPhone, vehicleNumber, etaMinutes)
                .onSuccess { showMessage("Shipment updated to $status live!") }
                .onFailure { showMessage(it.localizedMessage ?: "Failed to update shipment") }
        }
    }
}
