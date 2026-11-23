package com.example.ui.screens.customer

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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DemoCatalog
import com.example.model.CartEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState

private val LogisticsRoyalStart = Color(0xFF1D4ED8)
private val LogisticsRoyalEnd = Color(0xFF2563EB)
private val LogisticsSkyBg = Color(0xFFEFF6FF)
private val LogisticsYellowCta = Color(0xFFFFD43B)

@Composable
fun ParcelDeliveryScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onBookParcel: (title: String, subtitle: String, amount: Double, payment: String, pickup: String, drop: String, otp: String, partner: String, onCreated: (String) -> Unit) -> Unit,
    onCompleteOrder: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    ParcelTransportUnifiedScreen(
        initialTab = 0,
        uiState = uiState,
        onBack = onBack,
        onBookParcel = onBookParcel,
        onBookTransport = onBookParcel,
        onCompleteOrder = onCompleteOrder,
        onShowMessage = onShowMessage
    )
}

@Composable
fun GoodsTransportScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onBookTransport: (title: String, subtitle: String, amount: Double, payment: String, pickup: String, drop: String, otp: String, partner: String, onCreated: (String) -> Unit) -> Unit
) {
    ParcelTransportUnifiedScreen(
        initialTab = 1,
        uiState = uiState,
        onBack = onBack,
        onBookParcel = onBookTransport,
        onBookTransport = onBookTransport,
        onCompleteOrder = {},
        onShowMessage = {}
    )
}

