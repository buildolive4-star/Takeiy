package com.example.takeit.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.customer.FoodPhoto
import com.example.takeit.ui.customer.VegMarkIcon
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel
import kotlin.random.Random

// ---------------------------------------------------------------------------
// 1. RESTAURANTS & MENU CATALOG TAB (with Android Photo Picker + ImgBB Cloud)
// ---------------------------------------------------------------------------

@Composable
fun AdminCatalogTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    var section by remember { mutableIntStateOf(0) } // 0 = Restaurants, 1 = Menu Dishes
    var editingRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var showRestaurantDialog by remember { mutableStateOf(false) }
    var editingDish by remember { mutableStateOf<MenuItem?>(null) }
    var showDishDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = section == 0,
                onClick = { section = 0 },
                label = { Text("Restaurants (${uiState.restaurants.size})", fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AdminGold,
                    selectedLabelColor = AdminBg,
                    containerColor = AdminSurface,
                    labelColor = AdminInk
                )
            )
            FilterChip(
                selected = section == 1,
                onClick = { section = 1 },
                label = { Text("Dishes (${uiState.menuItems.size})", fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AdminGold,
                    selectedLabelColor = AdminBg,
                    containerColor = AdminSurface,
                    labelColor = AdminInk
                )
            )
            Spacer(Modifier.weight(1f))
            FilledTonalButton(
                onClick = {
                    if (section == 0) {
                        editingRestaurant = null
                        showRestaurantDialog = true
                    } else {
                        if (uiState.restaurants.isEmpty()) {
                            viewModel.showMessage("Add a restaurant first before adding dishes.")
                        } else {
                            editingDish = null
                            showDishDialog = true
                        }
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(containerColor = AdminGold, contentColor = AdminBg),
                modifier = Modifier.testTag("admin_catalog_add_btn")
            ) {
                Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text(if (section == 0) "Add" else "Dish", fontWeight = FontWeight.ExtraBold)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (section == 0) {
                items(uiState.restaurants, key = { it.id }) { r ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AdminSurface,
                        border = BorderStroke(1.dp, AdminBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FoodPhoto(
                                    imageUrl = r.imageUrl,
                                    fallbackKey = r.category.ifBlank { r.name },
                                    contentDescription = r.name,
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(r.name, color = AdminInk, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${r.category} • ${r.cuisine}", style = MaterialTheme.typography.bodySmall, color = AdminMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("₹${r.priceForTwo} for two • ${r.deliveryTime} • ${r.offerTag}", style = MaterialTheme.typography.labelSmall, color = AdminGold)
                                }
                                Switch(
                                    checked = r.openFlag,
                                    onCheckedChange = { viewModel.toggleRestaurantOpen(r.id, it) }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingRestaurant = r
                                        showRestaurantDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AdminBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Edit, null, tint = AdminCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit & Photo", color = AdminCyan)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.deleteRestaurant(r.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, null, tint = AdminCrimson, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.menuItems, key = { it.id }) { dish ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AdminSurface,
                        border = BorderStroke(1.dp, AdminBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                FoodPhoto(
                                    imageUrl = dish.imageUrl,
                                    fallbackKey = dish.name,
                                    contentDescription = dish.name,
                                    modifier = Modifier
                                        .size(62.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                )
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        VegMarkIcon(isVeg = dish.vegFlag)
                                        Text(dish.name, color = AdminInk, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Text(dish.restaurantName, style = MaterialTheme.typography.labelSmall, color = AdminCyan)
                                    Text("₹${dish.price} • ${if (dish.availableFlag) "In Stock" else "Sold Out"}", style = MaterialTheme.typography.bodySmall, color = if (dish.availableFlag) AdminEmerald else AdminCrimson, fontWeight = FontWeight.Bold)
                                }
                                Switch(
                                    checked = dish.availableFlag,
                                    onCheckedChange = { viewModel.toggleMenuItemAvailable(dish.id, it) }
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingDish = dish
                                        showDishDialog = true
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AdminBorder),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Edit, null, tint = AdminCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit Dish", color = AdminCyan)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.deleteMenuItem(dish.id) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, null, tint = AdminCrimson, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    if (showRestaurantDialog) {
        RestaurantEditorDialog(
            initial = editingRestaurant,
            isUploading = uiState.isUploadingPhoto,
            onUploadImage = { uri, onUrl -> viewModel.uploadImageToImgBb(uri, "takeit_rest", onUrl) },
            onDismiss = { showRestaurantDialog = false },
            onSave = {
                viewModel.saveRestaurant(it)
                showRestaurantDialog = false
            }
        )
    }

    if (showDishDialog) {
        MenuDishEditorDialog(
            initial = editingDish,
            restaurants = uiState.restaurants,
            isUploading = uiState.isUploadingPhoto,
            onUploadImage = { uri, onUrl -> viewModel.uploadImageToImgBb(uri, "takeit_dish", onUrl) },
            onDismiss = { showDishDialog = false },
            onSave = {
                viewModel.saveMenuItem(it)
                showDishDialog = false
            }
        )
    }
}

@Composable
private fun RestaurantEditorDialog(
    initial: Restaurant?,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Restaurant) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var cuisine by remember { mutableStateOf(initial?.cuisine ?: "North Indian, Chinese") }
    var category by remember { mutableStateOf(initial?.category ?: "Pizza") }
    var deliveryTime by remember { mutableStateOf(initial?.deliveryTime ?: "30 min") }
    var priceForTwo by remember { mutableStateOf((initial?.priceForTwo ?: 200).toString()) }
    var offerTag by remember { mutableStateOf(initial?.offerTag ?: "40% OFF") }
    var offerCode by remember { mutableStateOf(initial?.offerCode ?: "FOOD50") }
    var address by remember { mutableStateOf(initial?.address ?: "Greater Noida") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }
    var pureVeg by remember { mutableStateOf(initial?.pureVegFlag ?: false) }
    var freeDelivery by remember { mutableStateOf(initial?.freeDeliveryFlag ?: true) }
    var featured by remember { mutableStateOf(initial?.featuredFlag ?: true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onUploadImage(uri) { uploadedUrl -> imageUrl = uploadedUrl }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text(if (initial == null) "Add Live Restaurant" else "Edit Restaurant", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FoodPhoto(
                        imageUrl = imageUrl,
                        fallbackKey = category,
                        contentDescription = "Restaurant Cover",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    FilledTonalButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        if (isUploading) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Uploading...")
                        } else {
                            Icon(Icons.Rounded.PhotoLibrary, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Upload Photo (ImgBB)")
                        }
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Restaurant Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category (Pizza, Burger, Biryani...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = cuisine, onValueChange = { cuisine = it }, label = { Text("Cuisines") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = deliveryTime, onValueChange = { deliveryTime = it }, label = { Text("ETA") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = priceForTwo, onValueChange = { priceForTwo = it.filter(Char::isDigit) }, label = { Text("₹ For Two") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = offerTag, onValueChange = { offerTag = it }, label = { Text("Offer Badge") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = offerCode, onValueChange = { offerCode = it.uppercase() }, label = { Text("Offer Code") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = address, onValueChange = { address = it }, label = { Text("Address") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Pure Veg", color = AdminInk)
                    Switch(checked = pureVeg, onCheckedChange = { pureVeg = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Free Delivery", color = AdminInk)
                    Switch(checked = freeDelivery, onCheckedChange = { freeDelivery = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Featured on Home", color = AdminInk)
                    Switch(checked = featured, onCheckedChange = { featured = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: Restaurant()).copy(
                                name = name.trim(),
                                cuisine = cuisine.trim(),
                                category = category.trim().ifBlank { "Pizza" },
                                deliveryTime = deliveryTime.trim().ifBlank { "30 min" },
                                priceForTwo = priceForTwo.toIntOrNull() ?: 200,
                                offerTag = offerTag.trim(),
                                offerCode = offerCode.trim().uppercase(),
                                address = address.trim(),
                                imageUrl = imageUrl.trim(),
                                pureVegFlag = pureVeg,
                                freeDeliveryFlag = freeDelivery,
                                featuredFlag = featured
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
            ) { Text("Save Live", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) } }
    )
}

@Composable
private fun MenuDishEditorDialog(
    initial: MenuItem?,
    restaurants: List<Restaurant>,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    var selectedRest by remember {
        mutableStateOf(restaurants.firstOrNull { it.id == initial?.restaurantId } ?: restaurants.first())
    }
    var restMenuExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category ?: "Recommended") }
    var price by remember { mutableStateOf((initial?.price ?: 149).toString()) }
    var originalPrice by remember { mutableStateOf((initial?.originalPrice ?: 199).toString()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }
    var isVeg by remember { mutableStateOf(initial?.vegFlag ?: true) }
    var isBestseller by remember { mutableStateOf(initial?.bestsellerFlag ?: true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onUploadImage(uri) { uploadedUrl -> imageUrl = uploadedUrl }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text(if (initial == null) "Add Menu Dish" else "Edit Menu Dish", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box {
                    OutlinedButton(
                        onClick = { restMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Restaurant: ${selectedRest.name}", color = AdminGold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    DropdownMenu(expanded = restMenuExpanded, onDismissRequest = { restMenuExpanded = false }) {
                        restaurants.forEach { r ->
                            DropdownMenuItem(
                                text = { Text(r.name) },
                                onClick = {
                                    selectedRest = r
                                    restMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FoodPhoto(
                        imageUrl = imageUrl,
                        fallbackKey = name.ifBlank { selectedRest.category },
                        contentDescription = "Dish Photo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                    FilledTonalButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isUploading) "Uploading..." else "Upload Dish Photo")
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Menu Section (Recommended, Combos...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it.filter(Char::isDigit) }, label = { Text("Price ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = originalPrice, onValueChange = { originalPrice = it.filter(Char::isDigit) }, label = { Text("MRP ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Vegetarian Dish", color = AdminInk)
                    Switch(checked = isVeg, onCheckedChange = { isVeg = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text("Bestseller Badge", color = AdminInk)
                    Switch(checked = isBestseller, onCheckedChange = { isBestseller = it })
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: MenuItem()).copy(
                                restaurantId = selectedRest.id,
                                restaurantName = selectedRest.name,
                                name = name.trim(),
                                description = description.trim(),
                                category = category.trim().ifBlank { "Recommended" },
                                price = price.toIntOrNull() ?: 149,
                                originalPrice = originalPrice.toIntOrNull() ?: 199,
                                vegFlag = isVeg,
                                bestsellerFlag = isBestseller,
                                imageUrl = imageUrl.trim()
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
            ) { Text("Save Dish", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) } }
    )
}

// ---------------------------------------------------------------------------
// 2. PROMO COUPONS TAB
// ---------------------------------------------------------------------------

@Composable
fun AdminCouponsTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    var editingCoupon by remember { mutableStateOf<PromoCoupon?>(null) }
    var showDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Live Promo Coupons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AdminInk)
                    Text("Synced instantly to Customer Offers & Cart", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                }
                Button(
                    onClick = {
                        editingCoupon = null
                        showDialog = true
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Code", fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        items(uiState.coupons, key = { it.id }) { coupon ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = AdminSurface,
                border = BorderStroke(1.dp, if (coupon.activeFlag) AdminGold.copy(alpha = 0.5f) else AdminBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF3B2806)) {
                                Text(
                                    coupon.code,
                                    color = AdminGold,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                            Text(
                                if (coupon.discountType == "FLAT") "Flat ₹${coupon.discountValue} OFF" else "${coupon.discountValue}% OFF (max ₹${coupon.maxDiscount})",
                                color = AdminEmerald,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Text(coupon.title, color = AdminInk, fontWeight = FontWeight.SemiBold)
                        Text("Min order ₹${coupon.minOrderAmount} • ${coupon.description}", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Switch(
                            checked = coupon.activeFlag,
                            onCheckedChange = { viewModel.saveCoupon(coupon.copy(activeFlag = it)) }
                        )
                        Row {
                            IconButton(onClick = {
                                editingCoupon = coupon
                                showDialog = true
                            }) { Icon(Icons.Rounded.Edit, "Edit", tint = AdminCyan) }
                            IconButton(onClick = { viewModel.deleteCoupon(coupon.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, "Delete", tint = AdminCrimson)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDialog) {
        var code by remember { mutableStateOf(editingCoupon?.code ?: "TAKEIT50") }
        var title by remember { mutableStateOf(editingCoupon?.title ?: "Flat 50% OFF") }
        var desc by remember { mutableStateOf(editingCoupon?.description ?: "Valid on all food orders") }
        var type by remember { mutableStateOf(editingCoupon?.discountType ?: "PERCENT") }
        var value by remember { mutableStateOf((editingCoupon?.discountValue ?: 50).toString()) }
        var maxDisc by remember { mutableStateOf((editingCoupon?.maxDiscount ?: 120).toString()) }
        var minOrder by remember { mutableStateOf((editingCoupon?.minOrderAmount ?: 149).toString()) }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = AdminSurface,
            titleContentColor = AdminInk,
            title = { Text(if (editingCoupon == null) "Create Live Coupon" else "Edit Coupon", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = code, onValueChange = { code = it.uppercase().replace(" ", "") }, label = { Text("Coupon Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Headline") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("Description") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == "PERCENT", onClick = { type = "PERCENT" }, label = { Text("% Percent") })
                        FilterChip(selected = type == "FLAT", onClick = { type = "FLAT" }, label = { Text("₹ Flat") })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = value, onValueChange = { value = it.filter(Char::isDigit) }, label = { Text(if (type == "FLAT") "Flat ₹" else "Percent %") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = maxDisc, onValueChange = { maxDisc = it.filter(Char::isDigit) }, label = { Text("Max ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = minOrder, onValueChange = { minOrder = it.filter(Char::isDigit) }, label = { Text("Min Order Amount ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (code.isNotBlank()) {
                            viewModel.saveCoupon(
                                (editingCoupon ?: PromoCoupon()).copy(
                                    code = code.trim().uppercase(),
                                    title = title.trim(),
                                    description = desc.trim(),
                                    discountType = type,
                                    discountValue = value.toIntOrNull() ?: 20,
                                    maxDiscount = maxDisc.toIntOrNull() ?: 100,
                                    minOrderAmount = minOrder.toIntOrNull() ?: 99,
                                    activeFlag = true
                                )
                            )
                            showDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
                ) { Text("Publish Live", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("Cancel", color = AdminMuted) } }
        )
    }
}

// ---------------------------------------------------------------------------
// 3. CUSTOMERS & SUPPORT TICKETS TAB
// ---------------------------------------------------------------------------

@Composable
fun AdminPeopleTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    var subTab by remember { mutableIntStateOf(0) } // 0 = Customers, 1 = Support Tickets
    var walletTarget by remember { mutableStateOf<TakeItUserProfile?>(null) }
    var replyTarget by remember { mutableStateOf<SupportTicket?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            FilterChip(
                selected = subTab == 0,
                onClick = { subTab = 0 },
                label = { Text("Customers (${uiState.allUsers.size})", fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminGold, selectedLabelColor = AdminBg, containerColor = AdminSurface, labelColor = AdminInk)
            )
            FilterChip(
                selected = subTab == 1,
                onClick = { subTab = 1 },
                label = { Text("Support Tickets (${uiState.allTickets.size})", fontWeight = FontWeight.Bold) },
                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = AdminGold, selectedLabelColor = AdminBg, containerColor = AdminSurface, labelColor = AdminInk)
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (subTab == 0) {
                items(uiState.allUsers, key = { it.id.ifBlank { it.userId } }) { user ->
                    val uid = user.id.ifBlank { user.userId }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AdminSurface,
                        border = BorderStroke(1.dp, if (user.suspendedFlag) AdminCrimson else AdminBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, color = AdminInk, fontWeight = FontWeight.ExtraBold)
                                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = AdminCyan)
                                    if (user.phone.isNotBlank()) {
                                        Text(user.phone, style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("₹${user.walletBalance.toInt()}", color = AdminEmerald, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                                    Text("Wallet", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = { walletTarget = user },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, AdminGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.CardGiftcard, null, tint = AdminGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Credit Bonus", color = AdminGold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.adminToggleSuspendUser(uid, !user.suspendedFlag) },
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, if (user.suspendedFlag) AdminEmerald else AdminCrimson),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        if (user.suspendedFlag) "Unsuspend" else "Suspend",
                                        color = if (user.suspendedFlag) AdminEmerald else AdminCrimson,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                items(uiState.allTickets, key = { it.id }) { ticket ->
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = AdminSurface,
                        border = BorderStroke(1.dp, AdminBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                                Text(ticket.subject, color = AdminInk, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f))
                                AdminOrderStatusPill(ticket.status)
                            }
                            Text("${ticket.userName} • ${ticket.userEmail}", style = MaterialTheme.typography.labelSmall, color = AdminCyan)
                            Text(ticket.message, style = MaterialTheme.typography.bodySmall, color = AdminMuted)
                            if (ticket.adminReply.isNotBlank()) {
                                Surface(shape = RoundedCornerShape(10.dp), color = AdminCard, modifier = Modifier.fillMaxWidth()) {
                                    Text("Admin Reply: ${ticket.adminReply}", style = MaterialTheme.typography.labelSmall, color = AdminEmerald, modifier = Modifier.padding(10.dp))
                                }
                            }
                            Button(
                                onClick = { replyTarget = ticket },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
                            ) {
                                Text("Reply & Update Status", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }

    walletTarget?.let { target ->
        var amountText by remember { mutableStateOf("100") }
        var reason by remember { mutableStateOf("Take It VIP Bonus") }
        AlertDialog(
            onDismissRequest = { walletTarget = null },
            containerColor = AdminSurface,
            titleContentColor = AdminInk,
            title = { Text("Credit ${target.name}'s Wallet", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it.filter(Char::isDigit) }, label = { Text("Amount (₹)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason / Note") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = amountText.toDoubleOrNull() ?: 0.0
                        if (amt > 0) {
                            viewModel.adminCreditCustomerWallet(target.id.ifBlank { target.userId }, amt, reason.trim())
                            walletTarget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
                ) { Text("Credit Now", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { walletTarget = null }) { Text("Cancel", color = AdminMuted) } }
        )
    }

    replyTarget?.let { ticket ->
        var reply by remember { mutableStateOf(ticket.adminReply) }
        var status by remember { mutableStateOf(if (ticket.status == "OPEN") "RESOLVED" else ticket.status) }
        AlertDialog(
            onDismissRequest = { replyTarget = null },
            containerColor = AdminSurface,
            titleContentColor = AdminInk,
            title = { Text("Reply to ${ticket.userName}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = reply, onValueChange = { reply = it }, label = { Text("Admin Response") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = status == "IN_PROGRESS", onClick = { status = "IN_PROGRESS" }, label = { Text("In Progress") })
                        FilterChip(selected = status == "RESOLVED", onClick = { status = "RESOLVED" }, label = { Text("Resolved") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (reply.isNotBlank()) {
                            viewModel.adminReplyTicket(ticket.id, reply.trim(), status)
                            replyTarget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
                ) { Text("Send Reply", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { replyTarget = null }) { Text("Cancel", color = AdminMuted) } }
        )
    }
}

// ---------------------------------------------------------------------------
// 4. CO-ADMIN PASSWORD & PERMISSION MANAGER (MAIN ADMIN buildolive4@gmail.com ONLY!)
// ---------------------------------------------------------------------------

@Composable
fun AdminCoAdminsTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    val context = LocalContext.current
    var editingCoAdmin by remember { mutableStateOf<CoAdminAccount?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AdminCard,
                border = BorderStroke(1.dp, AdminGold),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.AdminPanelSettings, null, tint = AdminGold, modifier = Modifier.size(24.dp))
                        Text("Main Admin Co-Admin Control", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AdminInk)
                    }
                    Text(
                        "Exclusive to $MAIN_ADMIN_EMAIL. Create Co-Admin accounts with custom passwords & exact permissions. Co-Admins sign in with their Gmail and enter the password you generate here.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminMuted
                    )
                    Button(
                        onClick = {
                            editingCoAdmin = null
                            showCreateDialog = true
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("admin_create_coadmin_btn")
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create New Co-Admin & Password", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        if (uiState.coAdmins.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AdminSurface,
                    border = BorderStroke(1.dp, AdminBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.Security, null, tint = AdminMuted, modifier = Modifier.size(38.dp))
                        Text("No Co-Admins added yet", color = AdminInk, fontWeight = FontWeight.Bold)
                        Text("Tap the button above to create a Co-Admin login & custom password.", style = MaterialTheme.typography.bodySmall, color = AdminMuted)
                    }
                }
            }
        } else {
            items(uiState.coAdmins, key = { it.email.ifBlank { it.id } }) { co ->
                var showPass by remember(co.email) { mutableStateOf(false) }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AdminSurface,
                    border = BorderStroke(1.dp, if (co.activeFlag) AdminCyan.copy(alpha = 0.5f) else AdminCrimson),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(co.name, color = AdminInk, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                                Text(co.email, style = MaterialTheme.typography.bodySmall, color = AdminCyan)
                                Text(co.roleTitle, style = MaterialTheme.typography.labelSmall, color = AdminGold)
                            }
                            Switch(
                                checked = co.activeFlag,
                                onCheckedChange = { viewModel.saveCoAdmin(co.copy(activeFlag = it)) }
                            )
                        }

                        // Passcode row with reveal + 1-tap Copy
                        Surface(shape = RoundedCornerShape(12.dp), color = AdminCard, modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Key, null, tint = AdminGold, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Assigned Password", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                                    Text(
                                        if (showPass) co.passcode else "•".repeat(co.passcode.length.coerceAtLeast(6)),
                                        color = AdminInk,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                                IconButton(onClick = { showPass = !showPass }) {
                                    Icon(
                                        if (showPass) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                        "Show Password",
                                        tint = AdminMuted
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        val shareText = "Take It Admin Access\nGmail: ${co.email}\nPassword: ${co.passcode}\nRole: ${co.roleTitle}"
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("TakeItCoAdmin", shareText))
                                        viewModel.showMessage("Co-Admin login details copied! Ready to share.")
                                    }
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, "Copy Credentials", tint = AdminGold)
                                }
                            }
                        }

                        // Permissions summary badges
                        val perms = buildList {
                            if (co.canManageOrders) add("Orders")
                            if (co.canManageRestaurants) add("Menu")
                            if (co.canManageCoupons) add("Coupons")
                            if (co.canManageUsers) add("Users")
                            if (co.canManageSettings) add("Controls")
                        }
                        Text(
                            "Permissions: ${if (perms.isEmpty()) "Dashboard Only" else perms.joinToString(" • ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = AdminEmerald
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = {
                                    editingCoAdmin = co
                                    showCreateDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AdminBorder),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Rounded.Edit, null, tint = AdminCyan, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Edit Password / Access", color = AdminCyan)
                            }
                            OutlinedButton(
                                onClick = { viewModel.deleteCoAdmin(co.email) },
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f))
                            ) {
                                Icon(Icons.Rounded.DeleteOutline, null, tint = AdminCrimson, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }

    if (showCreateDialog) {
        CoAdminEditorDialog(
            initial = editingCoAdmin,
            onDismiss = { showCreateDialog = false },
            onSave = {
                viewModel.saveCoAdmin(it)
                showCreateDialog = false
            }
        )
    }
}

@Composable
private fun CoAdminEditorDialog(
    initial: CoAdminAccount?,
    onDismiss: () -> Unit,
    onSave: (CoAdminAccount) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var roleTitle by remember { mutableStateOf(initial?.roleTitle ?: "Operations Manager") }
    var passcode by remember { mutableStateOf(initial?.passcode ?: "TK@${Random.nextInt(1000, 9999)}") }
    var canOrders by remember { mutableStateOf(initial?.canManageOrders ?: true) }
    var canRestaurants by remember { mutableStateOf(initial?.canManageRestaurants ?: true) }
    var canCoupons by remember { mutableStateOf(initial?.canManageCoupons ?: false) }
    var canUsers by remember { mutableStateOf(initial?.canManageUsers ?: false) }
    var canSettings by remember { mutableStateOf(initial?.canManageSettings ?: false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text(if (initial == null) "Create Co-Admin Account" else "Edit Co-Admin", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Co-Admin Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim().lowercase() },
                    label = { Text("Co-Admin Gmail Address") },
                    enabled = initial == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = roleTitle, onValueChange = { roleTitle = it }, label = { Text("Role Title (e.g. Kitchen Manager)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("Custom Password") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    FilledTonalIconButton(
                        onClick = { passcode = "TK#${Random.nextInt(10000, 99999)}" }
                    ) {
                        Icon(Icons.Rounded.Casino, "Generate Random Password", tint = AdminGold)
                    }
                }
                HorizontalDivider(color = AdminBorder)
                Text("Grant Specific Permissions", style = MaterialTheme.typography.labelMedium, color = AdminGold, fontWeight = FontWeight.Bold)
                PermissionRow("Manage Live Orders & Dispatch", canOrders) { canOrders = it }
                PermissionRow("Manage Restaurants & Menu", canRestaurants) { canRestaurants = it }
                PermissionRow("Manage Promo Coupons", canCoupons) { canCoupons = it }
                PermissionRow("Manage Customers & Tickets", canUsers) { canUsers = it }
                PermissionRow("Edit Global App Settings", canSettings) { canSettings = it }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cleanEmail = email.trim().lowercase()
                    if (name.isNotBlank() && cleanEmail.contains("@") && passcode.trim().length >= 4) {
                        onSave(
                            (initial ?: CoAdminAccount()).copy(
                                id = cleanEmail,
                                email = cleanEmail,
                                name = name.trim(),
                                roleTitle = roleTitle.trim().ifBlank { "Co-Admin" },
                                passcode = passcode.trim(),
                                activeFlag = initial?.activeFlag ?: true,
                                canManageOrders = canOrders,
                                canManageRestaurants = canRestaurants,
                                canManageCoupons = canCoupons,
                                canManageUsers = canUsers,
                                canManageSettings = canSettings
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
            ) { Text("Save Co-Admin", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) } }
    )
}

@Composable
private fun PermissionRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = AdminInk, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

// ---------------------------------------------------------------------------
// 5. ISOLATED RAZORPAY PAYMENTS LEDGER TAB
// ---------------------------------------------------------------------------

@Composable
fun AdminPaymentsTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = AdminCard,
                border = BorderStroke(1.dp, AdminEmerald.copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Column {
                            Text("Take It Razorpay Revenue", style = MaterialTheme.typography.labelMedium, color = AdminMuted)
                            Text("₹${uiState.razorpayTotalRevenue}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = AdminEmerald)
                        }
                        FilledTonalButton(
                            onClick = { viewModel.refreshRazorpayLedger() },
                            enabled = !uiState.isLoadingLedger,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (uiState.isLoadingLedger) "Syncing..." else "Refresh")
                        }
                    }
                    Text(
                        "Isolated Take It transactions verified via jaiambey.shop/takeit/admin/transactions.",
                        style = MaterialTheme.typography.labelSmall,
                        color = AdminMuted
                    )
                    if (!uiState.ledgerError.isNullOrBlank()) {
                        Text(uiState.ledgerError, style = MaterialTheme.typography.labelSmall, color = AdminCrimson)
                    }
                }
            }
        }

        if (uiState.razorpayTransactions.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = AdminSurface,
                    border = BorderStroke(1.dp, AdminBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Rounded.AccountBalanceWallet, null, tint = AdminMuted, modifier = Modifier.size(38.dp))
                        Text("No verified Razorpay transactions yet", color = AdminInk, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(uiState.razorpayTransactions) { tx ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = AdminSurface,
                    border = BorderStroke(1.dp, AdminBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(tx.itemTitle.ifBlank { tx.orderRef }, color = AdminInk, fontWeight = FontWeight.Bold)
                            Text("${tx.customerName} • ${tx.paymentId}", style = MaterialTheme.typography.labelSmall, color = AdminCyan)
                            Text("${tx.method.uppercase()} • ${tx.status.uppercase()}", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                        }
                        Text("₹${tx.amount}", color = AdminEmerald, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// 6. GLOBAL LIVE CUSTOMER APP CONTROLS TAB
// ---------------------------------------------------------------------------

@Composable
fun AdminSettingsTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    val cfg = uiState.config
    var cityName by remember(cfg.cityName) { mutableStateOf(cfg.cityName) }
    var heroTitle by remember(cfg.heroBannerTitle) { mutableStateOf(cfg.heroBannerTitle) }
    var heroSub by remember(cfg.heroBannerSubtitle) { mutableStateOf(cfg.heroBannerSubtitle) }
    var heroCode by remember(cfg.heroBannerCode) { mutableStateOf(cfg.heroBannerCode) }
    var nightTitle by remember(cfg.lateNightTitle) { mutableStateOf(cfg.lateNightTitle) }
    var nightSub by remember(cfg.lateNightSubtitle) { mutableStateOf(cfg.lateNightSubtitle) }
    var deliveryFee by remember(cfg.deliveryFee) { mutableStateOf(cfg.deliveryFee.toString()) }
    var platformFee by remember(cfg.platformFee) { mutableStateOf(cfg.platformFee.toString()) }
    var gstPercent by remember(cfg.gstPercent) { mutableStateOf(cfg.gstPercent.toString()) }
    var freeAbove by remember(cfg.freeDeliveryAbove) { mutableStateOf(cfg.freeDeliveryAbove.toString()) }
    var announcement by remember(cfg.announcementText) { mutableStateOf(cfg.announcementText) }
    var supportPhone by remember(cfg.supportPhone) { mutableStateOf(cfg.supportPhone) }
    var supportEmail by remember(cfg.supportEmail) { mutableStateOf(cfg.supportEmail) }
    var acceptingOrders by remember(cfg.acceptingOrders) { mutableStateOf(cfg.acceptingOrders) }
    var maintenanceMode by remember(cfg.maintenanceMode) { mutableStateOf(cfg.maintenanceMode) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Live Customer App Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AdminInk)
            Text("Changes push immediately to every Customer device running Take It.", style = MaterialTheme.typography.bodySmall, color = AdminMuted)
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = AdminSurface, border = BorderStroke(1.dp, AdminBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Banners & City", color = AdminGold, fontWeight = FontWeight.Bold)
                    OutlinedTextField(value = cityName, onValueChange = { cityName = it }, label = { Text("Default City Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = announcement, onValueChange = { announcement = it }, label = { Text("Live Announcement Bar (Leave blank to hide)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = heroTitle, onValueChange = { heroTitle = it }, label = { Text("Hero Banner Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = heroSub, onValueChange = { heroSub = it }, label = { Text("Hero Banner Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = heroCode, onValueChange = { heroCode = it.uppercase() }, label = { Text("Hero Promo Code") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nightTitle, onValueChange = { nightTitle = it }, label = { Text("Late Night Banner Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = nightSub, onValueChange = { nightSub = it }, label = { Text("Late Night Banner Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(18.dp), color = AdminSurface, border = BorderStroke(1.dp, AdminBorder), modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Fees, Taxes & Support", color = AdminGold, fontWeight = FontWeight.Bold)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it.filter(Char::isDigit) }, label = { Text("Delivery ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = platformFee, onValueChange = { platformFee = it.filter(Char::isDigit) }, label = { Text("Platform ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = gstPercent, onValueChange = { gstPercent = it.filter(Char::isDigit) }, label = { Text("GST %") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = freeAbove, onValueChange = { freeAbove = it.filter(Char::isDigit) }, label = { Text("Free Deliv Above ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    }
                    OutlinedTextField(value = supportPhone, onValueChange = { supportPhone = it }, label = { Text("Support Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = supportEmail, onValueChange = { supportEmail = it }, label = { Text("Support Email") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    HorizontalDivider(color = AdminBorder)
                    PermissionRow("Accepting Live Orders", acceptingOrders) { acceptingOrders = it }
                    PermissionRow("Maintenance Mode (Lock Customer App)", maintenanceMode) { maintenanceMode = it }
                }
            }
        }

        item {
            Button(
                onClick = {
                    viewModel.saveAppConfig(
                        cfg.copy(
                            cityName = cityName.trim().ifBlank { "Greater Noida" },
                            announcementText = announcement.trim(),
                            heroBannerTitle = heroTitle.trim().ifBlank { "Flat 50% OFF" },
                            heroBannerSubtitle = heroSub.trim(),
                            heroBannerCode = heroCode.trim().uppercase(),
                            lateNightTitle = nightTitle.trim(),
                            lateNightSubtitle = nightSub.trim(),
                            deliveryFee = deliveryFee.toIntOrNull() ?: 20,
                            platformFee = platformFee.toIntOrNull() ?: 5,
                            gstPercent = gstPercent.toIntOrNull() ?: 5,
                            freeDeliveryAbove = freeAbove.toIntOrNull() ?: 299,
                            supportPhone = supportPhone.trim(),
                            supportEmail = supportEmail.trim(),
                            acceptingOrders = acceptingOrders,
                            maintenanceMode = maintenanceMode
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("admin_push_live_settings_btn")
            ) {
                Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Push Live to Customer App", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
