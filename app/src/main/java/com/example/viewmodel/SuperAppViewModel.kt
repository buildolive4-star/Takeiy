package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class SuperAppUiState(
    val isSplashVisible: Boolean = true,
    val appState: AppStateEntity = AppStateEntity(),
    val activePanel: AppPanel = AppPanel.CUSTOMER,
    val backStack: List<AppRoute> = listOf(AppRoute.PanelHome),
    val orders: List<OrderEntity> = emptyList(),
    val transactions: List<WalletTxEntity> = emptyList(),
    val menuItems: List<MenuItemEntity> = emptyList(),
    val groceryProducts: List<GroceryProductEntity> = emptyList(),
    val notifications: List<NotificationEntity> = emptyList(),
    val managedUsers: List<ManagedUserEntity> = emptyList(),
    val cart: List<CartEntry> = emptyList(),
    val appliedCoupon: CouponItem? = null,
    val favouriteRestaurantIds: Set<String> = setOf("r1", "r4"),
    val recentSearches: List<String> = listOf("Bike Taxi to MG Road", "Biryani", "Fresh Strawberries", "Tata Ace"),
    val showAdminGate: Boolean = false,
    val showConfetti: Boolean = false,
    val snackbarMessage: String? = null,
    val driverEnabledServices: Set<String> = setOf("Bike Taxi", "Auto", "Food Delivery", "Parcel Delivery"),
    val driverEarningsToday: Int = 1485,
    val driverRidesToday: Int = 14,
    val driverIncentivesToday: Int = 350,
    val deliveryPartnerEarningsToday: Int = 1120,
    val deliveryPartnerCompletedToday: Int = 16,
    val activeDriverRequestIndex: Int = 0,
    val activeFoodDeliveryIndex: Int = 0,
    val activeParcelJobIndex: Int = 0,
    val adminConfig: AdminPlatformConfig = AdminPlatformConfig()
) {
    val currentRoute: AppRoute get() = backStack.lastOrNull() ?: AppRoute.PanelHome
    val unreadNotificationCount: Int get() = notifications.count { !it.isRead }
    val cartItemCount: Int get() = cart.sumOf { it.quantity }
    val cartSubtotal: Int get() = cart.sumOf { it.unitPrice * it.quantity }
    val cartMrpTotal: Int get() = cart.sumOf { it.mrp * it.quantity }
    val cartDiscount: Int
        get() = appliedCoupon?.let {
            if (cartSubtotal >= it.minOrder) it.discountAmount else 0
        } ?: 0
    val cartDeliveryFee: Int get() = if (cart.isEmpty()) 0 else adminConfig.baseDeliveryFee
    val cartPlatformFee: Int get() = if (cart.isEmpty()) 0 else 9
    val cartTaxes: Int get() = ((cartSubtotal - cartDiscount).coerceAtLeast(0) * 0.05).toInt()
    val cartGrandTotal: Int
        get() = if (cart.isEmpty()) 0 else (cartSubtotal - cartDiscount + cartDeliveryFee + cartPlatformFee + cartTaxes).coerceAtLeast(1)
}

class SuperAppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SuperAppRepository(SuperAppRepository.getDatabase(application))

    private val _localState = MutableStateFlow(SuperAppUiState())

    val uiState: StateFlow<SuperAppUiState> = combine(
        _localState,
        repository.appState,
        repository.orders,
        repository.transactions,
        repository.menuItems
    ) { local, appState, orders, txs, menu ->
        val panel = runCatching { AppPanel.valueOf(appState.activePanelName) }.getOrDefault(AppPanel.CUSTOMER)
        local.copy(
            appState = appState,
            activePanel = if (panel == AppPanel.ADMIN && !appState.isAdminAuthorized) AppPanel.CUSTOMER else panel,
            orders = orders,
            transactions = txs,
            menuItems = menu
        )
    }.combine(
        combine(repository.groceryProducts, repository.notifications, repository.managedUsers) { g, n, u ->
            Triple(g, n, u)
        }
    ) { merged, (groceries, notifs, users) ->
        merged.copy(
            groceryProducts = groceries,
            notifications = notifs,
            managedUsers = users
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SuperAppUiState()
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
            delay(1750)
            _localState.update { it.copy(isSplashVisible = false) }
        }
    }

    fun dismissSplash() {
        _localState.update { it.copy(isSplashVisible = false) }
    }

    fun showMessage(msg: String) {
        _localState.update { it.copy(snackbarMessage = msg) }
    }

    fun clearMessage() {
        _localState.update { it.copy(snackbarMessage = null) }
    }

    fun triggerConfetti() {
        viewModelScope.launch {
            _localState.update { it.copy(showConfetti = true) }
            delay(2600)
            _localState.update { it.copy(showConfetti = false) }
        }
    }

    // Navigation
    fun navigateTo(route: AppRoute) {
        _localState.update { state ->
            if (state.currentRoute == route) state
            else state.copy(backStack = state.backStack + route)
        }
    }

    fun navigateBack(): Boolean {
        val currentStack = _localState.value.backStack
        if (currentStack.size <= 1) return false
        _localState.update { it.copy(backStack = it.backStack.dropLast(1)) }
        return true
    }

    fun resetToHome() {
        _localState.update { it.copy(backStack = listOf(AppRoute.PanelHome)) }
    }

    // Universal Panel Switcher
    fun requestPanelSwitch(panel: AppPanel) {
        val currentState = uiState.value.appState
        if (panel.isAdminOnly && !currentState.isAdminAuthorized) {
            _localState.update { it.copy(showAdminGate = true) }
            return
        }
        viewModelScope.launch {
            repository.updateAppState { it.copy(activePanelName = panel.name) }
            _localState.update {
                it.copy(
                    backStack = listOf(AppRoute.PanelHome),
                    snackbarMessage = "Switched to ${panel.title}"
                )
            }
        }
    }

    fun dismissAdminGate() {
        _localState.update { it.copy(showAdminGate = false) }
    }

    fun verifyAdminPasscode(pin: String): Boolean {
        return if (pin.trim() == "2026") {
            viewModelScope.launch {
                repository.updateAppState {
                    it.copy(isAdminAuthorized = true, activePanelName = AppPanel.ADMIN.name)
                }
                _localState.update {
                    it.copy(
                        showAdminGate = false,
                        backStack = listOf(AppRoute.PanelHome),
                        snackbarMessage = "Admin session authorized"
                    )
                }
            }
            true
        } else {
            false
        }
    }

    fun revokeAdminAccess() {
        viewModelScope.launch {
            repository.updateAppState {
                it.copy(isAdminAuthorized = false, activePanelName = AppPanel.CUSTOMER.name)
            }
            _localState.update {
                it.copy(
                    backStack = listOf(AppRoute.PanelHome),
                    snackbarMessage = "Admin locked • Returned to Customer Mode"
                )
            }
        }
    }

    // Location & Search
    fun selectAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.updateAppState {
                it.copy(activeAddressLabel = address.label, activeAddressLine = address.fullAddress)
            }
            showMessage("Location set to ${address.label}")
        }
    }

    fun addSearchQuery(query: String) {
        val cleaned = query.trim()
        if (cleaned.isEmpty()) return
        _localState.update { state ->
            state.copy(recentSearches = (listOf(cleaned) + state.recentSearches.filterNot { it.equals(cleaned, true) }).take(6))
        }
    }

    fun toggleFavouriteRestaurant(restaurantId: String) {
        _localState.update { state ->
            val updated = if (restaurantId in state.favouriteRestaurantIds) {
                state.favouriteRestaurantIds - restaurantId
            } else {
                state.favouriteRestaurantIds + restaurantId
            }
            state.copy(favouriteRestaurantIds = updated)
        }
    }

    // Cart Operations
    fun addToCart(entry: CartEntry) {
        _localState.update { state ->
            val existing = state.cart.find { it.itemId == entry.itemId && it.customization == entry.customization }
            val updated = if (existing != null) {
                state.cart.map {
                    if (it.itemId == entry.itemId && it.customization == entry.customization)
                        it.copy(quantity = it.quantity + entry.quantity)
                    else it
                }
            } else {
                state.cart + entry
            }
            state.copy(cart = updated, snackbarMessage = "Added ${entry.name} to cart")
        }
    }

    fun updateCartQuantity(itemId: String, delta: Int) {
        _localState.update { state ->
            val updated = state.cart.mapNotNull { item ->
                if (item.itemId == itemId) {
                    val nextQty = item.quantity + delta
                    if (nextQty <= 0) null else item.copy(quantity = nextQty)
                } else item
            }
            state.copy(cart = updated)
        }
    }

    fun applyCoupon(coupon: CouponItem?) {
        _localState.update {
            it.copy(
                appliedCoupon = coupon,
                snackbarMessage = if (coupon != null) "Coupon ${coupon.code} applied!" else "Coupon removed"
            )
        }
    }

    fun placeCartOrder(paymentMethod: String, simulateFailure: Boolean): Boolean {
        if (simulateFailure) {
            showMessage("Simulated payment declined ($paymentMethod). Please retry or switch method.")
            return false
        }
        val state = uiState.value
        if (state.cart.isEmpty()) return false
        val total = state.cartGrandTotal.toDouble()
        if (paymentMethod == "Zipora Wallet" && state.appState.walletBalance < total) {
            showMessage("Insufficient Zipora Wallet balance. Please top up or select UPI/Card.")
            return false
        }
        val primaryType = state.cart.first().categoryType
        val primarySource = state.cart.first().sourceName
        val summary = state.cart.joinToString(", ") { "${it.quantity}x ${it.name}" }
        val otp = (1000..9999).random().toString()
        val order = OrderEntity(
            id = "ZP-${(1000..9999).random()}",
            type = primaryType,
            title = primarySource,
            subtitle = summary,
            status = if (primaryType == "FOOD") "PENDING" else "PREPARING",
            amount = total,
            paymentMethod = paymentMethod,
            pickupLocation = primarySource,
            dropLocation = state.appState.activeAddressLine,
            otp = otp,
            partnerName = listOf("Kiran Gowda", "Deepak R.", "Arjun Kumar", "Manoj Singh").random(),
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.createOrder(order, deductFromWallet = paymentMethod == "Zipora Wallet")
            _localState.update { it.copy(cart = emptyList(), appliedCoupon = null) }
            triggerConfetti()
            showMessage("Order ${order.id} placed! Delivery OTP: $otp")
        }
        return true
    }

    // Ride / Parcel / Transport Order Recording
    fun recordServiceOrder(
        type: String,
        title: String,
        subtitle: String,
        amount: Double,
        paymentMethod: String,
        pickup: String,
        drop: String,
        otp: String,
        partnerName: String,
        status: String = "ON_THE_WAY",
        onCreated: (String) -> Unit = {}
    ) {
        val id = "ZP-${(1000..9999).random()}"
        val order = OrderEntity(
            id = id,
            type = type,
            title = title,
            subtitle = subtitle,
            status = status,
            amount = amount,
            paymentMethod = paymentMethod,
            pickupLocation = pickup,
            dropLocation = drop,
            otp = otp,
            partnerName = partnerName,
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.createOrder(order, deductFromWallet = paymentMethod == "Zipora Wallet")
            onCreated(id)
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            if (newStatus == "COMPLETED") triggerConfetti()
        }
    }

    fun rateOrder(orderId: String, stars: Int) {
        viewModelScope.launch {
            repository.rateOrder(orderId, stars)
            showMessage("Rated $stars★ • Thank you for your feedback!")
        }
    }

    // Wallet
    fun topUpWallet(amount: Double, method: String, simulateFailure: Boolean): Boolean {
        if (simulateFailure) {
            showMessage("Simulated $method top-up failed. No money was charged.")
            return false
        }
        viewModelScope.launch {
            repository.addWalletMoney(amount, method)
            triggerConfetti()
            showMessage("₹${amount.toInt()} added to Zipora Wallet (Simulated)")
        }
        return true
    }

    // Profile & Notifications
    fun updateProfile(name: String, phone: String, email: String, emergencyName: String, emergencyPhone: String) {
        viewModelScope.launch {
            repository.updateAppState {
                it.copy(
                    userName = name.ifBlank { it.userName },
                    userPhone = phone.ifBlank { it.userPhone },
                    userEmail = email.ifBlank { it.userEmail },
                    emergencyContactName = emergencyName.ifBlank { it.emergencyContactName },
                    emergencyContactPhone = emergencyPhone.ifBlank { it.emergencyContactPhone }
                )
            }
            showMessage("Profile saved locally")
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            repository.updateAppState { it.copy(language = lang) }
            showMessage("Language set to $lang")
        }
    }

    fun toggleNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAppState { it.copy(notificationsEnabled = enabled) }
        }
    }

    fun markAllNotificationsRead() {
        viewModelScope.launch { repository.markAllNotificationsRead() }
    }

    // Partner Mode Actions
    fun toggleDriverOnline(online: Boolean) {
        viewModelScope.launch {
            repository.updateAppState { it.copy(driverOnline = online) }
            showMessage(if (online) "You are now ONLINE & receiving trips" else "You are now OFFLINE")
        }
    }

    fun toggleDeliveryPartnerOnline(online: Boolean) {
        viewModelScope.launch {
            repository.updateAppState { it.copy(deliveryPartnerOnline = online) }
            showMessage(if (online) "Delivery radar ACTIVE" else "Delivery radar PAUSED")
        }
    }

    fun toggleDriverService(serviceName: String) {
        _localState.update { state ->
            val updated = if (serviceName in state.driverEnabledServices) {
                state.driverEnabledServices - serviceName
            } else {
                state.driverEnabledServices + serviceName
            }
            state.copy(driverEnabledServices = updated)
        }
    }

    fun nextDriverRequest(completedEarnings: Int = 0) {
        _localState.update { state ->
            state.copy(
                activeDriverRequestIndex = (state.activeDriverRequestIndex + 1) % DemoCatalog.incomingDriverRequests.size,
                driverEarningsToday = state.driverEarningsToday + completedEarnings,
                driverRidesToday = if (completedEarnings > 0) state.driverRidesToday + 1 else state.driverRidesToday
            )
        }
        if (completedEarnings > 0) {
            viewModelScope.launch {
                repository.addWalletMoney(completedEarnings.toDouble(), "Captain Payout", "CREDIT", "Completed Trip Earnings")
            }
            triggerConfetti()
        }
    }

    fun nextFoodDeliveryJob(completedEarnings: Int = 0) {
        _localState.update { state ->
            state.copy(
                activeFoodDeliveryIndex = (state.activeFoodDeliveryIndex + 1) % DemoCatalog.incomingFoodDeliveries.size,
                deliveryPartnerEarningsToday = state.deliveryPartnerEarningsToday + completedEarnings,
                deliveryPartnerCompletedToday = if (completedEarnings > 0) state.deliveryPartnerCompletedToday + 1 else state.deliveryPartnerCompletedToday
            )
        }
        if (completedEarnings > 0) {
            viewModelScope.launch {
                repository.addWalletMoney(completedEarnings.toDouble(), "Delivery Payout", "CREDIT", "Food Delivery Earnings")
            }
            triggerConfetti()
        }
    }

    fun nextParcelJob(completedEarnings: Int = 0) {
        _localState.update { state ->
            state.copy(
                activeParcelJobIndex = (state.activeParcelJobIndex + 1) % DemoCatalog.incomingParcelAndTransport.size
            )
        }
        if (completedEarnings > 0) {
            viewModelScope.launch {
                repository.addWalletMoney(completedEarnings.toDouble(), "Logistics Payout", "CREDIT", "Parcel/Transport Earnings")
            }
            triggerConfetti()
        }
    }

    // Restaurant Owner Actions (live persisted!)
    fun addOrUpdateMenuItem(
        id: String?,
        restaurantId: String,
        category: String,
        name: String,
        description: String,
        price: Int,
        isVeg: Boolean
    ) {
        viewModelScope.launch {
            repository.upsertMenuItem(
                MenuItemEntity(
                    id = id ?: "m-${UUID.randomUUID().toString().take(5)}",
                    restaurantId = restaurantId,
                    category = category.ifBlank { "Chef Specials" },
                    name = name,
                    description = description.ifBlank { "Freshly crafted signature recipe" },
                    price = price.coerceAtLeast(25),
                    isVeg = isVeg,
                    isAvailable = true,
                    isBestseller = false
                )
            )
            showMessage("Menu updated: $name (₹$price)")
        }
    }

    fun toggleMenuItemAvailability(id: String, available: Boolean) {
        viewModelScope.launch { repository.setMenuItemAvailable(id, available) }
    }

    // Grocery Owner Actions (live persisted!)
    fun addOrUpdateGroceryProduct(
        id: String?,
        name: String,
        category: String,
        unit: String,
        mrp: Int,
        sellingPrice: Int,
        stock: Int
    ) {
        viewModelScope.launch {
            repository.upsertGroceryProduct(
                GroceryProductEntity(
                    id = id ?: "g-${UUID.randomUUID().toString().take(5)}",
                    name = name,
                    category = category.ifBlank { "Daily Essentials" },
                    unit = unit.ifBlank { "1 Pack" },
                    mrp = mrp.coerceAtLeast(sellingPrice),
                    sellingPrice = sellingPrice.coerceAtLeast(10),
                    stockCount = stock.coerceAtLeast(0),
                    deliveryMins = 9,
                    isFeatured = true
                )
            )
            showMessage("Inventory saved: $name")
        }
    }

    fun adjustGroceryStock(id: String, stock: Int, price: Int) {
        viewModelScope.launch {
            repository.updateGroceryStockAndPrice(id, stock.coerceAtLeast(0), price.coerceAtLeast(10))
        }
    }

    // Admin Actions
    fun toggleUserVerification(user: ManagedUserEntity) {
        viewModelScope.launch {
            repository.updateManagedUser(user.copy(isVerified = !user.isVerified))
            showMessage("${user.name} verification updated")
        }
    }

    fun toggleUserSuspension(user: ManagedUserEntity) {
        viewModelScope.launch {
            repository.updateManagedUser(user.copy(isSuspended = !user.isSuspended))
            showMessage("${user.name} ${if (!user.isSuspended) "suspended" else "reactivated"}")
        }
    }

    fun refundOrder(order: OrderEntity) {
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, "REFUNDED")
            repository.addWalletMoney(order.amount, "Admin Refund", "REFUND", "Refund for ${order.id}")
            showMessage("Refunded ₹${order.amount.toInt()} for ${order.id}")
        }
    }

    fun updateAdminConfig(transform: (AdminPlatformConfig) -> AdminPlatformConfig) {
        _localState.update { it.copy(adminConfig = transform(it.adminConfig)) }
    }

    fun sendAdminBroadcast(title: String, message: String) {
        viewModelScope.launch {
            repository.pushNotification("Promotional Offers", title, message)
            showMessage("Broadcast sent to all active users")
        }
    }
}
