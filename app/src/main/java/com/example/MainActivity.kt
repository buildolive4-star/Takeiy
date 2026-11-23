package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.TakeItAuthScreen
import com.example.firebase.signOutFromTakeIt
import com.example.model.AppPanel
import com.example.model.AppRoute
import com.example.model.TakeItBuildConfig
import com.example.network.PaymentDeepLinkResult
import com.example.network.TakeItPaymentBus
import com.example.ui.components.*
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.screens.customer.*
import com.example.ui.screens.partners.*
import com.example.ui.screens.shared.*
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PremiumYellow
import com.example.ui.theme.ZiporaTheme
import com.example.viewmodel.SuperAppViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handlePaymentDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            TakeItAppEntryPoint()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handlePaymentDeepLink(intent)
    }

    private fun handlePaymentDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "takeit" && data.host == "payment-result") {
            val status = data.getQueryParameter("status").orEmpty()
            val orderRef = data.getQueryParameter("orderRef").orEmpty()
            val paymentId = data.getQueryParameter("paymentId").orEmpty()
            val orderId = data.getQueryParameter("orderId").orEmpty()
            val amount = data.getQueryParameter("amount")?.toIntOrNull() ?: 0
            if (status.equals("success", ignoreCase = true) && orderRef.isNotBlank()) {
                TakeItPaymentBus.emitResult(
                    PaymentDeepLinkResult(
                        orderRef = orderRef,
                        paymentId = paymentId,
                        orderId = orderId,
                        amount = amount,
                        status = status
                    )
                )
            }
        }
    }
}

