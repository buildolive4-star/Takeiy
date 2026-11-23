package com.example.ui.screens.admin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ManagedUserEntity
import com.example.data.OrderEntity
import com.example.model.AdminPlatformConfig
import com.example.ui.components.StatusPill
import com.example.ui.screens.partners.DarkKpiTile
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState

@Composable
fun AdminPanelScreen(
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onLockAdmin: () -> Unit,
    onToggleVerifyUser: (ManagedUserEntity) -> Unit,
    onToggleSuspendUser: (ManagedUserEntity) -> Unit,
    onRefundOrder: (OrderEntity) -> Unit,
    onCompleteOrder: (String) -> Unit,
    onUpdateConfig: ((AdminPlatformConfig) -> AdminPlatformConfig) -> Unit,
    onSendBroadcast: (String, String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0 Overview, 1 Users, 2 Orders, 3 Finance & Control
    var userRoleFilter by remember { mutableStateOf("All") }
    var broadcastTitle by remember { mutableStateOf("Weekend Super Flash Sale!") }
    var broadcastBody by remember { mutableStateOf("Enjoy ₹50 OFF on Rides, Food & 9-min Grocery with code ZIP50.") }

    val totalOrderRevenue = uiState.orders.sumOf { it.amount }.toInt() + 1_248_900
    val commissionEarned = (totalOrderRevenue * uiState.adminConfig.platformCommissionPct) / 100

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Admin Executive Header
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF0C4A6E))))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SoftWhite.copy(alpha = 0.12f))
                        ) { Icon(Icons.Rounded.Menu, "Switch Panel", tint = SkyBlue) }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("AUTHORIZED ADMIN ONLY", style = MaterialTheme.typography.labelSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                            Text("Zipora Control Tower", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        }
                        FilledTonalButton(
                            onClick = onLockAdmin,
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CoralDanger.copy(alpha = 0.25f), contentColor = CoralDanger),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("admin_lock_button")
                        ) {
                            Icon(Icons.Rounded.Lock, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Lock")
                        }
                    }

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Overview", "Users (${uiState.managedUsers.size})", "Orders (${uiState.orders.size})", "Control Center").forEachIndexed { idx, title ->
                            item {
                                FilterChip(
                                    selected = selectedTab == idx,
                                    onClick = { selectedTab = idx },
                                    label = { Text(title, fontWeight = FontWeight.SemiBold) }
                                )
                            }
                        }
                    }
                }
            }

            when (selectedTab) {
                0 -> {
                    // OVERVIEW KPI GRID + REVENUE CHART
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Platform Ecosystem Overview", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DarkKpiTile("Total Users", "1,48,920", SkyBlue, Modifier.weight(1f))
                                DarkKpiTile("Active Drivers", "12,450", PremiumYellow, Modifier.weight(1f))
                                DarkKpiTile("Restaurants", "3,820", OrangeAccent, Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DarkKpiTile("Grocery Shops", "1,190", SuccessGreen, Modifier.weight(1f))
                                DarkKpiTile("Live Orders", "${uiState.orders.size + 4280}", VioletAccent, Modifier.weight(1f))
                                DarkKpiTile("GMV Revenue", "₹${totalOrderRevenue / 1000}K", PremiumYellow, Modifier.weight(1f))
                            }

                            // Canvas Bar Chart: Revenue by Vertical
                            Surface(shape = RoundedCornerShape(20.dp), color = DarkCardSurface) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Revenue Mix Across All 5 Verticals", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                    val bars = listOf(
                                        Triple("Rides", 0.78f, SkyBlue),
                                        Triple("Food", 0.92f, OrangeAccent),
                                        Triple("Grocery", 0.85f, SuccessGreen),
                                        Triple("Parcel", 0.54f, PremiumYellow),
                                        Triple("Cargo", 0.68f, VioletAccent)
                                    )
                                    Canvas(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                                        val colW = size.width / bars.size
                                        bars.forEachIndexed { i, (_, frac, col) ->
                                            val barW = colW * 0.52f
                                            val left = i * colW + (colW - barW) / 2f
                                            val h = size.height * frac
                                            drawRoundRect(
                                                color = col,
                                                topLeft = Offset(left, size.height - h),
                                                size = Size(barW, h),
                                                cornerRadius = CornerRadius(14f, 14f)
                                            )
                                        }
                                    }
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                                        bars.forEach { (label, _, col) ->
                                            Text(label, style = MaterialTheme.typography.labelSmall, color = col, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // USER MANAGEMENT
                    item {
                        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("All", "Customer", "Driver", "Restaurant", "Shop")) { role ->
                                FilterChip(selected = userRoleFilter == role, onClick = { userRoleFilter = role }, label = { Text(role) })
                            }
                        }
                    }
                    val filteredUsers = uiState.managedUsers.filter { userRoleFilter == "All" || it.role == userRoleFilter }
                    items(filteredUsers, key = { it.id }) { user ->
                        Surface(shape = RoundedCornerShape(18.dp), color = DarkCardSurface, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Column {
                                        Text(user.name, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                        Text("${user.role} • ${user.city} • ★ ${user.rating} • ${user.totalOrders} orders", style = MaterialTheme.typography.bodySmall, color = SkyBlue)
                                    }
                                    StatusPill(if (user.isSuspended) "SUSPENDED" else if (user.isVerified) "VERIFIED" else "PENDING")
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onToggleVerifyUser(user) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen)
                                    ) { Text(if (user.isVerified) "Unverify" else "Verify Partner") }
                                    Button(
                                        onClick = { onToggleSuspendUser(user) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (user.isSuspended) SuccessGreen else CoralDanger,
                                            contentColor = SoftWhite
                                        )
                                    ) { Text(if (user.isSuspended) "Activate" else "Suspend") }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // ORDER MANAGEMENT
                    items(uiState.orders, key = { it.id }) { order ->
                        Surface(shape = RoundedCornerShape(18.dp), color = DarkCardSurface, modifier = Modifier.padding(horizontal = 16.dp)) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("${order.id} • ${order.type}", style = MaterialTheme.typography.labelMedium, color = PremiumYellow)
                                    StatusPill(order.status)
                                }
                                Text(order.title, style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                Text("₹${order.amount.toInt()} via ${order.paymentMethod} • Partner: ${order.partnerName}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8D4EB))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (order.status != "COMPLETED" && order.status != "REFUNDED") {
                                        Button(
                                            onClick = { onCompleteOrder(order.id) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite)
                                        ) { Text("Force Complete") }
                                    }
                                    if (order.status != "REFUNDED") {
                                        OutlinedButton(
                                            onClick = { onRefundOrder(order) },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = VioletAccent)
                                        ) { Text("Issue Refund") }
                                    }
                                }
                            }
                        }
                    }
                }

                else -> {
                    // FINANCE & CONTROL CENTER
                    item {
                        Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Surface(shape = RoundedCornerShape(20.dp), color = DarkCardSurface) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Finance & Commission Controls", style = MaterialTheme.typography.titleMedium, color = PremiumYellow)
                                    Text("Platform Commission Earned: ₹$commissionEarned (${uiState.adminConfig.platformCommissionPct}%)", style = MaterialTheme.typography.bodyMedium, color = SuccessGreen)
                                    Text("Commission Rate: ${uiState.adminConfig.platformCommissionPct}%", style = MaterialTheme.typography.labelMedium, color = SoftWhite)
                                    Slider(
                                        value = uiState.adminConfig.platformCommissionPct.toFloat(),
                                        onValueChange = { v -> onUpdateConfig { it.copy(platformCommissionPct = v.toInt()) } },
                                        valueRange = 5f..25f
                                    )
                                    Text("Base Delivery Fee: ₹${uiState.adminConfig.baseDeliveryFee}", style = MaterialTheme.typography.labelMedium, color = SoftWhite)
                                    Slider(
                                        value = uiState.adminConfig.baseDeliveryFee.toFloat(),
                                        onValueChange = { v -> onUpdateConfig { it.copy(baseDeliveryFee = v.toInt()) } },
                                        valueRange = 15f..60f
                                    )
                                }
                            }

                            Surface(shape = RoundedCornerShape(20.dp), color = DarkCardSurface) {
                                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Broadcast Global Push Notification", style = MaterialTheme.typography.titleMedium, color = SkyBlue)
                                    OutlinedTextField(
                                        value = broadcastTitle,
                                        onValueChange = { broadcastTitle = it },
                                        label = { Text("Alert Title") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SoftWhite, unfocusedTextColor = SoftWhite, focusedBorderColor = SkyBlue, unfocusedBorderColor = DarkBorder)
                                    )
                                    OutlinedTextField(
                                        value = broadcastBody,
                                        onValueChange = { broadcastBody = it },
                                        label = { Text("Alert Message") },
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(focusedTextColor = SoftWhite, unfocusedTextColor = SoftWhite, focusedBorderColor = SkyBlue, unfocusedBorderColor = DarkBorder)
                                    )
                                    Button(
                                        onClick = { onSendBroadcast(broadcastTitle, broadcastBody) },
                                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("admin_broadcast_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = PremiumYellow, contentColor = DeepNavy)
                                    ) {
                                        Icon(Icons.Rounded.Campaign, null)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Send Live Broadcast", fontWeight = FontWeight.ExtraBold)
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
