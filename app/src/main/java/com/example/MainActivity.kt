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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.firebase.signOutFromTakeIt
import com.example.network.PaymentDeepLinkResult
import com.example.network.TakeItPaymentBus
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.admin.AdminCommandCenterRoot
import com.example.takeit.ui.auth.TakeItFoodAuthScreen
import com.example.takeit.ui.auth.TakeItFoodSplashScreen
import com.example.takeit.ui.customer.*
import com.example.takeit.ui.ride.RapidoRideAppRoot
import com.example.takeit.ui.transport.ParcelTransportAppRoot
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

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
        val supportedSchemes = setOf("takeit", "takeitride", "takeittransport", "takeitadmin")
        if (data.scheme in supportedSchemes && data.host == "payment-result") {
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
    val context = LocalContext.current
    val variant = remember(context) { TakeItAppVariant.fromContext(context) }
    var showSplash by remember { mutableStateOf(true) }
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }

    LaunchedEffect(Unit) {
        delay(1400)
        showSplash = false
    }

    DisposableEffect(Unit) {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            currentUser = auth.currentUser
        }
        Firebase.auth.addAuthStateListener(listener)
        onDispose { Firebase.auth.removeAuthStateListener(listener) }
    }

    val content: @Composable () -> Unit = {
        when {
            showSplash -> TakeItFoodSplashScreen(variant = variant)
            currentUser == null -> TakeItFoodAuthScreen(
                variant = variant,
                onAuthSuccess = { currentUser = Firebase.auth.currentUser }
            )
            else -> {
                val user = currentUser!!
                val uid = user.uid
                key(uid) {
                    AuthenticatedTakeItShell(
                        uid = uid,
                        displayName = user.displayName.orEmpty(),
                        email = user.email.orEmpty(),
                        viewModel = viewModel(key = uid)
                    )
                }
            }
        }
    }

    when (variant) {
        TakeItAppVariant.ADMIN -> TakeItAdminTheme(content = content)
        TakeItAppVariant.RIDE -> TakeItRideTheme(content = content)
        TakeItAppVariant.TRANSPORT -> TakeItTransportTheme(content = content)
        TakeItAppVariant.CUSTOMER -> TakeItCustomerTheme(content = content)
    }
}

