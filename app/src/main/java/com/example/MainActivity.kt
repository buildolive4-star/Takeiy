package com.example

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppPanel
import com.example.model.AppRoute
import com.example.ui.components.*
import com.example.ui.screens.admin.AdminPanelScreen
import com.example.ui.screens.customer.*
import com.example.ui.screens.partners.*
import com.example.ui.screens.shared.*
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.PremiumYellow
import com.example.ui.theme.ZiporaTheme
import com.example.viewmodel.SuperAppViewModel
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZiporaSuperAppRoot()
        }
    }
}

@Composable
fun ZiporaSuperAppRoot(viewModel: SuperAppViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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
        if (uiState.isSplashVisible) {
            AnimatedSplashScreen(onSkip = { viewModel.dismissSplash() })
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                ModalNavigationDrawer(
                    drawerState = drawerState,
                    gesturesEnabled = uiState.currentRoute == AppRoute.PanelHome,
                    drawerContent = {
                        UniversalPanelDrawerContent(
                            appState = uiState.appState,
                            activePanel = uiState.activePanel,
                            unreadNotifications = uiState.unreadNotificationCount,
                            onSelectPanel = { panel -> viewModel.requestPanelSwitch(panel) },
                            onNavigate = { route -> viewModel.navigateTo(route) },
                            onCloseDrawer = { scope.launch { drawerState.close() } },
                            onLogoutDemo = {
                                viewModel.revokeAdminAccess()
                                viewModel.showMessage("Session reset to Customer Mode")
                            }
                        )
                    }
                ) {
                    val openDrawer: () -> Unit = { scope.launch { drawerState.open() } }

                    AnimatedContent(
                        targetState = Pair(uiState.activePanel, uiState.currentRoute),
                        transitionSpec = { fadeIn() togetherWith fadeOut() },
                        label = "ziporaRouter"
                    ) { (panel, route) ->
                        when (route) {
                            AppRoute.PanelHome -> {
                                when (panel) {
                                    AppPanel.CUSTOMER -> CustomerHomeScreen(
                                        uiState = uiState,
                                        onOpenDrawer = openDrawer,
                                        onNavigate = viewModel::navigateTo,
                                        onSelectAddress = viewModel::selectAddress,
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
                                        onAddProduct = viewModel::addOrUpdateGroceryProduct
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
                                onBack = { viewModel.navigateBack() },
                                onRecordRide = { title, sub, amt, pay, pick, drop, otp, driver, onCreated ->
                                    viewModel.recordServiceOrder("RIDE", title, sub, amt, pay, pick, drop, otp, driver, "ON_THE_WAY", onCreated)
                                },
                                onCompleteRide = { id -> viewModel.updateOrderStatus(id, "COMPLETED") },
                                onRateRide = viewModel::rateOrder,
                                onShowMessage = viewModel::showMessage
                            )

                            AppRoute.FoodDiscovery -> FoodDiscoveryScreen(
                                uiState = uiState,
                                onBack = { viewModel.navigateBack() },
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
                                onBack = { viewModel.navigateBack() },
                                onAddToCart = viewModel::addToCart,
                                onUpdateQty = viewModel::updateCartQuantity,
                                onOpenCart = { viewModel.navigateTo(AppRoute.CartCheckout) }
                            )

                            AppRoute.ParcelDelivery -> ParcelDeliveryScreen(
                                uiState = uiState,
                                onBack = { viewModel.navigateBack() },
                                onBookParcel = { title, sub, amt, pay, pick, drop, otp, partner, onCreated ->
                                    viewModel.recordServiceOrder("PARCEL", title, sub, amt, pay, pick, drop, otp, partner, "ON_THE_WAY", onCreated)
                                },
                                onCompleteOrder = { id -> viewModel.updateOrderStatus(id, "COMPLETED") },
                                onShowMessage = viewModel::showMessage
                            )

                            AppRoute.GoodsTransport -> GoodsTransportScreen(
                                uiState = uiState,
                                onBack = { viewModel.navigateBack() },
                                onBookTransport = { title, sub, amt, pay, pick, drop, otp, partner, onCreated ->
                                    viewModel.recordServiceOrder("TRANSPORT", title, sub, amt, pay, pick, drop, otp, partner, "ON_THE_WAY", onCreated)
                                }
                            )

                            AppRoute.LocalShopping -> LocalShoppingScreen(
                                uiState = uiState,
                                onBack = { viewModel.navigateBack() },
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
                                }
                            )

                            AppRoute.Wallet -> WalletScreen(
                                uiState = uiState,
                                onBack = { viewModel.navigateBack() },
                                onTopUp = viewModel::topUpWallet
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
                                onToggleNotifications = viewModel::toggleNotificationsEnabled
                            )

                            is AppRoute.OrderHistory -> OrderHistoryScreen(
                                initialFilter = route.filterType,
                                uiState = uiState,
                                onBack = { viewModel.navigateBack() },
                                onRateOrder = viewModel::rateOrder
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
                                    viewModel.showMessage("Support ticket raised! Avg response time: 2 mins (Simulated)")
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
}
