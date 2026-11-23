package com.example.takeit.ui.ride

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.network.TakeItPaymentBus
import com.example.network.TakeItPaymentGateway
import com.example.takeit.model.RideBooking
import com.example.takeit.model.RideTab
import com.example.takeit.theme.*
import com.example.takeit.ui.components.Nav3DTabItem
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DFloatingNavBar
import com.example.takeit.ui.components.TakeIt3DHeroBanner
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.ui.customer.CustomerWalletScreen
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel
import kotlinx.coroutines.launch

private data class RideVehicleOption(
    val code: String,
    val title: String,
    val subtitle: String,
    val etaMin: Int,
    val baseFare: Int,
    val perKm: Int,
    val icon: ImageVector
) {
    fun fareFor(distanceKm: Int): Int = baseFare + (distanceKm.coerceAtLeast(1) * perKm)
}

private val rideVehicles = listOf(
    RideVehicleOption("BIKE", "Bike Moto", "1 rider • Beats traffic", 3, 25, 7, Icons.Rounded.TwoWheeler),
    RideVehicleOption("AUTO", "Auto Rickshaw", "Up to 3 • Doorstep", 4, 40, 11, Icons.Rounded.ElectricRickshaw),
    RideVehicleOption("MINI_CAB", "Mini Cab", "4 seats • AC Hatch", 6, 65, 15, Icons.Rounded.DirectionsCar),
    RideVehicleOption("PRIME_SEDAN", "Prime Sedan", "4 seats • Spacious AC", 7, 95, 20, Icons.Rounded.LocalTaxi)
)

