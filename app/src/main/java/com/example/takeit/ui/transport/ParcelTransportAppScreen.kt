package com.example.takeit.ui.transport

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.network.TakeItPaymentBus
import com.example.network.TakeItPaymentGateway
import com.example.takeit.model.ParcelOrder
import com.example.takeit.model.TransportTab
import com.example.takeit.theme.*
import com.example.takeit.ui.components.Nav3DTabItem
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DFloatingNavBar
import com.example.takeit.ui.components.TakeIt3DHeroBanner
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.customer.CustomerWalletScreen
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel
import kotlinx.coroutines.launch

private data class TransportVehicleOption(
    val code: String,
    val title: String,
    val capacity: String,
    val baseFare: Int,
    val perKg: Int,
    val icon: ImageVector
) {
    fun fareFor(weightKg: Int, fragile: Boolean): Int {
        val base = baseFare + (weightKg.coerceAtLeast(1) * perKg)
        return if (fragile) base + 30 else base
    }
}

private val transportVehicles = listOf(
    TransportVehicleOption("TWO_WHEELER", "2-Wheeler Express", "Up to 20 kg • Documents & Boxes", 49, 8, Icons.Rounded.DeliveryDining),
    TransportVehicleOption("THREE_WHEELER", "3-Wheeler Ape Cargo", "Up to 500 kg • Shop & Wholesale", 199, 6, Icons.Rounded.ElectricRickshaw),
    TransportVehicleOption("MINI_TRUCK", "Tata Ace Mini Truck", "Up to 750 kg • Appliances & Stock", 349, 5, Icons.Rounded.LocalShipping),
    TransportVehicleOption("PICKUP_8FT", "8ft Heavy Pickup", "Up to 1500 kg • House / Office Move", 599, 4, Icons.Rounded.FireTruck)
)

