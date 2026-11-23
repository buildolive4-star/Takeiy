package com.example.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.*

@Composable
fun AnimatedSplashScreen(onSkip: () -> Unit) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }

    val logoScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.72f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "logoScale"
    )
    val contentAlpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(650),
        label = "contentAlpha"
    )
    val loadingProgress by animateFloatAsState(
        targetValue = if (started) 0.94f else 0.12f,
        animationSpec = tween(durationMillis = 1900, easing = FastOutSlowInEasing),
        label = "loadingBar"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A4DA2))
            .clickable { onSkip() }
    ) {
        // Full-bleed 3D Pixar-style Take It Hyperlocal Street Scene
        Image(
            painter = painterResource(id = R.drawable.img_takeit_splash_1795427329280),
            contentDescription = "Take It 3D Super App Splash Scene",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Top Sky Scrim for crisp logo readability
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xD90A4DA2),
                            Color(0x991D6FD6),
                            Color.Transparent
                        )
                    )
                )
        )

        // Bottom Deep Navy Scrim for Golden Loading Bar
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(190.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Transparent,
                            Color(0xCC071B3E),
                            Color(0xF5071B3E)
                        )
                    )
                )
        )

        // Top Brand Emblem + "Take It" Wordmark + Subtitle
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 42.dp)
                .scale(logoScale)
                .alpha(contentAlpha),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .shadow(16.dp, RoundedCornerShape(22.dp), ambientColor = PremiumYellow, spotColor = PremiumYellow)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Brush.linearGradient(listOf(Color(0xFF0B2559), Color(0xFF1D4ED8))))
                    .border(2.2.dp, Brush.linearGradient(listOf(PremiumYellow, SoftWhite)), RoundedCornerShape(22.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bolt,
                    contentDescription = "Take It Logo",
                    tint = PremiumYellow,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = buildAnnotatedString {
                    withStyle(SpanStyle(color = SoftWhite, fontWeight = FontWeight.ExtraBold)) {
                        append("Take ")
                    }
                    withStyle(SpanStyle(color = PremiumYellow, fontWeight = FontWeight.ExtraBold)) {
                        append("It")
                    }
                },
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp, letterSpacing = (-0.5).sp)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "One App. Endless Possibilities.",
                style = MaterialTheme.typography.titleMedium,
                color = SoftWhite.copy(alpha = 0.96f),
                fontWeight = FontWeight.SemiBold
            )
        }

        // Bottom Golden Pill Loading Progress Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 36.dp, vertical = 34.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(SoftWhite.copy(alpha = 0.25f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(loadingProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(Brush.horizontalGradient(listOf(PremiumYellow, Color(0xFFF59E0B), PremiumYellow)))
                )
            }
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.titleSmall,
                color = SoftWhite.copy(alpha = 0.94f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}
