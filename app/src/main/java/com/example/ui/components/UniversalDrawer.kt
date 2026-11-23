package com.example.ui.components

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.AppStateEntity
import com.example.model.AppPanel
import com.example.model.AppRoute
import com.example.ui.theme.*

@Composable
fun UniversalPanelDrawerContent(
    appState: AppStateEntity,
    activePanel: AppPanel,
    unreadNotifications: Int,
    onSelectPanel: (AppPanel) -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onCloseDrawer: () -> Unit,
    onLogoutDemo: () -> Unit
) {
    ModalDrawerSheet(
        modifier = Modifier.widthIn(max = 336.dp).fillMaxHeight(),
        drawerContainerColor = DarkBackground,
        drawerContentColor = SoftWhite,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(DeepNavy, DarkBackground, Color(0xFF051320)))
                ),
            contentPadding = PaddingValues(bottom = 28.dp)
        ) {
            // Drawer Header with Atmospheric Glow, User Profile & Verified Badge
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    AtmosphericMeshHeaderBackground(modifier = Modifier.matchParentSize())
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(62.dp)
                                    .clip(CircleShape)
                                    .border(2.2.dp, PremiumYellow, CircleShape)
                                    .background(SkyBlue.copy(alpha = 0.25f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.img_launcher_zipora_1795421416651),
                                    contentDescription = "User profile avatar",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = appState.userName,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = SoftWhite,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        Icons.Rounded.Verified,
                                        contentDescription = "Verified Account",
                                        tint = SkyBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Text(
                                    text = appState.userPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFA7C6E0)
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(SuccessGreen.copy(alpha = 0.22f))
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text("KYC Verified", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50))
                                            .background(PremiumYellow)
                                            .clickable {
                                                onCloseDrawer()
                                                onNavigate(AppRoute.Profile)
                                            }
                                            .padding(horizontal = 10.dp, vertical = 3.dp)
                                            .testTag("drawer_edit_profile")
                                    ) {
                                        Text("Edit Profile", style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        // Mini Wallet & Points Glass Strip inside Drawer
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SoftWhite.copy(alpha = 0.10f))
                                    .border(1.dp, SkyBlue.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onCloseDrawer()
                                        onNavigate(AppRoute.Wallet)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.AccountBalanceWallet, null, tint = PremiumYellow, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("₹${appState.walletBalance.toInt()}", style = MaterialTheme.typography.labelLarge, color = SoftWhite)
                            }
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(SoftWhite.copy(alpha = 0.10f))
                                    .border(1.dp, PremiumYellow.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                    .clickable {
                                        onCloseDrawer()
                                        onNavigate(AppRoute.ReferEarn)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.WorkspacePremium, null, tint = SkyBlue, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("${appState.rewardPoints} Pts", style = MaterialTheme.typography.labelLarge, color = SoftWhite)
                            }
                        }
                    }
                }
            }

            // SWITCH YOUR MODE Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 18.dp, end = 18.dp, top = 18.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "SWITCH YOUR MODE",
                        style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
                        color = PremiumYellow,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "9 Unified Panels",
                        style = MaterialTheme.typography.labelSmall,
                        color = SkyBlue
                    )
                }
            }

            // All 9 Selectable Panels
            items(AppPanel.entries) { panel ->
                val isSelected = panel == activePanel
                val containerBg by animateColorAsState(
                    targetValue = if (isSelected) panel.accentColor.copy(alpha = 0.22f) else DarkCardSurface.copy(alpha = 0.7f),
                    label = "panelBg"
                )
                val borderCol by animateColorAsState(
                    targetValue = if (isSelected) panel.accentColor else DarkBorder.copy(alpha = 0.6f),
                    label = "panelBorder"
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(containerBg)
                        .border(if (isSelected) 1.5.dp else 1.dp, borderCol, RoundedCornerShape(16.dp))
                        .clickable {
                            onCloseDrawer()
                            onSelectPanel(panel)
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("drawer_panel_${panel.name.lowercase()}"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val charRes = when (panel) {
                        AppPanel.DRIVER_CAPTAIN -> R.drawable.img_char_ride_captain_1795426023899
                        AppPanel.FOOD_DELIVERY_PARTNER, AppPanel.RESTAURANT_OWNER -> R.drawable.img_char_food_rider_1795426023084
                        AppPanel.GROCERY_OWNER -> R.drawable.img_char_grocery_buddy_1795426023736
                        AppPanel.PARCEL_PARTNER, AppPanel.TRANSPORT_PARTNER -> R.drawable.img_char_porter_mover_1795426024294
                        else -> null
                    }
                    if (charRes != null) {
                        Image(
                            painter = painterResource(charRes),
                            contentDescription = panel.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .border(1.4.dp, panel.accentColor, RoundedCornerShape(13.dp))
                        )
                    } else {
                        IconOrb3D(
                            icon = panelIcon(panel),
                            tint = DeepNavy,
                            gradientStart = SoftWhite,
                            gradientEnd = panel.accentColor,
                            size = 42.dp,
                            iconSize = 21.dp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = panel.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = SoftWhite,
                                maxLines = 1
                            )
                            if (panel.isAdminOnly) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = if (appState.isAdminAuthorized) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                    contentDescription = "Admin restricted",
                                    tint = PremiumYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                        Text(
                            text = if (panel.isAdminOnly && !appState.isAdminAuthorized) "Authorized Admin Only" else panel.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF9FB3C8),
                            maxLines = 1
                        )
                    }
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(panel.accentColor)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "ACTIVE",
                                style = MaterialTheme.typography.labelSmall,
                                color = DeepNavy,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Divider + Other Menu Options
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    color = DarkBorder
                )
                Text(
                    text = "ACCOUNT & ECOSYSTEM",
                    style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
                    color = SkyBlue,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }

            val menuEntries = listOf(
                Triple("My Orders", Icons.Rounded.ReceiptLong, AppRoute.OrderHistory("ALL")),
                Triple("My Rides", Icons.Rounded.TwoWheeler, AppRoute.OrderHistory("RIDE")),
                Triple("My Deliveries", Icons.Rounded.LocalShipping, AppRoute.OrderHistory("PARCEL")),
                Triple("My Wallet (₹${appState.walletBalance.toInt()})", Icons.Rounded.AccountBalanceWallet, AppRoute.Wallet),
                Triple("Payment Methods", Icons.Rounded.CreditCard, AppRoute.Wallet),
                Triple("Coupons & Offers", Icons.Rounded.LocalOffer, AppRoute.CouponsOffers),
                Triple("Refer & Earn", Icons.Rounded.CardGiftcard, AppRoute.ReferEarn),
                Triple("Notifications${if (unreadNotifications > 0) " ($unreadNotifications)" else ""}", Icons.Rounded.Notifications, AppRoute.Notifications),
                Triple("Customer Support", Icons.AutoMirrored.Rounded.HelpOutline, AppRoute.Support),
                Triple("Settings & Profile", Icons.Rounded.Settings, AppRoute.Profile),
                Triple("Language (${appState.language})", Icons.Rounded.Translate, AppRoute.Profile),
                Triple(
                    "Privacy Policy",
                    Icons.Rounded.PrivacyTip,
                    AppRoute.InfoPage(
                        "Privacy Policy",
                        "Zipora Prototype stores all demo bookings, wallet transactions, inventory updates, and role switches locally on your device via Android Room Database. No real financial charges or external tracking occur in prototype mode."
                    )
                ),
                Triple(
                    "Terms & Conditions",
                    Icons.Rounded.Gavel,
                    AppRoute.InfoPage(
                        "Terms & Conditions",
                        "Zipora is a unified hyperlocal super app prototype demonstrating Bike Taxi, Auto & Cab Mobility, Food Delivery, Instant Grocery, Express Parcel Delivery, Goods Transport, Partner Operations, and Admin Controls inside one cohesive mobile experience."
                    )
                )
            )

            items(menuEntries) { (label, icon, route) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            onCloseDrawer()
                            onNavigate(route)
                        }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(icon, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        color = SoftWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = MutedText, modifier = Modifier.size(18.dp))
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(CoralDanger.copy(alpha = 0.14f))
                        .clickable {
                            onCloseDrawer()
                            onLogoutDemo()
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .navigationBarsPadding(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = CoralDanger, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Logout / Reset Session", style = MaterialTheme.typography.titleSmall, color = CoralDanger)
                }
            }
        }
    }
}

fun panelIcon(panel: AppPanel): ImageVector = when (panel) {
    AppPanel.CUSTOMER -> Icons.Rounded.Home
    AppPanel.DRIVER_CAPTAIN -> Icons.Rounded.TwoWheeler
    AppPanel.FOOD_DELIVERY_PARTNER -> Icons.Rounded.DeliveryDining
    AppPanel.RESTAURANT_OWNER -> Icons.Rounded.Storefront
    AppPanel.GROCERY_OWNER -> Icons.Rounded.LocalGroceryStore
    AppPanel.PARCEL_PARTNER -> Icons.Rounded.Inventory2
    AppPanel.TRANSPORT_PARTNER -> Icons.Rounded.LocalShipping
    AppPanel.BUSINESS_OWNER -> Icons.Rounded.BusinessCenter
    AppPanel.ADMIN -> Icons.Rounded.AdminPanelSettings
}
