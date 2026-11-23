package com.example.ui.screens.shared

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
import androidx.compose.ui.unit.dp
import com.example.data.DemoCatalog
import com.example.model.CouponItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun WalletScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onTopUp: (amount: Double, method: String, simulateFailure: Boolean) -> Boolean
) {
    var topUpAmount by remember { mutableStateOf("500") }
    var selectedMethod by remember { mutableStateOf("UPI") }
    var simulateFail by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = { ZiporaSubTopBar("Take It Universal Wallet", "Payments, cashback & instant refunds", onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 36.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF0C4A6E), SkyBlueDark)))
                        .padding(20.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("AVAILABLE BALANCE", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                        AnimatedWalletAmount(uiState.appState.walletBalance, color = SoftWhite, style = MaterialTheme.typography.displayMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("★ ${uiState.appState.rewardPoints} Reward Points", style = MaterialTheme.typography.labelMedium, color = PremiumYellow)
                            Text("• Zero Fee Refunds", style = MaterialTheme.typography.labelMedium, color = SuccessGreen)
                        }
                    }
                }
            }

            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 3.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Add Money (Prototype Payment Simulator)", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                        OutlinedTextField(
                            value = topUpAmount,
                            onValueChange = { topUpAmount = it },
                            label = { Text("Amount (₹)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("wallet_amount_input")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("250", "500", "1000", "2000").forEach { preset ->
                                FilterChip(selected = topUpAmount == preset, onClick = { topUpAmount = preset }, label = { Text("+₹$preset") })
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("UPI", "Debit Card", "Credit Card", "Net Banking", "Supported Wallets", "Cash")) { m ->
                                FilterChip(selected = selectedMethod == m, onClick = { selectedMethod = m }, label = { Text(m) })
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Simulate Gateway Failure", style = MaterialTheme.typography.labelMedium, color = CoralDanger)
                            Switch(checked = simulateFail, onCheckedChange = { simulateFail = it })
                        }
                        Button(
                            onClick = {
                                val amt = topUpAmount.toDoubleOrNull() ?: 500.0
                                onTopUp(amt.coerceAtLeast(10.0), selectedMethod, simulateFail)
                            },
                            modifier = Modifier.fillMaxWidth().height(52.dp).testTag("wallet_add_money_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Text("Add ₹$topUpAmount via $selectedMethod", fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            item {
                Text("Transaction History", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
            }

            items(uiState.transactions, key = { it.id }) { tx ->
                val isCredit = tx.txType != "DEBIT"
                Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconOrb3D(
                            icon = if (isCredit) Icons.Rounded.ArrowDownward else Icons.Rounded.ArrowUpward,
                            tint = SoftWhite,
                            gradientStart = if (isCredit) SuccessGreen else CoralDanger,
                            gradientEnd = DeepNavy,
                            size = 40.dp,
                            iconSize = 20.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.title, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                            Text("${tx.txType} • ${tx.method} • ${formatTime(tx.timestamp)}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Text(
                            "${if (isCredit) "+" else "-"}₹${tx.amount.toInt()}",
                            style = MaterialTheme.typography.titleMedium,
                            color = if (isCredit) SuccessGreen else CoralDanger,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationsScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onMarkAllRead: () -> Unit
) {
    Scaffold(
        containerColor = LightSkyBackground,
        topBar = {
            ZiporaSubTopBar(
                title = "Notifications",
                subtitle = "${uiState.unreadNotificationCount} unread alerts",
                onBack = onBack,
                trailing = {
                    TextButton(onClick = onMarkAllRead) { Text("Mark Read", color = SkyBlueDark, fontWeight = FontWeight.Bold) }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(uiState.notifications, key = { it.id }) { notif ->
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (notif.isRead) SoftWhite else RideCardStart,
                    modifier = Modifier.border(1.dp, if (notif.isRead) LightBorder else SkyBlue, RoundedCornerShape(18.dp))
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.Top) {
                        IconOrb3D(Icons.Rounded.NotificationsActive, SoftWhite, SkyBlue, DeepNavy, size = 40.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(notif.category.uppercase(), style = MaterialTheme.typography.labelSmall, color = SkyBlueDark, fontWeight = FontWeight.ExtraBold)
                                Text(formatTime(notif.timestamp), style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                            Text(notif.title, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = SlateText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onSaveProfile: (name: String, phone: String, email: String, emName: String, emPhone: String) -> Unit,
    onSelectLanguage: (String) -> Unit,
    onToggleNotifications: (Boolean) -> Unit
) {
    var name by remember(uiState.appState.userName) { mutableStateOf(uiState.appState.userName) }
    var phone by remember(uiState.appState.userPhone) { mutableStateOf(uiState.appState.userPhone) }
    var email by remember(uiState.appState.userEmail) { mutableStateOf(uiState.appState.userEmail) }
    var emName by remember(uiState.appState.emergencyContactName) { mutableStateOf(uiState.appState.emergencyContactName) }
    var emPhone by remember(uiState.appState.emergencyContactPhone) { mutableStateOf(uiState.appState.emergencyContactPhone) }

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = { ZiporaSubTopBar("Profile & Settings", "Account, safety & preferences", onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 36.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Personal Information", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Mobile Number") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())
                        Text("Emergency SOS Contact", style = MaterialTheme.typography.titleSmall, color = CoralDanger)
                        OutlinedTextField(value = emName, onValueChange = { emName = it }, label = { Text("Emergency Contact Name") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = emPhone, onValueChange = { emPhone = it }, label = { Text("Emergency Contact Phone") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = { onSaveProfile(name, phone, email, emName, emPhone) },
                            modifier = Modifier.fillMaxWidth().height(50.dp).testTag("save_profile_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Save Profile Locally", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("App Language", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(listOf("English (India)", "हिन्दी", "ಕನ್ನಡ", "தமிழ்", "తెలుగు")) { lang ->
                                FilterChip(selected = uiState.appState.language == lang, onClick = { onSelectLanguage(lang) }, label = { Text(lang) })
                            }
                        }
                        var highContrastAccessibility by remember { mutableStateOf(false) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Push Notifications & Live Alerts", style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
                            Switch(checked = uiState.appState.notificationsEnabled, onCheckedChange = onToggleNotifications)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Accessibility Mode", style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
                                Text("Enhanced contrast & simplified cues", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                            Switch(checked = highContrastAccessibility, onCheckedChange = { highContrastAccessibility = it })
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderHistoryScreen(
    initialFilter: String,
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onRateOrder: (String, Int) -> Unit,
    onRepeatOrder: (String) -> Unit = {}
) {
    var filter by remember { mutableStateOf(initialFilter) }
    val filtered = remember(uiState.orders, filter) {
        uiState.orders.filter { filter == "ALL" || it.type == filter }
    }

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = { ZiporaSubTopBar("Activity & Orders", "${filtered.size} records synced locally", onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("ALL", "RIDE", "FOOD", "GROCERY", "PARCEL", "TRANSPORT", "SHOPPING")) { f ->
                        FilterChip(selected = filter == f, onClick = { filter = f }, label = { Text(f) })
                    }
                }
            }
            items(filtered, key = { it.id }) { order ->
                Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("${order.type} • ${order.id}", style = MaterialTheme.typography.labelMedium, color = SkyBlueDark, fontWeight = FontWeight.Bold)
                            StatusPill(order.status)
                        }
                        Text(order.title, style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                        Text(order.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("₹${order.amount.toInt()} • ${order.paymentMethod}", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                            Text("OTP: ${order.otp}", style = MaterialTheme.typography.labelMedium, color = OrangeAccent, fontWeight = FontWeight.Bold)
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (order.rating == 0 && order.status == "COMPLETED") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Rate: ", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    for (s in 1..5) {
                                        Icon(
                                            Icons.Rounded.StarBorder,
                                            contentDescription = "Rate $s",
                                            tint = PremiumYellowDark,
                                            modifier = Modifier.size(22.dp).clickable { onRateOrder(order.id, s) }
                                        )
                                    }
                                }
                            } else if (order.rating > 0) {
                                Text("Rated ${"★".repeat(order.rating)}", style = MaterialTheme.typography.labelSmall, color = PremiumYellowDark)
                            } else {
                                Spacer(Modifier.width(1.dp))
                            }
                            OutlinedButton(
                                onClick = { onRepeatOrder(order.type) },
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Rounded.Replay, null, Modifier.size(14.dp), tint = SkyBlueDark)
                                Spacer(Modifier.width(4.dp))
                                Text("Repeat", style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CouponsOffersScreen(
    onBack: () -> Unit,
    onApplyCoupon: (CouponItem) -> Unit
) {
    Scaffold(
        containerColor = LightSkyBackground,
        topBar = { ZiporaSubTopBar("Coupons & Super Offers", "Tap to apply to your cart", onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(DemoCatalog.coupons) { coupon ->
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 3.dp) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(DeepNavy).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(coupon.code, style = MaterialTheme.typography.labelMedium, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                            }
                            Text(coupon.title, style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            Text(coupon.description, style = MaterialTheme.typography.bodySmall, color = SlateText)
                        }
                        Button(
                            onClick = { onApplyCoupon(coupon) },
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = DeepNavy)
                        ) { Text("Apply", fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
fun ReferEarnScreen(
    onBack: () -> Unit,
    onClaimSimulatedBonus: () -> Unit
) {
    Scaffold(
        containerColor = LightSkyBackground,
        topBar = { ZiporaSubTopBar("Refer & Earn", "Invite friends to Take It Super App", onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            IconOrb3D(Icons.Rounded.CardGiftcard, DeepNavy, PremiumYellow, OrangeAccent, size = 76.dp, iconSize = 40.dp)
            Text("Earn ₹250 per Friend!", style = MaterialTheme.typography.headlineLarge, color = DeepNavy)
            Text("Share your invite code. When they book their first Ride, Food, or Grocery basket, you both get ₹250 Take It Wallet cash.", style = MaterialTheme.typography.bodyMedium, color = SlateText)
            Surface(shape = RoundedCornerShape(16.dp), color = DeepNavy, modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.padding(18.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("TAKEIT250", style = MaterialTheme.typography.headlineMedium, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                    StatusPill("ACTIVE")
                }
            }
            Button(
                onClick = onClaimSimulatedBonus,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite),
                shape = RoundedCornerShape(16.dp)
            ) { Text("Simulate Friend Referral (+₹250 Bonus)", fontWeight = FontWeight.ExtraBold) }
        }
    }
}

@Composable
fun CustomerSupportScreen(
    onBack: () -> Unit,
    onSubmitTicket: (String) -> Unit
) {
    var issue by remember { mutableStateOf("") }
    Scaffold(
        containerColor = LightSkyBackground,
        topBar = { ZiporaSubTopBar("24x7 Customer Support", "Rides, orders, wallet & partner help", onBack) }
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            listOf(
                "How do I switch to Driver, Restaurant, or Admin mode?" to "Tap the ☰ hamburger menu at the top-left and pick any of the 9 panels under SWITCH YOUR MODE. Admin requires passcode 2026.",
                "Are payments real in this prototype?" to "No. All UPI, Card, and Wallet payments are safely simulated locally with instant persistence in Android Room DB."
            ).forEach { (q, a) ->
                Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(q, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                        Text(a, style = MaterialTheme.typography.bodySmall, color = SlateText)
                    }
                }
            }
            OutlinedTextField(
                value = issue,
                onValueChange = { issue = it },
                label = { Text("Describe your query") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )
            Button(
                onClick = {
                    if (issue.isNotBlank()) {
                        onSubmitTicket(issue)
                        issue = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)
            ) { Text("Submit Support Ticket", fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
fun InfoPageScreen(title: String, body: String, onBack: () -> Unit) {
    Scaffold(
        containerColor = LightSkyBackground,
        topBar = { ZiporaSubTopBar(title, "Take It Ecosystem Legal", onBack) }
    ) { innerPadding ->
        Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            Text(body, style = MaterialTheme.typography.bodyLarge, color = SlateText, modifier = Modifier.padding(18.dp))
        }
    }
}

private fun formatTime(ts: Long): String {
    return SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(ts))
}
