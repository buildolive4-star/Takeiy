package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.DemoCatalog
import com.example.model.VehicleOption
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState
import kotlinx.coroutines.delay

private enum class RidePhase {
    SELECT_ROUTE_VEHICLE,
    MATCHING_DRIVER,
    DRIVER_ASSIGNED_TRACKING,
    COMPLETED_SUMMARY
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
    var phase by remember { mutableStateOf(RidePhase.SELECT_ROUTE_VEHICLE) }
    var pickup by remember { mutableStateOf(uiState.appState.activeAddressLabel + " • Indiranagar") }
    var destination by remember { mutableStateOf("Koramangala 4th Block") }
    var selectedVehicle by remember { mutableStateOf(DemoCatalog.rideVehicles.first()) }
    var selectedPayment by remember { mutableStateOf("UPI") }
    var otpInput by remember { mutableStateOf("") }
    val rideOtp = remember { "4821" }
    var activeOrderId by remember { mutableStateOf("") }
    var driverProgress by remember { mutableFloatStateOf(0.15f) }
    var userRating by remember { mutableIntStateOf(5) }
    var reviewText by remember { mutableStateOf("") }
    var showSosDialog by remember { mutableStateOf(false) }

    val quickDestinations = listOf(
        "Koramangala 4th Block" to 5.4,
        "MG Road Metro" to 4.1,
        "Bagmane Tech Park" to 6.8,
        "HSR Layout Sector 2" to 8.2,
        "Kempegowda Airport T2" to 32.5
    )
    val distanceKm = quickDestinations.find { it.first == destination }?.second ?: 6.2
    val etaMin = (distanceKm * 2.4).toInt().coerceAtLeast(8)
    val estimatedFare = selectedVehicle.baseFare + (distanceKm * selectedVehicle.perKmRate).toInt()

