package com.example.ui.screens.customer

import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Character3DPortrait
import com.example.ui.components.SimulatedMapCanvas
import com.example.ui.theme.*
import com.example.util.DeviceActions
import com.example.viewmodel.SuperAppUiState

private val TransportNavy = Color(0xFF0B2154)
private val TransportRoyal = Color(0xFF1D5BF2)
private val TransportBg = Color(0xFFF3F6FC)
private val TransportGold = Color(0xFFFFD43B)
private val TransportEmerald = Color(0xFF16A34A)

private enum class TransportStage {
    HOME,
    GOODS_FORM,
    FARE_DETAILS,
    BOOKING_SUCCESS,
    LIVE_TRACKING
}

private data class TransportFleetItem(
    val id: String,
    val name: String,
    val fare: Int,
    val eta: String,
    val capacity: String,
    val dimensions: String,
    val icon: ImageVector,
    val tint: Color
)

@Composable
fun GoodsTransportScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onBookTransport: (
        title: String,
        subtitle: String,
        amount: Double,
        payment: String,
        pickup: String,
        drop: String,
        otp: String,
        partner: String,
        onCreated: (String) -> Unit
    ) -> Unit
) {
    val fleet = remember {
        listOf(
            TransportFleetItem("bike", "Bike", 49, "~5 min", "20 kg", "2 ft x 2 ft", Icons.Rounded.TwoWheeler, Color(0xFF2563EB)),
            TransportFleetItem("auto", "Auto", 79, "~8 min", "500 kg", "5 ft x 4 ft", Icons.Rounded.ElectricRickshaw, Color(0xFFD97706)),
            TransportFleetItem("mini", "Mini Truck", 149, "~12 min", "1.5 Ton", "12 ft x 6 ft", Icons.Rounded.LocalShipping, Color(0xFF1D4ED8)),
            TransportFleetItem("pickup", "Pickup", 199, "~15 min", "1.2 Ton", "8 ft x 5 ft", Icons.Rounded.AirportShuttle, Color(0xFF0F172A)),
            TransportFleetItem("ace", "Tata Ace", 249, "~18 min", "750 kg", "7 ft x 4.8 ft", Icons.Rounded.FireTruck, Color(0xFFEAB308)),
            TransportFleetItem("large", "Large Truck", 399, "~25 min", "3.5 Ton", "14 ft x 7 ft", Icons.Rounded.DirectionsBus, Color(0xFF1E3A8A))
        )
    }

    var stage by remember { mutableStateOf(TransportStage.HOME) }
    var pickup by remember { mutableStateOf("Sector 62, Noida") }
    var drop by remember { mutableStateOf("Greater Noida") }
    var selectedVehicle by remember { mutableStateOf(fleet[2]) } // Mini Truck
    var goodsCategory by remember { mutableStateOf("Furniture & Home") }
    var weightKg by remember { mutableStateOf("250") }
    var includeHelper by remember { mutableStateOf(true) }
    var includeLoading by remember { mutableStateOf(true) }
    var multipleStops by remember { mutableStateOf(false) }
    var scheduleLater by remember { mutableStateOf(false) }
    var bookedId by remember { mutableStateOf("TKX20261123001") }
    var trackingStep by remember { mutableIntStateOf(1) }

    val baseFare = selectedVehicle.fare + 250 // Realistic cargo base + distance quote
    val helperCharge = if (includeHelper) 99 else 0
    val loadingCharge = if (includeLoading) 49 else 0
    val gstAmount = ((baseFare + helperCharge + loadingCharge) * 0.05).toInt()
    val totalAmount = baseFare + helperCharge + loadingCharge + gstAmount

    BackHandler(enabled = stage != TransportStage.HOME) {
        stage = when (stage) {
            TransportStage.GOODS_FORM -> TransportStage.HOME
            TransportStage.FARE_DETAILS -> TransportStage.GOODS_FORM
            TransportStage.BOOKING_SUCCESS, TransportStage.LIVE_TRACKING -> TransportStage.HOME
            TransportStage.HOME -> TransportStage.HOME
        }
    }

    when (stage) {
        TransportStage.HOME -> TransportHomeContent(
            fleet = fleet,
            selectedVehicle = selectedVehicle,
            pickup = pickup,
            drop = drop,
            onPickupChange = { pickup = it },
            onDropChange = { drop = it },
            onSwap = {
                val tmp = pickup
                pickup = drop
                drop = tmp
            },
            onSelectVehicle = {
                selectedVehicle = it
                stage = TransportStage.GOODS_FORM
            },
            onCalculateFare = { stage = TransportStage.GOODS_FORM },
            onBack = onBack
        )

        TransportStage.GOODS_FORM -> TransportGoodsFormContent(
            fleet = fleet,
            selectedVehicle = selectedVehicle,
            goodsCategory = goodsCategory,
            weightKg = weightKg,
            includeHelper = includeHelper,
            multipleStops = multipleStops,
            scheduleLater = scheduleLater,
            onSelectCategory = { goodsCategory = it },
            onSelectVehicle = { selectedVehicle = it },
            onWeightChange = { weightKg = it },
            onToggleHelper = { includeHelper = !includeHelper },
            onToggleMultiStop = { multipleStops = !multipleStops },
            onToggleSchedule = { scheduleLater = !scheduleLater },
            onGetQuote = { stage = TransportStage.FARE_DETAILS },
            onBack = { stage = TransportStage.HOME }
        )

        TransportStage.FARE_DETAILS -> TransportFareDetailsContent(
            vehicle = selectedVehicle,
            pickup = pickup,
            drop = drop,
            baseFare = baseFare,
            includeHelper = includeHelper,
            includeLoading = includeLoading,
            helperCharge = helperCharge,
            loadingCharge = loadingCharge,
            gstAmount = gstAmount,
            totalAmount = totalAmount,
            onToggleHelper = { includeHelper = it },
            onToggleLoading = { includeLoading = it },
            onConfirmBooking = {
                onBookTransport(
                    "${selectedVehicle.name} • $goodsCategory",
                    "$weightKg kg • ${selectedVehicle.capacity}",
                    totalAmount.toDouble(),
                    "UPI",
                    pickup,
                    drop,
                    "6641",
                    "Rajesh Singh"
                ) { id ->
                    bookedId = id
                    stage = TransportStage.BOOKING_SUCCESS
                }
            },
            onBack = { stage = TransportStage.GOODS_FORM }
        )

        TransportStage.BOOKING_SUCCESS -> TransportSuccessContent(
            bookingId = bookedId,
            vehicleName = selectedVehicle.name,
            onTrackOrder = { stage = TransportStage.LIVE_TRACKING },
            onBackHome = { stage = TransportStage.HOME }
        )

        TransportStage.LIVE_TRACKING -> TransportLiveTrackingContent(
            vehicle = selectedVehicle,
            pickup = pickup,
            drop = drop,
            currentStep = trackingStep,
            onAdvanceStep = { trackingStep = (trackingStep + 1) % 3 },
            onBack = { stage = TransportStage.HOME }
        )
    }
}

