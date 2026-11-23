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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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

private val RapidoYellow = PremiumYellow
private val RapidoYellowDeep = PremiumYellowDark
private val RapidoCharcoal = DeepNavy
private val RapidoSurface = LightSkyBackground

private enum class RidePhase {
    SELECT_ROUTE_VEHICLE,
    MATCHING_DRIVER,
    CAPTAIN_ASSIGNED,
    COMPLETED_SUMMARY
}

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
    var phase by remember { mutableStateOf(RidePhase.SELECT_ROUTE_VEHICLE) }
    var pickup by remember { mutableStateOf("${uiState.appState.activeAddressLabel} • Indiranagar 100ft Rd") }
    var destination by remember { mutableStateOf("Koramangala 4th Block") }
    var selectedVehicle by remember { mutableStateOf(DemoCatalog.rideVehicles.first()) }
    var selectedPayment by remember { mutableStateOf("UPI") }
    var scheduleSlot by remember { mutableStateOf("Now") }
    var promoApplied by remember { mutableStateOf(true) }
    var showDestinationModal by remember { mutableStateOf(false) }
    var mapPinFineTune by remember { mutableStateOf("Main Gate Pin Adjusted") }
    var otpInput by remember { mutableStateOf("") }
    val ridePin = remember { "4821" }
    var activeOrderId by remember { mutableStateOf("") }
    var driverProgress by remember { mutableFloatStateOf(0.14f) }
    var rideStepIndex by remember { mutableIntStateOf(1) } // 0..5 for 6-step ride timeline
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

    val quickDestinations = listOf(
        "Koramangala 4th Block" to 5.4,
        "MG Road Metro" to 4.1,
        "Bagmane Tech Park" to 6.8,
        "HSR Layout Sector 2" to 8.2,
        "Kempegowda Airport T2" to 32.5
    )
    val distanceKm = quickDestinations.find { it.first == destination }?.second ?: 6.2
    val etaMin = (distanceKm * 2.2).toInt().coerceAtLeast(7)
    val baseComponent = selectedVehicle.baseFare
    val distanceComponent = (distanceKm * selectedVehicle.perKmRate).toInt()
    val waitingCharge = 0
    val rawFare = baseComponent + distanceComponent + waitingCharge
    val taxComponent = (rawFare * 0.05).toInt().coerceAtLeast(3)
    val discount = if (promoApplied && rawFare > 45) 15 else 0
    val estimatedFare = (rawFare + taxComponent - discount).coerceAtLeast(25)

    LaunchedEffect(phase) {
        when (phase) {
            RidePhase.MATCHING_DRIVER -> {
                rideStepIndex = 0
                delay(2100)
                onRecordRide(
                    "${selectedVehicle.name} • $destination",
                    "${"%.1f".format(distanceKm)} km • $etaMin mins • $scheduleSlot • Captain Arjun",
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
                driverProgress = 0.14f
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

    Box(modifier = Modifier.fillMaxSize().background(RapidoSurface)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // 1. FULL-BLEED MAP HEADER WITH FLOATING BACK, ROUTE BADGE & SOS
            item {
                Box(modifier = Modifier.fillMaxWidth().height(295.dp)) {
                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxSize(),
                        pickupLabel = pickup.substringBefore(" •"),
                        dropLabel = destination,
                        distanceKm = distanceKm,
                        etaMinutes = etaMin,
                        driverProgress = if (phase == RidePhase.CAPTAIN_ASSIGNED) driverProgress else -1f,
                        showMovingDriver = phase != RidePhase.MATCHING_DRIVER,
                        showMatchingRadar = phase == RidePhase.MATCHING_DRIVER
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(46.dp)
                                .shadow(6.dp, CircleShape)
                                .clip(CircleShape)
                                .background(SoftWhite)
                                .testTag("back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = RapidoCharcoal)
                        }

                        Row(
                            modifier = Modifier
                                .shadow(6.dp, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(RapidoCharcoal)
                                .border(1.dp, SkyBlue.copy(alpha = 0.6f), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(RapidoYellow))
                            Spacer(Modifier.width(7.dp))
                            Text(
                                "ZIPORA RIDE • ${selectedVehicle.etaMin}m ETA",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (phase == RidePhase.CAPTAIN_ASSIGNED) {
                            Button(
                                onClick = { showSosDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CoralDanger, contentColor = SoftWhite),
                                shape = RoundedCornerShape(50),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(40.dp).testTag("ride_sos_button")
                            ) {
                                Icon(Icons.Rounded.Shield, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("SOS", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                            }
                        } else {
                            Spacer(Modifier.size(46.dp))
                        }
                    }
                }
            }

            // 2. ELEVATED BOTTOM SHEET SURFACE
            item {
                Surface(
                    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                    color = SoftWhite,
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth().offset(y = (-18).dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .width(44.dp)
                                .height(4.5.dp)
                                .clip(RoundedCornerShape(50))
                                .background(SkyBlue.copy(alpha = 0.55f))
                        )

                        AnimatedContent(targetState = phase, label = "rapidoRidePhase") { current ->
                            when (current) {
                                RidePhase.SELECT_ROUTE_VEHICLE -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(13.dp)) {
                                        // 3D Captain Greeting Strip
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(18.dp))
                                                .background(Brush.horizontalGradient(listOf(DeepNavy, Color(0xFF0C4A6E))))
                                                .padding(12.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Character3DPortrait(
                                                drawableRes = R.drawable.img_char_ride_captain_1795426023899,
                                                contentDescription = "3D Ride Captain",
                                                borderColor = PremiumYellow,
                                                size = 56.dp
                                            )
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Zipora Captain Ready!", style = MaterialTheme.typography.titleSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                                                Text("6 tiers • Zero surge • Helmet & GPS Shield", style = MaterialTheme.typography.bodySmall, color = SoftWhite.copy(alpha = 0.9f))
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(SkyBlue.copy(alpha = 0.22f))
                                                    .clickable { showDestinationModal = true }
                                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                                            ) {
                                                Text("Map Pin", style = MaterialTheme.typography.labelSmall, color = SkyBlue, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        // Connected Pickup -> Drop Card with Swap & Full Search Button
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = RapidoSurface,
                                            modifier = Modifier.border(1.dp, LightBorder, RoundedCornerShape(18.dp))
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    modifier = Modifier.padding(end = 10.dp)
                                                ) {
                                                    Box(Modifier.size(11.dp).clip(CircleShape).background(SuccessGreen))
                                                    Box(Modifier.width(2.dp).height(34.dp).background(SkyBlue))
                                                    Box(Modifier.size(11.dp).clip(CircleShape).background(CoralDanger))
                                                }
                                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                    OutlinedTextField(
                                                        value = pickup,
                                                        onValueChange = { pickup = it },
                                                        placeholder = { Text("Pickup location") },
                                                        singleLine = true,
                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = SoftWhite,
                                                            unfocusedContainerColor = SoftWhite,
                                                            focusedBorderColor = SkyBlueDark,
                                                            unfocusedBorderColor = LightBorder
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.fillMaxWidth().testTag("ride_pickup_input")
                                                    )
                                                    OutlinedTextField(
                                                        value = destination,
                                                        onValueChange = { destination = it },
                                                        placeholder = { Text("Drop destination") },
                                                        trailingIcon = {
                                                            IconButton(onClick = { showDestinationModal = true }) {
                                                                Icon(Icons.Rounded.Search, "Search destination", tint = SkyBlueDark)
                                                            }
                                                        },
                                                        singleLine = true,
                                                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                                        colors = OutlinedTextFieldDefaults.colors(
                                                            focusedContainerColor = SoftWhite,
                                                            unfocusedContainerColor = SoftWhite,
                                                            focusedBorderColor = SkyBlueDark,
                                                            unfocusedBorderColor = LightBorder
                                                        ),
                                                        shape = RoundedCornerShape(12.dp),
                                                        modifier = Modifier.fillMaxWidth().testTag("ride_destination_input")
                                                    )
                                                }
                                                IconButton(
                                                    onClick = {
                                                        val tmp = pickup
                                                        pickup = destination
                                                        destination = tmp
                                                    }
                                                ) {
                                                    Icon(Icons.Rounded.SwapVert, "Swap pickup and drop", tint = RapidoCharcoal)
                                                }
                                            }
                                        }

                                        // Schedule Ride Slots + Saved Places Shortcut
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(listOf("Now", "Today 6:00 PM", "Tomorrow 9:00 AM")) { slot ->
                                                val sel = scheduleSlot == slot
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(if (sel) SkyBlue else RapidoSurface)
                                                        .border(1.dp, if (sel) SkyBlueDark else LightBorder, RoundedCornerShape(50))
                                                        .clickable { scheduleSlot = slot }
                                                        .padding(horizontal = 12.dp, vertical = 7.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(Icons.Rounded.Schedule, null, tint = DeepNavy, modifier = Modifier.size(14.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(slot, style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                            items(quickDestinations) { (place, km) ->
                                                val sel = destination == place
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(50))
                                                        .background(if (sel) RapidoCharcoal else RapidoSurface)
                                                        .border(1.dp, if (sel) RapidoCharcoal else LightBorder, RoundedCornerShape(50))
                                                        .clickable { destination = place }
                                                        .padding(horizontal = 12.dp, vertical = 7.dp)
                                                ) {
                                                    Text(
                                                        "$place • ${km}km",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = if (sel) RapidoYellow else RapidoCharcoal,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }

                                        // 6-Tier Vehicle Selector Cards (Bike, Auto, Auto XL, Mini, Sedan, SUV)
                                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                            Text("Select Ride (6 Tiers)", style = MaterialTheme.typography.titleLarge, color = RapidoCharcoal, fontWeight = FontWeight.ExtraBold)
                                            Text("${"%.1f".format(distanceKm)} km • $etaMin min", style = MaterialTheme.typography.labelMedium, color = SkyBlueDark, fontWeight = FontWeight.Bold)
                                        }

                                        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                                            DemoCatalog.rideVehicles.forEach { v ->
                                                val isSel = selectedVehicle.id == v.id
                                                val vRaw = v.baseFare + (distanceKm * v.perKmRate).toInt()
                                                val vTax = (vRaw * 0.05).toInt().coerceAtLeast(3)
                                                val vFinal = (vRaw + vTax - (if (promoApplied && vRaw > 45) 15 else 0)).coerceAtLeast(25)
                                                RapidoVehicleCard(
                                                    vehicle = v,
                                                    fare = vFinal,
                                                    originalFare = if (promoApplied && vRaw > 45) vRaw + vTax else null,
                                                    selected = isSel,
                                                    onClick = { selectedVehicle = v }
                                                )
                                            }
                                        }

                                        // Payment & Promo Strip
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFFFFFBEB))
                                                .border(1.dp, RapidoYellowDeep.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Rounded.LocalOffer, null, tint = Color(0xFFB45309), modifier = Modifier.size(18.dp))
                                                Spacer(Modifier.width(8.dp))
                                                Text(
                                                    if (promoApplied) "RIDE15 Applied • Saving ₹15" else "Tap to apply ₹15 OFF",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    color = RapidoCharcoal,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Text(
                                                if (promoApplied) "Remove" else "Apply",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color(0xFFB45309),
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.clickable { promoApplied = !promoApplied }
                                            )
                                        }

                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(listOf("Cash", "UPI", "Zipora Wallet", "Card")) { method ->
                                                val sel = selectedPayment == method
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(12.dp))
                                                        .background(if (sel) RapidoCharcoal else RapidoSurface)
                                                        .clickable { selectedPayment = method }
                                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Icon(
                                                        when (method) {
                                                            "Cash" -> Icons.Rounded.Payments
                                                            "Zipora Wallet" -> Icons.Rounded.AccountBalanceWallet
                                                            else -> Icons.Rounded.QrCode2
                                                        },
                                                        null,
                                                        tint = if (sel) RapidoYellow else RapidoCharcoal,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(6.dp))
                                                    Text(method, style = MaterialTheme.typography.labelMedium, color = if (sel) SoftWhite else RapidoCharcoal, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }

                                        Button(
                                            onClick = { phase = RidePhase.MATCHING_DRIVER },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(58.dp)
                                                .navigationBarsPadding()
                                                .testTag("confirm_ride_button"),
                                            shape = RoundedCornerShape(18.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = RapidoYellow, contentColor = RapidoCharcoal)
                                        ) {
                                            Text(
                                                "Book ${selectedVehicle.name} ($scheduleSlot)  •  ₹$estimatedFare",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.ExtraBold
                                            )
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
                                            contentDescription = "Matching 3D Captain",
                                            borderColor = SkyBlue,
                                            size = 96.dp
                                        )
                                        Text("Matching Your Zipora Captain...", style = MaterialTheme.typography.headlineMedium, color = RapidoCharcoal, fontWeight = FontWeight.ExtraBold)
                                        LinearProgressIndicator(
                                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(50)),
                                            color = SkyBlueDark,
                                            trackColor = RideCardStart
                                        )
                                        Text("8 verified ${selectedVehicle.name} Captains within 600m • $mapPinFineTune", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                        OutlinedButton(
                                            onClick = { phase = RidePhase.SELECT_ROUTE_VEHICLE },
                                            shape = RoundedCornerShape(14.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger)
                                        ) { Text("Cancel Request", fontWeight = FontWeight.Bold) }
                                    }
                                }

                                RidePhase.CAPTAIN_ASSIGNED -> {
                                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                        // Iconic 4-Box PIN Strip + Arriving Status
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(rideTimelineSteps[rideStepIndex.coerceIn(0, 5)], style = MaterialTheme.typography.titleMedium, color = RapidoCharcoal, fontWeight = FontWeight.ExtraBold)
                                                Text("Share 4-digit PIN to start your ride", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                                ridePin.forEach { digit ->
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(RoundedCornerShape(9.dp))
                                                            .background(RapidoYellow)
                                                            .border(1.2.dp, RapidoCharcoal, RoundedCornerShape(9.dp)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(digit.toString(), style = MaterialTheme.typography.titleMedium, color = RapidoCharcoal, fontWeight = FontWeight.ExtraBold)
                                                    }
                                                }
                                            }
                                        }

                                        // 3D Captain Portrait + Vehicle Number Plate Card
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = RapidoCharcoal,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Character3DPortrait(
                                                        drawableRes = R.drawable.img_char_ride_captain_1795426023899,
                                                        contentDescription = "Captain Arjun Kumar 3D Avatar",
                                                        borderColor = PremiumYellow,
                                                        size = 62.dp
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
                                                        Text("${selectedVehicle.name} • Hero Xtreme 160R • Helmet Included", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8D4EB))
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .clip(RoundedCornerShape(8.dp))
                                                            .background(RapidoYellow)
                                                            .padding(horizontal = 8.dp, vertical = 5.dp)
                                                    ) {
                                                        Text("KA 01\nEQ 4920", style = MaterialTheme.typography.labelSmall.copy(lineHeight = 11.sp), color = RapidoCharcoal, fontWeight = FontWeight.ExtraBold)
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
                                                        onClick = { onShowMessage("Sent to Captain: 'I am waiting at the main gate!'") },
                                                        modifier = Modifier.weight(1f),
                                                        colors = ButtonDefaults.buttonColors(containerColor = SoftWhite.copy(alpha = 0.14f), contentColor = RapidoYellow),
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

                                        // 6-Step Live Ride Status Timeline
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = LightSkyBackground,
                                            modifier = Modifier.border(1.dp, LightBorder, RoundedCornerShape(18.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Text("LIVE RIDE TIMELINE (6 STEPS)", style = MaterialTheme.typography.labelMedium, color = SkyBlueDark, fontWeight = FontWeight.ExtraBold)
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
                                                    accentColor = SkyBlueDark
                                                )
                                            }
                                        }

                                        // Verify PIN & Finish Ride
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
                                            shape = RoundedCornerShape(16.dp)
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Complete Ride • Pay ₹$estimatedFare ($selectedPayment)", fontWeight = FontWeight.ExtraBold)
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
                                            contentDescription = "Ride Completed Captain",
                                            borderColor = SuccessGreen,
                                            size = 86.dp
                                        )
                                        Text("You've Arrived Safely!", style = MaterialTheme.typography.headlineMedium, color = RapidoCharcoal, fontWeight = FontWeight.ExtraBold)
                                        StatusPill("PAID • ₹$estimatedFare ($selectedPayment)")

                                        // Detailed Fare Breakdown Receipt Card
                                        Surface(
                                            shape = RoundedCornerShape(18.dp),
                                            color = LightSkyBackground,
                                            modifier = Modifier.fillMaxWidth().border(1.dp, LightBorder, RoundedCornerShape(18.dp))
                                        ) {
                                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                                Text("FARE BREAKDOWN RECEIPT", style = MaterialTheme.typography.labelMedium, color = SkyBlueDark, fontWeight = FontWeight.ExtraBold)
                                                RideFareLine("Base Fare (${selectedVehicle.name})", "₹$baseComponent")
                                                RideFareLine("Distance Charge (${"%.1f".format(distanceKm)} km)", "₹$distanceComponent")
                                                RideFareLine("Waiting Charge", "₹$waitingCharge")
                                                RideFareLine("GST & Platform Taxes (5%)", "₹$taxComponent")
                                                if (discount > 0) RideFareLine("Promo Discount (RIDE15)", "-₹$discount", SuccessGreen)
                                                HorizontalDivider(color = LightBorder)
                                                RideFareLine("Total Paid ($selectedPayment)", "₹$estimatedFare", DeepNavy, bold = true)
                                            }
                                        }

                                        Text("Rate Captain Arjun Kumar", style = MaterialTheme.typography.titleMedium, color = RapidoCharcoal, fontWeight = FontWeight.Bold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (s in 1..5) {
                                                IconButton(onClick = { userRating = s }) {
                                                    Icon(
                                                        if (s <= userRating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                        "Rate $s",
                                                        tint = RapidoYellowDeep,
                                                        modifier = Modifier.size(36.dp)
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
                                                onClick = { onShowMessage("Ride Receipt PDF downloaded (₹$estimatedFare • $selectedPayment)") },
                                                modifier = Modifier.weight(1f).height(52.dp),
                                                shape = RoundedCornerShape(16.dp)
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
                                                modifier = Modifier.weight(1.4f).height(52.dp).testTag("submit_ride_rating_button"),
                                                colors = ButtonDefaults.buttonColors(containerColor = RapidoYellow, contentColor = RapidoCharcoal),
                                                shape = RoundedCornerShape(16.dp)
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

    // Destination Search & Map Pin Adjustment Sheet
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
                    placeholder = { Text("Search metro, tech park, airport...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = SkyBlueDark) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Saved Locations", style = MaterialTheme.typography.labelLarge, color = MutedText)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = destination.contains("Home"),
                        onClick = { destination = "Home • Indiranagar 100ft Rd"; showDestinationModal = false },
                        label = { Text("🏠 Home") }
                    )
                    FilterChip(
                        selected = destination.contains("Bagmane"),
                        onClick = { destination = "Bagmane Tech Park"; showDestinationModal = false },
                        label = { Text("💼 Work") }
                    )
                    FilterChip(
                        selected = mapPinFineTune.contains("Gate"),
                        onClick = {
                            mapPinFineTune = if (mapPinFineTune == "Main Gate Pin Adjusted") "Metro Exit 2 Pin Adjusted" else "Main Gate Pin Adjusted"
                            onShowMessage("Map Pin updated: $mapPinFineTune")
                        },
                        label = { Text("📍 Fine-Tune Pin") }
                    )
                }
                Text("Popular & Recent Destinations", style = MaterialTheme.typography.labelLarge, color = MutedText)
                quickDestinations.filter { destQuery.isBlank() || it.first.contains(destQuery, true) }.forEach { (place, km) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightSkyBackground)
                            .clickable {
                                destination = place
                                showDestinationModal = false
                            }
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = SkyBlueDark, modifier = Modifier.size(18.dp))
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
            containerColor = RapidoCharcoal,
            titleContentColor = CoralDanger,
            textContentColor = SoftWhite,
            title = { Text("24x7 Safety SOS Shield") },
            text = {
                Text("Simulated Safety Alert:\n• Emergency Contact: ${uiState.appState.emergencyContactName} (${uiState.appState.emergencyContactPhone})\n• Live GPS & Captain plate KA 01 EQ 4920 shared with Zipora Safety Desk.")
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
                TextButton(onClick = { showSosDialog = false }) { Text("Close", color = RapidoYellow) }
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
private fun RapidoVehicleCard(
    vehicle: VehicleOption,
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) Color(0xFFFFFBEB) else SoftWhite)
            .border(if (selected) 2.2.dp else 1.dp, if (selected) RapidoYellowDeep else LightBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 13.dp, vertical = 12.dp)
            .testTag("vehicle_option_${vehicle.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) RapidoYellow else RideCardStart),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = vehicle.name, tint = RapidoCharcoal, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vehicle.name, style = MaterialTheme.typography.titleMedium, color = RapidoCharcoal, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(6.dp))
                Icon(Icons.Rounded.Person, null, tint = MutedText, modifier = Modifier.size(13.dp))
                Text(vehicle.capacity.take(1), style = MaterialTheme.typography.labelSmall, color = MutedText)
                if (vehicle.badge.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(RapidoCharcoal).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(vehicle.badge, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = RapidoYellow, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Text("${vehicle.etaMin} mins away • ${vehicle.tagline}", style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("₹$fare", style = MaterialTheme.typography.titleLarge, color = RapidoCharcoal, fontWeight = FontWeight.ExtraBold)
            if (originalFare != null) {
                Text(
                    "₹$originalFare",
                    style = MaterialTheme.typography.labelSmall.copy(textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough),
                    color = MutedText
                )
            }
        }
    }
}
