package com.example.takeit.ui.delivery

import android.content.Intent
import android.net.Uri
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
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel

private sealed class OtpJobTarget {
    data class Food(val order: FoodOrder) : OtpJobTarget()
    data class Ride(val ride: RideBooking) : OtpJobTarget()
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
                            contentDescription = "Delivery Partner Avatar",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = uiState.activeCoAdmin?.name?.ifBlank { uiState.displayName } ?: uiState.displayName,
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
                                text = if (uiState.deliveryPartnerOnline) "ONLINE • READY FOR JOBS" else "OFFLINE",
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
                        DeliveryTab.EARNINGS -> Icons.Rounded.History
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
                    onRequestOtpComplete = { otpTarget = it },
                    onOpenRouteMap = { viewModel.selectDeliveryTab(DeliveryTab.ACTIVE) }
                )
                DeliveryTab.ACTIVE -> DeliveryActiveRouteTab(
                    uiState = uiState,
                    onRequestOtpComplete = { otpTarget = it }
                )
                DeliveryTab.EARNINGS -> DeliveryHistoryTab(uiState = uiState)
                DeliveryTab.ACCOUNT -> DeliveryAccountTab(
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
                    is OtpJobTarget.Ride -> viewModel.deliveryCompleteRideWithOtp(target.ride, enteredOtp) {
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
    onVerifyPasscode: (String) -> Unit,
    onExitToApp: (() -> Unit)?,
    onSignOut: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

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
                            .size(98.dp)
                            .shadow(16.dp, CircleShape, ambientColor = DeliveryEmerald, spotColor = DeliveryEmerald)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(DeliveryEmerald.lighten3D(0.25f), DeliveryEmerald)))
                            .padding(3.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_char_food_rider_1795426023084),
                            contentDescription = "Take It Delivery Partner",
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
                                "DELIVERY PARTNER SECURITY VAULT",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeliveryEmerald,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Take It Delivery Partner",
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
                        "Enter the Delivery Partner passcode assigned to your Gmail by Admin or Co-Admin.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeliveryMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("Partner Passcode") },
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
                            Text("Unlock Delivery Panel", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    if (onExitToApp != null) {
                        Spacer(Modifier.height(10.dp))
                        TextButton(onClick = onExitToApp) {
                            Text("Back to Customer App", color = DeliveryAmber, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(Modifier.height(6.dp))
                    TextButton(onClick = onSignOut) {
                        Text("Switch Google Account", color = DeliveryMuted, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryTasksTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onRequestOtpComplete: (OtpJobTarget) -> Unit,
    onOpenRouteMap: () -> Unit
) {
    val context = LocalContext.current
    var categoryFilter by remember { mutableStateOf("ALL") }

    val activeOrders = remember(uiState.allOrders) {
        uiState.allOrders.filter { it.status in listOf("PLACED", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY") }
    }
    val activeRides = remember(uiState.allRides) {
        uiState.allRides.filter { it.status in listOf("REQUESTED", "CAPTAIN_ASSIGNED", "ARRIVING", "ON_TRIP") }
    }
    val activeParcels = remember(uiState.allParcels) {
        uiState.allParcels.filter { it.status in listOf("BOOKED", "PARTNER_ASSIGNED", "PICKED_UP", "IN_TRANSIT") }
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
                borderColor = DeliveryEmerald.copy(alpha = 0.4f),
                cornerRadius = 22.dp,
                depth = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Live Dispatch Queue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                        Text(
                            "${activeOrders.size} Food • ${activeRides.size} Rides • ${activeParcels.size} Parcels",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeliveryEmerald
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (uiState.deliveryPartnerOnline) Color(0xFF064E3B) else Color(0xFF3F1219),
                        border = BorderStroke(1.dp, if (uiState.deliveryPartnerOnline) DeliveryEmerald else AdminCrimson)
                    ) {
                        Text(
                            if (uiState.deliveryPartnerOnline) "ONLINE" else "PAUSED",
                            color = if (uiState.deliveryPartnerOnline) DeliveryEmerald else Color(0xFFFCA5A5),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(listOf("ALL", "FOOD", "RIDE", "PARCEL")) { f ->
                    FilterChip(
                        selected = categoryFilter == f,
                        onClick = { categoryFilter = f },
                        label = { Text(f, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeliveryEmerald,
                            selectedLabelColor = DeliveryBg,
                            containerColor = DeliveryCard,
                            labelColor = DeliveryInk
                        )
                    )
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
                        Text("Turn ON the header Online switch to accept new Food, Ride & Parcel jobs.", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            // FOOD ORDERS
            if (categoryFilter == "ALL" || categoryFilter == "FOOD") {
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
                                            onClick = {
                                                context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.customerPhone}")))
                                            },
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

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                                            .weight(1f)
                                            .testTag("delivery_claim_food_${order.id}")
                                    ) {
                                        Icon(Icons.Rounded.TwoWheeler, null, modifier = Modifier.size(17.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Pick Up & Deliver", fontWeight = FontWeight.ExtraBold)
                                    }
                                } else {
                                    TakeIt3DButton(
                                        onClick = { onRequestOtpComplete(OtpJobTarget.Food(order)) },
                                        containerColor = DeliveryEmerald,
                                        depthColor = DeliveryEmerald.darken3D(0.4f),
                                        contentColor = DeliveryBg,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 44.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("delivery_complete_food_${order.id}")
                                    ) {
                                        Icon(Icons.Rounded.Verified, null, modifier = Modifier.size(17.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Verify OTP & Deliver", fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // RIDES
            if (categoryFilter == "ALL" || categoryFilter == "RIDE") {
                items(activeRides, key = { "del_ride_${it.id}" }) { ride ->
                    TakeIt3DCard(
                        containerColor = DeliverySurface,
                        depthColor = Color(0xFF040A0D),
                        borderColor = RideYellow.copy(alpha = 0.4f),
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
                                        Text("RIDE • ${ride.vehicleType}", color = RideYellow, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Text(ride.rideNumber, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                                }
                                Text("₹${ride.totalFare}", fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald)
                            }
                            Text("Passenger: ${ride.customerName} (${ride.distanceKm} km)", color = DeliveryInk, fontWeight = FontWeight.Bold)
                            Text("Pickup: ${ride.pickupAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                            Text("Drop: ${ride.dropAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryInk)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                when (ride.status) {
                                    "REQUESTED" -> TakeIt3DButton(
                                        onClick = { viewModel.deliveryClaimRide(ride) },
                                        containerColor = RideYellow,
                                        depthColor = RideYellow.darken3D(0.4f),
                                        contentColor = RideCharcoal,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 42.dp,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Accept Ride", fontWeight = FontWeight.ExtraBold) }
                                    "CAPTAIN_ASSIGNED" -> TakeIt3DButton(
                                        onClick = { viewModel.adminUpdateRide(ride.id, "ARRIVING") },
                                        containerColor = AdminCyan,
                                        depthColor = AdminCyan.darken3D(0.4f),
                                        contentColor = DeliveryBg,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 42.dp,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Mark Arriving", fontWeight = FontWeight.ExtraBold) }
                                    "ARRIVING" -> TakeIt3DButton(
                                        onClick = { viewModel.adminUpdateRide(ride.id, "ON_TRIP") },
                                        containerColor = DeliveryAmber,
                                        depthColor = DeliveryAmber.darken3D(0.4f),
                                        contentColor = DeliveryBg,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 42.dp,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Start Ride", fontWeight = FontWeight.ExtraBold) }
                                    else -> TakeIt3DButton(
                                        onClick = { onRequestOtpComplete(OtpJobTarget.Ride(ride)) },
                                        containerColor = DeliveryEmerald,
                                        depthColor = DeliveryEmerald.darken3D(0.4f),
                                        contentColor = DeliveryBg,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 42.dp,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Verify OTP & Complete", fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                        }
                    }
                }
            }

            // PARCELS
            if (categoryFilter == "ALL" || categoryFilter == "PARCEL") {
                items(activeParcels, key = { "del_parcel_${it.id}" }) { parcel ->
                    TakeIt3DCard(
                        containerColor = DeliverySurface,
                        depthColor = Color(0xFF040A0D),
                        borderColor = TransportOrange.copy(alpha = 0.45f),
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
                                        Text("PARCEL • ${parcel.vehicleType}", color = TransportOrange, fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                    }
                                    Text(parcel.parcelNumber, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                                }
                                Text("₹${parcel.totalFare}", fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald)
                            }
                            Text("${parcel.packageType} • ${parcel.weightKg}kg${if (parcel.fragileFlag) " • FRAGILE" else ""}", color = DeliveryInk, fontWeight = FontWeight.Bold)
                            Text("Pickup: ${parcel.pickupAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                            Text("Drop (${parcel.receiverName}): ${parcel.dropAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryInk)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                when (parcel.status) {
                                    "BOOKED" -> TakeIt3DButton(
                                        onClick = { viewModel.deliveryClaimParcel(parcel) },
                                        containerColor = TransportOrange,
                                        depthColor = TransportOrange.darken3D(0.4f),
                                        contentColor = Color.White,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 42.dp,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Accept Parcel", fontWeight = FontWeight.ExtraBold) }
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
                                    ) { Text("In Transit", fontWeight = FontWeight.ExtraBold) }
                                    else -> TakeIt3DButton(
                                        onClick = { onRequestOtpComplete(OtpJobTarget.Parcel(parcel)) },
                                        containerColor = DeliveryEmerald,
                                        depthColor = DeliveryEmerald.darken3D(0.4f),
                                        contentColor = DeliveryBg,
                                        cornerRadius = 14.dp,
                                        depth = 4.dp,
                                        height = 42.dp,
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Verify OTP & Deliver", fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                        }
                    }
                }
            }

            if (activeOrders.isEmpty() && activeRides.isEmpty() && activeParcels.isEmpty()) {
                item {
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
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.CheckCircleOutline, null, tint = DeliveryEmerald, modifier = Modifier.size(44.dp))
                            Text("All Clear!", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                            Text("No pending Food, Ride, or Parcel jobs right now.", color = DeliveryMuted, textAlign = TextAlign.Center)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryActiveRouteTab(
    uiState: TakeItUiState,
    onRequestOtpComplete: (OtpJobTarget) -> Unit
) {
    val context = LocalContext.current
    val activeFood = uiState.allOrders.firstOrNull { it.status == "OUT_FOR_DELIVERY" }
        ?: uiState.allOrders.firstOrNull { it.status in listOf("PLACED", "CONFIRMED", "PREPARING") }
    val activeRide = uiState.allRides.firstOrNull { it.status in listOf("CAPTAIN_ASSIGNED", "ARRIVING", "ON_TRIP", "REQUESTED") }
    val activeParcel = uiState.allParcels.firstOrNull { it.status in listOf("PARTNER_ASSIGNED", "PICKED_UP", "IN_TRANSIT", "BOOKED") }

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
                        Text("Pick up an order, ride, or parcel from Tasks to see the live OpenStreetMap + Leaflet route here.", color = DeliveryMuted, textAlign = TextAlign.Center)
                    }
                }
            }
        } else {
            val pickupLat = activeFood?.pickupLat ?: 28.4744
            val pickupLng = activeFood?.pickupLng ?: 77.5040
            val dropLat = activeFood?.deliveryLat ?: 28.4842
            val dropLng = activeFood?.deliveryLng ?: 77.5150
            val pickupTitle = activeFood?.restaurantName ?: activeRide?.pickupAddress ?: activeParcel?.pickupAddress ?: "Pickup Point"
            val dropTitle = activeFood?.deliveryAddress ?: activeRide?.dropAddress ?: activeParcel?.dropAddress ?: "Drop Point"
            val partnerLabel = uiState.activeCoAdmin?.name?.ifBlank { uiState.displayName } ?: uiState.displayName

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

            if (activeFood != null) {
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
                            Text("Items: ${activeFood.itemsSummary}", style = MaterialTheme.typography.bodySmall, color = DeliveryInk)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                if (activeFood.customerPhone.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${activeFood.customerPhone}")))
                                        },
                                        border = BorderStroke(1.dp, DeliveryEmerald),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Call, null, tint = DeliveryEmerald, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Call", color = DeliveryEmerald)
                                    }
                                }
                                TakeIt3DButton(
                                    onClick = { onRequestOtpComplete(OtpJobTarget.Food(activeFood)) },
                                    containerColor = DeliveryEmerald,
                                    depthColor = DeliveryEmerald.darken3D(0.4f),
                                    contentColor = DeliveryBg,
                                    cornerRadius = 14.dp,
                                    depth = 4.dp,
                                    height = 44.dp,
                                    modifier = Modifier
                                        .weight(1.4f)
                                        .testTag("delivery_route_otp_btn")
                                ) {
                                    Text("Enter OTP & Deliver", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            } else if (activeRide != null) {
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
                            Text("Active Ride • ${activeRide.rideNumber}", fontWeight = FontWeight.ExtraBold, color = RideYellow)
                            Text("${activeRide.customerName} • ₹${activeRide.totalFare}", fontWeight = FontWeight.Bold, color = DeliveryInk)
                            Text("${activeRide.pickupAddress} → ${activeRide.dropAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                            TakeIt3DButton(
                                onClick = { onRequestOtpComplete(OtpJobTarget.Ride(activeRide)) },
                                containerColor = DeliveryEmerald,
                                depthColor = DeliveryEmerald.darken3D(0.4f),
                                contentColor = DeliveryBg,
                                cornerRadius = 14.dp,
                                depth = 4.dp,
                                height = 44.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Enter Ride OTP & Complete", fontWeight = FontWeight.ExtraBold)
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
                            Text("Active Parcel • ${activeParcel.parcelNumber}", fontWeight = FontWeight.ExtraBold, color = TransportOrange)
                            Text("${activeParcel.packageType} → ${activeParcel.receiverName}", fontWeight = FontWeight.Bold, color = DeliveryInk)
                            Text("${activeParcel.pickupAddress} → ${activeParcel.dropAddress}", style = MaterialTheme.typography.bodySmall, color = DeliveryMuted)
                            TakeIt3DButton(
                                onClick = { onRequestOtpComplete(OtpJobTarget.Parcel(activeParcel)) },
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
private fun DeliveryHistoryTab(uiState: TakeItUiState) {
    val completedFoods = remember(uiState.allOrders) { uiState.allOrders.filter { it.status == "DELIVERED" } }
    val completedRides = remember(uiState.allRides) { uiState.allRides.filter { it.status == "COMPLETED" } }
    val completedParcels = remember(uiState.allParcels) { uiState.allParcels.filter { it.status == "DELIVERED" } }
    val totalJobs = completedFoods.size + completedRides.size + completedParcels.size
    val totalValue = completedFoods.sumOf { it.totalAmount } + completedRides.sumOf { it.totalFare } + completedParcels.sumOf { it.totalFare }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TakeIt3DCard(
                    containerColor = DeliverySurface,
                    depthColor = Color.Black,
                    borderColor = DeliveryEmerald,
                    cornerRadius = 20.dp,
                    depth = 5.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Completed Jobs", style = MaterialTheme.typography.labelSmall, color = DeliveryMuted)
                        Text("$totalJobs", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald)
                    }
                }
                TakeIt3DCard(
                    containerColor = DeliverySurface,
                    depthColor = Color.Black,
                    borderColor = DeliveryAmber,
                    cornerRadius = 20.dp,
                    depth = 5.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("Order Value", style = MaterialTheme.typography.labelSmall, color = DeliveryMuted)
                        Text("₹$totalValue", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = DeliveryAmber)
                    }
                }
            }
        }

        items(completedFoods, key = { "hist_food_${it.id}" }) { o ->
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color.Black,
                borderColor = DeliveryBorder,
                cornerRadius = 18.dp,
                depth = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("FOOD • ${o.orderNumber}", fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald)
                        Text("${o.restaurantName} → ${o.customerName}", color = DeliveryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("₹${o.totalAmount}", fontWeight = FontWeight.ExtraBold, color = DeliveryAmber)
                }
            }
        }
        items(completedRides, key = { "hist_ride_${it.id}" }) { r ->
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color.Black,
                borderColor = DeliveryBorder,
                cornerRadius = 18.dp,
                depth = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("RIDE • ${r.rideNumber}", fontWeight = FontWeight.ExtraBold, color = RideYellow)
                        Text("${r.customerName} (${r.distanceKm} km)", color = DeliveryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("₹${r.totalFare}", fontWeight = FontWeight.ExtraBold, color = DeliveryAmber)
                }
            }
        }
        items(completedParcels, key = { "hist_parcel_${it.id}" }) { p ->
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color.Black,
                borderColor = DeliveryBorder,
                cornerRadius = 18.dp,
                depth = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PARCEL • ${p.parcelNumber}", fontWeight = FontWeight.ExtraBold, color = TransportOrange)
                        Text("${p.packageType} → ${p.receiverName}", color = DeliveryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text("₹${p.totalFare}", fontWeight = FontWeight.ExtraBold, color = DeliveryAmber)
                }
            }
        }
    }
}

@Composable
private fun DeliveryAccountTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color.Black,
                borderColor = DeliveryEmerald,
                cornerRadius = 24.dp,
                depth = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        uiState.activeCoAdmin?.name?.ifBlank { uiState.displayName } ?: uiState.displayName,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = DeliveryInk
                    )
                    Text("Gmail: ${uiState.email}", color = DeliveryEmerald, style = MaterialTheme.typography.bodySmall)
                    val phone = uiState.activeCoAdmin?.phone?.ifBlank { uiState.profile.phone } ?: uiState.profile.phone
                    if (phone.isNotBlank()) {
                        Text("Phone: $phone", color = DeliveryMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    val vehicle = uiState.activeCoAdmin?.vehicleNumber.orEmpty()
                    if (vehicle.isNotBlank()) {
                        Text("Vehicle: $vehicle", color = DeliveryAmber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.appVariant != TakeItAppVariant.DELIVERY) {
            item {
                TakeIt3DButton(
                    onClick = { viewModel.exitDeliveryMode() },
                    containerColor = DeliveryAmber,
                    depthColor = DeliveryAmber.darken3D(0.4f),
                    contentColor = DeliveryBg,
                    cornerRadius = 16.dp,
                    depth = 4.dp,
                    height = 50.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Switch to Customer App", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        item {
            TakeIt3DButton(
                onClick = onSignOut,
                containerColor = AdminCrimson,
                depthColor = AdminCrimson.darken3D(0.45f),
                contentColor = Color.White,
                cornerRadius = 16.dp,
                depth = 4.dp,
                height = 50.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delivery_account_signout_btn")
            ) {
                Text("Switch Google Account / Sign Out", fontWeight = FontWeight.ExtraBold)
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
    val title = when (target) {
        is OtpJobTarget.Food -> "Deliver Order ${target.order.orderNumber}"
        is OtpJobTarget.Ride -> "Complete Ride ${target.ride.rideNumber}"
        is OtpJobTarget.Parcel -> "Deliver Parcel ${target.parcel.parcelNumber}"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeliverySurface,
        titleContentColor = DeliveryInk,
        textContentColor = DeliveryInk,
        title = { Text(title, fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Ask the customer / recipient for the 4-digit delivery OTP shown in their Take It app.",
                    style = MaterialTheme.typography.bodySmall,
                    color = DeliveryMuted
                )
                OutlinedTextField(
                    value = otp,
                    onValueChange = { if (it.length <= 4) otp = it.filter { ch -> ch.isDigit() } },
                    label = { Text("4-Digit Customer OTP") },
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
                Text("Verify & Complete", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = DeliveryMuted) }
        }
    )
}
