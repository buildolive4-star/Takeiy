package com.example.takeit.ui.ride

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import kotlin.math.*

internal fun calculateHaversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Int {
    if ((lat1 == 0.0 && lon1 == 0.0) || (lat2 == 0.0 && lon2 == 0.0)) return 6
    val r = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
    val c = 2 * asin(sqrt(a))
    return (r * c).roundToInt().coerceIn(2, 180)
}

@Composable
internal fun RideOnboardingDialog(
    onFinish: () -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val steps = listOf(
        Triple(
            "Book a Ride Anywhere",
            "Bikes, Autos, Cabs & Rentals at transparent fares with 30-second pickup.",
            TakeItFreeMedia.rideMediaCatalog[0]
        ),
        Triple(
            "Track Your Captain Live",
            "Real-time OpenStreetMap GPS, 4-digit security OTP & instant Call/SMS.",
            TakeItFreeMedia.animatedGifs["RIDE_SCOOTER"].orEmpty()
        ),
        Triple(
            "Travel Safely & Conveniently",
            "24x7 SOS safety shield, trusted emergency contacts & digital Razorpay receipts.",
            TakeItFreeMedia.rideMediaCatalog[3]
        )
    )
    val current = steps[step]

    AlertDialog(
        onDismissRequest = onFinish,
        containerColor = Color.White,
        title = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(50), color = RideCharcoal) {
                    Text("RideX • ${step + 1}/3", color = RideYellow, fontWeight = FontWeight.ExtraBold, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }
                TextButton(onClick = onFinish, modifier = Modifier.testTag("ridex_onboarding_skip_btn")) {
                    Text("Skip", color = TakeItMuted, fontWeight = FontWeight.Bold)
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(136.dp)
                        .clip(CircleShape)
                        .background(RideYellowSoft),
                    contentAlignment = Alignment.Center
                ) {
                    TakeItMediaVisual(
                        imageUrl = current.third,
                        key = "ride_onboard_$step",
                        contentDescription = current.first,
                        modifier = Modifier.size(100.dp)
                    )
                }
                Text(current.first, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                Text(current.second, style = MaterialTheme.typography.bodyMedium, color = TakeItMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    steps.indices.forEach { idx ->
                        Box(
                            modifier = Modifier
                                .height(6.dp)
                                .width(if (idx == step) 24.dp else 8.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (idx == step) RideCharcoal else TakeItBorder)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TakeIt3DButton(
                onClick = { if (step < 2) step++ else onFinish() },
                containerColor = RideYellow,
                depthColor = RideYellow.darken3D(0.4f),
                contentColor = RideCharcoal,
                height = 44.dp,
                modifier = Modifier.fillMaxWidth().testTag("ridex_onboarding_next_btn")
            ) {
                Text(if (step < 2) "Next →" else "Get Started", fontWeight = FontWeight.ExtraBold)
            }
        }
    )
}

@Composable
internal fun RideActivityAndOffersTab(
    uiState: TakeItUiState,
    onApplyCoupon: (String) -> Unit
) {
    val context = LocalContext.current
    val activeCoupons = remember(uiState.coupons) {
        val list = uiState.coupons.filter { it.activeFlag }
        if (list.isNotEmpty()) list else listOf(
            PromoCoupon("def_ride50", "RIDE50", "Flat 50% OFF on Rides", "Up to ₹100 off your next Bike or Auto ride", "PERCENT", 50, 100, 40, true)
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(
                containerColor = RideCharcoal,
                depthColor = Color.Black,
                borderColor = RideYellow,
                cornerRadius = 24.dp,
                depth = 6.dp
            ) {
                Row(modifier = Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    TakeItMediaVisual(
                        imageUrl = TakeItFreeMedia.animatedGifs["OFFER_FIRE"].orEmpty(),
                        key = "ride_offers_banner",
                        contentDescription = "RideX Offers",
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(14.dp))
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Get Up to 50% OFF", style = MaterialTheme.typography.titleLarge, color = RideYellow, fontWeight = FontWeight.ExtraBold)
                        Text("Use Code: RIDE50 • Tap any coupon below to apply on checkout", style = MaterialTheme.typography.bodySmall, color = Color.White)
                    }
                }
            }
        }

        item { Text("Available Coupons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = RideCharcoal) }
        items(activeCoupons, key = { "cpn_${it.id}_${it.code}" }) { cpn ->
            TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), borderColor = RideYellow.copy(alpha = 0.6f), cornerRadius = 18.dp, depth = 4.dp) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = RoundedCornerShape(10.dp), color = RideYellowSoft) {
                        Text(cpn.code, color = RideCharcoal, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(cpn.title.ifBlank { cpn.code }, fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                        Text(cpn.description.ifBlank { "Min ride ₹${cpn.minOrderAmount}" }, style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                    }
                    Button(
                        onClick = { onApplyCoupon(cpn.code) },
                        colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = RideCharcoal)
                    ) { Text("Apply", fontWeight = FontWeight.ExtraBold) }
                }
            }
        }

        item { Text("Recent Activity & Notifications", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = RideCharcoal) }
        if (uiState.myRides.isEmpty()) {
            item {
                TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 18.dp, depth = 3.dp) {
                    Text("No ride activity yet. Book your first RideX trip from Home!", modifier = Modifier.padding(16.dp), color = TakeItMuted)
                }
            }
        } else {
            items(uiState.myRides.take(6), key = { "act_${it.id}" }) { r ->
                TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 16.dp, depth = 3.dp) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        TakeIt3DIconOrb(
                            icon = if (r.status == "COMPLETED") Icons.Rounded.CheckCircle else Icons.Rounded.NotificationsActive,
                            containerColor = if (r.status == "COMPLETED") TakeItGreen else DeliveryAmber,
                            size = 36.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${r.rideNumber} • ${r.status.replace("_", " ")}", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                            Text("${r.pickupAddress} → ${r.dropAddress}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Text("₹${r.totalFare}", fontWeight = FontWeight.ExtraBold, color = RideCharcoal)
                    }
                }
            }
        }

        item {
            SupportChannelsCard(
                config = uiState.config,
                accentColor = RideYellow,
                onRaiseTicket = { dialPhone(context, uiState.config.supportPhone) }
            )
        }
    }
}

