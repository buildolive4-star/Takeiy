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

private val FoodCrimson = Color(0xFFEF4444)
private val FoodCrimsonDeep = Color(0xFFDC2626)
private val FoodBannerOrange = Color(0xFFF97316)
private val FoodGoldYellow = Color(0xFFFACC15)
private val FoodLateNightBg = Color(0xFF1E1B4B)
private val FoodOfferGreen = Color(0xFF16A34A)
private val FoodPageBg = Color(0xFFF8FAFC)

private data class FoodGridCat(
    val name: String,
    val icon: ImageVector,
    val bg: Color,
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
    val gridCategories = remember {
        listOf(
            FoodGridCat("Pizza", Icons.Rounded.LocalPizza, Color(0xFFFEF3C7), Color(0xFFD97706)),
            FoodGridCat("Burger", Icons.Rounded.LunchDining, Color(0xFFFFEDD5), Color(0xFFEA580C)),
            FoodGridCat("Biryani", Icons.Rounded.RiceBowl, Color(0xFFFEF9C3), Color(0xFFA16207)),
            FoodGridCat("Chinese", Icons.Rounded.RamenDining, Color(0xFFFFE4E6), Color(0xFFE11D48)),
            FoodGridCat("South Indian", Icons.Rounded.BreakfastDining, Color(0xFFDCFCE7), Color(0xFF16A34A)),
            FoodGridCat("North Indian", Icons.Rounded.SoupKitchen, Color(0xFFF3E8FF), Color(0xFF7C3AED)),
            FoodGridCat("Desserts", Icons.Rounded.Cake, Color(0xFFFCE7F3), Color(0xFFDB2777)),
            FoodGridCat("Snacks", Icons.Rounded.Fastfood, Color(0xFFE0F2FE), Color(0xFF0284C7)),
            FoodGridCat("Healthy", Icons.Rounded.Eco, Color(0xFFD1FAE5), Color(0xFF059669)),
            FoodGridCat("All", Icons.Rounded.MoreHoriz, Color(0xFFFEE2E2), FoodCrimson)
        )
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var pureVegMode by remember { mutableStateOf(false) }
    var offersOnly by remember { mutableStateOf(false) }
    var highRatedOnly by remember { mutableStateOf(false) }
    var sortByRating by remember { mutableStateOf(false) }

    val filtered = remember(selectedCategory, search, pureVegMode, offersOnly, highRatedOnly, sortByRating) {
        DemoCatalog.restaurants.filter { r ->
            val matchCat = selectedCategory == "All" ||
                r.categoryTag.contains(selectedCategory, true) ||
                r.cuisines.contains(selectedCategory, true)
            val matchSearch = search.isBlank() || r.name.contains(search, true) || r.cuisines.contains(search, true)
            val matchVeg = !pureVegMode || r.isPureVeg
            val matchRated = !highRatedOnly || r.rating >= 4.7
            val matchOffer = !offersOnly || r.offerText.contains("OFF", true) || r.offerText.contains("FREE", true)
            matchCat && matchSearch && matchVeg && matchRated && matchOffer
        }.let { list ->
            if (sortByRating) list.sortedByDescending { it.rating } else list
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(FoodPageBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 104.dp else 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. TOP LOCATION HEADER (Image 1 Screen 2)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SoftWhite)
                        .statusBarsPadding()
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("back_button")) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                    }
                    Icon(Icons.Rounded.LocationOn, null, tint = DeepNavy, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = uiState.appState.activeAddressLabel.ifBlank { "Greater Noida" },
                        style = MaterialTheme.typography.titleMedium,
                        color = DeepNavy,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = onOpenCart, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Rounded.ShoppingCart, "Cart", tint = DeepNavy)
                    }
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(FoodCrimson),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(uiState.appState.userName.take(1).uppercase(), color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // 2. RED-ORANGE "FLAT 50% OFF ON YOUR FAVOURITE FOOD" HERO BANNER (Image 1 Screen 2)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(142.dp)
                        .shadow(10.dp, RoundedCornerShape(22.dp), spotColor = FoodCrimson)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(FoodGoldYellow, FoodBannerOrange, FoodCrimson)))
                        .clickable { onOpenRestaurant(DemoCatalog.restaurants.first().id) }
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_pizza_offer_1795427326877),
                        contentDescription = "Flat 50% OFF Pizza",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to Color(0xF0F97316),
                                    0.52f to Color(0xD8EF4444),
                                    0.82f to Color.Transparent
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
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("Flat 50% OFF", style = MaterialTheme.typography.headlineMedium, color = SoftWhite, fontWeight = FontWeight.Black)
                            Text("on Your Favourite Food", style = MaterialTheme.typography.titleSmall, color = Color(0xFFFFF7ED), fontWeight = FontWeight.SemiBold)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(FoodCrimsonDeep)
                                .border(1.dp, SoftWhite.copy(alpha = 0.4f), RoundedCornerShape(50))
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text("Order Now →", style = MaterialTheme.typography.labelMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // 3. SEARCH BAR ("Search for restaurants, cuisines...")
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SoftWhite,
                    shadowElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Search, null, tint = DeepNavy, modifier = Modifier.size(20.dp))
                        TextField(
                            value = search,
                            onValueChange = { search = it },
                            placeholder = { Text("Search for restaurants, cuisines...", style = MaterialTheme.typography.bodyMedium, color = MutedText) },
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
                        IconButton(onClick = { search = "Pizza" }) {
                            Icon(Icons.Rounded.Mic, "Voice search", tint = DeepNavy, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }

            // 4. 5×2 CIRCULAR FOOD CATEGORY GRID (Matches Image 1 Screen 2!)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    gridCategories.chunked(5).forEach { rowCats ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            rowCats.forEach { cat ->
                                val sel = selectedCategory == cat.name
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectedCategory = if (sel) "All" else cat.name }
                                        .testTag("craving_chip_${cat.name.lowercase().replace(" ", "_")}")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .shadow(if (sel) 6.dp else 2.dp, CircleShape)
                                            .clip(CircleShape)
                                            .background(cat.bg)
                                            .border(if (sel) 2.2.dp else 1.dp, if (sel) FoodCrimson else SoftWhite, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(cat.icon, contentDescription = cat.name, tint = cat.tint, modifier = Modifier.size(26.dp))
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = cat.name,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = if (sel) FoodCrimson else DeepNavy,
                                        fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. LATE NIGHT CRAVINGS DARK PURPLE BANNER (Matches Image 1 Screen 2!)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(106.dp)
                        .shadow(8.dp, RoundedCornerShape(20.dp), spotColor = FoodLateNightBg)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(FoodLateNightBg, Color(0xFF312E81), Color(0xFF4C1D95))))
                        .clickable { onOpenRestaurant(DemoCatalog.restaurants[1].id) }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "Late Night Cravings?\nWe're Here!",
                                style = MaterialTheme.typography.titleMedium,
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 20.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(FoodBannerOrange)
                                    .padding(horizontal = 12.dp, vertical = 5.dp)
                            ) {
                                Text("Order Now →", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Image(
                            painter = painterResource(R.drawable.img_food_feast_1795421416008),
                            contentDescription = "Late Night Cravings",
                            modifier = Modifier
                                .size(84.dp)
                                .clip(CircleShape)
                                .border(2.dp, FoodBannerOrange, CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // 6. TOP RESTAURANTS 🔥 HORIZONTAL CARDS WITH RED % OFF BADGES (Image 1 Screen 2)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top Restaurants 🔥", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        Text("See All →", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                    }
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(DemoCatalog.restaurants.take(5), key = { "top_${it.id}" }) { rest ->
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = SoftWhite,
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .width(172.dp)
                                    .clickable { onOpenRestaurant(rest.id) }
                            ) {
                                Column {
                                    Box(modifier = Modifier.fillMaxWidth().height(98.dp)) {
                                        Image(
                                            painter = painterResource(
                                                when (rest.categoryTag) {
                                                    "Pizza" -> R.drawable.img_takeit_pizza_offer_1795427326877
                                                    "Burger" -> R.drawable.img_takeit_hero_burger_1795427328311
                                                    else -> R.drawable.img_food_feast_1795421416008
                                                }
                                            ),
                                            contentDescription = rest.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(FoodCrimson)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text("50% OFF", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                        }
                                    }
                                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(rest.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("★ ${rest.rating} (${rest.reviewCount / 1000}K+)", style = MaterialTheme.typography.labelSmall, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                        Text("${rest.deliveryMins} min • ₹${rest.priceForTwo} for two", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 7. FILTER BAR (Sort ∨ | Ratings ∨ | Offers ∨ | Veg ∨ — Image 1 Screen 3)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item { FoodFilterChipPill("Sort ∨", sortByRating) { sortByRating = !sortByRating } }
                    item { FoodFilterChipPill("Ratings 4.7+", highRatedOnly) { highRatedOnly = !highRatedOnly } }
                    item { FoodFilterChipPill("Offers ∨", offersOnly) { offersOnly = !offersOnly } }
                    item { FoodFilterChipPill("Pure Veg", pureVegMode) { pureVegMode = !pureVegMode } }
                }
            }

            // 8. FULL-WIDTH PHOTO RESTAURANT CARDS WITH GREEN % OFF TAG (Image 1 Screen 3)
            items(filtered, key = { it.id }) { rest ->
                val isFav = rest.id in uiState.favouriteRestaurantIds
                FoodieRestaurantFullCard(
                    rest = rest,
                    isFav = isFav,
                    onOpen = { onOpenRestaurant(rest.id) },
                    onToggleFav = { onToggleFavourite(rest.id) }
                )
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
                tag = "food_view_cart_fab"
            )
        }
    }
}

@Composable
private fun FoodieRestaurantFullCard(
    rest: RestaurantInfo,
    isFav: Boolean,
    onOpen: () -> Unit,
    onToggleFav: () -> Unit
) {
    val imgRes = when (rest.categoryTag) {
        "Pizza" -> R.drawable.img_takeit_pizza_offer_1795427326877
        "Burger" -> R.drawable.img_takeit_hero_burger_1795427328311
        else -> R.drawable.img_food_feast_1795421416008
    }
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SoftWhite,
        shadowElevation = 4.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .bounceClick(onOpen)
            .testTag("restaurant_card_${rest.id}")
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(148.dp)) {
                Image(
                    painter = painterResource(imgRes),
                    contentDescription = rest.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                IconButton(
                    onClick = onToggleFav,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F172A))
                ) {
                    Icon(
                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        "Favourite",
                        tint = if (isFav) FoodCrimson else SoftWhite,
                        modifier = Modifier.size(19.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(FoodOfferGreen)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = if (rest.offerText.contains("50%")) "50% OFF" else "40% OFF",
                        style = MaterialTheme.typography.labelMedium,
                        color = SoftWhite,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(rest.name, style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(3.dp))
                    Text("${rest.rating} (${rest.reviewCount / 1000}K+)  •  ${rest.deliveryMins} min", style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.Bold)
                }
                Text("₹${rest.priceForTwo} for two  •  ${rest.cuisines}", style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
private fun FoodFilterChipPill(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) Color(0xFFFEE2E2) else SoftWhite)
            .border(1.dp, if (selected) FoodCrimson else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = if (selected) FoodCrimson else DeepNavy, fontWeight = FontWeight.Bold)
    }
}

// =========================================================================
// RESTAURANT DETAIL SCREEN (Matches Image 1 Screen 4: Cover photo, Crest, Menu/Reviews/About, Gold 50% OFF, Red Add+)
// =========================================================================
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
    var selectedTab by remember { mutableIntStateOf(0) } // 0=Menu, 1=Reviews, 2=About
    var customizingDish by remember { mutableStateOf<MenuItemEntity?>(null) }
    val isFav = restaurant.id in uiState.favouriteRestaurantIds

    Box(modifier = Modifier.fillMaxSize().background(FoodPageBg)) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 110.dp else 36.dp)
        ) {
            // Hero Food Cover Photo + Overlapping Crest Badge
            item {
                Box(modifier = Modifier.fillMaxWidth().height(215.dp)) {
                    Image(
                        painter = painterResource(
                            if (restaurant.categoryTag == "Pizza") R.drawable.img_takeit_pizza_offer_1795427326877
                            else R.drawable.img_food_feast_1795421416008
                        ),
                        contentDescription = restaurant.name,
                        modifier = Modifier.fillMaxWidth().height(180.dp),
                        contentScale = ContentScale.Crop
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x990F172A)).testTag("back_button")
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = SoftWhite)
                        }
                        IconButton(
                            onClick = { onToggleFavourite(restaurant.id) },
                            modifier = Modifier.size(44.dp).clip(CircleShape).background(Color(0x990F172A))
                        ) {
                            Icon(if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder, "Save", tint = if (isFav) FoodCrimson else SoftWhite)
                        }
                    }

                    // Overlapping Circular Crest Logo
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 18.dp)
                            .size(74.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(DeepNavy)
                            .border(3.dp, FoodGoldYellow, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Rounded.Restaurant, null, tint = FoodGoldYellow, modifier = Modifier.size(26.dp))
                            Text(restaurant.categoryTag.take(6), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                }
            }

            // Restaurant Info + Pure Veg / Free Delivery + Menu/Reviews/About Tabs
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(restaurant.name, style = MaterialTheme.typography.headlineMedium, color = DeepNavy, fontWeight = FontWeight.Black)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Rounded.Verified, null, tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Star, null, tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("${restaurant.rating} (${restaurant.reviewCount}+ ratings) • ${restaurant.deliveryMins} min • ₹${restaurant.priceForTwo} for two", style = MaterialTheme.typography.labelMedium, color = SlateText, fontWeight = FontWeight.SemiBold)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (restaurant.isPureVeg) {
                            Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFDCFCE7)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text("🟢 Pure Veg", style = MaterialTheme.typography.labelSmall, color = FoodOfferGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        Box(Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFFFFF7ED)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                            Text("🎁 Free Delivery", style = MaterialTheme.typography.labelSmall, color = FoodBannerOrange, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Menu | Reviews | About Tabs
                    Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                        listOf("Menu", "Reviews", "About").forEachIndexed { idx, tab ->
                            val sel = selectedTab == idx
                            Column(
                                modifier = Modifier.weight(1f).clickable { selectedTab = idx }.padding(vertical = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(tab, style = MaterialTheme.typography.titleSmall, color = if (sel) FoodCrimson else MutedText, fontWeight = if (sel) FontWeight.ExtraBold else FontWeight.SemiBold)
                                Spacer(Modifier.height(4.dp))
                                Box(Modifier.fillMaxWidth(0.6f).height(2.5.dp).background(if (sel) FoodCrimson else Color.Transparent))
                            }
                        }
                    }

                    // Golden % Flat 50% OFF Banner (Image 1 Screen 4)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(Brush.horizontalGradient(listOf(Color(0xFFFDE047), Color(0xFFFACC15), Color(0xFFF59E0B))))
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp).clip(CircleShape).background(FoodCrimson), contentAlignment = Alignment.Center) {
                                Text("%", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Flat 50% OFF", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.Black)
                                Text("Use Code: FOOD50", style = MaterialTheme.typography.labelMedium, color = Color(0xFF451A03), fontWeight = FontWeight.Bold)
                            }
                            Icon(Icons.Rounded.ChevronRight, null, tint = DeepNavy)
                        }
                    }
                }
            }

            if (selectedTab == 0) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recommended", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        Text("${allDishes.size} items", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                    }
                }

                // Dish Rows with Square Photo Left + Bold Price + Crimson Red "Add +" Right (Image 1 Screen 4)
                items(allDishes, key = { it.id }) { dish ->
                    val inCartQty = uiState.cart.filter { it.itemId == dish.id }.sumOf { it.quantity }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = SoftWhite,
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 5.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(
                                painter = painterResource(
                                    if (restaurant.categoryTag == "Pizza") R.drawable.img_takeit_pizza_offer_1795427326877
                                    else R.drawable.img_food_feast_1795421416008
                                ),
                                contentDescription = dish.name,
                                modifier = Modifier.size(70.dp).clip(RoundedCornerShape(14.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(dish.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(dish.description, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("₹${dish.price}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.Black)
                            }
                            Spacer(Modifier.width(8.dp))
                            if (inCartQty > 0) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(FoodCrimson)
                                ) {
                                    IconButton(onClick = { onUpdateCartQty(dish.id, -1) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Rounded.Remove, "Less", tint = SoftWhite, modifier = Modifier.size(16.dp))
                                    }
                                    Text("$inCartQty", color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                    IconButton(onClick = { onUpdateCartQty(dish.id, 1) }, modifier = Modifier.size(30.dp)) {
                                        Icon(Icons.Rounded.Add, "More", tint = SoftWhite, modifier = Modifier.size(16.dp))
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { customizingDish = dish },
                                    colors = ButtonDefaults.buttonColors(containerColor = FoodCrimson, contentColor = SoftWhite),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                    modifier = Modifier.height(36.dp).testTag("add_dish_${dish.id}")
                                ) {
                                    Text("Add +", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == 1) {
                item {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        listOf(
                            Triple("Rohit Sharma", "5.0 ★", "Hot, crispy crust and super fast 18-minute delivery!"),
                            Triple("Ananya Verma", "4.8 ★", "Packaging was spotless and the taste is unbeatable."),
                            Triple("Kabir Mehta", "4.7 ★", "Best late-night pizza in Greater Noida.")
                        ).forEach { (author, rating, note) ->
                            Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite) {
                                Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(author, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                                        Text(rating, style = MaterialTheme.typography.labelMedium, color = FoodOfferGreen, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text(note, style = MaterialTheme.typography.bodySmall, color = SlateText)
                                }
                            }
                        }
                    }
                }
            } else {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("About ${restaurant.name}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Text("Cuisines: ${restaurant.cuisines}", style = MaterialTheme.typography.bodyMedium, color = SlateText)
                            Text("FSSAI Verified Kitchen • Hygienic tamper-proof packaging • Open 9:00 AM – 2:00 AM", style = MaterialTheme.typography.bodySmall, color = MutedText)
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
            StickyFoodCartBar(uiState.cartItemCount, uiState.cartGrandTotal, onOpenCart, "detail_view_cart_fab")
        }
    }

    customizingDish?.let { dish ->
        var portion by remember { mutableStateOf("Regular") }
        var extraCheese by remember { mutableStateOf(false) }
        val extraCost = (if (portion == "Large") 80 else 0) + (if (extraCheese) 45 else 0)

        ModalBottomSheet(onDismissRequest = { customizingDish = null }, containerColor = SoftWhite) {
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Customize • ${dish.name}", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FoodFilterChipPill("Regular (₹${dish.price})", portion == "Regular") { portion = "Regular" }
                    FoodFilterChipPill("Large (+₹80)", portion == "Large") { portion = "Large" }
                }
                Row(Modifier.fillMaxWidth().clickable { extraCheese = !extraCheese }, horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Extra Melted Cheese (+₹45)", style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
                    Checkbox(checked = extraCheese, onCheckedChange = { extraCheese = it })
                }
                Button(
                    onClick = {
                        onAddToCart(
                            CartEntry(
                                itemId = dish.id,
                                name = dish.name,
                                sourceName = restaurant.name,
                                categoryType = "FOOD",
                                unitPrice = dish.price + extraCost,
                                mrp = dish.price + extraCost + 35,
                                quantity = 1,
                                customization = if (extraCheese) "$portion • Extra Cheese" else portion
                            )
                        )
                        customizingDish = null
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(containerColor = FoodCrimson, contentColor = SoftWhite)
                ) {
                    Text("Add Item • ₹${dish.price + extraCost}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun StickyFoodCartBar(itemCount: Int, total: Int, onClick: () -> Unit, tag: String) {
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
                .background(Brush.horizontalGradient(listOf(FoodCrimsonDeep, FoodCrimson)))
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

// =========================================================================
// CART, TRACK ORDER & ORDER DELIVERED CELEBRATION (Image 1 Screens 5, 6, 7!)
// =========================================================================
private enum class CheckoutStage { CART, TRACKING, DELIVERED }

@Composable
fun CartCheckoutScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onApplyCoupon: (CouponItem?) -> Unit,
    onPlaceOrder: (paymentMethod: String, simulateFailure: Boolean) -> Boolean,
    onOrderPlacedSuccess: () -> Unit
) {
    var stage by remember { mutableStateOf(CheckoutStage.CART) }
    var selectedPayment by remember { mutableStateOf("Take It Wallet") }
    var simulateFailure by remember { mutableStateOf(false) }
    var lastTitle by remember { mutableStateOf("Margherita Pizza") }
    var lastTotal by remember { mutableIntStateOf(241) }
    var lastCategory by remember { mutableStateOf("FOOD") }

    when (stage) {
        CheckoutStage.TRACKING -> {
            FoodieTrackOrderView(
                orderCategory = lastCategory,
                destinationLabel = uiState.appState.activeAddressLine.ifBlank { "Greater Noida" },
                onBack = onBack,
                onDelivered = { stage = CheckoutStage.DELIVERED }
            )
            return
        }
        CheckoutStage.DELIVERED -> {
            FoodieOrderDeliveredView(
                orderTitle = lastTitle,
                orderTotal = lastTotal,
                onRateAndHome = onOrderPlacedSuccess,
                onTrackAnother = { stage = CheckoutStage.TRACKING },
                onHome = onBack
            )
            return
        }
        CheckoutStage.CART -> Unit
    }

    val isGroceryCart = uiState.cart.firstOrNull()?.categoryType == "GROCERY"
    val isShopCart = uiState.cart.firstOrNull()?.categoryType == "SHOPPING"
    val ctaColor = when {
        isGroceryCart -> FoodOfferGreen
        isShopCart -> Color(0xFF6D28D9)
        else -> FoodCrimson
    }

    Scaffold(
        containerColor = FoodPageBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("back_button")) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                }
                Text("Your Cart", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                IconButton(onClick = { onApplyCoupon(null) }, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.DeleteOutline, "Clear coupon", tint = FoodCrimson)
                }
            }
        }
    ) { innerPadding ->
        if (uiState.cart.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconOrb3D(Icons.Rounded.ShoppingCart, SoftWhite, FoodCrimson, DeepNavy, size = 72.dp, iconSize = 36.dp)
                Spacer(Modifier.height(14.dp))
                Text("Your Cart is Empty", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                Text("Add dishes, groceries, or nearby store items to checkout.", style = MaterialTheme.typography.bodyMedium, color = MutedText, textAlign = TextAlign.Center)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = FoodCrimson, contentColor = SoftWhite), shape = RoundedCornerShape(50)) {
                    Text("Explore Now", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Store / Restaurant Header Pill
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEFF6FF)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(
                                    when {
                                        isGroceryCart -> R.drawable.img_grocery_basket_1795421415559
                                        isShopCart -> R.drawable.img_takeit_local_shop_hero_1795429839958
                                        else -> R.drawable.img_takeit_pizza_offer_1795427326877
                                    }
                                ),
                                contentDescription = "Store",
                                modifier = Modifier.size(42.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(uiState.cart.first().sourceName, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                Text("20 min • ${uiState.appState.activeAddressLabel}", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                        }
                    }
                }

                // Cart Items
                items(uiState.cart, key = { it.itemId + it.customization }) { entry ->
                    Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite, shadowElevation = 2.dp) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(
                                    when (entry.categoryType) {
                                        "GROCERY" -> R.drawable.img_grocery_basket_1795421415559
                                        "SHOPPING" -> R.drawable.img_takeit_local_shop_hero_1795429839958
                                        else -> R.drawable.img_takeit_pizza_offer_1795427326877
                                    }
                                ),
                                contentDescription = entry.name,
                                modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(entry.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                if (entry.customization.isNotBlank()) {
                                    Text(entry.customization, style = MaterialTheme.typography.labelSmall, color = MutedText)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFF1F5F9)).padding(horizontal = 4.dp)
                                ) {
                                    IconButton(onClick = { onUpdateQty(entry.itemId, -1) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Remove, "Less", tint = DeepNavy, modifier = Modifier.size(14.dp))
                                    }
                                    Text("${entry.quantity}", style = MaterialTheme.typography.labelLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    IconButton(onClick = { onUpdateQty(entry.itemId, 1) }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Add, "More", tint = DeepNavy, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                            Text("₹${entry.unitPrice * entry.quantity}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.Black)
                        }
                    }
                }

                // Apply Coupon Row
                item {
                    Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.LocalOffer, null, tint = FoodOfferGreen, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Apply Coupon", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                                Icon(Icons.Rounded.ChevronRight, null, tint = MutedText)
                            }
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(DemoCatalog.coupons) { coupon ->
                                    val applied = uiState.appliedCoupon?.code == coupon.code
                                    FoodFilterChipPill("${coupon.code} (-₹${coupon.discountAmount})", applied) {
                                        onApplyCoupon(if (applied) null else coupon)
                                    }
                                }
                            }
                        }
                    }
                }

                // Bill Summary + Golden Yellow Total Payable Bar (Exact Image 1 Screen 5!)
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            BillRow("Item Total", "₹${uiState.cartSubtotal}")
                            BillRow("Delivery Fee", "₹${uiState.cartDeliveryFee}")
                            BillRow("Platform Fee", "₹${uiState.cartPlatformFee}")
                            BillRow("GST & Taxes", "₹${uiState.cartTaxes}")
                            if (uiState.cartDiscount > 0) {
                                BillRow("Discount", "- ₹${uiState.cartDiscount}", FoodOfferGreen)
                            }
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFFDE047), FoodGoldYellow)))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text("Total Payable", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    Text("₹${uiState.cartGrandTotal}", style = MaterialTheme.typography.headlineSmall, color = DeepNavy, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    }
                }

                // Payment Methods
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Payment Method", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            listOf("Take It Wallet", "UPI • GPay", "Credit / Debit Card", "Cash on Delivery").forEach { method ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedPayment == method) Color(0xFFF8FAFC) else Color.Transparent)
                                        .clickable { selectedPayment = method }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedPayment == method, onClick = { selectedPayment = method })
                                    Text(
                                        if (method == "Take It Wallet") "$method (₹${uiState.appState.walletBalance.toInt()})" else method,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = DeepNavy
                                    )
                                }
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Simulate Payment Failure", style = MaterialTheme.typography.labelSmall, color = FoodCrimson)
                                Switch(checked = simulateFailure, onCheckedChange = { simulateFailure = it })
                            }
                        }
                    }
                }

                // Full-width Crimson Red "Proceed to Pay →" Button
                item {
                    Button(
                        onClick = {
                            val titleSnap = uiState.cart.firstOrNull()?.name ?: "Margherita Pizza"
                            val catSnap = uiState.cart.firstOrNull()?.categoryType ?: "FOOD"
                            val totalSnap = uiState.cartGrandTotal
                            if (onPlaceOrder(selectedPayment, simulateFailure)) {
                                lastTitle = titleSnap
                                lastCategory = catSnap
                                lastTotal = totalSnap
                                stage = CheckoutStage.TRACKING
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .navigationBarsPadding()
                            .testTag("checkout_pay_button"),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = ctaColor, contentColor = SoftWhite)
                    ) {
                        Text("Proceed to Pay →", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun FoodieTrackOrderView(
    orderCategory: String,
    destinationLabel: String,
    onBack: () -> Unit,
    onDelivered: () -> Unit
) {
    val steps = remember { listOf("Order\nPlaced", "Preparing", "Out for\nDelivery", "Delivered") }
    var stageIdx by remember { mutableIntStateOf(2) }
    var bannerMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftWhite)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("back_button")) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
            }
            Text("Track Order", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = { bannerMsg = "24x7 Support connected!" }, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.SupportAgent, "Support", tint = DeepNavy)
            }
        }

        // Map
        SimulatedMapCanvas(
            modifier = Modifier.fillMaxWidth().weight(1f),
            pickupLabel = if (orderCategory == "GROCERY") "FreshMart" else "Kitchen",
            dropLabel = destinationLabel.substringBefore(","),
            distanceKm = 2.8,
            etaMinutes = ((3 - stageIdx) * 6).coerceAtLeast(2),
            driverProgress = when (stageIdx) { 0 -> 0.15f; 1 -> 0.45f; 2 -> 0.72f; else -> 0.96f }
        )

        // Crisp White Tracking Card (Matches Image 1 Screen 6!)
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = SoftWhite,
            shadowElevation = 14.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Your order is on the way!", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        Text("Arriving in", style = MaterialTheme.typography.labelMedium, color = MutedText)
                        Text("${((3 - stageIdx) * 6).coerceAtLeast(3)} min", style = MaterialTheme.typography.headlineLarge, color = FoodOfferGreen, fontWeight = FontWeight.Black)
                    }
                    Character3DPortrait(
                        drawableRes = if (orderCategory == "GROCERY") R.drawable.img_char_grocery_buddy_1795426023736 else R.drawable.img_char_food_rider_1795426023084,
                        contentDescription = "Delivery Rider",
                        borderColor = FoodCrimson,
                        size = 66.dp,
                        bobbing = false
                    )
                }

                // Rider Row
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Rider: Rahul Kumar", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        Text("4.7 ★ | DL01AB1234", style = MaterialTheme.typography.labelMedium, color = MutedText)
                    }
                    IconButton(
                        onClick = { bannerMsg = "Calling Rahul Kumar..." },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFEE2E2))
                    ) { Icon(Icons.Rounded.Call, "Call", tint = FoodCrimson, modifier = Modifier.size(19.dp)) }
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { bannerMsg = "Sent message to Rahul Kumar!" },
                        modifier = Modifier.size(40.dp).clip(CircleShape).background(Color(0xFFFEE2E2))
                    ) { Icon(Icons.Rounded.Chat, "Chat", tint = FoodCrimson, modifier = Modifier.size(19.dp)) }
                }

                if (bannerMsg != null) {
                    Text(bannerMsg ?: "", style = MaterialTheme.typography.labelSmall, color = FoodOfferGreen, fontWeight = FontWeight.Bold)
                }

                // 4-Step Green Check Bar
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    steps.forEachIndexed { idx, label ->
                        val reached = idx <= stageIdx
                        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Box(Modifier.weight(1f).height(3.dp).background(if (idx == 0) Color.Transparent else if (idx <= stageIdx) FoodOfferGreen else Color(0xFFE2E8F0)))
                                Box(Modifier.size(24.dp).clip(CircleShape).background(if (reached) FoodOfferGreen else Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Check, null, tint = if (reached) SoftWhite else MutedText, modifier = Modifier.size(15.dp))
                                }
                                Box(Modifier.weight(1f).height(3.dp).background(if (idx == steps.lastIndex) Color.Transparent else if (idx < stageIdx) FoodOfferGreen else Color(0xFFE2E8F0)))
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (reached) DeepNavy else MutedText, textAlign = TextAlign.Center, lineHeight = 12.sp)
                        }
                    }
                }

                // Soft Lavender AI Tracking Pill + Mark Delivered Action
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEEF2FF))
                        .clickable {
                            if (stageIdx < 3) stageIdx++ else onDelivered()
                        }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Bolt, null, tint = Color(0xFF6366F1), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Live smart route tracking active", style = MaterialTheme.typography.labelMedium, color = Color(0xFF3730A3), fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (stageIdx < 3) "Next Step →" else "Delivered ✓",
                        style = MaterialTheme.typography.labelMedium,
                        color = FoodCrimson,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}

