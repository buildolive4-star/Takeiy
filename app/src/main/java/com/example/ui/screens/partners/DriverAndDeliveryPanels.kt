package com.example.ui.screens.partners

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DemoCatalog
import com.example.model.AppRoute
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState

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
    var acceptedJob by remember(uiState.activeDriverRequestIndex) { mutableStateOf(false) }
    var otpVerify by remember(uiState.activeDriverRequestIndex) { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Radar, 1: Services & Docs, 2: Earnings

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
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Dark Navy Command Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(DeepNavyLight, DeepNavy)))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SoftWhite.copy(alpha = 0.1f)).testTag("driver_menu_button")
                        ) {
                            Icon(Icons.Rounded.Menu, "Switch Panel", tint = PremiumYellow)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("CAPTAIN COMMAND", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                            Text("Driver / Captain Panel", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(if (isOnline) SuccessGreen.copy(alpha = 0.22f) else CoralDanger.copy(alpha = 0.22f))
                                .border(1.dp, if (isOnline) SuccessGreen else CoralDanger, RoundedCornerShape(50))
                                .clickable { onToggleOnline(!isOnline) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("driver_online_toggle")
                        ) {
                            Box(Modifier.size(8.dp).clip(CircleShape).background(if (isOnline) SuccessGreen else CoralDanger))
                            Spacer(Modifier.width(6.dp))
                            Text(if (isOnline) "ONLINE" else "OFFLINE", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // KPI Ribbon
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        DarkKpiTile("Today Earnings", "₹${uiState.driverEarningsToday}", PremiumYellow, Modifier.weight(1f))
                        DarkKpiTile("Trips Done", "${uiState.driverRidesToday}", SkyBlue, Modifier.weight(1f))
                        DarkKpiTile("Incentives", "₹${uiState.driverIncentivesToday}", SuccessGreen, Modifier.weight(1f))
                        DarkKpiTile("Rating", "4.92★", OrangeAccent, Modifier.weight(1f))
                    }

                    // Mode Tabs
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Live Radar", "Services & Docs", "Earnings").forEachIndexed { idx, title ->
                            FilterChip(
                                selected = selectedTab == idx,
                                onClick = { selectedTab = idx },
                                label = { Text(title, style = MaterialTheme.typography.labelMedium) }
                            )
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SimulatedMapCanvas(
                            modifier = Modifier.fillMaxWidth().height(205.dp),
                            pickupLabel = currentReq.pickupTitle,
                            dropLabel = currentReq.dropTitle,
                            distanceKm = currentReq.tripDistKm,
                            etaMinutes = (currentReq.tripDistKm * 2.5).toInt(),
                            isDarkStyle = true,
                            showMatchingRadar = isOnline && !acceptedJob
                        )

                        if (!isOnline) {
                            Surface(shape = RoundedCornerShape(20.dp), color = DarkCardSurface) {
                                Column(modifier = Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Rounded.PowerSettingsNew, null, tint = CoralDanger, modifier = Modifier.size(42.dp))
                                    Spacer(Modifier.height(8.dp))
                                    Text("You are currently Offline", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                                    Text("Toggle ONLINE above to receive Bike Taxi, Auto & Delivery requests.", style = MaterialTheme.typography.bodySmall, color = Color(0xFF9FB3C8))
                                }
                            }
                        } else if (!acceptedJob) {
                            // Animated Incoming Request Card
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = DarkCardSurface,
                                modifier = Modifier.border(1.5.dp, PremiumYellow, RoundedCornerShape(22.dp))
                            ) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconOrb3D(Icons.Rounded.Bolt, DeepNavy, PremiumYellow, OrangeAccent, size = 40.dp, iconSize = 20.dp)
                                            Spacer(Modifier.width(10.dp))
                                            Column {
                                                Text("INCOMING ${currentReq.serviceType.uppercase()}", style = MaterialTheme.typography.labelSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                                                Text(currentReq.customerName, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${currentReq.partnerEarnings}", style = MaterialTheme.typography.headlineMedium, color = SuccessGreen, fontWeight = FontWeight.ExtraBold)
                                            Text("Fare ₹${currentReq.fare} • ${currentReq.paymentType}", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                                        }
                                    }
                                    HorizontalDivider(color = DarkBorder)
                                    Text("Pickup (${currentReq.pickupDistKm} km away): ${currentReq.pickupTitle}", style = MaterialTheme.typography.bodySmall, color = SoftWhite)
                                    Text("Drop (${currentReq.tripDistKm} km trip): ${currentReq.dropTitle}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8D4EB))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                onNextRequest(0)
                                                onShowMessage("Request declined • Searching next nearby request")
                                            },
                                            modifier = Modifier.weight(1f).height(48.dp).testTag("driver_reject_btn"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger)
                                        ) {
                                            Text("Reject", fontWeight = FontWeight.Bold)
                                        }
                                        Button(
                                            onClick = {
                                                acceptedJob = true
                                                onShowMessage("Trip accepted! Navigate to ${currentReq.pickupTitle}")
                                            },
                                            modifier = Modifier.weight(1.4f).height(48.dp).testTag("driver_accept_btn"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite)
                                        ) {
                                            Text("Accept Trip", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        } else {
                            // Active Trip Panel with Call, Chat, OTP Complete
                            Surface(shape = RoundedCornerShape(22.dp), color = DarkCardSurface, modifier = Modifier.border(1.dp, SkyBlue, RoundedCornerShape(22.dp))) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Active Trip • ${currentReq.customerName}", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                        StatusPill("ON_THE_WAY")
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = { onShowMessage("Simulated call to ${currentReq.customerName}") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite)
                                        ) { Icon(Icons.Rounded.Call, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("Call") }
                                        OutlinedButton(
                                            onClick = { onShowMessage("Chat opened: 'I am 2 mins away at Gate 2!'") },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyBlue)
                                        ) { Icon(Icons.Rounded.Chat, null, Modifier.size(16.dp)); Spacer(Modifier.width(4.dp)); Text("In-App Chat") }
                                    }
                                    OutlinedTextField(
                                        value = otpVerify,
                                        onValueChange = { otpVerify = it },
                                        label = { Text("Customer OTP (${currentReq.otp})") },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = SoftWhite,
                                            unfocusedTextColor = SoftWhite,
                                            focusedBorderColor = PremiumYellow,
                                            unfocusedBorderColor = DarkBorder
                                        )
                                    )
                                    Button(
                                        onClick = {
                                            acceptedJob = false
                                            onNextRequest(currentReq.partnerEarnings)
                                            onShowMessage("Trip Completed! +₹${currentReq.partnerEarnings} credited to Captain Wallet.")
                                        },
                                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("driver_complete_trip_btn"),
                                        colors = ButtonDefaults.buttonColors(containerColor = PremiumYellow, contentColor = DeepNavy)
                                    ) {
                                        Text("Verify OTP & Complete (+₹${currentReq.partnerEarnings})", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Eligible Service Toggles", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        eligibleServices.forEach { (name, docStatus, eligible) ->
                            val active = name in uiState.driverEnabledServices && eligible
                            Surface(shape = RoundedCornerShape(16.dp), color = DarkCardSurface) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(name, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                                        Text(docStatus, style = MaterialTheme.typography.bodySmall, color = if (eligible) SuccessGreen else OrangeAccent)
                                    }
                                    Switch(
                                        checked = active,
                                        enabled = eligible,
                                        onCheckedChange = { onToggleService(name) }
                                    )
                                }
                            }
                        }
                        Surface(shape = RoundedCornerShape(18.dp), color = DarkCardElevated) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text("Document Verification Vault", style = MaterialTheme.typography.titleMedium, color = PremiumYellow)
                                Text("• Driving License (DL-KA01-2029) — Verified\n• Vehicle RC & Insurance — Active till 2027\n• Police & KYC Background Check — Approved", style = MaterialTheme.typography.bodySmall, color = SoftWhite)
                            }
                        }
                    }
                }
            } else {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Surface(shape = RoundedCornerShape(20.dp), color = DarkCardSurface) {
                            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Captain Wallet Balance", style = MaterialTheme.typography.labelMedium, color = SkyBlue)
                                AnimatedWalletAmount(uiState.appState.walletBalance, color = PremiumYellow, style = MaterialTheme.typography.displayMedium)
                                Text("Weekly Streak Bonus: ₹350 unlocked (14/12 rides)", style = MaterialTheme.typography.bodySmall, color = SuccessGreen)
                                Button(
                                    onClick = { onShowMessage("Simulated Instant Bank Payout initiated to HDFC ****4821") },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = DeepNavy)
                                ) {
                                    Text("Withdraw to Bank (Simulated)", fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                        OutlinedButton(
                            onClick = { onNavigate(AppRoute.OrderHistory("RIDE")) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite)
                        ) {
                            Text("View Complete Ride History")
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
    var stage by remember(uiState.activeFoodDeliveryIndex) { mutableIntStateOf(0) } // 0 incoming, 1 pickup, 2 delivering

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
                        ) {
                            Icon(Icons.Rounded.Menu, "Switch Panel", tint = OrangeAccent)
                        }
                        Spacer(Modifier.width(12.dp))
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
                                    OutlinedButton(onClick = { onNextDelivery(0) }, modifier = Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = CoralDanger)) {
                                        Text("Skip")
                                    }
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

                    // Payout History Card
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
            .clip(RoundedCornerShape(14.dp))
            .background(DarkCardSurface)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium, color = accent, fontWeight = FontWeight.ExtraBold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7C6E0), maxLines = 1)
    }
}
