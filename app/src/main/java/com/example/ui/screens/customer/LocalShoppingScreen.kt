package com.example.ui.screens.customer

import androidx.activity.compose.BackHandler
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
import com.example.data.DemoCatalog
import com.example.model.CartEntry
import com.example.network.PexelsImageService
import com.example.network.TakeItSmartPhoto
import com.example.ui.components.Character3DPortrait
import com.example.ui.theme.*
import com.example.util.DeviceActions
import com.example.viewmodel.SuperAppUiState

private val ShopPurple = Color(0xFF5B21B6)
private val ShopViolet = Color(0xFF7C3AED)
private val ShopRoyalBlue = Color(0xFF2563EB)
private val ShopBg = Color(0xFFF6F7FB)
private val ShopGreenAdd = Color(0xFF16A34A)
private val ShopGold = Color(0xFFFFD43B)

private enum class LocalShopStage {
    HOME,
    CATEGORIES,
    PRODUCT_DETAIL,
    TRACK_ORDER
}

private data class ShopProductCardModel(
    val id: String,
    val name: String,
    val subtitle: String,
    val shopName: String,
    val segment: String, // Men, Women, Kids, Electronics
    val price: Int,
    val mrp: Int,
    val offPercent: Int,
    val rating: String,
    val reviews: String,
    val icon: ImageVector,
    val accentBg: Color,
    val iconTint: Color,
    val sizes: List<String>
)

private data class ShopCategoryTile(
    val title: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color
)

