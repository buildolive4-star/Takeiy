package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DemoCatalog
import com.example.model.VehicleOption
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState
import kotlinx.coroutines.delay

private val RideDarkBg = Color(0xFF0A142D)
private val RideGoldYellow = Color(0xFFFFC107)
private val RideRoyalBlue = Color(0xFF2563EB)
private val RideGreen = Color(0xFF16A34A)

private enum class RideScreenStage {
    RIDE_HOME,
    SELECT_LOCATION,
    CHOOSE_VEHICLE,
    RIDE_IN_PROGRESS,
    RIDE_COMPLETED
}

@Composable
fun RideBookingScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onRecordRide: (title: String, subtitle: String, amount: Double, payment: String, pickup: String, drop: String, otp: String, driver: String, onCreated: (String) -> Unit) -> Unit,
    onCompleteRide: (String) -> Unit,
    onRateRide: (String, Int) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var stage by remember { mutableStateOf(RideScreenStage.RIDE_HOME) }
    var pickup by remember { mutableStateOf("Sector 62, Noida") }
    var destination by remember { mutableStateOf("Noida Sector 63") }
    var selectedVehicle by remember { mutableStateOf(DemoCatalog.rideVehicles.first()) }
    var showMoreVehicles by remember { mutableStateOf(false) }
    var showFareDetails by remember { mutableStateOf(false) }
    var showSosDialog by remember { mutableStateOf(false) }
    var activeOrderId by remember { mutableStateOf("") }
    var progressStep by remember { mutableIntStateOf(2) } // 0=Assigned, 1=Arriving, 2=On Ride, 3=Reached
    var userRating by remember { mutableIntStateOf(5) }

    val popularPlaces = remember {
        listOf(
            Triple("Home", "Sector 62, Noida", 3.5),
            Triple("Work", "Noida Sector 63 IT Park", 4.2),
            Triple("DLF Mall of India", "Sector 18, Noida", 5.1),
            Triple("Botanical Garden Metro", "Sector 37, Noida", 6.0)
        )
    }
    val distanceKm = popularPlaces.find { it.first == destination || it.second == destination }?.third ?: 3.5
    val etaMins = (distanceKm * 3.4).toInt().coerceAtLeast(10)
    val baseFare = selectedVehicle.baseFare
    val distFare = (distanceKm * selectedVehicle.perKmRate).toInt()
    val totalFare = (baseFare + distFare).coerceAtLeast(49)

    LaunchedEffect(stage) {
        if (stage == RideScreenStage.RIDE_IN_PROGRESS && activeOrderId.isEmpty()) {
            onRecordRide(
                "${selectedVehicle.name} • $destination",
                "${"%.1f".format(distanceKm)} km • $etaMins min • Rohit Kumar",
                totalFare.toDouble(),
                "UPI",
                pickup,
                destination,
                "4821",
                "Rohit Kumar"
            ) { id -> activeOrderId = id }
            delay(1800)
            progressStep = 2
        }
    }

    AnimatedContent(targetState = stage, label = "takeItRideStage") { current ->
        when (current) {
            // =========================================================================
            // SCREEN 2 OF IMAGE 2: RIDE HOME ("Where are you Going Today?", Bike/Auto/Cab, 50% OFF, Why Choose)
            // =========================================================================
            RideScreenStage.RIDE_HOME -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().background(SoftWhite),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    // Night City Skyline + 3D Yellow Scooter Captain Hero
                    item {
                        Box(modifier = Modifier.fillMaxWidth().height(270.dp)) {
                            Image(
                                painter = painterResource(R.drawable.img_takeit_ride_city_hero_1795429838137),
                                contentDescription = "Where are you Going Today",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0.0f to Color(0xBB0A142D),
                                            0.55f to Color(0x660A142D),
                                            1.0f to Color(0xCC0A142D)
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = onBack,
                                        modifier = Modifier.size(44.dp).clip(CircleShape).background(SoftWhite.copy(alpha = 0.14f)).testTag("back_button")
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SoftWhite)
                                    }
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.Rounded.LocationOn, null, tint = RideGoldYellow, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        text = "$pickup ∨",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = SoftWhite,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(onClick = { showSosDialog = true }) {
                                        Icon(Icons.Rounded.Shield, "Safety", tint = RideGoldYellow)
                                    }
                                }

                                Column(modifier = Modifier.padding(bottom = 26.dp, start = 4.dp)) {
                                    Text(
                                        text = "Where are you\nGoing Today?",
                                        style = MaterialTheme.typography.headlineLarge,
                                        color = SoftWhite,
                                        fontWeight = FontWeight.Black,
                                        lineHeight = 34.sp
                                    )
                                }
                            }
                        }
                    }

                    // Overlapping White Search Destination Pill
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = SoftWhite,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .offset(y = (-24).dp)
                                .clickable { stage = RideScreenStage.SELECT_LOCATION }
                                .testTag("ride_destination_input")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, null, tint = DeepNavy, modifier = Modifier.size(22.dp))
                                Spacer(Modifier.width(10.dp))
                                Text("Search destination...", style = MaterialTheme.typography.bodyLarge, color = MutedText)
                            }
                        }
                    }

                    // 3-Column Quick Ride Cards (Bike ₹49, Auto ₹79, Cab ₹99)
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .offset(y = (-8).dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                Triple(DemoCatalog.rideVehicles[0], "Bike", "₹49 • 2 min"),
                                Triple(DemoCatalog.rideVehicles[1], "Auto", "₹79 • 3 min"),
                                Triple(DemoCatalog.rideVehicles[3], "Cab", "₹99 • 4 min")
                            ).forEach { (veh, title, meta) ->
                                val icon = when (veh.id) {
                                    "bike" -> Icons.Rounded.TwoWheeler
                                    "auto" -> Icons.Rounded.ElectricRickshaw
                                    else -> Icons.Rounded.DirectionsCar
                                }
                                Surface(
                                    shape = RoundedCornerShape(18.dp),
                                    color = SoftWhite,
                                    shadowElevation = 4.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                                        .clickable {
                                            selectedVehicle = veh
                                            stage = RideScreenStage.CHOOSE_VEHICLE
                                        }
                                        .testTag("quick_ride_${veh.id}")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(50.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(Color(0xFFFEF9C3)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(icon, title, tint = DeepNavy, modifier = Modifier.size(30.dp))
                                        }
                                        Text(title, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                        Text(meta, style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    }
                                }
                            }
                        }
                    }

                    // 3 Saved Place Shortcut Boxes (Home | Work | + Add)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            listOf(
                                Triple("Home", Icons.Rounded.Home, "Sector 62, Noida"),
                                Triple("Work", Icons.Rounded.WorkOutline, "Noida Sector 63"),
                                Triple("Add", Icons.Rounded.AddCircleOutline, "DLF Mall of India")
                            ).forEach { (lbl, ic, dest) ->
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color(0xFFF8FAFC),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                        .clickable {
                                            destination = dest
                                            stage = RideScreenStage.CHOOSE_VEHICLE
                                        }
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(3.dp)
                                    ) {
                                        Icon(ic, lbl, tint = DeepNavy, modifier = Modifier.size(20.dp))
                                        Text(lbl, style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    // Golden Yellow "Flat 50% OFF on First Ride" Card (Exact Image 2 Screen 2!)
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFDE047), RideGoldYellow, Color(0xFFF59E0B))))
                                .clickable { stage = RideScreenStage.CHOOSE_VEHICLE }
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Flat 50% OFF", style = MaterialTheme.typography.headlineSmall, color = DeepNavy, fontWeight = FontWeight.Black)
                                    Text("on First Ride", style = MaterialTheme.typography.titleSmall, color = Color(0xFF451A03), fontWeight = FontWeight.Bold)
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(RideDarkBg)
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Book Now →", style = MaterialTheme.typography.labelMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                                Box(
                                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(SoftWhite.copy(alpha = 0.25f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.CardGiftcard, null, tint = Color(0xFFDC2626), modifier = Modifier.size(40.dp))
                                }
                            }
                        }
                    }

                    // Why Choose Take It Rides? 4-Badge Trust Strip
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Why Choose Take It Rides?", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                listOf(
                                    Triple("Fast\nService", Icons.Rounded.Speed, Color(0xFFEF4444)),
                                    Triple("Affordable\nFares", Icons.Rounded.Savings, RideGreen),
                                    Triple("Live\nTracking", Icons.Rounded.GpsFixed, RideRoyalBlue),
                                    Triple("24x7\nSupport", Icons.Rounded.SupportAgent, Color(0xFF0F766E))
                                ).forEach { (lbl, ic, tint) ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(CircleShape).background(tint.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(ic, lbl, tint = tint, modifier = Modifier.size(22.dp))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(lbl, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = DeepNavy, textAlign = TextAlign.Center, lineHeight = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SCREEN 3 OF IMAGE 2: SELECT LOCATION
            // =========================================================================
            RideScreenStage.SELECT_LOCATION -> {
                var query by remember { mutableStateOf("") }
                Column(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { stage = RideScreenStage.RIDE_HOME }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                        }
                        Text("Select Location", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Spacer(Modifier.size(48.dp))
                    }
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("Search for a place...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MutedText) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxWidth().height(210.dp),
                        pickupLabel = pickup,
                        dropLabel = destination,
                        distanceKm = distanceKm,
                        etaMinutes = etaMins,
                        driverProgress = -1f
                    )
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEFF6FF)) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.MyLocation, null, tint = RideRoyalBlue)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Current Location", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                        Text(pickup, style = MaterialTheme.typography.bodySmall, color = MutedText)
                                    }
                                }
                            }
                        }
                        item {
                            Text("Popular Places", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        }
                        items(popularPlaces.filter { query.isBlank() || it.first.contains(query, true) || it.second.contains(query, true) }) { (name, addr, _) ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (destination == name) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, if (destination == name) RideRoyalBlue else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                    .clickable { destination = name }
                            ) {
                                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.LocationOn, null, tint = RideRoyalBlue)
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text(name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                                        Text(addr, style = MaterialTheme.typography.bodySmall, color = MutedText)
                                    }
                                }
                            }
                        }
                    }
                    Button(
                        onClick = { stage = RideScreenStage.CHOOSE_VEHICLE },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = RideRoyalBlue, contentColor = SoftWhite)
                    ) {
                        Text("Confirm Location", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // =========================================================================
            // SCREEN 4 OF IMAGE 2: CHOOSE VEHICLE (Dark header, Map 12 min 3.5 km, Blue-selected card, Gold Confirm Booking)
            // =========================================================================
            RideScreenStage.CHOOSE_VEHICLE -> {
                val vehiclesList = if (showMoreVehicles) DemoCatalog.rideVehicles else DemoCatalog.rideVehicles.take(4)
                Column(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RideDarkBg)
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { stage = RideScreenStage.RIDE_HOME }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SoftWhite)
                        }
                        Text("Choose Vehicle", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Spacer(Modifier.size(48.dp))
                    }

                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxWidth().height(215.dp),
                        pickupLabel = pickup,
                        dropLabel = destination,
                        distanceKm = distanceKm,
                        etaMinutes = etaMins,
                        driverProgress = -1f
                    )

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(vehiclesList, key = { it.id }) { v ->
                            val isSel = v.id == selectedVehicle.id
                            val fare = (v.baseFare + (distanceKm * v.perKmRate).toInt()).coerceAtLeast(49)
                            val icon = when (v.id) {
                                "bike" -> Icons.Rounded.TwoWheeler
                                "auto", "auto_xl" -> Icons.Rounded.ElectricRickshaw
                                "suv" -> Icons.Rounded.AirportShuttle
                                else -> Icons.Rounded.DirectionsCar
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isSel) Color(0xFFEFF6FF) else SoftWhite)
                                    .border(if (isSel) 2.dp else 1.dp, if (isSel) RideRoyalBlue else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                    .clickable { selectedVehicle = v }
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("vehicle_option_${v.id}"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFFEF9C3)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(icon, v.name, tint = DeepNavy, modifier = Modifier.size(28.dp))
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(v.name, style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    Text("${etaMins + v.etaMin} min • ${"%.1f".format(distanceKm)} km", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                Text("₹$fare", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.Black)
                                if (isSel) {
                                    Spacer(Modifier.width(8.dp))
                                    Box(Modifier.size(22.dp).clip(CircleShape).background(RideRoyalBlue), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Check, null, tint = SoftWhite, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }

                        item {
                            Text(
                                text = if (showMoreVehicles) "Show Less ▲" else "View More ∨",
                                style = MaterialTheme.typography.labelLarge,
                                color = DeepNavy,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth().clickable { showMoreVehicles = !showMoreVehicles }.padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    Button(
                        onClick = { stage = RideScreenStage.RIDE_IN_PROGRESS },
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .height(54.dp)
                            .testTag("confirm_ride_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = RideGoldYellow, contentColor = DeepNavy)
                    ) {
                        Text("Confirm Booking • ₹$totalFare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }

            // =========================================================================
            // SCREEN 5 OF IMAGE 2: RIDE IN PROGRESS
            // =========================================================================
            RideScreenStage.RIDE_IN_PROGRESS -> {
                val steps = listOf("Assigned", "Arriving", "On Ride", "Reached")
                Column(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(RideDarkBg)
                            .statusBarsPadding()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { stage = RideScreenStage.CHOOSE_VEHICLE }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SoftWhite)
                        }
                        Text("Ride in Progress", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        TextButton(onClick = { onShowMessage("Take It 24x7 Ride Help Desk ready") }) {
                            Text("Help", color = SoftWhite, fontWeight = FontWeight.Bold)
                        }
                    }

                    Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
                        SimulatedMapCanvas(
                            modifier = Modifier.fillMaxSize(),
                            pickupLabel = pickup,
                            dropLabel = destination,
                            distanceKm = 1.2,
                            etaMinutes = 3,
                            driverProgress = (progressStep + 1) * 0.24f
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = SoftWhite,
                            shadowElevation = 6.dp,
                            modifier = Modifier.align(Alignment.TopStart).padding(14.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Arriving in", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                Text("3 min", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.Black)
                                Text("(1.2 km)", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
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
                                    drawableRes = R.drawable.img_char_ride_captain_1795426023899,
                                    contentDescription = "Rohit Kumar",
                                    borderColor = RideGoldYellow,
                                    size = 56.dp,
                                    bobbing = false
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Rohit Kumar ★ 4.8", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    Text("DL01AB1234 • Hero Splendor", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                    Text("Ride PIN: 4821", style = MaterialTheme.typography.labelSmall, color = RideRoyalBlue, fontWeight = FontWeight.Bold)
                                }
                                IconButton(
                                    onClick = { onShowMessage("Calling Captain Rohit Kumar...") },
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEFF6FF))
                                ) { Icon(Icons.Rounded.Call, "Call", tint = RideRoyalBlue, modifier = Modifier.size(20.dp)) }
                                Spacer(Modifier.width(8.dp))
                                IconButton(
                                    onClick = { onShowMessage("Chatting with Captain Rohit Kumar...") },
                                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFEFF6FF))
                                ) { Icon(Icons.Rounded.Chat, "Chat", tint = RideRoyalBlue, modifier = Modifier.size(20.dp)) }
                            }

                            // 4-Step Green Check Bar
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                                steps.forEachIndexed { idx, lbl ->
                                    val done = idx <= progressStep
                                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                            Box(Modifier.weight(1f).height(2.5.dp).background(if (idx == 0) Color.Transparent else if (done) RideGreen else Color(0xFFE2E8F0)))
                                            Box(Modifier.size(22.dp).clip(CircleShape).background(if (done) RideGreen else Color(0xFFCBD5E1)), contentAlignment = Alignment.Center) {
                                                Icon(Icons.Rounded.Check, null, tint = SoftWhite, modifier = Modifier.size(13.dp))
                                            }
                                            Box(Modifier.weight(1f).height(2.5.dp).background(if (idx == steps.lastIndex) Color.Transparent else if (idx < progressStep) RideGreen else Color(0xFFE2E8F0)))
                                        }
                                        Spacer(Modifier.height(4.dp))
                                        Text(lbl, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (done) DeepNavy else MutedText, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedButton(
                                    onClick = { onShowMessage("Live ride link shared with ${uiState.appState.emergencyContactName}") },
                                    modifier = Modifier.weight(1f).height(46.dp),
                                    shape = RoundedCornerShape(50)
                                ) { Text("Share Ride", color = DeepNavy, fontWeight = FontWeight.Bold) }
                                Button(
                                    onClick = { showSosDialog = true },
                                    modifier = Modifier.weight(1f).height(46.dp).testTag("ride_sos_button"),
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = SoftWhite)
                                ) { Text("SOS", fontWeight = FontWeight.ExtraBold) }
                            }

                            Button(
                                onClick = {
                                    if (activeOrderId.isNotEmpty()) onCompleteRide(activeOrderId)
                                    stage = RideScreenStage.RIDE_COMPLETED
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("complete_ride_button"),
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(containerColor = RideGreen, contentColor = SoftWhite)
                            ) {
                                Text("Complete Ride & Pay ₹$totalFare", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // SCREEN 6 OF IMAGE 2: RIDE COMPLETED!
            // =========================================================================
            RideScreenStage.RIDE_COMPLETED -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to RideDarkBg,
                                0.34f to Color(0xFFE2E8F0),
                                1.0f to SoftWhite
                            )
                        )
                        .statusBarsPadding()
                        .navigationBarsPadding()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(86.dp)
                            .shadow(14.dp, CircleShape, spotColor = RideGreen)
                            .clip(CircleShape)
                            .background(RideGreen)
                            .border(4.dp, SoftWhite, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Check, "Completed", tint = SoftWhite, modifier = Modifier.size(48.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Ride Completed!", style = MaterialTheme.typography.headlineLarge, color = DeepNavy, fontWeight = FontWeight.Black)
                    Text("Thank you for riding with Take It", style = MaterialTheme.typography.bodyMedium, color = SlateText)

                    Spacer(Modifier.height(18.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Total Fare", style = MaterialTheme.typography.titleSmall, color = MutedText)
                            Text("₹$totalFare", style = MaterialTheme.typography.displaySmall, color = DeepNavy, fontWeight = FontWeight.Black)
                            Text(
                                if (showFareDetails) "Hide Details ▲" else "View Details →",
                                style = MaterialTheme.typography.labelLarge,
                                color = RideRoyalBlue,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.clickable { showFareDetails = !showFareDetails }
                            )
                            if (showFareDetails) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                                Text("Base Fare: ₹$baseFare  •  Distance (${"%.1f".format(distanceKm)} km): ₹$distFare", style = MaterialTheme.typography.bodySmall, color = SlateText)
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 4.dp, modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Rate Your Ride", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                for (s in 1..5) {
                                    IconButton(onClick = { userRating = s }) {
                                        Icon(
                                            if (s <= userRating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                            "Rate $s",
                                            tint = RideGoldYellow,
                                            modifier = Modifier.size(34.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = {
                            if (activeOrderId.isNotEmpty()) onRateRide(activeOrderId, userRating)
                            onBack()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_ride_rating_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = RideGoldYellow, contentColor = DeepNavy)
                    ) { Text("Submit Rating", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold) }
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onShowMessage("Ride Receipt downloaded (₹$totalFare)") },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(50)
                    ) { Text("Download Receipt", color = DeepNavy, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            containerColor = RideDarkBg,
            titleContentColor = CoralDanger,
            textContentColor = SoftWhite,
            title = { Text("24x7 Safety SOS Shield") },
            text = { Text("Simulated SOS alert ready for ${uiState.appState.emergencyContactName} (${uiState.appState.emergencyContactPhone}).") },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        onShowMessage("Simulated SOS alert sent to ${uiState.appState.emergencyContactName}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralDanger)
                ) { Text("Trigger SOS") }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) { Text("Close", color = RideGoldYellow) }
            }
        )
    }
}
