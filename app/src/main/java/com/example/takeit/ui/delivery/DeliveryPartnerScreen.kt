package com.example.takeit.ui.delivery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.components.LeafletTrackingMap
import com.example.takeit.ui.components.Nav3DTabItem
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DFloatingNavBar
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.dialPhone
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.ui.components.sendSmsIntent
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel
import kotlinx.coroutines.delay

private sealed class OtpJobTarget {
    data class Food(val order: FoodOrder) : OtpJobTarget()
    data class RideStart(val ride: RideBooking) : OtpJobTarget()
    data class RideComplete(val ride: RideBooking) : OtpJobTarget()
    data class Parcel(val parcel: ParcelOrder) : OtpJobTarget()
}

@Composable
fun DeliveryPartnerRoot(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    if (!uiState.hasUnlockedDeliveryAccess) {
        DeliveryPartnerGateScreen(
            uiState = uiState,
            viewModel = viewModel,
            onVerifyPasscode = viewModel::verifyCoAdminPasscode,
            onExitToApp = if (uiState.appVariant != TakeItAppVariant.DELIVERY) {
                { viewModel.exitDeliveryMode() }
            } else null,
            onSignOut = onSignOut
        )
        return
    }

    var otpTarget by remember { mutableStateOf<OtpJobTarget?>(null) }

    Scaffold(
        containerColor = DeliveryBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color(0xFF03090C),
                borderColor = DeliveryEmerald.copy(alpha = 0.45f),
                cornerRadius = 24.dp,
                depth = 5.dp,
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(DeliverySurface, Color(0xFF0B2A26))))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .shadow(8.dp, CircleShape, ambientColor = DeliveryEmerald, spotColor = DeliveryEmerald)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(DeliveryEmerald.lighten3D(0.25f), DeliveryEmerald)))
                            .padding(2.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_char_food_rider_1795426023084),
                            contentDescription = "Take It Captain Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.myCaptainProfile?.fullName?.ifBlank { null }
                                ?: uiState.activeCoAdmin?.name?.ifBlank { uiState.displayName } ?: uiState.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = DeliveryInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (uiState.deliveryPartnerOnline) DeliveryEmerald else AdminCrimson)
                            )
                            Text(
                                text = if (uiState.deliveryPartnerOnline) "CAPTAIN ONLINE • READY" else "CAPTAIN OFFLINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (uiState.deliveryPartnerOnline) DeliveryEmerald else DeliveryMuted,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Switch(
                        checked = uiState.deliveryPartnerOnline,
                        onCheckedChange = viewModel::toggleDeliveryOnline,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DeliveryBg,
                            checkedTrackColor = DeliveryEmerald,
                            uncheckedThumbColor = DeliveryMuted,
                            uncheckedTrackColor = DeliveryCard
                        ),
                        modifier = Modifier.testTag("delivery_online_switch")
                    )

                    if (uiState.appVariant != TakeItAppVariant.DELIVERY) {
                        Spacer(Modifier.width(6.dp))
                        FilledTonalButton(
                            onClick = { viewModel.exitDeliveryMode() },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 9.dp, vertical = 4.dp),
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = DeliveryCard,
                                contentColor = DeliveryInk
                            ),
                            modifier = Modifier
                                .height(36.dp)
                                .testTag("delivery_exit_to_app_btn")
                        ) {
                            Text("App", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.width(6.dp))
                    TakeIt3DIconOrb(
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        containerColor = DeliveryCard,
                        depthColor = Color.Black,
                        iconTint = AdminCrimson,
                        size = 38.dp,
                        contentDescription = "Sign Out",
                        onClick = onSignOut,
                        modifier = Modifier.testTag("delivery_sign_out_btn")
                    )
                }
            }
        },
        bottomBar = {
            TakeIt3DFloatingNavBar(
                items = DeliveryTab.entries.map { tab ->
                    val icon = when (tab) {
                        DeliveryTab.TASKS -> Icons.Rounded.Inbox
                        DeliveryTab.ACTIVE -> Icons.Rounded.Map
                        DeliveryTab.EARNINGS -> Icons.Rounded.BarChart
                        DeliveryTab.KYC -> Icons.Rounded.Badge
                        DeliveryTab.ACCOUNT -> Icons.Rounded.Person
                    }
                    Nav3DTabItem(
                        id = tab.name,
                        label = tab.label,
                        icon = icon,
                        selected = uiState.deliveryTab == tab,
                        testTag = "delivery_nav_${tab.name.lowercase()}",
                        onClick = { viewModel.selectDeliveryTab(tab) }
                    )
                },
                containerColor = DeliverySurface,
                activePodColor = DeliveryEmerald,
                activeContentColor = DeliveryBg,
                inactiveColor = DeliveryMuted,
                borderColor = DeliveryBorder
            )
        }
    ) { innerPadding ->
        TakeIt3DBackdrop(
            accentPrimary = DeliveryEmerald,
            accentSecondary = DeliveryAmber,
            darkMode = true,
            baseColor = DeliveryBg,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
        ) {
            when (uiState.deliveryTab) {
                DeliveryTab.TASKS -> DeliveryTasksTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    onRequestOtp = { otpTarget = it },
                    onOpenRouteMap = { viewModel.selectDeliveryTab(DeliveryTab.ACTIVE) }
                )
                DeliveryTab.ACTIVE -> DeliveryActiveRouteTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    onRequestOtp = { otpTarget = it }
                )
                DeliveryTab.EARNINGS -> CaptainEarningsTab(uiState = uiState, viewModel = viewModel)
                DeliveryTab.KYC -> CaptainKycTab(uiState = uiState, viewModel = viewModel)
                DeliveryTab.ACCOUNT -> CaptainAccountTab(
                    uiState = uiState,
                    viewModel = viewModel,
                    onSignOut = onSignOut
                )
            }
        }
    }

    otpTarget?.let { target ->
        DeliveryOtpVerifyDialog(
            target = target,
            onDismiss = { otpTarget = null },
            onVerifyAndComplete = { enteredOtp ->
                when (target) {
                    is OtpJobTarget.Food -> viewModel.deliveryCompleteOrderWithOtp(target.order, enteredOtp) {
                        otpTarget = null
                    }
                    is OtpJobTarget.RideStart -> viewModel.captainStartRideWithOtp(target.ride, enteredOtp) {
                        otpTarget = null
                    }
                    is OtpJobTarget.RideComplete -> viewModel.deliveryCompleteRideWithOtp(target.ride, enteredOtp) {
                        otpTarget = null
                    }
                    is OtpJobTarget.Parcel -> viewModel.deliveryCompleteParcelWithOtp(target.parcel, enteredOtp) {
                        otpTarget = null
                    }
                }
            }
        )
    }
}

