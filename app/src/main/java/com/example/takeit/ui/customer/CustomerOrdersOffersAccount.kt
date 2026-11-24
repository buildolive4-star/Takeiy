package com.example.takeit.ui.customer

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.network.TakeItPaymentBus
import com.example.network.TakeItPaymentGateway
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.components.LeafletAddressPickerMap
import com.example.takeit.ui.components.SupportChannelsCard
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.autofetchCurrentAddress
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.ui.components.reverseGeocodeCoordinates
import com.example.takeit.viewmodel.TakeItUiState
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerMyOrdersScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onOpenTrack: (String) -> Unit,
    onOpenDelivered: (String) -> Unit
) {
    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("My Orders", fontWeight = FontWeight.ExtraBold)
                        Surface(shape = RoundedCornerShape(50), color = TakeItGoldSoft, border = BorderStroke(1.dp, TakeItGold)) {
                            Text(
                                "${uiState.myOrders.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = TakeItRedDark,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("my_orders_back_btn")) {
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (uiState.myOrders.isEmpty()) {
                    item {
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFD5DCE6),
                            cornerRadius = 24.dp,
                            depth = 5.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(36.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                TakeIt3DIconOrb(
                                    icon = Icons.Rounded.ReceiptLong,
                                    containerColor = TakeItRed,
                                    size = 64.dp
                                )
                                Text("No orders placed yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text("Order your favorite meal and track it live here!", color = TakeItMuted, textAlign = TextAlign.Center)
                            }
                        }
                    }
                } else {
                    items(uiState.myOrders, key = { it.id }) { order ->
                        val statusColor = when (order.status) {
                            "DELIVERED" -> TakeItGreen
                            "CANCELLED", "REFUNDED" -> TakeItRed
                            else -> TakeItOrange
                        }
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFD5DCE6),
                            cornerRadius = 20.dp,
                            depth = 5.dp,
                            onClick = {
                                if (order.status == "DELIVERED") onOpenDelivered(order.id) else onOpenTrack(order.id)
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(order.restaurantName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                    Surface(shape = RoundedCornerShape(50), color = statusColor.copy(alpha = 0.14f)) {
                                        Text(
                                            order.status.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = statusColor,
                                            fontWeight = FontWeight.ExtraBold,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Text(order.itemsSummary, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("${order.orderNumber} • ₹${order.totalAmount} (${order.paymentMethod})", fontWeight = FontWeight.Bold, color = TakeItInk)
                                    Text(
                                        if (order.status == "DELIVERED") "Rate / View →" else "Live Track →",
                                        color = TakeItRed,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.labelMedium
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

private data class OrderedDishTile(
    val restaurantId: String,
    val restaurantName: String,
    val dishName: String,
    val imageUrl: String,
    val price: Int?,
    val isVeg: Boolean
)

@Composable
fun CustomerHistoryTab(
    uiState: TakeItUiState,
    onOpenRestaurantDetail: (String) -> Unit,
    onExploreFood: () -> Unit
) {
    val dishTiles = remember(uiState.myOrders, uiState.menuItems) {
        val seen = linkedSetOf<String>()
        val result = mutableListOf<OrderedDishTile>()
        val qtyPrefix = Regex("^\\d+x\\s*", RegexOption.IGNORE_CASE)

        uiState.myOrders.forEach { order ->
            val rawParts = order.itemsSummary.split(",")
                .map { it.trim().replace(qtyPrefix, "").trim() }
                .filter { it.isNotBlank() }

            rawParts.forEach { dishName ->
                val dedupKey = "${order.restaurantId}|${dishName.lowercase()}"
                if (seen.add(dedupKey)) {
                    val matched = uiState.menuItems.firstOrNull { m ->
                        (order.restaurantId.isBlank() || m.restaurantId == order.restaurantId) &&
                            m.name.equals(dishName, ignoreCase = true)
                    } ?: uiState.menuItems.firstOrNull { m ->
                        m.name.equals(dishName, ignoreCase = true)
                    }
                    result += OrderedDishTile(
                        restaurantId = matched?.restaurantId?.ifBlank { order.restaurantId } ?: order.restaurantId,
                        restaurantName = order.restaurantName,
                        dishName = dishName,
                        imageUrl = matched?.imageUrl.orEmpty(),
                        price = matched?.price,
                        isVeg = matched?.vegFlag ?: true
                    )
                }
            }
        }
        result
    }

    TakeIt3DBackdrop(TakeItRed, TakeItOrange, baseColor = TakeItBg) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 0.dp, bottom = 120.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(3) }) {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFDCE2EB),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            TakeIt3DIconOrb(icon = Icons.Rounded.History, containerColor = TakeItRed, size = 38.dp)
                            Column {
                                Text("Dish History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                                Text("Tap any past dish to reorder", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                        }
                        Surface(shape = RoundedCornerShape(50), color = TakeItGoldSoft, border = BorderStroke(1.dp, TakeItGold)) {
                            Text(
                                "${dishTiles.size} Dishes",
                                style = MaterialTheme.typography.labelMedium,
                                color = TakeItRedDark,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            if (dishTiles.isEmpty()) {
                item(span = { GridItemSpan(3) }) {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 24.dp,
                        depth = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TakeIt3DIconOrb(icon = Icons.Rounded.Fastfood, containerColor = TakeItOrange, size = 62.dp)
                            Text("No Dish History Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                            Text(
                                "Dishes from every meal you order appear here in a quick 1:1 grid for instant reordering.",
                                color = TakeItMuted,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                            TakeIt3DButton(
                                onClick = onExploreFood,
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                height = 46.dp,
                                modifier = Modifier.width(180.dp)
                            ) {
                                Text("Explore Dishes", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            } else {
                itemsIndexed(dishTiles, key = { index, t -> "${index}_${t.restaurantId}_${t.dishName}" }) { index, tile ->
                    TakeIt3DCard(
                        onClick = {
                            if (tile.restaurantId.isNotBlank()) {
                                onOpenRestaurantDetail(tile.restaurantId)
                            } else {
                                onExploreFood()
                            }
                        },
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 16.dp,
                        depth = 4.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .testTag("history_dish_card_$index")
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            FoodPhoto(
                                imageUrl = tile.imageUrl,
                                fallbackKey = tile.dishName,
                                contentDescription = tile.dishName,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )
                            VegMarkIcon(
                                isVeg = tile.isVeg,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                                    .background(Color.White, RoundedCornerShape(4.dp))
                            )
                            if (tile.price != null) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = TakeItGold,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(6.dp)
                                ) {
                                    Text(
                                        "₹${tile.price}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TakeItInk,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 10.sp,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                Text(
                                    tile.dishName,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    tile.restaurantName,
                                    color = TakeItGoldSoft,
                                    fontSize = 9.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerOffersTab(
    uiState: TakeItUiState,
    onOpenRestaurantDetail: (String) -> Unit,
    onApplyCouponAndOpenCart: (PromoCoupon) -> Unit
) {
    val activeCoupons = uiState.coupons.filter { it.activeFlag }
    val offerRestaurants = remember(uiState.restaurants) {
        uiState.restaurants.filter { it.offerImageUrl.isNotBlank() || it.offerTag.isNotBlank() }
    }

    TakeIt3DBackdrop(TakeItGold, TakeItRed, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TakeIt3DIconOrb(
                            icon = Icons.Rounded.LocalOffer,
                            containerColor = TakeItRed,
                            size = 38.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Live Offers & Coupons", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                            Text("Tap any 16:9 restaurant deal poster to order", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                        }
                    }
                }
            }

            if (offerRestaurants.isNotEmpty()) {
                item {
                    Text(
                        "Restaurant Offers & Deals",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TakeItInk,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
                items(offerRestaurants, key = { "offer_rest_${it.id}" }) { r ->
                    TakeIt3DCard(
                        onClick = { onOpenRestaurantDetail(r.id) },
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        borderColor = TakeItOrange.copy(alpha = 0.5f),
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        shadowElevation = 10.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag("offer_restaurant_card_${r.id}")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                        ) {
                            FoodPhoto(
                                imageUrl = r.offerImageUrl.ifBlank { r.imageUrl },
                                fallbackKey = "${r.category} ${r.name}",
                                contentDescription = "${r.name} offer",
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                Color.Transparent,
                                                Color.Black.copy(alpha = 0.25f),
                                                Color.Black.copy(alpha = 0.85f)
                                            )
                                        )
                                    )
                            )
                            if (r.offerTag.isNotBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = TakeItRed,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(12.dp)
                                ) {
                                    Text(
                                        r.offerTag,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        r.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${r.category} • ${r.deliveryTime}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TakeItGoldSoft
                                    )
                                }
                                Surface(shape = RoundedCornerShape(50), color = Color.White) {
                                    Text(
                                        "View Menu →",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TakeItRed,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Platform Promo Coupons",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = TakeItInk,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            if (activeCoupons.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No active promo codes right now. Check back soon!", color = TakeItMuted)
                    }
                }
            } else {
                items(activeCoupons, key = { it.id }) { c ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        borderColor = TakeItGold.copy(alpha = 0.5f),
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Surface(shape = RoundedCornerShape(8.dp), color = TakeItGoldSoft, border = BorderStroke(1.dp, TakeItGold)) {
                                    Text(
                                        c.code,
                                        color = TakeItRedDark,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                                Text(c.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                                Text(c.description, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                                Text("Min order ₹${c.minOrderAmount}", style = MaterialTheme.typography.labelSmall, color = TakeItGreen, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(10.dp))
                            TakeIt3DButton(
                                onClick = { onApplyCouponAndOpenCart(c) },
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                cornerRadius = 20.dp,
                                depth = 4.dp,
                                height = 44.dp,
                                modifier = Modifier.width(98.dp)
                            ) {
                                Text("Apply", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomerAccountTab(
    uiState: TakeItUiState,
    onUpdateProfile: (name: String, phone: String) -> Unit,
    onOpenAddresses: () -> Unit,
    onOpenWallet: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenAbout: () -> Unit,
    onOpenMyOrders: () -> Unit,
    onEnterAdminCommandCenter: () -> Unit,
    onEnterDeliveryPartner: () -> Unit,
    onSignOut: () -> Unit
) {
    var showEditDialog by remember { mutableStateOf(false) }

    TakeIt3DBackdrop(TakeItRed, TakeItGold, baseColor = TakeItBg) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 3D Crimson-Orange Profile Slab
            item {
                TakeIt3DCard(
                    containerColor = TakeItRed,
                    depthColor = TakeItRedDark.darken3D(0.45f),
                    borderColor = TakeItOrange.lighten3D(0.35f),
                    cornerRadius = 28.dp,
                    depth = 6.dp,
                    shadowElevation = 14.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(TakeItRedDark, TakeItRed, TakeItOrange)))
                            .statusBarsPadding()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .shadow(10.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(3.dp, TakeItGold, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    uiState.profile.name.ifBlank { uiState.displayName }.take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = TakeItRed,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    uiState.profile.name.ifBlank { uiState.displayName },
                                    style = MaterialTheme.typography.titleLarge,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                                Text(
                                    uiState.profile.phone.ifBlank { uiState.email },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                val badgeText = when {
                                    uiState.isMainAdmin -> "👑 Main Admin Account"
                                    uiState.adminRole == AdminRole.CO_ADMIN -> "🛡️ Co-Admin Staff"
                                    uiState.isRestaurantOwner -> "🍽️ Restaurant Partner"
                                    uiState.isDeliveryPartner -> "🛵 Delivery Partner"
                                    else -> "👑 Gold Member"
                                }
                                Surface(shape = RoundedCornerShape(50), color = TakeItGold, shadowElevation = 3.dp) {
                                    Text(
                                        badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TakeItInk,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                    )
                                }
                            }
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.Edit,
                                containerColor = Color.White.copy(alpha = 0.22f),
                                depthColor = TakeItRedDark,
                                iconTint = Color.White,
                                size = 40.dp,
                                contentDescription = "Edit Profile",
                                onClick = { showEditDialog = true }
                            )
                        }
                    }
                }
            }

            // My Orders Card (Moved inside Profile section as requested!)
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("My Orders", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        TextButton(
                            onClick = onOpenMyOrders,
                            modifier = Modifier.testTag("profile_view_all_orders_btn")
                        ) {
                            Text("View All (${uiState.myOrders.size}) →", color = TakeItRed, fontWeight = FontWeight.Bold)
                        }
                    }
                    TakeIt3DCard(
                        onClick = onOpenMyOrders,
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("profile_my_orders_card")
                    ) {
                        val latest = uiState.myOrders.firstOrNull()
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                TakeIt3DIconOrb(icon = Icons.Rounded.ReceiptLong, containerColor = TakeItRed, size = 42.dp)
                                Column {
                                    Text(
                                        if (latest != null) "${latest.restaurantName} • ${latest.status.replace("_", " ")}" else "Track & Manage Your Orders",
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TakeItInk,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        if (latest != null) "${latest.orderNumber} • ₹${latest.totalAmount}" else "Tap to view live order tracking & receipts",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TakeItMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Open Orders", tint = TakeItRed)
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        val itemsToShow = uiState.restaurants.take(5)
                        items(itemsToShow, key = { "strip_${it.id}" }) { r ->
                            Box(
                                modifier = Modifier
                                    .size(70.dp)
                                    .shadow(5.dp, RoundedCornerShape(16.dp))
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.5.dp, Color.White, RoundedCornerShape(16.dp))
                            ) {
                                FoodPhoto(
                                    imageUrl = r.imageUrl,
                                    fallbackKey = r.name,
                                    contentDescription = r.name,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            }

            // 3D Admin / Restaurant Owner Portal Card
            if (uiState.isMainAdmin || uiState.adminRole == AdminRole.CO_ADMIN || uiState.isRestaurantOwner) {
                item {
                    TakeIt3DCard(
                        containerColor = AdminBg,
                        depthColor = Color.Black,
                        borderColor = AdminGold,
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        onClick = onEnterAdminCommandCenter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("open_admin_command_center_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                TakeIt3DIconOrb(
                                    icon = if (uiState.isRestaurantOwner) Icons.Rounded.Storefront else Icons.Rounded.AdminPanelSettings,
                                    containerColor = AdminGold,
                                    iconTint = AdminBg,
                                    size = 44.dp
                                )
                                Column {
                                    Text(
                                        when {
                                            uiState.isMainAdmin -> "Open Main Admin Command Center"
                                            uiState.isRestaurantOwner -> "Open Restaurant Partner Panel"
                                            else -> "Open Co-Admin Control Panel"
                                        },
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        when {
                                            uiState.isMainAdmin -> "Full control + Roles, Categories & Carousel"
                                            uiState.isRestaurantOwner -> "Upload dishes, 16:9 offers & dispatch orders"
                                            else -> "Login with Admin passcode"
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AdminGold
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Open Admin", tint = AdminGold)
                        }
                    }
                }
            }

            // 3D Delivery Partner Portal Card
            if (uiState.isMainAdmin || uiState.isDeliveryPartner) {
                item {
                    TakeIt3DCard(
                        containerColor = DeliveryBg,
                        depthColor = Color.Black,
                        borderColor = DeliveryEmerald,
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        onClick = onEnterDeliveryPartner,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .testTag("open_delivery_partner_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
                                TakeIt3DIconOrb(
                                    icon = Icons.Rounded.DeliveryDining,
                                    containerColor = DeliveryEmerald,
                                    iconTint = DeliveryBg,
                                    size = 44.dp
                                )
                                Column {
                                    Text(
                                        "Open Captain Panel",
                                        color = Color.White,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    Text(
                                        "Accept Food, RideX & MoveX Captain jobs with live Leaflet GPS & OTP",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DeliveryEmerald
                                    )
                                }
                            }
                            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, "Open Delivery Panel", tint = DeliveryEmerald)
                        }
                    }
                }
            }

            // 3D Account Menu Card
            item {
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 22.dp,
                    depth = 5.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Column {
                        AccountMenuRow(Icons.Rounded.ReceiptLong, "My Orders", "${uiState.myOrders.size} orders", onOpenMyOrders)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.LocationOn, "My Addresses", "${uiState.addresses.size} saved", onOpenAddresses)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.AccountBalanceWallet, "Wallet & Offers", "₹${uiState.profile.walletBalance.toInt()}", onOpenWallet)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.SupportAgent, "Help & Support", "${uiState.myTickets.size} tickets", onOpenSupport)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.Info, "About Take It", "v3.0", onOpenAbout)
                        HorizontalDivider(color = TakeItBorder)
                        AccountMenuRow(Icons.Rounded.SwitchAccount, "Switch Google Account / Sign Out", "", onSignOut, tint = TakeItRed)
                    }
                }
            }

            // Live 24x7 Support Channels Card
            item {
                SupportChannelsCard(
                    config = uiState.config,
                    accentColor = TakeItRed,
                    onRaiseTicket = onOpenSupport,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }

    if (showEditDialog) {
        var nameInput by remember { mutableStateOf(uiState.profile.name.ifBlank { uiState.displayName }) }
        var phoneInput by remember { mutableStateOf(uiState.profile.phone) }
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text("Edit Profile", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = nameInput, onValueChange = { nameInput = it }, label = { Text("Full Name") }, singleLine = true)
                    OutlinedTextField(value = phoneInput, onValueChange = { phoneInput = it }, label = { Text("Phone Number") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateProfile(nameInput, phoneInput)
                        showEditDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TakeItRed)
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showEditDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun AccountMenuRow(
    icon: ImageVector,
    title: String,
    badge: String,
    onClick: () -> Unit,
    tint: Color = TakeItInk
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TakeIt3DIconOrb(
                icon = icon,
                containerColor = if (tint == TakeItRed) TakeItRed else TakeItOrange,
                size = 34.dp
            )
            Text(title, fontWeight = FontWeight.Bold, color = tint)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (badge.isNotBlank()) {
                Surface(shape = RoundedCornerShape(50), color = TakeItGoldSoft) {
                    Text(badge, style = MaterialTheme.typography.labelSmall, color = TakeItRedDark, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                }
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, null, tint = TakeItMuted)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAddressesScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onSaveAddress: (SavedAddress) -> Unit,
    onSelectAddress: (SavedAddress) -> Unit,
    onDeleteAddress: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var label by remember { mutableStateOf("Home") }
    var city by remember { mutableStateOf(uiState.config.cityName.ifBlank { "Greater Noida" }) }
    var areaStreet by remember { mutableStateOf("") }
    var fullAddress by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }
    var mapLink by remember { mutableStateOf("") }
    var forSomeoneElse by remember { mutableStateOf(false) }
    var receiverName by remember { mutableStateOf("") }
    var receiverPhone by remember { mutableStateOf("") }
    var selectedLat by remember { mutableStateOf(uiState.activeLatitude.takeIf { it != 0.0 } ?: 28.4744) }
    var selectedLng by remember { mutableStateOf(uiState.activeLongitude.takeIf { it != 0.0 } ?: 77.5040) }
    var isFetchingGps by remember { mutableStateOf(false) }
    var gpsStatus by remember { mutableStateOf<String?>(null) }

    fun triggerAutofetch() {
        isFetchingGps = true
        gpsStatus = "Fetching current GPS coordinates..."
        autofetchCurrentAddress(
            context = context,
            scope = scope,
            onSuccess = { lat, lng, detectedCity, detectedArea, detectedFull, link ->
                isFetchingGps = false
                selectedLat = lat
                selectedLng = lng
                if (detectedCity.isNotBlank()) city = detectedCity
                if (detectedArea.isNotBlank()) areaStreet = detectedArea
                if (detectedFull.isNotBlank()) fullAddress = detectedFull
                mapLink = link
                gpsStatus = "Pinned exact GPS (${"%.4f".format(Locale.US, lat)}, ${"%.4f".format(Locale.US, lng)})"
            },
            onError = { err ->
                isFetchingGps = false
                gpsStatus = err
            }
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val granted = perms[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            perms[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            triggerAutofetch()
        } else {
            gpsStatus = "Location permission denied. Tap anywhere on the map below to pin your doorstep."
        }
    }

    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("Delivery Location & Address", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        TakeIt3DBackdrop(TakeItRed, TakeItGold, baseColor = TakeItBg) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Interactive Leaflet OSM Map Picker + GPS Autofetch
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        borderColor = TakeItOrange.copy(alpha = 0.45f),
                        cornerRadius = 22.dp,
                        depth = 5.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Pin Exact Doorstep (Leaflet OSM)", fontWeight = FontWeight.ExtraBold, color = TakeItInk)
                                Text(
                                    "${"%.4f".format(Locale.US, selectedLat)}, ${"%.4f".format(Locale.US, selectedLng)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TakeItMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(185.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, TakeItBorder, RoundedCornerShape(16.dp))
                            ) {
                                LeafletAddressPickerMap(
                                    selectedLat = selectedLat,
                                    selectedLng = selectedLng,
                                    onLocationPicked = { lat, lng ->
                                        selectedLat = lat
                                        selectedLng = lng
                                        reverseGeocodeCoordinates(context, scope, lat, lng) { _, _, c, area, full, link ->
                                            if (c.isNotBlank()) city = c
                                            if (area.isNotBlank()) areaStreet = area
                                            if (full.isNotBlank()) fullAddress = full
                                            mapLink = link
                                            gpsStatus = "Pin updated via map tap"
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize()
                                )
                            }

                            TakeIt3DButton(
                                onClick = {
                                    val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                                    if (fine || coarse) {
                                        triggerAutofetch()
                                    } else {
                                        locationPermissionLauncher.launch(
                                            arrayOf(
                                                Manifest.permission.ACCESS_FINE_LOCATION,
                                                Manifest.permission.ACCESS_COARSE_LOCATION
                                            )
                                        )
                                    }
                                },
                                enabled = !isFetchingGps,
                                containerColor = TakeItGreen,
                                depthColor = TakeItGreen.darken3D(0.4f),
                                cornerRadius = 14.dp,
                                depth = 4.dp,
                                height = 46.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("autofetch_location_btn")
                            ) {
                                if (isFetchingGps) {
                                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Detecting Current GPS...", fontWeight = FontWeight.ExtraBold)
                                } else {
                                    Icon(Icons.Rounded.MyLocation, "Autofetch", modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Autofetch Current Location (GPS)", fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            if (!gpsStatus.isNullOrBlank()) {
                                Text(gpsStatus!!, style = MaterialTheme.typography.labelSmall, color = TakeItGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Structured Address Form + Ordering For Toggle
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 22.dp,
                        depth = 5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Complete Delivery Details", fontWeight = FontWeight.ExtraBold, color = TakeItInk)

                            Text("Who is this order for?", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                                FilterChip(
                                    selected = !forSomeoneElse,
                                    onClick = { forSomeoneElse = false },
                                    leadingIcon = { Icon(Icons.Rounded.Person, null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("Myself", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                                FilterChip(
                                    selected = forSomeoneElse,
                                    onClick = { forSomeoneElse = true },
                                    leadingIcon = { Icon(Icons.Rounded.Group, null, modifier = Modifier.size(16.dp)) },
                                    label = { Text("Someone else", fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            if (forSomeoneElse) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = receiverName,
                                        onValueChange = { receiverName = it },
                                        label = { Text("Receiver Name *") },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    OutlinedTextField(
                                        value = receiverPhone,
                                        onValueChange = { receiverPhone = it },
                                        label = { Text("Receiver Phone *") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            Text("Save Address As", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Home", "Work", "Other").forEach { tag ->
                                    FilterChip(selected = label == tag, onClick = { label = tag }, label = { Text(tag) })
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = city,
                                    onValueChange = { city = it },
                                    label = { Text("City *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = areaStreet,
                                    onValueChange = { areaStreet = it },
                                    label = { Text("Area / Street *") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1.3f)
                                )
                            }
                            OutlinedTextField(
                                value = fullAddress,
                                onValueChange = { fullAddress = it },
                                label = { Text("Complete Address (House / Flat / Floor) *") },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = landmark,
                                onValueChange = { landmark = it },
                                label = { Text("Nearby Landmark (Optional)") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = mapLink,
                                onValueChange = { mapLink = it },
                                label = { Text("Map / Location Link") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            TakeIt3DButton(
                                onClick = {
                                    val fullValid = fullAddress.isNotBlank() && city.isNotBlank() && areaStreet.isNotBlank()
                                    val recValid = !forSomeoneElse || (receiverName.isNotBlank() && receiverPhone.isNotBlank())
                                    if (fullValid && recValid) {
                                        val structured = SavedAddress(
                                            label = label,
                                            fullAddress = fullAddress.trim(),
                                            landmark = landmark.trim(),
                                            city = city.trim(),
                                            areaStreet = areaStreet.trim(),
                                            mapLink = mapLink.trim(),
                                            receiverName = if (forSomeoneElse) receiverName.trim() else "",
                                            receiverPhone = if (forSomeoneElse) receiverPhone.trim() else "",
                                            forSomeoneElse = forSomeoneElse,
                                            latitude = selectedLat,
                                            longitude = selectedLng
                                        )
                                        onSaveAddress(structured)
                                        onSelectAddress(structured)
                                        areaStreet = ""
                                        fullAddress = ""
                                        landmark = ""
                                        receiverName = ""
                                        receiverPhone = ""
                                    }
                                },
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                height = 50.dp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("save_address_button")
                            ) { Text("Save & Deliver Here", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }

                if (uiState.addresses.isNotEmpty()) {
                    item {
                        Text("Saved Addresses", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = TakeItInk)
                    }
                    items(uiState.addresses, key = { it.id }) { addr ->
                        val isActive = uiState.profile.activeAddressLabel == addr.label &&
                            uiState.profile.activeAddressLine.contains(addr.fullAddress.take(12), ignoreCase = true)
                        TakeIt3DCard(
                            containerColor = Color.White,
                            depthColor = Color(0xFFD5DCE6),
                            borderColor = if (isActive) TakeItRed else Color.White,
                            cornerRadius = 18.dp,
                            depth = 4.dp,
                            onClick = { onSelectAddress(addr) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(addr.label, fontWeight = FontWeight.ExtraBold, color = TakeItRed)
                                        if (addr.forSomeoneElse && addr.receiverName.isNotBlank()) {
                                            Surface(shape = RoundedCornerShape(50), color = TakeItGoldSoft) {
                                                Text(
                                                    "For ${addr.receiverName}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TakeItRedDark,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        listOf(addr.fullAddress, addr.areaStreet, addr.city).filter { it.isNotBlank() }.distinct().joinToString(", "),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = TakeItInk
                                    )
                                    if (addr.landmark.isNotBlank()) {
                                        Text("Near ${addr.landmark}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                                    }
                                }
                                IconButton(onClick = { onDeleteAddress(addr.id) }) {
                                    Icon(Icons.Rounded.DeleteOutline, "Delete", tint = TakeItRed)
                                }
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
fun CustomerWalletScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onTopUpConfirmed: (Double, String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    var amountInput by remember { mutableStateOf("500") }
    var pendingRef by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        TakeItPaymentBus.results.collect { res ->
            if (res.orderRef == pendingRef && res.status.equals("success", ignoreCase = true)) {
                pendingRef = null
                onTopUpConfirmed(res.amount.toDouble(), "RAZORPAY", "Wallet Top-Up (${res.paymentId})")
            }
        }
    }

    DisposableEffect(lifecycleOwner, pendingRef) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && pendingRef != null) {
                val ref = pendingRef ?: return@LifecycleEventObserver
                scope.launch {
                    val res = TakeItPaymentGateway.checkPaymentStatus(ref)
                    if (res != null) {
                        pendingRef = null
                        onTopUpConfirmed(res.amount.toDouble(), "RAZORPAY", "Wallet Top-Up (${res.paymentId})")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("Take It Wallet", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        TakeIt3DBackdrop(TakeItOrange, TakeItRed, baseColor = TakeItBg) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    TakeIt3DCard(
                        containerColor = TakeItRed,
                        depthColor = TakeItRedDark.darken3D(0.45f),
                        borderColor = TakeItGold,
                        cornerRadius = 24.dp,
                        depth = 6.dp
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Brush.horizontalGradient(listOf(TakeItRedDark, TakeItOrange)))
                                .padding(22.dp)
                        ) {
                            Column {
                                Text("Available Wallet Balance", color = TakeItGoldSoft)
                                Text(
                                    "₹${uiState.profile.walletBalance.toInt()}",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Add Money via Razorpay (Chrome)", fontWeight = FontWeight.ExtraBold)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("200", "500", "1000").forEach { preset ->
                                    FilterChip(selected = amountInput == preset, onClick = { amountInput = preset }, label = { Text("₹$preset") })
                                }
                            }
                            OutlinedTextField(value = amountInput, onValueChange = { amountInput = it.filter(Char::isDigit) }, label = { Text("Amount (₹)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            TakeIt3DButton(
                                onClick = {
                                    val amt = amountInput.toIntOrNull() ?: 0
                                    if (amt < 10) {
                                        onShowMessage("Enter at least ₹10")
                                    } else {
                                        val ref = "TKW_${System.currentTimeMillis()}"
                                        pendingRef = ref
                                        TakeItPaymentGateway.launchChromePayment(
                                            context = context,
                                            orderRef = ref,
                                            amount = amt,
                                            serviceType = "WALLET_TOPUP",
                                            itemTitle = "Take It Wallet Top-Up",
                                            customerName = uiState.profile.name.ifBlank { uiState.displayName },
                                            customerPhone = uiState.profile.phone,
                                            userId = uiState.uid
                                        )
                                    }
                                },
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                height = 50.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Add ₹$amountInput via Razorpay", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
                item { Text("Transaction Ledger", fontWeight = FontWeight.ExtraBold) }
                items(uiState.walletTx, key = { it.id }) { tx ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 16.dp,
                        depth = 3.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.title, fontWeight = FontWeight.Bold)
                                Text(tx.method, style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            }
                            val isCredit = tx.txType != "DEBIT"
                            Text(
                                "${if (isCredit) "+" else "-"}₹${tx.amount.toInt()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isCredit) TakeItGreen else TakeItRed
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerSupportScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit,
    onSubmitTicket: (subject: String, message: String) -> Unit
) {
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }

    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("Help & Support", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SupportChannelsCard(
                        config = uiState.config,
                        accentColor = TakeItRed
                    )
                }
                item {
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 20.dp,
                        depth = 5.dp
                    ) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Raise Live Support Ticket", fontWeight = FontWeight.ExtraBold)
                            Text("Support Hotline: ${uiState.config.supportPhone}", style = MaterialTheme.typography.labelSmall, color = TakeItMuted)
                            OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("Issue Subject") }, modifier = Modifier.fillMaxWidth())
                            OutlinedTextField(value = message, onValueChange = { message = it }, label = { Text("Describe your issue...") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                            TakeIt3DButton(
                                onClick = {
                                    if (subject.isNotBlank() && message.isNotBlank()) {
                                        onSubmitTicket(subject, message)
                                        subject = ""
                                        message = ""
                                    }
                                },
                                containerColor = TakeItRed,
                                depthColor = TakeItRedDark.darken3D(0.4f),
                                height = 48.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Send to Take It Support", fontWeight = FontWeight.ExtraBold) }
                        }
                    }
                }
                items(uiState.myTickets, key = { it.id }) { t ->
                    TakeIt3DCard(
                        containerColor = Color.White,
                        depthColor = Color(0xFFD5DCE6),
                        cornerRadius = 18.dp,
                        depth = 4.dp
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(t.subject, fontWeight = FontWeight.ExtraBold)
                                Text(t.status, color = if (t.status == "RESOLVED") TakeItGreen else TakeItOrange, fontWeight = FontWeight.Bold)
                            }
                            Text(t.message, style = MaterialTheme.typography.bodySmall, color = TakeItMuted)
                            if (t.adminReply.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(10.dp), color = TakeItGreenSoft) {
                                    Text(
                                        "Admin Reply: ${t.adminReply}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = TakeItGreen,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(10.dp)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerAboutScreen(
    uiState: TakeItUiState,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = TakeItBg,
        topBar = {
            TopAppBar(
                title = { Text("About Take It", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        TakeIt3DBackdrop(TakeItOrange, TakeItRed, baseColor = TakeItBg) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .shadow(16.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(TakeItGold, TakeItOrange, TakeItRed)))
                        .padding(4.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_takeit_food),
                        contentDescription = "Take It Official Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                TakeIt3DCard(
                    containerColor = Color.White,
                    depthColor = Color(0xFFD5DCE6),
                    cornerRadius = 24.dp,
                    depth = 5.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("Take It — Food Delivery App", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        Text("Serving ${uiState.config.cityName} with real-time kitchen sync, verified online payments, and live doorstep tracking.", color = TakeItMuted)
                        Text("Support: ${uiState.config.supportEmail} • ${uiState.config.supportPhone}", style = MaterialTheme.typography.labelMedium, color = TakeItRed, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
