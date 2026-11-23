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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.components.Character3DPortrait
import com.example.ui.components.SimulatedMapCanvas
import com.example.ui.theme.*
import com.example.util.DeviceActions
import com.example.viewmodel.SuperAppUiState

private val ParcelRoyalBlue = Color(0xFF1D5BF2)
private val ParcelDeepBlue = Color(0xFF0E2C6E)
private val ParcelSkyBg = Color(0xFFF2F7FF)
private val ParcelGold = Color(0xFFFFD43B)
private val ParcelEmerald = Color(0xFF16A34A)
private val ParcelCardTint = Color(0xFFEFF6FF)

private enum class ParcelStage {
    HOME,
    BOOK_PARCEL,
    AVAILABLE_OPTIONS,
    TRACK_PARCEL,
    PARCEL_DELIVERED,
    MY_DELIVERIES,
    OFFERS
}

private data class ParcelOptionTier(
    val id: String,
    val name: String,
    val eta: String,
    val subtitle: String,
    val price: Int,
    val icon: ImageVector,
    val badgeColor: Color
)

private data class ParcelTypeChip(
    val name: String,
    val icon: ImageVector,
    val tint: Color,
    val bg: Color
)

@Composable
fun ParcelDeliveryScreen(
    uiState: SuperAppUiState,
    onBack: () -> Unit,
    onBookParcel: (
        title: String,
        subtitle: String,
        amount: Double,
        payment: String,
        pickup: String,
        drop: String,
        otp: String,
        partner: String,
        onCreated: (String) -> Unit
    ) -> Unit,
    onCompleteOrder: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var stage by remember { mutableStateOf(ParcelStage.HOME) }

    // Booking form state
    var pickupAddress by remember { mutableStateOf("Sector 62, Noida") }
    var dropAddress by remember { mutableStateOf("Greater Noida") }
    var selectedType by remember { mutableStateOf("Document") }
    var selectedSize by remember { mutableStateOf("Medium") }
    var specialInstructions by remember { mutableStateOf("") }
    var appliedCouponDiscount by remember { mutableIntStateOf(0) }

    val deliveryOptions = remember {
        listOf(
            ParcelOptionTier("std", "Standard Delivery", "12.5 km • 28 min", "Best for regular parcels", 49, Icons.Rounded.Inventory2, Color(0xFFD97706)),
            ParcelOptionTier("exp", "Express Delivery", "12.5 km • 20 min", "Fastest delivery", 69, Icons.Rounded.Timer, Color(0xFFEA580C)),
            ParcelOptionTier("prm", "Premium Delivery", "12.5 km • 16 min", "Extra Care + Insured", 89, Icons.Rounded.VerifiedUser, Color(0xFF1D4ED8)),
            ParcelOptionTier("bik", "Bike Delivery", "12.5 km • 25 min", "For small parcels", 39, Icons.Rounded.TwoWheeler, Color(0xFF0284C7))
        )
    }
    var selectedOption by remember { mutableStateOf(deliveryOptions[0]) }

    // Live order & receipt state
    var bookedOrderId by remember { mutableStateOf<String?>(null) }
    var trackingStep by remember { mutableIntStateOf(2) } // 0..4
    var selectedRating by remember { mutableIntStateOf(5) }
    var deliveryFilter by remember { mutableStateOf("All") }

    val sizeExtra = when (selectedSize) {
        "Small" -> 0
        "Medium" -> 10
        else -> 30
    }
    val deliveryFee = (selectedOption.price + sizeExtra - appliedCouponDiscount).coerceAtLeast(29)
    val insuranceFee = 10
    val grandTotal = deliveryFee + insuranceFee

    BackHandler(enabled = stage != ParcelStage.HOME) {
        stage = when (stage) {
            ParcelStage.BOOK_PARCEL, ParcelStage.MY_DELIVERIES, ParcelStage.OFFERS -> ParcelStage.HOME
            ParcelStage.AVAILABLE_OPTIONS -> ParcelStage.BOOK_PARCEL
            ParcelStage.TRACK_PARCEL -> ParcelStage.HOME
            ParcelStage.PARCEL_DELIVERED -> ParcelStage.HOME
            ParcelStage.HOME -> ParcelStage.HOME
        }
    }

    when (stage) {
        ParcelStage.HOME -> ParcelHomeContent(
            onBack = onBack,
            onBookNow = { stage = ParcelStage.BOOK_PARCEL },
            onOpenTrack = { stage = ParcelStage.TRACK_PARCEL },
            onOpenMyDeliveries = { stage = ParcelStage.MY_DELIVERIES },
            onOpenOffers = { stage = ParcelStage.OFFERS },
            onOpenSupport = { onShowMessage("Take It Parcel 24×7 Support connected • Avg reply 1 min") },
            onQuickService = { type ->
                selectedType = type
                stage = ParcelStage.BOOK_PARCEL
            }
        )

        ParcelStage.BOOK_PARCEL -> ParcelBookFormContent(
            pickup = pickupAddress,
            drop = dropAddress,
            selectedType = selectedType,
            selectedSize = selectedSize,
            instructions = specialInstructions,
            onPickupChange = { pickupAddress = it },
            onDropChange = { dropAddress = it },
            onSwap = {
                val tmp = pickupAddress
                pickupAddress = dropAddress
                dropAddress = tmp
            },
            onSelectType = { selectedType = it },
            onSelectSize = { selectedSize = it },
            onInstructionsChange = { specialInstructions = it },
            onBack = { stage = ParcelStage.HOME },
            onCalculateFare = { stage = ParcelStage.AVAILABLE_OPTIONS }
        )

        ParcelStage.AVAILABLE_OPTIONS -> ParcelOptionsContent(
            pickup = pickupAddress,
            drop = dropAddress,
            options = deliveryOptions,
            selectedOption = selectedOption,
            totalFare = deliveryFee,
            onSelectOption = { selectedOption = it },
            onBack = { stage = ParcelStage.BOOK_PARCEL },
            onConfirmBooking = {
                onBookParcel(
                    "Parcel • $selectedType",
                    "$selectedSize • ${selectedOption.name}",
                    grandTotal.toDouble(),
                    "UPI",
                    pickupAddress,
                    dropAddress,
                    "7925",
                    "Rakesh Kumar"
                ) { id ->
                    bookedOrderId = id
                    trackingStep = 2
                    stage = ParcelStage.TRACK_PARCEL
                }
            }
        )

        ParcelStage.TRACK_PARCEL -> ParcelTrackContent(
            orderId = bookedOrderId ?: "TK-PRC-8841",
            pickup = pickupAddress,
            drop = dropAddress,
            currentStep = trackingStep,
            onAdvanceStep = {
                if (trackingStep < 4) {
                    trackingStep++
                } else {
                    bookedOrderId?.let { onCompleteOrder(it) }
                    stage = ParcelStage.PARCEL_DELIVERED
                }
            },
            onCompleteDelivery = {
                bookedOrderId?.let { onCompleteOrder(it) }
                stage = ParcelStage.PARCEL_DELIVERED
            },
            onCallPartner = { DeviceActions.dialPhoneNumber(context, "+919811044221") },
            onChatPartner = { onShowMessage("Chat opened with Rakesh Kumar") },
            onBack = { stage = ParcelStage.HOME }
        )

        ParcelStage.PARCEL_DELIVERED -> ParcelDeliveredReceiptContent(
            parcelType = selectedType,
            packageSize = when (selectedSize) {
                "Small" -> "Small (0-1kg)"
                "Medium" -> "Medium (1-5kg)"
                else -> "Large (5-15kg)"
            },
            from = pickupAddress,
            to = dropAddress,
            deliveryFee = deliveryFee,
            insuranceFee = insuranceFee,
            totalPaid = grandTotal,
            rating = selectedRating,
            onRatingChange = {
                selectedRating = it
                onShowMessage("Thanks for rating $it★ on Take It Parcel!")
            },
            onDownloadReceipt = {
                DeviceActions.shareText(
                    context,
                    "Take It Parcel GST Invoice",
                    "Take It Parcel Invoice\nType: $selectedType ($selectedSize)\nRoute: $pickupAddress → $dropAddress\nTotal Paid: ₹$grandTotal\nOrder ID: ${bookedOrderId ?: "TK-PRC-8841"}"
                )
            },
            onBackHome = { stage = ParcelStage.HOME }
        )

        ParcelStage.MY_DELIVERIES -> ParcelMyDeliveriesContent(
            uiState = uiState,
            selectedFilter = deliveryFilter,
            onSelectFilter = { deliveryFilter = it },
            onTrackClick = { stage = ParcelStage.TRACK_PARCEL },
            onBack = { stage = ParcelStage.HOME }
        )

        ParcelStage.OFFERS -> ParcelOffersContent(
            onApplyCode = { code, discount ->
                appliedCouponDiscount = discount
                onShowMessage("Coupon $code applied! ₹$discount OFF on your parcel.")
                stage = ParcelStage.BOOK_PARCEL
            },
            onBack = { stage = ParcelStage.HOME }
        )
    }
}