@Composable
fun LocalShoppingScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onAddToCart: (CartEntry) -> Unit,
    onOpenCart: () -> Unit
) {
    var stage by remember { mutableStateOf(LocalShopStage.HOME) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedSegment by remember { mutableStateOf("All") }
    var wishlistIds by remember { mutableStateOf(setOf<String>()) }

    val catalog = remember {
        val curated = listOf(
            ShopProductCardModel("ls_shirt", "Men's Cotton Shirt", "Slim Fit • Breathable", "Urban Loom Studio", "Men", 499, 999, 50, "4.6", "4.2k", Icons.Rounded.Checkroom, Color(0xFFE0F2FE), Color(0xFF0369A1), listOf("S", "M", "L", "XL", "XXL")),
            ShopProductCardModel("ls_dress", "Women's Dress", "Festive Pastel Midi", "Aura Boutique", "Women", 799, 1499, 47, "4.7", "6.8k", Icons.Rounded.DryCleaning, Color(0xFFFCE7F3), Color(0xFFBE185D), listOf("XS", "S", "M", "L", "XL")),
            ShopProductCardModel("ls_shoes", "Sports Shoes", "Running & Training", "Stride Local Hub", "Men", 1299, 2499, 48, "4.5", "12.5k", Icons.Rounded.DirectionsRun, Color(0xFFDBEAFE), Color(0xFF1D4ED8), listOf("6", "7", "8", "9", "10")),
            ShopProductCardModel("ls_watch", "Smart Watch", "AMOLED • Calling", "TechZone Noida", "Electronics", 2999, 5999, 50, "4.8", "9.1k", Icons.Rounded.Watch, Color(0xFFEDE9FE), Color(0xFF6D28D9), listOf("42mm", "44mm", "46mm")),
            ShopProductCardModel("ls_kids", "Kids Sneaker Set", "Soft Cushion Sole", "LittleSteps Store", "Kids", 649, 1199, 46, "4.6", "2.1k", Icons.Rounded.ChildCare, Color(0xFFFEF3C7), Color(0xFFD97706), listOf("3", "4", "5", "6"))
        )
        val fromDemo = DemoCatalog.localShopProducts.mapIndexed { idx, p ->
            val off = if (p.mrp > 0) (((p.mrp - p.price) * 100) / p.mrp).coerceAtLeast(15) else 25
            ShopProductCardModel(
                id = p.id,
                name = p.name,
                subtitle = "${p.badge} • ${p.deliveryMins} min delivery",
                shopName = p.shopName,
                segment = if (p.category == "Fashion") "Women" else if (p.category == "Electronics") "Electronics" else "Men",
                price = p.price,
                mrp = p.mrp,
                offPercent = off,
                rating = "${p.rating}",
                reviews = "${3 + idx}.4k",
                icon = when (p.category) {
                    "Electronics" -> Icons.Rounded.Headphones
                    "Fashion" -> Icons.Rounded.ShoppingBag
                    else -> Icons.Rounded.Chair
                },
                accentBg = if (idx % 2 == 0) Color(0xFFE0E7FF) else Color(0xFFECFDF5),
                iconTint = if (idx % 2 == 0) ShopPurple else ShopGreenAdd,
                sizes = listOf("S", "M", "L", "XL")
            )
        }
        curated + fromDemo
    }

    var selectedProduct by remember { mutableStateOf(catalog[2]) } // Sports Shoes
    var selectedSize by remember { mutableStateOf("8") }
    var selectedColorName by remember { mutableStateOf("Blue") }

    BackHandler(enabled = stage != LocalShopStage.HOME) {
        stage = LocalShopStage.HOME
    }

    when (stage) {
        LocalShopStage.HOME -> LocalShopHomeContent(
            uiState = uiState,
            searchQuery = searchQuery,
            selectedSegment = selectedSegment,
            products = catalog.filter { prod ->
                (selectedSegment == "All" || prod.segment.equals(selectedSegment, ignoreCase = true)) &&
                    (searchQuery.isBlank() || prod.name.contains(searchQuery, ignoreCase = true) || prod.shopName.contains(searchQuery, ignoreCase = true))
            },
            wishlistIds = wishlistIds,
            onSearchChange = { searchQuery = it },
            onSelectSegment = { selectedSegment = it },
            onToggleWishlist = { id ->
                wishlistIds = if (id in wishlistIds) wishlistIds - id else wishlistIds + id
            },
            onOpenCategories = { stage = LocalShopStage.CATEGORIES },
            onOpenTrackOrder = { stage = LocalShopStage.TRACK_ORDER },
            onOpenProduct = { prod ->
                selectedProduct = prod
                selectedSize = prod.sizes.getOrElse(2) { prod.sizes.first() }
                stage = LocalShopStage.PRODUCT_DETAIL
            },
            onQuickAdd = { prod ->
                onAddToCart(CartEntry(prod.id, prod.name, prod.shopName, "SHOPPING", prod.price, prod.mrp, 1))
            },
            onOpenCart = onOpenCart,
            onBack = onBack
        )

        LocalShopStage.CATEGORIES -> LocalShopCategoriesContent(
            onPickCategory = { segment ->
                selectedSegment = segment
                stage = LocalShopStage.HOME
            },
            onBack = { stage = LocalShopStage.HOME }
        )

        LocalShopStage.PRODUCT_DETAIL -> LocalShopProductDetailContent(
            product = selectedProduct,
            selectedSize = selectedSize,
            selectedColor = selectedColorName,
            isWishlisted = selectedProduct.id in wishlistIds,
            onSelectSize = { selectedSize = it },
            onSelectColor = { selectedColorName = it },
            onToggleWishlist = {
                wishlistIds = if (selectedProduct.id in wishlistIds) wishlistIds - selectedProduct.id else wishlistIds + selectedProduct.id
            },
            onAddToCart = {
                onAddToCart(
                    CartEntry(
                        selectedProduct.id,
                        "${selectedProduct.name} ($selectedSize • $selectedColorName)",
                        selectedProduct.shopName,
                        "SHOPPING",
                        selectedProduct.price,
                        selectedProduct.mrp,
                        1
                    )
                )
                onOpenCart()
            },
            onBuyNow = {
                onAddToCart(
                    CartEntry(
                        selectedProduct.id,
                        "${selectedProduct.name} ($selectedSize • $selectedColorName)",
                        selectedProduct.shopName,
                        "SHOPPING",
                        selectedProduct.price,
                        selectedProduct.mrp,
                        1
                    )
                )
                stage = LocalShopStage.TRACK_ORDER
            },
            onBack = { stage = LocalShopStage.HOME }
        )

        LocalShopStage.TRACK_ORDER -> LocalShopTrackOrderContent(
            productName = selectedProduct.name,
            onBackHome = { stage = LocalShopStage.HOME }
        )
    }
}

@Composable
private fun LocalShopHomeContent(
    uiState: SuperAppUiState,
    searchQuery: String,
    selectedSegment: String,
    products: List<ShopProductCardModel>,
    wishlistIds: Set<String>,
    onSearchChange: (String) -> Unit,
    onSelectSegment: (String) -> Unit,
    onToggleWishlist: (String) -> Unit,
    onOpenCategories: () -> Unit,
    onOpenTrackOrder: () -> Unit,
    onOpenProduct: (ShopProductCardModel) -> Unit,
    onQuickAdd: (ShopProductCardModel) -> Unit,
    onOpenCart: () -> Unit,
    onBack: () -> Unit
) {
    val topCats = remember {
        listOf(
            ShopCategoryTile("Fashion &\nWear", Icons.Rounded.Checkroom, Color(0xFFDB2777), Color(0xFFFCE7F3)),
            ShopCategoryTile("Electronics", Icons.Rounded.Headphones, Color(0xFF2563EB), Color(0xFFDBEAFE)),
            ShopCategoryTile("Home &\nKitchen", Icons.Rounded.Chair, Color(0xFFD97706), Color(0xFFFEF3C7)),
            ShopCategoryTile("Beauty &\nCare", Icons.Rounded.AutoAwesome, Color(0xFF9333EA), Color(0xFFF3E8FF)),
            ShopCategoryTile("Grocery", Icons.Rounded.LocalGroceryStore, Color(0xFF16A34A), Color(0xFFDCFCE7)),
            ShopCategoryTile("More", Icons.Rounded.GridView, ShopPurple, Color(0xFFEDE9FE))
        )
    }

    Scaffold(
        containerColor = ShopBg,
        floatingActionButton = {
            if (uiState.cartItemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCart,
                    containerColor = ShopPurple,
                    contentColor = SoftWhite,
                    modifier = Modifier.navigationBarsPadding().testTag("local_shop_cart_fab")
                ) {
                    Icon(Icons.Rounded.ShoppingBag, null)
                    Spacer(Modifier.width(8.dp))
                    Text("${uiState.cartItemCount} • ₹${uiState.cartGrandTotal}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Location & Cart Header + Search Pill
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SoftWhite)
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(48.dp).testTag("local_shop_back")) {
                            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                        }
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Rounded.LocationOn, null, tint = ShopViolet, modifier = Modifier.size(20.dp))
                            Text(
                                uiState.appState.activeAddressLabel.ifBlank { "Greater Noida" },
                                style = MaterialTheme.typography.titleMedium,
                                color = DeepNavy,
                                fontWeight = FontWeight.ExtraBold
                            )
                            Icon(Icons.Rounded.KeyboardArrowDown, null, tint = DeepNavy, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = onOpenCart, modifier = Modifier.size(48.dp).testTag("local_shop_cart_icon")) {
                            BadgedBox(
                                badge = {
                                    if (uiState.cartItemCount > 0) {
                                        Badge(containerColor = Color(0xFFDC2626)) { Text("${uiState.cartItemCount}") }
                                    }
                                }
                            ) {
                                Icon(Icons.Rounded.ShoppingCart, "Cart", tint = DeepNavy)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        placeholder = { Text("Search for products...") },
                        leadingIcon = { Icon(Icons.Rounded.Search, null, tint = MutedText) },
                        singleLine = true,
                        shape = RoundedCornerShape(50),
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            focusedContainerColor = SoftWhite,
                            unfocusedBorderColor = Color(0xFFE2E8F0),
                            focusedBorderColor = ShopViolet
                        ),
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                    )
                }
            }

            // Hero Banner: "Shop Local • Get Best Deals!"
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(176.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.horizontalGradient(listOf(ShopRoyalBlue, ShopPurple)))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_local_shop_hero_1795429839958),
                        contentDescription = "Shop Local Best Deals",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(188.dp).fillMaxHeight().align(Alignment.CenterEnd)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(0.0f to ShopRoyalBlue, 0.56f to ShopPurple.copy(alpha = 0.86f), 1.0f to Color.Transparent))
                    )
                    Column(
                        modifier = Modifier.fillMaxHeight().fillMaxWidth(0.64f).padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text("Shop Local", style = MaterialTheme.typography.headlineSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                            Text("Get Best Deals!", style = MaterialTheme.typography.titleLarge, color = ShopGold, fontWeight = FontWeight.ExtraBold)
                            Text("Great Offers Near You", style = MaterialTheme.typography.labelMedium, color = SoftWhite.copy(alpha = 0.9f))
                        }
                        Button(
                            onClick = onOpenCategories,
                            colors = ButtonDefaults.buttonColors(containerColor = ShopGold, contentColor = DeepNavy),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            modifier = Modifier.height(44.dp).testTag("local_shop_hero_cta")
                        ) {
                            Text("Shop Now", fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(4.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 4 Circular Pastel Shortcuts
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ShopShortcutCircle("Nearby\nStores", Icons.Rounded.LocationOn, ShopViolet, Color(0xFFF3E8FF), onOpenCategories)
                    ShopShortcutCircle("Deals &\nOffers", Icons.Rounded.Percent, Color(0xFFEA580C), Color(0xFFFFEDD5), onOpenCategories)
                    ShopShortcutCircle("My\nOrders", Icons.Rounded.ReceiptLong, ShopRoyalBlue, Color(0xFFDBEAFE), onOpenTrackOrder)
                    ShopShortcutCircle("Track\nOrder", Icons.Rounded.LocalShipping, ShopPurple, Color(0xFFEDE9FE), onOpenTrackOrder)
                }
            }

            // Purple "Flat 50% OFF on Local Stores" Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(ShopPurple, Color(0xFF9333EA), Color(0xFFEC4899))))
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Flat 50% OFF", style = MaterialTheme.typography.titleLarge, color = ShopGold, fontWeight = FontWeight.ExtraBold)
                            Text("on Local Stores", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.Bold)
                            Surface(shape = RoundedCornerShape(50), color = ShopGold) {
                                Text(
                                    "Use Code: LOCAL50",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepNavy,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                                )
                            }
                        }
                        Box(
                            modifier = Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)).background(SoftWhite.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.CardGiftcard, null, tint = ShopGold, modifier = Modifier.size(36.dp))
                        }
                    }
                }
            }

            // Top Categories 3×2 Preview + See All →
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Top Categories", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        TextButton(onClick = onOpenCategories, modifier = Modifier.testTag("local_shop_see_all_cats")) {
                            Text("See All →", color = ShopRoyalBlue, fontWeight = FontWeight.Bold)
                        }
                    }
                    topCats.chunked(3).forEach { row ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { cat ->
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = SoftWhite,
                                    shadowElevation = 2.dp,
                                    modifier = Modifier.weight(1f).clickable(onClick = onOpenCategories)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(cat.bg),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(cat.icon, cat.title, tint = cat.tint, modifier = Modifier.size(24.dp))
                                        }
                                        Text(
                                            cat.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DeepNavy,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            lineHeight = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Purple Segment Tabs: All | Men | Women | Kids | Electronics
            item {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(listOf("All", "Men", "Women", "Kids", "Electronics")) { seg ->
                        val active = selectedSegment == seg
                        Surface(
                            onClick = { onSelectSegment(seg) },
                            shape = RoundedCornerShape(50),
                            color = if (active) ShopViolet else SoftWhite,
                            shadowElevation = if (active) 3.dp else 1.dp,
                            modifier = Modifier.testTag("local_shop_tab_${seg.lowercase()}")
                        ) {
                            Text(
                                seg,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) SoftWhite else DeepNavy,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
                            )
                        }
                    }
                }
            }

            // 2-Column Product Grid (Image 6 exact cards with Heart, % OFF & Green Add +)
            items(products.chunked(2)) { pair ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    pair.forEach { prod ->
                        val fav = prod.id in wishlistIds
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = SoftWhite,
                            shadowElevation = 3.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onOpenProduct(prod) }
                                .testTag("local_shop_product_${prod.id}")
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(126.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(prod.accentBg),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (PexelsImageService.isConfigured()) {
                                        TakeItSmartPhoto(
                                            query = prod.name,
                                            fallbackDrawableRes = R.drawable.img_takeit_local_shop_hero_1795429839958,
                                            contentDescription = prod.name,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(prod.icon, prod.name, tint = prod.iconTint, modifier = Modifier.size(56.dp))
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(SoftWhite)
                                            .clickable { onToggleWishlist(prod.id) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = if (fav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                            contentDescription = "Wishlist",
                                            tint = if (fav) Color(0xFFDC2626) else DeepNavy,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomStart)
                                            .padding(6.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ShopPurple)
                                            .padding(horizontal = 7.dp, vertical = 2.dp)
                                    ) {
                                        Text("${prod.offPercent}% OFF", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                                    }
                                }

                                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                    Text(prod.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(prod.shopName, style = MaterialTheme.typography.labelSmall, color = MutedText, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("₹${prod.price}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                        Text("₹${prod.mrp}", style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough), color = MutedText)
                                    }
                                    Button(
                                        onClick = { onQuickAdd(prod) },
                                        colors = ButtonDefaults.buttonColors(containerColor = ShopGreenAdd, contentColor = SoftWhite),
                                        shape = RoundedCornerShape(50),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                        modifier = Modifier.height(36.dp).testTag("local_shop_add_${prod.id}")
                                    ) {
                                        Text("Add +", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.ExtraBold)
                                    }
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ShopShortcutCircle(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick).padding(4.dp)
    ) {
        Box(
            modifier = Modifier.size(54.dp).clip(CircleShape).background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = DeepNavy,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun LocalShopCategoriesContent(
    onPickCategory: (String) -> Unit,
    onBack: () -> Unit
) {
    val allCats = remember {
        listOf(
            Triple("Fashion & Wear", "Women", Pair(Icons.Rounded.Checkroom, Color(0xFFFCE7F3))),
            Triple("Electronics", "Electronics", Pair(Icons.Rounded.Headphones, Color(0xFFDBEAFE))),
            Triple("Home & Kitchen", "All", Pair(Icons.Rounded.Chair, Color(0xFFFEF3C7))),
            Triple("Beauty & Care", "Women", Pair(Icons.Rounded.AutoAwesome, Color(0xFFF3E8FF))),
            Triple("Grocery & Daily", "All", Pair(Icons.Rounded.LocalGroceryStore, Color(0xFFDCFCE7))),
            Triple("Sports & Fitness", "Men", Pair(Icons.Rounded.FitnessCenter, Color(0xFFE0F2FE))),
            Triple("Mobile & Tech", "Electronics", Pair(Icons.Rounded.Smartphone, Color(0xFFE0E7FF))),
            Triple("Toys & Games", "Kids", Pair(Icons.Rounded.Toys, Color(0xFFFFEDD5))),
            Triple("Books & Study", "All", Pair(Icons.Rounded.MenuBook, Color(0xFFE0F2FE))),
            Triple("Health & Care", "All", Pair(Icons.Rounded.HealthAndSafety, Color(0xFFDCFCE7))),
            Triple("Automotive", "Men", Pair(Icons.Rounded.DirectionsCar, Color(0xFFFEE2E2))),
            Triple("More Categories", "All", Pair(Icons.Rounded.GridView, Color(0xFFEDE9FE)))
        )
    }

    Column(modifier = Modifier.fillMaxSize().background(ShopBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(SoftWhite).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
            }
            Text("Categories", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(48.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(allCats.chunked(3)) { row ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { (title, seg, iconPair) ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = iconPair.second,
                            modifier = Modifier.weight(1f).clickable { onPickCategory(seg) }
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(iconPair.first, title, tint = ShopPurple, modifier = Modifier.size(32.dp))
                                Text(
                                    title,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DeepNavy,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFEF3C7), Color(0xFFDBEAFE))))
                        .padding(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Local Deals Near You", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                            Text("Save up to 70% on nearby shops", style = MaterialTheme.typography.labelMedium, color = MutedText)
                            Spacer(Modifier.height(4.dp))
                            Button(
                                onClick = { onPickCategory("All") },
                                colors = ButtonDefaults.buttonColors(containerColor = ShopGold, contentColor = DeepNavy),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.height(42.dp)
                            ) {
                                Text("Explore Deals →", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                        Icon(Icons.Rounded.LocationOn, null, tint = ShopViolet, modifier = Modifier.size(48.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalShopProductDetailContent(
    product: ShopProductCardModel,
    selectedSize: String,
    selectedColor: String,
    isWishlisted: Boolean,
    onSelectSize: (String) -> Unit,
    onSelectColor: (String) -> Unit,
    onToggleWishlist: () -> Unit,
    onAddToCart: () -> Unit,
    onBuyNow: () -> Unit,
    onBack: () -> Unit
) {
    val swatches = listOf(
        "Blue" to Color(0xFF2563EB),
        "Pink" to Color(0xFFEC4899),
        "Yellow" to Color(0xFFEAB308),
        "Black" to Color(0xFF1E293B)
    )

    Scaffold(
        containerColor = SoftWhite,
        bottomBar = {
            Surface(color = SoftWhite, shadowElevation = 12.dp) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onAddToCart,
                        shape = RoundedCornerShape(50),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, ShopPurple),
                        modifier = Modifier.weight(1f).height(52.dp).testTag("local_shop_detail_add_cart")
                    ) {
                        Icon(Icons.Rounded.ShoppingCart, null, tint = ShopPurple, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Add to Cart", color = ShopPurple, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = onBuyNow,
                        colors = ButtonDefaults.buttonColors(containerColor = ShopViolet, contentColor = SoftWhite),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.weight(1f).height(52.dp).testTag("local_shop_detail_buy_now")
                    ) {
                        Text("Buy Now", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Product Hero Showcase
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(276.dp)
                        .background(Brush.verticalGradient(listOf(product.accentBg, Color(0xFFEFF6FF))))
                        .statusBarsPadding()
                ) {
                    if (PexelsImageService.isConfigured()) {
                        TakeItSmartPhoto(
                            query = product.name,
                            fallbackDrawableRes = R.drawable.img_takeit_local_shop_hero_1795429839958,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            product.icon,
                            product.name,
                            tint = product.iconTint,
                            modifier = Modifier.size(120.dp).align(Alignment.Center)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(onClick = onBack, shape = CircleShape, color = SoftWhite, shadowElevation = 3.dp, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
                            }
                        }
                        Surface(onClick = onToggleWishlist, shape = CircleShape, color = SoftWhite, shadowElevation = 3.dp, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    if (isWishlisted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                                    "Wishlist",
                                    tint = if (isWishlisted) Color(0xFFDC2626) else DeepNavy
                                )
                            }
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = ShopViolet,
                        modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
                    ) {
                        Text(
                            "${product.offPercent}% OFF",
                            style = MaterialTheme.typography.labelLarge,
                            color = SoftWhite,
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Product Title, Rating, Trusted Seller & Price
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(product.name, style = MaterialTheme.typography.headlineSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    Text("${product.subtitle} • ${product.shopName}", style = MaterialTheme.typography.bodyMedium, color = MutedText)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("★ ${product.rating} (${product.reviews} reviews)", style = MaterialTheme.typography.labelLarge, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                        Surface(shape = RoundedCornerShape(50), color = Color(0xFFDCFCE7)) {
                            Row(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Rounded.Verified, null, tint = ShopGreenAdd, modifier = Modifier.size(14.dp))
                                Text("Trusted Seller", style = MaterialTheme.typography.labelSmall, color = Color(0xFF166534), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("₹${product.price}", style = MaterialTheme.typography.headlineMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        Text("₹${product.mrp}", style = MaterialTheme.typography.titleMedium.copy(textDecoration = TextDecoration.LineThrough), color = MutedText)
                    }
                }
            }

            // Available Sizes Selector
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Available Sizes", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        product.sizes.forEach { sz ->
                            val active = selectedSize == sz
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 50.dp)
                                    .height(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (active) Color(0xFFEDE9FE) else SoftWhite)
                                    .border(if (active) 2.dp else 1.dp, if (active) ShopViolet else Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                                    .clickable { onSelectSize(sz) }
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(sz, style = MaterialTheme.typography.labelLarge, color = if (active) ShopPurple else DeepNavy, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            // Color Swatches
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Color: $selectedColor", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        swatches.forEach { (name, color) ->
                            val active = selectedColor == name
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(color.copy(alpha = 0.16f))
                                    .border(if (active) 2.dp else 1.dp, if (active) ShopViolet else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
                                    .clickable { onSelectColor(name) },
                                contentAlignment = Alignment.Center
                            ) {
                                Box(modifier = Modifier.size(22.dp).clip(CircleShape).background(color))
                            }
                        }
                    }
                }
            }

            // Product Highlights
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Product Highlights", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    listOf(
                        "Lightweight & Comfortable everyday build",
                        "100% Authentic from Verified Local Store",
                        "Instant 30-min doorstep delivery & Easy Exchange"
                    ).forEach { hl ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Rounded.CheckCircle, null, tint = ShopGreenAdd, modifier = Modifier.size(18.dp))
                            Text(hl, style = MaterialTheme.typography.bodyMedium, color = DeepNavy)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocalShopTrackOrderContent(
    productName: String,
    onBackHome: () -> Unit
) {
    val steps = listOf("Order\nPlaced", "Packed", "Out for\nDelivery", "Delivered")
    var currentStep by remember { mutableIntStateOf(2) }

    Column(modifier = Modifier.fillMaxSize().background(ShopBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(SoftWhite).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackHome, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = DeepNavy)
            }
            Text("Track Order", style = MaterialTheme.typography.titleLarge, color = DeepNavy, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.HeadsetMic, "Support", tint = DeepNavy)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Purple Courier Banner
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.verticalGradient(listOf(Color(0xFFEDE9FE), ShopPurple)))
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(145.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.img_takeit_local_shop_hero_1795429839958),
                            contentDescription = "Local Shop Delivery",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Brush.horizontalGradient(listOf(ShopPurple, ShopViolet)))
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Your order is on the way!", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                            Text("Arriving in 28 min", style = MaterialTheme.typography.titleLarge, color = ShopGold, fontWeight = FontWeight.ExtraBold)
                            Text(productName, style = MaterialTheme.typography.labelSmall, color = SoftWhite.copy(alpha = 0.9f))
                        }
                        Surface(shape = RoundedCornerShape(50), color = SoftWhite) {
                            Text(
                                "Live Tracking",
                                style = MaterialTheme.typography.labelMedium,
                                color = ShopPurple,
                                fontWeight = FontWeight.ExtraBold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                            )
                        }
                    }
                }
            }

            // 4-Step Blue Check Timeline
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentStep = (currentStep + 1) % 4 }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        steps.forEachIndexed { idx, label ->
                            val active = idx <= currentStep
                            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier.size(28.dp).clip(CircleShape).background(if (active) ShopRoyalBlue else Color(0xFFCBD5E1)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Rounded.Check, label, tint = SoftWhite, modifier = Modifier.size(16.dp))
                                }
                                Text(label, style = MaterialTheme.typography.labelSmall, color = if (active) DeepNavy else MutedText, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 14.sp)
                            }
                        }
                    }
                }
            }

            // Delivery Partner Card: Sandeep Kumar ★ 4.8
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 3.dp) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Delivery Partner", style = MaterialTheme.typography.titleSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Character3DPortrait(
                                drawableRes = R.drawable.img_char_ride_captain_1795426023899,
                                contentDescription = "Sandeep Kumar",
                                borderColor = ShopViolet,
                                size = 54.dp,
                                bobbing = false
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sandeep Kumar", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                                Text("★ 4.8 (2.5k deliveries)", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                            }
                        }
                        val context = LocalContext.current
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { DeviceActions.dialPhoneNumber(context, "+919811223344") },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Rounded.Call, null, tint = ShopRoyalBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Call", color = DeepNavy, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    DeviceActions.shareText(
                                        context,
                                        "Take It Local Shop Delivery",
                                        "My $productName order from Take It Local Shop is out for delivery with Sandeep Kumar!"
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Icon(Icons.Rounded.Share, null, tint = ShopRoyalBlue, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share", color = DeepNavy, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onBackHome,
                    colors = ButtonDefaults.buttonColors(containerColor = ShopViolet, contentColor = SoftWhite),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(54.dp).navigationBarsPadding()
                ) {
                    Text("Continue Shopping", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
