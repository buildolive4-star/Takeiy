package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

/**
 * Interactive vector-styled city map rendered cleanly with Compose Canvas.
 * Clearly labeled as a Simulated Map Preview for prototype demonstration.
 */
@Composable
fun SimulatedMapCanvas(
    modifier: Modifier = Modifier,
    pickupLabel: String = "Indiranagar",
    dropLabel: String = "MG Road",
    distanceKm: Double = 4.8,
    etaMinutes: Int = 12,
    driverProgress: Float = 0.35f,
    isDarkStyle: Boolean = false,
    showMovingDriver: Boolean = true,
    showMatchingRadar: Boolean = false
) {
    val infinite = rememberInfiniteTransition(label = "mapAnim")
    val pulseRadius by infinite.animateFloat(
        initialValue = 14f,
        targetValue = 52f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "pulse"
    )
    val radarAlpha by infinite.animateFloat(
        initialValue = 0.65f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "radarAlpha"
    )
    val autoDriverT by infinite.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(7500, easing = LinearEasing), RepeatMode.Reverse),
        label = "driverMove"
    )

    val effectiveT = if (driverProgress >= 0f) driverProgress.coerceIn(0.05f, 0.95f) else autoDriverT

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(if (isDarkStyle) DarkBackground else Color(0xFFE6F4FE))
            .border(1.dp, if (isDarkStyle) DarkBorder else LightBorder, RoundedCornerShape(24.dp))
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Base map land blocks & parks
            val roadColor = if (isDarkStyle) Color(0xFF173654) else Color(0xFFFFFFFF)
            val mainAvenueColor = if (isDarkStyle) Color(0xFF214B73) else Color(0xFFD0EAFC)
            val parkColor = if (isDarkStyle) Color(0xFF103B38) else Color(0xFFC8F3DC)
            val waterColor = if (isDarkStyle) Color(0xFF0C3454) else Color(0xFFBAE6FD)

            // Parks
            drawRoundRect(
                color = parkColor,
                topLeft = Offset(w * 0.06f, h * 0.14f),
                size = Size(w * 0.22f, h * 0.24f),
                cornerRadius = CornerRadius(24f, 24f)
            )
            drawRoundRect(
                color = parkColor,
                topLeft = Offset(w * 0.68f, h * 0.56f),
                size = Size(w * 0.25f, h * 0.26f),
                cornerRadius = CornerRadius(28f, 28f)
            )
            // Lake / River body
            drawRoundRect(
                color = waterColor,
                topLeft = Offset(w * 0.62f, h * 0.08f),
                size = Size(w * 0.30f, h * 0.20f),
                cornerRadius = CornerRadius(40f, 40f)
            )

            // Grid streets
            for (i in 1..5) {
                val y = h * (i / 6f)
                drawLine(roadColor, Offset(0f, y), Offset(w, y), strokeWidth = if (i == 3) 14f else 6f)
            }
            for (j in 1..4) {
                val x = w * (j / 5f)
                drawLine(roadColor, Offset(x, 0f), Offset(x, h), strokeWidth = if (j == 2) 14f else 6f)
            }
            // Diagonal arterial boulevard
            drawLine(mainAvenueColor, Offset(0f, h * 0.85f), Offset(w, h * 0.20f), strokeWidth = 18f)

            // Pickup -> Waypoint -> Destination Route
            val p0 = Offset(w * 0.20f, h * 0.74f)
            val p1 = Offset(w * 0.44f, h * 0.52f)
            val p2 = Offset(w * 0.56f, h * 0.34f)
            val p3 = Offset(w * 0.80f, h * 0.26f)

            val routePath = Path().apply {
                moveTo(p0.x, p0.y)
                cubicTo(p1.x, p1.y, p2.x, p2.y, p3.x, p3.y)
            }

            // Route glow
            drawPath(
                path = routePath,
                color = SkyBlue.copy(alpha = 0.28f),
                style = Stroke(width = 22f, cap = StrokeCap.Round)
            )
            // Primary route line
            drawPath(
                path = routePath,
                brush = Brush.linearGradient(listOf(SkyBlue, PremiumYellow, OrangeAccent)),
                style = Stroke(
                    width = 9f,
                    cap = StrokeCap.Round,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(24f, 12f), 0f)
                )
            )

            // Matching radar rings around pickup
            if (showMatchingRadar) {
                drawCircle(
                    color = SkyBlue.copy(alpha = radarAlpha),
                    radius = pulseRadius * 2.1f,
                    center = p0
                )
                drawCircle(
                    color = PremiumYellow.copy(alpha = radarAlpha * 0.8f),
                    radius = pulseRadius * 1.3f,
                    center = p0,
                    style = Stroke(width = 4f)
                )
            }

            // Pickup Pin (Pulsing Sky Blue)
            drawCircle(color = SkyBlue.copy(alpha = 0.25f), radius = pulseRadius * 0.65f, center = p0)
            drawCircle(color = SoftWhite, radius = 15f, center = p0)
            drawCircle(color = SkyBlue, radius = 9f, center = p0)

            // Destination Pin (Amber/Coral)
            drawCircle(color = OrangeAccent.copy(alpha = 0.25f), radius = 24f, center = p3)
            drawCircle(color = SoftWhite, radius = 15f, center = p3)
            drawCircle(color = CoralDanger, radius = 9f, center = p3)

            // Animated moving driver marker on cubic bezier curve
            if (showMovingDriver) {
                val u = 1f - effectiveT
                val bx = u * u * u * p0.x + 3 * u * u * effectiveT * p1.x + 3 * u * effectiveT * effectiveT * p2.x + effectiveT * effectiveT * effectiveT * p3.x
                val by = u * u * u * p0.y + 3 * u * u * effectiveT * p1.y + 3 * u * effectiveT * effectiveT * p2.y + effectiveT * effectiveT * effectiveT * p3.y
                val driverPos = Offset(bx, by)

                drawCircle(color = PremiumYellow.copy(alpha = 0.35f), radius = 28f, center = driverPos)
                drawCircle(color = DeepNavy, radius = 16f, center = driverPos)
                drawCircle(color = PremiumYellow, radius = 10f, center = driverPos)
            }
        }

        // Top-left Route Pill
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(DeepNavy.copy(alpha = 0.88f))
                .border(1.dp, SkyBlue.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Navigation, contentDescription = null, tint = PremiumYellow, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "${"%.1f".format(distanceKm)} km • $etaMinutes min",
                style = MaterialTheme.typography.labelMedium,
                color = SoftWhite,
                fontWeight = FontWeight.Bold
            )
        }

        // Top-right Simulated Badge
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(if (isDarkStyle) DarkCardElevated.copy(alpha = 0.92f) else SoftWhite.copy(alpha = 0.92f))
                .padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Speed, contentDescription = null, tint = SkyBlue, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Simulated GPS",
                style = MaterialTheme.typography.labelSmall,
                color = if (isDarkStyle) SkyBlue else DeepNavy
            )
        }

        // Bottom pickup/drop tag bar
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(10.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(if (isDarkStyle) DarkCardSurface.copy(alpha = 0.9f) else SoftWhite.copy(alpha = 0.92f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "● $pickupLabel",
                style = MaterialTheme.typography.labelMedium,
                color = SkyBlueDark,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "  →  ",
                style = MaterialTheme.typography.labelMedium,
                color = MutedText
            )
            Text(
                text = "● $dropLabel",
                style = MaterialTheme.typography.labelMedium,
                color = CoralDanger,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
