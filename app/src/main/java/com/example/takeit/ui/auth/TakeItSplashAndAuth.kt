package com.example.takeit.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.signInWithGoogleBrowser
import com.example.takeit.model.MAIN_ADMIN_EMAIL
import com.example.takeit.model.TakeItAppVariant
import com.example.takeit.theme.*
import com.example.takeit.ui.components.TakeIt3DBackdrop
import com.example.takeit.ui.components.TakeIt3DButton
import com.example.takeit.ui.components.TakeIt3DCard
import com.example.takeit.ui.components.TakeIt3DHeroBanner
import com.example.takeit.ui.components.darken3D
import com.example.takeit.ui.components.lighten3D
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest

private fun readInstalledSha1(context: Context): String? {
    return try {
        val pm = context.packageManager
        val certBytes = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            val s = info.signingInfo
            val sigs = if (s?.hasMultipleSigners() == true) s.apkContentsSigners else s?.signingCertificateHistory
            sigs?.firstOrNull()?.toByteArray()
        } else {
            @Suppress("DEPRECATION")
            val info = pm.getPackageInfo(context.packageName, PackageManager.GET_SIGNATURES)
            @Suppress("DEPRECATION")
            info.signatures?.firstOrNull()?.toByteArray()
        } ?: return null
        MessageDigest.getInstance("SHA-1").digest(certBytes).joinToString(":") { "%02X".format(it) }
    } catch (_: Exception) {
        null
    }
}

@Composable
fun TakeItFoodSplashScreen(
    variant: TakeItAppVariant
) {
    var visible by remember { mutableStateOf(false) }
    val logoScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.78f,
        animationSpec = tween(700),
        label = "splashScale"
    )
    LaunchedEffect(Unit) { visible = true }

    val inf = rememberInfiniteTransition(label = "splash3DKenBurns")
    val kenScale by inf.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "kenScale"
    )
    val tiltY by inf.animateFloat(
        initialValue = -3.2f,
        targetValue = 3.2f,
        animationSpec = infiniteRepeatable(tween(2400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "splashTiltY"
    )
    val floatY by inf.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(tween(2000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "splashFloatY"
    )

    if (variant == TakeItAppVariant.ADMIN || variant == TakeItAppVariant.DELIVERY) {
        val isDelivery = variant == TakeItAppVariant.DELIVERY
        val primaryAccent = if (isDelivery) DeliveryEmerald else AdminGold
        val secondaryAccent = if (isDelivery) DeliveryAmber else AdminCyan
        val backdropColor = if (isDelivery) DeliveryBg else AdminBg
        val cardColor = if (isDelivery) DeliverySurface else AdminCard
        val inkColor = if (isDelivery) DeliveryInk else AdminInk

        TakeIt3DBackdrop(
            accentPrimary = primaryAccent,
            accentSecondary = secondaryAccent,
            darkMode = true,
            baseColor = backdropColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .scale(logoScale)
                        .graphicsLayer {
                            rotationY = tiltY * 2.2f
                            translationY = floatY
                            cameraDistance = 14f * density
                        }
                        .size(168.dp)
                        .shadow(28.dp, CircleShape, ambientColor = primaryAccent, spotColor = primaryAccent)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(primaryAccent.lighten3D(0.3f), primaryAccent, primaryAccent.darken3D(0.3f))))
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDelivery) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(DeliverySurface),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeliveryDining,
                                contentDescription = "Take It Captain Logo",
                                tint = DeliveryEmerald,
                                modifier = Modifier.size(84.dp)
                            )
                        }
                    } else {
                        Image(
                            painter = painterResource(R.drawable.logo_takeit_food),
                            contentDescription = "Take It Admin Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(Color.White)
                        )
                    }
                }
                Spacer(Modifier.height(26.dp))
                Text(
                    text = if (isDelivery) "TAKE IT CAPTAIN" else "TAKE IT ADMIN",
                    style = MaterialTheme.typography.headlineLarge,
                    color = primaryAccent,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.8.sp
                )
                Spacer(Modifier.height(6.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = cardColor,
                    border = BorderStroke(1.dp, primaryAccent.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = if (isDelivery) "CAPTAIN PANEL • FOOD • RIDEX • MOVEX" else "3D Command Center • Live Cloud Sync",
                        style = MaterialTheme.typography.labelMedium,
                        color = inkColor,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = primaryAccent, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
            }
        }
        return
    }

    val welcomeDrawable = when (variant) {
        TakeItAppVariant.RIDE -> R.drawable.welcome_rapido_ride
        TakeItAppVariant.TRANSPORT -> R.drawable.welcome_parcel_transport
        else -> R.drawable.welcome_food_delivery
    }
    val accent = when (variant) {
        TakeItAppVariant.RIDE -> RideYellow
        TakeItAppVariant.TRANSPORT -> TransportOrange
        else -> TakeItGold
    }
    val secondary = when (variant) {
        TakeItAppVariant.RIDE -> RideCharcoal
        TakeItAppVariant.TRANSPORT -> TransportNavy
        else -> TakeItRed
    }

    TakeIt3DBackdrop(
        accentPrimary = accent,
        accentSecondary = secondary,
        darkMode = true,
        baseColor = secondary.darken3D(0.45f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(Modifier.height(8.dp))

            // 3D Framed Welcome Showcase Card with seamless ambient gradient blend
            TakeIt3DHeroBanner(
                borderColor = accent.copy(alpha = 0.6f),
                depthColor = secondary.darken3D(0.55f),
                cornerRadius = 32.dp,
                depth = 8.dp,
                tiltAmplitude = 3.2f,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .scale(logoScale)
            ) {
                Image(
                    painter = painterResource(welcomeDrawable),
                    contentDescription = "Take It Welcome Showcase",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = kenScale
                            scaleY = kenScale
                            translationY = floatY
                        }
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    secondary.darken3D(0.5f).copy(alpha = 0.35f),
                                    Color.Transparent,
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.72f)
                                )
                            )
                        )
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.7f),
                    border = BorderStroke(1.5.dp, accent),
                    shadowElevation = 10.dp
                ) {
                    Text(
                        text = when (variant) {
                            TakeItAppVariant.RIDE -> "TAKE IT RIDE • BIKE • AUTO • CAB"
                            TakeItAppVariant.TRANSPORT -> "TAKE IT TRANSPORT • PARCEL & CARGO"
                            else -> "TAKE IT • FOOD DELIVERY"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = accent,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                }
                CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
            }
        }
    }
}

