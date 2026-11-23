package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Clean light street-map Canvas matching Take It Book a Ride & Track Your Order screens,
 * with solid royal-blue route polyline, green pickup pin, red destination pin, and moving rider dot.
 */
@Composable
fun SimulatedMapCanvas(
    modifier: Modifier = Modifier,
    pickupLabel: String = "Current Location",
    dropLabel: String = "DLF Mall, Noida",
    distanceKm: Double = 3.2,
    etaMinutes: Int = 12,
    driverProgress: Float = 0.35f,
    isDarkStyle: Boolean = false,
    showMovingDriver: Boolean = true,
    showMatchingRadar: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "mapAnim")
    val pulseRadius by infinite.animateFloat(
        initialValue = 16f,
        targetValue = 54f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val radarAlpha by infinite.animateFloat(
        initialValue = 0.58f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "radarAlpha"
    )
    val autoDriverT by infinite.animateFloat(
        initialValue = 0.12f,
        targetValue = 0.88f,
        animationSpec = infiniteRepeatable(tween(7200, easing = LinearEasing), RepeatMode.Reverse),
        label = "driverMove"
    )

    val effectiveT = if (driverProgress >= 0f) driverProgress.coerceIn(0.06f, 0.94f) else autoDriverT
    val routeBlue = Color(0xFF2563EB)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDarkStyle) DarkBackground else Color(0xFFE9EEF5))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val blockColor = if (isDarkStyle) Color(0xFF0E2942) else Color(0xFFDFE6F0)
            val roadColor = if (isDarkStyle) Color(0xFF173A5C) else Color(0xFFFFFFFF)
            val parkColor = if (isDarkStyle) Color(0xFF103B38) else Color(0xFFC4EBC7)
            val waterColor = if (isDarkStyle) Color(0xFF0C3454) else Color(0xFFBAE6FD)

            // Subtle city blocks
            drawRoundRect(blockColor, Offset(w * 0.04f, h * 0.06f), Size(w * 0.25f, h * 0.22f), CornerRadius(14f, 14f))
            drawRoundRect(blockColor, Offset(w * 0.36f, h * 0.10f), Size(w * 0.24f, h * 0.24f), CornerRadius(14f, 14f))
            drawRoundRect(blockColor, Offset(w * 0.34f, h * 0.54f), Size(w * 0.28f, h * 0.28f), CornerRadius(14f, 14f))

            // Parks
            drawRoundRect(parkColor, Offset(w * 0.04f, h * 0.38f), Size(w * 0.22f, h * 0.28f), CornerRadius(24f, 24f))
            drawRoundRect(parkColor, Offset(w * 0.72f, h * 0.58f), Size(w * 0.24f, h * 0.28f), CornerRadius(26f, 26f))
            drawRoundRect(waterColor, Offset(w * 0.68f, h * 0.06f), Size(w * 0.26f, h * 0.20f), CornerRadius(32f, 32f))

            // Crisp white streets grid
            for (i in 1..5) {
                val y = h * (i / 6f)
                drawLine(roadColor, Offset(0f, y), Offset(w, y), strokeWidth = if (i == 3) 14f else 8f)
            }
            for (j in 1..4) {
                val x = w * (j / 5f)
                drawLine(roadColor, Offset(x, 0f), Offset(x, h), strokeWidth = if (j == 2) 14f else 8f)
            }
            drawLine(roadColor, Offset(0f, h * 0.82f), Offset(w, h * 0.22f), strokeWidth = 14f)
            drawLine(roadColor, Offset(w * 0.12f, 0f), Offset(w * 0.86f, h), strokeWidth = 9f)

            // Crisp Google-Maps style segmented polyline route
            val p0 = Offset(w * 0.22f, h * 0.34f)
            val p1 = Offset(w * 0.44f, h * 0.44f)
            val p2 = Offset(w * 0.56f, h * 0.66f)
            val p3 = Offset(w * 0.78f, h * 0.58f)

            val routePath = Path().apply {
                moveTo(p0.x, p0.y)
                lineTo(p1.x, p1.y)
                lineTo(p2.x, p2.y)
                lineTo(p3.x, p3.y)
            }

            // Soft route halo
            drawPath(
                path = routePath,
                color = routeBlue.copy(alpha = 0.24f),
                style = Stroke(width = 20f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            // Solid bright blue route
            drawPath(
                path = routePath,
                color = routeBlue,
                style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Radar rings when matching driver
            if (showMatchingRadar) {
                drawCircle(routeBlue.copy(alpha = radarAlpha), pulseRadius * 2.1f, p0)
                drawCircle(PremiumYellow.copy(alpha = radarAlpha * 0.8f), pulseRadius * 1.3f, p0, style = Stroke(4f))
            }

            // Green Pickup Pin
            drawCircle(SuccessGreen.copy(alpha = 0.22f), pulseRadius * 0.6f, p0)
            drawCircle(SuccessGreen, 16f, p0)
            drawCircle(SoftWhite, 6.5f, p0)

            // Red Destination Pin
            drawCircle(CoralDanger.copy(alpha = 0.22f), 24f, p3)
            drawCircle(CoralDanger, 16f, p3)
            drawCircle(SoftWhite, 6.5f, p3)

            // Moving rider marker interpolated along polyline segments
            if (showMovingDriver) {
                val seg = effectiveT * 3f
                val driverPos = when {
                    seg < 1f -> Offset(p0.x + (p1.x - p0.x) * seg, p0.y + (p1.y - p0.y) * seg)
                    seg < 2f -> {
                        val t = seg - 1f
                        Offset(p1.x + (p2.x - p1.x) * t, p1.y + (p2.y - p1.y) * t)
                    }
                    else -> {
                        val t = (seg - 2f).coerceIn(0f, 1f)
                        Offset(p2.x + (p3.x - p2.x) * t, p2.y + (p3.y - p2.y) * t)
                    }
                }
                drawCircle(PremiumYellow.copy(alpha = 0.4f), 26f, driverPos)
                drawCircle(Color(0xFF0B1F4B), 15f, driverPos)
                drawCircle(PremiumYellow, 9f, driverPos)
            }
        }

        // Center-top Crisp White Floating Distance & ETA Card (exact mockup style)
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isDarkStyle) DarkCardElevated.copy(alpha = 0.94f) else SoftWhite,
            shadowElevation = 6.dp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp)
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "${"%.1f".format(distanceKm)} km",
                    style = MaterialTheme.typography.titleSmall,
                    color = if (isDarkStyle) SoftWhite else DeepNavy,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = "$etaMinutes min",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isDarkStyle) PremiumYellow else MutedText,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Subtle Simulated GPS Pill in bottom-right
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .clip(RoundedCornerShape(50))
                .background((if (isDarkStyle) DarkCardSurface else SoftWhite).copy(alpha = 0.88f))
                .border(1.dp, if (isDarkStyle) DarkBorder else LightBorder, RoundedCornerShape(50))
                .padding(horizontal = 9.dp, vertical = 3.dp)
        ) {
            Text(
                text = "$pickupLabel → $dropLabel",
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                color = if (isDarkStyle) SkyBlue else SlateText,
                maxLines = 1
            )
        }
    }
}