@Composable
internal fun RideProfileTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onOpenRidesTab: () -> Unit,
    onOpenWalletTab: () -> Unit,
    onReplayOnboarding: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    var showEditProfile by remember { mutableStateOf(false) }
    var showEmergencyDialog by remember { mutableStateOf(false) }
    var showSupportDialog by remember { mutableStateOf(false) }
    var showAddressDialog by remember { mutableStateOf(false) }
    var legalDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(
                containerColor = RideCharcoal,
                depthColor = Color.Black,
                borderColor = RideYellow,
                cornerRadius = 24.dp,
                depth = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    TakeItMediaVisual(
                        imageUrl = TakeItFreeMedia.avatarAndSupportCatalog[0],
                        key = "rider_avatar",
                        contentDescription = "Rider Avatar",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(RideYellow)
                            .padding(4.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(uiState.profile.name.ifBlank { uiState.displayName }, style = MaterialTheme.typography.titleLarge, color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Text(uiState.email, style = MaterialTheme.typography.labelSmall, color = RideYellow)
                        Text(uiState.profile.phone.ifBlank { "Tap Edit Profile to add mobile number" }, style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.8f))
                    }
                    OutlinedButton(
                        onClick = { showEditProfile = true },
                        border = BorderStroke(1.dp, RideYellow),
                        modifier = Modifier.testTag("ridex_edit_profile_btn")
                    ) { Text("Edit", color = RideYellow, fontWeight = FontWeight.Bold) }
                }
            }
        }

        item {
            TakeIt3DCard(containerColor = Color.White, depthColor = Color(0xFFD5DCE6), cornerRadius = 22.dp, depth = 5.dp) {
                Column {
                    RideProfileMenuRow(Icons.Rounded.DirectionsBike, "My Rides", "${uiState.myRides.size} trips", onOpenRidesTab)
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.AccountBalanceWallet, "RideX Wallet", "₹${uiState.profile.walletBalance.toInt()}", onOpenWalletTab)
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.Bookmark, "Saved Addresses", "${uiState.addresses.size} saved", { showAddressDialog = true })
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.EmergencyShare, "Emergency SOS Contacts", "${uiState.emergencyContacts.size} contacts", { showEmergencyDialog = true })
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.SupportAgent, "Help & 24x7 Support", "${uiState.myTickets.size} tickets", { showSupportDialog = true })
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.AutoAwesome, "App Tour (Onboarding)", "3 steps", onReplayOnboarding)
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.Info, "About RideX", "", { legalDialog = "About RideX" to uiState.config.aboutUsText })
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.Gavel, "Terms & Conditions", "", { legalDialog = "Terms & Conditions" to uiState.config.termsText })
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.PrivacyTip, "Privacy Policy", "", { legalDialog = "Privacy Policy" to uiState.config.privacyText })
                    HorizontalDivider(color = TakeItBorder)
                    RideProfileMenuRow(Icons.Rounded.SwitchAccount, "Switch Google Account / Sign Out", "", onSignOut, tint = TakeItRed)
                }
            }
        }

        item {
            SupportChannelsCard(
                config = uiState.config,
                accentColor = RideYellow,
                onRaiseTicket = { showSupportDialog = true }
            )
        }
    }

    if (showEditProfile) {
        var name by remember { mutableStateOf(uiState.profile.name.ifBlank { uiState.displayName }) }
        var phone by remember { mutableStateOf(uiState.profile.phone) }
        AlertDialog(
            onDismissRequest = { showEditProfile = false },
            title = { Text("Edit Rider Profile", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateCustomerPhoneAndName(name, phone)
                        showEditProfile = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = RideCharcoal)
                ) { Text("Save", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showEditProfile = false }) { Text("Cancel") } }
        )
    }

    if (showEmergencyDialog) {
        var cName by remember { mutableStateOf("") }
        var cPhone by remember { mutableStateOf("") }
        var cRel by remember { mutableStateOf("Family") }
        AlertDialog(
            onDismissRequest = { showEmergencyDialog = false },
            title = { Text("Emergency SOS Contacts", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.emergencyContacts.forEach { ec ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("${ec.name} (${ec.relation})", fontWeight = FontWeight.Bold)
                                Text(ec.phone, style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                            IconButton(onClick = { dialPhone(context, ec.phone) }) { Icon(Icons.Rounded.Call, "Call Contact", tint = TakeItGreen) }
                            IconButton(onClick = { viewModel.deleteEmergencyContact(ec.id) }) { Icon(Icons.Rounded.DeleteOutline, "Remove", tint = TakeItRed) }
                        }
                    }
                    HorizontalDivider()
                    OutlinedTextField(value = cName, onValueChange = { cName = it }, label = { Text("Contact Name") }, singleLine = true)
                    OutlinedTextField(value = cPhone, onValueChange = { cPhone = it }, label = { Text("Phone Number") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                    OutlinedTextField(value = cRel, onValueChange = { cRel = it }, label = { Text("Relation") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cName.isNotBlank() && cPhone.isNotBlank()) {
                            viewModel.saveEmergencyContact(EmergencyContact(name = cName, phone = cPhone, relation = cRel))
                            cName = ""
                            cPhone = ""
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TakeItRed, contentColor = Color.White)
                ) { Text("Add Contact", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showEmergencyDialog = false }) { Text("Done") } }
        )
    }

    if (showSupportDialog) {
        var subject by remember { mutableStateOf("") }
        var message by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSupportDialog = false },
            title = { Text("Raise RideX Support Ticket", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Issue Subject") }, singleLine = true)
                    OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe your issue") }, minLines = 3)
                    uiState.myTickets.take(2).forEach { t ->
                        Text("• ${t.subject}: ${t.status} ${if (t.adminReply.isNotBlank()) "— ${t.adminReply}" else ""}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.createSupportTicket(subject, message)
                        showSupportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = RideCharcoal)
                ) { Text("Submit", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showSupportDialog = false }) { Text("Close") } }
        )
    }

    if (showAddressDialog) {
        var label by remember { mutableStateOf("Work") }
        var fullAddr by remember { mutableStateOf("") }
        var city by remember { mutableStateOf(uiState.config.cityName) }
        AlertDialog(
            onDismissRequest = { showAddressDialog = false },
            title = { Text("Saved Addresses (Home / Work)", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    uiState.addresses.take(4).forEach { addr ->
                        Row(Modifier.fillMaxWidth().clickable { viewModel.selectActiveAddress(addr) }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${addr.label}: ${addr.fullAddress}", style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            IconButton(onClick = { viewModel.deleteAddress(addr.id) }) { Icon(Icons.Rounded.DeleteOutline, "Remove", tint = TakeItRed) }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("Home", "Work", "Fav").forEach { l ->
                            FilterChip(selected = label == l, onClick = { label = l }, label = { Text(l) })
                        }
                    }
                    OutlinedTextField(value = fullAddr, onValueChange = { fullAddr = it }, label = { Text("Address Line") }, singleLine = true)
                    OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("City") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveStructuredAddress(SavedAddress(label = label, fullAddress = fullAddr, city = city, areaStreet = city))
                        showAddressDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RideYellow, contentColor = RideCharcoal)
                ) { Text("Save Place", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showAddressDialog = false }) { Text("Close") } }
        )
    }

    legalDialog?.let { (title, body) ->
        AboutPolicyDialog(title = title, bodyText = body, accentColor = RideYellow, onDismiss = { legalDialog = null })
    }
}

@Composable
private fun RideProfileMenuRow(
    icon: ImageVector,
    title: String,
    badge: String,
    onClick: () -> Unit,
    tint: Color = RideCharcoal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TakeIt3DIconOrb(icon = icon, containerColor = if (tint == TakeItRed) TakeItRed else RideCharcoal, iconTint = if (tint == TakeItRed) Color.White else RideYellow, size = 34.dp)
            Text(title, fontWeight = FontWeight.Bold, color = tint)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (badge.isNotBlank()) {
                Surface(shape = RoundedCornerShape(50), color = RideYellowSoft) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = RideCharcoal, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TakeItMuted)
        }
    }
}
