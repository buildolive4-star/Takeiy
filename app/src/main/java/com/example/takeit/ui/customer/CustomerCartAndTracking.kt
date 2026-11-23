package com.example.takeit.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerCartScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onClearCart: () -> Unit,
    onApplyCoupon: (PromoCoupon) -> Unit,
    onRemoveCoupon: () -> Unit,
    onOpenAddresses: () -> Unit,
    onPlaceOrder: (method: String, payStatus: String, payRef: String, onCreated: (String) -> Unit) -> Unit,
    onOrderCreated: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var selectedMethod by remember { mutableStateOf("RAZORPAY") } // RAZORPAY, WALLET, COD
    var showCouponSheet by remember { mutableStateOf(false) }
    var pendingOrderRef by remember { mutableStateOf<String?>(null) }
    var isVerifyingPayment by remember { mutableStateOf(false) }

    // Listen for takeit://payment-result deep link from Chrome Razorpay Checkout
    LaunchedEffect(Unit) {
        TakeItPaymentBus.results.collect { res ->
            if (res.orderRef == pendingOrderRef && res.status.equals("success", ignoreCase = true)) {
                pendingOrderRef = null
                isVerifyingPayment = false
                onPlaceOrder("RAZORPAY", "PAID", res.paymentId, onOrderCreated)
            }
        }
    }

    // Fallback verification when returning from Chrome to the app
    DisposableEffect(lifecycleOwner, pendingOrderRef) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingOrderRef != null && !isVerifyingPayment) {
                val ref = pendingOrderRef ?: return@LifecycleEventObserver
                scope.launch {
                    isVerifyingPayment = true
                    val verified = TakeItPaymentGateway.checkPaymentStatus(ref)
                    isVerifyingPayment = false
                    if (verified != null) {
                        pendingOrderRef = null
                        onPlaceOrder("RAZORPAY", "PAID", verified.paymentId, onOrderCreated)
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
                title = { Text("Your Cart", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (uiState.cart.isNotEmpty()) {
                        IconButton(onClick = onClearCart) {
                            Icon(Icons.Rounded.DeleteOutline, "Clear Cart", tint = TakeItRed)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            if (uiState.cart.isNotEmpty()) {
                Surface(
                    color = Color.White,
                    shadowElevation = 14.dp,
                    border = BorderStroke(1.dp, TakeItBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "₹${uiState.cartTotalPayable}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = TakeItInk
                            )
                            Text(
                                text = when (selectedMethod) {
                                    "RAZORPAY" -> "via Razorpay UPI/Card"
                                    "WALLET" -> "via Take It Wallet"
                                    else -> "Cash on Delivery"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = TakeItGreen,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Button(
                            onClick = {
                                when (selectedMethod) {
                                    "RAZORPAY" -> {
                                        val ref = "TKF_${System.currentTimeMillis()}"
                                        pendingOrderRef = ref
                                        val launched = TakeItPaymentGateway.launchChromePayment(
                                            context = context,
                                            orderRef = ref,
                                            amount = uiState.cartTotalPayable,
                                            serviceType = "FOOD",
                                            itemTitle = uiState.cartRestaurantName,
                                            customerName = uiState.profile.name.ifBlank { uiState.displayName },
                                            customerPhone = uiState.profile.phone,
                                            userId = uiState.uid
                                        )
                                        if (!launched) {
                                            pendingOrderRef = null
                                            onShowMessage("Could not open browser for Razorpay Checkout.")
                                        }
                                    }
                                    "WALLET" -> onPlaceOrder("WALLET", "PAID", "WALLET_${System.currentTimeMillis()}", onOrderCreated)
                                    else -> onPlaceOrder("COD", "PENDING", "COD_${System.currentTimeMillis()}", onOrderCreated)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TakeItRed, contentColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .height(54.dp)
                                .testTag("sticky_pay_now_button")
                        ) {
                            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = when (selectedMethod) {
                                    "RAZORPAY" -> "Pay ₹${uiState.cartTotalPayable} →"
                                    "WALLET" -> "Pay Wallet ₹${uiState.cartTotalPayable} →"
                                    else -> "Place Order ₹${uiState.cartTotalPayable} →"
                                },
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (uiState.cart.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(Icons.Rounded.ShoppingBag, null, tint = TakeItRed, modifier = Modifier.size(64.dp))
                    Text("Your food cart is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                    Text("Add delicious dishes from top restaurants to start ordering!", color = TakeItMuted, textAlign = TextAlign.Center)
                    Button(
                        onClick = onBack,
                        colors = ButtonDefaults.buttonColors(containerColor = TakeItRed),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Explore Restaurants", fontWeight = FontWeight.Bold)
                    }
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Restaurant Header Card
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_takeit_food),
                            contentDescription = "Restaurant",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(uiState.cartRestaurantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Delivering to ${uiState.profile.activeAddressLabel}: ${uiState.profile.activeAddressLine}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TakeItMuted,
                                maxLines = 1
                            )
                        }
                        TextButton(onClick = onOpenAddresses) {
                            Text("Change", color = TakeItRed, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Cart Items List
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        uiState.cart.forEachIndexed { index, item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                FoodPhoto(
                                    imageUrl = item.menuItem.imageUrl,
                                    fallbackKey = item.menuItem.name,
                                    contentDescription = item.menuItem.name,
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        VegMarkIcon(item.menuItem.vegFlag)
                                        Text(item.menuItem.name, fontWeight = FontWeight.Bold, color = TakeItInk)
                                    }
                                    Text("₹${item.menuItem.price} each", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                                }
                                Surface(shape = RoundedCornerShape(10.dp), color = TakeItBg, border = BorderStroke(1.dp, TakeItBorder)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = { onUpdateQty(item.menuItem.id, -1) }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Rounded.Remove, "Dec", tint = TakeItRed, modifier = Modifier.size(16.dp))
                                        }
                                        Text("${item.quantity}", fontWeight = FontWeight.ExtraBold, color = TakeItInk)
                                        IconButton(onClick = { onUpdateQty(item.menuItem.id, 1) }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Rounded.Add, "Inc", tint = TakeItRed, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                                Text(
                                    "₹${item.menuItem.price * item.quantity}",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TakeItInk
                                )
                            }
                            if (index < uiState.cart.lastIndex) HorizontalDivider(color = TakeItBorder)
                        }
                    }
                }
            }

            // Apply Coupon Row
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showCouponSheet = true }
                        .testTag("apply_coupon_row")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.LocalOffer, null, tint = TakeItGreen)
                            Column {
                                Text(
                                    if (uiState.appliedCoupon != null) "Coupon Applied: ${uiState.appliedCoupon.code}" else "Apply Coupon",
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (uiState.appliedCoupon != null) TakeItGreen else TakeItInk
                                )
                                Text(
                                    if (uiState.appliedCoupon != null) "You saved ₹${uiState.calculatedDiscount}!" else "${uiState.coupons.count { it.activeFlag }} live offers available",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TakeItMuted
                                )
                            }
                        }
                        if (uiState.appliedCoupon != null) {
                            TextButton(onClick = onRemoveCoupon) {
                                Text("Remove", color = TakeItRed, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = TakeItMuted)
                        }
                    }
                }
            }

            // Payment Method Selection
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Choose Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        val methods = listOf(
                            Triple("RAZORPAY", "UPI / Cards / NetBanking (Razorpay Chrome)", "Instant verified online payment via jaiambey.shop"),
                            Triple("WALLET", "Take It Wallet (Balance: ₹${uiState.profile.walletBalance.toInt()})", "Pay instantly with zero checkout wait"),
                            Triple("COD", "Cash on Delivery", "Pay cash or UPI to delivery partner at doorstep")
                        )
                        methods.forEach { (code, title, sub) ->
                            val selected = selectedMethod == code
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) Color(0xFFFFF1F2) else TakeItBg,
                                border = BorderStroke(1.5.dp, if (selected) TakeItRed else TakeItBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedMethod = code }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selected, onClick = { selectedMethod = code })
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = TakeItInk)
                                        Text(sub, style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Bill Breakdown matching FoodieX reference!
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        BillRow("Item Total", "₹${uiState.cartItemTotal}")
                        BillRow(
                            "Delivery Fee",
                            if (uiState.calculatedDeliveryFee == 0) "FREE" else "₹${uiState.calculatedDeliveryFee}",
                            valueColor = if (uiState.calculatedDeliveryFee == 0) TakeItGreen else TakeItInk
                        )
                        BillRow("Platform Fee", "₹${uiState.calculatedPlatformFee}")
                        BillRow("GST & Taxes (${uiState.config.gstPercent}%)", "₹${uiState.calculatedGst}")
                        if (uiState.calculatedDiscount > 0) {
                            BillRow("Coupon Discount", "- ₹${uiState.calculatedDiscount}", valueColor = TakeItGreen)
                        }
                        Spacer(Modifier.height(4.dp))
                        // Golden Total Payable Pill matching reference image!
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TakeItGold
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Payable", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TakeItInk)
                                Text("₹${uiState.cartTotalPayable}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TakeItInk)
                            }
                        }
                    }
                }
            }

            // Pending Chrome Payment Check Banner
            if (pendingOrderRef != null) {
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = TakeItGoldSoft, border = BorderStroke(1.dp, TakeItGold)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Completed Razorpay in Chrome?", fontWeight = FontWeight.Bold, color = TakeItInk)
                            Button(
                                onClick = {
                                    val ref = pendingOrderRef ?: return@Button
                                    scope.launch {
                                        isVerifyingPayment = true
                                        val verified = TakeItPaymentGateway.checkPaymentStatus(ref)
                                        isVerifyingPayment = false
                                        if (verified != null) {
                                            pendingOrderRef = null
                                            onPlaceOrder("RAZORPAY", "PAID", verified.paymentId, onOrderCreated)
                                        } else {
                                            onShowMessage("Payment not verified yet on server. Complete payment in Chrome.")
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = TakeItGreen)
                            ) {
                                Text(if (isVerifyingPayment) "Checking..." else "Verify Now")
                            }
                        }
                    }
                }
            }

            // Proceed to Pay Button
            item {
                Button(
                    onClick = {
                        when (selectedMethod) {
                            "RAZORPAY" -> {
                                val ref = "TKF_${System.currentTimeMillis()}"
                                pendingOrderRef = ref
                                val launched = TakeItPaymentGateway.launchChromePayment(
                                    context = context,
                                    orderRef = ref,
                                    amount = uiState.cartTotalPayable,
                                    serviceType = "FOOD",
                                    itemTitle = uiState.cartRestaurantName,
                                    customerName = uiState.profile.name.ifBlank { uiState.displayName },
                                    customerPhone = uiState.profile.phone,
                                    userId = uiState.uid
                                )
                                if (!launched) {
                                    pendingOrderRef = null
                                    onShowMessage("Could not open browser for Razorpay Checkout.")
                                }
                            }
                            "WALLET" -> onPlaceOrder("WALLET", "PAID", "WALLET_${System.currentTimeMillis()}", onOrderCreated)
                            else -> onPlaceOrder("COD", "PENDING", "COD_${System.currentTimeMillis()}", onOrderCreated)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TakeItRed, contentColor = Color.White),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("proceed_to_pay_button")
                ) {
                    Text(
                        when (selectedMethod) {
                            "RAZORPAY" -> "Proceed to Pay ₹${uiState.cartTotalPayable} →"
                            "WALLET" -> "Pay ₹${uiState.cartTotalPayable} via Wallet →"
                            else -> "Place COD Order ₹${uiState.cartTotalPayable} →"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.navigationBarsPadding())
            }
        }
    }

    if (showCouponSheet) {
        AlertDialog(
            onDismissRequest = { showCouponSheet = false },
            title = { Text("Live Take It Coupons", fontWeight = FontWeight.ExtraBold) },
            text = {
                val activeCoupons = uiState.coupons.filter { it.activeFlag }
                if (activeCoupons.isEmpty()) {
                    Text("No active coupons right now.", color = TakeItMuted)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        activeCoupons.forEach { c ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = TakeItBg,
                                border = BorderStroke(1.dp, TakeItBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onApplyCoupon(c)
                                        showCouponSheet = false
                                    }
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(c.code, fontWeight = FontWeight.ExtraBold, color = TakeItRed)
                                        Text("APPLY", fontWeight = FontWeight.ExtraBold, color = TakeItGreen)
                                    }
                                    Text(c.title, fontWeight = FontWeight.Bold)
                                    Text("${c.description} (Min ₹${c.minOrderAmount})", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCouponSheet = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun BillRow(label: String, value: String, valueColor: Color = TakeItInk) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = TakeItMuted)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = valueColor)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackOrderScreen(
    orderId: String,
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onCancelOrder: (String) -> Unit,
    onOrderDelivered: (String) -> Unit
) {
    val context = LocalContext.current
    val order = uiState.myOrders.firstOrNull { it.id == orderId }

    LaunchedEffect(order?.status) {
        if (order?.status == "DELIVERED") {
            onOrderDelivered(order.id)
        }
    }

    if (order == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Loading live order...", fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Back") }
            }
        }
        return
    }

    val progressTarget = when (order.status) {
        "PLACED" -> 0.15f
        "CONFIRMED" -> 0.35f
        "PREPARING" -> 0.55f
        "OUT_FOR_DELIVERY" -> 0.82f
        "DELIVERED" -> 1.0f
        else -> 0.05f
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(700),
        label = "deliveryRouteProgress"
    )

    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("Track Order • ${order.orderNumber}", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
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
            // Live Route Map Canvas (Synced with real-time Firestore order status!)
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color(0xFFEFF6FF),
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            // Subtle city blocks grid
                            for (i in 1..6) {
                                drawLine(Color.White.copy(alpha = 0.7f), Offset(w * i / 7f, 0f), Offset(w * i / 7f, h), strokeWidth = 10f)
                                drawLine(Color.White.copy(alpha = 0.7f), Offset(0f, h * i / 7f), Offset(w, h * i / 7f), strokeWidth = 10f)
                            }
                            val start = Offset(w * 0.18f, h * 0.28f)
                            val mid = Offset(w * 0.52f, h * 0.56f)
                            val end = Offset(w * 0.84f, h * 0.74f)

                            // Dashed full route
                            drawLine(
                                color = Color(0xFF93C5FD),
                                start = start,
                                end = mid,
                                strokeWidth = 12f,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                            )
                            drawLine(
                                color = Color(0xFF93C5FD),
                                start = mid,
                                end = end,
                                strokeWidth = 12f,
                                cap = StrokeCap.Round,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                            )

                            // Active completed segment
                            val riderPos = if (animatedProgress < 0.5f) {
                                val t = animatedProgress * 2f
                                Offset(start.x + (mid.x - start.x) * t, start.y + (mid.y - start.y) * t)
                            } else {
                                val t = (animatedProgress - 0.5f) * 2f
                                Offset(mid.x + (end.x - mid.x) * t, mid.y + (end.y - mid.y) * t)
                            }
                            drawLine(Color(0xFF2563EB), start, if (animatedProgress < 0.5f) riderPos else mid, strokeWidth = 12f, cap = StrokeCap.Round)
                            if (animatedProgress >= 0.5f) {
                                drawLine(Color(0xFF2563EB), mid, riderPos, strokeWidth = 12f, cap = StrokeCap.Round)
                            }

                            // Restaurant Pin (Red)
                            drawCircle(TakeItRed, radius = 20f, center = start)
                            drawCircle(Color.White, radius = 9f, center = start)

                            // Home Pin (Green)
                            drawCircle(TakeItGreen, radius = 20f, center = end)
                            drawCircle(Color.White, radius = 9f, center = end)

                            // Live Rider Marker (Gold + Orange)
                            drawCircle(TakeItOrange.copy(alpha = 0.25f), radius = 32f, center = riderPos)
                            drawCircle(TakeItOrange, radius = 16f, center = riderPos)
                            drawCircle(Color.White, radius = 6f, center = riderPos)
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = Color.White,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                        ) {
                            Text(
                                "🔴 ${order.restaurantName}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = TakeItGreen,
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp)
                        ) {
                            Text(
                                "OTP: ${order.otp}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }

            // ETA & Status Card matching FoodieX reference!
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = Color.White, shadowElevation = 4.dp) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    when (order.status) {
                                        "PLACED" -> "Order received by Take It"
                                        "CONFIRMED" -> "Restaurant confirmed your order"
                                        "PREPARING" -> "Chef is preparing your fresh meal"
                                        "OUT_FOR_DELIVERY" -> "Your order is on the way!"
                                        "CANCELLED" -> "Order Cancelled"
                                        "REFUNDED" -> "Order Refunded to Wallet"
                                        else -> "Order Delivered!"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TakeItInk
                                )
                                if (order.status !in listOf("CANCELLED", "REFUNDED", "DELIVERED")) {
                                    Text(
                                        "Arriving in ${order.etaMinutes} min",
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = TakeItGreen,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                            Icon(Icons.Rounded.DeliveryDining, null, tint = TakeItRed, modifier = Modifier.size(44.dp))
                        }

                        // Rider Info Row
                        Surface(shape = RoundedCornerShape(16.dp), color = TakeItBg) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        "Rider: ${order.riderName.ifBlank { "Assigning Rider..." }}",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TakeItInk
                                    )
                                    Text(
                                        listOf(order.riderVehicle, order.riderPhone).filter { it.isNotBlank() }.joinToString(" • ").ifBlank { "Live assignment from Take It Admin" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TakeItMuted
                                    )
                                }
                                if (order.riderPhone.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.riderPhone}"))
                                            runCatching { context.startActivity(dial) }
                                        }
                                    ) {
                                        Icon(Icons.Rounded.Call, "Call Rider", tint = TakeItGreen)
                                    }
                                }
                            }
                        }

                        // 4-Step Live Status Stepper
                        val steps = listOf(
                            "Order\nPlaced" to (order.status in listOf("PLACED", "CONFIRMED", "PREPARING", "OUT_FOR_DELIVERY", "DELIVERED")),
                            "Preparing" to (order.status in listOf("PREPARING", "OUT_FOR_DELIVERY", "DELIVERED")),
                            "Out for\nDelivery" to (order.status in listOf("OUT_FOR_DELIVERY", "DELIVERED")),
                            "Delivered" to (order.status == "DELIVERED")
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            steps.forEach { (label, done) ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(if (done) TakeItGreen else TakeItBorder),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Check, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (done) TakeItInk else TakeItMuted,
                                        fontWeight = if (done) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Order Summary Card
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Order Details", fontWeight = FontWeight.ExtraBold)
                        Text(order.itemsSummary, style = MaterialTheme.typography.bodyMedium, color = TakeItInk)
                        Text("Address: ${order.deliveryAddress}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                        Text("Total: ₹${order.totalAmount} • ${order.paymentMethod} (${order.paymentStatus})", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = TakeItRed)
                        if (order.status == "PLACED") {
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = { onCancelOrder(order.id) },
                                border = BorderStroke(1.dp, TakeItRed),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Cancel Order", color = TakeItRed, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderDeliveredCelebrationScreen(
    orderId: String,
    uiState: TakeItUiState,
    onSubmitReview: (orderId: String, restaurantId: String, rating: Int, comment: String) -> Unit,
    onGoHome: () -> Unit
) {
    val order = uiState.myOrders.firstOrNull { it.id == orderId }
    var rating by remember { mutableIntStateOf(5) }
    var comment by remember { mutableStateOf("") }
    var submitted by remember { mutableStateOf((order?.rating ?: 0) > 0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFECFDF5), Color.White, TakeItBg)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(Modifier.height(12.dp))

        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                modifier = Modifier
                    .size(92.dp)
                    .clip(CircleShape)
                    .background(TakeItGreen),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, "Delivered", tint = Color.White, modifier = Modifier.size(54.dp))
            }
            Text("Order Delivered!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TakeItInk)
            Text("Enjoy your delicious meal from ${order?.restaurantName ?: "Take It"} 🎉", color = TakeItMuted, textAlign = TextAlign.Center)

            Image(
                painter = painterResource(resolveFoodDrawable(order?.restaurantName ?: "pizza")),
                contentDescription = "Delivered Meal",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(150.dp)
                    .clip(CircleShape)
            )

            if (!submitted && order != null) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Rate Your Meal", fontWeight = FontWeight.ExtraBold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { star ->
                                IconButton(onClick = { rating = star }) {
                                    Icon(
                                        Icons.Rounded.Star,
                                        contentDescription = "$star star",
                                        tint = if (star <= rating) TakeItGold else TakeItBorder,
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            placeholder = { Text("Write a quick review...") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp)
                        )
                        Button(
                            onClick = {
                                onSubmitReview(order.id, order.restaurantId, rating, comment)
                                submitted = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = TakeItGold, contentColor = TakeItInk),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Submit Rating ★", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            } else {
                Surface(shape = RoundedCornerShape(50), color = TakeItGreenSoft) {
                    Text(
                        "Thank you for rating ${order?.rating ?: rating}★!",
                        color = TakeItGreen,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }
            }
        }

        Button(
            onClick = onGoHome,
            colors = ButtonDefaults.buttonColors(containerColor = TakeItRed, contentColor = Color.White),
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
        ) {
            Text("Back to Home", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}
