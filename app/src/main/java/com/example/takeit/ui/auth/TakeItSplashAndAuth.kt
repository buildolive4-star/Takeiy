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
        targetValue = if (visible) 1f else 0.82f,
        animationSpec = tween(700),
        label = "splashScale"
    )
    LaunchedEffect(Unit) { visible = true }

    val inf = rememberInfiniteTransition(label = "splashKenBurns")
    val kenScale by inf.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "kenScale"
    )
    val kenShiftY by inf.animateFloat(
        initialValue = 0f,
        targetValue = -14f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "kenShiftY"
    )

    if (variant == TakeItAppVariant.ADMIN) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(AdminBg, AdminSurface, Color(0xFF1E293B))))
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
                        .size(164.dp)
                        .shadow(20.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, AdminGold, CircleShape)
                        .padding(6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_takeit_food),
                        contentDescription = "Take It Admin Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }
                Spacer(Modifier.height(22.dp))
                Text(
                    text = "TAKE IT ADMIN",
                    style = MaterialTheme.typography.headlineLarge,
                    color = AdminGold,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.6.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Executive Command Center • Live Cloud Sync",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(24.dp))
                CircularProgressIndicator(color = AdminGold, strokeWidth = 3.dp, modifier = Modifier.size(32.dp))
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

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(welcomeDrawable),
            contentDescription = "Take It Welcome Showcase",
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .scale(logoScale)
                .graphicsLayer {
                    scaleX = kenScale
                    scaleY = kenScale
                    translationY = kenShiftY
                }
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color.Black.copy(alpha = 0.12f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.65f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.65f),
                border = BorderStroke(1.dp, accent)
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
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
                )
            }
            CircularProgressIndicator(color = accent, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
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
    val isRide = variant == TakeItAppVariant.RIDE
    val isTransport = variant == TakeItAppVariant.TRANSPORT

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

    val inf = rememberInfiniteTransition(label = "authAnim")
    val artScale by inf.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(4200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "artScale"
    )
    val floatOffset by inf.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(2600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "floatOffset"
    )

    val welcomeDrawable = when (variant) {
        TakeItAppVariant.RIDE -> R.drawable.welcome_rapido_ride
        TakeItAppVariant.TRANSPORT -> R.drawable.welcome_parcel_transport
        else -> R.drawable.welcome_food_delivery
    }

    val badgeText = when (variant) {
        TakeItAppVariant.ADMIN -> "EXECUTIVE ADMIN CONTROL APP"
        TakeItAppVariant.RIDE -> "BIKE • AUTO • CAB • PARCEL"
        TakeItAppVariant.TRANSPORT -> "PARCEL & TRUCK TRANSPORT"
        else -> "FAST FOOD DELIVERY APP"
    }

    val headlineText = when (variant) {
        TakeItAppVariant.ADMIN -> "Take It Admin HQ"
        TakeItAppVariant.RIDE -> "Take It Ride Smart"
        TakeItAppVariant.TRANSPORT -> "Take It Parcel Transport"
        else -> "Take It Food Delivery"
    }

    val subtitleText = when (variant) {
        TakeItAppVariant.ADMIN -> "Main Admin ($MAIN_ADMIN_EMAIL) & Authorized Co-Admins only"
        TakeItAppVariant.RIDE -> "Rapido-fast Bike, Auto & Cab rides at lowest fares"
        TakeItAppVariant.TRANSPORT -> "Send parcels, boxes & mini-truck cargo safely with live OTP"
        else -> "Hot meals from top restaurants • Live tracking & Razorpay"
    }

    val primaryBtnColor = when (variant) {
        TakeItAppVariant.ADMIN -> AdminGold
        TakeItAppVariant.RIDE -> RideCharcoal
        TakeItAppVariant.TRANSPORT -> TransportOrange
        else -> TakeItRed
    }
    val primaryBtnContentColor = when (variant) {
        TakeItAppVariant.ADMIN -> AdminBg
        TakeItAppVariant.RIDE -> RideYellow
        else -> Color.White
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                when (variant) {
                    TakeItAppVariant.ADMIN -> Brush.verticalGradient(listOf(AdminBg, AdminSurface, Color(0xFF0F172A)))
                    TakeItAppVariant.RIDE -> Brush.verticalGradient(listOf(RideCharcoal, Color(0xFF1F2937), RideYellowSoft))
                    TakeItAppVariant.TRANSPORT -> Brush.verticalGradient(listOf(TransportNavy, Color(0xFF1E293B), TransportOrangeSoft))
                    else -> Brush.verticalGradient(listOf(TakeItRedDark, TakeItRed, TakeItOrange, TakeItBg))
                }
            )
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
                    color = Color.Black.copy(alpha = 0.28f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            when (variant) {
                                TakeItAppVariant.ADMIN -> Icons.Rounded.AdminPanelSettings
                                TakeItAppVariant.RIDE -> Icons.Rounded.TwoWheeler
                                TakeItAppVariant.TRANSPORT -> Icons.Rounded.LocalShipping
                                else -> Icons.Rounded.Restaurant
                            },
                            contentDescription = null,
                            tint = if (isAdmin || isRide) RideYellow else TakeItGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                if (isAdmin) {
                    Box(
                        modifier = Modifier
                            .size(144.dp)
                            .graphicsLayer { translationY = floatOffset }
                            .shadow(18.dp, CircleShape)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(4.dp, AdminGold, CircleShape)
                            .padding(5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo_takeit_food),
                            contentDescription = "Take It Admin Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    }
                } else {
                    // Animated Welcome Artwork Banner for Food, Rapido Ride, and Parcel Transport!
                    Surface(
                        shape = RoundedCornerShape(26.dp),
                        shadowElevation = 14.dp,
                        border = BorderStroke(2.5.dp, if (isRide) RideYellow else if (isTransport) TransportOrange else TakeItGold),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(275.dp)
                            .graphicsLayer { translationY = floatOffset }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
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
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

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
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center
                )
            }

            Spacer(Modifier.height(16.dp))

            // Bottom Sign-In Card with Explicit Google Account Chooser!
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (isAdmin) AdminCard else Color.White,
                border = if (isAdmin) BorderStroke(1.dp, AdminBorder) else null,
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = if (isAdmin) "Choose Admin / Co-Admin Google Account" else "Choose your Google Account to continue",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAdmin) AdminInk else TakeItInk,
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

                    Button(
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
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryBtnColor,
                            contentColor = primaryBtnContentColor
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
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

                    OutlinedButton(
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
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, if (isAdmin) AdminBorder else TakeItBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_browser_sign_in_button")
                    ) {
                        Icon(
                            Icons.Rounded.SwitchAccount,
                            contentDescription = "Choose Another Gmail via Chrome",
                            tint = if (isAdmin) AdminGold else primaryBtnColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Select Account in Chrome",
                            color = if (isAdmin) AdminInk else TakeItInk,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Tip: Tap either button above anytime to pick any Gmail account",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAdmin) AdminMuted else TakeItMuted,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