@Composable
fun RapidoRideAppRoot(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var pickup by remember(uiState.profile.activeAddressLine) {
        mutableStateOf(uiState.profile.activeAddressLine.ifBlank { "Sector 62 Metro, Noida" })
    }
    var drop by remember { mutableStateOf("Pari Chowk Metro, Greater Noida") }
    var distanceKm by remember { mutableIntStateOf(6) }
    var selectedVehicleCode by remember { mutableStateOf("BIKE") }
    var paymentMethod by remember { mutableStateOf("RAZORPAY") }
    var pendingPayRef by remember { mutableStateOf<String?>(null) }
    var pendingRideDraft by remember { mutableStateOf<Triple<String, Int, Int>?>(null) }

    val selectedVehicle = remember(selectedVehicleCode) {
        rideVehicles.firstOrNull { it.code == selectedVehicleCode } ?: rideVehicles.first()
    }
    val calculatedFare = remember(selectedVehicle, distanceKm) {
        selectedVehicle.fareFor(distanceKm)
    }

    fun finalizePendingRazorpayRide(paymentId: String, amount: Int) {
        val draft = pendingRideDraft
        pendingPayRef = null
        pendingRideDraft = null
        viewModel.bookRide(
            vehicleType = draft?.first ?: selectedVehicle.code,
            pickup = pickup,
            drop = drop,
            distanceKm = draft?.second ?: distanceKm,
            fare = if (amount > 0) amount else (draft?.third ?: calculatedFare),
            paymentMethod = "RAZORPAY",
            paymentStatus = "PAID",
            paymentRef = paymentId,
            onBooked = { viewModel.selectRideTab(RideTab.BOOK) }
        )
    }

    LaunchedEffect(Unit) {
        TakeItPaymentBus.results.collect { res ->
            if (res.orderRef == pendingPayRef && res.status.equals("success", ignoreCase = true)) {
                finalizePendingRazorpayRide(res.paymentId, res.amount)
            }
        }
    }

    DisposableEffect(lifecycleOwner, pendingPayRef) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingPayRef != null) {
                val ref = pendingPayRef ?: return@LifecycleEventObserver
                scope.launch {
                    val res = TakeItPaymentGateway.checkPaymentStatus(ref)
                    if (res != null) {
                        finalizePendingRazorpayRide(res.paymentId, res.amount)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val startBooking: () -> Unit = {
        if (pickup.isBlank() || drop.isBlank()) {
            viewModel.showMessage("Enter both pickup & drop locations")
        } else {
            when (paymentMethod) {
                "RAZORPAY" -> {
                    val ref = "TKRD_${System.currentTimeMillis()}"
                    pendingPayRef = ref
                    pendingRideDraft = Triple(selectedVehicle.code, distanceKm, calculatedFare)
                    val launched = TakeItPaymentGateway.launchChromePayment(
                        context = context,
                        orderRef = ref,
                        amount = calculatedFare,
                        serviceType = "RAPIDO_RIDE",
                        itemTitle = "${selectedVehicle.title} ($distanceKm km)",
                        customerName = uiState.profile.name.ifBlank { uiState.displayName },
                        customerPhone = uiState.profile.phone,
                        userId = uiState.uid
                    )
                    if (!launched) {
                        pendingPayRef = null
                        viewModel.showMessage("Could not open browser for Razorpay checkout")
                    }
                }
                "WALLET" -> {
                    viewModel.bookRide(
                        vehicleType = selectedVehicle.code,
                        pickup = pickup,
                        drop = drop,
                        distanceKm = distanceKm,
                        fare = calculatedFare,
                        paymentMethod = "WALLET",
                        paymentStatus = "PAID",
                        paymentRef = "WALLET"
                    )
                }
                else -> {
                    viewModel.bookRide(
                        vehicleType = selectedVehicle.code,
                        pickup = pickup,
                        drop = drop,
                        distanceKm = distanceKm,
                        fare = calculatedFare,
                        paymentMethod = "CASH",
                        paymentStatus = "PAY_ON_DROP",
                        paymentRef = "CASH"
                    )
                }
            }
        }
    }

    val navItems = remember(uiState.rideTab) {
        RideTab.entries.map { tab ->
            val icon = when (tab) {
                RideTab.BOOK -> Icons.Rounded.TwoWheeler
                RideTab.TRIPS -> Icons.Rounded.Route
                RideTab.WALLET -> Icons.Rounded.AccountBalanceWallet
                RideTab.ACCOUNT -> Icons.Rounded.Person
            }
            Nav3DTabItem(
                id = tab.name,
                label = tab.label,
                icon = icon,
                selected = uiState.rideTab == tab,
                testTag = "ride_nav_${tab.name.lowercase()}",
                onClick = { viewModel.selectRideTab(tab) }
            )
        }
    }

    Scaffold(
        containerColor = TakeItBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.rideTab == RideTab.BOOK) {
                    Surface(
                        color = Color.White,
                        shadowElevation = 18.dp,
                        border = BorderStroke(1.dp, TakeItBorder),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${selectedVehicle.title} • $distanceKm km",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TakeItMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "₹$calculatedFare • $paymentMethod",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = RideCharcoal
                                )
                            }
                            TakeIt3DButton(
                                onClick = startBooking,
                                containerColor = RideYellow,
                                depthColor = RideYellow.darken3D(0.42f),
                                contentColor = RideCharcoal,
                                cornerRadius = 16.dp,
                                depth = 5.dp,
                                height = 50.dp,
                                modifier = Modifier
                                    .widthIn(min = 148.dp)
                                    .testTag("ride_sticky_pay_button")
                            ) {
                                Text(
                                    if (paymentMethod == "CASH") "Book ₹$calculatedFare" else "Pay ₹$calculatedFare",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                TakeIt3DFloatingNavBar(
                    items = navItems,
                    containerColor = Color.White,
                    activePodColor = RideYellow,
                    activeContentColor = RideCharcoal,
                    inactiveColor = TakeItMuted,
                    borderColor = TakeItBorder
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (uiState.rideTab) {
                RideTab.BOOK -> RideBookingTabContent(
                    uiState = uiState,
                    pickup = pickup,
                    drop = drop,
                    distanceKm = distanceKm,
                    selectedVehicle = selectedVehicle,
                    calculatedFare = calculatedFare,
                    paymentMethod = paymentMethod,
                    pendingPayRef = pendingPayRef,
                    onPickupChange = { pickup = it },
                    onDropChange = { drop = it },
                    onSwapLocations = {
                        val oldPickup = pickup
                        pickup = drop
                        drop = oldPickup
                    },
                    onDistanceChange = { distanceKm = it },
                    onSelectVehicle = { selectedVehicleCode = it },
                    onSelectPayment = { paymentMethod = it },
                    onVerifyPendingPayment = {
                        val ref = pendingPayRef ?: return@RideBookingTabContent
                        scope.launch {
                            val res = TakeItPaymentGateway.checkPaymentStatus(ref)
                            if (res != null) {
                                finalizePendingRazorpayRide(res.paymentId, res.amount)
                            } else {
                                viewModel.showMessage("Payment not confirmed yet. Complete checkout in Chrome first.")
                            }
                        }
                    },
                    onCancelRide = viewModel::cancelMyRide
                )

                RideTab.TRIPS -> RideHistoryTab(
                    rides = uiState.myRides,
                    onRateRide = viewModel::rateMyRide,
                    onCancelRide = viewModel::cancelMyRide
                )

                RideTab.WALLET -> CustomerWalletScreen(
                    uiState = uiState,
                    onBack = { viewModel.selectRideTab(RideTab.BOOK) },
                    onTopUpConfirmed = viewModel::recordWalletTopUp,
                    onShowMessage = viewModel::showMessage
                )

                RideTab.ACCOUNT -> RideAccountTab(
                    uiState = uiState,
                    onUpdateProfile = viewModel::updateCustomerPhoneAndName,
                    onOpenWallet = { viewModel.selectRideTab(RideTab.WALLET) },
                    onOpenRides = { viewModel.selectRideTab(RideTab.TRIPS) },
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun RideBookingTabContent(
    uiState: TakeItUiState,
    pickup: String,
    drop: String,
    distanceKm: Int,
    selectedVehicle: RideVehicleOption,
    calculatedFare: Int,
    paymentMethod: String,
    pendingPayRef: String?,
    onPickupChange: (String) -> Unit,
    onDropChange: (String) -> Unit,
    onSwapLocations: () -> Unit,
    onDistanceChange: (Int) -> Unit,
    onSelectVehicle: (String) -> Unit,
    onSelectPayment: (String) -> Unit,
    onVerifyPendingPayment: () -> Unit,
    onCancelRide: (String) -> Unit
) {
    val activeRide = remember(uiState.myRides) {
        uiState.myRides.firstOrNull {
            it.status in listOf("REQUESTED", "CAPTAIN_ASSIGNED", "ARRIVING", "ON_TRIP")
        }
    }

    val inf = rememberInfiniteTransition(label = "rideWelcomeAnim")
    val heroScale by inf.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rideHeroScale"
    )
    val heroFloat by inf.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "rideHeroFloat"
    )

    TakeIt3DBackdrop(
        accentPrimary = RideYellow,
        accentSecondary = RideBlue,
        baseColor = TakeItBg
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 3D Charcoal & Rapido Yellow Header Deck
            item {
                TakeIt3DCard(
                    containerColor = RideCharcoal,
                    depthColor = Color.Black,
                    borderColor = RideYellow.copy(alpha = 0.45f),
                    cornerRadius = 26.dp,
                    depth = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(RideCharcoal, Color(0xFF1E293B))))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .shadow(8.dp, CircleShape, ambientColor = RideYellow, spotColor = RideYellow)
                                        .clip(CircleShape)
                                        .background(RideYellow)
                                        .padding(2.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.logo_takeit_ride),
                                        contentDescription = "Take It Ride Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                                Column {
                                    Text(
                                        "Take It Ride",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = RideYellow,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        "Bike • Auto • Cab in ${uiState.config.cityName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.85f)
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = RideYellow.copy(alpha = 0.16f),
                                border = BorderStroke(1.dp, RideYellow)
                            ) {
                                Text(
                                    "₹${uiState.profile.walletBalance.toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = RideYellow,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3D Perspective 2nd Image Welcome Showcase Banner
            item {
                TakeIt3DHeroBanner(
                    borderColor = RideYellow,
                    depthColor = RideCharcoal,
                    cornerRadius = 26.dp,
                    depth = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(178.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.welcome_rapido_ride),
                        contentDescription = "Take It Ride Welcome",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = heroScale
                                scaleY = heroScale
                                translationY = heroFloat
                            }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(Color.Black.copy(alpha = 0.74f), Color.Transparent)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = RideYellow, shadowElevation = 4.dp) {
                            Text(
                                "RAPIDO SPEED",
                                style = MaterialTheme.typography.labelSmall,
                                color = RideCharcoal,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            "Zero Surge\nFastest Pickups",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Bike ₹25+ • Live Captain OTP",
                            style = MaterialTheme.typography.labelSmall,
                            color = RideYellowSoft
                        )
                    }
                }
            }

            if (pendingPayRef != null) {
                item {
                    TakeIt3DCard(
                        containerColor = RideYellowSoft,
                        depthColor = RideYellow.darken3D(0.35f),
                        borderColor = RideCharcoal,
                        cornerRadius = 20.dp,
                        depth = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Waiting for Razorpay Confirmation", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                                Text("Completed payment in Chrome? Tap Verify now.", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                            Button(
                                onClick = onVerifyPendingPayment,
                                colors = ButtonDefaults.buttonColors(containerColor = RideCharcoal, contentColor = RideYellow),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Verify", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            if (activeRide != null) {
                item {
                    ActiveRideTrackingCard(
                        ride = activeRide,
                        onCancel = { onCancelRide(activeRide.id) }
                    )
                }
            }

            // 3D Pickup, Drop & Isometric Route Map Card
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Where are you going?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.SwapVert,
                                containerColor = RideBlue,
                                size = 38.dp,
                                contentDescription = "Swap Pickup & Drop",
                                onClick = onSwapLocations
                            )
                        }

                        LiveRideRouteCanvas(distanceKm = distanceKm)

                        OutlinedTextField(
                            value = pickup,
                            onValueChange = onPickupChange,
                            label = { Text("Pickup Location") },
                            leadingIcon = { Icon(Icons.Rounded.MyLocation, null, tint = TakeItGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ride_pickup_input")
                        )
                        OutlinedTextField(
                            value = drop,
                            onValueChange = onDropChange,
                            label = { Text("Drop Location") },
                            leadingIcon = { Icon(Icons.Rounded.Place, null, tint = TakeItRed) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("ride_drop_input")
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(
                                listOf(
                                    "Pari Chowk Metro",
                                    "Alpha 1 Market",
                                    "Knowledge Park III",
                                    "Sector 62 Noida",
                                    "Botanical Garden"
                                )
                            ) { spot ->
                                AssistChip(
                                    onClick = { onDropChange(spot) },
                                    label = { Text(spot, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }

                        Text("Estimated Distance: $distanceKm km", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf(2, 4, 6, 10, 15, 22)) { km ->
                                FilterChip(
                                    selected = distanceKm == km,
                                    onClick = { onDistanceChange(km) },
                                    label = { Text("$km km", fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RideYellow,
                                        selectedLabelColor = RideCharcoal
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Choose Your Ride",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(rideVehicles, key = { it.code }) { v ->
                val selected = v.code == selectedVehicle.code
                val fare = v.fareFor(distanceKm)
                TakeIt3DCard(
                    onClick = { onSelectVehicle(v.code) },
                    containerColor = if (selected) RideYellowSoft else Color.White,
                    depthColor = if (selected) RideYellow.darken3D(0.4f) else Color(0xFFD5DCE6),
                    borderColor = if (selected) RideCharcoal else Color.White,
                    cornerRadius = 20.dp,
                    depth = if (selected) 6.dp else 4.dp,
                    shadowElevation = if (selected) 10.dp else 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("ride_vehicle_${v.code.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TakeIt3DIconOrb(
                            icon = v.icon,
                            containerColor = if (selected) RideYellow else RideCharcoal,
                            depthColor = if (selected) RideYellow.darken3D(0.35f) else Color.Black,
                            iconTint = if (selected) RideCharcoal else RideYellow,
                            size = 50.dp,
                            shape = RoundedCornerShape(15.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(v.title, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                                Surface(shape = RoundedCornerShape(50), color = TakeItGreenSoft) {
                                    Text(
                                        "${v.etaMin} min away",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TakeItGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(v.subtitle, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                            Text("₹${v.baseFare} base + ₹${v.perKm}/km", style = MaterialTheme.typography.labelSmall, color = RideBlue)
                        }
                        Text(
                            "₹$fare",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = RideCharcoal
                        )
                    }
                }
            }

            // 3D Payment Method Card
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Payment Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("RAZORPAY" to "Razorpay Online", "WALLET" to "Wallet (₹${uiState.profile.walletBalance.toInt()})", "CASH" to "Cash").forEach { (key, label) ->
                                FilterChip(
                                    selected = paymentMethod == key,
                                    onClick = { onSelectPayment(key) },
                                    label = { Text(label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = RideCharcoal,
                                        selectedLabelColor = RideYellow
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            "Total payable: ₹$calculatedFare • Tap the yellow 3D Pay/Book bar below to confirm ride.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TakeItMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRideRouteCanvas(distanceKm: Int) {
    val inf = rememberInfiniteTransition(label = "routePulse")
    val t by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "pulseT"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(98.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF1F5F9), Color(0xFFE2E8F0))))
            .border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 18.dp)) {
            val start = Offset(0f, size.height * 0.72f)
            val end = Offset(size.width, size.height * 0.28f)
            val ctrl = Offset(size.width * 0.45f, -size.height * 0.15f)

            val path = Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(ctrl.x, ctrl.y, end.x, end.y)
            }
            // 3D Road Drop Shadow
            drawPath(path = path, color = Color.Black.copy(alpha = 0.14f), style = Stroke(width = 12f, cap = StrokeCap.Round))
            drawPath(path = path, color = RideCharcoal, style = Stroke(width = 8f, cap = StrokeCap.Round))
            drawPath(path = path, color = RideYellow, style = Stroke(width = 4f, cap = StrokeCap.Round))

            val oneMinusT = 1f - t
            val cx = oneMinusT * oneMinusT * start.x + 2f * oneMinusT * t * ctrl.x + t * t * end.x
            val cy = oneMinusT * oneMinusT * start.y + 2f * oneMinusT * t * ctrl.y + t * t * end.y
            drawCircle(color = RideYellow.copy(alpha = 0.3f), radius = 24f, center = Offset(cx, cy))
            drawCircle(color = RideCharcoal, radius = 15f, center = Offset(cx, cy))
            drawCircle(color = RideYellow, radius = 8f, center = Offset(cx, cy))
            drawCircle(color = Color.Black.copy(alpha = 0.22f), radius = 12f, center = Offset(start.x, start.y + 5f))
            drawCircle(color = TakeItGreen, radius = 12f, center = start)
            drawCircle(color = Color.Black.copy(alpha = 0.22f), radius = 12f, center = Offset(end.x, end.y + 5f))
            drawCircle(color = TakeItRed, radius = 12f, center = end)
        }
        Surface(
            shape = RoundedCornerShape(50),
            color = RideCharcoal,
            shadowElevation = 4.dp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(8.dp)
        ) {
            Text(
                "$distanceKm km • ~${(distanceKm * 2 + 2)} min",
                style = MaterialTheme.typography.labelSmall,
                color = RideYellow,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun ActiveRideTrackingCard(
    ride: RideBooking,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    TakeIt3DCard(
        containerColor = RideCharcoal,
        depthColor = Color.Black,
        borderColor = RideYellow,
        cornerRadius = 22.dp,
        depth = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("LIVE RIDE • ${ride.rideNumber}", color = RideYellow, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Text(
                        ride.status.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Surface(shape = RoundedCornerShape(10.dp), color = RideYellow, shadowElevation = 4.dp) {
                    Text(
                        "OTP ${ride.otp}",
                        color = RideCharcoal,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
            Text("${ride.pickupAddress} -> ${ride.dropAddress}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.08f)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(ride.captainName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            if (ride.vehicleNumber.isNotBlank()) "${ride.vehicleType} • ${ride.vehicleNumber} • ETA ${ride.etaMinutes} min"
                            else "${ride.vehicleType} • ₹${ride.totalFare} (${ride.paymentMethod})",
                            style = MaterialTheme.typography.labelSmall,
                            color = RideYellow
                        )
                    }
                    if (ride.captainPhone.isNotBlank()) {
                        TakeIt3DIconOrb(
                            icon = Icons.Rounded.Call,
                            containerColor = RideYellow,
                            iconTint = RideCharcoal,
                            size = 40.dp,
                            contentDescription = "Call Captain",
                            onClick = {
                                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${ride.captainPhone}"))
                                context.startActivity(dial)
                            }
                        )
                    }
                }
            }
            if (ride.status == "REQUESTED") {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, AdminCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Ride Request", color = AdminCrimson, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun RideHistoryTab(
    rides: List<RideBooking>,
    onRateRide: (String, Int) -> Unit,
    onCancelRide: (String) -> Unit
) {
    TakeIt3DBackdrop(RideYellow, RideCharcoal, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TakeIt3DCard(
                    containerColor = RideCharcoal,
                    depthColor = Color.Black,
                    borderColor = RideYellow.copy(alpha = 0.45f),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Rides", style = MaterialTheme.typography.headlineSmall, color = RideYellow, fontWeight = FontWeight.ExtraBold)
                        Surface(shape = RoundedCornerShape(50), color = RideYellow) {
                            Text(
                                "${rides.size} Trips",
                                color = RideCharcoal,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
            if (rides.isEmpty()) {
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 24.dp,
                        depth = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.TwoWheeler,
                                containerColor = RideCharcoal,
                                iconTint = RideYellow,
                                size = 64.dp
                            )
                            Text("No rides booked yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Book your first Bike, Auto, or Cab ride in Book Ride tab!", color = TakeItMuted)
                        }
                    }
                }
            } else {
                items(rides, key = { it.id }) { ride ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${ride.rideNumber} • ${ride.vehicleType}", fontWeight = FontWeight.ExtraBold)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (ride.status == "COMPLETED") TakeItGreenSoft else RideYellowSoft
                                ) {
                                    Text(
                                        ride.status.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (ride.status == "COMPLETED") TakeItGreen else RideCharcoal,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text("From: ${ride.pickupAddress}", style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                            Text("To: ${ride.dropAddress}", style = MaterialTheme.typography.bodySmall, color = TakeItInk, fontWeight = FontWeight.SemiBold)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("OTP: ${ride.otp} • Captain: ${ride.captainName}", style = MaterialTheme.typography.labelSmall, color = RideBlue)
                                Text("₹${ride.totalFare} (${ride.paymentMethod})", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                            }
                            if (ride.status == "REQUESTED") {
                                OutlinedButton(onClick = { onCancelRide(ride.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Cancel Ride", color = TakeItRed)
                                }
                            }
                            if (ride.status == "COMPLETED" && ride.rating == 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Rate Captain:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    (1..5).forEach { stars ->
                                        IconButton(onClick = { onRateRide(ride.id, stars) }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Rounded.Star, "Rate $stars", tint = TakeItGold)
                                        }
                                    }
                                }
                            } else if (ride.rating > 0) {
                                Text("Rated ${ride.rating} ★", color = TakeItGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RideAccountTab(
    uiState: TakeItUiState,
    onUpdateProfile: (String, String) -> Unit,
    onOpenWallet: () -> Unit,
    onOpenRides: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }

    TakeIt3DBackdrop(RideYellow, RideCharcoal, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TakeIt3DCard(
                    containerColor = RideCharcoal,
                    depthColor = Color.Black,
                    borderColor = RideYellow,
                    cornerRadius = 26.dp,
                    depth = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(RideCharcoal, Color(0xFF1E293B))))
                            .statusBarsPadding()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(RideYellow)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    uiState.profile.name.ifBlank { uiState.displayName }.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = RideCharcoal,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uiState.profile.name.ifBlank { uiState.displayName }, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(uiState.email, style = MaterialTheme.typography.bodySmall, color = RideYellow)
                                Text(uiState.profile.phone.ifBlank { "Tap edit to add phone" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            }
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.Edit,
                                containerColor = RideYellow,
                                iconTint = RideCharcoal,
                                size = 40.dp,
                                contentDescription = "Edit Profile",
                                onClick = { showEdit = true }
                            )
                        }
                    }
                }
            }

            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("My Ride History", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${uiState.myRides.size} rides") },
                            leadingContent = {
                                TakeIt3DIconOrb(Icons.Rounded.Route, RideCharcoal, iconTint = RideYellow, size = 36.dp)
                            },
                            modifier = Modifier.clickable(onClick = onOpenRides)
                        )
                        HorizontalDivider(color = TakeItBorder)
                        ListItem(
                            headlineContent = { Text("Take It Wallet & Razorpay", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Balance: ₹${uiState.profile.walletBalance.toInt()}") },
                            leadingContent = {
                                TakeIt3DIconOrb(Icons.Rounded.AccountBalanceWallet, RideBlue, iconTint = Color.White, size = 36.dp)
                            },
                            modifier = Modifier.clickable(onClick = onOpenWallet)
                        )
                        HorizontalDivider(color = TakeItBorder)
                        ListItem(
                            headlineContent = { Text("Switch Google Account / Sign Out", fontWeight = FontWeight.ExtraBold, color = TakeItRed) },
                            supportingContent = { Text("Pick any Gmail account cleanly") },
                            leadingContent = {
                                TakeIt3DIconOrb(Icons.Rounded.SwitchAccount, TakeItRed, iconTint = Color.White, size = 36.dp)
                            },
                            modifier = Modifier
                                .clickable(onClick = onSignOut)
                                .testTag("ride_switch_account_btn")
                        )
                    }
                }
            }
        }
    }

    if (showEdit) {
        var nameInput by remember { mutableStateOf(uiState.profile.name.ifBlank { uiState.displayName }) }
        var phoneInput by remember { mutableStateOf(uiState.profile.phone) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit Rider Profile", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, phoneInput)
                        showEdit = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RideCharcoal, contentColor = RideYellow)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }
}
