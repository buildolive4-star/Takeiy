package com.example.ui.screens.customer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DemoCatalog
import com.example.model.CartEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState

@Composable
fun ParcelDeliveryScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onBookParcel: (title: String, subtitle: String, amount: Double, payment: String, pickup: String, drop: String, otp: String, partner: String, onCreated: (String) -> Unit) -> Unit,
    onCompleteOrder: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var pickup by remember { mutableStateOf("Indiranagar 100ft Rd") }
    var drop by remember { mutableStateOf("Koramangala WeWork Galaxy") }
    var senderName by remember { mutableStateOf(uiState.appState.userName) }
    var senderPhone by remember { mutableStateOf(uiState.appState.userPhone) }
    var receiverName by remember { mutableStateOf("Vikram Nair") }
    var receiverPhone by remember { mutableStateOf("+91 98200 44112") }
    var parcelType by remember { mutableStateOf("Documents") }
    var sizeOption by remember { mutableStateOf("Small (Up to 2 kg)") }
    var dimensions by remember { mutableStateOf("30 × 20 × 12 cm") }
    var instructions by remember { mutableStateOf("Hand over at reception desk") }
    var isScheduled by remember { mutableStateOf(false) }
    var bookedOrderId by remember { mutableStateOf<String?>(null) }
    var deliveredProof by remember { mutableStateOf(false) }

    val pickupOtp = "3418"
    val deliveryOtp = "7925"
    val fare = when {
        sizeOption.startsWith("Small") -> 89
        sizeOption.startsWith("Medium") -> 139
        else -> 199
    } + if (isScheduled) 0 else 20

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = {
            ZiporaSubTopBar(
                title = "Express Parcel Delivery",
                subtitle = "Door-to-door OTP secured courier",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3D Porter Mover Hero Strip
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(DeepNavy, SkyBlueDark)))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Character3DPortrait(
                        drawableRes = R.drawable.img_char_porter_mover_1795426024294,
                        contentDescription = "3D Parcel Courier Character",
                        borderColor = PremiumYellow,
                        size = 76.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("OTP-SECURED COURIER", style = MaterialTheme.typography.labelSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                        Text("Send Anything Across City", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        Text("Live GPS tracking • Dual Pickup & Drop OTP Shield", style = MaterialTheme.typography.bodySmall, color = RideCardStart)
                    }
                }
            }

            if (bookedOrderId != null) {
                item {
                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxWidth().height(210.dp),
                        pickupLabel = pickup,
                        dropLabel = drop,
                        distanceKm = 6.4,
                        etaMinutes = 19,
                        driverProgress = -1f
                    )
                }
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = DeepNavy) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Character3DPortrait(
                                        drawableRes = R.drawable.img_char_porter_mover_1795426024294,
                                        contentDescription = "Partner Manoj Singh",
                                        borderColor = SkyBlue,
                                        size = 48.dp,
                                        bobbing = false
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Parcel $bookedOrderId", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                        Text("Manoj Singh • $dimensions", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                                    }
                                }
                                StatusPill(if (deliveredProof) "DELIVERED" else "ON_THE_WAY")
                            }
                            Text("$parcelType ($sizeOption) • To $receiverName ($receiverPhone)", style = MaterialTheme.typography.bodySmall, color = RideCardStart)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(DarkCardElevated).padding(10.dp)) {
                                    Column {
                                        Text("Pickup OTP", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                                        Text(pickupOtp, style = MaterialTheme.typography.titleLarge, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(DarkCardElevated).padding(10.dp)) {
                                    Column {
                                        Text("Delivery OTP", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                                        Text(deliveryOtp, style = MaterialTheme.typography.titleLarge, color = SuccessGreen, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            if (!deliveredProof) {
                                Button(
                                    onClick = {
                                        deliveredProof = true
                                        bookedOrderId?.let { onCompleteOrder(it) }
                                        onShowMessage("Proof of Delivery verified with OTP $deliveryOtp!")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite)
                                ) {
                                    Text("Simulate Delivery & Proof of Delivery", fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Surface(shape = RoundedCornerShape(14.dp), color = SuccessGreen.copy(alpha = 0.2f)) {
                                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.Verified, null, tint = SuccessGreen)
                                        Spacer(Modifier.width(8.dp))
                                        Text("Proof of Delivery captured • Signed by $receiverName", style = MaterialTheme.typography.labelMedium, color = SoftWhite)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = !isScheduled, onClick = { isScheduled = false }, label = { Text("⚡ Instant Express") })
                                FilterChip(selected = isScheduled, onClick = { isScheduled = true }, label = { Text("📅 Scheduled Slot") })
                            }
                            OutlinedTextField(value = pickup, onValueChange = { pickup = it }, label = { Text("Pickup Address") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = drop, onValueChange = { drop = it }, label = { Text("Delivery Address") }, modifier = Modifier.fillMaxWidth())
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = senderName, onValueChange = { senderName = it }, label = { Text("Sender Name") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = senderPhone, onValueChange = { senderPhone = it }, label = { Text("Sender Mobile") }, modifier = Modifier.weight(1f))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(value = receiverName, onValueChange = { receiverName = it }, label = { Text("Receiver Name") }, modifier = Modifier.weight(1f))
                                OutlinedTextField(value = receiverPhone, onValueChange = { receiverPhone = it }, label = { Text("Receiver Mobile") }, modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Package Type, Size & Dimensions", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Documents", "Electronics", "Keys & Wallet", "Gift Box", "Clothes", "Medicines")) { t ->
                                    FilterChip(selected = parcelType == t, onClick = { parcelType = t }, label = { Text(t) })
                                }
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Small (Up to 2 kg)", "Medium (2-5 kg)", "Large (5-12 kg)")) { s ->
                                    FilterChip(selected = sizeOption == s, onClick = { sizeOption = s }, label = { Text(s) })
                                }
                            }
                            OutlinedTextField(value = dimensions, onValueChange = { dimensions = it }, label = { Text("Package Dimensions (L × W × H)") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = instructions, onValueChange = { instructions = it }, label = { Text("Delivery Instructions") }, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            onBookParcel(
                                "Parcel • $parcelType",
                                "$sizeOption ($dimensions) • To $receiverName",
                                fare.toDouble(),
                                "UPI",
                                pickup,
                                drop,
                                deliveryOtp,
                                "Manoj Singh"
                            ) { id -> bookedOrderId = id }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("book_parcel_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Confirm Parcel Booking • ₹$fare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun GoodsTransportScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onBookTransport: (title: String, subtitle: String, amount: Double, payment: String, pickup: String, drop: String, otp: String, partner: String, onCreated: (String) -> Unit) -> Unit
) {
    var pickup by remember { mutableStateOf("HSR Layout Sector 1") }
    var intermediateStop by remember { mutableStateOf("") }
    var enableMultiStop by remember { mutableStateOf(false) }
    var drop by remember { mutableStateOf("Whitefield Prestige Tech Park") }
    var goodsCategory by remember { mutableStateOf("Furniture & Appliances") }
    var selectedVehicle by remember { mutableStateOf(DemoCatalog.transportVehicles[2]) } // Tata Ace
    var loadingAssist by remember { mutableStateOf(true) }
    var unloadingAssist by remember { mutableStateOf(true) }
    var isScheduled by remember { mutableStateOf(false) }
    var extraHelpers by remember { mutableIntStateOf(1) }
    var bookedId by remember { mutableStateOf<String?>(null) }
    var transportStepIndex by remember { mutableIntStateOf(2) }

    val transportSteps = remember {
        listOf(
            "Pickup Confirmed & Truck Dispatched",
            "Loading Cargo & Strapping",
            "In Transit via Outer Ring Rd",
            "Arriving at Drop Bay",
            "Delivered • OTP & Proof Verified"
        )
    }

    val distanceKm = if (enableMultiStop && intermediateStop.isNotBlank()) 16.8 else 13.5
    val helperCost = (if (loadingAssist) 150 else 0) + (if (unloadingAssist) 150 else 0) + (extraHelpers * 220)
    val totalFare = selectedVehicle.baseFare + (distanceKm * selectedVehicle.perKmRate).toInt() + helperCost

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = {
            ZiporaSubTopBar(
                title = "Goods Transport & Mini Trucks",
                subtitle = "Industrial intra-city logistics & movers",
                onBack = onBack,
                isDark = true
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Industrial Blue + Yellow + Dark Navy Hero with 3D Mover Character
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(DarkBackground, IndustrialBlue)))
                        .border(1.3.dp, PremiumYellow.copy(alpha = 0.6f), RoundedCornerShape(22.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(6.dp)).background(PremiumYellow).padding(horizontal = 8.dp, vertical = 2.dp)) {
                            Text("ZIPORA LOGISTICS FLEET", style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        }
                        Text("Mini Trucks & Heavy Movers", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        Text("6 capacity tiers • Multi-stop • Loading & unloading crew", style = MaterialTheme.typography.bodySmall, color = RideCardStart)
                    }
                    Spacer(Modifier.width(10.dp))
                    Character3DPortrait(
                        drawableRes = R.drawable.img_char_porter_mover_1795426024294,
                        contentDescription = "3D Goods Transport Mover",
                        borderColor = PremiumYellow,
                        size = 82.dp
                    )
                }
            }

            if (bookedId != null) {
                item {
                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxWidth().height(215.dp),
                        pickupLabel = pickup,
                        dropLabel = drop,
                        distanceKm = distanceKm,
                        etaMinutes = 26,
                        driverProgress = -1f,
                        isDarkStyle = true
                    )
                }
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = DeepNavy) {
                        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Character3DPortrait(
                                    drawableRes = R.drawable.img_char_porter_mover_1795426024294,
                                    contentDescription = "Driver Ramesh Yadav",
                                    borderColor = PremiumYellow,
                                    size = 56.dp,
                                    bobbing = false
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    StatusPill(if (transportStepIndex == 4) "DELIVERED" else "IN_TRANSIT")
                                    Spacer(Modifier.height(4.dp))
                                    Text("${selectedVehicle.name} • $bookedId", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                                    Text("Ramesh Yadav • KA-05-TR-8821 • $extraHelpers Helper(s)", style = MaterialTheme.typography.bodySmall, color = SkyBlue)
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("LIVE 5-STEP LOGISTICS TRACKER", style = MaterialTheme.typography.labelSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                                TextButton(onClick = { transportStepIndex = (transportStepIndex + 1) % transportSteps.size }) {
                                    Text("Advance Step →", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                                }
                            }
                            MultiStepStatusTimeline(
                                steps = transportSteps,
                                currentStepIndex = transportStepIndex,
                                accentColor = PremiumYellow,
                                isDark = true
                            )

                            Text("Total Estimate: ₹$totalFare (OTP: 6641 • Simulated)", style = MaterialTheme.typography.titleMedium, color = PremiumYellow, fontWeight = FontWeight.Bold)
                            Button(
                                onClick = { bookedId = null },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumYellow, contentColor = DeepNavy),
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Text("Book Another Commercial Vehicle", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(selected = !isScheduled, onClick = { isScheduled = false }, label = { Text("⚡ Dispatch Now") })
                                FilterChip(selected = isScheduled, onClick = { isScheduled = true }, label = { Text("📅 Schedule Booking") })
                                FilterChip(selected = enableMultiStop, onClick = { enableMultiStop = !enableMultiStop }, label = { Text("+ Multi-Stop") })
                            }
                            OutlinedTextField(value = pickup, onValueChange = { pickup = it }, label = { Text("Pickup Location") }, modifier = Modifier.fillMaxWidth())
                            if (enableMultiStop) {
                                OutlinedTextField(
                                    value = intermediateStop,
                                    onValueChange = { intermediateStop = it },
                                    label = { Text("Intermediate Stop (Optional)") },
                                    placeholder = { Text("e.g. Marathahalli Warehouse Gate 2") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            OutlinedTextField(value = drop, onValueChange = { drop = it }, label = { Text("Drop Location") }, modifier = Modifier.fillMaxWidth())
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Furniture & Appliances", "Retail Wholesale", "Office Shift", "Hardware & Timber", "E-Commerce Bulk")) { cat ->
                                    FilterChip(selected = goodsCategory == cat, onClick = { goodsCategory = cat }, label = { Text(cat) })
                                }
                            }
                        }
                    }
                }

                item {
                    Text("Select Commercial Vehicle (6 Tiers)", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                }

                items(DemoCatalog.transportVehicles) { v ->
                    val selected = v.id == selectedVehicle.id
                    val est = v.baseFare + (distanceKm * v.perKmRate).toInt()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (selected) Color(0xFFDBEAFE) else SoftWhite)
                            .border(if (selected) 2.2.dp else 1.dp, if (selected) IndustrialBlue else LightBorder, RoundedCornerShape(18.dp))
                            .clickable { selectedVehicle = v }
                            .padding(14.dp)
                            .testTag("transport_vehicle_${v.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconOrb3D(Icons.Rounded.LocalShipping, PremiumYellow, IndustrialBlue, DeepNavy, size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(v.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Text("Max ${v.maxWeightKg} kg • ${v.dimensions} • ${v.etaMin} min", style = MaterialTheme.typography.bodySmall, color = SlateText)
                            Text(v.idealFor, style = MaterialTheme.typography.labelSmall, color = IndustrialBlue, fontWeight = FontWeight.SemiBold)
                        }
                        Text("₹$est", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    }
                }

                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Loading Assistance", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                    Text("Floor-to-vehicle loading (+₹150)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                Switch(checked = loadingAssist, onCheckedChange = { loadingAssist = it })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Unloading Assistance", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                    Text("Safe drop-off unloading (+₹150)", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                Switch(checked = unloadingAssist, onCheckedChange = { unloadingAssist = it })
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Additional Helpers", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                    Text("₹220 per helper for heavy lifting", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = { if (extraHelpers > 0) extraHelpers-- }) { Icon(Icons.Rounded.RemoveCircleOutline, "Less", tint = DeepNavy) }
                                    Text("$extraHelpers", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                    IconButton(onClick = { if (extraHelpers < 4) extraHelpers++ }) { Icon(Icons.Rounded.AddCircleOutline, "More", tint = DeepNavy) }
                                }
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            val stopNote = if (enableMultiStop && intermediateStop.isNotBlank()) " • Via $intermediateStop" else ""
                            onBookTransport(
                                "${selectedVehicle.name} • $goodsCategory",
                                "${"%.1f".format(distanceKm)} km$stopNote • $extraHelpers helper(s)",
                                totalFare.toDouble(),
                                "UPI",
                                pickup,
                                drop,
                                "6641",
                                "Ramesh Yadav"
                            ) { id -> bookedId = id }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp).testTag("book_transport_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = IndustrialBlue, contentColor = PremiumYellow),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text("Book ${selectedVehicle.name} • ₹$totalFare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
fun LocalShoppingScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onAddToCart: (CartEntry) -> Unit,
    onOpenCart: () -> Unit
) {
    val cats = listOf("All", "Electronics", "Fashion", "Home Essentials")
    var selectedCat by remember { mutableStateOf("All") }
    val itemsList = remember(selectedCat) {
        DemoCatalog.localShopProducts.filter { selectedCat == "All" || it.category == selectedCat }
    }

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = {
            ZiporaSubTopBar(
                title = "Local Shopping",
                subtitle = "Nearby Electronics, Fashion & Home in 30 mins",
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (uiState.cartItemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCart,
                    containerColor = DeepNavy,
                    contentColor = PremiumYellow,
                    modifier = Modifier.navigationBarsPadding()
                ) {
                    Icon(Icons.Rounded.ShoppingBag, null)
                    Spacer(Modifier.width(8.dp))
                    Text("${uiState.cartItemCount} • ₹${uiState.cartGrandTotal}", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(cats) { cat ->
                        FilterChip(selected = selectedCat == cat, onClick = { selectedCat = cat }, label = { Text(cat) })
                    }
                }
            }
            items(itemsList, key = { it.id }) { prod ->
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 3.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconOrb3D(
                            icon = when (prod.category) {
                                "Electronics" -> Icons.Rounded.Headphones
                                "Fashion" -> Icons.Rounded.Checkroom
                                else -> Icons.Rounded.Chair
                            },
                            tint = SoftWhite,
                            gradientStart = PinkAccent,
                            gradientEnd = DeepNavy,
                            size = 56.dp,
                            iconSize = 28.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(6.dp)).background(ShoppingCardStart).padding(horizontal = 6.dp, vertical = 2.dp)) {
                                Text("${prod.badge} • ${prod.deliveryMins} min", style = MaterialTheme.typography.labelSmall, color = PinkAccent)
                            }
                            Text(prod.name, style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            Text("${prod.shopName} • ★ ${prod.rating}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("₹${prod.price}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                Text("₹${prod.mrp}", style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough), color = MutedText)
                            }
                        }
                        Button(
                            onClick = {
                                onAddToCart(CartEntry(prod.id, prod.name, prod.shopName, "SHOPPING", prod.price, prod.mrp, 1))
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Buy", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
