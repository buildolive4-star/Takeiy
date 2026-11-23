package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AppRoute
import com.example.ui.theme.*
import kotlin.math.sin

fun Modifier.bounceClick(onClick: () -> Unit): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce"
    )
    this
        .scale(scale)
        .clickable(interactionSource = interactionSource, indication = ripple(), onClick = onClick)
}

/**
 * Adds a subtle diagonal glass specular sheen behind premium 3D cards.
 */
fun Modifier.glassSheenOverlay(alpha: Float = 0.22f): Modifier = this.drawBehind {
    drawRoundRect(
        brush = Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = alpha),
                Color.Transparent,
                Color.White.copy(alpha = alpha * 0.45f)
            ),
            start = Offset(0f, 0f),
            end = Offset(size.width, size.height)
        ),
        cornerRadius = CornerRadius(24.dp.toPx(), 24.dp.toPx())
    )
}

/**
 * Ambient luminous mesh orbs for ultra-premium dark navy hero headers.
 */
@Composable
fun AtmosphericMeshHeaderBackground(
    modifier: Modifier = Modifier,
    primaryOrb: Color = SkyBlue,
    secondaryOrb: Color = PremiumYellow
) {
    Canvas(modifier = modifier) {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(Color(0xFF0A1F35), DeepNavy, Color(0xFF14385A))
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryOrb.copy(alpha = 0.28f), Color.Transparent),
                center = Offset(size.width * 0.88f, size.height * 0.16f),
                radius = size.width * 0.62f
            ),
            radius = size.width * 0.62f,
            center = Offset(size.width * 0.88f, size.height * 0.16f)
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryOrb.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.10f, size.height * 0.85f),
                radius = size.width * 0.48f
            ),
            radius = size.width * 0.48f,
            center = Offset(size.width * 0.10f, size.height * 0.85f)
        )
    }
}

/**
 * Floating glassmorphic pill bottom navigation dock for the Customer Super App.
 * Respects system navigationBars insets so icons and labels never clip.
 */
@Composable
fun ZiporaFloatingBottomDock(
    cartCount: Int,
    onNavigate: (AppRoute) -> Unit,
    onScrollTop: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navItems = listOf(
        Triple("Home", Icons.Rounded.Home, null),
        Triple("Ride", Icons.Rounded.TwoWheeler, AppRoute.RideBooking),
        Triple("Food", Icons.Rounded.LunchDining, AppRoute.FoodDiscovery),
        Triple("Grocery", Icons.Rounded.ShoppingBasket, AppRoute.GroceryStore),
        Triple("Wallet", Icons.Rounded.AccountBalanceWallet, AppRoute.Wallet)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp)
                .shadow(18.dp, RoundedCornerShape(28.dp), ambientColor = DeepNavy, spotColor = SkyBlue)
                .clip(RoundedCornerShape(28.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xF5092036), Color(0xF5102A43), Color(0xF50C263F))
                    )
                )
                .border(
                    width = 1.2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(SkyBlue.copy(alpha = 0.5f), PremiumYellow.copy(alpha = 0.4f), SkyBlue.copy(alpha = 0.5f))
                    ),
                    shape = RoundedCornerShape(28.dp)
                )
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            navItems.forEachIndexed { idx, (label, icon, route) ->
                val isHome = idx == 0
                val tint by animateColorAsState(
                    targetValue = if (isHome) DeepNavy else Color(0xFFC5DFF5),
                    label = "dockTint"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 50.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(if (isHome) PremiumYellow else Color.Transparent)
                        .clickable {
                            if (route == null) onScrollTop() else onNavigate(route)
                        }
                        .padding(vertical = 6.dp)
                        .testTag("bottom_dock_${label.lowercase()}"),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box {
                            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(21.dp))
                            if (label == "Food" && cartCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-4).dp)
                                        .size(14.dp)
                                        .clip(CircleShape)
                                        .background(CoralDanger),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        cartCount.coerceAtMost(9).toString(),
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp),
                                        color = SoftWhite
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = tint,
                            fontWeight = if (isHome) FontWeight.ExtraBold else FontWeight.SemiBold,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * 4-step animated live order progress bar for active orders on Customer Home.
 */