    // Simulated driver matching & live movement
    LaunchedEffect(phase) {
        when (phase) {
            RidePhase.MATCHING_DRIVER -> {
                delay(2200)
                onRecordRide(
                    "${selectedVehicle.name} • $destination",
                    "${"%.1f".format(distanceKm)} km • $etaMin mins • Captain Arjun Kumar",
                    estimatedFare.toDouble(),
                    selectedPayment,
                    pickup,
                    destination,
                    rideOtp,
                    "Arjun Kumar"
                ) { id -> activeOrderId = id }
                phase = RidePhase.DRIVER_ASSIGNED_TRACKING
            }
            RidePhase.DRIVER_ASSIGNED_TRACKING -> {
                driverProgress = 0.12f
                while (driverProgress < 0.92f && phase == RidePhase.DRIVER_ASSIGNED_TRACKING) {
                    delay(450)
                    driverProgress = (driverProgress + 0.04f).coerceAtMost(0.92f)
                }
            }
            else -> Unit
        }
    }

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = {
            ZiporaSubTopBar(
                title = "Zipora Ride",
                subtitle = "Bike Taxi • Auto • Mini • Sedan • SUV",
                onBack = onBack,
                trailing = {
                    if (phase == RidePhase.DRIVER_ASSIGNED_TRACKING) {
                        FilledTonalButton(
                            onClick = { showSosDialog = true },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CoralDanger, contentColor = SoftWhite),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            modifier = Modifier.testTag("ride_sos_button")
                        ) {
                            Icon(Icons.Rounded.Warning, contentDescription = "SOS", modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("SOS", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Simulated Map
            item {
                SimulatedMapCanvas(
                    modifier = Modifier.fillMaxWidth().height(235.dp),
                    pickupLabel = pickup,
                    dropLabel = destination,
                    distanceKm = distanceKm,
                    etaMinutes = etaMin,
                    driverProgress = if (phase == RidePhase.DRIVER_ASSIGNED_TRACKING) driverProgress else -1f,
                    showMovingDriver = phase != RidePhase.MATCHING_DRIVER,
                    showMatchingRadar = phase == RidePhase.MATCHING_DRIVER
                )
            }

            item {
                AnimatedContent(targetState = phase, label = "ridePhase") { currentPhase ->
                    when (currentPhase) {
                        RidePhase.SELECT_ROUTE_VEHICLE -> {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                // Route Inputs Card
                                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 3.dp) {
                                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = pickup,
                                            onValueChange = { pickup = it },
                                            label = { Text("Pickup Location") },
                                            leadingIcon = { Icon(Icons.Rounded.TripOrigin, null, tint = SkyBlueDark) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("ride_pickup_input")
                                        )
                                        OutlinedTextField(
                                            value = destination,
                                            onValueChange = { destination = it },
                                            label = { Text("Where to?") },
                                            leadingIcon = { Icon(Icons.Rounded.LocationOn, null, tint = CoralDanger) },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("ride_destination_input")
                                        )
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(quickDestinations) { (place, km) ->
                                                FilterChip(
                                                    selected = destination == place,
                                                    onClick = { destination = place },
                                                    label = { Text("$place ($km km)", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Vehicle Categories
                                Text("Choose Your Ride", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    DemoCatalog.rideVehicles.forEach { vehicle ->
                                        val isSelected = selectedVehicle.id == vehicle.id
                                        val fare = vehicle.baseFare + (distanceKm * vehicle.perKmRate).toInt()
                                        VehicleRowCard(
                                            vehicle = vehicle,
                                            fare = fare,
                                            selected = isSelected,
                                            onClick = { selectedVehicle = vehicle }
                                        )
                                    }
                                }

                                // Payment Method Selector
                                Text("Payment Method", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    val methods = listOf("UPI", "Zipora Wallet", "Credit Card", "Cash")
                                    items(methods) { method ->
                                        FilterChip(
                                            selected = selectedPayment == method,
                                            onClick = { selectedPayment = method },
                                            label = { Text(method, fontWeight = FontWeight.SemiBold) }
                                        )
                                    }
                                }

                                // Confirm Booking Button
                                Button(
                                    onClick = { phase = RidePhase.MATCHING_DRIVER },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(56.dp)
                                        .testTag("confirm_ride_button"),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)
                                ) {
                                    Text(
                                        "Confirm ${selectedVehicle.name} • ₹$estimatedFare",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        RidePhase.MATCHING_DRIVER -> {
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = DeepNavy,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(color = PremiumYellow, strokeWidth = 4.dp)
                                    Text("Matching Nearest Captain...", style = MaterialTheme.typography.headlineMedium, color = SoftWhite)
                                    Text(
                                        "Broadcasting your ${selectedVehicle.name} trip (${"%.1f".format(distanceKm)} km) to verified captains nearby.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = SkyBlue
                                    )
                                }
                            }
                        }

                        RidePhase.DRIVER_ASSIGNED_TRACKING -> {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                // Captain Details Card
                                Surface(shape = RoundedCornerShape(22.dp), color = DeepNavy) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(CircleShape)
                                                    .background(PremiumYellow),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text("AK", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                            }
                                            Spacer(Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Captain Arjun Kumar", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                                Text("★ 4.92 • KA-01-EQ-4920 • ${selectedVehicle.name}", style = MaterialTheme.typography.bodySmall, color = SkyBlue)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("OTP", style = MaterialTheme.typography.labelSmall, color = Color(0xFF9FB3C8))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(PremiumYellow)
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(rideOtp, style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                                }
                                            }
                                        }

                                        // Quick Actions: Call, Share Trip, Emergency Contact
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            OutlinedButton(
                                                onClick = { onShowMessage("Simulated call to Captain Arjun (+91 98*** **420)") },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SoftWhite)
                                            ) {
                                                Icon(Icons.Rounded.Call, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Call", style = MaterialTheme.typography.labelMedium)
                                            }
                                            OutlinedButton(
                                                onClick = { onShowMessage("Live trip link shared with ${uiState.appState.emergencyContactName}") },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SkyBlue)
                                            ) {
                                                Icon(Icons.Rounded.ShareLocation, null, modifier = Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Share Trip", style = MaterialTheme.typography.labelMedium)
                                            }
                                        }
                                    }
                                }

                                // Complete Ride with OTP Verification
                                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 3.dp) {
                                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                        Text("Live Trip Verification", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                        Text("Enter OTP $rideOtp to verify pickup and simulate ride completion.", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                        OutlinedTextField(
                                            value = otpInput,
                                            onValueChange = { otpInput = it },
                                            label = { Text("Enter 4-digit Ride OTP ($rideOtp)") },
                                            singleLine = true,
                                            modifier = Modifier.fillMaxWidth().testTag("ride_otp_input")
                                        )
                                        Button(
                                            onClick = {
                                                if (otpInput.isBlank() || otpInput == rideOtp) {
                                                    if (activeOrderId.isNotEmpty()) onCompleteRide(activeOrderId)
                                                    phase = RidePhase.COMPLETED_SUMMARY
                                                } else {
                                                    onShowMessage("Incorrect OTP. Use $rideOtp for this demo trip.")
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("complete_ride_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite),
                                            shape = RoundedCornerShape(14.dp)
                                        ) {
                                            Icon(Icons.Rounded.CheckCircle, null)
                                            Spacer(Modifier.width(8.dp))
                                            Text("Verify OTP & Complete Ride", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        RidePhase.COMPLETED_SUMMARY -> {
                            Surface(shape = RoundedCornerShape(22.dp), color = SoftWhite, shadowElevation = 4.dp) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    IconOrb3D(Icons.Rounded.CheckCircle, SoftWhite, SuccessGreen, Color(0xFF15803D), size = 60.dp, iconSize = 32.dp)
                                    Text("Ride Completed!", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                                    Text("Paid ₹$estimatedFare via $selectedPayment (Simulated)", style = MaterialTheme.typography.titleSmall, color = SuccessGreen)
                                    HorizontalDivider(color = LightBorder)
                                    Text("Rate Captain Arjun Kumar", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        for (star in 1..5) {
                                            IconButton(onClick = { userRating = star }) {
                                                Icon(
                                                    if (star <= userRating) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                                    contentDescription = "Rate $star stars",
                                                    tint = PremiumYellowDark,
                                                    modifier = Modifier.size(34.dp)
                                                )
                                            }
                                        }
                                    }
                                    OutlinedTextField(
                                        value = reviewText,
                                        onValueChange = { reviewText = it },
                                        label = { Text("Leave a quick compliment (optional)") },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    Button(
                                        onClick = {
                                            if (activeOrderId.isNotEmpty()) onRateRide(activeOrderId, userRating)
                                            onBack()
                                        },
                                        modifier = Modifier.fillMaxWidth().height(52.dp).testTag("submit_ride_rating_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                                        shape = RoundedCornerShape(14.dp)
                                    ) {
                                        Text("Submit Rating & Finish", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSosDialog) {
        AlertDialog(
            onDismissRequest = { showSosDialog = false },
            containerColor = DeepNavy,
            titleContentColor = CoralDanger,
            textContentColor = SoftWhite,
            title = { Text("Emergency SOS Shield") },
            text = {
                Text(
                    "Simulated Safety Alert ready:\n• Emergency Contact: ${uiState.appState.emergencyContactName} (${uiState.appState.emergencyContactPhone})\n• Zipora 24x7 Safety Desk notified with live route."
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSosDialog = false
                        onShowMessage("Simulated SOS alert dispatched to ${uiState.appState.emergencyContactName}")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CoralDanger)
                ) { Text("Trigger Demo Alert") }
            },
            dismissButton = {
                TextButton(onClick = { showSosDialog = false }) { Text("Close", color = SkyBlue) }
            }
        )
    }
}

@Composable
private fun VehicleRowCard(
    vehicle: VehicleOption,
    fare: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (vehicle.id) {
        "bike" -> Icons.Rounded.TwoWheeler
        "auto" -> Icons.Rounded.ElectricRickshaw
        "suv" -> Icons.Rounded.AirportShuttle
        else -> Icons.Rounded.DirectionsCar
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) RideCardStart else SoftWhite)
            .border(if (selected) 2.dp else 1.dp, if (selected) SkyBlueDark else LightBorder, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(14.dp)
            .testTag("vehicle_option_${vehicle.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconOrb3D(icon, if (selected) DeepNavy else SoftWhite, if (selected) PremiumYellow else SkyBlue, if (selected) OrangeAccent else DeepNavy, size = 46.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(vehicle.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                if (vehicle.badge.isNotEmpty()) {
                    Spacer(Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DeepNavy)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(vehicle.badge, style = MaterialTheme.typography.labelSmall, color = PremiumYellow)
                    }
                }
            }
            Text("${vehicle.tagline} • ${vehicle.etaMin} min away • ${vehicle.capacity}", style = MaterialTheme.typography.bodySmall, color = SlateText)
        }
        Text("₹$fare", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
    }
}
