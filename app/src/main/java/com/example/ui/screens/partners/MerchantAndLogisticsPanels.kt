package com.example.ui.screens.partners

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.DemoCatalog
import com.example.model.AppPanel
import com.example.model.AppRoute
import com.example.network.ImgBbUploadService
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.SuperAppUiState
import kotlinx.coroutines.launch

@Composable
fun RestaurantOwnerPanelScreen(
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onUpdateOrderStatus: (String, String) -> Unit,
    onAddOrUpdateDish: (id: String?, restaurantId: String, category: String, name: String, desc: String, price: Int, isVeg: Boolean, imageUrl: String?) -> Unit,
    onToggleDishAvailable: (String, Boolean) -> Unit,
    onShowMessage: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) } // 0 Orders, 1 Menu, 2 Settlements
    var showAddDishDialog by remember { mutableStateOf(false) }
    var prepTimeMins by remember { mutableIntStateOf(18) }

    val foodOrders = remember(uiState.orders) { uiState.orders.filter { it.type == "FOOD" } }
    val restaurantDishes = remember(uiState.menuItems) { uiState.menuItems.filter { it.restaurantId == "r1" } }
    val totalSales = foodOrders.sumOf { it.amount }.toInt() + 18420

    Scaffold(
        containerColor = LightSkyBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            if (tab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddDishDialog = true },
                    containerColor = DeepNavy,
                    contentColor = PremiumYellow,
                    modifier = Modifier.navigationBarsPadding().testTag("restaurant_add_dish_fab")
                ) {
                    Icon(Icons.Rounded.Add, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Add Menu Dish", fontWeight = FontWeight.Bold)
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF7C2D12))))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SoftWhite.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Rounded.Menu, "Switch Panel", tint = PremiumYellow)
                        }
                        Spacer(Modifier.width(10.dp))
                        Character3DPortrait(
                            drawableRes = R.drawable.img_char_food_rider_1795426023084,
                            contentDescription = "Restaurant Kitchen Partner",
                            borderColor = PremiumYellow,
                            size = 46.dp,
                            bobbing = false
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("MERCHANT CONSOLE", style = MaterialTheme.typography.labelSmall, color = PremiumYellow)
                            Text("Artisan Crust Pizzeria", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        }
                        StatusPill("ONLINE")
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkKpiTile("Today Sales", "₹$totalSales", PremiumYellow, Modifier.weight(1f))
                        DarkKpiTile("Orders", "${foodOrders.size + 42}", SkyBlue, Modifier.weight(1f))
                        DarkKpiTile("Pending", "${foodOrders.count { it.status == "PENDING" || it.status == "PREPARING" }}", OrangeAccent, Modifier.weight(1f))
                        DarkKpiTile("Rating", "4.7★", SuccessGreen, Modifier.weight(1f))
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Live Kitchen", "Menu (${restaurantDishes.size})", "Settlements").forEachIndexed { i, title ->
                            FilterChip(selected = tab == i, onClick = { tab = i }, label = { Text(title) })
                        }
                    }
                }
            }

            if (tab == 0) {
                item {
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Default Kitchen Prep Time", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                Text("Live ETA shown to customers", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { if (prepTimeMins > 10) prepTimeMins -= 2 }) { Icon(Icons.Rounded.RemoveCircleOutline, null, tint = DeepNavy) }
                                Text("$prepTimeMins m", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                IconButton(onClick = { prepTimeMins += 2 }) { Icon(Icons.Rounded.AddCircleOutline, null, tint = DeepNavy) }
                            }
                        }
                    }
                }
                items(foodOrders, key = { it.id }) { order ->
                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${order.id} • ₹${order.amount.toInt()}", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                StatusPill(order.status)
                            }
                            Text(order.subtitle, style = MaterialTheme.typography.bodySmall, color = SlateText)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (order.status == "PENDING") {
                                    OutlinedButton(onClick = { onUpdateOrderStatus(order.id, "CANCELLED") }, modifier = Modifier.weight(1f)) { Text("Reject", color = CoralDanger) }
                                    Button(onClick = { onUpdateOrderStatus(order.id, "PREPARING") }, modifier = Modifier.weight(1.3f), colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)) { Text("Accept ($prepTimeMins m)") }
                                } else if (order.status == "PREPARING") {
                                    Button(onClick = { onUpdateOrderStatus(order.id, "ON_THE_WAY") }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)) {
                                        Text("Mark Ready for Pickup")
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (tab == 1) {
                items(restaurantDishes, key = { it.id }) { dish ->
                    val imgUrl = uiState.dishImageUrls[dish.id]
                    Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!imgUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = dish.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(48.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(dish.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                Text("${dish.category} • ₹${dish.price} • ${if (dish.isVeg) "Veg" else "Non-Veg"}", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                            Switch(checked = dish.isAvailable, onCheckedChange = { onToggleDishAvailable(dish.id, it) })
                        }
                    }
                }
            } else {
                item {
                    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Commission & Settlements", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                Text("Gross Weekly Sales: ₹1,14,800\nPlatform Commission (${uiState.adminConfig.platformCommissionPct}%): -₹${(114800 * uiState.adminConfig.platformCommissionPct) / 100}\nNet Payable Settlement: ₹${114800 - (114800 * uiState.adminConfig.platformCommissionPct) / 100}", style = MaterialTheme.typography.bodyMedium, color = SlateText)
                                Button(onClick = { onShowMessage("Settlement PDF exported") }, colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)) {
                                    Text("Download Statement")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDishDialog) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var priceText by remember { mutableStateOf("320") }
        var category by remember { mutableStateOf("Wood-Fired Pizza") }
        var isVeg by remember { mutableStateOf(true) }
        var uploadedPhotoUrl by remember { mutableStateOf<String?>(null) }
        var isUploadingPhoto by remember { mutableStateOf(false) }

        val dishPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    isUploadingPhoto = true
                    val res = ImgBbUploadService.uploadImageUri(context, uri, "dish")
                    isUploadingPhoto = false
                    res.onSuccess { url ->
                        uploadedPhotoUrl = url
                        onShowMessage("Dish photo uploaded to ImgBB!")
                    }.onFailure { err ->
                        onShowMessage(err.localizedMessage ?: "ImgBB upload failed")
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDishDialog = false },
            containerColor = SoftWhite,
            title = { Text("Add New Dish to Live Menu", color = DeepNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Name") }, modifier = Modifier.fillMaxWidth().testTag("new_dish_name_input"))
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = priceText, onValueChange = { priceText = it }, label = { Text("Price (₹)") }, modifier = Modifier.fillMaxWidth())
                    if (!uploadedPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = uploadedPhotoUrl,
                            contentDescription = "Uploaded dish preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                    OutlinedButton(
                        onClick = { dishPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploadingPhoto,
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("dish_imgbb_upload_button")
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Uploading to ImgBB...")
                        } else {
                            Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (uploadedPhotoUrl != null) "Change Dish Photo (ImgBB)" else "Upload Dish Photo (ImgBB)")
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isVeg, onCheckedChange = { isVeg = it })
                        Text("Vegetarian Dish", color = DeepNavy)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddOrUpdateDish(null, "r1", category, name, "Chef signature dish added via Merchant Panel", priceText.toIntOrNull() ?: 299, isVeg, uploadedPhotoUrl)
                            showAddDishDialog = false
                        }
                    },
                    enabled = !isUploadingPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)
                ) { Text("Publish Live") }
            },
            dismissButton = { TextButton(onClick = { showAddDishDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun GroceryOwnerPanelScreen(
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onUpdateOrderStatus: (String, String) -> Unit,
    onAdjustStock: (String, Int, Int) -> Unit,
    onAddProduct: (id: String?, name: String, category: String, unit: String, mrp: Int, price: Int, stock: Int, imageUrl: String?) -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    var tab by remember { mutableIntStateOf(0) } // 0 Inventory, 1 Packing Orders
    var showAddDialog by remember { mutableStateOf(false) }
    val groceryOrders = remember(uiState.orders) { uiState.orders.filter { it.type == "GROCERY" } }

    Scaffold(
        containerColor = LightSkyBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = SuccessGreen,
                contentColor = SoftWhite,
                modifier = Modifier.navigationBarsPadding().testTag("grocery_owner_add_fab")
            ) {
                Icon(Icons.Rounded.Add, null)
                Spacer(Modifier.width(6.dp))
                Text("Add SKU", fontWeight = FontWeight.Bold)
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 90.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF065F46))))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onOpenDrawer,
                            modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SoftWhite.copy(alpha = 0.12f))
                        ) { Icon(Icons.Rounded.Menu, "Switch Panel", tint = PremiumYellow) }
                        Spacer(Modifier.width(10.dp))
                        Character3DPortrait(
                            drawableRes = R.drawable.img_char_grocery_buddy_1795426023736,
                            contentDescription = "3D Grocery Store Partner",
                            borderColor = SuccessGreen,
                            size = 46.dp,
                            bobbing = false
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("DARK STORE HUB", style = MaterialTheme.typography.labelSmall, color = SuccessGreen)
                            Text("FreshMart Grocery Store", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        }
                        StatusPill("ONLINE")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkKpiTile("Live SKUs", "${uiState.groceryProducts.size}", SuccessGreen, Modifier.weight(1f))
                        DarkKpiTile("Revenue", "₹34,280", PremiumYellow, Modifier.weight(1f))
                        DarkKpiTile("Avg Pack", "2.4 min", SkyBlue, Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("Inventory & Pricing") })
                        FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("Orders (${groceryOrders.size})") })
                    }
                }
            }

            if (tab == 0) {
                items(uiState.groceryProducts, key = { it.id }) { prod ->
                    val imgUrl = uiState.groceryImageUrls[prod.id]
                    Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            if (!imgUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = prod.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(46.dp).clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(Modifier.width(10.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prod.name, style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                Text("${prod.category} • ₹${prod.sellingPrice} (MRP ₹${prod.mrp})", style = MaterialTheme.typography.bodySmall, color = MutedText)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onAdjustStock(prod.id, (prod.stockCount - 5).coerceAtLeast(0), prod.sellingPrice) }) {
                                    Icon(Icons.Rounded.RemoveCircleOutline, "Decrease Stock", tint = CoralDanger)
                                }
                                Text("${prod.stockCount}", style = MaterialTheme.typography.titleSmall, color = DeepNavy)
                                IconButton(onClick = { onAdjustStock(prod.id, prod.stockCount + 5, prod.sellingPrice) }) {
                                    Icon(Icons.Rounded.AddCircleOutline, "Restock", tint = SuccessGreen)
                                }
                            }
                        }
                    }
                }
            } else {
                items(groceryOrders, key = { it.id }) { order ->
                    Surface(shape = RoundedCornerShape(16.dp), color = SoftWhite, modifier = Modifier.padding(horizontal = 16.dp)) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${order.id} • ₹${order.amount.toInt()}", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                                StatusPill(order.status)
                            }
                            Text(order.subtitle, style = MaterialTheme.typography.bodySmall, color = SlateText)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onUpdateOrderStatus(order.id, "ON_THE_WAY") }, modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow)) {
                                    Text("Pack & Assign Rider")
                                }
                                OutlinedButton(onClick = { onUpdateOrderStatus(order.id, "COMPLETED") }, modifier = Modifier.weight(1f)) {
                                    Text("Mark Delivered")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Fruits") }
        var price by remember { mutableStateOf("129") }
        var mrp by remember { mutableStateOf("165") }
        var uploadedPhotoUrl by remember { mutableStateOf<String?>(null) }
        var isUploadingPhoto by remember { mutableStateOf(false) }

        val prodPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                scope.launch {
                    isUploadingPhoto = true
                    val res = ImgBbUploadService.uploadImageUri(context, uri, "grocery")
                    isUploadingPhoto = false
                    res.onSuccess { url ->
                        uploadedPhotoUrl = url
                        onShowMessage("Product photo uploaded to ImgBB!")
                    }.onFailure { err ->
                        onShowMessage(err.localizedMessage ?: "ImgBB upload failed")
                    }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = SoftWhite,
            title = { Text("Add Grocery SKU", color = DeepNavy) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = mrp, onValueChange = { mrp = it }, label = { Text("MRP ₹") }, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Selling ₹") }, modifier = Modifier.weight(1f))
                    }
                    if (!uploadedPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = uploadedPhotoUrl,
                            contentDescription = "Uploaded product preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().height(90.dp).clip(RoundedCornerShape(12.dp))
                        )
                    }
                    OutlinedButton(
                        onClick = { prodPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploadingPhoto,
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("grocery_imgbb_upload_button")
                    ) {
                        if (isUploadingPhoto) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Uploading to ImgBB...")
                        } else {
                            Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (uploadedPhotoUrl != null) "Change Photo (ImgBB)" else "Upload Product Photo (ImgBB)")
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            onAddProduct(null, name, category, "1 Pack", mrp.toIntOrNull() ?: 150, price.toIntOrNull() ?: 120, 40, uploadedPhotoUrl)
                            showAddDialog = false
                        }
                    },
                    enabled = !isUploadingPhoto,
                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite)
                ) { Text("Add to Store") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancel") } }
        )
    }
}