@Composable
fun TakeItFoodAuthScreen(
    variant: TakeItAppVariant,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingPending by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isAdmin = variant == TakeItAppVariant.ADMIN
    val isDelivery = variant == TakeItAppVariant.DELIVERY
    val isDarkPanel = isAdmin || isDelivery

    // Only resume an explicit active Chrome OAuth pendingAuthResult; NEVER auto-login silently so Google Account Picker always shows!
    LaunchedEffect(Unit) {
        if (Firebase.auth.currentUser != null) {
            isCheckingPending = false
            onAuthSuccess()
            return@LaunchedEffect
        }
        val pending = Firebase.auth.pendingAuthResult
        if (pending != null) {
            runCatching { pending.await() }
            if (Firebase.auth.currentUser != null) {
                isCheckingPending = false
                onAuthSuccess()
                return@LaunchedEffect
            }
        }
        isCheckingPending = false
    }

    val inf = rememberInfiniteTransition(label = "auth3DAnim")
    val artScale by inf.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.07f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "artScale"
    )
    val floatOffset by inf.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatOffset"
    )
    val adminTilt by inf.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2800, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "adminTilt"
    )

    val welcomeDrawable = when (variant) {
        TakeItAppVariant.RIDE -> R.drawable.welcome_rapido_ride
        TakeItAppVariant.TRANSPORT -> R.drawable.welcome_parcel_transport
        TakeItAppVariant.DELIVERY -> R.drawable.welcome_rapido_ride
        else -> R.drawable.welcome_food_delivery
    }

    val badgeText = when (variant) {
        TakeItAppVariant.ADMIN -> "EXECUTIVE ADMIN CONTROL APP"
        TakeItAppVariant.DELIVERY -> "CAPTAIN PARTNER • EARN DAILY"
        TakeItAppVariant.RIDE -> "BIKE • AUTO • CAB • PARCEL"
        TakeItAppVariant.TRANSPORT -> "PARCEL & TRUCK TRANSPORT"
        else -> "FAST FOOD DELIVERY APP"
    }

    val headlineText = when (variant) {
        TakeItAppVariant.ADMIN -> "Take It Admin HQ"
        TakeItAppVariant.DELIVERY -> "Take It Captain"
        TakeItAppVariant.RIDE -> "Take It Ride Smart"
        TakeItAppVariant.TRANSPORT -> "Take It Parcel Transport"
        else -> "Take It Food Delivery"
    }

    val subtitleText = when (variant) {
        TakeItAppVariant.ADMIN -> "Main Admin ($MAIN_ADMIN_EMAIL), Co-Admins & Restaurant Owners"
        TakeItAppVariant.DELIVERY -> "Food, RideX & MoveX jobs • Live Leaflet GPS, OTP & KYC"
        TakeItAppVariant.RIDE -> "Rapido-fast Bike, Auto & Cab rides at lowest fares"
        TakeItAppVariant.TRANSPORT -> "Send parcels, boxes & mini-truck cargo safely with live OTP"
        else -> "Hot meals from top restaurants • Live tracking & Razorpay"
    }

    val primaryBtnColor = when (variant) {
        TakeItAppVariant.ADMIN -> AdminGold
        TakeItAppVariant.DELIVERY -> DeliveryEmerald
        TakeItAppVariant.RIDE -> RideCharcoal
        TakeItAppVariant.TRANSPORT -> TransportOrange
        else -> TakeItRed
    }
    val primaryBtnDepth = when (variant) {
        TakeItAppVariant.ADMIN -> AdminGold.darken3D(0.45f)
        TakeItAppVariant.DELIVERY -> DeliveryEmerald.darken3D(0.45f)
        TakeItAppVariant.RIDE -> Color.Black
        TakeItAppVariant.TRANSPORT -> TransportOrange.darken3D(0.42f)
        else -> TakeItRedDark.darken3D(0.35f)
    }
    val primaryBtnContentColor = when (variant) {
        TakeItAppVariant.ADMIN -> AdminBg
        TakeItAppVariant.DELIVERY -> DeliveryBg
        TakeItAppVariant.RIDE -> RideYellow
        else -> Color.White
    }
    val accentBorder = when (variant) {
        TakeItAppVariant.ADMIN -> AdminGold
        TakeItAppVariant.DELIVERY -> DeliveryAmber
        TakeItAppVariant.RIDE -> RideYellow
        TakeItAppVariant.TRANSPORT -> TransportOrange
        else -> TakeItGold
    }
    val backdropBase = when (variant) {
        TakeItAppVariant.ADMIN -> AdminBg
        TakeItAppVariant.DELIVERY -> DeliveryBg
        TakeItAppVariant.RIDE -> RideCharcoal
        TakeItAppVariant.TRANSPORT -> TransportNavy
        else -> Color(0xFF7F1D1D)
    }

    TakeIt3DBackdrop(
        accentPrimary = accentBorder,
        accentSecondary = primaryBtnColor,
        darkMode = true,
        baseColor = backdropBase
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.Black.copy(alpha = 0.38f),
                    border = BorderStroke(1.2.dp, accentBorder.copy(alpha = 0.7f)),
                    shadowElevation = 6.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            when (variant) {
                                TakeItAppVariant.ADMIN -> Icons.Rounded.AdminPanelSettings
                                TakeItAppVariant.DELIVERY -> Icons.Rounded.DeliveryDining
                                TakeItAppVariant.RIDE -> Icons.Rounded.TwoWheeler
                                TakeItAppVariant.TRANSPORT -> Icons.Rounded.LocalShipping
                                else -> Icons.Rounded.Restaurant
                            },
                            contentDescription = null,
                            tint = accentBorder,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.9.sp
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                if (isDarkPanel) {
                    Box(
                        modifier = Modifier
                            .size(148.dp)
                            .graphicsLayer {
                                translationY = floatOffset
                                rotationY = adminTilt
                                cameraDistance = 14f * density
                            }
                            .shadow(22.dp, CircleShape, ambientColor = accentBorder, spotColor = accentBorder)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(accentBorder.lighten3D(0.35f), primaryBtnColor, accentBorder.darken3D(0.35f))))
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDelivery) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(DeliverySurface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DeliveryDining,
                                    contentDescription = "Take It Captain",
                                    tint = DeliveryEmerald,
                                    modifier = Modifier.size(76.dp)
                                )
                            }
                        } else {
                            Image(
                                painter = painterResource(R.drawable.logo_takeit_food),
                                contentDescription = "Take It Admin Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(Color.White)
                            )
                        }
                    }
                } else {
                    // 3D Perspective Welcome Artwork Banner with seamless radial/vertical edge fade
                    TakeIt3DHeroBanner(
                        borderColor = accentBorder.copy(alpha = 0.55f),
                        depthColor = primaryBtnDepth,
                        cornerRadius = 28.dp,
                        depth = 7.dp,
                        tiltAmplitude = 2.8f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(272.dp)
                            .graphicsLayer { translationY = floatOffset * 0.5f }
                    ) {
                        Image(
                            painter = painterResource(welcomeDrawable),
                            contentDescription = headlineText,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    scaleX = artScale
                                    scaleY = artScale
                                }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            backdropBase.copy(alpha = 0.35f),
                                            Color.Transparent,
                                            Color.Transparent,
                                            backdropBase.copy(alpha = 0.65f)
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = headlineText,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            // 3D Extruded Sign-In Card with Explicit Google Account Chooser!
            TakeIt3DCard(
                containerColor = when {
                    isAdmin -> AdminCard
                    isDelivery -> DeliveryCard
                    else -> Color.White
                },
                depthColor = if (isDarkPanel) Color(0xFF060A15) else Color(0xFFCBD5E1),
                borderColor = if (isDarkPanel) accentBorder.copy(alpha = 0.45f) else Color.White,
                cornerRadius = 28.dp,
                depth = 7.dp,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = when {
                            isAdmin -> "Choose Admin / Partner Google Account"
                            isDelivery -> "Choose Captain Google Account"
                            else -> "Choose your Google Account to continue"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isDarkPanel) DeliveryInk else TakeItInk,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        val sha1 = remember(context) { readInstalledSha1(context) }
                        val showSha = remember(errorMessage) {
                            val m = errorMessage?.lowercase() ?: ""
                            m.contains("certificate") || m.contains("hash") || m.contains("sha") || m.contains("cancelled")
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFFEE2E2),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    errorMessage.orEmpty(),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color(0xFFB91C1C),
                                    textAlign = TextAlign.Center
                                )
                                if (showSha && !sha1.isNullOrBlank()) {
                                    Text(
                                        "Pkg: ${context.packageName}\nAPK SHA-1: $sha1",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF7F1D1D),
                                        textAlign = TextAlign.Center
                                    )
                                    TextButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText("APK SHA-1", sha1))
                                            Toast.makeText(context, "Copied APK SHA-1", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.testTag("copy_sha1_button")
                                    ) {
                                        Icon(Icons.Rounded.ContentCopy, "Copy SHA-1", tint = Color(0xFF991B1B), modifier = Modifier.size(14.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Copy APK SHA-1", color = Color(0xFF991B1B), fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }

                    TakeIt3DButton(
                        onClick = {
                            errorMessage = null
                            isSigningIn = true
                            onGoogleSignInClicked(
                                context = context,
                                credentialManager = credentialManager,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    errorMessage = msg
                                },
                                scope = scope,
                                onAuthCancelled = { isSigningIn = false }
                            )
                        },
                        enabled = !isSigningIn && !isCheckingPending,
                        containerColor = primaryBtnColor,
                        depthColor = primaryBtnDepth,
                        contentColor = primaryBtnContentColor,
                        cornerRadius = 18.dp,
                        depth = 5.dp,
                        height = 52.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn || isCheckingPending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = primaryBtnContentColor,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(if (isCheckingPending) "Checking..." else "Opening Google Picker...", fontWeight = FontWeight.Bold)
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(Color.White),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = TakeItRed, fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text("Choose Google Account", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
                        }
                    }

                    TakeIt3DButton(
                        onClick = {
                            errorMessage = null
                            isSigningIn = true
                            signInWithGoogleBrowser(
                                context = context,
                                onAuthSuccess = {
                                    isSigningIn = false
                                    onAuthSuccess()
                                },
                                onAuthError = { msg ->
                                    isSigningIn = false
                                    errorMessage = msg
                                },
                                scope = scope
                            )
                        },
                        enabled = !isSigningIn && !isCheckingPending,
                        containerColor = if (isDarkPanel) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                        depthColor = if (isDarkPanel) Color(0xFF060A15) else Color(0xFFCBD5E1),
                        contentColor = if (isDarkPanel) AdminInk else TakeItInk,
                        cornerRadius = 16.dp,
                        depth = 4.dp,
                        height = 48.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("google_browser_sign_in_button")
                    ) {
                        Icon(
                            Icons.Rounded.SwitchAccount,
                            contentDescription = "Choose Another Gmail via Chrome",
                            tint = if (isDarkPanel) accentBorder else primaryBtnColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Select Account in Chrome",
                            color = if (isDarkPanel) AdminInk else TakeItInk,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Tip: Tap either 3D button above anytime to pick any Gmail account",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDarkPanel) AdminMuted else TakeItMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