@Composable
private fun AuthenticatedTakeItShell(
    uid: String,
    displayName: String,
    email: String,
    viewModel: TakeItViewModel
) {
    val context = LocalContext.current
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val scope = rememberCoroutineScope()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uid) {
        viewModel.attachSession(uid = uid, displayName = displayName, email = email)
    }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearMessage()
        }
    }

    val handleSignOut: () -> Unit = {
        signOutFromTakeIt(
            credentialManager = credentialManager,
            scope = scope,
            onBeforeAuthSignOut = { viewModel.detachSession() }
        )
    }

    when {
        uiState.isInAdminMode -> {
        BackHandler(enabled = uiState.appVariant == TakeItAppVariant.CUSTOMER) {
            viewModel.exitAdminMode()
        }
        TakeItAdminTheme {
            Box(modifier = Modifier.fillMaxSize()) {
                AdminCommandCenterRoot(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSignOut = handleSignOut
                )
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(12.dp)
                ) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = AdminGold,
                        contentColor = AdminBg
                    )
                }
            }
        }
        }
        uiState.appVariant == TakeItAppVariant.RIDE -> {
            TakeItRideTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.config.maintenanceMode && !uiState.isMainAdmin && uiState.adminRole != AdminRole.CO_ADMIN) {
                        CustomerMaintenanceScreen(config = uiState.config, onSignOut = handleSignOut)
                    } else {
                        RapidoRideAppRoot(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSignOut = handleSignOut
                        )
                    }
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 136.dp, start = 14.dp, end = 14.dp)
                    ) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = RideCharcoal,
                            contentColor = RideYellow
                        )
                    }
                }
            }
        }
        uiState.appVariant == TakeItAppVariant.TRANSPORT -> {
            TakeItTransportTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.config.maintenanceMode && !uiState.isMainAdmin && uiState.adminRole != AdminRole.CO_ADMIN) {
                        CustomerMaintenanceScreen(config = uiState.config, onSignOut = handleSignOut)
                    } else {
                        ParcelTransportAppRoot(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSignOut = handleSignOut
                        )
                    }
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 136.dp, start = 14.dp, end = 14.dp)
                    ) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = TransportNavy,
                            contentColor = Color.White
                        )
                    }
                }
            }
        }
        else -> {
            TakeItCustomerTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (uiState.config.maintenanceMode && !uiState.isMainAdmin && uiState.adminRole != AdminRole.CO_ADMIN) {
                        CustomerMaintenanceScreen(
                            config = uiState.config,
                            onSignOut = handleSignOut
                        )
                    } else {
                        CustomerAppRoot(
                            uiState = uiState,
                            viewModel = viewModel,
                            onSignOut = handleSignOut
                        )
                    }

                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 78.dp, start = 14.dp, end = 14.dp)
                    ) { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = TakeItInk,
                            contentColor = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomerAppRoot(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    BackHandler(enabled = uiState.customerBackStack.size > 1) {
        viewModel.navigateCustomerBack()
    }

    val showBottomBar = uiState.currentCustomerScreen == CustomerScreen.MainTabs

    Scaffold(
        containerColor = TakeItBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = Color.White,
                    tonalElevation = 8.dp
                ) {
                    CustomerTab.entries.forEach { tab ->
                        val selected = uiState.customerTab == tab
                        val icon = when (tab) {
                            CustomerTab.HOME -> Icons.Rounded.Home
                            CustomerTab.ORDERS -> Icons.Rounded.ReceiptLong
                            CustomerTab.OFFERS -> Icons.Rounded.LocalOffer
                            CustomerTab.ACCOUNT -> Icons.Rounded.Person
                        }
                        NavigationBarItem(
                            selected = selected,
                            onClick = { viewModel.selectCustomerTab(tab) },
                            icon = {
                                Icon(icon, contentDescription = tab.label)
                            },
                            label = {
                                Text(
                                    tab.label,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TakeItRed,
                                selectedTextColor = TakeItRed,
                                indicatorColor = Color(0xFFFFE4E6),
                                unselectedIconColor = TakeItMuted,
                                unselectedTextColor = TakeItMuted
                            ),
                            modifier = Modifier.testTag("customer_nav_${tab.name.lowercase()}")
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = uiState.currentCustomerScreen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "takeItCustomerNav"
            ) { screen ->
                when (screen) {
                    CustomerScreen.MainTabs -> {
                        when (uiState.customerTab) {
                            CustomerTab.HOME -> CustomerHomeTab(
                                uiState = uiState,
                                onSearchChange = viewModel::updateSearchQuery,
                                onOpenRestaurants = { cat ->
                                    viewModel.navigateCustomer(CustomerScreen.RestaurantsList(cat))
                                },
                                onOpenRestaurantDetail = { id ->
                                    viewModel.navigateCustomer(CustomerScreen.RestaurantDetail(id))
                                },
                                onToggleFavorite = viewModel::toggleFavoriteRestaurant,
                                onOpenAddresses = { viewModel.navigateCustomer(CustomerScreen.Addresses) },
                                onOpenCart = { viewModel.navigateCustomer(CustomerScreen.Cart) },
                                onOpenTrackOrder = { orderId ->
                                    viewModel.navigateCustomer(CustomerScreen.TrackOrder(orderId))
                                },
                                onOpenOffersTab = { viewModel.selectCustomerTab(CustomerTab.OFFERS) }
                            )
                            CustomerTab.ORDERS -> CustomerOrdersTab(
                                uiState = uiState,
                                onOpenTrack = { orderId ->
                                    viewModel.navigateCustomer(CustomerScreen.TrackOrder(orderId))
                                },
                                onOpenDelivered = { orderId ->
                                    viewModel.navigateCustomer(CustomerScreen.OrderDelivered(orderId))
                                }
                            )
                            CustomerTab.OFFERS -> CustomerOffersTab(
                                uiState = uiState,
                                onApplyCouponAndOpenCart = { coupon ->
                                    viewModel.applyCoupon(coupon)
                                    if (uiState.cart.isNotEmpty()) {
                                        viewModel.navigateCustomer(CustomerScreen.Cart)
                                    } else {
                                        viewModel.navigateCustomer(CustomerScreen.RestaurantsList("All"))
                                    }
                                }
                            )
                            CustomerTab.ACCOUNT -> CustomerAccountTab(
                                uiState = uiState,
                                onUpdateProfile = viewModel::updateCustomerPhoneAndName,
                                onOpenAddresses = { viewModel.navigateCustomer(CustomerScreen.Addresses) },
                                onOpenWallet = { viewModel.navigateCustomer(CustomerScreen.Wallet) },
                                onOpenSupport = { viewModel.navigateCustomer(CustomerScreen.Support) },
                                onOpenAbout = { viewModel.navigateCustomer(CustomerScreen.About) },
                                onOpenOrdersTab = { viewModel.selectCustomerTab(CustomerTab.ORDERS) },
                                onEnterAdminCommandCenter = { viewModel.enterAdminMode() },
                                onSignOut = onSignOut
                            )
                        }
                    }

                    is CustomerScreen.RestaurantsList -> RestaurantsListScreen(
                        initialCategory = screen.initialCategory,
                        uiState = uiState,
                        onBack = viewModel::navigateCustomerBack,
                        onOpenRestaurantDetail = { id ->
                            viewModel.navigateCustomer(CustomerScreen.RestaurantDetail(id))
                        },
                        onToggleFavorite = viewModel::toggleFavoriteRestaurant
                    )

                    is CustomerScreen.RestaurantDetail -> RestaurantDetailScreen(
                        restaurantId = screen.restaurantId,
                        uiState = uiState,
                        onBack = viewModel::navigateCustomerBack,
                        onAddToCart = viewModel::addToCart,
                        onUpdateCartQty = viewModel::updateCartQuantity,
                        onToggleFavorite = viewModel::toggleFavoriteRestaurant,
                        onOpenCart = { viewModel.navigateCustomer(CustomerScreen.Cart) }
                    )

                    CustomerScreen.Cart -> CustomerCartScreen(
                        uiState = uiState,
                        onBack = viewModel::navigateCustomerBack,
                        onUpdateQty = viewModel::updateCartQuantity,
                        onClearCart = viewModel::clearCart,
                        onApplyCoupon = viewModel::applyCoupon,
                        onRemoveCoupon = viewModel::removeCoupon,
                        onOpenAddresses = { viewModel.navigateCustomer(CustomerScreen.Addresses) },
                        onPlaceOrder = viewModel::placeFoodOrder,
                        onOrderCreated = { newOrderId ->
                            viewModel.resetCustomerHome()
                            viewModel.navigateCustomer(CustomerScreen.TrackOrder(newOrderId))
                        },
                        onShowMessage = viewModel::showMessage
                    )

                    is CustomerScreen.TrackOrder -> TrackOrderScreen(
                        orderId = screen.orderId,
                        uiState = uiState,
                        onBack = viewModel::navigateCustomerBack,
                        onCancelOrder = viewModel::cancelMyOrder,
                        onOrderDelivered = { deliveredId ->
                            viewModel.navigateCustomerBack()
                            viewModel.navigateCustomer(CustomerScreen.OrderDelivered(deliveredId))
                        }
                    )

                    is CustomerScreen.OrderDelivered -> OrderDeliveredCelebrationScreen(
                        orderId = screen.orderId,
                        uiState = uiState,
                        onSubmitReview = viewModel::submitOrderReview,
                        onGoHome = viewModel::resetCustomerHome
                    )

                    CustomerScreen.Addresses -> CustomerAddressesScreen(
                        uiState = uiState,
                        onBack = viewModel::navigateCustomerBack,
                        onSaveAddress = viewModel::saveAddress,
                        onSelectAddress = viewModel::selectActiveAddress,
                        onDeleteAddress = viewModel::deleteAddress
                    )

                    CustomerScreen.Wallet -> CustomerWalletScreen(
                        uiState = uiState,
                        onBack = viewModel::navigateCustomerBack,
                        onTopUpConfirmed = viewModel::recordWalletTopUp,
                        onShowMessage = viewModel::showMessage
                    )

                    CustomerScreen.Support -> CustomerSupportScreen(
                        uiState = uiState,
                        onBack = viewModel::navigateCustomerBack,
                        onSubmitTicket = viewModel::createSupportTicket
                    )

                    CustomerScreen.About -> CustomerAboutScreen(
                        uiState = uiState,
                        onBack = viewModel::navigateCustomerBack
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomerMaintenanceScreen(
    config: TakeItAppConfig,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TakeItBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            border = BorderStroke(1.dp, TakeItBorder),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_takeit_food),
                    contentDescription = "Take It Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(88.dp)
                        .clip(CircleShape)
                        .border(2.dp, TakeItRed, CircleShape)
                )
                Icon(Icons.Rounded.Engineering, null, tint = TakeItOrange, modifier = Modifier.size(40.dp))
                Text(
                    "Take It is Under Maintenance",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    "Our kitchens & delivery systems are getting an upgrade. We'll be back online shortly!\nSupport: ${config.supportPhone}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TakeItMuted,
                    textAlign = TextAlign.Center
                )
                OutlinedButton(
                    onClick = onSignOut,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Sign Out", color = TakeItRed, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
