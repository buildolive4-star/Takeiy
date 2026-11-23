package com.example.ui.components

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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
            .shadow(6.dp, RoundedCornerShape(size * 0.34f), ambientColor = gradientEnd, spotColor = gradientEnd)
            .clip(RoundedCornerShape(size * 0.34f))
            .background(Brush.linearGradient(listOf(gradientStart, gradientEnd)))
            .border(1.2.dp, SoftWhite.copy(alpha = 0.55f), RoundedCornerShape(size * 0.34f)),
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
        shadowElevation = 2.dp
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
            .clip(RoundedCornerShape(12.dp))
            .background(SkyBlue.copy(alpha = 0.12f))
            .border(1.dp, SkyBlue.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 7.dp),
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
