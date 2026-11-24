package com.example.takeit.ui.admin

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.components.*
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel

@Composable
fun AdminMobilityTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    val context = LocalContext.current
    var activeSubSection by remember { mutableStateOf("VEHICLES") } // VEHICLES, CAPTAINS, WITHDRAWALS, ANALYTICS
    var verticalFilter by remember { mutableStateOf("ALL") } // ALL, RIDE, TRANSPORT
    var editingVehicle by remember { mutableStateOf<MobilityVehicleCategory?>(null) }
    var showVehicleDialog by remember { mutableStateOf(false) }

    val sosRides = remember(uiState.allRides) {
        uiState.allRides.filter { it.sosActive }
    }
    val pendingCaptains = remember(uiState.allCaptains) {
        uiState.allCaptains.count { it.approvalStatus == "PENDING" }
    }
    val pendingWithdrawals = remember(uiState.allWithdrawals) {
        uiState.allWithdrawals.count { it.status == "PENDING" }
    }
    val filteredVehicles = remember(uiState.mobilityVehicles, verticalFilter) {
        if (verticalFilter == "ALL") uiState.mobilityVehicles
        else uiState.mobilityVehicles.filter { it.vertical.equals(verticalFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // LIVE SOS ALERT BANNER (Priority 0)
        if (sosRides.isNotEmpty()) {
            items(sosRides, key = { "sos_${it.id}" }) { ride ->
                TakeIt3DCard(
                    containerColor = Color(0xFF450A0A),
                    depthColor = Color.Black,
                    borderColor = AdminCrimson,
                    cornerRadius = 20.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.Warning, "SOS Alert", tint = AdminCrimson)
                            Text("LIVE SOS ALERT • ${ride.rideNumber}", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        }
                        Text(
                            "Rider: ${ride.customerName} (${ride.customerPhone.ifBlank { "No phone" }})\nCaptain: ${ride.captainName} (${ride.vehicleNumber.ifBlank { ride.vehicleType }})\nRoute: ${ride.pickupAddress} → ${ride.dropAddress}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFECACA)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { dialPhone(context, ride.customerPhone.ifBlank { uiState.config.sosNumber }) },
                                colors = ButtonDefaults.buttonColors(containerColor = AdminCrimson),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Call, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Call Rider", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { viewModel.triggerRideSos(ride.id, false) },
                                border = BorderStroke(1.dp, AdminGold),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Resolve SOS", color = AdminGold, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Sub-navigation Pills
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val sections = listOf(
                    "VEHICLES" to "Fleet Vehicles (${uiState.mobilityVehicles.size})",
                    "CAPTAINS" to "Captain KYC ($pendingCaptains)",
                    "WITHDRAWALS" to "Payouts ($pendingWithdrawals)",
                    "ANALYTICS" to "Revenue Charts"
                )
                items(sections) { (key, label) ->
                    val selected = activeSubSection == key
                    FilterChip(
                        selected = selected,
                        onClick = { activeSubSection = key },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdminGold,
                            selectedLabelColor = AdminBg,
                            containerColor = AdminSurface,
                            labelColor = AdminInk
                        ),
                        modifier = Modifier.testTag("admin_fleet_sub_${key.lowercase()}")
                    )
                }
            }
        }

        when (activeSubSection) {
            "VEHICLES" -> {
                item {
                    TakeIt3DCard(
                        containerColor = AdminSurface,
                        depthColor = Color(0xFF060A15),
                        borderColor = AdminGold.copy(alpha = 0.5f),
                        cornerRadius = 20.dp,
                        depth = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Dynamic Fleet Categories & Pricing", color = AdminGold, fontWeight = FontWeight.ExtraBold)
                                    Text("Live-synced to RideX & MoveX apps", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    if (uiState.mobilityVehicles.isEmpty()) {
                                        FilledTonalButton(
                                            onClick = { viewModel.seedDefaultMobilityVehicles() },
                                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = AdminCard, contentColor = AdminCyan),
                                            modifier = Modifier.testTag("admin_seed_vehicles_btn")
                                        ) {
                                            Text("Seed Defaults", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    }
                                    TakeIt3DButton(
                                        onClick = {
                                            editingVehicle = null
                                            showVehicleDialog = true
                                        },
                                        containerColor = AdminGold,
                                        depthColor = AdminGold.darken3D(0.4f),
                                        contentColor = AdminBg,
                                        cornerRadius = 12.dp,
                                        depth = 3.dp,
                                        height = 38.dp,
                                        modifier = Modifier
                                            .width(120.dp)
                                            .testTag("admin_add_vehicle_btn")
                                    ) {
                                        Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Add Vehicle", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                    }
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("ALL", "RIDE", "TRANSPORT").forEach { v ->
                                    FilterChip(
                                        selected = verticalFilter == v,
                                        onClick = { verticalFilter = v },
                                        label = { Text(v, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }
                        }
                    }
                }

                if (filteredVehicles.isEmpty()) {
                    item {
                        TakeIt3DCard(
                            containerColor = AdminSurface,
                            depthColor = Color(0xFF060A15),
                            borderColor = AdminBorder,
                            cornerRadius = 18.dp,
                            depth = 3.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("No fleet vehicles configured yet", color = AdminInk, fontWeight = FontWeight.Bold)
                                Button(
                                    onClick = { viewModel.seedDefaultMobilityVehicles() },
                                    colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
                                ) {
                                    Text("Seed 12 Default RideX & MoveX Vehicles", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                } else {
                    items(filteredVehicles, key = { it.id }) { veh ->
                        val isTransport = veh.vertical.equals("TRANSPORT", ignoreCase = true)
                        val visualUrl = veh.imageUrl.ifBlank { TakeItFreeMedia.resolveVehicleMediaUrl(veh.code, isTransport) }
                        TakeIt3DCard(
                            containerColor = AdminSurface,
                            depthColor = Color(0xFF060A15),
                            borderColor = if (veh.activeFlag) AdminBorder else AdminCrimson.copy(alpha = 0.5f),
                            cornerRadius = 18.dp,
                            depth = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color.White.copy(alpha = 0.92f))
                                        .padding(6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TakeItMediaVisual(
                                        imageUrl = visualUrl,
                                        key = veh.code,
                                        contentDescription = veh.name,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(veh.name, color = AdminInk, fontWeight = FontWeight.ExtraBold)
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (isTransport) Color(0xFF451A03) else Color(0xFF0C2D48)
                                        ) {
                                            Text(
                                                veh.vertical,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isTransport) TransportOrange else AdminCyan,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        "Base ₹${veh.baseFare} • ₹${veh.perKmRate}/km • ₹${veh.perMinRate}/min • Min ₹${veh.minFare}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdminGold
                                    )
                                    Text(
                                        "${veh.capacityLabel} • ETA ${veh.etaMinutes}m • Comm ${veh.commissionPercent}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdminMuted
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Switch(
                                        checked = veh.activeFlag,
                                        onCheckedChange = { viewModel.saveMobilityVehicle(veh.copy(activeFlag = it)) }
                                    )
                                    Row {
                                        IconButton(onClick = {
                                            editingVehicle = veh
                                            showVehicleDialog = true
                                        }) {
                                            Icon(Icons.Rounded.Edit, "Edit Vehicle", tint = AdminCyan)
                                        }
                                        IconButton(onClick = { viewModel.deleteMobilityVehicle(veh.id) }) {
                                            Icon(Icons.Rounded.DeleteOutline, "Delete Vehicle", tint = AdminCrimson)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "CAPTAINS" -> {
                if (uiState.allCaptains.isEmpty()) {
                    item {
                        TakeIt3DCard(
                            containerColor = AdminSurface,
                            depthColor = Color(0xFF060A15),
                            borderColor = AdminBorder,
                            cornerRadius = 18.dp,
                            depth = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Badge, null, tint = AdminMuted, modifier = Modifier.size(40.dp))
                                Text("No Captain KYC applications submitted yet", color = AdminInk, fontWeight = FontWeight.Bold)
                                Text("Captains can submit Licence, Vehicle & Bank details in the Captain app KYC tab.", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                            }
                        }
                    }
                } else {
                    items(uiState.allCaptains, key = { it.id }) { cap ->
                        val statusColor = when (cap.approvalStatus) {
                            "APPROVED" -> AdminEmerald
                            "REJECTED", "SUSPENDED" -> AdminCrimson
                            else -> AdminGold
                        }
                        TakeIt3DCard(
                            containerColor = AdminSurface,
                            depthColor = Color(0xFF060A15),
                            borderColor = statusColor.copy(alpha = 0.55f),
                            cornerRadius = 20.dp,
                            depth = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(cap.fullName.ifBlank { cap.email }, color = AdminInk, fontWeight = FontWeight.ExtraBold)
                                        Text("${cap.vehicleCategory} • ${cap.vehicleModel} (${cap.vehicleNumber})", style = MaterialTheme.typography.labelSmall, color = AdminCyan)
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = statusColor.copy(alpha = 0.18f)) {
                                        Text(cap.approvalStatus, color = statusColor, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                Text(
                                    "DL: ${cap.licenseNumber} • Aadhaar: XXXX-${cap.aadhaarLast4} • Phone: ${cap.phone}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdminMuted
                                )
                                if (cap.licensePhotoUrl.isNotBlank() || cap.vehiclePhotoUrl.isNotBlank()) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (cap.licensePhotoUrl.isNotBlank()) {
                                            TakeItMediaVisual(
                                                imageUrl = cap.licensePhotoUrl,
                                                key = "dl",
                                                contentDescription = "Driver Licence Photo",
                                                modifier = Modifier
                                                    .size(width = 110.dp, height = 66.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                        if (cap.vehiclePhotoUrl.isNotBlank()) {
                                            TakeItMediaVisual(
                                                imageUrl = cap.vehiclePhotoUrl,
                                                key = cap.vehicleCategory,
                                                contentDescription = "Captain Vehicle Photo",
                                                modifier = Modifier
                                                    .size(width = 110.dp, height = 66.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { viewModel.adminUpdateCaptainStatus(cap.userId.ifBlank { cap.id }, "APPROVED") },
                                        colors = ButtonDefaults.buttonColors(containerColor = AdminEmerald, contentColor = AdminBg),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Approve", fontWeight = FontWeight.ExtraBold) }
                                    OutlinedButton(
                                        onClick = { viewModel.adminUpdateCaptainStatus(cap.userId.ifBlank { cap.id }, "SUSPENDED") },
                                        border = BorderStroke(1.dp, AdminCrimson),
                                        modifier = Modifier.weight(1f)
                                    ) { Text("Suspend", color = AdminCrimson, fontWeight = FontWeight.Bold) }
                                }
                            }
                        }
                    }
                }
            }

            "WITHDRAWALS" -> {
                if (uiState.allWithdrawals.isEmpty()) {
                    item {
                        TakeIt3DCard(
                            containerColor = AdminSurface,
                            depthColor = Color(0xFF060A15),
                            borderColor = AdminBorder,
                            cornerRadius = 18.dp,
                            depth = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Rounded.Payments, null, tint = AdminMuted, modifier = Modifier.size(40.dp))
                                Text("No Captain payout requests yet", color = AdminInk, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    items(uiState.allWithdrawals, key = { it.id }) { wd ->
                        val color = when (wd.status) {
                            "PAID" -> AdminEmerald
                            "REJECTED" -> AdminCrimson
                            else -> AdminGold
                        }
                        TakeIt3DCard(
                            containerColor = AdminSurface,
                            depthColor = Color(0xFF060A15),
                            borderColor = color.copy(alpha = 0.5f),
                            cornerRadius = 18.dp,
                            depth = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${wd.captainName} • ₹${wd.amount.toInt()}", color = AdminInk, fontWeight = FontWeight.ExtraBold)
                                        Text(wd.bankDetails, style = MaterialTheme.typography.labelSmall, color = AdminCyan)
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = color.copy(alpha = 0.18f)) {
                                        Text(wd.status, color = color, fontWeight = FontWeight.Bold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                    }
                                }
                                if (wd.status == "PENDING") {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.adminUpdateWithdrawal(wd.id, "PAID", "Transferred by Admin") },
                                            colors = ButtonDefaults.buttonColors(containerColor = AdminEmerald, contentColor = AdminBg),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Mark Paid", fontWeight = FontWeight.ExtraBold) }
                                        OutlinedButton(
                                            onClick = { viewModel.adminUpdateWithdrawal(wd.id, "REJECTED", "Bank verification pending") },
                                            border = BorderStroke(1.dp, AdminCrimson),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Reject", color = AdminCrimson, fontWeight = FontWeight.Bold) }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "ANALYTICS" -> {
                item {
                    val foodRev = uiState.allOrders.filter { it.status == "DELIVERED" }.sumOf { it.totalAmount }
                    val rideRev = uiState.allRides.filter { it.status == "COMPLETED" }.sumOf { it.totalFare }
                    val moveRev = uiState.allParcels.filter { it.status == "DELIVERED" }.sumOf { it.totalFare }
                    val totalAllRev = (foodRev + rideRev + moveRev).coerceAtLeast(1)

                    TakeIt3DCard(
                        containerColor = AdminSurface,
                        depthColor = Color(0xFF060A15),
                        borderColor = AdminCyan.copy(alpha = 0.55f),
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Column {
                                    Text("Multi-App Revenue & Fleet Analytics", color = AdminGold, fontWeight = FontWeight.ExtraBold)
                                    Text("Food ₹$foodRev • RideX ₹$rideRev • MoveX ₹$moveRev", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                                }
                                FilledTonalButton(
                                    onClick = {
                                        val csv = buildString {
                                            appendLine("Vertical,Count,CompletedRevenueINR")
                                            appendLine("Food,${uiState.allOrders.size},$foodRev")
                                            appendLine("RideX,${uiState.allRides.size},$rideRev")
                                            appendLine("MoveX,${uiState.allParcels.size},$moveRev")
                                        }
                                        shareTextIntent(context, "Take It Analytics CSV", csv)
                                    },
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = AdminCard, contentColor = AdminCyan)
                                ) {
                                    Icon(Icons.Rounded.Share, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Export CSV", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                }
                            }

                            // 3D-style Canvas Bar Chart comparing verticals
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(150.dp)
                            ) {
                                val bars = listOf(
                                    Triple(foodRev.toFloat(), TakeItOrange, "Food"),
                                    Triple(rideRev.toFloat(), RideYellow, "RideX"),
                                    Triple(moveRev.toFloat(), AdminCyan, "MoveX")
                                )
                                val maxVal = (bars.maxOfOrNull { it.first } ?: 1f).coerceAtLeast(100f)
                                val barWidth = size.width / 6f
                                val gap = (size.width - barWidth * 3f) / 4f
                                bars.forEachIndexed { idx, (value, color, _) ->
                                    val x = gap + idx * (barWidth + gap)
                                    val frac = (value / maxVal).coerceIn(0.08f, 1f)
                                    val h = size.height * 0.82f * frac
                                    val y = size.height - h
                                    drawRoundRect(
                                        color = color.copy(alpha = 0.22f),
                                        topLeft = Offset(x, 0f),
                                        size = Size(barWidth, size.height),
                                        cornerRadius = CornerRadius(14f, 14f)
                                    )
                                    drawRoundRect(
                                        color = color,
                                        topLeft = Offset(x, y),
                                        size = Size(barWidth, h),
                                        cornerRadius = CornerRadius(14f, 14f)
                                    )
                                }
                            }

                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                AnalyticsLegendDot("Food (${uiState.allOrders.size})", TakeItOrange)
                                AnalyticsLegendDot("RideX (${uiState.allRides.size})", RideYellow)
                                AnalyticsLegendDot("MoveX (${uiState.allParcels.size})", AdminCyan)
                            }
                            Text(
                                "Combined Gross Platform Value: ₹${foodRev + rideRev + moveRev} • Fleet Vehicles Active: ${uiState.mobilityVehicles.count { it.activeFlag }}/${uiState.mobilityVehicles.size} • Share: Food ${(foodRev * 100 / totalAllRev)}% | RideX ${(rideRev * 100 / totalAllRev)}% | MoveX ${(moveRev * 100 / totalAllRev)}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminInk
                            )
                        }
                    }
                }
            }
        }
    }

    if (showVehicleDialog) {
        MobilityVehicleEditorDialog(
            initial = editingVehicle,
            isUploading = uiState.isUploadingPhoto,
            onUploadImage = { uri, onUrl -> viewModel.uploadImageToImgBb(uri, "takeit_veh", onUrl) },
            onDismiss = { showVehicleDialog = false },
            onSave = {
                viewModel.saveMobilityVehicle(it)
                showVehicleDialog = false
            }
        )
    }
}

@Composable
private fun AnalyticsLegendDot(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelSmall, color = AdminInk, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MobilityVehicleEditorDialog(
    initial: MobilityVehicleCategory?,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (MobilityVehicleCategory) -> Unit
) {
    var vertical by remember { mutableStateOf(initial?.vertical ?: "RIDE") }
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var subtitle by remember { mutableStateOf(initial?.subtitle ?: "") }
    var capacity by remember { mutableStateOf(initial?.capacityLabel ?: "1 Seat") }
    var eta by remember { mutableStateOf((initial?.etaMinutes ?: 4).toString()) }
    var baseFare by remember { mutableStateOf((initial?.baseFare ?: 25).toString()) }
    var perKm by remember { mutableStateOf((initial?.perKmRate ?: 8).toString()) }
    var perMin by remember { mutableStateOf((initial?.perMinRate ?: 1).toString()) }
    var minFare by remember { mutableStateOf((initial?.minFare ?: 35).toString()) }
    var commission by remember { mutableStateOf((initial?.commissionPercent ?: 15).toString()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadImage(uri) { url -> imageUrl = url }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text(if (initial == null) "Add Mobility Vehicle" else "Edit ${initial.name}", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("RIDE", "TRANSPORT").forEach { v ->
                        FilterChip(
                            selected = vertical == v,
                            onClick = { vertical = v },
                            label = { Text(v, fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase() }, label = { Text("Code *") }, placeholder = { Text("BIKE") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name *") }, singleLine = true, modifier = Modifier.weight(1.3f))
                }
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Capacity Label") }, singleLine = true, modifier = Modifier.weight(1.2f))
                    OutlinedTextField(value = eta, onValueChange = { eta = it.filter(Char::isDigit) }, label = { Text("ETA min") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(0.8f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = baseFare, onValueChange = { baseFare = it.filter(Char::isDigit) }, label = { Text("Base ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = perKm, onValueChange = { perKm = it.filter(Char::isDigit) }, label = { Text("Per Km ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = perMin, onValueChange = { perMin = it.filter(Char::isDigit) }, label = { Text("Per Min ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = minFare, onValueChange = { minFare = it.filter(Char::isDigit) }, label = { Text("Min Fare ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = commission, onValueChange = { commission = it.filter(Char::isDigit) }, label = { Text("Platform Commission %") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = imageUrl, onValueChange = { imageUrl = it }, label = { Text("Vehicle PNG/Image URL (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                FilledTonalButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isUploading) "Uploading Photo..." else "Pick Vehicle Image (ImgBB)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (code.isNotBlank() && name.isNotBlank()) {
                        onSave(
                            (initial ?: MobilityVehicleCategory()).copy(
                                vertical = vertical,
                                code = code.trim().uppercase(),
                                name = name.trim(),
                                subtitle = subtitle.trim(),
                                capacityLabel = capacity.trim().ifBlank { "Standard" },
                                etaMinutes = eta.toIntOrNull() ?: 4,
                                baseFare = baseFare.toIntOrNull() ?: 25,
                                perKmRate = perKm.toIntOrNull() ?: 8,
                                perMinRate = perMin.toIntOrNull() ?: 1,
                                minFare = minFare.toIntOrNull() ?: 35,
                                commissionPercent = commission.toIntOrNull() ?: 15,
                                imageUrl = imageUrl.trim(),
                                activeFlag = initial?.activeFlag ?: true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
            ) { Text("Save Vehicle", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) } }
    )
}
