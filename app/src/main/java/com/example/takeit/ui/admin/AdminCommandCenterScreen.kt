package com.example.takeit.ui.admin

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
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
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel

@Composable
fun AdminCommandCenterRoot(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    if (!uiState.hasUnlockedAdminAccess) {
        AdminAccessGateScreen(
            uiState = uiState,
            onVerifyPasscode = viewModel::verifyCoAdminPasscode,
            onReturnToCustomer = if (uiState.appVariant == TakeItAppVariant.CUSTOMER) {
                { viewModel.exitAdminMode() }
            } else null,
            onSignOut = onSignOut
        )
        return
    }

    val allowedTabs = remember(
        uiState.isMainAdmin,
        uiState.canManageOrders,
        uiState.canManageRestaurants,
        uiState.canManageCoupons,
        uiState.canManageUsers,
        uiState.canManageSettings
    ) {
        buildList {
            add(AdminTab.OVERVIEW)
            if (uiState.canManageOrders) add(AdminTab.ORDERS)
            if (uiState.canManageRestaurants) add(AdminTab.CATALOG)
            if (uiState.canManageCoupons) add(AdminTab.OFFERS)
            if (uiState.canManageUsers) add(AdminTab.PEOPLE)
            if (uiState.isMainAdmin) add(AdminTab.CO_ADMINS)
            if (uiState.isMainAdmin || uiState.canManageOrders) add(AdminTab.PAYMENTS)
            if (uiState.canManageSettings) add(AdminTab.SETTINGS)
        }
    }

    val activeTab = if (allowedTabs.contains(uiState.adminTab)) uiState.adminTab else AdminTab.OVERVIEW

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TakeIt3DCard(
                containerColor = AdminSurface,
                depthColor = Color(0xFF050814),
                borderColor = AdminGold.copy(alpha = 0.4f),
                cornerRadius = 26.dp,
                depth = 5.dp,
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), AdminSurface)))
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(8.dp, CircleShape, ambientColor = AdminGold, spotColor = AdminGold)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(AdminGold.lighten3D(0.3f), AdminGold)))
                                .padding(2.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.logo_takeit_food),
                                contentDescription = "Take It Admin Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Take It Command",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = AdminInk,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(AdminEmerald)
                                )
                            }
                            Spacer(Modifier.height(2.dp))
                            val badgeLabel = if (uiState.isMainAdmin) {
                                "MAIN ADMIN • SUPER OWNER"
                            } else {
                                "CO-ADMIN • ${uiState.activeCoAdmin?.roleTitle?.uppercase() ?: "STAFF"}"
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (uiState.isMainAdmin) Color(0xFF3B2806) else Color(0xFF0C2D48),
                                border = BorderStroke(1.dp, if (uiState.isMainAdmin) AdminGold else AdminCyan)
                            ) {
                                Text(
                                    text = badgeLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.isMainAdmin) AdminGold else AdminCyan,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (uiState.appVariant == TakeItAppVariant.CUSTOMER) {
                            FilledTonalButton(
                                onClick = { viewModel.exitAdminMode() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(
                                    containerColor = AdminCard,
                                    contentColor = AdminInk
                                ),
                                modifier = Modifier
                                    .height(38.dp)
                                    .testTag("admin_switch_to_customer_btn")
                            ) {
                                Icon(Icons.Rounded.Storefront, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("App", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(6.dp))
                        }

                        TakeIt3DIconOrb(
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            containerColor = AdminCard,
                            depthColor = Color.Black,
                            iconTint = AdminCrimson,
                            size = 40.dp,
                            contentDescription = "Sign Out",
                            onClick = onSignOut,
                            modifier = Modifier.testTag("admin_sign_out_btn")
                        )
                    }

                    // 3D Beveled Horizontal Tab Strip
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(allowedTabs) { tab ->
                            val selected = tab == activeTab
                            val icon = when (tab) {
                                AdminTab.OVERVIEW -> Icons.Rounded.Dashboard
                                AdminTab.ORDERS -> Icons.Rounded.ReceiptLong
                                AdminTab.CATALOG -> Icons.Rounded.RestaurantMenu
                                AdminTab.OFFERS -> Icons.Rounded.LocalOffer
                                AdminTab.PEOPLE -> Icons.Rounded.Groups
                                AdminTab.CO_ADMINS -> Icons.Rounded.AdminPanelSettings
                                AdminTab.PAYMENTS -> Icons.Rounded.AccountBalanceWallet
                                AdminTab.SETTINGS -> Icons.Rounded.Tune
                            }
                            TakeIt3DCard(
                                onClick = { viewModel.selectAdminTab(tab) },
                                containerColor = if (selected) AdminGold else AdminCard,
                                depthColor = if (selected) AdminGold.darken3D(0.42f) else Color(0xFF070C1A),
                                borderColor = if (selected) AdminGold.lighten3D(0.4f) else AdminBorder,
                                cornerRadius = 14.dp,
                                depth = if (selected) 4.dp else 2.dp,
                                shadowElevation = if (selected) 8.dp else 2.dp,
                                modifier = Modifier.testTag("admin_tab_${tab.name.lowercase()}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = tab.label,
                                        tint = if (selected) AdminBg else AdminMuted,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) AdminBg else AdminInk
                                    )
                                    if (tab == AdminTab.ORDERS) {
                                        val liveCount =
                                            uiState.allOrders.count { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") } +
                                            uiState.allRides.count { it.status !in listOf("COMPLETED", "CANCELLED", "REFUNDED") } +
                                            uiState.allParcels.count { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
                                        if (liveCount > 0) {
                                            Surface(
                                                shape = CircleShape,
                                                color = if (selected) AdminBg else AdminCrimson
                                            ) {
                                                Text(
                                                    text = "$liveCount",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) AdminGold else Color.White,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                                                )
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
    ) { innerPadding ->
        TakeIt3DBackdrop(
            accentPrimary = AdminGold,
            accentSecondary = AdminCyan,
            darkMode = true,
            baseColor = AdminBg,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (activeTab) {
                AdminTab.OVERVIEW -> AdminOverviewTab(uiState = uiState, viewModel = viewModel)
                AdminTab.ORDERS -> AdminOrdersTab(uiState = uiState, viewModel = viewModel)
                AdminTab.CATALOG -> AdminCatalogTab(uiState = uiState, viewModel = viewModel)
                AdminTab.OFFERS -> AdminCouponsTab(uiState = uiState, viewModel = viewModel)
                AdminTab.PEOPLE -> AdminPeopleTab(uiState = uiState, viewModel = viewModel)
                AdminTab.CO_ADMINS -> AdminCoAdminsTab(uiState = uiState, viewModel = viewModel)
                AdminTab.PAYMENTS -> AdminPaymentsTab(uiState = uiState, viewModel = viewModel)
                AdminTab.SETTINGS -> AdminSettingsTab(uiState = uiState, viewModel = viewModel)
            }
        }
    }
}

@Composable
private fun AdminAccessGateScreen(
    uiState: TakeItUiState,
    onVerifyPasscode: (String) -> Unit,
    onReturnToCustomer: (() -> Unit)?,
    onSignOut: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    TakeIt3DBackdrop(
        accentPrimary = AdminGold,
        accentSecondary = AdminCyan,
        darkMode = true,
        baseColor = AdminBg
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
                containerColor = AdminSurface,
                depthColor = Color(0xFF040711),
                borderColor = AdminGold.copy(alpha = 0.5f),
                cornerRadius = 28.dp,
                depth = 7.dp,
                shadowElevation = 16.dp,
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
                            .size(96.dp)
                            .shadow(14.dp, CircleShape, ambientColor = AdminGold, spotColor = AdminGold)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(AdminGold.lighten3D(0.3f), AdminGold)))
                            .padding(3.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_takeit_food),
                            contentDescription = "Take It Admin Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF3B2806),
                        border = BorderStroke(1.dp, AdminGold)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, null, tint = AdminGold, modifier = Modifier.size(15.dp))
                            Text(
                                "CO-ADMIN SECURITY VAULT",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminGold,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Take It Command Center",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdminInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Signed in as ${uiState.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminCyan,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Enter the Co-Admin password created for your Gmail by Main Admin ($MAIN_ADMIN_EMAIL).",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminMuted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(18.dp))

                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("Co-Admin Password") },
                        placeholder = { Text("Enter assigned passcode") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(
                                    if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = "Toggle password visibility",
                                    tint = AdminMuted
                                )
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AdminGold,
                            unfocusedBorderColor = AdminBorder,
                            focusedTextColor = AdminInk,
                            unfocusedTextColor = AdminInk,
                            focusedContainerColor = AdminCard,
                            unfocusedContainerColor = AdminCard
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coadmin_passcode_input")
                    )

                    if (!uiState.coAdminAuthError.isNullOrBlank()) {
                        Spacer(Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF3F1219),
                            border = BorderStroke(1.dp, AdminCrimson),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = AdminCrimson, modifier = Modifier.size(18.dp))
                                Text(
                                    uiState.coAdminAuthError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFFCA5A5)
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))

                    TakeIt3DButton(
                        onClick = { onVerifyPasscode(passcode) },
                        enabled = passcode.isNotBlank() && !uiState.isCheckingCoAdmin,
                        containerColor = AdminGold,
                        depthColor = AdminGold.darken3D(0.45f),
                        contentColor = AdminBg,
                        cornerRadius = 16.dp,
                        depth = 5.dp,
                        height = 50.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("coadmin_unlock_btn")
                    ) {
                        if (uiState.isCheckingCoAdmin) {
                            CircularProgressIndicator(color = AdminBg, strokeWidth = 2.dp, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(10.dp))
                            Text("Verifying...", fontWeight = FontWeight.ExtraBold)
                        } else {
                            Icon(Icons.Rounded.LockOpen, null, modifier = Modifier.size(19.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Unlock Admin Panel", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (onReturnToCustomer != null) {
                            OutlinedButton(
                                onClick = onReturnToCustomer,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, AdminBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(46.dp)
                            ) {
                                Text("Customer App", color = AdminInk)
                            }
                        }
                        OutlinedButton(
                            onClick = onSignOut,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f)),
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .testTag("coadmin_gate_signout_btn")
                        ) {
                            Text("Switch Account", color = AdminCrimson)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOverviewTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    val grossRevenue = remember(uiState.allOrders, uiState.allRides, uiState.allParcels) {
        uiState.allOrders.filter { it.status !in listOf("CANCELLED", "REFUNDED") }.sumOf { it.totalAmount } +
        uiState.allRides.filter { it.status !in listOf("CANCELLED", "REFUNDED") }.sumOf { it.totalFare } +
        uiState.allParcels.filter { it.status !in listOf("CANCELLED", "REFUNDED") }.sumOf { it.totalFare }
    }
    val activeOrders = remember(uiState.allOrders, uiState.allRides, uiState.allParcels) {
        uiState.allOrders.count { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") } +
        uiState.allRides.count { it.status !in listOf("COMPLETED", "CANCELLED", "REFUNDED") } +
        uiState.allParcels.count { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
    }
    val openRestaurants = remember(uiState.restaurants) { uiState.restaurants.count { it.openFlag } }
    val openTickets = remember(uiState.allTickets) { uiState.allTickets.count { it.status != "RESOLVED" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 3D Live System Banner
        item {
            TakeIt3DCard(
                containerColor = AdminCard,
                depthColor = Color(0xFF070C1A),
                borderColor = AdminGold.copy(alpha = 0.55f),
                cornerRadius = 22.dp,
                depth = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TakeIt3DIconOrb(
                            icon = Icons.Rounded.Bolt,
                            containerColor = AdminGold,
                            iconTint = AdminBg,
                            size = 34.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Live Cloud Command Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AdminInk
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFF064E3B), border = BorderStroke(1.dp, AdminEmerald)) {
                            Text(
                                "LIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminEmerald,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Text(
                        "Manage Food Delivery, Take It Ride, and Parcel Transport live across all customer apps.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminMuted
                    )

                    if (uiState.canManageSettings) {
                        HorizontalDivider(color = AdminBorder)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Accepting Customer Orders", color = AdminInk, fontWeight = FontWeight.Bold)
                                Text(
                                    if (uiState.config.acceptingOrders) "Storefront is live & taking orders" else "Orders paused globally",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.config.acceptingOrders) AdminEmerald else AdminCrimson
                                )
                            }
                            Switch(
                                checked = uiState.config.acceptingOrders,
                                onCheckedChange = { enabled ->
                                    viewModel.saveAppConfig(uiState.config.copy(acceptingOrders = enabled))
                                },
                                modifier = Modifier.testTag("admin_quick_accept_orders_switch")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Emergency Maintenance Mode", color = AdminInk, fontWeight = FontWeight.Bold)
                                Text(
                                    if (uiState.config.maintenanceMode) "Customers see Maintenance Screen" else "Normal Customer access enabled",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (uiState.config.maintenanceMode) AdminCrimson else AdminMuted
                                )
                            }
                            Switch(
                                checked = uiState.config.maintenanceMode,
                                onCheckedChange = { maint ->
                                    viewModel.saveAppConfig(uiState.config.copy(maintenanceMode = maint))
                                },
                                modifier = Modifier.testTag("admin_quick_maintenance_switch")
                            )
                        }
                    }
                }
            }
        }

        // 3D KPI Grid Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminKpiCard(
                    title = "Suite Revenue",
                    value = "₹$grossRevenue",
                    subtitle = "${uiState.allOrders.size} food • ${uiState.allRides.size} rides • ${uiState.allParcels.size} parcels",
                    icon = Icons.Rounded.CurrencyRupee,
                    accent = AdminGold,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Live Requests",
                    value = "$activeOrders",
                    subtitle = "Food + Ride + Parcel",
                    icon = Icons.Rounded.DeliveryDining,
                    accent = if (activeOrders > 0) AdminCrimson else AdminEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3D KPI Grid Row 2
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminKpiCard(
                    title = "Restaurants",
                    value = "$openRestaurants/${uiState.restaurants.size}",
                    subtitle = "${uiState.menuItems.size} dishes live",
                    icon = Icons.Rounded.Storefront,
                    accent = AdminEmerald,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Customers",
                    value = "${uiState.allUsers.size}",
                    subtitle = "$openTickets open tickets",
                    icon = Icons.Rounded.Groups,
                    accent = AdminCyan,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 3D Quick Actions Card
        item {
            TakeIt3DCard(
                containerColor = AdminSurface,
                depthColor = Color(0xFF060A15),
                borderColor = AdminBorder,
                cornerRadius = 22.dp,
                depth = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Quick Launch Actions",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = AdminInk
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        if (uiState.canManageRestaurants) {
                            TakeIt3DButton(
                                onClick = { viewModel.seedStarterCatalog() },
                                containerColor = AdminGold,
                                depthColor = AdminGold.darken3D(0.45f),
                                contentColor = AdminBg,
                                cornerRadius = 14.dp,
                                depth = 4.dp,
                                height = 46.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_seed_catalog_btn")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Seed Starter Menu", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (uiState.isMainAdmin) {
                            TakeIt3DButton(
                                onClick = { viewModel.selectAdminTab(AdminTab.CO_ADMINS) },
                                containerColor = AdminCard,
                                depthColor = Color.Black,
                                contentColor = AdminCyan,
                                cornerRadius = 14.dp,
                                depth = 4.dp,
                                height = 46.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("admin_quick_coadmins_btn")
                            ) {
                                Icon(Icons.Rounded.PersonAdd, null, tint = AdminCyan, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Co-Admins (${uiState.coAdmins.size})", color = AdminCyan, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Latest Live Orders Feed
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Latest Incoming Orders",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = AdminInk
                )
                if (uiState.canManageOrders) {
                    TextButton(onClick = { viewModel.selectAdminTab(AdminTab.ORDERS) }) {
                        Text("Manage All ->", color = AdminGold, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (uiState.allOrders.isEmpty()) {
            item {
                TakeIt3DCard(
                    containerColor = AdminSurface,
                    depthColor = Color(0xFF060A15),
                    borderColor = AdminBorder,
                    cornerRadius = 20.dp,
                    depth = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TakeIt3DIconOrb(Icons.Rounded.ReceiptLong, AdminCard, iconTint = AdminMuted, size = 50.dp)
                        Text("No customer orders yet", color = AdminInk, fontWeight = FontWeight.Bold)
                        Text(
                            "Orders placed from Take It Customer app appear here live.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AdminMuted,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(uiState.allOrders.take(5), key = { it.id }) { order ->
                TakeIt3DCard(
                    onClick = { if (uiState.canManageOrders) viewModel.selectAdminTab(AdminTab.ORDERS) },
                    containerColor = AdminSurface,
                    depthColor = Color(0xFF060A15),
                    borderColor = AdminBorder,
                    cornerRadius = 18.dp,
                    depth = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(order.orderNumber, color = AdminGold, fontWeight = FontWeight.ExtraBold)
                                AdminOrderStatusPill(order.status)
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${order.customerName} • ${order.restaurantName}",
                                color = AdminInk,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                order.itemsSummary,
                                style = MaterialTheme.typography.bodySmall,
                                color = AdminMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.End) {
                            Text("₹${order.totalAmount}", color = AdminEmerald, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                            Text(order.paymentMethod, style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminKpiCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    modifier: Modifier = Modifier
) {
    TakeIt3DCard(
        containerColor = AdminSurface,
        depthColor = accent.darken3D(0.65f),
        borderColor = accent.copy(alpha = 0.45f),
        cornerRadius = 20.dp,
        depth = 5.dp,
        shadowElevation = 10.dp,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = AdminMuted, fontWeight = FontWeight.SemiBold)
                TakeIt3DIconOrb(
                    icon = icon,
                    containerColor = accent.copy(alpha = 0.28f),
                    depthColor = Color.Black,
                    iconTint = accent,
                    size = 34.dp,
                    shape = RoundedCornerShape(10.dp)
                )
            }
            Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AdminInk)
            Text(subtitle, style = MaterialTheme.typography.labelSmall, color = accent)
        }
    }
}

@Composable
fun AdminOrderStatusPill(status: String) {
    val (bg, fg) = when (status) {
        "PLACED" -> Color(0xFF451A03) to AdminGold
        "CONFIRMED", "PREPARING" -> Color(0xFF0C2D48) to AdminCyan
        "OUT_FOR_DELIVERY" -> Color(0xFF3B0764) to Color(0xFFE879F9)
        "DELIVERED" -> Color(0xFF064E3B) to AdminEmerald
        else -> Color(0xFF450A0A) to AdminCrimson
    }
    Surface(shape = RoundedCornerShape(50), color = bg, border = BorderStroke(0.5.dp, fg.copy(alpha = 0.5f))) {
        Text(
            text = status.replace("_", " "),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun AdminOrdersTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    var verticalMode by remember { mutableStateOf("FOOD") }
    var filter by remember { mutableStateOf("ALL") }
    var dispatchTarget by remember { mutableStateOf<FoodOrder?>(null) }
    var dispatchRideTarget by remember { mutableStateOf<RideBooking?>(null) }
    var dispatchParcelTarget by remember { mutableStateOf<ParcelOrder?>(null) }

    val filteredFood = remember(uiState.allOrders, filter) {
        when (filter) {
            "ACTIVE" -> uiState.allOrders.filter { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
            "DELIVERED" -> uiState.allOrders.filter { it.status == "DELIVERED" }
            "CANCELLED" -> uiState.allOrders.filter { it.status in listOf("CANCELLED", "REFUNDED") }
            else -> uiState.allOrders
        }
    }

    val filteredRides = remember(uiState.allRides, filter) {
        when (filter) {
            "ACTIVE" -> uiState.allRides.filter { it.status !in listOf("COMPLETED", "CANCELLED", "REFUNDED") }
            "DELIVERED" -> uiState.allRides.filter { it.status == "COMPLETED" }
            "CANCELLED" -> uiState.allRides.filter { it.status in listOf("CANCELLED", "REFUNDED") }
            else -> uiState.allRides
        }
    }

    val filteredParcels = remember(uiState.allParcels, filter) {
        when (filter) {
            "ACTIVE" -> uiState.allParcels.filter { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
            "DELIVERED" -> uiState.allParcels.filter { it.status == "DELIVERED" }
            "CANCELLED" -> uiState.allParcels.filter { it.status in listOf("CANCELLED", "REFUNDED") }
            else -> uiState.allParcels
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    Triple("FOOD", "Food (${uiState.allOrders.size})", Icons.Rounded.Restaurant),
                    Triple("RIDE", "Rapido Rides (${uiState.allRides.size})", Icons.Rounded.TwoWheeler),
                    Triple("PARCEL", "Parcels (${uiState.allParcels.size})", Icons.Rounded.LocalShipping)
                )
            ) { (mode, label, icon) ->
                val sel = verticalMode == mode
                FilterChip(
                    selected = sel,
                    onClick = { verticalMode = mode },
                    leadingIcon = { Icon(icon, null, modifier = Modifier.size(16.dp)) },
                    label = { Text(label, fontWeight = FontWeight.ExtraBold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AdminCyan,
                        selectedLabelColor = AdminBg,
                        selectedLeadingIconColor = AdminBg,
                        containerColor = AdminSurface,
                        labelColor = AdminInk
                    ),
                    modifier = Modifier.testTag("admin_vertical_${mode.lowercase()}")
                )
            }
        }

        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                listOf(
                    "ALL" to "All",
                    "ACTIVE" to "Live Active",
                    "DELIVERED" to if (verticalMode == "RIDE") "Completed" else "Delivered",
                    "CANCELLED" to "Cancelled / Refunded"
                )
            ) { (key, label) ->
                FilterChip(
                    selected = filter == key,
                    onClick = { filter = key },
                    label = { Text(label, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = AdminGold,
                        selectedLabelColor = AdminBg,
                        containerColor = AdminSurface,
                        labelColor = AdminInk
                    )
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (verticalMode) {
                "FOOD" -> {
                    if (filteredFood.isEmpty()) {
                        item { AdminEmptyOrdersCard("No food orders in this filter") }
                    } else {
                        items(filteredFood, key = { it.id }) { order ->
                            AdminOrderCard(
                                order = order,
                                onConfirm = { viewModel.adminUpdateOrderStatus(order.id, "CONFIRMED") },
                                onPreparing = { viewModel.adminUpdateOrderStatus(order.id, "PREPARING") },
                                onOpenDispatch = { dispatchTarget = order },
                                onMarkDelivered = { viewModel.adminUpdateOrderStatus(order.id, "DELIVERED") },
                                onCancel = { viewModel.adminUpdateOrderStatus(order.id, "CANCELLED") },
                                onRefund = { viewModel.adminRefundOrder(order) }
                            )
                        }
                    }
                }
                "RIDE" -> {
                    if (filteredRides.isEmpty()) {
                        item { AdminEmptyOrdersCard("No Rapido rides in this filter") }
                    } else {
                        items(filteredRides, key = { it.id }) { ride ->
                            AdminRideCard(
                                ride = ride,
                                onAssignCaptain = { dispatchRideTarget = ride },
                                onArriving = { viewModel.adminUpdateRide(ride.id, "ARRIVING") },
                                onStartTrip = { viewModel.adminUpdateRide(ride.id, "ON_TRIP") },
                                onComplete = { viewModel.adminUpdateRide(ride.id, "COMPLETED") },
                                onCancel = { viewModel.adminUpdateRide(ride.id, "CANCELLED") }
                            )
                        }
                    }
                }
                else -> {
                    if (filteredParcels.isEmpty()) {
                        item { AdminEmptyOrdersCard("No parcel shipments in this filter") }
                    } else {
                        items(filteredParcels, key = { it.id }) { parcel ->
                            AdminParcelCard(
                                parcel = parcel,
                                onAssignPartner = { dispatchParcelTarget = parcel },
                                onPickedUp = { viewModel.adminUpdateParcel(parcel.id, "PICKED_UP") },
                                onInTransit = { viewModel.adminUpdateParcel(parcel.id, "IN_TRANSIT") },
                                onDelivered = { viewModel.adminUpdateParcel(parcel.id, "DELIVERED") },
                                onCancel = { viewModel.adminUpdateParcel(parcel.id, "CANCELLED") }
                            )
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    dispatchTarget?.let { target ->
        DispatchRiderDialog(
            order = target,
            onDismiss = { dispatchTarget = null },
            onDispatch = { rName, rPhone, rVehicle, eta ->
                viewModel.adminUpdateOrderStatus(
                    orderId = target.id,
                    status = "OUT_FOR_DELIVERY",
                    riderName = rName,
                    riderPhone = rPhone,
                    riderVehicle = rVehicle,
                    etaMinutes = eta
                )
                dispatchTarget = null
            }
        )
    }

    dispatchRideTarget?.let { target ->
        DispatchRideCaptainDialog(
            ride = target,
            onDismiss = { dispatchRideTarget = null },
            onDispatch = { cName, cPhone, vehNo, eta ->
                viewModel.adminUpdateRide(
                    rideId = target.id,
                    status = "CAPTAIN_ASSIGNED",
                    captainName = cName,
                    captainPhone = cPhone,
                    vehicleNumber = vehNo,
                    etaMinutes = eta
                )
                dispatchRideTarget = null
            }
        )
    }

    dispatchParcelTarget?.let { target ->
        DispatchParcelPartnerDialog(
            parcel = target,
            onDismiss = { dispatchParcelTarget = null },
            onDispatch = { pName, pPhone, vehNo, eta ->
                viewModel.adminUpdateParcel(
                    parcelId = target.id,
                    status = "PARTNER_ASSIGNED",
                    partnerName = pName,
                    partnerPhone = pPhone,
                    vehicleNumber = vehNo,
                    etaMinutes = eta
                )
                dispatchParcelTarget = null
            }
        )
    }
}

@Composable
private fun AdminEmptyOrdersCard(message: String) {
    TakeIt3DCard(
        containerColor = AdminSurface,
        depthColor = Color(0xFF060A15),
        borderColor = AdminBorder,
        cornerRadius = 20.dp,
        depth = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TakeIt3DIconOrb(Icons.Rounded.Inventory2, AdminCard, iconTint = AdminMuted, size = 50.dp)
            Text(message, color = AdminInk, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun AdminOrderCard(
    order: FoodOrder,
    onConfirm: () -> Unit,
    onPreparing: () -> Unit,
    onOpenDispatch: () -> Unit,
    onMarkDelivered: () -> Unit,
    onCancel: () -> Unit,
    onRefund: () -> Unit
) {
    TakeIt3DCard(
        containerColor = AdminSurface,
        depthColor = Color(0xFF060A15),
        borderColor = AdminBorder,
        cornerRadius = 22.dp,
        depth = 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(order.orderNumber, color = AdminGold, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    AdminOrderStatusPill(order.status)
                }
                Text("₹${order.totalAmount}", color = AdminEmerald, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }

            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(order.restaurantName, color = AdminInk, fontWeight = FontWeight.Bold)
                Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall, color = AdminMuted)
            }

            Surface(shape = RoundedCornerShape(12.dp), color = AdminCard, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "Customer: ${order.customerName} (${order.customerPhone.ifBlank { order.customerEmail }})",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminInk,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "Deliver to: ${order.deliveryAddress}",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdminMuted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pay: ${order.paymentMethod} (${order.paymentStatus})", style = MaterialTheme.typography.labelSmall, color = AdminCyan)
                        Text("OTP: ${order.otp}", style = MaterialTheme.typography.labelSmall, color = AdminGold, fontWeight = FontWeight.ExtraBold)
                    }
                    if (order.riderName.isNotBlank() && order.riderName != "Assigning Rider...") {
                        Text(
                            "Rider: ${order.riderName} • ${order.riderVehicle} • ${order.riderPhone} (${order.etaMinutes} min)",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminEmerald
                        )
                    }
                }
            }

            if (order.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (order.status) {
                        "PLACED" -> TakeIt3DButton(
                            onClick = onConfirm,
                            containerColor = AdminCyan,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Confirm Order", fontWeight = FontWeight.ExtraBold) }
                        "CONFIRMED" -> TakeIt3DButton(
                            onClick = onPreparing,
                            containerColor = AdminGold,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Start Preparing", fontWeight = FontWeight.ExtraBold) }
                        "PREPARING" -> TakeIt3DButton(
                            onClick = onOpenDispatch,
                            containerColor = Color(0xFFA855F7),
                            contentColor = Color.White,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.TwoWheeler, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Assign Rider & Dispatch", fontWeight = FontWeight.ExtraBold)
                        }
                        "OUT_FOR_DELIVERY" -> TakeIt3DButton(
                            onClick = onMarkDelivered,
                            containerColor = AdminEmerald,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Mark Delivered", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    OutlinedButton(
                        onClick = onCancel,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AdminCrimson),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Text("Cancel", color = AdminCrimson, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (order.status != "REFUNDED") {
                TextButton(
                    onClick = onRefund,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Icon(Icons.Rounded.Replay, null, tint = AdminGold, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Refund ₹${order.totalAmount} to Customer Wallet", color = AdminGold, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun DispatchRiderDialog(
    order: FoodOrder,
    onDismiss: () -> Unit,
    onDispatch: (riderName: String, riderPhone: String, riderVehicle: String, etaMinutes: Int) -> Unit
) {
    var riderName by remember { mutableStateOf(if (order.riderName == "Assigning Rider...") "Rahul Kumar" else order.riderName) }
    var riderPhone by remember { mutableStateOf(order.riderPhone.ifBlank { "+91 98110 22334" }) }
    var riderVehicle by remember { mutableStateOf(order.riderVehicle.ifBlank { "UP16 AB 4821" }) }
    var etaText by remember { mutableStateOf(order.etaMinutes.coerceAtLeast(10).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text("Dispatch ${order.orderNumber}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Assign a delivery rider. Customer live tracking map will update instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdminMuted
                )
                OutlinedTextField(value = riderName, onValueChange = { riderName = it }, label = { Text("Rider Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = riderPhone, onValueChange = { riderPhone = it }, label = { Text("Rider Phone Number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = riderVehicle, onValueChange = { riderVehicle = it }, label = { Text("Vehicle Plate No.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = etaText, onValueChange = { etaText = it.filter { c -> c.isDigit() } }, label = { Text("ETA (Minutes)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onDispatch(
                        riderName.trim().ifBlank { "Take It Rider" },
                        riderPhone.trim(),
                        riderVehicle.trim(),
                        etaText.toIntOrNull()?.coerceIn(5, 120) ?: 18
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
            ) { Text("Dispatch Live", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) } }
    )
}

@Composable
private fun AdminRideCard(
    ride: RideBooking,
    onAssignCaptain: () -> Unit,
    onArriving: () -> Unit,
    onStartTrip: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    TakeIt3DCard(
        containerColor = AdminSurface,
        depthColor = Color(0xFF060A15),
        borderColor = RideYellow.copy(alpha = 0.35f),
        cornerRadius = 22.dp,
        depth = 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${ride.rideNumber} • ${ride.vehicleType}", color = RideYellow, fontWeight = FontWeight.ExtraBold)
                    AdminOrderStatusPill(ride.status)
                }
                Text("₹${ride.totalFare}", color = AdminEmerald, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = AdminCard, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Rider: ${ride.customerName} (${ride.customerPhone.ifBlank { ride.customerEmail }})", color = AdminInk, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text("${ride.pickupAddress} -> ${ride.dropAddress} (${ride.distanceKm} km)", color = AdminMuted, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pay: ${ride.paymentMethod} (${ride.paymentStatus})", color = AdminCyan, style = MaterialTheme.typography.labelSmall)
                        Text("OTP: ${ride.otp}", color = AdminGold, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                    }
                    if (ride.captainName.isNotBlank() && ride.captainName != "Finding Captain...") {
                        Text("Captain: ${ride.captainName} • ${ride.vehicleNumber} • ${ride.captainPhone}", color = AdminEmerald, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (ride.status !in listOf("COMPLETED", "CANCELLED", "REFUNDED")) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (ride.status) {
                        "REQUESTED" -> TakeIt3DButton(
                            onClick = onAssignCaptain,
                            containerColor = RideYellow,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Assign Captain", fontWeight = FontWeight.ExtraBold) }
                        "CAPTAIN_ASSIGNED" -> TakeIt3DButton(
                            onClick = onArriving,
                            containerColor = AdminCyan,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Mark Arriving", fontWeight = FontWeight.ExtraBold) }
                        "ARRIVING" -> TakeIt3DButton(
                            onClick = onStartTrip,
                            containerColor = AdminGold,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Start Trip", fontWeight = FontWeight.ExtraBold) }
                        "ON_TRIP" -> TakeIt3DButton(
                            onClick = onComplete,
                            containerColor = AdminEmerald,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Complete Ride", fontWeight = FontWeight.ExtraBold) }
                    }
                    OutlinedButton(onClick = onCancel, border = BorderStroke(1.dp, AdminCrimson), modifier = Modifier.height(44.dp)) {
                        Text("Cancel", color = AdminCrimson, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminParcelCard(
    parcel: ParcelOrder,
    onAssignPartner: () -> Unit,
    onPickedUp: () -> Unit,
    onInTransit: () -> Unit,
    onDelivered: () -> Unit,
    onCancel: () -> Unit
) {
    TakeIt3DCard(
        containerColor = AdminSurface,
        depthColor = Color(0xFF060A15),
        borderColor = TransportOrange.copy(alpha = 0.35f),
        cornerRadius = 22.dp,
        depth = 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${parcel.parcelNumber} • ${parcel.vehicleType}", color = TransportOrange, fontWeight = FontWeight.ExtraBold)
                    AdminOrderStatusPill(parcel.status)
                }
                Text("₹${parcel.totalFare}", color = AdminEmerald, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Surface(shape = RoundedCornerShape(12.dp), color = AdminCard, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Sender: ${parcel.senderName} -> Receiver: ${parcel.receiverName} (${parcel.receiverPhone})", color = AdminInk, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    Text("${parcel.pickupAddress} -> ${parcel.dropAddress} • ${parcel.weightKg}kg (${parcel.packageType})", color = AdminMuted, style = MaterialTheme.typography.labelSmall)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Pay: ${parcel.paymentMethod} (${parcel.paymentStatus})", color = AdminCyan, style = MaterialTheme.typography.labelSmall)
                        Text("OTP: ${parcel.otp}", color = AdminGold, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelSmall)
                    }
                    if (parcel.partnerName.isNotBlank() && parcel.partnerName != "Assigning Partner...") {
                        Text("Partner: ${parcel.partnerName} • ${parcel.vehicleNumber} • ${parcel.partnerPhone}", color = AdminEmerald, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            if (parcel.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED")) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (parcel.status) {
                        "BOOKED" -> TakeIt3DButton(
                            onClick = onAssignPartner,
                            containerColor = TransportOrange,
                            contentColor = Color.White,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Assign Partner", fontWeight = FontWeight.ExtraBold) }
                        "PARTNER_ASSIGNED" -> TakeIt3DButton(
                            onClick = onPickedUp,
                            containerColor = AdminCyan,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Mark Picked Up", fontWeight = FontWeight.ExtraBold) }
                        "PICKED_UP" -> TakeIt3DButton(
                            onClick = onInTransit,
                            containerColor = AdminGold,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("In Transit", fontWeight = FontWeight.ExtraBold) }
                        "IN_TRANSIT" -> TakeIt3DButton(
                            onClick = onDelivered,
                            containerColor = AdminEmerald,
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 44.dp,
                            modifier = Modifier.weight(1f)
                        ) { Text("Mark Delivered", fontWeight = FontWeight.ExtraBold) }
                    }
                    OutlinedButton(onClick = onCancel, border = BorderStroke(1.dp, AdminCrimson), modifier = Modifier.height(44.dp)) {
                        Text("Cancel", color = AdminCrimson, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DispatchRideCaptainDialog(
    ride: RideBooking,
    onDismiss: () -> Unit,
    onDispatch: (captainName: String, captainPhone: String, vehicleNumber: String, etaMinutes: Int) -> Unit
) {
    var name by remember { mutableStateOf("Vikram Singh") }
    var phone by remember { mutableStateOf("+91 98111 44556") }
    var plate by remember { mutableStateOf("UP16 BR 2026") }
    var eta by remember { mutableStateOf("4") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text("Assign Captain • ${ride.rideNumber}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Captain Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Captain Phone") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Vehicle Plate No.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = eta, onValueChange = { eta = it.filter(Char::isDigit) }, label = { Text("ETA (Min)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onDispatch(name.ifBlank { "Take It Captain" }, phone, plate, eta.toIntOrNull() ?: 4) },
                colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = AdminBg)
            ) { Text("Assign Live", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) } }
    )
}

@Composable
private fun DispatchParcelPartnerDialog(
    parcel: ParcelOrder,
    onDismiss: () -> Unit,
    onDispatch: (partnerName: String, partnerPhone: String, vehicleNumber: String, etaMinutes: Int) -> Unit
) {
    var name by remember { mutableStateOf("Suresh Yadav") }
    var phone by remember { mutableStateOf("+91 98990 77889") }
    var plate by remember { mutableStateOf("UP16 TR 8842") }
    var eta by remember { mutableStateOf("20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text("Assign Partner • ${parcel.parcelNumber}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Transport Partner Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Partner Phone") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = plate, onValueChange = { plate = it }, label = { Text("Vehicle / Truck No.") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = eta, onValueChange = { eta = it.filter(Char::isDigit) }, label = { Text("ETA (Min)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = { onDispatch(name.ifBlank { "Take It Partner" }, phone, plate, eta.toIntOrNull() ?: 20) },
                colors = ButtonDefaults.buttonColors(containerColor = TransportOrange, contentColor = Color.White)
            ) { Text("Dispatch Partner", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) } }
    )
}
