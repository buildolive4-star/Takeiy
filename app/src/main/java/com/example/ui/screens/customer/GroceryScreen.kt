package com.example.ui.screens.customer

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.platform.LocalContext
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
import com.example.network.TakeItSmartPhoto
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.util.DeviceActions
import com.example.viewmodel.SuperAppUiState

private val GrocerGreen = Color(0xFF16A34A)
private val GrocerDarkGreen = Color(0xFF15803D)
private val GrocerSoftGreen = Color(0xFFDCFCE7)
private val GrocerYellow = Color(0xFFFACC15)
private val GrocerBg = Color(0xFFF7FBF8)

private enum class GroceryViewMode {
    HOME,
    PRODUCT_DETAIL,
    OFFERS_DEALS,
    TRACK_ORDER
}

private data class GrocerCat(
    val name: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color
)

@Composable
fun GroceryScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onAddToCart: (CartEntry) -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onOpenCart: () -> Unit
) {
    val gridCategories = remember {
        listOf(
            GrocerCat("Fruits", Icons.Rounded.Spa, Color(0xFFDC2626), Color(0xFFFEE2E2)),
            GrocerCat("Vegetables", Icons.Rounded.Eco, GrocerGreen, GrocerSoftGreen),
            GrocerCat("Dairy", Icons.Rounded.LocalDrink, Color(0xFF2563EB), Color(0xFFDBEAFE)),
            GrocerCat("Snacks", Icons.Rounded.Fastfood, Color(0xFFD97706), Color(0xFFFEF3C7)),
            GrocerCat("Drinks", Icons.Rounded.LocalCafe, Color(0xFF0284C7), Color(0xFFE0F2FE)),
            GrocerCat("Bakery", Icons.Rounded.BakeryDining, Color(0xFFB45309), Color(0xFFFFEDD5)),
            GrocerCat("Personal Care", Icons.Rounded.FaceRetouchingNatural, Color(0xFFDB2777), Color(0xFFFCE7F3)),
            GrocerCat("Household Cleaning", Icons.Rounded.CleaningServices, Color(0xFF0D9488), Color(0xFFCCFBF1)),
            GrocerCat("Baby Care", Icons.Rounded.ChildFriendly, Color(0xFF7C3AED), Color(0xFFEDE9FE)),
            GrocerCat("All", Icons.Rounded.MoreHoriz, DeepNavy, Color(0xFFF1F5F9))
        )
    }

    var viewMode by remember { mutableStateOf(GroceryViewMode.HOME) }
    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var selectedProduct by remember { mutableStateOf<GroceryProductEntity?>(null) }
    var detailQty by remember { mutableIntStateOf(1) }
    var offersFilter by remember { mutableStateOf("All Offers") }
    var trackStep by remember { mutableIntStateOf(3) }

    val products = remember(uiState.groceryProducts, selectedCategory, search) {
        uiState.groceryProducts.filter { p ->
            val matchCat = selectedCategory == "All" || p.category.equals(selectedCategory, true)
            val matchSearch = search.isBlank() || p.name.contains(search, true) || p.category.contains(search, true)
            matchCat && matchSearch
        }
    }

    AnimatedContent(targetState = viewMode, label = "takeItGroceryMode") { mode ->
        when (mode) {
            // =========================================================================
            // IMAGE 3 SCREEN 1 & 2: GROCERY HOME + CATEGORY PRODUCT GRID
            // =========================================================================
            GroceryViewMode.HOME -> {
                Box(modifier = Modifier.fillMaxSize().background(GrocerBg)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = if (uiState.cartItemCount > 0) 104.dp else 32.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Top Bar + Green Leaf Brand Row
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SoftWhite)
                                    .statusBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(onClick = onBack, modifier = Modifier.size(44.dp).testTag("back_button")) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                                    }
                                    Icon(Icons.Rounded.LocationOn, null, tint = GrocerGreen, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        uiState.appState.activeAddressLabel.ifBlank { "Sector 62, Noida" } + " ∨",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = DeepNavy,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(onClick = { viewMode = GroceryViewMode.OFFERS_DEALS }) {
                                        Text("Offers", color = GrocerGreen, fontWeight = FontWeight.ExtraBold)
                                    }
                                    TextButton(onClick = { viewMode = GroceryViewMode.TRACK_ORDER }) {
                                        Text("Track", color = Color(0xFF2563EB), fontWeight = FontWeight.Bold)
                                    }
                                }

                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier.size(38.dp).clip(CircleShape).background(GrocerGreen),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.Eco, null, tint = SoftWhite, modifier = Modifier.size(22.dp))
                                    }
                                    Spacer(Modifier.width(10.dp))
                                    Column {
                                        Text("Take It Fresh", style = MaterialTheme.typography.titleLarge, color = GrocerDarkGreen, fontWeight = FontWeight.Black)
                                        Text("Freshness at Your Doorstep", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    }
                                }
                            }
                        }

                        // Emerald Green "50% OFF on Fresh Fruits & Vegetables" Hero Banner (Image 3 Screen 1)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .height(142.dp)
                                    .shadow(8.dp, RoundedCornerShape(22.dp), spotColor = GrocerGreen)
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Brush.horizontalGradient(listOf(GrocerDarkGreen, GrocerGreen, Color(0xFF4ADE80))))
                                    .clickable { viewMode = GroceryViewMode.OFFERS_DEALS }
                            ) {
                                Image(
                                    painter = painterResource(R.drawable.img_grocery_basket_1795421415559),
                                    contentDescription = "Fresh Fruits & Vegetables",
                                    modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.56f),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier.fillMaxSize().background(
                                        Brush.horizontalGradient(0.0f to Color(0xF015803D), 0.56f to Color(0xCC16A34A), 0.85f to Color.Transparent)
                                    )
                                )
                                Column(
                                    modifier = Modifier.fillMaxHeight().fillMaxWidth(0.66f).padding(16.dp),
                                    verticalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text("50% OFF", style = MaterialTheme.typography.headlineMedium, color = SoftWhite, fontWeight = FontWeight.Black)
                                        Text("on Fresh Fruits\n& Vegetables", style = MaterialTheme.typography.titleSmall, color = Color(0xFFDCFCE7), fontWeight = FontWeight.Bold)
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(50)).background(GrocerYellow).padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text("Order Now →", style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }

                        // Search Bar
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
                                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Rounded.Search, null, tint = DeepNavy, modifier = Modifier.size(20.dp))
                                    TextField(
                                        value = search,
                                        onValueChange = { search = it },
                                        placeholder = { Text("Search for groceries...", color = MutedText) },
                                        singleLine = true,
                                        colors = TextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedIndicatorColor = Color.Transparent,
                                            unfocusedIndicatorColor = Color.Transparent
                                        ),
                                        modifier = Modifier.weight(1f).testTag("grocery_search_input")
                                    )
                                    Icon(Icons.Rounded.Mic, "Voice search", tint = DeepNavy, modifier = Modifier.size(20.dp))
                                }
                            }
                        }

                        // 5×2 Category Icon Grid (Image 3 Screen 1)
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                gridCategories.chunked(5).forEach { rowCats ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        rowCats.forEach { cat ->
                                            val sel = selectedCategory == cat.name
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier.weight(1f).clickable { selectedCategory = if (sel) "All" else cat.name }
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(52.dp)
                                                        .clip(RoundedCornerShape(14.dp))
                                                        .background(cat.bg)
                                                        .border(if (sel) 2.dp else 1.dp, if (sel) GrocerGreen else SoftWhite, RoundedCornerShape(14.dp)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(cat.icon, cat.name, tint = cat.tint, modifier = Modifier.size(26.dp))
                                                }
                                                Spacer(Modifier.height(3.dp))
                                                Text(cat.name.substringBefore(" "), style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = if (sel) GrocerDarkGreen else DeepNavy, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Golden Yellow "10 Minute Instant Delivery ⚡" Banner (Image 3 Screen 1)
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Brush.horizontalGradient(listOf(Color(0xFFFEF08A), GrocerYellow, Color(0xFFF59E0B))))
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("10 Minute ⚡", style = MaterialTheme.typography.headlineSmall, color = DeepNavy, fontWeight = FontWeight.Black)
                                        Text("Instant Delivery", style = MaterialTheme.typography.titleSmall, color = Color(0xFF451A03), fontWeight = FontWeight.Bold)
                                    }
                                    Character3DPortrait(
                                        drawableRes = R.drawable.img_char_grocery_buddy_1795426023736,
                                        contentDescription = "10 Min Grocery Buddy",
                                        borderColor = GrocerGreen,
                                        size = 64.dp,
                                        bobbing = false
                                    )
                                }
                            }
                        }

                        // Top Picks for You Header + 2-Column Grid (Image 3 Screen 2 Cards with Veg Dot, Heart & Green Add +)
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(if (selectedCategory == "All") "Top Picks for You" else selectedCategory, style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "See All →",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = Color(0xFF2563EB),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.clickable { selectedCategory = "All"; search = "" }
                                )
                            }
                        }

                        items(products.chunked(2)) { rowItems ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                rowItems.forEach { prod ->
                                    val qty = uiState.cart.filter { it.itemId == prod.id }.sumOf { it.quantity }
                                    QuickGrocerCard(
                                        product = prod,
                                        qty = qty,
                                        directImageUrl = uiState.groceryImageUrls[prod.id],
                                        modifier = Modifier.weight(1f),
                                        onOpenDetail = {
                                            selectedProduct = prod
                                            detailQty = 1
                                            viewMode = GroceryViewMode.PRODUCT_DETAIL
                                        },
                                        onAdd = {
                                            onAddToCart(CartEntry(prod.id, prod.name, "Take It Fresh Mart", "GROCERY", prod.sellingPrice, prod.mrp, 1))
                                        },
                                        onQtyChange = { delta -> onUpdateQty(prod.id, delta) }
                                    )
                                }
                                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }

                    if (uiState.cartItemCount > 0) {
                        Box(
                            modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(10.dp, RoundedCornerShape(50), spotColor = GrocerGreen)
                                    .clip(RoundedCornerShape(50))
                                    .background(GrocerDarkGreen)
                                    .clickable(onClick = onOpenCart)
                                    .padding(horizontal = 20.dp, vertical = 14.dp)
                                    .testTag("grocery_view_cart_fab"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("${uiState.cartItemCount} items • ₹${uiState.cartGrandTotal}", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                Text("My Cart →", style = MaterialTheme.typography.titleMedium, color = GrocerYellow, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // IMAGE 3 SCREEN 3: FULL PRODUCT DETAIL PAGE
            // =========================================================================
            GroceryViewMode.PRODUCT_DETAIL -> {
                val prod = selectedProduct ?: uiState.groceryProducts.first()
                val pct = (((prod.mrp - prod.sellingPrice).toFloat() / prod.mrp.coerceAtLeast(1)) * 100).toInt().coerceAtLeast(10)
                Column(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
                    Box(modifier = Modifier.fillMaxWidth().height(275.dp)) {
                        TakeItSmartPhoto(
                            query = "fresh ${prod.name}",
                            fallbackDrawableRes = R.drawable.img_grocery_basket_1795421415559,
                            contentDescription = prod.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop,
                            directImageUrl = uiState.groceryImageUrls[prod.id]
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            IconButton(
                                onClick = { viewMode = GroceryViewMode.HOME },
                                modifier = Modifier.size(44.dp).clip(CircleShape).background(SoftWhite)
                            ) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy) }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(Modifier.size(42.dp).clip(CircleShape).background(SoftWhite), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.FavoriteBorder, "Save", tint = Color(0xFFEF4444))
                                }
                                Box(Modifier.size(42.dp).clip(CircleShape).background(SoftWhite), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Share, "Share", tint = DeepNavy)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                                .clip(RoundedCornerShape(50))
                                .background(GrocerGreen)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text("🌿 Fresh", style = MaterialTheme.typography.labelMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(prod.name, style = MaterialTheme.typography.headlineMedium, color = DeepNavy, fontWeight = FontWeight.Black)
                            Text("★ 4.5 (120K+ reviews)", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                        }
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("₹${prod.sellingPrice}", style = MaterialTheme.typography.headlineMedium, color = DeepNavy, fontWeight = FontWeight.Black)
                                Text("₹${prod.mrp}", style = MaterialTheme.typography.bodyLarge.copy(textDecoration = TextDecoration.LineThrough), color = MutedText)
                                Box(Modifier.clip(RoundedCornerShape(8.dp)).background(GrocerSoftGreen).padding(horizontal = 8.dp, vertical = 4.dp)) {
                                    Text("$pct% OFF", style = MaterialTheme.typography.labelMedium, color = GrocerDarkGreen, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }
                        item {
                            Text("About this item", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Text("Hand-picked farm fresh ${prod.name.lowercase()} (${prod.unit}), rich in natural nutrients and delivered cold-chain packed in ${prod.deliveryMins} minutes.", style = MaterialTheme.typography.bodyMedium, color = SlateText)
                        }
                        // 3 Quality Trust Tiles (100% Fresh | No Pesticides | Farm Fresh)
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf(
                                    "100% Fresh" to Icons.Rounded.Eco,
                                    "No Pesticides" to Icons.Rounded.VerifiedUser,
                                    "Farm Fresh" to Icons.Rounded.Agriculture
                                ).forEach { (lbl, ic) ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = GrocerBg,
                                        modifier = Modifier.weight(1f).border(1.dp, Color(0xFFD1FAE5), RoundedCornerShape(14.dp))
                                    ) {
                                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Icon(ic, lbl, tint = GrocerGreen, modifier = Modifier.size(22.dp))
                                            Text(lbl, style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                                        }
                                    }
                                }
                            }
                        }
                        // Qty Stepper + Unit
                        item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Color(0xFFF1F5F9)).padding(horizontal = 6.dp)
                                ) {
                                    IconButton(onClick = { if (detailQty > 1) detailQty-- }) { Icon(Icons.Rounded.Remove, "Less", tint = DeepNavy) }
                                    Text("$detailQty", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    IconButton(onClick = { detailQty++ }) { Icon(Icons.Rounded.Add, "More", tint = DeepNavy) }
                                }
                                Surface(shape = RoundedCornerShape(10.dp), color = Color(0xFFF1F5F9)) {
                                    Text(prod.unit, style = MaterialTheme.typography.labelLarge, color = DeepNavy, modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp))
                                }
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onAddToCart(CartEntry(prod.id, prod.name, "Take It Fresh Mart", "GROCERY", prod.sellingPrice, prod.mrp, detailQty))
                            viewMode = GroceryViewMode.HOME
                        },
                        modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(16.dp).height(54.dp),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = GrocerDarkGreen, contentColor = SoftWhite)
                    ) {
                        Icon(Icons.Rounded.ShoppingCart, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add to Cart • ₹${prod.sellingPrice * detailQty}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // =========================================================================
            // IMAGE 3 SCREEN 6: OFFERS & DEALS
            // =========================================================================
            GroceryViewMode.OFFERS_DEALS -> {
                Column(modifier = Modifier.fillMaxSize().background(GrocerBg)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().background(SoftWhite).statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewMode = GroceryViewMode.HOME }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                        }
                        Text("Offers & Deals", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Spacer(Modifier.size(48.dp))
                    }
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(22.dp))
                                    .background(Brush.horizontalGradient(listOf(GrocerDarkGreen, GrocerGreen)))
                                    .padding(18.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text("Flat 60% OFF\non Groceries", style = MaterialTheme.typography.headlineMedium, color = SoftWhite, fontWeight = FontWeight.Black)
                                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(SoftWhite.copy(alpha = 0.2f)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                        Text("Use Code: GRO60", style = MaterialTheme.typography.labelMedium, color = GrocerYellow, fontWeight = FontWeight.ExtraBold)
                                    }
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(50)).background(GrocerYellow).clickable { viewMode = GroceryViewMode.HOME }.padding(horizontal = 14.dp, vertical = 7.dp)
                                    ) { Text("Shop Now →", style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold) }
                                }
                            }
                        }
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                                listOf("All Offers", "Fresh Deals", "Combo Offers").forEach { tab ->
                                    Text(
                                        tab,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = if (offersFilter == tab) GrocerDarkGreen else MutedText,
                                        fontWeight = FontWeight.ExtraBold,
                                        modifier = Modifier.clickable { offersFilter = tab }
                                    )
                                }
                            }
                        }
                        items(
                            listOf(
                                Triple("₹100 OFF", "on Orders above ₹699 • Code: FRESH100", GrocerGreen),
                                Triple("Extra 20% OFF", "on Fresh Fruits & Vegetables", Color(0xFFEA580C)),
                                Triple("Free Delivery", "on First 3 Instant Grocery Orders", Color(0xFF2563EB))
                            )
                        ) { (title, sub, tint) ->
                            Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite, shadowElevation = 2.dp) {
                                Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.LocalOffer, null, tint = tint)
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                        Text(sub, style = MaterialTheme.typography.bodySmall, color = MutedText)
                                    }
                                    Text("Apply", style = MaterialTheme.typography.labelLarge, color = Color(0xFF2563EB), fontWeight = FontWeight.Bold, modifier = Modifier.clickable { viewMode = GroceryViewMode.HOME })
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // IMAGE 3 SCREEN 5: TRACK GROCERY ORDER (5-Step Green Check Bar + Map + Rohit Kumar Card)
            // =========================================================================
            GroceryViewMode.TRACK_ORDER -> {
                val steps = listOf("Placed", "Preparing", "Packed", "Out for\nDelivery", "Delivered")
                Column(modifier = Modifier.fillMaxSize().background(SoftWhite)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewMode = GroceryViewMode.HOME }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                        }
                        Text("Track Order", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
                        Spacer(Modifier.size(48.dp))
                    }

                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).clip(CircleShape).background(GrocerGreen), contentAlignment = Alignment.Center) {
                            Icon(Icons.Rounded.Check, null, tint = SoftWhite, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Order Placed", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Text("We have packed your fresh items", style = MaterialTheme.typography.bodySmall, color = MutedText)
                        }
                        TextButton(onClick = { trackStep = (trackStep + 1) % 5 }) {
                            Text("Next →", color = GrocerGreen, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    // 5-Step Green Progress Bar
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.Top) {
                        steps.forEachIndexed { idx, lbl ->
                            val reached = idx <= trackStep
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Box(Modifier.weight(1f).height(2.5.dp).background(if (idx == 0) Color.Transparent else if (reached) GrocerGreen else Color(0xFFE2E8F0)))
                                    Box(Modifier.size(20.dp).clip(CircleShape).background(if (reached) GrocerGreen else Color(0xFFE2E8F0)), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Rounded.Check, null, tint = SoftWhite, modifier = Modifier.size(12.dp))
                                    }
                                    Box(Modifier.weight(1f).height(2.5.dp).background(if (idx == steps.lastIndex) Color.Transparent else if (idx < trackStep) GrocerGreen else Color(0xFFE2E8F0)))
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(lbl, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = if (reached) DeepNavy else MutedText, textAlign = TextAlign.Center, lineHeight = 11.sp)
                            }
                        }
                    }

                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxWidth().weight(1f).padding(horizontal = 16.dp).clip(RoundedCornerShape(20.dp)),
                        pickupLabel = "FreshMart Hub",
                        dropLabel = uiState.appState.activeAddressLabel,
                        distanceKm = 2.8,
                        etaMinutes = 12,
                        driverProgress = (trackStep + 1) * 0.19f
                    )

                    Surface(
                        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                        color = SoftWhite,
                        shadowElevation = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.navigationBarsPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Character3DPortrait(
                                    drawableRes = R.drawable.img_char_grocery_buddy_1795426023736,
                                    contentDescription = "Rohit Kumar",
                                    borderColor = GrocerGreen,
                                    size = 52.dp,
                                    bobbing = false
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Rohit Kumar", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                    Text("Delivery Partner • ★ 4.8", style = MaterialTheme.typography.bodySmall, color = MutedText)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Arriving in", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                    Text("12 min", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.Black)
                                    Text("(2.8 km)", style = MaterialTheme.typography.labelSmall, color = MutedText)
                                }
                            }
                            val context = LocalContext.current
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                listOf("Call" to Icons.Rounded.Call, "Chat" to Icons.Rounded.Chat, "Share" to Icons.Rounded.Share).forEach { (lbl, ic) ->
                                    Surface(
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFF1F5F9),
                                        modifier = Modifier.weight(1f).clickable {
                                            when (lbl) {
                                                "Call" -> DeviceActions.dialPhoneNumber(context, "+919876543210")
                                                "Share" -> DeviceActions.shareText(
                                                    context,
                                                    "Take It Grocery Order Tracking",
                                                    "My Take It instant grocery order from FreshMart Hub is arriving in 12 min with Rohit Kumar!"
                                                )
                                                else -> viewMode = GroceryViewMode.HOME
                                            }
                                        }
                                    ) {
                                        Row(modifier = Modifier.padding(vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                                            Icon(ic, lbl, tint = Color(0xFF2563EB), modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(5.dp))
                                            Text(lbl, style = MaterialTheme.typography.labelMedium, color = DeepNavy, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickGrocerCard(
    product: GroceryProductEntity,
    qty: Int,
    directImageUrl: String? = null,
    modifier: Modifier = Modifier,
    onOpenDetail: () -> Unit,
    onAdd: () -> Unit,
    onQtyChange: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SoftWhite,
        shadowElevation = 3.dp,
        modifier = modifier
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
            .clickable(onClick = onOpenDetail)
            .testTag("grocery_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(modifier = Modifier.fillMaxWidth().height(92.dp).clip(RoundedCornerShape(12.dp)).background(GrocerBg)) {
                TakeItSmartPhoto(
                    query = "fresh ${product.name}",
                    fallbackDrawableRes = R.drawable.img_grocery_basket_1795421415559,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                    directImageUrl = directImageUrl
                )
                // Veg Dot top-left + Heart top-right (Image 3 Screen 2 exact detail!)
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(15.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(SoftWhite)
                        .border(1.2.dp, GrocerGreen, RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(GrocerGreen))
                }
                Icon(
                    Icons.Rounded.FavoriteBorder,
                    null,
                    tint = DeepNavy,
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(18.dp)
                )
            }
            Text(product.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("₹${product.sellingPrice}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.Black)
                    Text(product.unit, style = MaterialTheme.typography.labelSmall, color = MutedText, maxLines = 1)
                }
                if (qty > 0) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clip(RoundedCornerShape(50)).background(GrocerDarkGreen)) {
                        IconButton(onClick = { onQtyChange(-1) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Rounded.Remove, "Less", tint = SoftWhite, modifier = Modifier.size(14.dp)) }
                        Text("$qty", style = MaterialTheme.typography.labelMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        IconButton(onClick = { onQtyChange(1) }, modifier = Modifier.size(26.dp)) { Icon(Icons.Rounded.Add, "More", tint = SoftWhite, modifier = Modifier.size(14.dp)) }
                    }
                } else {
                    Button(
                        onClick = onAdd,
                        colors = ButtonDefaults.buttonColors(containerColor = GrocerDarkGreen, contentColor = SoftWhite),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp),
                        modifier = Modifier.height(30.dp).testTag("grocery_add_${product.id}")
                    ) { Text("Add +", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold) }
                }
            }
        }
    }
}