@Composable
private fun FoodieOrderDeliveredView(
    orderTitle: String,
    orderTotal: Int,
    onRateAndHome: () -> Unit,
    onTrackAnother: () -> Unit,
    onHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFEFF6FF), SoftWhite)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(86.dp)
                .shadow(12.dp, CircleShape, spotColor = FoodOfferGreen)
                .clip(CircleShape)
                .background(FoodOfferGreen),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, "Delivered", tint = SoftWhite, modifier = Modifier.size(48.dp))
        }
        Spacer(Modifier.height(18.dp))
        Text("Order Delivered!", style = MaterialTheme.typography.headlineLarge, color = DeepNavy, fontWeight = FontWeight.Black)
        Text("Enjoy your delicious meal 🎉", style = MaterialTheme.typography.titleSmall, color = Color(0xFF3B82F6), fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(20.dp))
        Image(
            painter = painterResource(R.drawable.img_takeit_pizza_offer_1795427326877),
            contentDescription = orderTitle,
            modifier = Modifier.fillMaxWidth().height(165.dp).clip(RoundedCornerShape(22.dp)),
            contentScale = ContentScale.Crop
        )
        Spacer(Modifier.height(10.dp))
        Text("$orderTitle • Paid ₹$orderTotal", style = MaterialTheme.typography.labelLarge, color = SlateText)
        Spacer(Modifier.height(22.dp))
        OutlinedButton(
            onClick = onRateAndHome,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Rate Order ★", color = FoodCrimson, fontWeight = FontWeight.ExtraBold)
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onTrackAnother,
            modifier = Modifier.fillMaxWidth().height(50.dp),
            shape = RoundedCornerShape(50)
        ) {
            Text("Track Another Order", color = DeepNavy, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = onHome,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = FoodCrimson, contentColor = SoftWhite)
        ) {
            Text("Home", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun BillRow(label: String, value: String, valueColor: Color = SlateText, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium, color = if (bold) DeepNavy else SlateText)
        Text(value, style = if (bold) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyMedium, color = valueColor, fontWeight = if (bold) FontWeight.ExtraBold else FontWeight.SemiBold)
    }
}