@Composable
fun ParcelTransportPartnerPanelScreen(
    panel: AppPanel,
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onNextJob: (Int) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val isTransport = panel == AppPanel.TRANSPORT_PARTNER
    val accent = if (isTransport) VioletAccent else TealAccent
    val job = DemoCatalog.incomingParcelAndTransport[uiState.activeParcelJobIndex % DemoCatalog.incomingParcelAndTransport.size]
    var accepted by remember(uiState.activeParcelJobIndex) { mutableStateOf(false) }

    Scaffold(
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.35f), DeepNavy)))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SoftWhite.copy(alpha = 0.12f))) {
                            Icon(Icons.Rounded.Menu, "Switch Panel", tint = PremiumYellow)
                        }
                        Spacer(Modifier.width(10.dp))
                        Character3DPortrait(
                            drawableRes = R.drawable.img_char_porter_mover_1795426024294,
                            contentDescription = "3D Logistics Partner",
                            borderColor = PremiumYellow,
                            size = 46.dp,
                            bobbing = false
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (isTransport) "HEAVY FLEET LOGISTICS" else "EXPRESS COURIER HUB", style = MaterialTheme.typography.labelSmall, color = accent)
                            Text(panel.title, style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        }
                        StatusPill("VERIFIED")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkKpiTile("Vehicle", if (isTransport) "Tata Ace" else "EV Scooter", accent, Modifier.weight(1f))
                        DarkKpiTile("Earnings", if (isTransport) "₹3,840" else "₹1,290", PremiumYellow, Modifier.weight(1f))
                        DarkKpiTile("Docs", "Active", SuccessGreen, Modifier.weight(1f))
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        pickupLabel = job.pickupTitle,
                        dropLabel = job.dropTitle,
                        distanceKm = job.tripDistKm,
                        etaMinutes = 24,
                        isDarkStyle = true
                    )

                    Surface(shape = RoundedCornerShape(22.dp), color = DarkCardSurface, modifier = Modifier.border(1.2.dp, accent, RoundedCornerShape(22.dp))) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${job.serviceType} • ${job.id}", style = MaterialTheme.typography.titleMedium, color = SoftWhite)
                                Text("₹${job.partnerEarnings}", style = MaterialTheme.typography.titleLarge, color = SuccessGreen, fontWeight = FontWeight.ExtraBold)
                            }
                            Text("Customer: ${job.customerName} • ${job.paymentType}", style = MaterialTheme.typography.bodySmall, color = SkyBlue)
                            Text("From: ${job.pickupTitle}\nTo: ${job.dropTitle} (${job.tripDistKm} km)", style = MaterialTheme.typography.bodySmall, color = Color(0xFFB8D4EB))
                            if (!accepted) {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(onClick = { onNextJob(0) }, modifier = Modifier.weight(1f)) { Text("Next Job", color = SkyBlue) }
                                    Button(onClick = { accepted = true; onShowMessage("Booking ${job.id} accepted!") }, modifier = Modifier.weight(1.4f), colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = SoftWhite)) {
                                        Text("Accept Load", fontWeight = FontWeight.Bold)
                                    }
                                }
                            } else {
                                Button(
                                    onClick = {
                                        accepted = false
                                        onNextJob(job.partnerEarnings)
                                        onShowMessage("Delivery completed with OTP ${job.otp}! +₹${job.partnerEarnings} credited.")
                                    },
                                    modifier = Modifier.fillMaxWidth().height(48.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = SoftWhite)
                                ) { Text("Complete & Capture Proof (OTP ${job.otp})", fontWeight = FontWeight.Bold) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BusinessOwnerPanelScreen(
    uiState: SuperAppUiState,
    onOpenDrawer: () -> Unit,
    onNavigate: (AppRoute) -> Unit,
    onShowMessage: (String) -> Unit
) {
    Scaffold(
        containerColor = LightSkyBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(bottom = 42.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.linearGradient(listOf(DeepNavy, Color(0xFF831843))))
                        .statusBarsPadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onOpenDrawer, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(14.dp)).background(SoftWhite.copy(alpha = 0.12f))) {
                            Icon(Icons.Rounded.Menu, "Switch Panel", tint = PremiumYellow)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ZIPORA FOR BUSINESS", style = MaterialTheme.typography.labelSmall, color = PinkAccent)
                            Text("Business Owner Panel", style = MaterialTheme.typography.titleLarge, color = SoftWhite)
                        }
                        StatusPill("ENTERPRISE")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DarkKpiTile("Monthly GMV", "₹4.82L", PremiumYellow, Modifier.weight(1f))
                        DarkKpiTile("Active Outlets", "3", SkyBlue, Modifier.weight(1f))
                        DarkKpiTile("B2B Fleet", "12 Runs", SuccessGreen, Modifier.weight(1f))
                    }
                }
            }

            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Enterprise Quick Dispatch", style = MaterialTheme.typography.titleLarge, color = DeepNavy)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onNavigate(AppRoute.GoodsTransport) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = DeepNavy, contentColor = PremiumYellow),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Book B2B Truck", fontWeight = FontWeight.Bold) }
                        Button(
                            onClick = { onNavigate(AppRoute.ParcelDelivery) },
                            modifier = Modifier.weight(1f).height(52.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = DeepNavy),
                            shape = RoundedCornerShape(14.dp)
                        ) { Text("Bulk Parcel", fontWeight = FontWeight.Bold) }
                    }

                    Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Connected Brand Outlets", style = MaterialTheme.typography.titleMedium, color = DeepNavy)
                            Text("• Artisan Crust Pizzeria (Indiranagar) — ★ 4.7\n• FreshMart Instant Store (100ft Rd) — ★ 4.85\n• Urban Loom Retail Hub (12th Main) — ★ 4.6", style = MaterialTheme.typography.bodySmall, color = SlateText)
                            OutlinedButton(onClick = { onShowMessage("GST Invoice Pack downloaded for Q4 (Simulated)") }, modifier = Modifier.fillMaxWidth()) {
                                Text("Export GST & Settlement Pack")
                            }
                        }
                    }
                }
            }
        }
    }
}
