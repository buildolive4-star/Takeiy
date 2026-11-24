package com.example.takeit.ui.restaurant

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.takeit.model.*
import com.example.takeit.theme.*
import com.example.takeit.ui.admin.AdminOrderStatusPill
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DIconOrb
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
import com.example.takeit.ui.customer.FoodPhoto
import com.example.takeit.ui.customer.VegMarkIcon
import com.example.takeit.viewmodel.TakeItUiState
import com.example.takeit.viewmodel.TakeItViewModel

@Composable
fun RestaurantPartnerRoot(
    uiState: TakeItUiState,
    viewModel: TakeItViewModel,
    onSignOut: () -> Unit
) {
    if (!uiState.hasUnlockedAdminAccess) {
        RestaurantPartnerPasscodeGate(
            uiState = uiState,
            onVerifyPasscode = viewModel::verifyCoAdminPasscode,
            onReturnToCustomer = if (uiState.appVariant == TakeItAppVariant.CUSTOMER) {
                { viewModel.exitAdminMode() }
            } else null,
            onSignOut = onSignOut
        )
        return
    }

    val myRest = remember(uiState.restaurants, uiState.assignedRestaurantId) {
        uiState.restaurants.firstOrNull { it.id == uiState.assignedRestaurantId }
    }
    val myDishes = remember(uiState.menuItems, uiState.assignedRestaurantId) {
        uiState.menuItems.filter { it.restaurantId == uiState.assignedRestaurantId }
    }

    var partnerTab by remember { mutableIntStateOf(0) } // 0 = Live Orders, 1 = Menu & 16:9 Offer
    var dispatchTarget by remember { mutableStateOf<FoodOrder?>(null) }
    var editingDish by remember { mutableStateOf<MenuItem?>(null) }
    var showDishDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = AdminBg,
        topBar = {
            TakeIt3DCard(
                containerColor = AdminSurface,
                depthColor = Color(0xFF050814),
                borderColor = AdminGold.copy(alpha = 0.45f),
                cornerRadius = 26.dp,
                depth = 5.dp,
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), AdminSurface)))
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TakeIt3DIconOrb(
                            icon = Icons.Rounded.Storefront,
                            containerColor = AdminGold,
                            iconTint = AdminBg,
                            size = 44.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = myRest?.name ?: "Restaurant Partner Panel",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = AdminInk,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "PARTNER PORTAL • ${uiState.email}",
                                style = MaterialTheme.typography.labelSmall,
                                color = AdminGold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (uiState.appVariant == TakeItAppVariant.CUSTOMER) {
                            FilledTonalButton(
                                onClick = { viewModel.exitAdminMode() },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                colors = ButtonDefaults.filledTonalButtonColors(containerColor = AdminCard, contentColor = AdminInk),
                                modifier = Modifier.height(38.dp)
                            ) {
                                Text("App", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            }
                            Spacer(Modifier.width(6.dp))
                        }
                        TakeIt3DIconOrb(
                            icon = Icons.AutoMirrored.Rounded.Logout,
                            containerColor = AdminCard,
                            depthColor = Color.Black,
                            iconTint = AdminCrimson,
                            size = 40.dp,
                            contentDescription = "Sign Out",
                            onClick = onSignOut
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        listOf(
                            0 to "Live Orders (${uiState.allOrders.size})",
                            1 to "Menu & 16:9 Offer"
                        ).forEach { (idx, label) ->
                            val sel = partnerTab == idx
                            TakeIt3DCard(
                                onClick = { partnerTab = idx },
                                containerColor = if (sel) AdminGold else AdminCard,
                                depthColor = if (sel) AdminGold.darken3D(0.42f) else Color(0xFF060A15),
                                borderColor = if (sel) AdminGold.lighten3D(0.35f) else AdminBorder,
                                cornerRadius = 14.dp,
                                depth = if (sel) 4.dp else 2.dp,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp, horizontal = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        label,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 13.sp,
                                        color = if (sel) AdminBg else AdminInk,
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
    ) { padding ->
        TakeIt3DBackdrop(AdminGold, AdminCyan, darkMode = true, baseColor = AdminBg, modifier = Modifier.padding(padding)) {
            if (myRest == null) {
                Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    TakeIt3DCard(containerColor = AdminSurface, depthColor = Color.Black, borderColor = AdminBorder, cornerRadius = 22.dp, depth = 5.dp) {
                        Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            TakeIt3DIconOrb(Icons.Rounded.Storefront, AdminGold, iconTint = AdminBg, size = 56.dp)
                            Text("No Restaurant Linked Yet", color = AdminInk, fontWeight = FontWeight.ExtraBold)
                            Text("Ask Main Admin ($MAIN_ADMIN_EMAIL) or Co-Admin to link your email to your restaurant in Staff & Roles.", color = AdminMuted, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
                        }
                    }
                }
            } else if (partnerTab == 0) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.allOrders.isEmpty()) {
                        item {
                            TakeIt3DCard(containerColor = AdminSurface, depthColor = Color.Black, borderColor = AdminBorder, cornerRadius = 20.dp, depth = 4.dp) {
                                Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    TakeIt3DIconOrb(Icons.Rounded.ReceiptLong, AdminCard, iconTint = AdminMuted, size = 48.dp)
                                    Text("No orders for ${myRest.name} yet", color = AdminInk, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        items(uiState.allOrders, key = { it.id }) { order ->
                            TakeIt3DCard(containerColor = AdminSurface, depthColor = Color(0xFF060A15), borderColor = AdminBorder, cornerRadius = 20.dp, depth = 5.dp) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Text(order.orderNumber, color = AdminGold, fontWeight = FontWeight.ExtraBold)
                                            AdminOrderStatusPill(order.status)
                                        }
                                        Text("₹${order.totalAmount}", color = AdminEmerald, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp)
                                    }
                                    Text(order.itemsSummary, color = AdminInk, style = MaterialTheme.typography.bodySmall)
                                    Text("Customer: ${order.customerName} • ${order.deliveryAddress}", color = AdminMuted, style = MaterialTheme.typography.labelSmall)
                                    if (order.status !in listOf("DELIVERED", "CANCELLED", "REFUNDED")) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                                            when (order.status) {
                                                "PLACED" -> TakeIt3DButton(
                                                    onClick = { viewModel.adminUpdateOrderStatus(order.id, "CONFIRMED") },
                                                    containerColor = AdminCyan,
                                                    contentColor = AdminBg,
                                                    height = 42.dp,
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Accept Order", fontWeight = FontWeight.ExtraBold) }
                                                "CONFIRMED" -> TakeIt3DButton(
                                                    onClick = { viewModel.adminUpdateOrderStatus(order.id, "PREPARING") },
                                                    containerColor = AdminGold,
                                                    contentColor = AdminBg,
                                                    height = 42.dp,
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Start Preparing", fontWeight = FontWeight.ExtraBold) }
                                                "PREPARING" -> TakeIt3DButton(
                                                    onClick = { dispatchTarget = order },
                                                    containerColor = Color(0xFFA855F7),
                                                    contentColor = Color.White,
                                                    height = 42.dp,
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Dispatch Rider", fontWeight = FontWeight.ExtraBold) }
                                                "OUT_FOR_DELIVERY" -> TakeIt3DButton(
                                                    onClick = { viewModel.adminUpdateOrderStatus(order.id, "DELIVERED") },
                                                    containerColor = AdminEmerald,
                                                    contentColor = AdminBg,
                                                    height = 42.dp,
                                                    modifier = Modifier.weight(1f)
                                                ) { Text("Mark Delivered", fontWeight = FontWeight.ExtraBold) }
                                            }
                                            OutlinedButton(
                                                onClick = { viewModel.adminUpdateOrderStatus(order.id, "CANCELLED") },
                                                border = BorderStroke(1.dp, AdminCrimson),
                                                modifier = Modifier.height(42.dp)
                                            ) { Text("Cancel", color = AdminCrimson, fontWeight = FontWeight.Bold) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                PartnerMenuAndOfferTab(
                    restaurant = myRest,
                    dishes = myDishes,
                    isUploading = uiState.isUploadingPhoto,
                    onUploadImage = { uri, prefix, onUrl -> viewModel.uploadImageToImgBb(uri, prefix, onUrl) },
                    onSaveRestaurant = viewModel::saveRestaurant,
                    onToggleOpen = { viewModel.toggleRestaurantOpen(myRest.id, it) },
                    onAddDish = {
                        editingDish = null
                        showDishDialog = true
                    },
                    onEditDish = {
                        editingDish = it
                        showDishDialog = true
                    },
                    onToggleDish = { id, avail -> viewModel.toggleMenuItemAvailable(id, avail) },
                    onDeleteDish = viewModel::deleteMenuItem
                )
            }
        }
    }

    dispatchTarget?.let { target ->
        var rName by remember { mutableStateOf("Partner Rider") }
        var rPhone by remember { mutableStateOf("+91 98110 22334") }
        var rVeh by remember { mutableStateOf("UP16 AB 4821") }
        var eta by remember { mutableStateOf("20") }
        AlertDialog(
            onDismissRequest = { dispatchTarget = null },
            containerColor = AdminSurface,
            titleContentColor = AdminInk,
            title = { Text("Dispatch ${target.orderNumber}", fontWeight = FontWeight.ExtraBold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = rName, onValueChange = { rName = it }, label = { Text("Rider Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rPhone, onValueChange = { rPhone = it }, label = { Text("Rider Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = rVeh, onValueChange = { rVeh = it }, label = { Text("Vehicle Plate") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = eta, onValueChange = { eta = it.filter(Char::isDigit) }, label = { Text("ETA (min)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.adminUpdateOrderStatus(target.id, "OUT_FOR_DELIVERY", rName, rPhone, rVeh, eta.toIntOrNull() ?: 20)
                        dispatchTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AdminGold, contentColor = AdminBg)
                ) { Text("Dispatch Live", fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { dispatchTarget = null }) { Text("Cancel", color = AdminMuted) } }
        )
    }

    if (showDishDialog && myRest != null) {
        PartnerDishDialog(
            initial = editingDish,
            restaurant = myRest,
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
}

@Composable
private fun PartnerMenuAndOfferTab(
    restaurant: Restaurant,
    dishes: List<MenuItem>,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, String, (String) -> Unit) -> Unit,
    onSaveRestaurant: (Restaurant) -> Unit,
    onToggleOpen: (Boolean) -> Unit,
    onAddDish: () -> Unit,
    onEditDish: (MenuItem) -> Unit,
    onToggleDish: (String, Boolean) -> Unit,
    onDeleteDish: (String) -> Unit
) {
    var coverUrl by remember(restaurant.imageUrl) { mutableStateOf(restaurant.imageUrl) }
    var offerImageUrl by remember(restaurant.offerImageUrl) { mutableStateOf(restaurant.offerImageUrl) }
    var offerTag by remember(restaurant.offerTag) { mutableStateOf(restaurant.offerTag) }
    var offerCode by remember(restaurant.offerCode) { mutableStateOf(restaurant.offerCode) }

    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadImage(uri, "takeit_rest") { url -> coverUrl = url }
    }
    val offerPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadImage(uri, "takeit_offer169") { url -> offerImageUrl = url }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TakeIt3DCard(containerColor = AdminSurface, depthColor = Color(0xFF060A15), borderColor = AdminGold.copy(alpha = 0.5f), cornerRadius = 22.dp, depth = 5.dp) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Kitchen Cover & 16:9 Offer Banner", color = AdminGold, fontWeight = FontWeight.ExtraBold)
                            Text("Your 16:9 offer appears in the Customer Offers tab.", style = MaterialTheme.typography.labelSmall, color = AdminMuted)
                        }
                        Switch(checked = restaurant.openFlag, onCheckedChange = onToggleOpen)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FoodPhoto(imageUrl = coverUrl, fallbackKey = restaurant.name, contentDescription = "Cover", modifier = Modifier.size(64.dp).clip(RoundedCornerShape(12.dp)))
                        FilledTonalButton(
                            onClick = { coverPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            enabled = !isUploading,
                            modifier = Modifier.weight(1f)
                        ) { Text(if (isUploading) "Uploading..." else "Upload Kitchen Cover") }
                    }

                    if (offerImageUrl.isNotBlank()) {
                        FoodPhoto(
                            imageUrl = offerImageUrl,
                            fallbackKey = offerTag,
                            contentDescription = "16:9 Offer",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp))
                        )
                    }
                    FilledTonalButton(
                        onClick = { offerPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                        enabled = !isUploading,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (offerImageUrl.isBlank()) "Upload 16:9 Offer Poster" else "Change 16:9 Offer Poster") }

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = offerTag, onValueChange = { offerTag = it }, label = { Text("Offer Headline") }, singleLine = true, modifier = Modifier.weight(1f))
                        OutlinedTextField(value = offerCode, onValueChange = { offerCode = it.uppercase() }, label = { Text("Offer Tag Code") }, singleLine = true, modifier = Modifier.weight(1f))
                    }

                    TakeIt3DButton(
                        onClick = {
                            onSaveRestaurant(
                                restaurant.copy(
                                    imageUrl = coverUrl.trim(),
                                    offerImageUrl = offerImageUrl.trim(),
                                    offerTag = offerTag.trim(),
                                    offerCode = offerCode.trim().uppercase()
                                )
                            )
                        },
                        containerColor = AdminGold,
                        contentColor = AdminBg,
                        height = 46.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Kitchen Photos & Offer", fontWeight = FontWeight.ExtraBold) }
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("My Dishes (${dishes.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = AdminInk)
                TakeIt3DButton(
                    onClick = onAddDish,
                    containerColor = AdminGold,
                    contentColor = AdminBg,
                    height = 40.dp,
                    modifier = Modifier.width(120.dp)
                ) {
                    Icon(Icons.Rounded.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Dish", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                }
            }
        }

        items(dishes, key = { it.id }) { dish ->
            TakeIt3DCard(containerColor = AdminSurface, depthColor = Color(0xFF060A15), borderColor = AdminBorder, cornerRadius = 18.dp, depth = 4.dp) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        FoodPhoto(imageUrl = dish.imageUrl, fallbackKey = dish.name, contentDescription = dish.name, modifier = Modifier.size(58.dp).clip(RoundedCornerShape(12.dp)))
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                VegMarkIcon(isVeg = dish.vegFlag)
                                Text(dish.name, color = AdminInk, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            Text("${dish.category} • Price ₹${dish.price} (MRP ₹${dish.originalPrice})", style = MaterialTheme.typography.labelSmall, color = AdminGold)
                        }
                        Switch(checked = dish.availableFlag, onCheckedChange = { onToggleDish(dish.id, it) })
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onEditDish(dish) }, border = BorderStroke(1.dp, AdminCyan), modifier = Modifier.weight(1f)) {
                            Text("Edit Dish", color = AdminCyan, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(onClick = { onDeleteDish(dish.id) }, border = BorderStroke(1.dp, AdminCrimson)) {
                            Icon(Icons.Rounded.DeleteOutline, "Delete", tint = AdminCrimson, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PartnerDishDialog(
    initial: MenuItem?,
    restaurant: Restaurant,
    categories: List<FoodCategory>,
    isUploading: Boolean,
    onUploadImage: (android.net.Uri, (String) -> Unit) -> Unit,
    onDismiss: () -> Unit,
    onSave: (MenuItem) -> Unit
) {
    val sectionOptions = remember(categories) {
        (TakeItDishSections + categories.map { it.name }).filter { it.isNotBlank() }.distinct()
    }
    var sectionExpanded by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var description by remember { mutableStateOf(initial?.description.orEmpty()) }
    var category by remember { mutableStateOf(initial?.category?.ifBlank { sectionOptions.first() } ?: sectionOptions.first()) }
    var price by remember { mutableStateOf((initial?.price ?: 149).toString()) }
    var originalPrice by remember { mutableStateOf((initial?.originalPrice ?: 199).toString()) }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl.orEmpty()) }
    var isVeg by remember { mutableStateOf(initial?.vegFlag ?: true) }
    var isBestseller by remember { mutableStateOf(initial?.bestsellerFlag ?: true) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onUploadImage(uri) { url -> imageUrl = url }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = AdminSurface,
        titleContentColor = AdminInk,
        title = { Text(if (initial == null) "Add Dish to ${restaurant.name}" else "Edit Dish", fontWeight = FontWeight.ExtraBold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FoodPhoto(imageUrl = imageUrl, fallbackKey = name, contentDescription = "Dish", modifier = Modifier.size(56.dp).clip(RoundedCornerShape(12.dp)))
                    FilledTonalButton(onClick = { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, enabled = !isUploading, modifier = Modifier.weight(1f)) {
                        Text(if (isUploading) "Uploading..." else "Upload Dish Photo")
                    }
                }
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Dish Name *") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") }, modifier = Modifier.fillMaxWidth())
                Box {
                    OutlinedButton(onClick = { sectionExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Section: $category", color = AdminCyan, modifier = Modifier.weight(1f))
                        Icon(Icons.Rounded.ArrowDropDown, "Select Section", tint = AdminCyan)
                    }
                    DropdownMenu(expanded = sectionExpanded, onDismissRequest = { sectionExpanded = false }) {
                        sectionOptions.forEach { sec ->
                            DropdownMenuItem(text = { Text(sec) }, onClick = { category = sec; sectionExpanded = false })
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = price, onValueChange = { price = it.filter(Char::isDigit) }, label = { Text("Selling ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                    OutlinedTextField(value = originalPrice, onValueChange = { originalPrice = it.filter(Char::isDigit) }, label = { Text("Cut MRP ₹") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    FilterChip(selected = isVeg, onClick = { isVeg = true }, leadingIcon = { VegMarkIcon(true) }, label = { Text("Veg") }, modifier = Modifier.weight(1f))
                    FilterChip(selected = !isVeg, onClick = { isVeg = false }, leadingIcon = { VegMarkIcon(false) }, label = { Text("Non-Veg") }, modifier = Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Bestseller", color = AdminInk)
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
                                restaurantId = restaurant.id,
                                restaurantName = restaurant.name,
                                name = name.trim(),
                                description = description.trim(),
                                category = category,
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

@Composable
private fun RestaurantPartnerPasscodeGate(
    uiState: TakeItUiState,
    onVerifyPasscode: (String) -> Unit,
    onReturnToCustomer: (() -> Unit)?,
    onSignOut: () -> Unit
) {
    var passcode by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    TakeIt3DBackdrop(AdminGold, AdminCyan, darkMode = true, baseColor = AdminBg) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            TakeIt3DCard(
                containerColor = AdminSurface,
                depthColor = Color(0xFF040711),
                borderColor = AdminGold.copy(alpha = 0.5f),
                cornerRadius = 28.dp,
                depth = 7.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth().widthIn(max = 460.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(R.drawable.logo_takeit_food),
                        contentDescription = "Restaurant Partner Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(86.dp).clip(CircleShape).background(Color.White)
                    )
                    Spacer(Modifier.height(14.dp))
                    Text("Restaurant Partner Portal", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold, color = AdminInk)
                    Spacer(Modifier.height(4.dp))
                    Text("Signed in as ${uiState.email}", style = MaterialTheme.typography.bodySmall, color = AdminCyan)
                    Spacer(Modifier.height(10.dp))
                    Text("Enter the partner password assigned by Admin to manage your restaurant menu, 16:9 offers, and live orders.", style = MaterialTheme.typography.bodySmall, color = AdminMuted, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = passcode,
                        onValueChange = { passcode = it },
                        label = { Text("Partner Password") },
                        singleLine = true,
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        trailingIcon = {
                            IconButton(onClick = { showPassword = !showPassword }) {
                                Icon(if (showPassword) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility, "Toggle password", tint = AdminMuted)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (!uiState.coAdminAuthError.isNullOrBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(uiState.coAdminAuthError, color = Color(0xFFFCA5A5), style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(16.dp))
                    TakeIt3DButton(
                        onClick = { onVerifyPasscode(passcode) },
                        enabled = passcode.isNotBlank() && !uiState.isCheckingCoAdmin,
                        containerColor = AdminGold,
                        contentColor = AdminBg,
                        height = 50.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) { Text(if (uiState.isCheckingCoAdmin) "Verifying..." else "Unlock Kitchen Panel", fontWeight = FontWeight.ExtraBold) }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (onReturnToCustomer != null) {
                            OutlinedButton(onClick = onReturnToCustomer, modifier = Modifier.weight(1f).height(46.dp)) {
                                Text("Customer App", color = AdminInk)
                            }
                        }
                        OutlinedButton(onClick = onSignOut, border = BorderStroke(1.dp, AdminCrimson), modifier = Modifier.weight(1f).height(46.dp)) {
                            Text("Switch Account", color = AdminCrimson)
                        }
                    }
                }
            }
        }
    }
}