@Composable
private fun TransportHomeContent(
    fleet: List<TransportFleetItem>,
    selectedVehicle: TransportFleetItem,
    pickup: String,
    drop: String,
    onPickupChange: (String) -> Unit,
    onDropChange: (String) -> Unit,
    onSwap: () -> Unit,
    onSelectVehicle: (TransportFleetItem) -> Unit,
    onCalculateFare: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(TransportBg)
    ) {
        // Deep Navy Brand Top Bar (Image 5 exact look)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(TransportNavy)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("transport_back_button")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SoftWhite)
            }
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Take It ", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                Text("Transport", style = MaterialTheme.typography.titleLarge, color = TransportGold, fontWeight = FontWeight.ExtraBold)
            }
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape).background(TransportGold),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.LocalShipping, null, tint = TransportNavy, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(8.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Banner: "Fast & Reliable Transport Services"
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(166.dp)
                        .shadow(8.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(TransportNavy, TransportRoyal)))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_parcel_truck_1795427329625),
                        contentDescription = "Goods transport mini truck",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(172.dp).fillMaxHeight().align(Alignment.CenterEnd)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(0.0f to TransportNavy, 0.56f to TransportNavy.copy(alpha = 0.88f), 1.0f to Color.Transparent))
                    )
                    Column(
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.65f).padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Fast & Reliable\nTransport Services",
                                style = MaterialTheme.typography.titleLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 24.sp
                            )
                            Text(
                                "For Everyone, Everywhere",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftWhite.copy(alpha = 0.85f)
                            )
                        }
                        Button(
                            onClick = onCalculateFare,
                            colors = ButtonDefaults.buttonColors(containerColor = TransportGold, contentColor = TransportNavy),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(44.dp).testTag("transport_hero_book_now")
                        ) {
                            Text("Book Now", fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // Floating White Pickup / Drop Card + Golden "Calculate Fare →"
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 4.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = pickup,
                                    onValueChange = onPickupChange,
                                    label = { Text("Pickup Location") },
                                    leadingIcon = { Icon(Icons.Rounded.LocationOn, null, tint = TransportRoyal) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("transport_pickup_field")
                                )
                                OutlinedTextField(
                                    value = drop,
                                    onValueChange = onDropChange,
                                    label = { Text("Drop Location") },
                                    leadingIcon = { Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFDC2626)) },
                                    singleLine = true,
                                    shape = RoundedCornerShape(14.dp),
                                    modifier = Modifier.fillMaxWidth().testTag("transport_drop_field")
                                )
                            }
                            Surface(
                                onClick = onSwap,
                                shape = CircleShape,
                                color = Color(0xFFEFF6FF),
                                shadowElevation = 2.dp,
                                modifier = Modifier.align(Alignment.CenterEnd).padding(end = 4.dp).size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.SwapVert, "Swap", tint = TransportRoyal)
                                }
                            }
                        }

                        Button(
                            onClick = onCalculateFare,
                            colors = ButtonDefaults.buttonColors(containerColor = TransportGold, contentColor = TransportNavy),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("transport_calculate_fare_cta")
                        ) {
                            Text("Calculate Fare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // "Select Vehicle Type" 3×2 Grid
            item {
                Text(
                    "Select Vehicle Type",
                    style = MaterialTheme.typography.titleLarge,
                    color = TransportNavy,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    fleet.chunked(3).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            rowItems.forEach { v ->
                                val selected = v.id == selectedVehicle.id
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = if (selected) Color(0xFFEFF6FF) else SoftWhite,
                                    shadowElevation = 3.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(if (selected) 2.dp else 1.dp, if (selected) TransportRoyal else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                        .clickable { onSelectVehicle(v) }
                                        .testTag("transport_grid_${v.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(15.dp))
                                                .background(v.tint.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(v.icon, v.name, tint = v.tint, modifier = Modifier.size(28.dp))
                                        }
                                        Text(v.name, style = MaterialTheme.typography.labelLarge, color = TransportNavy, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                                        Text("₹${v.fare}", style = MaterialTheme.typography.titleSmall, color = TransportRoyal, fontWeight = FontWeight.ExtraBold)
                                        Text(v.eta, style = MaterialTheme.typography.labelSmall, color = MutedText)
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

@Composable
private fun TransportGoodsFormContent(
    fleet: List<TransportFleetItem>,
    selectedVehicle: TransportFleetItem,
    goodsCategory: String,
    weightKg: String,
    includeHelper: Boolean,
    multipleStops: Boolean,
    scheduleLater: Boolean,
    onSelectCategory: (String) -> Unit,
    onSelectVehicle: (TransportFleetItem) -> Unit,
    onWeightChange: (String) -> Unit,
    onToggleHelper: () -> Unit,
    onToggleMultiStop: () -> Unit,
    onToggleSchedule: () -> Unit,
    onGetQuote: () -> Unit,
    onBack: () -> Unit
) {
    val categories = listOf("Furniture & Home", "Electronics", "Boxes & Cartons", "Building Material", "Business Cargo")

    Scaffold(
        containerColor = TransportBg,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(SoftWhite).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TransportNavy)
                }
                Text("Goods Transport", style = MaterialTheme.typography.titleLarge, color = TransportNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.HeadsetMic, "Support", tint = TransportNavy)
                }
            }
        },
        bottomBar = {
            Surface(color = SoftWhite, shadowElevation = 10.dp) {
                Button(
                    onClick = onGetQuote,
                    colors = ButtonDefaults.buttonColors(containerColor = TransportGold, contentColor = TransportNavy),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(54.dp)
                        .testTag("transport_get_quote_button")
                ) {
                    Text("Get Quote", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(148.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(TransportRoyal, Color(0xFF38BDF8))))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_parcel_truck_1795427329625),
                        contentDescription = "Transport Your Goods Easily",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.verticalGradient(listOf(TransportNavy.copy(alpha = 0.84f), Color.Transparent)))
                    )
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Transport Your\nGoods Easily", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        Text("Small to Large – We Deliver!", style = MaterialTheme.typography.labelMedium, color = TransportGold, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Goods Category Picker
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Inventory2, null, tint = TransportRoyal)
                            Text("Goods Category", style = MaterialTheme.typography.titleSmall, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories) { cat ->
                                FilterChip(
                                    selected = goodsCategory == cat,
                                    onClick = { onSelectCategory(cat) },
                                    label = { Text(cat) }
                                )
                            }
                        }
                    }
                }
            }

            // Vehicle Type Picker
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.LocalShipping, null, tint = TransportRoyal)
                            Text("Vehicle Type • ${selectedVehicle.name} (${selectedVehicle.capacity})", style = MaterialTheme.typography.titleSmall, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(fleet) { v ->
                                FilterChip(
                                    selected = selectedVehicle.id == v.id,
                                    onClick = { onSelectVehicle(v) },
                                    label = { Text("${v.name} ₹${v.fare}") }
                                )
                            }
                        }
                    }
                }
            }

            // Weight Input
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Scale, null, tint = TransportRoyal)
                        OutlinedTextField(
                            value = weightKg,
                            onValueChange = onWeightChange,
                            label = { Text("Estimated Weight (kg)") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Add-on Toggles: Add Helper, Multiple Stops, Schedule Booking
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)) {
                        TransportAddonRow(Icons.Rounded.PersonAdd, "Add Helper (+₹99)", includeHelper, onToggleHelper)
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        TransportAddonRow(Icons.Rounded.AddLocationAlt, "Multiple Stops", multipleStops, onToggleMultiStop)
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        TransportAddonRow(Icons.Rounded.EventAvailable, "Schedule Booking", scheduleLater, onToggleSchedule)
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportAddonRow(icon: ImageVector, label: String, active: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, label, tint = TransportRoyal, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, color = TransportNavy, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Icon(
            imageVector = if (active) Icons.Rounded.CheckCircle else Icons.Rounded.Add,
            contentDescription = label,
            tint = if (active) TransportEmerald else TransportNavy
        )
    }
}

