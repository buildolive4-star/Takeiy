package com.example.takeit.ui.admin

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF0F172A), AdminSurface))
                    )
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_takeit_food),
                        contentDescription = "Take It Admin Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(2.dp, AdminGold, CircleShape)
                    )
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

                    IconButton(
                        onClick = onSignOut,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(AdminCard)
                            .testTag("admin_sign_out_btn")
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.Logout,
                            contentDescription = "Sign Out",
                            tint = AdminCrimson,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }

                // Executive Horizontal Tab Strip
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp),
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
                        Surface(
                            onClick = { viewModel.selectAdminTab(tab) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (selected) AdminGold else AdminCard,
                            border = BorderStroke(1.dp, if (selected) AdminGold else AdminBorder),
                            modifier = Modifier
                                .height(42.dp)
                                .testTag("admin_tab_${tab.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp),
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
                                    val liveCount = uiState.allOrders.count {
                                        it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED")
                                    }
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
                HorizontalDivider(color = AdminBorder)
            }
        }
    ) { innerPadding ->
        Box(
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(AdminBg, Color(0xFF141D35), AdminBg)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = AdminSurface,
            border = BorderStroke(1.dp, AdminBorder),
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_takeit_food),
                    contentDescription = "Take It Admin Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(92.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, AdminGold, CircleShape)
                )
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

                Button(
                    onClick = { onVerifyPasscode(passcode) },
                    enabled = passcode.isNotBlank() && !uiState.isCheckingCoAdmin,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminGold,
                        contentColor = AdminBg
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
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

@Composable
fun AdminOverviewTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    val grossRevenue = remember(uiState.allOrders) {
        uiState.allOrders.filter { it.status !in listOf("CANCELLED", "REFUNDED") }.sumOf { it.totalAmount }
    }
    val activeOrders = remember(uiState.allOrders) {
        uiState.allOrders.count { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
    }
    val openRestaurants = remember(uiState.restaurants) { uiState.restaurants.count { it.openFlag } }
    val openTickets = remember(uiState.allTickets) { uiState.allTickets.count { it.status != "RESOLVED" } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Live System Banner
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AdminCard,
                border = BorderStroke(1.dp, AdminGold.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Bolt, null, tint = AdminGold, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Live Cloud Command Sync",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = AdminInk
                        )
                        Spacer(Modifier.weight(1f))
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFF064E3B)) {
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
                        "Every change made here updates all Take It Customer apps instantly in real time via Firestore.",
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

        // KPI Grid Row 1
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AdminKpiCard(
                    title = "Gross Revenue",
                    value = "₹$grossRevenue",
                    subtitle = "${uiState.allOrders.size} total orders",
                    icon = Icons.Rounded.CurrencyRupee,
                    accent = AdminGold,
                    modifier = Modifier.weight(1f)
                )
                AdminKpiCard(
                    title = "Live Orders",
                    value = "$activeOrders",
                    subtitle = "Need action now",
                    icon = Icons.Rounded.DeliveryDining,
                    accent = if (activeOrders > 0) AdminCrimson else AdminEmerald,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // KPI Grid Row 2
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

        // Quick Actions Bar
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AdminSurface,
                border = BorderStroke(1.dp, AdminBorder),
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
                            Button(
                                onClick = { viewModel.seedStarterCatalog() },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("admin_seed_catalog_btn")
                            ) {
                                Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Seed Starter Menu", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        if (uiState.isMainAdmin) {
                            OutlinedButton(
                                onClick = { viewModel.selectAdminTab(AdminTab.CO_ADMINS) },
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, AdminCyan),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
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
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AdminSurface,
                    border = BorderStroke(1.dp, AdminBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.ReceiptLong, null, tint = AdminMuted, modifier = Modifier.size(38.dp))
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
                Surface(
                    onClick = { if (uiState.canManageOrders) viewModel.selectAdminTab(AdminTab.ORDERS) },
                    shape = RoundedCornerShape(16.dp),
                    color = AdminSurface,
                    border = BorderStroke(1.dp, AdminBorder),
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
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = AdminSurface,
        border = BorderStroke(1.dp, AdminBorder),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.labelMedium, color = AdminMuted, fontWeight = FontWeight.SemiBold)
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
                }
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
    Surface(shape = RoundedCornerShape(50), color = bg) {
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
    var filter by remember { mutableStateOf("ALL") }
    var dispatchTarget by remember { mutableStateOf<FoodOrder?>(null) }

    val filtered = remember(uiState.allOrders, filter) {
        when (filter) {
            "ACTIVE" -> uiState.allOrders.filter { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
            "DELIVERED" -> uiState.allOrders.filter { it.status == "DELIVERED" }
            "CANCELLED" -> uiState.allOrders.filter { it.status in listOf("CANCELLED", "REFUNDED") }
            else -> uiState.allOrders
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("ALL" to "All (${uiState.allOrders.size})", "ACTIVE" to "Live Active", "DELIVERED" to "Delivered", "CANCELLED" to "Cancelled / Refunded")) { (key, label) ->
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
            if (filtered.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AdminSurface,
                        border = BorderStroke(1.dp, AdminBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Rounded.Inventory2, null, tint = AdminMuted, modifier = Modifier.size(40.dp))
                            Text("No orders in this filter", color = AdminInk, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                items(filtered, key = { it.id }) { order ->
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
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = AdminSurface,
        border = BorderStroke(1.dp, AdminBorder),
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

            // Live 1-tap Order Progression Controls
            if (order.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED")) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when (order.status) {
                        "PLACED" -> {
                            Button(
                                onClick = onConfirm,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdminCyan, contentColor = AdminBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Confirm Order", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        "CONFIRMED" -> {
                            Button(
                                onClick = onPreparing,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Text("Start Preparing", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        "PREPARING" -> {
                            Button(
                                onClick = onOpenDispatch,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA855F7), contentColor = Color.White),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Rounded.TwoWheeler, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Assign Rider & Dispatch", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        "OUT_FOR_DELIVERY" -> {
                            Button(
                                onClick = onMarkDelivered,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdminEmerald, contentColor = AdminBg),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                            ) {
                                Icon(Icons.Rounded.CheckCircle, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Mark Delivered", fontWeight = FontWeight.ExtraBold)
                            }
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
        title = {
            Text("Dispatch ${order.orderNumber}", fontWeight = FontWeight.ExtraBold)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Assign a delivery rider. Customer live tracking map will update instantly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AdminMuted
                )
                OutlinedTextField(
                    value = riderName,
                    onValueChange = { riderName = it },
                    label = { Text("Rider Full Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = riderPhone,
                    onValueChange = { riderPhone = it },
                    label = { Text("Rider Phone Number") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = riderVehicle,
                    onValueChange = { riderVehicle = it },
                    label = { Text("Vehicle Plate No.") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = etaText,
                    onValueChange = { etaText = it.filter { c -> c.isDigit() } },
                    label = { Text("ETA (Minutes)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
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
            ) {
                Text("Dispatch Live", fontWeight = FontWeight.ExtraBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) }
        }
    )
}
