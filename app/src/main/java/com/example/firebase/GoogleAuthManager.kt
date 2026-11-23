package com.example.firebase

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.ui.theme.DeepNavy
import com.example.ui.theme.MutedText
import com.example.ui.theme.PremiumYellow
import com.example.ui.theme.SkyBlue
import com.example.ui.theme.SoftWhite
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

/**
 * Reliable Firebase Google Sign-In via Chrome Custom Tab OAuthProvider ("google.com").
 * Bypasses OEM/ColorOS/MIUI CredentialManager cancellations while still producing a real Firebase Google user.
 */
fun signInWithGoogleBrowser(
    context: Context,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope
) {
    scope.launch {
        try {
            val activity = context.findActivity()
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val pending = Firebase.auth.pendingAuthResult
            if (pending != null) {
                pending.await()
            } else {
                val provider = OAuthProvider.newBuilder("google.com")
                    .addCustomParameter("prompt", "select_account")
                    .build()
                Firebase.auth.startActivityForSignInWithProvider(activity, provider).await()
            }
            if (Firebase.auth.currentUser != null) {
                onAuthSuccess()
            } else {
                onAuthError("Google Sign-In did not return a user session. Please try again.")
            }
        } catch (e: Exception) {
            Log.e("Auth", "Google browser OAuth Sign-In failed", e)
            onAuthError(e.localizedMessage ?: "Google Sign-In failed. Please try again.")
        }
    }
}

/**
 * Silent auto sign-in using strictly a single GetGoogleIdOption per request (API 36 safe).
 */
fun attemptAutoSignIn(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onUnauthenticated: () -> Unit,
    scope: CoroutineScope
) {
    if (Firebase.auth.currentUser != null) {
        onAuthSuccess()
        return
    }
    val pending = Firebase.auth.pendingAuthResult
    if (pending != null) {
        scope.launch {
            try {
                pending.await()
                if (Firebase.auth.currentUser != null) {
                    onAuthSuccess()
                } else {
                    onUnauthenticated()
                }
            } catch (_: Exception) {
                onUnauthenticated()
            }
        }
        return
    }
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        onUnauthenticated()
        return
    }
    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(clientId)
        .setAutoSelectEnabled(true)
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()
    scope.launch {
        try {
            val activity = context.findActivity() ?: run {
                onUnauthenticated()
                return@launch
            }
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                onUnauthenticated()
            }
        } catch (_: Exception) {
            onUnauthenticated()
        }
    }
}

/**
 * Interactive Google Sign-In: tries CredentialManager first, and automatically falls back
 * to Firebase Google OAuthProvider (Chrome Custom Tab) if OEM/Play Services cancels CredentialManager.
 */
fun onGoogleSignInClicked(
    context: Context,
    credentialManager: CredentialManager,
    onAuthSuccess: () -> Unit,
    onAuthError: (String) -> Unit,
    scope: CoroutineScope,
    onAuthCancelled: () -> Unit = {}
) {
    val clientId = try {
        context.getString(R.string.default_web_client_id)
    } catch (_: Exception) {
        signInWithGoogleBrowser(context, onAuthSuccess, onAuthError, scope)
        return
    }
    val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInOption)
        .build()
    scope.launch {
        try {
            val activity = context.findActivity()
                ?: throw IllegalStateException("Activity context required for Google Sign-In")
            val cred = credentialManager.getCredential(activity, request).credential
            if (cred is CustomCredential && cred.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val token = GoogleIdTokenCredential.createFrom(cred.data).idToken
                Firebase.auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
                onAuthSuccess()
            } else {
                signInWithGoogleBrowser(context, onAuthSuccess, onAuthError, scope)
            }
        } catch (e: GetCredentialCancellationException) {
            Log.w("Auth", "CredentialManager cancelled (${e.message}); falling back to Firebase Google OAuth", e)
            signInWithGoogleBrowser(context, onAuthSuccess, onAuthError, scope)
        } catch (e: Exception) {
            Log.w("Auth", "CredentialManager failed (${e.message}); falling back to Firebase Google OAuth", e)
            signInWithGoogleBrowser(context, onAuthSuccess, onAuthError, scope)
        }
    }
}

/**
 * Safe sign-out wrapped in NonCancellable so UI disposal cannot interrupt credential teardown.
 */
fun signOutFromTakeIt(
    credentialManager: CredentialManager,
    scope: CoroutineScope,
    onBeforeAuthSignOut: () -> Unit = {},
    onSignOutComplete: () -> Unit = {}
) {
    scope.launch {
        withContext(NonCancellable) {
            try {
                onBeforeAuthSignOut()
                credentialManager.clearCredentialState(ClearCredentialStateRequest())
            } catch (e: Exception) {
                Log.w("Auth", "Failed to clear credential state: ${e.message}")
            } finally {
                Firebase.auth.signOut()
                onSignOutComplete()
            }
        }
    }
}

@Composable
fun TakeItAuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isCheckingAuto by remember { mutableStateOf(true) }
    var isSigningIn by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {
                isCheckingAuto = false
                onAuthSuccess()
            },
            onUnauthenticated = {
                isCheckingAuto = false
            },
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0A2458), Color(0xFF1545A5), Color(0xFFF0F7FF))
                )
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
            // Top Brand Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(PremiumYellow),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.ShoppingBag, "Take It", tint = DeepNavy, modifier = Modifier.size(26.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Take It",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SoftWhite,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            // Center 3D Visual Card
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_takeit_main_hero_1795429839427),
                    contentDescription = "Take It All-in-One Super App",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(205.dp)
                        .shadow(14.dp, RoundedCornerShape(28.dp))
                        .clip(RoundedCornerShape(28.dp))
                )

                Text(
                    "Ride. Eat. Shop. Deliver.\nSynced Live in Cloud.",
                    style = MaterialTheme.typography.headlineSmall,
                    color = DeepNavy,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("7 Services", "9 Panels", "Live Cloud Sync").forEach { badge ->
                        Surface(shape = RoundedCornerShape(50), color = SoftWhite, shadowElevation = 2.dp) {
                            Text(
                                badge,
                                style = MaterialTheme.typography.labelMedium,
                                color = DeepNavy,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            // Bottom Sign-In Card
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = SoftWhite,
                shadowElevation = 10.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        "Sign in to sync your orders, wallet & partner dashboards",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedText,
                        textAlign = TextAlign.Center
                    )

                    if (errorMessage != null) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFEE2E2),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                errorMessage ?: "",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Center
                            )
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
                                onAuthCancelled = {
                                    isSigningIn = false
                                }
                            )
                        },
                        enabled = !isSigningIn && !isCheckingAuto,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1D4ED8),
                            contentColor = SoftWhite
                        ),
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("google_sign_in_button")
                    ) {
                        if (isSigningIn || isCheckingAuto) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = SoftWhite,
                                strokeWidth = 2.5.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                if (isCheckingAuto) "Checking account..." else "Signing in...",
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(SoftWhite),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("G", color = Color(0xFF1D4ED8), fontWeight = FontWeight.ExtraBold)
                            }
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "Sign in with Google",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("google_browser_sign_in_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Public,
                            contentDescription = "Direct Google Login via Chrome",
                            tint = Color(0xFF1D4ED8),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Direct Google Login (Chrome)",
                            style = MaterialTheme.typography.labelLarge,
                            color = DeepNavy,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        "Secured by Firebase Auth & Firestore Cloud",
                        style = MaterialTheme.typography.labelSmall,
                        color = MutedText
                    )
                }
            }
        }
    }
}
