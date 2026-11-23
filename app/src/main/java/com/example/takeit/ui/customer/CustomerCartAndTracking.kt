package com.example.takeit.ui.customer

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
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
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
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

    LaunchedEffect(Unit) {
        TakeItPaymentBus.results.collect { res ->
            if (res.orderRef == pendingOrderRef && res.status.equals("success", ignoreCase = true)) {
                pendingOrderRef = null
                isVerifyingPayment = false
                onPlaceOrder("RAZORPAY", "PAID", res.paymentId, onOrderCreated)
            }
        }
    }

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

    val triggerPayment: () -> Unit = {
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
                    shadowElevation = 18.dp,
                    border = BorderStroke(1.dp, TakeItBorder),
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
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
                        TakeIt3DButton(
                            onClick = triggerPayment,
                            containerColor = TakeItRed,
                            depthColor = TakeItRedDark.darken3D(0.4f),
                            contentColor = Color.White,
                            cornerRadius = 16.dp,
                            depth = 5.dp,
                            height = 52.dp,
                            modifier = Modifier
                                .widthIn(min = 165.dp)
                                .testTag("sticky_pay_now_button")
                        ) {
                            Icon(Icons.Rounded.Lock, null, modifier = Modifier.size(17.dp))
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
        TakeIt3DBackdrop(TakeItRed, TakeItGold, baseColor = TakeItBg) {
            if (uiState.cart.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 26.dp,
                        depth = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.ShoppingBag,
                                containerColor = TakeItRed,
                                iconTint = Color.White,
                                size = 72.dp
                            )
                            Text("Your food cart is empty", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                            Text("Add delicious dishes from top restaurants to start ordering!", color = TakeItMuted, textAlign = TextAlign.Center)
                            TakeIt3DButton(
                                onClick = onBack,
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                height = 50.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Explore Restaurants", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // 3D Restaurant Header Card
                    item {
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFD5DCE6),
                            cornerRadius = 20.dp,
                            depth = 4.dp
                        ) {
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
                                        .size(48.dp)
                                        .shadow(4.dp, CircleShape)
                                        .clip(CircleShape)
                                        .border(1.5.dp, TakeItGold, CircleShape)
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

                    // 3D Cart Items List
                    item {
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFD5DCE6),
                            cornerRadius = 20.dp,
                            depth = 5.dp
                        ) {
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
                                                .size(56.dp)
                                                .shadow(4.dp, RoundedCornerShape(12.dp))
                                                .clip(RoundedCornerShape(12.dp))
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                VegMarkIcon(item.menuItem.vegFlag)
                                                Text(item.menuItem.name, fontWeight = FontWeight.Bold, color = TakeItInk)
                                            }
                                            Text("₹${item.menuItem.price} each", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                                        }
                                        Surface(shape = RoundedCornerShape(12.dp), color = TakeItBg, border = BorderStroke(1.dp, TakeItBorder)) {
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

                    // 3D Apply Coupon Card
                    item {
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFD5DCE6),
                            cornerRadius = 20.dp,
                            depth = 4.dp,
                            onClick = { showCouponSheet = true },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("apply_coupon_row")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    TakeIt3DIconOrb(
                                        icon = Icons.Rounded.LocalOffer,
                                        containerColor = TakeItGreen,
                                        size = 38.dp
                                    )
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

                    // 3D Payment Method Selection Card
                    item {
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFD5DCE6),
                            cornerRadius = 20.dp,
                            depth = 5.dp
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Choose Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                                val methods = listOf(
                                    Triple("RAZORPAY", "UPI / Cards / NetBanking (Razorpay Chrome)", "Instant verified online payment via jaiambey.shop"),
                                    Triple("WALLET", "Take It Wallet (Balance: ₹${uiState.profile.walletBalance.toInt()})", "Pay instantly with zero checkout wait"),
                                    Triple("COD", "Cash on Delivery", "Pay cash or UPI to delivery partner at doorstep")
                                )
                                methods.forEach { (code, title, sub) ->
                                    val selected = selectedMethod == code
                                    TakeIt3DCard(
                                        containerColor = if (selected) Color(0xFFFFF1F2) else TakeItBg,
                                        depthColor = if (selected) TakeItRed.darken3D(0.25f) else Color(0xFFD5DCE6),
                                        borderColor = if (selected) TakeItRed else TakeItBorder,
                                        cornerRadius = 15.dp,
                                        depth = if (selected) 4.dp else 2.dp,
                                        onClick = { selectedMethod = code },
                                        modifier = Modifier.fillMaxWidth()
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

                    // 3D Bill Breakdown with Golden 3D Total Payable Slab!
                    item {
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFD5DCE6),
                            cornerRadius = 20.dp,
                            depth = 5.dp
                        ) {
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
                                TakeIt3DCard(
                                    containerColor = TakeItGold,
                                    depthColor = TakeItGold.darken3D(0.35f),
                                    borderColor = TakeItGold.lighten3D(0.4f),
                                    cornerRadius = 16.dp,
                                    depth = 4.dp
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
                            TakeIt3DCard(
                                containerColor = TakeItGoldSoft,
                                depthColor = TakeItGold.darken3D(0.3f),
                                borderColor = TakeItGold,
                                cornerRadius = 18.dp,
                                depth = 4.dp
                            ) {
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

                    // 3D Proceed to Pay Button
                    item {
                        TakeIt3DButton(
                            onClick = triggerPayment,
                            containerColor = TakeItRed,
                            depthColor = TakeItRedDark.darken3D(0.4f),
                            contentColor = Color.White,
                            cornerRadius = 24.dp,
                            depth = 5.dp,
                            height = 54.dp,
                            modifier = Modifier
                                .fillMaxWidth()
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
    val inf = rememberInfiniteTransition(label = "trackBeacon")
    val beaconPulse by inf.animateFloat(
        initialValue = 26f,
        targetValue = 40f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beaconPulse"
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
        TakeIt3DBackdrop(TakeItGreen, Color(0xFF2563EB), baseColor = TakeItBg) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 3D Live Route Map Canvas
                item {
                    TakeIt3DCard(
                        containerColor = Color(0xFFE8F1FF),
                        depthColor = Color(0xFF93C5FD),
                        borderColor = Color.White,
                        cornerRadius = 24.dp,
                        depth = 6.dp,
                        shadowElevation = 12.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(216.dp)
                        ) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val w = size.width
                                val h = size.height
                                for (i in 1..6) {
                                    drawLine(Color.White.copy(alpha = 0.75f), Offset(w * i / 7f, 0f), Offset(w * i / 7f, h), strokeWidth = 10f)
                                    drawLine(Color.White.copy(alpha = 0.75f), Offset(0f, h * i / 7f), Offset(w, h * i / 7f), strokeWidth = 10f)
                                }
                                val start = Offset(w * 0.18f, h * 0.28f)
                                val mid = Offset(w * 0.52f, h * 0.56f)
                                val end = Offset(w * 0.84f, h * 0.74f)

                                drawLine(
                                    color = Color(0xFF93C5FD),
                                    start = start,
                                    end = mid,
                                    strokeWidth = 14f,
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                                )
                                drawLine(
                                    color = Color(0xFF93C5FD),
                                    start = mid,
                                    end = end,
                                    strokeWidth = 14f,
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f))
                                )

                                val riderPos = if (animatedProgress < 0.5f) {
                                    val t = animatedProgress * 2f
                                    Offset(start.x + (mid.x - start.x) * t, start.y + (mid.y - start.y) * t)
                                } else {
                                    val t = (animatedProgress - 0.5f) * 2f
                                    Offset(mid.x + (end.x - mid.x) * t, mid.y + (end.y - mid.y) * t)
                                }
                                drawLine(Color(0xFF1D4ED8), start, if (animatedProgress < 0.5f) riderPos else mid, strokeWidth = 14f, cap = StrokeCap.Round)
                                if (animatedProgress >= 0.5f) {
                                    drawLine(Color(0xFF1D4ED8), mid, riderPos, strokeWidth = 14f, cap = StrokeCap.Round)
                                }

                                // 3D Pin Shadows
                                drawCircle(Color.Black.copy(alpha = 0.2f), radius = 20f, center = Offset(start.x, start.y + 8f))
                                drawCircle(TakeItRed, radius = 22f, center = start)
                                drawCircle(Color.White, radius = 9f, center = start)

                                drawCircle(Color.Black.copy(alpha = 0.2f), radius = 20f, center = Offset(end.x, end.y + 8f))
                                drawCircle(TakeItGreen, radius = 22f, center = end)
                                drawCircle(Color.White, radius = 9f, center = end)

                                // Pulsing 3D Rider Orb
                                drawCircle(TakeItOrange.copy(alpha = 0.28f), radius = beaconPulse, center = riderPos)
                                drawCircle(Color.Black.copy(alpha = 0.22f), radius = 17f, center = Offset(riderPos.x, riderPos.y + 6f))
                                drawCircle(TakeItOrange, radius = 18f, center = riderPos)
                                drawCircle(Color.White, radius = 7f, center = riderPos)
                            }

                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.White,
                                shadowElevation = 4.dp,
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
                                shadowElevation = 4.dp,
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

                // 3D ETA & 4-Step Status Card
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 24.dp,
                        depth = 5.dp
                    ) {
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
                                TakeIt3DIconOrb(
                                    icon = Icons.Rounded.DeliveryDining,
                                    containerColor = TakeItRed,
                                    size = 50.dp
                                )
                            }

                            Surface(shape = RoundedCornerShape(16.dp), color = TakeItBg, border = BorderStroke(1.dp, TakeItBorder)) {
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
                                        TakeIt3DIconOrb(
                                            icon = Icons.Rounded.Call,
                                            containerColor = TakeItGreen,
                                            size = 42.dp,
                                            contentDescription = "Call Rider",
                                            onClick = {
                                                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order.riderPhone}"))
                                                runCatching { context.startActivity(dial) }
                                            }
                                        )
                                    }
                                }
                            }

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
                                        TakeIt3DIconOrb(
                                            icon = Icons.Rounded.Check,
                                            containerColor = if (done) TakeItGreen else TakeItBorder,
                                            depthColor = if (done) TakeItGreen.darken3D(0.35f) else Color(0xFFCBD5E1),
                                            iconTint = if (done) Color.White else TakeItMuted,
                                            size = 34.dp
                                        )
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

                // 3D Order Details Card
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 4.dp
                    ) {
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

    val inf = rememberInfiniteTransition(label = "celeb3D")
    val orbFloat by inf.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "celebOrb"
    )

    TakeIt3DBackdrop(
        accentPrimary = TakeItGreen,
        accentSecondary = TakeItGold,
        baseColor = Color(0xFFF0FDF4)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(12.dp))

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.graphicsLayer { translationY = orbFloat }) {
                    TakeIt3DIconOrb(
                        icon = Icons.Rounded.Check,
                        containerColor = TakeItGreen,
                        depthColor = TakeItGreen.darken3D(0.4f),
                        size = 92.dp
                    )
                }
                Text("Order Delivered!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = TakeItInk)
                Text("Enjoy your delicious meal from ${order?.restaurantName ?: "Take It"} 🎉", color = TakeItMuted, textAlign = TextAlign.Center)

                Box(
                    modifier = Modifier
                        .size(146.dp)
                        .shadow(14.dp, CircleShape)
                        .clip(CircleShape)
                        .border(4.dp, Color.White, CircleShape)
                ) {
                    Image(
                        painter = painterResource(resolveFoodDrawable(order?.restaurantName ?: "pizza")),
                        contentDescription = "Delivered Meal",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (!submitted && order != null) {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
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
                            TakeIt3DButton(
                                onClick = {
                                    onSubmitReview(order.id, order.restaurantId, rating, comment)
                                    submitted = true
                                },
                                containerColor = TakeItGold,
                                depthColor = TakeItGold.darken3D(0.35f),
                                contentColor = TakeItInk,
                                cornerRadius = 24.dp,
                                depth = 4.dp,
                                height = 48.dp,
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

            TakeIt3DButton(
                onClick = onGoHome,
                containerColor = TakeItRed,
                depthColor = TakeItRedDark.darken3D(0.4f),
                contentColor = Color.White,
                cornerRadius = 24.dp,
                depth = 5.dp,
                height = 54.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Back to Home", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}
