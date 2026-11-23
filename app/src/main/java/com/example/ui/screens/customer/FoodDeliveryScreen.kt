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
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.DemoCatalog
import com.example.data.MenuItemEntity
import com.example.model.AppRoute
import com.example.model.CartEntry
import com.example.model.CouponItem
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState

@Composable
fun FoodDiscoveryScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onOpenRestaurant: (String) -> Unit,
    onToggleFavourite: (String) -> Unit,
    onOpenCart: () -> Unit
) {
    val categories = listOf("All", "Pizza", "Burger", "Biryani", "South Indian", "North Indian", "Chinese", "Desserts", "Beverages", "Fast Food")
    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var pureVegOnly by remember { mutableStateOf(false) }

    val filtered = remember(selectedCategory, search, pureVegOnly) {
        DemoCatalog.restaurants.filter { r ->
            val matchCat = selectedCategory == "All" ||
                r.categoryTag.equals(selectedCategory, true) ||
                r.cuisines.contains(selectedCategory, true) ||
                (selectedCategory == "Fast Food" && r.categoryTag in setOf("Pizza", "Burger")) ||
                (selectedCategory == "Beverages" && r.cuisines.contains("Coffee", true) || r.cuisines.contains("Shakes", true))
            val matchSearch = search.isBlank() || r.name.contains(search, true) || r.cuisines.contains(search, true)
            val matchVeg = !pureVegOnly || r.isPureVeg
            matchCat && matchSearch && matchVeg
        }
    }

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = {
            ZiporaSubTopBar(
                title = "Food Delivery",
                subtitle = "Delivering to ${uiState.appState.activeAddressLabel}",
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (uiState.cartItemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCart,
                    containerColor = DeepNavy,
                    contentColor = PremiumYellow,
                    modifier = Modifier.navigationBarsPadding().testTag("food_view_cart_fab")
                ) {
                    Icon(Icons.Rounded.ShoppingBag, null)
                    Spacer(Modifier.width(8.dp))
                    Text("${uiState.cartItemCount} items • ₹${uiState.cartGrandTotal}", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search pizza, biryani, burger, cafes...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = OrangeAccent) },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SoftWhite,
                        unfocusedContainerColor = SoftWhite,
                        focusedBorderColor = OrangeAccent,
                        unfocusedBorderColor = LightBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("food_search_input")
                )
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(categories) { cat ->
                        FilterChip(
                            selected = selectedCategory == cat,
                            onClick = { selectedCategory = cat },
                            label = { Text(cat, fontWeight = FontWeight.SemiBold) }
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${filtered.size} Restaurants Near You", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                    FilterChip(
                        selected = pureVegOnly,
                        onClick = { pureVegOnly = !pureVegOnly },
                        label = { Text("Pure Veg", style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = { Icon(Icons.Rounded.Eco, null, tint = SuccessGreen, modifier = Modifier.size(16.dp)) }
                    )
                }
            }

            items(filtered, key = { it.id }) { rest ->
                val isFav = rest.id in uiState.favouriteRestaurantIds
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SoftWhite,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bounceClick { onOpenRestaurant(rest.id) }
                        .testTag("restaurant_card_${rest.id}")
                ) {
                    Column {
                        Box(modifier = Modifier.fillMaxWidth().height(152.dp)) {
                            Image(
                                painter = painterResource(R.drawable.img_food_feast_1795421416008),
                                contentDescription = rest.name,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Brush.verticalGradient(listOf(Color.Transparent, DeepNavy.copy(alpha = 0.8f))))
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(if (rest.isPureVeg) SuccessGreen else DeepNavy.copy(alpha = 0.85f))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        if (rest.isPureVeg) "PURE VEG" else "VEG & NON-VEG",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SoftWhite
                                    )
                                }
                                IconButton(
                                    onClick = { onToggleFavourite(rest.id) },
                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(SoftWhite.copy(alpha = 0.9f))
                                ) {
                                    Icon(
                                        if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                        contentDescription = "Favourite",
                                        tint = if (isFav) CoralDanger else DeepNavy,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Row(
                                modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(rest.offerText, style = MaterialTheme.typography.titleSmall, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                                Box(
                                    modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(SuccessGreen).padding(horizontal = 8.dp, vertical = 3.dp)
                                ) {
                                    Text("★ ${rest.rating}", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(rest.name, style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                            Text(rest.cuisines, style = MaterialTheme.typography.bodySmall, color = MutedText)
                            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${rest.deliveryMins} mins", style = MaterialTheme.typography.labelMedium, color = SkyBlueDark)
                                Text("• ${rest.distanceKm} km", style = MaterialTheme.typography.labelMedium, color = SlateText)
                                Text("• ₹${rest.priceForTwo} for two", style = MaterialTheme.typography.labelMedium, color = SlateText)
                            }
                        }
                    }
                }
            }
        }
    }
}

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
    val dishes = remember(uiState.menuItems, restaurant.id) {
        uiState.menuItems.filter { it.restaurantId == restaurant.id }
    }
    var customizingDish by remember { mutableStateOf<MenuItemEntity?>(null) }

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = {
            ZiporaSubTopBar(
                title = restaurant.name,
                subtitle = "★ ${restaurant.rating} (${restaurant.reviewCount} reviews) • ${restaurant.deliveryMins} mins",
                onBack = onBack,
                trailing = {
                    val isFav = restaurant.id in uiState.favouriteRestaurantIds
                    IconButton(onClick = { onToggleFavourite(restaurant.id) }) {
                        Icon(
                            if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            contentDescription = "Toggle favourite",
                            tint = if (isFav) CoralDanger else DeepNavy
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.cartItemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCart,
                    containerColor = DeepNavy,
                    contentColor = PremiumYellow,
                    modifier = Modifier.navigationBarsPadding().testTag("detail_view_cart_fab")
                ) {
                    Icon(Icons.Rounded.ShoppingCartCheckout, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Checkout (${uiState.cartItemCount}) • ₹${uiState.cartGrandTotal}", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(175.dp)) {
                    Image(
                        painter = painterResource(R.drawable.img_food_feast_1795421416008),
                        contentDescription = restaurant.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, DeepNavy.copy(alpha = 0.85f))))
                    )
                    Column(modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)) {
                        Text(restaurant.offerText, style = MaterialTheme.typography.labelMedium, color = PremiumYellow, fontWeight = FontWeight.ExtraBold)
                        Text(restaurant.cuisines, style = MaterialTheme.typography.bodyMedium, color = SoftWhite)
                    }
                }
            }

            item {
                Text(
                    "Menu (${dishes.size} dishes)",
                    style = MaterialTheme.typography.headlineMedium,
                    color = DeepNavy,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
                )
            }

            items(dishes, key = { it.id }) { dish ->
                val inCartQty = uiState.cart.filter { it.itemId == dish.id }.sumOf { it.quantity }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = SoftWhite,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(14.dp)
                                        .border(1.5.dp, if (dish.isVeg) SuccessGreen else CoralDanger, RoundedCornerShape(3.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(if (dish.isVeg) SuccessGreen else CoralDanger))
                                }
                                if (dish.isBestseller) {
                                    Text("BESTSELLER", style = MaterialTheme.typography.labelSmall, color = OrangeAccent, fontWeight = FontWeight.ExtraBold)
                                }
                                if (!dish.isAvailable) {
                                    Text("OUT OF STOCK", style = MaterialTheme.typography.labelSmall, color = CoralDanger)
                                }
                            }
                            Text(dish.name, style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            Text(dish.description, style = MaterialTheme.typography.bodySmall, color = MutedText, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("₹${dish.price}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        }

                        Spacer(Modifier.width(12.dp))

                        if (!dish.isAvailable) {
                            OutlinedButton(onClick = {}, enabled = false) { Text("Sold Out") }
                        } else if (inCartQty > 0) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(DeepNavy)
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                IconButton(onClick = { onUpdateCartQty(dish.id, -1) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Remove, "Decrease", tint = PremiumYellow, modifier = Modifier.size(18.dp))
                                }
                                Text("$inCartQty", style = MaterialTheme.typography.titleSmall, color = SoftWhite)
                                IconButton(onClick = { onUpdateCartQty(dish.id, 1) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Add, "Increase", tint = PremiumYellow, modifier = Modifier.size(18.dp))
                                }
                            }
                        } else {
                            Button(
                                onClick = { customizingDish = dish },
                                colors = ButtonDefaults.buttonColors(containerColor = PremiumYellow, contentColor = DeepNavy),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.testTag("add_dish_${dish.id}")
                            ) {
                                Text("ADD +", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }

    customizingDish?.let { dish ->
        var spice by remember { mutableStateOf("Medium Spice") }
        var extraCheese by remember { mutableStateOf(false) }
        val addonCost = if (extraCheese) 45 else 0
        AlertDialog(
            onDismissRequest = { customizingDish = null },
            containerColor = SoftWhite,
            title = { Text("Customize ${dish.name}", color = DeepNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Spice Preference", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Mild", "Medium Spice", "Extra Hot").forEach { level ->
                            FilterChip(selected = spice == level, onClick = { spice = level }, label = { Text(level) })
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { extraCheese = !extraCheese },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Add Extra Cheese / Dip (+₹45)", style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
                        Checkbox(checked = extraCheese, onCheckedChange = { extraCheese = it })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val note = if (extraCheese) "$spice • Extra Cheese" else spice
                        onAddToCart(
                            CartEntry(
                                itemId = dish.id,
                                name = dish.name,
                                sourceName = restaurant.name,
                                categoryType = "FOOD",
                                unitPrice = dish.price + addonCost,
                                mrp = dish.price + addonCost + 30,
                                quantity = 1,
                                customization = note
                            )
                        )
                        customizingDish = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)
                ) {
                    Text("Add ₹${dish.price + addonCost}")
                }
            },
            dismissButton = {
                TextButton(onClick = { customizingDish = null }) { Text("Cancel") }
            }
        )
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

    Scaffold(
        containerColor = LightSkyBackground,
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
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconOrb3D(Icons.Rounded.CheckCircle, SoftWhite, SuccessGreen, Color(0xFF15803D), size = 76.dp, iconSize = 42.dp)
                Spacer(Modifier.height(16.dp))
                Text("Order Confirmed!", style = MaterialTheme.typography.headlineLarge, color = DeepNavy)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Your order is now live! Switch to Restaurant/Grocery Owner or Delivery Partner via the ☰ menu to manage it in real time.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = SlateText
                )
                Spacer(Modifier.height(20.dp))
                SimulatedMapCanvas(
                    modifier = Modifier.fillMaxWidth().height(190.dp),
                    pickupLabel = "Merchant Hub",
                    dropLabel = uiState.appState.activeAddressLabel,
                    distanceKm = 2.4,
                    etaMinutes = 14,
                    driverProgress = -1f
                )
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onOrderPlacedSuccess,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Track in My Orders", fontWeight = FontWeight.Bold)
                }
            }
        } else if (uiState.cart.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(innerPadding).padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                IconOrb3D(Icons.Rounded.ShoppingCart, SoftWhite, SkyBlue, DeepNavy, size = 72.dp, iconSize = 36.dp)
                Spacer(Modifier.height(14.dp))
                Text("Your Cart is Empty", style = MaterialTheme.typography.headlineMedium, color = DeepNavy)
                Text("Add dishes, groceries, or nearby store items to checkout.", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                Spacer(Modifier.height(18.dp))
                Button(onClick = onBack, colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)) {
                    Text("Continue Exploring")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 32.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.LocationOn, null, tint = SkyBlueDark)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text("Delivering to ${uiState.appState.activeAddressLabel}", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                Text(uiState.appState.activeAddressLine, style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                        }
                    }
                }

                items(uiState.cart, key = { it.itemId + it.customization }) { entry ->
                    Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(entry.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                Text(entry.sourceName, style = MaterialTheme.typography.labelSmall, color = SkyBlueDark)
                                if (entry.customization.isNotBlank()) {
                                    Text(entry.customization, style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("₹${entry.unitPrice * entry.quantity}", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                                    if (entry.mrp > entry.unitPrice) {
                                        Text(
                                            "₹${entry.mrp * entry.quantity}",
                                            style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough),
                                            color = MutedText
                                        )
                                    }
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(LightSkyBackground)
                            ) {
                                IconButton(onClick = { onUpdateQty(entry.itemId, -1) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Remove, "Remove", tint = DeepNavy, modifier = Modifier.size(16.dp))
                                }
                                Text("${entry.quantity}", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                IconButton(onClick = { onUpdateQty(entry.itemId, 1) }, modifier = Modifier.size(34.dp)) {
                                    Icon(Icons.Rounded.Add, "Add", tint = DeepNavy, modifier = Modifier.size(16.dp))
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
                            FilterChip(
                                selected = applied,
                                onClick = { onApplyCoupon(if (applied) null else coupon) },
                                label = { Text("${coupon.code} (-₹${coupon.discountAmount})") }
                            )
                        }
                    }
                }

                // Bill Summary
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Bill Details", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            BillRow("Item Subtotal", "₹${uiState.cartSubtotal}")
                            if (uiState.cartDiscount > 0) {
                                BillRow("Coupon Discount (${uiState.appliedCoupon?.code})", "-₹${uiState.cartDiscount}", SuccessGreen)
                            }
                            BillRow("Delivery Fee", "₹${uiState.cartDeliveryFee}")
                            BillRow("Platform Fee", "₹${uiState.cartPlatformFee}")
                            BillRow("GST & Taxes (5%)", "₹${uiState.cartTaxes}")
                            HorizontalDivider(color = LightBorder)
                            BillRow("Total Payable", "₹${uiState.cartGrandTotal}", DeepNavy, bold = true)
                        }
                    }
                }

                // Payment Methods & Prototype Simulator
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Payment Method (Simulated)", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            val options = listOf("Zipora Wallet", "UPI • GPay", "Debit / Credit Card", "Net Banking", "Cash on Delivery")
                            options.forEach { method ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (selectedPayment == method) RideCardStart else LightSkyBackground)
                                        .clickable { selectedPayment = method }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = selectedPayment == method, onClick = { selectedPayment = method })
                                    Spacer(Modifier.width(8.dp))
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
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)
                    ) {
                        Text(
                            "Pay ₹${uiState.cartGrandTotal} • Place Order",
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