@Composable
private fun ParcelHomeContent(
    onBack: () -> Unit,
    onBookNow: () -> Unit,
    onOpenTrack: () -> Unit,
    onOpenMyDeliveries: () -> Unit,
    onOpenOffers: () -> Unit,
    onOpenSupport: () -> Unit,
    onQuickService: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParcelSkyBg)
    ) {
        // Crisp Top Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftWhite)
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.size(48.dp).testTag("parcel_back_button")
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ParcelDeepBlue)
            }
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(ParcelRoyalBlue),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Inventory2, null, tint = SoftWhite, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "Take It Parcel",
                    style = MaterialTheme.typography.titleMedium,
                    color = ParcelDeepBlue,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            IconButton(
                onClick = onOpenOffers,
                modifier = Modifier.size(48.dp).testTag("parcel_offers_bell")
            ) {
                Icon(Icons.Rounded.NotificationsNone, "Offers", tint = ParcelDeepBlue)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Hero Banner: "Parcel Delivery Made Simple!"
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(198.dp)
                        .shadow(8.dp, RoundedCornerShape(24.dp))
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(listOf(Color(0xFFDBEAFE), Color(0xFF93C5FD), Color(0xFF3B82F6)))
                        )
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_parcel_truck_1795427329625),
                        contentDescription = "Take It Parcel Express Delivery",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxHeight()
                            .width(195.dp)
                            .align(Alignment.CenterEnd)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    0.0f to Color(0xFFEFF6FF),
                                    0.54f to Color(0xFFEFF6FF).copy(alpha = 0.92f),
                                    1.0f to Color.Transparent
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(0.64f)
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                "Parcel Delivery\nMade Simple!",
                                style = MaterialTheme.typography.headlineSmall,
                                color = ParcelDeepBlue,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 27.sp
                            )
                            Text(
                                "Send Anything,\nAnywhere",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ParcelDeepBlue.copy(alpha = 0.8f),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Button(
                            onClick = onBookNow,
                            colors = ButtonDefaults.buttonColors(containerColor = ParcelGold, contentColor = ParcelDeepBlue),
                            shape = RoundedCornerShape(50),
                            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
                            modifier = Modifier.height(48.dp).testTag("parcel_hero_book_cta")
                        ) {
                            Text("Book a Parcel", fontWeight = FontWeight.ExtraBold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }

            // 4 Quick Action Tiles
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = SoftWhite,
                    shadowElevation = 3.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp, horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        ParcelQuickActionTile("Track\nParcel", Icons.Rounded.LocationSearching, Color(0xFF2563EB), Color(0xFFDBEAFE), "parcel_quick_track", onOpenTrack)
                        ParcelQuickActionTile("My\nDeliveries", Icons.Rounded.LocalShipping, Color(0xFF0284C7), Color(0xFFE0F2FE), "parcel_quick_deliveries", onOpenMyDeliveries)
                        ParcelQuickActionTile("Coupons", Icons.Rounded.ConfirmationNumber, Color(0xFFDC2626), Color(0xFFFEE2E2), "parcel_quick_coupons", onOpenOffers)
                        ParcelQuickActionTile("Support", Icons.Rounded.SupportAgent, Color(0xFF1D4ED8), Color(0xFFE0E7FF), "parcel_quick_support", onOpenSupport)
                    }
                }
            }

            // Blue "Fast & Secure Delivery" Shield Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(5.dp, RoundedCornerShape(22.dp))
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(ParcelDeepBlue, ParcelRoyalBlue, Color(0xFF38BDF8))))
                        .clickable(onClick = onBookNow)
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                "Fast & Secure\nDelivery",
                                style = MaterialTheme.typography.titleLarge,
                                color = SoftWhite,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 24.sp
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                                Text("Real-time Tracking", style = MaterialTheme.typography.labelMedium, color = SoftWhite)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF4ADE80), modifier = Modifier.size(16.dp))
                                Text("On-Time Delivery & ₹5,000 Cover", style = MaterialTheme.typography.labelMedium, color = SoftWhite)
                            }
                        }
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(SoftWhite.copy(alpha = 0.16f))
                                .border(1.5.dp, SoftWhite.copy(alpha = 0.35f), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.VerifiedUser, "Insured Delivery", tint = ParcelGold, modifier = Modifier.size(42.dp))
                        }
                    }
                }
            }

            // "Our Services" 3-Column Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Our Services", style = MaterialTheme.typography.titleLarge, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                    TextButton(onClick = onBookNow) {
                        Text("See All →", color = ParcelRoyalBlue, fontWeight = FontWeight.Bold)
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ParcelServiceCard("Document\nDelivery", Icons.Rounded.Description, Color(0xFF2563EB), Color(0xFFDBEAFE), Modifier.weight(1f)) {
                        onQuickService("Document")
                    }
                    ParcelServiceCard("Personal\nParcel", Icons.Rounded.CardGiftcard, Color(0xFFEA580C), Color(0xFFFFEDD5), Modifier.weight(1f)) {
                        onQuickService("Package")
                    }
                    ParcelServiceCard("Commercial\nDelivery", Icons.Rounded.BusinessCenter, Color(0xFF0F172A), Color(0xFFE2E8F0), Modifier.weight(1f)) {
                        onQuickService("Fragile")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelQuickActionTile(
    label: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    tag: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .testTag(tag)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, label, tint = tint, modifier = Modifier.size(25.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = ParcelDeepBlue,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 14.sp
        )
    }
}