@Composable
private fun ParcelTransportUnifiedScreen(
    initialTab: Int,
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onBookParcel: (title: String, subtitle: String, amount: Double, payment: String, pickup: String, drop: String, otp: String, partner: String, onCreated: (String) -> Unit) -> Unit,
    onBookTransport: (title: String, subtitle: String, amount: Double, payment: String, pickup: String, drop: String, otp: String, partner: String, onCreated: (String) -> Unit) -> Unit,
    onCompleteOrder: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var activeTab by remember { mutableIntStateOf(initialTab) } // 0 = Parcel, 1 = Transport

    // Shared Pickup/Drop state
    var pickup by remember { mutableStateOf("Sector 62, Noida") }
    var drop by remember { mutableStateOf("DLF Cyber Hub, Gurugram") }
    var enableMultiStop by remember { mutableStateOf(false) }
    var intermediateStop by remember { mutableStateOf("") }

    // Parcel tab state
    var receiverName by remember { mutableStateOf("Vikram Nair") }
    var receiverPhone by remember { mutableStateOf("+91 98200 44112") }
    var parcelType by remember { mutableStateOf("Documents") }
    var sizeOption by remember { mutableStateOf("Small (Up to 2 kg)") }
    var showPackageDropdown by remember { mutableStateOf(false) }
    var showExtraDetails by remember { mutableStateOf(false) }
    var bookedOrderId by remember { mutableStateOf<String?>(null) }
    var bookedWasTransport by remember { mutableStateOf(false) }
    var deliveredProof by remember { mutableStateOf(false) }

    // Transport tab state
    var selectedVehicle by remember { mutableStateOf(DemoCatalog.transportVehicles[2]) } // Tata Ace
    var loadingAssist by remember { mutableStateOf(true) }
    var unloadingAssist by remember { mutableStateOf(true) }
    var extraHelpers by remember { mutableIntStateOf(1) }
    var transportStepIndex by remember { mutableIntStateOf(2) }

    val transportSteps = remember {
        listOf(
            "Pickup Confirmed & Truck Dispatched",
            "Loading Cargo & Strapping",
            "In Transit via Expressway",
            "Arriving at Drop Bay",
            "Delivered • OTP & Proof Verified"
        )
    }

    val pickupOtp = "3418"
    val deliveryOtp = "7925"
    val parcelFare = when {
        sizeOption.startsWith("Small") -> 99
        sizeOption.startsWith("Medium") -> 149
        else -> 219
    }
    val distanceKm = if (enableMultiStop && intermediateStop.isNotBlank()) 16.8 else 12.4
    val helperCost = (if (loadingAssist) 150 else 0) + (if (unloadingAssist) 150 else 0) + (extraHelpers * 220)
    val transportFare = selectedVehicle.baseFare + (distanceKm * selectedVehicle.perKmRate).toInt() + helperCost

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(LogisticsSkyBg)
    ) {
        // 1. ROYAL BLUE CURVED TOP HEADER + SEGMENTED TOGGLE (Screen 7 Exact Look)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(LogisticsRoyalStart, LogisticsRoyalEnd))
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(48.dp).testTag("back_button")
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SoftWhite)
                    }
                    Text(
                        text = "Parcel & Transport",
                        style = MaterialTheme.typography.titleLarge,
                        color = SoftWhite,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.size(48.dp))
                }

                // Segmented White Pill: Parcel | Transport
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoftWhite,
                    shadowElevation = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 4.dp, bottom = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp)
                    ) {
                        listOf("Parcel", "Transport").forEachIndexed { idx, label ->
                            val selected = activeTab == idx
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (selected) Color(0xFFEFF6FF) else Color.Transparent)
                                    .clickable {
                                        activeTab = idx
                                        bookedOrderId = null
                                    }
                                    .padding(vertical = 10.dp)
                                    .testTag("logistics_tab_${label.lowercase()}"),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = label,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (selected) LogisticsRoyalEnd else MutedText,
                                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                                    )
                                    if (selected) {
                                        Spacer(Modifier.height(3.dp))
                                        Box(
                                            Modifier
                                                .width(34.dp)
                                                .height(3.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(LogisticsRoyalEnd)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 2. SCROLLABLE BODY
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 3D Yellow Mini Cargo Truck + Parcel Boxes Banner (Screen 7 Exact Visual)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp)
                        .shadow(4.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFDBEAFE))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_parcel_truck_1795427329625),
                        contentDescription = "3D Yellow Cargo Truck and Parcel Boxes",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(50))
                            .background(DeepNavy.copy(alpha = 0.85f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (activeTab == 0) "⚡ OTP-Secured Express Parcel" else "🚚 Mini Trucks & Movers",
                            style = MaterialTheme.typography.labelSmall,
                            color = LogisticsYellowCta,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            if (bookedOrderId != null) {
                // LIVE TRACKING VIEW
                item {
                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(20.dp)),
                        pickupLabel = pickup,
                        dropLabel = drop,
                        distanceKm = distanceKm,
                        etaMinutes = if (bookedWasTransport) 24 else 16,
                        driverProgress = -1f
                    )
                }
                item {
                    Surface(shape = RoundedCornerShape(22.dp), color = DeepNavy) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Character3DPortrait(
                                        drawableRes = R.drawable.img_char_porter_mover_1795426024294,
                                        contentDescription = "Partner",
                                        borderColor = LogisticsYellowCta,
                                        size = 50.dp,
                                        bobbing = false
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            if (bookedWasTransport) "${selectedVehicle.name} • $bookedOrderId" else "Parcel $bookedOrderId",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SoftWhite,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                        Text(
                                            if (bookedWasTransport) "Ramesh Yadav • UP-16-TR-8821" else "Manoj Singh • $parcelType",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SkyBlue
                                        )
                                    }
                                }
                                StatusPill(if (deliveredProof || transportStepIndex == 4) "DELIVERED" else "ON_THE_WAY")
                            }

                            if (!bookedWasTransport) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).background(DarkCardElevated).padding(10.dp)) {
                                        Column {
                                            Text("Pickup OTP", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                                            Text(pickupOtp, style = MaterialTheme.typography.titleLarge, color = LogisticsYellowCta, fontWeight = FontWeight.ExtraBold)
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
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text("Confirm Delivery & Proof", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("LIVE 5-STEP LOGISTICS TRACKER", style = MaterialTheme.typography.labelSmall, color = LogisticsYellowCta, fontWeight = FontWeight.ExtraBold)
                                    TextButton(onClick = { transportStepIndex = (transportStepIndex + 1) % transportSteps.size }) {
                                        Text("Advance →", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                                    }
                                }
                                MultiStepStatusTimeline(
                                    steps = transportSteps,
                                    currentStepIndex = transportStepIndex,
                                    accentColor = LogisticsYellowCta,
                                    isDark = true
                                )
                            }

                            Button(
                                onClick = { bookedOrderId = null; deliveredProof = false },
                                colors = ButtonDefaults.buttonColors(containerColor = LogisticsYellowCta, contentColor = DeepNavy),
                                modifier = Modifier.fillMaxWidth().height(48.dp).navigationBarsPadding(),
                                shape = RoundedCornerShape(50)
                            ) {
                                Text("Book Another Delivery", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            } else {
                // FLOATING WHITE PICKUP / DROP CARD (Exact Screen 7 Mockup Geometry)
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SoftWhite,
                        shadowElevation = 3.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(end = 12.dp)
                            ) {
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF16A34A))
                                        .border(2.5.dp, Color(0xFFBBF7D0), CircleShape)
                                )
                                Box(Modifier.width(1.5.dp).height(46.dp).background(Color(0xFFCBD5E1)))
                                Box(
                                    Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444))
                                        .border(2.5.dp, Color(0xFFFECACA), CircleShape)
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column {
                                    Text("Pickup Location", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = pickup,
                                        onValueChange = { pickup = it },
                                        placeholder = { Text("Enter pickup address") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (activeTab == 1 && enableMultiStop) {
                                    OutlinedTextField(
                                        value = intermediateStop,
                                        onValueChange = { intermediateStop = it },
                                        label = { Text("Intermediate Stop") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                Column {
                                    Text("Drop Location", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
                                    OutlinedTextField(
                                        value = drop,
                                        onValueChange = { drop = it },
                                        placeholder = { Text("Enter drop address") },
                                        singleLine = true,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                if (activeTab == 0) {
                    // PARCEL TAB: PACKAGE TYPE SELECTOR CARD (Screen 7 Exact Card)
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SoftWhite,
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showPackageDropdown = !showPackageDropdown }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Package Type", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                        Text(parcelType, style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Icon(
                                        if (showPackageDropdown) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = "Select package type",
                                        tint = DeepNavy
                                    )
                                }
                                if (showPackageDropdown) {
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf("Documents", "Electronics", "Keys & Wallet", "Gift Box", "Clothes", "Medicines")) { t ->
                                            FilterChip(
                                                selected = parcelType == t,
                                                onClick = { parcelType = t; showPackageDropdown = false },
                                                label = { Text(t) }
                                            )
                                        }
                                    }
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf("Small (Up to 2 kg)", "Medium (2-5 kg)", "Large (5-12 kg)")) { s ->
                                            FilterChip(selected = sizeOption == s, onClick = { sizeOption = s }, label = { Text(s) })
                                        }
                                    }
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showExtraDetails = !showExtraDetails },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Receiver: $receiverName ($sizeOption)", style = MaterialTheme.typography.labelSmall, color = LogisticsRoyalEnd, fontWeight = FontWeight.Bold)
                                    Text(if (showExtraDetails) "Hide" else "Edit", style = MaterialTheme.typography.labelSmall, color = LogisticsRoyalEnd, fontWeight = FontWeight.ExtraBold)
                                }
                                if (showExtraDetails) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(value = receiverName, onValueChange = { receiverName = it }, label = { Text("Receiver Name") }, modifier = Modifier.weight(1f), singleLine = true)
                                        OutlinedTextField(value = receiverPhone, onValueChange = { receiverPhone = it }, label = { Text("Mobile") }, modifier = Modifier.weight(1f), singleLine = true)
                                    }
                                }
                            }
                        }
                    }

                    // GOLDEN YELLOW "Calculate Fare →" / BOOK CTA (Screen 7 Exact Look)
                    item {
                        Button(
                            onClick = {
                                bookedWasTransport = false
                                onBookParcel(
                                    "Parcel • $parcelType",
                                    "$sizeOption • To $receiverName",
                                    parcelFare.toDouble(),
                                    "UPI",
                                    pickup,
                                    drop,
                                    deliveryOtp,
                                    "Manoj Singh"
                                ) { id -> bookedOrderId = id }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .navigationBarsPadding()
                                .testTag("book_parcel_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = LogisticsYellowCta, contentColor = DeepNavy),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Calculate Fare & Book • ₹$parcelFare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                    }
                } else {
                    // TRANSPORT TAB: VEHICLE TIERS + LOADING ASSISTANCE + GOLDEN CTA
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Select Vehicle Tier", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            FilterChip(
                                selected = enableMultiStop,
                                onClick = { enableMultiStop = !enableMultiStop },
                                label = { Text("+ Multi-Stop", style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    items(DemoCatalog.transportVehicles) { v ->
                        val selected = v.id == selectedVehicle.id
                        val est = v.baseFare + (distanceKm * v.perKmRate).toInt()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(if (selected) Color(0xFFDBEAFE) else SoftWhite)
                                .border(if (selected) 2.dp else 1.dp, if (selected) LogisticsRoyalEnd else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                .clickable { selectedVehicle = v }
                                .padding(13.dp)
                                .testTag("transport_vehicle_${v.id}"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(13.dp))
                                    .background(LogisticsYellowCta),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.LocalShipping, v.name, tint = DeepNavy, modifier = Modifier.size(26.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(v.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                Text("Max ${v.maxWeightKg} kg • ${v.dimensions} • ${v.etaMin} min", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                            Text("₹$est", style = MaterialTheme.typography.titleLarge, color = LogisticsRoyalEnd, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    item {
                        Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Loading Assistance (+₹150)", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                    Switch(checked = loadingAssist, onCheckedChange = { loadingAssist = it })
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Unloading Assistance (+₹150)", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                    Switch(checked = unloadingAssist, onCheckedChange = { unloadingAssist = it })
                                }
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Extra Helpers (₹220 each)", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
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
                                bookedWasTransport = true
                                val stopNote = if (enableMultiStop && intermediateStop.isNotBlank()) " • Via $intermediateStop" else ""
                                onBookTransport(
                                    "${selectedVehicle.name} • Cargo Move",
                                    "${"%.1f".format(distanceKm)} km$stopNote • $extraHelpers helper(s)",
                                    transportFare.toDouble(),
                                    "UPI",
                                    pickup,
                                    drop,
                                    "6641",
                                    "Ramesh Yadav"
                                ) { id -> bookedOrderId = id }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .navigationBarsPadding()
                                .testTag("book_transport_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = LogisticsYellowCta, contentColor = DeepNavy),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Book ${selectedVehicle.name} • ₹$transportFare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
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
