package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.DemoCatalog
import com.example.data.MenuItemEntity
import com.example.model.CartEntry
import com.example.model.CouponItem
import com.example.model.RestaurantInfo
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState
import kotlinx.coroutines.delay

private val FoodCrimson = Color(0xFFE23744)
private val FoodCrimsonDark = Color(0xFFB91C2A)
private val FoodSurfaceBg = Color(0xFFF8FAFC)
private val RatingGreen = Color(0xFF1C7C3D)

private data class CravingCategory(
    val name: String,
    val icon: ImageVector,
    val bgStart: Color,
    val bgEnd: Color,
    val tint: Color
)

@Composable
fun FoodDiscoveryScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onOpenRestaurant: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenCart: () -> Unit
) {
    val cravings = remember {
        listOf(
            CravingCategory("All", Icons.Rounded.Restaurant, Color(0xFFFFE4E6), Color(0xFFFECDD3), FoodCrimson),
            CravingCategory("Biryani", Icons.Rounded.RiceBowl, Color(0xFFFFEDD5), Color(0xFFFED7AA), Color(0xFFC2410C)),
            CravingCategory("Pizza", Icons.Rounded.LocalPizza, Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFB45309)),
            CravingCategory("Burger", Icons.Rounded.LunchDining, Color(0xFFDCFCE7), Color(0xFFBBF7D0), RatingGreen),
            CravingCategory("South Indian", Icons.Rounded.BreakfastDining, Color(0xFFE0F2FE), Color(0xFFBAE6FD), SkyBlueDark),
            CravingCategory("North Indian", Icons.Rounded.SoupKitchen, Color(0xFFF3E8FF), Color(0xFFE9D5FF), VioletAccent),
            CravingCategory("Chinese", Icons.Rounded.RamenDining, Color(0xFFFFE4E6), Color(0xFFFECACA), FoodCrimson),
            CravingCategory("Desserts", Icons.Rounded.Cake, Color(0xFFFCE7F3), Color(0xFFFBCFE8), PinkAccent),
            CravingCategory("Beverages", Icons.Rounded.LocalCafe, Color(0xFFCCFBF1), Color(0xFF99F6E4), TealAccent)
        )
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var pureVegMode by remember { mutableStateOf(false) }
    var fastDeliveryOnly by remember { mutableStateOf(false) }
    var topRatedOnly by remember { mutableStateOf(false) }
    var under500Only by remember { mutableStateOf(false) }
    var sortByRating by remember { mutableStateOf(false) }

    // Rotating Zomato-style search placeholder hint
    val hints = remember { listOf("Search 'Hyderabadi Biryani'", "Search 'Wood-Fired Pizza'", "Search 'Benne Masala Dosa'", "Search 'Truffle Burger'") }
    var hintIdx by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(2600)
            hintIdx = (hintIdx + 1) % hints.size
        }
    }

    val filtered = remember(selectedCategory, search, pureVegMode, fastDeliveryOnly, topRatedOnly, under500Only, sortByRating) {
        DemoCatalog.restaurants.filter { r ->
            val matchCat = selectedCategory == "All" ||
                r.categoryTag.equals(selectedCategory, true) ||
                r.cuisines.contains(selectedCategory, true) ||
                (selectedCategory == "Beverages" && (r.cuisines.contains("Coffee", true) || r.cuisines.contains("Shakes", true) || r.cuisines.contains("Brew", true)))
            val matchSearch = search.isBlank() || r.name.contains(search, true) || r.cuisines.contains(search, true)
            val matchVeg = !pureVegMode || r.isPureVeg
            val matchFast = !fastDeliveryOnly || r.deliveryMins <= 22
            val matchRated = !topRatedOnly || r.rating >= 4.7
            val matchPrice = !under500Only || r.priceForTwo <= 500
            matchCat && matchSearch && matchVeg && matchFast && matchRated && matchPrice
        }.let { list ->
            if (sortByRating) list.sortedByDescending { it.rating } else list
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(FoodSurfaceBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 110.dp else 40.dp)
        ) {
            // 1. ZOMATO-INSPIRED LOCATION + INSTANT VEG MODE TOP HEADER
            item {
                Surface(color = SoftWhite, shadowElevation = 3.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(44.dp).testTag("back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                            }
                            Icon(Icons.Rounded.LocationOn, null, tint = FoodCrimson, modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(4.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        uiState.appState.activeAddressLabel,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = DeepNavy,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = DeepNavy, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    uiState.appState.activeAddressLine,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedText,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            // Iconic Compact VEG MODE Toggle
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (pureVegMode) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (pureVegMode) RatingGreen else LightBorder, RoundedCornerShape(12.dp))
                                    .clickable { pureVegMode = !pureVegMode }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                    .testTag("food_veg_mode_toggle")
                            ) {
                                Text(
                                    "VEG MODE",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.5.sp),
                                    color = if (pureVegMode) RatingGreen else SlateText,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Box(
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .width(30.dp)
                                        .height(14.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(if (pureVegMode) RatingGreen else Color(0xFFCBD5E1))
                                        .padding(2.dp),
                                    contentAlignment = if (pureVegMode) Alignment.CenterEnd else Alignment.CenterStart
                                ) {
                                    Box(Modifier.size(10.dp).clip(CircleShape).background(SoftWhite))
                                }
                            }
                        }

                        // Rounded Search Bar with Rotating Craving Hint
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, null, tint = FoodCrimson, modifier = Modifier.size(22.dp))
                                TextField(
                                    value = search,
                                    onValueChange = { search = it },
                                    placeholder = {
                                        Text(
                                            hints[hintIdx],
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MutedText
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
                                    modifier = Modifier.weight(1f).testTag("food_search_input")
                                )
                                VerticalDivider(modifier = Modifier.height(22.dp), color = Color(0xFFE2E8F0))
                                IconButton(onClick = { search = "Biryani" }) {
                                    Icon(Icons.Rounded.Mic, "Voice search", tint = FoodCrimson)
                                }
                            }
                        }
                    }
                }
            }

            // 2. ZIPORA FOOD PASS / GOLD HERO BANNER
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .height(142.dp)
                        .shadow(10.dp, RoundedCornerShape(24.dp), spotColor = FoodCrimson)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFF1F0A10), FoodCrimsonDark, FoodCrimson)))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_food_feast_1795421416008),
                        contentDescription = "Gourmet Feast Offer",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alpha = 0.38f
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(listOf(Color(0xEE18080D), Color(0xAA991B1B), Color.Transparent))
                        )
                    )
                    Column(
                        modifier = Modifier.fillMaxHeight().padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Brush.horizontalGradient(listOf(PremiumYellow, OrangeAccent)))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("ZIPORA GOLD", style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            }
                            Text("FREE DELIVERY ON ALL ORDERS", style = MaterialTheme.typography.labelSmall, color = SoftWhite.copy(alpha = 0.9f))
                        }
                        Column {
                            Text("Flat 50% OFF + Zero Delivery Fee", style = MaterialTheme.typography.headlineMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                            Text("Use code ZIP50 • Handpicked top-rated kitchens near you", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFFE4E6))
                        }
                    }
                }
            }

            // 3. "WHAT'S ON YOUR MIND?" CIRCULAR CRAVING CAROUSEL
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                        Text(
                            "  WHAT'S ON YOUR MIND?  ",
                            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.6.sp),
                            color = MutedText,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE2E8F0))
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(cravings) { item ->
                            val selected = selectedCategory == item.name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(68.dp)
                                    .clickable { selectedCategory = item.name }
                                    .testTag("craving_chip_${item.name.lowercase().replace(" ", "_")}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(62.dp)
                                        .shadow(if (selected) 8.dp else 3.dp, CircleShape, spotColor = item.tint)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(item.bgStart, item.bgEnd)))
                                        .border(if (selected) 2.5.dp else 1.dp, if (selected) FoodCrimson else SoftWhite, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(item.icon, contentDescription = item.name, tint = item.tint, modifier = Modifier.size(30.dp))
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (selected) FoodCrimson else DeepNavy,
                                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center
                                )
                                if (selected) {
                                    Spacer(Modifier.height(3.dp))
                                    Box(Modifier.width(24.dp).height(3.dp).clip(RoundedCornerShape(50)).background(FoodCrimson))
                                }
                            }
                        }
                    }
                }
            }

            // 4. INTERACTIVE FILTER CHIPS ROW
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FoodFilterPill(
                            label = if (sortByRating) "Top Rated ✓" else "Sort: Rating",
                            selected = sortByRating,
                            icon = Icons.Rounded.SwapVert,
                            onClick = { sortByRating = !sortByRating }
                        )
                    }
                    item {
                        FoodFilterPill(
                            label = "⚡ Under 22 mins",
                            selected = fastDeliveryOnly,
                            onClick = { fastDeliveryOnly = !fastDeliveryOnly }
                        )
                    }
                    item {
                        FoodFilterPill(
                            label = "Rating 4.7+",
                            selected = topRatedOnly,
                            onClick = { topRatedOnly = !topRatedOnly }
                        )
                    }
                    item {
                        FoodFilterPill(
                            label = "Under ₹500",
                            selected = under500Only,
                            onClick = { under500Only = !under500Only }
                        )
                    }
                    item {
                        FoodFilterPill(
                            label = "Pure Veg",
                            selected = pureVegMode,
                            onClick = { pureVegMode = !pureVegMode }
                        )
                    }
                }
            }

            // 5. SECTION HEADER: RECOMMENDED RESTAURANTS
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${filtered.size} RESTAURANTS DELIVERING TO YOU", style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.1.sp), color = MutedText, fontWeight = FontWeight.Bold)
                        Text("Featured Kitchens & Bestsellers", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                    }
                }
            }

            // 6. EDITORIAL ZOMATO-GRADE RESTAURANT CARDS
            items(filtered, key = { it.id }) { rest ->
                val isFav = rest.id in uiState.favouriteRestaurantIds
                EditorialRestaurantCard(
                    rest = rest,
                    isFav = isFav,
                    onOpen = { onOpenRestaurant(rest.id) },
                    onToggleFav = { onToggleFavourite(rest.id) }
                )
            }
        }

        // Sticky Floating View Cart Bar
        AnimatedVisibility(
            visible = uiState.cartItemCount > 0,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            StickyFoodCartBar(
                itemCount = uiState.cartItemCount,
                total = uiState.cartGrandTotal,
                onClick = onOpenCart,
                tag = "food_view_cart_fab"
            )
        }
    }
}

