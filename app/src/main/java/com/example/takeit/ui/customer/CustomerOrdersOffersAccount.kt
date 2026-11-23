package com.example.takeit.ui.customer

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.network.TakeItPaymentBus
import com.example.network.TakeItPaymentGateway
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.viewmodel.TakeItUiState
import kotlinx.coroutines.launch

@Composable
fun CustomerOrdersTab(
    uiState: TakeItUiState,
    onOpenTrack: (String) -> Unit,
    onOpenDelivered: (String) -> Unit
) {
    TakeIt3DBackdrop(TakeItRed, TakeItOrange, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFDCE2EB),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("My Orders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Surface(shape = RoundedCornerShape(50), color = TakeItGoldSoft, border = BorderStroke(1.dp, TakeItGold)) {
                            Text(
                                "${uiState.myOrders.size} Total",
                                style = MaterialTheme.typography.labelMedium,
                                color = TakeItRedDark,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
            if (uiState.myOrders.isEmpty()) {
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 24.dp,
                        depth = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.ReceiptLong,
                                containerColor = TakeItRed,
                                size = 64.dp
                            )
                            Text("No orders placed yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Order your favorite meal and track it live here!", color = TakeItMuted)
                        }
                    }
                }
            } else {
                items(uiState.myOrders, key = { it.id }) { order ->
                    val statusColor = when (order.status) {
                        "DELIVERED" -> TakeItGreen
                        "CANCELLED", "REFUNDED" -> TakeItRed
                        else -> TakeItOrange
                    }
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 5.dp,
                        onClick = {
                            if (order.status == "DELIVERED") onOpenDelivered(order.id) else onOpenTrack(order.id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(order.restaurantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.14f)) {
                                    Text(
                                        order.status.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = statusColor,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${order.orderNumber} • ₹${order.totalAmount} (${order.paymentMethod})", fontWeight = FontWeight.Bold, color = TakeItInk)
                                Text(
                                    if (order.status == "DELIVERED") "Rate / View →" else "Live Track →",
                                    color = TakeItRed,
                                    fontWeight = FontWeight.ExtraBold,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerOffersTab(
    uiState: TakeItUiState,
    onApplyCouponAndOpenCart: (PromoCoupon) -> Unit
) {
    val active = uiState.coupons.filter { it.activeFlag }
    TakeIt3DBackdrop(TakeItGold, TakeItRed, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFDCE2EB),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TakeIt3DIconOrb(
                            icon = Icons.Rounded.LocalOffer,
                            containerColor = TakeItRed,
                            size = 38.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Live Offers & Coupons", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            if (active.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No active promo codes right now. Check back soon!", color = TakeItMuted)
                    }
                }
            } else {
                items(active, key = { it.id }) { c ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        borderColor = TakeItGold.copy(alpha = 0.5f),
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(shape = RoundedCornerShape(8.dp), color = TakeItGoldSoft, border = BorderStroke(1.dp, TakeItGold)) {
                                    Text(
                                        c.code,
                                        color = TakeItRedDark,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text(c.description, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                                Text("Min order ₹${c.minOrderAmount}", style = MaterialTheme.typography.labelSmall, color = TakeItGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            TakeIt3DButton(
                                onClick = { onApplyCouponAndOpenCart(c) },
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                cornerRadius = 20.dp,
                                depth = 4.dp,
                                height = 44.dp,
                                modifier = Modifier.width(98.dp)
                            ) {
                                Text("Apply", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerAccountTab(
    uiState: TakeItUiState,
    onUpdateProfile: (name: String, phone: String) -> Unit,
    onOpenAddresses: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenOrdersTab: () -> Unit,
    onEnterAdminCommandCenter: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    TakeIt3DBackdrop(TakeItRed, TakeItGold, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3D Crimson-Orange Profile Slab
            item {
                TakeIt3DCard(
                    containerColor = TakeItRed,
                    depthColor = TakeItRedDark.darken3D(0.45f),
                    borderColor = TakeItOrange.lighten3D(0.35f),
                    cornerRadius = 28.dp,
                    depth = 6.dp,
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(TakeItRedDark, TakeItRed, TakeItOrange)))
                            .statusBarsPadding()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .shadow(10.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(3.dp, TakeItGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    uiState.profile.name.ifBlank { uiState.displayName }.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TakeItRed,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    uiState.profile.name.ifBlank { uiState.displayName },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    uiState.profile.phone.ifBlank { uiState.email },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Surface(shape = RoundedCornerShape(50), color = TakeItGold, shadowElevation = 3.dp) {
                                    Text(
                                        if (uiState.isMainAdmin) "👑 Main Admin Account" else if (uiState.adminRole == AdminRole.CO_ADMIN) "🛡️ Co-Admin Staff" else "👑 Gold Member",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TakeItInk,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.Edit,
                                containerColor = Color.White.copy(alpha = 0.22f),
                                depthColor = TakeItRedDark,
                                iconTint = Color.White,
                                size = 40.dp,
                                contentDescription = "Edit Profile",
                                onClick = { showEditDialog = true }
                            )
                        }
                    }
                }
            }

            // Recent Favorites Strip
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Recent Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        TextButton(onClick = onOpenOrdersTab) {
                            Text("View All →", color = TakeItRed, fontWeight = FontWeight.Bold)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        val itemsToShow = uiState.restaurants.take(5)
                        items(itemsToShow, key = { "strip_${it.id}" }) { r ->
                            Box(
                                modifier = Modifier
                                    .size(76.dp)
                                    .shadow(6.dp, RoundedCornerShape(18.dp))
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.5.dp, Color.White, RoundedCornerShape(18.dp))
                            ) {
                                FoodPhoto(
                                    imageUrl = r.imageUrl,
                                    fallbackKey = r.name,
                                    contentDescription = r.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // 3D Admin Portal Card
            if (uiState.isMainAdmin || uiState.adminRole == AdminRole.CO_ADMIN) {
                item {
                    TakeIt3DCard(
                        containerColor = AdminBg,
                        depthColor = Color.Black,
                        borderColor = AdminGold,
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        onClick = onEnterAdminCommandCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("open_admin_command_center_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                TakeIt3DIconOrb(
                                    icon = Icons.Rounded.AdminPanelSettings,
                                    containerColor = AdminGold,
                                    iconTint = AdminBg,
                                    size = 44.dp
                                )
                                Column {
                                    Text(
                                        if (uiState.isMainAdmin) "Open Main Admin Command Center" else "Open Co-Admin Control Panel",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        if (uiState.isMainAdmin) "Full control + Create Co-Admin Passwords" else "Login with Main Admin password",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdminGold
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = AdminGold)
                        }
                    }
                }
            }

            // 3D Account Menu Card
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column {
                        AccountMenuRow(Icons.Rounded.LocationOn, "My Addresses", "${uiState.addresses.size} saved", onOpenAddresses)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.AccountBalanceWallet, "Wallet & Offers", "₹${uiState.profile.walletBalance.toInt()}", onOpenWallet)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.SupportAgent, "Help & Support", "${uiState.myTickets.size} tickets", onOpenSupport)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.Info, "About Take It", "v2.0", onOpenAbout)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.SwitchAccount, "Switch Google Account / Sign Out", "", onSignOut, tint = TakeItRed)
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        var nameInput by remember { mutableStateOf(uiState.profile.name.ifBlank { uiState.displayName }) }
        var phoneInput by remember { mutableStateOf(uiState.profile.phone) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("Phone Number") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, phoneInput)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TakeItRed)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AccountMenuRow(
    icon: ImageVector,
    title: String,
    badge: String,
    onClick: () -> Unit,
    tint: Color = TakeItInk
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TakeIt3DIconOrb(
                icon = icon,
                containerColor = if (tint == TakeItRed) TakeItRed else TakeItOrange,
                size = 34.dp
            )
            Text(title, fontWeight = FontWeight.Bold, color = tint)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (badge.isNotBlank()) {
                Surface(shape = RoundedCornerShape(50), color = TakeItGoldSoft) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = TakeItRedDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TakeItMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAddressesScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onSaveAddress: (label: String, full: String, landmark: String) -> Unit,
    onSelectAddress: (label: String, full: String) -> Unit,
    onDeleteAddress: (String) -> Unit
) {
    var label by remember { mutableStateOf("Home") }
    var full by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }

    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("My Delivery Addresses", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        TakeIt3DBackdrop(TakeItRed, TakeItGold, baseColor = TakeItBg) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 22.dp,
                        depth = 5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add New Address", fontWeight = FontWeight.ExtraBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Home", "Work", "Other").forEach { tag ->
                                    FilterChip(selected = label == tag, onClick = { label = tag }, label = { Text(tag) })
                                }
                            }
                            OutlinedTextField(value = full, onValueChange = { full = it }, label = { Text("House / Flat / Street / Sector") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = landmark, onValueChange = { landmark = it }, label = { Text("Landmark (Optional)") }, modifier = Modifier.fillMaxWidth())
                            TakeIt3DButton(
                                onClick = {
                                    if (full.isNotBlank()) {
                                        onSaveAddress(label, full, landmark)
                                        full = ""
                                        landmark = ""
                                    }
                                },
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                height = 48.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Save & Deliver Here", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
                items(uiState.addresses, key = { it.id }) { addr ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 18.dp,
                        depth = 4.dp,
                        onClick = { onSelectAddress(addr.label, addr.fullAddress) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(addr.label, fontWeight = FontWeight.ExtraBold, color = TakeItRed)
                                Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall)
                                if (addr.landmark.isNotBlank()) Text("Near ${addr.landmark}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                            IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, "Delete", tint = TakeItRed)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerWalletScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onTopUpConfirmed: (Double, String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var amountInput by remember { mutableStateOf("500") }
    var pendingRef by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        TakeItPaymentBus.results.collect { res ->
            if (res.orderRef == pendingRef && res.status.equals("success", ignoreCase = true)) {
                pendingRef = null
                onTopUpConfirmed(res.amount.toDouble(), "RAZORPAY", "Wallet Top-Up (${res.paymentId})")
            }
        }
    }

    DisposableEffect(lifecycleOwner, pendingRef) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingRef != null) {
                val ref = pendingRef ?: return@LifecycleEventObserver
                scope.launch {
                    val res = TakeItPaymentGateway.checkPaymentStatus(ref)
                    if (res != null) {
                        pendingRef = null
                        onTopUpConfirmed(res.amount.toDouble(), "RAZORPAY", "Wallet Top-Up (${res.paymentId})")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("Take It Wallet", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        TakeIt3DBackdrop(TakeItOrange, TakeItRed, baseColor = TakeItBg) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    TakeIt3DCard(
                        containerColor = TakeItRed,
                        depthColor = TakeItRedDark.darken3D(0.45f),
                        borderColor = TakeItGold,
                        cornerRadius = 24.dp,
                        depth = 6.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(TakeItRedDark, TakeItOrange)))
                                .padding(22.dp)
                        ) {
                            Column {
                                Text("Available Wallet Balance", color = TakeItGoldSoft)
                                Text(
                                    "₹${uiState.profile.walletBalance.toInt()}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add Money via Razorpay (Chrome)", fontWeight = FontWeight.ExtraBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("200", "500", "1000").forEach { preset ->
                                    FilterChip(selected = amountInput == preset, onClick = { amountInput = preset }, label = { Text("₹$preset") })
                                }
                            }
                            OutlinedTextField(value = amountInput, onValueChange = { amountInput = it.filter(Char::isDigit) }, label = { Text("Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            TakeIt3DButton(
                                onClick = {
                                    val amt = amountInput.toIntOrNull() ?: 0
                                    if (amt < 10) {
                                        onShowMessage("Enter at least ₹10")
                                    } else {
                                        val ref = "TKW_${System.currentTimeMillis()}"
                                        pendingRef = ref
                                        TakeItPaymentGateway.launchChromePayment(
                                            context = context,
                                            orderRef = ref,
                                            amount = amt,
                                            serviceType = "WALLET_TOPUP",
                                            itemTitle = "Take It Wallet Top-Up",
                                            customerName = uiState.profile.name.ifBlank { uiState.displayName },
                                            customerPhone = uiState.profile.phone,
                                            userId = uiState.uid
                                        )
                                    }
                                },
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                height = 50.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Add ₹$amountInput via Razorpay", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
                item { Text("Transaction Ledger", fontWeight = FontWeight.ExtraBold) }
                items(uiState.walletTx, key = { it.id }) { tx ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 16.dp,
                        depth = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.title, fontWeight = FontWeight.Bold)
                                Text(tx.method, style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                            val isCredit = tx.txType != "DEBIT"
                            Text(
                                "${if (isCredit) "+" else "-"}₹${tx.amount.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCredit) TakeItGreen else TakeItRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSupportScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onSubmitTicket: (subject: String, message: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        TakeIt3DBackdrop(TakeItRed, TakeItOrange, baseColor = TakeItBg) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Raise Live Support Ticket", fontWeight = FontWeight.ExtraBold)
                            Text("Support Hotline: ${uiState.config.supportPhone}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Issue Subject") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe your issue...") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            TakeIt3DButton(
                                onClick = {
                                    if (subject.isNotBlank() && message.isNotBlank()) {
                                        onSubmitTicket(subject, message)
                                        subject = ""
                                        message = ""
                                    }
                                },
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                height = 48.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Send to Take It Support", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
                items(uiState.myTickets, key = { it.id }) { t ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 18.dp,
                        depth = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(t.subject, fontWeight = FontWeight.ExtraBold)
                                Text(t.status, color = if (t.status == "RESOLVED") TakeItGreen else TakeItOrange, fontWeight = FontWeight.Bold)
                            }
                            Text(t.message, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                            if (t.adminReply.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(10.dp), color = TakeItGreenSoft) {
                                    Text(
                                        "Admin Reply: ${t.adminReply}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TakeItGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAboutScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("About Take It", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        TakeIt3DBackdrop(TakeItOrange, TakeItRed, baseColor = TakeItBg) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(TakeItGold, TakeItOrange, TakeItRed)))
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_takeit_food),
                        contentDescription = "Take It Official Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Take It — Food Delivery App", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Serving ${uiState.config.cityName} with real-time kitchen sync, verified online payments, and live doorstep tracking.", color = TakeItMuted)
                        Text("Support: ${uiState.config.supportEmail} • ${uiState.config.supportPhone}", style = MaterialTheme.typography.labelMedium, color = TakeItRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
