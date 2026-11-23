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
import com.example.takeit.viewmodel.TakeItUiState
import kotlinx.coroutines.launch

@Composable
fun CustomerOrdersTab(
    uiState: TakeItUiState,
    onOpenTrack: (String) -> Unit,
    onOpenDelivered: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TakeItBg),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("My Orders", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                    Surface(shape = RoundedCornerShape(50), color = TakeItGoldSoft) {
                        Text(
                            "${uiState.myOrders.size} Total",
                            style = MaterialTheme.typography.labelMedium,
                            color = TakeItRedDark,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
        if (uiState.myOrders.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.ReceiptLong, null, tint = TakeItRed, modifier = Modifier.size(56.dp))
                    Text("No orders placed yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Order your favorite meal and track it live here!", color = TakeItMuted)
                }
            }
        } else {
            items(uiState.myOrders, key = { it.id }) { order ->
                val statusColor = when (order.status) {
                    "DELIVERED" -> TakeItGreen
                    "CANCELLED", "REFUNDED" -> TakeItRed
                    else -> TakeItOrange
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 7.dp)
                        .clickable {
                            if (order.status == "DELIVERED") onOpenDelivered(order.id) else onOpenTrack(order.id)
                        }
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

@Composable
fun CustomerOffersTab(
    uiState: TakeItUiState,
    onApplyCouponAndOpenCart: (PromoCoupon) -> Unit
) {
    val active = uiState.coupons.filter { it.activeFlag }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TakeItBg),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Surface(color = Color.White, shadowElevation = 2.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.LocalOffer, null, tint = TakeItRed)
                    Spacer(Modifier.width(10.dp))
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
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    shadowElevation = 3.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
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
                        Button(
                            onClick = { onApplyCouponAndOpenCart(c) },
                            colors = ButtonDefaults.buttonColors(containerColor = TakeItRed),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TakeItBg),
        contentPadding = PaddingValues(bottom = 110.dp)
    ) {
        // FoodieX / Zomato Crimson-Orange Profile Header
        item {
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
                            .size(68.dp)
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
                        Surface(shape = RoundedCornerShape(50), color = TakeItGold) {
                            Text(
                                if (uiState.isMainAdmin) "👑 Main Admin Account" else if (uiState.adminRole == AdminRole.CO_ADMIN) "🛡️ Co-Admin Staff" else "👑 Gold Member",
                                style = MaterialTheme.typography.labelSmall,
                                color = TakeItInk,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                    }
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Rounded.Edit, "Edit Profile", tint = Color.White)
                    }
                }
            }
        }

        // My Orders Strip
        item {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    val itemsToShow = uiState.restaurants.take(5)
                    items(itemsToShow, key = { "strip_${it.id}" }) { r ->
                        FoodPhoto(
                            imageUrl = r.imageUrl,
                            fallbackKey = r.name,
                            contentDescription = r.name,
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(16.dp))
                        )
                    }
                }
            }
        }

        // Admin Command Center Portal Card (Visible for Main Admin buildolive4@gmail.com OR Co-Admins!)
        if (uiState.isMainAdmin || uiState.adminRole == AdminRole.CO_ADMIN) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = AdminBg,
                    border = BorderStroke(1.5.dp, AdminGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clickable { onEnterAdminCommandCenter() }
                        .testTag("open_admin_command_center_button")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Rounded.AdminPanelSettings, null, tint = AdminGold, modifier = Modifier.size(32.dp))
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

        // Account Menu Card
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
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
                    AccountMenuRow(Icons.Rounded.Logout, "Sign Out", "", onSignOut, tint = TakeItRed)
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
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = if (tint == TakeItRed) TakeItRed else TakeItOrange)
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add New Address", fontWeight = FontWeight.ExtraBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("Home", "Work", "Other").forEach { tag ->
                                FilterChip(selected = label == tag, onClick = { label = tag }, label = { Text(tag) })
                            }
                        }
                        OutlinedTextField(value = full, onValueChange = { full = it }, label = { Text("House / Flat / Street / Sector") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = landmark, onValueChange = { landmark = it }, label = { Text("Landmark (Optional)") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (full.isNotBlank()) {
                                    onSaveAddress(label, full, landmark)
                                    full = ""
                                    landmark = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TakeItRed),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Save & Deliver Here", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            items(uiState.addresses, key = { it.id }) { addr ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectAddress(addr.label, addr.fullAddress) }
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
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
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add Money via Razorpay (Chrome)", fontWeight = FontWeight.ExtraBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("200", "500", "1000").forEach { preset ->
                                FilterChip(selected = amountInput == preset, onClick = { amountInput = preset }, label = { Text("₹$preset") })
                            }
                        }
                        OutlinedTextField(value = amountInput, onValueChange = { amountInput = it.filter(Char::isDigit) }, label = { Text("Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        Button(
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
                            colors = ButtonDefaults.buttonColors(containerColor = TakeItRed),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Add ₹$amountInput via Razorpay", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            item { Text("Transaction Ledger", fontWeight = FontWeight.ExtraBold) }
            items(uiState.walletTx, key = { it.id }) { tx ->
                Surface(shape = RoundedCornerShape(14.dp), color = Color.White) {
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Raise Live Support Ticket", fontWeight = FontWeight.ExtraBold)
                        Text("Support Hotline: ${uiState.config.supportPhone}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Issue Subject") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe your issue...") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (subject.isNotBlank() && message.isNotBlank()) {
                                    onSubmitTicket(subject, message)
                                    subject = ""
                                    message = ""
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TakeItRed),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Send to Take It Support", fontWeight = FontWeight.Bold) }
                    }
                }
            }
            items(uiState.myTickets, key = { it.id }) { t ->
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Image(
                painter = painterResource(R.drawable.logo_takeit_food),
                contentDescription = "Take It Official Logo",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(3.dp, TakeItOrange, CircleShape)
            )
            Text("Take It — Food Delivery App", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
            Text("Serving ${uiState.config.cityName} with real-time kitchen sync, verified online payments, and live doorstep tracking.", color = TakeItMuted)
            Text("Support: ${uiState.config.supportEmail} • ${uiState.config.supportPhone}", style = MaterialTheme.typography.labelMedium, color = TakeItRed, fontWeight = FontWeight.Bold)
        }
    }
}
