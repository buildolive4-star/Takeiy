package com.example.takeit.ui.delivery

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.components.SupportChannelsCard
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.TakeItMediaVisual
import com.example.takeit.ui.components.darken3D
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel

@Composable
internal fun CaptainEarningsTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    val completedFoods = remember(uiState.allOrders) { uiState.allOrders.filter { it.status == "DELIVERED" } }
    val completedRides = remember(uiState.allRides) { uiState.allRides.filter { it.status == "COMPLETED" } }
    val completedParcels = remember(uiState.allParcels) { uiState.allParcels.filter { it.status == "DELIVERED" } }

    val foodEarnings = remember(completedFoods) { completedFoods.sumOf { (it.deliveryFee + 25).coerceAtLeast(35) } }
    val rideEarnings = remember(completedRides) { completedRides.sumOf { it.driverEarnings.takeIf { d -> d > 0 } ?: (it.totalFare * 0.85).toInt() } }
    val parcelEarnings = remember(completedParcels) { completedParcels.sumOf { it.driverEarnings.takeIf { d -> d > 0 } ?: (it.totalFare * 0.82).toInt() } }

    val totalJobs = completedFoods.size + completedRides.size + completedParcels.size
    val totalEarnings = (foodEarnings + rideEarnings + parcelEarnings)
    val withdrawnPaid = remember(uiState.myWithdrawals) {
        uiState.myWithdrawals.filter { it.status != "REJECTED" }.sumOf { it.amount }.toInt()
    }
    val availableToWithdraw = ((uiState.myCaptainProfile?.earningsBalance?.toInt() ?: totalEarnings) - withdrawnPaid)
        .coerceAtLeast(totalEarnings - withdrawnPaid)
        .coerceAtLeast(0)

    var chartPeriod by remember { mutableStateOf("WEEK") }
    var showWithdrawDialog by remember { mutableStateOf(false) }

    val weekBars = remember(foodEarnings, rideEarnings, parcelEarnings) {
        val base = (totalEarnings / 7).coerceAtLeast(180)
        listOf(
            "Mon" to (base * 0.85f).coerceAtLeast(120f),
            "Tue" to (base * 1.10f).coerceAtLeast(150f),
            "Wed" to (base * 0.95f).coerceAtLeast(140f),
            "Thu" to (base * 1.35f).coerceAtLeast(210f),
            "Fri" to (base * 1.25f).coerceAtLeast(190f),
            "Sat" to (base * 1.60f).coerceAtLeast(260f),
            "Today" to totalEarnings.toFloat().coerceAtLeast(320f)
        )
    }
    val monthBars = remember(totalEarnings) {
        val wk = totalEarnings.toFloat().coerceAtLeast(650f)
        listOf(
            "Wk 1" to wk * 0.7f,
            "Wk 2" to wk * 0.9f,
            "Wk 3" to wk * 1.15f,
            "Wk 4" to wk * 1.35f
        )
    }
    val activeBars = if (chartPeriod == "WEEK") weekBars else monthBars

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color(0xFF03090C),
                borderColor = DeliveryEmerald.copy(alpha = 0.6f),
                cornerRadius = 24.dp,
                depth = 6.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.horizontalGradient(listOf(Color(0xFF064E3B), DeliverySurface)))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Available Captain Balance", style = MaterialTheme.typography.labelSmall, color = DeliveryEmerald, fontWeight = FontWeight.Bold)
                            Text("₹$availableToWithdraw", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            Text("Lifetime Earned: ₹$totalEarnings • Withdrawn: ₹$withdrawnPaid", style = MaterialTheme.typography.labelSmall, color = DeliveryMuted)
                        }
                        TakeIt3DButton(
                            onClick = { showWithdrawDialog = true },
                            containerColor = RideYellow,
                            depthColor = RideYellow.darken3D(0.45f),
                            contentColor = RideCharcoal,
                            cornerRadius = 14.dp,
                            depth = 4.dp,
                            height = 44.dp,
                            modifier = Modifier.testTag("captain_withdraw_money_btn")
                        ) {
                            Icon(Icons.Rounded.AccountBalanceWallet, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Withdraw", fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CaptainKpiMiniCard("Trips", "$totalJobs", DeliveryEmerald, Modifier.weight(1f))
                        CaptainKpiMiniCard("Food", "₹$foodEarnings", TakeItGold, Modifier.weight(1f))
                        CaptainKpiMiniCard("RideX", "₹$rideEarnings", RideYellow, Modifier.weight(1f))
                        CaptainKpiMiniCard("Rating", "${uiState.myCaptainProfile?.rating ?: 4.9} ★", DeliveryAmber, Modifier.weight(1f))
                    }
                }
            }
        }

        // Earnings Bar Graph
        item {
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color.Black,
                borderColor = DeliveryBorder,
                cornerRadius = 22.dp,
                depth = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Earnings Analytics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                            Text("Food • RideX • MoveX payouts", style = MaterialTheme.typography.labelSmall, color = DeliveryMuted)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf("WEEK" to "Week", "MONTH" to "Month").forEach { (id, label) ->
                                FilterChip(
                                    selected = chartPeriod == id,
                                    onClick = { chartPeriod = id },
                                    label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = DeliveryEmerald,
                                        selectedLabelColor = DeliveryBg,
                                        containerColor = DeliveryCard,
                                        labelColor = DeliveryInk
                                    )
                                )
                            }
                        }
                    }

                    val maxVal = (activeBars.maxOfOrNull { it.second } ?: 1f).coerceAtLeast(1f)
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                    ) {
                        val barCount = activeBars.size
                        val slotW = size.width / barCount
                        val barW = (slotW * 0.54f).coerceAtLeast(18f)
                        activeBars.forEachIndexed { idx, (_, value) ->
                            val ratio = (value / maxVal).coerceIn(0.08f, 1f)
                            val barH = size.height * ratio
                            val left = idx * slotW + (slotW - barW) / 2f
                            drawRoundRect(
                                color = DeliveryCard,
                                topLeft = Offset(left, 0f),
                                size = Size(barW, size.height),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                            drawRoundRect(
                                color = if (idx == barCount - 1) RideYellow else DeliveryEmerald,
                                topLeft = Offset(left, size.height - barH),
                                size = Size(barW, barH),
                                cornerRadius = CornerRadius(12f, 12f)
                            )
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        activeBars.forEach { (label, valAmt) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("₹${valAmt.toInt()}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = DeliveryEmerald)
                                Text(label, fontSize = 10.sp, color = DeliveryMuted)
                            }
                        }
                    }
                }
            }
        }

        if (uiState.myWithdrawals.isNotEmpty()) {
            item {
                Text("Withdrawal Requests", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
            }
            items(uiState.myWithdrawals, key = { "w_${it.id}" }) { w ->
                val statusColor = when (w.status.uppercase()) {
                    "PAID" -> DeliveryEmerald
                    "REJECTED" -> AdminCrimson
                    else -> DeliveryAmber
                }
                TakeIt3DCard(
                    containerColor = DeliverySurface,
                    depthColor = Color.Black,
                    borderColor = statusColor.copy(alpha = 0.4f),
                    cornerRadius = 18.dp,
                    depth = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("₹${w.amount.toInt()} Payout", fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                            Text(w.bankDetails, style = MaterialTheme.typography.labelSmall, color = DeliveryMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            if (w.adminNote.isNotBlank()) {
                                Text("Admin: ${w.adminNote}", style = MaterialTheme.typography.labelSmall, color = DeliveryAmber)
                            }
                        }
                        Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.16f)) {
                            Text(w.status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                        }
                    }
                }
            }
        }

        item {
            Text("Completed Trip & Order History ($totalJobs)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
        }

        if (totalJobs == 0) {
            item {
                TakeIt3DCard(
                    containerColor = DeliverySurface,
                    depthColor = Color.Black,
                    borderColor = DeliveryBorder,
                    cornerRadius = 18.dp,
                    depth = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "Complete Food, RideX, or MoveX trips to see your live ledger & payout history here.",
                        color = DeliveryMuted,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(18.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(completedFoods, key = { "hist_food_${it.id}" }) { o ->
                CaptainHistoryRow("FOOD • ${o.orderNumber}", "${o.restaurantName} → ${o.customerName}", "₹${(o.deliveryFee + 25).coerceAtLeast(35)}", DeliveryEmerald)
            }
            items(completedRides, key = { "hist_ride_${it.id}" }) { r ->
                CaptainHistoryRow("RIDEX • ${r.rideNumber}", "${r.customerName} (${r.distanceKm} km)", "₹${r.driverEarnings.takeIf { d -> d > 0 } ?: r.totalFare}", RideYellow)
            }
            items(completedParcels, key = { "hist_parcel_${it.id}" }) { p ->
                CaptainHistoryRow("MOVEX • ${p.parcelNumber}", "${p.packageType} → ${p.receiverName}", "₹${p.driverEarnings.takeIf { d -> d > 0 } ?: p.totalFare}", TransportOrange)
            }
        }
    }

    if (showWithdrawDialog) {
        var amountInput by remember { mutableStateOf(availableToWithdraw.coerceAtLeast(100).toString()) }
        AlertDialog(
            onDismissRequest = { showWithdrawDialog = false },
            containerColor = DeliverySurface,
            titleContentColor = DeliveryInk,
            textContentColor = DeliveryInk,
            title = { Text("Request Payout Withdrawal", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Funds will be transferred to your Captain KYC Bank Account / Registered UPI.",
                        style = MaterialTheme.typography.bodySmall,
                        color = DeliveryMuted
                    )
                    OutlinedTextField(
                        value = amountInput,
                        onValueChange = { amountInput = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Amount (₹, min ₹50)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("captain_withdraw_amount_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountInput.toDoubleOrNull() ?: 0.0
                        viewModel.requestCaptainWithdrawal(amt)
                        showWithdrawDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeliveryEmerald, contentColor = DeliveryBg),
                    modifier = Modifier.testTag("captain_withdraw_confirm_btn")
                ) { Text("Submit Request", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showWithdrawDialog = false }) { Text("Cancel", color = DeliveryMuted) }
            }
        )
    }
}

@Composable
private fun CaptainKpiMiniCard(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DeliveryCard.copy(alpha = 0.9f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, fontWeight = FontWeight.ExtraBold, color = accent, fontSize = 13.sp, maxLines = 1)
            Text(label, fontSize = 10.sp, color = DeliveryMuted, maxLines = 1)
        }
    }
}

@Composable
private fun CaptainHistoryRow(
    title: String,
    subtitle: String,
    earnedText: String,
    accent: Color
) {
    TakeIt3DCard(
        containerColor = DeliverySurface,
        depthColor = Color.Black,
        borderColor = DeliveryBorder,
        cornerRadius = 18.dp,
        depth = 4.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.ExtraBold, color = accent)
                Text(subtitle, color = DeliveryInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text("+$earnedText", fontWeight = FontWeight.ExtraBold, color = DeliveryEmerald)
        }
    }
}