@Composable
private fun ParcelServiceCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    bg: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = SoftWhite,
        shadowElevation = 3.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, title, tint = tint, modifier = Modifier.size(26.dp))
            }
            Text(
                title,
                style = MaterialTheme.typography.labelMedium,
                color = ParcelDeepBlue,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ParcelBookFormContent(
    pickup: String,
    drop: String,
    selectedType: String,
    selectedSize: String,
    instructions: String,
    onPickupChange: (String) -> Unit,
    onDropChange: (String) -> Unit,
    onSwap: () -> Unit,
    onSelectType: (String) -> Unit,
    onSelectSize: (String) -> Unit,
    onInstructionsChange: (String) -> Unit,
    onBack: () -> Unit,
    onCalculateFare: () -> Unit
) {
    val types = remember {
        listOf(
            ParcelTypeChip("Document", Icons.Rounded.Description, Color(0xFF2563EB), Color(0xFFDBEAFE)),
            ParcelTypeChip("Package", Icons.Rounded.Inventory2, Color(0xFFD97706), Color(0xFFFEF3C7)),
            ParcelTypeChip("Fragile", Icons.Rounded.WineBar, Color(0xFFDC2626), Color(0xFFFEE2E2)),
            ParcelTypeChip("Others", Icons.Rounded.WorkOutline, Color(0xFF1D4ED8), Color(0xFFE0E7FF))
        )
    }
    val sizes = remember {
        listOf(
            Triple("Small", "0-1kg", Icons.Rounded.AllInbox),
            Triple("Medium", "1-5kg", Icons.Rounded.Inventory),
            Triple("Large", "5-15kg", Icons.Rounded.LocalShipping)
        )
    }

    Scaffold(
        containerColor = ParcelSkyBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ParcelDeepBlue)
                }
                Text(
                    "Book a Parcel",
                    style = MaterialTheme.typography.titleLarge,
                    color = ParcelDeepBlue,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.HeadsetMic, "Support", tint = ParcelDeepBlue)
                }
            }
        },
        bottomBar = {
            Surface(color = SoftWhite, shadowElevation = 10.dp) {
                Button(
                    onClick = onCalculateFare,
                    colors = ButtonDefaults.buttonColors(containerColor = ParcelRoyalBlue, contentColor = SoftWhite),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .height(54.dp)
                        .testTag("parcel_calculate_fare_button")
                ) {
                    Text("Calculate Fare", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Royal Blue Banner: "Fill in the details"
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(ParcelDeepBlue, ParcelRoyalBlue, Color(0xFF38BDF8))))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_parcel_truck_1795427329625),
                        contentDescription = "Stacked parcel boxes",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(146.dp).fillMaxHeight().align(Alignment.CenterEnd)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(0.0f to ParcelDeepBlue, 0.58f to ParcelRoyalBlue.copy(alpha = 0.88f), 1.0f to Color.Transparent))
                    )
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Fill in the details", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                        Text("We'll take care of the rest!", style = MaterialTheme.typography.bodySmall, color = SoftWhite.copy(alpha = 0.9f))
                    }
                }
            }

            // Pickup & Delivery Address Card with Swap Button
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 3.dp) {
                    Box(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.LocationOn, null, tint = ParcelRoyalBlue, modifier = Modifier.size(20.dp))
                                Text("Pickup Address", style = MaterialTheme.typography.labelMedium, color = ParcelDeepBlue, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = pickup,
                                onValueChange = onPickupChange,
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("parcel_pickup_input")
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Rounded.LocationOn, null, tint = Color(0xFFDC2626), modifier = Modifier.size(20.dp))
                                Text("Delivery Address", style = MaterialTheme.typography.labelMedium, color = ParcelDeepBlue, fontWeight = FontWeight.Bold)
                            }
                            OutlinedTextField(
                                value = drop,
                                onValueChange = onDropChange,
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.fillMaxWidth().testTag("parcel_drop_input")
                            )
                        }
                        Surface(
                            onClick = onSwap,
                            shape = CircleShape,
                            color = ParcelCardTint,
                            shadowElevation = 2.dp,
                            modifier = Modifier.align(Alignment.CenterEnd).size(42.dp).testTag("parcel_swap_button")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.SwapVert, "Swap addresses", tint = ParcelRoyalBlue, modifier = Modifier.size(22.dp))
                            }
                        }
                    }
                }
            }

            // Parcel Type 4-Column Cards
            item {
                Text("Parcel Type", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    types.forEach { item ->
                        val selected = selectedType == item.name
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) ParcelCardTint else SoftWhite)
                                .border(if (selected) 2.dp else 1.dp, if (selected) ParcelRoyalBlue else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .clickable { onSelectType(item.name) }
                                .padding(vertical = 12.dp, horizontal = 4.dp)
                                .testTag("parcel_type_${item.name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(item.bg),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(item.icon, item.name, tint = item.tint, modifier = Modifier.size(20.dp))
                            }
                            Text(
                                item.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) ParcelRoyalBlue else ParcelDeepBlue,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Package Size 3-Column Cards
            item {
                Text("Package Size", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    sizes.forEach { (name, weightLabel, icon) ->
                        val selected = selectedSize == name
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) ParcelCardTint else SoftWhite)
                                .border(if (selected) 2.dp else 1.dp, if (selected) ParcelRoyalBlue else Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
                                .clickable { onSelectSize(name) }
                                .padding(vertical = 12.dp, horizontal = 6.dp)
                                .testTag("parcel_size_${name.lowercase()}"),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(icon, name, tint = if (selected) ParcelRoyalBlue else MutedText, modifier = Modifier.size(22.dp))
                            Text(name, style = MaterialTheme.typography.labelLarge, color = if (selected) ParcelRoyalBlue else ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                            Text("($weightLabel)", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        }
                    }
                }
            }

            // Special Instructions
            item {
                Text("Special Instructions (Optional)", style = MaterialTheme.typography.titleSmall, color = ParcelDeepBlue, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = instructions,
                    onValueChange = onInstructionsChange,
                    placeholder = { Text("e.g. Handle with care, call at main gate...") },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
private fun ParcelOptionsContent(
    pickup: String,
    drop: String,
    options: List<ParcelOptionTier>,
    selectedOption: ParcelOptionTier,
    totalFare: Int,
    onSelectOption: (ParcelOptionTier) -> Unit,
    onBack: () -> Unit,
    onConfirmBooking: () -> Unit
) {
    Scaffold(
        containerColor = ParcelSkyBg,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SoftWhite)
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ParcelDeepBlue)
                }
                Text(
                    "Available Options",
                    style = MaterialTheme.typography.titleLarge,
                    color = ParcelDeepBlue,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = {}, modifier = Modifier.size(48.dp)) {
                    Icon(Icons.Rounded.HeadsetMic, "Help", tint = ParcelDeepBlue)
                }
            }
        },
        bottomBar = {
            Surface(color = SoftWhite, shadowElevation = 12.dp) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total Fare", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.Bold)
                        Text("₹$totalFare", style = MaterialTheme.typography.headlineSmall, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                    }
                    Button(
                        onClick = onConfirmBooking,
                        colors = ButtonDefaults.buttonColors(containerColor = ParcelRoyalBlue, contentColor = SoftWhite),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.fillMaxWidth().height(54.dp).testTag("parcel_confirm_booking_button")
                    ) {
                        Text("Confirm Booking", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, null, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Box(modifier = Modifier.fillMaxWidth().height(175.dp).clip(RoundedCornerShape(20.dp))) {
                    SimulatedMapCanvas(
                        modifier = Modifier.fillMaxSize(),
                        pickupLabel = pickup,
                        dropLabel = drop,
                        distanceKm = 12.5,
                        etaMinutes = 28,
                        driverProgress = 0.25f
                    )
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = SoftWhite,
                        shadowElevation = 6.dp,
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 12.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("12.5 km", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                            Text("28 min", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        }
                    }
                }
            }

            items(options, key = { it.id }) { opt ->
                val selected = opt.id == selectedOption.id
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (selected) ParcelCardTint else SoftWhite,
                    shadowElevation = if (selected) 4.dp else 1.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(if (selected) 2.dp else 1.dp, if (selected) ParcelRoyalBlue else Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                        .clickable { onSelectOption(opt) }
                        .testTag("parcel_option_${opt.id}")
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(opt.badgeColor.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(opt.icon, opt.name, tint = opt.badgeColor, modifier = Modifier.size(26.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(opt.name, style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                            Text(opt.eta, style = MaterialTheme.typography.labelSmall, color = MutedText)
                            Text(opt.subtitle, style = MaterialTheme.typography.bodySmall, color = ParcelDeepBlue.copy(alpha = 0.78f))
                        }
                        Text("₹${opt.price}", style = MaterialTheme.typography.titleLarge, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            imageVector = if (selected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = if (selected) "Selected" else "Not selected",
                            tint = if (selected) ParcelRoyalBlue else Color(0xFF94A3B8),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Green Insurance Banner
            item {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Rounded.VerifiedUser, null, tint = ParcelEmerald, modifier = Modifier.size(26.dp))
                        Column {
                            Text("Your parcel is insured", style = MaterialTheme.typography.titleSmall, color = Color(0xFF14532D), fontWeight = FontWeight.ExtraBold)
                            Text("Protected up to ₹5,000 against loss or transit damage", style = MaterialTheme.typography.labelSmall, color = Color(0xFF166534))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelTrackContent(
    orderId: String,
    pickup: String,
    drop: String,
    currentStep: Int,
    onAdvanceStep: () -> Unit,
    onCompleteDelivery: () -> Unit,
    onCallPartner: () -> Unit,
    onChatPartner: () -> Unit,
    onBack: () -> Unit
) {
    val steps = listOf("Booked", "Picked\nUp", "In Transit", "Out for\nDelivery", "Delivered")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParcelSkyBg)
    ) {
        // Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftWhite)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ParcelDeepBlue)
            }
            Text(
                "Track Parcel",
                style = MaterialTheme.typography.titleLarge,
                color = ParcelDeepBlue,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onCallPartner, modifier = Modifier.size(48.dp)) {
                Icon(Icons.Rounded.HeadsetMic, "Support", tint = ParcelDeepBlue)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Deep Blue Hero: "Your parcel is On the Way! Arriving in 28 min"
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(142.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(ParcelDeepBlue, ParcelRoyalBlue)))
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_takeit_parcel_truck_1795427329625),
                        contentDescription = "Delivery vehicle on the way",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.width(155.dp).fillMaxHeight().align(Alignment.CenterEnd)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(0.0f to ParcelDeepBlue, 0.56f to ParcelDeepBlue.copy(alpha = 0.85f), 1.0f to Color.Transparent))
                    )
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text("Your parcel is", style = MaterialTheme.typography.titleMedium, color = SoftWhite, fontWeight = FontWeight.Bold)
                        Text("On the Way!", style = MaterialTheme.typography.headlineSmall, color = ParcelGold, fontWeight = FontWeight.ExtraBold)
                        Text("Arriving in", style = MaterialTheme.typography.labelSmall, color = SoftWhite.copy(alpha = 0.85f))
                        Text("28 min • #$orderId", style = MaterialTheme.typography.titleLarge, color = SoftWhite, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }

            // 5-Step Horizontal Progress Tracker
            item {
                Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        steps.forEachIndexed { idx, label ->
                            val done = idx < currentStep
                            val active = idx == currentStep
                            Column(
                                modifier = Modifier.weight(1f).clickable(onClick = onAdvanceStep),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(
                                            when {
                                                done -> ParcelEmerald
                                                active -> ParcelRoyalBlue
                                                else -> Color(0xFFCBD5E1)
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (active) Icons.Rounded.LocalShipping else Icons.Rounded.Check,
                                        contentDescription = label,
                                        tint = SoftWhite,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (done || active) ParcelDeepBlue else MutedText,
                                    fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 13.sp
                                )
                            }
                        }
                    }
                }
            }

            // Live Route Map
            item {
                SimulatedMapCanvas(
                    modifier = Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(20.dp)),
                    pickupLabel = pickup,
                    dropLabel = drop,
                    distanceKm = 12.5,
                    etaMinutes = 28,
                    driverProgress = -1f
                )
            }

            // Driver Card: Rakesh Kumar ★ 4.8 • Tata Ace DL01AB1234
            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, shadowElevation = 3.dp) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Character3DPortrait(
                                drawableRes = R.drawable.img_char_porter_mover_1795426024294,
                                contentDescription = "Rakesh Kumar",
                                borderColor = ParcelRoyalBlue,
                                size = 54.dp,
                                bobbing = false
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Rakesh Kumar", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                                Text("★ 4.8 (2.5k rides)", style = MaterialTheme.typography.labelMedium, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                                Text("Tata Ace • DL01AB1234", style = MaterialTheme.typography.labelSmall, color = MutedText)
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                IconButton(
                                    onClick = onCallPartner,
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(ParcelCardTint)
                                ) {
                                    Icon(Icons.Rounded.Call, "Call", tint = ParcelRoyalBlue)
                                }
                                IconButton(
                                    onClick = onChatPartner,
                                    modifier = Modifier.size(48.dp).clip(CircleShape).background(ParcelCardTint)
                                ) {
                                    Icon(Icons.Rounded.Chat, "Chat", tint = ParcelRoyalBlue)
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth().navigationBarsPadding(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = onAdvanceStep,
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Next Milestone", fontWeight = FontWeight.Bold, color = ParcelRoyalBlue)
                            }
                            Button(
                                onClick = onCompleteDelivery,
                                colors = ButtonDefaults.buttonColors(containerColor = ParcelEmerald, contentColor = SoftWhite),
                                shape = RoundedCornerShape(50),
                                modifier = Modifier.weight(1f).height(48.dp).testTag("parcel_mark_delivered_button")
                            ) {
                                Text("Mark Delivered", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelDeliveredReceiptContent(
    parcelType: String,
    packageSize: String,
    from: String,
    to: String,
    deliveryFee: Int,
    insuranceFee: Int,
    totalPaid: Int,
    rating: Int,
    onRatingChange: (Int) -> Unit,
    onDownloadReceipt: () -> Unit,
    onBackHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ParcelSkyBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftWhite)
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackHome, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ParcelDeepBlue)
            }
            Text(
                "Order Details",
                style = MaterialTheme.typography.titleLarge,
                color = ParcelDeepBlue,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.size(48.dp))
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(CircleShape).background(ParcelEmerald),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Check, "Delivered", tint = SoftWhite, modifier = Modifier.size(28.dp))
                        }
                        Column {
                            Text("Parcel Delivered!", style = MaterialTheme.typography.headlineSmall, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                            Text("Delivered Today, 4:25 PM", style = MaterialTheme.typography.labelMedium, color = MutedText)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Inventory2, "Open parcel box", tint = Color(0xFFD97706), modifier = Modifier.size(60.dp))
                    }
                }
            }

            item {
                Surface(shape = RoundedCornerShape(22.dp), color = SoftWhite, shadowElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Order Summary", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                        HorizontalDivider(color = Color(0xFFE2E8F0))
                        SummaryRow("Parcel Type", parcelType)
                        SummaryRow("Package Size", packageSize)
                        SummaryRow("From", from)
                        SummaryRow("To", to)
                        SummaryRow("Delivery Fee", "₹$deliveryFee")
                        SummaryRow("Insurance", "₹$insuranceFee")
                        Surface(shape = RoundedCornerShape(12.dp), color = Color(0xFFFEF9C3), modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total Paid", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                                Text("₹$totalPaid", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            item {
                OutlinedButton(
                    onClick = onDownloadReceipt,
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Rounded.Download, null, tint = ParcelDeepBlue)
                    Spacer(Modifier.width(8.dp))
                    Text("Download Receipt", color = ParcelDeepBlue, fontWeight = FontWeight.Bold)
                }
            }

            item {
                Surface(shape = RoundedCornerShape(20.dp), color = SoftWhite, modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Rate Your Experience", style = MaterialTheme.typography.titleSmall, color = ParcelDeepBlue, fontWeight = FontWeight.Bold)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            (1..5).forEach { star ->
                                IconButton(onClick = { onRatingChange(star) }, modifier = Modifier.size(48.dp)) {
                                    Icon(
                                        imageVector = Icons.Rounded.Star,
                                        contentDescription = "Rate $star stars",
                                        tint = if (star <= rating) Color(0xFFF59E0B) else Color(0xFFCBD5E1),
                                        modifier = Modifier.size(34.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = onBackHome,
                    colors = ButtonDefaults.buttonColors(containerColor = ParcelRoyalBlue, contentColor = SoftWhite),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier.fillMaxWidth().height(52.dp).navigationBarsPadding()
                ) {
                    Text("Back to Parcel Home", fontWeight = FontWeight.ExtraBold)
                }
            }
        }
    }
}

@Composable
private fun SummaryRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MutedText)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = ParcelDeepBlue, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun ParcelMyDeliveriesContent(
    uiState: SuperAppUiState,
    selectedFilter: String,
    onSelectFilter: (String) -> Unit,
    onTrackClick: () -> Unit,
    onBack: () -> Unit
) {
    val filters = listOf("All", "Ongoing", "Delivered", "Cancelled")
    val demoList = remember(uiState.orders) {
        val realParcels = uiState.orders.filter { it.type == "PARCEL" }.map {
            Triple(it.id, if (it.status == "COMPLETED") "Delivered" else "In Transit", it.amount.toInt())
        }
        realParcels + listOf(
            Triple("#PG12345678", "Delivered", 59),
            Triple("#PG12345677", "In Transit", 89),
            Triple("#PG12345676", "Delivered", 49),
            Triple("#PG12345675", "Cancelled", 0)
        )
    }
    val filtered = demoList.filter { (_, status, _) ->
        when (selectedFilter) {
            "Ongoing" -> status == "In Transit"
            "Delivered" -> status == "Delivered"
            "Cancelled" -> status == "Cancelled"
            else -> true
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(ParcelSkyBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(SoftWhite).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ParcelDeepBlue)
            }
            Text("My Deliveries", style = MaterialTheme.typography.titleLarge, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(48.dp))
        }

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(filters) { f ->
                val active = selectedFilter == f
                Surface(
                    onClick = { onSelectFilter(f) },
                    shape = RoundedCornerShape(50),
                    color = if (active) ParcelRoyalBlue else SoftWhite
                ) {
                    Text(
                        f,
                        color = if (active) SoftWhite else ParcelDeepBlue,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                    )
                }
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(filtered) { (id, status, amt) ->
                val bgTint = when (status) {
                    "Delivered" -> Color(0xFFECFDF5)
                    "In Transit" -> Color(0xFFEFF6FF)
                    else -> Color(0xFFFEF2F2)
                }
                val statusColor = when (status) {
                    "Delivered" -> ParcelEmerald
                    "In Transit" -> ParcelRoyalBlue
                    else -> Color(0xFFDC2626)
                }
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = bgTint,
                    shadowElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth().clickable { if (status == "In Transit") onTrackClick() }
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(SoftWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (status == "In Transit") Icons.Rounded.LocalShipping else Icons.Rounded.Inventory2,
                                status,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(status, style = MaterialTheme.typography.titleSmall, color = statusColor, fontWeight = FontWeight.ExtraBold)
                            Text(id, style = MaterialTheme.typography.bodySmall, color = ParcelDeepBlue, fontWeight = FontWeight.Bold)
                            Text("Sector 62 → Greater Noida", style = MaterialTheme.typography.labelSmall, color = MutedText)
                        }
                        Text("₹$amt", style = MaterialTheme.typography.titleLarge, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ParcelOffersContent(
    onApplyCode: (String, Int) -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(ParcelSkyBg)) {
        Row(
            modifier = Modifier.fillMaxWidth().background(SoftWhite).statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(48.dp)) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = ParcelDeepBlue)
            }
            Text("Offers & Coupons", style = MaterialTheme.typography.titleLarge, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold, modifier = Modifier.weight(1f), textAlign = TextAlign.Center)
            Spacer(Modifier.size(48.dp))
        }

        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Brush.horizontalGradient(listOf(Color(0xFFFACC15), Color(0xFFF59E0B), Color(0xFFEA580C))))
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Flat ₹50 OFF", style = MaterialTheme.typography.headlineSmall, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                        Text("on Parcel Delivery • First Booking", style = MaterialTheme.typography.bodyMedium, color = ParcelDeepBlue)
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = RoundedCornerShape(10.dp), color = SoftWhite.copy(alpha = 0.85f)) {
                                Text("Use Code: PARCEL50", style = MaterialTheme.typography.labelLarge, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp))
                            }
                            Button(
                                onClick = { onApplyCode("PARCEL50", 50) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = SoftWhite),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Text("Apply", fontWeight = FontWeight.ExtraBold)
                            }
                        }
                    }
                }
            }

            item {
                Text("Available Offers", style = MaterialTheme.typography.titleMedium, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
            }

            items(
                listOf(
                    Triple("₹30 OFF", "on orders above ₹99 • Code: DEL30", Pair("DEL30", 30)),
                    Triple("Free Insurance", "up to ₹5,000 on all parcels", Pair("SAFE0", 10)),
                    Triple("10% Cashback", "via Take It Wallet UPI", Pair("WALLET10", 15))
                )
            ) { (title, sub, codePair) ->
                Surface(shape = RoundedCornerShape(18.dp), color = SoftWhite, shadowElevation = 2.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)).background(ParcelCardTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.LocalOffer, title, tint = ParcelRoyalBlue)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(title, style = MaterialTheme.typography.titleSmall, color = ParcelDeepBlue, fontWeight = FontWeight.ExtraBold)
                            Text(sub, style = MaterialTheme.typography.labelSmall, color = MutedText)
                        }
                        Button(
                            onClick = { onApplyCode(codePair.first, codePair.second) },
                            colors = ButtonDefaults.buttonColors(containerColor = ParcelRoyalBlue, contentColor = SoftWhite),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Text("Apply", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
