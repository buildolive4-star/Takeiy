package com.example.takeit.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.firebase.attemptAutoSignIn
import com.example.firebase.onGoogleSignInClicked
import com.example.firebase.signInWithGoogleBrowser
import com.example.takeit.model.MAIN_ADMIN_EMAIL
import com.example.takeit.model.TakeItAppVariant
import com.example.takeit.theme.*
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
        animationSpec = tween(650),
        label = "logoScale"
    )
    LaunchedEffect(Unit) { visible = true }

    val isAdmin = variant == TakeItAppVariant.ADMIN

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isAdmin) {
                    Brush.verticalGradient(listOf(AdminBg, AdminSurface, Color(0xFF1E293B)))
                } else {
                    Brush.verticalGradient(listOf(Color(0xFF1F0709), TakeItRedDark, TakeItNightBlue))
                }
            )
    ) {
        if (!isAdmin) {
            Image(
                painter = painterResource(R.drawable.img_takeit_hero_burger_1795427328311),
                contentDescription = "Take It Gourmet Food",
                contentScale = ContentScale.Crop,
                alpha = 0.28f,
                modifier = Modifier.fillMaxSize()
            )
        }

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
                    .border(4.dp, if (isAdmin) AdminGold else TakeItGold, CircleShape)
                    .padding(6.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.logo_takeit_food),
                    contentDescription = "Take It Official Logo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape)
                )
            }

            Spacer(Modifier.height(22.dp))

            Text(
                text = if (isAdmin) "Take It Admin" else "Take It",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isAdmin) {
                    "Command Center • Live Store & Order Control"
                } else {
                    "Delicious Food, Delivered Happiness"
                },
                style = MaterialTheme.typography.bodyLarge,
                color = if (isAdmin) AdminGold else TakeItGoldSoft,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            CircularProgressIndicator(
                color = if (isAdmin) AdminGold else TakeItGold,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
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

    var isCheckingAuto by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val isAdmin = variant == TakeItAppVariant.ADMIN

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthSuccess()
            },
            onUnauthenticated = { isCheckingAuto = false },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                if (isAdmin) {
                    Brush.verticalGradient(listOf(AdminBg, AdminSurface, Color(0xFF0F172A)))
                } else {
                    Brush.verticalGradient(listOf(TakeItRedDark, TakeItRed, TakeItOrange, TakeItBg))
                }
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Logo + Brand Badge
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = Color.White.copy(alpha = 0.16f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            if (isAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.VerifiedUser,
                            contentDescription = null,
                            tint = if (isAdmin) AdminGold else TakeItGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            if (isAdmin) "EXECUTIVE ADMIN CONTROL APP" else "FAST FOOD DELIVERY APP",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.8.sp
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))

                Box(
                    modifier = Modifier
                        .size(144.dp)
                        .shadow(18.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(4.dp, if (isAdmin) AdminGold else TakeItGold, CircleShape)
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo_takeit_food),
                        contentDescription = "Take It Logo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = if (isAdmin) "Take It Admin HQ" else "Take It Food Delivery",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = if (isAdmin) {
                        "Main Admin ($MAIN_ADMIN_EMAIL) & Authorized Co-Admins only"
                    } else {
                        "Hot meals from top restaurants • Live order tracking & Razorpay checkout"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.88f),
                    textAlign = TextAlign.Center
                )
            }

            // Bottom Sign-In Card
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = if (isAdmin) AdminCard else Color.White,
                border = if (isAdmin) BorderStroke(1.dp, AdminBorder) else null,
                shadowElevation = 14.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isAdmin) "Sign in with Admin / Co-Admin Google Account" else "Sign in to order delicious food",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (isAdmin) AdminInk else TakeItInk,
                        fontWeight = FontWeight.Bold,
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
                        enabled = !isSigningIn && !isCheckingAuto,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isAdmin) AdminGold else TakeItRed,
                            contentColor = if (isAdmin) AdminBg else Color.White
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn || isCheckingAuto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = if (isAdmin) AdminBg else Color.White,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(if (isCheckingAuto) "Checking session..." else "Signing in...", fontWeight = FontWeight.Bold)
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
                            Text("Sign in with Google", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold)
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
                        enabled = !isSigningIn && !isCheckingAuto,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, if (isAdmin) AdminBorder else TakeItBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_browser_sign_in_button")
                    ) {
                        Icon(
                            Icons.Rounded.Public,
                            contentDescription = "Direct Chrome Google Login",
                            tint = if (isAdmin) AdminGold else TakeItRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Direct Google Login (Chrome)",
                            color = if (isAdmin) AdminInk else TakeItInk,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = if (isAdmin) "Secured by Firebase Cloud • Role-Based Access" else "Secured by Firebase Cloud • Take It Food Delivery",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isAdmin) AdminMuted else TakeItMuted
                    )
                }
            }
        }
    }
}
