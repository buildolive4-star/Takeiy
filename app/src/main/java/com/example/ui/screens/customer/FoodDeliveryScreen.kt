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
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
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

private val FoodPrimaryOrange = Color(0xFFF97316)
private val FoodDeepOrange = Color(0xFFEA580C)
private val FoodCleanBg = Color(0xFFFAFAFC)
private val RatingGreen = Color(0xFF16A34A)
private val TrackLimeStart = Color(0xFFD9F99D)
private val TrackLimeMid = Color(0xFFF7FEE7)
private val TrackRoyalBlue = Color(0xFF2563EB)

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
            CravingCategory("Pizza", Icons.Rounded.LocalPizza, Color(0xFFFEF3C7), Color(0xFFFDE68A), Color(0xFFD97706)),
            CravingCategory("Burger", Icons.Rounded.LunchDining, Color(0xFFFFEDD5), Color(0xFFFED7AA), FoodDeepOrange),
            CravingCategory("Biryani", Icons.Rounded.RiceBowl, Color(0xFFFEF9C3), Color(0xFFFDE047), Color(0xFFA16207)),
            CravingCategory("Chinese", Icons.Rounded.RamenDining, Color(0xFFFFE4E6), Color(0xFFFECDD3), Color(0xFFE11D48)),
            CravingCategory("South Indian", Icons.Rounded.BreakfastDining, Color(0xFFDCFCE7), Color(0xFFBBF7D0), RatingGreen),
            CravingCategory("North Indian", Icons.Rounded.SoupKitchen, Color(0xFFF3E8FF), Color(0xFFE9D5FF), VioletAccent),
            CravingCategory("Desserts", Icons.Rounded.Cake, Color(0xFFFCE7F3), Color(0xFFFBCFE8), PinkAccent),
            CravingCategory("Healthy Food", Icons.Rounded.Eco, Color(0xFFD1FAE5), Color(0xFFA7F3D0), RatingGreen),
            CravingCategory("All", Icons.Rounded.Restaurant, Color(0xFFE0F2FE), Color(0xFFBAE6FD), SkyBlueDark)
        )
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var pureVegMode by remember { mutableStateOf(false) }
    var fastDeliveryOnly by remember { mutableStateOf(false) }
    var topRatedOnly by remember { mutableStateOf(false) }
    var sortByRating by remember { mutableStateOf(false) }

    val filtered = remember(selectedCategory, search, pureVegMode, fastDeliveryOnly, topRatedOnly, sortByRating) {
        DemoCatalog.restaurants.filter { r ->
            val matchCat = selectedCategory == "All" ||
                r.categoryTag.equals(selectedCategory, true) ||
                r.cuisines.contains(selectedCategory, true)
            val matchSearch = search.isBlank() || r.name.contains(search, true) || r.cuisines.contains(search, true)
            val matchVeg = !pureVegMode || r.isPureVeg
            val matchFast = !fastDeliveryOnly || r.deliveryMins <= 22
            val matchRated = !topRatedOnly || r.rating >= 4.7
            matchCat && matchSearch && matchVeg && matchFast && matchRated
        }.let { list ->
            if (sortByRating) list.sortedByDescending { it.rating } else list
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(FoodCleanBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 104.dp else 32.dp)
        ) {
            // 1. CRISP WHITE ORDER FOOD HEADER (Matches Screen 5 Mockup)
            item {
                Surface(color = SoftWhite, shadowElevation = 2.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top Bar: Back + "Order Food" + Cart Badge
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onBack,
                                modifier = Modifier.size(48.dp).testTag("back_button")
                            ) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                            }
                            Text(
                                text = "Order Food",
                                style = MaterialTheme.typography.titleLarge,
                                color = DeepNavy,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                                IconButton(onClick = onOpenCart, modifier = Modifier.testTag("food_header_cart_button")) {
                                    Icon(Icons.Rounded.ShoppingCart, "View Cart", tint = DeepNavy)
                                }
                                if (uiState.cartItemCount > 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(top = 6.dp, end = 6.dp)
                                            .size(18.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEF4444)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "${uiState.cartItemCount}",
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                            color = SoftWhite,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }
                        }

                        // Location Row + Compact Pure Veg Switch
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFEAB308), modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = uiState.appState.activeAddressLine.ifBlank { "Sector 62, Noida" },
                                    style = MaterialTheme.typography.titleSmall,
                                    color = DeepNavy,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(Icons.Rounded.KeyboardArrowDown, null, tint = DeepNavy, modifier = Modifier.size(18.dp))
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(if (pureVegMode) Color(0xFFDCFCE7) else Color(0xFFF1F5F9))
                                    .border(1.dp, if (pureVegMode) RatingGreen else Color(0xFFE2E8F0), RoundedCornerShape(50))
                                    .clickable { pureVegMode = !pureVegMode }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("food_veg_mode_toggle")
                            ) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(if (pureVegMode) RatingGreen else MutedText))
                                Spacer(Modifier.width(5.dp))
                                Text("VEG", style = MaterialTheme.typography.labelSmall, color = if (pureVegMode) RatingGreen else SlateText, fontWeight = FontWeight.ExtraBold)
                            }
                        }

                        // Clean Rounded Search Bar ("Search for restaurants...")
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, null, tint = MutedText, modifier = Modifier.size(20.dp))
                                TextField(
                                    value = search,
                                    onValueChange = { search = it },
                                    placeholder = {
                                        Text(
                                            "Search for restaurants...",
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
                                if (search.isNotBlank()) {
                                    IconButton(onClick = { search = "" }) {
                                        Icon(Icons.Rounded.Close, "Clear", tint = MutedText, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. CRAVING CATEGORY ICONS ROW (Pizza, Burger, Biryani, Chinese, South Indian...)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(cravings) { item ->
                        val selected = selectedCategory == item.name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(64.dp)
                                .clickable { selectedCategory = if (selected) "All" else item.name }
                                .testTag("craving_chip_${item.name.lowercase().replace(" ", "_")}")
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .shadow(if (selected) 6.dp else 2.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Brush.linearGradient(listOf(item.bgStart, item.bgEnd)))
                                    .border(if (selected) 2.2.dp else 1.dp, if (selected) FoodDeepOrange else SoftWhite, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, contentDescription = item.name, tint = item.tint, modifier = Modifier.size(28.dp))
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) FoodDeepOrange else DeepNavy,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 3. GOLDEN YELLOW PROMO BANNER ("Flat 60% OFF on Top Restaurants" + Pizza Art, Screen 5 Mockup)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(144.dp)
                        .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = Color(0xFFEAB308))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFACC15), Color(0xFFFDE047), Color(0xFFF59E0B))))
                        .clickable { onOpenRestaurant(DemoCatalog.restaurants.first().id) }
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_pizza_offer_1795427326877),
                        contentDescription = "Flat 60% OFF Pizza Offer",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to Color(0xF0FACC15),
                                    0.52f to Color(0xD8FDE047),
                                    0.80f to Color(0x22F59E0B),
                                    1.0f to Color.Transparent
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.64f)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Flat 60% OFF",
                                style = MaterialTheme.typography.headlineMedium,
                                color = DeepNavy,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "on Top Restaurants",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF334155),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(DeepNavy)
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Order Now →",
                                style = MaterialTheme.typography.labelMedium,
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // 4. QUICK FILTER CHIPS
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
                            label = "★ 4.7+ Rated",
                            selected = topRatedOnly,
                            onClick = { topRatedOnly = !topRatedOnly }
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

            // 5. TOP RESTAURANTS 🔥 SECTION HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Top Restaurants 🔥",
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "See All →",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepNavy,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable {
                            selectedCategory = "All"
                            pureVegMode = false
                            fastDeliveryOnly = false
                            topRatedOnly = false
                        }
                    )
                }
            }

            // 6. MOCKUP-STYLE COMPACT HORIZONTAL RESTAURANT CARDS
            items(filtered, key = { it.id }) { rest ->
                val isFav = rest.id in uiState.favouriteRestaurantIds
                TakeItRestaurantCard(
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
private fun TakeItRestaurantCard(
    rest: RestaurantInfo,
    isFav: Boolean,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit
) {
    val thumbRes = when (rest.categoryTag) {
        "Pizza" -> R.drawable.img_takeit_pizza_offer_1795427326877
        "Burger" -> R.drawable.img_takeit_hero_burger_1795427328311
        else -> R.drawable.img_food_feast_1795421416008
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SoftWhite,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .bounceClick(onOpen)
            .testTag("restaurant_card_${rest.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Square Rounded Food Thumbnail
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Image(
                    painter = painterResource(thumbRes),
                    contentDescription = rest.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xCC0B1F4B))))
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = rest.offerText.take(15),
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                        color = PremiumYellow,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Right Restaurant Metadata (Exact Screen 5 Card Details)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rest.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepNavy,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Favourite",
                        tint = if (isFav) Color(0xFFEF4444) else Color(0xFFEF4444).copy(alpha = 0.7f),
                        modifier = Modifier
                            .size(22.dp)
                            .clickable(onClick = onToggleFav)
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "${rest.rating} (${rest.reviewCount / 1000}.${(rest.reviewCount % 1000) / 100}K+)",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepNavy,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Schedule, null, tint = MutedText, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "${rest.deliveryMins} min  •  ${rest.distanceKm} km",
                        style = MaterialTheme.typography.bodySmall,
                        color = SlateText,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₹${rest.priceForTwo} for two",
                        style = MaterialTheme.typography.labelMedium,
                        color = DeepNavy,
                        fontWeight = FontWeight.Bold
                    )
                    if (rest.isPureVeg) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFDCFCE7))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text("Pure Veg", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = RatingGreen, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }
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
            .background(if (selected) Color(0xFFFFF7ED) else SoftWhite)
            .border(1.dp, if (selected) FoodDeepOrange else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (selected) FoodDeepOrange else DeepNavy, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(4.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) FoodDeepOrange else DeepNavy,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
        )
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

    Box(modifier = Modifier.fillMaxSize().background(FoodCleanBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 120.dp else 90.dp)
        ) {
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
                                tint = if (isFav) Color(0xFFEF4444) else DeepNavy
                            )
                        }
                        IconButton(onClick = { showMenuSheet = true }) {
                            Icon(Icons.Rounded.RestaurantMenu, "Browse Menu", tint = DeepNavy)
                        }
                    }
                }
            }

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
                            Text("⏱ ${restaurant.deliveryMins} mins", style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.SemiBold)
                        }

                        // Offers strip
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            items(
                                listOf(
                                    restaurant.offerText to "Use code TAKE50",
                                    "FREE DELIVERY" to "Take It Gold active",
                                    "₹75 CASHBACK" to "Pay via Take It Wallet"
                                )
                            ) { (title, sub) ->
                                Row(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFFFFBEB))
                                        .border(1.dp, Color(0xFFFDE68A), RoundedCornerShape(12.dp))
                                        .padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.LocalOffer, null, tint = FoodDeepOrange, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Column {
                                        Text(title, style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                        Text(sub, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = MutedText)
                                    }
                                }
                            }
                        }

                        OutlinedTextField(
                            value = menuSearch,
                            onValueChange = { menuSearch = it },
                            placeholder = { Text("Search within ${restaurant.name}...") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null, tint = FoodDeepOrange) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item { FoodFilterPill("Veg Only", vegOnly) { vegOnly = !vegOnly } }
                            item { FoodFilterPill("★ Bestsellers", bestsellersOnly) { bestsellersOnly = !bestsellersOnly } }
                            items(categories) { cat ->
                                FoodFilterPill(cat, selectedMenuCategory == cat) { selectedMenuCategory = cat }
                            }
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
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

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
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .border(1.5.dp, if (dish.isVeg) RatingGreen else FoodDeepOrange, RoundedCornerShape(4.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (dish.isVeg) RatingGreen else FoodDeepOrange))
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
                            Text("₹${dish.price}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Text(dish.description, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(
                            modifier = Modifier.width(110.dp).height(122.dp),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Image(
                                painter = painterResource(
                                    if (restaurant.categoryTag == "Pizza") R.drawable.img_takeit_pizza_offer_1795427326877
                                    else R.drawable.img_food_feast_1795421416008
                                ),
                                contentDescription = dish.name,
                                modifier = Modifier
                                    .size(104.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp)),
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
                                            .background(FoodDeepOrange)
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    ) {
                                        IconButton(onClick = { onUpdateCartQty(dish.id, -1) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Rounded.Remove, "Less", tint = SoftWhite, modifier = Modifier.size(15.dp))
                                        }
                                        Text("$inCartQty", style = MaterialTheme.typography.titleSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                        IconButton(onClick = { onUpdateCartQty(dish.id, 1) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Rounded.Add, "More", tint = SoftWhite, modifier = Modifier.size(15.dp))
                                        }
                                    }
                                } else {
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = Color(0xFFFFF7ED),
                                        shadowElevation = 4.dp,
                                        modifier = Modifier
                                            .border(1.3.dp, FoodDeepOrange, RoundedCornerShape(12.dp))
                                            .clickable { customizingDish = dish }
                                            .testTag("add_dish_${dish.id}")
                                    ) {
                                        Text(
                                            "ADD +",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = FoodDeepOrange,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

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
                    Text("Roasted Garlic Dip (+₹35)", style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
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
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FoodDeepOrange, contentColor = SoftWhite)
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
                .shadow(12.dp, RoundedCornerShape(18.dp), spotColor = FoodDeepOrange)
                .clip(RoundedCornerShape(18.dp))
                .background(Brush.horizontalGradient(listOf(FoodDeepOrange, FoodPrimaryOrange)))
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp)
                .testTag(tag),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("$itemCount ITEM${if (itemCount > 1) "S" else ""} ADDED", style = MaterialTheme.typography.labelSmall, color = SoftWhite.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                Text("₹$total plus taxes", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("View Cart", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = SoftWhite, modifier = Modifier.size(20.dp))
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
    var selectedPayment by remember { mutableStateOf("Take It Wallet") }
    var simulateFailure by remember { mutableStateOf(false) }
    var orderConfirmed by remember { mutableStateOf(false) }
    var lastOrderSummary by remember { mutableStateOf("Farmhouse Pizza") }
    var lastOrderTotal by remember { mutableIntStateOf(299) }
    var lastOrderItemCount by remember { mutableIntStateOf(1) }
    var selectedTip by remember { mutableIntStateOf(20) }
    var selectedInstruction by remember { mutableStateOf("Leave at door") }
    val finalPayable = uiState.cartGrandTotal + selectedTip
    val totalSavings = (uiState.cartMrpTotal - uiState.cartSubtotal).coerceAtLeast(0) + uiState.cartDiscount

    if (orderConfirmed) {
        // SCREEN 8: TRACK YOUR ORDER (Matches uploaded mockup with soft lime-yellow header, Rakesh Kumar card, map, 4-step green bar, Your Order card)
        TakeItOrderTrackingView(
            orderTitle = lastOrderSummary,
            orderTotal = lastOrderTotal,
            itemCount = lastOrderItemCount,
            destinationLabel = uiState.appState.activeAddressLine.ifBlank { "Sector 62, Noida" },
            onBack = onBack,
            onViewAllOrders = onOrderPlacedSuccess
        )
        return
    }

    Scaffold(
        containerColor = FoodCleanBg,
        topBar = {
            ZiporaSubTopBar(
                title = "Cart & Checkout",
                subtitle = "${uiState.cartItemCount} items • ${uiState.appState.activeAddressLabel}",
                onBack = onBack
            )
        }
    ) { innerPadding ->
        if (uiState.cart.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconOrb3D(Icons.Rounded.ShoppingCart, SoftWhite, FoodPrimaryOrange, DeepNavy, size = 72.dp, iconSize = 36.dp)
                Spacer(Modifier.height(14.dp))
                Text("Your Cart is Empty", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                Text("Add dishes, groceries, or nearby store items to checkout.", style = MaterialTheme.typography.bodyMedium, color = MutedText, textAlign = TextAlign.Center)
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
                            Icon(Icons.Rounded.LocationOn, null, tint = FoodDeepOrange)
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
                                Text(entry.sourceName, style = MaterialTheme.typography.labelSmall, color = FoodDeepOrange)
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
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFF7ED)).border(1.dp, FoodDeepOrange, RoundedCornerShape(10.dp))
                            ) {
                                IconButton(onClick = { onUpdateQty(entry.itemId, -1) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Remove, "Remove", tint = FoodDeepOrange, modifier = Modifier.size(16.dp))
                                }
                                Text("${entry.quantity}", style = MaterialTheme.typography.titleSmall, color = FoodDeepOrange, fontWeight = FontWeight.ExtraBold)
                                IconButton(onClick = { onUpdateQty(entry.itemId, 1) }, modifier = Modifier.size(32.dp)) {
                                    Icon(Icons.Rounded.Add, "Add", tint = FoodDeepOrange, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

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

                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Payment Method (Simulated)", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            listOf("Take It Wallet", "UPI • GPay", "Debit / Credit Card", "Net Banking", "Cash on Delivery").forEach { method ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedPayment == method) Color(0xFFFFF7ED) else LightSkyBackground)
                                        .clickable { selectedPayment = method }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedPayment == method, onClick = { selectedPayment = method })
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        if (method == "Take It Wallet") "$method (Bal ₹${uiState.appState.walletBalance.toInt()})" else method,
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
                            val snapshotTitle = uiState.cart.firstOrNull()?.name ?: "Farmhouse Pizza"
                            val snapshotTotal = finalPayable
                            val snapshotCount = uiState.cartItemCount
                            if (onPlaceOrder(selectedPayment, simulateFailure)) {
                                lastOrderSummary = snapshotTitle
                                lastOrderTotal = snapshotTotal
                                lastOrderItemCount = snapshotCount
                                orderConfirmed = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .navigationBarsPadding()
                            .testTag("checkout_pay_button"),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = FoodDeepOrange, contentColor = SoftWhite)
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
private fun TakeItOrderTrackingView(
    orderTitle: String,
    orderTotal: Int,
    itemCount: Int,
    destinationLabel: String,
    onBack: () -> Unit,
    onViewAllOrders: () -> Unit
) {
    val compactSteps = remember { listOf("Order\nPlaced", "Preparing\nOrder", "On the Way", "Delivered") }
    val detailedSteps = remember {
        listOf(
            "Order Placed & Confirmed",
            "Restaurant Accepted Your Order",
            "Chef Preparing Fresh Food",
            "Food Packed & Ready",
            "Delivery Partner Rakesh Kumar Assigned",
            "Order Picked Up from Kitchen",
            "On the Way to $destinationLabel",
            "Delivered at Your Doorstep"
        )
    }
    var stepStage by remember { mutableIntStateOf(2) } // 0..3 matching mockup's 3rd green check active
    var showFullTimeline by remember { mutableStateOf(false) }
    var actionBanner by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        delay(3500)
        if (stepStage < 3) stepStage = 2
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF8FAFC)),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1. SOFT LIME-YELLOW-TO-WHITE GRADIENT TOP HEADER + FLOATING PARTNER CARD (Screen 8 Exact Look)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(TrackLimeStart, TrackLimeMid, Color(0xFFF8FAFC))
                        )
                    )
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("back_button")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                        }
                        Text(
                            text = "Track Your Order",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepNavy,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.size(48.dp))
                    }

                    // Floating White Partner Card (Rakesh Kumar • Arriving in 5 min • Call/Chat)
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = SoftWhite,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Character3DPortrait(
                                drawableRes = R.drawable.img_char_food_rider_1795426023084,
                                contentDescription = "Delivery Partner Rakesh Kumar",
                                borderColor = TrackRoyalBlue,
                                size = 56.dp,
                                bobbing = false
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Rakesh Kumar",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = DeepNavy,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    text = if (stepStage >= 3) "Order Delivered ✓" else "Arriving in ${((3 - stepStage) * 5).coerceAtLeast(3)} min",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = RatingGreen,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { actionBanner = "Calling Rakesh Kumar (+91 98*** **312)..." },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Icon(Icons.Rounded.Call, "Call Partner", tint = DeepNavy, modifier = Modifier.size(20.dp))
                            }
                            Spacer(Modifier.width(8.dp))
                            IconButton(
                                onClick = { actionBanner = "Sent to Rakesh: 'Please ring the bell once!'" },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Icon(Icons.Rounded.Chat, "Chat Partner", tint = DeepNavy, modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    if (actionBanner != null) {
                        Text(
                            text = actionBanner ?: "",
                            style = MaterialTheme.typography.labelMedium,
                            color = TrackRoyalBlue,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }

        // 2. LIGHT STREET MAP WITH BLUE ROUTE LINE
        item {
            SimulatedMapCanvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(225.dp)
                    .clip(RoundedCornerShape(22.dp)),
                pickupLabel = "Kitchen",
                dropLabel = destinationLabel.substringBefore(","),
                distanceKm = 2.1,
                etaMinutes = ((3 - stepStage) * 5).coerceAtLeast(2),
                driverProgress = when (stepStage) {
                    0 -> 0.15f
                    1 -> 0.42f
                    2 -> 0.72f
                    else -> 0.96f
                }
            )
        }

        // 3. 4-STEP HORIZONTAL GREEN CHECK PROGRESS BAR (Screen 8 Exact Look)
        item {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = SoftWhite,
                shadowElevation = 3.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        compactSteps.forEachIndexed { idx, label ->
                            val reached = idx <= stepStage
                            val lineReached = idx < stepStage
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(3.dp)
                                            .background(
                                                if (idx == 0) Color.Transparent
                                                else if (idx <= stepStage) RatingGreen
                                                else Color(0xFFE2E8F0)
                                            )
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(26.dp)
                                            .clip(CircleShape)
                                            .background(if (reached) RatingGreen else Color(0xFFE2E8F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Rounded.Check,
                                            contentDescription = label,
                                            tint = if (reached) SoftWhite else MutedText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(3.dp)
                                            .background(
                                                if (idx == compactSteps.lastIndex) Color.Transparent
                                                else if (lineReached) RatingGreen
                                                else Color(0xFFE2E8F0)
                                            )
                                    )
                                }
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (reached) DeepNavy else MutedText,
                                    fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showFullTimeline = !showFullTimeline }) {
                            Text(
                                if (showFullTimeline) "Hide Detailed Timeline ▲" else "8-Step Live Details ▼",
                                style = MaterialTheme.typography.labelMedium,
                                color = TrackRoyalBlue,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        TextButton(onClick = { stepStage = (stepStage + 1) % 4 }) {
                            Text("Simulate Next →", style = MaterialTheme.typography.labelMedium, color = RatingGreen, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    if (showFullTimeline) {
                        MultiStepStatusTimeline(
                            steps = detailedSteps,
                            currentStepIndex = (stepStage * 2 + 1).coerceAtMost(detailedSteps.lastIndex),
                            accentColor = RatingGreen
                        )
                    }
                }
            }
        }

        // 4. BOTTOM "YOUR ORDER" CARD (Matches Screen 8 Mockup)
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Your Order",
                    style = MaterialTheme.typography.titleMedium,
                    color = DeepNavy,
                    fontWeight = FontWeight.ExtraBold
                )
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = SoftWhite,
                    shadowElevation = 4.dp,
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_takeit_pizza_offer_1795427326877),
                            contentDescription = orderTitle,
                            modifier = Modifier
                                .size(62.dp)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = orderTitle,
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepNavy,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "₹$orderTotal • $itemCount Item${if (itemCount > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "View Details >",
                            style = MaterialTheme.typography.labelLarge,
                            color = TrackRoyalBlue,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier
                                .clickable(onClick = onViewAllOrders)
                                .padding(horizontal = 6.dp, vertical = 8.dp)
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
