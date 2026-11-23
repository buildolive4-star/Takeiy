package com.example.ui.screens.partners

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DemoCatalog
import com.example.model.AppRoute
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState
import kotlinx.coroutines.delay

private val CaptainYellow = Color(0xFFFFD43B)
private val CaptainCharcoal = Color(0xFF111827)

@Composable
fun DriverCaptainPanelScreen(
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onToggleOnline: (Boolean) -> Unit,
    onToggleService: (String) -> Unit,
    onNextRequest: (Int) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val isOnline = uiState.appState.driverOnline
    val currentReq = DemoCatalog.incomingDriverRequests[uiState.activeDriverRequestIndex % DemoCatalog.incomingDriverRequests.size]

    // 0 = Incoming Ping, 1 = Heading to Pickup, 2 = Arrived / Enter PIN, 3 = Trip In Progress
    var tripStage by remember(uiState.activeDriverRequestIndex) { mutableIntStateOf(0) }
    var pinInput by remember(uiState.activeDriverRequestIndex) { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Duty Radar, 1: Earnings, 2: Services & Docs

    val eligibleServices = listOf(
        Triple("Bike Taxi", "Verified • Hero Xtreme 160R", true),
        Triple("Auto", "Verified • Bajaj Compact RE", true),
        Triple("Cab", "Requires Commercial LMV Permit", false),
        Triple("Food Delivery", "Thermal Bag Verified", true),
        Triple("Grocery Delivery", "Express Crate Ready", true),
        Triple("Parcel Delivery", "KYC & OTP Shield Verified", true)
    )

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            // Clean Rapido Captain Bottom Bar
            Surface(color = CaptainCharcoal, shadowElevation = 12.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(
                        Triple("Duty", Icons.Rounded.Radar, 0),
                        Triple("Earnings", Icons.Rounded.AccountBalanceWallet, 1),
                        Triple("Services", Icons.Rounded.TwoWheeler, 2)
                    ).forEach { (label, icon, idx) ->
                        val active = selectedTab == idx
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (active) CaptainYellow else Color.Transparent)
                                .clickable { selectedTab = idx }
                                .padding(horizontal = 22.dp, vertical = 7.dp)
                        ) {
                            Icon(icon, label, tint = if (active) CaptainCharcoal else Color(0xFF9FB3C8), modifier = Modifier.size(20.dp))
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) CaptainCharcoal else Color(0xFF9FB3C8),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // RAPIDO CAPTAIN TOP FLOATING DUTY HEADER
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(CaptainCharcoal, DeepNavy)))
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(SoftWhite.copy(alpha = 0.12f))
                                .testTag("driver_menu_button")
                        ) {
                            Icon(Icons.Rounded.Menu, "Switch Panel", tint = CaptainYellow)
                        }

                        // Iconic Center ON DUTY / OFF DUTY Toggle Pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .shadow(8.dp, RoundedCornerShape(50))
                                .clip(RoundedCornerShape(50))
                                .background(if (isOnline) SuccessGreen else CoralDanger)
                                .border(2.dp, SoftWhite.copy(alpha = 0.55f), RoundedCornerShape(50))
                                .clickable { onToggleOnline(!isOnline) }
                                .padding(horizontal = 18.dp, vertical = 9.dp)
                                .testTag("driver_online_toggle")
                        ) {
                            Box(Modifier.size(10.dp).clip(CircleShape).background(SoftWhite))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isOnline) "ON DUTY" else "OFF DUTY",
                                style = MaterialTheme.typography.labelLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.PowerSettingsNew, null, tint = SoftWhite, modifier = Modifier.size(16.dp))
                        }

                        // Today's Earnings Quick Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CaptainYellow)
                                .clickable { selectedTab = 1 }
                                .padding(horizontal = 11.dp, vertical = 8.dp)
                        ) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text("TODAY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = CaptainCharcoal)
                                Text("₹${uiState.driverEarningsToday}", style = MaterialTheme.typography.titleSmall, color = CaptainCharcoal, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    // Daily Target Incentive Progress Strip (Classic Rapido Captain Motivation)
                    val targetRides = 16
                    val progress = (uiState.driverRidesToday.toFloat() / targetRides).coerceIn(0f, 1f)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = DarkCardSurface,
                        modifier = Modifier.border(1.dp, CaptainYellow.copy(alpha = 0.45f), RoundedCornerShape(16.dp))
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    if (uiState.driverRidesToday >= targetRides) "🎉 ₹350 Daily Bonus Unlocked!" else "Complete ${(targetRides - uiState.driverRidesToday).coerceAtLeast(1)} more rides for ₹350 Bonus",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = CaptainYellow,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text("${uiState.driverRidesToday}/$targetRides", style = MaterialTheme.typography.labelMedium, color = SoftWhite, fontWeight = FontWeight.Bold)
                            }
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                                color = CaptainYellow,
                                trackColor = DarkBorder
                            )
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    item {
                        Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SimulatedMapCanvas(
                                modifier = Modifier.fillMaxWidth().height(235.dp),
                                pickupLabel = currentReq.pickupTitle,
                                dropLabel = currentReq.dropTitle,
                                distanceKm = currentReq.tripDistKm,
                                etaMinutes = (currentReq.tripDistKm * 2.4).toInt(),
                                isDarkStyle = true,
                                showMatchingRadar = isOnline && tripStage == 0
                            )

                            if (!isOnline) {
                                Surface(shape = RoundedCornerShape(22.dp), color = DarkCardSurface) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(22.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Character3DPortrait(
                                            drawableRes = R.drawable.img_char_ride_captain_1795426023899,
                                            contentDescription = "3D Captain Character",
                                            borderColor = CaptainYellow,
                                            size = 86.dp
                                        )
                                        Text("You are OFF DUTY", style = MaterialTheme.typography.headlineMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                        Text("Go ON DUTY to start receiving Bike Taxi, Auto & Express Parcel pings in Indiranagar high-demand zone.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9FB3C8))
                                        Button(
                                            onClick = { onToggleOnline(true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite),
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.fillMaxWidth().height(50.dp)
                                        ) { Text("GO ON DUTY NOW", fontWeight = FontWeight.ExtraBold) }
                                    }
                                }
                            } else when (tripStage) {
                                0 -> {
                                    // RAPIDO CAPTAIN INCOMING RIDE PING CARD
                                    var secsLeft by remember(currentReq.id) { mutableIntStateOf(15) }
                                    LaunchedEffect(currentReq.id) {
                                        secsLeft = 15
                                        while (secsLeft > 1) {
                                            delay(1000)
                                            secsLeft--
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(24.dp),
                                        color = CaptainCharcoal,
                                        modifier = Modifier.border(2.dp, CaptainYellow, RoundedCornerShape(24.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(CaptainYellow).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                                    Text("⚡ ${currentReq.serviceType.uppercase()}", style = MaterialTheme.typography.labelSmall, color = CaptainCharcoal, fontWeight = FontWeight.ExtraBold)
                                                }
                                                Box(Modifier.clip(CircleShape).background(CoralDanger.copy(alpha = 0.22f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                                    Text("${secsLeft}s", style = MaterialTheme.typography.labelLarge, color = CaptainYellow, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }

                                            LinearProgressIndicator(
                                                progress = { secsLeft / 15f },
                                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                                                color = CaptainYellow,
                                                trackColor = DarkBorder
                                            )

                                            // Giant Payout + Distance Row
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                                                Column {
                                                    Text("ESTIMATED EARNING", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9FB3C8))
                                                    Text("₹${currentReq.partnerEarnings}", style = MaterialTheme.typography.displayMedium, color = SuccessGreen, fontWeight = FontWeight.ExtraBold)
                                                }
                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text("${currentReq.paymentType} • Fare ₹${currentReq.fare}", style = MaterialTheme.typography.labelMedium, color = SkyBlue)
                                                    Text("Pickup ${currentReq.pickupDistKm} km • Drop ${currentReq.tripDistKm} km", style = MaterialTheme.typography.labelMedium, color = SoftWhite, fontWeight = FontWeight.Bold)
                                                }
                                            }

                                            // Connected Pickup -> Drop Timeline
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 10.dp)) {
                                                    Box(Modifier.size(10.dp).clip(CircleShape).background(SuccessGreen))
                                                    Box(Modifier.width(2.dp).height(24.dp).background(DarkBorder))
                                                    Box(Modifier.size(10.dp).clip(CircleShape).background(CoralDanger))
                                                }
                                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                                    Text(currentReq.pickupTitle, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                                                    Text(currentReq.dropTitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8D4EB))
                                                }
                                            }

                                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                                OutlinedButton(
                                                    onClick = {
                                                        onNextRequest(0)
                                                        onShowMessage("Ride skipped • Scanning next ping")
                                                    },
                                                    modifier = Modifier.weight(0.85f).height(52.dp).testTag("driver_reject_btn"),
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger)
                                                ) { Text("SKIP", fontWeight = FontWeight.ExtraBold) }
                                                Button(
                                                    onClick = {
                                                        tripStage = 1
                                                        onShowMessage("Ride accepted! Navigating to ${currentReq.pickupTitle}")
                                                    },
                                                    modifier = Modifier.weight(1.6f).height(52.dp).testTag("driver_accept_btn"),
                                                    shape = RoundedCornerShape(14.dp),
                                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite)
                                                ) { Text("ACCEPT RIDE", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) }
                                            }
                                        }
                                    }
                                }

                                1 -> {
                                    // STAGE 1: HEADING TO PICKUP
                                    Surface(shape = RoundedCornerShape(22.dp), color = DarkCardSurface, modifier = Modifier.border(1.5.dp, SkyBlue, RoundedCornerShape(22.dp))) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("PICKUP RIDER (${currentReq.pickupDistKm} km)", style = MaterialTheme.typography.labelSmall, color = CaptainYellow, fontWeight = FontWeight.ExtraBold)
                                                    Text(currentReq.customerName, style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                                                    Text(currentReq.pickupTitle, style = MaterialTheme.typography.bodySmall, color = SkyBlue)
                                                }
                                                StatusPill("ACCEPTED")
                                            }
                                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                                OutlinedButton(onClick = { onShowMessage("Calling ${currentReq.customerName}...") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite)) {
                                                    Icon(Icons.Rounded.Call, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Call")
                                                }
                                                OutlinedButton(onClick = { onShowMessage("Sent: 'Captain is 1 min away!'") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = CaptainYellow)) {
                                                    Icon(Icons.Rounded.Chat, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Chat")
                                                }
                                            }
                                            Button(
                                                onClick = { tripStage = 2 },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = CaptainYellow, contentColor = CaptainCharcoal)
                                            ) { Text("I HAVE ARRIVED AT PICKUP", fontWeight = FontWeight.ExtraBold) }
                                        }
                                    }
                                }

                                2 -> {
                                    // STAGE 2: ENTER RIDER 4-DIGIT PIN TO START RIDE
                                    Surface(shape = RoundedCornerShape(22.dp), color = DarkCardSurface, modifier = Modifier.border(1.5.dp, CaptainYellow, RoundedCornerShape(22.dp))) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            Text("Ask ${currentReq.customerName} for 4-Digit PIN", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.Bold)
                                            OutlinedTextField(
                                                value = pinInput,
                                                onValueChange = { pinInput = it },
                                                label = { Text("Enter Rider PIN (Demo: ${currentReq.otp})") },
                                                singleLine = true,
                                                modifier = Modifier.fillMaxWidth(),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = SoftWhite,
                                                    unfocusedTextColor = SoftWhite,
                                                    focusedBorderColor = CaptainYellow,
                                                    unfocusedBorderColor = DarkBorder
                                                )
                                            )
                                            Button(
                                                onClick = {
                                                    if (pinInput.isBlank() || pinInput == currentReq.otp) {
                                                        tripStage = 3
                                                        onShowMessage("PIN Verified! Ride started toward ${currentReq.dropTitle}")
                                                    } else {
                                                        onShowMessage("Wrong PIN. Rider PIN is ${currentReq.otp}")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite)
                                            ) { Text("VERIFY PIN & START RIDE", fontWeight = FontWeight.ExtraBold) }
                                        }
                                    }
                                }

                                else -> {
                                    // STAGE 3: TRIP IN PROGRESS -> COMPLETE & COLLECT
                                    Surface(shape = RoundedCornerShape(22.dp), color = DarkCardSurface, modifier = Modifier.border(1.5.dp, SuccessGreen, RoundedCornerShape(22.dp))) {
                                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                                Column {
                                                    Text("ON TRIP TO DROP", style = MaterialTheme.typography.labelSmall, color = SuccessGreen, fontWeight = FontWeight.ExtraBold)
                                                    Text(currentReq.dropTitle, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                                }
                                                Text("₹${currentReq.fare}", style = MaterialTheme.typography.headlineMedium, color = CaptainYellow, fontWeight = FontWeight.ExtraBold)
                                            }
                                            Text("Collect via ${currentReq.paymentType} • Net Captain Payout: ₹${currentReq.partnerEarnings}", style = MaterialTheme.typography.bodySmall, color = SkyBlue)
                                            Button(
                                                onClick = {
                                                    tripStage = 0
                                                    pinInput = ""
                                                    onNextRequest(currentReq.partnerEarnings)
                                                    onShowMessage("Trip Completed! +₹${currentReq.partnerEarnings} credited.")
                                                },
                                                modifier = Modifier.fillMaxWidth().height(54.dp).testTag("driver_complete_trip_btn"),
                                                shape = RoundedCornerShape(14.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = CaptainYellow, contentColor = CaptainCharcoal)
                                            ) { Text("COMPLETE RIDE (+₹${currentReq.partnerEarnings})", fontWeight = FontWeight.ExtraBold) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // EARNINGS, INCENTIVES & WITHDRAWAL
                    item {
                        Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DarkKpiTile("Today", "₹${uiState.driverEarningsToday}", CaptainYellow, Modifier.weight(1f))
                                DarkKpiTile("Trips", "${uiState.driverRidesToday}", SkyBlue, Modifier.weight(1f))
                                DarkKpiTile("Incentives", "₹${uiState.driverIncentivesToday}", SuccessGreen, Modifier.weight(1f))
                                DarkKpiTile("Rating", "4.92★", OrangeAccent, Modifier.weight(1f))
                            }
                            Surface(shape = RoundedCornerShape(22.dp), color = DarkCardSurface) {
                                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Captain Wallet Balance", style = MaterialTheme.typography.labelMedium, color = SkyBlue)
                                    AnimatedWalletAmount(uiState.appState.walletBalance, color = CaptainYellow, style = MaterialTheme.typography.displayMedium)
                                    Text("Zero-Fee Instant Bank Transfer Available", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                                    Button(
                                        onClick = { onShowMessage("Simulated Instant Bank Payout initiated to HDFC ****4821") },
                                        modifier = Modifier.fillMaxWidth().height(50.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = CaptainYellow, contentColor = CaptainCharcoal),
                                        shape = RoundedCornerShape(14.dp)
                                    ) { Text("Withdraw to Bank (Simulated)", fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                            OutlinedButton(
                                onClick = { onNavigate(AppRoute.OrderHistory("RIDE")) },
                                modifier = Modifier.fillMaxWidth().height(48.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite)
                            ) { Text("View Completed Ride History") }
                        }
                    }
                }

                else -> {
                    // ELIGIBLE SERVICES & DOCUMENT VAULT
                    item {
                        Column(modifier = Modifier.padding(horizontal = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Active Vehicle Services", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                            eligibleServices.forEach { (name, docStatus, eligible) ->
                                val active = name in uiState.driverEnabledServices && eligible
                                Surface(shape = RoundedCornerShape(16.dp), color = DarkCardSurface) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(name, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                                            Text(docStatus, style = MaterialTheme.typography.bodySmall, color = if (eligible) SuccessGreen else OrangeAccent)
                                        }
                                        Switch(checked = active, enabled = eligible, onCheckedChange = { onToggleService(name) })
                                    }
                                }
                            }
                            Surface(shape = RoundedCornerShape(18.dp), color = DarkCardElevated) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Verified Captain Documents", style = MaterialTheme.typography.titleMedium, color = CaptainYellow)
                                    Text("• Driving License (DL-KA01-2029) — Verified\n• Vehicle RC & Insurance — Active till 2027\n• Helmet & Safety Verification — Passed", style = MaterialTheme.typography.bodySmall, color = SoftWhite)
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
fun FoodDeliveryPartnerPanelScreen(
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onToggleOnline: (Boolean) -> Unit,
    onNextDelivery: (Int) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val isOnline = uiState.appState.deliveryPartnerOnline
    val job = DemoCatalog.incomingFoodDeliveries[uiState.activeFoodDeliveryIndex % DemoCatalog.incomingFoodDeliveries.size]
    var stage by remember(uiState.activeFoodDeliveryIndex) { mutableIntStateOf(0) }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF431407), DeepNavy)))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SoftWhite.copy(alpha = 0.1f))
                        ) { Icon(Icons.Rounded.Menu, "Switch Panel", tint = OrangeAccent) }
                        Spacer(Modifier.width(10.dp))
                        Character3DPortrait(
                            drawableRes = R.drawable.img_char_food_rider_1795426023084,
                            contentDescription = "3D Food Delivery Partner",
                            borderColor = OrangeAccent,
                            size = 48.dp,
                            bobbing = false
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DELIVERY FLEET", style = MaterialTheme.typography.labelSmall, color = OrangeAccent)
                            Text("Food Delivery Partner", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        }
                        Switch(checked = isOnline, onCheckedChange = onToggleOnline)
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DarkKpiTile("Today Payout", "₹${uiState.deliveryPartnerEarningsToday}", OrangeAccent, Modifier.weight(1f))
                        DarkKpiTile("Delivered", "${uiState.deliveryPartnerCompletedToday}", SuccessGreen, Modifier.weight(1f))
                        DarkKpiTile("Surge Bonus", "₹220", PremiumYellow, Modifier.weight(1f))
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxWidth().height(205.dp),
                        pickupLabel = job.restaurantOrShop,
                        dropLabel = job.dropTitle,
                        distanceKm = job.tripDistKm,
                        etaMinutes = 16,
                        isDarkStyle = true,
                        showMatchingRadar = stage == 0 && isOnline
                    )

                    Surface(shape = RoundedCornerShape(22.dp), color = DarkCardSurface, modifier = Modifier.border(1.2.dp, OrangeAccent, RoundedCornerShape(22.dp))) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(job.restaurantOrShop, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                Text("+₹${job.partnerEarnings}", style = MaterialTheme.typography.titleLarge, color = SuccessGreen, fontWeight = FontWeight.ExtraBold)
                            }
                            Text("Customer: ${job.customerName} • ${job.dropTitle}", style = MaterialTheme.typography.bodySmall, color = SkyBlue)
                            Text("Delivery OTP: ${job.otp} • Order Value: ₹${job.fare} (${job.paymentType})", style = MaterialTheme.typography.labelSmall, color = PremiumYellow)

                            when (stage) {
                                0 -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { onNextDelivery(0) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger)) { Text("Skip") }
                                    Button(
                                        onClick = { stage = 1; onShowMessage("Heading to ${job.restaurantOrShop} for pickup") },
                                        modifier = Modifier.weight(1.4f),
                                        colors = ButtonDefaults.buttonColors(containerColor = OrangeAccent, contentColor = DeepNavy)
                                    ) { Text("Accept Order", fontWeight = FontWeight.ExtraBold) }
                                }
                                1 -> Button(
                                    onClick = { stage = 2; onShowMessage("Food picked up! Navigating to ${job.customerName}") },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = DeepNavy)
                                ) { Text("Confirm Restaurant Pickup", fontWeight = FontWeight.Bold) }
                                else -> Button(
                                    onClick = {
                                        stage = 0
                                        onNextDelivery(job.partnerEarnings)
                                        onShowMessage("Delivered with OTP ${job.otp}! +₹${job.partnerEarnings} earned.")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite)
                                ) { Text("Verify OTP (${job.otp}) & Complete Delivery", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }

                    Surface(shape = RoundedCornerShape(18.dp), color = DarkCardSurface) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Recent Payout Settlements", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                            listOf("Yesterday • 19 Deliveries" to "₹1,420", "Mon • 15 Deliveries" to "₹1,110", "Sun Peak Bonus" to "₹450").forEach { (k, v) ->
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(k, style = MaterialTheme.typography.bodySmall, color = Color(0xFF9FB3C8))
                                    Text(v, style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
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
fun DarkKpiTile(label: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.verticalGradient(listOf(DarkCardElevated, DarkCardSurface)))
            .glassSheenOverlay(0.12f)
            .border(1.1.dp, accent.copy(alpha = 0.42f), RoundedCornerShape(16.dp))
            .padding(horizontal = 8.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7C6E0), maxLines = 1)
    }
}
