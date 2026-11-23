package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun AnimatedSplashScreen(onSkip: () -> Unit) {
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }

    val logoScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.45f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val logoRotationY by animateFloatAsState(
        targetValue = if (revealed) 0f else -38f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "logoTilt"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (revealed) 1f else 0f,
        animationSpec = tween(750, delayMillis = 250),
        label = "alpha"
    )
    val infinite = rememberInfiniteTransition(label = "orbit")
    val orbitAngle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing)),
        label = "orbitAngle"
    )
    val pulse by infinite.animateFloat(
        initialValue = 0.94f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse"
    )

    val orbitIcons = listOf(
        Icons.Rounded.TwoWheeler to PremiumYellow,
        Icons.Rounded.LunchDining to OrangeAccent,
        Icons.Rounded.ShoppingBasket to SuccessGreen,
        Icons.Rounded.LocalShipping to SkyBlue,
        Icons.Rounded.Inventory2 to VioletAccent
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(DeepNavyLight, DeepNavy, DarkBackground),
                    radius = 1200f
                )
            )
            .clickable { onSkip() },
        contentAlignment = Alignment.Center
    ) {
        // Orbiting 3D-style service orbs
        Box(
            modifier = Modifier.size(270.dp),
            contentAlignment = Alignment.Center
        ) {
            // Subtle orbital ring
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .scale(pulse)
                    .border(1.5.dp, SkyBlue.copy(alpha = 0.22f), CircleShape)
            )

            orbitIcons.forEachIndexed { idx, (icon, color) ->
                val angleRad = Math.toRadians((orbitAngle + idx * 72f).toDouble())
                val offsetX = (cos(angleRad) * 112.0).dp
                val offsetY = (sin(angleRad) * 112.0).dp
                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(DarkCardSurface.copy(alpha = 0.9f))
                        .border(1.2.dp, color.copy(alpha = 0.7f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
                }
            }

            // Center 3D Logo Emblem
            Box(
                modifier = Modifier
                    .size(124.dp)
                    .scale(logoScale)
                    .graphicsLayer {
                        rotationY = logoRotationY
                        cameraDistance = 12f * density
                    }
                    .clip(RoundedCornerShape(34.dp))
                    .background(DeepNavy)
                    .border(
                        width = 2.5.dp,
                        brush = Brush.linearGradient(listOf(SkyBlue, PremiumYellow)),
                        shape = RoundedCornerShape(34.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_launcher_zipora_1795421416651),
                    contentDescription = "Zipora 3D Brand Emblem",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 58.dp)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "ZIPORA",
                style = MaterialTheme.typography.displayLarge.copy(letterSpacing = 6.sp),
                color = SoftWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Ride • Eat • Shop • Deliver • Move",
                style = MaterialTheme.typography.titleSmall,
                color = SkyBlue
            )
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SoftWhite.copy(alpha = 0.1f))
                    .border(1.dp, SkyBlue.copy(alpha = 0.35f), RoundedCornerShape(50))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "HYPERLOCAL SUPER ECOSYSTEM",
                    style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.5.sp),
                    color = PremiumYellow,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
