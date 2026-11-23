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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightSkyBackground)
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 112.dp)
        ) {
            // 1. ATMOSPHERIC MESH-GLOW TOP BAR + LOCATION + GLASS SEARCH
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp))
                ) {
                    AtmosphericMeshHeaderBackground(modifier = Modifier.matchParentSize())

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Row: Hamburger | Brand | Bell & Avatar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onOpenDrawer,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(SoftWhite.copy(alpha = 0.13f))
                                    .border(1.dp, PremiumYellow.copy(alpha = 0.45f), RoundedCornerShape(15.dp))
                                    .testTag("hamburger_menu_button")
                            ) {
                                Icon(
                                    Icons.Rounded.Menu,
                                    contentDescription = "Open Universal Panel Switcher",
                                    tint = PremiumYellow,
                                    modifier = Modifier.size(26.dp)
                                )
                            }

                            Spacer(Modifier.width(10.dp))

                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.img_launcher_zipora_1795421416651),
                                    contentDescription = "Zipora Logo",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .shadow(6.dp, RoundedCornerShape(11.dp), spotColor = SkyBlue)
                                        .clip(RoundedCornerShape(11.dp))
                                        .border(1.2.dp, SkyBlue, RoundedCornerShape(11.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.width(9.dp))
                                Column {
                                    Text(
                                        text = "ZIPORA",
                                        style = MaterialTheme.typography.titleLarge.copy(letterSpacing = 2.sp),
                                        color = SoftWhite,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        text = "HYPERLOCAL SUPER APP",
                                        style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp, fontSize = 9.sp),
                                        color = SkyBlue
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(15.dp))
                                    .background(SoftWhite.copy(alpha = 0.11f))
                                    .border(1.dp, SoftWhite.copy(alpha = 0.16f), RoundedCornerShape(15.dp))
                                    .clickable { onNavigate(AppRoute.Notifications) }
                                    .testTag("notification_bell_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Notifications, contentDescription = "Notifications", tint = SoftWhite)
                                if (uiState.unreadNotificationCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(7.dp)
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

                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .border(2.dp, PremiumYellow, CircleShape)
                                    .background(Brush.linearGradient(listOf(SkyBlue, SkyBlueDark)))
                                    .clickable { onNavigate(AppRoute.Profile) }
                                    .testTag("profile_avatar_button"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = uiState.appState.userName.take(1).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SoftWhite,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }

                        // LOCATION GLASS PILL
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(SoftWhite.copy(alpha = 0.10f))
                                .border(1.dp, SkyBlue.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
                                .clickable { showLocationSheet = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                .testTag("location_selector_bar"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(PremiumYellow.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Rounded.LocationOn,
                                    contentDescription = "Delivery Location Pin",
                                    tint = PremiumYellow,
                                    modifier = Modifier.size(20.dp).offset(y = pinBounce.dp)
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Delivering to • ${uiState.appState.activeAddressLabel}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = SkyBlue,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = SkyBlue, modifier = Modifier.size(16.dp))
                                }
                                Text(
                                    text = uiState.appState.activeAddressLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SoftWhite.copy(alpha = 0.92f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PremiumYellow.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("Change", style = MaterialTheme.typography.labelSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // FLOATING ROUNDED SEARCH BAR
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = SoftWhite,
                            shadowElevation = 10.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.Search, contentDescription = "Search", tint = SkyBlueDark, modifier = Modifier.size(22.dp))
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
                                        modifier = Modifier.weight(1f).testTag("home_search_input")
                                    )
                                    if (searchQuery.isNotEmpty()) {
                                        IconButton(onClick = {
                                            onAddSearch(searchQuery)
                                            routeSearchQuery(searchQuery, onNavigate)
                                            searchExpanded = false
                                        }) {
                                            Icon(Icons.Rounded.ArrowForward, contentDescription = "Go", tint = DeepNavy)
                                        }
                                    }
                                    Box(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(OrangeAccent.copy(alpha = 0.14f))
                                            .clickable {
                                                searchExpanded = !searchExpanded
                                                onShowMessage("Simulated Voice Search: Say 'Book bike taxi' or 'Order pizza'")
                                            }
                                            .testTag("voice_search_button"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Mic, contentDescription = "Voice search", tint = OrangeAccent, modifier = Modifier.size(20.dp))
                                    }
                                }

                                AnimatedVisibility(visible = searchExpanded) {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
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
                                        Text("Popular Now", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            items(listOf("Bike Taxi", "Biryani", "9-Min Milk", "Send Parcel", "Tata Ace", "Earbuds")) { pop ->
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

            // 2. QUICK-LAUNCH SPEED PILLS STRIP
            item {
                val quickPills = listOf(
                    Triple("⚡ 2m Bike", SkyBlueDark, AppRoute.RideBooking),
                    Triple("🍕 50% Food", OrangeAccent, AppRoute.FoodDiscovery),
                    Triple("🥬 9m Blink", SuccessGreen, AppRoute.GroceryStore),
                    Triple("📦 Parcel", Color(0xFFB45309), AppRoute.ParcelDelivery),
                    Triple("🚚 Cargo", VioletAccent, AppRoute.GoodsTransport),
                    Triple("🛍️ Shops", PinkAccent, AppRoute.LocalShopping)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(quickPills) { (text, accent, route) ->
                        Box(
                            modifier = Modifier
                                .shadow(3.dp, RoundedCornerShape(50), spotColor = accent.copy(alpha = 0.25f))
                                .clip(RoundedCornerShape(50))
                                .background(SoftWhite)
                                .border(1.dp, accent.copy(alpha = 0.28f), RoundedCornerShape(50))
                                .bounceClick { onNavigate(route) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(text, style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 3. ULTRA-PREMIUM 3D HERO BANNER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(216.dp)
                        .shadow(16.dp, RoundedCornerShape(28.dp), ambientColor = DeepNavy, spotColor = SkyBlue)
                        .clip(RoundedCornerShape(28.dp))
                        .background(DeepNavy)
                        .border(1.4.dp, SkyBlue.copy(alpha = 0.45f), RoundedCornerShape(28.dp))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_hero_superapp_1795421415685),
                        contentDescription = "Zipora 3D Super App Ecosystem Hero",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to Color(0xFA071B2D),
                                    0.54f to Color(0xCC102A43),
                                    1.0f to Color.Transparent
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.65f)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(SkyBlue.copy(alpha = 0.24f))
                                .border(1.dp, SkyBlue.copy(alpha = 0.65f), RoundedCornerShape(50))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "★ ONE UNIFIED ECOSYSTEM",
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
                            onClick = { scope.launch { listState.animateScrollToItem(4) } },
                            colors = ButtonDefaults.buttonColors(containerColor = PremiumYellow, contentColor = DeepNavy),
                            shape = RoundedCornerShape(15.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 9.dp),
                            modifier = Modifier.scale(ctaGlow).testTag("hero_explore_button")
                        ) {
                            Text("Explore Services", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Rounded.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            // 4. FINTECH WALLET & 9-MODE SUPER SWITCHER CARDS
            item {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        // Holographic Navy Wallet Card
                        Box(
                            modifier = Modifier
                                .weight(1.22f)
                                .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = SkyBlue)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Brush.linearGradient(listOf(Color(0xFF0A2138), DeepNavy, Color(0xFF0C4A6E))))
                                .glassSheenOverlay(0.14f)
                                .border(1.dp, SkyBlue.copy(alpha = 0.4f), RoundedCornerShape(22.dp))
                                .bounceClick { onNavigate(AppRoute.Wallet) }
                                .padding(14.dp)
                                .testTag("home_wallet_card")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconOrb3D(Icons.Rounded.AccountBalanceWallet, DeepNavy, PremiumYellow, OrangeAccent, size = 42.dp, iconSize = 21.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("ZIPORA WALLET", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SkyBlue)
                                    AnimatedWalletAmount(uiState.appState.walletBalance, color = SoftWhite, style = MaterialTheme.typography.titleLarge)
                                    Text("+ Add Money", style = MaterialTheme.typography.labelSmall, color = PremiumYellow)
                                }
                            }
                        }

                        // Rewards & Super Points Card
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .shadow(6.dp, RoundedCornerShape(22.dp), spotColor = PremiumYellow.copy(alpha = 0.25f))
                                .clip(RoundedCornerShape(22.dp))
                                .background(SoftWhite)
                                .border(1.2.dp, LightBorder, RoundedCornerShape(22.dp))
                                .bounceClick { onNavigate(AppRoute.ReferEarn) }
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconOrb3D(Icons.Rounded.WorkspacePremium, DeepNavy, PremiumYellow, Color(0xFFFDE047), size = 42.dp, iconSize = 22.dp)
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("REWARDS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = MutedText)
                                    Text("${uiState.appState.rewardPoints}", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    Text("Gold Tier", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                                }
                            }
                        }
                    }

                    // Universal 9-Panel Switcher Callout Banner
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(5.dp, RoundedCornerShape(20.dp), spotColor = SkyBlue.copy(alpha = 0.25f))
                            .clip(RoundedCornerShape(20.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFE0F2FE), SoftWhite, Color(0xFFFEF9C3))))
                            .border(1.2.dp, SkyBlue.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .bounceClick { onOpenDrawer() }
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                            .testTag("home_mode_switcher_card"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconOrb3D(Icons.Rounded.DashboardCustomize, SoftWhite, SkyBlueDark, DeepNavy, size = 40.dp, iconSize = 20.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("9 Business & Partner Panels Inside", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Text("Switch to Captain, Chef, Grocery, Logistics or Admin", style = MaterialTheme.typography.bodySmall, color = SlateText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(DeepNavy)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("Switch", style = MaterialTheme.typography.labelSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 5. MAIN 3D SERVICE CATEGORIES GRID (ALL 6 SERVICES)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Explore Services", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                            Text("Tap any vertical to launch full interactive experience", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DeepNavy)
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("6 Verticals", style = MaterialTheme.typography.labelSmall, color = PremiumYellow)
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

            // 6. LIVE ACTIVE ORDER WITH 4-STEP MILESTONE PROGRESS BAR
            if (activeOrders.isNotEmpty()) {
                item {
                    val live = activeOrders.first()
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
                        Text("Live Activity Tracker", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = SoftWhite,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.2.dp, SkyBlue.copy(alpha = 0.55f), RoundedCornerShape(22.dp))
                                .bounceClick { onNavigate(AppRoute.OrderHistory("ALL")) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconOrb3D(Icons.Rounded.DeliveryDining, SoftWhite, SkyBlue, DeepNavy, size = 46.dp)
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(live.title, style = MaterialTheme.typography.titleSmall, color = DeepNavy, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                                            Spacer(Modifier.width(8.dp))
                                            StatusPill(live.status)
                                        }
                                        Text(live.subtitle, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("Partner: ${live.partnerName} • OTP: ${live.otp}", style = MaterialTheme.typography.labelSmall, color = SkyBlueDark, fontWeight = FontWeight.Bold)
                                    }
                                    Icon(Icons.Rounded.ChevronRight, contentDescription = null, tint = DeepNavy)
                                }
                                LiveOrderMilestoneBar(status = live.status)
                            }
                        }
                    }
                }
            }

            // 7. FLOATING PROMOTIONAL BANNERS
            item {
                Column(modifier = Modifier.padding(top = 14.dp)) {
                    Text("Exclusive Super Offers", style = MaterialTheme.typography.titleLarge, color = DeepNavy, modifier = Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.height(10.dp))
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(DemoCatalog.coupons) { coupon ->
                            Box(
                                modifier = Modifier
                                    .width(256.dp)
                                    .shadow(8.dp, RoundedCornerShape(22.dp))
                                    .clip(RoundedCornerShape(22.dp))
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
                                    .glassSheenOverlay(0.14f)
                                    .bounceClick { onNavigate(AppRoute.CouponsOffers) }
                                    .padding(16.dp)
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(PremiumYellow).padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(coupon.code, style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(coupon.title, style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                                    Text(coupon.description, style = MaterialTheme.typography.bodySmall, color = SoftWhite.copy(alpha = 0.88f), maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // 8. TOP RATED RESTAURANTS CAROUSEL
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
                                shape = RoundedCornerShape(22.dp),
                                color = SoftWhite,
                                shadowElevation = 6.dp,
                                modifier = Modifier
                                    .width(228.dp)
                                    .bounceClick { onNavigate(AppRoute.RestaurantDetail(rest.id)) }
                            ) {
                                Column {
                                    Box(modifier = Modifier.fillMaxWidth().height(118.dp)) {
                                        Image(
                                            painter = painterResource(R.drawable.img_food_feast_1795421416008),
                                            contentDescription = rest.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, DeepNavy.copy(alpha = 0.78f))))
                                        )
                                        IconButton(onClick = { onToggleFavourite(rest.id) }, modifier = Modifier.align(Alignment.TopEnd)) {
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

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp)) {
                    PrototypeSimulationBanner("All 9 Panels Accessible via ☰ Menu • Local Room DB persistence active")
                }
            }
        }

        // Floating Cart Pill above Bottom Dock when cart has items
        if (uiState.cartItemCount > 0) {
            ExtendedFloatingActionButton(
                onClick = { onNavigate(AppRoute.CartCheckout) },
                containerColor = SuccessGreen,
                contentColor = SoftWhite,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 84.dp)
                    .testTag("home_floating_cart")
            ) {
                Icon(Icons.Rounded.ShoppingCart, contentDescription = "View Cart")
                Spacer(Modifier.width(8.dp))
                Text("${uiState.cartItemCount} • ₹${uiState.cartGrandTotal}", fontWeight = FontWeight.ExtraBold)
            }
        }

        // Sleek Floating Glassmorphic Bottom Navigation Dock
        ZiporaFloatingBottomDock(
            cartCount = uiState.cartItemCount,
            onNavigate = onNavigate,
            onScrollTop = { scope.launch { listState.animateScrollToItem(0) } },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    if (showLocationSheet) {
        ModalBottomSheet(onDismissRequest = { showLocationSheet = false }, containerColor = SoftWhite) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
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
                        Icon(if (addr.label == "Home") Icons.Rounded.Home else Icons.Rounded.LocationOn, null, tint = SkyBlueDark)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(addr.label, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                            Text(addr.fullAddress, style = MaterialTheme.typography.bodySmall, color = SlateText)
                        }
                        if (selected) Icon(Icons.Rounded.CheckCircle, "Selected", tint = SuccessGreen)
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
            .height(184.dp)
            .shadow(10.dp, RoundedCornerShape(24.dp), ambientColor = spec.accent.copy(alpha = 0.25f), spotColor = spec.accent.copy(alpha = 0.38f))
            .clip(RoundedCornerShape(24.dp))
            .background(Brush.linearGradient(listOf(spec.bgStart, spec.bgEnd)))
            .glassSheenOverlay(0.28f)
            .border(1.4.dp, SoftWhite.copy(alpha = 0.9f), RoundedCornerShape(24.dp))
            .bounceClick(onClick)
            .testTag(spec.tag)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(13.dp),
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
                        .background(SoftWhite.copy(alpha = 0.92f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(spec.badge, style = MaterialTheme.typography.labelSmall, color = spec.accent, fontWeight = FontWeight.ExtraBold)
                }

                if (spec.imageRes != null) {
                    Image(
                        painter = painterResource(spec.imageRes),
                        contentDescription = spec.title,
                        modifier = Modifier
                            .size(60.dp)
                            .shadow(8.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .border(1.8.dp, SoftWhite, RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    IconOrb3D(
                        icon = spec.icon,
                        tint = SoftWhite,
                        gradientStart = spec.accent,
                        gradientEnd = DeepNavy,
                        size = 54.dp,
                        iconSize = 28.dp
                    )
                }
            }

            Column {
                Text(spec.title, style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                Text(spec.subtitle, style = MaterialTheme.typography.bodySmall, color = SlateText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Brush.horizontalGradient(listOf(DeepNavy, DeepNavyLight)))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(spec.cta, style = MaterialTheme.typography.labelSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                Icon(Icons.Rounded.ArrowForward, contentDescription = null, tint = SoftWhite, modifier = Modifier.size(14.dp))
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
