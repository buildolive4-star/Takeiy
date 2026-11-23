package com.example.ui.components

import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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

private val DrawerNavy = Color(0xFF0E1D45)
private val DrawerRoyalBlue = Color(0xFF2563EB)

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
        modifier = Modifier.widthIn(max = 330.dp).fillMaxHeight(),
        drawerContainerColor = DrawerNavy,
        drawerContentColor = DeepNavy,
        windowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Box(modifier = Modifier.fillMaxSize().background(DrawerNavy)) {
            // Top-right golden yellow curved swoop accent (exact mockup style)
            Canvas(modifier = Modifier.fillMaxWidth().height(190.dp)) {
                val p = Path().apply {
                    moveTo(size.width * 0.72f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, size.height * 0.75f)
                    quadraticTo(size.width * 0.88f, size.height * 0.25f, size.width * 0.72f, 0f)
                    close()
                }
                drawPath(p, color = PremiumYellow)
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                // CURVED NAVY PROFILE HEADER
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(start = 18.dp, end = 22.dp, top = 16.dp, bottom = 20.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.img_char_ride_captain_1795426023899),
                                contentDescription = "User Profile Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(66.dp)
                                    .clip(CircleShape)
                                    .border(2.5.dp, SoftWhite, CircleShape)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = appState.userName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = SoftWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1
                                )
                                Text(
                                    text = appState.userPhone,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1)
                                )
                                Spacer(Modifier.height(3.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF1D2D58))
                                        .border(1.dp, PremiumYellow.copy(alpha = 0.45f), RoundedCornerShape(50))
                                        .padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = "👑 Gold Member",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = PremiumYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(PremiumYellow)
                                        .clickable {
                                            onCloseDrawer()
                                            onNavigate(AppRoute.Profile)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 6.dp)
                                        .testTag("drawer_edit_profile")
                                ) {
                                    Text(
                                        text = "Edit Profile  >",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = DrawerNavy,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }

                // CURVED WHITE CONTENT SHEET
                item {
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = SoftWhite,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Switch Your Panel",
                                style = MaterialTheme.typography.titleMedium,
                                color = DrawerNavy,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            AppPanel.entries.forEach { panel ->
                                val isSelected = panel == activePanel
                                val (badgeBg, badgeTint) = drawerPanelColors(panel)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(if (isSelected) Color(0xFFEFF6FF) else Color.Transparent)
                                        .border(
                                            width = if (isSelected) 1.2.dp else 0.dp,
                                            color = if (isSelected) DrawerRoyalBlue else Color.Transparent,
                                            shape = RoundedCornerShape(14.dp)
                                        )
                                        .clickable {
                                            onCloseDrawer()
                                            onSelectPanel(panel)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                        .testTag("drawer_panel_${panel.name.lowercase()}"),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(RoundedCornerShape(11.dp))
                                            .background(badgeBg),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = panelIcon(panel),
                                            contentDescription = panel.title,
                                            tint = badgeTint,
                                            modifier = Modifier.size(21.dp)
                                        )
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = panel.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                color = DrawerNavy,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                            if (panel.isAdminOnly) {
                                                Spacer(Modifier.width(5.dp))
                                                Icon(
                                                    imageVector = if (appState.isAdminAuthorized) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                                                    contentDescription = "Admin restricted",
                                                    tint = OrangeAccent,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = if (panel.isAdminOnly && !appState.isAdminAuthorized) "(Authorized Only)" else panel.subtitle,
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.5.sp),
                                            color = MutedText,
                                            maxLines = 1
                                        )
                                    }
                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(50))
                                                .background(DrawerRoyalBlue)
                                                .padding(horizontal = 7.dp, vertical = 2.dp)
                                        ) {
                                            Text("ON", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 10.dp),
                                color = Color(0xFFE2E8F0)
                            )

                            Text(
                                text = "More Options",
                                style = MaterialTheme.typography.titleSmall,
                                color = DrawerNavy,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            val moreOptions = listOf(
                                Triple("My Orders", Icons.Rounded.ReceiptLong, AppRoute.OrderHistory("ALL")),
                                Triple("My Wallet (₹${appState.walletBalance.toInt()})", Icons.Rounded.AccountBalanceWallet, AppRoute.Wallet),
                                Triple("Coupons & Offers", Icons.Rounded.LocalOffer, AppRoute.CouponsOffers),
                                Triple("Notifications${if (unreadNotifications > 0) " ($unreadNotifications)" else ""}", Icons.Rounded.Notifications, AppRoute.Notifications),
                                Triple("Settings", Icons.Rounded.Settings, AppRoute.Profile),
                                Triple("Help & Support", Icons.AutoMirrored.Rounded.HelpOutline, AppRoute.Support)
                            )

                            moreOptions.forEach { (label, icon, route) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            onCloseDrawer()
                                            onNavigate(route)
                                        }
                                        .padding(horizontal = 8.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(icon, contentDescription = null, tint = DrawerNavy, modifier = Modifier.size(20.dp))
                                    Spacer(Modifier.width(12.dp))
                                    Text(label, style = MaterialTheme.typography.bodyMedium, color = DrawerNavy, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                                    Icon(Icons.Rounded.ChevronRight, null, tint = MutedText, modifier = Modifier.size(18.dp))
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        onCloseDrawer()
                                        onLogoutDemo()
                                    }
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                                    .navigationBarsPadding(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, tint = CoralDanger, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(12.dp))
                                Text("Logout", style = MaterialTheme.typography.titleSmall, color = CoralDanger, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun drawerPanelColors(panel: AppPanel): Pair<Color, Color> = when (panel) {
    AppPanel.CUSTOMER -> Color(0xFFDBEAFE) to Color(0xFF2563EB)
    AppPanel.DRIVER_CAPTAIN -> Color(0xFFFEF08A) to Color(0xFF1E293B)
    AppPanel.FOOD_DELIVERY_PARTNER -> Color(0xFFF97316) to SoftWhite
    AppPanel.RESTAURANT_OWNER -> Color(0xFFF59E0B) to DrawerNavy
    AppPanel.GROCERY_OWNER -> Color(0xFFFEF08A) to Color(0xFF15803D)
    AppPanel.PARCEL_PARTNER -> Color(0xFF2563EB) to SoftWhite
    AppPanel.TRANSPORT_PARTNER -> Color(0xFF38BDF8) to DrawerNavy
    AppPanel.BUSINESS_OWNER -> Color(0xFF7C3AED) to SoftWhite
    AppPanel.ADMIN -> Color(0xFFF59E0B) to DrawerNavy
}

fun panelIcon(panel: AppPanel): ImageVector = when (panel) {
    AppPanel.CUSTOMER -> Icons.Rounded.TwoWheeler
    AppPanel.DRIVER_CAPTAIN -> Icons.Rounded.PersonPinCircle
    AppPanel.FOOD_DELIVERY_PARTNER -> Icons.Rounded.DeliveryDining
    AppPanel.RESTAURANT_OWNER -> Icons.Rounded.Storefront
    AppPanel.GROCERY_OWNER -> Icons.Rounded.LocalGroceryStore
    AppPanel.PARCEL_PARTNER -> Icons.Rounded.Inventory2
    AppPanel.TRANSPORT_PARTNER -> Icons.Rounded.LocalShipping
    AppPanel.BUSINESS_OWNER -> Icons.Rounded.BusinessCenter
    AppPanel.ADMIN -> Icons.Rounded.AdminPanelSettings
}
