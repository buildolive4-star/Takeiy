package com.example.ui.screens.customer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.data.GroceryProductEntity
import com.example.model.CartEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroceryScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onAddToCart: (CartEntry) -> Unit,
    onUpdateQty: (String, Int) -> Unit,
    onOpenCart: () -> Unit
) {
    val categories = listOf(
        "All", "Fruits", "Vegetables", "Dairy", "Snacks", "Drinks",
        "Bakery", "Personal Care", "Household Cleaning", "Baby Care", "Daily Essentials"
    )
    var selectedCategory by remember { mutableStateOf("All") }
    var search by remember { mutableStateOf("") }
    var detailProduct by remember { mutableStateOf<GroceryProductEntity?>(null) }

    val products = remember(uiState.groceryProducts, selectedCategory, search) {
        uiState.groceryProducts.filter { p ->
            val matchCat = selectedCategory == "All" || p.category.equals(selectedCategory, true)
            val matchSearch = search.isBlank() || p.name.contains(search, true) || p.category.contains(search, true)
            matchCat && matchSearch
        }
    }

    Scaffold(
        containerColor = LightSkyBackground,
        topBar = {
            ZiporaSubTopBar(
                title = "Zipora Instant Grocery",
                subtitle = "Delivery in 9 minutes • ${uiState.appState.activeAddressLabel}",
                onBack = onBack
            )
        },
        floatingActionButton = {
            if (uiState.cartItemCount > 0) {
                ExtendedFloatingActionButton(
                    onClick = onOpenCart,
                    containerColor = SuccessGreen,
                    contentColor = SoftWhite,
                    modifier = Modifier.navigationBarsPadding().testTag("grocery_view_cart_fab")
                ) {
                    Icon(Icons.Rounded.ShoppingBasket, null)
                    Spacer(Modifier.width(8.dp))
                    Text("${uiState.cartItemCount} items • ₹${uiState.cartGrandTotal}", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Instant Banner
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF065F46), SuccessGreen)))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier.clip(RoundedCornerShape(50)).background(PremiumYellow).padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text("⚡ 9 MIN EXPRESS", style = MaterialTheme.typography.labelSmall, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Text("Farm Fresh to Your Door", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        Text("Use BLINK100 for ₹100 OFF above ₹499", style = MaterialTheme.typography.bodySmall, color = SoftWhite.copy(alpha = 0.9f))
                    }
                    Image(
                        painter = painterResource(R.drawable.img_grocery_basket_1795421415559),
                        contentDescription = "Instant Grocery Basket",
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(16.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            // Search
            item {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Search milk, strawberries, bread, oil...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null, tint = SuccessGreen) },
                    shape = RoundedCornerShape(18.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = SoftWhite,
                        unfocusedContainerColor = SoftWhite,
                        focusedBorderColor = SuccessGreen,
                        unfocusedBorderColor = LightBorder
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("grocery_search_input")
                )
            }

            // 10 Category Pills
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

            // 2-Column Product Grid
            items(products.chunked(2)) { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowItems.forEach { product ->
                        val inCartQty = uiState.cart.filter { it.itemId == product.id }.sumOf { it.quantity }
                        GroceryProductCard(
                            product = product,
                            qty = inCartQty,
                            modifier = Modifier.weight(1f),
                            onOpenDetail = { detailProduct = product },
                            onAdd = {
                                onAddToCart(
                                    CartEntry(
                                        itemId = product.id,
                                        name = product.name,
                                        sourceName = "Zipora Instant Mart",
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
    }

    // Product Detail Sheet with Replacement Options & Inventory Status
    detailProduct?.let { prod ->
        ModalBottomSheet(
            onDismissRequest = { detailProduct = null },
            containerColor = SoftWhite
        ) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(R.drawable.img_grocery_basket_1795421415559),
                        contentDescription = prod.name,
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(18.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(Modifier.width(14.dp))
                    Column {
                        StatusPill(if (prod.stockCount > 5) "IN STOCK (${prod.stockCount})" else "LOW STOCK")
                        Spacer(Modifier.height(4.dp))
                        Text(prod.name, style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                        Text("${prod.unit} • ${prod.category} • ${prod.deliveryMins} mins", style = MaterialTheme.typography.bodySmall, color = MutedText)
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("₹${prod.sellingPrice}", style = MaterialTheme.typography.headlineMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    Text("₹${prod.mrp}", style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough), color = MutedText)
                    val pct = (((prod.mrp - prod.sellingPrice).toFloat() / prod.mrp.coerceAtLeast(1)) * 100).toInt()
                    Text("$pct% OFF", style = MaterialTheme.typography.labelMedium, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
                Surface(shape = RoundedCornerShape(14.dp), color = GroceryCardStart) {
                    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.VerifiedUser, null, tint = SuccessGreen)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "Instant Replacement Guarantee: Damaged or unripe? Instant free replacement within 2 hours.",
                            style = MaterialTheme.typography.bodySmall,
                            color = DeepNavy
                        )
                    }
                }
                Button(
                    onClick = {
                        onAddToCart(
                            CartEntry(prod.id, prod.name, "Zipora Instant Mart", "GROCERY", prod.sellingPrice, prod.mrp, 1)
                        )
                        detailProduct = null
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Add to Instant Basket • ₹${prod.sellingPrice}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GroceryProductCard(
    product: GroceryProductEntity,
    qty: Int,
    modifier: Modifier = Modifier,
    onOpenDetail: () -> Unit,
    onAdd: () -> Unit,
    onQtyChange: (Int) -> Unit
) {
    val discountPct = (((product.mrp - product.sellingPrice).toFloat() / product.mrp.coerceAtLeast(1)) * 100).toInt().coerceAtLeast(0)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SoftWhite,
        shadowElevation = 3.dp,
        modifier = modifier
            .clickable(onClick = onOpenDetail)
            .testTag("grocery_card_${product.id}")
    ) {
        Column(modifier = Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(GroceryCardStart)
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
                            .background(SuccessGreen)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("$discountPct% OFF", style = MaterialTheme.typography.labelSmall, color = SoftWhite, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Text("⚡ ${product.deliveryMins} MINS", style = MaterialTheme.typography.labelSmall, color = SkyBlueDark)
            Text(product.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy, maxLines = 2, overflow = TextOverflow.Ellipsis, minLines = 2)
            Text("${product.unit} • Stock: ${product.stockCount}", style = MaterialTheme.typography.bodySmall, color = MutedText)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("₹${product.sellingPrice}", style = MaterialTheme.typography.titleMedium, color = DeepNavy, fontWeight = FontWeight.ExtraBold)
                    Text("₹${product.mrp}", style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough), color = MutedText)
                }
                if (product.stockCount <= 0) {
                    Text("Sold Out", style = MaterialTheme.typography.labelSmall, color = CoralDanger)
                } else if (qty > 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(SuccessGreen)
                    ) {
                        IconButton(onClick = { onQtyChange(-1) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Remove, "Less", tint = SoftWhite, modifier = Modifier.size(14.dp))
                        }
                        Text("$qty", style = MaterialTheme.typography.labelLarge, color = SoftWhite)
                        IconButton(onClick = { onQtyChange(1) }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Rounded.Add, "More", tint = SoftWhite, modifier = Modifier.size(14.dp))
                        }
                    }
                } else {
                    Button(
                        onClick = onAdd,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(34.dp).testTag("grocery_add_${product.id}")
                    ) {
                        Text("ADD", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}