@Composable
private fun DeliveryPartnerGateScreen(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onVerifyPasscode: (String) -> Unit,
    onExitToApp: (() -> Unit)?,
    onSignOut: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var showKycModal by remember { mutableStateOf(false) }

    TakeIt3DBackdrop(
        accentPrimary = DeliveryEmerald,
        accentSecondary = DeliveryAmber,
        darkMode = true,
        baseColor = DeliveryBg
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color(0xFF03090C),
                borderColor = DeliveryEmerald.copy(alpha = 0.55f),
                cornerRadius = 28.dp,
                depth = 7.dp,
                shadowElevation = 18.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(94.dp)
                            .shadow(16.dp, CircleShape, ambientColor = DeliveryEmerald, spotColor = DeliveryEmerald)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(DeliveryEmerald.lighten3D(0.25f), DeliveryEmerald)))
                            .padding(3.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_char_food_rider_1795426023084),
                            contentDescription = "Take It Captain",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF064E3B),
                        border = BorderStroke(1.dp, DeliveryEmerald)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, null, tint = DeliveryEmerald, modifier = Modifier.size(15.dp))
                            Text(
                                "CAPTAIN PARTNER SECURITY VAULT",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeliveryEmerald,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Take It Captain Panel",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeliveryInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Signed in as ${uiState.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeliveryAmber,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Enter the Captain passcode assigned to your Gmail by Admin or Co-Admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeliveryMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("Captain Passcode") },
                        placeholder = { Text("Enter assigned password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = DeliveryMuted
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = DeliveryEmerald,
                            unfocusedBorderColor = DeliveryBorder,
                            focusedTextColor = DeliveryInk,
                            unfocusedTextColor = DeliveryInk,
                            focusedContainerColor = DeliveryCard,
                            unfocusedContainerColor = DeliveryCard
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_passcode_input")
                    )

                    if (!uiState.coAdminAuthError.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3F1219),
                            border = BorderStroke(1.dp, AdminCrimson),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.coAdminAuthError,
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFFCA5A5),
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    TakeIt3DButton(
                        onClick = { onVerifyPasscode(passcode) },
                        enabled = passcode.isNotBlank() && !uiState.isCheckingCoAdmin,
                        containerColor = DeliveryEmerald,
                        depthColor = DeliveryEmerald.darken3D(0.45f),
                        contentColor = DeliveryBg,
                        cornerRadius = 16.dp,
                        depth = 5.dp,
                        height = 52.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delivery_unlock_btn")
                    ) {
                        if (uiState.isCheckingCoAdmin) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = DeliveryBg, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Rounded.LockOpen, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Unlock Captain Panel", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { showKycModal = true },
                        border = BorderStroke(1.dp, RideYellow.copy(alpha = 0.7f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("gate_apply_kyc_btn")
                    ) {
                        Icon(Icons.Rounded.Badge, null, tint = RideYellow, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (uiState.myCaptainProfile == null) "New Captain? Apply via KYC" else "View / Update Captain KYC (${uiState.myCaptainProfile.approvalStatus})",
                            color = RideYellow,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (onExitToApp != null) {
                        Spacer(Modifier.height(6.dp))
                        TextButton(onClick = onExitToApp) {
                            Text("Back to Customer App", color = DeliveryAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(onClick = onSignOut) {
                        Text("Switch Google Account", color = DeliveryMuted, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }

    if (showKycModal) {
        CaptainKycRegistrationDialog(
            uiState = uiState,
            viewModel = viewModel,
            onDismiss = { showKycModal = false }
        )
    }
}

@Composable
private fun DeliveryTasksTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onRequestOtp: (OtpJobTarget) -> Unit,
    onOpenRouteMap: () -> Unit
) {
    val context = LocalContext.current
    var declinedRideIds by remember { mutableStateOf<Set<String>>(emptySet()) }

    val activeOrders = remember(uiState.allOrders) {
        uiState.allOrders.filter { it.status in listOf("PLACED", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY") }
    }
    val activeRides = remember(uiState.allRides, declinedRideIds) {
        uiState.allRides.filter {
            it.status in listOf("REQUESTED", "CAPTAIN_ASSIGNED", "ARRIVING", "ON_TRIP") &&
                !(it.status == "REQUESTED" && it.id in declinedRideIds)
        }
    }
    val activeParcels = remember(uiState.allParcels) {
        uiState.allParcels.filter { it.status in listOf("BOOKED", "PARTNER_ASSIGNED", "PICKED_UP", "IN_TRANSIT") }
    }

    // Today's quick stats banner
    val completedCount = remember(uiState.allOrders, uiState.allRides, uiState.allParcels) {
        uiState.allOrders.count { it.status == "DELIVERED" } +
            uiState.allRides.count { it.status == "COMPLETED" } +
            uiState.allParcels.count { it.status == "DELIVERED" }
    }
    val todayEstEarnings = remember(uiState.allOrders, uiState.allRides, uiState.allParcels) {
        uiState.allOrders.filter { it.status == "DELIVERED" }.sumOf { (it.deliveryFee + 25).coerceAtLeast(35) } +
            uiState.allRides.filter { it.status == "COMPLETED" }.sumOf { it.driverEarnings.takeIf { d -> d > 0 } ?: (it.totalFare * 0.85).toInt() } +
            uiState.allParcels.filter { it.status == "DELIVERED" }.sumOf { it.driverEarnings.takeIf { d -> d > 0 } ?: (it.totalFare * 0.82).toInt() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color(0xFF040B0E),
                borderColor = DeliveryEmerald.copy(alpha = 0.45f),
                cornerRadius = 22.dp,
                depth = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Captain Duty Dashboard", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                            Text(
                                "${activeOrders.size} Food • ${activeRides.size} RideX • ${activeParcels.size} MoveX live",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeliveryEmerald
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF064E3B),
                            border = BorderStroke(1.dp, DeliveryEmerald)
                        ) {
                            Text(
                                "₹$todayEstEarnings Earned • $completedCount Trips",
                                color = RideYellow,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }

                    // 3 Vertical Switcher Pills (Food Captain | RideX Captain | MoveX Captain)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(CaptainVertical.entries) { vert ->
                            val selected = uiState.captainVertical == vert
                            val accent = when (vert) {
                                CaptainVertical.FOOD -> DeliveryEmerald
                                CaptainVertical.RIDE -> RideYellow
                                CaptainVertical.TRANSPORT -> TransportOrange
                            }
                            FilterChip(
                                selected = selected,
                                onClick = { viewModel.selectCaptainVertical(vert) },
                                leadingIcon = {
                                    Icon(
                                        when (vert) {
                                            CaptainVertical.FOOD -> Icons.Rounded.Fastfood
                                            CaptainVertical.RIDE -> Icons.Rounded.TwoWheeler
                                            CaptainVertical.TRANSPORT -> Icons.Rounded.LocalShipping
                                        },
                                        contentDescription = vert.label,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                label = { Text(vert.label, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = RideCharcoal,
                                    selectedLeadingIconColor = RideCharcoal,
                                    containerColor = DeliveryCard,
                                    labelColor = DeliveryInk,
                                    iconColor = accent
                                ),
                                modifier = Modifier.testTag("captain_vertical_${vert.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        }

        if (!uiState.deliveryPartnerOnline) {
            item {
                TakeIt3DCard(
                    containerColor = DeliveryCard,
                    depthColor = Color.Black,
                    borderColor = DeliveryAmber,
                    cornerRadius = 20.dp,
                    depth = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.PauseCircle, null, tint = DeliveryAmber, modifier = Modifier.size(36.dp))
                        Text("You are currently Offline", fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                        Text("Turn ON the header Online switch to accept Food, RideX & MoveX jobs.", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            when (uiState.captainVertical) {
                CaptainVertical.FOOD -> {
                    if (activeOrders.isEmpty()) {
                        item { EmptyDispatchCard("No active Food Delivery orders right now.") }
                    } else {
                        items(activeOrders, key = { "del_food_${it.id}" }) { order ->
                            TakeIt3DCard(
                                containerColor = DeliverySurface,
                                depthColor = Color(0xFF040A0D),
                                borderColor = DeliveryBorder,
                                cornerRadius = 22.dp,
                                depth = 5.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(shape = RoundedCornerShape(8.dp), color = TakeItRed.copy(alpha = 0.2f)) {
                                                Text("FOOD", color = Color(0xFFFCA5A5), fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                            }
                                            Text(order.orderNumber, fontWeight = FontWeight.ExtraBold, color = DeliveryAmber)
                                        }
                                        Surface(shape = RoundedCornerShape(50), color = DeliveryCard) {
                                            Text(
                                                order.status.replace("_", " "),
                                                color = DeliveryEmerald,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text("${order.restaurantName} → ${order.customerName}", fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                                    Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                                    Text("Drop: ${order.deliveryAddress}", style = MaterialTheme.typography.labelSmall, color = DeliveryInk)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Bill ₹${order.totalAmount} (${order.paymentMethod})", fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            if (order.customerPhone.isNotBlank()) {
                                                FilledTonalIconButton(
                                                    onClick = { dialPhone(context, order.customerPhone) },
                                                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = DeliveryCard, contentColor = DeliveryEmerald),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(Icons.Rounded.Call, "Call Customer", modifier = Modifier.size(18.dp))
                                                }
                                            }
                                            FilledTonalButton(
                                                onClick = onOpenRouteMap,
                                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = DeliveryCard, contentColor = DeliveryAmber),
                                                contentPadding = PaddingValues(horizontal = 10.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Icon(Icons.Rounded.Map, null, modifier = Modifier.size(15.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Map", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    if (order.status != "OUT_FOR_DELIVERY") {
                                        TakeIt3DButton(
                                            onClick = { viewModel.deliveryClaimOrder(order) },
                                            containerColor = DeliveryAmber,
                                            depthColor = DeliveryAmber.darken3D(0.4f),
                                            contentColor = DeliveryBg,
                                            cornerRadius = 14.dp,
                                            depth = 4.dp,
                                            height = 44.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("delivery_claim_food_${order.id}")
                                        ) {
                                            Icon(Icons.Rounded.TwoWheeler, null, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Pick Up & Deliver", fontWeight = FontWeight.ExtraBold)
                                        }
                                    } else {
                                        TakeIt3DButton(
                                            onClick = { onRequestOtp(OtpJobTarget.Food(order)) },
                                            containerColor = DeliveryEmerald,
                                            depthColor = DeliveryEmerald.darken3D(0.4f),
                                            contentColor = DeliveryBg,
                                            cornerRadius = 14.dp,
                                            depth = 4.dp,
                                            height = 44.dp,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .testTag("delivery_complete_food_${order.id}")
                                        ) {
                                            Icon(Icons.Rounded.Verified, null, modifier = Modifier.size(17.dp))
                                            Spacer(Modifier.width(6.dp))
                                            Text("Verify Customer OTP & Deliver", fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                CaptainVertical.RIDE -> {
                    if (activeRides.isEmpty()) {
                        item { EmptyDispatchCard("No active RideX requests nearby right now.") }
                    } else {
                        items(activeRides, key = { "del_ride_${it.id}" }) { ride ->
                            RideCaptainDispatchCard(
                                ride = ride,
                                onAccept = { viewModel.deliveryClaimRide(ride) },
                                onDecline = { declinedRideIds = declinedRideIds + ride.id },
                                onArriving = { viewModel.adminUpdateRide(ride.id, "ARRIVING") },
                                onStartOtp = { onRequestOtp(OtpJobTarget.RideStart(ride)) },
                                onCompleteTrip = {
                                    if (ride.otpVerified) {
                                        viewModel.adminUpdateRide(ride.id, "COMPLETED")
                                    } else {
                                        onRequestOtp(OtpJobTarget.RideComplete(ride))
                                    }
                                },
                                onCallCustomer = { dialPhone(context, ride.customerPhone) },
                                onOpenMap = onOpenRouteMap
                            )
                        }
                    }
                }

                CaptainVertical.TRANSPORT -> {
                    if (activeParcels.isEmpty()) {
                        item { EmptyDispatchCard("No active MoveX parcel or cargo bookings right now.") }
                    } else {
                        items(activeParcels, key = { "del_parcel_${it.id}" }) { parcel ->
                            TakeIt3DCard(
                                containerColor = DeliverySurface,
                                depthColor = Color(0xFF040A0D),
                                borderColor = TransportOrange.copy(alpha = 0.5f),
                                cornerRadius = 22.dp,
                                depth = 5.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(shape = RoundedCornerShape(8.dp), color = TransportOrange.copy(alpha = 0.2f)) {
                                                Text("MOVEX • ${parcel.vehicleType}", color = TransportOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                            }
                                            Text(parcel.parcelNumber, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                                        }
                                        Text("Earn ₹${parcel.driverEarnings.takeIf { it > 0 } ?: (parcel.totalFare * 0.82).toInt()}", fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald)
                                    }
                                    Text("${parcel.packageType} • ${parcel.weightKg}kg • ${parcel.bodyType}${if (parcel.fragileFlag) " • FRAGILE" else ""}", color = DeliveryInk, fontWeight = FontWeight.Bold)
                                    Text("Pickup: ${parcel.pickupAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                                    Text("Drop (${parcel.receiverName}): ${parcel.dropAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryInk)
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                        if (parcel.receiverPhone.isNotBlank()) {
                                            FilledTonalIconButton(
                                                onClick = { dialPhone(context, parcel.receiverPhone) },
                                                colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = DeliveryCard, contentColor = DeliveryEmerald),
                                                modifier = Modifier.size(42.dp)
                                            ) {
                                                Icon(Icons.Rounded.Call, "Call Receiver", modifier = Modifier.size(18.dp))
                                            }
                                        }
                                        when (parcel.status) {
                                            "BOOKED" -> TakeIt3DButton(
                                                onClick = { viewModel.deliveryClaimParcel(parcel) },
                                                containerColor = TransportOrange,
                                                depthColor = TransportOrange.darken3D(0.4f),
                                                contentColor = Color.White,
                                                cornerRadius = 14.dp,
                                                depth = 4.dp,
                                                height = 42.dp,
                                                modifier = Modifier.weight(1f).testTag("movex_accept_${parcel.id}")
                                            ) { Text("Accept Shipment", fontWeight = FontWeight.ExtraBold) }
                                            "PARTNER_ASSIGNED" -> TakeIt3DButton(
                                                onClick = { viewModel.adminUpdateParcel(parcel.id, "PICKED_UP") },
                                                containerColor = AdminCyan,
                                                depthColor = AdminCyan.darken3D(0.4f),
                                                contentColor = DeliveryBg,
                                                cornerRadius = 14.dp,
                                                depth = 4.dp,
                                                height = 42.dp,
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Mark Picked Up", fontWeight = FontWeight.ExtraBold) }
                                            "PICKED_UP" -> TakeIt3DButton(
                                                onClick = { viewModel.adminUpdateParcel(parcel.id, "IN_TRANSIT") },
                                                containerColor = DeliveryAmber,
                                                depthColor = DeliveryAmber.darken3D(0.4f),
                                                contentColor = DeliveryBg,
                                                cornerRadius = 14.dp,
                                                depth = 4.dp,
                                                height = 42.dp,
                                                modifier = Modifier.weight(1f)
                                            ) { Text("Start In Transit", fontWeight = FontWeight.ExtraBold) }
                                            else -> TakeIt3DButton(
                                                onClick = { onRequestOtp(OtpJobTarget.Parcel(parcel)) },
                                                containerColor = DeliveryEmerald,
                                                depthColor = DeliveryEmerald.darken3D(0.4f),
                                                contentColor = DeliveryBg,
                                                cornerRadius = 14.dp,
                                                depth = 4.dp,
                                                height = 42.dp,
                                                modifier = Modifier.weight(1f).testTag("movex_otp_complete_${parcel.id}")
                                            ) { Text("Verify Drop OTP & Deliver", fontWeight = FontWeight.ExtraBold) }
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
private fun RideCaptainDispatchCard(
    ride: RideBooking,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
    onArriving: () -> Unit,
    onStartOtp: () -> Unit,
    onCompleteTrip: () -> Unit,
    onCallCustomer: () -> Unit,
    onOpenMap: () -> Unit
) {
    var secondsLeft by remember(ride.id, ride.status) { mutableIntStateOf(30) }
    LaunchedEffect(ride.id, ride.status) {
        if (ride.status == "REQUESTED") {
            secondsLeft = 30
            while (secondsLeft > 0) {
                delay(1000L)
                secondsLeft--
            }
        }
    }

    val captainEarning = ride.driverEarnings.takeIf { it > 0 } ?: (ride.totalFare * 0.85).toInt()

    TakeIt3DCard(
        containerColor = DeliverySurface,
        depthColor = Color(0xFF040A0D),
        borderColor = if (ride.sosActive) AdminCrimson else RideYellow.copy(alpha = 0.55f),
        cornerRadius = 22.dp,
        depth = 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(shape = RoundedCornerShape(8.dp), color = RideYellow.copy(alpha = 0.2f)) {
                        Text("RIDEX • ${ride.vehicleType}", color = RideYellow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                    }
                    Text(ride.rideNumber, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                }
                Text("Earn ₹$captainEarning (Fare ₹${ride.totalFare})", fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald, fontSize = 12.sp)
            }

            if (ride.status == "REQUESTED") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("New Ride Request • Respond fast", style = MaterialTheme.typography.labelSmall, color = RideYellow, fontWeight = FontWeight.Bold)
                        Text("${secondsLeft}s", style = MaterialTheme.typography.labelSmall, color = if (secondsLeft <= 8) AdminCrimson else RideYellow, fontWeight = FontWeight.ExtraBold)
                    }
                    LinearProgressIndicator(
                        progress = { (secondsLeft / 30f).coerceIn(0f, 1f) },
                        color = if (secondsLeft <= 8) AdminCrimson else RideYellow,
                        trackColor = DeliveryCard,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )
                }
            }

            Text("Rider: ${ride.customerName} • ${ride.distanceKm} km (~${ride.durationMin} min)", color = DeliveryInk, fontWeight = FontWeight.Bold)
            Text("Pickup: ${ride.pickupAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
            Text("Drop: ${ride.dropAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryInk)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                if (ride.customerPhone.isNotBlank()) {
                    FilledTonalIconButton(
                        onClick = onCallCustomer,
                        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = DeliveryCard, contentColor = DeliveryEmerald),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Rounded.Call, "Call Rider", modifier = Modifier.size(18.dp))
                    }
                }
                FilledTonalIconButton(
                    onClick = onOpenMap,
                    colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = DeliveryCard, contentColor = RideYellow),
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Rounded.Map, "Open Route Map", modifier = Modifier.size(18.dp))
                }

                when (ride.status) {
                    "REQUESTED" -> {
                        OutlinedButton(
                            onClick = onDecline,
                            border = BorderStroke(1.dp, AdminCrimson),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(0.8f).height(42.dp).testTag("ridex_decline_${ride.id}")
                        ) {
                            Text("Decline", color = AdminCrimson, fontWeight = FontWeight.Bold)
                        }
                        TakeIt3DButton(
                            onClick = onAccept,
                            containerColor = RideYellow,
                            depthColor = RideYellow.darken3D(0.4f),
                            contentColor = RideCharcoal,
                            cornerRadius = 14.dp,
                            depth = 4.dp,
                            height = 42.dp,
                            modifier = Modifier.weight(1.2f).testTag("ridex_accept_${ride.id}")
                        ) {
                            Text("Accept (${secondsLeft}s)", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    "CAPTAIN_ASSIGNED" -> {
                        OutlinedButton(
                            onClick = onArriving,
                            border = BorderStroke(1.dp, AdminCyan),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(0.9f).height(42.dp)
                        ) {
                            Text("Arriving", color = AdminCyan, fontWeight = FontWeight.Bold)
                        }
                        TakeIt3DButton(
                            onClick = onStartOtp,
                            containerColor = DeliveryAmber,
                            depthColor = DeliveryAmber.darken3D(0.4f),
                            contentColor = DeliveryBg,
                            cornerRadius = 14.dp,
                            depth = 4.dp,
                            height = 42.dp,
                            modifier = Modifier.weight(1.3f).testTag("ridex_start_otp_${ride.id}")
                        ) {
                            Text("Start OTP", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    "ARRIVING" -> TakeIt3DButton(
                        onClick = onStartOtp,
                        containerColor = RideYellow,
                        depthColor = RideYellow.darken3D(0.4f),
                        contentColor = RideCharcoal,
                        cornerRadius = 14.dp,
                        depth = 4.dp,
                        height = 42.dp,
                        modifier = Modifier.weight(1f).testTag("ridex_start_otp_${ride.id}")
                    ) {
                        Text("Enter 4-Digit Start OTP", fontWeight = FontWeight.ExtraBold)
                    }
                    else -> TakeIt3DButton(
                        onClick = onCompleteTrip,
                        containerColor = DeliveryEmerald,
                        depthColor = DeliveryEmerald.darken3D(0.4f),
                        contentColor = DeliveryBg,
                        cornerRadius = 14.dp,
                        depth = 4.dp,
                        height = 42.dp,
                        modifier = Modifier.weight(1f).testTag("ridex_complete_${ride.id}")
                    ) {
                        Text(if (ride.otpVerified) "Complete Ride & Collect ₹${ride.totalFare}" else "Verify OTP & Complete", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDispatchCard(message: String) {
    TakeIt3DCard(
        containerColor = DeliverySurface,
        depthColor = Color.Black,
        borderColor = DeliveryBorder,
        cornerRadius = 22.dp,
        depth = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(Icons.Rounded.CheckCircleOutline, null, tint = DeliveryEmerald, modifier = Modifier.size(44.dp))
            Text("All Clear!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
            Text(message, color = DeliveryMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DeliveryActiveRouteTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onRequestOtp: (OtpJobTarget) -> Unit
) {
    val context = LocalContext.current
    val activeFood = uiState.allOrders.firstOrNull { it.status == "OUT_FOR_DELIVERY" }
        ?: uiState.allOrders.firstOrNull { it.status in listOf("PLACED", "CONFIRMED", "PREPARING") }
    val activeRide = uiState.allRides.firstOrNull { it.status in listOf("ON_TRIP", "ARRIVING", "CAPTAIN_ASSIGNED", "REQUESTED") }
    val activeParcel = uiState.allParcels.firstOrNull { it.status in listOf("IN_TRANSIT", "PICKED_UP", "PARTNER_ASSIGNED", "BOOKED") }

    // Prefer the selected vertical first if active, else whichever job exists
    val preferredRide = uiState.captainVertical == CaptainVertical.RIDE && activeRide != null
    val preferredParcel = uiState.captainVertical == CaptainVertical.TRANSPORT && activeParcel != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (activeFood == null && activeRide == null && activeParcel == null) {
            item {
                TakeIt3DCard(
                    containerColor = DeliverySurface,
                    depthColor = Color.Black,
                    borderColor = DeliveryBorder,
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.Map, null, tint = DeliveryEmerald, modifier = Modifier.size(46.dp))
                        Text("No Active Route Selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                        Text("Accept a Food, RideX, or MoveX job from Jobs to see the live OpenStreetMap + Leaflet route here.", color = DeliveryMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            val pickupLat = when {
                preferredRide -> activeRide!!.pickupLat
                preferredParcel -> activeParcel!!.pickupLat
                activeFood != null -> activeFood.pickupLat
                activeRide != null -> activeRide.pickupLat
                else -> activeParcel!!.pickupLat
            }
            val pickupLng = when {
                preferredRide -> activeRide!!.pickupLng
                preferredParcel -> activeParcel!!.pickupLng
                activeFood != null -> activeFood.pickupLng
                activeRide != null -> activeRide.pickupLng
                else -> activeParcel!!.pickupLng
            }
            val dropLat = when {
                preferredRide -> activeRide!!.dropLat
                preferredParcel -> activeParcel!!.dropLat
                activeFood != null -> activeFood.deliveryLat
                activeRide != null -> activeRide.dropLat
                else -> activeParcel!!.dropLat
            }
            val dropLng = when {
                preferredRide -> activeRide!!.dropLng
                preferredParcel -> activeParcel!!.dropLng
                activeFood != null -> activeFood.deliveryLng
                activeRide != null -> activeRide.dropLng
                else -> activeParcel!!.dropLng
            }
            val pickupTitle = when {
                preferredRide -> activeRide!!.pickupAddress
                preferredParcel -> activeParcel!!.pickupAddress
                activeFood != null -> activeFood.restaurantName
                activeRide != null -> activeRide.pickupAddress
                else -> activeParcel!!.pickupAddress
            }
            val dropTitle = when {
                preferredRide -> activeRide!!.dropAddress
                preferredParcel -> activeParcel!!.dropAddress
                activeFood != null -> activeFood.deliveryAddress
                activeRide != null -> activeRide.dropAddress
                else -> activeParcel!!.dropAddress
            }
            val partnerLabel = uiState.myCaptainProfile?.fullName?.ifBlank { null }
                ?: uiState.activeCoAdmin?.name?.ifBlank { uiState.displayName } ?: uiState.displayName

            item {
                TakeIt3DCard(
                    containerColor = DeliverySurface,
                    depthColor = Color(0xFF040A0D),
                    borderColor = DeliveryEmerald,
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Live OpenStreetMap + Leaflet Route", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald)
                        LeafletTrackingMap(
                            pickupLat = pickupLat,
                            pickupLng = pickupLng,
                            pickupTitle = pickupTitle,
                            dropLat = dropLat,
                            dropLng = dropLng,
                            dropTitle = dropTitle,
                            riderLabel = partnerLabel,
                            progressFraction = 0.65f,
                            accentHex = "#10B981",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(290.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                }
            }

            if (!preferredRide && !preferredParcel && activeFood != null) {
                item {
                    TakeIt3DCard(
                        containerColor = DeliverySurface,
                        depthColor = Color.Black,
                        borderColor = DeliveryBorder,
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Active Food Drop • ${activeFood.orderNumber}", fontWeight = FontWeight.ExtraBold, color = DeliveryAmber)
                            Text("${activeFood.restaurantName} → ${activeFood.customerName}", fontWeight = FontWeight.Bold, color = DeliveryInk)
                            Text("Address: ${activeFood.deliveryAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (activeFood.customerPhone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { dialPhone(context, activeFood.customerPhone) },
                                        border = BorderStroke(1.dp, DeliveryEmerald),
                                        modifier = Modifier.weight(0.7f)
                                    ) {
                                        Icon(Icons.Rounded.Call, null, tint = DeliveryEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Call", color = DeliveryEmerald)
                                    }
                                    OutlinedButton(
                                        onClick = { sendSmsIntent(context, activeFood.customerPhone, "Hi, your Take It food order is arriving!") },
                                        border = BorderStroke(1.dp, DeliveryAmber),
                                        modifier = Modifier.weight(0.7f)
                                    ) {
                                        Icon(Icons.Rounded.Sms, null, tint = DeliveryAmber, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("SMS", color = DeliveryAmber)
                                    }
                                }
                                TakeIt3DButton(
                                    onClick = { onRequestOtp(OtpJobTarget.Food(activeFood)) },
                                    containerColor = DeliveryEmerald,
                                    depthColor = DeliveryEmerald.darken3D(0.4f),
                                    contentColor = DeliveryBg,
                                    cornerRadius = 14.dp,
                                    depth = 4.dp,
                                    height = 44.dp,
                                    modifier = Modifier
                                        .weight(1.3f)
                                        .testTag("delivery_route_otp_btn")
                                ) {
                                    Text("OTP & Deliver", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            } else if (!preferredParcel && activeRide != null) {
                item {
                    TakeIt3DCard(
                        containerColor = DeliverySurface,
                        depthColor = Color.Black,
                        borderColor = RideYellow.copy(alpha = 0.5f),
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Active RideX • ${activeRide.rideNumber} (${activeRide.status})", fontWeight = FontWeight.ExtraBold, color = RideYellow)
                            Text("${activeRide.customerName} • ₹${activeRide.totalFare}", fontWeight = FontWeight.Bold, color = DeliveryInk)
                            Text("${activeRide.pickupAddress} → ${activeRide.dropAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                if (activeRide.customerPhone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { dialPhone(context, activeRide.customerPhone) },
                                        border = BorderStroke(1.dp, DeliveryEmerald),
                                        modifier = Modifier.weight(0.7f)
                                    ) {
                                        Icon(Icons.Rounded.Call, null, tint = DeliveryEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Call", color = DeliveryEmerald)
                                    }
                                }
                                if (activeRide.status == "CAPTAIN_ASSIGNED" || activeRide.status == "ARRIVING") {
                                    TakeIt3DButton(
                                        onClick = { onRequestOtp(OtpJobTarget.RideStart(activeRide)) },
                                        containerColor = RideYellow,
                                        depthColor = RideYellow.darken3D(0.4f),
                                        contentColor = RideCharcoal,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 44.dp,
                                        modifier = Modifier.weight(1.4f)
                                    ) { Text("Verify Start OTP", fontWeight = FontWeight.ExtraBold) }
                                } else {
                                    TakeIt3DButton(
                                        onClick = {
                                            if (activeRide.otpVerified) viewModel.adminUpdateRide(activeRide.id, "COMPLETED")
                                            else onRequestOtp(OtpJobTarget.RideComplete(activeRide))
                                        },
                                        containerColor = DeliveryEmerald,
                                        depthColor = DeliveryEmerald.darken3D(0.4f),
                                        contentColor = DeliveryBg,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 44.dp,
                                        modifier = Modifier.weight(1.4f)
                                    ) { Text("Complete Ride", fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                        }
                    }
                }
            } else if (activeParcel != null) {
                item {
                    TakeIt3DCard(
                        containerColor = DeliverySurface,
                        depthColor = Color.Black,
                        borderColor = TransportOrange.copy(alpha = 0.5f),
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Active MoveX • ${activeParcel.parcelNumber}", fontWeight = FontWeight.ExtraBold, color = TransportOrange)
                            Text("${activeParcel.packageType} → ${activeParcel.receiverName}", fontWeight = FontWeight.Bold, color = DeliveryInk)
                            Text("${activeParcel.pickupAddress} → ${activeParcel.dropAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                            TakeIt3DButton(
                                onClick = { onRequestOtp(OtpJobTarget.Parcel(activeParcel)) },
                                containerColor = DeliveryEmerald,
                                depthColor = DeliveryEmerald.darken3D(0.4f),
                                contentColor = DeliveryBg,
                                cornerRadius = 14.dp,
                                depth = 4.dp,
                                height = 44.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enter Drop OTP & Deliver", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryOtpVerifyDialog(
    target: OtpJobTarget,
    onDismiss: () -> Unit,
    onVerifyAndComplete: (String) -> Unit
) {
    var otp by remember { mutableStateOf("") }
    val (title, subtitle, buttonLabel) = when (target) {
        is OtpJobTarget.Food -> Triple(
            "Deliver Order ${target.order.orderNumber}",
            "Ask the customer for the 4-digit delivery OTP shown in their Take It Food app.",
            "Verify & Deliver"
        )
        is OtpJobTarget.RideStart -> Triple(
            "Start Ride ${target.ride.rideNumber}",
            "Ask the passenger for their 4-digit boarding OTP shown on Live Tracking to begin the trip.",
            "Verify OTP & Start Ride"
        )
        is OtpJobTarget.RideComplete -> Triple(
            "Complete Ride ${target.ride.rideNumber}",
            "Verify the rider's 4-digit OTP to finish the trip and record Captain earnings.",
            "Verify & Complete Ride"
        )
        is OtpJobTarget.Parcel -> Triple(
            "Deliver Shipment ${target.parcel.parcelNumber}",
            "Ask the recipient for the 4-digit MoveX Drop OTP.",
            "Verify Drop OTP & Deliver"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeliverySurface,
        titleContentColor = DeliveryInk,
        textContentColor = DeliveryInk,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 4) otp = it.filter { ch -> ch.isDigit() } },
                    label = { Text("4-Digit OTP") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("delivery_otp_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerifyAndComplete(otp) },
                enabled = otp.length == 4,
                colors = ButtonDefaults.buttonColors(containerColor = DeliveryEmerald, contentColor = DeliveryBg),
                modifier = Modifier.testTag("delivery_otp_complete_btn")
            ) {
                Text(buttonLabel, fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = DeliveryMuted) }
        }
    )
}