@Composable
internal fun CaptainKycTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    val existing = uiState.myCaptainProfile
    val status = existing?.approvalStatus?.ifBlank { "NOT SUBMITTED" } ?: "NOT SUBMITTED"
    val statusColor = when (status.uppercase()) {
        "APPROVED" -> DeliveryEmerald
        "SUSPENDED", "REJECTED" -> AdminCrimson
        "PENDING" -> DeliveryAmber
        else -> AdminCyan
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color.Black,
                borderColor = statusColor,
                cornerRadius = 22.dp,
                depth = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Captain KYC Verification", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)
                        Text(
                            when (status.uppercase()) {
                                "APPROVED" -> "Verified Captain! Your documents are active across Food, RideX & MoveX."
                                "PENDING" -> "Under review by Take It Admin HQ."
                                "SUSPENDED", "REJECTED" -> "Action required: update your documents & re-submit."
                                else -> "Upload Driving Licence & Vehicle details for Admin approval."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = DeliveryMuted
                        )
                    }
                    Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.16f)) {
                        Text(status, color = statusColor, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
                    }
                }
            }
        }

        item {
            CaptainKycFormCard(
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

@Composable
internal fun CaptainKycFormCard(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSubmitted: () -> Unit = {}
) {
    val existing = uiState.myCaptainProfile
    var fullName by remember(existing) { mutableStateOf(existing?.fullName?.ifBlank { uiState.displayName } ?: uiState.displayName) }
    var phone by remember(existing) { mutableStateOf(existing?.phone?.ifBlank { uiState.profile.phone } ?: uiState.profile.phone) }
    var vehicleCategory by remember(existing) { mutableStateOf(existing?.vehicleCategory?.ifBlank { "BIKE" } ?: "BIKE") }
    var vehicleModel by remember(existing) { mutableStateOf(existing?.vehicleModel.orEmpty()) }
    var vehicleNumber by remember(existing) { mutableStateOf(existing?.vehicleNumber.orEmpty()) }
    var licenseNumber by remember(existing) { mutableStateOf(existing?.licenseNumber.orEmpty()) }
    var aadhaarLast4 by remember(existing) { mutableStateOf(existing?.aadhaarLast4.orEmpty()) }
    var bankAccount by remember(existing) { mutableStateOf(existing?.bankAccount.orEmpty()) }
    var ifscCode by remember(existing) { mutableStateOf(existing?.ifscCode.orEmpty()) }
    var licensePhotoUrl by remember(existing) { mutableStateOf(existing?.licensePhotoUrl.orEmpty()) }
    var vehiclePhotoUrl by remember(existing) { mutableStateOf(existing?.vehiclePhotoUrl.orEmpty()) }

    val licensePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.uploadImageToImgBb(uri, "captain_dl") { url -> licensePhotoUrl = url }
        }
    }
    val vehiclePicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.uploadImageToImgBb(uri, "captain_vehicle") { url -> vehiclePhotoUrl = url }
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = DeliveryEmerald,
        unfocusedBorderColor = DeliveryBorder,
        focusedTextColor = DeliveryInk,
        unfocusedTextColor = DeliveryInk,
        focusedContainerColor = DeliveryCard,
        unfocusedContainerColor = DeliveryCard,
        focusedLabelColor = DeliveryEmerald,
        unfocusedLabelColor = DeliveryMuted
    )

    TakeIt3DCard(
        containerColor = DeliverySurface,
        depthColor = Color.Black,
        borderColor = DeliveryBorder,
        cornerRadius = 22.dp,
        depth = 5.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Personal & Vehicle Details", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = DeliveryInk)

            OutlinedTextField(
                value = fullName,
                onValueChange = { fullName = it },
                label = { Text("Full Name (as per DL)") },
                singleLine = true,
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("kyc_full_name")
            )
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Mobile Number") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                colors = fieldColors,
                modifier = Modifier.fillMaxWidth().testTag("kyc_phone")
            )

            Text("Select Vehicle Category", style = MaterialTheme.typography.labelMedium, color = DeliveryEmerald, fontWeight = FontWeight.Bold)
            val cats = listOf("BIKE", "AUTO", "MINI", "SEDAN", "SUV", "MINI_TRUCK", "PICKUP", "TRUCK")
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cats) { cat ->
                    FilterChip(
                        selected = vehicleCategory == cat,
                        onClick = { vehicleCategory = cat },
                        label = { Text(cat.replace("_", " "), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DeliveryEmerald,
                            selectedLabelColor = DeliveryBg,
                            containerColor = DeliveryCard,
                            labelColor = DeliveryInk
                        )
                    )
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = vehicleModel,
                    onValueChange = { vehicleModel = it },
                    label = { Text("Vehicle Model") },
                    placeholder = { Text("Honda Activa / Tata Ace") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = vehicleNumber,
                    onValueChange = { vehicleNumber = it.uppercase() },
                    label = { Text("Plate No.") },
                    placeholder = { Text("UP16TK1234") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.weight(1f).testTag("kyc_vehicle_number")
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = licenseNumber,
                    onValueChange = { licenseNumber = it.uppercase() },
                    label = { Text("Driving Licence No.") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = aadhaarLast4,
                    onValueChange = { if (it.length <= 4) aadhaarLast4 = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Aadhaar Last 4") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = fieldColors,
                    modifier = Modifier.weight(0.8f)
                )
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = bankAccount,
                    onValueChange = { bankAccount = it },
                    label = { Text("Bank A/C / UPI ID") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.weight(1.2f)
                )
                OutlinedTextField(
                    value = ifscCode,
                    onValueChange = { ifscCode = it.uppercase() },
                    label = { Text("IFSC Code") },
                    singleLine = true,
                    colors = fieldColors,
                    modifier = Modifier.weight(0.8f)
                )
            }

            Text("Upload KYC Photos (Zero-Permission Photo Picker)", style = MaterialTheme.typography.labelMedium, color = DeliveryEmerald, fontWeight = FontWeight.Bold)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { licensePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !uiState.isUploadingPhoto,
                        border = BorderStroke(1.dp, DeliveryEmerald),
                        modifier = Modifier.fillMaxWidth().testTag("kyc_upload_license_btn")
                    ) {
                        Icon(Icons.Rounded.Badge, null, tint = DeliveryEmerald, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (licensePhotoUrl.isBlank()) "DL Photo" else "Change DL", color = DeliveryInk, fontSize = 12.sp)
                    }
                    if (licensePhotoUrl.isNotBlank()) {
                        TakeItMediaVisual(
                            imageUrl = licensePhotoUrl,
                            key = "captain",
                            contentDescription = "Driving Licence Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedButton(
                        onClick = { vehiclePicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !uiState.isUploadingPhoto,
                        border = BorderStroke(1.dp, DeliveryAmber),
                        modifier = Modifier.fillMaxWidth().testTag("kyc_upload_vehicle_btn")
                    ) {
                        Icon(Icons.Rounded.DirectionsCar, null, tint = DeliveryAmber, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (vehiclePhotoUrl.isBlank()) "Vehicle Photo" else "Change Veh", color = DeliveryInk, fontSize = 12.sp)
                    }
                    if (vehiclePhotoUrl.isNotBlank()) {
                        TakeItMediaVisual(
                            imageUrl = vehiclePhotoUrl,
                            key = vehicleCategory,
                            contentDescription = "Vehicle Photo Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(84.dp)
                                .clip(RoundedCornerShape(12.dp))
                        )
                    }
                }
            }

            if (uiState.isUploadingPhoto) {
                LinearProgressIndicator(color = DeliveryEmerald, modifier = Modifier.fillMaxWidth())
            }

            TakeIt3DButton(
                onClick = {
                    val payload = CaptainProfile(
                        id = uiState.uid,
                        userId = uiState.uid,
                        email = uiState.email,
                        fullName = fullName.trim(),
                        phone = phone.trim(),
                        vehicleCategory = vehicleCategory,
                        vehicleModel = vehicleModel.trim(),
                        vehicleNumber = vehicleNumber.trim(),
                        licenseNumber = licenseNumber.trim(),
                        aadhaarLast4 = aadhaarLast4.trim(),
                        bankAccount = bankAccount.trim(),
                        ifscCode = ifscCode.trim(),
                        licensePhotoUrl = licensePhotoUrl,
                        vehiclePhotoUrl = vehiclePhotoUrl,
                        approvalStatus = if (existing?.approvalStatus == "APPROVED") "APPROVED" else "PENDING",
                        onlineFlag = uiState.deliveryPartnerOnline,
                        earningsBalance = existing?.earningsBalance ?: 0.0,
                        totalCompletedTrips = existing?.totalCompletedTrips ?: 0,
                        rating = existing?.rating ?: 4.9
                    )
                    viewModel.saveMyCaptainKyc(payload)
                    onSubmitted()
                },
                enabled = !uiState.isUploadingPhoto,
                containerColor = DeliveryEmerald,
                depthColor = DeliveryEmerald.darken3D(0.45f),
                contentColor = DeliveryBg,
                cornerRadius = 16.dp,
                depth = 5.dp,
                height = 50.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("kyc_submit_btn")
            ) {
                Icon(Icons.Rounded.Verified, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (existing == null) "Submit Captain KYC" else "Update Captain KYC", fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

@Composable
internal fun CaptainKycRegistrationDialog(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeliveryBg,
        titleContentColor = DeliveryInk,
        textContentColor = DeliveryInk,
        title = { Text("New Captain KYC Application", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val st = uiState.myCaptainProfile?.approvalStatus
                if (!st.isNullOrBlank()) {
                    Surface(shape = RoundedCornerShape(12.dp), color = DeliveryEmerald.copy(alpha = 0.16f)) {
                        Text(
                            "Current Application Status: $st",
                            color = DeliveryEmerald,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }
                CaptainKycFormCard(uiState = uiState, viewModel = viewModel, onSubmitted = onDismiss)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = DeliveryAmber, fontWeight = FontWeight.Bold) }
        }
    )
}

@Composable
internal fun CaptainAccountTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    var showTicketDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(
                containerColor = DeliverySurface,
                depthColor = Color.Black,
                borderColor = DeliveryEmerald,
                cornerRadius = 24.dp,
                depth = 5.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        TakeIt3DIconOrb(Icons.Rounded.TwoWheeler, DeliveryEmerald, size = 50.dp)
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                uiState.myCaptainProfile?.fullName?.ifBlank { null }
                                    ?: uiState.activeCoAdmin?.name?.ifBlank { uiState.displayName } ?: uiState.displayName,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = DeliveryInk
                            )
                            Text("Gmail: ${uiState.email}", color = DeliveryEmerald, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    val phone = uiState.myCaptainProfile?.phone?.ifBlank { null }
                        ?: uiState.activeCoAdmin?.phone?.ifBlank { uiState.profile.phone } ?: uiState.profile.phone
                    if (phone.isNotBlank()) {
                        Text("Phone: $phone", color = DeliveryMuted, style = MaterialTheme.typography.bodySmall)
                    }
                    val vehicle = uiState.myCaptainProfile?.vehicleNumber?.ifBlank { null }
                        ?: uiState.activeCoAdmin?.vehicleNumber.orEmpty()
                    if (vehicle.isNotBlank()) {
                        Text("Vehicle: $vehicle (${uiState.myCaptainProfile?.vehicleCategory ?: "PARTNER"})", color = DeliveryAmber, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Captain Duty Status", fontWeight = FontWeight.Bold, color = DeliveryInk)
                        Switch(
                            checked = uiState.deliveryPartnerOnline,
                            onCheckedChange = viewModel::toggleDeliveryOnline,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DeliveryBg,
                                checkedTrackColor = DeliveryEmerald,
                                uncheckedThumbColor = DeliveryMuted,
                                uncheckedTrackColor = DeliveryCard
                            )
                        )
                    }
                }
            }
        }

        item {
            SupportChannelsCard(
                config = uiState.config,
                accentColor = DeliveryEmerald,
                darkMode = true,
                onRaiseTicket = { showTicketDialog = true }
            )
        }

        if (uiState.appVariant != TakeItAppVariant.DELIVERY) {
            item {
                TakeIt3DButton(
                    onClick = { viewModel.exitDeliveryMode() },
                    containerColor = DeliveryAmber,
                    depthColor = DeliveryAmber.darken3D(0.4f),
                    contentColor = DeliveryBg,
                    cornerRadius = 16.dp,
                    depth = 4.dp,
                    height = 50.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Switch to Customer App", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        item {
            TakeIt3DButton(
                onClick = onSignOut,
                containerColor = AdminCrimson,
                depthColor = AdminCrimson.darken3D(0.45f),
                contentColor = Color.White,
                cornerRadius = 16.dp,
                depth = 4.dp,
                height = 50.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("delivery_account_signout_btn")
            ) {
                Text("Switch Google Account / Sign Out", fontWeight = FontWeight.ExtraBold)
            }
        }
    }

    if (showTicketDialog) {
        var subject by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showTicketDialog = false },
            containerColor = DeliverySurface,
            titleContentColor = DeliveryInk,
            textContentColor = DeliveryInk,
            title = { Text("Captain Support Ticket", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Issue Subject") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text("Describe issue / payout query") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createSupportTicket("[CAPTAIN] $subject", message)
                        showTicketDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeliveryEmerald, contentColor = DeliveryBg)
                ) { Text("Send Ticket", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = {
                TextButton(onClick = { showTicketDialog = false }) { Text("Cancel", color = DeliveryMuted) }
            }
        )
    }
}
