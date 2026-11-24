package com.example.takeit.ui.transport

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.network.TakeItPaymentBus
import com.example.network.TakeItPaymentGateway
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.components.*
import com.example.takeit.ui.customer.CustomerWalletScreen
import com.example.takeit.ui.ride.calculateHaversineKm
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel
import kotlinx.coroutines.launch
import kotlin.math.min
import kotlin.math.roundToInt

private val defaultTransportVehicles = listOf(
    MobilityVehicleCategory("def_tr_bike", "TRANSPORT", "BIKE", "Bike Parcel", "Express parcel up to 20 kg", "20 kg Load", 5, 49, 8, 1, 49, 15, "", 1, true),
    MobilityVehicleCategory("def_tr_auto", "TRANSPORT", "AUTO", "Auto Cargo", "Up to 350 kg • Boxes", "350 kg Load", 8, 149, 12, 1, 149, 15, "", 2, true),
    MobilityVehicleCategory("def_tr_mini", "TRANSPORT", "MINI_TRUCK", "Mini Truck", "1 Ton Load • Open/Closed", "1 Ton Load", 15, 299, 18, 2, 349, 18, "", 3, true),
    MobilityVehicleCategory("def_tr_pickup", "TRANSPORT", "PICKUP", "Pickup", "1.5 Ton • Appliances & Stock", "1.5 Ton Load", 20, 499, 24, 2, 549, 18, "", 4, true),
    MobilityVehicleCategory("def_tr_truck", "TRANSPORT", "TRUCK", "Truck", "3.5 Ton • Wholesale Cargo", "3.5 Ton Load", 30, 899, 32, 3, 999, 20, "", 5, true),
    MobilityVehicleCategory("def_tr_container", "TRANSPORT", "CONTAINER", "Container", "7 Ton • Weatherproof Hub", "7 Ton Closed", 45, 1499, 42, 4, 1599, 20, "", 6, true)
)

private data class ParcelFareBreakdown(
    val baseFare: Int,
    val distanceCharge: Int,
    val platformFee: Int,
    val gstAmount: Int,
    val discountAmount: Int,
    val driverEarnings: Int,
    val totalFare: Int,
    val estMinutes: Int
)

private fun computeParcelFare(
    vehicle: MobilityVehicleCategory,
    distanceKm: Int,
    weightKg: Int,
    fragile: Boolean,
    bodyType: String,
    config: TakeItAppConfig,
    couponCode: String,
    coupons: List<PromoCoupon>
): ParcelFareBreakdown {
    val km = distanceKm.coerceAtLeast(1)
    val distCharge = km * vehicle.perKmRate
    val weightExtra = (weightKg.coerceAtLeast(1) - 1) * 3
    val bodyExtra = if (bodyType == "Closed Body") 40 else 0
    val fragileExtra = if (fragile) 35 else 0
    val rawSubtotal = maxOf(vehicle.minFare, vehicle.baseFare + distCharge + weightExtra + bodyExtra + fragileExtra)
    val platformFee = config.platformFee.coerceAtLeast(5)
    val gst = ((rawSubtotal * config.gstPercent) / 100.0).roundToInt()

    val cleanCode = couponCode.trim().uppercase()
    val matched = coupons.firstOrNull { it.activeFlag && it.code.equals(cleanCode, ignoreCase = true) }
    val discount = when {
        matched != null && rawSubtotal >= matched.minOrderAmount -> {
            if (matched.discountType == "FLAT") min(matched.discountValue, rawSubtotal)
            else {
                val pct = (rawSubtotal * matched.discountValue / 100.0).roundToInt()
                if (matched.maxDiscount > 0) min(pct, matched.maxDiscount) else pct
            }
        }
        cleanCode == "MOVE20" -> min(200, (rawSubtotal * 0.20).roundToInt())
        else -> 0
    }

    val total = (rawSubtotal + platformFee + gst - discount).coerceAtLeast(49)
    val comm = ((total * vehicle.commissionPercent) / 100.0).roundToInt()
    val driverEarn = (total - comm).coerceAtLeast(30)
    val estMin = (vehicle.etaMinutes + km * 3).coerceIn(15, 240)
    return ParcelFareBreakdown(
        baseFare = vehicle.baseFare,
        distanceCharge = distCharge + weightExtra + bodyExtra + fragileExtra,
        platformFee = platformFee,
        gstAmount = gst,
        discountAmount = discount,
        driverEarnings = driverEarn,
        totalFare = total,
        estMinutes = estMin
    )
}

