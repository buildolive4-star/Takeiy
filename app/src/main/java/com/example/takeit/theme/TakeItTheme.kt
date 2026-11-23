package com.example.takeit.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ZiporaTypography

// --- Zomato / FoodieX Inspired Customer Palette ---
val TakeItRed = Color(0xFFE23744)
val TakeItRedDark = Color(0xFFB91C2B)
val TakeItOrange = Color(0xFFFF5A1F)
val TakeItGold = Color(0xFFFFB800)
val TakeItGoldSoft = Color(0xFFFFF4CC)
val TakeItGreen = Color(0xFF16A34A)
val TakeItGreenSoft = Color(0xFFDCFCE7)
val TakeItBg = Color(0xFFF7F8FA)
val TakeItSurface = Color(0xFFFFFFFF)
val TakeItInk = Color(0xFF111827)
val TakeItMuted = Color(0xFF6B7280)
val TakeItBorder = Color(0xFFE5E7EB)
val TakeItNightBlue = Color(0xFF1E1B4B)

// --- Executive Dark Admin Command Center Palette ---
val AdminBg = Color(0xFF0A0F1E)
val AdminSurface = Color(0xFF121A2F)
val AdminCard = Color(0xFF1A2540)
val AdminBorder = Color(0xFF2B3A5C)
val AdminGold = Color(0xFFF59E0B)
val AdminEmerald = Color(0xFF10B981)
val AdminCrimson = Color(0xFFEF4444)
val AdminCyan = Color(0xFF38BDF8)
val AdminInk = Color(0xFFF8FAFC)
val AdminMuted = Color(0xFF94A3B8)

private val CustomerColorScheme = lightColorScheme(
    primary = TakeItRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE4E6),
    onPrimaryContainer = TakeItRedDark,
    secondary = TakeItOrange,
    onSecondary = Color.White,
    secondaryContainer = TakeItGoldSoft,
    onSecondaryContainer = TakeItInk,
    tertiary = TakeItGreen,
    onTertiary = Color.White,
    background = TakeItBg,
    onBackground = TakeItInk,
    surface = TakeItSurface,
    onSurface = TakeItInk,
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = TakeItMuted,
    outline = TakeItBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val AdminColorScheme = darkColorScheme(
    primary = AdminGold,
    onPrimary = AdminBg,
    primaryContainer = Color(0xFF3B2806),
    onPrimaryContainer = AdminGold,
    secondary = AdminCyan,
    onSecondary = AdminBg,
    secondaryContainer = AdminCard,
    onSecondaryContainer = AdminInk,
    tertiary = AdminEmerald,
    onTertiary = AdminBg,
    background = AdminBg,
    onBackground = AdminInk,
    surface = AdminSurface,
    onSurface = AdminInk,
    surfaceVariant = AdminCard,
    onSurfaceVariant = AdminMuted,
    outline = AdminBorder,
    error = AdminCrimson,
    onError = Color.White
)

val TakeItShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// --- Rapido Ride Inspired Palette ---
val RideYellow = Color(0xFFFACC15)
val RideYellowSoft = Color(0xFFFEF9C3)
val RideCharcoal = Color(0xFF111827)
val RideBlue = Color(0xFF2563EB)

// --- Parcel & Truck Transport Palette ---
val TransportNavy = Color(0xFF0F172A)
val TransportOrange = Color(0xFFF97316)
val TransportOrangeSoft = Color(0xFFFFEDD5)

private val RideColorScheme = lightColorScheme(
    primary = RideCharcoal,
    onPrimary = RideYellow,
    primaryContainer = RideYellow,
    onPrimaryContainer = RideCharcoal,
    secondary = RideBlue,
    onSecondary = Color.White,
    tertiary = TakeItGreen,
    onTertiary = Color.White,
    background = TakeItBg,
    onBackground = TakeItInk,
    surface = TakeItSurface,
    onSurface = TakeItInk,
    outline = TakeItBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

private val TransportColorScheme = lightColorScheme(
    primary = TransportOrange,
    onPrimary = Color.White,
    primaryContainer = TransportOrangeSoft,
    onPrimaryContainer = TransportNavy,
    secondary = TransportNavy,
    onSecondary = Color.White,
    tertiary = TakeItGreen,
    onTertiary = Color.White,
    background = TakeItBg,
    onBackground = TakeItInk,
    surface = TakeItSurface,
    onSurface = TakeItInk,
    outline = TakeItBorder,
    error = Color(0xFFDC2626),
    onError = Color.White
)

@Composable
fun TakeItCustomerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CustomerColorScheme,
        typography = ZiporaTypography,
        shapes = TakeItShapes,
        content = content
    )
}

@Composable
fun TakeItRideTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = RideColorScheme,
        typography = ZiporaTypography,
        shapes = TakeItShapes,
        content = content
    )
}

@Composable
fun TakeItTransportTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = TransportColorScheme,
        typography = ZiporaTypography,
        shapes = TakeItShapes,
        content = content
    )
}

@Composable
fun TakeItAdminTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AdminColorScheme,
        typography = ZiporaTypography,
        shapes = TakeItShapes,
        content = content
    )
}
