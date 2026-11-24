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
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.ui.customer.FoodPhoto
import com.example.takeit.ui.customer.VegMarkIcon
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel
import kotlin.random.Random

// ---------------------------------------------------------------------------
// 1. RESTAURANTS, DISHES & FOOD CATEGORIES CATALOG TAB
// ---------------------------------------------------------------------------

@Composable
fun AdminCatalogTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    var section by remember { mutableIntStateOf(0) } // 0 = Restaurants, 1 = Dishes, 2 = Categories
    var editingRestaurant by remember { mutableStateOf<Restaurant?>(null) }
    var showRestaurantDialog by remember { mutableStateOf(false) }
    var editingDish by remember { mutableStateOf<MenuItem?>(null) }
    var showDishDialog by remember { mutableStateOf(false) }
    var editingCategory by remember { mutableStateOf<FoodCategory?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            listOf(
                Triple(0, "Rest. (${uiState.restaurants.size})", "admin_cat_tab_rest"),
                Triple(1, "Dishes (${uiState.menuItems.size})", "admin_cat_tab_dish"),
                Triple(2, "Cat. (${uiState.categories.size})", "admin_cat_tab_cat")
            ).forEach { (idx, label, tag) ->
                val sel = section == idx
                TakeIt3DCard(
                    onClick = { section = idx },
                    containerColor = if (sel) AdminGold else AdminSurface,
                    depthColor = if (sel) AdminGold.darken3D(0.42f) else Color(0xFF060A15),
                    borderColor = if (sel) AdminGold.lighten3D(0.35f) else AdminBorder,
                    cornerRadius = 14.dp,
                    depth = if (sel) 4.dp else 2.dp,
                    modifier = Modifier
                        .weight(1f)
                        .testTag(tag)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp, vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 11.sp,
                            color = if (sel) AdminBg else AdminInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            TakeIt3DButton(
                onClick = {
                    when (section) {
                        0 -> {
                            editingRestaurant = null
                            showRestaurantDialog = true
                        }
                        1 -> {
                            if (uiState.restaurants.isEmpty()) {
                                viewModel.showMessage("Add a restaurant first before adding dishes.")
                            } else {
                                editingDish = null
                                showDishDialog = true
                            }
                        }
                        else -> {
                            editingCategory = null
                            showCategoryDialog = true
                        }
                    }
                },
                containerColor = AdminGold,
                depthColor = AdminGold.darken3D(0.45f),
                contentColor = AdminBg,
                cornerRadius = 14.dp,
                depth = 4.dp,
                height = 40.dp,
                modifier = Modifier
                    .width(76.dp)
                    .testTag("admin_catalog_add_btn")
            ) {
                Icon(Icons.Rounded.Add, "Add", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("Add", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (section) {
                0 -> {
                    items(uiState.restaurants, key = { it.id }) { r ->
                        TakeIt3DCard(
                            containerColor = AdminSurface,
                            depthColor = Color(0xFF060A15),
                            borderColor = if (r.openFlag) AdminGold.copy(alpha = 0.4f) else AdminBorder,
                            cornerRadius = 20.dp,
                            depth = 5.dp,
                            shadowElevation = 10.dp,
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
                                        if (r.offerImageUrl.isNotBlank()) {
                                            Text("16:9 Offer Poster Live ✓", style = MaterialTheme.typography.labelSmall, color = AdminEmerald, fontWeight = FontWeight.Bold)
                                        }
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
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, AdminCyan.copy(alpha = 0.55f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Edit, null, tint = AdminCyan, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit, Offer & Photo", color = AdminCyan, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.deleteRestaurant(r.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, "Delete", tint = AdminCrimson, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    items(uiState.menuItems, key = { it.id }) { dish ->
                        TakeIt3DCard(
                            containerColor = AdminSurface,
                            depthColor = Color(0xFF060A15),
                            borderColor = if (dish.availableFlag) AdminBorder else AdminCrimson.copy(alpha = 0.45f),
                            cornerRadius = 20.dp,
                            depth = 5.dp,
                            shadowElevation = 10.dp,
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
                                        Text("${dish.restaurantName} • ${dish.category}", style = MaterialTheme.typography.labelSmall, color = AdminCyan)
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text("₹${dish.price}", style = MaterialTheme.typography.bodySmall, color = AdminGold, fontWeight = FontWeight.ExtraBold)
                                            if (dish.originalPrice > dish.price) {
                                                Text(
                                                    "₹${dish.originalPrice}",
                                                    style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.LineThrough),
                                                    color = AdminMuted
                                                )
                                            }
                                            Text("• ${if (dish.availableFlag) "In Stock" else "Sold Out"}", style = MaterialTheme.typography.labelSmall, color = if (dish.availableFlag) AdminEmerald else AdminCrimson, fontWeight = FontWeight.Bold)
                                        }
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
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, AdminCyan.copy(alpha = 0.55f)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Rounded.Edit, null, tint = AdminCyan, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Edit Dish", color = AdminCyan, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { viewModel.deleteMenuItem(dish.id) },
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f))
                                    ) {
                                        Icon(Icons.Rounded.DeleteOutline, "Delete", tint = AdminCrimson, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }
                else -> {
                    item {
                        TakeIt3DCard(
                            containerColor = AdminCard,
                            depthColor = Color(0xFF060A15),
                            borderColor = AdminCyan.copy(alpha = 0.5f),
                            cornerRadius = 18.dp,
                            depth = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Dynamic Home Food Categories", color = AdminGold, fontWeight = FontWeight.ExtraBold)
                                Text(
                                    "Categories created here populate the single-line Home strip and Restaurant/Dish dropdown menus live.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AdminMuted
                                )
                            }
                        }
                    }
                    if (uiState.categories.isEmpty()) {
                        item {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("No custom categories yet. Tap + Add to create one!", color = AdminMuted)
                            }
                        }
                    } else {
                        items(uiState.categories, key = { it.id }) { cat ->
                            TakeIt3DCard(
                                containerColor = AdminSurface,
                                depthColor = Color(0xFF060A15),
                                borderColor = if (cat.activeFlag) AdminGold.copy(alpha = 0.45f) else AdminBorder,
                                cornerRadius = 18.dp,
                                depth = 4.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FoodPhoto(
                                        imageUrl = cat.imageUrl,
                                        fallbackKey = cat.name,
                                        contentDescription = cat.name,
                                        modifier = Modifier
                                            .size(52.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("${cat.emoji} ${cat.name}", color = AdminInk, fontWeight = FontWeight.ExtraBold)
                                        Text("Order #${cat.sortOrder} • ${if (cat.activeFlag) "Active on Home" else "Hidden"}", style = MaterialTheme.typography.labelSmall, color = if (cat.activeFlag) AdminEmerald else AdminMuted)
                                    }
                                    Switch(
                                        checked = cat.activeFlag,
                                        onCheckedChange = { viewModel.saveCategory(cat.copy(activeFlag = it)) }
                                    )
                                    IconButton(onClick = {
                                        editingCategory = cat
                                        showCategoryDialog = true
                                    }) { Icon(Icons.Rounded.Edit, "Edit", tint = AdminCyan) }
                                    IconButton(onClick = { viewModel.deleteCategory(cat.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, "Delete", tint = AdminCrimson)
                                    }
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
            categories = uiState.categories,
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
            categories = uiState.categories,
            isUploading = uiState.isUploadingPhoto,
            onUploadImage = { uri, onUrl -> viewModel.uploadImageToImgBb(uri, "takeit_dish", onUrl) },
            onDismiss = { showDishDialog = false },
            onSave = {
                viewModel.saveMenuItem(it)
                showDishDialog = false
            }
        )
    }

    if (showCategoryDialog) {
        FoodCategoryEditorDialog(
            initial = editingCategory,
            isUploading = uiState.isUploadingPhoto,
            onUploadImage = { uri, onUrl -> viewModel.uploadImageToImgBb(uri, "takeit_cat", onUrl) },
            onDismiss = { showCategoryDialog = false },
            onSave = {
                viewModel.saveCategory(it)
                showCategoryDialog = false
            }
        )
    }
}

@Composable
private fun FoodCategoryEditorDialog(
    initial: FoodCategory?,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (FoodCategory) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: "🍕") }
    var sortOrder by remember { mutableStateOf((initial?.sortOrder ?: 1).toString()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            onUploadImage(uri) { uploadedUrl -> imageUrl = uploadedUrl }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text(if (initial == null) "Add Food Category" else "Edit Category", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FoodPhoto(
                        imageUrl = imageUrl,
                        fallbackKey = name,
                        contentDescription = "Category Photo",
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                    )
                    FilledTonalButton(
                        onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isUploading) "Uploading..." else "Pick Category Photo")
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name (e.g. Momos)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Emoji") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = sortOrder, onValueChange = { sortOrder = it.filter(Char::isDigit) }, label = { Text("Order #") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(
                            (initial ?: FoodCategory()).copy(
                                name = name.trim(),
                                emoji = emoji.trim().ifBlank { "🍽️" },
                                sortOrder = sortOrder.toIntOrNull() ?: 1,
                                imageUrl = imageUrl.trim(),
                                activeFlag = initial?.activeFlag ?: true
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
private fun RestaurantEditorDialog(
    initial: Restaurant?,
    categories: List<FoodCategory>,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (Restaurant) -> Unit
) {
    val categoryOptions = remember(categories) {
        val dyn = categories.map { it.name }.filter { it.isNotBlank() }
        val base = TakeItFoodCategories.map { it.name }.filter { it != "All" }
        (dyn + base).distinct()
    }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var cuisine by remember { mutableStateOf(initial?.cuisine ?: "North Indian, Chinese") }
    var category by remember { mutableStateOf(initial?.category?.ifBlank { categoryOptions.firstOrNull() ?: "Pizza" } ?: (categoryOptions.firstOrNull() ?: "Pizza")) }
    var catMenuExpanded by remember { mutableStateOf(false) }
    var deliveryTime by remember { mutableStateOf(initial?.deliveryTime ?: "30 min") }
    var priceForTwo by remember { mutableStateOf((initial?.priceForTwo ?: 200).toString()) }
    var offerTag by remember { mutableStateOf(initial?.offerTag ?: "40% OFF") }
    var offerCode by remember { mutableStateOf(initial?.offerCode ?: "FOOD50") }
    var address by remember { mutableStateOf(initial?.address ?: "Greater Noida") }
    var latitudeText by remember { mutableStateOf((initial?.latitude ?: 28.4744).toString()) }
    var longitudeText by remember { mutableStateOf((initial?.longitude ?: 77.5040).toString()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }
    var offerImageUrl by remember { mutableStateOf(initial?.offerImageUrl.orEmpty()) }
    var pureVeg by remember { mutableStateOf(initial?.pureVegFlag ?: false) }
    var freeDelivery by remember { mutableStateOf(initial?.freeDeliveryFlag ?: true) }
    var featured by remember { mutableStateOf(initial?.featuredFlag ?: true) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadImage(uri) { url -> imageUrl = url }
    }
    val offerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadImage(uri) { url -> offerImageUrl = url }
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
                        onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploading,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (isUploading) "Uploading..." else "Cover Photo (ImgBB)")
                    }
                }
                Text("16:9 Offer Deal Poster (Customer Offers Tab)", style = MaterialTheme.typography.labelSmall, color = AdminGold, fontWeight = FontWeight.Bold)
                if (offerImageUrl.isNotBlank()) {
                    FoodPhoto(
                        imageUrl = offerImageUrl,
                        fallbackKey = offerTag,
                        contentDescription = "16:9 Offer Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                FilledTonalButton(
                    onClick = { offerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Image, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (offerImageUrl.isBlank()) "Upload 16:9 Offer Poster" else "Change 16:9 Offer Poster")
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Restaurant Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                // Single-Option Category Dropdown
                Box {
                    OutlinedButton(
                        onClick = { catMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Category: $category", color = AdminGold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, "Select Category", tint = AdminGold)
                    }
                    DropdownMenu(expanded = catMenuExpanded, onDismissRequest = { catMenuExpanded = false }) {
                        categoryOptions.forEach { opt ->
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    category = opt
                                    catMenuExpanded = false
                                }
                            )
                        }
                    }
                }

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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = latitudeText, onValueChange = { latitudeText = it }, label = { Text("Latitude") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = longitudeText, onValueChange = { longitudeText = it }, label = { Text("Longitude") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.weight(1f))
                }
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
                                latitude = latitudeText.toDoubleOrNull() ?: 28.4744,
                                longitude = longitudeText.toDoubleOrNull() ?: 77.5040,
                                imageUrl = imageUrl.trim(),
                                offerImageUrl = offerImageUrl.trim(),
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
    categories: List<FoodCategory>,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    val sectionOptions = remember(categories) {
        (TakeItDishSections + categories.map { it.name }).filter { it.isNotBlank() }.distinct()
    }
    var selectedRest by remember {
        mutableStateOf(restaurants.firstOrNull { it.id == initial?.restaurantId } ?: restaurants.first())
    }
    var restMenuExpanded by remember { mutableStateOf(false) }
    var sectionMenuExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category?.ifBlank { sectionOptions.first() } ?: sectionOptions.first()) }
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
                        Text("Restaurant: ${selectedRest.name}", color = AdminGold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, "Select Restaurant", tint = AdminGold)
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

                // Single-Option Dish Menu Section Dropdown
                Box {
                    OutlinedButton(
                        onClick = { sectionMenuExpanded = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Menu Section: $category", color = AdminCyan, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, "Select Section", tint = AdminCyan)
                    }
                    DropdownMenu(expanded = sectionMenuExpanded, onDismissRequest = { sectionMenuExpanded = false }) {
                        sectionOptions.forEach { sec ->
                            DropdownMenuItem(
                                text = { Text(sec) },
                                onClick = {
                                    category = sec
                                    sectionMenuExpanded = false
                                }
                            )
                        }
                    }
                }

                // Separate Selling Price & Cut MRP
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it.filter(Char::isDigit) }, label = { Text("Selling Price ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = originalPrice, onValueChange = { originalPrice = it.filter(Char::isDigit) }, label = { Text("Cut MRP ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }

                Text("Dietary Type", style = MaterialTheme.typography.labelMedium, color = AdminGold, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(
                        selected = isVeg,
                        onClick = { isVeg = true },
                        leadingIcon = { VegMarkIcon(isVeg = true) },
                        label = { Text("Veg", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isVeg,
                        onClick = { isVeg = false },
                        leadingIcon = { VegMarkIcon(isVeg = false) },
                        label = { Text("Non-Veg", fontWeight = FontWeight.Bold) },
                        modifier = Modifier.weight(1f)
                    )
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
                        val selling = price.toIntOrNull() ?: 149
                        val mrp = (originalPrice.toIntOrNull() ?: selling).coerceAtLeast(selling)
                        onSave(
                            (initial ?: MenuItem()).copy(
                                restaurantId = selectedRest.id,
                                restaurantName = selectedRest.name,
                                name = name.trim(),
                                description = description.trim(),
                                category = category.trim().ifBlank { "Recommended" },
                                price = selling,
                                originalPrice = mrp,
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
                Column(modifier = Modifier.weight(1f)) {
                    Text("Live Promo Coupons", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AdminInk)
                    Text("Synced instantly to Customer Offers & Cart", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                }
                Spacer(Modifier.width(8.dp))
                TakeIt3DButton(
                    onClick = {
                        editingCoupon = null
                        showDialog = true
                    },
                    containerColor = AdminGold,
                    depthColor = AdminGold.darken3D(0.45f),
                    contentColor = AdminBg,
                    cornerRadius = 14.dp,
                    depth = 4.dp,
                    height = 42.dp,
                    modifier = Modifier.width(126.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("New Code", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
                }
            }
        }

        items(uiState.coupons, key = { it.id }) { coupon ->
            TakeIt3DCard(
                containerColor = AdminSurface,
                depthColor = if (coupon.activeFlag) AdminGold.darken3D(0.72f) else Color(0xFF060A15),
                borderColor = if (coupon.activeFlag) AdminGold.copy(alpha = 0.55f) else AdminBorder,
                cornerRadius = 20.dp,
                depth = 5.dp,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    TakeIt3DIconOrb(
                        icon = Icons.Rounded.LocalOffer,
                        containerColor = if (coupon.activeFlag) AdminGold else AdminCard,
                        iconTint = if (coupon.activeFlag) AdminBg else AdminMuted,
                        size = 42.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF3B2806), border = BorderStroke(1.dp, AdminGold.copy(alpha = 0.45f))) {
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
            TakeIt3DCard(
                onClick = { subTab = 0 },
                containerColor = if (subTab == 0) AdminGold else AdminSurface,
                depthColor = if (subTab == 0) AdminGold.darken3D(0.42f) else Color(0xFF060A15),
                borderColor = if (subTab == 0) AdminGold.lighten3D(0.35f) else AdminBorder,
                cornerRadius = 14.dp,
                depth = if (subTab == 0) 4.dp else 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Customers (${uiState.allUsers.size})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (subTab == 0) AdminBg else AdminInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            TakeIt3DCard(
                onClick = { subTab = 1 },
                containerColor = if (subTab == 1) AdminGold else AdminSurface,
                depthColor = if (subTab == 1) AdminGold.darken3D(0.42f) else Color(0xFF060A15),
                borderColor = if (subTab == 1) AdminGold.lighten3D(0.35f) else AdminBorder,
                cornerRadius = 14.dp,
                depth = if (subTab == 1) 4.dp else 2.dp,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tickets (${uiState.allTickets.size})",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        color = if (subTab == 1) AdminBg else AdminInk,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (subTab == 0) {
                items(uiState.allUsers, key = { it.id.ifBlank { it.userId } }) { user ->
                    val uid = user.id.ifBlank { user.userId }
                    TakeIt3DCard(
                        containerColor = AdminSurface,
                        depthColor = Color(0xFF060A15),
                        borderColor = if (user.suspendedFlag) AdminCrimson else AdminBorder,
                        cornerRadius = 20.dp,
                        depth = 5.dp,
                        shadowElevation = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                TakeIt3DIconOrb(
                                    icon = Icons.Rounded.Person,
                                    containerColor = if (user.suspendedFlag) AdminCrimson else AdminCyan.copy(alpha = 0.25f),
                                    depthColor = Color.Black,
                                    iconTint = if (user.suspendedFlag) Color.White else AdminCyan,
                                    size = 40.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(user.name, color = AdminInk, fontWeight = FontWeight.ExtraBold)
                                    Text(user.email, style = MaterialTheme.typography.bodySmall, color = AdminCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AdminGold),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.CardGiftcard, null, tint = AdminGold, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Credit Bonus", color = AdminGold, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.adminToggleSuspendUser(uid, !user.suspendedFlag) },
                                    shape = RoundedCornerShape(12.dp),
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
                    TakeIt3DCard(
                        containerColor = AdminSurface,
                        depthColor = Color(0xFF060A15),
                        borderColor = AdminBorder,
                        cornerRadius = 20.dp,
                        depth = 5.dp,
                        shadowElevation = 10.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            TakeIt3DButton(
                                onClick = { replyTarget = ticket },
                                containerColor = AdminGold,
                                depthColor = AdminGold.darken3D(0.45f),
                                contentColor = AdminBg,
                                cornerRadius = 12.dp,
                                depth = 4.dp,
                                height = 44.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Rounded.Reply, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Reply & Update Status", fontWeight = FontWeight.ExtraBold)
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
// 4. STAFF, ROLES, RESTAURANT OWNERS & DELIVERY PARTNERS MANAGER
// ---------------------------------------------------------------------------

@Composable
fun AdminCoAdminsTab(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel
) {
    val context = LocalContext.current
    var roleFilter by remember { mutableStateOf("ALL") }
    var editingCoAdmin by remember { mutableStateOf<CoAdminAccount?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    val visibleStaff = remember(uiState.coAdmins, roleFilter) {
        if (roleFilter == "ALL") uiState.coAdmins
        else uiState.coAdmins.filter { it.roleType.equals(roleFilter, ignoreCase = true) }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(
                containerColor = AdminCard,
                depthColor = Color(0xFF060A15),
                borderColor = AdminGold.copy(alpha = 0.7f),
                cornerRadius = 22.dp,
                depth = 5.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        TakeIt3DIconOrb(
                            icon = Icons.Rounded.AdminPanelSettings,
                            containerColor = AdminGold,
                            iconTint = AdminBg,
                            size = 40.dp
                        )
                        Text("Staff, Owners & Delivery Roles", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AdminInk)
                    }
                    Text(
                        if (uiState.isMainAdmin) {
                            "Create Co-Admins, assign Restaurant Owners to manage their kitchen & 16:9 offers, or onboard Delivery Partners with custom passcodes."
                        } else {
                            "As Co-Admin, you can grant Restaurant Owner or Delivery Partner access with custom Gmail + password."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = AdminMuted
                    )
                    TakeIt3DButton(
                        onClick = {
                            editingCoAdmin = null
                            showCreateDialog = true
                        },
                        containerColor = AdminGold,
                        depthColor = AdminGold.darken3D(0.45f),
                        contentColor = AdminBg,
                        cornerRadius = 16.dp,
                        depth = 5.dp,
                        height = 50.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("admin_create_coadmin_btn")
                    ) {
                        Icon(Icons.Rounded.PersonAdd, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Staff / Owner / Partner", fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }

        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(
                    listOf(
                        "ALL" to "All (${uiState.coAdmins.size})",
                        STAFF_ROLE_CO_ADMIN to "Co-Admins",
                        STAFF_ROLE_RESTAURANT_OWNER to "Restaurant Owners",
                        STAFF_ROLE_DELIVERY_PARTNER to "Delivery Partners"
                    )
                ) { (key, label) ->
                    FilterChip(
                        selected = roleFilter == key,
                        onClick = { roleFilter = key },
                        label = { Text(label, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AdminCyan,
                            selectedLabelColor = AdminBg,
                            containerColor = AdminSurface,
                            labelColor = AdminInk
                        )
                    )
                }
            }
        }

        if (visibleStaff.isEmpty()) {
            item {
                TakeIt3DCard(
                    containerColor = AdminSurface,
                    depthColor = Color(0xFF060A15),
                    borderColor = AdminBorder,
                    cornerRadius = 20.dp,
                    depth = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TakeIt3DIconOrb(Icons.Rounded.Security, AdminCard, iconTint = AdminMuted, size = 48.dp)
                        Text("No staff accounts in this filter", color = AdminInk, fontWeight = FontWeight.Bold)
                        Text("Tap the button above to create a login & passcode.", style = MaterialTheme.typography.bodySmall, color = AdminMuted)
                    }
                }
            }
        } else {
            items(visibleStaff, key = { it.email.ifBlank { it.id } }) { co ->
                var showPass by remember(co.email) { mutableStateOf(false) }
                val roleColor = when (co.roleType.uppercase()) {
                    STAFF_ROLE_RESTAURANT_OWNER -> AdminGold
                    STAFF_ROLE_DELIVERY_PARTNER -> AdminEmerald
                    else -> AdminCyan
                }
                val canEditThis = uiState.isMainAdmin || co.roleType != STAFF_ROLE_CO_ADMIN
                TakeIt3DCard(
                    containerColor = AdminSurface,
                    depthColor = Color(0xFF060A15),
                    borderColor = if (co.activeFlag) roleColor.copy(alpha = 0.55f) else AdminCrimson,
                    cornerRadius = 20.dp,
                    depth = 5.dp,
                    shadowElevation = 10.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            TakeIt3DIconOrb(
                                icon = when (co.roleType.uppercase()) {
                                    STAFF_ROLE_RESTAURANT_OWNER -> Icons.Rounded.Storefront
                                    STAFF_ROLE_DELIVERY_PARTNER -> Icons.Rounded.DeliveryDining
                                    else -> Icons.Rounded.VerifiedUser
                                },
                                containerColor = if (co.activeFlag) roleColor.copy(alpha = 0.25f) else AdminCrimson.copy(alpha = 0.25f),
                                depthColor = Color.Black,
                                iconTint = if (co.activeFlag) roleColor else AdminCrimson,
                                size = 40.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(co.name, color = AdminInk, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
                                    Surface(shape = RoundedCornerShape(6.dp), color = roleColor.copy(alpha = 0.18f)) {
                                        Text(
                                            co.roleType.replace("_", " "),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = roleColor,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(co.email, style = MaterialTheme.typography.bodySmall, color = AdminCyan, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (co.assignedRestaurantName.isNotBlank()) {
                                    Text("Kitchen: ${co.assignedRestaurantName}", style = MaterialTheme.typography.labelSmall, color = AdminGold)
                                } else if (co.vehicleNumber.isNotBlank() || co.phone.isNotBlank()) {
                                    Text("${co.phone} • ${co.vehicleNumber}", style = MaterialTheme.typography.labelSmall, color = AdminEmerald)
                                } else {
                                    Text(co.roleTitle, style = MaterialTheme.typography.labelSmall, color = AdminGold)
                                }
                            }
                            if (canEditThis) {
                                Switch(
                                    checked = co.activeFlag,
                                    onCheckedChange = { viewModel.saveCoAdmin(co.copy(activeFlag = it)) }
                                )
                            }
                        }

                        Surface(shape = RoundedCornerShape(12.dp), color = AdminCard, border = BorderStroke(1.dp, AdminBorder), modifier = Modifier.fillMaxWidth()) {
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
                                        val shareText = "Take It Access (${co.roleType})\nGmail: ${co.email}\nPassword: ${co.passcode}\nRole: ${co.roleTitle}"
                                        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                        cm.setPrimaryClip(ClipData.newPlainText("TakeItStaff", shareText))
                                        viewModel.showMessage("Login credentials copied! Ready to share.")
                                    }
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, "Copy Credentials", tint = AdminGold)
                                }
                            }
                        }

                        if (canEditThis) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = {
                                        editingCoAdmin = co
                                        showCreateDialog = true
                                    },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AdminCyan.copy(alpha = 0.55f)),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Rounded.Edit, null, tint = AdminCyan, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Edit Password / Access", color = AdminCyan, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { viewModel.deleteCoAdmin(co) },
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, AdminCrimson.copy(alpha = 0.6f))
                                ) {
                                    Icon(Icons.Rounded.DeleteOutline, "Delete", tint = AdminCrimson, modifier = Modifier.size(16.dp))
                                }
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
            isMainAdmin = uiState.isMainAdmin,
            restaurants = uiState.restaurants,
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
    isMainAdmin: Boolean,
    restaurants: List<Restaurant>,
    onDismiss: () -> Unit,
    onSave: (CoAdminAccount) -> Unit
) {
    val defaultRole = initial?.roleType?.ifBlank {
        if (isMainAdmin) STAFF_ROLE_CO_ADMIN else STAFF_ROLE_RESTAURANT_OWNER
    } ?: if (isMainAdmin) STAFF_ROLE_CO_ADMIN else STAFF_ROLE_RESTAURANT_OWNER

    var roleType by remember { mutableStateOf(defaultRole) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var email by remember { mutableStateOf(initial?.email.orEmpty()) }
    var roleTitle by remember { mutableStateOf(initial?.roleTitle ?: "Operations Manager") }
    var phone by remember { mutableStateOf(initial?.phone.orEmpty()) }
    var vehicleNumber by remember { mutableStateOf(initial?.vehicleNumber.orEmpty()) }
    var selectedRest by remember {
        mutableStateOf(
            restaurants.firstOrNull { it.id == initial?.assignedRestaurantId } ?: restaurants.firstOrNull()
        )
    }
    var restDropExpanded by remember { mutableStateOf(false) }
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
        title = { Text(if (initial == null) "Add Staff / Owner / Partner" else "Edit Staff Account", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Select Role", style = MaterialTheme.typography.labelMedium, color = AdminGold, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isMainAdmin) {
                        item {
                            FilterChip(
                                selected = roleType == STAFF_ROLE_CO_ADMIN,
                                onClick = {
                                    roleType = STAFF_ROLE_CO_ADMIN
                                    roleTitle = "Operations Co-Admin"
                                },
                                label = { Text("Co-Admin") }
                            )
                        }
                    }
                    item {
                        FilterChip(
                            selected = roleType == STAFF_ROLE_RESTAURANT_OWNER,
                            onClick = {
                                roleType = STAFF_ROLE_RESTAURANT_OWNER
                                roleTitle = "Restaurant Owner"
                                canOrders = true
                                canRestaurants = true
                                canCoupons = false
                                canUsers = false
                                canSettings = false
                            },
                            label = { Text("Restaurant Owner") }
                        )
                    }
                    item {
                        FilterChip(
                            selected = roleType == STAFF_ROLE_DELIVERY_PARTNER,
                            onClick = {
                                roleType = STAFF_ROLE_DELIVERY_PARTNER
                                roleTitle = "Delivery Partner"
                                canOrders = true
                                canRestaurants = false
                                canCoupons = false
                                canUsers = false
                                canSettings = false
                            },
                            label = { Text("Delivery Partner") }
                        )
                    }
                }

                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Full Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it.trim().lowercase() },
                    label = { Text("Gmail Address") },
                    enabled = initial == null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(value = roleTitle, onValueChange = { roleTitle = it }, label = { Text("Role Title") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                if (roleType == STAFF_ROLE_RESTAURANT_OWNER && restaurants.isNotEmpty()) {
                    Box {
                        OutlinedButton(
                            onClick = { restDropExpanded = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                "Assign Restaurant: ${selectedRest?.name ?: "Select"}",
                                color = AdminGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.ArrowDropDown, "Choose Restaurant", tint = AdminGold)
                        }
                        DropdownMenu(expanded = restDropExpanded, onDismissRequest = { restDropExpanded = false }) {
                            restaurants.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name) },
                                    onClick = {
                                        selectedRest = r
                                        restDropExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                if (roleType == STAFF_ROLE_DELIVERY_PARTNER) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("Phone *") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = vehicleNumber, onValueChange = { vehicleNumber = it.uppercase() }, label = { Text("Vehicle No.") }, singleLine = true, modifier = Modifier.weight(1f))
                    }
                }

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

                if (roleType == STAFF_ROLE_CO_ADMIN) {
                    HorizontalDivider(color = AdminBorder)
                    Text("Grant Co-Admin Permissions", style = MaterialTheme.typography.labelMedium, color = AdminGold, fontWeight = FontWeight.Bold)
                    PermissionRow("Manage Live Orders & Dispatch", canOrders) { canOrders = it }
                    PermissionRow("Manage Restaurants & Menu", canRestaurants) { canRestaurants = it }
                    PermissionRow("Manage Promo Coupons", canCoupons) { canCoupons = it }
                    PermissionRow("Manage Customers & Tickets", canUsers) { canUsers = it }
                    PermissionRow("Edit Global Fees & Controls", canSettings) { canSettings = it }
                }
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
                                roleTitle = roleTitle.trim().ifBlank { "Staff" },
                                roleType = roleType,
                                assignedRestaurantId = if (roleType == STAFF_ROLE_RESTAURANT_OWNER) selectedRest?.id.orEmpty() else "",
                                assignedRestaurantName = if (roleType == STAFF_ROLE_RESTAURANT_OWNER) selectedRest?.name.orEmpty() else "",
                                phone = phone.trim(),
                                vehicleNumber = vehicleNumber.trim(),
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
            ) { Text("Save Account", fontWeight = FontWeight.ExtraBold) }
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
            TakeIt3DCard(
                containerColor = AdminCard,
                depthColor = AdminEmerald.darken3D(0.72f),
                borderColor = AdminEmerald.copy(alpha = 0.65f),
                cornerRadius = 22.dp,
                depth = 5.dp,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            TakeIt3DIconOrb(
                                icon = Icons.Rounded.AccountBalanceWallet,
                                containerColor = AdminEmerald,
                                iconTint = AdminBg,
                                size = 42.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Take It Razorpay Revenue", style = MaterialTheme.typography.labelMedium, color = AdminMuted)
                                Text("₹${uiState.razorpayTotalRevenue}", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = AdminEmerald)
                            }
                        }
                        TakeIt3DButton(
                            onClick = { viewModel.refreshRazorpayLedger() },
                            enabled = !uiState.isLoadingLedger,
                            containerColor = AdminEmerald,
                            depthColor = AdminEmerald.darken3D(0.45f),
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 4.dp,
                            height = 40.dp,
                            modifier = Modifier.width(114.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (uiState.isLoadingLedger) "Syncing" else "Refresh", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
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
                TakeIt3DCard(
                    containerColor = AdminSurface,
                    depthColor = Color(0xFF060A15),
                    borderColor = AdminBorder,
                    cornerRadius = 20.dp,
                    depth = 4.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TakeIt3DIconOrb(Icons.Rounded.AccountBalanceWallet, AdminCard, iconTint = AdminMuted, size = 48.dp)
                        Text("No verified Razorpay transactions yet", color = AdminInk, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            items(uiState.razorpayTransactions) { tx ->
                TakeIt3DCard(
                    containerColor = AdminSurface,
                    depthColor = Color(0xFF060A15),
                    borderColor = AdminBorder,
                    cornerRadius = 18.dp,
                    depth = 4.dp,
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

    var showBannerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Live Customer App Controls", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AdminInk)
            Text("Manage Home 3-second Hero Carousel, Delivery/Platform Fees & Promo Coupons.", style = MaterialTheme.typography.bodySmall, color = AdminMuted)
        }

        // Home 3s Auto-Rotating Hero Carousel Manager
        item {
            TakeIt3DCard(
                containerColor = AdminSurface,
                depthColor = Color(0xFF060A15),
                borderColor = AdminGold.copy(alpha = 0.5f),
                cornerRadius = 22.dp,
                depth = 5.dp,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Home Hero Carousel (3s Auto-Slide)", color = AdminGold, fontWeight = FontWeight.ExtraBold)
                            Text("${uiState.heroBanners.size} uploaded banners live", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                        }
                        TakeIt3DButton(
                            onClick = { showBannerDialog = true },
                            containerColor = AdminGold,
                            depthColor = AdminGold.darken3D(0.42f),
                            contentColor = AdminBg,
                            cornerRadius = 12.dp,
                            depth = 3.dp,
                            height = 38.dp,
                            modifier = Modifier
                                .width(120.dp)
                                .testTag("admin_add_hero_banner_btn")
                        ) {
                            Icon(Icons.Rounded.AddPhotoAlternate, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Slide", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                        }
                    }

                    if (uiState.heroBanners.isNotEmpty()) {
                        uiState.heroBanners.forEach { slide ->
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = AdminCard,
                                border = BorderStroke(1.dp, AdminBorder),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    FoodPhoto(
                                        imageUrl = slide.imageUrl,
                                        fallbackKey = slide.title,
                                        contentDescription = slide.title,
                                        modifier = Modifier
                                            .size(width = 72.dp, height = 46.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(slide.title.ifBlank { "Banner Slide" }, color = AdminInk, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(slide.subtitle, style = MaterialTheme.typography.labelSmall, color = AdminMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                    Switch(
                                        checked = slide.activeFlag,
                                        onCheckedChange = { viewModel.saveHeroBanner(slide.copy(activeFlag = it)) }
                                    )
                                    IconButton(onClick = { viewModel.deleteHeroBanner(slide.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, "Delete Slide", tint = AdminCrimson)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            TakeIt3DCard(
                containerColor = AdminSurface,
                depthColor = Color(0xFF060A15),
                borderColor = AdminBorder,
                cornerRadius = 22.dp,
                depth = 5.dp,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Banners & City", color = AdminGold, fontWeight = FontWeight.ExtraBold)
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
            TakeIt3DCard(
                containerColor = AdminSurface,
                depthColor = Color(0xFF060A15),
                borderColor = AdminBorder,
                cornerRadius = 22.dp,
                depth = 5.dp,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fees, Taxes & Coupons", color = AdminGold, fontWeight = FontWeight.ExtraBold)
                        if (uiState.canManageCoupons) {
                            TextButton(onClick = { viewModel.selectAdminTab(AdminTab.OFFERS) }) {
                                Text("Manage Coupons (${uiState.coupons.size}) →", color = AdminCyan, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = deliveryFee, onValueChange = { deliveryFee = it.filter(Char::isDigit) }, label = { Text("Delivery Fee ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                        OutlinedTextField(value = platformFee, onValueChange = { platformFee = it.filter(Char::isDigit) }, label = { Text("Platform Fee ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
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
            TakeIt3DButton(
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
                containerColor = AdminGold,
                depthColor = AdminGold.darken3D(0.45f),
                contentColor = AdminBg,
                cornerRadius = 18.dp,
                depth = 5.dp,
                height = 54.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("admin_push_live_settings_btn")
            ) {
                Icon(Icons.Rounded.CloudUpload, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Push Live to Customer App", fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }

    if (showBannerDialog) {
        HeroBannerEditorDialog(
            restaurants = uiState.restaurants,
            isUploading = uiState.isUploadingPhoto,
            onUploadImage = { uri, onUrl -> viewModel.uploadImageToImgBb(uri, "takeit_hero", onUrl) },
            onDismiss = { showBannerDialog = false },
            onSave = {
                viewModel.saveHeroBanner(it)
                showBannerDialog = false
            }
        )
    }
}

@Composable
private fun HeroBannerEditorDialog(
    restaurants: List<Restaurant>,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (HeroBannerSlide) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }
    var promoCode by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var selectedRest by remember { mutableStateOf<Restaurant?>(null) }
    var restExpanded by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadImage(uri) { url -> imageUrl = url }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text("Add 3s Home Carousel Slide", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (imageUrl.isNotBlank()) {
                    FoodPhoto(
                        imageUrl = imageUrl,
                        fallbackKey = title,
                        contentDescription = "Hero Banner Preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(12.dp))
                    )
                }
                FilledTonalButton(
                    onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    enabled = !isUploading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isUploading) "Uploading to Cloud..." else "Upload Banner Image (ImgBB)")
                }
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Banner Headline *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subtitle, onValueChange = { subtitle = it }, label = { Text("Subtitle") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = promoCode, onValueChange = { promoCode = it.uppercase() }, label = { Text("Promo Code (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (restaurants.isNotEmpty()) {
                    Box {
                        OutlinedButton(onClick = { restExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Link Restaurant: ${selectedRest?.name ?: "All Restaurants"}",
                                color = AdminCyan,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(Icons.Rounded.ArrowDropDown, "Select Restaurant", tint = AdminCyan)
                        }
                        DropdownMenu(expanded = restExpanded, onDismissRequest = { restExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("All Restaurants") },
                                onClick = {
                                    selectedRest = null
                                    restExpanded = false
                                }
                            )
                            restaurants.forEach { r ->
                                DropdownMenuItem(
                                    text = { Text(r.name) },
                                    onClick = {
                                        selectedRest = r
                                        restExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            HeroBannerSlide(
                                title = title.trim(),
                                subtitle = subtitle.trim(),
                                promoCode = promoCode.trim().uppercase(),
                                imageUrl = imageUrl.trim(),
                                restaurantId = selectedRest?.id.orEmpty(),
                                activeFlag = true
                            )
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
            ) { Text("Publish Slide", fontWeight = FontWeight.ExtraBold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel", color = AdminMuted) } }
    )
}