@Composable
fun TakeItAppEntryPoint() {
    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    LaunchedEffect(Unit) {
        delay(1500)
        showSplash = false
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    ZiporaTheme(darkTheme = false) {
        when {
            showSplash -> AnimatedSplashScreen(onSkip = { showSplash = false })
            currentUser == null -> TakeItAuthScreen(
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
            else -> {
                val user = currentUser!!
                val uid = user.uid
                // Strictly key authenticated subtree & ViewModel by UID so account switch disposes old listeners
                key(uid) {
                    AuthenticatedTakeItRoot(
                        uid = uid,
                        displayName = user.displayName.orEmpty(),
                        email = user.email.orEmpty(),
                        viewModel = viewModel(key = uid)
                    )
                }
            }
        }
    }
}

@Composable
private fun AuthenticatedTakeItRoot(
    uid: String,
    displayName: String,
    email: String,
    viewModel: SuperAppViewModel
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uid) {
        viewModel.attachCloudSession(uid = uid, displayName = displayName, email = email)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    // System Back Button handling for Drawer and nested routes
    BackHandler(enabled = drawerState.isOpen || uiState.backStack.size > 1) {
        if (drawerState.isOpen) {
            scope.launch { drawerState.close() }
        } else {
            viewModel.navigateBack()
        }
    }

    ZiporaTheme(darkTheme = uiState.activePanel.isDarkTheme) {
        Box(modifier = Modifier.fillMaxSize()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                gesturesEnabled = uiState.backStack.size <= 1,
                drawerContent = {
                    UniversalPanelDrawerContent(
                        appState = uiState.appState,
                        activePanel = uiState.activePanel,
                        unreadNotifications = uiState.unreadNotificationCount,
                        onSelectPanel = { panel -> viewModel.requestPanelSwitch(panel) },
                        onNavigate = { route -> viewModel.navigateTo(route) },
                        onCloseDrawer = { scope.launch { drawerState.close() } },
                        onLogoutDemo = {
                            signOutFromTakeIt(
                                credentialManager = credentialManager,
                                scope = scope,
                                onBeforeAuthSignOut = { viewModel.detachCloudSession() }
                            )
                        }
                    )
                }
            ) {
                val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

                AnimatedContent(
                    targetState = Pair(uiState.activePanel, uiState.currentRoute),
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "takeItRouter"
                ) { (panel, route) ->
                    when (route) {
                        AppRoute.PanelHome -> {
                            when (panel) {
                                AppPanel.CUSTOMER -> CustomerHomeScreen(
                                    uiState = uiState,
                                    onOpenDrawer = openDrawer,
                                    onNavigate = viewModel::navigateTo,
                                    onSelectAddress = viewModel::selectAddress,
                                    onSaveGpsLocation = viewModel::saveGpsLocation,
                                    onAddSearch = viewModel::addSearchQuery,
                                    onToggleFavourite = viewModel::toggleFavouriteRestaurant,
                                    onShowMessage = viewModel::showMessage
                                )
                                AppPanel.DRIVER_CAPTAIN -> DriverCaptainPanelScreen(
                                    uiState = uiState,
                                    onOpenDrawer = openDrawer,
                                    onToggleOnline = viewModel::toggleDriverOnline,
                                    onToggleService = viewModel::toggleDriverService,
                                    onNextRequest = viewModel::nextDriverRequest,
                                    onNavigate = viewModel::navigateTo,
                                    onShowMessage = viewModel::showMessage
                                )
                                AppPanel.FOOD_DELIVERY_PARTNER -> FoodDeliveryPartnerPanelScreen(
                                    uiState = uiState,
                                    onOpenDrawer = openDrawer,
                                    onToggleOnline = viewModel::toggleDeliveryPartnerOnline,
                                    onNextDelivery = viewModel::nextFoodDeliveryJob,
                                    onShowMessage = viewModel::showMessage
                                )
                                AppPanel.RESTAURANT_OWNER -> RestaurantOwnerPanelScreen(
                                    uiState = uiState,
                                    onOpenDrawer = openDrawer,
                                    onUpdateOrderStatus = viewModel::updateOrderStatus,
                                    onAddOrUpdateDish = viewModel::addOrUpdateMenuItem,
                                    onToggleDishAvailable = viewModel::toggleMenuItemAvailability,
                                    onShowMessage = viewModel::showMessage
                                )
                                AppPanel.GROCERY_OWNER -> GroceryOwnerPanelScreen(
                                    uiState = uiState,
                                    onOpenDrawer = openDrawer,
                                    onUpdateOrderStatus = viewModel::updateOrderStatus,
                                    onAdjustStock = viewModel::adjustGroceryStock,
                                    onAddProduct = viewModel::addOrUpdateGroceryProduct,
                                    onShowMessage = viewModel::showMessage
                                )
                                AppPanel.PARCEL_PARTNER, AppPanel.TRANSPORT_PARTNER -> ParcelTransportPartnerPanelScreen(
                                    panel = panel,
                                    uiState = uiState,
                                    onOpenDrawer = openDrawer,
                                    onNextJob = viewModel::nextParcelJob,
                                    onShowMessage = viewModel::showMessage
                                )
                                AppPanel.BUSINESS_OWNER -> BusinessOwnerPanelScreen(
                                    uiState = uiState,
                                    onOpenDrawer = openDrawer,
                                    onNavigate = viewModel::navigateTo,
                                    onShowMessage = viewModel::showMessage
                                )
                                AppPanel.ADMIN -> AdminPanelScreen(
                                    uiState = uiState,
                                    onOpenDrawer = openDrawer,
                                    onLockAdmin = viewModel::revokeAdminAccess,
                                    onToggleVerifyUser = viewModel::toggleUserVerification,
                                    onToggleSuspendUser = viewModel::toggleUserSuspension,
                                    onRefundOrder = viewModel::refundOrder,
                                    onCompleteOrder = { id -> viewModel.updateOrderStatus(id, "COMPLETED") },
                                    onUpdateConfig = viewModel::updateAdminConfig,
                                    onSendBroadcast = viewModel::sendAdminBroadcast
                                )
                            }
                        }

                        AppRoute.RideBooking -> RideBookingScreen(
                            uiState = uiState,
                            onBack = { if (uiState.backStack.size <= 1) openDrawer() else viewModel.navigateBack() },
                            onRecordRide = { title, sub, amt, pay, pick, drop, otp, driver, onCreated ->
                                viewModel.recordServiceOrder("RIDE", title, sub, amt, pay, pick, drop, otp, driver, "ON_THE_WAY", onCreated)
                            },
                            onCompleteRide = { id -> viewModel.updateOrderStatus(id, "COMPLETED") },
                            onRateRide = viewModel::rateOrder,
                            onShowMessage = viewModel::showMessage
                        )

                        AppRoute.FoodDiscovery -> FoodDiscoveryScreen(
                            uiState = uiState,
                            onBack = { if (uiState.backStack.size <= 1) openDrawer() else viewModel.navigateBack() },
                            onOpenRestaurant = { id -> viewModel.navigateTo(AppRoute.RestaurantDetail(id)) },
                            onToggleFavourite = viewModel::toggleFavouriteRestaurant,
                            onOpenCart = { viewModel.navigateTo(AppRoute.CartCheckout) }
                        )

                        is AppRoute.RestaurantDetail -> RestaurantDetailScreen(
                            restaurantId = route.restaurantId,
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onAddToCart = viewModel::addToCart,
                            onUpdateCartQty = viewModel::updateCartQuantity,
                            onToggleFavourite = viewModel::toggleFavouriteRestaurant,
                            onOpenCart = { viewModel.navigateTo(AppRoute.CartCheckout) }
                        )

                        AppRoute.GroceryStore -> GroceryScreen(
                            uiState = uiState,
                            onBack = { if (uiState.backStack.size <= 1) openDrawer() else viewModel.navigateBack() },
                            onAddToCart = viewModel::addToCart,
                            onUpdateQty = viewModel::updateCartQuantity,
                            onOpenCart = { viewModel.navigateTo(AppRoute.CartCheckout) }
                        )

                        AppRoute.ParcelDelivery -> ParcelDeliveryScreen(
                            uiState = uiState,
                            onBack = { if (uiState.backStack.size <= 1) openDrawer() else viewModel.navigateBack() },
                            onBookParcel = { title, sub, amt, pay, pick, drop, otp, partner, onCreated ->
                                viewModel.recordServiceOrder("PARCEL", title, sub, amt, pay, pick, drop, otp, partner, "ON_THE_WAY", onCreated)
                            },
                            onCompleteOrder = { id -> viewModel.updateOrderStatus(id, "COMPLETED") },
                            onShowMessage = viewModel::showMessage
                        )

                        AppRoute.GoodsTransport -> GoodsTransportScreen(
                            uiState = uiState,
                            onBack = { if (uiState.backStack.size <= 1) openDrawer() else viewModel.navigateBack() },
                            onBookTransport = { title, sub, amt, pay, pick, drop, otp, partner, onCreated ->
                                viewModel.recordServiceOrder("TRANSPORT", title, sub, amt, pay, pick, drop, otp, partner, "ON_THE_WAY", onCreated)
                            }
                        )

                        AppRoute.LocalShopping -> LocalShoppingScreen(
                            uiState = uiState,
                            onBack = { if (uiState.backStack.size <= 1) openDrawer() else viewModel.navigateBack() },
                            onAddToCart = viewModel::addToCart,
                            onOpenCart = { viewModel.navigateTo(AppRoute.CartCheckout) }
                        )

                        AppRoute.CartCheckout -> CartCheckoutScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onUpdateQty = viewModel::updateCartQuantity,
                            onApplyCoupon = viewModel::applyCoupon,
                            onPlaceOrder = viewModel::placeCartOrder,
                            onOrderPlacedSuccess = {
                                viewModel.resetToHome()
                                viewModel.navigateTo(AppRoute.OrderHistory("ALL"))
                            },
                            onShowMessage = viewModel::showMessage
                        )

                        AppRoute.Wallet -> WalletScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onTopUp = viewModel::topUpWallet,
                            onShowMessage = viewModel::showMessage
                        )

                        AppRoute.Notifications -> NotificationsScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onMarkAllRead = viewModel::markAllNotificationsRead
                        )

                        AppRoute.Profile -> ProfileScreen(
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onSaveProfile = viewModel::updateProfile,
                            onSelectLanguage = viewModel::updateLanguage,
                            onToggleNotifications = viewModel::toggleNotificationsEnabled,
                            onAvatarUploaded = viewModel::updateProfileAvatarUrl,
                            onShowMessage = viewModel::showMessage
                        )

                        is AppRoute.OrderHistory -> OrderHistoryScreen(
                            initialFilter = route.filterType,
                            uiState = uiState,
                            onBack = { viewModel.navigateBack() },
                            onRateOrder = viewModel::rateOrder,
                            onRepeatOrder = { type ->
                                val target = when (type.uppercase()) {
                                    "RIDE" -> AppRoute.RideBooking
                                    "FOOD" -> AppRoute.FoodDiscovery
                                    "GROCERY" -> AppRoute.GroceryStore
                                    "PARCEL" -> AppRoute.ParcelDelivery
                                    "TRANSPORT" -> AppRoute.GoodsTransport
                                    else -> AppRoute.LocalShopping
                                }
                                viewModel.navigateTo(target)
                            }
                        )

                        AppRoute.CouponsOffers -> CouponsOffersScreen(
                            onBack = { viewModel.navigateBack() },
                            onApplyCoupon = { c ->
                                viewModel.applyCoupon(c)
                                viewModel.navigateBack()
                            }
                        )

                        AppRoute.ReferEarn -> ReferEarnScreen(
                            onBack = { viewModel.navigateBack() },
                            onClaimSimulatedBonus = {
                                viewModel.topUpWallet(250.0, "Referral Bonus", false)
                            }
                        )

                        AppRoute.Support -> CustomerSupportScreen(
                            onBack = { viewModel.navigateBack() },
                            onSubmitTicket = {
                                viewModel.showMessage("Support ticket raised & logged to cloud!")
                            }
                        )

                        is AppRoute.InfoPage -> InfoPageScreen(
                            title = route.title,
                            body = route.body,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                }
            }

            // Global Snackbar
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(12.dp)
            ) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = DeepNavy,
                    contentColor = PremiumYellow
                )
            }

            // Confetti Overlay
            ConfettiCelebrationOverlay(visible = uiState.showConfetti)

            // Admin Authorization Gate
            if (uiState.showAdminGate) {
                AdminAuthorizationDialog(
                    onDismiss = viewModel::dismissAdminGate,
                    onVerify = viewModel::verifyAdminPasscode
                )
            }
        }
    }
}
