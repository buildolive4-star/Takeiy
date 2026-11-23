package com.example.takeit.ui.customer

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.viewmodel.TakeItUiState

@Composable
fun FoodPhoto(
    imageUrl: String,
    fallbackKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    if (imageUrl.startsWith("https://")) {
        AsyncImage(
            model = imageUrl,
            contentDescription = contentDescription,
            placeholder = painterResource(resolveFoodDrawable(fallbackKey)),
            error = painterResource(resolveFoodDrawable(fallbackKey)),
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Image(
            painter = painterResource(resolveFoodDrawable(fallbackKey)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    }
}

@Composable
fun VegMarkIcon(isVeg: Boolean, modifier: Modifier = Modifier) {
    val color = if (isVeg) TakeItGreen else TakeItRed
    Box(
        modifier = modifier
            .size(16.dp)
            .border(1.5.dp, color, RoundedCornerShape(4.dp))
            .padding(3.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun CustomerHomeTab(
    uiState: TakeItUiState,
    onSearchChange: (String) -> Unit,
    onOpenRestaurants: (String) -> Unit,
    onOpenRestaurantDetail: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenAddresses: () -> Unit,
    onOpenCart: () -> Unit,
    onOpenTrackOrder: (String) -> Unit,
    onOpenOffersTab: () -> Unit
) {
    val activeOrder = remember(uiState.myOrders) {
        uiState.myOrders.firstOrNull { it.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED") }
    }
    val filteredRestaurants = remember(uiState.restaurants, uiState.searchQuery) {
        val q = uiState.searchQuery.trim().lowercase()
        if (q.isBlank()) uiState.restaurants
        else uiState.restaurants.filter {
            it.name.lowercase().contains(q) || it.cuisine.lowercase().contains(q) || it.category.lowercase().contains(q)
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TakeItBg),
        contentPadding = PaddingValues(bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top Brand & Location Bar
        item {
            Surface(
                color = Color.White,
                shadowElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onOpenAddresses() }
                                .padding(vertical = 2.dp)
                        ) {
                            Image(
                                painter = painterResource(R.drawable.logo_takeit_food),
                                contentDescription = "Take It Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .border(1.5.dp, TakeItOrange, CircleShape)
                            )
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.LocationOn, null, tint = TakeItRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(2.dp))
                                    Text(
                                        text = uiState.profile.activeAddressLabel.ifBlank { uiState.config.cityName },
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TakeItInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Icon(Icons.Rounded.KeyboardArrowDown, null, tint = TakeItInk, modifier = Modifier.size(18.dp))
                                }
                                Text(
                                    text = uiState.profile.activeAddressLine.ifBlank { uiState.config.cityName },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TakeItMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                shape = CircleShape,
                                color = TakeItGoldSoft,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { onOpenOffersTab() }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.LocalOffer, "Offers", tint = TakeItRed, modifier = Modifier.size(20.dp))
                                }
                            }
                            Surface(
                                shape = CircleShape,
                                color = TakeItRed,
                                modifier = Modifier
                                    .size(40.dp)
                                    .clickable { onOpenCart() }
                                    .testTag("home_cart_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.ShoppingBag, "Cart", tint = Color.White, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    // Search Bar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search for restaurants, biryani, pizza...", color = TakeItMuted, fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TakeItRed) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchChange("") }) {
                                    Icon(Icons.Rounded.Close, "Clear", tint = TakeItMuted)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TakeItRed,
                            unfocusedBorderColor = TakeItBorder,
                            focusedContainerColor = TakeItBg,
                            unfocusedContainerColor = TakeItBg
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_search_input")
                    )
                }
            }
        }

        // Live Admin Announcement Bar (if set by Admin)
        if (uiState.config.announcementText.isNotBlank()) {
            item {
                Surface(
                    color = TakeItGoldSoft,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TakeItGold),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.Campaign, null, tint = TakeItRed, modifier = Modifier.size(20.dp))
                        Text(
                            uiState.config.announcementText,
                            style = MaterialTheme.typography.labelMedium,
                            color = TakeItInk,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Live Order Tracking Pill if Customer has an active order!
        if (activeOrder != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = TakeItGreen,
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clickable { onOpenTrackOrder(activeOrder.id) }
                        .testTag("active_order_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.DeliveryDining, null, tint = Color.White, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Live Order • ${activeOrder.status.replace("_", " ")}",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    "${activeOrder.restaurantName} • ETA ${activeOrder.etaMinutes} min",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.92f)
                                )
                            }
                        }
                        Surface(shape = RoundedCornerShape(50), color = Color.White) {
                            Text(
                                "Track →",
                                style = MaterialTheme.typography.labelMedium,
                                color = TakeItGreen,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Hero Offer Banner (100% Live Controlled by Admin!)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(164.dp)
                    .shadow(8.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.horizontalGradient(listOf(TakeItOrange, TakeItRed, TakeItRedDark)))
                    .clickable { onOpenRestaurants("All") }
            ) {
                Image(
                    painter = painterResource(R.drawable.img_takeit_pizza_offer_1795427326877),
                    contentDescription = "Special Offer",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(165.dp)
                        .clip(RoundedCornerShape(topStart = 90.dp, bottomStart = 90.dp))
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.62f)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = uiState.config.heroBannerTitle.ifBlank { "Flat 50% OFF" },
                            style = MaterialTheme.typography.headlineSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 28.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = uiState.config.heroBannerSubtitle.ifBlank { "on your Favourite Food" },
                            style = MaterialTheme.typography.bodySmall,
                            color = TakeItGoldSoft,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Order Now",
                                style = MaterialTheme.typography.labelMedium,
                                color = TakeItRed,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = TakeItRed, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }

        // 2-Row Circular Food Category Grid (10 categories like FoodieX/Zomato reference!)
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "What's on your mind?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TakeItInk
                )
                TakeItFoodCategories.chunked(5).forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        rowItems.forEach { cat ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(64.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { onOpenRestaurants(cat.name) }
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .shadow(3.dp, CircleShape)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                        .border(1.5.dp, TakeItBorder, CircleShape)
                                ) {
                                    Image(
                                        painter = painterResource(cat.fallbackDrawable),
                                        contentDescription = cat.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                Spacer(Modifier.height(5.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TakeItInk,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Late Night Cravings Banner (Live Admin Controlled!)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(112.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Brush.horizontalGradient(listOf(TakeItNightBlue, Color(0xFF312E81), Color(0xFF581C87))))
                    .clickable { onOpenRestaurants("All") }
            ) {
                Image(
                    painter = painterResource(R.drawable.img_food_feast_1795421416008),
                    contentDescription = "Late Night",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .fillMaxHeight()
                        .width(126.dp)
                        .clip(RoundedCornerShape(topStart = 70.dp, bottomStart = 70.dp))
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.65f)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            uiState.config.lateNightTitle.ifBlank { "Late Night Cravings?" },
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            uiState.config.lateNightSubtitle.ifBlank { "We're Here!" },
                            style = MaterialTheme.typography.labelMedium,
                            color = TakeItGoldSoft
                        )
                    }
                    Surface(shape = RoundedCornerShape(50), color = TakeItOrange) {
                        Text(
                            "Order Now →",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Top Restaurants Section Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Top Restaurants 🔥",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = TakeItInk
                )
                TextButton(onClick = { onOpenRestaurants("All") }) {
                    Text("See All →", color = TakeItRed, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Empty state if Admin hasn't added restaurants yet
        if (filteredRestaurants.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Rounded.RestaurantMenu, null, tint = TakeItRed, modifier = Modifier.size(44.dp))
                        Text("No restaurants live yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "Admin can add live restaurants & dishes from Take It Admin Panel.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TakeItMuted
                        )
                    }
                }
            }
        } else {
            // Horizontal Top Restaurants Cards
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredRestaurants.take(6), key = { "top_${it.id}" }) { r ->
                        CompactRestaurantCard(
                            restaurant = r,
                            onClick = { onOpenRestaurantDetail(r.id) }
                        )
                    }
                }
            }

            // Full Restaurant Feed Header
            item {
                Text(
                    "All Restaurants Near You",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TakeItInk,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            // Vertical Full-Width Zomato-Style Restaurant Cards
            items(filteredRestaurants, key = { it.id }) { r ->
                FullRestaurantCard(
                    restaurant = r,
                    isFavorite = uiState.profile.favoriteRestaurantIds.contains(r.id),
                    onToggleFavorite = { onToggleFavorite(r.id) },
                    onClick = { onOpenRestaurantDetail(r.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactRestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = Modifier
            .width(176.dp)
            .clickable(onClick = onClick)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                FoodPhoto(
                    imageUrl = restaurant.imageUrl,
                    fallbackKey = "${restaurant.category} ${restaurant.name}",
                    contentDescription = restaurant.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (restaurant.offerTag.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 10.dp),
                        color = TakeItRed,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            restaurant.offerTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (!restaurant.openFlag) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.55f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("CLOSED", color = Color.White, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    restaurant.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TakeItInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Star, null, tint = TakeItGold, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(3.dp))
                    Text(
                        "${restaurant.rating} (${restaurant.ratingCount})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TakeItInk,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "${restaurant.deliveryTime} • ₹${restaurant.priceForTwo} for two",
                    style = MaterialTheme.typography.labelSmall,
                    color = TakeItMuted,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun FullRestaurantCard(
    restaurant: Restaurant,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("restaurant_card_${restaurant.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(172.dp)
            ) {
                FoodPhoto(
                    imageUrl = restaurant.imageUrl,
                    fallbackKey = "${restaurant.category} ${restaurant.name}",
                    contentDescription = restaurant.name,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)))
                        )
                )
                // Favorite Heart
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.92f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .clickable(onClick = onToggleFavorite)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) TakeItRed else TakeItInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                // Green % OFF Badge matching FoodieX design
                if (restaurant.offerTag.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = TakeItGreen,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(12.dp)
                    ) {
                        Text(
                            restaurant.offerTag,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                if (!restaurant.openFlag) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.58f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(shape = RoundedCornerShape(50), color = TakeItRed) {
                            Text(
                                "CURRENTLY CLOSED",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            restaurant.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = TakeItInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (restaurant.verifiedFlag) {
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.Rounded.Verified, null, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                        }
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = TakeItGreen) {
                        Row(
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${restaurant.rating}", color = Color.White, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                            Icon(Icons.Rounded.Star, null, tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Text(
                    restaurant.cuisine,
                    style = MaterialTheme.typography.bodySmall,
                    color = TakeItMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${restaurant.deliveryTime} • ₹${restaurant.priceForTwo} for two",
                        style = MaterialTheme.typography.labelMedium,
                        color = TakeItInk,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (restaurant.pureVegFlag) {
                            Surface(shape = RoundedCornerShape(6.dp), color = TakeItGreenSoft) {
                                Text("Pure Veg", style = MaterialTheme.typography.labelSmall, color = TakeItGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        if (restaurant.freeDeliveryFlag) {
                            Surface(shape = RoundedCornerShape(6.dp), color = TakeItGoldSoft) {
                                Text("Free Delivery", style = MaterialTheme.typography.labelSmall, color = TakeItRedDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RestaurantsListScreen(
    initialCategory: String,
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onOpenRestaurantDetail: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var pureVegOnly by remember { mutableStateOf(false) }
    var highRatingOnly by remember { mutableStateOf(false) }
    var offersOnly by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val filtered = remember(uiState.restaurants, selectedCategory, pureVegOnly, highRatingOnly, offersOnly, search) {
        uiState.restaurants.filter { r ->
            val catOk = selectedCategory == "All" || r.category.equals(selectedCategory, ignoreCase = true) || r.cuisine.contains(selectedCategory, ignoreCase = true)
            val vegOk = !pureVegOnly || r.pureVegFlag
            val ratOk = !highRatingOnly || r.rating >= 4.4
            val offOk = !offersOnly || r.offerTag.isNotBlank()
            val qOk = search.isBlank() || r.name.contains(search, ignoreCase = true) || r.cuisine.contains(search, ignoreCase = true)
            catOk && vegOk && ratOk && offOk && qOk
        }
    }

    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectedCategory == "All") "Restaurants" else "$selectedCategory Restaurants",
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Filter restaurants...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = TakeItRed) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = highRatingOnly,
                            onClick = { highRatingOnly = !highRatingOnly },
                            label = { Text("Ratings 4.4+") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = offersOnly,
                            onClick = { offersOnly = !offersOnly },
                            label = { Text("Offers") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = pureVegOnly,
                            onClick = { pureVegOnly = !pureVegOnly },
                            label = { Text("Pure Veg") }
                        )
                    }
                    items(TakeItFoodCategories.map { it.name }) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat) }
                        )
                    }
                }
            }
            if (filtered.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No restaurants match these filters.", color = TakeItMuted)
                    }
                }
            } else {
                items(filtered, key = { it.id }) { r ->
                    FullRestaurantCard(
                        restaurant = r,
                        isFavorite = uiState.profile.favoriteRestaurantIds.contains(r.id),
                        onToggleFavorite = { onToggleFavorite(r.id) },
                        onClick = { onOpenRestaurantDetail(r.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun RestaurantDetailScreen(
    restaurantId: String,
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onAddToCart: (MenuItem) -> Unit,
    onUpdateCartQty: (String, Int) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onOpenCart: () -> Unit
) {
    val restaurant = uiState.restaurants.firstOrNull { it.id == restaurantId }
    val dishes = remember(uiState.menuItems, restaurantId) {
        uiState.menuItems.filter { it.restaurantId == restaurantId }
    }
    val restReviews = remember(uiState.reviews, restaurantId) {
        uiState.reviews.filter { it.restaurantId == restaurantId }
    }
    var selectedSectionTab by remember { mutableIntStateOf(0) } // 0=Menu, 1=Reviews, 2=About
    var vegOnly by remember { mutableStateOf(false) }

    if (restaurant == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Restaurant not found", fontWeight = FontWeight.Bold)
                TextButton(onClick = onBack) { Text("Go Back") }
            }
        }
        return
    }

    val isFav = uiState.profile.favoriteRestaurantIds.contains(restaurant.id)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TakeItBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp)
        ) {
            // Full-bleed Hero + Overlapping Circular Emblem
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    FoodPhoto(
                        imageUrl = restaurant.imageUrl,
                        fallbackKey = "${restaurant.category} ${restaurant.name}",
                        contentDescription = restaurant.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.3f))))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable(onClick = onBack)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = TakeItInk)
                            }
                        }
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.92f),
                            modifier = Modifier
                                .size(42.dp)
                                .clickable { onToggleFavorite(restaurant.id) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    "Favorite",
                                    tint = if (isFav) TakeItRed else TakeItInk
                                )
                            }
                        }
                    }

                    // Overlapping Restaurant Info White Card
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = Color.White,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 195.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    restaurant.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TakeItInk
                                )
                                if (restaurant.verifiedFlag) {
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Rounded.Verified, "Verified", tint = Color(0xFF2563EB), modifier = Modifier.size(20.dp))
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.Star, null, tint = TakeItGold, modifier = Modifier.size(18.dp))
                                Text(
                                    "${restaurant.rating} (${restaurant.ratingCount} ratings)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TakeItInk
                                )
                                Text("• ${restaurant.deliveryTime} • ₹${restaurant.priceForTwo} for two", style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (restaurant.pureVegFlag) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = TakeItGreenSoft) {
                                        Text("Pure Veg", style = MaterialTheme.typography.labelSmall, color = TakeItGreen, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                                if (restaurant.freeDeliveryFlag) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = TakeItGoldSoft) {
                                        Text("Free Delivery", style = MaterialTheme.typography.labelSmall, color = TakeItRedDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                                if (!restaurant.openFlag) {
                                    Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFFFEE2E2)) {
                                        Text("Closed Right Now", style = MaterialTheme.typography.labelSmall, color = TakeItRed, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                                    }
                                }
                            }

                            // Tabs: Menu | Reviews | About
                            TabRow(
                                selectedTabIndex = selectedSectionTab,
                                containerColor = Color.White,
                                contentColor = TakeItRed
                            ) {
                                listOf("Menu (${dishes.size})", "Reviews (${restReviews.size})", "About").forEachIndexed { idx, label ->
                                    Tab(
                                        selected = selectedSectionTab == idx,
                                        onClick = { selectedSectionTab = idx },
                                        text = { Text(label, fontWeight = FontWeight.Bold) }
                                    )
                                }
                            }

                            // Golden Offer Card
                            if (restaurant.offerTag.isNotBlank() && selectedSectionTab == 0) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = TakeItGoldSoft,
                                    border = BorderStroke(1.dp, TakeItGold),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(14.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(CircleShape)
                                                .background(TakeItRed),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text("%", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                        }
                                        Column {
                                            Text(
                                                "Flat ${restaurant.offerTag}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = TakeItInk
                                            )
                                            Text(
                                                "Use Code: ${restaurant.offerCode.ifBlank { "FOOD50" }} in Cart",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TakeItMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Tab Body
            when (selectedSectionTab) {
                0 -> {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Recommended Menu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            FilterChip(
                                selected = vegOnly,
                                onClick = { vegOnly = !vegOnly },
                                label = { Text("Veg Only") }
                            )
                        }
                    }
                    val visibleDishes = if (vegOnly) dishes.filter { it.vegFlag } else dishes
                    if (visibleDishes.isEmpty()) {
                        item {
                            Text(
                                "No dishes available right now.",
                                color = TakeItMuted,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        items(visibleDishes, key = { it.id }) { dish ->
                            val cartQty = uiState.cart.firstOrNull { it.menuItem.id == dish.id }?.quantity ?: 0
                            DishMenuCard(
                                dish = dish,
                                quantityInCart = cartQty,
                                restaurantOpen = restaurant.openFlag,
                                onAdd = { onAddToCart(dish) },
                                onInc = { onUpdateCartQty(dish.id, 1) },
                                onDec = { onUpdateCartQty(dish.id, -1) }
                            )
                        }
                    }
                }
                1 -> {
                    if (restReviews.isEmpty()) {
                        item {
                            Text(
                                "No reviews yet. Order and leave the first 5-star review!",
                                color = TakeItMuted,
                                modifier = Modifier.padding(24.dp)
                            )
                        }
                    } else {
                        items(restReviews, key = { it.id }) { rev ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(rev.userName, fontWeight = FontWeight.Bold, color = TakeItInk)
                                        Text("★ ${rev.rating}.0", color = TakeItGreen, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Text(rev.comment, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                                }
                            }
                        }
                    }
                }
                2 -> {
                    item {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("About ${restaurant.name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text("Cuisine: ${restaurant.cuisine}", color = TakeItInk)
                                Text("Address: ${restaurant.address}", color = TakeItMuted)
                                Text("Average Prep & Delivery: ${restaurant.deliveryTime}", color = TakeItMuted)
                                Text("Fulfilled live by Take It Food Delivery Cloud.", style = MaterialTheme.typography.labelSmall, color = TakeItRed)
                            }
                        }
                    }
                }
            }
        }

        // Sticky Floating Cart Bar
        if (uiState.cartItemCount > 0) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = TakeItRed,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clickable(onClick = onOpenCart)
                    .testTag("floating_view_cart_bar")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "${uiState.cartItemCount} item(s) | ₹${uiState.cartItemTotal}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            "From ${uiState.cartRestaurantName}",
                            color = TakeItGoldSoft,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("View Cart", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(4.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DishMenuCard(
    dish: MenuItem,
    quantityInCart: Int,
    restaurantOpen: Boolean,
    onAdd: () -> Unit,
    onInc: () -> Unit,
    onDec: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FoodPhoto(
                imageUrl = dish.imageUrl,
                fallbackKey = dish.name,
                contentDescription = dish.name,
                modifier = Modifier
                    .size(86.dp)
                    .clip(RoundedCornerShape(14.dp))
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    VegMarkIcon(dish.vegFlag)
                    if (dish.bestsellerFlag) {
                        Surface(shape = RoundedCornerShape(6.dp), color = TakeItGoldSoft) {
                            Text("Bestseller", style = MaterialTheme.typography.labelSmall, color = TakeItRedDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp))
                        }
                    }
                }
                Text(dish.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = TakeItInk)
                if (dish.description.isNotBlank()) {
                    Text(dish.description, style = MaterialTheme.typography.labelSmall, color = TakeItMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("₹${dish.price}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.ExtraBold, color = TakeItInk)
                    if (dish.originalPrice > dish.price) {
                        Text(
                            "₹${dish.originalPrice}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TakeItMuted,
                            textDecoration = TextDecoration.LineThrough
                        )
                    }
                }
            }
            if (!restaurantOpen || !dish.availableFlag) {
                Surface(shape = RoundedCornerShape(10.dp), color = TakeItBg) {
                    Text(
                        if (!dish.availableFlag) "Sold Out" else "Closed",
                        style = MaterialTheme.typography.labelSmall,
                        color = TakeItMuted,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                    )
                }
            } else if (quantityInCart <= 0) {
                Button(
                    onClick = onAdd,
                    colors = ButtonDefaults.buttonColors(containerColor = TakeItRed, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_dish_${dish.id}")
                ) {
                    Text("Add +", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = TakeItRed
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onDec, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Remove, "Decrease", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                        Text("$quantityInCart", color = Color.White, fontWeight = FontWeight.ExtraBold)
                        IconButton(onClick = onInc, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Add, "Increase", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}
