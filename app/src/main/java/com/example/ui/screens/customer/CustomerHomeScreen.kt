package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DemoCatalog
import com.example.model.AppRoute
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerHomeScreen(
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onSelectAddress: (com.example.model.SavedAddress) -> Unit,
    onAddSearch: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchExpanded by remember { mutableStateOf(false) }
    var showLocationSheet by remember { mutableStateOf(false) }

    val infinite = rememberInfiniteTransition(label = "homePulse")
    val pinBounce by infinite.animateFloat(
        initialValue = 0f,
        targetValue = -4f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pinBounce"
    )
    val ctaGlow by infinite.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(1100, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ctaGlow"
    )

    val activeOrders = remember(uiState.orders) {
        uiState.orders.filter { it.status in setOf("PENDING", "ACCEPTED", "PREPARING", "ON_THE_WAY") }
    }

    Scaffold(
        containerColor = LightSkyBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (uiState.cartItemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = { onNavigate(AppRoute.CartCheckout) },
                    containerColor = DeepNavy,
                    contentColor = PremiumYellow,
                    modifier = Modifier
                        .navigationBarsPadding()
                        .testTag("home_floating_cart")
                ) {
                    Icon(Icons.Rounded.ShoppingCart, contentDescription = "View Cart")
                    Spacer(Modifier.width(8.dp))
                    Text("${uiState.cartItemCount} items • ₹${uiState.cartGrandTotal}", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            // 1. TOP NAVIGATION BAR + LOCATION HEADER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(DeepNavy, Color(0xFF153A5C)))
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        // Top Row: Hamburger | Logo & Brand | Bell & Avatar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onOpenDrawer,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SoftWhite.copy(alpha = 0.12f))
                                    .testTag("hamburger_menu_button")
                            ) {
                                Icon(
                                    Icons.Rounded.Menu,
                                    contentDescription = "Open Universal Panel Switcher",
                                    tint = PremiumYellow,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(Modifier.width(12.dp))

                            // Center Brand Identity
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.img_launcher_zipora_1795421416651),
                                    contentDescription = "Zipora Logo",
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, SkyBlue, RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "ZIPORA",
                                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 1.6.sp),
                                        color = SoftWhite,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "SUPER APP",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.2.sp),
                                        color = SkyBlue
                                    )
                                }
                            }

                            // Notification Bell with animated badge
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SoftWhite.copy(alpha = 0.10f))
                                    .clickable { onNavigate(AppRoute.Notifications) }
                                    .testTag("notification_bell_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.Notifications,
                                    contentDescription = "Notifications",
                                    tint = SoftWhite
                                )
                                if (uiState.unreadNotificationCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(16.dp)
                                            .clip(CircleShape)
                                            .background(CoralDanger),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = uiState.unreadNotificationCount.coerceAtMost(9).toString(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                            color = SoftWhite
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.width(8.dp))

                            // Profile Avatar
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, PremiumYellow, CircleShape)
                                    .background(SkyBlue)
                                    .clickable { onNavigate(AppRoute.Profile) }
                                    .testTag("profile_avatar_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.appState.userName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepNavy,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // LOCATION SECTION
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SoftWhite.copy(alpha = 0.10f))
                                .border(1.dp, SkyBlue.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                                .clickable { showLocationSheet = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("location_selector_bar"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Rounded.LocationOn,
                                contentDescription = "Delivery Location Pin",
                                tint = PremiumYellow,
                                modifier = Modifier
                                    .size(24.dp)
                                    .offset(y = pinBounce.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Delivering to • ${uiState.appState.activeAddressLabel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SkyBlue
                                    )
                                    Icon(
                                        Icons.Rounded.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = SkyBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = uiState.appState.activeAddressLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftWhite,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Change",
                                style = MaterialTheme.typography.labelMedium,
                                color = PremiumYellow,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // LARGE ROUNDED SEARCH BAR
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SoftWhite,
                            shadowElevation = 6.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = SkyBlueDark,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    TextField(
                                        value = searchQuery,
                                        onValueChange = {
                                            searchQuery = it
                                            searchExpanded = true
                                        },
                                        placeholder = {
                                            Text(
                                                "Search rides, food, groceries, parcels...",
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
                                            focusedTextColor = DeepNavy,
                                            unfocusedTextColor = DeepNavy
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("home_search_input")
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(
                                            onClick = {
                                                onAddSearch(searchQuery)
                                                routeSearchQuery(searchQuery, onNavigate)
                                                searchExpanded = false
                                            }
                                        ) {
                                            Icon(Icons.Rounded.ArrowForward, contentDescription = "Go", tint = DeepNavy)
                                        }
                                    }
                                    IconButton(
                                        onClick = {
                                            searchExpanded = !searchExpanded
                                            onShowMessage("Simulated Voice Search: Say 'Book bike taxi' or 'Order pizza'")
                                        },
                                        modifier = Modifier.testTag("voice_search_button")
                                    ) {
                                        Icon(
                                            Icons.Rounded.Mic,
                                            contentDescription = "Voice search",
                                            tint = OrangeAccent
                                        )
                                    }
                                }

                                AnimatedVisibility(visible = searchExpanded) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        HorizontalDivider(color = LightBorder)
                                        Text("Recent Searches", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(uiState.recentSearches) { term ->
                                                AssistChip(
                                                    onClick = {
                                                        searchQuery = term
                                                        searchExpanded = false
                                                        routeSearchQuery(term, onNavigate)
                                                    },
                                                    label = { Text(term, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                        Text("Popular Right Now", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                        val popular = listOf("Bike Taxi", "Biryani", "9-Min Milk", "Send Parcel", "Tata Ace", "Earbuds")
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(popular) { pop ->
                                                SuggestionChip(
                                                    onClick = {
                                                        onAddSearch(pop)
                                                        searchExpanded = false
                                                        routeSearchQuery(pop, onNavigate)
                                                    },
                                                    label = { Text(pop, style = MaterialTheme.typography.labelSmall, color = DeepNavy) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. MAIN ANIMATED 3D HERO BANNER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .height(206.dp)
                        .shadow(12.dp, RoundedCornerShape(26.dp), ambientColor = DeepNavy, spotColor = SkyBlue)
                        .clip(RoundedCornerShape(26.dp))
                        .background(DeepNavy)
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_hero_superapp_1795421415685),
                        contentDescription = "Zipora 3D Super App Ecosystem Hero",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    // Left readability glass gradient
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to DeepNavy.copy(alpha = 0.94f),
                                    0.55f to DeepNavy.copy(alpha = 0.68f),
                                    1.0f to Color.Transparent
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.64f)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SkyBlue.copy(alpha = 0.25f))
                                .border(1.dp, SkyBlue.copy(alpha = 0.6f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "ALL-IN-ONE SUPER APP",
                                style = MaterialTheme.typography.labelSmall,
                                color = PremiumYellow,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Column {
                            Text(
                                text = "Everything You Need, One App.",
                                style = MaterialTheme.typography.headlineLarge,
                                color = SoftWhite,
                                lineHeight = 27.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Ride. Eat. Shop. Deliver. Move.",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFD6EEFF),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Button(
                            onClick = {
                                scope.launch { listState.animateScrollToItem(3) }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumYellow, contentColor = DeepNavy),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier
                                .scale(ctaGlow)
                                .testTag("hero_explore_button")
                        ) {
                            Text("Explore Services", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 3. WALLET + REWARDS + QUICK PANEL SWITCH RIBBON
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Wallet Pill Card
                    Row(
                        modifier = Modifier
                            .weight(1.15f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.linearGradient(listOf(DeepNavy, DeepNavyLight)))
                            .bounceClick { onNavigate(AppRoute.Wallet) }
                            .padding(14.dp)
                            .testTag("home_wallet_card"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconOrb3D(Icons.Rounded.AccountBalanceWallet, DeepNavy, PremiumYellow, OrangeAccent, size = 38.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Zipora Wallet", style = MaterialTheme.typography.labelSmall, color = SkyBlue)
                            AnimatedWalletAmount(
                                amount = uiState.appState.walletBalance,
                                color = SoftWhite,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    // Rewards + Mode Switcher Card
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .background(SoftWhite)
                            .border(1.dp, LightBorder, RoundedCornerShape(18.dp))
                            .bounceClick { onOpenDrawer() }
                            .padding(14.dp)
                            .testTag("home_mode_switcher_card"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconOrb3D(Icons.Rounded.SwapHoriz, SoftWhite, SkyBlue, SkyBlueDark, size = 38.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("9 Modes", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            Text(
                                "${uiState.appState.rewardPoints} Pts",
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepNavy,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // 4. MAIN SERVICE CATEGORIES (2-COLUMN 3D GRID — ALL 6 SERVICES)
            item {
                Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Core Services", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                            Text("Tap any ecosystem to launch live flow", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SkyBlue.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("6 Verticals", style = MaterialTheme.typography.labelSmall, color = SkyBlueDark)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    val services = listOf(
                        ServiceCardSpec(
                            title = "Ride Booking",
                            subtitle = "Bike Taxi • Auto • Cab",
                            cta = "Book a Ride",
                            icon = Icons.Rounded.TwoWheeler,
                            bgStart = RideCardStart,
                            bgEnd = RideCardEnd,
                            accent = SkyBlueDark,
                            badge = "2 MIN ETA",
                            imageRes = R.drawable.img_mobility_fleet_1795421417312,
                            route = AppRoute.RideBooking,
                            tag = "service_card_ride"
                        ),
                        ServiceCardSpec(
                            title = "Food Delivery",
                            subtitle = "Restaurants • Dishes",
                            cta = "Order Food",
                            icon = Icons.Rounded.LunchDining,
                            bgStart = FoodCardStart,
                            bgEnd = FoodCardEnd,
                            accent = OrangeAccent,
                            badge = "50% OFF",
                            imageRes = R.drawable.img_food_feast_1795421416008,
                            route = AppRoute.FoodDiscovery,
                            tag = "service_card_food"
                        ),
                        ServiceCardSpec(
                            title = "Instant Grocery",
                            subtitle = "Fruits • Veggies • Daily",
                            cta = "Shop Now",
                            icon = Icons.Rounded.ShoppingBasket,
                            bgStart = GroceryCardStart,
                            bgEnd = GroceryCardEnd,
                            accent = SuccessGreen,
                            badge = "9 MIN",
                            imageRes = R.drawable.img_grocery_basket_1795421415559,
                            route = AppRoute.GroceryStore,
                            tag = "service_card_grocery"
                        ),
                        ServiceCardSpec(
                            title = "Parcel Delivery",
                            subtitle = "Docs • Express Box",
                            cta = "Send Parcel",
                            icon = Icons.Rounded.Inventory2,
                            bgStart = ParcelCardStart,
                            bgEnd = ParcelCardEnd,
                            accent = Color(0xFFB45309),
                            badge = "OTP SAFE",
                            imageRes = null,
                            route = AppRoute.ParcelDelivery,
                            tag = "service_card_parcel"
                        ),
                        ServiceCardSpec(
                            title = "Goods Transport",
                            subtitle = "Tata Ace • Mini Truck",
                            cta = "Book Transport",
                            icon = Icons.Rounded.LocalShipping,
                            bgStart = TransportCardStart,
                            bgEnd = TransportCardEnd,
                            accent = VioletAccent,
                            badge = "UP TO 3.5T",
                            imageRes = null,
                            route = AppRoute.GoodsTransport,
                            tag = "service_card_transport"
                        ),
                        ServiceCardSpec(
                            title = "Local Shopping",
                            subtitle = "Tech • Fashion • Home",
                            cta = "Explore Shops",
                            icon = Icons.Rounded.ShoppingBag,
                            bgStart = ShoppingCardStart,
                            bgEnd = ShoppingCardEnd,
                            accent = PinkAccent,
                            badge = "30 MIN",
                            imageRes = null,
                            route = AppRoute.LocalShopping,
                            tag = "service_card_shopping"
                        )
                    )

                    services.chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            rowItems.forEach { spec ->
                                Service3DCard(
                                    spec = spec,
                                    modifier = Modifier.weight(1f),
                                    onClick = { onNavigate(spec.route) }
                                )
                            }
                        }
                    }
                }
            }

            // 5. LIVE ACTIVE BOOKING TRACKER
            if (activeOrders.isNotEmpty()) {
                item {
                    val live = activeOrders.first()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("Live Activity", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = SkyBlue)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SoftWhite)
                                .border(1.2.dp, SkyBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                .bounceClick { onNavigate(AppRoute.OrderHistory("ALL")) }
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconOrb3D(Icons.Rounded.DeliveryDining, SoftWhite, SkyBlue, DeepNavy, size = 48.dp)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(live.title, style = MaterialTheme.typography.titleSmall, color = DeepNavy, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                                    Spacer(Modifier.width(8.dp))
                                    StatusPill(live.status)
                                }
                                Text(live.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Partner: ${live.partnerName} • OTP: ${live.otp}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SkyBlueDark,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = DeepNavy)
                        }
                    }
                }
            }

            // 6. FLOATING PROMOTIONAL BANNERS
            item {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text(
                        "Exclusive Super Offers",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(DemoCatalog.coupons) { coupon ->
                            Box(
                                modifier = Modifier
                                    .width(248.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(
                                        Brush.linearGradient(
                                            when (coupon.category) {
                                                "RIDE" -> listOf(DeepNavy, SkyBlueDark)
                                                "GROCERY" -> listOf(Color(0xFF065F46), SuccessGreen)
                                                "TRANSPORT" -> listOf(Color(0xFF4C1D95), VioletAccent)
                                                else -> listOf(Color(0xFF9A3412), OrangeAccent)
                                            }
                                        )
                                    )
                                    .bounceClick { onNavigate(AppRoute.CouponsOffers) }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(PremiumYellow)
                                            .padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(coupon.code, style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(coupon.title, style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                                    Text(coupon.description, style = MaterialTheme.typography.bodySmall, color = SoftWhite.copy(alpha = 0.85f), maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // 7. FAVOURITE RESTAURANTS CAROUSEL
            item {
                Column(modifier = Modifier.padding(top = 18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top Rated Restaurants", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                        TextButton(onClick = { onNavigate(AppRoute.FoodDiscovery) }) {
                            Text("See All", color = SkyBlueDark, fontWeight = FontWeight.Bold)
                        }
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(DemoCatalog.restaurants.take(5)) { rest ->
                            val isFav = rest.id in uiState.favouriteRestaurantIds
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = SoftWhite,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .width(220.dp)
                                    .bounceClick { onNavigate(AppRoute.RestaurantDetail(rest.id)) }
                            ) {
                                Column {
                                    Box(modifier = Modifier.fillMaxWidth().height(112.dp)) {
                                        Image(
                                            painter = painterResource(R.drawable.img_food_feast_1795421416008),
                                            contentDescription = rest.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .background(
                                                    Brush.verticalGradient(listOf(Color.Transparent, DeepNavy.copy(alpha = 0.72f)))
                                                )
                                        )
                                        IconButton(
                                            onClick = { onToggleFavourite(rest.id) },
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Icon(
                                                if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                                contentDescription = "Favourite",
                                                tint = if (isFav) CoralDanger else SoftWhite
                                            )
                                        }
                                        Text(
                                            text = rest.offerText,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = PremiumYellow,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.align(Alignment.BottomStart).padding(10.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(rest.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(rest.cuisines, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Spacer(Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Rounded.Star, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(15.dp))
                                            Text(" ${rest.rating} • ${rest.deliveryMins} min • ${rest.distanceKm} km", style = MaterialTheme.typography.labelSmall, color = SlateText)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. POPULAR NEARBY GROCERY & LOCAL STORES
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                    PrototypeSimulationBanner("All 9 Panels Accessible via ☰ Menu • Changes persist locally in Room DB")
                }
            }
        }
    }

    if (showLocationSheet) {
        ModalBottomSheet(
            onDismissRequest = { showLocationSheet = false },
            containerColor = SoftWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Select Delivery Location", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                Text("Saved addresses across Bengaluru", style = MaterialTheme.typography.bodySmall, color = MutedText)
                DemoCatalog.savedAddresses.forEach { addr ->
                    val selected = addr.label == uiState.appState.activeAddressLabel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (selected) RideCardStart else LightSkyBackground)
                            .border(1.dp, if (selected) SkyBlue else LightBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                onSelectAddress(addr)
                                showLocationSheet = false
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (addr.label == "Home") Icons.Rounded.Home else Icons.Rounded.LocationOn,
                            contentDescription = null,
                            tint = SkyBlueDark
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(addr.label, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                            Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall, color = SlateText)
                        }
                        if (selected) {
                            Icon(Icons.Rounded.CheckCircle, contentDescription = "Selected", tint = SuccessGreen)
                        }
                    }
                }
            }
        }
    }
}

private data class ServiceCardSpec(
    val title: String,
    val subtitle: String,
    val cta: String,
    val icon: ImageVector,
    val bgStart: Color,
    val bgEnd: Color,
    val accent: Color,
    val badge: String,
    val imageRes: Int?,
    val route: AppRoute,
    val tag: String
)

@Composable
private fun Service3DCard(
    spec: ServiceCardSpec,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(178.dp)
            .shadow(8.dp, RoundedCornerShape(22.dp), ambientColor = spec.accent.copy(alpha = 0.25f), spotColor = spec.accent.copy(alpha = 0.35f))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.linearGradient(listOf(spec.bgStart, spec.bgEnd)))
            .border(1.2.dp, SoftWhite.copy(alpha = 0.85f), RoundedCornerShape(22.dp))
            .bounceClick(onClick)
            .testTag(spec.tag)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(13.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(SoftWhite.copy(alpha = 0.9f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = spec.badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = spec.accent,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                if (spec.imageRes != null) {
                    Image(
                        painter = painterResource(spec.imageRes),
                        contentDescription = spec.title,
                        modifier = Modifier
                            .size(56.dp)
                            .shadow(6.dp, RoundedCornerShape(16.dp))
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.5.dp, SoftWhite, RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    IconOrb3D(
                        icon = spec.icon,
                        tint = SoftWhite,
                        gradientStart = spec.accent,
                        gradientEnd = DeepNavy,
                        size = 52.dp,
                        iconSize = 27.dp
                    )
                }
            }

            Column {
                Text(
                    text = spec.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepNavy,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = spec.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = SlateText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DeepNavy)
                    .padding(horizontal = 10.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = spec.cta,
                    style = MaterialTheme.typography.labelSmall,
                    color = PremiumYellow,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    Icons.Rounded.ArrowForward,
                    contentDescription = null,
                    tint = SoftWhite,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun routeSearchQuery(query: String, onNavigate: (AppRoute) -> Unit) {
    val q = query.lowercase()
    when {
        "bike" in q || "ride" in q || "auto" in q || "cab" in q -> onNavigate(AppRoute.RideBooking)
        "milk" in q || "fruit" in q || "strawberr" in q || "grocery" in q || "blink" in q -> onNavigate(AppRoute.GroceryStore)
        "parcel" in q || "doc" in q || "send" in q -> onNavigate(AppRoute.ParcelDelivery)
        "truck" in q || "tata" in q || "transport" in q || "move" in q -> onNavigate(AppRoute.GoodsTransport)
        "earbud" in q || "shop" in q || "fashion" in q || "lamp" in q -> onNavigate(AppRoute.LocalShopping)
        else -> onNavigate(AppRoute.FoodDiscovery)
    }
}
