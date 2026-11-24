package com.example.takeit.ui.customer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DHeroBanner
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.viewmodel.TakeItUiState
import kotlinx.coroutines.delay

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
    onOpenOffersTab: () -> Unit,
    onOpenRestaurantTab: () -> Unit = {}
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

    val dynamicCategories = remember(uiState.categories) {
        val active = uiState.categories.filter { it.activeFlag }.sortedBy { it.sortOrder }
        if (active.isNotEmpty()) {
            active.map { cat ->
                FoodCategoryItem(
                    name = cat.name,
                    emoji = cat.emoji,
                    fallbackDrawable = resolveFoodDrawable(cat.name),
                    imageUrl = cat.imageUrl
                )
            }
        } else {
            TakeItFoodCategories.filter { it.name != "All" }
        }
    }

    val bannerSlides = remember(uiState.heroBanners, uiState.config) {
        val uploaded = uiState.heroBanners.filter { it.activeFlag }.sortedBy { it.sortOrder }
        if (uploaded.isNotEmpty()) {
            uploaded
        } else {
            listOf(
                HeroBannerSlide(
                    id = "builtin_offer",
                    title = uiState.config.heroBannerTitle.ifBlank { "Flat 50% OFF" },
                    subtitle = uiState.config.heroBannerSubtitle.ifBlank { "on your Favourite Food" },
                    promoCode = uiState.config.heroBannerCode,
                    imageUrl = "builtin://pizza"
                ),
                HeroBannerSlide(
                    id = "builtin_welcome",
                    title = "Hot Meals Delivered Fast",
                    subtitle = "Top restaurants in ${uiState.config.cityName} • Live GPS tracking",
                    promoCode = "",
                    imageUrl = "builtin://welcome"
                ),
                HeroBannerSlide(
                    id = "builtin_night",
                    title = uiState.config.lateNightTitle.ifBlank { "Late Night Cravings?" },
                    subtitle = uiState.config.lateNightSubtitle.ifBlank { "We're Here! Open till 3 AM" },
                    promoCode = "",
                    imageUrl = "builtin://feast"
                )
            )
        }
    }

    val pagerState = rememberPagerState(pageCount = { bannerSlides.size })
    LaunchedEffect(bannerSlides.size) {
        while (bannerSlides.size > 1) {
            delay(3000L)
            val next = (pagerState.currentPage + 1) % bannerSlides.size
            pagerState.animateScrollToPage(next)
        }
    }

    TakeIt3DBackdrop(
        accentPrimary = TakeItRed,
        accentSecondary = TakeItOrange,
        baseColor = TakeItBg
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 110.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Compact 3D Top Header + Compact Search Bar
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFE2E8F0),
                    borderColor = Color.White,
                    cornerRadius = 24.dp,
                    depth = 4.dp,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .shadow(6.dp, CircleShape, ambientColor = TakeItOrange, spotColor = TakeItRed)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(listOf(TakeItGold, TakeItOrange, TakeItRed)))
                                        .padding(2.dp)
                                ) {
                                    Image(
                                        painter = painterResource(R.drawable.logo_takeit_food),
                                        contentDescription = "Take It Logo",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(CircleShape)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Rounded.LocationOn, null, tint = TakeItRed, modifier = Modifier.size(15.dp))
                                        Spacer(Modifier.width(2.dp))
                                        Text(
                                            text = uiState.profile.activeAddressLabel.ifBlank { uiState.config.cityName },
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = TakeItInk,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Icon(Icons.Rounded.KeyboardArrowDown, null, tint = TakeItInk, modifier = Modifier.size(16.dp))
                                    }
                                    Text(
                                        text = uiState.profile.activeAddressLine.ifBlank { "Tap to autofetch or set address" },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TakeItMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                TakeIt3DIconOrb(
                                    icon = Icons.Rounded.LocalOffer,
                                    containerColor = TakeItGold,
                                    depthColor = TakeItGold.darken3D(0.35f),
                                    iconTint = TakeItInk,
                                    size = 38.dp,
                                    contentDescription = "Offers",
                                    onClick = onOpenOffersTab
                                )
                                Box {
                                    TakeIt3DIconOrb(
                                        icon = Icons.Rounded.ShoppingBag,
                                        containerColor = TakeItRed,
                                        depthColor = TakeItRedDark.darken3D(0.35f),
                                        iconTint = Color.White,
                                        size = 38.dp,
                                        contentDescription = "Cart",
                                        onClick = onOpenCart,
                                        modifier = Modifier.testTag("home_cart_button")
                                    )
                                    if (uiState.cartItemCount > 0) {
                                        Surface(
                                            shape = CircleShape,
                                            color = TakeItGold,
                                            border = BorderStroke(1.5.dp, Color.White),
                                            modifier = Modifier.align(Alignment.TopEnd)
                                        ) {
                                            Text(
                                                "${uiState.cartItemCount}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TakeItInk,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Compact 42dp Recessed Search Input (Saves vertical viewport!)
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TakeItBg,
                            border = BorderStroke(1.dp, Color(0xFFDDE3EC)),
                            shadowElevation = 2.dp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, "Search", tint = TakeItRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (uiState.searchQuery.isEmpty()) {
                                        Text(
                                            "Search restaurants, pizza, biryani...",
                                            color = TakeItMuted,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.searchQuery,
                                        onValueChange = onSearchChange,
                                        singleLine = true,
                                        textStyle = TextStyle(color = TakeItInk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                        cursorBrush = SolidColor(TakeItRed),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("home_search_input")
                                    )
                                }
                                if (uiState.searchQuery.isNotBlank()) {
                                    IconButton(onClick = { onSearchChange("") }, modifier = Modifier.size(28.dp)) {
                                        Icon(Icons.Rounded.Close, "Clear", tint = TakeItMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Admin-Controlled 3-Second Auto-Rotating Hero Carousel
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TakeIt3DHeroBanner(
                        borderColor = TakeItOrange.copy(alpha = 0.65f),
                        depthColor = TakeItRedDark.darken3D(0.45f),
                        cornerRadius = 24.dp,
                        depth = 5.dp,
                        tiltAmplitude = 1.8f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            val slide = bannerSlides[page]
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clickable {
                                        if (slide.restaurantId.isNotBlank()) {
                                            onOpenRestaurantDetail(slide.restaurantId)
                                        } else {
                                            onOpenRestaurants("All")
                                        }
                                    }
                            ) {
                                when (slide.imageUrl) {
                                    "builtin://welcome" -> Image(
                                        painter = painterResource(R.drawable.welcome_food_delivery),
                                        contentDescription = slide.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    "builtin://feast" -> Image(
                                        painter = painterResource(R.drawable.img_food_feast_1795421416008),
                                        contentDescription = slide.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    "builtin://pizza", "" -> Image(
                                        painter = painterResource(R.drawable.img_takeit_pizza_offer_1795427326877),
                                        contentDescription = slide.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    else -> FoodPhoto(
                                        imageUrl = slide.imageUrl,
                                        fallbackKey = slide.title,
                                        contentDescription = slide.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }
                                // Smooth gradient overlay so uploaded or bundled images look seamlessly integrated
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            Brush.horizontalGradient(
                                                listOf(Color.Black.copy(alpha = 0.72f), Color.Black.copy(alpha = 0.28f), Color.Transparent)
                                            )
                                        )
                                )
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .fillMaxWidth(0.75f)
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    if (slide.promoCode.isNotBlank()) {
                                        Surface(shape = RoundedCornerShape(50), color = TakeItGold) {
                                            Text(
                                                "CODE: ${slide.promoCode}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = TakeItInk,
                                                fontWeight = FontWeight.ExtraBold,
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = slide.title.ifBlank { "Special Deal" },
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (slide.subtitle.isNotBlank()) {
                                        Text(
                                            text = slide.subtitle,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = TakeItGoldSoft,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(50), color = Color.White) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Order Now", style = MaterialTheme.typography.labelSmall, color = TakeItRed, fontWeight = FontWeight.ExtraBold)
                                            Spacer(Modifier.width(4.dp))
                                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, tint = TakeItRed, modifier = Modifier.size(13.dp))
                                        }
                                    }
                                }
                            }
                        }

                        if (bannerSlides.size > 1) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                bannerSlides.indices.forEach { idx ->
                                    val active = idx == pagerState.currentPage
                                    Box(
                                        modifier = Modifier
                                            .height(6.dp)
                                            .width(if (active) 18.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(if (active) TakeItGold else Color.White.copy(alpha = 0.55f))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Live Announcement Pill (compact)
            if (uiState.config.announcementText.isNotBlank()) {
                item {
                    TakeIt3DCard(
                        containerColor = TakeItGoldSoft,
                        depthColor = TakeItGold.darken3D(0.32f),
                        borderColor = TakeItGold,
                        cornerRadius = 14.dp,
                        depth = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Rounded.Campaign, null, tint = TakeItRed, modifier = Modifier.size(18.dp))
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

            // Live Active Order Banner
            if (activeOrder != null) {
                item {
                    TakeIt3DCard(
                        containerColor = TakeItGreen,
                        depthColor = TakeItGreen.darken3D(0.42f),
                        borderColor = TakeItGreen.lighten3D(0.35f),
                        cornerRadius = 18.dp,
                        depth = 4.dp,
                        onClick = { onOpenTrackOrder(activeOrder.id) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("active_order_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                TakeIt3DIconOrb(
                                    icon = Icons.Rounded.DeliveryDining,
                                    containerColor = Color.White,
                                    depthColor = Color(0xFFDCFCE7),
                                    iconTint = TakeItGreen,
                                    size = 38.dp
                                )
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
                            Surface(shape = RoundedCornerShape(50), color = Color.White, shadowElevation = 4.dp) {
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

            // Strictly Single-Line Dynamic 3D Category Strip (LazyRow)
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "What's on your mind?",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TakeItInk,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(dynamicCategories, key = { it.name }) { cat ->
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .width(66.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { onOpenRestaurants(cat.name) }
                                    .padding(vertical = 2.dp)
                            ) {
                                Box(modifier = Modifier.size(56.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .offset(y = 3.dp)
                                            .shadow(5.dp, CircleShape, clip = false)
                                            .clip(CircleShape)
                                            .background(TakeItOrange.darken3D(0.2f))
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(54.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                            .border(2.dp, Color.White, CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (cat.imageUrl.startsWith("https://")) {
                                            FoodPhoto(
                                                imageUrl = cat.imageUrl,
                                                fallbackKey = cat.name,
                                                contentDescription = cat.name,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Image(
                                                painter = painterResource(cat.fallbackDrawable),
                                                contentDescription = cat.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TakeItInk,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
                    TextButton(
                        onClick = onOpenRestaurantTab,
                        modifier = Modifier.testTag("home_top_restaurants_see_all")
                    ) {
                        Text("See All →", color = TakeItRed, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (filteredRestaurants.isEmpty()) {
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFDCE1EA),
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
                // Horizontal 3D Compact Restaurants
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

                item {
                    Text(
                        "All Restaurants Near You",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TakeItInk,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // Vertical Full-Width 3D Restaurant Cards
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
}

@Composable
private fun CompactRestaurantCard(
    restaurant: Restaurant,
    onClick: () -> Unit
) {
    TakeIt3DCard(
        containerColor = Color.White,
        depthColor = Color(0xFFD5DCE6),
        cornerRadius = 20.dp,
        depth = 5.dp,
        shadowElevation = 8.dp,
        onClick = onClick,
        modifier = Modifier.width(180.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(114.dp)
            ) {
                FoodPhoto(
                    imageUrl = restaurant.imageUrl,
                    fallbackKey = "${restaurant.category} ${restaurant.name}",
                    contentDescription = restaurant.name,
                    modifier = Modifier.fillMaxSize()
                )
                if (restaurant.offerTag.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 12.dp),
                        color = TakeItRed,
                        shadowElevation = 4.dp,
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
            Column(modifier = Modifier.padding(11.dp)) {
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
    TakeIt3DCard(
        containerColor = Color.White,
        depthColor = Color(0xFFD5DCE6),
        cornerRadius = 24.dp,
        depth = 6.dp,
        shadowElevation = 10.dp,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .testTag("restaurant_card_${restaurant.id}")
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(176.dp)
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
                // 3D Favorite Orb
                TakeIt3DIconOrb(
                    icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    containerColor = Color.White.copy(alpha = 0.95f),
                    depthColor = Color(0xFFD1D5DB),
                    iconTint = if (isFavorite) TakeItRed else TakeItInk,
                    size = 38.dp,
                    contentDescription = "Favorite",
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                )
                if (restaurant.offerTag.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = TakeItGreen,
                        shadowElevation = 4.dp,
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
            Column(modifier = Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
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
                    Surface(shape = RoundedCornerShape(9.dp), color = TakeItGreen, shadowElevation = 2.dp) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
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

    val chipCategories = remember(uiState.categories) {
        val dyn = uiState.categories.filter { it.activeFlag }.sortedBy { it.sortOrder }.map { it.name }
        if (dyn.isNotEmpty()) listOf("All") + dyn else TakeItFoodCategories.map { it.name }
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
        TakeIt3DBackdrop(TakeItRed, TakeItOrange, baseColor = TakeItBg) {
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
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color.White
                        ),
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
                        items(chipCategories) { cat ->
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

    TakeIt3DBackdrop(
        accentPrimary = TakeItRed,
        accentSecondary = TakeItGold,
        baseColor = TakeItBg
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 116.dp)
        ) {
            // Full-bleed Hero + 3D Overlapping Restaurant Sheet
            item {
                Box(modifier = Modifier.fillMaxWidth()) {
                    FoodPhoto(
                        imageUrl = restaurant.imageUrl,
                        fallbackKey = "${restaurant.category} ${restaurant.name}",
                        contentDescription = restaurant.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(235.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(235.dp)
                            .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Color.Black.copy(alpha = 0.3f))))
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TakeIt3DIconOrb(
                            icon = Icons.AutoMirrored.Rounded.ArrowBack,
                            containerColor = Color.White,
                            depthColor = Color(0xFFD1D5DB),
                            iconTint = TakeItInk,
                            size = 44.dp,
                            contentDescription = "Back",
                            onClick = onBack
                        )
                        TakeIt3DIconOrb(
                            icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            containerColor = Color.White,
                            depthColor = Color(0xFFD1D5DB),
                            iconTint = if (isFav) TakeItRed else TakeItInk,
                            size = 44.dp,
                            contentDescription = "Favorite",
                            onClick = { onToggleFavorite(restaurant.id) }
                        )
                    }

                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFDCE2EB),
                        cornerRadius = 28.dp,
                        depth = 5.dp,
                        shadowElevation = 12.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 198.dp, start = 12.dp, end = 12.dp)
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

                            if (restaurant.offerTag.isNotBlank() && selectedSectionTab == 0) {
                                TakeIt3DCard(
                                    containerColor = TakeItGoldSoft,
                                    depthColor = TakeItGold.darken3D(0.3f),
                                    borderColor = TakeItGold,
                                    cornerRadius = 16.dp,
                                    depth = 3.dp,
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
                                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                            TakeIt3DCard(
                                containerColor = Color.White,
                                depthColor = Color(0xFFDCE2EB),
                                cornerRadius = 18.dp,
                                depth = 4.dp,
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
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFDCE2EB),
                            cornerRadius = 20.dp,
                            depth = 4.dp,
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

        // Sticky Floating 3D Cart Bar
        if (uiState.cartItemCount > 0) {
            TakeIt3DButton(
                onClick = onOpenCart,
                containerColor = TakeItRed,
                depthColor = TakeItRedDark.darken3D(0.4f),
                contentColor = Color.White,
                cornerRadius = 20.dp,
                depth = 5.dp,
                height = 60.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(16.dp)
                    .fillMaxWidth()
                    .testTag("floating_view_cart_bar")
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
    TakeIt3DCard(
        containerColor = Color.White,
        depthColor = Color(0xFFD5DCE6),
        cornerRadius = 20.dp,
        depth = 4.dp,
        shadowElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .shadow(6.dp, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
            ) {
                FoodPhoto(
                    imageUrl = dish.imageUrl,
                    fallbackKey = dish.name,
                    contentDescription = dish.name,
                    modifier = Modifier.fillMaxSize()
                )
            }
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
                TakeIt3DButton(
                    onClick = onAdd,
                    containerColor = TakeItRed,
                    depthColor = TakeItRedDark.darken3D(0.4f),
                    contentColor = Color.White,
                    cornerRadius = 14.dp,
                    depth = 4.dp,
                    height = 42.dp,
                    modifier = Modifier
                        .width(90.dp)
                        .testTag("add_dish_${dish.id}")
                ) {
                    Text("Add +", fontWeight = FontWeight.ExtraBold)
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = TakeItRed,
                    shadowElevation = 4.dp
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

@Composable
fun CustomerExploreTab(
    uiState: TakeItUiState,
    onOpenRestaurantDetail: (String) -> Unit,
    onToggleFavorite: (String) -> Unit
) {
    var selectedCategory by remember { mutableStateOf("All") }
    var pureVegOnly by remember { mutableStateOf(false) }
    var highRatingOnly by remember { mutableStateOf(false) }
    var offersOnly by remember { mutableStateOf(false) }
    var search by remember { mutableStateOf("") }

    val chipCategories = remember(uiState.categories) {
        val dyn = uiState.categories.filter { it.activeFlag }.sortedBy { it.sortOrder }.map { it.name }
        if (dyn.isNotEmpty()) listOf("All") + dyn else TakeItFoodCategories.map { it.name }
    }

    val filtered = remember(uiState.restaurants, selectedCategory, pureVegOnly, highRatingOnly, offersOnly, search) {
        uiState.restaurants.filter { r ->
            val catOk = selectedCategory == "All" || r.category.equals(selectedCategory, ignoreCase = true) || r.cuisine.contains(selectedCategory, ignoreCase = true)
            val vegOk = !pureVegOnly || r.pureVegFlag
            val ratOk = !highRatingOnly || r.rating >= 4.4
            val offOk = !offersOnly || r.offerTag.isNotBlank() || r.offerImageUrl.isNotBlank()
            val qOk = search.isBlank() || r.name.contains(search, ignoreCase = true) || r.cuisine.contains(search, ignoreCase = true)
            catOk && vegOk && ratOk && offOk && qOk
        }
    }

    TakeIt3DBackdrop(TakeItRed, TakeItOrange, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 115.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFDCE2EB),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            TakeIt3DIconOrb(icon = Icons.Rounded.Explore, containerColor = TakeItRed, size = 36.dp)
                            Column {
                                Text("Explore Kitchens & Dishes", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Text("Filter by Veg, 4.4+ ratings, offers & categories", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = TakeItBg,
                            border = BorderStroke(1.dp, Color(0xFFDDE3EC)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(42.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Search, "Search Explore", tint = TakeItRed, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                                    if (search.isEmpty()) {
                                        Text("Search kitchens, cuisines...", color = TakeItMuted, fontSize = 13.sp)
                                    }
                                    BasicTextField(
                                        value = search,
                                        onValueChange = { search = it },
                                        singleLine = true,
                                        textStyle = TextStyle(color = TakeItInk, fontSize = 13.sp, fontWeight = FontWeight.Medium),
                                        cursorBrush = SolidColor(TakeItRed),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (search.isNotBlank()) {
                                    IconButton(onClick = { search = "" }, modifier = Modifier.size(26.dp)) {
                                        Icon(Icons.Rounded.Close, "Clear", tint = TakeItMuted, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
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
                    items(chipCategories) { cat ->
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
                items(filtered, key = { "exp_${it.id}" }) { r ->
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
}
