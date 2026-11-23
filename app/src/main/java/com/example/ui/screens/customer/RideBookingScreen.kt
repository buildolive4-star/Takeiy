package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DemoCatalog
import com.example.model.VehicleOption
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState
import kotlinx.coroutines.delay

private val RideNavyHeader = Color(0xFF0B1F4B)
private val RideRoyalBlue = Color(0xFF2563EB)
private val RideSelectedBg = Color(0xFFEFF6FF)
private val RideYellowAccent = Color(0xFFFFD43B)

private enum class RidePhase {
    SELECT_ROUTE_VEHICLE,
    MATCHING_DRIVER,
    CAPTAIN_ASSIGNED,
    COMPLETED_SUMMARY
}

private data class RideModeTab(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val defaultVehicleId: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideBookingScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onRecordRide: (title: String, subtitle: String, amount: Double, payment: String, pickup: String, drop: String, otp: String, driver: String, onCreated: (String) -> Unit) -> Unit,
    onCompleteRide: (String) -> Unit,
    onRateRide: (String, Int) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val modeTabs = remember {
        listOf(
            RideModeTab("bike", "Bike", Icons.Rounded.TwoWheeler, "bike"),
            RideModeTab("auto", "Auto", Icons.Rounded.ElectricRickshaw, "auto"),
            RideModeTab("cab", "Cab", Icons.Rounded.DirectionsCar, "mini"),
            RideModeTab("share", "Share", Icons.Rounded.Groups, "sedan")
        )
    }

    var phase by remember { mutableStateOf(RidePhase.SELECT_ROUTE_VEHICLE) }
    var selectedModeTab by remember { mutableStateOf("bike") }
    var pickup by remember { mutableStateOf("Current Location") }
    var destination by remember { mutableStateOf("DLF Mall, Noida") }
    var selectedVehicle by remember { mutableStateOf(DemoCatalog.rideVehicles.first()) }
    val paymentMethods = remember { listOf("UPI •••• 1234", "Take It Wallet", "Cash", "Card") }
    var selectedPayment by remember { mutableStateOf(paymentMethods.first()) }
    var scheduleSlot by remember { mutableStateOf("Now") }
    var promoApplied by remember { mutableStateOf(false) }
    var showDestinationModal by remember { mutableStateOf(false) }
    var showAllVehicles by remember { mutableStateOf(false) }
    var mapPinFineTune by remember { mutableStateOf("Main Gate Pin Adjusted") }
    var otpInput by remember { mutableStateOf("") }
    val ridePin = remember { "4821" }
    var activeOrderId by remember { mutableStateOf("") }
    var driverProgress by remember { mutableFloatStateOf(0.16f) }
    var rideStepIndex by remember { mutableIntStateOf(1) }
    var userRating by remember { mutableIntStateOf(5) }
    var feedbackNote by remember { mutableStateOf("") }
    var selectedCompliments by remember { mutableStateOf(setOf("Safe Driving", "Clean Helmet")) }
    var showSosDialog by remember { mutableStateOf(false) }

    val rideTimelineSteps = remember {
        listOf(
            "Searching Nearby Captains",
            "Driver Assigned • Arjun Kumar",
            "Driver Arriving (2 mins away)",
            "Driver Arrived at Pickup",
            "Ride Started • En Route",
            "Ride Completed"
        )
    }

    val quickDestinations = remember {
        listOf(
            "DLF Mall, Noida" to 3.2,
            "Sector 18 Metro" to 4.1,
            "Electronic City Noida" to 5.4,
            "Botanical Garden" to 6.8,
            "IGI Airport T3" to 31.5
        )
    }
    val distanceKm = quickDestinations.find { it.first == destination }?.second ?: 3.2
    val etaMin = (distanceKm * 3.7).toInt().coerceAtLeast(10)
    val baseComponent = selectedVehicle.baseFare
    val distanceComponent = (distanceKm * selectedVehicle.perKmRate).toInt()
    val rawFare = baseComponent + distanceComponent
    val taxComponent = (rawFare * 0.05).toInt().coerceAtLeast(2)
    val discount = if (promoApplied && rawFare > 40) 15 else 0
    val estimatedFare = (rawFare + taxComponent - discount).coerceAtLeast(29)

    val displayedVehicles = remember(showAllVehicles, selectedModeTab) {
        if (showAllVehicles) {
            DemoCatalog.rideVehicles
        } else {
            listOf(
                DemoCatalog.rideVehicles[0], // Bike
                DemoCatalog.rideVehicles[1], // Auto
                DemoCatalog.rideVehicles[3]  // Mini Cab
            )
        }
    }

    LaunchedEffect(phase) {
        when (phase) {
            RidePhase.MATCHING_DRIVER -> {
                rideStepIndex = 0
                delay(1800)
                onRecordRide(
                    "${selectedVehicle.name} • $destination",
                    "${"%.1f".format(distanceKm)} km • $etaMin min • $scheduleSlot • Captain Arjun",
                    estimatedFare.toDouble(),
                    selectedPayment,
                    pickup,
                    destination,
                    ridePin,
                    "Arjun Kumar"
                ) { id -> activeOrderId = id }
                rideStepIndex = 2
                phase = RidePhase.CAPTAIN_ASSIGNED
            }
            RidePhase.CAPTAIN_ASSIGNED -> {
                driverProgress = 0.16f
                while (driverProgress < 0.92f && phase == RidePhase.CAPTAIN_ASSIGNED) {
                    delay(420)
                    driverProgress = (driverProgress + 0.04f).coerceAtMost(0.92f)
                    if (driverProgress > 0.55f && rideStepIndex < 3) rideStepIndex = 3
                    if (driverProgress > 0.80f && rideStepIndex < 4) rideStepIndex = 4
                }
            }
            RidePhase.COMPLETED_SUMMARY -> {
                rideStepIndex = 5
            }
            else -> Unit
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
    ) {
        // 1. DEEP NAVY TOP BAR + FLOATING ROUNDED MODE TABS (Matches Screen 4 Mockup)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(RideNavyHeader, Color(0xFF102A63)))
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).testTag("back_button")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = SoftWhite
                        )
                    }
                    Text(
                        text = "Book a Ride",
                        style = MaterialTheme.typography.titleLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(
                        onClick = { showSosDialog = true },
                        modifier = Modifier.size(48.dp).testTag("ride_sos_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(SoftWhite.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Shield,
                                contentDescription = "Safety SOS",
                                tint = SoftWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Rounded White Pill Mode Switcher (Bike | Auto | Cab | Share)
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SoftWhite,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp)
                        .padding(top = 4.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(5.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        modeTabs.forEach { tab ->
                            val isSelected = selectedModeTab == tab.id
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSelected) RideRoyalBlue else Color.Transparent)
                                    .clickable {
                                        selectedModeTab = tab.id
                                        DemoCatalog.rideVehicles.find { it.id == tab.defaultVehicleId }?.let {
                                            selectedVehicle = it
                                        }
                                        if (tab.id == "share") showAllVehicles = true
                                    }
                                    .padding(vertical = 8.dp)
                                    .testTag("ride_mode_${tab.id}")
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = tab.label,
                                    tint = if (isSelected) SoftWhite else DeepNavy,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = tab.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isSelected) SoftWhite else DeepNavy,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. SCROLLABLE MAP + BOTTOM SHEET CONTENT
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // Light Street Map with Route & "3.2 km / 12 min" Callout
            item {
                SimulatedMapCanvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(235.dp),
                    pickupLabel = pickup,
                    dropLabel = destination,
                    distanceKm = distanceKm,
                    etaMinutes = etaMin,
                    driverProgress = if (phase == RidePhase.CAPTAIN_ASSIGNED) driverProgress else -1f,
                    showMovingDriver = phase != RidePhase.MATCHING_DRIVER,
                    showMatchingRadar = phase == RidePhase.MATCHING_DRIVER
                )
            }

            // Crisp White Rounded Sheet overlapping map
            item {
                Surface(
                    shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
                    color = SoftWhite,
                    shadowElevation = 10.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = (-14).dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AnimatedContent(targetState = phase, label = "takeItRidePhase") { current ->
                            when (current) {
                                RidePhase.SELECT_ROUTE_VEHICLE -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        // Clean Pickup & Destination Card with Swap Icon (Screen 4 exact look)
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = SoftWhite,
                                            shadowElevation = 2.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(end = 12.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF16A34A))
                                                            .border(2.5.dp, Color(0xFFBBF7D0), CircleShape)
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .width(1.5.dp)
                                                            .height(26.dp)
                                                            .background(Color(0xFFCBD5E1))
                                                    )
                                                    Box(
                                                        modifier = Modifier
                                                            .size(12.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFEF4444))
                                                            .border(2.5.dp, Color(0xFFFECACA), CircleShape)
                                                    )
                                                }
                                                Column(
                                                    modifier = Modifier.weight(1f),
                                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { showDestinationModal = true }
                                                            .padding(vertical = 4.dp)
                                                            .testTag("ride_pickup_input"),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = pickup,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = DeepNavy,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                    }
                                                    HorizontalDivider(color = Color(0xFFF1F5F9))
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .clickable { showDestinationModal = true }
                                                            .padding(vertical = 4.dp)
                                                            .testTag("ride_destination_input"),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = destination,
                                                            style = MaterialTheme.typography.titleSmall,
                                                            color = DeepNavy,
                                                            fontWeight = FontWeight.Bold,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                        Text(
                                                            text = "Change",
                                                            style = MaterialTheme.typography.labelSmall,
                                                            color = RideRoyalBlue,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val tmp = pickup
                                                        pickup = destination
                                                        destination = tmp
                                                    },
                                                    modifier = Modifier.size(44.dp)
                                                ) {
                                                    Icon(
                                                        Icons.Rounded.SwapVert,
                                                        contentDescription = "Swap pickup and drop",
                                                        tint = RideRoyalBlue
                                                    )
                                                }
                                            }
                                        }

                                        // Quick Destination & Schedule Pills
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(listOf("Now", "6:00 PM", "9:00 AM")) { slot ->
                                                val sel = scheduleSlot == slot
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(if (sel) RideSelectedBg else Color(0xFFF8FAFC))
                                                        .border(1.dp, if (sel) RideRoyalBlue else Color(0xFFE2E8F0), RoundedCornerShape(50))
                                                        .clickable { scheduleSlot = slot }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Rounded.Schedule, null, tint = if (sel) RideRoyalBlue else SlateText, modifier = Modifier.size(13.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(slot, style = MaterialTheme.typography.labelSmall, color = if (sel) RideRoyalBlue else DeepNavy, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                            items(quickDestinations.take(4)) { (place, km) ->
                                                val sel = destination == place
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(if (sel) RideNavyHeader else Color(0xFFF8FAFC))
                                                        .border(1.dp, if (sel) RideNavyHeader else Color(0xFFE2E8F0), RoundedCornerShape(50))
                                                        .clickable { destination = place }
                                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                                ) {
                                                    Text(
                                                        "$place • ${km}km",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (sel) RideYellowAccent else DeepNavy,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                }
                                            }
                                        }

                                        // Vehicle Options (Blue-bordered selected card with checkmark, Screen 4 look)
                                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                            displayedVehicles.forEach { v ->
                                                val isSel = selectedVehicle.id == v.id
                                                val vRaw = v.baseFare + (distanceKm * v.perKmRate).toInt()
                                                val vTax = (vRaw * 0.05).toInt().coerceAtLeast(2)
                                                val vFinal = (vRaw + vTax - (if (promoApplied && vRaw > 40) 15 else 0)).coerceAtLeast(29)
                                                TakeItRideVehicleCard(
                                                    vehicle = v,
                                                    etaMinutes = (etaMin + v.etaMin).coerceAtLeast(10),
                                                    fare = vFinal,
                                                    originalFare = if (promoApplied && vRaw > 40) vRaw + vTax else null,
                                                    selected = isSel,
                                                    onClick = {
                                                        selectedVehicle = v
                                                        selectedModeTab = when (v.id) {
                                                            "bike" -> "bike"
                                                            "auto", "auto_xl" -> "auto"
                                                            else -> "cab"
                                                        }
                                                    }
                                                )
                                            }
                                        }

                                        // Expand/Collapse all 6 ride tiers
                                        Text(
                                            text = if (showAllVehicles) "Show Compact Tiers ▲" else "Show All 6 Ride Tiers (Auto XL, Sedan, SUV) ▼",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = RideRoyalBlue,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .align(Alignment.CenterHorizontally)
                                                .clickable { showAllVehicles = !showAllVehicles }
                                                .padding(vertical = 2.dp)
                                        )

                                        // Payment Selector + Apply Coupon Row (Screen 4 exact footer)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFFF8FAFC))
                                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.clickable {
                                                    val idx = (paymentMethods.indexOf(selectedPayment) + 1) % paymentMethods.size
                                                    selectedPayment = paymentMethods[idx]
                                                }
                                            ) {
                                                Icon(
                                                    imageVector = if (selectedPayment.contains("Wallet")) Icons.Rounded.AccountBalanceWallet else Icons.Rounded.QrCode2,
                                                    contentDescription = "Payment method",
                                                    tint = DeepNavy,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    text = selectedPayment,
                                                    style = MaterialTheme.typography.titleSmall,
                                                    color = DeepNavy,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MutedText, modifier = Modifier.size(18.dp))
                                            }
                                            Text(
                                                text = if (promoApplied) "RIDE15 Applied ✓" else "Apply Coupon",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = if (promoApplied) SuccessGreen else RideRoyalBlue,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.clickable {
                                                    promoApplied = !promoApplied
                                                    onShowMessage(if (promoApplied) "Coupon RIDE15 applied • ₹15 OFF!" else "Coupon removed")
                                                }
                                            )
                                        }

                                        // Full-Width Royal Blue Confirm Ride Button
                                        Button(
                                            onClick = { phase = RidePhase.MATCHING_DRIVER },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(54.dp)
                                                .navigationBarsPadding()
                                                .testTag("confirm_ride_button"),
                                            shape = RoundedCornerShape(50),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = RideRoyalBlue,
                                                contentColor = SoftWhite
                                            )
                                        ) {
                                            Text(
                                                text = "Confirm Ride  •  ₹$estimatedFare",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                }

                                RidePhase.MATCHING_DRIVER -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Character3DPortrait(
                                            drawableRes = R.drawable.img_char_ride_captain_1795426023899,
                                            contentDescription = "Matching Take It Captain",
                                            borderColor = RideRoyalBlue,
                                            size = 92.dp
                                        )
                                        Text(
                                            "Matching Your Take It Captain...",
                                            style = MaterialTheme.typography.headlineSmall,
                                            color = DeepNavy,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                                            color = RideRoyalBlue,
                                            trackColor = RideSelectedBg
                                        )
                                        Text(
                                            "8 verified ${selectedVehicle.name} Captains within 500m • $mapPinFineTune",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedText
                                        )
                                        OutlinedButton(
                                            onClick = { phase = RidePhase.SELECT_ROUTE_VEHICLE },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger)
                                        ) { Text("Cancel Request", fontWeight = FontWeight.Bold) }
                                    }
                                }

                                RidePhase.CAPTAIN_ASSIGNED -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(rideTimelineSteps[rideStepIndex.coerceIn(0, 5)], style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                                Text("Share 4-digit PIN to start your ride", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                ridePin.forEach { digit ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(RoundedCornerShape(9.dp))
                                                            .background(RideYellowAccent)
                                                            .border(1.2.dp, DeepNavy, RoundedCornerShape(9.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(digit.toString(), style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = RideNavyHeader,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Character3DPortrait(
                                                        drawableRes = R.drawable.img_char_ride_captain_1795426023899,
                                                        contentDescription = "Captain Arjun Kumar",
                                                        borderColor = RideYellowAccent,
                                                        size = 60.dp
                                                    )
                                                    Spacer(Modifier.width(12.dp))
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                                            Text("Arjun Kumar", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.Bold)
                                                            Spacer(Modifier.width(6.dp))
                                                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(SuccessGreen).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                                                Text("4.9 ★", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                        Text("${selectedVehicle.name} • Hero Xtreme 160R • Helmet Included", style = MaterialTheme.typography.bodySmall, color = Color(0xFFBFDBFE))
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(RideYellowAccent)
                                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                                    ) {
                                                        Text("UP 16\nEQ 4920", style = MaterialTheme.typography.labelSmall.copy(lineHeight = 11.sp), color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                                    }
                                                }

                                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Button(
                                                        onClick = { onShowMessage("Calling Captain Arjun (+91 98*** **420)...") },
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(containerColor = SoftWhite.copy(alpha = 0.14f), contentColor = SoftWhite),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.Call, null, Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Call", style = MaterialTheme.typography.labelMedium)
                                                    }
                                                    Button(
                                                        onClick = { onShowMessage("Sent to Captain: 'I am waiting at the gate!'") },
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(containerColor = SoftWhite.copy(alpha = 0.14f), contentColor = RideYellowAccent),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.Chat, null, Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Chat", style = MaterialTheme.typography.labelMedium)
                                                    }
                                                    Button(
                                                        onClick = { onShowMessage("Live ride link shared with ${uiState.appState.emergencyContactName}") },
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(containerColor = SoftWhite.copy(alpha = 0.14f), contentColor = SkyBlue),
                                                        shape = RoundedCornerShape(12.dp)
                                                    ) {
                                                        Icon(Icons.Rounded.ShareLocation, null, Modifier.size(16.dp))
                                                        Spacer(Modifier.width(4.dp))
                                                        Text("Share", style = MaterialTheme.typography.labelMedium)
                                                    }
                                                }
                                            }
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = RideSelectedBg,
                                            modifier = Modifier.border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(18.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("LIVE RIDE TIMELINE (6 STEPS)", style = MaterialTheme.typography.labelMedium, color = RideRoyalBlue, fontWeight = FontWeight.ExtraBold)
                                                    TextButton(
                                                        onClick = { if (rideStepIndex < 4) rideStepIndex++ },
                                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                                    ) {
                                                        Text("Next Stage →", style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                                MultiStepStatusTimeline(
                                                    steps = rideTimelineSteps,
                                                    currentStepIndex = rideStepIndex,
                                                    accentColor = RideRoyalBlue
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = otpInput,
                                            onValueChange = { otpInput = it },
                                            label = { Text("Enter Ride PIN ($ridePin) or tap Complete") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().testTag("ride_otp_input")
                                        )
                                        Button(
                                            onClick = {
                                                if (otpInput.isBlank() || otpInput == ridePin) {
                                                    if (activeOrderId.isNotEmpty()) onCompleteRide(activeOrderId)
                                                    phase = RidePhase.COMPLETED_SUMMARY
                                                } else {
                                                    onShowMessage("PIN mismatch. Enter $ridePin")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(54.dp).navigationBarsPadding().testTag("complete_ride_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Complete Ride • Pay ₹$estimatedFare", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }

                                RidePhase.COMPLETED_SUMMARY -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Character3DPortrait(
                                            drawableRes = R.drawable.img_char_ride_captain_1795426023899,
                                            contentDescription = "Ride Completed",
                                            borderColor = SuccessGreen,
                                            size = 84.dp
                                        )
                                        Text("You've Arrived Safely!", style = MaterialTheme.typography.headlineMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                        StatusPill("PAID • ₹$estimatedFare ($selectedPayment)")

                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = RideSelectedBg,
                                            modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(18.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("FARE BREAKDOWN RECEIPT", style = MaterialTheme.typography.labelMedium, color = RideRoyalBlue, fontWeight = FontWeight.ExtraBold)
                                                RideFareLine("Base Fare (${selectedVehicle.name})", "₹$baseComponent")
                                                RideFareLine("Distance Charge (${"%.1f".format(distanceKm)} km)", "₹$distanceComponent")
                                                RideFareLine("GST & Platform Taxes (5%)", "₹$taxComponent")
                                                if (discount > 0) RideFareLine("Promo Discount (RIDE15)", "-₹$discount", SuccessGreen)
                                                HorizontalDivider(color = Color(0xFFBFDBFE))
                                                RideFareLine("Total Paid ($selectedPayment)", "₹$estimatedFare", DeepNavy, bold = true)
                                            }
                                        }

                                        Text("Rate Captain Arjun Kumar", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (s in 1..5) {
                                                IconButton(onClick = { userRating = s }) {
                                                    Icon(
                                                        if (s <= userRating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                        "Rate $s",
                                                        tint = PremiumYellowDark,
                                                        modifier = Modifier.size(34.dp)
                                                    )
                                                }
                                            }
                                        }
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(listOf("Safe Driving", "Clean Helmet", "On Time", "Polite Captain")) { tag ->
                                                val sel = tag in selectedCompliments
                                                FilterChip(
                                                    selected = sel,
                                                    onClick = {
                                                        selectedCompliments = if (sel) selectedCompliments - tag else selectedCompliments + tag
                                                    },
                                                    label = { Text(tag, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                        OutlinedTextField(
                                            value = feedbackNote,
                                            onValueChange = { feedbackNote = it },
                                            label = { Text("Add optional compliment or feedback") },
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                            OutlinedButton(
                                                onClick = { onShowMessage("Ride Receipt downloaded (₹$estimatedFare)") },
                                                modifier = Modifier.weight(1f).height(50.dp),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Icon(Icons.Rounded.ReceiptLong, null, Modifier.size(18.dp))
                                                Spacer(Modifier.width(5.dp))
                                                Text("Receipt", fontWeight = FontWeight.Bold)
                                            }
                                            Button(
                                                onClick = {
                                                    if (activeOrderId.isNotEmpty()) onRateRide(activeOrderId, userRating)
                                                    onBack()
                                                },
                                                modifier = Modifier.weight(1.4f).height(50.dp).testTag("submit_ride_rating_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = RideRoyalBlue, contentColor = SoftWhite),
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text("Submit & Done", fontWeight = FontWeight.ExtraBold)
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

    // Destination Search & Map Pin Modal
    if (showDestinationModal) {
        var destQuery by remember { mutableStateOf("") }
        ModalBottomSheet(onDismissRequest = { showDestinationModal = false }, containerColor = SoftWhite) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Search Destination & Pin", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                OutlinedTextField(
                    value = destQuery,
                    onValueChange = { destQuery = it },
                    placeholder = { Text("Search mall, metro, tech park, airport...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = RideRoyalBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = pickup == "Sector 62, Noida",
                        onClick = { pickup = "Sector 62, Noida" },
                        label = { Text("🏠 Sector 62") }
                    )
                    FilterChip(
                        selected = destination == "DLF Mall, Noida",
                        onClick = { destination = "DLF Mall, Noida"; showDestinationModal = false },
                        label = { Text("🛍 DLF Mall") }
                    )
                    FilterChip(
                        selected = mapPinFineTune.contains("Gate"),
                        onClick = {
                            mapPinFineTune = if (mapPinFineTune == "Main Gate Pin Adjusted") "Metro Exit 2 Pin Adjusted" else "Main Gate Pin Adjusted"
                            onShowMessage("Map Pin updated: $mapPinFineTune")
                        },
                        label = { Text("📍 Pin") }
                    )
                }
                quickDestinations.filter { destQuery.isBlank() || it.first.contains(destQuery, true) }.forEach { (place, km) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF8FAFC))
                            .clickable {
                                destination = place
                                showDestinationModal = false
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = RideRoyalBlue, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(place, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                        }
                        Text("$km km", style = MaterialTheme.typography.labelMedium, color = MutedText)
                    }
                }
            }
        }
    }

    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            containerColor = RideNavyHeader,
            titleContentColor = CoralDanger,
            textContentColor = SoftWhite,
            title = { Text("24x7 Safety SOS Shield") },
            text = {
                Text("Simulated Safety Alert:\n• Emergency Contact: ${uiState.appState.emergencyContactName} (${uiState.appState.emergencyContactPhone})\n• Live GPS & Captain plate UP 16 EQ 4920 shared with Take It Safety Desk.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        onShowMessage("Simulated SOS alert sent to ${uiState.appState.emergencyContactName}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralDanger)
                ) { Text("Trigger Demo SOS") }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) { Text("Close", color = RideYellowAccent) }
            }
        )
    }
}

@Composable
private fun RideFareLine(label: String, value: String, valueColor: Color = SlateText, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodySmall, color = if (bold) DeepNavy else SlateText, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.Medium)
        Text(value, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodySmall, color = valueColor, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}

@Composable
private fun TakeItRideVehicleCard(
    vehicle: VehicleOption,
    etaMinutes: Int,
    fare: Int,
    originalFare: Int?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (vehicle.id) {
        "bike" -> Icons.Rounded.TwoWheeler
        "auto", "auto_xl" -> Icons.Rounded.ElectricRickshaw
        "suv" -> Icons.Rounded.AirportShuttle
        else -> Icons.Rounded.DirectionsCar
    }
    val shortTitle = when (vehicle.id) {
        "bike" -> "Bike"
        "auto" -> "Auto"
        "mini" -> "Cab"
        else -> vehicle.name
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) RideSelectedBg else SoftWhite)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) RideRoyalBlue else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .testTag("vehicle_option_${vehicle.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFFFEF08A), RideYellowAccent)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = vehicle.name, tint = DeepNavy, modifier = Modifier.size(30.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = shortTitle,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepNavy,
                    fontWeight = FontWeight.ExtraBold
                )
                if (vehicle.badge.isNotEmpty() && selected) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(RideRoyalBlue.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            vehicle.badge,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = RideRoyalBlue,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
            Text(
                text = "$etaMinutes min • ${vehicle.capacity}",
                style = MaterialTheme.typography.bodySmall,
                color = MutedText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "₹$fare",
                    style = MaterialTheme.typography.titleLarge,
                    color = if (selected) RideRoyalBlue else DeepNavy,
                    fontWeight = FontWeight.ExtraBold
                )
                if (originalFare != null) {
                    Text(
                        text = "₹$originalFare",
                        style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                        color = MutedText
                    )
                }
            }
            if (selected) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(RideRoyalBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = SoftWhite, modifier = Modifier.size(14.dp))
                }
            }
        }
    }
}