@Composable
fun ParcelTransportAppRoot(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val transportVehicles = remember(uiState.mobilityVehicles) {
        val cloud = uiState.mobilityVehicles.filter { it.vertical.equals("TRANSPORT", ignoreCase = true) && it.activeFlag }
        if (cloud.isNotEmpty()) cloud else defaultTransportVehicles
    }

    var pickup by remember(uiState.profile.activeAddressLine) {
        mutableStateOf(uiState.profile.activeAddressLine.ifBlank { "Sector 62 Warehouse, Greater Noida" })
    }
    var pickupLat by remember(uiState.activeLatitude) { mutableDoubleStateOf(uiState.activeLatitude.takeIf { it != 0.0 } ?: 28.4744) }
    var pickupLng by remember(uiState.activeLongitude) { mutableDoubleStateOf(uiState.activeLongitude.takeIf { it != 0.0 } ?: 77.5040) }
    var drop by remember { mutableStateOf("Alpha 1 Commercial Belt, Greater Noida") }
    var dropLat by remember { mutableDoubleStateOf(28.4682) }
    var dropLng by remember { mutableDoubleStateOf(77.5162) }

    var receiverName by remember { mutableStateOf("") }
    var receiverPhone by remember { mutableStateOf("") }
    var packageType by remember { mutableStateOf("Boxes / Commercial Goods") }
    var weightKg by remember { mutableIntStateOf(15) }
    var fragile by remember { mutableStateOf(false) }
    var bodyType by remember { mutableStateOf("Open Body") }
    var selectedVehicleCode by remember { mutableStateOf("MINI_TRUCK") }
    var couponInput by remember { mutableStateOf("") }
    var appliedCoupon by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("RAZORPAY") }

    var showLocationSheet by remember { mutableStateOf(false) }
    var businessAccountMode by remember { mutableStateOf(false) }
    var pendingPayRef by remember { mutableStateOf<String?>(null) }
    var pendingDraftTotal by remember { mutableIntStateOf(0) }

    val distanceKm = remember(pickupLat, pickupLng, dropLat, dropLng) {
        calculateHaversineKm(pickupLat, pickupLng, dropLat, dropLng)
    }
    val selectedVehicle = remember(transportVehicles, selectedVehicleCode) {
        transportVehicles.firstOrNull { it.code.equals(selectedVehicleCode, ignoreCase = true) } ?: transportVehicles.first()
    }
    val fare = remember(selectedVehicle, distanceKm, weightKg, fragile, bodyType, uiState.config, appliedCoupon, uiState.coupons) {
        computeParcelFare(selectedVehicle, distanceKm, weightKg, fragile, bodyType, uiState.config, appliedCoupon, uiState.coupons)
    }

    fun submitParcelWithPayment(method: String, status: String, ref: String, amount: Int) {
        viewModel.bookParcel(
            vehicleType = selectedVehicle.code,
            pickup = pickup,
            drop = drop,
            receiverName = receiverName.ifBlank { "Receiver" },
            receiverPhone = receiverPhone.ifBlank { uiState.profile.phone },
            packageType = packageType,
            weightKg = weightKg,
            fragile = fragile,
            fare = amount,
            paymentMethod = method,
            paymentStatus = status,
            paymentRef = ref,
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            dropLat = dropLat,
            dropLng = dropLng,
            distanceKm = distanceKm,
            bodyType = bodyType,
            baseFare = fare.baseFare,
            distanceCharge = fare.distanceCharge,
            platformFee = fare.platformFee,
            gstAmount = fare.gstAmount,
            discountAmount = fare.discountAmount,
            couponCode = appliedCoupon,
            driverEarnings = fare.driverEarnings,
            onBooked = { viewModel.selectTransportTab(TransportTab.SEND) }
        )
    }

    LaunchedEffect(Unit) {
        TakeItPaymentBus.results.collect { res ->
            if (res.orderRef == pendingPayRef && res.status.equals("success", ignoreCase = true)) {
                val amt = if (res.amount > 0) res.amount else pendingDraftTotal
                pendingPayRef = null
                submitParcelWithPayment("RAZORPAY", "PAID", res.paymentId, amt)
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
                        val amt = if (res.amount > 0) res.amount else pendingDraftTotal
                        pendingPayRef = null
                        submitParcelWithPayment("RAZORPAY", "PAID", res.paymentId, amt)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val startBooking: () -> Unit = {
        when {
            pickup.isBlank() || drop.isBlank() -> viewModel.showMessage("Enter both pickup & drop addresses")
            receiverName.isBlank() || receiverPhone.isBlank() -> viewModel.showMessage("Enter receiver name & mobile number")
            paymentMethod in listOf("RAZORPAY", "CARD") -> {
                val ref = "TKTP_${System.currentTimeMillis()}"
                pendingPayRef = ref
                pendingDraftTotal = fare.totalFare
                val launched = TakeItPaymentGateway.launchChromePayment(
                    context = context,
                    orderRef = ref,
                    amount = fare.totalFare,
                    serviceType = "MOVEX_LOGISTICS",
                    itemTitle = "${selectedVehicle.name} ($distanceKm km)",
                    customerName = uiState.profile.name.ifBlank { uiState.displayName },
                    customerPhone = receiverPhone,
                    userId = uiState.uid
                )
                if (!launched) {
                    pendingPayRef = null
                    viewModel.showMessage("Could not open payment browser. Try Wallet or COD.")
                }
            }
            paymentMethod == "WALLET" -> submitParcelWithPayment("WALLET", "PAID", "WALLET", fare.totalFare)
            else -> submitParcelWithPayment("CASH", "PAY_ON_DELIVERY", "COD", fare.totalFare)
        }
    }

    Scaffold(
        containerColor = TakeItBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            TakeIt3DFloatingNavBar(
                items = TransportTab.entries.map { tab ->
                    val icon = when (tab) {
                        TransportTab.SEND -> Icons.Rounded.Home
                        TransportTab.SHIPMENTS -> Icons.Rounded.LocalShipping
                        TransportTab.WALLET -> Icons.Rounded.AccountBalanceWallet
                        TransportTab.SUPPORT -> Icons.Rounded.SupportAgent
                        TransportTab.ACCOUNT -> Icons.Rounded.Person
                    }
                    Nav3DTabItem(
                        id = tab.name,
                        label = tab.label,
                        icon = icon,
                        selected = uiState.transportTab == tab,
                        testTag = "transport_nav_${tab.name.lowercase()}",
                        onClick = { viewModel.selectTransportTab(tab) }
                    )
                },
                containerColor = TransportNavy,
                activePodColor = TransportOrange,
                activeContentColor = Color.White,
                inactiveColor = Color(0xFF94A3B8),
                borderColor = RideYellow.copy(alpha = 0.45f)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (uiState.transportTab) {
                TransportTab.SEND -> MoveXHomeTab(
                    uiState = uiState,
                    vehicles = transportVehicles,
                    pickup = pickup,
                    drop = drop,
                    distanceKm = distanceKm,
                    selectedVehicle = selectedVehicle,
                    fare = fare,
                    bodyType = bodyType,
                    packageType = packageType,
                    weightKg = weightKg,
                    fragile = fragile,
                    receiverName = receiverName,
                    receiverPhone = receiverPhone,
                    couponInput = couponInput,
                    appliedCoupon = appliedCoupon,
                    paymentMethod = paymentMethod,
                    onSelectVehicle = { selectedVehicleCode = it },
                    onBodyTypeChange = { bodyType = it },
                    onPackageTypeChange = { packageType = it },
                    onWeightChange = { weightKg = it },
                    onFragileChange = { fragile = it },
                    onReceiverNameChange = { receiverName = it },
                    onReceiverPhoneChange = { receiverPhone = it },
                    onCouponInputChange = { couponInput = it.uppercase() },
                    onApplyCoupon = {
                        appliedCoupon = couponInput.trim().uppercase()
                        viewModel.showMessage("Applied $appliedCoupon")
                    },
                    onPaymentMethodChange = { paymentMethod = it },
                    onOpenLocationPicker = { showLocationSheet = true },
                    onBookNow = startBooking,
                    onCancelParcel = viewModel::cancelMyParcel,
                    onRateParcel = viewModel::rateMyParcel,
                    onViewAllBookings = { viewModel.selectTransportTab(TransportTab.SHIPMENTS) },
                    onOpenProfile = { viewModel.selectTransportTab(TransportTab.ACCOUNT) }
                )

                TransportTab.SHIPMENTS -> MoveXBookingsTab(
                    parcels = uiState.myParcels,
                    onCancel = viewModel::cancelMyParcel,
                    onRate = viewModel::rateMyParcel,
                    onRebook = { old ->
                        pickup = old.pickupAddress
                        drop = old.dropAddress
                        if (old.pickupLat != 0.0) pickupLat = old.pickupLat
                        if (old.pickupLng != 0.0) pickupLng = old.pickupLng
                        if (old.dropLat != 0.0) dropLat = old.dropLat
                        if (old.dropLng != 0.0) dropLng = old.dropLng
                        selectedVehicleCode = old.vehicleType
                        viewModel.selectTransportTab(TransportTab.SEND)
                    }
                )

                TransportTab.WALLET -> CustomerWalletScreen(
                    uiState = uiState,
                    onBack = { viewModel.selectTransportTab(TransportTab.SEND) },
                    onTopUpConfirmed = viewModel::recordWalletTopUp,
                    onShowMessage = viewModel::showMessage
                )

                TransportTab.SUPPORT -> MoveXSupportTab(uiState = uiState, viewModel = viewModel)

                TransportTab.ACCOUNT -> MoveXProfileTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    businessAccountMode = businessAccountMode,
                    onToggleBusiness = { businessAccountMode = it },
                    onOpenSupportTab = { viewModel.selectTransportTab(TransportTab.SUPPORT) },
                    onSignOut = onSignOut
                )
            }
        }
    }

    if (showLocationSheet) {
        MoveXLocationPickerDialog(
            initialPickup = pickup,
            initialDrop = drop,
            initialPickupLat = pickupLat,
            initialPickupLng = pickupLng,
            initialDropLat = dropLat,
            initialDropLng = dropLng,
            onDismiss = { showLocationSheet = false },
            onConfirm = { pAddr, pLat, pLng, dAddr, dLat, dLng ->
                pickup = pAddr; pickupLat = pLat; pickupLng = pLng
                drop = dAddr; dropLat = dLat; dropLng = dLng
                showLocationSheet = false
            }
        )
    }
}

@Composable
private fun MoveXHomeTab(
    uiState: TakeItUiState,
    vehicles: List<MobilityVehicleCategory>,
    pickup: String,
    drop: String,
    distanceKm: Int,
    selectedVehicle: MobilityVehicleCategory,
    fare: ParcelFareBreakdown,
    bodyType: String,
    packageType: String,
    weightKg: Int,
    fragile: Boolean,
    receiverName: String,
    receiverPhone: String,
    couponInput: String,
    appliedCoupon: String,
    paymentMethod: String,
    onSelectVehicle: (String) -> Unit,
    onBodyTypeChange: (String) -> Unit,
    onPackageTypeChange: (String) -> Unit,
    onWeightChange: (Int) -> Unit,
    onFragileChange: (Boolean) -> Unit,
    onReceiverNameChange: (String) -> Unit,
    onReceiverPhoneChange: (String) -> Unit,
    onCouponInputChange: (String) -> Unit,
    onApplyCoupon: () -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onOpenLocationPicker: () -> Unit,
    onBookNow: () -> Unit,
    onCancelParcel: (String) -> Unit,
    onRateParcel: (String, Int) -> Unit,
    onViewAllBookings: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val context = LocalContext.current
    var showFareDetails by remember { mutableStateOf(false) }

    val activeParcel = remember(uiState.myParcels) {
        uiState.myParcels.firstOrNull { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
    }

    TakeIt3DBackdrop(TransportOrange, TransportNavy, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // MoveX Navy + Yellow Header
            item {
                TakeIt3DCard(
                    containerColor = TransportNavy,
                    depthColor = Color.Black,
                    borderColor = RideYellow,
                    cornerRadius = 26.dp,
                    depth = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Hello, ${uiState.profile.name.ifBlank { uiState.displayName }.substringBefore(" ")} 👋",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text("MoveX • Deliver Anything, Easily", style = MaterialTheme.typography.labelMedium, color = RideYellow, fontWeight = FontWeight.Bold)
                            }
                            TakeItMediaVisual(
                                imageUrl = TakeItFreeMedia.avatarAndSupportCatalog[0],
                                key = "movex_avatar",
                                contentDescription = "Profile",
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(RideYellow)
                                    .clickable { onOpenProfile() }
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.fillMaxWidth().clickable { onOpenLocationPicker() }
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.LocationOn, null, tint = RideYellow, modifier = Modifier.size(16.dp))
                                Text(pickup, color = Color.White, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.ChevronRight, null, tint = RideYellow)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier.fillMaxWidth().clickable { onOpenLocationPicker() }.testTag("movex_search_dest_btn")
                        ) {
                            Row(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Icon(Icons.Rounded.Search, "Search drop", tint = TransportNavy)
                                Text(drop.ifBlank { "Search destination..." }, color = if (drop.isBlank()) TakeItMuted else TransportNavy, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                                Surface(shape = RoundedCornerShape(8.dp), color = RideYellowSoft) {
                                    Text("$distanceKm km", color = TransportNavy, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Live Active Shipment Tracking (Leaflet + 3-step Bar + OTP + Call/SMS)
            if (activeParcel != null) {
                item {
                    val progress = when (activeParcel.status) {
                        "BOOKED" -> 0.15f
                        "PARTNER_ASSIGNED" -> 0.42f
                        "PICKED_UP" -> 0.65f
                        "IN_TRANSIT" -> 0.85f
                        else -> 0.95f
                    }
                    val stepIdx = when (activeParcel.status) {
                        "BOOKED", "PARTNER_ASSIGNED" -> 0
                        "PICKED_UP", "IN_TRANSIT" -> 1
                        else -> 2
                    }

                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        borderColor = TransportOrange,
                        cornerRadius = 24.dp,
                        depth = 6.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Driver Arriving in ${activeParcel.etaMinutes} min", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                                    Text("${activeParcel.parcelNumber} • ${activeParcel.vehicleType} (${activeParcel.bodyType})", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                                }
                                Surface(shape = RoundedCornerShape(10.dp), color = TransportNavy) {
                                    Text("OTP ${activeParcel.otp}", color = RideYellow, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                                }
                            }

                            Box(modifier = Modifier.fillMaxWidth().height(185.dp).clip(RoundedCornerShape(18.dp))) {
                                LeafletTrackingMap(
                                    pickupLat = activeParcel.pickupLat,
                                    pickupLng = activeParcel.pickupLng,
                                    pickupTitle = "Pickup: ${activeParcel.pickupAddress}",
                                    dropLat = activeParcel.dropLat,
                                    dropLng = activeParcel.dropLng,
                                    dropTitle = "Drop: ${activeParcel.dropAddress}",
                                    riderLabel = activeParcel.partnerName,
                                    progressFraction = progress,
                                    accentHex = "#FF6D00",
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            // 3-step Pickup -> In Transit -> Delivered Bar
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                listOf("Pickup", "In Transit", "Delivered").forEachIndexed { idx, label ->
                                    val done = idx <= stepIdx
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(if (done) TransportOrange else TakeItBorder))
                                        Text(label, style = MaterialTheme.typography.labelSmall, fontWeight = if (done) FontWeight.ExtraBold else FontWeight.Normal, color = if (done) TransportNavy else TakeItMuted)
                                    }
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TakeItMediaVisual(
                                    imageUrl = TakeItFreeMedia.avatarAndSupportCatalog[3],
                                    key = "movex_driver",
                                    contentDescription = "Driver",
                                    modifier = Modifier.size(42.dp).clip(CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(activeParcel.partnerName, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                                    Text(activeParcel.vehicleNumber.ifBlank { activeParcel.vehicleType }, style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                                }
                                IconButton(onClick = { dialPhone(context, activeParcel.partnerPhone.ifBlank { uiState.config.supportPhone }) }, modifier = Modifier.clip(CircleShape).background(TakeItGreenSoft)) {
                                    Icon(Icons.Rounded.Call, "Call Driver", tint = TakeItGreen)
                                }
                                IconButton(onClick = { sendSmsIntent(context, activeParcel.partnerPhone, "MoveX Parcel OTP: ${activeParcel.otp}") }, modifier = Modifier.clip(CircleShape).background(RideYellowSoft)) {
                                    Icon(Icons.Rounded.Sms, "SMS Driver", tint = TransportNavy)
                                }
                            }

                            if (activeParcel.status == "BOOKED") {
                                OutlinedButton(onClick = { onCancelParcel(activeParcel.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Cancel Booking", color = TakeItRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            // 6-Vehicle Grid (Bike, Auto, Mini Truck, Pickup, Truck, Container)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose Logistics Vehicle", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                    val rows = vehicles.chunked(3)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        rows.forEach { rowItems ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                rowItems.forEach { v ->
                                    val selected = v.code.equals(selectedVehicle.code, ignoreCase = true)
                                    val imgUrl = v.imageUrl.ifBlank { TakeItFreeMedia.resolveVehicleMediaUrl(v.code, true) }
                                    TakeIt3DCard(
                                        onClick = { onSelectVehicle(v.code) },
                                        containerColor = if (selected) RideYellowSoft else Color.White,
                                        depthColor = if (selected) RideYellow.darken3D(0.4f) else Color(0xFFD5DCE6),
                                        borderColor = if (selected) TransportOrange else TakeItBorder,
                                        cornerRadius = 16.dp,
                                        depth = if (selected) 4.dp else 2.dp,
                                        modifier = Modifier.weight(1f).testTag("movex_veh_${v.code.lowercase()}")
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            TakeItMediaVisual(
                                                imageUrl = imgUrl,
                                                key = v.code,
                                                contentDescription = v.name,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Text(v.name, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, color = TransportNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text(v.capacityLabel, fontSize = 9.sp, color = TakeItMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                                repeat(3 - rowItems.size) { Spacer(Modifier.weight(1f)) }
                            }
                        }
                    }
                }
            }

            // Promo Banner ("Flat 20% OFF")
            item {
                TakeIt3DCard(
                    containerColor = TransportNavy,
                    depthColor = Color.Black,
                    borderColor = RideYellow,
                    cornerRadius = 20.dp,
                    depth = 5.dp,
                    onClick = {
                        onCouponInputChange("MOVE20")
                        onApplyCoupon()
                    },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(TransportNavy, Color(0xFF1D4ED8))))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = RideYellow) {
                                Text("Code: MOVE20", color = TransportNavy, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text("Flat 20% OFF", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text("on your next logistics booking", style = MaterialTheme.typography.labelSmall, color = Color(0xFFDBEAFE))
                        }
                        TakeItMediaVisual(
                            imageUrl = TakeItFreeMedia.transportMediaCatalog[2],
                            key = "movex_promo_truck",
                            contentDescription = "MoveX Truck Promo",
                            modifier = Modifier.size(66.dp)
                        )
                    }
                }
            }

            // Selected Vehicle & Shipment Form + Open/Closed Body + Fare Details + Payment
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                TakeItMediaVisual(
                                    imageUrl = selectedVehicle.imageUrl.ifBlank { TakeItFreeMedia.resolveVehicleMediaUrl(selectedVehicle.code, true) },
                                    key = selectedVehicle.code,
                                    contentDescription = selectedVehicle.name,
                                    modifier = Modifier.size(48.dp)
                                )
                                Column {
                                    Text(selectedVehicle.name, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                                    Text("Est. ${fare.estMinutes} min • $distanceKm km • ${selectedVehicle.capacityLabel}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                                }
                            }
                            Text("₹${fare.totalFare}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                        }

                        // Open Body vs Closed Body Toggle
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Open Body", "Closed Body").forEach { bt ->
                                FilterChip(
                                    selected = bodyType == bt,
                                    onClick = { onBodyTypeChange(bt) },
                                    label = { Text(bt, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        OutlinedTextField(value = packageType, onValueChange = onPackageTypeChange, label = { Text("Goods / Parcel Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = weightKg.toString(),
                                onValueChange = { onWeightChange(it.filter(Char::isDigit).toIntOrNull() ?: 1) },
                                label = { Text("Weight (kg)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = fragile,
                                onClick = { onFragileChange(!fragile) },
                                label = { Text("Fragile Care (+₹35)", fontWeight = FontWeight.Bold) }
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(value = receiverName, onValueChange = onReceiverNameChange, label = { Text("Receiver Name *") }, singleLine = true, modifier = Modifier.weight(1f).testTag("movex_receiver_name"))
                            OutlinedTextField(value = receiverPhone, onValueChange = onReceiverPhoneChange, label = { Text("Receiver Phone *") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f).testTag("movex_receiver_phone"))
                        }

                        // Expandable Fare Details
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showFareDetails = !showFareDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fare Details • ₹${fare.totalFare}", fontWeight = FontWeight.Bold, color = TransportNavy)
                            Icon(if (showFareDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = TransportOrange)
                        }
                        AnimatedVisibility(visible = showFareDetails) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Base Fare: ₹${fare.baseFare} • Distance/Weight/Body: ₹${fare.distanceCharge} • Platform: ₹${fare.platformFee} • GST: ₹${fare.gstAmount}${if (fare.discountAmount > 0) " • Discount ($appliedCoupon): -₹${fare.discountAmount}" else ""}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                        }

                        // Coupon apply
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(value = couponInput, onValueChange = onCouponInputChange, label = { Text("Apply Coupon (e.g. MOVE20)") }, singleLine = true, modifier = Modifier.weight(1f))
                            Button(onClick = onApplyCoupon, colors = ButtonDefaults.buttonColors(containerColor = TransportNavy, contentColor = RideYellow)) { Text("Apply", fontWeight = FontWeight.Bold) }
                        }

                        // 4 Payment Options matching Image 5: UPI/Razorpay, Wallet, Card, Cash
                        Text("Select Payment Method", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = paymentMethod == "RAZORPAY", onClick = { onPaymentMethodChange("RAZORPAY") }, label = { Text("UPI / Razorpay", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                                FilterChip(selected = paymentMethod == "WALLET", onClick = { onPaymentMethodChange("WALLET") }, label = { Text("Wallet ₹${uiState.profile.walletBalance.toInt()}", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                FilterChip(selected = paymentMethod == "CARD", onClick = { onPaymentMethodChange("CARD") }, label = { Text("Credit/Debit Card", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                                FilterChip(selected = paymentMethod == "CASH", onClick = { onPaymentMethodChange("CASH") }, label = { Text("Cash on Delivery", fontSize = 11.sp) }, modifier = Modifier.weight(1f))
                            }
                        }

                        TakeIt3DButton(
                            onClick = onBookNow,
                            containerColor = RideYellow,
                            depthColor = RideYellow.darken3D(0.4f),
                            contentColor = TransportNavy,
                            cornerRadius = 16.dp,
                            depth = 5.dp,
                            height = 54.dp,
                            modifier = Modifier.fillMaxWidth().testTag("movex_book_now_btn")
                        ) { Text("Book Now • ₹${fare.totalFare}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }

            // Recent Bookings Summary
            if (uiState.myParcels.isNotEmpty()) {
                item {
                    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text("Recent Bookings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                        TextButton(onClick = onViewAllBookings) { Text("View All →", color = TransportOrange, fontWeight = FontWeight.Bold) }
                    }
                }
                items(uiState.myParcels.take(2), key = { "rec_${it.id}" }) { p ->
                    TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 18.dp, depth = 3.dp, modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                        Row(Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${p.parcelNumber} • ${p.vehicleType}", fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                                Text("${p.pickupAddress} → ${p.dropAddress}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("₹${p.totalFare}", fontWeight = FontWeight.ExtraBold, color = TransportOrange)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveXBookingsTab(
    parcels: List<ParcelOrder>,
    onCancel: (String) -> Unit,
    onRate: (String, Int) -> Unit,
    onRebook: (ParcelOrder) -> Unit
) {
    var filter by remember { mutableStateOf("All") }
    val filtered = remember(parcels, filter) {
        when (filter) {
            "Active" -> parcels.filter { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
            "Completed" -> parcels.filter { it.status == "DELIVERED" }
            "Cancelled" -> parcels.filter { it.status in listOf("CANCELLED", "REFUNDED") }
            else -> parcels
        }
    }

    TakeIt3DBackdrop(TransportOrange, TransportNavy, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("My Bookings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("All", "Active", "Completed", "Cancelled")) { tab ->
                        FilterChip(selected = filter == tab, onClick = { filter = tab }, label = { Text(tab, fontWeight = FontWeight.Bold) })
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 20.dp, depth = 4.dp) {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TakeIt3DIconOrb(Icons.Rounded.LocalShipping, TransportNavy, iconTint = RideYellow, size = 56.dp)
                            Text("No $filter shipments yet", fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                        }
                    }
                }
            } else {
                items(filtered, key = { "pcl_${it.id}" }) { p ->
                    TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 18.dp, depth = 4.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${p.parcelNumber} • ${p.vehicleType}", fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                                Surface(shape = RoundedCornerShape(50), color = if (p.status == "DELIVERED") TakeItGreenSoft else RideYellowSoft) {
                                    Text(p.status.replace("_", " "), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = if (p.status == "DELIVERED") TakeItGreen else TransportNavy, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            Text("Pickup: ${p.pickupAddress}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            Text("Drop: ${p.dropAddress} (To: ${p.receiverName})", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = TransportNavy)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("OTP: ${p.otp} • ${p.bodyType}", style = MaterialTheme.typography.labelSmall, color = RideBlue)
                                Text("₹${p.totalFare} (${p.paymentMethod})", fontWeight = FontWeight.ExtraBold, color = TransportOrange)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (p.status == "BOOKED") {
                                    Button(onClick = { onCancel(p.id) }, colors = ButtonDefaults.buttonColors(containerColor = TakeItRed), modifier = Modifier.weight(1f)) { Text("Cancel") }
                                }
                                Button(onClick = { onRebook(p) }, colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = TransportNavy), modifier = Modifier.weight(1f)) { Text("Rebook", fontWeight = FontWeight.ExtraBold) }
                            }
                            if (p.status == "DELIVERED" && p.rating == 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Rate Driver:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    (1..5).forEach { s ->
                                        IconButton(onClick = { onRate(p.id, s) }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Rounded.Star, "Rate $s", tint = RideYellow)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoveXSupportTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var policyDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    TakeIt3DBackdrop(TransportOrange, TransportNavy, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Text("MoveX 24×7 Support", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = TransportNavy) }
            item {
                SupportChannelsCard(
                    config = uiState.config,
                    accentColor = TransportOrange
                )
            }
            item {
                TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 20.dp, depth = 4.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Raise Shipment Support Ticket", fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Booking ID / Subject") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe issue...") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        TakeIt3DButton(
                            onClick = {
                                if (subject.isNotBlank() && message.isNotBlank()) {
                                    viewModel.createSupportTicket(subject, message)
                                    subject = ""; message = ""
                                }
                            },
                            containerColor = TransportNavy,
                            depthColor = Color.Black,
                            contentColor = RideYellow,
                            height = 46.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Submit Support Ticket", fontWeight = FontWeight.ExtraBold) }
                    }
                }
            }
            item {
                TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 18.dp, depth = 3.dp) {
                    Column {
                        ListItem(headlineContent = { Text("Terms & Conditions", fontWeight = FontWeight.Bold) }, trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) }, modifier = Modifier.clickable { policyDialog = "Terms & Conditions" to uiState.config.termsText })
                        HorizontalDivider(color = TakeItBorder)
                        ListItem(headlineContent = { Text("Privacy Policy", fontWeight = FontWeight.Bold) }, trailingContent = { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null) }, modifier = Modifier.clickable { policyDialog = "Privacy Policy" to uiState.config.privacyText })
                    }
                }
            }
            items(uiState.myTickets, key = { "sup_${it.id}" }) { t ->
                TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 16.dp, depth = 3.dp) {
                    Column(Modifier.padding(12.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(t.subject, fontWeight = FontWeight.Bold, color = TransportNavy)
                            Text(t.status, color = if (t.status == "RESOLVED") TakeItGreen else TransportOrange, fontWeight = FontWeight.Bold)
                        }
                        if (t.adminReply.isNotBlank()) Text("Admin Reply: ${t.adminReply}", style = MaterialTheme.typography.labelSmall, color = TakeItGreen)
                    }
                }
            }
        }
    }

    policyDialog?.let { (title, body) ->
        AboutPolicyDialog(title = title, bodyText = body, accentColor = TransportOrange, onDismiss = { policyDialog = null })
    }
}

@Composable
private fun MoveXProfileTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    businessAccountMode: Boolean,
    onToggleBusiness: (Boolean) -> Unit,
    onOpenSupportTab: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }
    var showAbout by remember { mutableStateOf(false) }

    TakeIt3DBackdrop(TransportOrange, TransportNavy, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TakeIt3DCard(containerColor = TransportNavy, depthColor = Color.Black, borderColor = RideYellow, cornerRadius = 24.dp, depth = 6.dp) {
                    Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TakeItMediaVisual(
                            imageUrl = TakeItFreeMedia.avatarAndSupportCatalog[0],
                            key = "movex_prof",
                            contentDescription = "Profile",
                            modifier = Modifier.size(62.dp).clip(CircleShape).background(RideYellow).padding(4.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.profile.name.ifBlank { uiState.displayName }, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text(uiState.email, style = MaterialTheme.typography.labelSmall, color = RideYellow)
                            Text(uiState.profile.phone.ifBlank { "Add phone number" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                        }
                        OutlinedButton(onClick = { showEdit = true }, border = BorderStroke(1.dp, RideYellow)) { Text("Edit", color = RideYellow) }
                    }
                }
            }
            item {
                TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 22.dp, depth = 5.dp) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Business / Commercial Account", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text(if (businessAccountMode) "GST Invoice enabled" else "Personal shipments") },
                            leadingContent = { TakeIt3DIconOrb(Icons.Rounded.Business, TransportNavy, iconTint = RideYellow, size = 34.dp) },
                            trailingContent = { Switch(checked = businessAccountMode, onCheckedChange = onToggleBusiness) }
                        )
                        HorizontalDivider(color = TakeItBorder)
                        ListItem(
                            headlineContent = { Text("Help & 24×7 Support", fontWeight = FontWeight.Bold) },
                            leadingContent = { TakeIt3DIconOrb(Icons.Rounded.SupportAgent, TransportOrange, size = 34.dp) },
                            modifier = Modifier.clickable(onClick = onOpenSupportTab)
                        )
                        HorizontalDivider(color = TakeItBorder)
                        ListItem(
                            headlineContent = { Text("About MoveX", fontWeight = FontWeight.Bold) },
                            leadingContent = { TakeIt3DIconOrb(Icons.Rounded.Info, RideBlue, size = 34.dp) },
                            modifier = Modifier.clickable { showAbout = true }
                        )
                        HorizontalDivider(color = TakeItBorder)
                        ListItem(
                            headlineContent = { Text("Switch Google Account / Sign Out", fontWeight = FontWeight.ExtraBold, color = TakeItRed) },
                            leadingContent = { TakeIt3DIconOrb(Icons.Rounded.SwitchAccount, TakeItRed, size = 34.dp) },
                            modifier = Modifier.clickable(onClick = onSignOut).testTag("movex_sign_out_btn")
                        )
                    }
                }
            }
        }
    }

    if (showEdit) {
        var name by remember { mutableStateOf(uiState.profile.name.ifBlank { uiState.displayName }) }
        var phone by remember { mutableStateOf(uiState.profile.phone) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit MoveX Profile", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.updateCustomerPhoneAndName(name, phone); showEdit = false }, colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = TransportNavy)) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }

    if (showAbout) {
        AboutPolicyDialog(title = "About MoveX Logistics", bodyText = uiState.config.aboutUsText, accentColor = TransportOrange, onDismiss = { showAbout = false })
    }
}

@Composable
private fun MoveXLocationPickerDialog(
    initialPickup: String,
    initialDrop: String,
    initialPickupLat: Double,
    initialPickupLng: Double,
    initialDropLat: Double,
    initialDropLng: Double,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var target by remember { mutableStateOf("DROP") }
    var pickup by remember { mutableStateOf(initialPickup) }
    var drop by remember { mutableStateOf(initialDrop) }
    var pLat by remember { mutableDoubleStateOf(initialPickupLat) }
    var pLng by remember { mutableDoubleStateOf(initialPickupLng) }
    var dLat by remember { mutableDoubleStateOf(initialDropLat) }
    var dLng by remember { mutableDoubleStateOf(initialDropLng) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Select Pickup & Drop on Map", fontWeight = FontWeight.ExtraBold, color = TransportNavy) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = target == "PICKUP", onClick = { target = "PICKUP" }, label = { Text("Pin Pickup") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = target == "DROP", onClick = { target = "DROP" }, label = { Text("Pin Drop") }, modifier = Modifier.weight(1f))
                }
                Box(modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp))) {
                    LeafletAddressPickerMap(
                        selectedLat = if (target == "PICKUP") pLat else dLat,
                        selectedLng = if (target == "PICKUP") pLng else dLng,
                        onLocationPicked = { lat, lng ->
                            reverseGeocodeCoordinates(context, scope, lat, lng) { rLat, rLng, _, _, full, _ ->
                                if (target == "PICKUP") { pLat = rLat; pLng = rLng; pickup = full }
                                else { dLat = rLat; dLng = rLng; drop = full }
                            }
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
                OutlinedTextField(
                    value = pickup,
                    onValueChange = { pickup = it },
                    label = { Text("Pickup Location") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            autofetchCurrentAddress(context, scope, { lat, lng, _, _, full, _ -> pLat = lat; pLng = lng; pickup = full }, {})
                        }) { Icon(Icons.Rounded.MyLocation, "GPS Current", tint = TransportOrange) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = drop, onValueChange = { drop = it }, label = { Text("Drop Destination") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pickup, pLat, pLng, drop, dLat, dLng) },
                colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = TransportNavy)
            ) { Text("Confirm Location (${calculateHaversineKm(pLat, pLng, dLat, dLng)} km)", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