@Composable
fun LiveOrderMilestoneBar(status: String, modifier: Modifier = Modifier) {
    val activeStep = when (status.uppercase()) {
        "PENDING" -> 1
        "ACCEPTED", "PREPARING" -> 2
        "ON_THE_WAY", "DISPATCHED" -> 3
        "COMPLETED", "DELIVERED" -> 4
        else -> 1
    }
    val labels = listOf("Booked", "Preparing", "On Way", "Done")
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        labels.forEachIndexed { idx, stepLabel ->
            val reached = (idx + 1) <= activeStep
            Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (reached) SuccessGreen else LightBorder)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    stepLabel,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                    color = if (reached) DeepNavy else MutedText,
                    fontWeight = if (reached) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun AnimatedWalletAmount(
    amount: Double,
    color: Color = SoftWhite,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium
) {
    val animatedVal by animateFloatAsState(
        targetValue = amount.toFloat(),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "walletCounter"
    )
    Text(
        text = "₹${"%,.0f".format(animatedVal)}",
        style = style,
        color = color,
        fontWeight = FontWeight.ExtraBold
    )
}

@Composable
fun IconOrb3D(
    icon: ImageVector,
    tint: Color,
    gradientStart: Color,
    gradientEnd: Color,
    size: Dp = 46.dp,
    iconSize: Dp = 24.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .shadow(8.dp, RoundedCornerShape(size * 0.34f), ambientColor = gradientEnd, spotColor = gradientEnd)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .border(1.3.dp, SoftWhite.copy(alpha = 0.65f), RoundedCornerShape(size * 0.34f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun ZiporaSubTopBar(
    title: String,
    subtitle: String? = null,
    onBack: () -> Unit,
    isDark: Boolean = false,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    Surface(
        color = if (isDark) DarkBackground else SoftWhite,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(48.dp)
                    .testTag("back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = if (isDark) SoftWhite else DeepNavy
                )
            }
            Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isDark) SoftWhite else DeepNavy,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) SkyBlue else MutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            trailing()
        }
    }
}

@Composable
fun StatusPill(status: String) {
    val (bg, fg) = when (status.uppercase()) {
        "COMPLETED", "DELIVERED", "VERIFIED", "ONLINE" -> SuccessGreen.copy(alpha = 0.16f) to SuccessGreen
        "PENDING", "PREPARING", "PACKING" -> OrangeAccent.copy(alpha = 0.18f) to OrangeAccent
        "ON_THE_WAY", "ACCEPTED", "DISPATCHED" -> SkyBlue.copy(alpha = 0.18f) to SkyBlueDark
        "CANCELLED", "SUSPENDED", "OFFLINE" -> CoralDanger.copy(alpha = 0.16f) to CoralDanger
        "REFUNDED" -> VioletAccent.copy(alpha = 0.16f) to VioletAccent
        else -> SkyBlue.copy(alpha = 0.14f) to DeepNavy
    }
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.replace('_', ' '),
            style = MaterialTheme.typography.labelSmall,
            color = fg,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun PrototypeSimulationBanner(text: String = "Simulated Prototype • Local Room persistence active") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(SkyBlue.copy(alpha = 0.12f))
            .border(1.dp, SkyBlue.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = SkyBlueDark, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = SlateText
        )
    }
}

@Composable
fun ConfettiCelebrationOverlay(visible: Boolean) {
    if (!visible) return
    val anim = remember { Animatable(0f) }
    LaunchedEffect(visible) {
        anim.snapTo(0f)
        anim.animateTo(1f, animationSpec = tween(2400, easing = LinearEasing))
    }
    val colors = remember { listOf(SkyBlue, PremiumYellow, SuccessGreen, OrangeAccent, PinkAccent, VioletAccent) }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val progress = anim.value
        for (i in 0 until 42) {
            val seed = (i * 97 + 13) % 100
            val startX = size.width * (seed / 100f)
            val fallSpeed = 0.7f + (i % 5) * 0.18f
            val y = (-40f + progress * size.height * fallSpeed * 1.25f)
            val sway = sin(progress * 10f + i) * 34f
            val x = startX + sway
            val c = colors[i % colors.size].copy(alpha = (1f - progress * 0.7f).coerceIn(0f, 1f))
            drawCircle(color = c, radius = 7f + (i % 4) * 2.5f, center = Offset(x, y))
        }
    }
}

@Composable
fun AdminAuthorizationDialog(
    onDismiss: () -> Unit,
    onVerify: (String) -> Boolean
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DeepNavy,
        titleContentColor = SoftWhite,
        textContentColor = SoftWhite,
        icon = {
            IconOrb3D(Icons.Rounded.AdminPanelSettings, DeepNavy, SkyBlue, PremiumYellow, size = 54.dp)
        },
        title = {
            Text("Authorized Admin Access", style = MaterialTheme.typography.headlineMedium)
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Admin controls are restricted to verified Zipora platform administrators. Ordinary customer accounts cannot access this console.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFFB8D4EB)
                )
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it; error = false },
                    label = { Text("Enter Admin Passcode") },
                    singleLine = true,
                    isError = error,
                    modifier = Modifier.fillMaxWidth().testTag("admin_pin_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = SkyBlue,
                        unfocusedBorderColor = DarkBorder,
                        focusedTextColor = SoftWhite,
                        unfocusedTextColor = SoftWhite,
                        focusedLabelColor = SkyBlue,
                        unfocusedLabelColor = Color(0xFF9FB3C8)
                    )
                )
                if (error) {
                    Text("Invalid passcode. Please try again.", color = CoralDanger, style = MaterialTheme.typography.labelMedium)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SkyBlue.copy(alpha = 0.14f))
                        .padding(10.dp)
                ) {
                    Text(
                        "Prototype Demo Hint: Enter passcode 2026 to unlock the Admin Panel.",
                        style = MaterialTheme.typography.labelSmall,
                        color = PremiumYellow
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (!onVerify(pin)) error = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = PremiumYellow, contentColor = DeepNavy),
                modifier = Modifier.testTag("admin_unlock_button")
            ) {
                Text("Unlock Console", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = SkyBlue)
            }
        }
    )
}
