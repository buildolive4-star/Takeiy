package com.example.takeit.ui.ride

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
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
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val defaultRideCategories = listOf(
    MobilityVehicleCategory("def_bike", "RIDE", "BIKE", "Bike", "1 rider • Beats traffic", "1 Seat", 2, 25, 7, 1, 35, 15, "", 1, true),
    MobilityVehicleCategory("def_auto", "RIDE", "AUTO", "Auto", "Doorstep pickup", "3 Seats", 3, 35, 11, 1, 45, 15, "", 2, true),
    MobilityVehicleCategory("def_mini", "RIDE", "MINI", "Mini Cab", "Compact AC Hatch", "4 Seats", 4, 55, 14, 2, 75, 18, "", 3, true),
    MobilityVehicleCategory("def_sedan", "RIDE", "SEDAN", "Prime Sedan", "Spacious AC Sedan", "4 Seats", 5, 80, 17, 2, 110, 18, "", 4, true),
    MobilityVehicleCategory("def_suv", "RIDE", "SUV", "SUV XL", "6-seater family AC", "6 Seats", 6, 110, 22, 3, 149, 20, "", 5, true),
    MobilityVehicleCategory("def_prem", "RIDE", "PREMIUM", "Premium", "Executive chauffeur", "4 Seats", 5, 149, 26, 3, 199, 20, "", 6, true)
)

private val quickModeShortcuts = listOf(
    Triple("BIKE", "Bike", "Fastest 2W"),
    Triple("AUTO", "Auto", "Budget 3W"),
    Triple("MINI", "Cab", "AC Hatch"),
    Triple("PREMIUM", "Premium", "Luxury Sedan"),
    Triple("BIKE", "Parcel", "2W Express"),
    Triple("SUV", "Rental", "Hourly / Out")
)

private data class RideFareSummary(
    val baseFare: Int,
    val distanceCharge: Int,
    val timeCharge: Int,
    val surchargeAmount: Int,
    val platformFee: Int,
    val gstAmount: Int,
    val discountAmount: Int,
    val totalFare: Int,
    val commissionAmount: Int,
    val driverEarnings: Int,
    val durationMin: Int
)

private fun computeRideFare(
    vehicle: MobilityVehicleCategory,
    distanceKm: Int,
    config: TakeItAppConfig,
    couponCode: String,
    coupons: List<PromoCoupon>
): RideFareSummary {
    val km = distanceKm.coerceAtLeast(1)
    val durationMin = (km * 2.4).roundToInt().coerceIn(5, 240)
    val rawSubtotal = max(vehicle.minFare, vehicle.baseFare + (km * vehicle.perKmRate) + (durationMin * vehicle.perMinRate))
    val distCharge = km * vehicle.perKmRate
    val timeCharge = durationMin * vehicle.perMinRate
    val peakExtra = if (config.peakMultiplier > 1.01) ((rawSubtotal * (config.peakMultiplier - 1.0))).roundToInt() else 0
    val platformFee = config.platformFee.coerceAtLeast(0)
    val gstAmount = ((rawSubtotal * config.gstPercent) / 100.0).roundToInt()

    val cleanCode = couponCode.trim().uppercase()
    val matchedCoupon = coupons.firstOrNull { it.activeFlag && it.code.equals(cleanCode, ignoreCase = true) }
    val discount = when {
        matchedCoupon != null && rawSubtotal >= matchedCoupon.minOrderAmount -> {
            if (matchedCoupon.discountType == "FLAT") min(matchedCoupon.discountValue, rawSubtotal)
            else {
                val pct = (rawSubtotal * matchedCoupon.discountValue / 100.0).roundToInt()
                if (matchedCoupon.maxDiscount > 0) min(pct, matchedCoupon.maxDiscount) else pct
            }
        }
        cleanCode == "RIDE50" -> min(100, (rawSubtotal * 0.5).roundToInt())
        else -> 0
    }
    val total = (rawSubtotal + peakExtra + platformFee + gstAmount - discount).coerceAtLeast(25)
    val commission = ((total * vehicle.commissionPercent) / 100.0).roundToInt().coerceAtLeast(0)
    val driverEarn = (total - commission).coerceAtLeast(15)
    return RideFareSummary(
        baseFare = vehicle.baseFare,
        distanceCharge = distCharge,
        timeCharge = timeCharge,
        surchargeAmount = peakExtra,
        platformFee = platformFee,
        gstAmount = gstAmount,
        discountAmount = discount,
        totalFare = total,
        commissionAmount = commission,
        driverEarnings = driverEarn,
        durationMin = durationMin
    )
}

