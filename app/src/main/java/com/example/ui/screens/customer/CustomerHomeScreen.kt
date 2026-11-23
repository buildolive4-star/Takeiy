package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.launch

private val TakeItNavy = Color(0xFF0B1F4B)
private val TakeItNavyLight = Color(0xFF153473)
private val TakeItRoyalBlue = Color(0xFF2563EB)

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
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }

    val activeOrders = remember(uiState.orders) {
        uiState.orders.filter { it.status in setOf("PENDING", "ACCEPTED", "PREPARING", "ON_THE_WAY") }
    }

    Scaffold(
        containerColor = Color(0xFFF4F7FB),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            TakeItBottomNavBar(
                activeIndex = 0,
                onSelect = { idx ->
                    when (idx) {
                        0 -> scope.launch { listState.animateScrollToItem(0) }
                        1 -> onNavigate(AppRoute.OrderHistory("ALL"))
                        2 -> onNavigate(AppRoute.Wallet)
                        3 -> onNavigate(AppRoute.Support)
                        4 -> onNavigate(AppRoute.Profile)
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. DEEP NAVY CURVED HEADER (HAMBURGER | TAKE IT LOGO | BELL & AVATAR + LOCATION + SEARCH)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))
                        .background(Brush.verticalGradient(listOf(TakeItNavy, TakeItNavyLight)))
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(13.dp))
                                .background(SoftWhite.copy(alpha = 0.12f))
                                .testTag("hamburger_menu_button")
                        ) {
                            Icon(Icons.Rounded.Menu, "Open Menu", tint = SoftWhite, modifier = Modifier.size(26.dp))
                        }

                        // Centered Take It Brand Logo
                        Row(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(RoundedCornerShape(9.dp))
                                    .background(Brush.linearGradient(listOf(PremiumYellow, OrangeAccent))),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Bolt, null, tint = TakeItNavy, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = buildAnnotatedString {
                                    withStyle(SpanStyle(color = SoftWhite, fontWeight = FontWeight.ExtraBold)) { append("Take ") }
                                    withStyle(SpanStyle(color = PremiumYellow, fontWeight = FontWeight.ExtraBold)) { append("It") }
                                },
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp)
                            )
                        }

                        // Bell
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .clickable { onNavigate(AppRoute.Notifications) }
                                .testTag("notification_bell_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.NotificationsNone, "Notifications", tint = SoftWhite, modifier = Modifier.size(26.dp))
                            if (uiState.unreadNotificationCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(7.dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(CoralDanger)
                                )
                            }
                        }

                        Spacer(Modifier.width(4.dp))

                        // Circular User Avatar
                        Image(
                            painter = painterResource(R.drawable.img_char_ride_captain_1795426023899),
                            contentDescription = "Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .border(1.8.dp, PremiumYellow, CircleShape)
                                .clickable { onNavigate(AppRoute.Profile) }
                                .testTag("profile_avatar_button")
                        )
                    }

                    // Delivering To Row with Solid Yellow "Change" Pill
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showLocationSheet = true }
                            .testTag("location_selector_bar"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = PremiumYellow, modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Delivering to", style = MaterialTheme.typography.labelSmall, color = Color(0xFFCBD5E1))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = uiState.appState.activeAddressLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SoftWhite,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = SoftWhite, modifier = Modifier.size(18.dp))
                            }
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PremiumYellow)
                                .clickable { showLocationSheet = true }
                                .padding(horizontal = 16.dp, vertical = 7.dp)
                        ) {
                            Text("Change", style = MaterialTheme.typography.labelMedium, color = TakeItNavy, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // Crisp White Rounded Search Bar
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SoftWhite,
                        shadowElevation = 4.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, "Search", tint = Color(0xFF475569), modifier = Modifier.size(22.dp))
                                TextField(
                                    value = searchQuery,
                                    onValueChange = {
                                        searchQuery = it
                                        searchExpanded = true
                                    },
                                    placeholder = {
                                        Text(
                                            "Search rides, food, groceries...",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MutedText,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        focusedIndicatorColor = Color.Transparent,
                                        unfocusedIndicatorColor = Color.Transparent,
                                        focusedTextColor = TakeItNavy,
                                        unfocusedTextColor = TakeItNavy
                                    ),
                                    modifier = Modifier.weight(1f).testTag("home_search_input")
                                )
                                IconButton(
                                    onClick = {
                                        if (searchQuery.isNotBlank()) {
                                            onAddSearch(searchQuery)
                                            routeSearchQuery(searchQuery, onNavigate)
                                            searchExpanded = false
                                        } else {
                                            searchExpanded = !searchExpanded
                                            onShowMessage("Voice Search ready: Say 'Book Ride', 'Pizza' or 'Grocery'")
                                        }
                                    },
                                    modifier = Modifier.testTag("voice_search_button")
                                ) {
                                    Icon(
                                        if (searchQuery.isNotBlank()) Icons.Rounded.ArrowForward else Icons.Rounded.Mic,
                                        contentDescription = "Voice search",
                                        tint = TakeItNavy
                                    )
                                }
                            }
                            AnimatedVisibility(visible = searchExpanded) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    HorizontalDivider(color = LightBorder)
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(listOf("Bike Taxi", "Pizza", "9-Min Milk", "Send Parcel", "Mini Truck", "Local Shop")) { pop ->
                                            AssistChip(
                                                onClick = {
                                                    onAddSearch(pop)
                                                    searchExpanded = false
                                                    routeSearchQuery(pop, onNavigate)
                                                },
                                                label = { Text(pop, style = MaterialTheme.typography.labelSmall) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. HERO BANNER: "Everything You Need in One App!" WITH 3D BURGER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(164.dp)
                        .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = TakeItNavy)
                        .clip(RoundedCornerShape(22.dp))
                        .background(TakeItNavy)
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_hero_burger_1795427328311),
                        contentDescription = "Take It Everything You Need in One App Hero",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(
                                0.0f to Color(0xEE091B42),
                                0.52f to Color(0xBB0E2A66),
                                1.0f to Color.Transparent
                            )
                        )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.66f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Everything You Need",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "in One App!",
                                style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                                color = PremiumYellow,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.height(3.dp))
                            Text(
                                text = "Ride • Eat • Shop • Deliver",
                                style = MaterialTheme.typography.labelSmall,
                                color = SoftWhite.copy(alpha = 0.88f)
                            )
                        }
                        Button(
                            onClick = { scope.launch { listState.animateScrollToItem(2) } },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumYellow, contentColor = TakeItNavy),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp).testTag("hero_explore_button")
                        ) {
                            Text("Explore Now  →", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 3. VIBRANT 3x2 COLORFUL SERVICE GRID (EXACT MOCKUP STYLE!)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        TakeItServiceTile(
                            title = "Ride",
                            subtitle = "Book a Ride",
                            bgStart = Color(0xFF3B82F6),
                            bgEnd = Color(0xFF1D4ED8),
                            imageRes = R.drawable.img_char_ride_captain_1795426023899,
                            modifier = Modifier.weight(1f).testTag("service_card_ride"),
                            onClick = { onNavigate(AppRoute.RideBooking) }
                        )
                        TakeItServiceTile(
                            title = "Food",
                            subtitle = "Order Now",
                            bgStart = Color(0xFFFB923C),
                            bgEnd = Color(0xFFEA580C),
                            imageRes = R.drawable.img_char_food_rider_1795426023084,
                            modifier = Modifier.weight(1f).testTag("service_card_food"),
                            onClick = { onNavigate(AppRoute.FoodDiscovery) }
                        )
                        TakeItServiceTile(
                            title = "Grocery",
                            subtitle = "Shop Now",
                            bgStart = Color(0xFF4ADE80),
                            bgEnd = Color(0xFF16A34A),
                            imageRes = R.drawable.img_char_grocery_buddy_1795426023736,
                            modifier = Modifier.weight(1f).testTag("service_card_grocery"),
                            onClick = { onNavigate(AppRoute.GroceryStore) }
                        )
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        TakeItServiceTile(
                            title = "Parcel",
                            subtitle = "Send Now",
                            bgStart = Color(0xFFA855F7),
                            bgEnd = Color(0xFF6D28D9),
                            imageRes = R.drawable.img_char_porter_mover_1795426024294,
                            modifier = Modifier.weight(1f).testTag("service_card_parcel"),
                            onClick = { onNavigate(AppRoute.ParcelDelivery) }
                        )
                        TakeItServiceTile(
                            title = "Transport",
                            subtitle = "Book Transport",
                            bgStart = Color(0xFF60A5FA),
                            bgEnd = Color(0xFF1E3A8A),
                            imageRes = R.drawable.img_takeit_parcel_truck_1795427329625,
                            modifier = Modifier.weight(1f).testTag("service_card_transport"),
                            onClick = { onNavigate(AppRoute.GoodsTransport) }
                        )
                        // Vibrant Pink/Crimson "More" Grid Tile
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(138.dp)
                                .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = Color(0xFFE11D48))
                                .clip(RoundedCornerShape(20.dp))
                                .background(Brush.verticalGradient(listOf(Color(0xFFFB7185), Color(0xFFE11D48))))
                                .bounceClick { onNavigate(AppRoute.LocalShopping) }
                                .padding(12.dp)
                                .testTag("service_card_shopping")
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier.size(60.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.GridView, "More Services", tint = SoftWhite, modifier = Modifier.size(48.dp))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("More", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                    Text("Explore All", style = MaterialTheme.typography.labelSmall, color = SoftWhite.copy(alpha = 0.9f))
                                }
                            }
                        }
                    }
                }
            }

            // 4. HOT OFFERS 🔥 GOLDEN PIZZA BANNER (EXACT MOCKUP!)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Hot Offers 🔥", style = MaterialTheme.typography.titleLarge, color = TakeItNavy, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "See All →",
                            style = MaterialTheme.typography.labelLarge,
                            color = TakeItRoyalBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { onNavigate(AppRoute.CouponsOffers) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(118.dp)
                            .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = OrangeAccent)
                            .clip(RoundedCornerShape(20.dp))
                            .background(PremiumYellow)
                            .bounceClick { onNavigate(AppRoute.FoodDiscovery) }
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_takeit_pizza_offer_1795427326877),
                            contentDescription = "Flat 50% OFF on Food Orders",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Column(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(0.58f)
                                .padding(horizontal = 18.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Flat 50% OFF",
                                style = MaterialTheme.typography.headlineMedium,
                                color = TakeItNavy,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Text(
                                text = "on Food Orders",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFF451A03),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 5. LIVE ACTIVE ORDER TRACKER + 9-PANEL SWITCHER PILL
            if (activeOrders.isNotEmpty()) {
                item {
                    val live = activeOrders.first()
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SoftWhite,
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .border(1.2.dp, TakeItRoyalBlue.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .bounceClick { onNavigate(AppRoute.OrderHistory("ALL")) }
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Character3DPortrait(
                                    drawableRes = R.drawable.img_char_food_rider_1795426023084,
                                    contentDescription = "Active Partner",
                                    borderColor = SuccessGreen,
                                    size = 46.dp,
                                    bobbing = false
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("LIVE ORDER • ${live.title}", style = MaterialTheme.typography.titleSmall, color = TakeItNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("Partner ${live.partnerName} • OTP ${live.otp}", style = MaterialTheme.typography.bodySmall, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                }
                                StatusPill(live.status)
                            }
                            LiveOrderMilestoneBar(status = live.status)
                        }
                    }
                }
            }

            // 6. SWITCH 9 PANELS CARD
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(TakeItNavy, Color(0xFF1E3A8A))))
                        .bounceClick { onOpenDrawer() }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("home_mode_switcher_card"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.DashboardCustomize, null, tint = PremiumYellow, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Switch Panel (9 Modes Inside)", style = MaterialTheme.typography.titleSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        Text("Captain, Delivery, Restaurant, Grocery, Logistics & Admin", style = MaterialTheme.typography.labelSmall, color = Color(0xFFBFDBFE), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Box(Modifier.clip(RoundedCornerShape(50)).background(PremiumYellow).padding(horizontal = 12.dp, vertical = 5.dp)) {
                        Text("Switch", style = MaterialTheme.typography.labelSmall, color = TakeItNavy, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }

    // Address Picker Sheet
    if (showLocationSheet) {
        ModalBottomSheet(onDismissRequest = { showLocationSheet = false }, containerColor = SoftWhite) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Delivery Location", style = MaterialTheme.typography.titleLarge, color = TakeItNavy, fontWeight = FontWeight.ExtraBold)
                val addresses = listOf(
                    SavedAddress("Sector 62, Noida", "Sector 62, Noida • Electronic City Metro", "Default"),
                    *DemoCatalog.savedAddresses.toTypedArray()
                )
                addresses.forEach { addr ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (uiState.appState.activeAddressLabel == addr.label) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(1.dp, if (uiState.appState.activeAddressLabel == addr.label) TakeItRoyalBlue else LightBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectAddress(addr)
                                showLocationSheet = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = TakeItRoyalBlue)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(addr.label, style = MaterialTheme.typography.titleSmall, color = TakeItNavy, fontWeight = FontWeight.Bold)
                            Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TakeItServiceTile(
    title: String,
    subtitle: String,
    bgStart: Color,
    bgEnd: Color,
    imageRes: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(138.dp)
            .shadow(6.dp, RoundedCornerShape(20.dp), spotColor = bgEnd)
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.verticalGradient(listOf(bgStart, bgEnd)))
            .bounceClick(onClick)
            .padding(10.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(imageRes),
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(1.5.dp, SoftWhite.copy(alpha = 0.55f), RoundedCornerShape(16.dp))
            )
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = SoftWhite.copy(alpha = 0.9f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun TakeItBottomNavBar(
    activeIndex: Int,
    onSelect: (Int) -> Unit
) {
    val items = listOf(
        "Home" to Icons.Rounded.Home,
        "Orders" to Icons.Rounded.ReceiptLong,
        "Wallet" to Icons.Rounded.AccountBalanceWallet,
        "Support" to Icons.Rounded.HeadsetMic,
        "Profile" to Icons.Rounded.PersonOutline
    )
    Surface(
        color = SoftWhite,
        shadowElevation = 12.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEachIndexed { idx, (label, icon) ->
                val selected = idx == activeIndex
                val tint = if (selected) TakeItRoyalBlue else Color(0xFF475569)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onSelect(idx) }
                        .padding(vertical = 4.dp)
                        .testTag("bottom_dock_${label.lowercase()}")
                ) {
                    Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(23.dp))
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp),
                        color = tint,
                        fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

private fun routeSearchQuery(query: String, onNavigate: (AppRoute) -> Unit) {
    val q = query.lowercase()
    when {
        q.contains("bike") || q.contains("auto") || q.contains("cab") || q.contains("ride") -> onNavigate(AppRoute.RideBooking)
        q.contains("milk") || q.contains("fruit") || q.contains("grocery") || q.contains("veg") -> onNavigate(AppRoute.GroceryStore)
        q.contains("parcel") || q.contains("courier") -> onNavigate(AppRoute.ParcelDelivery)
        q.contains("truck") || q.contains("ace") || q.contains("cargo") || q.contains("transport") -> onNavigate(AppRoute.GoodsTransport)
        q.contains("shop") || q.contains("earbud") || q.contains("local") -> onNavigate(AppRoute.LocalShopping)
        else -> onNavigate(AppRoute.FoodDiscovery)
    }
}
