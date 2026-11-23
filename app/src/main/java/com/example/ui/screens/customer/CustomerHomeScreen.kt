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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DemoCatalog
import com.example.model.AppRoute
import com.example.model.SavedAddress
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState

private val MainSkyTop = Color(0xFFDCEEFF)
private val MainSkyMid = Color(0xFFF2F7FF)
private val MainRoyalBlue = Color(0xFF2563EB)
private val MainElectricPurple = Color(0xFF7C3AED)
private val MainGoldYellow = Color(0xFFFFD43B)

private data class TrendingChipItem(
    val label: String,
    val icon: ImageVector,
    val tint: Color,
    val bgStart: Color,
    val bgEnd: Color,
    val route: AppRoute
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSelectAddress: (SavedAddress) -> Unit,
    onAddSearch: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var showLocationSheet by remember { mutableStateOf(false) }
    var showMoreServicesSheet by remember { mutableStateOf(false) }
    var activeBottomTab by remember { mutableIntStateOf(0) }

    val trendingItems = remember {
        listOf(
            TrendingChipItem("Pizza", Icons.Rounded.LocalPizza, Color(0xFFEA580C), Color(0xFFFFEDD5), Color(0xFFFED7AA), AppRoute.FoodDiscovery),
            TrendingChipItem("Ride", Icons.Rounded.TwoWheeler, Color(0xFF1D4ED8), Color(0xFFDBEAFE), Color(0xFFBFDBFE), AppRoute.RideBooking),
            TrendingChipItem("Trucks", Icons.Rounded.LocalShipping, Color(0xFF0F766E), Color(0xFFCCFBF1), Color(0xFF99F6E4), AppRoute.GoodsTransport),
            TrendingChipItem("Cafe", Icons.Rounded.LocalCafe, Color(0xFF92400E), Color(0xFFFEF3C7), Color(0xFFFDE68A), AppRoute.FoodDiscovery),
            TrendingChipItem("Cake", Icons.Rounded.Cake, Color(0xFFDB2777), Color(0xFFFCE7F3), Color(0xFFFBCFE8), AppRoute.FoodDiscovery),
            TrendingChipItem("Gifts", Icons.Rounded.CardGiftcard, Color(0xFFD97706), Color(0xFFFEF9C3), Color(0xFFFDE047), AppRoute.LocalShopping),
            TrendingChipItem("Electronics", Icons.Rounded.Headphones, Color(0xFF1E3A8A), Color(0xFFE0E7FF), Color(0xFFC7D2FE), AppRoute.LocalShopping),
            TrendingChipItem("Fashion", Icons.Rounded.Checkroom, Color(0xFFE11D48), Color(0xFFFFE4E6), Color(0xFFFECDD3), AppRoute.LocalShopping),
            TrendingChipItem("Mobiles", Icons.Rounded.Smartphone, Color(0xFF2563EB), Color(0xFFE0F2FE), Color(0xFFBAE6FD), AppRoute.LocalShopping)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    0.0f to MainSkyTop,
                    0.26f to MainSkyMid,
                    1.0f to SoftWhite
                )
            )
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. AIRY PASTEL-SKY TOP BAR (Matches Image 7 Header)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Brand Bag Emblem (Tap opens Universal 9-Panel Drawer)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(6.dp, RoundedCornerShape(15.dp), spotColor = MainElectricPurple)
                                .clip(RoundedCornerShape(15.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), MainRoyalBlue, MainElectricPurple)))
                                .clickable(onClick = onOpenDrawer)
                                .testTag("open_drawer_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ShoppingBag,
                                contentDescription = "Open Menu",
                                tint = SoftWhite,
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showLocationSheet = true }
                                .testTag("change_location_button")
                        ) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = DeepNavy, fontWeight = FontWeight.Black)) { append("Take ") }
                                    withStyle(SpanStyle(color = MainRoyalBlue, fontWeight = FontWeight.Black)) { append("It") }
                                },
                                fontSize = 23.sp,
                                lineHeight = 25.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "All Services in One Place • ${uiState.appState.activeAddressLabel}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SlateText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = MainRoyalBlue, modifier = Modifier.size(15.dp))
                            }
                        }

                        // Notification Bell with Red Counter Pill
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onNavigate(AppRoute.Notifications) }
                                .testTag("notifications_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(SoftWhite)
                                    .border(1.dp, Color(0xFFDBEAFE), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.NotificationsNone, "Notifications", tint = MainRoyalBlue, modifier = Modifier.size(22.dp))
                            }
                            if (uiState.unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 3.dp, end = 3.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFFEF4444)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "${uiState.unreadNotificationCount}",
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = SoftWhite,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(4.dp))

                        // Circular Profile Avatar with Green Online Status Dot
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { onNavigate(AppRoute.Profile) }
                                .testTag("profile_avatar_button")
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(MainRoyalBlue, DeepNavy)))
                                    .border(2.dp, SoftWhite, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.appState.userName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MainGoldYellow,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .size(12.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                                    .border(2.dp, SoftWhite, CircleShape)
                            )
                        }
                    }

                    // 2. PILL SEARCH BAR WITH MIC & SCANNER (Exact Image 7 Style)
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = SoftWhite,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE0ECFF), RoundedCornerShape(50))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Search, null, tint = DeepNavy, modifier = Modifier.size(22.dp))
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = {
                                    Text("Search services, products...", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                                },
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent,
                                    focusedTextColor = DeepNavy,
                                    unfocusedTextColor = DeepNavy
                                ),
                                modifier = Modifier.weight(1f).testTag("home_search_input")
                            )
                            IconButton(
                                onClick = {
                                    onAddSearch("Pizza")
                                    onShowMessage("Voice Search ready — opening Food Delivery")
                                    onNavigate(AppRoute.FoodDiscovery)
                                },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Rounded.Mic, "Voice search", tint = DeepNavy, modifier = Modifier.size(20.dp))
                            }
                            Box(Modifier.width(1.dp).height(20.dp).background(Color(0xFFE2E8F0)))
                            IconButton(
                                onClick = { onShowMessage("Smart QR & Product Scanner ready") },
                                modifier = Modifier.size(38.dp)
                            ) {
                                Icon(Icons.Rounded.QrCodeScanner, "Scan", tint = DeepNavy, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }

            // Live Quick Search Suggestions
            if (searchQuery.isNotBlank()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SoftWhite,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "Food Delivery • Order '$searchQuery'" to AppRoute.FoodDiscovery,
                                "Instant Grocery • Find '$searchQuery'" to AppRoute.GroceryStore,
                                "Book a Ride to '$searchQuery'" to AppRoute.RideBooking,
                                "Local Shopping • Shop '$searchQuery'" to AppRoute.LocalShopping
                            ).forEach { (title, route) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF8FAFC))
                                        .clickable {
                                            onAddSearch(searchQuery)
                                            onNavigate(route)
                                        }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Search, null, tint = MainRoyalBlue, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(title, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                }
                            }
                        }
                    }
                }
            }

            // 3. VIBRANT BLUE-TO-PURPLE HERO BANNER ("Discover More with Take It!" — Image 7 Exact Look)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(166.dp)
                            .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = MainRoyalBlue)
                            .clip(RoundedCornerShape(24.dp))
                            .clickable { onNavigate(AppRoute.LocalShopping) }
                            .testTag("home_hero_banner")
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_takeit_main_hero_1795429839427),
                            contentDescription = "Discover More with Take It",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        0.0f to Color(0xD81D4ED8),
                                        0.52f to Color(0xAA2563EB),
                                        0.82f to Color.Transparent
                                    )
                                )
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(0.64f)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = buildAnnotatedString {
                                        withStyle(SpanStyle(color = SoftWhite, fontWeight = FontWeight.ExtraBold)) { append("Discover More\nwith ") }
                                        withStyle(SpanStyle(color = MainGoldYellow, fontWeight = FontWeight.Black)) { append("Take It!") }
                                    },
                                    fontSize = 21.sp,
                                    lineHeight = 25.sp
                                )
                                Text(
                                    text = "Shop • Book • Deliver • Travel",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFE0ECFF)
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(MainGoldYellow)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Explore Now", style = MaterialTheme.typography.labelLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = DeepNavy, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    // Carousel indicator dots matching Image 7
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(22.dp).height(5.dp).clip(RoundedCornerShape(50)).background(MainRoyalBlue))
                        repeat(4) {
                            Box(Modifier.size(5.dp).clip(CircleShape).background(Color(0xFFBFDBFE)))
                        }
                    }
                }
            }

            // 4. PASTEL 3×2 SERVICE CARDS GRID (EXACT Image 7 Layout & Badges!)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PastelServiceCard(
                            title = "Food Delivery",
                            subtitle = "Order Delicious Food",
                            badgeText = "30% OFF",
                            badgeColor = Color(0xFFEF4444),
                            bgStart = Color(0xFFFFF1EB),
                            bgEnd = Color(0xFFFFD8C8),
                            borderColor = Color(0xFFFECACA),
                            imageRes = R.drawable.img_takeit_hero_burger_1795427328311,
                            tag = "service_card_food",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppRoute.FoodDiscovery) }
                        )
                        PastelServiceCard(
                            title = "Grocery",
                            subtitle = "Fresh & Healthy",
                            badgeText = "Up to 50%",
                            badgeColor = Color(0xFF16A34A),
                            bgStart = Color(0xFFECFDF5),
                            bgEnd = Color(0xFFBBF7D0),
                            borderColor = Color(0xFF86EFAC),
                            imageRes = R.drawable.img_grocery_basket_1795421415559,
                            tag = "service_card_grocery",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppRoute.GroceryStore) }
                        )
                        PastelServiceCard(
                            title = "Parcel",
                            subtitle = "Send & Receive",
                            badgeText = null,
                            badgeColor = MainRoyalBlue,
                            bgStart = Color(0xFFEFF6FF),
                            bgEnd = Color(0xFFBFDBFE),
                            borderColor = Color(0xFF93C5FD),
                            imageRes = R.drawable.img_takeit_parcel_truck_1795427329625,
                            tag = "service_card_parcel",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppRoute.ParcelDelivery) }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        PastelServiceCard(
                            title = "Ride & Cab",
                            subtitle = "Ride & Travel",
                            badgeText = "Fast",
                            badgeColor = MainRoyalBlue,
                            bgStart = Color(0xFFEEF2FF),
                            bgEnd = Color(0xFFC7D2FE),
                            borderColor = Color(0xFFA5B4FC),
                            imageRes = R.drawable.img_takeit_ride_city_hero_1795429838137,
                            tag = "service_card_ride",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppRoute.RideBooking) }
                        )
                        PastelServiceCard(
                            title = "Local Shop",
                            subtitle = "Shop Local",
                            badgeText = null,
                            badgeColor = MainElectricPurple,
                            bgStart = Color(0xFFFDF2F8),
                            bgEnd = Color(0xFFFBCFE8),
                            borderColor = Color(0xFFF9A8D4),
                            imageRes = R.drawable.img_takeit_local_shop_hero_1795429839958,
                            tag = "service_card_shopping",
                            modifier = Modifier.weight(1f),
                            onClick = { onNavigate(AppRoute.LocalShopping) }
                        )
                        PastelMoreServicesCard(
                            modifier = Modifier.weight(1f),
                            onClick = { showMoreServicesSheet = true }
                        )
                    }
                }
            }

            // 5. PURPLE-TO-ORANGE PROMO BANNER ("Flat 50% OFF on your first order" — Image 7)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(94.dp)
                        .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = MainElectricPurple)
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF5B21B6), Color(0xFF9333EA), Color(0xFFF97316), Color(0xFFFACC15))
                            )
                        )
                        .clickable { onNavigate(AppRoute.FoodDiscovery) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = SoftWhite, fontWeight = FontWeight.Black)) { append("Flat ") }
                                    withStyle(SpanStyle(color = MainGoldYellow, fontWeight = FontWeight.Black)) { append("50% OFF") }
                                },
                                fontSize = 21.sp
                            )
                            Text(
                                text = "on your first order",
                                style = MaterialTheme.typography.labelLarge,
                                color = SoftWhite.copy(alpha = 0.92f)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MainGoldYellow)
                                .padding(horizontal = 12.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Order Now →", style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.width(8.dp))
                        Character3DPortrait(
                            drawableRes = R.drawable.img_char_food_rider_1795426023084,
                            contentDescription = "Delivery Rider",
                            borderColor = MainGoldYellow,
                            size = 62.dp,
                            bobbing = false
                        )
                    }
                }
            }

            // 6. TRENDING SERVICES 🔥 HORIZONTAL CIRCULAR ROW (Image 7 Exact Style)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("🔥 Trending Services", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        Text(
                            text = "See All →",
                            style = MaterialTheme.typography.labelLarge,
                            color = MainRoyalBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showMoreServicesSheet = true }
                        )
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(trendingItems) { chip ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(60.dp)
                                    .clickable { onNavigate(chip.route) }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .shadow(3.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(chip.bgStart, chip.bgEnd)))
                                        .border(1.5.dp, SoftWhite, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(chip.icon, contentDescription = chip.label, tint = chip.tint, modifier = Modifier.size(26.dp))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = chip.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepNavy,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            // 7. SIDE-BY-SIDE "TRACK YOUR ORDER" & "EXCLUSIVE DEALS" CARDS (Image 7 Bottom Cards)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Track Your Order (Mint Green Card)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(114.dp)
                            .shadow(5.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFF22C55E))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFECFCCB), Color(0xFFBBF7D0))))
                            .border(1.dp, Color(0xFF86EFAC), RoundedCornerShape(20.dp))
                            .clickable { onNavigate(AppRoute.OrderHistory("ALL")) }
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Track Your Order", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                Text("Real-time Updates", style = MaterialTheme.typography.labelSmall, color = Color(0xFF166534))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(Color(0xFF15803D))
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("Track Now →", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                }
                                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFEF4444), modifier = Modifier.size(28.dp))
                            }
                        }
                    }

                    // Exclusive Deals (Sky Blue Card)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(114.dp)
                            .shadow(5.dp, RoundedCornerShape(20.dp), spotColor = MainRoyalBlue)
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFFE0F2FE), Color(0xFFBFDBFE))))
                            .border(1.dp, Color(0xFF93C5FD), RoundedCornerShape(20.dp))
                            .clickable { onNavigate(AppRoute.CouponsOffers) }
                            .padding(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Exclusive Deals", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                Text("Save More Everyday", style = MaterialTheme.typography.labelSmall, color = Color(0xFF1E3A8A))
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(MainRoyalBlue)
                                        .padding(horizontal = 10.dp, vertical = 5.dp)
                                ) {
                                    Text("View Deals →", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                }
                                Icon(Icons.Rounded.CardGiftcard, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(28.dp))
                            }
                        }
                    }
                }
            }

            // 8. COMPACT 9-PANEL & GOODS TRANSPORT QUICK STRIP
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = DeepNavy,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable(onClick = onOpenDrawer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MainGoldYellow),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.DashboardCustomize, null, tint = DeepNavy, modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("9-in-1 Take It Ecosystem", style = MaterialTheme.typography.titleSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                            Text("Switch Driver, Restaurant, Grocery, Logistics & Admin", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBFDBFE))
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MainGoldYellow)
                    }
                }
            }
        }

        // FLOATING WHITE CURVED BOTTOM BAR WITH CENTER BLUE CART FAB (Exact Image 7 Look!)
        TakeItCenterCartBottomBar(
            activeIndex = activeBottomTab,
            cartCount = uiState.cartItemCount,
            modifier = Modifier.align(Alignment.BottomCenter),
            onSelect = { idx ->
                activeBottomTab = idx
                when (idx) {
                    0 -> Unit
                    1 -> showMoreServicesSheet = true
                    2 -> onNavigate(AppRoute.CartCheckout)
                    3 -> onNavigate(AppRoute.OrderHistory("ALL"))
                    4 -> onNavigate(AppRoute.Profile)
                }
            }
        )
    }

    // Location Picker Sheet
    if (showLocationSheet) {
        ModalBottomSheet(onDismissRequest = { showLocationSheet = false }, containerColor = SoftWhite) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Delivery Location", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                val addresses = listOf(
                    SavedAddress("Sector 62, Noida", "Sector 62, Noida • Electronic City Metro", "Default"),
                    SavedAddress("Greater Noida", "Pari Chowk, Greater Noida, UP", "Hub"),
                    *DemoCatalog.savedAddresses.toTypedArray()
                )
                addresses.forEach { addr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (uiState.appState.activeAddressLabel == addr.label) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(1.dp, if (uiState.appState.activeAddressLabel == addr.label) MainRoyalBlue else LightBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectAddress(addr)
                                showLocationSheet = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = MainRoyalBlue)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(addr.label, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                            Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                }
            }
        }
    }

    // All Services & Quick Panel Sheet (Goods Transport, Wallet, Refer, Support, 9 Panels)
    if (showMoreServicesSheet) {
        ModalBottomSheet(onDismissRequest = { showMoreServicesSheet = false }, containerColor = SoftWhite) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("All Take It Services", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                listOf(
                    Triple("Goods Transport & Mini Trucks", "Tata Ace, Pickup 8ft, Heavy Movers", AppRoute.GoodsTransport),
                    Triple("Book a Ride (Bike, Auto, Cab)", "Fastest daily commute & outstation", AppRoute.RideBooking),
                    Triple("Express Parcel Delivery", "Send documents & packages citywide", AppRoute.ParcelDelivery),
                    Triple("Food Delivery", "Top restaurants with 50% OFF", AppRoute.FoodDiscovery),
                    Triple("Instant Grocery (10 Min)", "Farm-fresh fruits, veggies & dairy", AppRoute.GroceryStore),
                    Triple("Local Shopping", "Nearby fashion, electronics & deals", AppRoute.LocalShopping),
                    Triple("Take It Wallet (₹${uiState.appState.walletBalance.toInt()})", "Cashback, UPI top-up & refunds", AppRoute.Wallet),
                    Triple("Refer & Earn ₹250", "Invite friends to Take It", AppRoute.ReferEarn)
                ).forEach { (title, sub, route) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFF8FAFC))
                            .clickable {
                                showMoreServicesSheet = false
                                onNavigate(route)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                            Text(sub, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = MainRoyalBlue)
                    }
                }
                Button(
                    onClick = {
                        showMoreServicesSheet = false
                        onOpenDrawer()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = MainGoldYellow)
                ) {
                    Text("Switch Partner / Admin Panel (9 Modes)", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun PastelServiceCard(
    title: String,
    subtitle: String,
    badgeText: String?,
    badgeColor: Color,
    bgStart: Color,
    bgEnd: Color,
    borderColor: Color,
    imageRes: Int,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(142.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = borderColor)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(bgStart, bgEnd)))
            .border(1.dp, borderColor, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .clip(RoundedCornerShape(14.dp))
            ) {
                Image(
                    painter = painterResource(imageRes),
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (badgeText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(50))
                            .background(badgeColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                            color = SoftWhite,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepNavy,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = SlateText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(SoftWhite),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = DeepNavy, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun PastelMoreServicesCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(142.dp)
            .shadow(4.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFC4B5FD))
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFFF5F3FF), Color(0xFFE9D5FF))))
            .border(1.dp, Color(0xFFD8B4FE), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .testTag("service_card_more")
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().height(72.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF6D28D9)))
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF7C3AED)))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF7C3AED)))
                        Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(Color(0xFF5B21B6)))
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("More Services", style = MaterialTheme.typography.labelLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("Explore More", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = SlateText, maxLines = 1)
                }
                Box(Modifier.size(22.dp).clip(CircleShape).background(SoftWhite), contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = DeepNavy, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun TakeItCenterCartBottomBar(
    activeIndex: Int,
    cartCount: Int,
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = SoftWhite,
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .border(1.dp, Color(0xFFE0ECFF), RoundedCornerShape(26.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BottomNavItemMini("Home", Icons.Rounded.Home, activeIndex == 0, Modifier.weight(1f)) { onSelect(0) }
                BottomNavItemMini("Services", Icons.Rounded.GridView, activeIndex == 1, Modifier.weight(1f)) { onSelect(1) }
                Spacer(Modifier.width(72.dp))
                BottomNavItemMini("Orders", Icons.Rounded.FavoriteBorder, activeIndex == 3, Modifier.weight(1f)) { onSelect(3) }
                BottomNavItemMini("Profile", Icons.Rounded.PersonOutline, activeIndex == 4, Modifier.weight(1f)) { onSelect(4) }
            }
        }

        // Elevated Center Glowing Blue Cart Button (Exact Image 7 Signature!)
        Box(
            modifier = Modifier
                .offset(y = (-14).dp)
                .size(64.dp)
                .shadow(12.dp, CircleShape, spotColor = MainRoyalBlue)
                .clip(CircleShape)
                .background(SoftWhite)
                .padding(5.dp)
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF38BDF8), MainRoyalBlue)))
                .clickable { onSelect(2) }
                .testTag("bottom_center_cart_button"),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.ShoppingCart, contentDescription = "Cart", tint = SoftWhite, modifier = Modifier.size(28.dp))
            if (cartCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEF4444)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$cartCount", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun BottomNavItemMini(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick)
    ) {
        Icon(icon, contentDescription = label, tint = if (selected) MainRoyalBlue else Color(0xFF64748B), modifier = Modifier.size(22.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (selected) MainRoyalBlue else Color(0xFF64748B),
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium
        )
    }
}