@Composable
fun RapidoRideAppRoot(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    val rideVehicles = remember(uiState.mobilityVehicles) {
        val cloud = uiState.mobilityVehicles.filter { it.vertical.equals("RIDE", ignoreCase = true) && it.activeFlag }
        if (cloud.isNotEmpty()) cloud else defaultRideCategories
    }

    var pickup by remember(uiState.profile.activeAddressLine) {
        mutableStateOf(uiState.profile.activeAddressLine.ifBlank { "Current Location • Sector 62, Greater Noida" })
    }
    var pickupLat by remember(uiState.activeLatitude) { mutableDoubleStateOf(uiState.activeLatitude.takeIf { it != 0.0 } ?: 28.4744) }
    var pickupLng by remember(uiState.activeLongitude) { mutableDoubleStateOf(uiState.activeLongitude.takeIf { it != 0.0 } ?: 77.5040) }
    var drop by remember { mutableStateOf("Pari Chowk Metro, Greater Noida") }
    var dropLat by remember { mutableDoubleStateOf(28.4658) }
    var dropLng by remember { mutableDoubleStateOf(77.5106) }

    var selectedVehicleCode by remember { mutableStateOf("BIKE") }
    var bookingCategory by remember { mutableStateOf("Ride Now") } // Ride Now, Scheduled, Rental, Parcel
    var scheduledFor by remember { mutableStateOf("Today, 6:30 PM") }
    var couponCodeInput by remember { mutableStateOf("") }
    var appliedCode by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("RAZORPAY") }

    var showLocationSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(false) }
    var sosTargetRide by remember { mutableStateOf<RideBooking?>(null) }
    var receiptRide by remember { mutableStateOf<RideBooking?>(null) }

    var pendingPayRef by remember { mutableStateOf<String?>(null) }
    var pendingDraftTotal by remember { mutableIntStateOf(0) }

    val distanceKm = remember(pickupLat, pickupLng, dropLat, dropLng) {
        calculateHaversineKm(pickupLat, pickupLng, dropLat, dropLng)
    }

    val selectedVehicle = remember(rideVehicles, selectedVehicleCode) {
        rideVehicles.firstOrNull { it.code.equals(selectedVehicleCode, ignoreCase = true) } ?: rideVehicles.first()
    }

    val fareSummary = remember(selectedVehicle, distanceKm, uiState.config, appliedCode, uiState.coupons) {
        computeRideFare(selectedVehicle, distanceKm, uiState.config, appliedCode, uiState.coupons)
    }

    fun submitBookingWithPayment(method: String, status: String, ref: String, finalAmount: Int) {
        viewModel.bookRide(
            vehicleType = selectedVehicle.code,
            pickup = pickup,
            drop = drop,
            distanceKm = distanceKm,
            fare = finalAmount,
            paymentMethod = method,
            paymentStatus = status,
            paymentRef = ref,
            bookingCategory = bookingCategory,
            scheduledFor = if (bookingCategory == "Scheduled") scheduledFor else "",
            pickupLat = pickupLat,
            pickupLng = pickupLng,
            dropLat = dropLat,
            dropLng = dropLng,
            durationMin = fareSummary.durationMin,
            baseFare = fareSummary.baseFare,
            distanceCharge = fareSummary.distanceCharge,
            timeCharge = fareSummary.timeCharge,
            surchargeAmount = fareSummary.surchargeAmount,
            platformFee = fareSummary.platformFee,
            gstAmount = fareSummary.gstAmount,
            discountAmount = fareSummary.discountAmount,
            couponCode = appliedCode,
            commissionAmount = fareSummary.commissionAmount,
            driverEarnings = fareSummary.driverEarnings,
            onBooked = { viewModel.selectRideTab(RideTab.BOOK) }
        )
    }

    LaunchedEffect(Unit) {
        TakeItPaymentBus.results.collect { res ->
            if (res.orderRef == pendingPayRef && res.status.equals("success", ignoreCase = true)) {
                val amt = if (res.amount > 0) res.amount else pendingDraftTotal
                pendingPayRef = null
                submitBookingWithPayment("RAZORPAY", "PAID", res.paymentId, amt)
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
                        submitBookingWithPayment("RAZORPAY", "PAID", res.paymentId, amt)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val handleBookClick: () -> Unit = {
        when {
            pickup.isBlank() || drop.isBlank() -> viewModel.showMessage("Select both Pickup & Drop locations")
            paymentMethod == "RAZORPAY" -> {
                val ref = "TKRD_${System.currentTimeMillis()}"
                pendingPayRef = ref
                pendingDraftTotal = fareSummary.totalFare
                val launched = TakeItPaymentGateway.launchChromePayment(
                    context = context,
                    orderRef = ref,
                    amount = fareSummary.totalFare,
                    serviceType = "RIDEX_MOBILITY",
                    itemTitle = "${selectedVehicle.name} ($distanceKm km)",
                    customerName = uiState.profile.name.ifBlank { uiState.displayName },
                    customerPhone = uiState.profile.phone,
                    userId = uiState.uid
                )
                if (!launched) {
                    pendingPayRef = null
                    viewModel.showMessage("Could not open payment browser. Try Wallet or Cash.")
                }
            }
            paymentMethod == "WALLET" -> submitBookingWithPayment("WALLET", "PAID", "WALLET", fareSummary.totalFare)
            else -> submitBookingWithPayment("CASH", "PAY_ON_DROP", "CASH", fareSummary.totalFare)
        }
    }

    Scaffold(
        containerColor = TakeItBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            TakeIt3DFloatingNavBar(
                items = RideTab.entries.map { tab ->
                    val icon = when (tab) {
                        RideTab.BOOK -> Icons.Rounded.Home
                        RideTab.TRIPS -> Icons.Rounded.DirectionsBike
                        RideTab.WALLET -> Icons.Rounded.AccountBalanceWallet
                        RideTab.ACTIVITY -> Icons.Rounded.LocalOffer
                        RideTab.PROFILE -> Icons.Rounded.Person
                    }
                    Nav3DTabItem(
                        id = tab.name,
                        label = tab.label,
                        icon = icon,
                        selected = uiState.rideTab == tab,
                        testTag = "ride_nav_${tab.name.lowercase()}",
                        onClick = { viewModel.selectRideTab(tab) }
                    )
                },
                containerColor = RideCharcoal,
                activePodColor = RideYellow,
                activeContentColor = RideCharcoal,
                inactiveColor = Color(0xFF94A3B8),
                borderColor = RideYellow.copy(alpha = 0.4f)
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (uiState.rideTab) {
                RideTab.BOOK -> RideHomeAndBookingTab(
                    uiState = uiState,
                    rideVehicles = rideVehicles,
                    pickup = pickup,
                    drop = drop,
                    pickupLat = pickupLat,
                    pickupLng = pickupLng,
                    dropLat = dropLat,
                    dropLng = dropLng,
                    distanceKm = distanceKm,
                    selectedVehicle = selectedVehicle,
                    fareSummary = fareSummary,
                    bookingCategory = bookingCategory,
                    scheduledFor = scheduledFor,
                    couponCodeInput = couponCodeInput,
                    appliedCode = appliedCode,
                    paymentMethod = paymentMethod,
                    onSelectVehicle = { selectedVehicleCode = it },
                    onBookingCategoryChange = { bookingCategory = it },
                    onScheduledForChange = { scheduledFor = it },
                    onCouponInputChange = { couponCodeInput = it.uppercase() },
                    onApplyCoupon = {
                        appliedCode = couponCodeInput.trim().uppercase()
                        viewModel.showMessage(
                            if (fareSummary.discountAmount > 0 || appliedCode == "RIDE50") "Coupon $appliedCode applied!"
                            else "Checking coupon $appliedCode..."
                        )
                    },
                    onPaymentMethodChange = { paymentMethod = it },
                    onOpenLocationPicker = { showLocationSheet = true },
                    onOpenNotifications = { showNotificationsDialog = true },
                    onOpenProfile = { viewModel.selectRideTab(RideTab.PROFILE) },
                    onSelectSavedDrop = { name, lat, lng ->
                        drop = name
                        if (lat != 0.0) dropLat = lat
                        if (lng != 0.0) dropLng = lng
                    },
                    onConfirmBook = handleBookClick,
                    onCancelRide = viewModel::cancelMyRide,
                    onRateRide = viewModel::rateMyRide,
                    onTriggerSos = { ride -> sosTargetRide = ride },
                    onOpenReceipt = { ride -> receiptRide = ride }
                )

                RideTab.TRIPS -> RideMyTripsTab(
                    rides = uiState.myRides,
                    onCancelRide = viewModel::cancelMyRide,
                    onRateRide = viewModel::rateMyRide,
                    onOpenReceipt = { receiptRide = it },
                    onRebook = { old ->
                        pickup = old.pickupAddress
                        drop = old.dropAddress
                        if (old.pickupLat != 0.0) pickupLat = old.pickupLat
                        if (old.pickupLng != 0.0) pickupLng = old.pickupLng
                        if (old.dropLat != 0.0) dropLat = old.dropLat
                        if (old.dropLng != 0.0) dropLng = old.dropLng
                        selectedVehicleCode = old.vehicleType
                        viewModel.selectRideTab(RideTab.BOOK)
                    }
                )

                RideTab.WALLET -> CustomerWalletScreen(
                    uiState = uiState,
                    onBack = { viewModel.selectRideTab(RideTab.BOOK) },
                    onTopUpConfirmed = viewModel::recordWalletTopUp,
                    onShowMessage = viewModel::showMessage
                )

                RideTab.ACTIVITY -> RideActivityAndOffersTab(
                    uiState = uiState,
                    onApplyCoupon = { code ->
                        couponCodeInput = code
                        appliedCode = code
                        viewModel.selectRideTab(RideTab.BOOK)
                        viewModel.showMessage("Coupon $code applied to your next RideX booking!")
                    }
                )

                RideTab.PROFILE -> RideProfileTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    onOpenRidesTab = { viewModel.selectRideTab(RideTab.TRIPS) },
                    onOpenWalletTab = { viewModel.selectRideTab(RideTab.WALLET) },
                    onReplayOnboarding = { showOnboarding = true },
                    onSignOut = onSignOut
                )
            }
        }
    }

    if (showOnboarding) {
        RideOnboardingDialog(onFinish = { showOnboarding = false })
    }

    if (showLocationSheet) {
        RideLocationPickerDialog(
            initialPickup = pickup,
            initialDrop = drop,
            initialPickupLat = pickupLat,
            initialPickupLng = pickupLng,
            initialDropLat = dropLat,
            initialDropLng = dropLng,
            savedAddresses = uiState.addresses,
            onDismiss = { showLocationSheet = false },
            onConfirm = { pAddr, pLat, pLng, dAddr, dLat, dLng ->
                pickup = pAddr
                pickupLat = pLat
                pickupLng = pLng
                drop = dAddr
                dropLat = dLat
                dropLng = dLng
                showLocationSheet = false
            }
        )
    }

    if (showNotificationsDialog) {
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            containerColor = Color.White,
            title = { Text("RideX Notifications", fontWeight = FontWeight.ExtraBold, color = RideCharcoal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = RoundedCornerShape(14.dp), color = RideYellowSoft) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Flat 50% OFF on all rides! 🎉", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                            Text("Apply code RIDE50 before booking.", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                        }
                    }
                    uiState.myRides.take(3).forEach { r ->
                        Surface(shape = RoundedCornerShape(12.dp), color = TakeItBg) {
                            Column(Modifier.padding(10.dp).fillMaxWidth()) {
                                Text("${r.rideNumber} • ${r.status.replace("_", " ")}", fontWeight = FontWeight.Bold, color = RideCharcoal)
                                Text("Fare ₹${r.totalFare} • ${r.dropAddress}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showNotificationsDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = RideCharcoal)
                ) { Text("Got it", fontWeight = FontWeight.ExtraBold) }
            }
        )
    }

    sosTargetRide?.let { ride ->
        AlertDialog(
            onDismissRequest = { sosTargetRide = null },
            containerColor = Color(0xFF1F0A0A),
            titleContentColor = Color.White,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Warning, "Emergency SOS", tint = AdminCrimson)
                    Text("Emergency SOS Safety", fontWeight = FontWeight.ExtraBold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Tap below to alert RideX Safety Team, dial emergency helpline (${uiState.config.sosNumber}), or send SMS with your live location to trusted contacts.",
                        color = Color(0xFFFECACA),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Button(
                        onClick = {
                            viewModel.triggerRideSos(ride.id, true)
                            dialPhone(context, uiState.config.sosNumber)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson),
                        modifier = Modifier.fillMaxWidth().testTag("ridex_sos_trigger_btn")
                    ) {
                        Icon(Icons.Rounded.LocalPolice, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("TRIGGER SOS & CALL ${uiState.config.sosNumber}", fontWeight = FontWeight.ExtraBold)
                    }
                    if (uiState.emergencyContacts.isNotEmpty()) {
                        uiState.emergencyContacts.take(2).forEach { ec ->
                            OutlinedButton(
                                onClick = {
                                    viewModel.triggerRideSos(ride.id, true)
                                    val link = "https://www.openstreetmap.org/?mlat=${ride.pickupLat}&mlon=${ride.pickupLng}"
                                    sendSmsIntent(context, ec.phone, "EMERGENCY SOS! Ride ${ride.rideNumber} with ${ride.captainName} (${ride.vehicleNumber}). Live map: $link")
                                },
                                border = BorderStroke(1.dp, RideYellow),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Alert ${ec.name} via SMS", color = RideYellow, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val link = "https://www.openstreetmap.org/?mlat=${ride.pickupLat}&mlon=${ride.pickupLng}"
                            shareTextIntent(context, "My Live RideX Location", "Tracking my RideX trip ${ride.rideNumber} (${ride.vehicleNumber}): $link")
                        },
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Share Live Location", color = Color.White, fontWeight = FontWeight.Bold) }
                }
            },
            confirmButton = {
                TextButton(onClick = { sosTargetRide = null }) { Text("Close", color = RideYellow) }
            }
        )
    }

    receiptRide?.let { r ->
        AlertDialog(
            onDismissRequest = { receiptRide = null },
            containerColor = Color.White,
            title = { Text("Ride Receipt • ${r.rideNumber}", fontWeight = FontWeight.ExtraBold, color = RideCharcoal) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("${r.vehicleType} (${r.distanceKm} km • ${r.durationMin} min)", fontWeight = FontWeight.Bold, color = RideCharcoal)
                    Text("Pickup: ${r.pickupAddress}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                    Text("Drop: ${r.dropAddress}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                    HorizontalDivider()
                    FareRowLine("Base Fare", "₹${r.baseFare}")
                    FareRowLine("Distance Charge", "₹${r.distanceCharge}")
                    if (r.timeCharge > 0) FareRowLine("Time Charge", "₹${r.timeCharge}")
                    if (r.surchargeAmount > 0) FareRowLine("Peak / Night", "₹${r.surchargeAmount}")
                    FareRowLine("Platform Fee", "₹${r.platformFee}")
                    if (r.gstAmount > 0) FareRowLine("GST & Taxes", "₹${r.gstAmount}")
                    if (r.discountAmount > 0) FareRowLine("Discount (${r.couponCode})", "-₹${r.discountAmount}", highlight = TakeItGreen)
                    HorizontalDivider()
                    FareRowLine("Total (${r.paymentMethod})", "₹${r.totalFare}", bold = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        shareTextIntent(context, "RideX Receipt ${r.rideNumber}", "RideX Receipt ${r.rideNumber}\nRoute: ${r.pickupAddress} -> ${r.dropAddress}\nTotal Paid: ₹${r.totalFare} (${r.paymentMethod})")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = RideCharcoal)
                ) { Text("Share Receipt", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { receiptRide = null }) { Text("Close") } }
        )
    }
}

@Composable
private fun RideHomeAndBookingTab(
    uiState: TakeItUiState,
    rideVehicles: List<MobilityVehicleCategory>,
    pickup: String,
    drop: String,
    pickupLat: Double,
    pickupLng: Double,
    dropLat: Double,
    dropLng: Double,
    distanceKm: Int,
    selectedVehicle: MobilityVehicleCategory,
    fareSummary: RideFareSummary,
    bookingCategory: String,
    scheduledFor: String,
    couponCodeInput: String,
    appliedCode: String,
    paymentMethod: String,
    onSelectVehicle: (String) -> Unit,
    onBookingCategoryChange: (String) -> Unit,
    onScheduledForChange: (String) -> Unit,
    onCouponInputChange: (String) -> Unit,
    onApplyCoupon: () -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onOpenLocationPicker: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenProfile: () -> Unit,
    onSelectSavedDrop: (String, Double, Double) -> Unit,
    onConfirmBook: () -> Unit,
    onCancelRide: (String) -> Unit,
    onRateRide: (String, Int) -> Unit,
    onTriggerSos: (RideBooking) -> Unit,
    onOpenReceipt: (RideBooking) -> Unit
) {
    val context = LocalContext.current
    var showFareDetails by remember { mutableStateOf(false) }

    val activeRide = remember(uiState.myRides) {
        uiState.myRides.firstOrNull { it.status !in listOf("COMPLETED", "CANCELLED", "REFUNDED") }
    }
    val justCompletedUnrated = remember(uiState.myRides) {
        uiState.myRides.firstOrNull { it.status == "COMPLETED" && it.rating == 0 }
    }

    TakeIt3DBackdrop(RideYellow, RideCharcoal, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Dark Navy + Yellow Branded Header
            item {
                TakeIt3DCard(
                    containerColor = RideCharcoal,
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Hello, ${uiState.profile.name.ifBlank { uiState.displayName }.substringBefore(" ")} 👋",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onOpenLocationPicker() }
                                ) {
                                    Icon(Icons.Rounded.LocationOn, null, tint = RideYellow, modifier = Modifier.size(15.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        uiState.config.cityName,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = RideYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = RideYellow, modifier = Modifier.size(16.dp))
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = onOpenNotifications, modifier = Modifier.testTag("ridex_notifications_btn")) {
                                    Icon(Icons.Rounded.Notifications, "Notifications", tint = Color.White)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(RideYellow)
                                        .clickable { onOpenProfile() }
                                        .testTag("ridex_profile_avatar_btn"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TakeItMediaVisual(
                                        imageUrl = TakeItFreeMedia.avatarAndSupportCatalog[0],
                                        key = "rider_avatar_top",
                                        contentDescription = "Profile",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                                    )
                                }
                            }
                        }

                        // "Where are you going?" interactive Search CTA
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpenLocationPicker() }
                                .testTag("ridex_where_going_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Rounded.Search, "Search destination", tint = RideCharcoal)
                                Text(
                                    drop.ifBlank { "Where are you going?" },
                                    color = if (drop.isBlank()) TakeItMuted else RideCharcoal,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Surface(shape = RoundedCornerShape(8.dp), color = RideYellowSoft) {
                                    Text("$distanceKm km", color = RideCharcoal, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Active Ride States (Finding Captain Radar OR Live Tracking Map + SOS + OTP)
            if (activeRide != null) {
                item {
                    if (activeRide.status == "REQUESTED") {
                        FindingCaptainRadarCard(
                            ride = activeRide,
                            onCancel = { onCancelRide(activeRide.id) }
                        )
                    } else {
                        ActiveRideTrackingCard(
                            ride = activeRide,
                            onCall = { dialPhone(context, activeRide.captainPhone.ifBlank { uiState.config.supportPhone }) },
                            onSms = { sendSmsIntent(context, activeRide.captainPhone, "Hi ${activeRide.captainName}, my Ride OTP is ${activeRide.otp}") },
                            onShareLocation = {
                                val link = "https://www.openstreetmap.org/?mlat=${activeRide.pickupLat}&mlon=${activeRide.pickupLng}"
                                shareTextIntent(context, "Live RideX Trip ${activeRide.rideNumber}", "Captain ${activeRide.captainName} (${activeRide.vehicleNumber}). Track pickup: $link")
                            },
                            onSos = { onTriggerSos(activeRide) }
                        )
                    }
                }
            }

            // Recent Completed Unrated Ride Banner
            if (justCompletedUnrated != null) {
                item {
                    TakeIt3DCard(
                        containerColor = Color(0xFF052E16),
                        depthColor = Color.Black,
                        borderColor = TakeItGreen,
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, "Ride Completed", tint = TakeItGreen, modifier = Modifier.size(44.dp))
                            Text("Ride Completed! • ₹${justCompletedUnrated.totalFare}", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text("Rate Captain ${justCompletedUnrated.captainName}", style = MaterialTheme.typography.labelMedium, color = Color(0xFFBBF7D0))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                (1..5).forEach { stars ->
                                    IconButton(onClick = { onRateRide(justCompletedUnrated.id, stars) }) {
                                        Icon(Icons.Rounded.Star, "Rate $stars", tint = RideYellow, modifier = Modifier.size(30.dp))
                                    }
                                }
                            }
                            OutlinedButton(
                                onClick = { onOpenReceipt(justCompletedUnrated) },
                                border = BorderStroke(1.dp, RideYellow)
                            ) { Text("View Digital Receipt", color = RideYellow, fontWeight = FontWeight.Bold) }
                        }
                    }
                }
            }

            // Promo Banner ("Flat 50% OFF on your next ride • Code: RIDE50")
            item {
                TakeIt3DCard(
                    containerColor = RideCharcoal,
                    depthColor = Color.Black,
                    borderColor = RideYellow,
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    onClick = {
                        onCouponInputChange("RIDE50")
                        onApplyCoupon()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(RideCharcoal, Color(0xFF1E293B))))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Surface(shape = RoundedCornerShape(6.dp), color = RideYellow) {
                                Text("Use Code: RIDE50", color = RideCharcoal, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                            }
                            Text("Flat 50% OFF", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                            Text("on your next ride • Tap to apply", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                        }
                        TakeItMediaVisual(
                            imageUrl = TakeItFreeMedia.rideMediaCatalog[3],
                            key = "promo_car",
                            contentDescription = "RideX Promo",
                            modifier = Modifier.size(72.dp)
                        )
                    }
                }
            }

            // 6 Quick Ride Categories (Bike, Auto, Cab, Premium, Parcel, Rental)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Ride Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(quickModeShortcuts) { (vehCode, label, sub) ->
                            val selected = (label == "Rental" && bookingCategory == "Rental") ||
                                (label == "Parcel" && bookingCategory == "Parcel") ||
                                (label !in listOf("Rental", "Parcel") && selectedVehicle.code.equals(vehCode, ignoreCase = true) && bookingCategory == "Ride Now")
                            TakeIt3DCard(
                                onClick = {
                                    onSelectVehicle(vehCode)
                                    onBookingCategoryChange(
                                        when (label) {
                                            "Rental" -> "Rental"
                                            "Parcel" -> "Parcel"
                                            else -> "Ride Now"
                                        }
                                    )
                                },
                                containerColor = if (selected) RideYellowSoft else Color.White,
                                depthColor = if (selected) RideYellow.darken3D(0.4f) else Color(0xFFD5DCE6),
                                borderColor = if (selected) RideCharcoal else TakeItBorder,
                                cornerRadius = 18.dp,
                                depth = 4.dp,
                                modifier = Modifier.width(96.dp).testTag("ridex_cat_${label.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    TakeItMediaVisual(
                                        imageUrl = TakeItFreeMedia.resolveVehicleMediaUrl(vehCode, false),
                                        key = vehCode,
                                        contentDescription = label,
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Text(label, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = RideCharcoal, maxLines = 1)
                                    Text(sub, fontSize = 9.sp, color = TakeItMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Popular Destinations (Home, Work, Favourites & Recent Drops)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Popular Destinations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        item {
                            val homeAddr = uiState.addresses.firstOrNull { it.label.equals("Home", true) }
                            SavedPlaceChip(
                                icon = Icons.Rounded.Home,
                                title = "Home",
                                sub = homeAddr?.areaStreet?.ifBlank { "Sector 62" } ?: "Sector 62",
                                onClick = {
                                    onSelectSavedDrop(
                                        homeAddr?.fullAddress ?: "Home • Sector 62, Greater Noida",
                                        homeAddr?.latitude ?: 28.4812,
                                        homeAddr?.longitude ?: 77.5125
                                    )
                                }
                            )
                        }
                        item {
                            val workAddr = uiState.addresses.firstOrNull { it.label.equals("Work", true) }
                            SavedPlaceChip(
                                icon = Icons.Rounded.Work,
                                title = "Work",
                                sub = workAddr?.areaStreet?.ifBlank { "Knowledge Park II" } ?: "Knowledge Park II",
                                onClick = {
                                    onSelectSavedDrop(
                                        workAddr?.fullAddress ?: "Work • Knowledge Park II, Greater Noida",
                                        workAddr?.latitude ?: 28.4631,
                                        workAddr?.longitude ?: 77.4981
                                    )
                                }
                            )
                        }
                        item {
                            SavedPlaceChip(
                                icon = Icons.Rounded.LocalMall,
                                title = "DLF / Mall",
                                sub = "4.8 km",
                                onClick = { onSelectSavedDrop("Grand Venice / DLF Mall", 28.4552, 77.5260) }
                            )
                        }
                    }
                }
            }

            // Route Card & Leaflet Preview Trigger
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Pickup & Drop Route", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                            TextButton(onClick = onOpenLocationPicker, modifier = Modifier.testTag("ridex_edit_map_btn")) {
                                Icon(Icons.Rounded.Map, null, tint = RideBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Adjust on Map", color = RideBlue, fontWeight = FontWeight.Bold)
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TakeItGreen))
                            Text(pickup, style = MaterialTheme.typography.bodySmall, color = TakeItMuted, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(TakeItRed))
                            Text(drop, style = MaterialTheme.typography.bodySmall, color = RideCharcoal, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        }

                        // Ride Now vs Schedule for Later
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Ride Now", "Scheduled").forEach { mode ->
                                val active = bookingCategory == mode || (bookingCategory !in listOf("Scheduled") && mode == "Ride Now")
                                FilterChip(
                                    selected = active,
                                    onClick = { onBookingCategoryChange(mode) },
                                    label = { Text(mode, fontWeight = FontWeight.Bold) }
                                )
                            }
                        }
                        if (bookingCategory == "Scheduled") {
                            OutlinedTextField(
                                value = scheduledFor,
                                onValueChange = onScheduledForChange,
                                label = { Text("Pickup Date & Time") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Dynamic Vehicle Selection List
            item {
                Text(
                    "Select Vehicle ($distanceKm km • ~${fareSummary.durationMin} min)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = RideCharcoal,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(rideVehicles, key = { "rveh_${it.id}_${it.code}" }) { veh ->
                val isSelected = veh.code.equals(selectedVehicle.code, ignoreCase = true)
                val est = computeRideFare(veh, distanceKm, uiState.config, appliedCode, uiState.coupons)
                val imgUrl = veh.imageUrl.ifBlank { TakeItFreeMedia.resolveVehicleMediaUrl(veh.code, false) }

                TakeIt3DCard(
                    onClick = { onSelectVehicle(veh.code) },
                    containerColor = if (isSelected) RideYellowSoft else Color.White,
                    depthColor = if (isSelected) RideYellow.darken3D(0.4f) else Color(0xFFD5DCE6),
                    borderColor = if (isSelected) RideCharcoal else TakeItBorder,
                    cornerRadius = 18.dp,
                    depth = if (isSelected) 5.dp else 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("ridex_vehicle_${veh.code.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White)
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            TakeItMediaVisual(
                                imageUrl = imgUrl,
                                key = veh.code,
                                contentDescription = veh.name,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(veh.name, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                            Text("${veh.etaMinutes} min • ${veh.capacityLabel} • ${veh.subtitle}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${est.totalFare}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                            if (est.discountAmount > 0) {
                                Text("Saved ₹${est.discountAmount}", style = MaterialTheme.typography.labelSmall, color = TakeItGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Fare Details + Coupon + Payment Method + Confirm CTA
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable { showFareDetails = !showFareDetails },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Fare Details • ₹${fareSummary.totalFare}", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (showFareDetails) "Hide" else "Breakdown", style = MaterialTheme.typography.labelSmall, color = RideBlue, fontWeight = FontWeight.Bold)
                                Icon(if (showFareDetails) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, null, tint = RideBlue)
                            }
                        }

                        AnimatedVisibility(visible = showFareDetails) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                FareRowLine("Base Fare", "₹${fareSummary.baseFare}")
                                FareRowLine("Distance Charge ($distanceKm km)", "₹${fareSummary.distanceCharge}")
                                FareRowLine("Time Charge (${fareSummary.durationMin} min)", "₹${fareSummary.timeCharge}")
                                if (fareSummary.surchargeAmount > 0) FareRowLine("Peak Multiplier Surge", "₹${fareSummary.surchargeAmount}")
                                FareRowLine("Platform Fee", "₹${fareSummary.platformFee}")
                                FareRowLine("GST (${uiState.config.gstPercent}%)", "₹${fareSummary.gstAmount}")
                                if (fareSummary.discountAmount > 0) FareRowLine("Coupon ($appliedCode)", "-₹${fareSummary.discountAmount}", highlight = TakeItGreen)
                            }
                        }

                        // Coupon input
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = couponCodeInput,
                                onValueChange = onCouponInputChange,
                                label = { Text("Coupon Code (e.g. RIDE50)") },
                                singleLine = true,
                                modifier = Modifier.weight(1f).testTag("ridex_coupon_input")
                            )
                            Button(
                                onClick = onApplyCoupon,
                                colors = ButtonDefaults.buttonColors(containerColor = RideCharcoal, contentColor = RideYellow),
                                modifier = Modifier.testTag("ridex_coupon_apply_btn")
                            ) { Text("Apply", fontWeight = FontWeight.ExtraBold) }
                        }

                        // Payment Selector
                        Text("Payment Method", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("RAZORPAY" to "UPI / Card", "WALLET" to "Wallet ₹${uiState.profile.walletBalance.toInt()}", "CASH" to "Cash").forEach { (key, title) ->
                                FilterChip(
                                    selected = paymentMethod == key,
                                    onClick = { onPaymentMethodChange(key) },
                                    label = { Text(title, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f).testTag("ridex_pay_${key.lowercase()}")
                                )
                            }
                        }

                        TakeIt3DButton(
                            onClick = onConfirmBook,
                            containerColor = RideYellow,
                            depthColor = RideYellow.darken3D(0.4f),
                            contentColor = RideCharcoal,
                            cornerRadius = 16.dp,
                            depth = 5.dp,
                            height = 54.dp,
                            modifier = Modifier.fillMaxWidth().testTag("ridex_book_confirm_btn")
                        ) {
                            Text(
                                "Book ${selectedVehicle.name} • ₹${fareSummary.totalFare}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SavedPlaceChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    sub: String,
    onClick: () -> Unit
) {
    TakeIt3DCard(
        onClick = onClick,
        containerColor = Color.White,
        depthColor = Color(0xFFD5DCE6),
        cornerRadius = 16.dp,
        depth = 3.dp,
        modifier = Modifier.width(136.dp)
    ) {
        Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TakeIt3DIconOrb(icon = icon, containerColor = RideCharcoal, iconTint = RideYellow, size = 32.dp)
            Column {
                Text(title, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp, color = RideCharcoal)
                Text(sub, style = MaterialTheme.typography.labelSmall, color = TakeItMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FindingCaptainRadarCard(
    ride: RideBooking,
    onCancel: () -> Unit
) {
    val inf = rememberInfiniteTransition(label = "captainRadar")
    val pulse by inf.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "radarPulse"
    )

    TakeIt3DCard(
        containerColor = RideCharcoal,
        depthColor = Color.Black,
        borderColor = RideYellow,
        cornerRadius = 24.dp,
        depth = 6.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(modifier = Modifier.size(136.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val c = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(color = RideYellow.copy(alpha = 0.16f), radius = size.minDimension * 0.48f, center = c, style = Stroke(3f))
                    drawCircle(color = RideYellow.copy(alpha = 0.25f), radius = size.minDimension * 0.34f, center = c, style = Stroke(3f))
                    drawCircle(color = RideYellow.copy(alpha = (1f - pulse) * 0.65f), radius = (size.minDimension * 0.48f) * pulse, center = c, style = Stroke(5f))
                }
                TakeIt3DIconOrb(Icons.Rounded.TwoWheeler, RideYellow, iconTint = RideCharcoal, size = 54.dp)
            }
            Text("Finding Captain...", style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
            Text("Please wait while nearby Captains respond • ${ride.rideNumber} (${ride.vehicleType})", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
            Surface(shape = RoundedCornerShape(50), color = RideYellowSoft) {
                Text("Ride Start OTP: ${ride.otp}", color = RideCharcoal, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp))
            }
            OutlinedButton(
                onClick = onCancel,
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth().testTag("ridex_cancel_searching_btn")
            ) {
                Text("Cancel Ride", color = Color.White, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActiveRideTrackingCard(
    ride: RideBooking,
    onCall: () -> Unit,
    onSms: () -> Unit,
    onShareLocation: () -> Unit,
    onSos: () -> Unit
) {
    val progress = when (ride.status) {
        "CAPTAIN_ASSIGNED" -> 0.35f
        "ARRIVING" -> 0.6f
        "ON_TRIP" -> 0.85f
        else -> 0.2f
    }

    TakeIt3DCard(
        containerColor = Color.White,
        depthColor = Color(0xFFD5DCE6),
        borderColor = RideYellow,
        cornerRadius = 24.dp,
        depth = 6.dp,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text(
                        if (ride.status == "ON_TRIP") "On Trip to Destination" else "Captain Arriving in ${ride.etaMinutes} min",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = RideCharcoal
                    )
                    Text("${ride.rideNumber} • ${ride.vehicleType}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = RideCharcoal) {
                    Text("OTP ${ride.otp}", color = RideYellow, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }

            Box(modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(18.dp))) {
                LeafletTrackingMap(
                    pickupLat = ride.pickupLat,
                    pickupLng = ride.pickupLng,
                    pickupTitle = "Pickup: ${ride.pickupAddress}",
                    dropLat = ride.dropLat,
                    dropLng = ride.dropLng,
                    dropTitle = "Drop: ${ride.dropAddress}",
                    riderLabel = ride.captainName,
                    progressFraction = progress,
                    accentHex = "#F59E0B",
                    modifier = Modifier.fillMaxSize()
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TakeItMediaVisual(
                    imageUrl = TakeItFreeMedia.avatarAndSupportCatalog[2],
                    key = "captain_avatar",
                    contentDescription = "Captain",
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(ride.captainName, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                    Text(ride.vehicleNumber.ifBlank { ride.vehicleType }, style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                }
                IconButton(onClick = onCall, modifier = Modifier.clip(CircleShape).background(TakeItGreenSoft)) {
                    Icon(Icons.Rounded.Call, "Call Captain", tint = TakeItGreen)
                }
                IconButton(onClick = onSms, modifier = Modifier.clip(CircleShape).background(RideYellowSoft)) {
                    Icon(Icons.Rounded.Sms, "SMS Captain", tint = RideCharcoal)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onShareLocation,
                    border = BorderStroke(1.dp, RideCharcoal),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.ShareLocation, null, modifier = Modifier.size(16.dp), tint = RideCharcoal)
                    Spacer(Modifier.width(4.dp))
                    Text("Share Live", color = RideCharcoal, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Button(
                    onClick = onSos,
                    colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson, contentColor = Color.White),
                    modifier = Modifier.weight(1f).testTag("ridex_live_sos_btn")
                ) {
                    Icon(Icons.Rounded.Warning, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("SOS Emergency", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun RideMyTripsTab(
    rides: List<RideBooking>,
    onCancelRide: (String) -> Unit,
    onRateRide: (String, Int) -> Unit,
    onOpenReceipt: (RideBooking) -> Unit,
    onRebook: (RideBooking) -> Unit
) {
    var filter by remember { mutableStateOf("Upcoming") }
    val filtered = remember(rides, filter) {
        when (filter) {
            "Upcoming" -> rides.filter { it.status !in listOf("COMPLETED", "CANCELLED", "REFUNDED") }
            "Completed" -> rides.filter { it.status == "COMPLETED" }
            else -> rides.filter { it.status in listOf("CANCELLED", "REFUNDED") }
        }
    }

    TakeIt3DBackdrop(RideYellow, RideCharcoal, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("My Rides", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Upcoming", "Completed", "Cancelled").forEach { tab ->
                        FilterChip(
                            selected = filter == tab,
                            onClick = { filter = tab },
                            label = { Text(tab, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f).testTag("ridex_trips_${tab.lowercase()}")
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 20.dp, depth = 4.dp) {
                        Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TakeIt3DIconOrb(Icons.Rounded.DirectionsBike, RideCharcoal, iconTint = RideYellow, size = 56.dp)
                            Text("No $filter rides yet", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                        }
                    }
                }
            } else {
                items(filtered, key = { "trip_${it.id}" }) { r ->
                    TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 20.dp, depth = 4.dp) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("${r.rideNumber} • ${r.vehicleType}", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                                Surface(shape = RoundedCornerShape(50), color = if (r.status == "COMPLETED") TakeItGreenSoft else RideYellowSoft) {
                                    Text(r.status.replace("_", " "), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold, color = if (r.status == "COMPLETED") TakeItGreen else RideCharcoal, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                            Text("From: ${r.pickupAddress}", style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                            Text("To: ${r.dropAddress}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = RideCharcoal)
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("OTP: ${r.otp} • ${r.captainName}", style = MaterialTheme.typography.labelSmall, color = RideBlue)
                                Text("₹${r.totalFare} (${r.paymentMethod})", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onOpenReceipt(r) }, modifier = Modifier.weight(1f)) {
                                    Text("Receipt", fontWeight = FontWeight.Bold)
                                }
                                if (r.status == "REQUESTED") {
                                    Button(onClick = { onCancelRide(r.id) }, colors = ButtonDefaults.buttonColors(containerColor = TakeItRed), modifier = Modifier.weight(1f)) {
                                        Text("Cancel", fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Button(onClick = { onRebook(r) }, colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = RideCharcoal), modifier = Modifier.weight(1f)) {
                                        Text("Rebook", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            if (r.status == "COMPLETED" && r.rating == 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Rate:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                    (1..5).forEach { s ->
                                        IconButton(onClick = { onRateRide(r.id, s) }, modifier = Modifier.size(32.dp)) {
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
private fun RideLocationPickerDialog(
    initialPickup: String,
    initialDrop: String,
    initialPickupLat: Double,
    initialPickupLng: Double,
    initialDropLat: Double,
    initialDropLng: Double,
    savedAddresses: List<SavedAddress>,
    onDismiss: () -> Unit,
    onConfirm: (String, Double, Double, String, Double, Double) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var editingTarget by remember { mutableStateOf("DROP") } // PICKUP or DROP
    var pickup by remember { mutableStateOf(initialPickup) }
    var drop by remember { mutableStateOf(initialDrop) }
    var pLat by remember { mutableDoubleStateOf(initialPickupLat) }
    var pLng by remember { mutableDoubleStateOf(initialPickupLng) }
    var dLat by remember { mutableDoubleStateOf(initialDropLat) }
    var dLng by remember { mutableDoubleStateOf(initialDropLng) }

    val activeLat = if (editingTarget == "PICKUP") pLat else dLat
    val activeLng = if (editingTarget == "PICKUP") pLng else dLng

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        title = { Text("Select Pickup & Drop on Map", fontWeight = FontWeight.ExtraBold, color = RideCharcoal) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(selected = editingTarget == "PICKUP", onClick = { editingTarget = "PICKUP" }, label = { Text("Pin Pickup") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = editingTarget == "DROP", onClick = { editingTarget = "DROP" }, label = { Text("Pin Drop") }, modifier = Modifier.weight(1f))
                }
                Box(modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(14.dp))) {
                    LeafletAddressPickerMap(
                        selectedLat = activeLat,
                        selectedLng = activeLng,
                        onLocationPicked = { lat, lng ->
                            reverseGeocodeCoordinates(context, scope, lat, lng) { rLat, rLng, _, _, full, _ ->
                                if (editingTarget == "PICKUP") {
                                    pLat = rLat; pLng = rLng; pickup = full
                                } else {
                                    dLat = rLat; dLng = rLng; drop = full
                                }
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
                            autofetchCurrentAddress(
                                context = context,
                                scope = scope,
                                onSuccess = { lat, lng, _, _, full, _ ->
                                    pLat = lat; pLng = lng; pickup = full
                                },
                                onError = {}
                            )
                        }) { Icon(Icons.Rounded.MyLocation, "Current GPS", tint = RideBlue) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = drop,
                    onValueChange = { drop = it },
                    label = { Text("Drop Destination") },
                    singleLine = true,
                    trailingIcon = {
                        IconButton(onClick = {
                            val tmpAddr = pickup; val tmpLat = pLat; val tmpLng = pLng
                            pickup = drop; pLat = dLat; pLng = dLng
                            drop = tmpAddr; dLat = tmpLat; dLng = tmpLng
                        }) { Icon(Icons.Rounded.SwapVert, "Swap Pickup/Drop", tint = RideCharcoal) }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (savedAddresses.isNotEmpty()) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(savedAddresses.take(4)) { addr ->
                            SuggestionChip(
                                onClick = {
                                    val full = listOf(addr.fullAddress, addr.city).filter { it.isNotBlank() }.joinToString(", ")
                                    if (editingTarget == "PICKUP") {
                                        pickup = full
                                        if (addr.latitude != 0.0) pLat = addr.latitude
                                        if (addr.longitude != 0.0) pLng = addr.longitude
                                    } else {
                                        drop = full
                                        if (addr.latitude != 0.0) dLat = addr.latitude
                                        if (addr.longitude != 0.0) dLng = addr.longitude
                                    }
                                },
                                label = { Text(addr.label) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(pickup, pLat, pLng, drop, dLat, dLng) },
                colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = RideCharcoal)
            ) { Text("Confirm Location (${calculateHaversineKm(pLat, pLng, dLat, dLng)} km)", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun FareRowLine(label: String, value: String, bold: Boolean = false, highlight: Color = RideCharcoal) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = if (bold) RideCharcoal else TakeItMuted, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Normal)
        Text(value, style = MaterialTheme.typography.bodySmall, color = highlight, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}