@Composable
fun ParcelTransportAppRoot(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var pickup by remember(uiState.profile.activeAddressLine) {
        mutableStateOf(uiState.profile.activeAddressLine.ifBlank { "Sector 62 Warehouse, Noida" })
    }
    var drop by remember { mutableStateOf("Alpha 1 Commercial Belt, Greater Noida") }
    var receiverName by remember { mutableStateOf("") }
    var receiverPhone by remember { mutableStateOf("") }
    var packageType by remember { mutableStateOf("Box / Documents") }
    var weightKg by remember { mutableIntStateOf(5) }
    var fragile by remember { mutableStateOf(false) }
    var selectedVehicleCode by remember { mutableStateOf("TWO_WHEELER") }
    var paymentMethod by remember { mutableStateOf("RAZORPAY") }
    var pendingPayRef by remember { mutableStateOf<String?>(null) }
    var pendingDraftFare by remember { mutableIntStateOf(0) }

    val selectedVehicle = remember(selectedVehicleCode) {
        transportVehicles.firstOrNull { it.code == selectedVehicleCode } ?: transportVehicles.first()
    }
    val calculatedFare = remember(selectedVehicle, weightKg, fragile) {
        selectedVehicle.fareFor(weightKg, fragile)
    }

    fun finalizeRazorpayParcel(paymentId: String, amount: Int) {
        val finalFare = if (amount > 0) amount else if (pendingDraftFare > 0) pendingDraftFare else calculatedFare
        pendingPayRef = null
        pendingDraftFare = 0
        viewModel.bookParcel(
            vehicleType = selectedVehicle.code,
            pickup = pickup,
            drop = drop,
            receiverName = receiverName.ifBlank { "Receiver" },
            receiverPhone = receiverPhone.ifBlank { uiState.profile.phone },
            packageType = packageType,
            weightKg = weightKg,
            fragile = fragile,
            fare = finalFare,
            paymentMethod = "RAZORPAY",
            paymentStatus = "PAID",
            paymentRef = paymentId
        )
    }

    LaunchedEffect(Unit) {
        TakeItPaymentBus.results.collect { res ->
            if (res.orderRef == pendingPayRef && res.status.equals("success", ignoreCase = true)) {
                finalizeRazorpayParcel(res.paymentId, res.amount)
            }
        }
    }

    DisposableEffect(lifecycleOwner, pendingPayRef) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingPayRef != null) {
                val ref = pendingPayRef ?: return@LifecycleEventObserver
                scope.launch {
                    val res = TakeItPaymentGateway.checkPaymentStatus(ref)
                    if (res != null) {
                        finalizeRazorpayParcel(res.paymentId, res.amount)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val startBooking: () -> Unit = {
        when {
            pickup.isBlank() || drop.isBlank() -> viewModel.showMessage("Enter pickup and drop addresses")
            receiverName.isBlank() || receiverPhone.isBlank() -> viewModel.showMessage("Enter receiver name and phone number")
            paymentMethod == "RAZORPAY" -> {
                val ref = "TKTP_${System.currentTimeMillis()}"
                pendingPayRef = ref
                pendingDraftFare = calculatedFare
                val launched = TakeItPaymentGateway.launchChromePayment(
                    context = context,
                    orderRef = ref,
                    amount = calculatedFare,
                    serviceType = "PARCEL_TRANSPORT",
                    itemTitle = "${selectedVehicle.title} (${weightKg}kg)",
                    customerName = uiState.profile.name.ifBlank { uiState.displayName },
                    customerPhone = receiverPhone,
                    userId = uiState.uid
                )
                if (!launched) {
                    pendingPayRef = null
                    viewModel.showMessage("Could not open browser for Razorpay checkout")
                }
            }
            paymentMethod == "WALLET" -> {
                viewModel.bookParcel(
                    vehicleType = selectedVehicle.code,
                    pickup = pickup,
                    drop = drop,
                    receiverName = receiverName,
                    receiverPhone = receiverPhone,
                    packageType = packageType,
                    weightKg = weightKg,
                    fragile = fragile,
                    fare = calculatedFare,
                    paymentMethod = "WALLET",
                    paymentStatus = "PAID",
                    paymentRef = "WALLET"
                )
            }
            else -> {
                viewModel.bookParcel(
                    vehicleType = selectedVehicle.code,
                    pickup = pickup,
                    drop = drop,
                    receiverName = receiverName,
                    receiverPhone = receiverPhone,
                    packageType = packageType,
                    weightKg = weightKg,
                    fragile = fragile,
                    fare = calculatedFare,
                    paymentMethod = "COD",
                    paymentStatus = "PAY_ON_DELIVERY",
                    paymentRef = "COD"
                )
            }
        }
    }

    val navItems = remember(uiState.transportTab) {
        TransportTab.entries.map { tab ->
            val icon = when (tab) {
                TransportTab.SEND -> Icons.Rounded.LocalShipping
                TransportTab.SHIPMENTS -> Icons.Rounded.Inventory2
                TransportTab.WALLET -> Icons.Rounded.AccountBalanceWallet
                TransportTab.ACCOUNT -> Icons.Rounded.Person
            }
            Nav3DTabItem(
                id = tab.name,
                label = tab.label,
                icon = icon,
                selected = uiState.transportTab == tab,
                testTag = "transport_nav_${tab.name.lowercase()}",
                onClick = { viewModel.selectTransportTab(tab) }
            )
        }
    }

    Scaffold(
        containerColor = TakeItBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (uiState.transportTab == TransportTab.SEND) {
                    Surface(
                        color = Color.White,
                        shadowElevation = 18.dp,
                        border = BorderStroke(1.dp, TakeItBorder),
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${selectedVehicle.title} • ${weightKg}kg",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TakeItMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "₹$calculatedFare • $paymentMethod",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TransportNavy
                                )
                            }
                            TakeIt3DButton(
                                onClick = startBooking,
                                containerColor = TransportOrange,
                                depthColor = TransportOrange.darken3D(0.42f),
                                contentColor = Color.White,
                                cornerRadius = 16.dp,
                                depth = 5.dp,
                                height = 50.dp,
                                modifier = Modifier
                                    .widthIn(min = 148.dp)
                                    .testTag("transport_sticky_pay_button")
                            ) {
                                Text(
                                    if (paymentMethod == "COD") "Book ₹$calculatedFare" else "Pay ₹$calculatedFare",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Spacer(Modifier.width(6.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                TakeIt3DFloatingNavBar(
                    items = navItems,
                    containerColor = Color.White,
                    activePodColor = TransportOrange,
                    activeContentColor = Color.White,
                    inactiveColor = TakeItMuted,
                    borderColor = TakeItBorder
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = innerPadding.calculateBottomPadding())
        ) {
            when (uiState.transportTab) {
                TransportTab.SEND -> TransportSendTab(
                    uiState = uiState,
                    pickup = pickup,
                    drop = drop,
                    receiverName = receiverName,
                    receiverPhone = receiverPhone,
                    packageType = packageType,
                    weightKg = weightKg,
                    fragile = fragile,
                    selectedVehicle = selectedVehicle,
                    calculatedFare = calculatedFare,
                    paymentMethod = paymentMethod,
                    pendingPayRef = pendingPayRef,
                    onPickupChange = { pickup = it },
                    onDropChange = { drop = it },
                    onReceiverNameChange = { receiverName = it },
                    onReceiverPhoneChange = { receiverPhone = it },
                    onPackageTypeChange = { packageType = it },
                    onWeightChange = { weightKg = it.coerceIn(1, 1500) },
                    onFragileChange = { fragile = it },
                    onSelectVehicle = { selectedVehicleCode = it },
                    onSelectPayment = { paymentMethod = it },
                    onVerifyPendingPayment = {
                        val ref = pendingPayRef ?: return@TransportSendTab
                        scope.launch {
                            val res = TakeItPaymentGateway.checkPaymentStatus(ref)
                            if (res != null) {
                                finalizeRazorpayParcel(res.paymentId, res.amount)
                            } else {
                                viewModel.showMessage("Payment not confirmed yet. Complete checkout in Chrome first.")
                            }
                        }
                    },
                    onCancelParcel = viewModel::cancelMyParcel
                )

                TransportTab.SHIPMENTS -> TransportShipmentsTab(
                    parcels = uiState.myParcels,
                    onRateParcel = viewModel::rateMyParcel,
                    onCancelParcel = viewModel::cancelMyParcel
                )

                TransportTab.WALLET -> CustomerWalletScreen(
                    uiState = uiState,
                    onBack = { viewModel.selectTransportTab(TransportTab.SEND) },
                    onTopUpConfirmed = viewModel::recordWalletTopUp,
                    onShowMessage = viewModel::showMessage
                )

                TransportTab.ACCOUNT -> TransportAccountTab(
                    uiState = uiState,
                    onUpdateProfile = viewModel::updateCustomerPhoneAndName,
                    onOpenWallet = { viewModel.selectTransportTab(TransportTab.WALLET) },
                    onOpenShipments = { viewModel.selectTransportTab(TransportTab.SHIPMENTS) },
                    onSignOut = onSignOut
                )
            }
        }
    }
}

@Composable
private fun TransportSendTab(
    uiState: TakeItUiState,
    pickup: String,
    drop: String,
    receiverName: String,
    receiverPhone: String,
    packageType: String,
    weightKg: Int,
    fragile: Boolean,
    selectedVehicle: TransportVehicleOption,
    calculatedFare: Int,
    paymentMethod: String,
    pendingPayRef: String?,
    onPickupChange: (String) -> Unit,
    onDropChange: (String) -> Unit,
    onReceiverNameChange: (String) -> Unit,
    onReceiverPhoneChange: (String) -> Unit,
    onPackageTypeChange: (String) -> Unit,
    onWeightChange: (Int) -> Unit,
    onFragileChange: (Boolean) -> Unit,
    onSelectVehicle: (String) -> Unit,
    onSelectPayment: (String) -> Unit,
    onVerifyPendingPayment: () -> Unit,
    onCancelParcel: (String) -> Unit
) {
    val activeParcel = remember(uiState.myParcels) {
        uiState.myParcels.firstOrNull {
            it.status in listOf("BOOKED", "PARTNER_ASSIGNED", "PICKED_UP", "IN_TRANSIT")
        }
    }

    val inf = rememberInfiniteTransition(label = "transportWelcomeAnim")
    val heroScale by inf.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "transportHeroScale"
    )
    val heroFloat by inf.animateFloat(
        initialValue = -4f,
        targetValue = 4f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "transportHeroFloat"
    )

    TakeIt3DBackdrop(
        accentPrimary = TransportOrange,
        accentSecondary = TransportNavy,
        baseColor = TakeItBg
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 3D Navy & Orange Logistics Header Deck
            item {
                TakeIt3DCard(
                    containerColor = TransportNavy,
                    depthColor = Color(0xFF060A16),
                    borderColor = TransportOrange.copy(alpha = 0.5f),
                    cornerRadius = 26.dp,
                    depth = 6.dp,
                    shadowElevation = 12.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.verticalGradient(listOf(TransportNavy, Color(0xFF1E293B))))
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .shadow(8.dp, CircleShape, ambientColor = TransportOrange, spotColor = TransportOrange)
                                        .clip(CircleShape)
                                        .background(TransportOrange)
                                        .padding(2.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.logo_takeit_transport),
                                        contentDescription = "Take It Transport Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                                Column {
                                    Text(
                                        "Take It Transport",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        "Parcel, Goods & Mini-Trucks • ${uiState.config.cityName}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TransportOrangeSoft
                                    )
                                }
                            }
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = TransportOrange.copy(alpha = 0.2f),
                                border = BorderStroke(1.dp, TransportOrange)
                            ) {
                                Text(
                                    "₹${uiState.profile.walletBalance.toInt()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 3D Perspective 3rd Image Welcome Showcase Banner
            item {
                TakeIt3DHeroBanner(
                    borderColor = TransportOrange,
                    depthColor = TransportNavy,
                    cornerRadius = 26.dp,
                    depth = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(178.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.welcome_parcel_transport),
                        contentDescription = "Take It Parcel Transport Welcome",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                scaleX = heroScale
                                scaleY = heroScale
                                translationY = heroFloat
                            }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(TransportNavy.copy(alpha = 0.84f), Color.Transparent)
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = TransportOrange, shadowElevation = 4.dp) {
                            Text(
                                "SEND SMART • DELIVER FAST",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            "Safe Parcels &\nCargo Trucks",
                            style = MaterialTheme.typography.titleLarge,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "Verified Delivery OTP • Live Tracking",
                            style = MaterialTheme.typography.labelSmall,
                            color = TransportOrangeSoft
                        )
                    }
                }
            }

            if (pendingPayRef != null) {
                item {
                    TakeIt3DCard(
                        containerColor = TransportOrangeSoft,
                        depthColor = TransportOrange.darken3D(0.35f),
                        borderColor = TransportOrange,
                        cornerRadius = 20.dp,
                        depth = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Verify Razorpay Checkout", fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                                Text("Paid in Chrome? Tap Verify to book shipment.", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                            Button(
                                onClick = onVerifyPendingPayment,
                                colors = ButtonDefaults.buttonColors(containerColor = TransportOrange),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Verify", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            if (activeParcel != null) {
                item {
                    ActiveParcelTrackingCard(
                        parcel = activeParcel,
                        onCancel = { onCancelParcel(activeParcel.id) }
                    )
                }
            }

            // 3D Pickup, Drop & Receiver Details Card
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Pickup & Receiver Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        OutlinedTextField(
                            value = pickup,
                            onValueChange = onPickupChange,
                            label = { Text("Pickup Address") },
                            leadingIcon = { Icon(Icons.Rounded.Outbox, null, tint = TransportOrange) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transport_pickup_input")
                        )
                        OutlinedTextField(
                            value = drop,
                            onValueChange = onDropChange,
                            label = { Text("Drop / Delivery Address") },
                            leadingIcon = { Icon(Icons.Rounded.MoveToInbox, null, tint = TakeItGreen) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("transport_drop_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = receiverName,
                                onValueChange = onReceiverNameChange,
                                label = { Text("Receiver Name") },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("transport_receiver_name_input")
                            )
                            OutlinedTextField(
                                value = receiverPhone,
                                onValueChange = onReceiverPhoneChange,
                                label = { Text("Receiver Phone") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("transport_receiver_phone_input")
                            )
                        }

                        Text("Package Category", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("Box / Documents", "Food / Gift", "Electronics", "Shop Stock", "Furniture")) { cat ->
                                FilterChip(
                                    selected = packageType == cat,
                                    onClick = { onPackageTypeChange(cat) },
                                    label = { Text(cat, fontWeight = FontWeight.SemiBold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TransportOrange,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Weight: $weightKg kg", fontWeight = FontWeight.Bold)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(listOf(2, 5, 15, 50, 250)) { w ->
                                    FilterChip(
                                        selected = weightKg == w,
                                        onClick = { onWeightChange(w) },
                                        label = { Text("${w}kg", fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Fragile / Extra Care (+₹30)", fontWeight = FontWeight.Bold)
                                Text("Padded handling for glass or electronics", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                            Switch(checked = fragile, onCheckedChange = onFragileChange)
                        }
                    }
                }
            }

            item {
                Text(
                    "Select Transport Vehicle",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            items(transportVehicles, key = { it.code }) { v ->
                val selected = v.code == selectedVehicle.code
                val fare = v.fareFor(weightKg, fragile)
                TakeIt3DCard(
                    onClick = { onSelectVehicle(v.code) },
                    containerColor = if (selected) TransportOrangeSoft else Color.White,
                    depthColor = if (selected) TransportOrange.darken3D(0.4f) else Color(0xFFD5DCE6),
                    borderColor = if (selected) TransportOrange else Color.White,
                    cornerRadius = 20.dp,
                    depth = if (selected) 6.dp else 4.dp,
                    shadowElevation = if (selected) 10.dp else 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("transport_vehicle_${v.code.lowercase()}")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        TakeIt3DIconOrb(
                            icon = v.icon,
                            containerColor = if (selected) TransportOrange else TransportNavy,
                            iconTint = Color.White,
                            size = 50.dp,
                            shape = RoundedCornerShape(15.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.title, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                            Text(v.capacity, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                            Text("₹${v.baseFare} base + ₹${v.perKg}/kg", style = MaterialTheme.typography.labelSmall, color = TransportOrange)
                        }
                        Text("₹$fare", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = TransportNavy)
                    }
                }
            }

            // 3D Payment Method Card
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Payment Method", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("RAZORPAY" to "Razorpay Online", "WALLET" to "Wallet (₹${uiState.profile.walletBalance.toInt()})", "COD" to "Pay on Delivery").forEach { (key, label) ->
                                FilterChip(
                                    selected = paymentMethod == key,
                                    onClick = { onSelectPayment(key) },
                                    label = { Text(label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = TransportNavy,
                                        selectedLabelColor = Color.White
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                        Text(
                            "Total fare: ₹$calculatedFare • Tap the orange 3D Pay/Book bar below to confirm shipment.",
                            style = MaterialTheme.typography.labelSmall,
                            color = TakeItMuted
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveParcelTrackingCard(
    parcel: ParcelOrder,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    TakeIt3DCard(
        containerColor = TransportNavy,
        depthColor = Color(0xFF060A16),
        borderColor = TransportOrange,
        cornerRadius = 22.dp,
        depth = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ACTIVE SHIPMENT • ${parcel.parcelNumber}", color = TransportOrange, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                    Text(parcel.status.replace("_", " "), style = MaterialTheme.typography.titleMedium, color = Color.White, fontWeight = FontWeight.ExtraBold)
                }
                Surface(shape = RoundedCornerShape(10.dp), color = TransportOrange, shadowElevation = 4.dp) {
                    Text("OTP ${parcel.otp}", color = Color.White, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
                }
            }
            Text("${parcel.pickupAddress} -> ${parcel.dropAddress}", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f))
            Text("Receiver: ${parcel.receiverName} (${parcel.receiverPhone}) • ${parcel.weightKg}kg ${parcel.packageType}", style = MaterialTheme.typography.labelSmall, color = TransportOrangeSoft)
            Surface(shape = RoundedCornerShape(12.dp), color = Color.White.copy(alpha = 0.08f)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(parcel.partnerName, color = Color.White, fontWeight = FontWeight.Bold)
                        Text(
                            if (parcel.vehicleNumber.isNotBlank()) "${parcel.vehicleType} • ${parcel.vehicleNumber} • ETA ${parcel.etaMinutes} min"
                            else "${parcel.vehicleType} • ₹${parcel.totalFare} (${parcel.paymentMethod})",
                            style = MaterialTheme.typography.labelSmall,
                            color = TransportOrange
                        )
                    }
                    if (parcel.partnerPhone.isNotBlank()) {
                        TakeIt3DIconOrb(
                            icon = Icons.Rounded.Call,
                            containerColor = TransportOrange,
                            iconTint = Color.White,
                            size = 40.dp,
                            contentDescription = "Call Partner",
                            onClick = {
                                val dial = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${parcel.partnerPhone}"))
                                context.startActivity(dial)
                            }
                        )
                    }
                }
            }
            if (parcel.status == "BOOKED") {
                OutlinedButton(
                    onClick = onCancel,
                    border = BorderStroke(1.dp, AdminCrimson),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel Shipment", color = AdminCrimson, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TransportShipmentsTab(
    parcels: List<ParcelOrder>,
    onRateParcel: (String, Int) -> Unit,
    onCancelParcel: (String) -> Unit
) {
    TakeIt3DBackdrop(TransportOrange, TransportNavy, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TakeIt3DCard(
                    containerColor = TransportNavy,
                    depthColor = Color(0xFF060A16),
                    borderColor = TransportOrange.copy(alpha = 0.5f),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Shipments", style = MaterialTheme.typography.headlineSmall, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Surface(shape = RoundedCornerShape(50), color = TransportOrange) {
                            Text(
                                "${parcels.size} Total",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
            if (parcels.isEmpty()) {
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 24.dp,
                        depth = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.LocalShipping,
                                containerColor = TransportOrange,
                                size = 64.dp
                            )
                            Text("No shipments sent yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Send parcels or book mini-trucks safely from the Send Parcel tab!", color = TakeItMuted)
                        }
                    }
                }
            } else {
                items(parcels, key = { it.id }) { parcel ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${parcel.parcelNumber} • ${parcel.vehicleType}", fontWeight = FontWeight.ExtraBold)
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (parcel.status == "DELIVERED") TakeItGreenSoft else TransportOrangeSoft
                                ) {
                                    Text(
                                        parcel.status.replace("_", " "),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (parcel.status == "DELIVERED") TakeItGreen else TransportOrange,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Text("${parcel.pickupAddress} -> ${parcel.dropAddress}", style = MaterialTheme.typography.bodySmall, color = TakeItInk)
                            Text("To: ${parcel.receiverName} (${parcel.receiverPhone}) • ${parcel.weightKg}kg", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("OTP: ${parcel.otp} • Partner: ${parcel.partnerName}", style = MaterialTheme.typography.labelSmall, color = TransportNavy)
                                Text("₹${parcel.totalFare} (${parcel.paymentMethod})", fontWeight = FontWeight.ExtraBold, color = TransportOrange)
                            }
                            if (parcel.status == "BOOKED") {
                                OutlinedButton(onClick = { onCancelParcel(parcel.id) }, modifier = Modifier.fillMaxWidth()) {
                                    Text("Cancel Shipment", color = TakeItRed)
                                }
                            }
                            if (parcel.status == "DELIVERED" && parcel.rating == 0) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text("Rate Partner:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                                    (1..5).forEach { stars ->
                                        IconButton(onClick = { onRateParcel(parcel.id, stars) }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Rounded.Star, "Rate $stars", tint = TakeItGold)
                                        }
                                    }
                                }
                            } else if (parcel.rating > 0) {
                                Text("Rated ${parcel.rating} ★", color = TakeItGreen, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportAccountTab(
    uiState: TakeItUiState,
    onUpdateProfile: (String, String) -> Unit,
    onOpenWallet: () -> Unit,
    onOpenShipments: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEdit by remember { mutableStateOf(false) }

    TakeIt3DBackdrop(TransportOrange, TransportNavy, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TakeIt3DCard(
                    containerColor = TransportNavy,
                    depthColor = Color(0xFF060A16),
                    borderColor = TransportOrange,
                    cornerRadius = 26.dp,
                    depth = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(TransportNavy, Color(0xFF1E293B))))
                            .statusBarsPadding()
                            .padding(20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(66.dp)
                                    .shadow(8.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(TransportOrange)
                                    .border(2.dp, Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    uiState.profile.name.ifBlank { uiState.displayName }.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(uiState.profile.name.ifBlank { uiState.displayName }, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                                Text(uiState.email, style = MaterialTheme.typography.bodySmall, color = TransportOrangeSoft)
                                Text(uiState.profile.phone.ifBlank { "Tap edit to add phone" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                            }
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.Edit,
                                containerColor = TransportOrange,
                                iconTint = Color.White,
                                size = 40.dp,
                                contentDescription = "Edit Profile",
                                onClick = { showEdit = true }
                            )
                        }
                    }
                }
            }

            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column {
                        ListItem(
                            headlineContent = { Text("My Parcel Shipments", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("${uiState.myParcels.size} shipments") },
                            leadingContent = {
                                TakeIt3DIconOrb(Icons.Rounded.Inventory2, TransportOrange, size = 36.dp)
                            },
                            modifier = Modifier.clickable(onClick = onOpenShipments)
                        )
                        HorizontalDivider(color = TakeItBorder)
                        ListItem(
                            headlineContent = { Text("Take It Wallet & Razorpay", fontWeight = FontWeight.Bold) },
                            supportingContent = { Text("Balance: ₹${uiState.profile.walletBalance.toInt()}") },
                            leadingContent = {
                                TakeIt3DIconOrb(Icons.Rounded.AccountBalanceWallet, TransportNavy, size = 36.dp)
                            },
                            modifier = Modifier.clickable(onClick = onOpenWallet)
                        )
                        HorizontalDivider(color = TakeItBorder)
                        ListItem(
                            headlineContent = { Text("Switch Google Account / Sign Out", fontWeight = FontWeight.ExtraBold, color = TakeItRed) },
                            supportingContent = { Text("Choose another Gmail account") },
                            leadingContent = {
                                TakeIt3DIconOrb(Icons.Rounded.SwitchAccount, TakeItRed, size = 36.dp)
                            },
                            modifier = Modifier
                                .clickable(onClick = onSignOut)
                                .testTag("transport_switch_account_btn")
                        )
                    }
                }
            }
        }
    }

    if (showEdit) {
        var nameInput by remember { mutableStateOf(uiState.profile.name.ifBlank { uiState.displayName }) }
        var phoneInput by remember { mutableStateOf(uiState.profile.phone) }
        AlertDialog(
            onDismissRequest = { showEdit = false },
            title = { Text("Edit Sender Profile", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, phoneInput)
                        showEdit = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TransportOrange)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEdit = false }) { Text("Cancel") } }
        )
    }
}