@Composable
private fun FoodFilterPill(
    label: String,
    selected: Boolean,
    icon: ImageVector? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFFFF1F2) else SoftWhite)
            .border(1.dp, if (selected) FoodCrimson else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) FoodCrimson else DeepNavy, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) FoodCrimson else DeepNavy,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}

@Composable
private fun EditorialRestaurantCard(
    rest: RestaurantInfo,
    isFav: Boolean,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = SoftWhite,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .bounceClick(onOpen)
            .testTag("restaurant_card_${rest.id}")
    ) {
        Column {
            // Tall Appetizing Hero Food Image
            Box(modifier = Modifier.fillMaxWidth().height(188.dp)) {
                Image(
                    painter = painterResource(R.drawable.img_food_feast_1795421416008),
                    contentDescription = rest.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier.fillMaxSize().background(
                        Brush.verticalGradient(
                            0.0f to Color(0x44000000),
                            0.45f to Color.Transparent,
                            1.0f to Color(0xD8091522)
                        )
                    )
                )

                // Top Row: Pure Veg / Promo Tag + Bookmark Heart
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (rest.isPureVeg) {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RatingGreen).padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("🌿 PURE VEG", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                            }
                        } else {
                            Box(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(DeepNavy.copy(alpha = 0.85f)).padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text("⚡ TOP PICK", style = MaterialTheme.typography.labelSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }

                    IconButton(
                        onClick = onToggleFav,
                        modifier = Modifier.size(38.dp).clip(CircleShape).background(SoftWhite.copy(alpha = 0.92f))
                    ) {
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Save restaurant",
                            tint = if (isFav) FoodCrimson else DeepNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Bottom Overlay: Offer Ribbon + Floating ETA Pill
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocalOffer, null, tint = PremiumYellow, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(5.dp))
                            Text(
                                rest.offerText,
                                style = MaterialTheme.typography.titleMedium,
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Text("Above ₹199 • Extra Gold savings", style = MaterialTheme.typography.labelSmall, color = Color(0xFFE2E8F0))
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(SoftWhite)
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Text(
                            "⏱ ${rest.deliveryMins} mins",
                            style = MaterialTheme.typography.labelSmall,
                            color = DeepNavy,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            // Bottom Card Details
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        rest.name,
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    // Iconic Green Rating Badge
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(RatingGreen)
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${rest.rating}", style = MaterialTheme.typography.labelMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(2.dp))
                        Icon(Icons.Rounded.Star, null, tint = SoftWhite, modifier = Modifier.size(13.dp))
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        rest.cuisines,
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "₹${rest.priceForTwo} for two",
                        style = MaterialTheme.typography.labelMedium,
                        color = SlateText,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Verified, null, tint = SkyBlueDark, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Free Delivery with Zipora Gold", style = MaterialTheme.typography.labelSmall, color = SkyBlueDark, fontWeight = FontWeight.Bold)
                    }
                    Text("${rest.distanceKm} km • ${rest.reviewCount}+ orders", style = MaterialTheme.typography.labelSmall, color = MutedText)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantDetailScreen(
    restaurantId: String,
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onAddToCart: (CartEntry) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenCart: () -> Unit
) {
    val restaurant = remember(restaurantId) {
        DemoCatalog.restaurants.find { it.id == restaurantId } ?: DemoCatalog.restaurants.first()
    }
    val allDishes = remember(uiState.menuItems, restaurant.id) {
        uiState.menuItems.filter { it.restaurantId == restaurant.id }
    }

    var menuSearch by remember { mutableStateOf("") }
    var vegOnly by remember { mutableStateOf(false) }
    var bestsellersOnly by remember { mutableStateOf(false) }
    var selectedMenuCategory by remember { mutableStateOf("All") }
    var showMenuSheet by remember { mutableStateOf(false) }
    var customizingDish by remember { mutableStateOf<MenuItemEntity?>(null) }

    val categories = remember(allDishes) { listOf("All") + allDishes.map { it.category }.distinct() }
    val dishes = remember(allDishes, menuSearch, vegOnly, bestsellersOnly, selectedMenuCategory) {
        allDishes.filter { d ->
            (selectedMenuCategory == "All" || d.category == selectedMenuCategory) &&
                (!vegOnly || d.isVeg) &&
                (!bestsellersOnly || d.isBestseller) &&
                (menuSearch.isBlank() || d.name.contains(menuSearch, true) || d.description.contains(menuSearch, true))
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(FoodSurfaceBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 120.dp else 90.dp)
        ) {
            // Top Bar
            item {
                val isFav = restaurant.id in uiState.favouriteRestaurantIds
                Surface(color = SoftWhite, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.testTag("back_button")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { onToggleFavourite(restaurant.id) }) {
                            Icon(
                                if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                "Favourite",
                                tint = if (isFav) FoodCrimson else DeepNavy
                            )
                        }
                        IconButton(onClick = { showMenuSheet = true }) {
                            Icon(Icons.Rounded.RestaurantMenu, "Browse Menu", tint = DeepNavy)
                        }
                    }
                }
            }

            // Zomato-Inspired Restaurant Hero Header Card
            item {
                Surface(
                    shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp),
                    color = SoftWhite,
                    shadowElevation = 4.dp
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            restaurant.name,
                            style = MaterialTheme.typography.headlineLarge,
                            color = DeepNavy,
                            fontWeight = FontWeight.ExtraBold,
                            textAlign = TextAlign.Center
                        )
                        Text(restaurant.cuisines, style = MaterialTheme.typography.bodySmall, color = MutedText, textAlign = TextAlign.Center)

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Row(
                                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(RatingGreen).padding(horizontal = 9.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${restaurant.rating}", style = MaterialTheme.typography.labelLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                Icon(Icons.Rounded.Star, null, tint = SoftWhite, modifier = Modifier.size(14.dp))
                            }
                            Text("${restaurant.reviewCount}+ ratings", style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.SemiBold)
                            Text("•", color = MutedText)
                            Text("⏱ ${restaurant.deliveryMins} mins • ${restaurant.distanceKm} km", style = MaterialTheme.typography.labelMedium, color = SlateText)
                        }

                        // Horizontal Offer Strip
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            items(
                                listOf(
                                    restaurant.offerText to "Use code ZIP50",
                                    "FREE DELIVERY" to "Zipora Gold active",
                                    "₹75 CASHBACK" to "Pay via Zipora Wallet"
                                )
                            ) { (offer, sub) ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(Color(0xFFFFF7ED))
                                        .border(1.dp, OrangeAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.LocalOffer, null, tint = FoodCrimson, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(offer, style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                        Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp), color = MutedText)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // In-Menu Search & Veg / Bestseller Filters
            item {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = menuSearch,
                        onValueChange = { menuSearch = it },
                        placeholder = { Text("Search dishes inside ${restaurant.name}...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = FoodCrimson) },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = SoftWhite,
                            unfocusedContainerColor = SoftWhite,
                            focusedBorderColor = FoodCrimson,
                            unfocusedBorderColor = Color(0xFFE2E8F0)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        item {
                            FoodFilterPill("🌿 Veg Only", vegOnly, onClick = { vegOnly = !vegOnly })
                        }
                        item {
                            FoodFilterPill("★ Bestseller", bestsellersOnly, onClick = { bestsellersOnly = !bestsellersOnly })
                        }
                        items(categories) { cat ->
                            FoodFilterPill(cat, selectedMenuCategory == cat, onClick = { selectedMenuCategory = cat })
                        }
                    }
                }
            }

            item {
                Text(
                    "Recommended (${dishes.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = DeepNavy,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Zomato-Inspired Dish Cards with Overlapping ADD+ Thumbnail
            items(dishes, key = { it.id }) { dish ->
                val inCartQty = uiState.cart.filter { it.itemId == dish.id }.sumOf { it.quantity }
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SoftWhite,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 7.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        // Left Info Column
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .border(1.5.dp, if (dish.isVeg) RatingGreen else FoodCrimson, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (dish.isVeg) RatingGreen else FoodCrimson))
                                }
                                if (dish.isBestseller) {
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Color(0xFFFEF3C7)).padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("★ Bestseller", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309), fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                            Text(dish.name, style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.clip(RoundedCornerShape(5.dp)).background(Color(0xFFFEF9C3)).padding(horizontal = 5.dp, vertical = 1.dp)) {
                                    Text("★ ${dish.rating}", style = MaterialTheme.typography.labelSmall, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(6.dp))
                                Text("(180+ orders)", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                            Text("₹${dish.price}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Text(dish.description, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(Modifier.width(12.dp))

                        // Right Dish Photo + Overlapping ADD Button
                        Box(
                            modifier = Modifier.width(114.dp).height(126.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                painter = painterResource(R.drawable.img_food_feast_1795421416008),
                                contentDescription = dish.name,
                                modifier = Modifier
                                    .size(108.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp)),
                                contentScale = ContentScale.Crop
                            )

                            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                                if (!dish.isAvailable) {
                                    Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF1F5F9), shadowElevation = 2.dp) {
                                        Text("Sold Out", style = MaterialTheme.typography.labelSmall, color = MutedText, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                                    }
                                } else if (inCartQty > 0) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .shadow(5.dp, RoundedCornerShape(12.dp))
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(FoodCrimson)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(onClick = { onUpdateCartQty(dish.id, -1) }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Rounded.Remove, "Less", tint = SoftWhite, modifier = Modifier.size(16.dp))
                                        }
                                        Text("$inCartQty", style = MaterialTheme.typography.titleSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                        IconButton(onClick = { onUpdateCartQty(dish.id, 1) }, modifier = Modifier.size(30.dp)) {
                                            Icon(Icons.Rounded.Add, "More", tint = SoftWhite, modifier = Modifier.size(16.dp))
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFF1F2),
                                        shadowElevation = 5.dp,
                                        modifier = Modifier
                                            .border(1.3.dp, FoodCrimson, RoundedCornerShape(12.dp))
                                            .clickable { customizingDish = dish }
                                            .testTag("add_dish_${dish.id}")
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 7.dp)
                                        ) {
                                            Text("ADD +", style = MaterialTheme.typography.labelLarge, color = FoodCrimson, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Floating MENU Quick-Jump Button
        FloatingActionButton(
            onClick = { showMenuSheet = true },
            containerColor = DeepNavy,
            contentColor = PremiumYellow,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(end = 16.dp, bottom = if (uiState.cartItemCount > 0) 84.dp else 18.dp)
        ) {
            Row(modifier = Modifier.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.MenuBook, "Menu", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("MENU", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
            }
        }

        // Sticky Bottom Cart Bar
        AnimatedVisibility(
            visible = uiState.cartItemCount > 0,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            StickyFoodCartBar(
                itemCount = uiState.cartItemCount,
                total = uiState.cartGrandTotal,
                onClick = onOpenCart,
                tag = "detail_view_cart_fab"
            )
        }
    }

    if (showMenuSheet) {
        ModalBottomSheet(onDismissRequest = { showMenuSheet = false }, containerColor = DeepNavy) {
            Column(modifier = Modifier.fillMaxWidth().padding(20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Browse Menu Categories", style = MaterialTheme.typography.titleLarge, color = PremiumYellow)
                categories.forEach { cat ->
                    val count = if (cat == "All") allDishes.size else allDishes.count { it.category == cat }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedMenuCategory == cat) SoftWhite.copy(alpha = 0.14f) else Color.Transparent)
                            .clickable {
                                selectedMenuCategory = cat
                                showMenuSheet = false
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(cat, style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                        Text("$count", style = MaterialTheme.typography.labelMedium, color = SkyBlue)
                    }
                }
            }
        }
    }

    // Rich Customization Sheet
    customizingDish?.let { dish ->
        var portion by remember { mutableStateOf("Regular") }
        var spice by remember { mutableStateOf("Medium") }
        var extraCheese by remember { mutableStateOf(false) }
        var garlicDip by remember { mutableStateOf(false) }
        val extraCost = (if (portion == "Large") 80 else 0) + (if (extraCheese) 45 else 0) + (if (garlicDip) 35 else 0)

        ModalBottomSheet(onDismissRequest = { customizingDish = null }, containerColor = SoftWhite) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Customize • ${dish.name}", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                Text("Choice of Portion", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodFilterPill("Regular (₹${dish.price})", portion == "Regular", onClick = { portion = "Regular" })
                    FoodFilterPill("Large (+₹80)", portion == "Large", onClick = { portion = "Large" })
                }
                Text("Spice Level", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Mild", "Medium", "Extra Spicy").forEach { s ->
                        FoodFilterPill(s, spice == s, onClick = { spice = s })
                    }
                }
                Text("Popular Add-ons", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
                Row(
                    Modifier.fillMaxWidth().clickable { extraCheese = !extraCheese },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Extra Melted Cheese (+₹45)", style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
                    Checkbox(checked = extraCheese, onCheckedChange = { extraCheese = it })
                }
                Row(
                    Modifier.fillMaxWidth().clickable { garlicDip = !garlicDip },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Roasted Garlic Aioli Dip (+₹35)", style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
                    Checkbox(checked = garlicDip, onCheckedChange = { garlicDip = it })
                }
                Button(
                    onClick = {
                        val notes = buildList {
                            add(portion)
                            add(spice)
                            if (extraCheese) add("Extra Cheese")
                            if (garlicDip) add("Garlic Dip")
                        }.joinToString(" • ")
                        onAddToCart(
                            CartEntry(
                                itemId = dish.id,
                                name = dish.name,
                                sourceName = restaurant.name,
                                categoryType = "FOOD",
                                unitPrice = dish.price + extraCost,
                                mrp = dish.price + extraCost + 35,
                                quantity = 1,
                                customization = notes
                            )
                        )
                        customizingDish = null
                    },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FoodCrimson, contentColor = SoftWhite)
                ) {
                    Text("Add Item • ₹${dish.price + extraCost}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun StickyFoodCartBar(
    itemCount: Int,
    total: Int,
    onClick: () -> Unit,
    tag: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = FoodCrimson)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(FoodCrimsonDark, FoodCrimson)))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag(tag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("$itemCount ITEM${if (itemCount > 1) "S" else ""} ADDED", style = MaterialTheme.typography.labelSmall, color = SoftWhite.copy(alpha = 0.88f), fontWeight = FontWeight.Bold)
                Text("₹$total plus taxes", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View Cart", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.Rounded.ArrowForward, null, tint = SoftWhite, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun CartCheckoutScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onApplyCoupon: (CouponItem?) -> Unit,
    onPlaceOrder: (paymentMethod: String, simulateFailure: Boolean) -> Boolean,
    onOrderPlacedSuccess: () -> Unit
) {
    var selectedPayment by remember { mutableStateOf("Zipora Wallet") }
    var simulateFailure by remember { mutableStateOf(false) }
    var orderConfirmed by remember { mutableStateOf(false) }
    var selectedTip by remember { mutableIntStateOf(20) }
    var selectedInstruction by remember { mutableStateOf("Leave at door") }
    val finalPayable = uiState.cartGrandTotal + selectedTip
    val totalSavings = (uiState.cartMrpTotal - uiState.cartSubtotal).coerceAtLeast(0) + uiState.cartDiscount

    Scaffold(
        containerColor = FoodSurfaceBg,
        topBar = {
            ZiporaSubTopBar(
                title = "Cart & Checkout",
                subtitle = "${uiState.cartItemCount} items • ${uiState.appState.activeAddressLabel}",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (orderConfirmed) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconOrb3D(Icons.Rounded.CheckCircle, SoftWhite, RatingGreen, Color(0xFF14532D), size = 76.dp, iconSize = 42.dp)
                Spacer(Modifier.height(14.dp))
                Text("Order Placed! Chef is Preparing", style = MaterialTheme.typography.headlineMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Live synced! Switch to Restaurant Owner or Food Delivery Partner via ☰ menu to manage this order live.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateText,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(14.dp))
                LiveOrderMilestoneBar(status = "PREPARING")
                Spacer(Modifier.height(16.dp))
                SimulatedMapCanvas(
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    pickupLabel = "Kitchen Hub",
                    dropLabel = uiState.appState.activeAddressLabel,
                    distanceKm = 2.1,
                    etaMinutes = 18,
                    driverProgress = -1f
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onOrderPlacedSuccess,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FoodCrimson, contentColor = SoftWhite),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Track Live in My Orders", fontWeight = FontWeight.ExtraBold)
                }
            }
        } else if (uiState.cart.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconOrb3D(Icons.Rounded.ShoppingCart, SoftWhite, FoodCrimson, DeepNavy, size = 72.dp, iconSize = 36.dp)
                Spacer(Modifier.height(14.dp))
                Text("Your Cart is Empty", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                Text("Add dishes, groceries, or nearby store items to checkout.", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)) {
                    Text("Browse Food & Stores")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 36.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (totalSavings > 0) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color(0xFFDCFCE7))
                                .border(1.dp, RatingGreen.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Celebration, null, tint = RatingGreen)
                            Spacer(Modifier.width(8.dp))
                            Text("You're saving ₹$totalSavings on this order!", style = MaterialTheme.typography.titleSmall, color = RatingGreen, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }

                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = FoodCrimson)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Delivery in 20 mins • ${uiState.appState.activeAddressLabel}", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                Text(uiState.appState.activeAddressLine, style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                        }
                    }
                }

                items(uiState.cart, key = { it.itemId + it.customization }) { entry ->
                    Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                Text(entry.sourceName, style = MaterialTheme.typography.labelSmall, color = FoodCrimson)
                                if (entry.customization.isNotBlank()) {
                                    Text(entry.customization, style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("₹${entry.unitPrice * entry.quantity}", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                                    if (entry.mrp > entry.unitPrice) {
                                        Text("₹${entry.mrp * entry.quantity}", style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough), color = MutedText)
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF1F2)).border(1.dp, FoodCrimson, RoundedCornerShape(10.dp))
                            ) {
                                IconButton(onClick = { onUpdateQty(entry.itemId, -1) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Remove, "Remove", tint = FoodCrimson, modifier = Modifier.size(16.dp))
                                }
                                Text("${entry.quantity}", style = MaterialTheme.typography.titleSmall, color = FoodCrimson, fontWeight = FontWeight.ExtraBold)
                                IconButton(onClick = { onUpdateQty(entry.itemId, 1) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Add, "Add", tint = FoodCrimson, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                // Delivery Instructions & Partner Tip
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Delivery Instructions", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(listOf("Leave at door", "Avoid calling", "Don't send cutlery", "Extra spicy")) { ins ->
                                    FoodFilterPill(ins, selectedInstruction == ins, onClick = { selectedInstruction = ins })
                                }
                            }
                            HorizontalDivider(color = Color(0xFFF1F5F9))
                            Text("Tip Your Delivery Partner (100% goes to partner)", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(0 to "No Tip", 20 to "₹20", 30 to "₹30 ★", 50 to "₹50").forEach { (amt, lbl) ->
                                    FoodFilterPill(lbl, selectedTip == amt, onClick = { selectedTip = amt })
                                }
                            }
                        }
                    }
                }

                // Coupons
                item {
                    Text("Apply Coupon", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(DemoCatalog.coupons) { coupon ->
                            val applied = uiState.appliedCoupon?.code == coupon.code
                            FoodFilterPill("${coupon.code} (-₹${coupon.discountAmount})", applied) {
                                onApplyCoupon(if (applied) null else coupon)
                            }
                        }
                    }
                }

                // Bill Summary
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bill Summary", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            BillRow("Item Subtotal", "₹${uiState.cartSubtotal}")
                            if (uiState.cartDiscount > 0) {
                                BillRow("Coupon Discount (${uiState.appliedCoupon?.code})", "-₹${uiState.cartDiscount}", RatingGreen)
                            }
                            BillRow("Delivery Fee", "₹${uiState.cartDeliveryFee}")
                            BillRow("Platform Fee", "₹${uiState.cartPlatformFee}")
                            if (selectedTip > 0) BillRow("Delivery Partner Tip", "₹$selectedTip")
                            BillRow("GST & Taxes (5%)", "₹${uiState.cartTaxes}")
                            HorizontalDivider(color = LightBorder)
                            BillRow("Grand Total", "₹$finalPayable", DeepNavy, bold = true)
                        }
                    }
                }

                // Payment Methods & Prototype Simulator
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Payment Method (Simulated)", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            listOf("Zipora Wallet", "UPI • GPay", "Debit / Credit Card", "Net Banking", "Cash on Delivery").forEach { method ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedPayment == method) Color(0xFFFFF1F2) else LightSkyBackground)
                                        .clickable { selectedPayment = method }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedPayment == method, onClick = { selectedPayment = method })
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (method == "Zipora Wallet") "$method (Bal ₹${uiState.appState.walletBalance.toInt()})" else method,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepNavy
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Simulate Payment Failure (Test Mode)", style = MaterialTheme.typography.labelMedium, color = CoralDanger)
                                Switch(checked = simulateFailure, onCheckedChange = { simulateFailure = it })
                            }
                        }
                    }
                }

                item {
                    Button(
                        onClick = {
                            if (onPlaceOrder(selectedPayment, simulateFailure)) {
                                orderConfirmed = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .navigationBarsPadding()
                            .testTag("checkout_pay_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FoodCrimson, contentColor = SoftWhite)
                    ) {
                        Text(
                            "Place Order • Pay ₹$finalPayable",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, valueColor: Color = SlateText, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = if (bold) DeepNavy else SlateText)
        Text(
            value,
            style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium,
            color = valueColor,
            fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold
        )
    }
}