@Composable
private fun TransportFareDetailsContent(
    vehicle: TransportFleetItem,
    pickup: String,
    drop: String,
    baseFare: Int,
    includeHelper: Boolean,
    includeLoading: Boolean,
    helperCharge: Int,
    loadingCharge: Int,
    gstAmount: Int,
    totalAmount: Int,
    onToggleHelper: (Boolean) -> Unit,
    onToggleLoading: (Boolean) -> Unit,
    onConfirmBooking: () -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = TransportBg,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(SoftWhite).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TransportNavy)
                }
                Text("Fare Details", style = MaterialTheme.typography.titleLarge, color = TransportNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                Spacer(Modifier.size(48.dp))
            }
        },
        bottomBar = {
            Surface(color = SoftWhite, shadowElevation = 10.dp) {
                Button(
                    onClick = onConfirmBooking,
                    colors = ButtonDefaults.buttonColors(containerColor = TransportGold, contentColor = TransportNavy),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(54.dp)
                        .testTag("transport_confirm_booking_button")
                ) {
                    Text("Confirm Booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Vehicle Quote Highlight Card
            item {
                Surface(shape = RoundedCornerShape(22.dp), color = Color(0xFFEFF6FF), shadowElevation = 2.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            Box(
                                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(SoftWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(vehicle.icon, vehicle.name, tint = TransportRoyal, modifier = Modifier.size(38.dp))
                            }
                            Column {
                                Text(vehicle.name, style = MaterialTheme.typography.titleLarge, color = TransportRoyal, fontWeight = FontWeight.ExtraBold)
                                Text("Capacity: ${vehicle.capacity}", style = MaterialTheme.typography.bodySmall, color = TransportNavy, fontWeight = FontWeight.SemiBold)
                                Text("Dimensions: ${vehicle.dimensions}", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                        }
                        Text("₹$baseFare", style = MaterialTheme.typography.displaySmall, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                        Text("Estimated Fare", style = MaterialTheme.typography.labelMedium, color = MutedText)
                    }
                }
            }

            // Trip Details
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Trip Details", style = MaterialTheme.typography.titleMedium, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.LocationOn, null, tint = TransportRoyal, modifier = Modifier.size(20.dp))
                            Text(pickup, style = MaterialTheme.typography.bodyMedium, color = TransportNavy, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                            Text(drop, style = MaterialTheme.typography.bodyMedium, color = TransportNavy, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Additional Services + Bill Breakdown
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Additional Services", style = MaterialTheme.typography.titleMedium, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onToggleHelper(!includeHelper) }) {
                            Checkbox(checked = includeHelper, onCheckedChange = onToggleHelper)
                            Text("Helper (+₹99)", style = MaterialTheme.typography.bodyMedium, color = TransportNavy, fontWeight = FontWeight.SemiBold)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().clickable { onToggleLoading(!includeLoading) }) {
                            Checkbox(checked = includeLoading, onCheckedChange = onToggleLoading)
                            Text("Loading Assistance (+₹49)", style = MaterialTheme.typography.bodyMedium, color = TransportNavy, fontWeight = FontWeight.SemiBold)
                        }
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        FareLine("Base Fare", "₹$baseFare")
                        if (helperCharge > 0) FareLine("Helper Charges", "₹$helperCharge")
                        if (loadingCharge > 0) FareLine("Loading Assistance", "₹$loadingCharge")
                        FareLine("GST (5%)", "₹$gstAmount")

                        Surface(shape = RoundedCornerShape(14.dp), color = Color(0xFFFEF3C7), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Total Amount", style = MaterialTheme.typography.titleMedium, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                                Text("₹$totalAmount", style = MaterialTheme.typography.headlineSmall, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FareLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedText)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = TransportNavy, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun TransportSuccessContent(
    bookingId: String,
    vehicleName: String,
    onTrackOrder: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(84.dp).clip(CircleShape).background(TransportEmerald),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, "Success", tint = SoftWhite, modifier = Modifier.size(50.dp))
        }
        Spacer(Modifier.height(18.dp))
        Image(
            painter = painterResource(R.drawable.img_takeit_parcel_truck_1795427329625),
            contentDescription = "Booked transport truck",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxWidth(0.84f).height(150.dp).clip(RoundedCornerShape(22.dp))
        )
        Spacer(Modifier.height(20.dp))
        Text("Booking Successful!", style = MaterialTheme.typography.headlineMedium, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
        Spacer(Modifier.height(6.dp))
        Text(
            "Your $vehicleName is assigned and on the way.",
            style = MaterialTheme.typography.bodyMedium,
            color = MutedText,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(18.dp))
        val context = LocalContext.current
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEFF6FF),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { DeviceActions.copyToClipboard(context, "Booking ID", bookingId) }
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Booking ID", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    Text(bookingId, style = MaterialTheme.typography.titleMedium, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                }
                Icon(Icons.Rounded.ContentCopy, "Copy ID", tint = TransportRoyal)
            }
        }
        Spacer(Modifier.height(22.dp))
        Button(
            onClick = onTrackOrder,
            colors = ButtonDefaults.buttonColors(containerColor = TransportRoyal, contentColor = SoftWhite),
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().height(54.dp).testTag("transport_track_order_button")
        ) {
            Text("Track Order", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onBackHome,
            shape = RoundedCornerShape(50),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) {
            Text("Back to Home", color = TransportNavy, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun TransportLiveTrackingContent(
    vehicle: TransportFleetItem,
    pickup: String,
    drop: String,
    currentStep: Int,
    onAdvanceStep: () -> Unit,
    onBack: () -> Unit
) {
    val steps = listOf("Picked Up\n10:20 AM", "In Transit\n10:35 AM", "Arriving Soon\nETA 18m")

    Column(modifier = Modifier.fillMaxSize().background(TransportBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(TransportNavy).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SoftWhite)
            }
            Text("Live Tracking", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = onAdvanceStep, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.Refresh, "Refresh", tint = TransportGold)
            }
        }

        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            SimulatedMapCanvas(
                modifier = Modifier.fillMaxSize(),
                pickupLabel = pickup,
                dropLabel = drop,
                distanceKm = 14.2,
                etaMinutes = 18,
                driverProgress = -1f
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = SoftWhite,
                shadowElevation = 6.dp,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("ETA", style = MaterialTheme.typography.labelSmall, color = MutedText, fontWeight = FontWeight.Bold)
                    Text("18 min", style = MaterialTheme.typography.titleLarge, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        // Bottom Floating Driver & 3-Step Timeline Sheet
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = SoftWhite,
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Character3DPortrait(
                        drawableRes = R.drawable.img_char_porter_mover_1795426024294,
                        contentDescription = "Rajesh Singh",
                        borderColor = TransportRoyal,
                        size = 56.dp,
                        bobbing = false
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rajesh Singh", style = MaterialTheme.typography.titleMedium, color = TransportNavy, fontWeight = FontWeight.ExtraBold)
                        Text("★ 4.7 (1.2k trips)", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                        Text("${vehicle.name} • UP16BT8421", style = MaterialTheme.typography.labelSmall, color = MutedText)
                    }
                }

                val context = LocalContext.current
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    TransportCircleAction(Icons.Rounded.Call, "Call") {
                        DeviceActions.dialPhoneNumber(context, "+919876500112")
                    }
                    TransportCircleAction(Icons.Rounded.Chat, "Chat", onAdvanceStep)
                    TransportCircleAction(Icons.Rounded.Share, "Share") {
                        DeviceActions.shareText(
                            context,
                            "Take It Goods Transport Live Tracking",
                            "Tracking my ${vehicle.name} (UP16BT8421) with Driver Rajesh Singh from $pickup to $drop. ETA 18 min."
                        )
                    }
                }

                // 3-Step Blue Check Timeline
                Row(
                    modifier = Modifier.fillMaxWidth().clickable(onClick = onAdvanceStep),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    steps.forEachIndexed { idx, label ->
                        val active = idx <= currentStep
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (active) TransportRoyal else Color(0xFFCBD5E1)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Check, label, tint = SoftWhite, modifier = Modifier.size(16.dp))
                            }
                            Text(
                                label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (active) TransportNavy else MutedText,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                textAlign = TextAlign.Center,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TransportCircleAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).clip(CircleShape).background(Color(0xFFEFF6FF)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = TransportRoyal, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TransportNavy, fontWeight = FontWeight.Bold)
    }
}
