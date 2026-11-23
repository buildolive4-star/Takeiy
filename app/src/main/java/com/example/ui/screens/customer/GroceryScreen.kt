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
import com.example.data.GroceryProductEntity
import com.example.model.CartEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState

private val GroceryPastelBg = Color(0xFFF5F9E8)
private val GroceryHeroLight = Color(0xFF84CC16)
private val GroceryHeroDark = Color(0xFF16A34A)
private val GroceryAddGreen = Color(0xFF84CC16)

private data class GroceryCatTile(
    val name: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onAddToCart: (CartEntry) -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onOpenCart: () -> Unit
) {
    val categoryTiles = remember {
        listOf(
            GroceryCatTile("Fruits", Icons.Rounded.Spa, Color(0xFFEA580C), Color(0xFFFFEDD5)),
            GroceryCatTile("Vegetables", Icons.Rounded.Eco, Color(0xFF16A34A), Color(0xFFDCFCE7)),
            GroceryCatTile("Dairy", Icons.Rounded.LocalDrink, Color(0xFF2563EB), Color(0xFFDBEAFE)),
            GroceryCatTile("Snacks", Icons.Rounded.Fastfood, Color(0xFFD97706), Color(0xFFFEF3C7)),
            GroceryCatTile("Drinks", Icons.Rounded.LocalCafe, Color(0xFFDC2626), Color(0xFFFEE2E2)),
            GroceryCatTile("Bakery", Icons.Rounded.BakeryDining, Color(0xFF9333EA), Color(0xFFF3E8FF)),
            GroceryCatTile("Daily Essentials", Icons.Rounded.ShoppingBasket, Color(0xFF0D9488), Color(0xFFCCFBF1)),
            GroceryCatTile("Personal Care", Icons.Rounded.FaceRetouchingNatural, Color(0xFFDB2777), Color(0xFFFCE7F3)),
            GroceryCatTile("Household Cleaning", Icons.Rounded.CleaningServices, Color(0xFF0284C7), Color(0xFFE0F2FE)),
            GroceryCatTile("Baby Care", Icons.Rounded.ChildFriendly, Color(0xFF7C3AED), Color(0xFFEDE9FE)),
            GroceryCatTile("All", Icons.Rounded.GridView, DeepNavy, Color(0xFFF1F5F9))
        )
    }

    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var detailProduct by remember { mutableStateOf<GroceryProductEntity?>(null) }
    var substitutionPref by remember { mutableStateOf("Replace with best match") }
    var trackerStep by remember { mutableIntStateOf(2) }
    val grocerySteps = remember {
        listOf(
            "Order Received",
            "Store Picking Fresh Items",
            "Quality Checked & Packed",
            "Delivery Buddy Assigned",
            "Out For Delivery (4 mins)",
            "Delivered at Doorstep"
        )
    }
    val activeGroceryOrder = remember(uiState.orders) {
        uiState.orders.firstOrNull { it.type == "GROCERY" && it.status != "COMPLETED" && it.status != "CANCELLED" }
    }

    val products = remember(uiState.groceryProducts, selectedCategory, search) {
        uiState.groceryProducts.filter { p ->
            val matchCat = selectedCategory == "All" || p.category.equals(selectedCategory, true)
            val matchSearch = search.isBlank() || p.name.contains(search, true) || p.category.contains(search, true)
            matchCat && matchSearch
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFFAFCF6))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 104.dp else 32.dp)
        ) {
            // 1. SOFT PASTEL CREAM-GREEN HEADER + LOCATION + SEARCH BAR (Screen 6 Exact Look)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(GroceryPastelBg, Color(0xFFFAFCF6)))
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                            text = "Instant Grocery",
                            style = MaterialTheme.typography.titleLarge,
                            color = DeepNavy,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                            IconButton(onClick = onOpenCart) {
                                Icon(Icons.Rounded.ShoppingBasket, "Cart", tint = DeepNavy)
                            }
                            if (uiState.cartItemCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 6.dp, end = 6.dp)
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(GroceryHeroDark),
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

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(Icons.Rounded.LocationOn, null, tint = GroceryHeroDark, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = uiState.appState.activeAddressLine.ifBlank { "Sector 62, Noida" },
                            style = MaterialTheme.typography.titleSmall,
                            color = DeepNavy,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Crisp White Rounded Search Field ("Search products...")
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SoftWhite,
                        shadowElevation = 2.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
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
                                    Text("Search products...", style = MaterialTheme.typography.bodyMedium, color = MutedText)
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
                                modifier = Modifier.weight(1f).testTag("grocery_search_input")
                            )
                        }
                    }
                }
            }

            // 2. VIBRANT GREEN HERO BANNER ("Fresh & Healthy / Delivered Fast ⚡" + Yellow "Shop Now →")
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .height(142.dp)
                        .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = GroceryHeroDark)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(GroceryHeroDark, GroceryHeroLight, Color(0xFFA3E635))))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_grocery_basket_1795421415559),
                        contentDescription = "Fresh and Healthy Groceries",
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight()
                            .fillMaxWidth(0.55f),
                        contentScale = ContentScale.Crop
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to Color(0xF015803D),
                                    0.54f to Color(0xDD4ADE80),
                                    0.78f to Color(0x3384CC16),
                                    1.0f to Color.Transparent
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.65f)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = "Fresh & Healthy",
                                style = MaterialTheme.typography.titleLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Delivered Fast ⚡",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color(0xFFFEF08A),
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(PremiumYellow)
                                .clickable { selectedCategory = "Fruits" }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "Shop Now →",
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepNavy,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            // Active Grocery Order Live Tracker
            if (activeGroceryOrder != null) {
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SoftWhite,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .border(1.5.dp, GroceryHeroDark.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Character3DPortrait(
                                        drawableRes = R.drawable.img_char_grocery_buddy_1795426023736,
                                        contentDescription = "Grocery Buddy",
                                        borderColor = GroceryHeroDark,
                                        size = 42.dp,
                                        bobbing = false
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("LIVE BASKET • ${activeGroceryOrder.id}", style = MaterialTheme.typography.labelSmall, color = GroceryHeroDark, fontWeight = FontWeight.ExtraBold)
                                        Text("ETA 6 mins • FreshMart Hub", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                    }
                                }
                                TextButton(onClick = { trackerStep = (trackerStep + 1) % grocerySteps.size }) {
                                    Text("Next →", style = MaterialTheme.typography.labelSmall, color = GroceryHeroDark, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            MultiStepStatusTimeline(
                                steps = grocerySteps,
                                currentStepIndex = trackerStep,
                                accentColor = GroceryHeroDark
                            )
                        }
                    }
                }
            }

            // 3. WHITE CATEGORY ICON TILES ROW (Fruits, Vegetables, Dairy, Snacks, Drinks...)
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(categoryTiles) { tile ->
                        val selected = selectedCategory == tile.name
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(66.dp)
                                .clickable { selectedCategory = if (selected) "All" else tile.name }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = SoftWhite,
                                shadowElevation = if (selected) 5.dp else 2.dp,
                                modifier = Modifier
                                    .size(58.dp)
                                    .border(
                                        width = if (selected) 2.dp else 1.dp,
                                        color = if (selected) GroceryHeroDark else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(6.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(tile.bg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(tile.icon, contentDescription = tile.name, tint = tile.tint, modifier = Modifier.size(26.dp))
                                }
                            }
                            Spacer(Modifier.height(5.dp))
                            Text(
                                text = tile.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) GroceryHeroDark else DeepNavy,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 4. "BEST SELLERS" + "See All →" HEADER
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (selectedCategory == "All") "Best Sellers" else selectedCategory,
                        style = MaterialTheme.typography.titleLarge,
                        color = DeepNavy,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = "See All →",
                        style = MaterialTheme.typography.labelLarge,
                        color = DeepNavy,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { selectedCategory = "All"; search = "" }
                    )
                }
            }

            // 5. 2-COLUMN CRISP WHITE PRODUCT CARDS WITH GREEN "Add +" BUTTON (Screen 6 Exact Look)
            items(products.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { product ->
                        val inCartQty = uiState.cart.filter { it.itemId == product.id }.sumOf { it.quantity }
                        TakeItGroceryProductCard(
                            product = product,
                            qty = inCartQty,
                            modifier = Modifier.weight(1f),
                            onOpenDetail = { detailProduct = product },
                            onAdd = {
                                onAddToCart(
                                    CartEntry(
                                        itemId = product.id,
                                        name = product.name,
                                        sourceName = "Take It Instant Mart",
                                        categoryType = "GROCERY",
                                        unitPrice = product.sellingPrice,
                                        mrp = product.mrp,
                                        quantity = 1
                                    )
                                )
                            },
                            onQtyChange = { delta -> onUpdateQty(product.id, delta) }
                        )
                    }
                    if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // Sticky Bottom Basket Bar
        if (uiState.cartItemCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(10.dp, RoundedCornerShape(18.dp), spotColor = GroceryHeroDark)
                        .clip(RoundedCornerShape(18.dp))
                        .background(Brush.horizontalGradient(listOf(GroceryHeroDark, Color(0xFF22C55E))))
                        .clickable(onClick = onOpenCart)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                        .testTag("grocery_view_cart_fab"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("${uiState.cartItemCount} ITEMS IN BASKET", style = MaterialTheme.typography.labelSmall, color = SoftWhite.copy(alpha = 0.9f), fontWeight = FontWeight.Bold)
                        Text("₹${uiState.cartGrandTotal} • 9 Min Delivery", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                    }
                    Text("View Cart →", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }

    // Product Detail Bottom Sheet (Substitution Preferences + Frequently Bought Together)
    detailProduct?.let { prod ->
        val companions = remember(uiState.groceryProducts, prod.id) {
            uiState.groceryProducts.filter { it.id != prod.id }.take(3)
        }
        ModalBottomSheet(onDismissRequest = { detailProduct = null }, containerColor = SoftWhite) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.img_grocery_basket_1795421415559),
                        contentDescription = prod.name,
                        modifier = Modifier.size(80.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        StatusPill(if (prod.stockCount > 5) "IN STOCK (${prod.stockCount})" else "LOW STOCK")
                        Spacer(Modifier.height(4.dp))
                        Text(prod.name, style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        Text("Take It Fresh Farm • ${prod.unit} • ${prod.deliveryMins} mins", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("₹${prod.sellingPrice}", style = MaterialTheme.typography.headlineMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    Text("₹${prod.mrp}", style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough), color = MutedText)
                    val pct = (((prod.mrp - prod.sellingPrice).toFloat() / prod.mrp.coerceAtLeast(1)) * 100).toInt()
                    Text("$pct% OFF", style = MaterialTheme.typography.labelMedium, color = GroceryHeroDark, fontWeight = FontWeight.Bold)
                }

                Text("Substitution Preference", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(listOf("Call me", "Replace with best match", "Refund item")) { pref ->
                        FilterChip(
                            selected = substitutionPref == pref,
                            onClick = { substitutionPref = pref },
                            label = { Text(pref, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                Text("Frequently Bought Together", style = MaterialTheme.typography.labelLarge, color = DeepNavy)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(companions) { item ->
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(GroceryPastelBg)
                                .clickable {
                                    onAddToCart(CartEntry(item.id, item.name, "Take It Instant Mart", "GROCERY", item.sellingPrice, item.mrp, 1))
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.Bold)
                                Text("₹${item.sellingPrice} • + Add", style = MaterialTheme.typography.labelSmall, color = GroceryHeroDark)
                            }
                        }
                    }
                }

                Button(
                    onClick = {
                        onAddToCart(
                            CartEntry(prod.id, prod.name, "Take It Instant Mart", "GROCERY", prod.sellingPrice, prod.mrp, 1, customization = "Sub: $substitutionPref")
                        )
                        detailProduct = null
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GroceryHeroDark, contentColor = SoftWhite),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add to Instant Basket • ₹${prod.sellingPrice}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun TakeItGroceryProductCard(
    product: GroceryProductEntity,
    qty: Int,
    modifier: Modifier = Modifier,
    onOpenDetail: () -> Unit,
    onAdd: () -> Unit,
    onQtyChange: (Int) -> Unit
) {
    val discountPct = (((product.mrp - product.sellingPrice).toFloat() / product.mrp.coerceAtLeast(1)) * 100).toInt().coerceAtLeast(0)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = SoftWhite,
        shadowElevation = 3.dp,
        modifier = modifier
            .border(1.dp, Color(0xFFECF2E4), RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("grocery_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(94.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GroceryPastelBg)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_grocery_basket_1795421415559),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (discountPct > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(GroceryHeroDark)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("$discountPct% OFF", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = SoftWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text(
                text = product.name,
                style = MaterialTheme.typography.titleSmall,
                color = DeepNavy,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                minLines = 2
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("₹${product.sellingPrice}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    Text(product.unit, style = MaterialTheme.typography.labelSmall, color = MutedText, maxLines = 1)
                }
                if (product.stockCount <= 0) {
                    Text("Sold Out", style = MaterialTheme.typography.labelSmall, color = CoralDanger)
                } else if (qty > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(GroceryHeroDark)
                    ) {
                        IconButton(onClick = { onQtyChange(-1) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Remove, "Less", tint = SoftWhite, modifier = Modifier.size(14.dp))
                        }
                        Text("$qty", style = MaterialTheme.typography.labelLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        IconButton(onClick = { onQtyChange(1) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Add, "More", tint = SoftWhite, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = onAdd,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GroceryAddGreen, contentColor = DeepNavy),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp).testTag("grocery_add_${product.id}")
                    ) {
                        Text("Add +", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
