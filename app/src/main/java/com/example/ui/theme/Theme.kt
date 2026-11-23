package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ZiporaLightColors = lightColorScheme(
    primary = SkyBlue,
    onPrimary = DeepNavy,
    primaryContainer = ColorTokens.SkyContainer,
    onPrimaryContainer = DeepNavy,
    secondary = PremiumYellow,
    onSecondary = DeepNavy,
    secondaryContainer = ParcelCardStart,
    onSecondaryContainer = DeepNavy,
    tertiary = OrangeAccent,
    onTertiary = SoftWhite,
    background = LightSkyBackground,
    onBackground = DeepNavy,
    surface = SoftWhite,
    onSurface = DeepNavy,
    surfaceVariant = ColorTokens.SurfaceVariantLight,
    onSurfaceVariant = SlateText,
    outline = LightBorder,
    error = CoralDanger,
    onError = SoftWhite
)

private val ZiporaDarkColors = darkColorScheme(
    primary = SkyBlue,
    onPrimary = DarkBackground,
    primaryContainer = DeepNavyLight,
    onPrimaryContainer = SoftWhite,
    secondary = PremiumYellow,
    onSecondary = DeepNavy,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = PremiumYellow,
    tertiary = OrangeAccent,
    onTertiary = DeepNavy,
    background = DarkBackground,
    onBackground = SoftWhite,
    surface = DarkCardSurface,
    onSurface = SoftWhite,
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = ColorTokens.OnDarkMuted,
    outline = DarkBorder,
    error = CoralDanger,
    onError = SoftWhite
)

private object ColorTokens {
    val SkyContainer = androidx.compose.ui.graphics.Color(0xFFD6F1FE)
    val SurfaceVariantLight = androidx.compose.ui.graphics.Color(0xFFE6F4FE)
    val OnDarkMuted = androidx.compose.ui.graphics.Color(0xFF9FB3C8)
}

val ZiporaShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun ZiporaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) ZiporaDarkColors else ZiporaLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = ZiporaTypography,
        shapes = ZiporaShapes,
        content = content
    )
}
