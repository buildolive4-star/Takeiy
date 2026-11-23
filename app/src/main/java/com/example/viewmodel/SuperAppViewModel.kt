package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.firebase.TakeItCloudRepository
import com.example.model.*
import com.example.util.GpsLocationResult
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class SuperAppUiState(
    val isSplashVisible: Boolean = true,
    val isCloudSynced: Boolean = false,
    val cloudError: String? = null,
    val userAvatarUrl: String = "",
    val dishImageUrls: Map<String, String> = emptyMap(),
    val groceryImageUrls: Map<String, String> = emptyMap(),
    val appState: AppStateEntity = AppStateEntity(),
    val activePanel: AppPanel = AppPanel.CUSTOMER,
    val backStack: List<AppRoute> = listOf(TakeItBuildConfig.rootRoute),
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
    private val cloudRepo = TakeItCloudRepository(application.applicationContext)

    private val _localState = MutableStateFlow(SuperAppUiState(isSplashVisible = false))
    private val _cloudOrders = MutableStateFlow<List<OrderEntity>?>(null)
    private val _cloudTransactions = MutableStateFlow<List<WalletTxEntity>?>(null)
    private val _cloudNotifications = MutableStateFlow<List<NotificationEntity>?>(null)
    private val _cloudMenuItems = MutableStateFlow<List<MenuItemEntity>?>(null)
    private val _cloudGroceries = MutableStateFlow<List<GroceryProductEntity>?>(null)

    private var activeCloudUid: String? = null
    private val cloudJobs = mutableListOf<Job>()

    val uiState: StateFlow<SuperAppUiState> = combine(
        _localState,
        repository.appState,
        combine(repository.orders, _cloudOrders) { local, cloud ->
            if (cloud != null && cloud.isNotEmpty()) cloud else local
        },
        combine(repository.transactions, _cloudTransactions) { local, cloud ->
            if (cloud != null && cloud.isNotEmpty()) cloud else local
        },
        combine(repository.menuItems, _cloudMenuItems) { local, cloud ->
            if (cloud != null && cloud.isNotEmpty()) {
                val cloudIds = cloud.map { it.id }.toSet()
                cloud + local.filterNot { it.id in cloudIds }
            } else local
        }
    ) { local, appState, orders, txs, menu ->
        val panel = if (!TakeItBuildConfig.showPanelSwitcher) {
            AppPanel.CUSTOMER
        } else {
            runCatching { AppPanel.valueOf(appState.activePanelName) }.getOrDefault(AppPanel.CUSTOMER)
        }
        local.copy(
            appState = appState,
            activePanel = if (panel == AppPanel.ADMIN && !appState.isAdminAuthorized) AppPanel.CUSTOMER else panel,
            orders = orders,
            transactions = txs,
            menuItems = menu
        )
    }.combine(
        combine(
            combine(repository.groceryProducts, _cloudGroceries) { local, cloud ->
                if (cloud != null && cloud.isNotEmpty()) {
                    val cloudIds = cloud.map { it.id }.toSet()
                    cloud + local.filterNot { it.id in cloudIds }
                } else local
            },
            combine(repository.notifications, _cloudNotifications) { local, cloud ->
                if (cloud != null && cloud.isNotEmpty()) cloud else local
            },
            repository.managedUsers
        ) { g, n, u -> Triple(g, n, u) }
    ) { merged, (groceries, notifs, users) ->
        merged.copy(
            groceryProducts = groceries,
            notifications = notifs,
            managedUsers = users
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SuperAppUiState(isSplashVisible = false)
    )

    init {
        viewModelScope.launch {
            repository.ensureSeeded()
        }
    }

    /**
     * Attaches real-time Firestore snapshot listeners ONLY for an authenticated user UID.
     */
    fun attachCloudSession(uid: String, displayName: String, email: String) {
        if (uid.isBlank() || activeCloudUid == uid) return
        detachCloudSession()
        activeCloudUid = uid

        viewModelScope.launch {
            val initResult = cloudRepo.ensureProfileInitialized(displayName, email)
            if (initResult.isFailure) {
                _localState.update {
                    it.copy(cloudError = initResult.exceptionOrNull()?.localizedMessage ?: "Cloud sync unavailable")
                }
            } else {
                _localState.update { it.copy(isCloudSynced = true, cloudError = null) }
            }
        }

        // 1. Live User Profile listener
        cloudJobs += viewModelScope.launch {
            cloudRepo.observeUserProfile(uid)
                .catch { error -> handleStreamError(error, "profile") }
                .collect { cloudProfile ->
                    if (cloudProfile != null) {
                        repository.updateAppState { current ->
                            current.copy(
                                userName = cloudProfile.userName.ifBlank { current.userName },
                                userPhone = cloudProfile.userPhone.ifBlank { current.userPhone },
                                userEmail = cloudProfile.userEmail.ifBlank { current.userEmail },
                                activeAddressLabel = cloudProfile.activeAddressLabel.ifBlank { current.activeAddressLabel },
                                activeAddressLine = cloudProfile.activeAddressLine.ifBlank { current.activeAddressLine },
                                walletBalance = cloudProfile.walletBalance,
                                rewardPoints = cloudProfile.rewardPoints.toInt(),
                                activePanelName = cloudProfile.activePanelName.ifBlank { current.activePanelName },
                                driverOnline = cloudProfile.driverOnline,
                                deliveryPartnerOnline = cloudProfile.deliveryPartnerOnline,
                                language = cloudProfile.language.ifBlank { current.language },
                                notificationsEnabled = cloudProfile.notificationsEnabled
                            )
                        }
                        _localState.update {
                            it.copy(
                                isCloudSynced = true,
                                cloudError = null,
                                userAvatarUrl = cloudProfile.avatarUrl.ifBlank { it.userAvatarUrl }
                            )
                        }
                    }
                }
        }

        // 2. Live Orders listener
        cloudJobs += viewModelScope.launch {
            cloudRepo.observeOrders(uid)
                .catch { error -> handleStreamError(error, "orders") }
                .collect { list -> _cloudOrders.value = list }
        }

        // 3. Live Wallet Transactions listener
        cloudJobs += viewModelScope.launch {
            cloudRepo.observeTransactions(uid)
                .catch { error -> handleStreamError(error, "wallet") }
                .collect { list -> _cloudTransactions.value = list }
        }

        // 4. Live Notifications listener
        cloudJobs += viewModelScope.launch {
            cloudRepo.observeNotifications(uid)
                .catch { error -> handleStreamError(error, "notifications") }
                .collect { list -> _cloudNotifications.value = list }
        }

        // 5. Live Restaurant Menu listener (with ImgBB photo URLs)
        cloudJobs += viewModelScope.launch {
            cloudRepo.observeMenuItemsWithImages(uid)
                .catch { error -> handleStreamError(error, "menu") }
                .collect { (list, imgMap) ->
                    _cloudMenuItems.value = list
                    _localState.update { it.copy(dishImageUrls = it.dishImageUrls + imgMap) }
                }
        }

        // 6. Live Grocery Inventory listener (with ImgBB photo URLs)
        cloudJobs += viewModelScope.launch {
            cloudRepo.observeGroceryProductsWithImages(uid)
                .catch { error -> handleStreamError(error, "grocery") }
                .collect { (list, imgMap) ->
                    _cloudGroceries.value = list
                    _localState.update { it.copy(groceryImageUrls = it.groceryImageUrls + imgMap) }
                }
        }
    }

    private fun handleStreamError(error: Throwable, section: String) {
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        val rootError = firestoreError ?: error
        _localState.update {
            it.copy(cloudError = "Cloud sync ($section): ${rootError.message ?: "Offline mode"}")
        }
    }

    /**
     * Cancels all active Firestore listeners cleanly before sign-out.
     */
    fun detachCloudSession() {
        activeCloudUid = null
        cloudJobs.forEach { it.cancel() }
        cloudJobs.clear()
        _cloudOrders.value = null
        _cloudTransactions.value = null
        _cloudNotifications.value = null
        _cloudMenuItems.value = null
        _cloudGroceries.value = null
        _localState.update { it.copy(isCloudSynced = false) }
    }

    override fun onCleared() {
        detachCloudSession()
        super.onCleared()
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
        _localState.update { it.copy(backStack = listOf(TakeItBuildConfig.rootRoute)) }
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
            cloudRepo.updateProfileFields(mapOf("activePanelName" to panel.name))
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
                cloudRepo.updateProfileFields(mapOf("activePanelName" to AppPanel.ADMIN.name))
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
            cloudRepo.updateProfileFields(mapOf("activePanelName" to AppPanel.CUSTOMER.name))
            _localState.update {
                it.copy(
                    backStack = listOf(AppRoute.PanelHome),
                    snackbarMessage = "Admin locked • Returned to Customer Mode"
                )
            }
        }
    }

    // Location & GPS Sync
    fun selectAddress(address: SavedAddress) {
        viewModelScope.launch {
            repository.updateAppState {
                it.copy(activeAddressLabel = address.label, activeAddressLine = address.fullAddress)
            }
            cloudRepo.updateProfileFields(
                mapOf(
                    "activeAddressLabel" to address.label,
                    "activeAddressLine" to address.fullAddress
                )
            )
            showMessage("Location set to ${address.label}")
        }
    }

    fun saveGpsLocation(gps: GpsLocationResult) {
        viewModelScope.launch {
            repository.updateAppState {
                it.copy(activeAddressLabel = gps.label, activeAddressLine = gps.fullAddress)
            }
            cloudRepo.updateProfileFields(
                mapOf(
                    "activeAddressLabel" to gps.label,
                    "activeAddressLine" to gps.fullAddress
                )
            )
            cloudRepo.saveGpsAddress(
                label = gps.label,
                fullAddress = gps.fullAddress,
                landmark = "Live GPS Location",
                lat = gps.latitude,
                lng = gps.longitude
            )
            showMessage("Live GPS detected: ${gps.label}")
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
            showMessage("Payment declined ($paymentMethod). Please retry or choose Wallet/COD.")
            return false
        }
        val state = uiState.value
        if (state.cart.isEmpty()) return false
        val total = state.cartGrandTotal.toDouble()
        val isWallet = paymentMethod.contains("Wallet", ignoreCase = true)
        if (isWallet && state.appState.walletBalance < total) {
            showMessage("Insufficient Take It Wallet balance. Please top up or select Cash on Delivery.")
            return false
        }
        val primaryType = state.cart.first().categoryType
        val primarySource = state.cart.first().sourceName
        val summary = state.cart.joinToString(", ") { "${it.quantity}x ${it.name}" }
        val otp = (1000..9999).random().toString()
        val order = OrderEntity(
            id = "TK-${(1000..9999).random()}",
            type = primaryType,
            title = primarySource,
            subtitle = summary,
            status = if (primaryType == "FOOD") "PENDING" else "PREPARING",
            amount = total,
            paymentMethod = paymentMethod,
            pickupLocation = primarySource,
            dropLocation = state.appState.activeAddressLine,
            otp = otp,
            partnerName = listOf("Rakesh Kumar", "Deepak R.", "Arjun Kumar", "Manoj Singh").random(),
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            repository.createOrder(order, deductFromWallet = isWallet)
            cloudRepo.createOrder(order)
            if (isWallet) {
                val newBalance = (state.appState.walletBalance - total).coerceAtLeast(0.0)
                cloudRepo.updateProfileFields(mapOf("walletBalance" to newBalance))
                cloudRepo.recordWalletTransaction(
                    WalletTxEntity("TX_${System.currentTimeMillis()}", "Paid for ${order.title}", "DEBIT", total, paymentMethod, System.currentTimeMillis())
                )
            }
            cloudRepo.pushNotification(
                id = "N_${System.currentTimeMillis()}",
                category = "ORDER",
                title = "Order ${order.id} Confirmed",
                body = "${order.title} • ${order.subtitle} is now live."
            )
            _localState.update { it.copy(cart = emptyList(), appliedCoupon = null) }
            triggerConfetti()
            showMessage("Order ${order.id} synced to cloud! Delivery PIN: $otp")
        }
        return true
    }

    // Ride / Parcel / Transport Order Recording (Cloud + Local)
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
        val id = "TK-${(1000..9999).random()}"
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
        val isWallet = paymentMethod.contains("Wallet", ignoreCase = true)
        val currentBalance = uiState.value.appState.walletBalance
        viewModelScope.launch {
            repository.createOrder(order, deductFromWallet = isWallet)
            cloudRepo.createOrder(order)
            if (isWallet) {
                val newBal = (currentBalance - amount).coerceAtLeast(0.0)
                cloudRepo.updateProfileFields(mapOf("walletBalance" to newBal))
                cloudRepo.recordWalletTransaction(
                    WalletTxEntity("TX_${System.currentTimeMillis()}", title, "DEBIT", amount, paymentMethod, System.currentTimeMillis())
                )
            }
            cloudRepo.pushNotification(
                id = "N_${System.currentTimeMillis()}",
                category = type,
                title = "$title Confirmed",
                body = "$pickup → $drop • Partner: $partnerName"
            )
            onCreated(id)
        }
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        viewModelScope.launch {
            repository.updateOrderStatus(orderId, newStatus)
            cloudRepo.updateOrderStatus(orderId, newStatus)
            if (newStatus == "COMPLETED") triggerConfetti()
        }
    }

    fun rateOrder(orderId: String, stars: Int) {
        viewModelScope.launch {
            repository.rateOrder(orderId, stars)
            cloudRepo.rateOrder(orderId, stars)
            showMessage("Rated $stars★ • Saved to your Take It account!")
        }
    }

    // Wallet
    fun topUpWallet(amount: Double, method: String, simulateFailure: Boolean): Boolean {
        if (simulateFailure) {
            showMessage("$method top-up declined. No money was charged.")
            return false
        }
        val newBalance = (uiState.value.appState.walletBalance + amount).coerceAtMost(1_000_000.0)
        viewModelScope.launch {
            repository.addWalletMoney(amount, method)
            cloudRepo.updateProfileFields(mapOf("walletBalance" to newBalance))
            cloudRepo.recordWalletTransaction(
                WalletTxEntity("TX_${System.currentTimeMillis()}", "Wallet Top-up ($method)", "CREDIT", amount, method, System.currentTimeMillis())
            )
            triggerConfetti()
            showMessage("₹${amount.toInt()} added to Take It Wallet & synced")
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
            cloudRepo.updateProfileFields(
                mapOf(
                    "userName" to name.ifBlank { null },
                    "userPhone" to phone.ifBlank { null },
                    "userEmail" to email.ifBlank { null }
                )
            )
            showMessage("Profile saved to cloud")
        }
    }

    fun updateProfileAvatarUrl(url: String) {
        val safeUrl = url.trim()
        if (!safeUrl.startsWith("https://")) return
        _localState.update { it.copy(userAvatarUrl = safeUrl) }
        viewModelScope.launch {
            cloudRepo.updateProfileFields(mapOf("avatarUrl" to safeUrl))
            showMessage("Profile photo uploaded via ImgBB & synced!")
        }
    }

    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            repository.updateAppState { it.copy(language = lang) }
            cloudRepo.updateProfileFields(mapOf("language" to lang))
            showMessage("Language set to $lang")
        }
    }

    fun toggleNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateAppState { it.copy(notificationsEnabled = enabled) }
            cloudRepo.updateProfileFields(mapOf("notificationsEnabled" to enabled))
        }
    }

    fun markAllNotificationsRead() {
        val unreadIds = uiState.value.notifications.filter { !it.isRead }.map { it.id }
        viewModelScope.launch {
            repository.markAllNotificationsRead()
            if (unreadIds.isNotEmpty()) cloudRepo.markAllNotificationsRead(unreadIds)
        }
    }

    // Partner Mode Actions
    fun toggleDriverOnline(online: Boolean) {
        viewModelScope.launch {
            repository.updateAppState { it.copy(driverOnline = online) }
            cloudRepo.updateProfileFields(mapOf("driverOnline" to online))
            showMessage(if (online) "You are now ONLINE & receiving trips" else "You are now OFFLINE")
        }
    }

    fun toggleDeliveryPartnerOnline(online: Boolean) {
        viewModelScope.launch {
            repository.updateAppState { it.copy(deliveryPartnerOnline = online) }
            cloudRepo.updateProfileFields(mapOf("deliveryPartnerOnline" to online))
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
            val newBalance = uiState.value.appState.walletBalance + completedEarnings
            viewModelScope.launch {
                repository.addWalletMoney(completedEarnings.toDouble(), "Captain Payout", "CREDIT", "Completed Trip Earnings")
                cloudRepo.updateProfileFields(mapOf("walletBalance" to newBalance))
                cloudRepo.recordWalletTransaction(
                    WalletTxEntity("TX_${System.currentTimeMillis()}", "Completed Trip Earnings", "CREDIT", completedEarnings.toDouble(), "Captain Payout", System.currentTimeMillis())
                )
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
            val newBalance = uiState.value.appState.walletBalance + completedEarnings
            viewModelScope.launch {
                repository.addWalletMoney(completedEarnings.toDouble(), "Delivery Payout", "CREDIT", "Food Delivery Earnings")
                cloudRepo.updateProfileFields(mapOf("walletBalance" to newBalance))
                cloudRepo.recordWalletTransaction(
                    WalletTxEntity("TX_${System.currentTimeMillis()}", "Food Delivery Earnings", "CREDIT", completedEarnings.toDouble(), "Delivery Payout", System.currentTimeMillis())
                )
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
            val newBalance = uiState.value.appState.walletBalance + completedEarnings
            viewModelScope.launch {
                repository.addWalletMoney(completedEarnings.toDouble(), "Logistics Payout", "CREDIT", "Parcel/Transport Earnings")
                cloudRepo.updateProfileFields(mapOf("walletBalance" to newBalance))
                cloudRepo.recordWalletTransaction(
                    WalletTxEntity("TX_${System.currentTimeMillis()}", "Parcel/Transport Earnings", "CREDIT", completedEarnings.toDouble(), "Logistics Payout", System.currentTimeMillis())
                )
            }
            triggerConfetti()
        }
    }

    // Restaurant Owner Actions (live synced to Firestore + ImgBB photo!)
    fun addOrUpdateMenuItem(
        id: String?,
        restaurantId: String,
        category: String,
        name: String,
        description: String,
        price: Int,
        isVeg: Boolean,
        imageUrl: String? = null
    ) {
        val itemId = id ?: "m_${UUID.randomUUID().toString().take(6)}"
        val item = MenuItemEntity(
            id = itemId,
            restaurantId = restaurantId,
            category = category.ifBlank { "Chef Specials" },
            name = name,
            description = description.ifBlank { "Freshly crafted signature recipe" },
            price = price.coerceAtLeast(25),
            isVeg = isVeg,
            isAvailable = true,
            isBestseller = false
        )
        if (!imageUrl.isNullOrBlank()) {
            _localState.update { it.copy(dishImageUrls = it.dishImageUrls + (itemId to imageUrl)) }
        }
        viewModelScope.launch {
            repository.upsertMenuItem(item)
            cloudRepo.upsertMenuItem(item, imageUrl)
            showMessage("Menu synced: $name (₹$price)")
        }
    }

    fun toggleMenuItemAvailability(id: String, available: Boolean) {
        val target = uiState.value.menuItems.find { it.id == id }
        viewModelScope.launch {
            repository.setMenuItemAvailable(id, available)
            if (target != null) cloudRepo.upsertMenuItem(target.copy(isAvailable = available), uiState.value.dishImageUrls[id])
        }
    }

    // Grocery Owner Actions (live synced to Firestore + ImgBB photo!)
    fun addOrUpdateGroceryProduct(
        id: String?,
        name: String,
        category: String,
        unit: String,
        mrp: Int,
        sellingPrice: Int,
        stock: Int,
        imageUrl: String? = null
    ) {
        val prodId = id ?: "g_${UUID.randomUUID().toString().take(6)}"
        val prod = GroceryProductEntity(
            id = prodId,
            name = name,
            category = category.ifBlank { "Daily Essentials" },
            unit = unit.ifBlank { "1 Pack" },
            mrp = mrp.coerceAtLeast(sellingPrice),
            sellingPrice = sellingPrice.coerceAtLeast(10),
            stockCount = stock.coerceAtLeast(0),
            deliveryMins = 9,
            isFeatured = true
        )
        if (!imageUrl.isNullOrBlank()) {
            _localState.update { it.copy(groceryImageUrls = it.groceryImageUrls + (prodId to imageUrl)) }
        }
        viewModelScope.launch {
            repository.upsertGroceryProduct(prod)
            cloudRepo.upsertGroceryProduct(prod, imageUrl)
            showMessage("Inventory synced: $name")
        }
    }

    fun adjustGroceryStock(id: String, stock: Int, price: Int) {
        val target = uiState.value.groceryProducts.find { it.id == id }
        viewModelScope.launch {
            repository.updateGroceryStockAndPrice(id, stock.coerceAtLeast(0), price.coerceAtLeast(10))
            if (target != null) {
                cloudRepo.upsertGroceryProduct(target.copy(stockCount = stock.coerceAtLeast(0), sellingPrice = price.coerceAtLeast(10)))
            }
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
        val newBalance = uiState.value.appState.walletBalance + order.amount
        viewModelScope.launch {
            repository.updateOrderStatus(order.id, "REFUNDED")
            cloudRepo.updateOrderStatus(order.id, "REFUNDED")
            repository.addWalletMoney(order.amount, "Admin Refund", "REFUND", "Refund for ${order.id}")
            cloudRepo.updateProfileFields(mapOf("walletBalance" to newBalance))
            cloudRepo.recordWalletTransaction(
                WalletTxEntity("TX_${System.currentTimeMillis()}", "Refund for ${order.id}", "REFUND", order.amount, "Admin Refund", System.currentTimeMillis())
            )
            showMessage("Refunded ₹${order.amount.toInt()} for ${order.id}")
        }
    }

    fun updateAdminConfig(transform: (AdminPlatformConfig) -> AdminPlatformConfig) {
        _localState.update { it.copy(adminConfig = transform(it.adminConfig)) }
    }

    fun sendAdminBroadcast(title: String, message: String) {
        viewModelScope.launch {
            repository.pushNotification("Promotional Offers", title, message)
            cloudRepo.pushNotification("N_${System.currentTimeMillis()}", "PROMO", title, message)
            showMessage("Broadcast synced to cloud")
        }
    }
}
